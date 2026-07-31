(function () {
    if (!App.requireAuth()) {
        return;
    }
    App.wireHeaderAuth();

    var applyBtn = document.getElementById("applyTxFilters");
    var resetBtn = document.getElementById("resetTxFilters");
    var bodyNode = document.getElementById("transactionsBody");
    var messageNode = document.getElementById("transactionsMessage");

    applyBtn.addEventListener("click", loadTransactions);
    resetBtn.addEventListener("click", resetFilters);

    function resetFilters() {
        document.getElementById("searchTx").value = "";
        document.getElementById("account").value = "";
        document.getElementById("fromDate").value = "";
        document.getElementById("toDate").value = "";
        bodyNode.innerHTML = "<tr><td colspan='6'>Use filters and click Apply Filters.</td></tr>";
        setMessage("", false);
    }

    async function loadTransactions() {
        var txId = document.getElementById("searchTx").value.trim();
        var accountId = document.getElementById("account").value.trim();
        var from = document.getElementById("fromDate").value;
        var to = document.getElementById("toDate").value;

        bodyNode.innerHTML = "<tr><td colspan='6'>Loading transactions...</td></tr>";

        try {
            var transactions;

            if (txId) {
                var tx = await App.request("GET", "/api/transactions/" + encodeURIComponent(txId));
                transactions = [tx];
            } else if (accountId && from && to) {
                var url = "/api/transactions/range?from=" + encodeURIComponent(toIso(from)) + "&to=" + encodeURIComponent(toIso(to));
                var list = await App.request("GET", url);
                transactions = list.filter(function (t) { return String(t.accountId) === String(accountId); });
            } else if (accountId) {
                transactions = await App.request("GET", "/api/transactions/account/" + encodeURIComponent(accountId));
            } else if (from && to) {
                var rangeUrl = "/api/transactions/range?from=" + encodeURIComponent(toIso(from)) + "&to=" + encodeURIComponent(toIso(to));
                transactions = await App.request("GET", rangeUrl);
            } else {
                transactions = [];
                setMessage("Please provide at least Transaction ID, Account ID, or date range.", true);
            }

            renderRows(transactions || []);
            if ((transactions || []).length) {
                setMessage("Loaded " + transactions.length + " transaction(s).", false);
            }
        } catch (error) {
            bodyNode.innerHTML = "<tr><td colspan='6'>Unable to load transactions.</td></tr>";
            setMessage(error.message, true);
        }
    }

    function renderRows(rows) {
        if (!rows.length) {
            bodyNode.innerHTML = "<tr><td colspan='6'>No transactions found.</td></tr>";
            return;
        }

        bodyNode.innerHTML = rows.map(function (t) {
            return "<tr>" +
                "<td>" + value(t.transactionId || t.id) + "</td>" +
                "<td>" + value(t.accountId) + "</td>" +
                "<td>" + value(t.payeeId) + "</td>" +
                "<td>" + currency(t.amount) + "</td>" +
                "<td>" + value(t.transactionType) + "</td>" +
                "<td>" + App.formatDate(t.timeStamp || t.timestamp || t.createdAt) + "</td>" +
                "</tr>";
        }).join("");
    }

    function setMessage(text, isError) {
        messageNode.textContent = text;
        messageNode.classList.remove("msg-success", "msg-error");
        if (text) {
            messageNode.classList.add(isError ? "msg-error" : "msg-success");
        }
    }

    function toIso(value) {
        return new Date(value).toISOString();
    }

    function value(v) {
        return v === undefined || v === null || v === "" ? "-" : String(v);
    }

    function currency(v) {
        var num = Number(v);
        if (!Number.isFinite(num)) return "-";
        return "$" + num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
})();

