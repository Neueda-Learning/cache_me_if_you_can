document.addEventListener("DOMContentLoaded", async () => {
  window.HawkUI.showLoader();

  const now = new Date();
  const toIso = window.HawkUI.nowLocalIsoNoZone();

  function daysAgoIso(days) {
    const d = new Date(now.getTime() - days * 86400000);
    const p = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T00:00:00`;
  }

  const CHART_DEFAULTS = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: "nearest", intersect: false },
    plugins: {
      legend: { labels: { font: { size: 11 }, boxWidth: 12 } },
      tooltip: {
        enabled: true,
        displayColors: true,
        callbacks: {
          label(context) {
            const value = context.parsed?.y ?? context.parsed ?? 0;
            return `${context.dataset.label || context.label}: ${value}`;
          },
        },
      },
    },
  };

  function countBy(arr, key) {
    return arr.reduce((acc, item) => {
      const k = item[key] || "UNKNOWN";
      acc[k] = (acc[k] || 0) + 1;
      return acc;
    }, {});
  }

  function buildDoughnut(id, labels, data, colors) {
    new Chart(document.getElementById(id), {
      type: "doughnut",
      data: { labels, datasets: [{ data, backgroundColor: colors, borderWidth: 2, borderColor: "#fff" }] },
      options: { ...CHART_DEFAULTS, cutout: "62%" },
    });
  }

  function buildBar(id, labels, data, colors, horizontal) {
    new Chart(document.getElementById(id), {
      type: "bar",
      data: {
        labels,
        datasets: [{
          label: "Count",
          data,
          backgroundColor: colors,
          borderRadius: 5,
          borderSkipped: false,
        }],
      },
      options: {
        ...CHART_DEFAULTS,
        indexAxis: horizontal ? "y" : "x",
        plugins: { ...CHART_DEFAULTS.plugins, legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { stepSize: 1, font: { size: 11 } }, grid: { color: "#F0F0F0" } },
          x: { beginAtZero: true, ticks: { stepSize: 1, font: { size: 11 } }, grid: { color: "#F8F8F8" } },
        },
      },
    });
  }

  function buildLine(id, labels, data, label, color, fillColor) {
    new Chart(document.getElementById(id), {
      type: "line",
      data: {
        labels,
        datasets: [{
          label,
          data,
          borderColor: color,
          backgroundColor: fillColor,
          fill: true,
          tension: 0.35,
          pointBackgroundColor: color,
          pointBorderColor: "#fff",
          pointBorderWidth: 1,
          pointRadius: 4,
          pointHoverRadius: 7,
          borderWidth: 2,
        }],
      },
      options: {
        ...CHART_DEFAULTS,
        plugins: { ...CHART_DEFAULTS.plugins, legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { stepSize: 1, font: { size: 11 } }, grid: { color: "#F0F0F0" } },
          x: { ticks: { font: { size: 10 } }, grid: { display: false } },
        },
      },
    });
  }

  function buildPolar(id, labels, data, colors) {
    new Chart(document.getElementById(id), {
      type: "polarArea",
      data: { labels, datasets: [{ label: "Transactions", data, backgroundColor: colors }] },
      options: { ...CHART_DEFAULTS, plugins: { ...CHART_DEFAULTS.plugins, legend: { position: "right" } } },
    });
  }

  function buildTxTrendData(txList) {
    const days = [];
    const counts = {};
    for (let i = 6; i >= 0; i--) {
      const d = new Date(now.getTime() - i * 86400000);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
      days.push(key);
      counts[key] = 0;
    }
    (txList || []).forEach((tx) => {
      const ds = tx.timeStamp ? String(tx.timeStamp).substring(0, 10) : null;
      if (ds && counts[ds] !== undefined) counts[ds]++;
    });
    return {
      labels: days.map((d) => `${d.substring(8, 10)}/${d.substring(5, 7)}`),
      data: days.map((d) => counts[d]),
    };
  }

  function buildHourlyData(txList) {
    const labels = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, "0")}:00`);
    const data = Array(24).fill(0);
    (txList || []).forEach((tx) => {
      const t = tx.timeStamp ? new Date(tx.timeStamp) : null;
      if (t && !Number.isNaN(t.getTime())) data[t.getHours()] += 1;
    });
    return { labels, data };
  }

  function buildAmountBands(txList) {
    const bands = { "0-100": 0, "100-500": 0, "500-1000": 0, "1000+": 0 };
    (txList || []).forEach((tx) => {
      const amount = Number(tx.amount || 0);
      if (amount < 100) bands["0-100"] += 1;
      else if (amount < 500) bands["100-500"] += 1;
      else if (amount < 1000) bands["500-1000"] += 1;
      else bands["1000+"] += 1;
    });
    return bands;
  }

  try {
    // Use the same joined transaction list endpoint as the transactions page
    // for consistency; then compute the 24h and 7-day slices client-side.
    const [openAlerts, allAlerts, allTx] = await Promise.all([
      window.HawkUI.apiRequest("/api/v1/alerts?status=OPEN"),
      window.HawkUI.apiRequest("/api/v1/alerts"),
      window.HawkUI.apiRequest(`/api/transactions/list?filterBy=ALL&value=`),
    ]);

    const openArr = Array.isArray(openAlerts) ? openAlerts : [];
    const allArr = Array.isArray(allAlerts) ? allAlerts : [];
    const allTxArr = Array.isArray(allTx) ? allTx : [];

    const nowMs = now.getTime();
    const ms24 = 24 * 60 * 60 * 1000;
    const ms7 = 7 * 24 * 60 * 60 * 1000;
    const todayArr = allTxArr.filter((tx) => {
      try { const t = new Date(tx.timeStamp); return !Number.isNaN(t.getTime()) && (nowMs - t.getTime()) <= ms24; } catch (_) { return false; }
    });
    const tx7Arr = allTxArr.filter((tx) => {
      try { const t = new Date(tx.timeStamp); return !Number.isNaN(t.getTime()) && (nowMs - t.getTime()) <= ms7; } catch (_) { return false; }
    });

    document.getElementById("kpiOpen").textContent = String(openArr.length);
    document.getElementById("kpiTx").textContent = String(todayArr.length);
    document.getElementById("kpiAlerts").textContent = String(allArr.length);
    const vol = todayArr.reduce((s, t) => s + Number(t.amount || 0), 0);
    document.getElementById("kpiVolume").textContent = "₹" + window.HawkUI.fmtAmountCompact(vol);

    const statusCounts = countBy(allArr, "status");
    const statusLabels = ["OPEN", "ACKNOWLEDGED", "INVESTIGATING", "CLOSED", "DISMISSED"];
    const statusColors = ["#DB0011", "#F5A623", "#0066CC", "#00854A", "#9FA1A4"];
    buildDoughnut(
      "chartAlertStatus",
      statusLabels.filter((s) => statusCounts[s]),
      statusLabels.filter((s) => statusCounts[s]).map((s) => statusCounts[s]),
      statusLabels.filter((s) => statusCounts[s]).map((s) => statusColors[statusLabels.indexOf(s)]),
    );

    const sevCounts = countBy(allArr, "severity");
    const sevLabels = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
    buildBar("chartAlertSeverity", sevLabels, sevLabels.map((s) => sevCounts[s] || 0), ["#DB0011", "#FF6B35", "#F5A623", "#00854A"], false);

    const trend = buildTxTrendData(tx7Arr);
    buildLine("chartTxTrend", trend.labels, trend.data, "Transactions", "#DB0011", "rgba(219,0,17,0.08)");

    const hourly = buildHourlyData(todayArr);
    buildLine("chartTxHourly", hourly.labels, hourly.data, "Transactions per hour", "#4B6CB7", "rgba(75,108,183,0.10)");

    const bands = buildAmountBands(tx7Arr);
    buildPolar("chartAmountBands", Object.keys(bands), Object.values(bands), ["#E57373", "#FFB74D", "#64B5F6", "#81C784"]);

    buildBar(
      "chartLifecycle",
      statusLabels,
      statusLabels.map((s) => statusCounts[s] || 0),
      ["#DB0011", "#F5A623", "#2E86DE", "#2ECC71", "#95A5A6"],
      true,
    );

    const tbody = document.getElementById("recentAlertsBody");
    tbody.innerHTML = "";
    const feedAlerts = allArr
      .slice()
      .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
      .slice(0, 6);

    if (feedAlerts.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--hawk-grey);padding:20px;">No alerts found</td></tr>`;
    } else {
      feedAlerts.forEach((a) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td><strong>#${a.alertId}</strong></td>
          <td><span class="badge ${window.HawkUI.statusClass(a.severity)}">${a.severity}</span></td>
          <td><span class="badge ${window.HawkUI.statusClass(a.status)}">${a.status}</span></td>
          <td>${window.HawkUI.fmtDate(a.createdAt)}</td>
          <td><a href="/alert-detail.html?id=${a.alertId}" style="color:var(--hawk-red);font-weight:500;">Investigate -&gt;</a></td>
        `;
        tbody.appendChild(tr);
      });
    }
  } catch (err) {
    window.HawkUI.showToast("Failed to load dashboard data: " + err.message);
  } finally {
    window.HawkUI.hideLoader();
  }

  const closeBtn   = document.getElementById("alertPopupClose");
  const dismissBtn = document.getElementById("alertPopupDismiss");
  const alertPopup = document.getElementById("alertPopup");
  if (closeBtn)   closeBtn.addEventListener("click", () => { alertPopup.style.display = "none"; });
  if (dismissBtn) dismissBtn.addEventListener("click", () => { alertPopup.style.display = "none"; });
  if (alertPopup) alertPopup.addEventListener("click", (e) => { if (e.target === alertPopup) alertPopup.style.display = "none"; });
});
