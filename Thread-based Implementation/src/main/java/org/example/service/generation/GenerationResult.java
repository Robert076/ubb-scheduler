package org.example.service.generation;

import org.example.model.Activity;

import java.util.*;

/**
 * Complete result of timetable generation
 */
public class GenerationResult {
    private final List<Activity> activities;
    private final int totalActivities;
    private final double successRate;
    private final long totalTime;
    private final GenerationMetrics metrics;
    private final List<TimetableGenerator.SubjectGenerationResult> subjectResults;
    private final String errorMessage;

    public GenerationResult(List<Activity> activities, int totalActivities,
                            double successRate, long totalTime, GenerationMetrics metrics,
                            List<TimetableGenerator.SubjectGenerationResult> subjectResults,
                            String errorMessage) {
        this.activities = activities;
        this.totalActivities = totalActivities;
        this.successRate = successRate;
        this.totalTime = totalTime;
        this.metrics = metrics;
        this.subjectResults = subjectResults;
        this.errorMessage = errorMessage;
    }

    public List<Activity> getActivities() { return activities; }
    public int getTotalActivities() { return totalActivities; }
    public double getSuccessRate() { return successRate; }
    public long getTotalTimeMs() { return totalTime; }
    public GenerationMetrics metrics() { return metrics; }
    public List<TimetableGenerator.SubjectGenerationResult> subjectResults() { return subjectResults; }
    public String errorMessage() { return errorMessage; }
    public boolean success() { return successRate >= 100.0 && errorMessage == null; }
}
