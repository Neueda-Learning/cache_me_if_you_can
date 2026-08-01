package com.neueda.transaction_monitor.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.neueda.transaction_monitor.common.ApiResponse;
import com.neueda.transaction_monitor.entity.Account;
import com.neueda.transaction_monitor.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Account>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved", accountService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Account>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Account retrieved", accountService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> create(@RequestBody Account account) {
        Account created = accountService.create(account);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Account created", created));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> notFound(java.util.NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.ErrorResponse.of(ex.getMessage()));
    }
}

