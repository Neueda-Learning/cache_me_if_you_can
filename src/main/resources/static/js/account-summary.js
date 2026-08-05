document.addEventListener("DOMContentLoaded", async () => {
  const tbody = document.getElementById("summaryBody");
  const subtitle = document.getElementById("summarySubtitle");
  const sumAccountId = document.getElementById("sumAccountId");
  const sumTxCount = document.getElementById("sumTxCount");
  const sumTotalAmount = document.getElementById("sumTotalAmount");

  const params = new URLSearchParams(window.location.search);
  const accountIdRaw = params.get("accountId");
  const accountId = Number(accountIdRaw);

  if (!Number.isInteger(accountId) || accountId <= 0) {
    window.HawkUI.showToast("Invalid accountId. Please open from Accounts page.");
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--hawk-grey);padding:20px;">Invalid account selected.</td></tr>`;
    return;
  }

  window.HawkUI.showLoader();
  try {
    const [accountsResp, txResp, payeesResp, alertsResp, summaryResp] = await Promise.all([
      window.HawkUI.apiRequest("/api/accounts"),
      window.HawkUI.apiRequest(`/api/transactions/account/${accountId}`),
      window.HawkUI.apiRequest("/api/payees"),
      window.HawkUI.apiRequest("/api/v1/alerts"),
      window.HawkUI.apiRequest(`/api/transactions/account/${accountId}/summary`),
    ]);

    const accounts = Array.isArray(accountsResp) ? accountsResp : [];
    const txList = Array.isArray(txResp) ? txResp : [];
    const payees = Array.isArray(payeesResp) ? payeesResp : [];
    const alerts = Array.isArray(alertsResp) ? alertsResp : [];

    const account = accounts.find((a) => Number(a.accountId) === accountId);
    if (account) {
      subtitle.textContent = `${account.accountHolderName} (${account.accountNumber}) - transaction history with alert traceability.`;
    }

    sumAccountId.textContent = String(accountId);
    sumTxCount.textContent = String(summaryResp?.totalTransactions ?? txList.length);
    sumTotalAmount.textContent = `₹${window.HawkUI.fmtAmount(summaryResp?.totalAmount ?? 0)}`;

    const payeeById = new Map(payees.map((p) => [Number(p.payeeId), p]));
    const alertsByTx = new Map();

    alerts.forEach((a) => {
      const txId = Number(a.transactionId);
      if (!alertsByTx.has(txId)) alertsByTx.set(txId, []);
      alertsByTx.get(txId).push(a);
    });

    tbody.innerHTML = "";
    if (txList.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--hawk-grey);padding:20px;">No transactions found for this account.</td></tr>`;
      return;
    }

    txList.forEach((tx) => {
      const payee = payeeById.get(Number(tx.payeeId));
      const payeeName = payee?.payeeName || `Payee #${tx.payeeId}`;
      const payeeAcc = payee?.payeeAccountNumber || "-";
      const generatedAlerts = alertsByTx.get(Number(tx.transactionId)) || [];

      const alertCell = generatedAlerts.length === 0
        ? `<span class="badge status-closed">No Alerts</span>`
        : generatedAlerts.map((a) =>
            `<a href="/alert-detail.html?id=${a.alertId}" style="display:inline-block;margin:2px 4px 2px 0;">` +
            `<span class="badge ${window.HawkUI.statusClass(a.severity)}">#${a.alertId} ${a.severity} ${a.status}</span></a>`
          ).join("");

      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td><strong>#${tx.transactionId}</strong></td>
        <td>${payeeName}</td>
        <td><code>${payeeAcc}</code></td>
        <td>₹${window.HawkUI.fmtAmount(tx.amount)}</td>
        <td>${tx.transactionType}</td>
        <td>${window.HawkUI.fmtDate(tx.timeStamp)}</td>
        <td>${alertCell}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--hawk-grey);padding:20px;">Failed to load account summary.</td></tr>`;
    window.HawkUI.showToast("Failed to load account summary: " + err.message);
  } finally {
    window.HawkUI.hideLoader();
  }
});

