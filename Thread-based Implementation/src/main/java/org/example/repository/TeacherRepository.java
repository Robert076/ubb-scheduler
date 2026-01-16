package org.example.repository;

import org.example.model.Teacher;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

/**
 * Repository for loading and managing Teacher entities from configuration.
 * Data is loaded once and exposed as unmodifiable collections.
 */
public class TeacherRepository {

    private final Map<String, Teacher> teachers;

    public TeacherRepository() throws Exception {
        this.teachers = Collections.unmodifiableMap(loadTeachers());
    }

    private Map<String, Teacher> loadTeachers() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("teachers-config.json");

        if (inputStream == null) {
            throw new RuntimeException("teachers-config.json not found in resources!");
        }

        Map<String, Teacher> parsedTeachers = objectMapper.readValue(inputStream,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Teacher.class));

        // Set name for each teacher (from JSON key)
        parsedTeachers.forEach((name, teacher) -> teacher.setName(name));

        return parsedTeachers;
    }

    /**
     * Get all teachers as an unmodifiable map.
     */
    public Map<String, Teacher> getAllTeachers() {
        return teachers;
    }

    /**
     * Get total available hours across all teachers per week.
     */
    public int getTotalTeacherHours() {
        return teachers.values().stream()
                .mapToInt(Teacher::getMaxHoursPerWeek)
                .sum();
    }

    /**
     * Print all teachers for debugging purposes.
     */
    public void printAllTeachers() {
        System.out.println("\n=== ALL TEACHERS ===");
        teachers.forEach((name, teacher) -> {
            System.out.println("\nTeacher: " + name);
            System.out.println(" Max hours/week: " + teacher.getMaxHoursPerWeek());
            System.out.println(" Preferred buildings: " + teacher.getPreferredBuildings());
            System.out.println(" Languages: " + teacher.getLanguages());
            System.out.println(" Can teach subjects: " + teacher.getSubjects().keySet());
        });
    }
}
