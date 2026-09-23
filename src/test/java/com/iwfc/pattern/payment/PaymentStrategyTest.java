package com.iwfc.pattern.payment;

import com.iwfc.exception.PaymentFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStrategyTest {

    @Test
    void testCardPaymentFeeCalculation() {
        PaymentStrategy strategy = new CardPayment();
        // 2.5% fee
        assertEquals(25.0, strategy.calculateProcessingFee(1000.0), 0.01);
    }

    @Test
    void testCashPaymentFeeCalculation() {
        PaymentStrategy strategy = new CashPayment();
        // 0% fee
        assertEquals(0.0, strategy.calculateProcessingFee(1000.0), 0.01);
    }

    @Test
    void testBankTransferFeeCalculation() {
        PaymentStrategy strategy = new BankTransferPayment();
        // Flat 50.0 LKR fee
        assertEquals(50.0, strategy.calculateProcessingFee(1000.0), 0.01);
    }

    @Test
    void testDigitalWalletFeeCalculation() {
        PaymentStrategy strategy = new DigitalWalletPayment();
        // 1.5% fee
        assertEquals(15.0, strategy.calculateProcessingFee(1000.0), 0.01);
    }

    @Test
    void testStrategySwapAtRuntime() throws PaymentFailedException {
        PaymentContext ctx = new PaymentContext();
        ctx.setCardNumber("1234567812345678");
        ctx.setExpiryDate("12/25");

        PaymentStrategy strategy = new CardPayment();
        PaymentResult res1 = strategy.pay(500.0, ctx);
        assertTrue(res1.isSuccess());
        assertTrue(res1.getTransactionReference().startsWith("TXN-"));

        // Swap strategy at runtime
        strategy = new CashPayment();
        PaymentResult res2 = strategy.pay(500.0, ctx);
        assertTrue(res2.isSuccess());
        assertTrue(res2.getTransactionReference().startsWith("CASH-"));
    }

    @Test
    void testInvalidCardFormatThrowsException() {
        PaymentContext ctx = new PaymentContext();
        ctx.setCardNumber("123"); // Invalid length
        ctx.setExpiryDate("12/25");

        PaymentStrategy strategy = new CardPayment();
        
        PaymentFailedException ex = assertThrows(PaymentFailedException.class, 
            () -> strategy.pay(500.0, ctx));
        assertEquals("Invalid card number provided.", ex.getErrorCode());
    }
    
    @Test
    void testInvalidDigitalWalletFormatThrowsException() {
        PaymentContext ctx = new PaymentContext();
        ctx.setWalletId("abc"); // Invalid format, needs 10 digits

        PaymentStrategy strategy = new DigitalWalletPayment();
        
        PaymentFailedException ex = assertThrows(PaymentFailedException.class, 
            () -> strategy.pay(500.0, ctx));
        assertEquals("Invalid Digital Wallet ID (must be a 10-digit phone number).", ex.getErrorCode());
    }
}
