package com.iwfc.pattern.payment;

import com.iwfc.exception.PaymentFailedException;
import com.iwfc.service.MockPaymentGateway;

public class CardPayment implements PaymentStrategy {

    private final MockPaymentGateway gateway = MockPaymentGateway.getInstance();

    @Override
    public PaymentResult pay(double amount, PaymentContext ctx) throws PaymentFailedException {
        if (ctx.getCardNumber() == null || ctx.getCardNumber().length() < 16) {
            throw new PaymentFailedException("Invalid card number provided.");
        }
        if (ctx.getExpiryDate() == null || !ctx.getExpiryDate().matches("\\d{2}/\\d{2}")) {
            throw new PaymentFailedException("Invalid expiry date format. Expected MM/YY.");
        }

        double totalAmount = amount + calculateProcessingFee(amount);
        String txnRef = gateway.authorise(totalAmount);
        
        return new PaymentResult(true, txnRef);
    }

    @Override
    public String getMethodName() {
        return "Credit/Debit Card";
    }

    @Override
    public double calculateProcessingFee(double amount) {
        // 2.5% fee
        return amount * 0.025;
    }
}
