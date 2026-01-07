package org.example;

import org.example.repository.PlaceRepository;
import org.example.repository.SubjectRepository;
import org.example.repository.TeacherRepository;
import org.example.repository.GroupRepository;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println("  UNIVERSITY SCHEDULE MANAGER - TEST");
            System.out.println("========================================");

            // Load all repositories
            System.out.println("\n[1] Loading PLACES from config...");
            PlaceRepository placeRepo = new PlaceRepository();
            placeRepo.printAllPlaces();

            System.out.println("\n[2] Loading SUBJECTS from config...");
            SubjectRepository subjectRepo = new SubjectRepository();
            subjectRepo.printAllSubjects();

            System.out.println("\n[3] Loading TEACHERS from config...");
            TeacherRepository teacherRepo = new TeacherRepository();
            teacherRepo.printAllTeachers();

            System.out.println("\n[4] Loading GROUPS from config...");
            GroupRepository groupRepo = new GroupRepository();
            groupRepo.printAllGroups();

            // Additional tests
            System.out.println("\n========================================");
            System.out.println("  QUICK ACCESS TESTS");
            System.out.println("========================================");

            // Test getting single items
            System.out.println("\nPlace FSEGA: " + placeRepo.getPlace("FSEGA"));
            System.out.println("Subject 'Fundamentals of Programming': " + subjectRepo.getSubject("Fundamentals of Programming"));
            System.out.println("Teacher 'Gabriel Mircea': " + teacherRepo.getTeacher("Gabriel Mircea"));
            System.out.println("Group '911': " + groupRepo.getGroup("911"));

            System.out.println("\n========================================");
            System.out.println("  ALL DATA LOADED SUCCESSFULLY!");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("Error loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}