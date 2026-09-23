package com.iwfc.exception;

/**
 * Custom exception thrown when a booking operation violates business rules.
 * Scenarios: double-booking (same time + location), booking outside operating hours (06:00-22:00).
 */
public class InvalidBookingException extends IWFCException {

    public InvalidBookingException(String message) {
        super(message, "ERR_INVALID_BOOKING");
    }

    @Override
    public String getUserFriendlyMessage() {
        return "Booking Failed: " + getMessage();
    }
}
