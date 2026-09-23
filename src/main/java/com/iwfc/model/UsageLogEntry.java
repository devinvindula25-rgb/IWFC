package com.iwfc.model;

import java.time.LocalDateTime;

/**
 * Represents a log entry for equipment usage.
 */
public class UsageLogEntry {
    private LocalDateTime timestamp;
    private double hoursLogged;
    private String loggedByUserId;
    private String notes;

    public UsageLogEntry(LocalDateTime timestamp, double hoursLogged, String loggedByUserId, String notes) {
        this.timestamp = timestamp;
        this.hoursLogged = hoursLogged;
        this.loggedByUserId = loggedByUserId;
        this.notes = notes;
    }

    public UsageLogEntry(double hoursLogged, String loggedByUserId) {
        this(LocalDateTime.now(), hoursLogged, loggedByUserId, "");
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getHoursLogged() {
        return hoursLogged;
    }

    public void setHoursLogged(double hoursLogged) {
        this.hoursLogged = hoursLogged;
    }

    public String getLoggedByUserId() {
        return loggedByUserId;
    }

    public void setLoggedByUserId(String loggedByUserId) {
        this.loggedByUserId = loggedByUserId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
