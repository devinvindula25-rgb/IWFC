package com.iwfc.model;

import java.util.List;

public class GroupClassPlan extends FitnessPlan {
    private final int classesPerWeek;
    private final double groupDiscountRate;

    public GroupClassPlan(String id, String title, String description, double baseMonthlyFee, int durationWeeks, List<String> objectives, int classesPerWeek, double groupDiscountRate) {
        super(id, title, description, baseMonthlyFee, durationWeeks, objectives);
        this.classesPerWeek = classesPerWeek;
        this.groupDiscountRate = groupDiscountRate;
    }

    public int getClassesPerWeek() {
        return classesPerWeek;
    }

    @Override
    public double calculateTotalCost() {
        // Group discount is applied to the base fee over the duration (approx 4 weeks per month)
        double months = getDurationWeeks() / 4.0;
        return (getBaseMonthlyFee() * (1 - groupDiscountRate)) * months;
    }

    @Override
    public String getProgressionSummary() {
        return "Progress is tracked through group milestone events. Attend " + classesPerWeek + " classes per week to unlock the next level.";
    }

    @Override
    public String getPlanType() {
        return "Group Class Plan";
    }
}
