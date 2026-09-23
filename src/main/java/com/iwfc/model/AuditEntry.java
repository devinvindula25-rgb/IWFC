package com.iwfc.model;

import java.time.LocalDateTime;

/**
 * Represents an audit log entry for actions such as unauthorized access attempts.
 */
public class AuditEntry implements Identifiable {

    private String entryId;
    private String userId;
    private String actionAttempted;
    private String deniedReason;
    private LocalDateTime timestamp;

    public AuditEntry(String entryId, String userId, String actionAttempted, String deniedReason) {
        this.entryId = entryId;
        this.userId = userId;
        this.actionAttempted = actionAttempted;
        this.deniedReason = deniedReason;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return entryId;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActionAttempted() {
        return actionAttempted;
    }

    public void setActionAttempted(String actionAttempted) {
        this.actionAttempted = actionAttempted;
    }

    public String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
