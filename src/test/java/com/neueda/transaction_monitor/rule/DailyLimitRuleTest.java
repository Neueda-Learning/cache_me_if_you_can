package com.neueda.transaction_monitor.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.neueda.transaction_monitor.model.Rule;
import com.neueda.transaction_monitor.model.Transaction;

@ExtendWith(MockitoExtension.class)
class DailyLimitRuleTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldTriggerWhenDailyTotalWouldExceedThreshold() {
        Rule definition = new Rule();
        definition.setThreshold(new BigDecimal("50000"));

        when(jdbcTemplate.queryForObject(any(String.class), eq(BigDecimal.class), anyInt()))
                .thenReturn(new BigDecimal("49500"));

        Transaction tx = new Transaction();
        tx.setAccountId(1);
        tx.setAmount(new BigDecimal("600"));

        DailyLimitRule rule = new DailyLimitRule(definition, jdbcTemplate);

        assertTrue(rule.evaluate(tx));
    }

    @Test
    void shouldNotTriggerWhenBelowThreshold() {
        Rule definition = new Rule();
        definition.setThreshold(new BigDecimal("50000"));

        when(jdbcTemplate.queryForObject(any(String.class), eq(BigDecimal.class), anyInt()))
                .thenReturn(new BigDecimal("49000"));

        Transaction tx = new Transaction();
        tx.setAccountId(1);
        tx.setAmount(new BigDecimal("600"));

        DailyLimitRule rule = new DailyLimitRule(definition, jdbcTemplate);

        assertFalse(rule.evaluate(tx));
    }

    @Test
    void shouldNotTriggerWhenThresholdMissing() {
        Rule definition = new Rule();
        definition.setThreshold(null);

        Transaction tx = new Transaction();
        tx.setAccountId(1);
        tx.setAmount(new BigDecimal("100"));

        DailyLimitRule rule = new DailyLimitRule(definition, jdbcTemplate);

        assertFalse(rule.evaluate(tx));
    }
}

