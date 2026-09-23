package com.iwfc.pattern;

import com.iwfc.model.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton Pattern — Centralized In-Memory Data Store.
 * Holds all application data as a substitute for a database.
 * Thread-safe lazy initialization ensures a single instance exists.
 *
 * Pre-populated with realistic seed data for demonstration purposes.
 */
public class DataStore {

    private static volatile DataStore instance;

    private final List<User> users;
    private final Map<String, Equipment> equipmentMap;
    private final Map<String, FitnessSession> sessionMap;
    private final List<Booking> bookings;
    private final List<MaintenanceRequest> maintenanceRequests;
    private final List<Notification> notifications;

    /**
     * Private constructor — prevents external instantiation (Singleton pattern).
     */
    private DataStore() {
        this.users = new ArrayList<>();
        this.equipmentMap = new HashMap<>();
        this.sessionMap = new HashMap<>();
        this.bookings = new ArrayList<>();
        this.maintenanceRequests = new ArrayList<>();
        this.notifications = new ArrayList<>();

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

    /**
     * Generic accessor — returns all items of a given type.
     * Demonstrates use of Java Generics.
     *
     * @param type the class type to filter by
     * @param <T>  the generic type parameter
     * @return a list of items matching the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getAll(Class<T> type) {
        if (type == User.class || type == Admin.class || type == Instructor.class || type == Member.class) {
            List<T> result = new ArrayList<>();
            for (User user : users) {
                if (type.isInstance(user)) {
                    result.add((T) user);
                }
            }
            return result;
        }
        if (type == Equipment.class) {
            return (List<T>) new ArrayList<>(equipmentMap.values());
        }
        if (type == FitnessSession.class) {
            return (List<T>) new ArrayList<>(sessionMap.values());
        }
        if (type == Booking.class) {
            return (List<T>) new ArrayList<>(bookings);
        }
        if (type == MaintenanceRequest.class) {
            return (List<T>) new ArrayList<>(maintenanceRequests);
        }
        if (type == Notification.class) {
            return (List<T>) new ArrayList<>(notifications);
        }
        return new ArrayList<>();
    }

    // --- Direct accessors for each collection ---

    public List<User> getUsers() {
        return users;
    }

    public Map<String, Equipment> getEquipmentMap() {
        return equipmentMap;
    }

    public Map<String, FitnessSession> getSessionMap() {
        return sessionMap;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public List<MaintenanceRequest> getMaintenanceRequests() {
        return maintenanceRequests;
    }

    public List<Notification> getNotifications() {
        return notifications;
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

        users.add(admin);
        users.add(instructor1);
        users.add(instructor2);
        users.add(member1);
        users.add(member2);
        users.add(member3);

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

        equipmentMap.put(treadmill1.getEquipmentId(), treadmill1);
        equipmentMap.put(treadmill2.getEquipmentId(), treadmill2);
        equipmentMap.put(spinBike1.getEquipmentId(), spinBike1);
        equipmentMap.put(spinBike2.getEquipmentId(), spinBike2);
        equipmentMap.put(rowingMachine.getEquipmentId(), rowingMachine);
        equipmentMap.put(yogaMats.getEquipmentId(), yogaMats);
        equipmentMap.put(dumbbellSet.getEquipmentId(), dumbbellSet);
        equipmentMap.put(cableMachine.getEquipmentId(), cableMachine);

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

        sessionMap.put(hiitMon.getSessionId(), hiitMon);
        sessionMap.put(yogaTue.getSessionId(), yogaTue);
        sessionMap.put(spinWed.getSessionId(), spinWed);
        sessionMap.put(pilatesThu.getSessionId(), pilatesThu);
        sessionMap.put(crossfitFri.getSessionId(), crossfitFri);
        sessionMap.put(hiitSat.getSessionId(), hiitSat);
        
        // --- Sample Bookings ---
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextTuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        
        Booking b1 = new Booking("BK001", member1, hiitMon, nextMonday, LocalDateTime.now().minusDays(1));
        Booking b2 = new Booking("BK002", member2, hiitMon, nextMonday, LocalDateTime.now().minusDays(2));
        Booking b3 = new Booking("BK003", member1, yogaTue, nextTuesday, LocalDateTime.now().minusHours(5));
        
        bookings.add(b1);
        bookings.add(b2);
        bookings.add(b3);

        // --- Sample Maintenance Requests ---
        MaintenanceRequest req1 = new MaintenanceRequest("MR001", spinBike2, instructor1,
                "Spin bike resistance knob stuck — unable to adjust difficulty",
                Urgency.HIGH);
        MaintenanceRequest req2 = new MaintenanceRequest("MR002", cableMachine, instructor1,
                "Cable fraying on left side — safety risk",
                Urgency.HIGH);
        req2.assign("Tech Team Alpha");

        maintenanceRequests.add(req1);
        maintenanceRequests.add(req2);

        // --- Sample Notifications ---
        Notification n1 = new Notification("NTF0001", NotificationCategory.MAINTENANCE, "High Urgency Maintenance", "Cable machine fraying reported by Mike Torres.", Role.INSTRUCTOR);
        Notification n2 = new Notification("NTF0002", NotificationCategory.BOOKING, "Booking Confirmed", "Your spot in Morning HIIT Blast is confirmed.", Role.MEMBER);
        Notification n3 = new Notification("NTF0003", NotificationCategory.SYSTEM, "System Maintenance", "Scheduled downtime this Sunday at 2 AM for upgrades.", null);
        Notification n4 = new Notification("NTF0004", NotificationCategory.EQUIPMENT, "New Equipment Added", "20 new Yoga Mats have arrived.", Role.ADMIN);

        notifications.add(n1);
        notifications.add(n2);
        notifications.add(n3);
        notifications.add(n4);
    }
}
