package com.iwfc.model;

public enum Convertibility {
    HOT("Hot"),
    WARM("Warm"),
    COLD("Cold"),
    NONE("None");

    private final String displayName;

    Convertibility(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
