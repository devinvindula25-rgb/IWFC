package com.iwfc.exception;

/**
 * Custom exception thrown when attempting to add data that conflicts
 * with existing records.
 * Example: Registering equipment with an ID that already exists.
 */
public class DuplicateDataException extends Exception {

    public DuplicateDataException(String message) {
        super(message);
    }

    public DuplicateDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
