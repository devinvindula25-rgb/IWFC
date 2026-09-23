package com.iwfc.pattern;

import com.iwfc.model.*;

import com.iwfc.repository.Repository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Singleton Pattern — Centralized In-Memory Data Store.
 * Holds all application data as a substitute for a database.
 * Thread-safe lazy initialization ensures a single instance exists.
 *
 * Pre-populated with realistic seed data for demonstration purposes.
 */
public class DataStore {

    private static volatile DataStore instance;

    private final Repository<User> users;
    private final Repository<Equipment> equipment;
    private final Repository<FitnessSession> sessions;
    private final Repository<Booking> bookings;
    private final Repository<MaintenanceRequest> maintenanceRequests;
    private final Repository<Notification> notifications;
    private final Repository<Payment> payments;
    private final Repository<AuditEntry> auditLogs;

    /**
     * Private constructor — prevents external instantiation (Singleton pattern).
     */
    private DataStore() {
        this.users = new Repository<>();
        this.equipment = new Repository<>();
        this.sessions = new Repository<>();
        this.bookings = new Repository<>();
        this.maintenanceRequests = new Repository<>();
        this.notifications = new Repository<>();
        this.payments = new Repository<>();
        this.auditLogs = new Repository<>();

        initSeedData();
    }

    /**
     * Returns the single DataStore instance, creating it if necessary.
     * Uses double-checked locking for thread safety.
     *
     * @return the singleton DataStore instance
     */
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    /**
     * Resets the DataStore to its initial state.
     * Primarily used for testing to ensure clean state between tests.
     */
    public static void resetInstance() {
        synchronized (DataStore.class) {
            instance = null;
        }
    }

    // --- Direct accessors for each collection ---

    public Repository<User> getUsers() {
        return users;
    }

    public Repository<Equipment> getEquipment() {
        return equipment;
    }

    public Repository<FitnessSession> getSessions() {
        return sessions;
    }

    public Repository<Booking> getBookings() {
        return bookings;
    }

    public Repository<MaintenanceRequest> getMaintenanceRequests() {
        return maintenanceRequests;
    }

    public Repository<Notification> getNotifications() {
        return notifications;
    }

    public Repository<Payment> getPayments() {
        return payments;
    }

    public Repository<AuditEntry> getAuditLogs() {
        return auditLogs;
    }

