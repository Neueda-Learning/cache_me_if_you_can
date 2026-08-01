package com.neueda.transaction_monitor.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.neueda.transaction_monitor.model.Rule;
import com.neueda.transaction_monitor.model.Transaction;

class AmountThresholdRuleTest {

    @Test
    void shouldTriggerWhenAmountExceedsThreshold() {
        Rule definition = new Rule();
        definition.setThreshold(new BigDecimal("10000"));

        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("10000.01"));

        AmountThresholdRule rule = new AmountThresholdRule(definition);

        assertTrue(rule.evaluate(tx));
    }

    @Test
    void shouldNotTriggerWhenAmountEqualsThreshold() {
        Rule definition = new Rule();
        definition.setThreshold(new BigDecimal("10000"));

        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("10000"));

        AmountThresholdRule rule = new AmountThresholdRule(definition);

        assertFalse(rule.evaluate(tx));
    }

    @Test
    void shouldNotTriggerWhenThresholdMissing() {
        Rule definition = new Rule();

        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("15000"));

        AmountThresholdRule rule = new AmountThresholdRule(definition);

        assertFalse(rule.evaluate(tx));
    }
}

