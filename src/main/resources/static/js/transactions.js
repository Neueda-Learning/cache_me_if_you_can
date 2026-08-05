document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("txBody");
  const filterForm = document.getElementById("txFilters");
  const filterByEl = document.getElementById("filterBy");
  const filterValueEl = document.getElementById("filterValue");
  const fromEl = document.getElementById("from");
  const toEl = document.getElementById("to");
  const stats = {
    count: document.getElementById("statCount"),
    volume: document.getElementById("statVolume"),
  };

  function toEpoch(value) {
    if (!value) return null;
    const t = new Date(value).getTime();
    return Number.isNaN(t) ? null : t;
  }

  function inDateRange(txTimeStamp, from, to) {
    const ts = toEpoch(txTimeStamp);
    if (ts === null) return true;
    const fromTs = toEpoch(from);
    const toTs = toEpoch(to);
    if (fromTs !== null && ts < fromTs) return false;
    if (toTs !== null && ts > toTs) return false;
    return true;
  }

  async function loadTransactions() {
    window.HawkUI.showLoader();
    const filterBy = (filterByEl?.value || "ALL").trim();
    const filterValue = (filterValueEl?.value || "").trim();
    const from = fromEl?.value || "";
    const to = toEl?.value || "";

    const params = new URLSearchParams();
    if (filterBy) params.set("filterBy", filterBy);
    if (filterValue) params.set("value", filterValue);

    const data = await window.HawkUI.apiRequest(`/api/transactions/list?${params.toString()}`);
    const rows = (Array.isArray(data) ? data : []).filter((tx) => inDateRange(tx.timeStamp, from, to));
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
        <td>${tx.accountNumber || "-"}</td>
        <td>${tx.payeeAccountNumber || "-"}</td>
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

  // Dynamic filter input behaviour ------------------------------------------------
  function updateFilterInput() {
    if (!filterByEl || !filterValueEl) return;
    const val = (filterByEl.value || "ALL");
    switch (val) {
      case 'transactionId':
        filterValueEl.placeholder = 'Enter Transaction ID';
        filterValueEl.style.display = '';
        filterValueEl.type = 'number';
        break;
      case 'accountNumber':
        filterValueEl.placeholder = 'Enter Account Number';
        filterValueEl.style.display = '';
        filterValueEl.type = 'text';
        break;
      case 'payeeAccountNumber':
        filterValueEl.placeholder = 'Enter Payee Account Number';
        filterValueEl.style.display = '';
        filterValueEl.type = 'text';
        break;
      default:
        filterValueEl.style.display = 'none';
        filterValueEl.value = '';
        break;
    }
  }

  if (filterByEl) filterByEl.addEventListener('change', updateFilterInput);

  document.getElementById("btnClear").addEventListener("click", () => {
    filterForm.reset();
    updateFilterInput();
    loadTransactions().catch((err) => {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast(err.message);
    });
  });

  // Initialize UI state and load
  updateFilterInput();
  loadTransactions().catch((err) => {
    window.HawkUI.hideLoader();
    window.HawkUI.showToast(err.message);
  });
});

