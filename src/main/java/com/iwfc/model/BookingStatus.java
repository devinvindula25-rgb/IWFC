package com.iwfc.model;

/**
 * Represents the status of a booking.
 */
public enum BookingStatus {
    CONFIRMED("Confirmed"),
    ATTENDED("Attended"),
    CANCELLED("Cancelled");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
