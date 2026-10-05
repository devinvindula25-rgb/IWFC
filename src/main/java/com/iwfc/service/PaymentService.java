package com.iwfc.service;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.PaymentFailedException;
import com.iwfc.model.Payment;
import com.iwfc.model.PaymentMethod;
import com.iwfc.model.PaymentPurpose;
import com.iwfc.model.PaymentStatus;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;
import com.iwfc.pattern.payment.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for processing payments.
 * Manages the Payment Strategy selection and records transactions.
 */
public class PaymentService {

    private final DataStore dataStore;
    private final NotificationService notificationService;
    private final Map<PaymentMethod, PaymentStrategy> strategies;

    public PaymentService(NotificationService notificationService) {
        this.dataStore = DataStore.getInstance();
        this.notificationService = notificationService;
        
        // Initialize strategies
        this.strategies = new HashMap<>();
        this.strategies.put(PaymentMethod.CARD, new CardPayment());
        this.strategies.put(PaymentMethod.CASH, new CashPayment());
        this.strategies.put(PaymentMethod.BANK_TRANSFER, new BankTransferPayment());
        this.strategies.put(PaymentMethod.DIGITAL_WALLET, new DigitalWalletPayment());
    }

    /**
     * Processes a payment using the strategy corresponding to the selected method.
     */
    public Payment processPayment(String memberId, double amount, PaymentMethod method, 
                                  PaymentPurpose purpose, String description, PaymentContext ctx) 
            throws PaymentFailedException {
        
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }

        double fee = strategy.calculateProcessingFee(amount);
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Payment payment = new Payment(paymentId, memberId, amount, fee, method, purpose, description);

        try {
            // Save initial PENDING payment record
            dataStore.getPayments().save(payment);
            
            // Execute payment strategy
            PaymentResult result = strategy.pay(amount, ctx);
            
            if (result.isSuccess()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionReference(result.getTransactionReference());
                
                notificationService.notifyListeners("PAYMENT_COMPLETED", 
                    String.format("✅ Payment successful: LKR %.2f for %s via %s", 
                        payment.getTotalCharged(), purpose.getDisplayName(), strategy.getMethodName()));
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                throw new PaymentFailedException("Transaction was not successful.");
            }
            
        } catch (PaymentFailedException e) {
            payment.setStatus(PaymentStatus.FAILED);
            notificationService.notifyListeners("PAYMENT_FAILED", 
                String.format("❌ Payment failed for %s: %s", purpose.getDisplayName(), e.getMessage()));
            throw e;
        } catch (DuplicateDataException e) {
            throw new RuntimeException("System error: Duplicate payment ID generated.", e);
        }

        return payment;
    }

    /**
     * Retrieves all payments for a specific member.
     */
    public List<Payment> getPaymentsForMember(String memberId) {
        return dataStore.getPayments().findAll().stream()
                .filter(p -> p.getMemberId().equals(memberId))
                .sorted((a, b) -> b.getPaidAt().compareTo(a.getPaidAt()))
                .collect(Collectors.toList());
    }
    
    /**
     * Calculates the total amount of all COMPLETED payments for a member.
     */
    public double getTotalPaymentsForMember(String memberId) {
        return dataStore.getPayments().findAll().stream()
                .filter(p -> p.getMemberId().equals(memberId) && p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(Payment::getTotalCharged)
                .sum();
    }

    /**
     * Retrieves all payments in the system.
     */
    public List<Payment> getAllPayments() {
        return dataStore.getPayments().findAll().stream()
                .sorted((a, b) -> b.getPaidAt().compareTo(a.getPaidAt()))
                .collect(Collectors.toList());
    }

    /**
     * Refunds a payment and updates the repository.
     */
    public void refundPayment(String paymentId, String reason) {
        Payment payment = dataStore.getPayments().findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));
        payment.setStatus(PaymentStatus.REFUNDED);
        dataStore.getPayments().update(payment);
        notificationService.notifyListeners("PAYMENT_REFUNDED",
                String.format("Payment %s refunded (LKR %.2f). Reason: %s", paymentId, payment.getTotalCharged(), reason));
    }
}
