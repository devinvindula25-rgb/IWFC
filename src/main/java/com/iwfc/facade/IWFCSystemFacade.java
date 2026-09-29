package com.iwfc.facade;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.InvalidBookingException;
import com.iwfc.exception.UnauthorizedAccessException;
import com.iwfc.model.*;
import com.iwfc.pattern.NotificationService;
import com.iwfc.pattern.plan.PlanCatalogueFactory;
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
    private final EnrollmentService enrollmentService;
    private final ScheduleMaker scheduleMaker;
    private final MeasurementAnalyzer measurementAnalyzer;
    private final List<FitnessPlan> planCatalogue;

    private User currentUser;

    public IWFCSystemFacade() {
        this.notificationService = new NotificationService();
        this.equipmentService = new EquipmentService(notificationService);
        this.sessionService = new SessionService(notificationService);
        this.maintenanceService = new MaintenanceService(notificationService);
        this.paymentService = new PaymentService(notificationService);
        this.userService = new UserService();
        this.enrollmentService = new EnrollmentService();
        this.planCatalogue = PlanCatalogueFactory.createInitialCatalogue();
        this.scheduleMaker = new ScheduleMaker(sessionService);
        this.measurementAnalyzer = new MeasurementAnalyzer();
        
        this.currentUser = com.iwfc.pattern.DataStore.getInstance().getUsers().findById("ADM001").orElse(null);
        if (this.currentUser != null) {
            try {
                List<User> members = com.iwfc.pattern.DataStore.getInstance().getUsers().findAll().stream()
                    .filter(u -> u.getRole() == com.iwfc.model.Role.MEMBER).collect(java.util.stream.Collectors.toList());
                List<User> instructors = com.iwfc.pattern.DataStore.getInstance().getUsers().findAll().stream()
                    .filter(u -> u.getRole() == com.iwfc.model.Role.INSTRUCTOR).collect(java.util.stream.Collectors.toList());
                
                for (int i = 0; i < members.size(); i++) {
                    Member m = (Member) members.get(i);
                    com.iwfc.model.FitnessPlan plan1 = planCatalogue.get(i % planCatalogue.size());
                    String instrId1 = instructors.get(i % instructors.size()).getUserId();
                    com.iwfc.pattern.payment.PaymentContext cardCtx = new com.iwfc.pattern.payment.PaymentContext();
                    cardCtx.setCardNumber("1234567812345678");
                    cardCtx.setExpiryDate("12/25");
                    assignWorkoutPlanToMember(m.getUserId(), plan1.getId(), instrId1, com.iwfc.model.PaymentMethod.CARD, cardCtx);
                    
                    if (i % 3 == 0) {
                        com.iwfc.model.FitnessPlan plan2 = planCatalogue.get((i + 5) % planCatalogue.size());
                        String instrId2 = instructors.get((i + 1) % instructors.size()).getUserId();
                        com.iwfc.pattern.payment.PaymentContext walletCtx = new com.iwfc.pattern.payment.PaymentContext();
                        walletCtx.setWalletId("5551234567");
                        assignWorkoutPlanToMember(m.getUserId(), plan2.getId(), instrId2, com.iwfc.model.PaymentMethod.DIGITAL_WALLET, walletCtx);
                    }
                    
                    measurementAnalyzer.addLog(new com.iwfc.model.BodyMeasurementLog("BML_AUTO_1_" + i, m.getUserId(), java.time.LocalDate.now().minusDays(30), 80.0 - (i % 5), 175.0, 95.0, 30.0, 50.0, 35.0));
                    measurementAnalyzer.addLog(new com.iwfc.model.BodyMeasurementLog("BML_AUTO_2_" + i, m.getUserId(), java.time.LocalDate.now(), 78.0 - (i % 5), 175.0, 93.0, 29.0, 49.0, 34.5));
                    
                    int hours = sessionService.getBookingsForMember(m.getUserId()).size();
                    m.setHoursSpent(hours);
                    com.iwfc.pattern.DataStore.getInstance().getUsers().update(m);
                }
            } catch(Exception e) {
                System.err.println("Facade initialization error: " + e.getMessage());
            }
        }
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
        if (currentUser.getRole() == com.iwfc.model.Role.ADMIN || currentUser.getUserId().equals(memberId)) {
            return paymentService.getPaymentsForMember(memberId);
        } else if (currentUser instanceof Instructor instructor) {
            if (instructor.getTraineeIds().contains(memberId)) {
                return paymentService.getPaymentsForMember(memberId);
            }
        }
        throw new UnauthorizedAccessException("Cannot view payments for other members.");
    }
    
    public double getTotalPaymentsForMember(String memberId) {
        return paymentService.getTotalPaymentsForMember(memberId);
    }

    public java.util.List<com.iwfc.model.Payment> getAllPayments() throws UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_USERS");
        return paymentService.getAllPayments();
    }

    // =========================================================================
    // Enrollment & Fitness Plans
    // =========================================================================

    public List<FitnessPlan> getPlanCatalogue() {
        return planCatalogue;
    }

    public FitnessPlan getPlanById(String planId) {
        return planCatalogue.stream().filter(p -> p.getId().equals(planId)).findFirst().orElse(null);
    }

    public Enrollment enrollMember(String memberId, String planId, String instructorId, int durationWeeks) throws DuplicateDataException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_USERS"); // Or self-service check
        return enrollmentService.enrollMember(memberId, planId, instructorId, durationWeeks);
    }

    /**
     * Complete flow: Enroll a member, process payment, and generate their schedule.
     */
    public final Enrollment assignWorkoutPlanToMember(String memberId, String planId, String instructorId, PaymentMethod paymentMethod, com.iwfc.pattern.payment.PaymentContext paymentCtx) throws Exception {
        if (currentUser.getRole() == Role.MEMBER && !currentUser.getUserId().equals(memberId)) {
            throw new UnauthorizedAccessException("Cannot assign plans to other members.");
        }
        if (currentUser.getRole() == Role.INSTRUCTOR) {
            Instructor instr = (Instructor) currentUser;
            if (!instr.getTraineeIds().contains(memberId) && instructorId != null && !instructorId.equals(instr.getUserId())) {
                throw new UnauthorizedAccessException("Instructors can only assign plans to their own trainees.");
            }
        }
        
        FitnessPlan plan = getPlanById(planId);
        if (plan == null) throw new IllegalArgumentException("Plan not found: " + planId);

        Enrollment enrollment = enrollmentService.enrollMember(memberId, planId, instructorId, plan.getDurationWeeks());
        
        double cost = plan.calculateTotalCost();
        if (cost > 0) {
            paymentService.processPayment(memberId, cost, paymentMethod, PaymentPurpose.MEMBERSHIP_FEE, 
                "Plan Enrollment: " + plan.getTitle(), paymentCtx);
        }

        User uMember = com.iwfc.pattern.DataStore.getInstance().getUsers().findById(memberId).orElse(null);
        User uInstructor = instructorId != null ? com.iwfc.pattern.DataStore.getInstance().getUsers().findById(instructorId).orElse(null) : null;
        
        if (uMember instanceof Member m) {
            Instructor i = (uInstructor instanceof Instructor) ? (Instructor) uInstructor : null;
            scheduleMaker.generateScheduleForEnrollment(enrollment, plan, m, i);
        }
        
        return enrollment;
    }

    /**
     * Change an existing workout plan.
     */
    public Enrollment changeWorkoutPlanForMember(String enrollmentId, String newPlanId, String newInstructorId, PaymentMethod paymentMethod, com.iwfc.pattern.payment.PaymentContext paymentCtx) throws Exception {
        Enrollment oldEnrollment = enrollmentService.getEnrollmentById(enrollmentId);
        if (oldEnrollment == null) throw new IllegalArgumentException("Enrollment not found");

        if (currentUser.getRole() == Role.MEMBER && !currentUser.getUserId().equals(oldEnrollment.getMemberId())) {
            throw new UnauthorizedAccessException("Cannot change plans for other members.");
        }

        // Expire old enrollment
        oldEnrollment.setExpirationDate(LocalDate.now());
        enrollmentService.updateEnrollment(oldEnrollment);

        // Assign new plan
        return assignWorkoutPlanToMember(oldEnrollment.getMemberId(), newPlanId, newInstructorId, paymentMethod, paymentCtx);
    }
    
    public void updateEnrollment(Enrollment enrollment) {
        enrollmentService.updateEnrollment(enrollment);
    }

    public List<Enrollment> getEnrollmentsForMember(String memberId) {
        return enrollmentService.getEnrollmentsForMember(memberId);
    }

    public List<Enrollment> getEnrollmentsForInstructor(String instructorId) {
        return enrollmentService.getEnrollmentsForInstructor(instructorId);
    }

    public InstructorChange requestInstructorChange(String enrollmentId, String requestedInstructorId, String reason) throws InvalidBookingException, DuplicateDataException {
        return enrollmentService.requestInstructorChange(enrollmentId, requestedInstructorId, reason);
    }

    public void approveInstructorChange(String requestId) throws InvalidBookingException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_USERS");
        enrollmentService.approveInstructorChange(requestId);
    }

    public void rejectInstructorChange(String requestId) throws InvalidBookingException, UnauthorizedAccessException {
        userService.checkPermission(currentUser, "MANAGE_USERS");
        enrollmentService.rejectInstructorChange(requestId);
    }

    public List<InstructorChange> getPendingInstructorChanges() {
        return enrollmentService.getPendingChangeRequests();
    }

    // =========================================================================
    // Body Measurements
    // =========================================================================

    public void addMeasurementLog(BodyMeasurementLog log) throws DuplicateDataException, UnauthorizedAccessException {
        if (currentUser.getRole() == Role.MEMBER && !currentUser.getUserId().equals(log.getMemberId())) {
            throw new UnauthorizedAccessException("Cannot log measurements for other members.");
        }
        measurementAnalyzer.addLog(log);
    }

    public List<BodyMeasurementLog> getMeasurementLogs(String memberId) throws UnauthorizedAccessException {
        if (currentUser.getRole() == Role.MEMBER && !currentUser.getUserId().equals(memberId)) {
            throw new UnauthorizedAccessException("Cannot view measurements of other members.");
        }
        if (currentUser.getRole() == Role.INSTRUCTOR) {
            Instructor instr = (Instructor) currentUser;
            if (!instr.getTraineeIds().contains(memberId)) {
                throw new UnauthorizedAccessException("Instructors can only view measurements of their own trainees.");
            }
        }
        return measurementAnalyzer.getLogsForMember(memberId);
    }

    public com.iwfc.service.MeasurementAnalyzer.InsightReport getMeasurementAnalysis(String memberId) throws UnauthorizedAccessException {
        // reuse same permission check by calling getMeasurementLogs (which will throw if not allowed)
        getMeasurementLogs(memberId);
        return measurementAnalyzer.generateInsightReport(memberId);
    }

    // =========================================================================
    // Follow Ups
    // =========================================================================

    public List<FollowUp> getFollowUps() {
        return com.iwfc.pattern.DataStore.getInstance().getFollowUps().findAll();
    }
}
