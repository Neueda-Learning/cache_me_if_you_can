document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("payeesBody");
  const searchEl = document.getElementById("payeeSearch");
  const suggestionsEl = document.getElementById("payeeSuggestions");
  let allPayees = [];

  function renderPayees() {
    const query = (searchEl?.value || "").trim().toLowerCase();
    const rows = allPayees.filter((p) => {
      if (!query) return true;
      return [p.payeeId, p.payeeName, p.payeeAccountNumber, p.bankName]
        .some((v) => String(v || "").toLowerCase().includes(query));
    });

    tbody.innerHTML = "";
    if (rows.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:var(--hawk-grey);padding:20px;">No payees match your search.</td></tr>`;
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
  }

  function updateSuggestions() {
    if (!suggestionsEl) return;
    const query = (searchEl?.value || "").trim().toLowerCase();
    const values = new Set();
    allPayees.forEach((p) => {
      values.add(String(p.payeeName || ""));
      values.add(String(p.payeeAccountNumber || ""));
      values.add(String(p.bankName || ""));
    });
    suggestionsEl.innerHTML = Array.from(values)
      .filter((v) => v && (!query || v.toLowerCase().includes(query)))
      .slice(0, 12)
      .map((v) => `<option value="${v}"></option>`)
      .join("");
  }

  async function loadPayees() {
    window.HawkUI.showLoader();
    try {
      const data = await window.HawkUI.apiRequest("/api/payees");
      allPayees = Array.isArray(data) ? data : [];
      window.HawkUI.hideLoader();
      if (allPayees.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:var(--hawk-grey);padding:20px;">No payees registered yet. Add one above.</td></tr>`;
        return;
      }
      updateSuggestions();
      renderPayees();
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

  if (searchEl) {
    searchEl.addEventListener("input", () => {
      updateSuggestions();
      renderPayees();
    });
  }

  loadPayees();
});

