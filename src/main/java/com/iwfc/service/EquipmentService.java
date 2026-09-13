package com.iwfc.service;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.Location;
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
        Map<String, Equipment> equipmentMap = dataStore.getEquipmentMap();
        if (equipmentMap.containsKey(equipment.getEquipmentId())) {
            throw new DuplicateDataException(
                "Equipment with ID '" + equipment.getEquipmentId() + "' already exists.");
        }
        equipmentMap.put(equipment.getEquipmentId(), equipment);
    }

    /**
     * Updates the attributes of an existing equipment item.
     *
     * @param equipmentId the ID of the equipment to update
     * @param name        new name (null to keep existing)
     * @param status      new status (null to keep existing)
     * @param location    new location (null to keep existing)
     * @throws IllegalArgumentException if equipment not found
     */
    public void editEquipment(String equipmentId, String name,
                              EquipmentStatus status, Location location) {
        Equipment equipment = getEquipmentById(equipmentId);
        if (name != null) equipment.setName(name);
        if (status != null) equipment.setStatus(status);
        if (location != null) equipment.setLocation(location);
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
     */
    public void logUsage(String equipmentId, double hours) {
        Equipment equipment = getEquipmentById(equipmentId);
        boolean wasBelowThreshold = !equipment.needsMaintenance();
        equipment.addUsageHours(hours);

        if (wasBelowThreshold && equipment.needsMaintenance()) {
            notificationService.notifyListeners("EQUIPMENT_THRESHOLD",
                String.format("⚠ ALERT: %s [%s] has reached %.0f/%.0f usage hours — maintenance required!",
                    equipment.getName(), equipment.getEquipmentId(),
                    equipment.getUsageHours(), equipment.getMaintenanceThreshold()));
        }
    }

    /**
     * Retrieves equipment by ID.
     *
     * @param equipmentId the equipment ID
     * @return the equipment
     * @throws IllegalArgumentException if not found
     */
    public Equipment getEquipmentById(String equipmentId) {
        Equipment equipment = dataStore.getEquipmentMap().get(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment not found with ID: " + equipmentId);
        }
        return equipment;
    }

    /**
     * Returns all equipment in the inventory.
     */
    public List<Equipment> getAllEquipment() {
        return dataStore.getAll(Equipment.class);
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
}
