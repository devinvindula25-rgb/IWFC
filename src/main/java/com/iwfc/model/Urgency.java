package com.iwfc.model;

/**
 * Enum representing the urgency level of a maintenance request.
 */
public enum Urgency {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String displayName;

    Urgency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
