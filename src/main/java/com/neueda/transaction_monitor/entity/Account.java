package com.neueda.transaction_monitor.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private Integer accountId;
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private String email;
    private LocalDateTime createdAt;

    public Account() {}

    public Account(Integer accountId, String accountNumber, String accountHolderName, BigDecimal balance, String email, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

}
