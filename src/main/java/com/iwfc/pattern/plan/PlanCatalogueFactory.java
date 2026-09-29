package com.iwfc.pattern.plan;

import com.iwfc.model.FitnessPlan;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class to seed a diverse catalogue of 18 fitness plans.
 * Demonstrates the Factory and Builder creational patterns in action.
 */
public class PlanCatalogueFactory {

    public static List<FitnessPlan> createInitialCatalogue() {
        List<FitnessPlan> catalogue = new ArrayList<>();
        
        // 1-6: Group Classes
        catalogue.add(new FitnessPlanBuilder().setId("FP001").setTitle("Yoga Essentials")
            .setDescription("Beginner flexibility and core").setBaseMonthlyFee(40.0).setDurationWeeks(12)
            .addObjective("Flexibility").asGroupClass(2, 0.10).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP002").setTitle("Advanced HIIT")
            .setDescription("High intensity intervals").setBaseMonthlyFee(60.0).setDurationWeeks(8)
            .addObjective("Cardio").asGroupClass(3, 0.15).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP003").setTitle("Spin City")
            .setDescription("Indoor cycling marathon").setBaseMonthlyFee(50.0).setDurationWeeks(4)
            .addObjective("Endurance").asGroupClass(3, 0.10).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP004").setTitle("Aqua Aerobics")
            .setDescription("Low impact pool workouts").setBaseMonthlyFee(45.0).setDurationWeeks(10)
            .addObjective("Recovery").asGroupClass(2, 0.05).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP005").setTitle("Crossfit Intro")
            .setDescription("Learn the olympic lifts").setBaseMonthlyFee(70.0).setDurationWeeks(6)
            .addObjective("Strength").asGroupClass(4, 0.20).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP006").setTitle("Zumba Party")
            .setDescription("Dance cardio").setBaseMonthlyFee(35.0).setDurationWeeks(12)
            .addObjective("Weight Loss").asGroupClass(2, 0.10).build());

        // 7-12: Personal Training
        catalogue.add(new FitnessPlanBuilder().setId("FP007").setTitle("1-on-1 Powerlifting")
            .setDescription("Master the big three lifts").setBaseMonthlyFee(100.0).setDurationWeeks(12)
            .addObjective("Strength").asPersonalTraining(3, 150.0, true).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP008").setTitle("Bridal Bootcamp PT")
            .setDescription("Get ready for the big day").setBaseMonthlyFee(80.0).setDurationWeeks(8)
            .addObjective("Weight Loss").asPersonalTraining(2, 100.0, true).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP009").setTitle("Senior Mobility PT")
            .setDescription("Safe guided workouts for seniors").setBaseMonthlyFee(60.0).setDurationWeeks(16)
            .addObjective("Mobility").asPersonalTraining(2, 80.0, false).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP010").setTitle("Pro Bodybuilding Prep")
            .setDescription("Contest preparation coaching").setBaseMonthlyFee(150.0).setDurationWeeks(16)
            .addObjective("Hypertrophy").asPersonalTraining(4, 300.0, true).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP011").setTitle("Post-Injury Rehab PT")
            .setDescription("Careful muscle reconstruction").setBaseMonthlyFee(90.0).setDurationWeeks(12)
            .addObjective("Rehab").asPersonalTraining(2, 120.0, false).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP012").setTitle("Marathon Prep PT")
            .setDescription("Running form and endurance coaching").setBaseMonthlyFee(75.0).setDurationWeeks(20)
            .addObjective("Endurance").asPersonalTraining(1, 80.0, true).build());

        // 13-18: Self Guided
        catalogue.add(new FitnessPlanBuilder().setId("FP013").setTitle("5x5 StrongLifts App")
            .setDescription("Famous 3-day strength program").setBaseMonthlyFee(20.0).setDurationWeeks(12)
            .addObjective("Strength").asSelfGuided(true, "Barbells").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP014").setTitle("Couch to 5K")
            .setDescription("Beginner running plan").setBaseMonthlyFee(15.0).setDurationWeeks(9)
            .addObjective("Cardio").asSelfGuided(true, "Treadmills").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP015").setTitle("Dumbbell Only Hypertrophy")
            .setDescription("Grow muscle with just dumbbells").setBaseMonthlyFee(25.0).setDurationWeeks(8)
            .addObjective("Hypertrophy").asSelfGuided(false, "Dumbbells").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP016").setTitle("Calisthenics Mastery")
            .setDescription("Bodyweight skill acquisition").setBaseMonthlyFee(30.0).setDurationWeeks(24)
            .addObjective("Body Control").asSelfGuided(true, "Pullup Bars / Rings").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP017").setTitle("Kettlebell Flow")
            .setDescription("Functional movement and cardio").setBaseMonthlyFee(25.0).setDurationWeeks(6)
            .addObjective("Functional").asSelfGuided(false, "Kettlebells").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP018").setTitle("Full Gym Access - No Plan")
            .setDescription("Standard open gym membership").setBaseMonthlyFee(50.0).setDurationWeeks(52)
            .addObjective("General Health").asSelfGuided(true, "All Equipment").build());

        // 19-32: Mixed New Plans
        catalogue.add(new FitnessPlanBuilder().setId("FP019").setTitle("Kettlebell Conditioning")
            .setDescription("High intensity kettlebell workouts").setBaseMonthlyFee(40.0).setDurationWeeks(8)
            .addObjective("Conditioning").asGroupClass(3, 0.1).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP020").setTitle("Pilates Core Focus")
            .setDescription("Build a strong core and improve posture").setBaseMonthlyFee(45.0).setDurationWeeks(10)
            .addObjective("Core Strength").asGroupClass(2, 0.05).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP021").setTitle("Triathlon Prep PT")
            .setDescription("Swim, Bike, Run mastery").setBaseMonthlyFee(110.0).setDurationWeeks(24)
            .addObjective("Endurance").asPersonalTraining(3, 200.0, true).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP022").setTitle("Youth Athletics PT")
            .setDescription("Speed and agility for young athletes").setBaseMonthlyFee(65.0).setDurationWeeks(8)
            .addObjective("Agility").asPersonalTraining(2, 100.0, false).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP023").setTitle("Home Workout Mastery")
            .setDescription("No equipment home program").setBaseMonthlyFee(15.0).setDurationWeeks(12)
            .addObjective("General Fitness").asSelfGuided(true, "None").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP024").setTitle("Grip Strength Pro")
            .setDescription("Forearm and grip development").setBaseMonthlyFee(20.0).setDurationWeeks(6)
            .addObjective("Strength").asSelfGuided(false, "Grip Trainers").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP025").setTitle("Stretching & Recovery")
            .setDescription("Active recovery sessions").setBaseMonthlyFee(30.0).setDurationWeeks(4)
            .addObjective("Recovery").asGroupClass(1, 0.0).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP026").setTitle("Boxing Basics")
            .setDescription("Learn the fundamentals of boxing").setBaseMonthlyFee(55.0).setDurationWeeks(8)
            .addObjective("Cardio").asGroupClass(2, 0.1).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP027").setTitle("Olympic Weightlifting")
            .setDescription("Clean & Jerk, Snatch coaching").setBaseMonthlyFee(130.0).setDurationWeeks(12)
            .addObjective("Power").asPersonalTraining(3, 250.0, true).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP028").setTitle("Pre-Natal Fitness PT")
            .setDescription("Safe workouts for expecting mothers").setBaseMonthlyFee(85.0).setDurationWeeks(16)
            .addObjective("Health").asPersonalTraining(2, 120.0, false).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP029").setTitle("Ironman Journey")
            .setDescription("Extreme distance prep").setBaseMonthlyFee(40.0).setDurationWeeks(48)
            .addObjective("Endurance").asSelfGuided(true, "All Cardio").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP030").setTitle("Desk Worker Fix")
            .setDescription("Posture correction for office workers").setBaseMonthlyFee(25.0).setDurationWeeks(8)
            .addObjective("Mobility").asSelfGuided(true, "Bands, Mats").build());
        catalogue.add(new FitnessPlanBuilder().setId("FP031").setTitle("Advanced Calisthenics PT")
            .setDescription("Learn the Planche and Front Lever").setBaseMonthlyFee(95.0).setDurationWeeks(20)
            .addObjective("Body Control").asPersonalTraining(2, 140.0, true).build());
        catalogue.add(new FitnessPlanBuilder().setId("FP032").setTitle("Mindful Meditation & Yoga")
            .setDescription("Mental and physical relaxation").setBaseMonthlyFee(50.0).setDurationWeeks(12)
            .addObjective("Wellness").asGroupClass(3, 0.05).build());

        return catalogue;
    }
}
