package com.iwfc.facade;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.InvalidBookingException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.*;
import com.iwfc.pattern.NotificationService;
import com.iwfc.service.*;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Facade Pattern — Unified API for the IWFC system.
 * Provides a simplified interface that hides the complexity of coordinating
 * multiple services (Equipment, Session, Maintenance, User).
 *
 * The UI layer interacts exclusively with this Facade, never directly
 * with individual services.
 */
public class IWFCSystemFacade {

    private final EquipmentService equipmentService;
    private final SessionService sessionService;
    private final MaintenanceService maintenanceService;
    private final UserService userService;
    private final NotificationService notificationService;

    private User currentUser;

    public IWFCSystemFacade() {
        this.notificationService = new NotificationService();
        this.equipmentService = new EquipmentService(notificationService);
        this.sessionService = new SessionService(notificationService);
        this.maintenanceService = new MaintenanceService(notificationService);
        this.userService = new UserService();
        this.currentUser = null;
    }

    // =========================================================================
    // Authentication
    // =========================================================================

    /**
     * Logs in a user by their user ID.
     *
     * @param userId the user ID
     * @return the authenticated User (polymorphic: Admin, Instructor, or Member)
     */
    public User login(String userId) {
        this.currentUser = userService.login(userId);
        return currentUser;
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Returns the currently logged-in user.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns whether a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // =========================================================================
    // Equipment Management
    // =========================================================================

    /**
     * Adds new equipment. Requires ADMIN role.
     */
    public void addEquipment(Equipment equipment)
            throws DuplicateDataException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_EQUIPMENT");
        equipmentService.addEquipment(equipment);
    }

    /**
     * Edits equipment attributes. Requires ADMIN role.
     */
    public void editEquipment(String equipmentId, String name,
                              EquipmentStatus status, Location location)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_EQUIPMENT");
        equipmentService.editEquipment(equipmentId, name, status, location);
    }

    /**
     * Deactivates equipment. Requires ADMIN role.
     */
    public void deactivateEquipment(String equipmentId)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_EQUIPMENT");
        equipmentService.deactivateEquipment(equipmentId);
    }

    /**
     * Logs usage hours for equipment. Available to ADMIN and INSTRUCTOR.
     */
    public void logEquipmentUsage(String equipmentId, double hours)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "LOG_EQUIPMENT_USAGE");
        equipmentService.logUsage(equipmentId, hours);
    }

    /**
     * Returns all equipment. Available to all roles.
     */
    public List<Equipment> getAllEquipment() {
        return equipmentService.getAllEquipment();
    }

    /**
     * Returns equipment filtered by status.
     */
    public List<Equipment> getEquipmentByStatus(EquipmentStatus status) {
        return equipmentService.getByStatus(status);
    }

    /**
     * Returns equipment filtered by location.
     */
    public List<Equipment> getEquipmentByLocation(Location location) {
        return equipmentService.getByLocation(location);
    }

    /**
     * Returns equipment by ID.
     */
    public Equipment getEquipmentById(String equipmentId) {
        return equipmentService.getEquipmentById(equipmentId);
    }

    // =========================================================================
    // Session Scheduling
    // =========================================================================

    /**
     * Creates a new session. Available to ADMIN and INSTRUCTOR.
     */
    public void createSession(FitnessSession session)
            throws InvalidBookingException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "CREATE_SESSION");
        sessionService.createSession(session);
    }

    /**
     * Books a member into a session. Available to MEMBER role.
     */
    public Booking bookSession(String sessionId)
            throws InvalidBookingException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "BOOK_SESSION");
        if (!(currentUser instanceof Member member)) {
            throw new InvalidBookingException("Only members can book sessions.");
        }
        return sessionService.bookSession(member, sessionId);
    }

    /**
     * Returns all sessions.
     */
    public List<FitnessSession> getAllSessions() {
        return sessionService.getAllSessions();
    }

    /**
     * Returns sessions by day of week.
     */
    public List<FitnessSession> getSessionsByDay(DayOfWeek day) {
        return sessionService.getSessionsByDay(day);
    }

    /**
     * Returns sessions with available capacity.
     */
    public List<FitnessSession> getAvailableSessions() {
        return sessionService.getAvailableSessions();
    }

    /**
     * Returns all bookings.
     */
    public List<Booking> getAllBookings() {
        return sessionService.getAllBookings();
    }

    /**
     * Returns bookings for a specific session.
     */
    public List<Booking> getBookingsForSession(String sessionId) {
        return sessionService.getBookingsForSession(sessionId);
    }

    /**
     * Returns bookings for the current logged-in member.
     */
    public List<Booking> getMyBookings() {
        if (currentUser != null) {
            return sessionService.getBookingsForMember(currentUser.getUserId());
        }
        return List.of();
    }

    // =========================================================================
    // Maintenance
    // =========================================================================

    /**
     * Reports a maintenance issue. Available to INSTRUCTOR role.
     */
    public void reportMaintenance(Equipment equipment, String description, Urgency urgency)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "REPORT_MAINTENANCE");
        if (!(currentUser instanceof Instructor instructor)) {
            throw new IllegalStateException("Only instructors can report issues.");
        }
        String requestId = maintenanceService.generateRequestId();
        MaintenanceRequest request = new MaintenanceRequest(
                requestId, equipment, instructor, description, urgency);
        maintenanceService.reportIssue(request);
    }

    /**
     * Assigns a maintenance request. Requires ADMIN role.
     */
    public void assignMaintenance(String requestId, String technicianName)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "ASSIGN_MAINTENANCE");
        maintenanceService.assignRequest(requestId, technicianName);
    }

    /**
     * Completes a maintenance request. Requires ADMIN role.
     */
    public void completeMaintenance(String requestId)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "UPDATE_MAINTENANCE_STATUS");
        maintenanceService.completeRequest(requestId);
    }

    /**
     * Returns all maintenance requests.
     */
    public List<MaintenanceRequest> getAllMaintenanceRequests() {
        return maintenanceService.getAllRequests();
    }

    /**
     * Returns maintenance requests filtered by status.
     */
    public List<MaintenanceRequest> getMaintenanceByStatus(RequestStatus status) {
        return maintenanceService.getRequestsByStatus(status);
    }

    // =========================================================================
    // Users
    // =========================================================================

    /**
     * Returns all users. Requires ADMIN role.
     */
    public List<User> getAllUsers() throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_USERS");
        return userService.getAllUsers();
    }

    // =========================================================================
    // Notification Service (for UI to subscribe)
    // =========================================================================

    /**
     * Returns the notification service for the UI to subscribe to events.
     */
    public NotificationService getNotificationService() {
        return notificationService;
    }
}
