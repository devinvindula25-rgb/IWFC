package com.iwfc.model;

/**
 * Enum representing the operational status of fitness equipment.
 */
public enum EquipmentStatus {
    OPERATIONAL("Operational"),
    FAULTY("Faulty"),
    UNDER_MAINTENANCE("Under Maintenance");

    private final String displayName;

    EquipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
