package com.neueda.transaction_monitor.service;

import com.neueda.transaction_monitor.exception.TransactionNotFoundException;
import com.neueda.transaction_monitor.model.AccountSummary;
import com.neueda.transaction_monitor.model.Transaction;
import com.neueda.transaction_monitor.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TransactionService {

    private static final Set<String> VALID_TYPES = Set.of("TRANSFER", "PAYMENT", "WITHDRAWAL");

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Validates and persists a new transaction.
     * Throws IllegalArgumentException for bad input — caught by GlobalExceptionHandler.
     */
    public Transaction createTransaction(Transaction t) {
        if (t.getAccountId() == null || t.getAccountId() <= 0) {
            throw new IllegalArgumentException("Account ID must be positive");
        }
        if (t.getPayeeId() == null || t.getPayeeId() <= 0) {
            throw new IllegalArgumentException("Payee ID must be positive");
        }
        if (t.getAmount() == null || t.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (t.getTransactionType() == null || !VALID_TYPES.contains(t.getTransactionType().toUpperCase())) {
            throw new IllegalArgumentException("Transaction type must be one of: TRANSFER, PAYMENT, WITHDRAWAL");
        }
        t.setTransactionType(t.getTransactionType().toUpperCase());
        return transactionRepository.save(t);
    }

    /**
     * Fetches a single transaction or throws TransactionNotFoundException (→ HTTP 404).
     */
    public Transaction getTransactionById(int id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    /**
     * Returns all transactions for a given account — empty list is a valid response.
     */
    public List<Transaction> getTransactionsByAccount(int accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    /**
     * Returns transactions in a time range. Validates that 'from' precedes 'to'.
     */
    public List<Transaction> getTransactionsByTimeWindow(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' timestamp must be before 'to' timestamp");
        }
        return transactionRepository.findByTimeWindow(from, to);
    }

    /**
     * Returns a summary for an account via the stored procedure (CallableStatement path).
     */
    public AccountSummary getAccountSummary(int accountId) {
        return transactionRepository.getAccountTransactionSummary(accountId);
    }
}
