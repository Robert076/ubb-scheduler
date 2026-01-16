package org.example.service.generation;

/**
 * Configuration constants for the scheduler
 */
public class SchedulerConfig {
    // Timing constraints
    public static final int MIN_TIME_BETWEEN_BUILDINGS = 60; // minutes
    public static final int MAX_CONSECUTIVE_HOURS = 3;
    public static final int PREFERRED_GAP_DURATION = 30; // minutes

    // Scarcity thresholds
    public static final double SCARCITY_THRESHOLD = 0.7;

    // Time slots
    public static final int FIRST_HOUR = 8;
    public static final int LAST_HOUR = 19;
    public static final String[] DAYS = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
    public static final int[] HOURS = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
}
