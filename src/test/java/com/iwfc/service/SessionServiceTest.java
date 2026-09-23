package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionService.
 * Validates booking logic, double-booking prevention, and operating hours enforcement.
 */
class SessionServiceTest {

    private SessionService sessionService;
    private NotificationService notificationService;
    private Member testMember;
    private FitnessSession session1;

    @BeforeEach
    public void setUp() {
        // Reset DataStore completely to ensure fresh state for all tests
        com.iwfc.pattern.DataStore.resetInstance();
        
        notificationService = new NotificationService();
        sessionService = new SessionService(notificationService);
        testMember = (Member) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("MEM001"))
                .findFirst().orElseThrow();
        session1 = sessionService.getSessionById("SES001");
    }

    @Test
    void testBookSessionForDate() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(7);
        while (futureDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            futureDate = futureDate.plusDays(1);
        }
        
        Booking booking = sessionService.bookSessionForDate(testMember, session1.getSessionId(), futureDate);

        assertNotNull(booking);
        assertEquals(testMember, booking.getMember());
        assertEquals(session1, booking.getSession());
        assertEquals(futureDate, booking.getSessionDate());

        List<Booking> memberBookings = sessionService.getBookingsForMember(testMember.getUserId());
        assertEquals(3, memberBookings.size()); // 2 seeded bookings + 1 new booking
    }

    @Test
    void testBookSessionForPastDateThrows() {
        LocalDate pastDate = LocalDate.now().minusDays(7);
        while (pastDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            pastDate = pastDate.minusDays(1);
        }
        final LocalDate finalPastDate = pastDate;
        
        assertThrows(InvalidBookingException.class, () -> {
            sessionService.bookSessionForDate(testMember, session1.getSessionId(), finalPastDate);
        });
    }

    @Test
    void testDuplicateDateBookingThrows() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(7);
        while (futureDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            futureDate = futureDate.plusDays(1);
        }
        final LocalDate finalFutureDate = futureDate;
        
        sessionService.bookSessionForDate(testMember, session1.getSessionId(), finalFutureDate);

        assertThrows(InvalidBookingException.class, () -> {
            sessionService.bookSessionForDate(testMember, session1.getSessionId(), finalFutureDate); // Duplicate on same date
        });
        
        // But booking on a different date should work
        LocalDate anotherDate = futureDate.plusWeeks(1);
        assertDoesNotThrow(() -> {
            sessionService.bookSessionForDate(testMember, session1.getSessionId(), anotherDate);
        });
    }

    @Test
    void testCapacityPerDate() throws Exception {
        session1.setMaxCapacity(2);
        
        LocalDate futureDate = LocalDate.now().plusDays(7);
        while (futureDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            futureDate = futureDate.plusDays(1);
        }
        final LocalDate finalFutureDate = futureDate;

        Member member2 = new Member("U002", "Jane Doe", "jane@test.com", "pass");
        Member member3 = new Member("U003", "Bob Smith", "bob@test.com", "pass");
        
        sessionService.bookSessionForDate(testMember, session1.getSessionId(), finalFutureDate);
        sessionService.bookSessionForDate(member2, session1.getSessionId(), finalFutureDate);
        
        // Third booking on same date should fail
        assertThrows(InvalidBookingException.class, () -> {
            sessionService.bookSessionForDate(member3, session1.getSessionId(), finalFutureDate);
        });
        
        // Third booking on a DIFFERENT date should succeed
        LocalDate anotherDate = futureDate.plusWeeks(1);
        assertDoesNotThrow(() -> {
            sessionService.bookSessionForDate(member3, session1.getSessionId(), anotherDate);
        });
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
        // By default, sessions should be available if no bookings exist
        List<FitnessSession> available = sessionService.getAvailableSessions();
        assertFalse(available.isEmpty());

        FitnessSession session = available.get(0);
        session.setMaxCapacity(0); // Make it full

        List<FitnessSession> availableAfter = sessionService.getAvailableSessions();
        assertTrue(availableAfter.size() < available.size());
    }

    @Test
    @DisplayName("Should throw InvalidBookingException for session conflict with same instructor at same time")
    void testInstructorConflictThrowsException() {
        Instructor instructor = (Instructor) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("INS001"))
                .findFirst().orElseThrow();

        // SES001 is Monday 07:00-08:00 with INS001 at STUDIO_A
        // Create conflicting session with SAME instructor but DIFFERENT location
        FitnessSession conflicting = new FitnessSession(
                "SES_CONFLICT_INST", "Conflicting Session", "HIIT",
                instructor, DayOfWeek.MONDAY,
                LocalTime.of(7, 30), LocalTime.of(8, 30),
                Location.STUDIO_B, 10);

        InvalidBookingException exception = assertThrows(
                InvalidBookingException.class,
                () -> sessionService.createSession(conflicting)
        );

        assertTrue(exception.getMessage().contains("conflict"));
    }

    @Test
    void testCancelBookingNotOwned() throws Exception {
        Member member1 = (Member) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("MEM001"))
                .findFirst().orElseThrow();
        Member member2 = (Member) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("MEM002"))
                .findFirst().orElseThrow();
                
        LocalDate futureDate = LocalDate.now().plusDays(7);
        while (futureDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            futureDate = futureDate.plusDays(1);
        }
        Booking booking = sessionService.bookSessionForDate(member1, "SES001", futureDate);
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sessionService.cancelBooking(booking.getBookingId(), member2.getUserId())
        );
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("own bookings"));
    }

    @Test
    @DisplayName("Should successfully cancel an entire session and all its bookings")
    void testCancelSession() throws Exception {
        Member member = (Member) DataStore.getInstance().getUsers().stream()
                .filter(u -> u.getUserId().equals("MEM001"))
                .findFirst().orElseThrow();
                
        LocalDate futureDate = LocalDate.now().plusDays(7);
        while (futureDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            futureDate = futureDate.plusDays(1);
        }
        
        Booking booking = sessionService.bookSessionForDate(member, "SES001", futureDate);
        
        sessionService.cancelSession("SES001");
        
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        Exception e = assertThrows(IllegalArgumentException.class, () -> sessionService.getSessionById("SES001"));
        assertNotNull(e);
    }

    @Test
    @DisplayName("Should successfully edit a session")
    void testEditSession() throws InvalidBookingException {
        FitnessSession session = sessionService.getSessionById("SES002");
        LocalTime newStart = LocalTime.of(12, 0);
        LocalTime newEnd = LocalTime.of(13, 0);
        sessionService.editSession("SES002", "New Name", newStart, newEnd, 25);
        
        assertEquals("New Name", session.getName());
        assertEquals(newStart, session.getStartTime());
        assertEquals(newEnd, session.getEndTime());
        assertEquals(25, session.getMaxCapacity());
    }

    @Test
    @DisplayName("Should correctly handle recurring fields")
    void testRecurringFields() {
        FitnessSession session = sessionService.getSessionById("SES003");
        assertTrue(session.isRecurring());
        assertNotNull(session.getRecurrenceNote());
        assertTrue(session.getRecurrenceNote().startsWith("Every "));
        
        session.setRecurring(false);
        assertFalse(session.isRecurring());
    }
}
