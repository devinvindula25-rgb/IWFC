package com.iwfc.model;

import java.util.List;

/**
 * Instructor (Staff) user who manages sessions and reports equipment issues.
 * Demonstrates Inheritance (extends User) and Polymorphism (overrides abstract methods).
 */
public class Instructor extends User {

    private String specialization;

    public Instructor(String userId, String name, String email, String specialization) {
        super(userId, name, email, Role.INSTRUCTOR);
        this.specialization = specialization;
    }

    @Override
    public String getRoleDescription() {
        return "Fitness Instructor — schedules fitness sessions, tracks usage of " +
               "specialized equipment, and reports technical or mechanical issues.";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(
            "CREATE_SESSION",
            "VIEW_EQUIPMENT",
            "LOG_EQUIPMENT_USAGE",
            "REPORT_MAINTENANCE",
            "VIEW_OWN_SESSIONS",
            "VIEW_BOOKINGS"
        );
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }
}
