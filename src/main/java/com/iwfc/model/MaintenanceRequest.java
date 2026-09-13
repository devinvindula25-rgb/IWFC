package com.iwfc.model;

import java.time.LocalDateTime;

/**
 * Represents a maintenance request reported by an Instructor for faulty equipment.
 * Follows the workflow: PENDING -> ASSIGNED -> COMPLETED.
 */
public class MaintenanceRequest {
    private String requestId;
    private Equipment equipment;
    private Instructor reportedBy;
    private String description;
    private Urgency urgency;
    private RequestStatus status;
    private String assignedTo;
    private LocalDateTime createdDate;
    private LocalDateTime completedDate;

    public MaintenanceRequest(String requestId, Equipment equipment, Instructor reportedBy,
                              String description, Urgency urgency) {
        this.requestId = requestId;
        this.equipment = equipment;
        this.reportedBy = reportedBy;
        this.description = description;
        this.urgency = urgency;
        this.status = RequestStatus.PENDING;
        this.assignedTo = null;
        this.createdDate = LocalDateTime.now();
        this.completedDate = null;
    }

    /**
     * Assigns this request to a technician and transitions status to ASSIGNED.
     *
     * @param technicianName the name of the technician assigned
     * @throws IllegalStateException if the request is not in PENDING status
     */
    public void assign(String technicianName) {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot assign request — current status is " + status.getDisplayName() +
                ". Only PENDING requests can be assigned.");
        }
        this.assignedTo = technicianName;
        this.status = RequestStatus.ASSIGNED;
    }

    /**
     * Marks this request as completed.
     *
     * @throws IllegalStateException if the request is not in ASSIGNED status
     */
    public void complete() {
        if (this.status != RequestStatus.ASSIGNED) {
            throw new IllegalStateException(
                "Cannot complete request — current status is " + status.getDisplayName() +
                ". Only ASSIGNED requests can be completed.");
        }
        this.status = RequestStatus.COMPLETED;
        this.completedDate = LocalDateTime.now();
    }

    // --- Encapsulated accessors ---

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Instructor getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(Instructor reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    @Override
    public String toString() {
        return String.format("Maintenance [%s] - %s: %s | Urgency: %s | Status: %s",
                requestId, equipment.getName(), description,
                urgency.getDisplayName(), status.getDisplayName());
    }
}
