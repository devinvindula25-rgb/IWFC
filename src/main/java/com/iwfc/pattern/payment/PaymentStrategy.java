package com.iwfc.pattern.payment;

import com.iwfc.exception.PaymentFailedException;

/**
 * The Strategy interface for processing payments.
 */
public interface PaymentStrategy {
    
    /**
     * Executes the payment.
     *
     * @param amount the base amount to charge
     * @param ctx the context data for the specific payment method
     * @return PaymentResult containing success flag and reference
     * @throws PaymentFailedException if the payment fails (e.g. insufficient funds)
     */
    PaymentResult pay(double amount, PaymentContext ctx) throws PaymentFailedException;
    
    /**
     * Gets the display name of this payment method.
     */
    String getMethodName();
    
    /**
     * Calculates the processing fee for this method based on the base amount.
     */
    double calculateProcessingFee(double amount);
}
