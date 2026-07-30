package com.neueda.transaction_monitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.transaction_monitor.dto.AlertDto.AlertResponse;
import com.neueda.transaction_monitor.dto.AlertDto.CreateAlertRequest;
import com.neueda.transaction_monitor.model.Alert;
import com.neueda.transaction_monitor.model.Alert.AlertSeverity;
import com.neueda.transaction_monitor.model.Alert.AlertStatus;
import com.neueda.transaction_monitor.repository.AlertRepository;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository);
    }

    @Test
    void shouldAcknowledgeOpenAlert() {
        Alert alert = buildAlert(AlertStatus.OPEN);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        AlertResponse response = alertService.acknowledge(1L);

        assertEquals(AlertStatus.ACKNOWLEDGED, response.status());
        verify(alertRepository).updateStatus(eq(1L), eq(AlertStatus.ACKNOWLEDGED), eq(null));
    }

    @Test
    void shouldSetClosedAtWhenClosing() {
        Alert alert = buildAlert(AlertStatus.INVESTIGATING);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        AlertResponse response = alertService.close(1L);

        assertEquals(AlertStatus.CLOSED, response.status());
        assertNotNull(response.closedAt());

        ArgumentCaptor<LocalDateTime> closedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(alertRepository).updateStatus(eq(1L), eq(AlertStatus.CLOSED), closedAtCaptor.capture());
        assertNotNull(closedAtCaptor.getValue());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Alert alert = buildAlert(AlertStatus.OPEN);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, () -> alertService.close(1L));
    }

    @Test
    void shouldThrowWhenAlertMissing() {
        when(alertRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> alertService.acknowledge(99L));
    }

    private Alert buildAlert(AlertStatus status) {
        Alert alert = new Alert();
        alert.setAlertId(1L);
        alert.setRuleId(10L);
        alert.setTransactionId(20L);
        alert.setStatus(status);
        alert.setSeverity(AlertSeverity.HIGH);
        alert.setCreatedAt(LocalDateTime.now());
        return alert;
    }
}
