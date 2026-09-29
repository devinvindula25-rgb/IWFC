package com.iwfc.model;

import java.time.LocalDateTime;

/**
 * Represents a member's request to change their assigned instructor.
 */
public class InstructorChange implements Identifiable {
    private final String id;
    private final String enrollmentId;
    private final String requestedInstructorId;
    private final String reason;
    private final LocalDateTime requestDate;
    private ChangeRequestStatus status;

    public InstructorChange(String id, String enrollmentId, String requestedInstructorId, String reason, LocalDateTime requestDate, ChangeRequestStatus status) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.requestedInstructorId = requestedInstructorId;
        this.reason = reason;
        this.requestDate = requestDate;
        this.status = status;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getRequestedInstructorId() {
        return requestedInstructorId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public ChangeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ChangeRequestStatus status) {
        this.status = status;
    }
}
