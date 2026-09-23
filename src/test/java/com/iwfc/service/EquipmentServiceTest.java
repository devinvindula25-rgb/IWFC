package com.iwfc.service;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EquipmentService.
 */
class EquipmentServiceTest {

    private EquipmentService equipmentService;
    private NotificationService notificationService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        DataStore.resetInstance();
        notificationService = new NotificationService();
        equipmentService = new EquipmentService(notificationService);
        testUser = DataStore.getInstance().getUsers().get(0);
    }

    @Test
    @DisplayName("Should successfully add new equipment")
    void testAddEquipment() throws DuplicateDataException {
        Equipment newEq = new Equipment("EQ100", "New Treadmill", "Treadmill", Location.CARDIO_ZONE);
        equipmentService.addEquipment(newEq);
        
        Equipment retrieved = equipmentService.getEquipmentById("EQ100");
        assertNotNull(retrieved);
        assertEquals("New Treadmill", retrieved.getName());
    }

    @Test
    @DisplayName("Should throw DuplicateDataException on duplicate equipment ID")
    void testDuplicateEquipment() {
        Equipment eq1 = new Equipment("EQ200", "Eq 1", "Category A", Location.STUDIO_A);
        Equipment eq2 = new Equipment("EQ200", "Eq 2", "Category B", Location.STUDIO_A);
        
        assertDoesNotThrow(() -> equipmentService.addEquipment(eq1));
        
        Exception e = assertThrows(DuplicateDataException.class, () -> equipmentService.addEquipment(eq2));
        assertNotNull(e);
    }

    @Test
    @DisplayName("Should edit equipment attributes")
    void testEditEquipment() throws DuplicateDataException {
        Equipment eq = new Equipment("EQ300", "Old Name", "Old Category", Location.POOL_AREA);
        equipmentService.addEquipment(eq);
        
        equipmentService.editEquipment("EQ300", "New Name", "New Category", EquipmentStatus.FAULTY, Location.WEIGHT_ROOM, 1000.0);
        
        Equipment updated = equipmentService.getEquipmentById("EQ300");
        assertEquals("New Name", updated.getName());
        assertEquals(EquipmentStatus.FAULTY, updated.getStatus());
        assertEquals(Location.WEIGHT_ROOM, updated.getLocation());
    }

    @Test
    @DisplayName("Should deactivate equipment")
    void testDeactivateEquipment() throws DuplicateDataException {
        Equipment eq = new Equipment("EQ400", "To Deactivate", "Category", Location.STUDIO_B);
        equipmentService.addEquipment(eq);
        
        equipmentService.deactivateEquipment("EQ400");
        
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, equipmentService.getEquipmentById("EQ400").getStatus());
    }

    @Test
    @DisplayName("Should log usage and trigger alert if threshold exceeded")
    void testLogUsageAndAlert() throws DuplicateDataException {
        Equipment eq = new Equipment("EQ500", "Usage Test", "Category", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 0, 10);
        equipmentService.addEquipment(eq);
        
        // Log 5 hours, no alert expected (but we can't easily assert on standard out, we just ensure no exception)
        equipmentService.logUsage("EQ500", 5.0, testUser);
        assertEquals(5.0, eq.getUsageHours());
        assertEquals(1, eq.getUsageLog().size());
        
        // Log another 6 hours, crosses threshold (11 > 10)
        equipmentService.logUsage("EQ500", 6.0, testUser);
        assertEquals(11.0, eq.getUsageHours());
        assertEquals(2, eq.getUsageLog().size());
        assertTrue(eq.needsMaintenance());
    }

    @Test
    @DisplayName("Should remove equipment if no active sessions or pending maintenance")
    void testRemoveEquipmentSuccess() throws DuplicateDataException {
        Equipment eq = new Equipment("EQ600", "To Remove", "Category", Location.STUDIO_A);
        equipmentService.addEquipment(eq);
        
        equipmentService.removeEquipment("EQ600");
        
        Exception e = assertThrows(IllegalArgumentException.class, () -> equipmentService.getEquipmentById("EQ600"));
        assertNotNull(e);
    }

    @Test
    @DisplayName("Should block removal if equipment used in active session")
    void testRemoveEquipmentBlockedBySession() throws DuplicateDataException {
        Equipment eq = new Equipment("EQ700", "Required Eq", "Category", Location.STUDIO_A);
        equipmentService.addEquipment(eq);
        
        FitnessSession session = new FitnessSession("SES_TEST", "Test Session", "HIIT", null, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 10);
        session.getRequiredEquipment().add(eq);
        DataStore.getInstance().getSessionMap().put(session.getSessionId(), session);
        
        Exception e = assertThrows(IllegalStateException.class, () -> equipmentService.removeEquipment("EQ700"));
        assertNotNull(e);
    }

    @Test
    @DisplayName("Should block removal if equipment has pending maintenance")
    void testRemoveEquipmentBlockedByMaintenance() throws DuplicateDataException {
        Equipment eq = new Equipment("EQ800", "Broken Eq", "Category", Location.STUDIO_A);
        equipmentService.addEquipment(eq);
        
        MaintenanceRequest request = new MaintenanceRequest("MR999", eq, null, "Broken", Urgency.HIGH);
        DataStore.getInstance().getMaintenanceRequests().add(request);
        
        Exception e = assertThrows(IllegalStateException.class, () -> equipmentService.removeEquipment("EQ800"));
        assertNotNull(e);
    }

    @Test
    @DisplayName("Should filter equipment by category")
    void testGetByCategory() throws DuplicateDataException {
        Equipment eq1 = new Equipment("EQ901", "Eq 1", "CatA", Location.STUDIO_A);
        Equipment eq2 = new Equipment("EQ902", "Eq 2", "CatA", Location.STUDIO_B);
        Equipment eq3 = new Equipment("EQ903", "Eq 3", "CatB", Location.WEIGHT_ROOM);
        
        equipmentService.addEquipment(eq1);
        equipmentService.addEquipment(eq2);
        equipmentService.addEquipment(eq3);
        
        java.util.List<Equipment> catA = equipmentService.getByCategory("CatA");
        assertEquals(2, catA.size());
        
        java.util.List<Equipment> catB = equipmentService.getByCategory("CatB");
        assertEquals(1, catB.size());
    }

    @Test
    @DisplayName("Should return correct summary stats")
    void testGetEquipmentSummary() throws DuplicateDataException {
        // DataStore pre-populates some data. Let's add some more explicitly to test exactly.
        Equipment eq1 = new Equipment("EQ911", "Eq 1", "CatA", EquipmentStatus.OPERATIONAL, Location.STUDIO_A, 0, 500);
        Equipment eq2 = new Equipment("EQ912", "Eq 2", "CatA", EquipmentStatus.FAULTY, Location.STUDIO_A, 0, 500);
        Equipment eq3 = new Equipment("EQ913", "Eq 3", "CatB", EquipmentStatus.UNDER_MAINTENANCE, Location.STUDIO_A, 0, 500);
        
        equipmentService.addEquipment(eq1);
        equipmentService.addEquipment(eq2);
        equipmentService.addEquipment(eq3);
        
        java.util.Map<EquipmentStatus, Long> summary = equipmentService.getEquipmentSummary();
        assertNotNull(summary);
        assertTrue(summary.getOrDefault(EquipmentStatus.OPERATIONAL, 0L) > 0);
        assertTrue(summary.getOrDefault(EquipmentStatus.FAULTY, 0L) > 0);
        assertTrue(summary.getOrDefault(EquipmentStatus.UNDER_MAINTENANCE, 0L) > 0);
    }
}
