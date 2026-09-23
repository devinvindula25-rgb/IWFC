package com.iwfc.model;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Represents a booking made by a Member for a FitnessSession.
 */
public class Booking implements Identifiable {
    private String bookingId;
    private Member member;
    private FitnessSession session;
    private LocalDate sessionDate;
    private LocalDateTime bookingDate;
    private BookingStatus status;

    public Booking(String bookingId, Member member, FitnessSession session,
                   LocalDate sessionDate, LocalDateTime bookingDate) {
        this.bookingId = bookingId;
        this.member = member;
        this.session = session;
        this.sessionDate = sessionDate;
        this.bookingDate = bookingDate;
        this.status = BookingStatus.CONFIRMED;
    }

    /**
     * Cancels the booking.
     */
    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    // --- Encapsulated accessors ---

    public String getBookingId() {
        return bookingId;
    }

    @Override
    public String getId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public FitnessSession getSession() {
        return session;
    }

    public void setSession(FitnessSession session) {
        this.session = session;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Booking [%s] - %s booked %s on %s (booked on %s) (%s)",
                bookingId, member.getName(), session.getName(),
                sessionDate, bookingDate, status.getDisplayName());
    }
}
