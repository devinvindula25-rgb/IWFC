package com.iwfc.exception;

/**
 * Custom exception thrown when a booking operation violates business rules.
 * Scenarios: double-booking (same time + location), booking outside operating hours (06:00-22:00).
 */
public class InvalidBookingException extends Exception {

    public InvalidBookingException(String message) {
        super(message);
    }

    public InvalidBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
