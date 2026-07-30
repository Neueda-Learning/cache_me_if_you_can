package com.neueda.transaction_monitor.controller;

import com.neueda.transaction_monitor.model.AccountSummary;
import com.neueda.transaction_monitor.model.Transaction;
import com.neueda.transaction_monitor.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for the transaction domain.
 * Deliberately thin — no business logic, no try/catch.
 * All errors are handled centrally by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * POST /api/transactions
     * Body: { "accountId": 1, "payeeId": 2, "amount": 500.00, "transactionType": "PAYMENT" }
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody Transaction transaction) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(transaction));
    }

    /**
     * GET /api/transactions/{id}
     * Returns HTTP 404 automatically if the ID does not exist.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable int id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    /**
     * GET /api/transactions/account/{accountId}
     * Returns an empty array if the account has no transactions — never 404.
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transaction>> getByAccount(@PathVariable int accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
    }

    /**
     * GET /api/transactions/range?from=2026-07-01T00:00:00&to=2026-07-30T23:59:59
     * ISO 8601 datetime format required for both params.
     */
    @GetMapping("/range")
    public ResponseEntity<List<Transaction>> getByTimeWindow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(transactionService.getTransactionsByTimeWindow(from, to));
    }

    /**
     * GET /api/transactions/account/{accountId}/summary
     * Calls the MySQL stored procedure GetAccountSummary via a CallableStatement.
     */
    @GetMapping("/account/{accountId}/summary")
    public ResponseEntity<AccountSummary> getAccountSummary(@PathVariable int accountId) {
        return ResponseEntity.ok(transactionService.getAccountSummary(accountId));
    }
}
