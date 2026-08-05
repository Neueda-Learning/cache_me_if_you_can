document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("accountsBody");
  const searchEl = document.getElementById("accountSearch");
  const suggestionsEl = document.getElementById("accountSuggestions");
  let allAccounts = [];

  function renderAccounts() {
    const query = (searchEl?.value || "").trim().toLowerCase();
    const rows = allAccounts.filter((a) => {
      if (!query) return true;
      return [a.accountId, a.accountNumber, a.accountHolderName, a.email]
        .some((v) => String(v || "").toLowerCase().includes(query));
    });

    tbody.innerHTML = "";
    if (rows.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--hawk-grey);padding:20px;">No accounts match your search.</td></tr>`;
      return;
    }
    rows.forEach((a) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td><strong>#${a.accountId}</strong></td>
        <td><code>${a.accountNumber}</code></td>
        <td>${a.accountHolderName}</td>
        <td>${a.email || "-"}</td>
        <td>₹${window.HawkUI.fmtAmount(a.balance)}</td>
        <td>${window.HawkUI.fmtDate(a.createdAt)}</td>
        <td><a class="btn-ghost" href="/account-summary.html?accountId=${encodeURIComponent(a.accountId)}">Account Summary</a></td>
      `;
      tbody.appendChild(tr);
    });
  }

  function updateSuggestions() {
    if (!suggestionsEl) return;
    const query = (searchEl?.value || "").trim().toLowerCase();
    const values = new Set();
    allAccounts.forEach((a) => {
      values.add(String(a.accountNumber || ""));
      values.add(String(a.accountHolderName || ""));
      if (a.email) values.add(String(a.email));
    });
    suggestionsEl.innerHTML = Array.from(values)
      .filter((v) => v && (!query || v.toLowerCase().includes(query)))
      .slice(0, 12)
      .map((v) => `<option value="${v}"></option>`)
      .join("");
  }

  async function loadAccounts() {
    window.HawkUI.showLoader();
    try {
      const data = await window.HawkUI.apiRequest("/api/accounts");
      allAccounts = Array.isArray(data) ? data : [];
      window.HawkUI.hideLoader();
      if (allAccounts.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--hawk-grey);padding:20px;">No accounts registered yet. Add one above.</td></tr>`;
        return;
      }
      updateSuggestions();
      renderAccounts();
    } catch (err) {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast("Failed to load accounts: " + err.message);
    }
  }

  document.getElementById("accountForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const holderName  = document.getElementById("accHolder").value.trim();
    const accNumber   = document.getElementById("accNumber").value.trim();
    const email       = document.getElementById("accEmail").value.trim();
    const balance     = parseFloat(document.getElementById("accBalance").value || "0");
    try {
      await window.HawkUI.apiRequest("/api/accounts", {
        method: "POST",
        body: JSON.stringify({ accountHolderName: holderName, accountNumber: accNumber, email, balance })
      });
      window.HawkUI.showToast("✅ Account registered successfully.");
      e.target.reset();
      loadAccounts();
    } catch (err) {
      window.HawkUI.showToast("Error: " + err.message);
    }
  });

  if (searchEl) {
    searchEl.addEventListener("input", () => {
      updateSuggestions();
      renderAccounts();
    });
  }

  loadAccounts();
});

