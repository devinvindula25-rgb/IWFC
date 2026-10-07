package com.iwfc;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.*;
import com.iwfc.exception.*;
import com.iwfc.pattern.payment.PaymentContext;
import com.iwfc.service.MeasurementAnalyzer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Console-based CLI application for the Intelligent Wellness and Fitness Center (IWFC).
 * <p>
 * Provides an interactive, headless terminal interface driven directly by {@link IWFCSystemFacade}.
 * Demonstrates architectural decoupling: both this CLI console and the JavaFX enterprise GUI
 * share the exact same underlying business logic, models, design patterns, and RBAC security rules.
 */
public class ConsoleApp {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final IWFCSystemFacade facade;
    private final Scanner scanner;
    private boolean running = true;

    public ConsoleApp() {
        this.facade = new IWFCSystemFacade();
        this.scanner = new Scanner(System.in);
        // Default login as Admin
        this.facade.login("ADM001");
    }

    public static void main(String[] args) {
        ConsoleApp app = new ConsoleApp();
        app.run();
    }

    public void run() {
        printBanner();
        while (running) {
            printMainMenu();
            String choice = readLine("Select an option [0-9]: ").trim();
            System.out.println();
            switch (choice) {
                case "1" -> handleDashboardOverview();
                case "2" -> handleUserManagement();
                case "3" -> handleEquipmentManagement();
                case "4" -> handleSessionBooking();
                case "5" -> handleMaintenanceOrders();
                case "6" -> handleBillingAndPayments();
                case "7" -> handleBodyMetrics();
                case "8" -> handleNotifications();
                case "9" -> handleSwitchUser();
                case "0" -> exitApp();
                default -> printWarning("Invalid selection. Please choose an option from the menu.");
            }
            if (running) {
                System.out.println();
                pausePrompt();
            }
        }
    }

    // =========================================================================
    // UI Headers & Helpers
    // =========================================================================

    private void printBanner() {
        System.out.println("================================================================================");
        System.out.println("          INTELLIGENT WELLNESS & FITNESS CENTER (IWFC) - CONSOLE SUITE          ");
        System.out.println("                  Enterprise Facade-Driven Architecture                        ");
        System.out.println("================================================================================");
    }

    private void printMainMenu() {
        User user = facade.getCurrentUser();
        String userInfo = (user != null) 
            ? String.format("%s (%s) | Role: %s", user.getName(), user.getUserId(), user.getRole())
            : "No user logged in";

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("  ACTIVE SESSION: [ %s ]%n", userInfo);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("  [1] System Dashboard & KPI Overview");
        System.out.println("  [2] Member & User Directory");
        System.out.println("  [3] Equipment Fleet & Health Monitoring");
        System.out.println("  [4] Fitness Sessions & Class Booking");
        System.out.println("  [5] Maintenance Orders & Technician Dispatch");
        System.out.println("  [6] Point of Sale & Billing Management");
        System.out.println("  [7] Body Composition & Health Analytics");
        System.out.println("  [8] Notification Hub & Activity Alerts");
        System.out.println("  [9] Switch User Profile (Admin / Trainer / Member)");
        System.out.println("  [0] Exit IWFC Console");
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void printSectionHeader(String title) {
        System.out.println("================================================================================");
        System.out.println("  " + title.toUpperCase());
        System.out.println("================================================================================");
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            running = false;
            return "0";
        }
        return scanner.nextLine();
    }

    private void pausePrompt() {
        System.out.print("Press [ENTER] to return to the main menu...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            running = false;
        }
    }

    private void printSuccess(String msg) {
        System.out.println("[SUCCESS] " + msg);
    }

    private void printWarning(String msg) {
        System.out.println("[WARNING] " + msg);
    }

    private void printError(String msg) {
        System.out.println("[ERROR] " + msg);
    }

    // =========================================================================
    // 1. Dashboard Overview
    // =========================================================================

