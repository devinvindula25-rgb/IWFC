package com.iwfc.service;

import com.iwfc.exception.InsufficientFundsException;
import com.iwfc.exception.PaymentFailedException;

import java.util.UUID;

/**
 * A simulated payment gateway to demonstrate processing without external dependencies.
 */
public class MockPaymentGateway {

    private static MockPaymentGateway instance;
    private boolean forceFailureMode = false;

    private MockPaymentGateway() {}

    public static MockPaymentGateway getInstance() {
        if (instance == null) {
            instance = new MockPaymentGateway();
        }
        return instance;
    }

    /**
     * Toggles the failure mode for testing PaymentFailedException flows.
     */
    public void setForceFailureMode(boolean forceFailureMode) {
        this.forceFailureMode = forceFailureMode;
    }

    /**
     * Authorises a transaction, simulating network latency.
     *
     * @param amount the amount to authorise
     * @return a transaction reference string
     * @throws PaymentFailedException if the transaction fails
     */
    public String authorise(double amount) throws PaymentFailedException {
        try {
            // Simulate network latency (200-800ms)
            long latency = 200 + (long)(Math.random() * 600);
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (forceFailureMode) {
            throw new InsufficientFundsException("Mock Gateway forced a failure due to testing mode.");
        }

        // Return a mock transaction reference
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
