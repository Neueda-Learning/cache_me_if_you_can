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

    // Explicit getters/setters included to avoid IDE/compiler issues when Lombok
    // annotation processing is not available in the environment.
    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public Integer getPayeeId() { return payeeId; }
    public void setPayeeId(Integer payeeId) { this.payeeId = payeeId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public LocalDateTime getTimeStamp() { return timeStamp; }
    public void setTimeStamp(LocalDateTime timeStamp) { this.timeStamp = timeStamp; }
}
