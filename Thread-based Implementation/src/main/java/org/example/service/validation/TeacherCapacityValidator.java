package org.example.service.validation;

import org.example.context.TimetableDataContext;
import org.example.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Teacher Capacity & Language Matching Validator
 * 
 * 1. Validates that for each subject, there are enough capable teachers
 *     with sufficient hours to cover all groups requiring that subject.
 * 2. Validates that teachers speaking a subject's language exist.
 * 
 * Threading: PARALLEL by subjects
 * - Each subject analysis runs independently on separate thread
 * - Thread pool size = CPU cores
 * Complexity: O(subjects × teachers) with parallelism
 */
public class TeacherCapacityValidator implements Validator {
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    @Override
    public String getValidatorName() {
        return "TeacherCapacity";
    }

    /**
     * Subject coverage analysis - stores data for one subject
     */
    private static class SubjectCoverageAnalysis {
        String subjectName;
        int neededHours;
        int availableHours;
        List<String> capableTeachers;
        boolean languageMatch;

        SubjectCoverageAnalysis(String subjectName, int neededHours, int availableHours,
                               List<String> capableTeachers, boolean languageMatch) {
            this.subjectName = subjectName;
            this.neededHours = neededHours;
            this.availableHours = availableHours;
            this.capableTeachers = capableTeachers;
            this.languageMatch = languageMatch;
        }

        boolean hasCapacityDeficit() {
            return availableHours < neededHours;
        }

        boolean hasLanguageIssue() {
            return !languageMatch;
        }
    }

    @Override
    public ValidationResult validate(TimetableDataContext context) {
        long startTime = System.currentTimeMillis();

        Map<String, SubjectCoverageAnalysis> analyses = Collections.synchronizedMap(new HashMap<>());
        List<Future<?>> futures = new ArrayList<>();

        // Spawn parallel task for each subject
        for (String subjectName : context.getSubjects().keySet()) {
            Future<?> task = executor.submit(() -> {
                SubjectCoverageAnalysis analysis = analyzeSubjectCoverage(subjectName, context);
                analyses.put(subjectName, analysis);
            });
            futures.add(task);
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("V1V5 validation interrupted", e);
            }
        }

        // Aggregate results
        List<String> capacityDeficits = new ArrayList<>();
        List<String> languageIssues = new ArrayList<>();

        analyses.forEach((subjectName, analysis) -> {
            if (analysis.hasCapacityDeficit()) {
                capacityDeficits.add(String.format("%s: need %dh, have %dh (teachers: %s)",
                    subjectName, analysis.neededHours, analysis.availableHours,
                    analysis.capableTeachers.size()));
            }
            if (analysis.hasLanguageIssue()) {
                languageIssues.add(subjectName + " -> no teachers speak required language");
            }
        });

        boolean pass = capacityDeficits.isEmpty() && languageIssues.isEmpty();
        long executionTime = System.currentTimeMillis() - startTime;

        Map<String, Object> details = new HashMap<>();
        details.put("subjectsAnalyzed", analyses.size());
        details.put("capacityDeficitCount", capacityDeficits.size());
        details.put("languageIssueCount", languageIssues.size());

        if (!capacityDeficits.isEmpty()) {
            details.put("capacityDeficits", capacityDeficits);
        }
        if (!languageIssues.isEmpty()) {
            details.put("languageIssues", languageIssues);
        }

        ValidationResult.Status status = pass ? ValidationResult.Status.PASS : ValidationResult.Status.FAIL;
        String message = pass
            ? "All subjects can be covered by available teachers"
            : "Deficits found: " + capacityDeficits.size() + " capacity, " + languageIssues.size() + " language";

        return new ValidationResult(
            getValidatorName(),
            status,
            message,
            details,
            executionTime
        );
    }

    /**
     * Analyze coverage for a single subject (runs in parallel)
     */
    private SubjectCoverageAnalysis analyzeSubjectCoverage(String subjectName, TimetableDataContext context) {
        Subject subject = context.getSubjects().get(subjectName);

        // Calculate needed hours
        int neededHours = 0;
        boolean allGroupsLanguageMatch = true;

        List<String> groupsNeedingThis = context.getGroupsBySubject().getOrDefault(subjectName, List.of());
        for (String groupId : groupsNeedingThis) {
            Group group = context.getGroups().get(groupId);
            neededHours += calculateGroupSubjectHours(group, subject);
        }

        // Find capable teachers (V1)
        List<Teacher> capableTeachers = context.getCapableTeachers(subjectName);

        // Check language match (V5)
        boolean languageMatch = groupsNeedingThis.stream()
            .allMatch(groupId -> {
                String groupLanguage = context.getGroups().get(groupId).getLanguage();
                return capableTeachers.stream()
                    .anyMatch(t -> t.speaksLanguage(groupLanguage));
            });

        // Calculate available hours (optimistic - sum all capable teachers)
        int availableHours = capableTeachers.stream()
            .mapToInt(Teacher::getMaxHoursPerWeek)
            .sum();

        List<String> capableTeacherNames = capableTeachers.stream()
            .map(Teacher::getName)
            .collect(Collectors.toList());

        return new SubjectCoverageAnalysis(
            subjectName,
            neededHours,
            availableHours,
            capableTeacherNames,
            languageMatch
        );
    }

    /**
     * Calculate total hours needed for a group to take a subject
     * Accounts for splits in seminars and labs
     */
    private int calculateGroupSubjectHours(Group group, Subject subject) {
        int courseHours = subject.getCoursesPerWeek() * subject.getCourseLenght();
        int seminarHours = subject.getSeminarsPerWeek() * subject.getSeminarLenght() * group.getSeminarySplit();
        int labHours = (int) (subject.getLaboratoriesPerWeek() * subject.getLaboratoriesLenght() * group.getLaboratorySplit());

        return courseHours + seminarHours + labHours;
    }

    /**
     * Shutdown executor service gracefully
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
