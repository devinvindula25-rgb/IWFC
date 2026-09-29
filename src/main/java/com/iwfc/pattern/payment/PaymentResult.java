package com.iwfc.pattern.payment;

/**
 * Result of a payment attempt.
 */
public class PaymentResult {
    private final boolean success;
    private final String transactionReference;

    public PaymentResult(boolean success, String transactionReference) {
        this.success = success;
        this.transactionReference = transactionReference;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransactionReference() {
        return transactionReference;
    }
}
