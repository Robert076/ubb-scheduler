package org.example.repository;

import org.example.model.Subject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SubjectRepository {
    private Map<String, Subject> subjects = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public SubjectRepository() throws Exception {
        loadSubjects();
    }

    private void loadSubjects() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("subjects-config.json");
        
        if (inputStream == null) {
            throw new RuntimeException("subjects-config.json not found in resources!");
        }

        // Parse JSON as Map<String, Subject>
        Map<String, Subject> parsedSubjects = objectMapper.readValue(inputStream,
            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Subject.class));

        // Set name for each subject
        parsedSubjects.forEach((name, subject) -> subject.setName(name));
        
        this.subjects = parsedSubjects;
    }

    public Subject getSubject(String name) {
        return subjects.get(name);
    }

    public Map<String, Subject> getAllSubjects() {
        return new HashMap<>(subjects);
    }

    public void printAllSubjects() {
        System.out.println("\n=== ALL SUBJECTS ===");
        subjects.forEach((name, subject) -> {
            System.out.println("\nSubject: " + name);
            System.out.println("  Teacher: " + subject.getMainTeacher());
            System.out.println("  Language: " + subject.getLanguage());
            System.out.println("  Courses/week: " + subject.getCoursesPerWeek() + " x " + subject.getCourseLenght() + "h");
            System.out.println("  Seminars/week: " + subject.getSeminarsPerWeek() + " x " + subject.getSeminarLenght() + "h");
            System.out.println("  Labs/week: " + subject.getLaboratoriesPerWeek() + " x " + subject.getLaboratoriesLenght() + "h");
            System.out.println("  Total hours/week: " + subject.getTotalHoursPerWeek());
        });
    }
}