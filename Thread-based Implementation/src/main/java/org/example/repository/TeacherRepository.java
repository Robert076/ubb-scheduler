package org.example.repository;

import org.example.model.Teacher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TeacherRepository {
    private Map<String, Teacher> teachers = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public TeacherRepository() throws Exception {
        loadTeachers();
    }

    private void loadTeachers() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("teachers-config.json");
        
        if (inputStream == null) {
            throw new RuntimeException("teachers-config.json not found in resources!");
        }

        // Parse JSON as Map<String, Teacher>
        Map<String, Teacher> parsedTeachers = objectMapper.readValue(inputStream,
            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Teacher.class));

        // Set name for each teacher
        parsedTeachers.forEach((name, teacher) -> teacher.setName(name));
        
        this.teachers = parsedTeachers;
    }

    public Teacher getTeacher(String name) {
        return teachers.get(name);
    }

    public Map<String, Teacher> getAllTeachers() {
        return new HashMap<>(teachers);
    }

    public void printAllTeachers() {
        System.out.println("\n=== ALL TEACHERS ===");
        teachers.forEach((name, teacher) -> {
            System.out.println("\nTeacher: " + name);
            System.out.println("  Max hours/week: " + teacher.getMaxHoursPerWeek());
            System.out.println("  Preferred buildings: " + teacher.getPreferredBuildings());
            System.out.println("  Languages: " + teacher.getLanguages());
            System.out.println("  Can teach subjects: " + teacher.getSubjects().keySet());
        });
    }
}