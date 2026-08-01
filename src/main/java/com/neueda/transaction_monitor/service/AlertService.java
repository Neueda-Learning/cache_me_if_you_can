package com.neueda.transaction_monitor.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.neueda.transaction_monitor.dto.AlertDto.AlertResponse;
import com.neueda.transaction_monitor.dto.AlertDto.CreateAlertRequest;
import com.neueda.transaction_monitor.model.Alert;
import com.neueda.transaction_monitor.model.Alert.AlertSeverity;
import com.neueda.transaction_monitor.model.Alert.AlertStatus;
import com.neueda.transaction_monitor.repository.AlertRepository;
import com.neueda.transaction_monitor.repository.RuleRepository;
@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private RuleRepository ruleRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    // Autowired constructor to also receive RuleRepository for grouping summaries
    @org.springframework.beans.factory.annotation.Autowired
    public AlertService(AlertRepository alertRepository,
                        com.neueda.transaction_monitor.repository.RuleRepository ruleRepository) {
        this.alertRepository = alertRepository;
        this.ruleRepository = ruleRepository;
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

    public List<com.neueda.transaction_monitor.dto.AlertDto.GroupedAlertResponse> getGroupedAlerts(Integer minutes, AlertSeverity severity) {
        int mins = (minutes == null || minutes <= 0) ? 60 : minutes;
        var groups = alertRepository.findGroupedAlerts(mins, severity);

        return groups.stream().map(g -> {
            String ruleName = null;
            if (ruleRepository != null) {
                var r = ruleRepository.findById(g.ruleId()).orElse(null);
                ruleName = r == null ? null : r.getRuleName();
            }
            OffsetDateTime firstCreated = g.firstCreated() == null ? null : g.firstCreated().atOffset(ZoneOffset.UTC);
            String message = String.format("%d %s alerts for rule %s in the last %d minutes",
                g.count(), g.severity().name().toLowerCase(), ruleName == null ? g.ruleId().toString() : ruleName, mins);

            return new com.neueda.transaction_monitor.dto.AlertDto.GroupedAlertResponse(
                g.ruleId(), ruleName, g.severity(), g.count(), firstCreated, message
            );
        }).collect(Collectors.toList());
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
        OffsetDateTime created = alert.getCreatedAt() == null ? null : alert.getCreatedAt().atOffset(ZoneOffset.UTC);
        OffsetDateTime closed  = alert.getClosedAt()  == null ? null : alert.getClosedAt().atOffset(ZoneOffset.UTC);
        return new AlertResponse(
            alert.getAlertId(), alert.getRuleId(), alert.getTransactionId(),
            alert.getStatus(), alert.getSeverity(), created, closed
        );
    }
}
