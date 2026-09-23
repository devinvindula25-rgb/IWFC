package com.iwfc.model;

/**
 * The purpose of the payment.
 */
public enum PaymentPurpose {
    MEMBERSHIP_FEE("Membership Fee"),
    SESSION_BOOKING("Session Booking"),
    PERSONAL_TRAINING("Personal Training"),
    DAY_PASS("Day Pass"),
    MERCHANDISE("Merchandise");

    private final String displayName;

    PaymentPurpose(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
