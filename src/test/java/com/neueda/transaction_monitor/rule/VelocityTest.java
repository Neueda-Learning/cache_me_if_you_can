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
class VelocityTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldTriggerWhenCountMeetsThreshold() {
        Rule definition = new Rule();
        definition.setThreshold(new BigDecimal("5"));
        definition.setTimeWindow(10);

        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), anyInt(), anyInt()))
                .thenReturn(5L);

        Transaction tx = new Transaction();
        tx.setAccountId(1);

        Velocity rule = new Velocity(definition, jdbcTemplate);

        assertTrue(rule.evaluate(tx));
    }

    @Test
    void shouldNotTriggerWhenCountBelowThreshold() {
        Rule definition = new Rule();
        definition.setThreshold(new BigDecimal("5"));
        definition.setTimeWindow(10);

        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), anyInt(), anyInt()))
                .thenReturn(4L);

        Transaction tx = new Transaction();
        tx.setAccountId(1);

        Velocity rule = new Velocity(definition, jdbcTemplate);

        assertFalse(rule.evaluate(tx));
    }

    @Test
    void shouldNotTriggerWhenConfigMissing() {
        Rule definition = new Rule();
        definition.setThreshold(null);
        definition.setTimeWindow(10);

        Transaction tx = new Transaction();
        tx.setAccountId(1);

        Velocity rule = new Velocity(definition, jdbcTemplate);

        assertFalse(rule.evaluate(tx));
    }
}

