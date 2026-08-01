document.addEventListener("DOMContentLoaded", () => {
  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", (event) => {
      event.preventDefault();
      localStorage.setItem("hawk-operator", document.getElementById("username").value || "Operator");
      window.location.href = "/dashboard.html";
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

