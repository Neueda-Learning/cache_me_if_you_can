(function () {
    if (!App.requireAuth()) {
        return;
    }
    App.wireHeaderAuth();

    var searchBtn = document.getElementById("searchAlerts");
    var resetBtn = document.getElementById("resetAlerts");
    var tbody = document.getElementById("alertsBody");
    var messageNode = document.getElementById("alertsMessage");
    var detailsNode = document.getElementById("alertDetails");

    searchBtn.addEventListener("click", loadAlerts);
    resetBtn.addEventListener("click", function () {
        document.getElementById("status").value = "";
        document.getElementById("severity").value = "";
        loadAlerts();
    });

    loadAlerts();

    async function loadAlerts() {
        tbody.innerHTML = "<tr><td colspan='7'>Loading alerts...</td></tr>";
        setMessage("", false);

        try {
            var status = document.getElementById("status").value;
            var severity = document.getElementById("severity").value;
            var query = [];
            if (status) query.push("status=" + encodeURIComponent(status));
            if (severity) query.push("severity=" + encodeURIComponent(severity));

            var url = "/api/v1/alerts" + (query.length ? "?" + query.join("&") : "");
            var response = await App.request("GET", url);
            var alerts = App.unwrapData(response) || [];

            renderAlerts(alerts);
            setMessage("Loaded " + alerts.length + " alert(s).", false);
        } catch (error) {
            tbody.innerHTML = "<tr><td colspan='7'>Unable to load alerts.</td></tr>";
            setMessage(error.message, true);
        }
    }

    function renderAlerts(alerts) {
        if (!alerts.length) {
            tbody.innerHTML = "<tr><td colspan='7'>No alerts found.</td></tr>";
            return;
        }

        tbody.innerHTML = alerts.map(function (a) {
            return "<tr class='row-clickable' data-alert-id='" + a.alertId + "'>" +
                "<td>" + a.alertId + "</td>" +
                "<td><span class='badge " + App.toBadgeClass(a.severity) + "'>" + a.severity + "</span></td>" +
                "<td><span class='badge " + App.toBadgeClass(a.status) + "'>" + a.status + "</span></td>" +
                "<td>" + a.ruleId + "</td>" +
                "<td>" + a.transactionId + "</td>" +
                "<td>" + App.formatDate(a.createdAt) + "</td>" +
                "<td>" + actionButtons(a) + "</td>" +
                "</tr>";
        }).join("");

        Array.prototype.forEach.call(document.querySelectorAll(".row-clickable"), function (row) {
            row.addEventListener("click", function (event) {
                if (event.target.tagName === "BUTTON") return;
                var alertId = row.getAttribute("data-alert-id");
                showDetails(alertId);
            });
        });

        Array.prototype.forEach.call(document.querySelectorAll("[data-action]"), function (btn) {
            btn.addEventListener("click", function () {
                var alertId = btn.getAttribute("data-alert-id");
                var action = btn.getAttribute("data-action");
                applyAction(alertId, action);
            });
        });
    }

    function actionButtons(alert) {
        var actions = [];
        if (alert.status === "OPEN") actions.push(btn(alert.alertId, "acknowledge", "Acknowledge"));
        if (alert.status === "ACKNOWLEDGED") actions.push(btn(alert.alertId, "investigating", "Investigate"));
        if (alert.status === "INVESTIGATING") actions.push(btn(alert.alertId, "close", "Close"));
        if (["OPEN", "ACKNOWLEDGED", "INVESTIGATING"].indexOf(alert.status) >= 0) {
            actions.push(btn(alert.alertId, "dismiss", "Dismiss"));
        }
        return "<div class='action-group'>" + actions.join("") + "</div>";
    }

    function btn(alertId, action, label) {
        return "<button type='button' data-alert-id='" + alertId + "' data-action='" + action + "'>" + label + "</button>";
    }

    async function applyAction(alertId, action) {
        try {
            await App.request("PATCH", "/api/v1/alerts/" + alertId + "/" + action);
            await loadAlerts();
            await showDetails(alertId);
        } catch (error) {
            setMessage(error.message, true);
        }
    }

    async function showDetails(alertId) {
        try {
            var response = await App.request("GET", "/api/v1/alerts/" + alertId);
            var alert = App.unwrapData(response) || response;

            var transactionInfo = "";
            try {
                var tx = await App.request("GET", "/api/transactions/" + alert.transactionId);
                transactionInfo = "<p><strong>Transaction:</strong> ID " + (tx.transactionId || tx.id) +
                    ", Account " + tx.accountId + ", Payee " + tx.payeeId +
                    ", Amount " + tx.amount + "</p>";
            } catch (_ignore) {
                transactionInfo = "<p><strong>Transaction:</strong> not available from API.</p>";
            }

            detailsNode.innerHTML =
                "<p><strong>Alert ID:</strong> " + alert.alertId + "</p>" +
                "<p><strong>Status:</strong> <span class='badge " + App.toBadgeClass(alert.status) + "'>" + alert.status + "</span></p>" +
                "<p><strong>Severity:</strong> <span class='badge " + App.toBadgeClass(alert.severity) + "'>" + alert.severity + "</span></p>" +
                "<p><strong>Rule ID:</strong> " + alert.ruleId + "</p>" +
                "<p><strong>Created:</strong> " + App.formatDate(alert.createdAt) + "</p>" +
                "<p><strong>Closed:</strong> " + App.formatDate(alert.closedAt) + "</p>" +
                transactionInfo;
        } catch (error) {
            detailsNode.innerHTML = "<p>Unable to load alert details.</p>";
            setMessage(error.message, true);
        }
    }

    function setMessage(text, isError) {
        messageNode.textContent = text;
        messageNode.classList.remove("msg-success", "msg-error");
        if (text) messageNode.classList.add(isError ? "msg-error" : "msg-success");
    }
})();

