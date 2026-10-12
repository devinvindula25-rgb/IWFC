package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.Booking;
import com.iwfc.model.BookingStatus;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.Member;
import com.iwfc.model.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Validates scheduling rules. 
 * Designed to be dependency-free for easy unit testing in Phase 5.
 */
public class SchedulingValidator {

    private static final LocalTime OPERATING_START = LocalTime.of(6, 0);
    private static final LocalTime OPERATING_END = LocalTime.of(22, 0);

    /**
     * Checks if two TimeSlots overlap.
     * Overlap occurs if one starts before the other ends, AND starts before the other ends.
     */
    public boolean overlaps(TimeSlot a, TimeSlot b) {
        return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }

    /**
     * Validates that the given session's times are within operating hours (06:00-22:00)
     * and that the start time is strictly before the end time.
     */
    public void validateOperatingHours(TimeSlot slot) throws InvalidBookingException {
        LocalTime startTime = slot.getStartTime().toLocalTime();
        LocalTime endTime = slot.getEndTime().toLocalTime();

        if (startTime.isBefore(OPERATING_START) || endTime.isAfter(OPERATING_END)) {
            throw new InvalidBookingException(
                "Session times must be within operating hours (" +
                OPERATING_START + " - " + OPERATING_END + ").");
        }
        if (!startTime.isBefore(endTime)) {
            throw new InvalidBookingException("Session start time must be before end time.");
        }
    }

    /**
     * Validates that a session does not double-book a room (Location) at the same time.
     */
    public void validateNoRoomClash(FitnessSession newSession, List<FitnessSession> existingSessions) throws InvalidBookingException {
        TimeSlot newSlot = new TimeSlot(
            newSession.getValidFrom().atTime(newSession.getStartTime()),
            newSession.getValidFrom().atTime(newSession.getEndTime()) // Simplified for time overlap check
        );

        for (FitnessSession existing : existingSessions) {
            if (existing.getSessionId().equals(newSession.getSessionId())) continue;
            
            // Only check if they occur on the same day of the week
            if (existing.getDayOfWeek() != newSession.getDayOfWeek()) continue;
            
            TimeSlot existingSlot = new TimeSlot(
                existing.getValidFrom().atTime(existing.getStartTime()),
                existing.getValidFrom().atTime(existing.getEndTime())
            );

            if (overlaps(newSlot, existingSlot) && existing.getLocation() == newSession.getLocation()) {
                throw new InvalidBookingException(
                    "Schedule conflict: Room '" + newSession.getLocation().getDisplayName() + 
                    "' is already booked by session '" + existing.getName() + "' at this time.");
            }
        }
    }

    /**
     * Validates that the session does not double-book an instructor at the same time.
     */
    public void validateNoInstructorClash(FitnessSession newSession, List<FitnessSession> existingSessions) throws InvalidBookingException {
        if (newSession.getInstructor() == null) return;
        
        TimeSlot newSlot = new TimeSlot(
            newSession.getValidFrom().atTime(newSession.getStartTime()),
            newSession.getValidFrom().atTime(newSession.getEndTime())
        );

        for (FitnessSession existing : existingSessions) {
            if (existing.getSessionId().equals(newSession.getSessionId())) continue;
            if (existing.getInstructor() == null) continue;
            if (existing.getDayOfWeek() != newSession.getDayOfWeek()) continue;
            
            TimeSlot existingSlot = new TimeSlot(
                existing.getValidFrom().atTime(existing.getStartTime()),
                existing.getValidFrom().atTime(existing.getEndTime())
            );

            if (overlaps(newSlot, existingSlot) && 
                existing.getInstructor().getUserId().equals(newSession.getInstructor().getUserId())) {
                throw new InvalidBookingException(
                    "Schedule conflict: Instructor '" + newSession.getInstructor().getName() + 
                    "' is already teaching session '" + existing.getName() + "' at this time.");
            }
        }
    }

    /**
     * Validates that the active bookings do not exceed the max capacity.
     */
    public void validateCapacity(int activeBookings, int maxCapacity) throws InvalidBookingException {
        if (activeBookings >= maxCapacity) {
            throw new InvalidBookingException("Session is fully booked (capacity: " + maxCapacity + ").");
        }
    }

    /**
     * Validates that a member is not double-booking:
     * 1. Cannot book the exact same session twice on the same date.
     * 2. Cannot book another session that overlaps in time on the same date.
     */
    public void validateNoMemberDoubleBooking(Member member, FitnessSession sessionToBook, LocalDate date, List<Booking> existingBookings) throws InvalidBookingException {
        TimeSlot newSlot = new TimeSlot(
            date.atTime(sessionToBook.getStartTime()),
            date.atTime(sessionToBook.getEndTime())
        );

        for (Booking b : existingBookings) {
            if (b.getMember() == null || !b.getMember().getUserId().equals(member.getUserId())) continue;
            if (b.getStatus() == BookingStatus.CANCELLED) continue;
            if (!b.getSessionDate().equals(date)) continue;

            FitnessSession existingSession = b.getSession();
            if (existingSession == null) continue;

            // 1. Exact same session on same date
            if (existingSession.getSessionId().equals(sessionToBook.getSessionId())) {
                throw new InvalidBookingException(
                    "Double-booking prevented: Member '" + member.getName() + 
                    "' is already booked for session '" + sessionToBook.getName() + 
                    "' on " + date + ".");
            }

            // 2. Overlapping time slot conflict on same date
            TimeSlot existingSlot = new TimeSlot(
                date.atTime(existingSession.getStartTime()),
                date.atTime(existingSession.getEndTime())
            );

            if (overlaps(newSlot, existingSlot)) {
                throw new InvalidBookingException(
                    "Schedule conflict: Member '" + member.getName() + 
                    "' is already booked for overlapping session '" + existingSession.getName() + 
                    "' (" + existingSession.getStartTime() + " - " + existingSession.getEndTime() + 
                    ") on " + date + ". Double-booking is physically impossible.");
            }
        }
    }

    /**
     * Comprehensive booking validation engine:
     * - Member and session non-null
     * - Date non-null and not in the past
     * - Session active on date
     * - Session capacity check
     * - Member double-booking check (same session and time overlap)
     */
    public void validateBooking(Member member, FitnessSession session, LocalDate date, List<Booking> existingBookings) throws InvalidBookingException {
        if (member == null) {
            throw new InvalidBookingException("A valid gym member must be provided for booking.");
        }
        if (session == null) {
            throw new InvalidBookingException("A valid session must be provided for booking.");
        }
        if (date == null) {
            throw new InvalidBookingException("A booking date must be specified.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new InvalidBookingException("Cannot book a session in the past (" + date + ").");
        }
        if (!session.isActiveOn(date)) {
            throw new InvalidBookingException("Session '" + session.getName() + "' is not active on " + date + ".");
        }

        // Capacity check for this specific session on this date
        long activeCount = existingBookings.stream()
            .filter(b -> b.getSession() != null
                      && b.getSession().getSessionId().equals(session.getSessionId())
                      && b.getSessionDate().equals(date)
                      && b.getStatus() != BookingStatus.CANCELLED)
            .count();
        validateCapacity((int) activeCount, session.getMaxCapacity());

        // Double-booking check across all sessions on this date
        validateNoMemberDoubleBooking(member, session, date, existingBookings);
    }
}
