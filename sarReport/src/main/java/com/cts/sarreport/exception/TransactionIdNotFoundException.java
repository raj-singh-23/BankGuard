package com.cts.sarreport.exception;

public class TransactionIdNotFoundException extends RuntimeException {
    public TransactionIdNotFoundException(Long transactionId) {
        super("Transaction ID " + transactionId + " could not be located.");
    }
}
