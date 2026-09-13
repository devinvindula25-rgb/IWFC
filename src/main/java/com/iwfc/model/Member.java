package com.iwfc.model;

import java.util.List;

/**
 * Member (Client) user who books sessions and receives notifications.
 * Demonstrates Inheritance (extends User) and Polymorphism (overrides abstract methods).
 */
public class Member extends User {

    private String membershipType;

    public Member(String userId, String name, String email, String membershipType) {
        super(userId, name, email, Role.MEMBER);
        this.membershipType = membershipType;
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
}
