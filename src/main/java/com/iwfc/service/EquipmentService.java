package com.iwfc.service;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.Location;
import com.iwfc.model.User;
import com.iwfc.model.RequestStatus;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing fitness equipment inventory.
 * Handles CRUD operations, usage tracking, and maintenance threshold alerts.
 */
public class EquipmentService {

    private final DataStore dataStore;
    private final NotificationService notificationService;

    public EquipmentService(NotificationService notificationService) {
        this.dataStore = DataStore.getInstance();
        this.notificationService = notificationService;
    }

    /**
     * Adds a new piece of equipment to the inventory.
     *
     * @param equipment the equipment to add
     * @throws DuplicateDataException if equipment with the same ID already exists
     */
    public void addEquipment(Equipment equipment) throws DuplicateDataException {
        dataStore.getEquipment().save(equipment);
    }

    /**
     * Updates the attributes of an existing equipment item.
     *
     * @param equipmentId the ID of the equipment to update
     * @param name        new name (null to keep existing)
     * @param category    new category (null to keep existing)
     * @param status      new status (null to keep existing)
     * @param location    new location (null to keep existing)
     * @param maintenanceThreshold new threshold (-1 to keep existing)
     * @throws IllegalArgumentException if equipment not found
     */
    public void editEquipment(String equipmentId, String name, String category,
                              EquipmentStatus status, Location location, double maintenanceThreshold) {
        Equipment equipment = getEquipmentById(equipmentId);
        if (name != null) equipment.setName(name);
        if (category != null) equipment.setCategory(category);
        if (status != null) equipment.setStatus(status);
        if (location != null) equipment.setLocation(location);
        if (maintenanceThreshold >= 0) equipment.setMaintenanceThreshold(maintenanceThreshold);
    }

    /**
     * Deactivates equipment by setting its status to UNDER_MAINTENANCE.
     *
     * @param equipmentId the ID of the equipment to deactivate
     */
    public void deactivateEquipment(String equipmentId) {
        Equipment equipment = getEquipmentById(equipmentId);
        equipment.setStatus(EquipmentStatus.UNDER_MAINTENANCE);
    }

    /**
     * Logs usage hours for equipment and triggers alerts if threshold exceeded.
     *
     * @param equipmentId the ID of the equipment
     * @param hours       the number of hours to add
     * @param user        the user who logged the usage
     */
    public void logUsage(String equipmentId, double hours, User user) {
        Equipment equipment = getEquipmentById(equipmentId);
        boolean wasBelowThreshold = !equipment.needsMaintenance();
        equipment.addUsageHours(hours);

        if (wasBelowThreshold && equipment.needsMaintenance()) {
            notificationService.notifyListeners("EQUIPMENT_THRESHOLD",
                String.format("⚠ ALERT: %s [%s] has reached %.0f/%.0f usage hours — maintenance required!",
                    equipment.getName(), equipment.getEquipmentId(),
                    equipment.getCumulativeUsageHours(), equipment.getMaintenanceThreshold()));
        }
    }

    /**
     * Removes equipment from the inventory.
     *
     * @param equipmentId the ID of the equipment to remove
     * @throws IllegalStateException if equipment is required by a session or has pending/assigned maintenance
     */
    public void removeEquipment(String equipmentId) {
        getEquipmentById(equipmentId); // Validates existence

        // Validate no active sessions require this equipment
        boolean isUsedInSession = dataStore.getSessions().findAll().stream()
                .anyMatch(s -> s.getRequiredEquipment().stream()
                        .anyMatch(e -> e.getEquipmentId().equals(equipmentId)));
        if (isUsedInSession) {
            throw new IllegalStateException("Cannot remove equipment: It is required by one or more fitness sessions.");
        }

        // Validate no pending or assigned maintenance requests
        boolean hasPendingMaintenance = dataStore.getMaintenanceRequests().findAll().stream()
                .anyMatch(r -> r.getEquipment().getEquipmentId().equals(equipmentId) &&
                        (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.ASSIGNED));
        if (hasPendingMaintenance) {
            throw new IllegalStateException("Cannot remove equipment: It has pending or assigned maintenance requests.");
        }

        dataStore.getEquipment().delete(equipmentId);
    }

    /**
     * Retrieves equipment by ID.
     *
     * @param equipmentId the equipment ID
     * @return the equipment
     * @throws IllegalArgumentException if not found
     */
    public Equipment getEquipmentById(String equipmentId) {
        Equipment equipment = dataStore.getEquipment().findById(equipmentId).orElse(null);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found with ID: " + equipmentId);
        }
        return equipment;
    }

    /**
     * Returns all equipment in the inventory.
     */
    public List<Equipment> getAllEquipment() {
        return dataStore.getEquipment().findAll();
    }

    /**
     * Returns equipment filtered by status.
     */
    public List<Equipment> getByStatus(EquipmentStatus status) {
        return getAllEquipment().stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Returns equipment filtered by location.
     */
    public List<Equipment> getByLocation(Location location) {
        return getAllEquipment().stream()
                .filter(e -> e.getLocation() == location)
                .collect(Collectors.toList());
    }

    /**
     * Returns equipment filtered by category.
     */
    public List<Equipment> getByCategory(String category) {
        return getAllEquipment().stream()
                .filter(e -> e.getCategory() != null && e.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /**
     * Returns a summary of equipment counts by status.
     */
    public Map<EquipmentStatus, Long> getEquipmentSummary() {
        return getAllEquipment().stream()
                .collect(Collectors.groupingBy(Equipment::getStatus, Collectors.counting()));
    }
}
