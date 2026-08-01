(function () {

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
    const resp = await fetch(url, options);
    let json = null;
    try { json = await resp.json(); } catch (_) { json = null; }
    if (!resp.ok) {
      const message = (json && (json.message || (json.error && json.error.message))) ||
        `Request failed with status ${resp.status}`;
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
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
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
  };

  document.addEventListener("DOMContentLoaded", () => {
    setYear();
    setOperatorName();
  });
})();
