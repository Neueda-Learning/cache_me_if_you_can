document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("txBody");
  const filterForm = document.getElementById("txFilters");
  const filterByEl = document.getElementById("filterBy");
  const filterValueEl = document.getElementById("filterValue");
  const stats = {
    count: document.getElementById("statCount"),
    volume: document.getElementById("statVolume"),
  };

  async function loadTransactions() {
    window.HawkUI.showLoader();
    const filterBy = (filterByEl && filterByEl.value) ? filterByEl.value : 'ALL';
    const filterValue = (filterValueEl && filterValueEl.value) ? filterValueEl.value.trim() : '';
    const from = document.getElementById("from").value;
    const to = document.getElementById("to").value;

    let data;
    if (filterBy && filterBy !== 'ALL' && filterValue) {
      // Use the new legend stored-proc endpoint when filtering by id or account numbers
      data = await window.HawkUI.apiRequest(`/api/transactions/list?filterBy=${encodeURIComponent(filterBy)}&value=${encodeURIComponent(filterValue)}`);
    } else if (filterBy === 'ALL') {
      // When 'All' is selected, prefer the list endpoint so the UI reflects joined account/payee numbers.
      // If a date range is provided, the range endpoint is more appropriate and will be used below.
      if (from && to) {
        data = await window.HawkUI.apiRequest(`/api/transactions/range?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
      } else {
        data = await window.HawkUI.apiRequest(`/api/transactions/list?filterBy=ALL&value=`);
      }
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
        <td>${tx.accountNumber ?? (tx.accountId ?? "-")}</td>
        <td>${tx.payeeAccountNumber ?? (tx.payeeId ?? "-")}</td>
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
  // Update the placeholder / enabled state of the filter value input based on selection
  function updateFilterInput() {
    if (!filterByEl || !filterValueEl) return;
    const v = filterByEl.value;
    switch (v) {
      case 'transactionId':
        filterValueEl.placeholder = 'Enter Transaction ID';
        filterValueEl.style.display = '';
        filterValueEl.disabled = false;
        filterValueEl.type = 'number';
        filterValueEl.min = 1;
        break;
      case 'accountNumber':
        filterValueEl.placeholder = 'Enter Account Number';
        filterValueEl.style.display = '';
        filterValueEl.disabled = false;
        filterValueEl.type = 'text';
        break;
      case 'payeeAccountNumber':
        filterValueEl.placeholder = 'Enter Payee Account Number';
        filterValueEl.style.display = '';
        filterValueEl.disabled = false;
        filterValueEl.type = 'text';
        break;
      default:
        filterValueEl.placeholder = '';
        filterValueEl.style.display = 'none';
        filterValueEl.disabled = true;
        filterValueEl.type = 'text';
        break;
    }
  }

  if (filterByEl) {
    filterByEl.addEventListener('change', updateFilterInput);
    // initialize
    updateFilterInput();
  }

  filterForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await loadTransactions();
    } catch (err) {
      window.HawkUI.hideLoader();
      console.error('loadTransactions error', err);
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

