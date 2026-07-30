package com.neueda.transaction_monitor.exception;

/**
 * Thrown by TransactionService when a transaction ID does not exist in the database.
 * Caught by GlobalExceptionHandler and returned as HTTP 404.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(int id) {
        super("Transaction not found with ID: " + id);
    }
}

