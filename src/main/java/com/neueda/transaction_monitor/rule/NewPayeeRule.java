package com.neueda.transaction_monitor.rule;

import org.springframework.jdbc.core.JdbcTemplate;

import com.neueda.transaction_monitor.model.Transaction;

/**
 * NEW_PAYEE rule — fires when the payee has never received a payment
 * from this account before (first-time payee detection).
 *
 * DB Rule_Type : NEW_PAYEE
 * Uses fields  : none (Threshold / Time_Window not required)
 *
 * Example: Account 101 sends to Payee 55 for the first time → alert
 */
public class NewPayeeRule implements Rule {

    private final com.neueda.transaction_monitor.model.Rule ruleDefinition;
    private final JdbcTemplate jdbcTemplate;

    public NewPayeeRule(com.neueda.transaction_monitor.model.Rule ruleDefinition,
                        JdbcTemplate jdbcTemplate) {
        this.ruleDefinition = ruleDefinition;
        this.jdbcTemplate   = jdbcTemplate;
    }

    /**
     * Triggered when: no prior transaction exists for this (accountId, payeeId) pair
     */
    @Override
    public boolean evaluate(Transaction transaction) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM TRANSACTION_TABLE " +
            "WHERE Account_ID = ? AND Payee_ID = ?",
            Long.class,
            transaction.getAccountId(),
            transaction.getPayeeId()
        );
        return count == null || count == 0;
    }

    @Override
    public com.neueda.transaction_monitor.model.Rule getRuleDefinition() {
        return ruleDefinition;
    }
}

