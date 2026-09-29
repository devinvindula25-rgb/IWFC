package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScheduleMaker {
    private final SessionService sessionService;
    public ScheduleMaker(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public List<Booking> generateScheduleForEnrollment(Enrollment enrollment, FitnessPlan plan, Member member, Instructor instructor) {
        List<Booking> generatedBookings = new ArrayList<>();
        int weeks = plan.getDurationWeeks();
        LocalDate startDate = enrollment.getEnrollmentDate();

        try {
            switch (plan) {
                case GroupClassPlan groupPlan -> {
                    int classesPerWeek = groupPlan.getClassesPerWeek();
                    List<FitnessSession> allSessions = sessionService.getAllSessions();
                    
                    // Find available group classes
                    List<FitnessSession> availableClasses = allSessions.stream()
                            .filter(s -> "Group Class".equalsIgnoreCase(s.getSessionType()) || s.getMaxCapacity() > 1)
                            .toList();

                    if (!availableClasses.isEmpty()) {
                        for (int w = 0; w < weeks; w++) {
                            int bookedThisWeek = 0;
                            LocalDate weekStart = startDate.plusWeeks(w);
                            for (int i = 0; i < 7 && bookedThisWeek < classesPerWeek; i++) {
                                LocalDate currentDate = weekStart.plusDays(i);
                                for (FitnessSession s : availableClasses) {
                                    if (s.isActiveOn(currentDate) && bookedThisWeek < classesPerWeek) {
                                        try {
                                            Booking b = sessionService.bookSessionForDate(member, s.getSessionId(), currentDate);
                                            generatedBookings.add(b);
                                            bookedThisWeek++;
                                        } catch (InvalidBookingException ignored) {
                                            // Skip if full or already booked
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                case PersonalTrainingPlan ptPlan -> {
                    int sessionsPerWeek = ptPlan.getOneOnOneSessionsPerWeek();
                    
                    // Create personal sessions and book them
                    for (int w = 0; w < weeks; w++) {
                        for (int i = 0; i < sessionsPerWeek; i++) {
                            LocalDate sessionDate = startDate.plusWeeks(w).plusDays(i * 2L); // Spread them out
                            DayOfWeek dayOfWeek = sessionDate.getDayOfWeek();
                            
                            String sessionId = "PT" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                            FitnessSession newSession = new FitnessSession(
                                    sessionId,
                                    "1-on-1 PT: " + member.getName(),
                                    "Personal Training",
                                    instructor,
                                    dayOfWeek,
                                    LocalTime.of(10 + i, 0), // Spread hours 
                                    LocalTime.of(11 + i, 0),
                                    Location.CARDIO_ZONE, // Default location
                                    1
                            );
                            newSession.setValidFrom(sessionDate);
                            newSession.setValidUntil(sessionDate); // One-off specific dates
                            
                            try {
                                sessionService.createSession(newSession);
                                Booking b = sessionService.bookSessionForDate(member, sessionId, sessionDate);
                                generatedBookings.add(b);
                            } catch (InvalidBookingException e) {
                                // If conflict, try another time, skipping for simplicity
                            }
                        }
                    }
                }
                default -> {
                    // Do nothing for other plan types
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating schedule: " + e.getMessage());
        }

        return generatedBookings;
    }
}
