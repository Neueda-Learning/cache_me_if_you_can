package com.neueda.transaction_monitor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.neueda.transaction_monitor.common.ApiResponse;
import com.neueda.transaction_monitor.service.AuthService;
import com.neueda.transaction_monitor.service.AuthService.ChangePasswordRequest;
import com.neueda.transaction_monitor.service.AuthService.LoginRequest;
import com.neueda.transaction_monitor.service.AuthService.LoginResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/login — open to all, returns JWT + role */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success("Login successful", authService.login(request)));
    }

    /** PUT /api/auth/change-password — requires valid JWT (any role) */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    // ── Exception handler for auth errors ────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.ErrorResponse.of(ex.getMessage()));
    }
}

