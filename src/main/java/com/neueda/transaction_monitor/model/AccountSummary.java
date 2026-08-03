package com.neueda.transaction_monitor.model;

import java.math.BigDecimal;

/**
 * Response model for the account summary reporting endpoint.
 * Populated exclusively via the GetAccountSummary stored procedure (CallableStatement).
 */
public class AccountSummary {

    private int accountId;
    private int totalTransactions;
    private BigDecimal totalAmount;

    // No-arg constructor
    public AccountSummary() {
    }

    // All-args constructor
    public AccountSummary(int accountId, int totalTransactions, BigDecimal totalAmount) {
        this.accountId = accountId;
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}

