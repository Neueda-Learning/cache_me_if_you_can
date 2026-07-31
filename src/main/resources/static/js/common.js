window.App = (function () {
    var STORAGE_USERS = "tm_users";
    var STORAGE_CURRENT_USER = "tm_current_user";

    function getUsers() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_USERS) || "[]");
        } catch (error) {
            return [];
        }
    }

    function saveUsers(users) {
        localStorage.setItem(STORAGE_USERS, JSON.stringify(users));
    }

    function signup(name, email, password) {
        var users = getUsers();
        var normalizedEmail = String(email || "").trim().toLowerCase();

        if (!name || !normalizedEmail || !password) {
            throw new Error("Name, email and password are required.");
        }

        if (users.some(function (u) { return u.email === normalizedEmail; })) {
            throw new Error("User already exists for this email.");
        }

        var user = { name: String(name).trim(), email: normalizedEmail, password: String(password) };
        users.push(user);
        saveUsers(users);
        localStorage.setItem(STORAGE_CURRENT_USER, JSON.stringify({ name: user.name, email: user.email }));
        return user;
    }

    function login(email, password) {
        var users = getUsers();
        var normalizedEmail = String(email || "").trim().toLowerCase();
        var user = users.find(function (u) { return u.email === normalizedEmail && u.password === String(password); });

        if (!user) {
            throw new Error("Invalid email or password.");
        }

        localStorage.setItem(STORAGE_CURRENT_USER, JSON.stringify({ name: user.name, email: user.email }));
        return user;
    }

    function logout() {
        localStorage.removeItem(STORAGE_CURRENT_USER);
    }

    function currentUser() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_CURRENT_USER) || "null");
        } catch (error) {
            return null;
        }
    }

    function requireAuth() {
        if (!currentUser()) {
            window.location.href = "/login.html";
            return false;
        }
        return true;
    }

    function wireHeaderAuth() {
        var user = currentUser();
        var userNode = document.getElementById("currentUser");
        if (userNode && user) {
            userNode.textContent = user.name + " (" + user.email + ")";
        }

        Array.prototype.forEach.call(document.querySelectorAll(".logout-btn"), function (btn) {
            btn.addEventListener("click", function () {
                logout();
                window.location.href = "/login.html";
            });
        });
    }

    async function request(method, url, payload) {
        var options = {
            method: method,
            headers: { "Content-Type": "application/json" }
        };
        if (payload !== undefined) {
            options.body = JSON.stringify(payload);
        }

        var response = await fetch(url, options);
        var body = await parseJson(response);

        if (!response.ok) {
            throw new Error(readErrorMessage(body, response.status));
        }

        return body;
    }

    async function parseJson(response) {
        try {
            return await response.json();
        } catch (error) {
            return null;
        }
    }

    function readErrorMessage(body, status) {
        if (!body) return "Request failed with status " + status + ".";
        if (typeof body.message === "string") return body.message;
        if (body.error && typeof body.error.message === "string") return body.error.message;
        return "Request failed with status " + status + ".";
    }

    function unwrapData(body) {
        if (body && body.data !== undefined) {
            return body.data;
        }
        return body;
    }

    function formatDate(value) {
        if (!value) return "-";
        var d = new Date(value);
        if (Number.isNaN(d.getTime())) return String(value);
        return d.toLocaleString();
    }

    function toBadgeClass(statusOrSeverity) {
        var key = String(statusOrSeverity || "").toLowerCase();
        if (key === "open") return "badge-status-open";
        if (key === "acknowledged") return "badge-status-acknowledged";
        if (key === "investigating") return "badge-status-investigating";
        if (key === "closed") return "badge-status-closed";
        if (key === "dismissed") return "badge-status-dismissed";
        if (key === "low") return "badge-sev-low";
        if (key === "medium") return "badge-sev-medium";
        if (key === "high") return "badge-sev-high";
        if (key === "critical") return "badge-sev-critical";
        return "";
    }

    return {
        signup: signup,
        login: login,
        logout: logout,
        currentUser: currentUser,
        requireAuth: requireAuth,
        wireHeaderAuth: wireHeaderAuth,
        request: request,
        unwrapData: unwrapData,
        formatDate: formatDate,
        toBadgeClass: toBadgeClass
    };
})();

