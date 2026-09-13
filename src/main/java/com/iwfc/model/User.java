package com.iwfc.model;

import java.util.List;

/**
 * Abstract base class for all users in the IWFC system.
 * Demonstrates Abstraction — subclasses must provide role-specific behavior.
 * Demonstrates Encapsulation — all fields are private with controlled access.
 */
public abstract class User {
    private String userId;
    private String name;
    private String email;
    private Role role;

    /**
     * Protected constructor — only subclasses can instantiate.
     */
    protected User(String userId, String name, String email, Role role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    /**
     * Returns a human-readable description of this user's role.
     * Demonstrates Polymorphism — each subclass provides its own implementation.
     */
    public abstract String getRoleDescription();

    /**
     * Returns the list of permissions granted to this user's role.
     * Demonstrates Polymorphism — permissions vary by role.
     */
    public abstract List<String> getPermissions();

    // --- Encapsulated accessors ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s (%s)", name, userId, role.getDisplayName(), email);
    }
}
