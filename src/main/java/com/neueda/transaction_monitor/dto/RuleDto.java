package com.neueda.transaction_monitor.dto;

import com.neueda.transaction_monitor.model.Rule.RuleSeverity;
import com.neueda.transaction_monitor.model.Rule.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RuleDto {

    public record CreateRuleRequest(
        @NotBlank String ruleName,
        @NotNull RuleType ruleType,
        @NotNull RuleSeverity severity,
        BigDecimal threshold,
        Integer timeWindow
    ) {}

    public record UpdateRuleRequest(
        String ruleName,
        RuleSeverity severity,
        BigDecimal threshold,
        Integer timeWindow,
        Boolean activeStatus
    ) {}

    public record RuleResponse(
        Long ruleId,
        String ruleName,
        RuleType ruleType,
        RuleSeverity severity,
        BigDecimal threshold,
        Integer timeWindow,
        Boolean activeStatus
    ) {}
}

