package com.neueda.transaction_monitor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.transaction_monitor.dto.RuleDto.CreateRuleRequest;
import com.neueda.transaction_monitor.dto.RuleDto.RuleResponse;
import com.neueda.transaction_monitor.dto.RuleDto.UpdateRuleRequest;
import com.neueda.transaction_monitor.model.Rule;
import com.neueda.transaction_monitor.model.Rule.RuleSeverity;
import com.neueda.transaction_monitor.model.Rule.RuleType;
import com.neueda.transaction_monitor.repository.RuleRepository;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService(ruleRepository);
    }

    // ── createRule ────────────────────────────────────────────────────────────

    @Test
    void shouldCreateThresholdRule() {
        Rule saved = buildRule(1L, "High Value", RuleType.THRESHOLD,
                RuleSeverity.HIGH, new BigDecimal("10000"), null, true);

        when(ruleRepository.create(
                eq("High Value"), eq(RuleType.THRESHOLD), eq(RuleSeverity.HIGH),
                eq(new BigDecimal("10000")), eq(null)))
                .thenReturn(saved);

        CreateRuleRequest request = new CreateRuleRequest(
                "High Value", RuleType.THRESHOLD, RuleSeverity.HIGH,
                new BigDecimal("10000"), null);

        RuleResponse response = ruleEngineService.createRule(request);

        assertEquals(1L, response.ruleId());
        assertEquals("High Value", response.ruleName());
        assertEquals(RuleType.THRESHOLD, response.ruleType());
        assertTrue(response.activeStatus());
    }

    // ── getRuleById ───────────────────────────────────────────────────────────

    @Test
    void shouldReturnRuleWhenFound() {
        Rule rule = buildRule(2L, "Velocity Rule", RuleType.VELOCITY,
                RuleSeverity.MEDIUM, new BigDecimal("5"), 10, true);
        when(ruleRepository.findById(2L)).thenReturn(Optional.of(rule));

        RuleResponse response = ruleEngineService.getRuleById(2L);

        assertEquals(2L, response.ruleId());
        assertEquals(RuleType.VELOCITY, response.ruleType());
        assertEquals(10, response.timeWindow());
    }

    @Test
    void shouldThrowWhenRuleNotFound() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> ruleEngineService.getRuleById(999L));
    }

    // ── getRules ──────────────────────────────────────────────────────────────

    @Test
    void shouldReturnAllActiveRules() {
        when(ruleRepository.findAll(null, true)).thenReturn(List.of(
                buildRule(1L, "Rule A", RuleType.THRESHOLD, RuleSeverity.HIGH,
                        new BigDecimal("10000"), null, true),
                buildRule(2L, "Rule B", RuleType.NEW_PAYEE, RuleSeverity.MEDIUM,
                        null, null, true)
        ));

        List<RuleResponse> result = ruleEngineService.getRules(null, true);

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoRules() {
        when(ruleRepository.findAll(null, null)).thenReturn(List.of());

        assertEquals(0, ruleEngineService.getRules(null, null).size());
    }

    // ── updateRule ────────────────────────────────────────────────────────────

    @Test
    void shouldUpdateRuleFields() {
        Rule existing = buildRule(1L, "Old Name", RuleType.THRESHOLD,
                RuleSeverity.HIGH, new BigDecimal("10000"), null, true);
        Rule updated  = buildRule(1L, "New Name", RuleType.THRESHOLD,
                RuleSeverity.CRITICAL, new BigDecimal("20000"), null, true);

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ruleRepository.update(eq(1L), eq("New Name"), eq(RuleSeverity.CRITICAL),
                eq(new BigDecimal("20000")), eq(null), eq(null)))
                .thenReturn(updated);

        UpdateRuleRequest request = new UpdateRuleRequest(
                "New Name", RuleSeverity.CRITICAL, new BigDecimal("20000"), null, null);

        RuleResponse response = ruleEngineService.updateRule(1L, request);

        assertEquals("New Name", response.ruleName());
        assertEquals(RuleSeverity.CRITICAL, response.severity());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentRule() {
        when(ruleRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateRuleRequest request = new UpdateRuleRequest(
                "Any", RuleSeverity.LOW, null, null, null);

        assertThrows(NoSuchElementException.class,
                () -> ruleEngineService.updateRule(99L, request));
        verify(ruleRepository, never()).update(any(), any(), any(), any(), any(), any());
    }

    // ── activateRule / deactivateRule ─────────────────────────────────────────

    @Test
    void shouldActivateRule() {
        Rule inactive = buildRule(1L, "Rule", RuleType.THRESHOLD,
                RuleSeverity.HIGH, new BigDecimal("10000"), null, false);
        Rule active   = buildRule(1L, "Rule", RuleType.THRESHOLD,
                RuleSeverity.HIGH, new BigDecimal("10000"), null, true);

        when(ruleRepository.findById(1L))
                .thenReturn(Optional.of(inactive))
                .thenReturn(Optional.of(active));

        RuleResponse response = ruleEngineService.activateRule(1L);

        verify(ruleRepository).toggleActive(1L, true);
        assertTrue(response.activeStatus());
    }

    @Test
    void shouldDeactivateRule() {
        Rule active   = buildRule(1L, "Rule", RuleType.THRESHOLD,
                RuleSeverity.HIGH, new BigDecimal("10000"), null, true);
        Rule inactive = buildRule(1L, "Rule", RuleType.THRESHOLD,
                RuleSeverity.HIGH, new BigDecimal("10000"), null, false);

        when(ruleRepository.findById(1L))
                .thenReturn(Optional.of(active))
                .thenReturn(Optional.of(inactive));

        RuleResponse response = ruleEngineService.deactivateRule(1L);

        verify(ruleRepository).toggleActive(1L, false);
        assertFalse(response.activeStatus());
    }

    // ── deleteRule ────────────────────────────────────────────────────────────

    @Test
    void shouldDeleteExistingRule() {
        Rule rule = buildRule(1L, "Rule", RuleType.THRESHOLD,
                RuleSeverity.HIGH, new BigDecimal("10000"), null, true);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));

        ruleEngineService.deleteRule(1L);

        verify(ruleRepository).delete(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentRule() {
        when(ruleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> ruleEngineService.deleteRule(99L));
        verify(ruleRepository, never()).delete(any());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Rule buildRule(Long id, String name, RuleType type,
                           RuleSeverity severity, BigDecimal threshold,
                           Integer timeWindow, boolean active) {
        Rule r = new Rule();
        r.setRuleId(id);
        r.setRuleName(name);
        r.setRuleType(type);
        r.setSeverity(severity);
        r.setThreshold(threshold);
        r.setTimeWindow(timeWindow);
        r.setActiveStatus(active);
        return r;
    }
}