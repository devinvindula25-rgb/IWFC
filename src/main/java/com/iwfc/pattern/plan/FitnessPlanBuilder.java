package com.iwfc.pattern.plan;

import com.iwfc.model.FitnessPlan;
import com.iwfc.model.GroupClassPlan;
import com.iwfc.model.PersonalTrainingPlan;
import com.iwfc.model.SelfGuidedPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the Builder pattern for constructing complex FitnessPlan objects.
 */
public class FitnessPlanBuilder {
    private String id;
    private String title;
    private String description;
    private double baseMonthlyFee;
    private int durationWeeks;
    private final List<String> objectives = new ArrayList<>();
    
    // Type specific fields
    private String type = "SelfGuided";
    private int classesPerWeek = 0;
    private double groupDiscountRate = 0.0;
    private int oneOnOneSessionsPerWeek = 0;
    private double instructorPremiumFee = 0.0;
    private boolean includesDietaryPlan = false;
    private boolean includesAppAccess = false;
    private String equipmentFocus = "None";

    public FitnessPlanBuilder setId(String id) { this.id = id; return this; }
    public FitnessPlanBuilder setTitle(String title) { this.title = title; return this; }
    public FitnessPlanBuilder setDescription(String description) { this.description = description; return this; }
    public FitnessPlanBuilder setBaseMonthlyFee(double fee) { this.baseMonthlyFee = fee; return this; }
    public FitnessPlanBuilder setDurationWeeks(int weeks) { this.durationWeeks = weeks; return this; }
    public FitnessPlanBuilder addObjective(String objective) { this.objectives.add(objective); return this; }
    
    public FitnessPlanBuilder asGroupClass(int classesPerWeek, double discountRate) {
        this.type = "Group";
        this.classesPerWeek = classesPerWeek;
        this.groupDiscountRate = discountRate;
        return this;
    }

    public FitnessPlanBuilder asPersonalTraining(int sessions, double premium, boolean diet) {
        this.type = "Personal";
        this.oneOnOneSessionsPerWeek = sessions;
        this.instructorPremiumFee = premium;
        this.includesDietaryPlan = diet;
        return this;
    }

    public FitnessPlanBuilder asSelfGuided(boolean app, String focus) {
        this.type = "SelfGuided";
        this.includesAppAccess = app;
        this.equipmentFocus = focus;
        return this;
    }

    public FitnessPlan build() {
        return switch (type) {
            case "Group" -> new GroupClassPlan(id, title, description, baseMonthlyFee, durationWeeks, objectives, classesPerWeek, groupDiscountRate);
            case "Personal" -> new PersonalTrainingPlan(id, title, description, baseMonthlyFee, durationWeeks, objectives, oneOnOneSessionsPerWeek, instructorPremiumFee, includesDietaryPlan);
            default -> new SelfGuidedPlan(id, title, description, baseMonthlyFee, durationWeeks, objectives, includesAppAccess, equipmentFocus);
        };
    }
}
