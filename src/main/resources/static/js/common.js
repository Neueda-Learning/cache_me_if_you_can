(function () {

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

    // Inject settings button before logout link
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
      // Replace hard logout link with JS logout
      logoutLink.href = "#";
      logoutLink.addEventListener("click", (e) => { e.preventDefault(); logout(); });
    } else {
      userArea.appendChild(settingsBtn);
    }
  }

  /* ── Guard: redirect to login if no JWT (for inner app pages) */
  function authGuard() {
    const pub = ["/", "/index.html", "/login.html", "/signup.html"];
    const path = window.location.pathname.replace(/\/$/, "") || "/index.html";
    const isPublic = pub.some(p => path === p || path.endsWith(p));
    if (!isPublic && !getToken()) {
      window.location.href = "/login.html";
    }
  }

  /* ── Expose ────────────────────────────────────────────── */
  window.HawkUI = {
    apiRequest,
    showLoader,
    hideLoader,
    statusClass,
    fmtAmount,
    fmtDate,
    nowLocalIsoNoZone,
    showToast,
    logout,
    getToken,
    getRole,
  };

  document.addEventListener("DOMContentLoaded", () => {
    authGuard();
    setYear();
    setOperatorName();
    injectChangePasswordModal();
    wireTopbar();
  });
})();
