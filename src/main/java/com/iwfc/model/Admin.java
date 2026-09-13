package com.iwfc.model;

import java.util.List;

/**
 * Administrator user with full system access.
 * Demonstrates Inheritance (extends User) and Polymorphism (overrides abstract methods).
 */
public class Admin extends User {

    public Admin(String userId, String name, String email) {
        super(userId, name, email, Role.ADMIN);
    }

    @Override
    public String getRoleDescription() {
        return "System Administrator — manages equipment inventory, oversees user accounts, " +
               "and monitors the global maintenance log.";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(
            "MANAGE_EQUIPMENT",
            "MANAGE_USERS",
            "VIEW_MAINTENANCE_LOG",
            "ASSIGN_MAINTENANCE",
            "UPDATE_MAINTENANCE_STATUS",
            "VIEW_ALL_BOOKINGS",
            "MANAGE_SESSIONS"
        );
    }
}
