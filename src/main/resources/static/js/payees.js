document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("payeesBody");

  async function loadPayees() {
    window.HawkUI.showLoader();
    try {
      const data = await window.HawkUI.apiRequest("/api/payees");
      const rows = Array.isArray(data) ? data : [];
      window.HawkUI.hideLoader();
      tbody.innerHTML = "";
      if (rows.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:var(--hawk-grey);padding:20px;">No payees registered yet. Add one above.</td></tr>`;
        return;
      }
      rows.forEach((p) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td><strong>#${p.payeeId}</strong></td>
          <td>${p.payeeName}</td>
          <td><code>${p.payeeAccountNumber}</code></td>
          <td>${p.bankName}</td>
        `;
        tbody.appendChild(tr);
      });
    } catch (err) {
      window.HawkUI.hideLoader();
      window.HawkUI.showToast("Failed to load payees: " + err.message);
    }
  }

  document.getElementById("payeeForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payeeName         = document.getElementById("payeeName").value.trim();
    const payeeAccountNumber = document.getElementById("payeeAccNum").value.trim();
    const bankName          = document.getElementById("payeeBank").value.trim();
    try {
      await window.HawkUI.apiRequest("/api/payees", {
        method: "POST",
        body: JSON.stringify({ payeeName, payeeAccountNumber, bankName })
      });
      window.HawkUI.showToast("✅ Payee registered successfully.");
      e.target.reset();
      loadPayees();
    } catch (err) {
      window.HawkUI.showToast("Error: " + err.message);
    }
  });

  loadPayees();
});

