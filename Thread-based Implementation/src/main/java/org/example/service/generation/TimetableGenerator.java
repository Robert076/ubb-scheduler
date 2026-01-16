package org.example.service.generation;

import org.example.context.TimetableDataContext;
import org.example.model.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TimetableGenerator {
    private final TimetableDataContext context;
    private final GenerationMetrics metrics;
    private final ExecutorService executor;

    public record SubjectGenerationResult(
            String subjectName,
            boolean success,
            int scheduledHours,
            long executionTimeMs
    ) {}

    public TimetableGenerator(TimetableDataContext context) {
        this.context = context;
        this.metrics = new GenerationMetrics();
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public GenerationResult generate() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        metrics.recordPhaseStart("GENERATION");

        try {
            // Get all subjects and sort by required hours (hardest first)
            Map<String, Subject> subjects = context.getSubjects();
            List<String> sortedSubjects = subjects.keySet().stream()
                    .sorted((s1, s2) -> Integer.compare(
                            subjects.get(s2).getTotalHoursPerWeek(),
                            subjects.get(s1).getTotalHoursPerWeek()
                    ))
                    .collect(Collectors.toList());

            // Initialize states
            Map<String, Teacher> teachers = context.getTeachers();
            Map<String, Group> groups = context.getGroups();

            TeacherScheduleState teacherState = new TeacherScheduleState(teachers);
            RoomScheduleState roomState = new RoomScheduleState(context.getPlaces());
            GroupScheduleState groupState = new GroupScheduleState(groups);

            // Schedule subjects in parallel
            List<Future<List<Activity>>> futures = new ArrayList<>();
            for (String subjectName : sortedSubjects) {
                Subject subject = subjects.get(subjectName);
                SubjectScheduler scheduler = new SubjectScheduler(
                        subjectName, subject, context, teacherState, roomState, groupState
                );
                futures.add(executor.submit(scheduler::schedule));
            }

            // Collect results
            List<Activity> allActivities = Collections.synchronizedList(new ArrayList<>());
            List<SubjectGenerationResult> subjectResults = Collections.synchronizedList(new ArrayList<>());
            
            for (int i = 0; i < sortedSubjects.size(); i++) {
                String subjectName = sortedSubjects.get(i);
                Future<List<Activity>> future = futures.get(i);
                long subStartTime = System.currentTimeMillis();
                try {
                    List<Activity> subjectActivities = future.get();
                    if (subjectActivities != null && !subjectActivities.isEmpty()) {
                        allActivities.addAll(subjectActivities);
                    }
                    
                    Subject subj = subjects.get(subjectName);
                    int requiredHours = (subj != null) ? subj.getTotalHoursPerWeek() : 0;
                    
                    // A subject is successful if we scheduled AT LEAST the required hours
                    // Note: activities list contains duplicates for courses (one per group)
                    // We need to count unique teacher-time slots.
                    long uniqueScheduledHours = subjectActivities == null ? 0 : 
                        subjectActivities.stream()
                            .filter(a -> a.groupId().equals("ALL_GROUPS") || (!a.activityType().equals("COURSE") && !a.activityType().equals("BUSY") && !a.activityType().equals("CLOSED")))
                            .mapToInt(Activity::getDurationHours)
                            .sum();

                    // Success rate check (strict 100% or allow near 100%)
                    boolean subjectSuccess = uniqueScheduledHours >= (requiredHours - 0.01);
                    
                    if (!subjectSuccess) {
                        String reason = (subjectActivities == null || subjectActivities.isEmpty()) 
                                ? "No activities scheduled." 
                                : "Only " + uniqueScheduledHours + "/" + requiredHours + " hours scheduled.";
                        System.err.println("Subject " + subjectName + " failed: " + reason);
                        metrics.recordError("Subject " + subjectName + " failed: " + reason);
                    }
                    
                    subjectResults.add(new SubjectGenerationResult(
                            subjectName,
                            subjectSuccess,
                            (int) uniqueScheduledHours,
                            System.currentTimeMillis() - subStartTime
                    ));
                    
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String errorMsg;
                    if (cause != null) {
                        java.io.StringWriter sw = new java.io.StringWriter();
                        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                        cause.printStackTrace(pw);
                        errorMsg = sw.toString();
                    } else {
                        errorMsg = e.toString();
                    }
                    System.err.println("Execution error for subject " + subjectName + ": " + errorMsg);
                    metrics.recordError("Execution error for subject " + subjectName + ": " + errorMsg);
                    subjectResults.add(new SubjectGenerationResult(
                            subjectName,
                            false,
                            0,
                            System.currentTimeMillis() - subStartTime
                    ));
                } catch (Exception e) {
                    String errorMsg = e.toString();
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    e.printStackTrace(pw);
                    errorMsg = sw.toString();
                    
                    System.err.println("Unexpected error for subject " + subjectName + ": " + errorMsg);
                    metrics.recordError("Unexpected error for subject " + subjectName + ": " + errorMsg);
                    subjectResults.add(new SubjectGenerationResult(
                            subjectName,
                            false,
                            0,
                            System.currentTimeMillis() - subStartTime
                    ));
                }
            }

            metrics.recordPhaseEnd("GENERATION");
            long totalTime = System.currentTimeMillis() - startTime;
            metrics.setTotalTime(totalTime);

            // Success Rate Calculation
            double totalScheduled = subjectResults.stream()
                    .mapToDouble(SubjectGenerationResult::scheduledHours)
                    .sum();
            
            double totalRequired = sortedSubjects.stream()
                    .map(subjects::get)
                    .filter(Objects::nonNull)
                    .mapToDouble(Subject::getTotalHoursPerWeek)
                    .sum();
            
            double successRate = (totalRequired > 0) ? (totalScheduled / totalRequired) * 100.0 : 0.0;
            // Round to 1 decimal place to avoid floating point noise in comparisons
            successRate = Math.round(successRate * 10.0) / 10.0;

            String fatalError = null;
            if (!metrics.getErrors().isEmpty()) {
                fatalError = metrics.getErrors().get(0);
                if (fatalError != null && fatalError.length() > 200) {
                    fatalError = fatalError.substring(0, 200) + "...";
                }
            } else if (successRate < 100.0) {
                fatalError = "Incomplete schedule (Success Rate: " + String.format("%.1f%%", successRate) + ")";
            }
            
            return new GenerationResult(allActivities, allActivities.size(), successRate, totalTime, metrics, subjectResults, fatalError);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            String detailedError = sw.toString();
            
            System.err.println("Fatal error during generation: " + detailedError);
            metrics.recordError("Fatal error during generation: " + detailedError);
            return new GenerationResult(new ArrayList<>(), 0, 0.0, 0, metrics, new ArrayList<>(), detailedError);
        } finally {
            metrics.printSummary();
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
