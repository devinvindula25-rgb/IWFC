package com.iwfc.model;

import java.time.LocalDateTime;

public class TimeSlot {
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean overlaps(TimeSlot other) {
        return this.startTime.isBefore(other.getEndTime()) && other.getStartTime().isBefore(this.endTime);
    }
}
