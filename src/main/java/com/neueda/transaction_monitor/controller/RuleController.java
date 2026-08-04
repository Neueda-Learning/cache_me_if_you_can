package com.neueda.transaction_monitor.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.transaction_monitor.common.ApiResponse;
import com.neueda.transaction_monitor.common.ApiResponse.ErrorResponse;
import com.neueda.transaction_monitor.dto.RuleDto.CreateRuleRequest;
import com.neueda.transaction_monitor.dto.RuleDto.RuleResponse;
import com.neueda.transaction_monitor.dto.RuleDto.UpdateRuleRequest;
import com.neueda.transaction_monitor.model.Rule.RuleType;
import com.neueda.transaction_monitor.service.RuleEngineService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rules")
@Validated
public class RuleController {

    private final RuleEngineService ruleEngineService;

    public RuleController(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<RuleResponse>> createRule(@Valid @RequestBody CreateRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Rule created", ruleEngineService.createRule(request)));
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RuleResponse>> getRuleById(@PathVariable Long ruleId) {
        return ResponseEntity.ok(ApiResponse.success("Rule fetched", ruleEngineService.getRuleById(ruleId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getRules(
        @RequestParam(required = false) RuleType ruleType,
        @RequestParam(required = false) Boolean activeStatus
    ) {
        return ResponseEntity.ok(ApiResponse.success("Rules fetched", ruleEngineService.getRules(ruleType, activeStatus)));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(
        @PathVariable Long ruleId, @Valid @RequestBody UpdateRuleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Rule updated", ruleEngineService.updateRule(ruleId, request)));
    }

    @PatchMapping("/{ruleId}/activate")
    public ResponseEntity<ApiResponse<RuleResponse>> activateRule(@PathVariable Long ruleId) {
        return ResponseEntity.ok(ApiResponse.success("Rule activated", ruleEngineService.activateRule(ruleId)));
    }

    @PatchMapping("/{ruleId}/deactivate")
    public ResponseEntity<ApiResponse<RuleResponse>> deactivateRule(@PathVariable Long ruleId) {
        return ResponseEntity.ok(ApiResponse.success("Rule deactivated", ruleEngineService.deactivateRule(ruleId)));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long ruleId) {
        ruleEngineService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted", null));
    }

    // ── Exception Handlers ────────────────────────────────────────────────────

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining(" | "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(ex.getMessage()));
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
