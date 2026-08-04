package com.neueda.transaction_monitor.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight DTO used by the UI to display transactions together with
 * the payer and payee account numbers (legend view).
 */
public class TransactionView {

    private Integer transactionId;
    private String accountNumber;
    private String payeeAccountNumber;
    private BigDecimal amount;
    private String transactionType;
    private LocalDateTime timeStamp;

    public TransactionView() {}

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getPayeeAccountNumber() { return payeeAccountNumber; }
    public void setPayeeAccountNumber(String payeeAccountNumber) { this.payeeAccountNumber = payeeAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public LocalDateTime getTimeStamp() { return timeStamp; }
    public void setTimeStamp(LocalDateTime timeStamp) { this.timeStamp = timeStamp; }
}

