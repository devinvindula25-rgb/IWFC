package com.iwfc.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class FollowUp implements Identifiable {
    private final String id;
    private String name;
    private String phoneNumber;
    private FollowUpType type;
    private LocalDateTime scheduledDate;
    private Convertibility convertibility;
    private String comments;

    public FollowUp(String name, String phoneNumber, FollowUpType type, LocalDateTime scheduledDate, Convertibility convertibility, String comments) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.type = type;
        this.scheduledDate = scheduledDate;
        this.convertibility = convertibility;
        this.comments = comments;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public FollowUpType getType() {
        return type;
    }

    public void setType(FollowUpType type) {
        this.type = type;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Convertibility getConvertibility() {
        return convertibility;
    }

    public void setConvertibility(Convertibility convertibility) {
        this.convertibility = convertibility;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
