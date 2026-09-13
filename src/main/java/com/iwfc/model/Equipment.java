package com.iwfc.model;

/**
 * Represents a piece of fitness equipment in the IWFC system.
 * Tracks usage hours for preventative maintenance alerts.
 * Demonstrates Encapsulation — all fields private with controlled access.
 */
public class Equipment {
    private String equipmentId;
    private String name;
    private EquipmentStatus status;
    private Location location;
    private double usageHours;
    private double maintenanceThreshold;

    public Equipment(String equipmentId, String name, EquipmentStatus status,
                     Location location, double usageHours, double maintenanceThreshold) {
        this.equipmentId = equipmentId;
        this.name = name;
        this.status = status;
        this.location = location;
        this.usageHours = usageHours;
        this.maintenanceThreshold = maintenanceThreshold;
    }

    /**
     * Convenience constructor with default threshold of 500 hours and 0 usage.
     */
    public Equipment(String equipmentId, String name, Location location) {
        this(equipmentId, name, EquipmentStatus.OPERATIONAL, location, 0.0, 500.0);
    }

    /**
     * Adds usage hours to the cumulative total.
     *
     * @param hours the number of hours to add (must be positive)
     * @throws IllegalArgumentException if hours is negative
     */
    public void addUsageHours(double hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Usage hours cannot be negative.");
        }
        this.usageHours += hours;
    }

    /**
     * Checks whether this equipment has exceeded its maintenance threshold.
     *
     * @return true if cumulative usage hours meet or exceed the threshold
     */
    public boolean needsMaintenance() {
        return usageHours >= maintenanceThreshold;
    }

    /**
     * Returns the usage as a percentage of the maintenance threshold (0.0 to 1.0+).
     */
    public double getUsagePercentage() {
        if (maintenanceThreshold <= 0) return 1.0;
        return usageHours / maintenanceThreshold;
    }

    // --- Encapsulated accessors ---

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getUsageHours() {
        return usageHours;
    }

    public void setUsageHours(double usageHours) {
        this.usageHours = usageHours;
    }

    public double getMaintenanceThreshold() {
        return maintenanceThreshold;
    }

    public void setMaintenanceThreshold(double maintenanceThreshold) {
        this.maintenanceThreshold = maintenanceThreshold;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s at %s (%.1f/%.1f hrs)",
                name, equipmentId, status.getDisplayName(),
                location.getDisplayName(), usageHours, maintenanceThreshold);
    }
}
