package com.neueda.transaction_monitor.service;

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
        // ensure rule exists before update
        getOrThrow(ruleId);
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

    // ── Helpers ──────────────────────────────────────────────────────────────

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
