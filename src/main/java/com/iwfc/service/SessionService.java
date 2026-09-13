package com.iwfc.service;

import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private static final LocalTime OPERATING_START = LocalTime.of(6, 0);
    private static final LocalTime OPERATING_END = LocalTime.of(22, 0);

    private final DataStore dataStore;
    private final NotificationService notificationService;
    private final Map<String, List<Booking>> sessionBookings;

    public SessionService(NotificationService notificationService) {
        this.dataStore = DataStore.getInstance();
        this.notificationService = notificationService;
        this.sessionBookings = new HashMap<>();

        // Index existing bookings by session ID
        for (Booking booking : dataStore.getBookings()) {
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
        validateOperatingHours(session.getStartTime(), session.getEndTime());
        validateNoConflict(session);
        dataStore.getSessionMap().put(session.getSessionId(), session);
    }

    /**
     * Books a member into a fitness session.
     *
     * @param member    the member making the booking
     * @param sessionId the session ID to book
     * @return the created Booking
     * @throws InvalidBookingException if session is full, or member already booked
     */
    public Booking bookSession(Member member, String sessionId) throws InvalidBookingException {
        FitnessSession session = getSessionById(sessionId);

        // Check capacity
        List<Booking> existingBookings = sessionBookings.getOrDefault(sessionId, new ArrayList<>());
        if (existingBookings.size() >= session.getMaxCapacity()) {
            throw new InvalidBookingException(
                "Session '" + session.getName() + "' is fully booked (capacity: " +
                session.getMaxCapacity() + ").");
        }

        // Check if member already booked this session
        boolean alreadyBooked = existingBookings.stream()
                .anyMatch(b -> b.getMember().getUserId().equals(member.getUserId()));
        if (alreadyBooked) {
            throw new InvalidBookingException(
                "Member '" + member.getName() + "' has already booked session '" +
                session.getName() + "'.");
        }

        // Create booking
        String bookingId = "BK" + String.format("%03d", dataStore.getBookings().size() + 1);
        Booking booking = new Booking(bookingId, member, session, LocalDateTime.now());

        dataStore.getBookings().add(booking);
        sessionBookings.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(booking);

        // Fire Observer notification
        notificationService.notifyListeners("BOOKING_CONFIRMED",
            String.format("✓ Booking confirmed: %s booked '%s' on %s %s-%s",
                member.getName(), session.getName(), session.getDayOfWeek(),
                session.getStartTime(), session.getEndTime()));

        return booking;
    }

    /**
     * Validates that a session does not conflict with existing sessions
     * at the same location and time.
     *
     * @param newSession the session to validate
     * @throws InvalidBookingException if a conflict exists
     */
    public void validateNoConflict(FitnessSession newSession) throws InvalidBookingException {
        for (FitnessSession existing : dataStore.getSessionMap().values()) {
            if (existing.getSessionId().equals(newSession.getSessionId())) continue;
            if (existing.conflictsWith(newSession)) {
                throw new InvalidBookingException(
                    "Schedule conflict: '" + newSession.getName() +
                    "' conflicts with existing session '" + existing.getName() +
                    "' at " + existing.getLocation().getDisplayName() +
                    " on " + existing.getDayOfWeek() + " " +
                    existing.getStartTime() + "-" + existing.getEndTime());
            }
        }
    }

    /**
     * Validates that session times fall within operating hours (06:00-22:00).
     */
    public void validateOperatingHours(LocalTime startTime, LocalTime endTime)
            throws InvalidBookingException {
        if (startTime.isBefore(OPERATING_START) || endTime.isAfter(OPERATING_END)) {
            throw new InvalidBookingException(
                "Session times must be within operating hours (" +
                OPERATING_START + " - " + OPERATING_END + ").");
        }
        if (!startTime.isBefore(endTime)) {
            throw new InvalidBookingException(
                "Session start time must be before end time.");
        }
    }

    /**
     * Returns a session by its ID.
     */
    public FitnessSession getSessionById(String sessionId) {
        FitnessSession session = dataStore.getSessionMap().get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found with ID: " + sessionId);
        }
        return session;
    }

    /**
     * Returns all sessions.
     */
    public List<FitnessSession> getAllSessions() {
        return dataStore.getAll(FitnessSession.class);
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
     * Returns sessions with available capacity.
     */
    public List<FitnessSession> getAvailableSessions() {
        return getAllSessions().stream()
                .filter(s -> {
                    List<Booking> bookings = sessionBookings.getOrDefault(s.getSessionId(), new ArrayList<>());
                    return bookings.size() < s.getMaxCapacity();
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns all bookings for a specific session.
     */
    public List<Booking> getBookingsForSession(String sessionId) {
        return sessionBookings.getOrDefault(sessionId, new ArrayList<>());
    }

    /**
     * Returns all bookings.
     */
    public List<Booking> getAllBookings() {
        return dataStore.getAll(Booking.class);
    }

    /**
     * Returns bookings for a specific member.
     */
    public List<Booking> getBookingsForMember(String memberId) {
        return dataStore.getBookings().stream()
                .filter(b -> b.getMember().getUserId().equals(memberId))
                .collect(Collectors.toList());
    }
}
