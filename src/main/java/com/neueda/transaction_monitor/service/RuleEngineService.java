package com.neueda.transaction_monitor.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.neueda.transaction_monitor.dto.RuleDto.CreateRuleRequest;
import com.neueda.transaction_monitor.dto.RuleDto.RuleResponse;
import com.neueda.transaction_monitor.dto.RuleDto.UpdateRuleRequest;
import com.neueda.transaction_monitor.model.Rule;
import com.neueda.transaction_monitor.model.Rule.RuleType;
import com.neueda.transaction_monitor.repository.RuleRepository;

@Service
public class RuleEngineService {

    private final RuleRepository ruleRepository;

    public RuleEngineService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public RuleResponse createRule(CreateRuleRequest request) {
        validateRuleFields(request.ruleType(), request.threshold(), request.timeWindow());
        Rule rule = ruleRepository.create(
                request.ruleName(), request.ruleType(), request.severity(),
                request.threshold(), request.timeWindow()
        );
        return toResponse(rule);
    }

    public RuleResponse getRuleById(Long ruleId) {
        return toResponse(getOrThrow(ruleId));
    }

    public List<RuleResponse> getRules(RuleType ruleType, Boolean activeStatus) {
        return ruleRepository.findAll(ruleType, activeStatus).stream().map(this::toResponse).toList();
    }

    public RuleResponse updateRule(Long ruleId, UpdateRuleRequest request) {
        Rule existing = getOrThrow(ruleId);
        // validate updated threshold/timeWindow if provided
        BigDecimal threshold = request.threshold() != null ? request.threshold() : existing.getThreshold();
        Integer timeWindow   = request.timeWindow()  != null ? request.timeWindow()  : existing.getTimeWindow();
        validateRuleFields(existing.getRuleType(), threshold, timeWindow);

        Rule updated = ruleRepository.update(
                ruleId, request.ruleName(), request.severity(),
                request.threshold(), request.timeWindow(), request.activeStatus()
        );
        return toResponse(updated);
    }

    public RuleResponse activateRule(Long ruleId) {
        getOrThrow(ruleId);
        ruleRepository.toggleActive(ruleId, true);
        return toResponse(getOrThrow(ruleId));
    }

    public RuleResponse deactivateRule(Long ruleId) {
        getOrThrow(ruleId);
        ruleRepository.toggleActive(ruleId, false);
        return toResponse(getOrThrow(ruleId));
    }

    public void deleteRule(Long ruleId) {
        getOrThrow(ruleId);
        ruleRepository.delete(ruleId);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Enforces per-rule-type field requirements:
     *
     * THRESHOLD   — threshold required and > 0
     * DAILY_LIMIT — threshold required and > 0
     * VELOCITY    — threshold (max tx count) required + timeWindow (minutes) required
     * NEW_PAYEE   — no threshold or timeWindow needed
     */
    private void validateRuleFields(RuleType type, BigDecimal threshold, Integer timeWindow) {
        switch (type) {
            case THRESHOLD -> {
                if (threshold == null)
                    throw new IllegalArgumentException(
                            "Threshold value is required");
                if (threshold.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException(
                            "Threshold must be greater than zero");
            }
            case DAILY_LIMIT -> {
                if (threshold == null)
                    throw new IllegalArgumentException(
                            "Daily limit value is required");
                if (threshold.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException(
                            "Threshold must be greater than zero");
            }
            case VELOCITY -> {
                if (threshold == null)
                    throw new IllegalArgumentException(
                            "Threshold value is required");
                if (threshold.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException(
                            "Threshold must be greater than zero");
                if (timeWindow == null)
                    throw new IllegalArgumentException(
                            "Time window value is required");
                if (timeWindow < 1)
                    throw new IllegalArgumentException(
                            "Time window must be at least 1 minute");
            }
            case NEW_PAYEE -> {
                // no fields required for new payee detection
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Rule getOrThrow(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Rule not found: " + ruleId));
    }

    private RuleResponse toResponse(Rule rule) {
        return new RuleResponse(
                rule.getRuleId(), rule.getRuleName(), rule.getRuleType(),
                rule.getSeverity(), rule.getThreshold(), rule.getTimeWindow(), rule.getActiveStatus()
        );
    }
}