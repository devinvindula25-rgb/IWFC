package com.iwfc.model;

/**
 * Enum representing the roles available in the IWFC system.
 * Used for authentication and authorization across the application.
 */
public enum Role {
    ADMIN("Administrator"),
    INSTRUCTOR("Instructor"),
    MEMBER("Member");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
