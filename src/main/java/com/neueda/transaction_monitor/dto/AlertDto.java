package com.neueda.transaction_monitor.dto;

import com.neueda.transaction_monitor.model.Alert.AlertSeverity;
import com.neueda.transaction_monitor.model.Alert.AlertStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class AlertDto {

    public record CreateAlertRequest(
        @NotNull Long ruleId,
        @NotNull Long transactionId,
        @NotNull AlertSeverity severity
    ) {}

    public record UpdateAlertStatusRequest(@NotNull AlertStatus status) {}

    public record AlertResponse(
        Long alertId,
        Long ruleId,
        Long transactionId,
        AlertStatus status,
        AlertSeverity severity,
        OffsetDateTime createdAt,
        OffsetDateTime closedAt
    ) {}

    public record GroupedAlertResponse(
        Long ruleId,
        String ruleName,
        AlertSeverity severity,
        Integer count,
        OffsetDateTime firstCreated,
        String message
    ) {}
}

