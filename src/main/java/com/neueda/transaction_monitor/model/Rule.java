package com.neueda.transaction_monitor.model;

import java.math.BigDecimal;

public class Rule {

    public enum RuleType {
        THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT
    }

    public enum RuleSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    private Long ruleId;
    private String ruleName;
    private RuleType ruleType;
    private RuleSeverity severity;
    private BigDecimal threshold;
    private Integer timeWindow;
    private Boolean activeStatus;

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public RuleSeverity getSeverity() { return severity; }
    public void setSeverity(RuleSeverity severity) { this.severity = severity; }

    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }

    public Integer getTimeWindow() { return timeWindow; }
    public void setTimeWindow(Integer timeWindow) { this.timeWindow = timeWindow; }

    public Boolean getActiveStatus() { return activeStatus; }
    public void setActiveStatus(Boolean activeStatus) { this.activeStatus = activeStatus; }
}

