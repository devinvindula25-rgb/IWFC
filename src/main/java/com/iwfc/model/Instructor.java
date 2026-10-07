package com.iwfc.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Instructor (Staff) user who manages sessions and reports equipment issues.
 * Demonstrates Inheritance (extends User) and Polymorphism (overrides abstract methods).
 */
public class Instructor extends User {

    private String specialization;
    private List<String> traineeIds;
    private List<String> feedbacks;
    private double performanceScore;
    private double attendanceRate;
    private String bio;

    public Instructor(String userId, String name, String email, String phone, String address, String profileImageUrl, String specialization) {
        super(userId, name, email, phone, address, profileImageUrl, Role.INSTRUCTOR);
        this.specialization = specialization;
        this.traineeIds = new ArrayList<>();
        this.feedbacks = new ArrayList<>();
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
            "MANAGE_SESSIONS",
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

    public List<String> getTraineeIds() {
        return traineeIds;
    }

    public void setTraineeIds(List<String> traineeIds) {
        this.traineeIds = traineeIds;
    }

    public List<String> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<String> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