    private void handleDashboardOverview() {
        printSectionHeader("System Dashboard & Key Performance Indicators");
        try {
            List<User> users = facade.getAllUsers();
            long memberCount = users.stream().filter(u -> u.getRole() == Role.MEMBER).count();
            long instructorCount = users.stream().filter(u -> u.getRole() == Role.INSTRUCTOR).count();

            List<Equipment> equipmentList = facade.getAllEquipment();
            long operationalEq = equipmentList.stream().filter(e -> e.getStatus() == EquipmentStatus.OPERATIONAL).count();
            long maintenanceEq = equipmentList.stream().filter(e -> e.getStatus() == EquipmentStatus.UNDER_MAINTENANCE || e.getStatus() == EquipmentStatus.FAULTY).count();

            List<FitnessSession> sessions = facade.getAllFitnessSessions();
            List<MaintenanceRequest> maintReqs = facade.getAllMaintenanceRequests();
            long pendingMaint = maintReqs.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();

            List<Payment> payments = facade.getAllPayments();
            double totalRevenue = payments.stream().mapToDouble(Payment::getAmount).sum();

            System.out.printf("  Total Registered Members  : %d%n", memberCount);
            System.out.printf("  Active Fitness Instructors: %d%n", instructorCount);
            System.out.printf("  Total Equipment Assets   : %d (%d Operational, %d In Maintenance)%n", equipmentList.size(), operationalEq, maintenanceEq);
            System.out.printf("  Scheduled Fitness Classes : %d weekly offerings%n", sessions.size());
            System.out.printf("  Active Maintenance Orders : %d open / pending dispatch%n", pendingMaint);
            System.out.printf("  Processed Transactions    : %d records (Total Revenue: $%,.2f)%n", payments.size(), totalRevenue);
            System.out.printf("  Unread System Alerts      : %d operational notifications%n", facade.getUnreadNotificationCount());
        } catch (UnauthorizedAccessException e) {
            // For member or restricted view, show member-level dashboard
            User curr = facade.getCurrentUser();
            System.out.printf("  Logged in as: %s (Member Portal)%n", curr != null ? curr.getName() : "Unknown");
            System.out.printf("  Scheduled Fitness Classes : %d weekly offerings%n", facade.getAllFitnessSessions().size());
            if (curr != null) {
                System.out.printf("  Upcoming Bookings         : %d active reservations%n", facade.getMyBookings().size());
                System.out.printf("  Total Payments Made       : $%,.2f%n", facade.getTotalPaymentsForMember(curr.getUserId()));
            }
        }
    }

    // =========================================================================
    // 2. Member & User Directory
    // =========================================================================

    private void handleUserManagement() {
        printSectionHeader("User & Member Directory");
        try {
            List<User> users = facade.getAllUsers();
            System.out.printf("%-10s | %-20s | %-12s | %-25s%n", "USER ID", "FULL NAME", "ROLE", "EMAIL ADDRESS");
            System.out.println("--------------------------------------------------------------------------------");
            for (User u : users) {
                System.out.printf("%-10s | %-20s | %-12s | %-25s%n",
                        u.getUserId(),
                        truncate(u.getName(), 20),
                        u.getRole(),
                        truncate(u.getEmail(), 25));
            }
            System.out.println();
            System.out.println("Options: [1] Register New Member  [2] View User Details  [0] Back");
            String subChoice = readLine("Choice: ").trim();
            if ("1".equals(subChoice)) {
                registerNewMember();
            } else if ("2".equals(subChoice)) {
                viewUserDetails();
            }
        } catch (UnauthorizedAccessException e) {
            printError("Access denied: " + e.getMessage());
        }
    }

    private void registerNewMember() {
        System.out.println("--- Register New Member ---");
        String id = readLine("Member ID (e.g. MEM099): ").trim();
        String name = readLine("Full Name: ").trim();
        String email = readLine("Email: ").trim();
        String phone = readLine("Phone: ").trim();
        String tier = readLine("Membership Tier (Bronze/Silver/Gold/Platinum): ").trim();
        if (id.isEmpty() || name.isEmpty() || email.isEmpty()) {
            printWarning("ID, Name, and Email cannot be empty.");
            return;
        }
        try {
            Member newMember = new Member(id, name, email, phone, "123 Gym Lane", "default.png", tier.isEmpty() ? "Silver" : tier);
            facade.addUser(newMember);
            printSuccess("Member '" + name + "' successfully registered with ID " + id + ".");
        } catch (DuplicateDataException | UnauthorizedAccessException e) {
            printError(e.getMessage());
        }
    }

