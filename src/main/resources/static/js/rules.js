document.addEventListener("DOMContentLoaded", () => {
  const body = document.getElementById("rulesBody");

  function ensureDeleteModal() {
    if (document.getElementById("ruleDeleteModal")) return;
    const modal = document.createElement("div");
    modal.id = "ruleDeleteModal";
    modal.className = "hawk-confirm-overlay";
    modal.innerHTML = `
      <div class="hawk-confirm-card">
        <h3>Delete Rule</h3>
        <p id="ruleDeleteMessage">Are you sure you want to delete this rule?</p>
        <div class="hawk-confirm-actions">
          <button type="button" class="btn-secondary" id="ruleDeleteCancel">Cancel</button>
          <button type="button" class="btn-danger" id="ruleDeleteConfirm">Delete</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);
  }

  // clear inline error when user edits threshold
  const thresholdInput = document.getElementById('threshold');
  if (thresholdInput) {
    thresholdInput.addEventListener('input', () => hideInlineFieldError('threshold'));
  }

  // Validation / field error modal (used to mirror time-window style messages)
  function ensureValidationModal() {
    if (document.getElementById("ruleFieldModal")) return;
    const modal = document.createElement("div");
    modal.id = "ruleFieldModal";
    modal.className = "hawk-confirm-overlay";
    modal.innerHTML = `
      <div class="hawk-confirm-card">
        <h3 id="ruleFieldModalTitle">Validation</h3>
        <p id="ruleFieldModalMessage">-</p>
        <div style="display:flex;gap:10px;justify-content:flex-end;margin-top:12px;">
          <button type="button" class="btn-secondary" id="ruleFieldModalClose">Close</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);

    const closeBtn = document.getElementById("ruleFieldModalClose");
    closeBtn.addEventListener("click", () => { modal.style.display = "none"; });
    modal.addEventListener("click", (e) => { if (e.target === modal) modal.style.display = "none"; });
  }

  function showValidationModal(message) {
    ensureValidationModal();
    const modal = document.getElementById("ruleFieldModal");
    const msg = document.getElementById("ruleFieldModalMessage");
    const title = document.getElementById("ruleFieldModalTitle");
    if (!modal || !msg || !title) return;
    title.textContent = "Invalid rule fields";
    msg.textContent = message;
    modal.style.display = "flex";
  }

  // Inline field error (below input) for threshold
  function showInlineFieldError(fieldId, message) {
    const input = document.getElementById(fieldId);
    if (!input) return;
    // remove existing
    hideInlineFieldError(fieldId);
    const err = document.createElement('div');
    err.className = 'field-error';
    err.id = fieldId + '-error';
    err.textContent = message;
    input.classList.add('input-error');
    input.parentNode.insertBefore(err, input.nextSibling);
  }

  function hideInlineFieldError(fieldId) {
    const existing = document.getElementById(fieldId + '-error');
    const input = document.getElementById(fieldId);
    if (existing && existing.parentNode) existing.parentNode.removeChild(existing);
    if (input) input.classList.remove('input-error');
  }

  function confirmDeleteRule(ruleId) {
    ensureDeleteModal();
    const modal = document.getElementById("ruleDeleteModal");
    const message = document.getElementById("ruleDeleteMessage");
    const confirmBtn = document.getElementById("ruleDeleteConfirm");
    const cancelBtn = document.getElementById("ruleDeleteCancel");

    message.textContent = `Are you sure you want to delete rule #${ruleId}?`;
    modal.style.display = "flex";

    return new Promise((resolve) => {
      const close = (result) => {
        modal.style.display = "none";
        confirmBtn.removeEventListener("click", onConfirm);
        cancelBtn.removeEventListener("click", onCancel);
        modal.removeEventListener("click", onBackdrop);
        resolve(result);
      };

      const onConfirm = () => close(true);
      const onCancel = () => close(false);
      const onBackdrop = (event) => {
        if (event.target === modal) close(false);
      };

      confirmBtn.addEventListener("click", onConfirm);
      cancelBtn.addEventListener("click", onCancel);
      modal.addEventListener("click", onBackdrop);
    });
  }

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
        <td class="rule-actions-cell">
          <div class="rule-actions">
          <button class="btn-ghost" data-action="toggle" data-id="${r.ruleId}" data-active="${r.activeStatus}">
            ${r.activeStatus ? "Deactivate" : "Activate"}
          </button>
          <button class="btn-danger" data-action="delete" data-id="${r.ruleId}">
            Delete
          </button>
          </div>
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

  async function deleteRule(ruleId) {
    const confirmed = await confirmDeleteRule(ruleId);
    if (!confirmed) return;

    await window.HawkUI.apiRequest(`/api/v1/rules/${ruleId}`, {
      method: "DELETE"
    });

    await loadRules();
  }

  document.getElementById("rulesBody").addEventListener("click", (event) => {
    const toggleBtn = event.target.closest("button[data-action='toggle']");
    if (toggleBtn) {
      const ruleId = toggleBtn.getAttribute("data-id");
      const active = toggleBtn.getAttribute("data-active") === "true";
      toggleRule(ruleId, active).catch((err) => window.HawkUI.showToast(err.message));
      return;
    }

    const deleteBtn = event.target.closest("button[data-action='delete']");
    if (deleteBtn) {
      const ruleId = deleteBtn.getAttribute("data-id");
      deleteRule(ruleId).catch((err) => window.HawkUI.showToast(err.message));
    }
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
      threshold: document.getElementById("threshold").value
        ? Number(document.getElementById("threshold").value)
        : null,
      timeWindow: document.getElementById("timeWindow").value
        ? Number(document.getElementById("timeWindow").value)
        : null,
    };

    try {
      await window.HawkUI.apiRequest("/api/v1/rules", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      // clear any inline errors and reset form
      hideInlineFieldError('threshold');
      event.target.reset();
      await loadRules();
    } catch (err) {
      const msg = err && err.message ? String(err.message) : "Failed to create rule.";
      // If the error is a field validation, handle threshold inline, others via modal
      if (/threshold/i.test(msg)) {
        // show error just below threshold input
        showInlineFieldError('threshold', msg);
      } else if (/(rule name|time window|daily limit)/i.test(msg)) {
        showValidationModal(msg);
      } else {
        window.HawkUI.showToast(msg);
      }
    }
  });

  loadRules().catch((err) => {
    window.HawkUI.hideLoader();
    window.HawkUI.showToast(err.message);
  });
});