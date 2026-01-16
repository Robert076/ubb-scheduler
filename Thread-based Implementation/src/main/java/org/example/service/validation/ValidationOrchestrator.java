package org.example.service.validation;

import org.example.context.TimetableDataContext;
import org.example.repository.PlaceRepository;
import java.util.*;
import java.util.concurrent.*;

/**
 * ValidationOrchestrator - Runs all validators in correct order
 *
 * Execution order:
 * 1. V0 (must pass - blocks rest if fails)
 * 2. V1+V5, V2, V3+V6, V4 (run in parallel - non-blocking)
 * 3. Generate comprehensive report
 *
 * NOTE: WARN validators never block - they're informative!
 */
public class ValidationOrchestrator {
    private static final int THREAD_POOL_SIZE = 5;
    private final ExecutorService mainExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final TimetableDataContext dataContext;
    private final PlaceRepository placeRepository;

    public ValidationOrchestrator(TimetableDataContext dataContext, PlaceRepository placeRepository) {
        this.dataContext = dataContext;
        this.placeRepository = placeRepository;
    }

    /**
     * Run all validations in correct order with proper dependency handling
     */
    public ValidationReport runAllValidations() {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║   RUNNING TIMETABLE VALIDATIONS (v1.0)    ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        long totalStartTime = System.currentTimeMillis();
        List<ValidationResult> results = new ArrayList<>();

        // PHASE 1: V0 (BLOCKING - must pass)
        System.out.println("[PHASE 1] Running Teacher Definition Validator (prerequisite check)...");
        TeacherDefinitionValidator v0 = new TeacherDefinitionValidator();
        ValidationResult v0Result = v0.validate(dataContext);
        results.add(v0Result);
        System.out.println("  → [" + getStatusIcon(v0Result.getStatus()) + "] " + v0Result.getMessage());

        // If V0 fails, block completely
        if (v0Result.getStatus() == ValidationResult.Status.FAIL) {
            System.out.println("\n⚠️  Teacher Definition Validator FAILED - Cannot proceed with other validations");
            System.out.println("    Fix teacher definitions and try again.\n");
            long totalTime = System.currentTimeMillis() - totalStartTime;
            return new ValidationReport(results, totalTime);
        }

        // PHASE 2: V1+V5, V2, V3+V6, V4 (PARALLEL - non-blocking)
        System.out.println("\n[PHASE 2] Running secondary validators in parallel...");
        List<Future<ValidationResult>> futures = new ArrayList<>();

        // V1+V5 validator
        TeacherCapacityValidator v1v5 = new TeacherCapacityValidator();
        Future<ValidationResult> f1 = mainExecutor.submit(() -> {
            System.out.println("  Teacher Capacity Validator Running (parallel by subjects)...");
            return v1v5.validate(dataContext);
        });
        futures.add(f1);

        // V2 validator
        RoomCapacityValidator v2 = new RoomCapacityValidator(placeRepository);
        Future<ValidationResult> f2 = mainExecutor.submit(() -> {
            System.out.println("  Room Capacity Validator running (parallel by places)...");
            return v2.validate(dataContext);
        });
        futures.add(f2);

        // V3+V6 validator
        TeacherAvailabilityValidator v3v6 = new TeacherAvailabilityValidator();
        Future<ValidationResult> f3 = mainExecutor.submit(() -> {
            System.out.println("  Teacher Availability Validator Running (sequential aggregate)...");
            return v3v6.validate(dataContext);
        });
        futures.add(f3);

        // V4 validator
        TimeSlotCollisionValidator v4 = new TimeSlotCollisionValidator();
        Future<ValidationResult> f4 = mainExecutor.submit(() -> {
            System.out.println("  Time Slot Collision Validator running (parallel matrix build)...");
            return v4.validate(dataContext);
        });
        futures.add(f4);

        // Collect results as they complete
        for (Future<ValidationResult> future : futures) {
            try {
                ValidationResult result = future.get();
                results.add(result);
                System.out.println("  ✓ [" + getStatusIcon(result.getStatus()) + "] " + result.getMessage());
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Validation interrupted", e);
            }
        }

        // Cleanup executors
        v1v5.shutdown();
        v2.shutdown();
        v4.shutdown();

        long totalTime = System.currentTimeMillis() - totalStartTime;
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║     ALL VALIDATIONS COMPLETED              ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        return new ValidationReport(results, totalTime);
    }

    /**
     * Shutdown executor gracefully
     */
    public void shutdown() {
        mainExecutor.shutdown();
        try {
            if (!mainExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                mainExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            mainExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String getStatusIcon(ValidationResult.Status status) {
        return switch (status) {
            case PASS -> "✓ PASS";
            case WARN -> "⚠ WARN";
            case FAIL -> "✗ FAIL";
        };
    }

    /**
     * Validation Report - summarizes all results
     */
    public static class ValidationReport {
        private final List<ValidationResult> results;
        private final long totalTimeMs;

        public ValidationReport(List<ValidationResult> results, long totalTimeMs) {
            this.results = Collections.unmodifiableList(new ArrayList<>(results));
            this.totalTimeMs = totalTimeMs;
        }

        public List<ValidationResult> getResults() {
            return results;
        }

        public long getTotalTimeMs() {
            return totalTimeMs;
        }

        /**
         * Get overall status:
         * - FAIL if any critical validator fails
         * - WARN if any validator warns (but no failures)
         * - PASS if all pass
         *
         * IMPORTANT: WARN does NOT block generation!
         */
        public ValidationResult.Status getOverallStatus() {
            // Only FAIL blocks
            if (results.stream().anyMatch(r -> r.getStatus() == ValidationResult.Status.FAIL)) {
                return ValidationResult.Status.FAIL;
            }

            // WARN is informative but non-blocking
            if (results.stream().anyMatch(r -> r.getStatus() == ValidationResult.Status.WARN)) {
                return ValidationResult.Status.WARN;
            }

            return ValidationResult.Status.PASS;
        }
    }
}