    private void viewUserDetails() {
        String id = readLine("Enter User ID to inspect: ").trim();
        try {
            User user = facade.getAllUsers().stream()
                    .filter(u -> u.getUserId().equalsIgnoreCase(id))
                    .findFirst()
                    .orElse(null);
            if (user == null) {
                printWarning("User ID '" + id + "' not found.");
                return;
            }
            System.out.println("--- User Details ---");
            System.out.printf("  ID      : %s%n", user.getUserId());
            System.out.printf("  Name    : %s%n", user.getName());
            System.out.printf("  Role    : %s%n", user.getRole());
            System.out.printf("  Email   : %s%n", user.getEmail());
            System.out.printf("  Phone   : %s%n", user.getPhone());
            if (user instanceof Member m) {
                System.out.printf("  Tier    : %s%n", m.getMembershipType());
                System.out.printf("  Payments: $%.2f%n", m.getTotalPayments());
            } else if (user instanceof Instructor ins) {
                System.out.printf("  Specialty: %s%n", ins.getSpecialization());
                System.out.printf("  Trainees : %s%n", ins.getTraineeIds());
            }
        } catch (UnauthorizedAccessException e) {
            printError(e.getMessage());
        }
    }

    // =========================================================================
    // 3. Equipment Fleet
    // =========================================================================

    private void handleEquipmentManagement() {
        printSectionHeader("Equipment Fleet & Asset Health");
        List<Equipment> items = facade.getAllEquipment();
        System.out.printf("%-8s | %-22s | %-12s | %-16s | %-15s%n", "ID", "NAME", "CATEGORY", "STATUS", "LOCATION");
        System.out.println("--------------------------------------------------------------------------------");
        for (Equipment eq : items) {
            System.out.printf("%-8s | %-22s | %-12s | %-16s | %-15s%n",
                    eq.getId(),
                    truncate(eq.getName(), 22),
                    truncate(eq.getCategory(), 12),
                    eq.getStatus(),
                    eq.getLocation());
        }

        System.out.println();
        System.out.println("Options: [1] Report Equipment Issue / Maintenance  [2] Filter By Status  [0] Back");
        String subChoice = readLine("Choice: ").trim();
        if ("1".equals(subChoice)) {
            reportEquipmentIssue();
        } else if ("2".equals(subChoice)) {
            filterEquipmentByStatus();
        }
    }

    private void reportEquipmentIssue() {
        System.out.println("--- Report Equipment Issue ---");
        String eqId = readLine("Equipment ID: ").trim();
        Equipment eq = facade.getAllEquipment().stream()
                .filter(e -> e.getId().equalsIgnoreCase(eqId))
                .findFirst().orElse(null);
        if (eq == null) {
            printWarning("Equipment not found.");
            return;
        }
        String desc = readLine("Problem Description: ").trim();
        System.out.println("Urgency: [1] LOW  [2] MEDIUM  [3] HIGH");
        String uChoice = readLine("Select urgency [1-3]: ").trim();
        Urgency urgency = switch (uChoice) {
            case "1" -> Urgency.LOW;
            case "3" -> Urgency.HIGH;
            default -> Urgency.MEDIUM;
        };

        try {
            User current = facade.getCurrentUser();
            if (current instanceof Instructor) {
                facade.reportMaintenance(eq, desc, urgency);
            } else {
                // Admin or staff reporting
                Instructor dummyStaff = new Instructor("STAFF_AUTO", "Staff Reporter", "staff@iwfc.com", "000", "p", "p", "Fitness");
                facade.reportMaintenanceByStaff(eq, dummyStaff, desc, urgency);
            }
            printSuccess("Maintenance report created for " + eq.getName() + " with urgency " + urgency + ".");
        } catch (UnauthorizedAccessException e) {
            printError("Permission error: " + e.getMessage());
        } catch (Exception e) {
            printError("Failed to report issue: " + e.getMessage());
        }
    }

