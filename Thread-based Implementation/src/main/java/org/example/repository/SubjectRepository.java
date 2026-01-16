package org.example.repository;

import org.example.model.Subject;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

/**
 * Repository for loading and managing Subject entities from configuration.
 * Data is loaded once and exposed as unmodifiable collections.
 */
public class SubjectRepository {

    private final Map<String, Subject> subjects;

    public SubjectRepository() throws Exception {
        this.subjects = Collections.unmodifiableMap(loadSubjects());
    }

    private Map<String, Subject> loadSubjects() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("subjects-config.json");

        if (inputStream == null) {
            throw new RuntimeException("subjects-config.json not found in resources!");
        }

        Map<String, Subject> parsedSubjects = objectMapper.readValue(inputStream,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Subject.class));

        // Set name for each subject (from JSON key)
        parsedSubjects.forEach((name, subject) -> subject.setName(name));

        return parsedSubjects;
    }

    /**
     * Get all subjects as an unmodifiable map.
     */
    public Map<String, Subject> getAllSubjects() {
        return subjects;
    }

    /**
     * Get total required hours per week across all subjects.
     * Includes courses, seminars, and laboratories.
     */
    public int getTotalRequiredHours() {
        return subjects.values().stream()
                .mapToInt(Subject::getTotalHoursPerWeek)
                .sum();
    }

    /**
     * Get total course hours needed per week.
     */
    public int getTotalCourseHours() {
        return subjects.values().stream()
                .mapToInt(s -> s.getCoursesPerWeek() * s.getCourseLenght())
                .sum();
    }

    /**
     * Get total seminar hours needed per week.
     */
    public int getTotalSeminarHours() {
        return subjects.values().stream()
                .mapToInt(s -> s.getSeminarsPerWeek() * s.getSeminarLenght())
                .sum();
    }

    /**
     * Get total laboratory hours needed per week.
     */
    public int getTotalLaboratoryHours() {
        return subjects.values().stream()
                .mapToInt(s -> (int) (s.getLaboratoriesPerWeek() * s.getLaboratoriesLenght()))
                .sum();
    }

    /**
     * Print all subjects for debugging purposes.
     */
    public void printAllSubjects() {
        System.out.println("\n=== ALL SUBJECTS ===");
        subjects.forEach((name, subject) -> {
            System.out.println("\nSubject: " + name);
            System.out.println(" Teacher: " + subject.getMainTeacher());
            System.out.println(" Language: " + subject.getLanguage());
            System.out.println(" Courses/week: " + subject.getCoursesPerWeek() + " x " + subject.getCourseLenght() + "h");
            System.out.println(" Seminars/week: " + subject.getSeminarsPerWeek() + " x " + subject.getSeminarLenght() + "h");
            System.out.println(" Labs/week: " + subject.getLaboratoriesPerWeek() + " x " + subject.getLaboratoriesLenght() + "h");
            System.out.println(" Total hours/week: " + subject.getTotalHoursPerWeek());
        });
    }
}
