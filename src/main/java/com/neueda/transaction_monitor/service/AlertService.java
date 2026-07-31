package com.neueda.transaction_monitor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.neueda.transaction_monitor.dto.AlertDto.AlertResponse;
import com.neueda.transaction_monitor.dto.AlertDto.CreateAlertRequest;
import com.neueda.transaction_monitor.model.Alert;
import com.neueda.transaction_monitor.model.Alert.AlertSeverity;
import com.neueda.transaction_monitor.model.Alert.AlertStatus;
import com.neueda.transaction_monitor.repository.AlertRepository;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public AlertResponse createAlert(CreateAlertRequest request) {
        Alert alert = alertRepository.create(request.ruleId(), request.transactionId(), request.severity());
        return toResponse(alert);
    }

    public AlertResponse getAlertById(Long alertId) {
        return toResponse(getOrThrow(alertId));
    }

    public List<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity) {
        return alertRepository.findAll(status, severity).stream().map(this::toResponse).toList();
    }

    public AlertResponse updateStatus(Long alertId, AlertStatus nextStatus) {
        Alert current = getOrThrow(alertId);
        validateTransition(current.getStatus(), nextStatus);
        LocalDateTime closedAt = isTerminal(nextStatus) ? LocalDateTime.now() : null;
        alertRepository.updateStatus(alertId, nextStatus, closedAt);
        current.setStatus(nextStatus);
        current.setClosedAt(closedAt);
        return toResponse(current);
    }

    public AlertResponse acknowledge(Long alertId) { return updateStatus(alertId, AlertStatus.ACKNOWLEDGED); }

    public AlertResponse investigate(Long alertId) { return updateStatus(alertId, AlertStatus.INVESTIGATING); }

    public AlertResponse close(Long alertId) { return updateStatus(alertId, AlertStatus.CLOSED); }

    public AlertResponse dismiss(Long alertId) { return updateStatus(alertId, AlertStatus.DISMISSED); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Alert getOrThrow(Long alertId) {
        return alertRepository.findById(alertId)
            .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));
    }

    private void validateTransition(AlertStatus current, AlertStatus next) {
        if (current == next) throw new IllegalStateException("Alert already in status: " + next);

        boolean valid = switch (current) {
            case OPEN          -> next == AlertStatus.ACKNOWLEDGED || next == AlertStatus.DISMISSED;
            case ACKNOWLEDGED  -> next == AlertStatus.INVESTIGATING || next == AlertStatus.DISMISSED;
            case INVESTIGATING -> next == AlertStatus.CLOSED || next == AlertStatus.DISMISSED;
            case CLOSED, DISMISSED -> false;
        };

        if (!valid) throw new IllegalStateException("Invalid status transition: " + current + " -> " + next);
    }

    private boolean isTerminal(AlertStatus status) {
        return status == AlertStatus.CLOSED || status == AlertStatus.DISMISSED;
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
            alert.getAlertId(), alert.getRuleId(), alert.getTransactionId(),
            alert.getStatus(), alert.getSeverity(), alert.getCreatedAt(), alert.getClosedAt()
        );
    }
}
