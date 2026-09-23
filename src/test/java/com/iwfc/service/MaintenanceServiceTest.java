package com.iwfc.service;

import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.EventListener;
import com.iwfc.pattern.NotificationService;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MaintenanceService.
 * Validates workflow transitions (PENDING → ASSIGNED → COMPLETED)
 * and Observer notification firing.
 */
class MaintenanceServiceTest {

    private MaintenanceService maintenanceService;
    private NotificationService notificationService;
    private List<String> receivedNotifications;

    @BeforeEach
    public void setUp() {
        DataStore.resetInstance();
        notificationService = new NotificationService();
        maintenanceService = new MaintenanceService(notificationService);

        // Register a test listener to capture notifications
        receivedNotifications = new ArrayList<>();
        notificationService.subscribe("MAINTENANCE_STATUS_CHANGED",
                (EventListener<String>) (eventType, data) -> receivedNotifications.add(data));
        notificationService.subscribe("MAINTENANCE_ALERT",
                (EventListener<String>) (eventType, data) -> receivedNotifications.add(data));
    }

    @Test
    @DisplayName("Reported issue should have PENDING status")
    void testReportIssueStatusIsPending() {
        Equipment eq = DataStore.getInstance().getEquipmentMap().get("EQ001");
        Instructor instructor = (Instructor) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("INS001"))
                .findFirst().orElseThrow();

        MaintenanceRequest request = new MaintenanceRequest(
                "MR_TEST", eq, instructor, "Test fault", Urgency.MEDIUM);

        maintenanceService.reportIssue(request);

        assertEquals(RequestStatus.PENDING, request.getStatus());
        assertFalse(receivedNotifications.isEmpty()); // MAINTENANCE_ALERT should fire
    }

    @Test
    @DisplayName("Assigning request should transition status to ASSIGNED")
    void testAssignRequestTransitionsToAssigned() {
        // MR001 is a PENDING request from seed data
        maintenanceService.assignRequest("MR001", "Tech John");

        MaintenanceRequest req = maintenanceService.getRequestById("MR001");
        assertEquals(RequestStatus.ASSIGNED, req.getStatus());
        assertEquals("Tech John", req.getAssignedTo());
    }

    @Test
    @DisplayName("Completing request should transition status to COMPLETED, save notes, and reset usage")
    void testCompleteRequestTransitionsToCompleted() {
        // MR002 is already ASSIGNED from seed data
        MaintenanceRequest req = maintenanceService.getRequestById("MR002");
        req.getEquipment().setUsageHours(50.0); // Set initial usage
        
        maintenanceService.completeRequest("MR002", "Fixed the motor");

        assertEquals(RequestStatus.COMPLETED, req.getStatus());
        assertNotNull(req.getCompletedDate());
        assertEquals("Fixed the motor", req.getResolutionNotes());
        assertEquals(0.0, req.getEquipment().getUsageHours(), "Usage hours should be reset");
        assertEquals(EquipmentStatus.OPERATIONAL, req.getEquipment().getStatus());
    }

    @Test
    @DisplayName("Observer notification fires on status change")
    void testObserverNotificationFires() {
        receivedNotifications.clear();

        maintenanceService.assignRequest("MR001", "Tech Alpha");

        assertFalse(receivedNotifications.isEmpty());
        assertTrue(receivedNotifications.get(0).contains("MR001"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException for invalid transition (PENDING → COMPLETED)")
    void testInvalidTransitionThrowsException() {
        // MR001 is PENDING — cannot be completed directly
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> maintenanceService.completeRequest("MR001", "Notes"));
        assertNotNull(exception);
    }

    @Test
    @DisplayName("isHighUrgency should correctly identify HIGH urgency requests")
    void testIsHighUrgency() {
        Equipment eq = DataStore.getInstance().getEquipmentMap().get("EQ001");
        Instructor instructor = (Instructor) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("INS001"))
                .findFirst().orElseThrow();

        MaintenanceRequest reqHigh = new MaintenanceRequest(
                "MR_HIGH", eq, instructor, "Test", Urgency.HIGH);
        MaintenanceRequest reqLow = new MaintenanceRequest(
                "MR_LOW", eq, instructor, "Test", Urgency.LOW);

        assertTrue(reqHigh.isHighUrgency());
        assertFalse(reqLow.isHighUrgency());
    }

    @Test
    @DisplayName("Should correctly filter HIGH urgency PENDING requests")
    void testGetHighUrgencyPendingRequests() {
        Equipment eq = DataStore.getInstance().getEquipmentMap().get("EQ001");
        Instructor instructor = (Instructor) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("INS001"))
                .findFirst().orElseThrow();

        MaintenanceRequest pendingHigh = new MaintenanceRequest("MR_H1", eq, instructor, "T1", Urgency.HIGH);
        MaintenanceRequest pendingLow = new MaintenanceRequest("MR_L1", eq, instructor, "T2", Urgency.LOW);
        
        MaintenanceRequest assignedHigh = new MaintenanceRequest("MR_H2", eq, instructor, "T3", Urgency.HIGH);
        assignedHigh.assign("Tech");

        DataStore.getInstance().getMaintenanceRequests().add(pendingHigh);
        DataStore.getInstance().getMaintenanceRequests().add(pendingLow);
        DataStore.getInstance().getMaintenanceRequests().add(assignedHigh);

        List<MaintenanceRequest> highPending = maintenanceService.getHighUrgencyPendingRequests();

        assertTrue(highPending.contains(pendingHigh));
        assertFalse(highPending.contains(pendingLow));
        assertFalse(highPending.contains(assignedHigh));
    }

    @Test
    @DisplayName("Should successfully reopen a completed request")
    void testReopenRequest() {
        MaintenanceRequest req = maintenanceService.getRequestById("MR002");
        maintenanceService.completeRequest("MR002", "Done");
        
        maintenanceService.reopenRequest("MR002");
        
        assertEquals(RequestStatus.PENDING, req.getStatus());
        assertNull(req.getAssignedTo());
        assertNull(req.getCompletedDate());
        assertEquals("", req.getResolutionNotes());
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, req.getEquipment().getStatus());
    }
}
