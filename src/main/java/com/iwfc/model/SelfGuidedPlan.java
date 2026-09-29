package com.iwfc.model;

import java.util.List;

public class SelfGuidedPlan extends FitnessPlan {
    private final boolean includesAppAccess;
    private final String equipmentFocus;

    public SelfGuidedPlan(String id, String title, String description, double baseMonthlyFee, int durationWeeks, List<String> objectives, boolean includesAppAccess, String equipmentFocus) {
        super(id, title, description, baseMonthlyFee, durationWeeks, objectives);
        this.includesAppAccess = includesAppAccess;
        this.equipmentFocus = equipmentFocus;
    }

    public boolean isIncludesAppAccess() {
        return includesAppAccess;
    }

    public String getEquipmentFocus() {
        return equipmentFocus;
    }

    @Override
    public double calculateTotalCost() {
        double months = getDurationWeeks() / 4.0;
        // Self-guided plans have a 10% discount on the base fee
        return (getBaseMonthlyFee() * 0.90) * months;
    }

    @Override
    public String getProgressionSummary() {
        return "Self-paced progression. Users record their own milestones via the mobile app integration.";
    }

    @Override
    public String getPlanType() {
        return "Self-Guided Plan";
    }
}