    private void filterEquipmentByStatus() {
        System.out.println("Statuses: [1] OPERATIONAL  [2] UNDER_MAINTENANCE  [3] FAULTY");
        String choice = readLine("Select [1-3]: ").trim();
        EquipmentStatus status = switch (choice) {
            case "2" -> EquipmentStatus.UNDER_MAINTENANCE;
            case "3" -> EquipmentStatus.FAULTY;
            default -> EquipmentStatus.OPERATIONAL;
        };
        List<Equipment> filtered = facade.getEquipmentByStatus(status);
        System.out.println("Matching Equipment (" + filtered.size() + "):");
        for (Equipment e : filtered) {
            System.out.printf("  [%s] %s - %s (%s)%n", e.getId(), e.getName(), e.getLocation(), e.getStatus());
        }
    }

    // =========================================================================
    // 4. Fitness Sessions & Booking
    // =========================================================================

    private void handleSessionBooking() {
        printSectionHeader("Fitness Sessions & Class Booking");
        List<FitnessSession> sessions = facade.getAllFitnessSessions();
        System.out.printf("%-8s | %-22s | %-10s | %-12s | %-16s | %-8s%n", "ID", "SESSION NAME", "DAY", "TIME", "ROOM", "CAPACITY");
        System.out.println("--------------------------------------------------------------------------------");
        for (FitnessSession s : sessions) {
            String timeStr = s.getStartTime() + "-" + s.getEndTime();
            System.out.printf("%-8s | %-22s | %-10s | %-12s | %-16s | %d%n",
                    s.getSessionId(),
                    truncate(s.getName(), 22),
                    s.getDayOfWeek(),
                    timeStr,
                    truncate(s.getLocation() != null ? s.getLocation().getDisplayName() : "Studio", 16),
                    s.getMaxCapacity());
        }

        System.out.println();
        System.out.println("Options: [1] Book Class (as Current Member)  [2] View My Bookings  [3] Cancel Booking  [0] Back");
        String subChoice = readLine("Choice: ").trim();
        if ("1".equals(subChoice)) {
            bookSessionFlow();
        } else if ("2".equals(subChoice)) {
            viewMyBookingsFlow();
        } else if ("3".equals(subChoice)) {
            cancelBookingFlow();
        }
    }

    private void bookSessionFlow() {
        User user = facade.getCurrentUser();
        if (!(user instanceof Member)) {
            printWarning("You are currently logged in as " + (user != null ? user.getRole() : "Guest") + 
                ". Only Members can book sessions. Use option [9] to switch to a Member.");
            return;
        }

        String sessionId = readLine("Enter Session ID to book (e.g. SES001): ").trim();
        String dateStr = readLine("Enter Booking Date (YYYY-MM-DD) or press ENTER for next occurrence: ").trim();
        try {
            Booking b;
            if (dateStr.isEmpty()) {
                b = facade.bookSession(sessionId);
            } else {
                LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
                b = facade.bookSessionForDate(sessionId, date);
            }
            printSuccess(String.format("Successfully booked! Booking ID: %s for %s (%s)", 
                    b.getBookingId(), b.getSession().getName(), b.getSessionDate()));
        } catch (InvalidBookingException | UnauthorizedAccessException e) {
            printError(e.getMessage());
        } catch (Exception e) {
            printError("Booking failed: " + e.getMessage());
        }
    }

    private void viewMyBookingsFlow() {
        User user = facade.getCurrentUser();
        if (user == null) {
            printWarning("No active user.");
            return;
        }
        List<Booking> bookings = facade.getMyBookings();
        if (bookings.isEmpty()) {
            System.out.println("No active bookings found for user " + user.getUserId());
            return;
        }
        System.out.println("Active Bookings for " + user.getName() + ":");
        for (Booking b : bookings) {
            System.out.printf("  Booking ID: %-8s | Session: %-20s | Date: %s | Status: %s%n",
                    b.getBookingId(), b.getSession().getName(), b.getSessionDate(), b.getStatus());
        }
    }

