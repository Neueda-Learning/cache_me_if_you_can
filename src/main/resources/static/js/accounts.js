document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("accountsBody");

  async function loadAccounts() {
    window.HawkUI.showLoader();
    try {
      const data = await window.HawkUI.apiRequest("/api/accounts");
      const rows = Array.isArray(data) ? data : [];
      window.HawkUI.hideLoader();
      tbody.innerHTML = "";
      if (rows.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;color:var(--hawk-grey);padding:20px;">No accounts registered yet. Add one above.</td></tr>`;
        return;
      }
      rows.forEach((a) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td><strong>#${a.accountId}</strong></td>
          <td><code>${a.accountNumber}</code></td>
          <td>${a.accountHolderName}</td>
          <td>₹${window.HawkUI.fmtAmount(a.balance)}</td>
          <td>${window.HawkUI.fmtDate(a.createdAt)}</td>
        `;
        tbody.appendChild(tr);
      });
    } catch (err) {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast("Failed to load accounts: " + err.message);
    }
  }

  document.getElementById("accountForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const holderName  = document.getElementById("accHolder").value.trim();
    const accNumber   = document.getElementById("accNumber").value.trim();
    const balance     = parseFloat(document.getElementById("accBalance").value || "0");
    try {
      await window.HawkUI.apiRequest("/api/accounts", {
        method: "POST",
        body: JSON.stringify({ accountHolderName: holderName, accountNumber: accNumber, balance })
      });
      window.HawkUI.showToast("✅ Account registered successfully.");
      e.target.reset();
      loadAccounts();
    } catch (err) {
      window.HawkUI.showToast("Error: " + err.message);
    }
  });

  loadAccounts();
});

