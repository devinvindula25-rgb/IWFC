package com.iwfc.exception;

/**
 * Specific error case for card/wallet failures due to lack of funds.
 */
public class InsufficientFundsException extends PaymentFailedException {

    public InsufficientFundsException(String message) {
        super("PAY-002", message);
    }

    @Override
    public String getUserFriendlyMessage() {
        return "Transaction declined due to insufficient funds: " + getMessage();
    }
}
