package com.neueda.transaction_monitor.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.neueda.transaction_monitor.common.ApiResponse;
import com.neueda.transaction_monitor.common.ApiResponse.ErrorResponse;
import com.neueda.transaction_monitor.dto.AlertDto.AlertResponse;
import com.neueda.transaction_monitor.dto.AlertDto.CreateAlertRequest;
import com.neueda.transaction_monitor.dto.AlertDto.UpdateAlertStatusRequest;
import com.neueda.transaction_monitor.model.Alert.AlertSeverity;
import com.neueda.transaction_monitor.model.Alert.AlertStatus;
import com.neueda.transaction_monitor.service.AlertService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/alerts")
@Validated
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<AlertResponse>> createAlert(@Valid @RequestBody CreateAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Alert created", alertService.createAlert(request)));
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlertById(@PathVariable Long alertId) {
        return ResponseEntity.ok(ApiResponse.success("Alert fetched", alertService.getAlertById(alertId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlerts(
        @RequestParam(required = false) AlertStatus status,
        @RequestParam(required = false) AlertSeverity severity
    ) {
        return ResponseEntity.ok(ApiResponse.success("Alerts fetched", alertService.getAlerts(status, severity)));
    }

    @PatchMapping("/{alertId}/status")
    public ResponseEntity<ApiResponse<AlertResponse>> updateStatus(
        @PathVariable Long alertId, @Valid @RequestBody UpdateAlertStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Alert status updated",
            alertService.updateStatus(alertId, request.status())));
    }

    @PatchMapping("/{alertId}/acknowledge")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledge(@PathVariable Long alertId) {
        return ResponseEntity.ok(ApiResponse.success("Alert acknowledged", alertService.acknowledge(alertId)));
    }

    @PatchMapping("/{alertId}/investigating")
    public ResponseEntity<ApiResponse<AlertResponse>> markInvestigating(@PathVariable Long alertId) {
        return ResponseEntity.ok(ApiResponse.success("Alert under investigation", alertService.investigate(alertId)));
    }

    @PatchMapping("/{alertId}/close")
    public ResponseEntity<ApiResponse<AlertResponse>> close(@PathVariable Long alertId) {
        return ResponseEntity.ok(ApiResponse.success("Alert closed", alertService.close(alertId)));
    }

    @PatchMapping("/{alertId}/dismiss")
    public ResponseEntity<ApiResponse<AlertResponse>> dismiss(@PathVariable Long alertId) {
        return ResponseEntity.ok(ApiResponse.success("Alert dismissed", alertService.dismiss(alertId)));
    }

    // ── Exception Handlers ────────────────────────────────────────────────────

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseError(DataAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("Database error: " + ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of("Unexpected error: " + ex.getMessage()));
    }
}
