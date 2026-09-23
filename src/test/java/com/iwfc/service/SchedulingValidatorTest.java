package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.Instructor;
import com.iwfc.model.Location;
import com.iwfc.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
}
