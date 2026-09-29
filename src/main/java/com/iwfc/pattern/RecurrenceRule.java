package com.iwfc.pattern;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class RecurrenceRule {
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final int durationMinutes;
    private final int weeksToRepeat;

    public RecurrenceRule(DayOfWeek dayOfWeek, LocalTime startTime, int durationMinutes, int weeksToRepeat) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.weeksToRepeat = weeksToRepeat;
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getWeeksToRepeat() { return weeksToRepeat; }
}
