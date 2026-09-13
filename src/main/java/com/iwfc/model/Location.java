package com.iwfc.model;

/**
 * Enum representing physical locations/zones within the fitness center.
 */
public enum Location {
    CARDIO_ZONE("Cardio Zone"),
    STUDIO_A("Studio A"),
    STUDIO_B("Studio B"),
    WEIGHT_ROOM("Weight Room"),
    POOL_AREA("Pool Area"),
    OUTDOOR_AREA("Outdoor Area");

    private final String displayName;

    Location(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
