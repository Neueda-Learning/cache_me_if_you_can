package com.neueda.transaction_monitor.rule;

import com.neueda.transaction_monitor.model.Transaction;

/**
 * Strategy interface for all transaction-monitoring rules.
 *
 * Each concrete implementation covers one Rule_Type from the DB:
 * THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT.
 *
 * evaluate() returns true when the rule is TRIGGERED
 * (an alert should be raised for this transaction).
 */
public interface Rule {

    /**
     * Evaluates whether the transaction violates this rule.
     * @return true if triggered
     */
    boolean evaluate(Transaction transaction);

    /**
     * Returns the DB rule definition powering this instance
     * (ruleId, severity, threshold, timeWindow, etc.).
     */
    com.neueda.transaction_monitor.model.Rule getRuleDefinition();
}

