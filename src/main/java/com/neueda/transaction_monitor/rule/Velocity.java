package com.neueda.transaction_monitor.rule;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;

import com.neueda.transaction_monitor.model.Transaction;

/**
 * VELOCITY rule — fires when the number of transactions made by an
 * account within the last Time_Window minutes meets or exceeds the
 * Threshold count.
 *
 * DB Rule_Type : VELOCITY
 * Uses fields  :
 *   Time_Window — look-back period in minutes (required)
 *   Threshold   — max allowed transaction count in that window (required)
 *
 * Example: Threshold = 5, Time_Window = 10
 *          → alert if the account makes ≥ 5 transactions in 10 minutes
 */
public class Velocity implements Rule {

    private final com.neueda.transaction_monitor.model.Rule ruleDefinition;
    private final JdbcTemplate jdbcTemplate;

    public Velocity(com.neueda.transaction_monitor.model.Rule ruleDefinition,
                    JdbcTemplate jdbcTemplate) {
        this.ruleDefinition = ruleDefinition;
        this.jdbcTemplate   = jdbcTemplate;
    }

    /**
     * Triggered when: count of recent transactions for the account
     * (within last timeWindow minutes) >= threshold
     */
    @Override
    public boolean evaluate(Transaction transaction) {
        if (ruleDefinition.getTimeWindow() == null || ruleDefinition.getThreshold() == null) {
            return false;
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(ruleDefinition.getTimeWindow());
        LocalDateTime now         = LocalDateTime.now();

        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM TRANSACTION_TABLE " +
            "WHERE Account_ID = ? AND Time_Stamp BETWEEN ? AND ?",
            Long.class,
            transaction.getAccountId(),
            Timestamp.valueOf(windowStart),
            Timestamp.valueOf(now)
        );

        long txCount = count != null ? count : 0L;
        return txCount >= ruleDefinition.getThreshold().longValue();
    }

    @Override
    public com.neueda.transaction_monitor.model.Rule getRuleDefinition() {
        return ruleDefinition;
    }
}

