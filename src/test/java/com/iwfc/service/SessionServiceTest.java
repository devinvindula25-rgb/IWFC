package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionService.
 * Validates booking logic, double-booking prevention, and operating hours enforcement.
 */
class SessionServiceTest {

    private SessionService sessionService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        DataStore.resetInstance();
        notificationService = new NotificationService();
        sessionService = new SessionService(notificationService);
    }

    @Test
    @DisplayName("Should successfully book a session for a member")
    void testSuccessfulBooking() throws InvalidBookingException {
        // Get a session and a member from seed data
        FitnessSession session = sessionService.getSessionById("SES001");
        Member member = (Member) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("MEM001"))
                .findFirst().orElseThrow();

        Booking booking = sessionService.bookSession(member, "SES001");

        assertNotNull(booking);
        assertEquals("Confirmed", booking.getStatus());
        assertEquals(member, booking.getMember());
        assertEquals(session, booking.getSession());
    }

    @Test
    @DisplayName("Should throw InvalidBookingException on double-booking same session")
    void testDoubleBookingThrowsException() throws InvalidBookingException {
        Member member = (Member) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("MEM001"))
                .findFirst().orElseThrow();

        // First booking should succeed
        sessionService.bookSession(member, "SES001");

        // Second booking for same member + session should fail
        InvalidBookingException exception = assertThrows(
                InvalidBookingException.class,
                () -> sessionService.bookSession(member, "SES001")
        );

        assertTrue(exception.getMessage().contains("already booked"));
    }

    @Test
    @DisplayName("Should throw InvalidBookingException for session conflict at same location and time")
    void testConflictingSessionThrowsException() {
        Instructor instructor = (Instructor) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("INS001"))
                .findFirst().orElseThrow();

        // SES001 is Monday 07:00-08:00 at STUDIO_A
        // Try to create a conflicting session at same time and location
        FitnessSession conflicting = new FitnessSession(
                "SES_CONFLICT", "Conflicting Session", "HIIT",
                instructor, DayOfWeek.MONDAY,
                LocalTime.of(7, 30), LocalTime.of(8, 30),
                Location.STUDIO_A, 10);

        InvalidBookingException exception = assertThrows(
                InvalidBookingException.class,
                () -> sessionService.createSession(conflicting)
        );

        assertTrue(exception.getMessage().contains("conflict"));
    }

    @Test
    @DisplayName("Should throw InvalidBookingException for session outside operating hours")
    void testOutsideOperatingHoursThrowsException() {
        InvalidBookingException exception = assertThrows(
                InvalidBookingException.class,
                () -> sessionService.validateOperatingHours(
                        LocalTime.of(5, 0), LocalTime.of(6, 0))
        );

        assertTrue(exception.getMessage().contains("operating hours"));
    }

    @Test
    @DisplayName("Should return available sessions with open capacity")
    void testGetAvailableSessions() {
        var available = sessionService.getAvailableSessions();
        assertFalse(available.isEmpty());
        // All seed sessions should have capacity since no bookings exist yet
        assertEquals(sessionService.getAllSessions().size(), available.size());
    }
}
