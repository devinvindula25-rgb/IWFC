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
}
