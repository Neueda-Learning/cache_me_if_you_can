(function () {
    if (!App.requireAuth()) {
        return;
    }
    App.wireHeaderAuth();

    var THRESHOLD_RULE_AMOUNT = 10000;
    var THRESHOLD_RULE_ID = 1;

    var submitBtn = document.getElementById("submitTransaction");
    var clearBtn = document.getElementById("clearTransaction");
    var closeModalBtn = document.getElementById("closeModal");

    var accountInput = document.getElementById("accountId");
    var payeeInput = document.getElementById("payeeId");
    var amountInput = document.getElementById("amount");
    var typeInput = document.getElementById("transactionType");

    var resultNode = document.getElementById("transactionResult");
    var modal = document.getElementById("alertModal");
    var modalMessage = document.getElementById("alertModalMessage");
    var alertsBody = document.getElementById("dashboardAlertsBody");

    loadDashboardAlerts();

    submitBtn.addEventListener("click", onSubmitTransaction);
    clearBtn.addEventListener("click", onClearForm);
    closeModalBtn.addEventListener("click", closeModal);
    modal.addEventListener("click", function (event) {
        if (event.target === modal) closeModal();
    });

    async function loadDashboardAlerts() {
        try {
            var response = await App.request("GET", "/api/v1/alerts?status=OPEN", undefined);
            var alerts = App.unwrapData(response) || [];

            var openCount = alerts.length;
            setCard("cardOpen", openCount);

            var ack = await App.request("GET", "/api/v1/alerts?status=ACKNOWLEDGED", undefined);
            setCard("cardAck", (App.unwrapData(ack) || []).length);

            var inv = await App.request("GET", "/api/v1/alerts?status=INVESTIGATING", undefined);
            setCard("cardInv", (App.unwrapData(inv) || []).length);

            var closed = await App.request("GET", "/api/v1/alerts?status=CLOSED", undefined);
            setCard("cardClosed", (App.unwrapData(closed) || []).length);

            renderAlertsTable(alerts.slice(0, 8));
        } catch (error) {
            renderAlertsError(error.message);
        }
    }

    function setCard(id, value) {
        var node = document.getElementById(id);
        if (node) node.textContent = String(value);
    }

    function renderAlertsTable(alerts) {
        if (!alerts.length) {
            alertsBody.innerHTML = "<tr><td colspan='6'>No open alerts.</td></tr>";
            return;
        }

        alertsBody.innerHTML = alerts.map(function (a) {
            return "<tr>" +
                "<td>#" + (a.alertId || "-") + "</td>" +
                "<td><span class='badge " + App.toBadgeClass(a.severity) + "'>" + (a.severity || "-") + "</span></td>" +
                "<td><span class='badge " + App.toBadgeClass(a.status) + "'>" + (a.status || "-") + "</span></td>" +
                "<td>" + (a.ruleId || "-") + "</td>" +
                "<td>" + (a.transactionId || "-") + "</td>" +
                "<td>" + App.formatDate(a.createdAt) + "</td>" +
                "</tr>";
        }).join("");
    }

    function renderAlertsError(message) {
        alertsBody.innerHTML = "<tr><td colspan='6'>" + message + "</td></tr>";
        setCard("cardOpen", "-");
        setCard("cardAck", "-");
        setCard("cardInv", "-");
        setCard("cardClosed", "-");
    }

    async function onSubmitTransaction() {
        var payload = {
            accountId: toNumber(accountInput.value),
            payeeId: toNumber(payeeInput.value),
            amount: toNumber(amountInput.value),
            transactionType: typeInput.value
        };

        if (!payload.accountId || !payload.payeeId || !payload.amount || payload.amount <= 0) {
            setResult("Please enter valid Account ID, Payee Account ID and Amount.", true);
            return;
        }

        setResult("Submitting transaction...", false);
        submitBtn.disabled = true;

        try {
            var transaction = await App.request("POST", "/api/transactions", payload);
            var transactionId = transaction.transactionId || transaction.id || transaction.transactionID;

            setResult("Transaction submitted successfully (ID: " + (transactionId || "N/A") + ").", false);

            if (payload.amount > THRESHOLD_RULE_AMOUNT) {
                await createAlertForThreshold(transactionId, payload.amount);
            }

            await loadDashboardAlerts();
        } catch (error) {
            setResult(error.message || "Unable to create transaction.", true);
        } finally {
            submitBtn.disabled = false;
        }
    }

    async function createAlertForThreshold(transactionId, amount) {
        if (!transactionId) {
            showModal("Threshold violated, but no transaction ID returned to create alert.");
            return;
        }

        var alertPayload = {
            ruleId: THRESHOLD_RULE_ID,
            transactionId: transactionId,
            severity: amount >= 20000 ? "CRITICAL" : "HIGH"
        };

        try {
            var alertResponse = await App.request("POST", "/api/v1/alerts", alertPayload);
            var alertData = App.unwrapData(alertResponse) || {};
            showModal(
                "Alert triggered for transaction " + transactionId +
                (alertData.alertId ? " (Alert ID: " + alertData.alertId + ")" : "") + "."
            );
        } catch (error) {
            showModal("Threshold violated, but alert creation failed: " + error.message);
        }
    }

    function showModal(message) {
        modalMessage.textContent = message;
        modal.classList.remove("hidden");
    }

    function closeModal() {
        modal.classList.add("hidden");
    }

    function onClearForm() {
        accountInput.value = "";
        payeeInput.value = "";
        amountInput.value = "";
        typeInput.value = "TRANSFER";
        setResult("No transaction submitted yet.", false);
    }

    function toNumber(value) {
        var n = Number(value);
        return Number.isFinite(n) ? n : NaN;
    }

    function setResult(message, isError) {
        resultNode.textContent = message;
        resultNode.classList.remove("msg-success", "msg-error");
        resultNode.classList.add(isError ? "msg-error" : "msg-success");
    }
})();
