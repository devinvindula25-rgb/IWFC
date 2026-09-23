package com.iwfc.pattern.payment;

import com.iwfc.exception.PaymentFailedException;
import com.iwfc.service.MockPaymentGateway;

public class DigitalWalletPayment implements PaymentStrategy {

    private final MockPaymentGateway gateway = MockPaymentGateway.getInstance();

    @Override
    public PaymentResult pay(double amount, PaymentContext ctx) throws PaymentFailedException {
        if (ctx.getWalletId() == null || !ctx.getWalletId().matches("^[0-9]{10}$")) {
            throw new PaymentFailedException("Invalid Digital Wallet ID (must be a 10-digit phone number).");
        }

        double totalAmount = amount + calculateProcessingFee(amount);
        String txnRef = gateway.authorise(totalAmount);
        
        return new PaymentResult(true, txnRef);
    }

    @Override
    public String getMethodName() {
        return "Digital Wallet";
    }

    @Override
    public double calculateProcessingFee(double amount) {
        // 1.5% fee
        return amount * 0.015;
    }
}
