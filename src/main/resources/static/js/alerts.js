document.addEventListener("DOMContentLoaded", () => {
  const body = document.getElementById("alertsBody");
  const statusFilter = document.getElementById("statusFilter");
  const severityFilter = document.getElementById("severityFilter");
  const timeRangeFilter = document.getElementById("timeRangeFilter");

  // Debounce helper to avoid firing too many requests while user toggles filters
  function debounce(fn, wait) {
    let t = null;
    return (...args) => {
      if (t) clearTimeout(t);
      t = setTimeout(() => fn.apply(null, args), wait);
    };
  }

  async function loadAlerts() {
    window.HawkUI.showLoader();
    const q = new URLSearchParams();
    if (statusFilter.value) q.set("status", statusFilter.value);
    if (severityFilter.value) q.set("severity", severityFilter.value);
    const query = q.toString();
    const data = await window.HawkUI.apiRequest(`/api/v1/alerts${query ? `?${query}` : ""}`);
    let rows = Array.isArray(data) ? data : [];
    // Apply client-side time-range filtering if requested (value is minutes)
    try {
      const trv = timeRangeFilter?.value || 'ALL';
      if (trv && trv !== 'ALL') {
        const minutes = Number.parseInt(trv, 10);
        if (!Number.isNaN(minutes) && minutes > 0) {
          const threshold = Date.now() - (minutes * 60 * 1000);
          rows = rows.filter((a) => {
            const t = a.createdAt ? Date.parse(a.createdAt) : NaN;
            return !Number.isNaN(t) && t >= threshold;
          });
        }
      }
    } catch (e) {
      // if anything goes wrong with client filtering, skip it and show full list
      console.warn('Time-range filter failed', e);
    }
    window.HawkUI.hideLoader();

    body.innerHTML = "";
    if (rows.length === 0) {
      body.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--hawk-grey);padding:20px;">No alerts found</td></tr>`;
      return;
    }
      rows.forEach((a) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td><strong>#${a.alertId}</strong></td>
        <td>${a.ruleName ? a.ruleName : a.ruleId}</td>
        <td>${a.transactionId}</td>
        <td><span class="badge ${window.HawkUI.statusClass(a.severity)}">${a.severity}</span></td>
        <td><span class="badge ${window.HawkUI.statusClass(a.status)}">${a.status}</span></td>
        <td>${window.HawkUI.fmtDate(a.createdAt)}</td>
        <td><a href="/alert-detail.html?id=${a.alertId}" style="color:var(--hawk-red);font-weight:500;">Investigate →</a></td>
      `;
      body.appendChild(tr);
    });
  }

  document.getElementById("alertFilters").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await loadAlerts();
    } catch (err) {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast(err.message);
    }
  });

  // Debounced loader for interactive filtering
  const debouncedLoad = debounce(() => {
    loadAlerts().catch((err) => {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast(err.message);
    });
  }, 300);

  // Wire change events so selecting filters auto-loads results
  if (statusFilter) statusFilter.addEventListener('change', debouncedLoad);
  if (severityFilter) severityFilter.addEventListener('change', debouncedLoad);
  if (timeRangeFilter) timeRangeFilter.addEventListener('change', debouncedLoad);

  // Initial load
  loadAlerts().catch((err) => {
    window.HawkUI.hideLoader();
    window.HawkUI.showToast(err.message);
  });
});

