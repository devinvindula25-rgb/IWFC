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
    void setUp() {
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
    @DisplayName("Completing request should transition status to COMPLETED")
    void testCompleteRequestTransitionsToCompleted() {
        // MR002 is already ASSIGNED from seed data
        maintenanceService.completeRequest("MR002");

        MaintenanceRequest req = maintenanceService.getRequestById("MR002");
        assertEquals(RequestStatus.COMPLETED, req.getStatus());
        assertNotNull(req.getCompletedDate());
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
        assertThrows(IllegalStateException.class,
                () -> maintenanceService.completeRequest("MR001"));
    }
}
