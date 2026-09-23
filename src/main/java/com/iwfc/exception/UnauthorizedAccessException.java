package com.iwfc.exception;

/**
 * Custom exception thrown when a user attempts to access functionality
 * that is restricted to a different role.
 * Example: A Member trying to access Administrator maintenance logs.
 */
public class UnauthorizedAccessException extends IWFCException {

    public UnauthorizedAccessException(String message) {
        super(message, "ERR_UNAUTHORIZED");
    }

    @Override
    public String getUserFriendlyMessage() {
        return "Access Denied: " + getMessage();
    }
}
