package com.iwfc.model;

import java.util.List;

/**
 * Abstract base class for Fitness Plans, demonstrating inheritance and polymorphism.
 */
public abstract class FitnessPlan implements Identifiable {
    private final String id;
    private final String title;
    private final String description;
    private final double baseMonthlyFee;
    private final int durationWeeks;
    private final List<String> objectives;

    public FitnessPlan(String id, String title, String description, double baseMonthlyFee, int durationWeeks, List<String> objectives) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.baseMonthlyFee = baseMonthlyFee;
        this.durationWeeks = durationWeeks;
        this.objectives = objectives;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public double getBaseMonthlyFee() {
        return baseMonthlyFee;
    }

    public int getDurationWeeks() {
        return durationWeeks;
    }

    public List<String> getObjectives() {
        return objectives;
    }

    /**
     * Polymorphic method to calculate the total cost of the plan.
     */
    public abstract double calculateTotalCost();

    /**
     * Polymorphic method to summarize how the user progresses through the plan.
     */
    public abstract String getProgressionSummary();

    /**
     * Returns a human-readable type for UI rendering.
     */
    public abstract String getPlanType();
}
