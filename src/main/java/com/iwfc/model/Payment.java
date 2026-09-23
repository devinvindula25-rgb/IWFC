package com.iwfc.model;

import java.time.LocalDateTime;

/**
 * Represents a payment transaction in the system.
 */
public class Payment implements Identifiable {
    private String paymentId;
    private String memberId;
    private double amount;
    private double processingFee;
    private double totalCharged;
    private PaymentMethod method;
    private PaymentStatus status;
    private PaymentPurpose purpose;
    private LocalDateTime paidAt;
    private String transactionReference;
    private String description;

    public Payment(String paymentId, String memberId, double amount, double processingFee,
                   PaymentMethod method, PaymentPurpose purpose, String description) {
        this.paymentId = paymentId;
        this.memberId = memberId;
        this.amount = amount;
        this.processingFee = processingFee;
        this.totalCharged = amount + processingFee;
        this.method = method;
        this.purpose = purpose;
        this.description = description;
        this.status = PaymentStatus.PENDING;
        this.paidAt = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getMemberId() {
        return memberId;
    }

    public double getAmount() {
        return amount;
    }

    public double getProcessingFee() {
        return processingFee;
    }

    public double getTotalCharged() {
        return totalCharged;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentPurpose getPurpose() {
        return purpose;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
