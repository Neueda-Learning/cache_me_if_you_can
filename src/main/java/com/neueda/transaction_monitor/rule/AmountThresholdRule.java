package com.neueda.transaction_monitor.rule;

import com.neueda.transaction_monitor.model.Transaction;

/**
 * THRESHOLD rule — fires when a single transaction amount
 * exceeds the configured threshold value.
 *
 * DB Rule_Type : THRESHOLD
 * Uses field   : Threshold (required)
 *
 * Example: Threshold = 10000 → alert on any transaction > £10,000
 */
public class AmountThresholdRule implements Rule {

    private final com.neueda.transaction_monitor.model.Rule ruleDefinition;

    public AmountThresholdRule(com.neueda.transaction_monitor.model.Rule ruleDefinition) {
        this.ruleDefinition = ruleDefinition;
    }

    /** Triggered when: transaction.amount > rule.threshold */
    @Override
    public boolean evaluate(Transaction transaction) {
        if (ruleDefinition.getThreshold() == null) return false;
        return transaction.getAmount().compareTo(ruleDefinition.getThreshold()) > 0;
    }

    @Override
    public com.neueda.transaction_monitor.model.Rule getRuleDefinition() {
        return ruleDefinition;
    }
}

