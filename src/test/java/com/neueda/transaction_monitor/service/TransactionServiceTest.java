package com.neueda.transaction_monitor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.transaction_monitor.exception.TransactionNotFoundException;
import com.neueda.transaction_monitor.model.Transaction;
import com.neueda.transaction_monitor.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository);
    }

    // ── createTransaction happy path ──────────────────────────────────────────

    @Test
    void shouldSaveValidTransaction() {
        Transaction input = build(1, 2, new BigDecimal("500.00"), "PAYMENT");
        Transaction saved = build(1, 2, new BigDecimal("500.00"), "PAYMENT");
        saved.setTransactionId(1);

        when(transactionRepository.save(any())).thenReturn(saved);

        Transaction result = transactionService.createTransaction(input);

        assertNotNull(result);
        assertEquals(1, result.getTransactionId());
        verify(transactionRepository).save(input);
    }

    @Test
    void shouldUppercaseTransactionTypeBeforeSave() {
        Transaction input = build(1, 2, new BigDecimal("100.00"), "payment");
        when(transactionRepository.save(any())).thenReturn(input);

        transactionService.createTransaction(input);

        assertEquals("PAYMENT", input.getTransactionType());
    }

    // ── createTransaction validation failures ─────────────────────────────────

    @Test
    void shouldThrowWhenAccountIdIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        build(0, 2, new BigDecimal("100.00"), "PAYMENT")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAccountIdIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        build(-5, 2, new BigDecimal("100.00"), "PAYMENT")));
    }

    @Test
    void shouldThrowWhenPayeeIdIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        build(1, 0, new BigDecimal("100.00"), "PAYMENT")));
    }

    @Test
    void shouldThrowWhenAmountIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        build(1, 2, BigDecimal.ZERO, "PAYMENT")));
    }

    @Test
    void shouldThrowWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        build(1, 2, new BigDecimal("-50.00"), "PAYMENT")));
    }

    @Test
    void shouldThrowWhenTransactionTypeIsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        build(1, 2, new BigDecimal("100.00"), "INVALID")));
    }

    @Test
    void shouldThrowWhenTransactionTypeIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        build(1, 2, new BigDecimal("100.00"), null)));
    }

    // ── getTransactionById ────────────────────────────────────────────────────

    @Test
    void shouldReturnTransactionWhenFound() {
        Transaction tx = build(1, 2, new BigDecimal("200.00"), "TRANSFER");
        tx.setTransactionId(5);
        when(transactionRepository.findById(5)).thenReturn(Optional.of(tx));

        Transaction result = transactionService.getTransactionById(5);

        assertEquals(5, result.getTransactionId());
    }

    @Test
    void shouldThrowTransactionNotFoundExceptionWhenMissing() {
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransactionById(999));
    }

    // ── getTransactionsByAccount ──────────────────────────────────────────────

    @Test
    void shouldReturnListForAccount() {
        when(transactionRepository.findByAccountId(1)).thenReturn(List.of(
                build(1, 2, new BigDecimal("100.00"), "PAYMENT"),
                build(1, 3, new BigDecimal("200.00"), "TRANSFER")
        ));

        List<Transaction> result = transactionService.getTransactionsByAccount(1);

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoTransactions() {
        when(transactionRepository.findByAccountId(99)).thenReturn(List.of());

        assertEquals(0, transactionService.getTransactionsByAccount(99).size());
    }

    // ── getTransactionsByTimeWindow ───────────────────────────────────────────

    @Test
    void shouldReturnTransactionsInTimeWindow() {
        LocalDateTime from = LocalDateTime.now().minusHours(2);
        LocalDateTime to   = LocalDateTime.now();
        when(transactionRepository.findByTimeWindow(from, to)).thenReturn(List.of());

        List<Transaction> result = transactionService.getTransactionsByTimeWindow(from, to);

        assertNotNull(result);
        verify(transactionRepository).findByTimeWindow(from, to);
    }

    @Test
    void shouldThrowWhenFromIsAfterTo() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to   = LocalDateTime.now().minusHours(1);

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.getTransactionsByTimeWindow(from, to));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Transaction build(int accountId, int payeeId, BigDecimal amount, String type) {
        Transaction t = new Transaction();
        t.setAccountId(accountId);
        t.setPayeeId(payeeId);
        t.setAmount(amount);
        t.setTransactionType(type);
        return t;
    }
}