    /**
     * Pre-populates the data store with realistic sample data.
     * Includes users, equipment, sessions, bookings, and maintenance requests.
     */
    private void initSeedData() {
        // --- Users ---
        Admin admin = new Admin("ADM001", "Sarah Johnson", "sarah.johnson@iwfc.com",
                "+1 (555) 123-4567", "100 Executive Way, Suite 500, New York, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_sarah_johnson_1790079443262.jpg");

        Instructor instructor1 = new Instructor("INS001", "Mike Torres", "mike.torres@iwfc.com",
                "+1 (555) 987-6543", "45 Fitness Ave, Brooklyn, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_mike_torres_1790079456178.jpg", "HIIT & CrossFit");
        instructor1.getTraineeIds().addAll(List.of("MEM001", "MEM003"));
        instructor1.setPerformanceScore(92.5);
        instructor1.setAttendanceRate(88.0);
        instructor1.setBio("Specializes in high-intensity interval training and strength conditioning. Believes in pushing limits safely.");
        instructor1.getFeedbacks().addAll(List.of(
            "Mike is incredibly motivating! My stamina has doubled.",
            "Tough classes but totally worth the sweat."
        ));

        Instructor instructor2 = new Instructor("INS002", "Lisa Chen", "lisa.chen@iwfc.com",
                "+1 (555) 555-1212", "78 Zen Street, Queens, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_lisa_chen_1790079469004.jpg", "Yoga & Pilates");
        instructor2.getTraineeIds().addAll(List.of("MEM002"));
        instructor2.setPerformanceScore(96.0);
        instructor2.setAttendanceRate(95.5);
        instructor2.setBio("Passionate about mindful movement, core stability, and holistic wellness.");
        instructor2.getFeedbacks().addAll(List.of(
            "Lisa's yoga classes are the highlight of my week.",
            "Very patient and corrects posture perfectly."
        ));

        Member member1 = new Member("MEM001", "James Wilson", "james.wilson@email.com",
                "+1 (555) 111-2222", "12 Maple Street, Springfield, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_james_wilson_1790079480555.jpg", "Premium");
        member1.setTotalPayments(1250.00);
        member1.setHoursSpent(145);
        member1.setDietHabits("High protein, keto-friendly, 2500 kcal/day.");
        member1.getMachinesUsed().addAll(List.of("Treadmill Pro X1", "Spin Bike S200", "Dumbbell Set"));

        Member member2 = new Member("MEM002", "Emma Davis", "emma.davis@email.com",
                "+1 (555) 333-4444", "88 Oak Lane, Springfield, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_emma_davis_1790079493580.jpg", "Standard");
        member2.setTotalPayments(450.00);
        member2.setHoursSpent(60);
        member2.setDietHabits("Vegetarian, low carb.");
        member2.getMachinesUsed().addAll(List.of("Yoga Mat", "Treadmill Pro X2"));

        Member member3 = new Member("MEM003", "Alex Kumar", "alex.kumar@email.com",
                "+1 (555) 777-8888", "304 Pine Road, Manhattan, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_alex_kumar_1790079510469.jpg", "Premium");
        member3.setTotalPayments(950.00);
        member3.setHoursSpent(210);
        member3.setDietHabits("Balanced macro diet, 3000 kcal/day for bulking.");
        member3.getMachinesUsed().addAll(List.of("Cable Crossover Machine", "Dumbbell Set", "Rowing Machine"));

        try {
            users.save(admin);
            users.save(instructor1);
            users.save(instructor2);
            users.save(member1);
            users.save(member2);
            users.save(member3);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed users: " + e.getMessage());
        }

        // --- Equipment ---
        Equipment treadmill1 = new Equipment("EQ001", "Treadmill Pro X1", "Treadmill", EquipmentStatus.OPERATIONAL,
                Location.CARDIO_ZONE, 342.0, 500.0);
        Equipment treadmill2 = new Equipment("EQ002", "Treadmill Pro X2", "Treadmill", EquipmentStatus.OPERATIONAL,
                Location.CARDIO_ZONE, 128.0, 500.0);
        Equipment spinBike1 = new Equipment("EQ003", "Spin Bike S200", "Spin Bike", EquipmentStatus.OPERATIONAL,
                Location.STUDIO_A, 275.0, 500.0);
        Equipment spinBike2 = new Equipment("EQ004", "Spin Bike S200", "Spin Bike", EquipmentStatus.FAULTY,
                Location.STUDIO_A, 489.0, 500.0);
        Equipment rowingMachine = new Equipment("EQ005", "Rowing Machine RM-5", "Rowing Machine", EquipmentStatus.OPERATIONAL,
                Location.CARDIO_ZONE, 156.0, 500.0);
        Equipment yogaMats = new Equipment("EQ006", "Yoga Mat Set (x20)", "Mat/Accessory", EquipmentStatus.OPERATIONAL,
                Location.STUDIO_B, 80.0, 1000.0);
        Equipment dumbbellSet = new Equipment("EQ007", "Dumbbell Set 5-50kg", "Free Weights", EquipmentStatus.OPERATIONAL,
                Location.WEIGHT_ROOM, 410.0, 500.0);
        Equipment cableMachine = new Equipment("EQ008", "Cable Crossover Machine", "Cable Machine", EquipmentStatus.UNDER_MAINTENANCE,
                Location.WEIGHT_ROOM, 520.0, 500.0);

        try {
            equipment.save(treadmill1);
            equipment.save(treadmill2);
            equipment.save(spinBike1);
            equipment.save(spinBike2);
            equipment.save(rowingMachine);
            equipment.save(yogaMats);
            equipment.save(dumbbellSet);
            equipment.save(cableMachine);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed equipment: " + e.getMessage());
        }

        // --- Fitness Sessions ---
        FitnessSession hiitMon = new FitnessSession("SES001", "Morning HIIT Blast", "HIIT",
                instructor1, DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0),
                Location.STUDIO_A, 20);
        FitnessSession yogaTue = new FitnessSession("SES002", "Yoga Flow", "YOGA",
                instructor2, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0),
                Location.STUDIO_B, 15);
        FitnessSession spinWed = new FitnessSession("SES003", "Spin Cycle Power", "SPIN",
                instructor1, DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(19, 0),
                Location.STUDIO_A, 25);
        FitnessSession pilatesThu = new FitnessSession("SES004", "Core Pilates", "PILATES",
                instructor2, DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 0),
                Location.STUDIO_B, 12);
        FitnessSession crossfitFri = new FitnessSession("SES005", "CrossFit Challenge", "CROSSFIT",
                instructor1, DayOfWeek.FRIDAY, LocalTime.of(17, 0), LocalTime.of(18, 0),
                Location.WEIGHT_ROOM, 18);
        FitnessSession hiitSat = new FitnessSession("SES006", "Weekend HIIT", "HIIT",
                instructor1, DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(10, 0),
                Location.STUDIO_A, 20);

        try {
            sessions.save(hiitMon);
            sessions.save(yogaTue);
            sessions.save(spinWed);
            sessions.save(pilatesThu);
            sessions.save(crossfitFri);
            sessions.save(hiitSat);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed sessions: " + e.getMessage());
        }
        
        // --- Sample Bookings ---
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextTuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        
        Booking b1 = new Booking("BK001", member1, hiitMon, nextMonday, LocalDateTime.now().minusDays(1));
        Booking b2 = new Booking("BK002", member2, hiitMon, nextMonday, LocalDateTime.now().minusDays(2));
        Booking b3 = new Booking("BK003", member1, yogaTue, nextTuesday, LocalDateTime.now().minusHours(5));
        
        try {
            bookings.save(b1);
            bookings.save(b2);
            bookings.save(b3);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed bookings: " + e.getMessage());
        }

        // --- Sample Maintenance Requests ---
        MaintenanceRequest req1 = new MaintenanceRequest("MR001", spinBike2, instructor1,
                "Spin bike resistance knob stuck — unable to adjust difficulty",
                Urgency.HIGH);
        MaintenanceRequest req2 = new MaintenanceRequest("MR002", cableMachine, instructor1,
                "Cable fraying on left side — safety risk",
                Urgency.HIGH);
        req2.assign("Tech Team Alpha");

        try {
            maintenanceRequests.save(req1);
            maintenanceRequests.save(req2);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed maintenance requests: " + e.getMessage());
        }

        // --- Sample Notifications ---
        Notification n1 = new Notification("NTF0001", NotificationCategory.MAINTENANCE, "High Urgency Maintenance", "Cable machine fraying reported by Mike Torres.", Role.INSTRUCTOR);
        Notification n2 = new Notification("NTF0002", NotificationCategory.BOOKING, "Booking Confirmed", "Your spot in Morning HIIT Blast is confirmed.", Role.MEMBER);
        Notification n3 = new Notification("NTF0003", NotificationCategory.SYSTEM, "System Maintenance", "Scheduled downtime this Sunday at 2 AM for upgrades.", null);
        Notification n4 = new Notification("NTF0004", NotificationCategory.EQUIPMENT, "New Equipment Added", "20 new Yoga Mats have arrived.", Role.ADMIN);

        try {
            notifications.save(n1);
            notifications.save(n2);
            notifications.save(n3);
            notifications.save(n4);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed notifications: " + e.getMessage());
        }
    }
}
