document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("txBody");
  const filterForm = document.getElementById("txFilters");
  const filterByEl = document.getElementById("filterBy");
  const filterValueEl = document.getElementById("filterValue");
  const suggestionListEl = document.getElementById("txFilterSuggestions");
  const fromEl = document.getElementById("from");
  const toEl = document.getElementById("to");
  const stats = {
    count: document.getElementById("statCount"),
    volume: document.getElementById("statVolume"),
  };
  let allRowsCache = [];

  // Simple debounce to avoid firing too many requests while the user is typing
  function debounce(fn, wait) {
    let t = null;
    return (...args) => {
      if (t) clearTimeout(t);
      t = setTimeout(() => fn.apply(null, args), wait);
    };
  }

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

  function rowMatchesFilter(tx, filterBy, filterValue) {
    if (!filterValue || filterBy === "ALL") return true;
    const needle = String(filterValue).toLowerCase();
    if (filterBy === "transactionId") return String(tx.transactionId || "").toLowerCase().includes(needle);
    if (filterBy === "accountNumber") return String(tx.accountNumber || "").toLowerCase().includes(needle);
    if (filterBy === "payeeAccountNumber") return String(tx.payeeAccountNumber || "").toLowerCase().includes(needle);
    return true;
  }

  function updateTypeaheadSuggestions() {
    if (!suggestionListEl || !filterByEl || !filterValueEl) return;
    const filterBy = filterByEl.value || "ALL";
    const query = (filterValueEl.value || "").trim().toLowerCase();
    const source = new Set();

    if (filterBy === "transactionId") {
      allRowsCache.forEach((row) => source.add(String(row.transactionId || "")));
    } else if (filterBy === "accountNumber") {
      allRowsCache.forEach((row) => source.add(String(row.accountNumber || "")));
    } else if (filterBy === "payeeAccountNumber") {
      allRowsCache.forEach((row) => source.add(String(row.payeeAccountNumber || "")));
    }

    const options = Array.from(source)
      .filter((v) => v && (!query || v.toLowerCase().includes(query)))
      .slice(0, 12)
      .map((v) => `<option value="${v}"></option>`)
      .join("");

    suggestionListEl.innerHTML = options;
  }

  async function loadTransactions() {
    window.HawkUI.showLoader();
    const filterBy = (filterByEl?.value || "ALL").trim();
    const filterValue = (filterValueEl?.value || "").trim();
    const from = fromEl?.value || "";
    const to = toEl?.value || "";

    // Use ALL + client-side contains-match so partial account/payee/id searches work.
    const data = await window.HawkUI.apiRequest(`/api/transactions/list?filterBy=ALL&value=`);
    allRowsCache = Array.isArray(data) ? data : [];
    updateTypeaheadSuggestions();

    const rows = allRowsCache
      .filter((tx) => rowMatchesFilter(tx, filterBy, filterValue))
      .filter((tx) => inDateRange(tx.timeStamp, from, to));
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
        filterValueEl.type = 'text';
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
        if (suggestionListEl) suggestionListEl.innerHTML = '';
        break;
    }
    updateTypeaheadSuggestions();
  }

  // Debounced loader used by interactive filter inputs
  const debouncedLoad = debounce(() => {
    loadTransactions().catch((err) => {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast(err.message);
    });
  }, 300);

  if (filterByEl) filterByEl.addEventListener('change', () => {
    updateFilterInput();
    // trigger load when the filter type changes
    debouncedLoad();
  });

  // When user types/selects a filter value or changes date range, fetch automatically
  if (filterValueEl) filterValueEl.addEventListener('input', () => {
    updateTypeaheadSuggestions();
    debouncedLoad();
  });
  if (fromEl) fromEl.addEventListener('change', debouncedLoad);
  if (toEl) toEl.addEventListener('change', debouncedLoad);
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

