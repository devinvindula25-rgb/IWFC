package com.iwfc.service;

import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;

import java.util.List;

/**
 * Service class for user management and authentication.
 * Handles login, role-based authorization, and user retrieval.
 */
public class UserService {

    private final DataStore dataStore;

    public UserService() {
        this.dataStore = DataStore.getInstance();
    }

    /**
     * Authenticates a user by their user ID.
     * Returns the polymorphic User (Admin, Instructor, or Member).
     *
     * @param userId the user ID to look up
     * @return the authenticated User
     * @throws IllegalArgumentException if user not found
     */
    public User login(String userId) {
        return dataStore.getUsers().stream()
                .filter(u -> u.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "User not found with ID: " + userId));
    }

    /**
     * Checks whether a user has the required role for an operation.
     * Demonstrates role-based access control.
     *
     * @param user         the user to check
     * @param requiredRole the role required for the operation
     * @throws UnauthorizedAccessException if the user's role doesn't match
     */
    public void checkAuthorization(User user, Role requiredRole)
            throws UnauthorizedAccessException {
        if (user.getRole() != requiredRole) {
            throw new UnauthorizedAccessException(
                "Access denied: User '" + user.getName() + "' with role '" +
                user.getRole().getDisplayName() + "' is not authorized. " +
                "Required role: " + requiredRole.getDisplayName());
        }
    }

    /**
     * Checks whether a user has a specific permission.
     *
     * @param user       the user to check
     * @param permission the permission string to verify
     * @throws UnauthorizedAccessException if the user doesn't have the permission
     */
    public void checkPermission(User user, String permission)
            throws UnauthorizedAccessException {
        if (!user.getPermissions().contains(permission)) {
            throw new UnauthorizedAccessException(
                "Access denied: User '" + user.getName() + "' does not have " +
                "permission '" + permission + "'.");
        }
    }

    /**
     * Returns all users in the system.
     */
    public List<User> getAllUsers() {
        return dataStore.getAll(User.class);
    }

    /**
     * Returns all users of a specific role type.
     *
     * @param role the role to filter by
     */
    public List<User> getUsersByRole(Role role) {
        return dataStore.getUsers().stream()
                .filter(u -> u.getRole() == role)
                .toList();
    }

    /**
     * Adds a new user to the system.
     * @param user the user to add
     */
    public void addUser(User user) {
        dataStore.getUsers().add(user);
    }
}
