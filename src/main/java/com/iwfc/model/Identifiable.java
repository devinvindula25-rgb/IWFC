package com.iwfc.model;

/**
 * Interface for all entities that can be stored in a generic Repository.
 * Demands a single method to retrieve a unique identifier.
 */
public interface Identifiable {
    String getId();
}
