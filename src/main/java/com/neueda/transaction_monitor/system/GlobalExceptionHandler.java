package com.neueda.transaction_monitor.system;

import com.neueda.transaction_monitor.exception.TransactionNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared, app-wide exception handler.
 * Every controller in the project (transactions, alerts, rules) benefits from this
 * automatically — no try/catch blocks needed anywhere else.
 *
 * Every error response has the same JSON shape:
 * {
 *   "timestamp": "...",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Transaction not found with ID: 99"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Domain Exceptions ──────────────────────────────────────────────────────

    /** 404 — transaction ID does not exist in DB */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(TransactionNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── Validation & Input Exceptions ─────────────────────────────────────────

    /** 400 — service-level guard clause failed (e.g. negative amount, bad type) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 400 — @Valid on @RequestBody failed; collects ALL field errors into one message */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(" | "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /** 400 — malformed JSON or incompatible JSON value types in request body */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableJson(HttpMessageNotReadableException ex) {
        String cause = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return build(HttpStatus.BAD_REQUEST, "Invalid request body: " + cause);
    }

    // ── Data Access Exceptions ─────────────────────────────────────────────────

    /** 500 — any JDBC / SQL error from JdbcTemplate or stored procedures */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        String cause = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Database error: " + cause);
    }

    // ── Catch-All ─────────────────────────────────────────────────────────────

    /** 500 — anything that wasn't caught above; hides internal detail from the client */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ── Shared Builder ────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

