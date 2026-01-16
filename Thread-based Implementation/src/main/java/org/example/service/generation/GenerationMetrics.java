package org.example.service.generation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GenerationMetrics - Tracks all metrics during generation
 */
public class GenerationMetrics {
    private final Map<String, Long> phaseTimings = new ConcurrentHashMap<>();
    private final Map<String, Long> phaseStarts = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> subjectMetrics = new ConcurrentHashMap<>();
    private final Map<String, String> detailedMetrics = new ConcurrentHashMap<>();
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private volatile long totalTime = 0;

    /**
     * Record start of a phase
     */
    public void recordPhaseStart(String phaseName) {
        phaseStarts.put(phaseName, System.currentTimeMillis());
    }

    /**
     * Record a detailed metric
     */
    public void recordMetric(String key, String value) {
        detailedMetrics.put(key, value);
    }

    /**
     * Record an error
     */
    public void recordError(String error) {
        errors.add(error);
    }

    /**
     * Get all phase timings
     */
    public Map<String, Long> getPhaseTimes() {
        return new HashMap<>(phaseTimings);
    }

    /**
     * Get all detailed metrics
     */
    public Map<String, String> getMetrics() {
        return new HashMap<>(detailedMetrics);
    }

    /**
     * Get all errors
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Record end of a phase
     */
    public void recordPhaseEnd(String phaseName) {
        Long startTime = phaseStarts.get(phaseName);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            phaseTimings.put(phaseName, duration);
        }
    }

    /**
     * Record a metric for a subject
     */
    public void recordSubjectMetric(String subjectName, String metricName, Object value) {
        subjectMetrics.computeIfAbsent(subjectName, k -> new HashMap<>())
                .put(metricName, value);
    }

    /**
     * Get phase timing
     */
    public long getPhaseTime(String phaseName) {
        return phaseTimings.getOrDefault(phaseName, 0L);
    }

    /**
     * Get all phase timings
     */
    public Map<String, Long> getAllPhaseTimings() {
        return new HashMap<>(phaseTimings);
    }

    /**
     * Get metrics for a subject
     */
    public Map<String, Object> getSubjectMetrics(String subjectName) {
        return subjectMetrics.getOrDefault(subjectName, new HashMap<>());
    }

    /**
     * Get all subject metrics
     */
    public Map<String, Map<String, Object>> getAllSubjectMetrics() {
        return new HashMap<>(subjectMetrics);
    }

    /**
     * Set total generation time
     */
    public void setTotalTime(long time) {
        this.totalTime = time;
    }

    /**
     * Get total time
     */
    public long getTotalTime() {
        return totalTime;
    }

    /**
     * Print metrics summary
     */
    public void printSummary() {
        System.out.println("\n========== GENERATION METRICS ==========");
        System.out.println("Total Time: " + totalTime + " ms");

        System.out.println("\n--- Phase Timings ---");
        phaseTimings.forEach((phase, time) ->
                System.out.println(phase + ": " + time + " ms")
        );

        System.out.println("\n--- Subject Metrics ---");
        subjectMetrics.forEach((subject, metrics) -> {
            System.out.println(subject + ":");
            metrics.forEach((metric, value) ->
                    System.out.println("  " + metric + ": " + value)
            );
        });

        System.out.println("\n========================================\n");
    }

    /**
     * Check if phase exists
     */
    public boolean hasPhase(String phaseName) {
        return phaseTimings.containsKey(phaseName);
    }

    /**
     * Get number of subjects processed
     */
    public int getSubjectCount() {
        return subjectMetrics.size();
    }
}
