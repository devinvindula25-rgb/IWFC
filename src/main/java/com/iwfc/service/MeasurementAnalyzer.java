package com.iwfc.service;

import com.iwfc.model.BodyMeasurementLog;
import com.iwfc.pattern.DataStore;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for analyzing body measurement data, detecting trends, and finding symmetries.
 */
public class MeasurementAnalyzer {

    private final DataStore dataStore;

    public MeasurementAnalyzer() {
        this.dataStore = DataStore.getInstance();
    }

    /**
     * Retrieves all logs for a member, sorted chronologically.
     */
    public List<BodyMeasurementLog> getLogsForMember(String memberId) {
        return dataStore.getMeasurementLogs().findAll().stream()
                .filter(log -> log.getMemberId().equals(memberId))
                .sorted(Comparator.comparing(BodyMeasurementLog::getDate))
                .collect(Collectors.toList());
    }

    public record Insight(String metricName, String type, String title, String description, double diff) {}
    
    public record InsightReport(String dateRange, List<Insight> trends, List<Insight> symmetry) {}

    /**
     * Generates a structured InsightReport summarizing trends and symmetries.
     */
    public InsightReport generateInsightReport(String memberId) {
        List<BodyMeasurementLog> logs = getLogsForMember(memberId);
        if (logs.size() < 2) {
            return null; // Not enough data
        }

        BodyMeasurementLog oldest = logs.get(0);
        BodyMeasurementLog latest = logs.get(logs.size() - 1);

        String dateRange = oldest.getDate() + " to " + latest.getDate();

        List<Insight> trends = new java.util.ArrayList<>();
        trends.add(compareMetric("Weight", oldest.getWeight(), latest.getWeight(), "kg"));
        trends.add(compareMetric("Chest", oldest.getChest(), latest.getChest(), "cm"));
        trends.add(compareMetric("Upper Arms", oldest.getUpperArms(), latest.getUpperArms(), "cm"));
        trends.add(compareMetric("Thighs", oldest.getThighs(), latest.getThighs(), "cm"));
        trends.add(compareMetric("Calves", oldest.getCalves(), latest.getCalves(), "cm"));
        // Remove stable (null) insights
        trends.removeIf(i -> i == null);

        List<Insight> symmetry = checkSymmetry(latest);

        return new InsightReport(dateRange, trends, symmetry);
    }

    private Insight compareMetric(String name, double oldVal, double newVal, String unit) {
        double diff = newVal - oldVal;
        if (Math.abs(diff) < 0.1) {
            return null; // Stable, don't show an insight to keep UI clean
        }
        
        String direction = diff > 0 ? "Increased" : "Decreased";
        String type = diff > 0 ? "up" : "down";
        // Contextually, if weight increases, it might be good or bad, but let's just use up/down
        String desc = String.format("Changed by %.1f%s. Now %.1f%s.", Math.abs(diff), unit, newVal, unit);
        return new Insight(name, type, direction, desc, diff);
    }

    /**
     * Simple heuristic checks for symmetry.
     */
    private List<Insight> checkSymmetry(BodyMeasurementLog log) {
        List<Insight> insights = new java.util.ArrayList<>();
        
        // General aesthetic ratio heuristics (simplified)
        double armToCalfRatio = log.getUpperArms() > 0 ? log.getCalves() / log.getUpperArms() : 0;
        
        if (armToCalfRatio > 0) {
            if (armToCalfRatio > 1.2) {
                insights.add(new Insight("Arms vs Calves", "warning", "Upper Body Lagging", "Calves are significantly larger than upper arms.", armToCalfRatio));
            } else if (armToCalfRatio < 0.8) {
                insights.add(new Insight("Arms vs Calves", "warning", "Lower Body Lagging", "Upper arms are significantly larger than calves.", armToCalfRatio));
            } else {
                insights.add(new Insight("Arms vs Calves", "good", "Good Symmetry", "Well balanced between upper arms and calves.", armToCalfRatio));
            }
        }
        
        if (log.getChest() > 0 && log.getThighs() > 0) {
            double chestToThighRatio = log.getChest() / log.getThighs();
            if (chestToThighRatio < 1.5) {
                insights.add(new Insight("Chest vs Thighs", "good", "Strong Thighs", "Thigh development is strong relative to chest.", chestToThighRatio));
            }
        }

        return insights;
    }
    
    public void addLog(BodyMeasurementLog log) throws com.iwfc.exception.DuplicateDataException {
        // Ensure ID is generated
        if (log.getId() == null || log.getId().isEmpty()) {
            int size = dataStore.getMeasurementLogs().findAll().size();
            log.setId("BML" + String.format("%04d", size + 1));
        }
        dataStore.getMeasurementLogs().save(log);
    }
}
