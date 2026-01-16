package org.example.service.validation;

import org.example.context.TimetableDataContext;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Teacher Availability & Building Preferences Validator
 * 
 * Simple check - total teacher hours vs total required hours (rough estimate)
 * Preference Check - ensure prefered buildings fit the teachers
 * 
 * Threading: SEQUENTIAL (fast aggregate stats)
 * Complexity: O(teachers + buildings)
 */
public class TeacherAvailabilityValidator implements Validator {

    @Override
    public String getValidatorName() {
        return "TeacherAvailability";
    }

    @Override
    public ValidationResult validate(TimetableDataContext context) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> details = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        // Total hours check
        int totalTeacherHours = context.getTotalTeacherHours();
        int totalRequiredHours = context.getTotalRequiredHours();
        double utilizationRatio = (double) totalRequiredHours / totalTeacherHours;

        details.put("totalTeacherHours", totalTeacherHours);
        details.put("totalRequiredHours", totalRequiredHours);
        details.put("utilizationRatio", String.format("%.2f%%", utilizationRatio * 100));
        details.put("headroom", String.format("%.2f%%", (1.0 - utilizationRatio) * 100));

        // Building preference analysis
        Map<String, Integer> teachersPerBuilding = analyzeTeacherDistribution(context);
        Map<String, Integer> roomsPerBuilding = context.getPlaces().keySet().stream()
            .collect(Collectors.toMap(
                building -> building,
                building -> context.getPlaces().get(building).getRooms().size()
            ));

        // Check for empty buildings
        for (String building : roomsPerBuilding.keySet()) {
            int teacherCount = teachersPerBuilding.getOrDefault(building, 0);
            int roomCount = roomsPerBuilding.get(building);

            if (roomCount > 0 && teacherCount == 0) {
                warnings.add("Building '" + building + "' has " + roomCount + " rooms but NO teachers prefer it");
            }
        }

        details.put("teachersPerBuilding", teachersPerBuilding);
        details.put("roomsPerBuilding", roomsPerBuilding);

        if (!warnings.isEmpty()) {
            details.put("buildingWarnings", warnings);
        }

        // Determine status
        ValidationResult.Status status = ValidationResult.Status.PASS;
        String message;

        if (utilizationRatio > 1.0) {
            // Not enough total hours
            status = ValidationResult.Status.FAIL;
            message = String.format("FAIL: Total teacher hours insufficient (%.2f%% utilization)", 
                utilizationRatio * 100);
        } else if (utilizationRatio > 0.85) {
            // Very tight on hours
            status = ValidationResult.Status.WARN;
            message = String.format("WARN: Teacher capacity is tight (%.2f%% utilization)", 
                utilizationRatio * 100);
        } else if (!warnings.isEmpty()) {
            // Building issues
            status = ValidationResult.Status.WARN;
            message = "WARN: Some buildings have no preferred teachers (" + warnings.size() + " issues)";
        } else {
            message = String.format("OK: Teacher hours sufficient (%.2f%% utilization) and building distribution OK", 
                utilizationRatio * 100);
        }

        long executionTime = System.currentTimeMillis() - startTime;

        return new ValidationResult(
            getValidatorName(),
            status,
            message,
            details,
            executionTime
        );
    }

    /**
     * Analyze how many teachers prefer each building
     */
    private Map<String, Integer> analyzeTeacherDistribution(TimetableDataContext context) {
        Map<String, Integer> distribution = new HashMap<>();

        // Initialize with all places
        for (String placeName : context.getPlaces().keySet()) {
            distribution.put(placeName, 0);
        }

        // Count teachers per building
        for (String building : context.getTeachersByBuilding().keySet()) {
            int count = context.getTeachersByBuilding().get(building).size();
            distribution.put(building, count);
        }

        return distribution;
    }
}
