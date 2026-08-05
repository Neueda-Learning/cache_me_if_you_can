package com.neueda.transaction_monitor.rule;

import java.math.BigDecimal;

import org.springframework.jdbc.core.JdbcTemplate;

import com.neueda.transaction_monitor.model.Transaction;

/**
 * DAILY_LIMIT rule — fires when the account's total spend today
 * (all committed transactions + this incoming transaction) would
 * exceed the configured threshold.
 *
 * DB Rule_Type : DAILY_LIMIT
 * Uses field   : Threshold (required — the max allowed daily amount)
 *
 * Example: Threshold = 50000 → alert when daily total would exceed £50,000
 */
public class DailyLimitRule implements Rule {

    private final com.neueda.transaction_monitor.model.Rule ruleDefinition;
    private final JdbcTemplate jdbcTemplate;

    public DailyLimitRule(com.neueda.transaction_monitor.model.Rule ruleDefinition,
                          JdbcTemplate jdbcTemplate) {
        this.ruleDefinition = ruleDefinition;
        this.jdbcTemplate   = jdbcTemplate;
    }

    /**
     * Triggered when: (today's existing spend + incoming amount) > rule.threshold
     */
    @Override
    public boolean evaluate(Transaction transaction) {
        if (ruleDefinition.getThreshold() == null) return false;

        BigDecimal todaySpend = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(Amount), 0) FROM TRANSACTION_TABLE " +
            "WHERE Account_ID = ? AND Time_Stamp BETWEEN CURRENT_DATE() AND NOW()",
            BigDecimal.class,
            transaction.getAccountId()
        );

        if (todaySpend == null) todaySpend = BigDecimal.ZERO;

        return todaySpend.add(transaction.getAmount())
                         .compareTo(ruleDefinition.getThreshold()) > 0;
    }

    @Override
    public com.neueda.transaction_monitor.model.Rule getRuleDefinition() {
        return ruleDefinition;
    }
}

