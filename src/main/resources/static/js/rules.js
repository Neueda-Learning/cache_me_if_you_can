document.addEventListener("DOMContentLoaded", () => {
  const body = document.getElementById("rulesBody");

  async function loadRules() {
    window.HawkUI.showLoader();
    const activeOnly = document.getElementById("activeOnly").checked;
    const q = activeOnly ? "?activeStatus=true" : "";
    const data = await window.HawkUI.apiRequest(`/api/v1/rules${q}`);
    const rows = Array.isArray(data) ? data : [];
    window.HawkUI.hideLoader();

    body.innerHTML = "";
    if (rows.length === 0) {
      body.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--hawk-grey);padding:20px;">No rules found</td></tr>`;
      return;
    }
    rows.forEach((r) => {
      const tr = document.createElement("tr");
      const thresholdWindow = [r.threshold ?? "-", r.timeWindow ?? "-"].join(" / ");
      tr.innerHTML = `
        <td>${r.ruleId}</td>
        <td>${r.ruleName}</td>
        <td>${r.ruleType}</td>
        <td><span class="badge ${window.HawkUI.statusClass(r.severity)}">${r.severity}</span></td>
        <td>${thresholdWindow}</td>
        <td>${r.activeStatus ? "ACTIVE" : "INACTIVE"}</td>
        <td>
          <button class="btn-ghost" data-action="toggle" data-id="${r.ruleId}" data-active="${r.activeStatus}">${r.activeStatus ? "Deactivate" : "Activate"}</button>
        </td>
      `;
      body.appendChild(tr);
    });
  }

  async function toggleRule(ruleId, active) {
    const endpoint = active ? "deactivate" : "activate";
    await window.HawkUI.apiRequest(`/api/v1/rules/${ruleId}/${endpoint}`, { method: "PATCH" });
    await loadRules();
  }

  document.getElementById("rulesBody").addEventListener("click", (event) => {
    const btn = event.target.closest("button[data-action='toggle']");
    if (!btn) return;
    const ruleId = btn.getAttribute("data-id");
    const active = btn.getAttribute("data-active") === "true";
    toggleRule(ruleId, active).catch((err) => window.HawkUI.showToast(err.message));
  });

  document.getElementById("activeOnly").addEventListener("change", () => {
    loadRules().catch((err) => window.HawkUI.showToast(err.message));
  });

  document.getElementById("ruleForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = {
      ruleName: document.getElementById("ruleName").value,
      ruleType: document.getElementById("ruleType").value,
      severity: document.getElementById("severity").value,
      threshold: document.getElementById("threshold").value ? Number(document.getElementById("threshold").value) : null,
      timeWindow: document.getElementById("timeWindow").value ? Number(document.getElementById("timeWindow").value) : null,
    };

    try {
      await window.HawkUI.apiRequest("/api/v1/rules", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      event.target.reset();
      await loadRules();
      window.HawkUI.showToast("Rule created");
    } catch (err) {
      window.HawkUI.showToast(err.message);
    }
  });

  loadRules().catch((err) => {
    window.HawkUI.hideLoader();
    window.HawkUI.showToast(err.message);
  });
});

