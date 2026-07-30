package com.neueda.transaction_monitor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response model for the account summary reporting endpoint.
 * Populated exclusively via the GetAccountSummary stored procedure (CallableStatement).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummary {

    private int accountId;
    private int totalTransactions;
    private BigDecimal totalAmount;
}

