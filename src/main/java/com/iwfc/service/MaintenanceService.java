package com.iwfc.service;

import com.iwfc.model.*;
import com.iwfc.pattern.DataStore;
import com.iwfc.pattern.NotificationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing maintenance requests.
 * Handles the workflow: Report → Assign → Complete, with Observer notifications.
 */
public class MaintenanceService {

    private final DataStore dataStore;
    private final NotificationService notificationService;

    public MaintenanceService(NotificationService notificationService) {
        this.dataStore = DataStore.getInstance();
        this.notificationService = notificationService;
    }

    /**
     * Reports a new maintenance issue. Creates a request in PENDING status.
     *
     * @param request the maintenance request to report
     */
    public void reportIssue(MaintenanceRequest request) {
        request.getEquipment().setStatus(EquipmentStatus.FAULTY);
        dataStore.getMaintenanceRequests().add(request);

        notificationService.notifyListeners("MAINTENANCE_ALERT",
            String.format("🔧 New maintenance report: %s — %s [Urgency: %s]",
                request.getEquipment().getName(),
                request.getDescription(),
                request.getUrgency().getDisplayName()));
    }

    /**
     * Assigns a maintenance request to a technician.
     * Transitions status from PENDING to ASSIGNED.
     *
     * @param requestId      the request ID
     * @param technicianName the name of the assigned technician
     * @throws IllegalStateException if request is not in PENDING status
     */
    public void assignRequest(String requestId, String technicianName) {
        MaintenanceRequest request = getRequestById(requestId);
        request.assign(technicianName);

        notificationService.notifyListeners("MAINTENANCE_STATUS_CHANGED",
            String.format("📋 Maintenance request %s assigned to %s — %s",
                requestId, technicianName, request.getEquipment().getName()));
    }

    /**
     * Completes a maintenance request and records resolution notes.
     * Transitions status from ASSIGNED to COMPLETED and resets equipment usage.
     *
     * @param requestId       the request ID
     * @param resolutionNotes optional notes describing the repair
     * @throws IllegalStateException if request is not in ASSIGNED status
     */
    public void completeRequest(String requestId, String resolutionNotes) {
        MaintenanceRequest request = getRequestById(requestId);
        if (resolutionNotes != null) {
            request.setResolutionNotes(resolutionNotes);
        }
        request.complete();

        // Restore the equipment status to OPERATIONAL and reset usage
        request.getEquipment().setStatus(EquipmentStatus.OPERATIONAL);
        request.getEquipment().resetUsageHours();

        notificationService.notifyListeners("MAINTENANCE_STATUS_CHANGED",
            String.format("✅ Maintenance completed: %s [%s] is now operational",
                request.getEquipment().getName(), requestId));
    }

    /**
     * Re-opens a completed maintenance request.
     */
    public void reopenRequest(String requestId) {
        MaintenanceRequest request = getRequestById(requestId);
        request.reopen();
        request.getEquipment().setStatus(EquipmentStatus.UNDER_MAINTENANCE);
        
        notificationService.notifyListeners("MAINTENANCE_STATUS_CHANGED",
            String.format("🔄 Maintenance re-opened: %s [%s]",
                request.getEquipment().getName(), requestId));
    }

    /**
     * Retrieves a maintenance request by ID.
     */
    public MaintenanceRequest getRequestById(String requestId) {
        return dataStore.getMaintenanceRequests().stream()
                .filter(r -> r.getRequestId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Maintenance request not found with ID: " + requestId));
    }

    /**
     * Returns all maintenance requests.
     */
    public List<MaintenanceRequest> getAllRequests() {
        return dataStore.getAll(MaintenanceRequest.class);
    }

    /**
     * Returns requests filtered by status.
     */
    public List<MaintenanceRequest> getRequestsByStatus(RequestStatus status) {
        return getAllRequests().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Returns requests filtered by urgency.
     */
    public List<MaintenanceRequest> getRequestsByUrgency(Urgency urgency) {
        return getAllRequests().stream()
                .filter(r -> r.getUrgency() == urgency)
                .collect(Collectors.toList());
    }

    /**
     * Returns pending requests with HIGH urgency.
     */
    public List<MaintenanceRequest> getHighUrgencyPendingRequests() {
        return getAllRequests().stream()
                .filter(r -> r.getUrgency() == Urgency.HIGH && r.getStatus() == RequestStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * Generates a new unique request ID.
     */
    public String generateRequestId() {
        int nextId = dataStore.getMaintenanceRequests().size() + 1;
        return "MR" + String.format("%03d", nextId);
    }

    /**
     * Returns maintenance history for a specific piece of equipment.
     */
    public List<MaintenanceRequest> getRequestsForEquipment(String equipmentId) {
        return getAllRequests().stream()
                .filter(r -> r.getEquipment().getEquipmentId().equals(equipmentId))
                .collect(Collectors.toList());
    }

    /**
     * Returns maintenance statistics.
     */
    public java.util.Map<String, Long> getMaintenanceStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        List<MaintenanceRequest> all = getAllRequests();
        stats.put("total", (long) all.size());
        stats.put("pending", all.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count());
        stats.put("assigned", all.stream().filter(r -> r.getStatus() == RequestStatus.ASSIGNED).count());
        stats.put("completed", all.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count());
        return stats;
    }
}
