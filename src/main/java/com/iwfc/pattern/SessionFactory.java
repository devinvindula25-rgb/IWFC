package com.iwfc.pattern;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.FitnessSession;
import com.iwfc.model.Instructor;
import com.iwfc.model.Location;
import com.iwfc.model.TimeSlot;
import com.iwfc.service.SchedulingValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating FitnessSessions.
 * Implements the Factory Creational Pattern for generating recurring sessions.
 */
public class SessionFactory {

    /**
     * Generates a series of FitnessSession instances for each week based on the RecurrenceRule.
     * Skips weeks that have a scheduling clash.
     *
     * @param baseId           The base ID for the sessions
     * @param baseName         The base name for the sessions
     * @param type             The session type
     * @param instructor       The assigned instructor
     * @param location         The location
     * @param capacity         The max capacity
     * @param rule             The recurrence rule
     * @param startDate        The date to start generating from
     * @param existingSessions The existing sessions to check against
     * @return A list of successfully created FitnessSessions (no clashes)
     */
    public static List<FitnessSession> createRecurringSessions(
            String baseId, String baseName, String type, Instructor instructor, 
            Location location, int capacity, RecurrenceRule rule, 
            LocalDate startDate, List<FitnessSession> existingSessions) {
        
        List<FitnessSession> generatedSessions = new ArrayList<>();
        SchedulingValidator validator = new SchedulingValidator();
        LocalDate currentDate = startDate;

        // Fast-forward to the first matching day of the week
        while (currentDate.getDayOfWeek() != rule.getDayOfWeek()) {
            currentDate = currentDate.plusDays(1);
        }

        LocalTime endTime = rule.getStartTime().plusMinutes(rule.getDurationMinutes());

        for (int i = 0; i < rule.getWeeksToRepeat(); i++) {
            FitnessSession session = new FitnessSession(
                baseId + "-W" + (i + 1),
                baseName + " (W" + (i + 1) + ")",
                type,
                instructor,
                rule.getDayOfWeek(),
                rule.getStartTime(),
                endTime,
                location,
                capacity
            );
            
            // Restrict validity to just this specific week so it behaves as a single instance
            session.setValidFrom(currentDate);
            session.setValidUntil(currentDate.plusDays(6));
            session.setRecurring(false); // Since we made a distinct instance for this week

            try {
                TimeSlot slot = new TimeSlot(currentDate.atTime(rule.getStartTime()), currentDate.atTime(endTime));
                validator.validateOperatingHours(slot);
                validator.validateNoRoomClash(session, existingSessions);
                validator.validateNoInstructorClash(session, existingSessions);
                
                // If we get here, no exceptions were thrown, so it's a valid session!
                generatedSessions.add(session);
            } catch (InvalidBookingException e) {
                // Clash detected! We skip creating the session for this specific week.
                System.out.println("Skipping week " + (i + 1) + " due to clash: " + e.getMessage());
            }

            // Move to next week
            currentDate = currentDate.plusWeeks(1);
        }

        return generatedSessions;
    }
}
