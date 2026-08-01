package com.neueda.transaction_monitor.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.neueda.transaction_monitor.model.Rule;
import com.neueda.transaction_monitor.model.Transaction;

@ExtendWith(MockitoExtension.class)
class NewPayeeRuleTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldTriggerForFirstTimePayee() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(), any())).thenReturn(0L);

        Rule definition = new Rule();
        Transaction tx = new Transaction();
        tx.setAccountId(1);
        tx.setPayeeId(10);

        NewPayeeRule rule = new NewPayeeRule(definition, jdbcTemplate);

        assertTrue(rule.evaluate(tx));
    }

    @Test
    void shouldNotTriggerWhenPayeeSeenBefore() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(), any())).thenReturn(3L);

        Rule definition = new Rule();
        Transaction tx = new Transaction();
        tx.setAccountId(1);
        tx.setPayeeId(10);

        NewPayeeRule rule = new NewPayeeRule(definition, jdbcTemplate);

        assertFalse(rule.evaluate(tx));
    }
}

