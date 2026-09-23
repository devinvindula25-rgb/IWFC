package com.iwfc.exception;

import java.time.LocalDateTime;

/**
 * Abstract base class for all custom IWFC exceptions.
 */
public abstract class IWFCException extends Exception {
    
    private final String errorCode;
    private final LocalDateTime timestamp;

    public IWFCException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public abstract String getUserFriendlyMessage();
}
