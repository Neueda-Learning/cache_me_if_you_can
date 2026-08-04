package com.neueda.transaction_monitor.dto;

import com.neueda.transaction_monitor.model.Rule.RuleSeverity;
import com.neueda.transaction_monitor.model.Rule.RuleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RuleDto {

    public record CreateRuleRequest(
            @NotBlank(message = "Rule name is required")
            @Size(max = 100, message = "Rule name must be 100 characters or fewer")
            String ruleName,

            @NotNull(message = "Rule type is required")
            RuleType ruleType,

            @NotNull(message = "Severity is required")
            RuleSeverity severity,

            // When provided, threshold must be > 0
            @DecimalMin(value = "0.01", message = "Threshold must be greater than zero")
            BigDecimal threshold,

            // When provided, time window must be >= 1 minute
            @Min(value = 1, message = "Time window must be at least 1 minute")
            Integer timeWindow
    ) {}

    public record UpdateRuleRequest(
            @Size(max = 100, message = "Rule name must be 100 characters or fewer")
            String ruleName,

            RuleSeverity severity,

            @DecimalMin(value = "0.01", message = "Threshold must be greater than zero")
            BigDecimal threshold,

            @Min(value = 1, message = "Time window must be at least 1 minute")
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