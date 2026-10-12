package com.iwfc.model;

import java.util.List;

/**
 * Abstract base class for all users in the IWFC system.
 * Demonstrates Abstraction — subclasses must provide role-specific behavior.
 * Demonstrates Encapsulation — all fields are private with controlled access.
 */
public abstract class User implements Identifiable {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String profileImageUrl;
    private Role role;
    private String password = "0000";

    /**
     * Protected constructor — only subclasses can instantiate.
     */
    protected User(String userId, String name, String email, String phone, String address, String profileImageUrl, Role role) {
        this(userId, name, email, phone, address, profileImageUrl, role, "0000");
    }

    /**
     * Protected constructor with custom password.
     */
    protected User(String userId, String name, String email, String phone, String address, String profileImageUrl, Role role, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.password = (password != null && !password.trim().isEmpty()) ? password.trim() : "0000";
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

    @Override
    public String getId() {
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getPassword() {
        return password != null ? password : "0000";
    }

    public void setPassword(String password) {
        this.password = (password != null && !password.trim().isEmpty()) ? password.trim() : "0000";
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s (%s)", name, userId, role.getDisplayName(), email);
    }
}
