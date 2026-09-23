package com.iwfc.model;

/**
 * Supported payment methods in the system.
 */
public enum PaymentMethod {
    CARD("Credit/Debit Card"),
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer"),
    DIGITAL_WALLET("Digital Wallet");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
