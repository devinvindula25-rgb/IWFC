package com.iwfc.pattern.payment;

import com.iwfc.exception.PaymentFailedException;

public class CashPayment implements PaymentStrategy {

    @Override
    public PaymentResult pay(double amount, PaymentContext ctx) throws PaymentFailedException {
        // Cash payment doesn't use the mock gateway because it's processed physically at the counter
        return new PaymentResult(true, "CASH-" + System.currentTimeMillis());
    }

    @Override
    public String getMethodName() {
        return "Cash";
    }

    @Override
    public double calculateProcessingFee(double amount) {
        // 0% fee
        return 0.0;
    }
}
