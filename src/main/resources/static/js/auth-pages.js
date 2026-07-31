(function () {
    var loginBtn = document.getElementById("loginButton");
    var signupBtn = document.getElementById("signupButton");
    var message = document.getElementById("authMessage");

    if (App.currentUser() && (window.location.pathname.endsWith("/login.html") || window.location.pathname.endsWith("/signup.html"))) {
        window.location.href = "/dashboard.html";
        return;
    }

    if (loginBtn) {
        loginBtn.addEventListener("click", function () {
            try {
                var email = document.getElementById("loginEmail").value;
                var password = document.getElementById("loginPassword").value;
                App.login(email, password);
                window.location.href = "/dashboard.html";
            } catch (error) {
                render(error.message, true);
            }
        });
    }

    if (signupBtn) {
        signupBtn.addEventListener("click", function () {
            try {
                var name = document.getElementById("signupName").value;
                var email = document.getElementById("signupEmail").value;
                var password = document.getElementById("signupPassword").value;
                App.signup(name, email, password);
                window.location.href = "/dashboard.html";
            } catch (error) {
                render(error.message, true);
            }
        });
    }

    function render(text, isError) {
        if (!message) return;
        message.textContent = text;
        message.classList.remove("msg-success", "msg-error");
        message.classList.add(isError ? "msg-error" : "msg-success");
    }
})();

