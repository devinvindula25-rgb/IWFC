package com.iwfc.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scheduled fitness session at the IWFC.
 * Contains scheduling details, instructor assignment, and equipment requirements.
 */
public class FitnessSession implements Identifiable {
    private String sessionId;
    private String name;
    private String sessionType;
    private Instructor instructor;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Location location;
    private int maxCapacity;
    private List<Equipment> requiredEquipment;
    private boolean recurring;
    private String recurrenceNote;
    private LocalDate validFrom;
    private LocalDate validUntil;

    public FitnessSession(String sessionId, String name, String sessionType,
                          Instructor instructor, DayOfWeek dayOfWeek,
                          LocalTime startTime, LocalTime endTime,
                          Location location, int maxCapacity) {
        this.sessionId = sessionId;
        this.name = name;
        this.sessionType = sessionType;
        this.instructor = instructor;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.maxCapacity = maxCapacity;
        this.requiredEquipment = new ArrayList<>();
        this.recurring = true; // Default to true
        this.recurrenceNote = "Every " + dayOfWeek.toString().substring(0, 1) + dayOfWeek.toString().substring(1).toLowerCase();
        
        // Defaults to a one-year validity starting today
        this.validFrom = LocalDate.now();
        this.validUntil = LocalDate.now().plusYears(1).withDayOfYear(LocalDate.now().plusYears(1).lengthOfYear());
    }

    /**
     * Checks if this session is active on the given date.
     * Must fall within the validFrom / validUntil range and match the DayOfWeek.
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(validFrom) || date.isAfter(validUntil)) {
            return false;
        }
        return date.getDayOfWeek() == this.dayOfWeek;
    }

    /**
     * Returns all occurrences of this session in a specific month.
     */
    public List<LocalDate> getOccurrencesInMonth(YearMonth month) {
        List<LocalDate> occurrences = new ArrayList<>();
        LocalDate current = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth();

        // Find the first occurrence
        while (current.getDayOfWeek() != this.dayOfWeek && !current.isAfter(endOfMonth)) {
            current = current.plusDays(1);
        }

        while (!current.isAfter(endOfMonth)) {
            if (isActiveOn(current)) {
                occurrences.add(current);
            }
            current = current.plusWeeks(1);
        }
        return occurrences;
    }

    /**
     * Returns a display color hex code based on the session type.
     * Used by the UI timetable to color-code session blocks.
     */
    public String getColor() {
        return switch (sessionType.toUpperCase()) {
            case "HIIT" -> "#FF6B6B";       // Coral red
            case "YOGA" -> "#51CF66";        // Soft green
            case "SPIN" -> "#339AF0";        // Vibrant blue
            case "PILATES" -> "#CC5DE8";     // Purple
            case "CROSSFIT" -> "#FF922B";    // Orange
            case "BOXING" -> "#F06595";      // Pink
            default -> "#868E96";            // Gray fallback
        };
    }

    /**
     * Checks if this session overlaps in time with another session at the same location,
     * or if the same instructor is scheduled for overlapping sessions.
     *
     * @param other the other session to check against
     * @return true if there is a location or instructor conflict
     */
    public boolean conflictsWith(FitnessSession other) {
        if (this.dayOfWeek != other.dayOfWeek) return false;

        boolean timeOverlap = this.startTime.isBefore(other.endTime) &&
                              other.startTime.isBefore(this.endTime);
                              
        if (!timeOverlap) return false;

        // Conflict if same location
        if (this.location == other.location) return true;

        // Conflict if same instructor
        return this.instructor != null && other.instructor != null &&
               this.instructor.getUserId().equals(other.instructor.getUserId());
    }

    // --- Encapsulated accessors ---

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public List<Equipment> getRequiredEquipment() {
        return requiredEquipment;
    }

    public void setRequiredEquipment(List<Equipment> requiredEquipment) {
        this.requiredEquipment = requiredEquipment;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public String getRecurrenceNote() {
        return recurrenceNote;
    }

    public void setRecurrenceNote(String recurrenceNote) {
        this.recurrenceNote = recurrenceNote;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s with %s on %s %s-%s (Cap: %d)",
                name, sessionId, sessionType,
                instructor != null ? instructor.getName() : "TBD",
                dayOfWeek, startTime, endTime, maxCapacity);
    }
}
