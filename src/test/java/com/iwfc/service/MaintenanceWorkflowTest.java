package com.iwfc.service;

import com.iwfc.model.*;
import com.iwfc.pattern.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceWorkflowTest {

    private MaintenanceService maintenanceService;
    private NotificationService mockNotificationService;
    private Equipment testEquipment;
    private Instructor testInstructor;
    private int notificationCount = 0;

    @BeforeEach
    void setUp() {
        notificationCount = 0;
        
        // Setup a simple spy-like NotificationService to count invocations
        mockNotificationService = new NotificationService() {
            @Override
            public void notifyListeners(String eventType, String message) {
                notificationCount++;
            }
        };

        maintenanceService = new MaintenanceService(mockNotificationService);
        testEquipment = new Equipment("EQ1", "Treadmill", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 100.0, 500.0);
        testEquipment.addUsageHours(150); // Cross threshold
        testInstructor = new Instructor("INS1", "John Doe", "john@example.com", "555-1234", "john_pic", "123", "Fitness");
    }

    @Test
    void testValidWorkflowTransitions() {
        String reqId = "REQ1_" + System.currentTimeMillis();
        // 1. Report issue (PENDING)
        MaintenanceRequest req = new MaintenanceRequest(reqId, testEquipment, testInstructor, "Broken belt", Urgency.HIGH);
        maintenanceService.reportIssue(req);
        
        assertEquals(RequestStatus.PENDING, req.getStatus());
        assertEquals(EquipmentStatus.FAULTY, testEquipment.getStatus());
        assertEquals(1, notificationCount); // "MAINTENANCE_REPORTED"

        // 2. Assign issue (ASSIGNED)
        maintenanceService.assignRequest(reqId, "Tech Bob");
        assertEquals(RequestStatus.ASSIGNED, req.getStatus());
        assertEquals("Tech Bob", req.getAssignedTo());
        assertEquals(2, notificationCount); // "MAINTENANCE_UPDATED"

        // 3. Complete issue (COMPLETED)
        maintenanceService.completeRequest(reqId, "Replaced belt");
        assertEquals(RequestStatus.COMPLETED, req.getStatus());
        assertEquals("Replaced belt", req.getResolutionNotes());
        assertEquals(EquipmentStatus.OPERATIONAL, testEquipment.getStatus());
        assertEquals(0, testEquipment.getCumulativeUsageHours()); // Verify reset
        assertEquals(3, notificationCount); // "MAINTENANCE_COMPLETED"
    }

    @Test
    void testInvalidTransitionPendingToCompletedThrowsException() {
        String reqId = "REQ2_" + System.currentTimeMillis();
        MaintenanceRequest req = new MaintenanceRequest(reqId, testEquipment, testInstructor, "Broken belt", Urgency.HIGH);
        maintenanceService.reportIssue(req);

        // Attempt to skip ASSIGNED state
        IllegalStateException ex = assertThrows(IllegalStateException.class, 
            () -> maintenanceService.completeRequest(reqId, "Replaced belt"));
        
        assertTrue(ex.getMessage().contains("Cannot complete request"));
    }

    @Test
    void testInvalidTransitionCompletedToPendingThrowsException() {
        String reqId = "REQ3_" + System.currentTimeMillis();
        MaintenanceRequest req = new MaintenanceRequest(reqId, testEquipment, testInstructor, "Broken belt", Urgency.HIGH);
        maintenanceService.reportIssue(req);
        maintenanceService.assignRequest(reqId, "Tech Bob");
        maintenanceService.completeRequest(reqId, "Replaced belt");

        // Attempt to complete a completed request again
        IllegalStateException ex = assertThrows(IllegalStateException.class, 
            () -> maintenanceService.completeRequest(reqId, "Replaced belt again"));
            
        assertTrue(ex.getMessage().contains("Cannot complete request"));
    }
}
