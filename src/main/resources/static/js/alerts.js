document.addEventListener("DOMContentLoaded", () => {
  const body = document.getElementById("alertsBody");
  const statusFilter = document.getElementById("statusFilter");
  const severityFilter = document.getElementById("severityFilter");

  async function loadAlerts() {
    window.HawkUI.showLoader();
    const q = new URLSearchParams();
    if (statusFilter.value) q.set("status", statusFilter.value);
    if (severityFilter.value) q.set("severity", severityFilter.value);
    const query = q.toString();
    const data = await window.HawkUI.apiRequest(`/api/v1/alerts${query ? `?${query}` : ""}`);
    const rows = Array.isArray(data) ? data : [];
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

  loadAlerts().catch((err) => {
    window.HawkUI.hideLoader();
    window.HawkUI.showToast(err.message);
  });
});

