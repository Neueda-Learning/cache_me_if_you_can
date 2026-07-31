package com.neueda.transaction_monitor.dto;

import com.neueda.transaction_monitor.model.Alert.AlertSeverity;
import com.neueda.transaction_monitor.model.Alert.AlertStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

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
        LocalDateTime createdAt,
        LocalDateTime closedAt
    ) {}
}

