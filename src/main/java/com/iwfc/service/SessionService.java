package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing fitness session scheduling and bookings.
 * Enforces business rules: no double-bookings, operating hours validation.
 */
public class SessionService {

    private final DataStore dataStore;
    private final NotificationService notificationService;
    private final Map<String, List<Booking>> sessionBookings;

    public SessionService(NotificationService notificationService) {
        this.dataStore = DataStore.getInstance();
        this.notificationService = notificationService;
        this.sessionBookings = new HashMap<>();

        // Index existing bookings by session ID
        for (Booking booking : dataStore.getBookings().findAll()) {
            sessionBookings.computeIfAbsent(booking.getSession().getSessionId(),
                    k -> new ArrayList<>()).add(booking);
        }
    }

    /**
     * Creates a new fitness session.
     *
     * @param session the session to create
     * @throws InvalidBookingException if the session conflicts with an existing session
     *                                 or falls outside operating hours
     */
    public void createSession(FitnessSession session) throws InvalidBookingException {
        SchedulingValidator validator = new SchedulingValidator();
        TimeSlot slot = new TimeSlot(
            session.getValidFrom().atTime(session.getStartTime()),
            session.getValidFrom().atTime(session.getEndTime())
        );
        validator.validateOperatingHours(slot);
        validator.validateNoRoomClash(session, dataStore.getSessions().findAll());
        validator.validateNoInstructorClash(session, dataStore.getSessions().findAll());
        try {
            dataStore.getSessions().save(session);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            throw new RuntimeException("Failed to create session: " + e.getMessage());
        }
    }

    /**
     * Edits an existing fitness session.
     *
     * @param sessionId the session ID
     * @param name new name
     * @param start new start time
     * @param end new end time
     * @param capacity new capacity
     * @throws InvalidBookingException if operating hours or conflict validation fails
     */
    public void editSession(String sessionId, String name, LocalTime start, LocalTime end, int capacity) throws InvalidBookingException {
        FitnessSession session = getSessionById(sessionId);
        
        SchedulingValidator validator = new SchedulingValidator();
        TimeSlot slot = new TimeSlot(
            session.getValidFrom().atTime(start),
            session.getValidFrom().atTime(end)
        );
        validator.validateOperatingHours(slot);
        
        // Check for conflicts with other sessions using a temporary copy
        FitnessSession tempSession = new FitnessSession(
                session.getSessionId(), name, session.getSessionType(),
                session.getInstructor(), session.getDayOfWeek(), start, end,
                session.getLocation(), capacity
        );
        validator.validateNoRoomClash(tempSession, dataStore.getSessions().findAll());
        validator.validateNoInstructorClash(tempSession, dataStore.getSessions().findAll());
        
        session.setName(name);
        session.setStartTime(start);
        session.setEndTime(end);
        session.setMaxCapacity(capacity);
        
        notificationService.notifyListeners("SESSION_UPDATED",
            String.format("Session updated: '%s'", session.getName()));
    }

    /**
     * Books a member into a fitness session for today.
     * Preserved for backward compatibility.
     *
     * @param member    the member making the booking
     * @param sessionId the session ID to book
     * @return the created Booking
     * @throws InvalidBookingException if session is full, or member already booked
     */
    public Booking bookSession(Member member, String sessionId) throws InvalidBookingException {
        // By default, book for the next available occurrence or today
        FitnessSession session = getSessionById(sessionId);
        LocalDate nextDate = LocalDate.now();
        while (!session.isActiveOn(nextDate) && nextDate.isBefore(LocalDate.now().plusMonths(3))) {
            nextDate = nextDate.plusDays(1);
        }
        return bookSessionForDate(member, sessionId, nextDate);
    }

    /**
     * Books a member into a fitness session for a specific date.
     *
     * @param member    the member making the booking
     * @param sessionId the session ID to book
     * @param date      the specific date to book
     * @return the created Booking
     * @throws InvalidBookingException if session is full, member already booked, or date invalid
     */
    public Booking bookSessionForDate(Member member, String sessionId, LocalDate date) throws InvalidBookingException {
        FitnessSession session = getSessionById(sessionId);

        // Enforce all scheduling and booking rules via SchedulingValidator engine
        SchedulingValidator validator = new SchedulingValidator();
        validator.validateBooking(member, session, date, dataStore.getBookings().findAll());

        // Create booking
        String bookingId = "BK" + String.format("%03d", dataStore.getBookings().findAll().size() + 1);
        Booking booking = new Booking(bookingId, member, session, date, LocalDateTime.now());

        try {
            dataStore.getBookings().save(booking);
        } catch (com.iwfc.exception.DuplicateDataException e) {
            throw new RuntimeException("Failed to save booking: " + e.getMessage());
        }
        sessionBookings.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(booking);

        // Fire Observer notification
        notificationService.notifyListeners("BOOKING_CONFIRMED",
            String.format("✓ Booking confirmed: %s booked '%s' on %s %s-%s",
                member.getName(), session.getName(), date,
                session.getStartTime(), session.getEndTime()));

        return booking;
    }

