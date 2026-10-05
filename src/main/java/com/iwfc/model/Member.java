package com.iwfc.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Member (Client) user who books sessions and receives notifications.
 * Demonstrates Inheritance (extends User) and Polymorphism (overrides abstract methods).
 */
public class Member extends User {

    private String membershipType;
    private double totalPayments;
    private int hoursSpent;
    private String dietHabits;
    private List<String> machinesUsed;

    public Member(String userId, String name, String email, String phone, String address, String profileImageUrl, String membershipType) {
        super(userId, name, email, phone, address, profileImageUrl, Role.MEMBER);
        this.membershipType = membershipType;
        this.machinesUsed = new ArrayList<>();
    }

    @Override
    public String getRoleDescription() {
        return "Fitness Member — views session schedules, requests bookings for " +
               "specific slots, and receives automated wellness or schedule notifications.";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(
            "VIEW_SESSIONS",
            "BOOK_SESSION",
            "VIEW_OWN_BOOKINGS",
            "VIEW_EQUIPMENT_STATUS",
            "RECEIVE_NOTIFICATIONS"
        );
    }

    public String getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    public double getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(double totalPayments) {
        this.totalPayments = totalPayments;
    }

    public int getHoursSpent() {
        return hoursSpent;
    }

    public void setHoursSpent(int hoursSpent) {
        this.hoursSpent = hoursSpent;
    }

    public String getDietHabits() {
        return dietHabits;
    }

    public void setDietHabits(String dietHabits) {
        this.dietHabits = dietHabits;
    }

    public List<String> getMachinesUsed() {
        return machinesUsed;
    }

    public void setMachinesUsed(List<String> machinesUsed) {
        this.machinesUsed = machinesUsed;
    }

    // Additional Profile & Lifestyle Attributes
    private String gender = "Female";
    private String dateOfBirth = "1995-04-12";
    private String nationality = "American";
    private double waterGoal = 2.5;
    private String allergies = "None";
    private String supplements = "Whey Protein, Multivitamins";

    public String getGender() {
        return gender != null ? gender : "N/A";
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth != null ? dateOfBirth : "N/A";
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality != null ? nationality : "N/A";
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public double getWaterGoal() {
        return waterGoal > 0 ? waterGoal : 2.5;
    }

    public void setWaterGoal(double waterGoal) {
        this.waterGoal = waterGoal;
    }

    public String getAllergies() {
        return allergies != null ? allergies : "None";
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getSupplements() {
        return supplements != null ? supplements : "None";
    }

    public void setSupplements(String supplements) {
        this.supplements = supplements;
    }
}
