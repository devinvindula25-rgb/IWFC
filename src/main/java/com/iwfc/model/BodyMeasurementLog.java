package com.iwfc.model;

import java.time.LocalDate;

/**
 * Data model for capturing daily body measurements.
 */
public class BodyMeasurementLog implements Identifiable {
    private String id;
    private String memberId;
    private LocalDate date;
    
    // Measurements
    private double weight;     // in kg
    private double height;     // in cm
    private double chest;      // in cm
    private double upperArms;  // in cm
    private double thighs;     // in cm
    private double calves;     // in cm

    public BodyMeasurementLog(String id, String memberId, LocalDate date, double weight, double height, 
                              double chest, double upperArms, double thighs, double calves) {
        this.id = id;
        this.memberId = memberId;
        this.date = date;
        this.weight = weight;
        this.height = height;
        this.chest = chest;
        this.upperArms = upperArms;
        this.thighs = thighs;
        this.calves = calves;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getChest() {
        return chest;
    }

    public void setChest(double chest) {
        this.chest = chest;
    }

    public double getUpperArms() {
        return upperArms;
    }

    public void setUpperArms(double upperArms) {
        this.upperArms = upperArms;
    }

    public double getThighs() {
        return thighs;
    }

    public void setThighs(double thighs) {
        this.thighs = thighs;
    }

    public double getCalves() {
        return calves;
    }

    public void setCalves(double calves) {
        this.calves = calves;
    }
}
