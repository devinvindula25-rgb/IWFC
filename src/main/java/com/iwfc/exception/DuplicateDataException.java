package com.iwfc.exception;

/**
 * Custom exception thrown when attempting to add data that conflicts
 * with existing records.
 * Example: Registering equipment with an ID that already exists.
 */
public class DuplicateDataException extends IWFCException {

    public DuplicateDataException(String message) {
        super(message, "ERR_DUPLICATE_DATA");
    }

    @Override
    public String getUserFriendlyMessage() {
        return "Duplicate Data Error: " + getMessage();
    }
}
