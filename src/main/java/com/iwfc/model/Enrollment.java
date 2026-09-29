package com.iwfc.model;

import java.time.LocalDate;

/**
 * Represents a member's enrollment in a specific fitness plan.
 * Also tracks if they are assigned a specific instructor (for PersonalTraining or Group classes).
 */
public class Enrollment implements Identifiable {
    private final String id;
    private final String memberId;
    private final String planId;
    private String instructorId; // Can be null if self-guided
    private final LocalDate enrollmentDate;
    private LocalDate expirationDate;
    private String instructorNotes;

    public Enrollment(String id, String memberId, String planId, String instructorId, LocalDate enrollmentDate, LocalDate expirationDate) {
        this.id = id;
        this.memberId = memberId;
        this.planId = planId;
        this.instructorId = instructorId;
        this.enrollmentDate = enrollmentDate;
        this.expirationDate = expirationDate;
        this.instructorNotes = "";
    }

    @Override
    public String getId() {
        return id;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getInstructorNotes() {
        return instructorNotes;
    }

    public void setInstructorNotes(String instructorNotes) {
        this.instructorNotes = instructorNotes;
    }
}
