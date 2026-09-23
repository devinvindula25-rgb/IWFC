package com.iwfc.pattern.payment;

import com.iwfc.exception.PaymentFailedException;

public class BankTransferPayment implements PaymentStrategy {

    @Override
    public PaymentResult pay(double amount, PaymentContext ctx) throws PaymentFailedException {
        if (ctx.getReferenceNumber() == null || ctx.getReferenceNumber().trim().isEmpty()) {
            throw new PaymentFailedException("Bank reference number is required for Bank Transfer payments.");
        }
        
        // Simulating the acceptance of a bank transfer reference
        return new PaymentResult(true, "BT-" + ctx.getReferenceNumber());
    }

    @Override
    public String getMethodName() {
        return "Bank Transfer";
    }

    @Override
    public double calculateProcessingFee(double amount) {
        // Flat 50 LKR fee
        return 50.0;
    }
}
