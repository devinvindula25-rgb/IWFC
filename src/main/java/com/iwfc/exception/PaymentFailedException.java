package com.iwfc.exception;

/**
 * Base exception for payment-related failures.
 */
public class PaymentFailedException extends IWFCException {

    public PaymentFailedException(String message) {
        super("PAY-001", message);
    }
    
    public PaymentFailedException(String errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public String getUserFriendlyMessage() {
        return "Payment failed: " + getMessage() + " (Error Code: " + getErrorCode() + ")";
    }
}