    /**
     * Cancels a member's booking.
     *
     * @param bookingId the ID of the booking to cancel
     * @param memberId  the ID of the member requesting the cancellation
     * @throws IllegalArgumentException if the booking is not found or does not belong to the member
     */
    public void cancelBooking(String bookingId, String memberId) {
        Booking booking = dataStore.getBookings().findAll().stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getMember().getUserId().equals(memberId)) {
            throw new IllegalArgumentException("You can only cancel your own bookings.");
        }
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled.");
        }

        booking.cancel();

        notificationService.notifyListeners("BOOKING_CANCELLED",
            String.format("⚠ Booking cancelled: %s cancelled '%s' on %s",
                booking.getMember().getName(), booking.getSession().getName(),
                booking.getSession().getDayOfWeek()));
    }

    /**
     * Cancels an entire session and all its active bookings.
     *
     * @param sessionId the ID of the session to cancel
     */
    public void cancelSession(String sessionId) {
        FitnessSession session = getSessionById(sessionId);
        
        // Cancel all active bookings
        List<Booking> bookings = sessionBookings.getOrDefault(sessionId, new ArrayList<>());
        for (Booking booking : bookings) {
            if (booking.getStatus() != BookingStatus.CANCELLED) {
                booking.cancel();
            }
        }
        
        // Remove the session
        dataStore.getSessions().delete(sessionId);

        notificationService.notifyListeners("SESSION_CANCELLED",
            String.format("⚠ Session cancelled: '%s' on %s has been cancelled.",
                session.getName(), session.getDayOfWeek()));
    }

    /**
     * Validates that a session does not conflict with existing sessions
     * at the same location and time.
     *
     * @param newSession the session to validate
     * @throws InvalidBookingException if a conflict exists
     */
    public void validateNoConflict(FitnessSession newSession) throws InvalidBookingException {
        SchedulingValidator validator = new SchedulingValidator();
        validator.validateNoRoomClash(newSession, dataStore.getSessions().findAll());
        validator.validateNoInstructorClash(newSession, dataStore.getSessions().findAll());
    }

    /**
     * Validates that session times fall within operating hours (06:00-22:00).
     */
    public void validateOperatingHours(LocalTime startTime, LocalTime endTime)
            throws InvalidBookingException {
        SchedulingValidator validator = new SchedulingValidator();
        TimeSlot slot = new TimeSlot(LocalDate.now().atTime(startTime), LocalDate.now().atTime(endTime));
        validator.validateOperatingHours(slot);
    }

    /**
     * Returns a session by its ID.
     */
    public FitnessSession getSessionById(String sessionId) {
        FitnessSession session = dataStore.getSessions().findById(sessionId).orElse(null);
        if (session == null) {
            throw new IllegalArgumentException("Session not found with ID: " + sessionId);
        }
        return session;
    }

    /**
     * Returns all sessions.
     */
    public List<FitnessSession> getAllSessions() {
        return dataStore.getSessions().findAll();
    }

    /**
     * Returns sessions filtered by day of week (for timetable rendering).
     */
    public List<FitnessSession> getSessionsByDay(DayOfWeek day) {
        return getAllSessions().stream()
                .filter(s -> s.getDayOfWeek() == day)
                .collect(Collectors.toList());
    }

    /**
     * Returns sessions filtered by specific date.
     */
    public List<FitnessSession> getSessionsForDate(LocalDate date) {
        return getAllSessions().stream()
                .filter(s -> s.isActiveOn(date))
                .collect(Collectors.toList());
    }

    /**
     * Returns a map of all sessions in a month, grouped by date.
     */
    public Map<LocalDate, List<FitnessSession>> getSessionsForMonth(YearMonth month) {
        Map<LocalDate, List<FitnessSession>> sessionsByDate = new HashMap<>();
        List<FitnessSession> allSessions = getAllSessions();
        
        for (FitnessSession session : allSessions) {
            List<LocalDate> occurrences = session.getOccurrencesInMonth(month);
            for (LocalDate date : occurrences) {
                sessionsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(session);
            }
        }
        return sessionsByDate;
    }

    /**
     * Returns sessions with available capacity for a specific date.
     */
    public List<FitnessSession> getAvailableSessionsForDate(LocalDate date) {
        return getSessionsForDate(date).stream()
                .filter(s -> {
                    List<Booking> bookings = sessionBookings.getOrDefault(s.getSessionId(), new ArrayList<>());
                    long activeBookings = bookings.stream()
                            .filter(b -> b.getStatus() != BookingStatus.CANCELLED && b.getSessionDate().equals(date))
                            .count();
                    return activeBookings < s.getMaxCapacity();
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns sessions with available capacity. Note: Less useful with date-based.
     */
    public List<FitnessSession> getAvailableSessions() {
        return getAvailableSessionsForDate(LocalDate.now());
    }

    /**
     * Returns all bookings for a specific session.
     */
    public List<Booking> getBookingsForSession(String sessionId) {
        return sessionBookings.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * Returns all bookings for a specific session on a specific date.
     */
    public List<Booking> getBookingsForDate(String sessionId, LocalDate date) {
        return sessionBookings.getOrDefault(sessionId, new ArrayList<>()).stream()
                .filter(b -> b.getSessionDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * Returns all bookings.
     */
    public List<Booking> getAllBookings() {
        return dataStore.getBookings().findAll();
    }

    /**
     * Returns bookings for a specific member.
     */
    public List<Booking> getBookingsForMember(String memberId) {
        return dataStore.getBookings().findAll().stream()
                .filter(b -> b.getMember().getUserId().equals(memberId))
                .collect(Collectors.toList());
    }
    
    /**
     * Returns bookings for a specific member in a given month.
     */
    public List<Booking> getBookingsForMemberInMonth(String memberId, YearMonth month) {
        return getBookingsForMember(memberId).stream()
                .filter(b -> YearMonth.from(b.getSessionDate()).equals(month))
                .collect(Collectors.toList());
    }
}
