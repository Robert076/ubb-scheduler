package org.example.service.validation;

import org.example.context.TimetableDataContext;
import org.example.model.Subject;

import java.util.*;

/**
 * Teacher Definition Validator
 *
 * Validates that every subject has its mainTeacher defined in the system.
 * This is a prerequisite check - no point continuing if subjects reference missing teachers.
 *
 * Threading: SEQUENTIAL (fast, no parallelism needed)
 * Complexity: O(subjects)
 */
public class TeacherDefinitionValidator implements Validator {

    @Override
    public String getValidatorName() {
        return "TeacherDefinition";
    }

    @Override
    public ValidationResult validate(TimetableDataContext context) {
        long startTime = System.currentTimeMillis();
        List<String> invalidSubjects = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();

        // For each subject, verify its main teacher exists
        for (String subjectName : context.getSubjects().keySet()) {
            Subject subject = context.getSubjects().get(subjectName);
            String mainTeacher = subject.getMainTeacher();

            if (mainTeacher == null || mainTeacher.isBlank()) {
                invalidSubjects.add(subjectName + " -> main teacher is NULL");
            } else if (!context.getTeachers().containsKey(mainTeacher)) {
                invalidSubjects.add(subjectName + " -> teacher '" + mainTeacher + "' not found in system");
            }
        }

        boolean pass = invalidSubjects.isEmpty();
        long executionTime = System.currentTimeMillis() - startTime;

        details.put("subjectsChecked", context.getSubjects().size());
        details.put("invalidCount", invalidSubjects.size());

        if (!invalidSubjects.isEmpty()) {
            details.put("invalidSubjects", invalidSubjects);
        }

        ValidationResult.Status status = pass ? ValidationResult.Status.PASS : ValidationResult.Status.FAIL;
        String message = pass
                ? "All " + context.getSubjects().size() + " subjects have defined teachers"
                : "Found " + invalidSubjects.size() + " subjects with missing/invalid teachers: \n   " + String.join(";\n   ", invalidSubjects);

        return new ValidationResult(
                getValidatorName(),
                status,
                message,
                details,
                executionTime
        );
    }
}
