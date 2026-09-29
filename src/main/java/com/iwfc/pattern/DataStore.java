package com.iwfc.pattern;

import com.iwfc.model.*;

import com.iwfc.repository.Repository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final Repository<BodyMeasurementLog> measurementLogs;
    private final Repository<FollowUp> followUps;

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
        this.measurementLogs = new Repository<>();
        this.followUps = new Repository<>();

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

    public Repository<BodyMeasurementLog> getMeasurementLogs() {
        return measurementLogs;
    }

    public Repository<FollowUp> getFollowUps() {
        return followUps;
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
            "Tough classes, but totally worth the sweat."
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
        member1.setDietHabits("High protein, keto-friendly, 2500 kcal/day.");
        member1.getMachinesUsed().addAll(List.of("Treadmill Pro X1", "Spin Bike S200", "Dumbbell Set"));

        Member member2 = new Member("MEM002", "Emma Davis", "emma.davis@email.com",
                "+1 (555) 333-4444", "88 Oak Lane, Springfield, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_emma_davis_1790079493580.jpg", "Standard");
        member2.setDietHabits("Vegetarian, low carb.");
        member2.getMachinesUsed().addAll(List.of("Yoga Mat", "Treadmill Pro X2"));

        Member member3 = new Member("MEM003", "Alex Kumar", "alex.kumar@email.com",
                "+1 (555) 777-8888", "304 Pine Road, Manhattan, NY",
                "file:///C:/Users/devin/.gemini/antigravity-ide/brain/bb808584-8f28-4f8d-ba96-f69f83198ed5/avatar_alex_kumar_1790079510469.jpg", "Premium");
        member3.setDietHabits("Balanced macro diet, 3000 kcal/day for bulking.");
        member3.getMachinesUsed().addAll(List.of("Cable Crossover Machine", "Dumbbell Set", "Rowing Machine"));

        try {
            users.save(admin);
            users.save(instructor1);
            users.save(instructor2);
            users.save(member1);
            users.save(member2);
            users.save(member3);
            
            // Seed 30 more members
            String[] firstNames = {"John", "Emma", "Michael", "Sophia", "William", "Olivia", "James", "Ava", "Benjamin", "Isabella", "Lucas", "Mia", "Henry", "Evelyn", "Alexander", "Harper", "Sebastian", "Camila", "Jack", "Gianna", "Daniel", "Abigail", "Matthew", "Luna", "Samuel", "Ella", "David", "Elizabeth", "Joseph", "Sofia"};
            String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson"};
            String[] avatarUrls = {
                "file:///d:/Projects/IWFC/src/main/resources/com/iwfc/images/avatars/avatar_male_1.jpg",
                "file:///d:/Projects/IWFC/src/main/resources/com/iwfc/images/avatars/avatar_male_2.jpg",
                "file:///d:/Projects/IWFC/src/main/resources/com/iwfc/images/avatars/avatar_female_1.jpg",
                "file:///d:/Projects/IWFC/src/main/resources/com/iwfc/images/avatars/avatar_female_2.jpg"
            };
            
            for (int i = 0; i < 30; i++) {
                String name = firstNames[i] + " " + lastNames[i];
                String email = firstNames[i].toLowerCase() + "." + lastNames[i].toLowerCase() + "@email.com";
                String phone = String.format("+1 (555) %03d-%04d", 100 + i, 1000 + i);
                String address = (100 + i) + " Main St, Springfield, NY";
                
                // Determine gender for avatar array matching (simple heuristic)
                boolean isFemale = (i % 2 != 0); // Simplistic alternating
                String avatarUrl = isFemale ? avatarUrls[2 + (i % 2)] : avatarUrls[i % 2];
                
                String plan = (i % 3 == 0) ? "Premium" : "Standard";
                
                Member m = new Member("MEM" + String.format("%03d", i + 4), name, email, phone, address, avatarUrl, plan);
                m.setDietHabits("Balanced diet");
                
                users.save(m);
            }
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed users: " + e.getMessage());
        }

        // --- Equipment ---
        Equipment treadmill1 = new Equipment("EQ001", "Treadmill Pro X1", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 342.0, 500.0);
        Equipment treadmill2 = new Equipment("EQ002", "Treadmill Pro X2", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 128.0, 500.0);
        Equipment spinBike1 = new Equipment("EQ003", "Spin Bike S200", "Cardio", EquipmentStatus.OPERATIONAL, Location.STUDIO_A, 275.0, 500.0);
        Equipment spinBike2 = new Equipment("EQ004", "Spin Bike S200", "Cardio", EquipmentStatus.FAULTY, Location.STUDIO_A, 489.0, 500.0);
        Equipment rowingMachine = new Equipment("EQ005", "Rowing Machine RM-5", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 156.0, 500.0);
        Equipment elliptical = new Equipment("EQ009", "Elliptical Trainer Pro", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 90.0, 500.0);
        Equipment stairClimber = new Equipment("EQ010", "Stair Climber 5000", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 45.0, 500.0);
        Equipment airBike = new Equipment("EQ011", "Assault Air Bike", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 200.0, 500.0);
        
        Equipment cableMachine = new Equipment("EQ008", "Cable Crossover Machine", "Strength (Machines)", EquipmentStatus.UNDER_MAINTENANCE, Location.WEIGHT_ROOM, 520.0, 500.0);
        Equipment legPress = new Equipment("EQ012", "Leg Press Machine", "Strength (Machines)", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 100.0, 1000.0);
        Equipment latPulldown = new Equipment("EQ013", "Lat Pulldown Machine", "Strength (Machines)", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 120.0, 1000.0);
        Equipment smithMachine = new Equipment("EQ014", "Smith Machine", "Strength (Machines)", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 300.0, 1000.0);

        Equipment dumbbellSet = new Equipment("EQ007", "Dumbbell Set 5-50kg", "Strength (Free Weights)", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 410.0, 500.0);
        Equipment powerRack = new Equipment("EQ015", "Power Squat Rack", "Strength (Free Weights)", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 50.0, 2000.0);
        Equipment olympicBar = new Equipment("EQ016", "Olympic Barbell & Bumpers", "Strength (Free Weights)", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 20.0, 1000.0);
        Equipment kettlebellSet = new Equipment("EQ017", "Kettlebell Set", "Strength (Free Weights)", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 15.0, 1000.0);
        
        Equipment yogaMats = new Equipment("EQ006", "Yoga Mat Set (x20)", "Studio & Group", EquipmentStatus.OPERATIONAL, Location.STUDIO_B, 80.0, 1000.0);
        Equipment aerobicSteps = new Equipment("EQ018", "Aerobic Steps (x15)", "Studio & Group", EquipmentStatus.OPERATIONAL, Location.STUDIO_A, 40.0, 1000.0);
        Equipment pilatesReformer = new Equipment("EQ019", "Pilates Reformer", "Studio & Group", EquipmentStatus.OPERATIONAL, Location.STUDIO_B, 110.0, 500.0);
        
        Equipment plyoBox = new Equipment("EQ020", "Plyometric Box", "Functional", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 10.0, 1000.0);
        Equipment battleRopes = new Equipment("EQ021", "Battle Ropes", "Functional", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 25.0, 1000.0);
        Equipment trx = new Equipment("EQ022", "TRX Suspension Trainer", "Functional", EquipmentStatus.OPERATIONAL, Location.STUDIO_A, 5.0, 1000.0);
        
        Equipment foamRoller = new Equipment("EQ023", "Foam Roller Set", "Mobility & Recovery", EquipmentStatus.OPERATIONAL, Location.STUDIO_B, 10.0, 1000.0);
        Equipment massageGun = new Equipment("EQ024", "Percussive Massage Gun", "Mobility & Recovery", EquipmentStatus.OPERATIONAL, Location.STUDIO_B, 50.0, 200.0);

        try {
            equipment.save(treadmill1); equipment.save(treadmill2); equipment.save(spinBike1); equipment.save(spinBike2);
            equipment.save(rowingMachine); equipment.save(elliptical); equipment.save(stairClimber); equipment.save(airBike);
            equipment.save(cableMachine); equipment.save(legPress); equipment.save(latPulldown); equipment.save(smithMachine);
            equipment.save(dumbbellSet); equipment.save(powerRack); equipment.save(olympicBar); equipment.save(kettlebellSet);
            equipment.save(yogaMats); equipment.save(aerobicSteps); equipment.save(pilatesReformer);
            equipment.save(plyoBox); equipment.save(battleRopes); equipment.save(trx);
            equipment.save(foamRoller); equipment.save(massageGun);
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
        
        // --- Generate Extra Group Sessions ---
        String[] classNames = {"HIIT Blast", "Yoga Flow", "Spin Cycle", "Pilates Core", "CrossFit", "Zumba Party", "Aqua Aerobics", "Boxing Basics", "Stretching & Recovery"};
        String[] classTypes = {"HIIT", "YOGA", "SPIN", "PILATES", "CROSSFIT", "ZUMBA", "AQUA", "BOXING", "RECOVERY"};
        Location[] locations = Location.values();
        Instructor[] insts = {instructor1, instructor2};
        for (int i = 0; i < 50; i++) {
            DayOfWeek day = DayOfWeek.of((i % 7) + 1);
            int hour = 6 + (i % 14); 
            LocalTime start = LocalTime.of(hour, (i % 2) * 30);
            FitnessSession fs = new FitnessSession("SES_GEN_" + i, classNames[i % classNames.length] + " " + (i/10 + 1), classTypes[i % classTypes.length], insts[i % insts.length], day, start, start.plusHours(1), locations[i % locations.length], 15 + (i % 10));
            fs.setValidFrom(java.time.LocalDate.now().minusYears(1));
            try { sessions.save(fs); } catch (com.iwfc.exception.DuplicateDataException e) {}
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
        Notification n1 = new Notification("NTF0001", NotificationCategory.MAINTENANCE, "High Urgency Maintenance", "Cable machine fraying reported by Mike Torres.", null);
        Notification n2 = new Notification("NTF0002", NotificationCategory.BOOKING, "Booking Confirmed", "Your spot in Morning HIIT Blast is confirmed.", null);
        Notification n3 = new Notification("NTF0003", NotificationCategory.SYSTEM, "System Maintenance", "Scheduled downtime this Sunday at 2 AM for upgrades.", null);
        Notification n4 = new Notification("NTF0004", NotificationCategory.EQUIPMENT, "New Equipment Added", "20 new Yoga Mats have arrived.", null);
        Notification n5 = new Notification("NTF0005", NotificationCategory.SESSION, "New Session Type: Pilates Reformer Flow", "A new weekly advanced Pilates session with Instructor Sarah has been added. Focus on precision and control.", null);

        try {
            notifications.save(n1);
            notifications.save(n2);
            notifications.save(n3);
            notifications.save(n4);
            notifications.save(n5);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed notifications: " + e.getMessage());
        }





        // --- Sample Follow Ups ---
        try {
            followUps.save(new FollowUp("Archana", "852750000X", FollowUpType.TRIAL, LocalDateTime.of(2025, 12, 10, 15, 30), Convertibility.HOT, "Call Me, rewolution content, what tixt message, somementer..."));
            followUps.save(new FollowUp("Rahul Bajaj", "9876543210", FollowUpType.ANNIVERSARY, LocalDateTime.of(2025, 12, 10, 10, 0), Convertibility.WARM, "Send wishes"));
            followUps.save(new FollowUp("Heena khan", "98792X00XX", FollowUpType.ENQUIRY, LocalDateTime.of(2025, 12, 10, 14, 0), Convertibility.HOT, "Call Me, re rrecommenterd, about hira incidiing sration..."));
            followUps.save(new FollowUp("Roop kumar", "89000700XX", FollowUpType.FEEDBACK, LocalDateTime.of(2025, 12, 2, 11, 0), Convertibility.WARM, "Call Me"));
            followUps.save(new FollowUp("Sujal Doshi", "99622X00XX", FollowUpType.FEEDBACK, LocalDateTime.of(2025, 12, 2, 16, 0), Convertibility.COLD, "Call Me"));
            followUps.save(new FollowUp("Amit Kumar", "9123456789", FollowUpType.PARTY, LocalDateTime.of(2025, 12, 4, 18, 0), Convertibility.WARM, "Invite"));
            followUps.save(new FollowUp("Lisa Chen", "5551234567", FollowUpType.BALANCE_DUE, LocalDateTime.of(2025, 12, 15, 9, 0), Convertibility.HOT, "Follow up on overdue PT payment."));
            followUps.save(new FollowUp("Mike Torres", "5559876543", FollowUpType.MEMBERSHIP_RENEWAL, LocalDateTime.of(2025, 12, 20, 10, 0), Convertibility.HOT, "Annual renewal discussion."));
            followUps.save(new FollowUp("Sarah Johnson", "5557778888", FollowUpType.BIRTHDAY, LocalDateTime.of(2025, 12, 22, 12, 0), Convertibility.WARM, "Send birthday gift card."));
        } catch (com.iwfc.exception.DuplicateDataException e) {
            System.err.println("Failed to seed follow ups: " + e.getMessage());
        }
    }
}
