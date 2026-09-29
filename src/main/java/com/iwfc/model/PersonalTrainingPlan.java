package com.iwfc.model;

import java.util.List;

public class PersonalTrainingPlan extends FitnessPlan {
    private final int oneOnOneSessionsPerWeek;
    private final double instructorPremiumFee;
    private final boolean includesDietaryPlan;

    public PersonalTrainingPlan(String id, String title, String description, double baseMonthlyFee, int durationWeeks, List<String> objectives, int oneOnOneSessionsPerWeek, double instructorPremiumFee, boolean includesDietaryPlan) {
        super(id, title, description, baseMonthlyFee, durationWeeks, objectives);
        this.oneOnOneSessionsPerWeek = oneOnOneSessionsPerWeek;
        this.instructorPremiumFee = instructorPremiumFee;
        this.includesDietaryPlan = includesDietaryPlan;
    }

    public int getOneOnOneSessionsPerWeek() {
        return oneOnOneSessionsPerWeek;
    }

    public boolean isIncludesDietaryPlan() {
        return includesDietaryPlan;
    }

    @Override
    public double calculateTotalCost() {
        double months = getDurationWeeks() / 4.0;
        // Instructor premium is charged per month on top of the base fee
        return (getBaseMonthlyFee() + instructorPremiumFee) * months;
    }

    @Override
    public String getProgressionSummary() {
        return "Progress is strictly monitored by your personal instructor with bi-weekly performance checks.";
    }

    @Override
    public String getPlanType() {
        return "Personal Training Plan";
    }
}
