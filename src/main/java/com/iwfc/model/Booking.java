package com.iwfc.model;

import java.time.LocalDateTime;

/**
 * Represents a booking made by a Member for a FitnessSession.
 */
public class Booking {
    private String bookingId;
    private Member member;
    private FitnessSession session;
    private LocalDateTime bookingDate;
    private String status;

    public Booking(String bookingId, Member member, FitnessSession session,
                   LocalDateTime bookingDate) {
        this.bookingId = bookingId;
        this.member = member;
        this.session = session;
        this.bookingDate = bookingDate;
        this.status = "Confirmed";
    }

    // --- Encapsulated accessors ---

    public String getBookingId() {
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

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Booking [%s] - %s booked %s on %s (%s)",
                bookingId, member.getName(), session.getName(),
                bookingDate, status);
    }
}
