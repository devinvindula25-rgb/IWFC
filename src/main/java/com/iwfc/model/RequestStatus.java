package com.iwfc.model;

/**
 * Enum representing the status of a maintenance request through its workflow lifecycle.
 * Transitions: PENDING -> ASSIGNED -> COMPLETED
 */
public enum RequestStatus {
    PENDING("Pending"),
    ASSIGNED("Assigned"),
    COMPLETED("Completed");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
