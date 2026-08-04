document.addEventListener("DOMContentLoaded", () => {
  const id = new URLSearchParams(window.location.search).get("id");
  const title = document.getElementById("alertTitle");
  const statusEl = document.getElementById("alertStatus");
  const severityEl = document.getElementById("alertSeverity");
  const ruleEl = document.getElementById("alertRule");
  const createdEl = document.getElementById("alertCreated");
  const txEl = document.getElementById("alertTx");
  const accountEl = document.getElementById("alertAccount");
  const payeeEl = document.getElementById("alertPayee");
  const actionWrap = document.getElementById("alertActions");

  if (!id) {
    window.HawkUI.showToast("Missing alert id in URL");
    return;
  }

  async function setStatus(nextStatus) {
    await window.HawkUI.apiRequest(`/api/v1/alerts/${id}/status`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: nextStatus }),
    });
    if (nextStatus === "CLOSED" || nextStatus === "DISMISSED") {
      window.HawkUI.clearAlertNotification(id);
    }
    await load();
    window.HawkUI.showToast(`Alert moved to ${nextStatus}`);
  }

  function renderActions(status) {
    actionWrap.innerHTML = "";
    const addBtn = (label, next) => {
      const b = document.createElement("button");
      b.className = next === "CLOSED" ? "btn-secondary" : "btn-primary";
      b.textContent = label;
      b.addEventListener("click", () => setStatus(next).catch((e) => window.HawkUI.showToast(e.message)));
      actionWrap.appendChild(b);
    };

    if (status === "OPEN") {
      addBtn("Acknowledge", "ACKNOWLEDGED");
      addBtn("Dismiss", "DISMISSED");
    } else if (status === "ACKNOWLEDGED") {
      addBtn("Mark Investigating", "INVESTIGATING");
      addBtn("Dismiss", "DISMISSED");
    } else if (status === "INVESTIGATING") {
      addBtn("Close Alert", "CLOSED");
      addBtn("Dismiss", "DISMISSED");
    }
  }

  async function load() {
    window.HawkUI.showLoader();
    const a = await window.HawkUI.apiRequest(`/api/v1/alerts/${id}`);
    window.HawkUI.hideLoader();
    window.HawkUI.syncAlertNotificationState([a]);
    title.textContent = `Alert #${a.alertId}`;
    statusEl.innerHTML = `<span class="badge ${window.HawkUI.statusClass(a.status)}">${a.status}</span>`;
    severityEl.innerHTML = `<span class="badge ${window.HawkUI.statusClass(a.severity)}">${a.severity}</span>`;
    ruleEl.textContent = a.ruleName ? String(a.ruleName) : String(a.ruleId);
    createdEl.textContent = window.HawkUI.fmtDate(a.createdAt);
    txEl.textContent = String(a.transactionId);
    accountEl.textContent = a.accountNumber ? String(a.accountNumber) : "—";
    payeeEl.textContent = a.payeeAccountNumber ? String(a.payeeAccountNumber) : "—";
    renderActions(a.status);
  }

  load().catch((err) => {
    window.HawkUI.hideLoader();
    window.HawkUI.showToast(err.message);
  });
});
