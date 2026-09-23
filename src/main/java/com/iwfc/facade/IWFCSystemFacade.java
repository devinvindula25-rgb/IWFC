package com.iwfc.facade;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.InvalidBookingException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.*;
import com.iwfc.pattern.NotificationService;
import com.iwfc.service.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private final PaymentService paymentService;
    private final UserService userService;
    private final NotificationService notificationService;

    private User currentUser;

    public IWFCSystemFacade() {
        this.notificationService = new NotificationService();
        this.equipmentService = new EquipmentService(notificationService);
        this.sessionService = new SessionService(notificationService);
        this.maintenanceService = new MaintenanceService(notificationService);
        this.paymentService = new PaymentService(notificationService);
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
    public void editEquipment(String equipmentId, String name, String category,
                              EquipmentStatus status, Location location, double maintenanceThreshold)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_EQUIPMENT");
        equipmentService.editEquipment(equipmentId, name, category, status, location, maintenanceThreshold);
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
     * Removes equipment from the system. Requires ADMIN role.
     */
    public void removeEquipment(String equipmentId)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_EQUIPMENT");
        equipmentService.removeEquipment(equipmentId);
    }

    /**
     * Logs usage hours for equipment. Available to ADMIN and INSTRUCTOR.
     */
    public void logEquipmentUsage(String equipmentId, double hours)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "LOG_EQUIPMENT_USAGE");
        
        Equipment equipment = equipmentService.getEquipmentById(equipmentId);
        boolean wasBelowThreshold = !equipment.needsMaintenance();
        
        equipmentService.logUsage(equipmentId, hours, currentUser);
        
        if (wasBelowThreshold && equipment.needsMaintenance()) {
            // Auto-generate maintenance request
            String requestId = maintenanceService.generateRequestId();
            // Using SYSTEM as the reporter since it's auto-generated, but we need an instructor type.
            // For now, if current user is an instructor, use them, otherwise pass null (which might fail validation).
            Instructor reporter = (currentUser instanceof Instructor) ? (Instructor) currentUser : null;
            MaintenanceRequest request = new MaintenanceRequest(
                    requestId, equipment, reporter, 
                    "AUTO-GENERATED: Usage threshold exceeded (" + equipment.getCumulativeUsageHours() + " hrs)", 
                    Urgency.HIGH);
            maintenanceService.reportIssue(request);
        }
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
     * Returns equipment filtered by category.
     */
    public List<Equipment> getEquipmentByCategory(String category) {
        return equipmentService.getByCategory(category);
    }

    /**
     * Returns equipment summary stats.
     */
    public java.util.Map<EquipmentStatus, Long> getEquipmentSummary() {
        return equipmentService.getEquipmentSummary();
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
     * Edits a session. Available to ADMIN and INSTRUCTOR.
     */
    public void editSession(String sessionId, String name, java.time.LocalTime start, java.time.LocalTime end, int capacity)
            throws InvalidBookingException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "CREATE_SESSION");
        sessionService.editSession(sessionId, name, start, end, capacity);
    }

    /**
     * Books a member into a session for the next available occurrence. Available to MEMBER role.
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
     * Books a member into a session for a specific date. Available to MEMBER role.
     */
    public Booking bookSessionForDate(String sessionId, LocalDate date)
            throws InvalidBookingException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "BOOK_SESSION");
        if (!(currentUser instanceof Member member)) {
            throw new InvalidBookingException("Only members can book sessions.");
        }
        return sessionService.bookSessionForDate(member, sessionId, date);
    }

    /**
     * Cancels a member's booking. Available to MEMBER role.
     */
    public void cancelBooking(String bookingId)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "BOOK_SESSION");
        sessionService.cancelBooking(bookingId, currentUser.getUserId());
    }

    /**
     * Cancels a session and all its bookings. Available to ADMIN and INSTRUCTOR.
     */
    public void cancelSession(String sessionId)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "CREATE_SESSION");
        sessionService.cancelSession(sessionId);
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
     * Returns sessions for a specific date.
     */
    public List<FitnessSession> getSessionsForDate(LocalDate date) {
        return sessionService.getSessionsForDate(date);
    }
    
    /**
     * Returns a map of sessions for a specific month.
     */
    public java.util.Map<LocalDate, List<FitnessSession>> getSessionsForMonth(YearMonth month) {
        return sessionService.getSessionsForMonth(month);
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
     * Returns bookings for a specific session on a specific date.
     */
    public List<Booking> getBookingsForDate(String sessionId, LocalDate date) {
        return sessionService.getBookingsForDate(sessionId, date);
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
    
    /**
     * Returns bookings for the current logged-in member in a given month.
     */
    public List<Booking> getMyBookingsForMonth(YearMonth month) {
        if (currentUser != null) {
            return sessionService.getBookingsForMemberInMonth(currentUser.getUserId(), month);
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
    public void completeMaintenance(String requestId, String resolutionNotes)
            throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "UPDATE_MAINTENANCE_STATUS");
        maintenanceService.completeRequest(requestId, resolutionNotes);
    }

    public void reopenMaintenanceRequest(String requestId) throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "UPDATE_MAINTENANCE_STATUS");
        maintenanceService.reopenRequest(requestId);
    }

    public java.util.List<MaintenanceRequest> getRequestsForEquipment(String equipmentId) {
        return maintenanceService.getRequestsForEquipment(equipmentId);
    }

    public java.util.Map<String, Long> getMaintenanceStats() {
        return maintenanceService.getMaintenanceStats();
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

    /**
     * Returns HIGH urgency maintenance requests that are still PENDING.
     */
    public List<MaintenanceRequest> getHighUrgencyPendingRequests() {
        return maintenanceService.getHighUrgencyPendingRequests();
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

    /**
     * Returns all users of a specific role. Requires ADMIN role (MANAGE_USERS permission).
     */
    public List<User> getUsersByRole(com.iwfc.model.Role role) throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_USERS");
        return userService.getUsersByRole(role);
    }

    /**
     * Adds a new user to the system. Requires ADMIN role (MANAGE_USERS permission).
     */
    public void addUser(User user) throws UnauthorizedAccessException, DuplicateDataException {
        userService.checkPermission(currentUser, "MANAGE_USERS");
        userService.addUser(user);
    }

    // =========================================================================
    // Notification Service (for UI to subscribe)
    // =========================================================================

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public java.util.List<Notification> getNotifications() {
        if (currentUser == null) return java.util.List.of();
        return notificationService.getNotificationsForRole(currentUser.getRole());
    }

    public java.util.List<Notification> getNotificationsByCategory(com.iwfc.model.NotificationCategory category) {
        if (currentUser == null) return java.util.List.of();
        return notificationService.getNotificationsByCategory(category, currentUser.getRole());
    }

    public long getUnreadNotificationCount() {
        if (currentUser == null) return 0;
        return notificationService.getUnreadCount(currentUser.getRole());
    }

    public void markNotificationAsRead(String notificationId) {
        notificationService.markAsRead(notificationId);
    }

    public void markAllNotificationsAsRead() {
        if (currentUser != null) {
            notificationService.markAllAsRead(currentUser.getRole());
        }
    }

    public void clearAllNotifications() throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_USERS"); // Or some specific permission, but Admin has MANAGE_USERS
        notificationService.clearAll();
    }

    // =========================================================================
    // Payment Management
    // =========================================================================

    public com.iwfc.model.Payment processPayment(String memberId, double amount, 
                                                 com.iwfc.model.PaymentMethod method, 
                                                 com.iwfc.model.PaymentPurpose purpose, 
                                                 String description, 
                                                 com.iwfc.pattern.payment.PaymentContext ctx) 
            throws com.iwfc.exception.PaymentFailedException, UnauthorizedAccessException {
        // Members process their own payments, or Admins process them for members
        if (currentUser.getRole() != com.iwfc.model.Role.ADMIN && !currentUser.getUserId().equals(memberId)) {
            // Need a specific permission for this, or just throw
            throw new UnauthorizedAccessException("Cannot process payments for other members.");
        }
        return paymentService.processPayment(memberId, amount, method, purpose, description, ctx);
    }

    public java.util.List<com.iwfc.model.Payment> getPaymentsForMember(String memberId) 
            throws UnauthorizedAccessException {
        if (currentUser.getRole() != com.iwfc.model.Role.ADMIN && !currentUser.getUserId().equals(memberId)) {
            throw new UnauthorizedAccessException("Cannot view payments for other members.");
        }
        return paymentService.getPaymentsForMember(memberId);
    }
    
    public double getTotalPaymentsForMember(String memberId) {
        return paymentService.getTotalPaymentsForMember(memberId);
    }
}
