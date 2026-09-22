package com.iwfc.exception;

import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;
import com.iwfc.service.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for custom exception handling.
 * Includes one INTENTIONAL FAILING TEST to demonstrate that exceptions
 * are correctly thrown under error conditions.
 */
class ExceptionHandlingTest {

    @BeforeEach
    public void setUp() {
        DataStore.resetInstance();
    }

    @Test
    @DisplayName("DuplicateDataException thrown when adding equipment with existing ID")
    void testDuplicateEquipmentThrowsException() {
        NotificationService ns = new NotificationService();
        EquipmentService service = new EquipmentService(ns);

        // EQ001 already exists in seed data
        Equipment duplicate = new Equipment("EQ001", "Duplicate Treadmill", Location.CARDIO_ZONE);

        DuplicateDataException exception = assertThrows(
                DuplicateDataException.class,
                () -> service.addEquipment(duplicate)
        );

        assertTrue(exception.getMessage().contains("EQ001"));
    }

    @Test
    @DisplayName("UnauthorizedAccessException thrown when Member accesses admin functions")
    void testUnauthorizedMemberThrowsException() {
        UserService userService = new UserService();
        User member = userService.login("MEM001");

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> userService.checkAuthorization(member, Role.ADMIN)
        );

        assertTrue(exception.getMessage().contains("Access denied"));
        assertTrue(exception.getMessage().contains("Administrator"));
    }

    /**
     * ❌ INTENTIONAL FAILING TEST
     *
     * This test deliberately ASSERTS that booking a conflicting session
     * does NOT throw an exception. However, the system correctly throws
     * InvalidBookingException for conflicts.
     *
     * Purpose: Demonstrates that our custom exception handling is working
     * as designed — the test fails because the exception IS thrown,
     * proving the validation logic is sound.
     *
     * Required by the assignment: "Include at least one intentional failing
     * test to verify that your custom exceptions are correctly thrown and
     * handled when error conditions are met."
     */
    @Test
    @DisplayName("INTENTIONAL FAIL — Asserts conflicting booking does NOT throw (it should)")
    void testIntentionalFail_ConflictingBookingShouldNotThrow() {
        NotificationService ns = new NotificationService();
        SessionService sessionService = new SessionService(ns);

        Instructor instructor = (Instructor) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("INS001"))
                .findFirst().orElseThrow();

        // SES001 is Monday 07:00-08:00 at STUDIO_A
        // Creating a directly conflicting session at same time and location
        FitnessSession conflicting = new FitnessSession(
                "SES_FAIL", "Intentional Conflict", "HIIT",
                instructor, java.time.DayOfWeek.MONDAY,
                java.time.LocalTime.of(7, 0), java.time.LocalTime.of(8, 0),
                Location.STUDIO_A, 10);

        // This DELIBERATELY asserts no exception is thrown.
        // It WILL FAIL because InvalidBookingException IS correctly thrown.
        assertDoesNotThrow(() -> sessionService.createSession(conflicting),
            "This test intentionally fails to prove that InvalidBookingException " +
            "is correctly thrown for scheduling conflicts.");
    }
}