    private void cancelBookingFlow() {
        String bookingId = readLine("Enter Booking ID to cancel: ").trim();
        try {
            facade.cancelBooking(bookingId);
            printSuccess("Booking " + bookingId + " has been cancelled.");
        } catch (UnauthorizedAccessException e) {
            printError(e.getMessage());
        } catch (Exception e) {
            printError("Cancellation failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. Maintenance Orders
    // =========================================================================

    private void handleMaintenanceOrders() {
        printSectionHeader("Maintenance Work Orders & Dispatch");
        List<MaintenanceRequest> requests = facade.getAllMaintenanceRequests();
        System.out.printf("%-10s | %-16s | %-12s | %-10s | %-20s%n", "ORDER ID", "EQUIPMENT", "STATUS", "URGENCY", "DESCRIPTION");
        System.out.println("--------------------------------------------------------------------------------");
        for (MaintenanceRequest r : requests) {
            System.out.printf("%-10s | %-16s | %-12s | %-10s | %-20s%n",
                    r.getRequestId(),
                    truncate(r.getEquipment().getName(), 16),
                    r.getStatus(),
                    r.getUrgency(),
                    truncate(r.getDescription(), 20));
        }

        System.out.println();
        System.out.println("Options: [1] Assign Technician  [2] Complete Maintenance Order  [0] Back");
        String subChoice = readLine("Choice: ").trim();
        if ("1".equals(subChoice)) {
            String reqId = readLine("Maintenance Order ID: ").trim();
            String techName = readLine("Assign Technician Name: ").trim();
            try {
                facade.assignMaintenance(reqId, techName);
                printSuccess("Order " + reqId + " assigned to " + techName + ".");
            } catch (UnauthorizedAccessException e) {
                printError(e.getMessage());
            } catch (Exception e) {
                printError("Failed to assign technician: " + e.getMessage());
            }
        } else if ("2".equals(subChoice)) {
            String reqId = readLine("Maintenance Order ID: ").trim();
            String notes = readLine("Resolution Notes: ").trim();
            try {
                facade.completeMaintenance(reqId, notes);
                printSuccess("Order " + reqId + " marked as COMPLETED.");
            } catch (UnauthorizedAccessException e) {
                printError(e.getMessage());
            } catch (Exception e) {
                printError("Failed to complete order: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 6. Point of Sale & Billing Management
    // =========================================================================

    private void handleBillingAndPayments() {
        printSectionHeader("Point of Sale & Billing Ledger");
        try {
            List<Payment> payments = facade.getAllPayments();
            System.out.printf("%-12s | %-10s | %-10s | %-15s | %-20s%n", "PAYMENT ID", "MEMBER ID", "AMOUNT", "METHOD", "PURPOSE");
            System.out.println("--------------------------------------------------------------------------------");
            for (Payment p : payments) {
                System.out.printf("%-12s | %-10s | $%-9.2f | %-15s | %-20s%n",
                        p.getPaymentId(),
                        p.getMemberId(),
                        p.getAmount(),
                        p.getMethod(),
                        p.getPurpose());
            }

            System.out.println();
            System.out.println("Options: [1] Process New Payment  [0] Back");
            String subChoice = readLine("Choice: ").trim();
            if ("1".equals(subChoice)) {
                processNewPaymentFlow();
            }
        } catch (UnauthorizedAccessException e) {
            printError(e.getMessage());
        }
    }

    private void processNewPaymentFlow() {
        System.out.println("--- Process New Transaction ---");
        String memberId = readLine("Member ID (e.g. MEM001): ").trim();
        String amtStr = readLine("Amount ($): ").trim();
        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            printWarning("Invalid amount.");
            return;
        }

        System.out.println("Method: [1] CARD  [2] CASH  [3] BANK_TRANSFER");
        String mChoice = readLine("Select [1-3]: ").trim();
        PaymentMethod method = switch (mChoice) {
            case "2" -> PaymentMethod.CASH;
            case "3" -> PaymentMethod.BANK_TRANSFER;
            default -> PaymentMethod.CARD;
        };

        System.out.println("Purpose: [1] MEMBERSHIP_FEE  [2] PERSONAL_TRAINING  [3] SESSION_BOOKING  [4] MERCHANDISE");
        String pChoice = readLine("Select [1-4]: ").trim();
        PaymentPurpose purpose = switch (pChoice) {
            case "2" -> PaymentPurpose.PERSONAL_TRAINING;
            case "3" -> PaymentPurpose.SESSION_BOOKING;
            case "4" -> PaymentPurpose.MERCHANDISE;
            default -> PaymentPurpose.MEMBERSHIP_FEE;
        };

        String desc = readLine("Description (optional): ").trim();
        if (desc.isEmpty()) desc = "Console POS Payment";

        PaymentContext ctx = new PaymentContext();
        if (method == PaymentMethod.CARD) {
            String card = readLine("Card Number (16 digits, or ENTER for mock default): ").trim();
            ctx.setCardNumber(card.isEmpty() ? "4532111122223333" : card);
            ctx.setExpiryDate("12/28");
        }

        try {
            Payment receipt = facade.processPayment(memberId, amount, method, purpose, desc, ctx);
            printSuccess("Payment approved!");
            System.out.println("--------------------------------------------------");
            System.out.printf("  TRANSACTION RECEIPT: %s%n", receipt.getPaymentId());
            System.out.printf("  Status             : %s%n", receipt.getStatus());
            System.out.printf("  Total Paid         : $%.2f%n", receipt.getAmount());
            System.out.printf("  Timestamp          : %s%n", receipt.getPaidAt());
            System.out.println("--------------------------------------------------");
        } catch (PaymentFailedException | UnauthorizedAccessException e) {
            printError(e.getMessage());
        }
    }

    // =========================================================================
    // 7. Body Composition & Health Analytics
    // =========================================================================

    private void handleBodyMetrics() {
        printSectionHeader("Body Composition & Health Analytics");
        String memberId = readLine("Enter Member ID to analyze (e.g. MEM001): ").trim();
        try {
            List<BodyMeasurementLog> logs = facade.getMeasurementLogs(memberId);
            System.out.printf("Found %d measurement entries for member %s:%n", logs.size(), memberId);
            for (BodyMeasurementLog l : logs) {
                System.out.printf("  [%s] Weight: %.1f kg | Height: %.1f cm | Chest: %.1f cm | Arms: %.1f cm%n",
                        l.getDate(), l.getWeight(), l.getHeight(), l.getChest(), l.getUpperArms());
            }

            System.out.println();
            System.out.println("Options: [1] Log New Measurement  [2] View Automated Insight Report  [0] Back");
            String subChoice = readLine("Choice: ").trim();
            if ("1".equals(subChoice)) {
                logNewMeasurementFlow(memberId);
            } else if ("2".equals(subChoice)) {
                MeasurementAnalyzer.InsightReport report = facade.getMeasurementAnalysis(memberId);
                if (report == null) {
                    System.out.println("Not enough historical data (at least 2 measurement entries required for trend analysis).");
                } else {
                    System.out.println("--------------------------------------------------");
                    System.out.println("  IWFC BIOMETRIC INSIGHT REPORT");
                    System.out.println("--------------------------------------------------");
                    System.out.printf("  Date Range: %s%n", report.dateRange());
                    System.out.println("  Trends Detected:");
                    for (MeasurementAnalyzer.Insight trend : report.trends()) {
                        System.out.printf("   * [%s] %s: %s (diff: %.1f)%n", trend.type(), trend.title(), trend.description(), trend.diff());
                    }
                    if (!report.symmetry().isEmpty()) {
                        System.out.println("  Symmetry Analysis:");
                        for (MeasurementAnalyzer.Insight sym : report.symmetry()) {
                            System.out.printf("   * %s: %s%n", sym.title(), sym.description());
                        }
                    }
                    System.out.println("--------------------------------------------------");
                }
            }
        } catch (UnauthorizedAccessException e) {
            printError(e.getMessage());
        }
    }

    private void logNewMeasurementFlow(String memberId) {
        System.out.println("--- Log New Biometric Data ---");
        try {
            double weight = Double.parseDouble(readLine("Weight (kg): ").trim());
            double height = Double.parseDouble(readLine("Height (cm): ").trim());
            double chest = Double.parseDouble(readLine("Chest (cm): ").trim());
            double arms = Double.parseDouble(readLine("Upper Arms (cm): ").trim());
            double thighs = Double.parseDouble(readLine("Thighs (cm): ").trim());
            double calves = Double.parseDouble(readLine("Calves (cm): ").trim());

            String logId = "BML_" + System.currentTimeMillis();
            BodyMeasurementLog log = new BodyMeasurementLog(logId, memberId, LocalDate.now(), weight, height, chest, arms, thighs, calves);
            facade.addMeasurementLog(log);
            printSuccess("New biometric log saved successfully.");
        } catch (NumberFormatException e) {
            printWarning("Invalid numeric input.");
        } catch (DuplicateDataException | UnauthorizedAccessException e) {
            printError(e.getMessage());
        }
    }

    // =========================================================================
    // 8. Notifications Hub
    // =========================================================================

    private void handleNotifications() {
        printSectionHeader("Operational Alerts & Notification Hub");
        List<Notification> notifications = facade.getNotifications();
        if (notifications.isEmpty()) {
            System.out.println("No notifications found for current role.");
            return;
        }

        System.out.printf("%-10s | %-6s | %-12s | %-35s%n", "ID", "STATE", "CATEGORY", "MESSAGE");
        System.out.println("--------------------------------------------------------------------------------");
        for (Notification n : notifications) {
            System.out.printf("%-10s | %-6s | %-12s | %-35s%n",
                    n.getId(),
                    n.isRead() ? "READ" : "NEW",
                    n.getCategory(),
                    truncate(n.getMessage(), 35));
        }

        System.out.println();
        System.out.println("Options: [1] Mark Notification as Read  [2] Mark All as Read  [0] Back");
        String subChoice = readLine("Choice: ").trim();
        if ("1".equals(subChoice)) {
            String id = readLine("Notification ID: ").trim();
            facade.markNotificationAsRead(id);
            printSuccess("Marked " + id + " as read.");
        } else if ("2".equals(subChoice)) {
            facade.markAllNotificationsAsRead();
            printSuccess("All notifications marked as read.");
        }
    }

    // =========================================================================
    // 9. Switch User
    // =========================================================================

    private void handleSwitchUser() {
        printSectionHeader("Switch User Profile");
        System.out.println("Quick Profiles:");
        System.out.println("  [1] ADM001  - Devin Admin   (Administrator)");
        System.out.println("  [2] TRN001  - Alex Rivera   (Fitness Instructor)");
        System.out.println("  [3] MEM001  - Sarah Connor  (Active Member)");
        System.out.println("  [4] Custom User ID");
        System.out.println("  [0] Cancel");

        String choice = readLine("Select profile: ").trim();
        String targetId = switch (choice) {
            case "1" -> "ADM001";
            case "2" -> "TRN001";
            case "3" -> "MEM001";
            case "4" -> readLine("Enter User ID: ").trim();
            default -> null;
        };

        if (targetId != null && !targetId.isEmpty()) {
            User logged = facade.login(targetId);
            if (logged != null) {
                printSuccess(String.format("Switched to %s (%s) [%s]", logged.getName(), logged.getUserId(), logged.getRole()));
            } else {
                printWarning("User ID '" + targetId + "' not found. Kept current session.");
            }
        }
    }

    private void exitApp() {
        printSuccess("Shutting down IWFC Console Suite. Thank you!");
        running = false;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
