package com.neueda.transaction_monitor.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    /** Set by the database after INSERT — not required in request body. */
    private Integer transactionId;

    @NotNull(message = "Account ID is required")
    @Positive(message = "Account ID must be positive")
    private Integer accountId;

    @NotNull(message = "Payee ID is required")
    @Positive(message = "Payee ID must be positive")
    private Integer payeeId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Transaction type is required")
    private String transactionType; // TRANSFER | PAYMENT | WITHDRAWAL

    /** Auto-set by MySQL default — omit from POST body. */
    private LocalDateTime timeStamp;
}
