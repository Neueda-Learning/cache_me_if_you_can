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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TransactionService {

    private static final Set<String> VALID_TYPES = Set.of("TRANSFER", "PAYMENT", "WITHDRAWAL");

    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final AlertService alertService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              RuleRepository ruleRepository,
                              AlertService alertService,
                              JdbcTemplate jdbcTemplate) {
        this.transactionRepository = transactionRepository;
        this.ruleRepository = ruleRepository;
        this.alertService = alertService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // Backwards-compatible constructor used by unit tests that only supply the
    // TransactionRepository. When this constructor is used the rule/alert
    // integration is effectively disabled (no alerts will be created).
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
        this.ruleRepository = null;
        this.alertService = null;
        this.jdbcTemplate = null;
    }

    /**
     * Validates and persists a new transaction.
     * Throws IllegalArgumentException for bad input — caught by GlobalExceptionHandler.
     */
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
                case THRESHOLD -> ruleImpl = new AmountThresholdRule(ruleDef);
                case DAILY_LIMIT -> ruleImpl = new DailyLimitRule(ruleDef, jdbcTemplate);
                case NEW_PAYEE -> ruleImpl = new NewPayeeRule(ruleDef, jdbcTemplate);
                default -> ruleImpl = null; // unsupported/placeholder
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
}
