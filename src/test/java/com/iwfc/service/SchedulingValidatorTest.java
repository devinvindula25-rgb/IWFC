package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.Booking;
import com.iwfc.model.BookingStatus;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.Instructor;
import com.iwfc.model.Location;
import com.iwfc.model.Member;
import com.iwfc.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingValidatorTest {

    private SchedulingValidator validator;
    private List<FitnessSession> existingSessions;
    private Instructor testInstructor1;
    private Instructor testInstructor2;

    @BeforeEach
    void setUp() {
        validator = new SchedulingValidator();
        existingSessions = new ArrayList<>();
        testInstructor1 = new Instructor("INS1", "Inst 1", "email1", "phone1", "pic1", "pw1", "Yoga");
        testInstructor2 = new Instructor("INS2", "Inst 2", "email2", "phone2", "pic2", "pw2", "Pilates");
    }

    private void validateSession(FitnessSession session, List<FitnessSession> existing) throws InvalidBookingException {
        TimeSlot slot = new TimeSlot(
            session.getValidFrom().atTime(session.getStartTime()),
            session.getValidFrom().atTime(session.getEndTime())
        );
        validator.validateOperatingHours(slot);
        validator.validateNoRoomClash(session, existing);
        validator.validateNoInstructorClash(session, existing);
    }

    @Test
    void testValidBookingIntoEmptySlot() {
        FitnessSession newSession = new FitnessSession(
                "S1", "Yoga", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);

        assertDoesNotThrow(() -> validateSession(newSession, existingSessions));
    }

    @Test
    void testBackToBackBookingSucceeds() {
        FitnessSession existing = new FitnessSession(
                "S1", "Yoga", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);
        existingSessions.add(existing);

        FitnessSession newSession = new FitnessSession(
                "S2", "Pilates", "Pilates", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(11, 0), LocalTime.of(12, 0), Location.STUDIO_A, 20);

        assertDoesNotThrow(() -> validateSession(newSession, existingSessions));
    }

    @Test
    void testExactSameTimeRoomClashThrowsException() {
        FitnessSession existing = new FitnessSession(
                "S1", "Yoga", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);
        existingSessions.add(existing);

        FitnessSession newSession = new FitnessSession(
                "S2", "Pilates", "Pilates", testInstructor2, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validateSession(newSession, existingSessions));
        assertTrue(ex.getMessage().contains("Schedule conflict: Room"));
    }

    @Test
    void testPartialOverlapClashThrowsException() {
        FitnessSession existing = new FitnessSession(
                "S1", "Yoga", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 30), Location.STUDIO_A, 20);
        existingSessions.add(existing);

        FitnessSession newSession = new FitnessSession(
                "S2", "Pilates", "Pilates", testInstructor2, DayOfWeek.MONDAY,
                LocalTime.of(11, 0), LocalTime.of(12, 0), Location.STUDIO_A, 20);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validateSession(newSession, existingSessions));
        assertTrue(ex.getMessage().contains("Schedule conflict: Room"));
    }

    @Test
    void testSameInstructorDifferentRoomClashThrowsException() {
        FitnessSession existing = new FitnessSession(
                "S1", "Yoga", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);
        existingSessions.add(existing);

        FitnessSession newSession = new FitnessSession(
                "S2", "Pilates", "Pilates", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_B, 20);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validateSession(newSession, existingSessions));
        assertTrue(ex.getMessage().contains("Schedule conflict: Instructor"));
    }

    @Test
    void testBookingBeforeOperatingHoursThrowsException() {
        FitnessSession newSession = new FitnessSession(
                "S1", "Early Bird", "HIIT", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(5, 0), LocalTime.of(6, 0), Location.STUDIO_A, 20);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validateSession(newSession, existingSessions));
        assertTrue(ex.getMessage().contains("operating hours"));
    }

    @Test
    void testBookingAfterOperatingHoursThrowsException() {
        FitnessSession newSession = new FitnessSession(
                "S1", "Night Owl", "HIIT", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(21, 30), LocalTime.of(22, 30), Location.STUDIO_A, 20);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validateSession(newSession, existingSessions));
        assertTrue(ex.getMessage().contains("operating hours"));
    }

    @Test
    void testInvalidTimeRangeThrowsException() {
        FitnessSession newSession = new FitnessSession(
                "S1", "Time Travel", "HIIT", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(11, 0), LocalTime.of(10, 0), Location.STUDIO_A, 20);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateSession(newSession, existingSessions));
        assertTrue(ex.getMessage().contains("before end time"));
    }

    @Test
    void testValidOperatingHoursDoesNotThrow() {
        TimeSlot validSlot = new TimeSlot(
            LocalDate.now().atTime(10, 0),
            LocalDate.now().atTime(11, 0)
        );
        assertDoesNotThrow(() -> validator.validateOperatingHours(validSlot));
    }

    @Test
    void testOverlapsMethodReturnsFalseForDistinctTimes() {
        TimeSlot slot1 = new TimeSlot(
            LocalDate.now().atTime(10, 0),
            LocalDate.now().atTime(11, 0)
        );
        TimeSlot slot2 = new TimeSlot(
            LocalDate.now().atTime(11, 0),
            LocalDate.now().atTime(12, 0)
        );
        
        // They touch at 11:00 but do not overlap
        assertFalse(validator.overlaps(slot1, slot2));
    }

    @Test
    void testMemberCannotDoubleBookExactSameSessionOnSameDate() {
        Member member = new Member("MEM1", "James Wilson", "jw@test.com", "555-1234", "12 Maple St", "pic.jpg", "Premium");
        FitnessSession session = new FitnessSession("S1", "Yoga Flow", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);
        LocalDate targetDate = LocalDate.now().plusDays(7);

        Booking existingBooking = new Booking("B1", member, session, targetDate, LocalDateTime.now());
        List<Booking> bookings = List.of(existingBooking);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validator.validateNoMemberDoubleBooking(member, session, targetDate, bookings));
        assertTrue(ex.getMessage().contains("Double-booking prevented"));
        assertTrue(ex.getMessage().contains("already booked for session 'Yoga Flow'"));
    }

    @Test
    void testMemberCannotBookOverlappingSessionOnSameDate() {
        Member member = new Member("MEM1", "James Wilson", "jw@test.com", "555-1234", "12 Maple St", "pic.jpg", "Premium");
        FitnessSession session1 = new FitnessSession("S1", "Morning HIIT", "HIIT", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 30), Location.STUDIO_A, 20);
        FitnessSession session2 = new FitnessSession("S2", "Spin Cycle", "Spin", testInstructor2, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_B, 20);
        LocalDate targetDate = LocalDate.now().plusDays(7);

        Booking existingBooking = new Booking("B1", member, session1, targetDate, LocalDateTime.now());
        List<Booking> bookings = List.of(existingBooking);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validator.validateNoMemberDoubleBooking(member, session2, targetDate, bookings));
        assertTrue(ex.getMessage().contains("Schedule conflict: Member 'James Wilson' is already booked for overlapping session"));
        assertTrue(ex.getMessage().contains("Double-booking is physically impossible"));
    }

    @Test
    void testMemberCanBookBackToBackNonOverlappingSessions() {
        Member member = new Member("MEM1", "James Wilson", "jw@test.com", "555-1234", "12 Maple St", "pic.jpg", "Premium");
        FitnessSession session1 = new FitnessSession("S1", "Morning HIIT", "HIIT", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), Location.STUDIO_A, 20);
        FitnessSession session2 = new FitnessSession("S2", "Yoga Flow", "Yoga", testInstructor2, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_B, 20);
        LocalDate targetDate = LocalDate.now().plusDays(7);

        Booking existingBooking = new Booking("B1", member, session1, targetDate, LocalDateTime.now());
        List<Booking> bookings = List.of(existingBooking);

        assertDoesNotThrow(() -> validator.validateNoMemberDoubleBooking(member, session2, targetDate, bookings));
    }

    @Test
    void testMemberCanRebookIfPreviousBookingWasCancelled() {
        Member member = new Member("MEM1", "James Wilson", "jw@test.com", "555-1234", "12 Maple St", "pic.jpg", "Premium");
        FitnessSession session = new FitnessSession("S1", "Yoga Flow", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);
        LocalDate targetDate = LocalDate.now().plusDays(7);

        Booking cancelledBooking = new Booking("B1", member, session, targetDate, LocalDateTime.now());
        cancelledBooking.cancel();
        List<Booking> bookings = List.of(cancelledBooking);

        assertDoesNotThrow(() -> validator.validateNoMemberDoubleBooking(member, session, targetDate, bookings));
    }

    @Test
    void testValidateBookingRejectsNullsAndPastDates() {
        Member member = new Member("MEM1", "James Wilson", "jw@test.com", "555-1234", "12 Maple St", "pic.jpg", "Premium");
        FitnessSession session = new FitnessSession("S1", "Yoga Flow", "Yoga", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 20);

        assertThrows(InvalidBookingException.class, () -> validator.validateBooking(null, session, LocalDate.now(), List.of()));
        assertThrows(InvalidBookingException.class, () -> validator.validateBooking(member, null, LocalDate.now(), List.of()));
        assertThrows(InvalidBookingException.class, () -> validator.validateBooking(member, session, null, List.of()));
        assertThrows(InvalidBookingException.class, () -> validator.validateBooking(member, session, LocalDate.now().minusDays(1), List.of()));
    }

    @Test
    void testValidateBookingRejectsFullCapacitySession() {
        Member member1 = new Member("MEM1", "James Wilson", "jw@test.com", "555-1234", "12 Maple St", "pic.jpg", "Premium");
        Member member2 = new Member("MEM2", "Emma Davis", "ed@test.com", "555-5678", "88 Oak Lane", "pic.jpg", "Standard");
        
        FitnessSession session = new FitnessSession("S1", "Personal Training", "PT", testInstructor1, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), Location.STUDIO_A, 1);
        
        // Find next Monday
        LocalDate nextMonday = LocalDate.now();
        while (nextMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            nextMonday = nextMonday.plusDays(1);
        }

        Booking existing = new Booking("B1", member1, session, nextMonday, LocalDateTime.now());
        List<Booking> bookings = List.of(existing);

        LocalDate finalDate = nextMonday;
        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> validator.validateBooking(member2, session, finalDate, bookings));
        assertTrue(ex.getMessage().contains("fully booked"));
    }
}
