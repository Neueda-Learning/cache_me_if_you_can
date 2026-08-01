document.addEventListener("DOMContentLoaded", () => {
  // If already logged in, skip login page
  if (localStorage.getItem("hawk-jwt") && window.location.pathname.includes("login")) {
    window.location.href = "/dashboard.html";
    return;
  }

  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    const errEl = document.getElementById("loginError");
    loginForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (errEl) errEl.style.display = "none";
      const username = document.getElementById("username").value.trim();
      const password = document.getElementById("password").value;
      const submitBtn = loginForm.querySelector("button[type='submit']");
      if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = "Signing in…"; }
      try {
        const result = await window.HawkUI.apiRequest("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({ username, password })
        });
        if (result && result.token) {
          localStorage.setItem("hawk-jwt", result.token);
          localStorage.setItem("hawk-role", result.role || "USER");
          localStorage.setItem("hawk-operator", result.name || username);
          window.location.href = "/dashboard.html";
        }
      } catch (err) {
        if (errEl) {
          errEl.textContent = err.message || "Invalid credentials. Please try again.";
          errEl.style.display = "block";
        } else {
          window.HawkUI.showToast(err.message || "Invalid credentials. Please try again.");
        }
        if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = "Sign In"; }
      }
    });
  }

  const signupForm = document.getElementById("signupForm");
  if (signupForm) {
    signupForm.addEventListener("submit", (event) => {
      event.preventDefault();
      window.HawkUI.showToast("Profile created. Redirecting to login...");
      setTimeout(() => {
        window.location.href = "/login.html";
      }, 900);
    });
  }
});
