document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("txBody");
  const filterForm = document.getElementById("txFilters");
  const stats = {
    count: document.getElementById("statCount"),
    volume: document.getElementById("statVolume"),
  };

  async function loadTransactions() {
    window.HawkUI.showLoader();
    const accountId = document.getElementById("accountId").value.trim();
    const from = document.getElementById("from").value;
    const to = document.getElementById("to").value;

    let data;
    if (accountId) {
      data = await window.HawkUI.apiRequest(`/api/transactions/account/${encodeURIComponent(accountId)}`);
    } else if (from && to) {
      data = await window.HawkUI.apiRequest(`/api/transactions/range?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
    } else {
      const now = window.HawkUI.nowLocalIsoNoZone();
      const d = new Date();
      d.setDate(d.getDate() - 7);
      const start = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}T00:00:00`;
      data = await window.HawkUI.apiRequest(`/api/transactions/range?from=${start}&to=${now}`);
    }

    const rows = Array.isArray(data) ? data : [];
    window.HawkUI.hideLoader();

    tbody.innerHTML = "";
    if (rows.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--hawk-grey);padding:20px;">No transactions found</td></tr>`;
      stats.count.textContent = "0";
      stats.volume.textContent = "0.00";
      return;
    }
    rows.forEach((tx) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${tx.transactionId ?? "-"}</td>
        <td>${tx.accountId}</td>
        <td>${tx.payeeId}</td>
        <td>${window.HawkUI.fmtAmount(tx.amount)}</td>
        <td>${tx.transactionType || "-"}</td>
        <td>${window.HawkUI.fmtDate(tx.timeStamp)}</td>
      `;
      tbody.appendChild(tr);
    });

    const total = rows.reduce((sum, item) => sum + Number(item.amount || 0), 0);
    stats.count.textContent = String(rows.length);
    stats.volume.textContent = window.HawkUI.fmtAmount(total);
  }

  filterForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await loadTransactions();
    } catch (err) {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast(err.message);
    }
  });

  document.getElementById("btnClear").addEventListener("click", () => {
    filterForm.reset();
  });

  loadTransactions().catch((err) => {
    window.HawkUI.hideLoader();
    window.HawkUI.showToast(err.message);
  });
});

