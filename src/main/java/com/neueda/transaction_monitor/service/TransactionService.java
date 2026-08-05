package com.neueda.transaction_monitor.service;

import com.neueda.transaction_monitor.exception.TransactionNotFoundException;
import com.neueda.transaction_monitor.model.AccountSummary;
import com.neueda.transaction_monitor.model.Transaction;
import com.neueda.transaction_monitor.repository.TransactionRepository;
import com.neueda.transaction_monitor.repository.RuleRepository;
import com.neueda.transaction_monitor.dto.AlertDto.CreateAlertRequest;
import com.neueda.transaction_monitor.service.AlertService;
import com.neueda.transaction_monitor.rule.AmountThresholdRule;
import com.neueda.transaction_monitor.rule.DailyLimitRule;
import com.neueda.transaction_monitor.rule.NewPayeeRule;
import com.neueda.transaction_monitor.rule.Velocity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;

@Service
public class TransactionService {

    private static final Set<String> VALID_TYPES = Set.of("TRANSFER", "PAYMENT", "WITHDRAWAL");

    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final AlertService alertService;
    private final JdbcTemplate jdbcTemplate;
    private final com.neueda.transaction_monitor.repository.AccountRepository accountRepository;
    private final MailService mailService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              RuleRepository ruleRepository,
                              AlertService alertService,
                              JdbcTemplate jdbcTemplate,
                              com.neueda.transaction_monitor.repository.AccountRepository accountRepository,
                              MailService mailService) {
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
        this.alertService = alertService;
        this.jdbcTemplate = jdbcTemplate;
        this.accountRepository = accountRepository;
        this.mailService = mailService;
    }

    // Backwards-compatible constructor used by unit tests that only supply the
    // TransactionRepository. When this constructor is used the rule/alert
    // integration is effectively disabled (no alerts will be created).
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
        this.ruleRepository = null;
        this.alertService = null;
        this.jdbcTemplate = null;
        this.accountRepository = null;
        this.mailService = null;
    }

    /**
     * Validates and persists a new transaction.
     * Throws IllegalArgumentException for bad input — caught by GlobalExceptionHandler.
     */
    @Transactional
    public Transaction createTransaction(Transaction t) {
        if (t.getAccountId() == null || t.getAccountId() <= 0) {
            throw new IllegalArgumentException("Account ID must be positive");
        }
        if (t.getPayeeId() == null || t.getPayeeId() <= 0) {
            throw new IllegalArgumentException("Payee ID must be positive");
        }
        if (t.getAmount() == null || t.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (t.getTransactionType() == null || !VALID_TYPES.contains(t.getTransactionType().toUpperCase())) {
            throw new IllegalArgumentException("Transaction type must be one of: TRANSFER, PAYMENT, WITHDRAWAL");
        }
        t.setTransactionType(t.getTransactionType().toUpperCase());

        // Validate sender account exists, then debit atomically to avoid race conditions.
        if (accountRepository != null) {
            var acctOpt = accountRepository.findById(t.getAccountId());
            if (acctOpt.isEmpty()) {
                throw new IllegalArgumentException("Account with ID " + t.getAccountId() + " not found");
            }
            boolean debited = accountRepository.debitIfSufficientBalance(t.getAccountId(), t.getAmount());
            if (!debited) {
                throw new IllegalArgumentException("Insufficient balance for this transaction");
            }
        }

        Transaction saved;

        if (ruleRepository == null || alertService == null || jdbcTemplate == null) {
            // Rule/alert integration disabled (e.g. unit test environment)
            saved = transactionRepository.save(t);
            return saved;
        }

        // ── Rule evaluation (evaluate BEFORE persisting so rules that look at prior
        // transactions — e.g. NEW_PAYEE — are not influenced by the incoming tx)
        var activeRules = ruleRepository.findAll(null, true);

        // Collect rules that trigger for this transaction
        record TriggeredRule(Long ruleId, com.neueda.transaction_monitor.model.Rule.RuleSeverity severity) {}
        java.util.List<TriggeredRule> triggered = new java.util.ArrayList<>();

        for (var ruleDef : activeRules) {
            com.neueda.transaction_monitor.rule.Rule ruleImpl = null;
            switch (ruleDef.getRuleType()) {
                case THRESHOLD   -> ruleImpl = new AmountThresholdRule(ruleDef);
                case DAILY_LIMIT -> ruleImpl = new DailyLimitRule(ruleDef, jdbcTemplate);
                case NEW_PAYEE   -> ruleImpl = new NewPayeeRule(ruleDef, jdbcTemplate);
                case VELOCITY    -> ruleImpl = new Velocity(ruleDef, jdbcTemplate);
                default          -> ruleImpl = null;
            }

            if (ruleImpl != null && ruleImpl.evaluate(t)) {
                triggered.add(new TriggeredRule(ruleDef.getRuleId(), ruleDef.getSeverity()));
            }
        }

        // Persist transaction and then create alerts for any triggered rules
        saved = transactionRepository.save(t);


        for (var tr : triggered) {
            // Map RuleSeverity -> AlertSeverity (same enum names)
            com.neueda.transaction_monitor.model.Alert.AlertSeverity sev =
                com.neueda.transaction_monitor.model.Alert.AlertSeverity.valueOf(tr.severity().name());

            // Create alert using AlertService DTO
            CreateAlertRequest req = new CreateAlertRequest(tr.ruleId(), Long.valueOf(saved.getTransactionId()), sev);
            alertService.createAlert(req);

            // If the triggered alert is HIGH severity, write a mail into the account's folder
            if (sev == com.neueda.transaction_monitor.model.Alert.AlertSeverity.HIGH) {
                try {
                    String accNum = null;
                    String acctEmail = null;
                    if (accountRepository != null) {
                        var acctOpt = accountRepository.findById(saved.getAccountId());
                        if (acctOpt.isPresent()) {
                            accNum = acctOpt.get().getAccountNumber();
                            try { acctEmail = acctOpt.get().getEmail(); } catch (Exception ignore) {}
                        }
                    }
                    String subject = "HAWK: High Severity Transaction";

                    LocalDateTime ts = saved.getTimeStamp() == null ? LocalDateTime.now() : saved.getTimeStamp();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a", Locale.ENGLISH);
                    String formattedDate = ts.format(dtf);

                    NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                    String formattedAmount = nf.format(saved.getAmount() == null ? java.math.BigDecimal.ZERO : saved.getAmount());

                    String txnLabel = "TXN-" + saved.getTransactionId();

                    StringBuilder sb = new StringBuilder();
                    sb.append("Dear Customer,\n\n");
                    sb.append("We detected a transaction on your account that has been flagged for review by our monitoring system.\n\n");
                    sb.append("Transaction Details\n");
                    sb.append("------------------------------------------------------------\n");
                    sb.append(String.format("Transaction ID : %s\n", txnLabel));
                    sb.append(String.format("Date & Time    : %s\n", formattedDate));
                    sb.append(String.format("Amount         : %s\n", formattedAmount));
                    sb.append("Status         : Under Review\n");
                    sb.append(String.format("Severity       : %s\n\n", sev.name()));
                    sb.append("Why are you receiving this email?\n");
                    sb.append("Our monitoring system identified this transaction as unusual based on our security checks. This does not necessarily mean the transaction is fraudulent, but we recommend reviewing it as soon as possible.\n\n");
                    sb.append("Kind regards,\n\nTransaction Monitoring & Alert Management Team\n");

                    String body = sb.toString();
                    if (mailService != null) {
                        mailService.sendToAccountFolder(accNum, acctEmail, subject, body);
                    }
                } catch (Exception e) {
                    // don't block transaction processing on mail failures
                }
            }
        }

        return saved;
    }

    /**
     * Fetches a single transaction or throws TransactionNotFoundException (→ HTTP 404).
     */
    public Transaction getTransactionById(int id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    /**
     * Returns all transactions for a given account — empty list is a valid response.
     */
    public List<Transaction> getTransactionsByAccount(int accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    /**
     * Returns all transactions, most recent first.
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Returns transactions in a time range. Validates that 'from' precedes 'to'.
     */
    public List<Transaction> getTransactionsByTimeWindow(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' timestamp must be before 'to' timestamp");
        }
        return transactionRepository.findByTimeWindow(from, to);
    }

    /**
     * Returns a summary for an account via the stored procedure (CallableStatement path).
     */
    public AccountSummary getAccountSummary(int accountId) {
        return transactionRepository.getAccountTransactionSummary(accountId);
    }

    /**
     * Returns transactions for the UI legend view (includes account numbers).
     * filterBy accepted values (case-insensitive): transactionId, accountNumber, payeeAccountNumber
     */
    public java.util.List<com.neueda.transaction_monitor.model.TransactionView> getTransactionList(String filterBy, String value) {
        String fb = "ALL";
        String val = value;
        if (filterBy != null && !filterBy.isBlank()) {
            switch (filterBy.toLowerCase()) {
                case "transactionid", "transactionId", "txn", "txnid", "transaction" -> fb = "TXN";
                case "accountnumber", "accountNumber", "account" -> fb = "ACCOUNT";
                case "payeeaccountnumber", "payeeAccountNumber", "payee" -> fb = "PAYEE";
                default -> fb = "ALL";
            }
        }
        if (val == null) val = "";
        return transactionRepository.getTransactionList(fb, val);
    }
}
