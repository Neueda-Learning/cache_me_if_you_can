(function () {

  const ALERT_STATE_KEY = "hawk-alert-popup-state";
  const ALERT_POLL_INTERVAL_MS = 8000;
  const ALERT_REMINDER_INTERVAL_MS = 24 * 60 * 60 * 1000;
  const ACTIVE_ALERT_STATUSES = new Set(["OPEN", "ACKNOWLEDGED", "INVESTIGATING"]);
  const TERMINAL_ALERT_STATUSES = new Set(["CLOSED", "DISMISSED"]);

  /* ── JWT helpers ──────────────────────────────────────────── */
  function getToken()  { return localStorage.getItem("hawk-jwt"); }
  function getRole()   { return localStorage.getItem("hawk-role"); }
  function clearAuth() {
    localStorage.removeItem("hawk-jwt");
    localStorage.removeItem("hawk-role");
    localStorage.removeItem("hawk-operator");
  }

  function logout() {
    clearAuth();
    window.location.href = "/login.html";
  }

  /* ── Loader ────────────────────────────────────────────── */
  function showLoader() {
    const el = document.getElementById("hawkLoader");
    if (!el) return;
    el.style.display = "flex";
    el.classList.remove("hide");
  }

  function hideLoader() {
    const el = document.getElementById("hawkLoader");
    if (!el) return;
    el.classList.add("hide");
    setTimeout(() => { el.style.display = "none"; }, 360);
  }

  /* ── API ───────────────────────────────────────────────── */
  function parseApiResponse(json) {
    if (json && typeof json === "object" && Object.prototype.hasOwnProperty.call(json, "data")) {
      return json.data;
    }
    return json;
  }

  async function apiRequest(url, options) {
    options = options || {};
    const token = getToken();
    const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
    if (token) headers["Authorization"] = "Bearer " + token;
    const resp = await fetch(url, Object.assign({}, options, { headers }));
    if (resp.status === 401) {
      clearAuth();
      window.location.href = "/login.html";
      return;
    }
    let json = null;
    try { json = await resp.json(); } catch (_) { json = null; }
    if (!resp.ok) {
      const message = (json && (json.message || (json.error && json.error.message))) ||
        "Request failed with status " + resp.status;
      throw new Error(message);
    }
    return parseApiResponse(json);
  }

  /* ── Formatters ────────────────────────────────────────── */
  function statusClass(value) {
    if (!value) return "";
    return value.toString().toLowerCase();
  }

  function fmtAmount(amount) {
    const n = Number(amount || 0);
    return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function fmtAmountCompact(amount) {
    const n = Number(amount || 0);
    if (n >= 10000000) return (n / 10000000).toFixed(2).replace(/\.?0+$/, "") + " Cr";
    if (n >= 100000) return (n / 100000).toFixed(2).replace(/\.?0+$/, "") + " L";
    if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.?0+$/, "") + " K";
    return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function fmtDate(input) {
    if (!input) return "-";
    const d = new Date(input);
    if (Number.isNaN(d.getTime())) return String(input);
    return d.toLocaleString();
  }

  function nowLocalIsoNoZone() {
    const d = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    return d.getFullYear() + "-" + pad(d.getMonth()+1) + "-" + pad(d.getDate()) +
           "T" + pad(d.getHours()) + ":" + pad(d.getMinutes()) + ":" + pad(d.getSeconds());
  }

  /* ── Toast ─────────────────────────────────────────────── */
  function showToast(msg, duration) {
    const toast = document.getElementById("toast");
    if (!toast) return;
    toast.textContent = msg;
    toast.style.display = "block";
    setTimeout(() => { toast.style.display = "none"; }, duration || 2800);
  }

  /* ── Year ──────────────────────────────────────────────── */
  function setYear() {
    document.querySelectorAll("[data-year]").forEach((el) => {
      el.textContent = String(new Date().getFullYear());
    });
  }

  /* ── Operator name ─────────────────────────────────────── */
  function setOperatorName() {
    const el = document.getElementById("operatorName");
    if (el) el.textContent = localStorage.getItem("hawk-operator") || "Operator";
  }

  /* ── Change-Password Modal (injected into every inner page) */
  function injectChangePasswordModal() {
    if (document.getElementById("cpModal")) return;
    const modal = document.createElement("div");
    modal.id = "cpModal";
    modal.style.cssText = "display:none;position:fixed;inset:0;background:rgba(0,0,0,0.55);z-index:9999;align-items:center;justify-content:center;";
    modal.innerHTML = `
      <div style="background:#fff;border-radius:10px;padding:32px 36px;width:360px;max-width:92vw;box-shadow:0 8px 40px rgba(0,0,0,0.22);">
        <h2 style="margin:0 0 20px;font-size:1.1rem;font-weight:700;color:#1a1a1a;">Change Password</h2>
        <form id="cpForm" autocomplete="off">
          <div style="margin-bottom:14px;">
            <label style="display:block;font-size:0.78rem;font-weight:600;color:#555;margin-bottom:5px;text-transform:uppercase;">Current Password</label>
            <input id="cpCurrent" type="password" required style="width:100%;padding:9px 12px;border:1px solid #ddd;border-radius:6px;font-size:0.9rem;box-sizing:border-box;">
          </div>
          <div style="margin-bottom:14px;">
            <label style="display:block;font-size:0.78rem;font-weight:600;color:#555;margin-bottom:5px;text-transform:uppercase;">New Password</label>
            <input id="cpNew" type="password" required minlength="6" style="width:100%;padding:9px 12px;border:1px solid #ddd;border-radius:6px;font-size:0.9rem;box-sizing:border-box;">
          </div>
          <div style="margin-bottom:20px;">
            <label style="display:block;font-size:0.78rem;font-weight:600;color:#555;margin-bottom:5px;text-transform:uppercase;">Confirm New Password</label>
            <input id="cpConfirm" type="password" required style="width:100%;padding:9px 12px;border:1px solid #ddd;border-radius:6px;font-size:0.9rem;box-sizing:border-box;">
          </div>
          <div style="display:flex;gap:10px;">
            <button type="submit" style="flex:1;padding:10px;background:#DB0011;color:#fff;border:none;border-radius:6px;font-weight:700;cursor:pointer;font-size:0.9rem;">Update</button>
            <button type="button" id="cpCancel" style="flex:1;padding:10px;background:#f5f5f5;color:#333;border:1px solid #ddd;border-radius:6px;font-weight:600;cursor:pointer;font-size:0.9rem;">Cancel</button>
          </div>
          <p id="cpError" style="color:#DB0011;font-size:0.8rem;margin:10px 0 0;display:none;"></p>
        </form>
      </div>`;
    document.body.appendChild(modal);

    document.getElementById("cpCancel").addEventListener("click", () => { modal.style.display = "none"; });
    modal.addEventListener("click", (e) => { if (e.target === modal) modal.style.display = "none"; });

    document.getElementById("cpForm").addEventListener("submit", async (e) => {
      e.preventDefault();
      const newPwd = document.getElementById("cpNew").value;
      const confirm = document.getElementById("cpConfirm").value;
      const errEl = document.getElementById("cpError");
      errEl.style.display = "none";
      if (newPwd !== confirm) {
        errEl.textContent = "New passwords do not match.";
        errEl.style.display = "block";
        return;
      }
      try {
        await apiRequest("/api/auth/change-password", {
          method: "PUT",
          body: JSON.stringify({ currentPassword: document.getElementById("cpCurrent").value, newPassword: newPwd })
        });
        modal.style.display = "none";
        showToast("✅ Password changed successfully. Please log in again.", 3500);
        setTimeout(logout, 3600);
      } catch (err) {
        errEl.textContent = err.message || "Failed to change password.";
        errEl.style.display = "block";
      }
    });
  }

  /* ── Wire topbar settings button (injected into .topbar-user) */
  function wireTopbar() {
    const userArea = document.querySelector(".topbar-user");
    if (!userArea) return;

    const logoutLink = userArea.querySelector("#logoutLink") || userArea.querySelector("a[href='/login.html']");
    const settingsBtn = document.createElement("button");
    settingsBtn.textContent = "⚙️";
    settingsBtn.title = "Settings";
    settingsBtn.style.cssText = "background:none;border:none;cursor:pointer;font-size:1rem;padding:2px 6px;margin-right:4px;opacity:0.7;transition:opacity 0.2s;";
    settingsBtn.addEventListener("mouseenter", () => { settingsBtn.style.opacity = "1"; });
    settingsBtn.addEventListener("mouseleave", () => { settingsBtn.style.opacity = "0.7"; });
    settingsBtn.addEventListener("click", () => {
      const modal = document.getElementById("cpModal");
      if (modal) {
        document.getElementById("cpCurrent").value = "";
        document.getElementById("cpNew").value = "";
        document.getElementById("cpConfirm").value = "";
        document.getElementById("cpError").style.display = "none";
        modal.style.display = "flex";
      }
    });

    if (logoutLink) {
      userArea.insertBefore(settingsBtn, logoutLink);
      logoutLink.href = "#";
      logoutLink.addEventListener("click", (e) => { e.preventDefault(); logout(); });
    } else {
      userArea.appendChild(settingsBtn);
    }
  }

  /* ── Guard / route helpers ─────────────────────────────── */
  function currentPath() {
    return window.location.pathname.replace(/\/$/, "") || "/index.html";
  }

  function isPublicPage() {
    const pub = ["/", "/index.html", "/login.html", "/signup.html"];
    const path = currentPath();
    return pub.some(p => path === p || path.endsWith(p));
  }

  function isAlertsWorkbenchPage() {
    const path = currentPath();
    return path.endsWith("/alerts.html") || path.endsWith("/alert-detail.html") || path === "/alerts.html" || path === "/alert-detail.html";
  }

  function authGuard() {
    if (!isPublicPage() && !getToken()) {
      window.location.href = "/login.html";
    }
  }

  /* ── Alert notification state ──────────────────────────── */
  function readAlertState() {
    try {
      const raw = JSON.parse(localStorage.getItem(ALERT_STATE_KEY) || "{}");
      return raw && typeof raw === "object" ? raw : {};
    } catch (_) {
      return {};
    }
  }

  function writeAlertState(state) {
    localStorage.setItem(ALERT_STATE_KEY, JSON.stringify(state));
  }

  function clearAlertNotification(alertId) {
    const state = readAlertState();
    delete state[String(alertId)];
    writeAlertState(state);
  }

  function syncAlertNotificationState(alerts) {
    const state = readAlertState();
    const byId = new Map((alerts || []).map((a) => [String(a.alertId), a]));

    Object.keys(state).forEach((id) => {
      const alert = byId.get(id);
      if (!alert || TERMINAL_ALERT_STATUSES.has(alert.status)) {
        delete state[id];
        return;
      }
      state[id].status = alert.status;
    });

    writeAlertState(state);
    return state;
  }

  function rememberAlertsShown(alerts, type) {
    const state = readAlertState();
    const now = Date.now();
    (alerts || []).forEach((alert) => {
      const id = String(alert.alertId);
      const existing = state[id] || {};
      if (!existing.firstShownAt) existing.firstShownAt = now;
      existing.lastShownAt = now;
      if (type === "reminder") existing.lastReminderAt = now;
      existing.status = alert.status;
      state[id] = existing;
    });
    writeAlertState(state);
  }

  function shouldShowFreshAlert(alert, state) {
    if (!alert || !ACTIVE_ALERT_STATUSES.has(alert.status)) return false;
    const entry = state[String(alert.alertId)];
    return !entry || !entry.lastShownAt;
  }

  function shouldShowReminder(alert, state, now) {
    if (!alert || !ACTIVE_ALERT_STATUSES.has(alert.status)) return false;
    const entry = state[String(alert.alertId)];
    if (!entry || !entry.lastShownAt) return false;
    const anchor = entry.lastReminderAt || entry.lastShownAt;
    return (now - anchor) >= ALERT_REMINDER_INTERVAL_MS;
  }

  function hideAnyAlertPopup() {
    const popup = document.getElementById("alertPopup") || document.getElementById("globalAlertPopup");
    if (popup) popup.style.display = "none";
  }

  function ensureGlobalAlertPopup() {
    if (document.getElementById("alertPopup") || document.getElementById("globalAlertPopup")) return;
    const overlay = document.createElement("div");
    overlay.id = "globalAlertPopup";
    overlay.style.cssText = "display:none;position:fixed;inset:0;background:rgba(0,0,0,0.6);z-index:8888;align-items:center;justify-content:center;";
    overlay.innerHTML = `
      <div style="background:#fff;border-radius:12px;width:460px;max-width:94vw;max-height:80vh;overflow-y:auto;box-shadow:0 12px 60px rgba(219,0,17,0.25);border-top:4px solid #DB0011;">
        <div style="padding:20px 24px 0;display:flex;align-items:center;justify-content:space-between;">
          <div style="display:flex;align-items:center;gap:10px;">
            <span style="font-size:1.5rem;">🚨</span>
            <div>
              <p id="globalAlertPopupLabel" style="margin:0;font-size:0.7rem;font-weight:700;color:#DB0011;text-transform:uppercase;letter-spacing:0.06em;">Alert Notification</p>
              <h3 id="globalAlertPopupTitle" style="margin:2px 0 0;font-size:1.1rem;color:#1a1a1a;">-</h3>
            </div>
          </div>
          <button id="globalAlertPopupClose" style="background:none;border:none;font-size:1.4rem;cursor:pointer;color:#999;line-height:1;" title="Dismiss">×</button>
        </div>
        <div id="globalAlertPopupBody" style="padding:16px 24px;"></div>
        <div style="padding:0 24px 20px;display:flex;gap:10px;">
          <a href="/alerts.html" style="flex:1;text-align:center;padding:10px;background:#DB0011;color:#fff;border-radius:7px;font-weight:700;font-size:0.9rem;text-decoration:none;">Go to Alerts →</a>
          <button id="globalAlertPopupDismiss" style="flex:1;padding:10px;background:#f5f5f5;border:1px solid #ddd;border-radius:7px;font-weight:600;cursor:pointer;font-size:0.9rem;">Dismiss</button>
        </div>
      </div>`;
    document.body.appendChild(overlay);

    const close = () => { overlay.style.display = "none"; };
    document.getElementById("globalAlertPopupClose").addEventListener("click", close);
    document.getElementById("globalAlertPopupDismiss").addEventListener("click", close);
    overlay.addEventListener("click", (e) => { if (e.target === overlay) close(); });
  }

  function getPopupRefs() {
    if (document.getElementById("alertPopup")) {
      return {
        overlay: document.getElementById("alertPopup"),
        title: document.getElementById("alertPopupTitle"),
        body: document.getElementById("alertPopupBody"),
        label: null,
      };
    }
    ensureGlobalAlertPopup();
    return {
      overlay: document.getElementById("globalAlertPopup"),
      title: document.getElementById("globalAlertPopupTitle"),
      body: document.getElementById("globalAlertPopupBody"),
      label: document.getElementById("globalAlertPopupLabel"),
    };
  }

  function showAlertPopup(alerts, type) {
    const popup = getPopupRefs();
    if (!popup.overlay || !popup.title || !popup.body) return;

    const isReminder = type === "reminder";
    if (popup.label) {
      popup.label.textContent = isReminder ? "Pending Alert Reminder" : "New Alert(s) Detected";
    }
    popup.title.textContent = isReminder
      ? `${alerts.length} alert${alerts.length === 1 ? "" : "s"} still unresolved after 24 hours`
      : (alerts.length === 1 ? "1 New Alert — Immediate Attention Required" : `${alerts.length} New Alerts — Immediate Attention Required`);

    popup.body.innerHTML = alerts.slice(0, 5).map((a) => `
      <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #f0f0f0;">
        <div>
          <span class="badge ${statusClass(a.severity)}" style="margin-right:8px;">${a.severity}</span>
          <strong>Alert #${a.alertId}</strong>
          <span style="font-size:0.8rem;color:#777;margin-left:8px;">Tx #${a.transactionId || "—"}</span>
        </div>
        <a href="/alert-detail.html?id=${a.alertId}" style="font-size:0.8rem;color:#DB0011;font-weight:600;">Investigate →</a>
      </div>`).join("") +
      (alerts.length > 5 ? `<p style="font-size:0.8rem;color:#777;margin:10px 0 0;">…and ${alerts.length - 5} more.</p>` : "");

    popup.overlay.style.display = "flex";

    let blink = 0;
    const origTitle = document.title;
    const blinkTimer = setInterval(() => {
      document.title = (blink++ % 2 === 0)
        ? (isReminder ? "⏰ ALERT REMINDER — HAWK" : "🚨 NEW ALERT — HAWK")
        : origTitle;
      if (blink > 8) {
        clearInterval(blinkTimer);
        document.title = origTitle;
      }
    }, 700);
  }

  async function autoAcknowledgeShownOpenAlerts(alerts) {
    const state = readAlertState();
    const toAck = (alerts || []).filter((alert) => alert.status === "OPEN" && state[String(alert.alertId)]?.lastShownAt);
    if (toAck.length === 0) return;

    await Promise.all(toAck.map(async (alert) => {
      try {
        await apiRequest(`/api/v1/alerts/${alert.alertId}/acknowledge`, { method: "PATCH" });
      } catch (_) {
        // Ignore race conditions / already-advanced alerts.
      }
    }));
  }

  function updateDashboardOpenCount(alerts) {
    const el = document.getElementById("kpiOpen");
    if (!el) return;
    const count = (alerts || []).filter((a) => a.status === "OPEN").length;
    el.textContent = String(count);
  }

  function startGlobalAlertPolling() {
    if (isPublicPage() || !getToken()) return;
    if (!document.getElementById("alertPopup")) {
      ensureGlobalAlertPopup();
    }

    const poll = async () => {
      try {
        const allAlerts = await apiRequest("/api/v1/alerts");
        const alerts = Array.isArray(allAlerts) ? allAlerts : [];
        const activeAlerts = alerts.filter((a) => ACTIVE_ALERT_STATUSES.has(a.status));
        const state = syncAlertNotificationState(alerts);
        updateDashboardOpenCount(alerts);

        if (isAlertsWorkbenchPage()) {
          hideAnyAlertPopup();
          await autoAcknowledgeShownOpenAlerts(activeAlerts);
          return;
        }

        const freshAlerts = activeAlerts.filter((alert) => shouldShowFreshAlert(alert, state));
        if (freshAlerts.length > 0) {
          rememberAlertsShown(freshAlerts, "new");
          showAlertPopup(freshAlerts, "new");
          return;
        }

        const now = Date.now();
        const reminderAlerts = activeAlerts.filter((alert) => shouldShowReminder(alert, state, now));
        if (reminderAlerts.length > 0) {
          rememberAlertsShown(reminderAlerts, "reminder");
          showAlertPopup(reminderAlerts, "reminder");
        }
      } catch (err) {
        console.warn("[HAWK] Global alert polling failed:", err);
      }
    };

    poll();
    setInterval(poll, ALERT_POLL_INTERVAL_MS);
  }

  /* ── Expose ────────────────────────────────────────────── */
  window.HawkUI = {
    apiRequest,
    showLoader,
    hideLoader,
    statusClass,
    fmtAmount,
    fmtAmountCompact,
    fmtDate,
    nowLocalIsoNoZone,
    showToast,
    logout,
    getToken,
    getRole,
    syncAlertNotificationState,
    clearAlertNotification,
  };

  document.addEventListener("DOMContentLoaded", () => {
    authGuard();
    setYear();
    setOperatorName();
    injectChangePasswordModal();
    wireTopbar();
    startGlobalAlertPolling();
  });
})();
