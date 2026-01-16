package org.example.context;

import org.example.model.*;
import org.example.repository.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TimetableDataContext - Central singleton that holds all loaded data and provides
 * pre-computed indices for fast O(1) lookups during validation.
 *
 * Initialized once at application startup and then immutable.
 * Thread-safe for reads (all collections are immutable).
 *
 * Provides critical indices:
 * - subjectsByTeacher: fast lookup of which subjects a teacher can teach
 * - groupsBySubject: fast lookup of which groups take a subject
 * - roomsByCapability: fast lookup of rooms by their capability (7 types)
 * - teachersByBuilding: fast lookup of teachers in a building
 */
public class TimetableDataContext {
    private static volatile TimetableDataContext instance;
    private static final Object lock = new Object();

    // Primary immutable collections (loaded from repos)
    private final Map<String, Group> groups;
    private final Map<String, Teacher> teachers;
    private final Map<String, Subject> subjects;
    private final Map<String, Place> places;

    // Pre-computed indices for O(1) lookups
    private final Map<String, List<String>> subjectsByTeacher;        // teacher name -> list of subject names
    private final Map<String, List<String>> groupsBySubject;          // subject name -> list of group ids
    private final Map<String, List<String>> teachersByBuilding;       // building name -> list of teacher names
    private final Map<String, Integer> roomCountByCapability;         // capability type -> count of rooms
    private final Map<String, List<Room>> roomsByCapability;          // capability type -> list of rooms
    private final Map<String, Integer> totalHoursByCapability;        // capability type -> total available hours

    // Aggregated statistics
    private final int totalTeacherHours;
    private final int totalRequiredHours;
    private final int totalCourseHours;
    private final int totalSeminarHours;
    private final int totalLaboratoryHours;

    /**
     * Private constructor - initializes all data and pre-computed indices.
     */
    private TimetableDataContext(GroupRepository groupRepo,
                                TeacherRepository teacherRepo,
                                SubjectRepository subjectRepo,
                                PlaceRepository placeRepo) throws Exception {
        this.groups = Collections.unmodifiableMap(groupRepo.getAllGroups());
        this.teachers = Collections.unmodifiableMap(teacherRepo.getAllTeachers());
        this.subjects = Collections.unmodifiableMap(subjectRepo.getAllSubjects());
        this.places = Collections.unmodifiableMap(placeRepo.getAllPlaces());

        // Build all indices
        this.subjectsByTeacher = Collections.unmodifiableMap(buildSubjectsByTeacherIndex());
        this.groupsBySubject = Collections.unmodifiableMap(buildGroupsBySubjectIndex());
        this.teachersByBuilding = Collections.unmodifiableMap(buildTeachersByBuildingIndex());

        // Room capability indices
        this.roomsByCapability = Collections.unmodifiableMap(buildRoomsByCapabilityIndex(placeRepo));
        this.roomCountByCapability = Collections.unmodifiableMap(buildRoomCountByCapabilityIndex());
        this.totalHoursByCapability = Collections.unmodifiableMap(buildTotalHoursByCapabilityIndex(placeRepo));

        // Aggregate statistics
        this.totalTeacherHours = teacherRepo.getTotalTeacherHours();
        this.totalRequiredHours = subjectRepo.getTotalRequiredHours();
        this.totalCourseHours = subjectRepo.getTotalCourseHours();
        this.totalSeminarHours = subjectRepo.getTotalSeminarHours();
        this.totalLaboratoryHours = subjectRepo.getTotalLaboratoryHours();
    }

    /**
     * Get or create singleton instance using double-checked locking.
     */
    public static TimetableDataContext getInstance(GroupRepository groupRepo,
                                                     TeacherRepository teacherRepo,
                                                     SubjectRepository subjectRepo,
                                                     PlaceRepository placeRepo) throws Exception {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new TimetableDataContext(groupRepo, teacherRepo, subjectRepo, placeRepo);
                }
            }
        }
        return instance;
    }

    /**
     * Reset singleton (useful for testing).
     */
    public static void reset() {
        synchronized (lock) {
            instance = null;
        }
    }

    // ==================== PRIMARY GETTERS ====================

    public Map<String, Group> getGroups() { return groups; }
    public Map<String, Teacher> getTeachers() { return teachers; }
    public Map<String, Subject> getSubjects() { return subjects; }
    public Map<String, Place> getPlaces() { return places; }

    // ==================== INDEX GETTERS ====================

    public Map<String, List<String>> getSubjectsByTeacher() { return subjectsByTeacher; }
    public Map<String, List<String>> getGroupsBySubject() { return groupsBySubject; }
    public Map<String, List<String>> getTeachersByBuilding() { return teachersByBuilding; }
    public Map<String, List<Room>> getRoomsByCapability() { return roomsByCapability; }
    public Map<String, Integer> getRoomCountByCapability() { return roomCountByCapability; }
    public Map<String, Integer> getTotalHoursByCapability() { return totalHoursByCapability; }

    // ==================== STATISTICS GETTERS ====================

    public int getTotalTeacherHours() { return totalTeacherHours; }
    public int getTotalRequiredHours() { return totalRequiredHours; }
    public int getTotalCourseHours() { return totalCourseHours; }
    public int getTotalSeminarHours() { return totalSeminarHours; }
    public int getTotalLaboratoryHours() { return totalLaboratoryHours; }

    // ==================== CONVENIENCE GETTERS ====================

    public List<String> getSubjectsForTeacher(String teacherName) {
        return subjectsByTeacher.getOrDefault(teacherName, List.of());
    }

    public List<String> getGroupsForSubject(String subjectName) {
        return groupsBySubject.getOrDefault(subjectName, List.of());
    }

    public List<String> getTeachersInBuilding(String buildingName) {
        return teachersByBuilding.getOrDefault(buildingName, List.of());
    }

    public List<Room> getRoomsWithCapability(String capability) {
        return roomsByCapability.getOrDefault(capability, List.of());
    }

    // ==================== INDEX BUILDERS ====================

    /**
     * Build index: teacher name -> list of subject names they can teach
     */
    private Map<String, List<String>> buildSubjectsByTeacherIndex() {
        Map<String, List<String>> index = new HashMap<>();
        teachers.forEach((teacherName, teacher) -> {
            if (teacher.getSubjects() != null) {
                index.put(teacherName, new ArrayList<>(teacher.getSubjects().keySet()));
            } else {
                index.put(teacherName, List.of());
            }
        });
        return index;
    }

    /**
     * Build index: subject name -> list of group ids that take it
     */
    private Map<String, List<String>> buildGroupsBySubjectIndex() {
        Map<String, List<String>> index = new HashMap<>();
        groups.forEach((groupId, group) -> {
            if (group.getSubjects() != null) {
                group.getSubjects().forEach(subjectName ->
                    index.computeIfAbsent(subjectName, k -> new ArrayList<>()).add(groupId)
                );
            }
        });
        return index;
    }

    /**
     * Build index: building name -> list of teacher names who prefer it
     */
    private Map<String, List<String>> buildTeachersByBuildingIndex() {
        Map<String, List<String>> index = new HashMap<>();
        teachers.forEach((teacherName, teacher) -> {
            if (teacher.getPreferredBuildings() != null) {
                teacher.getPreferredBuildings().forEach(buildingName ->
                    index.computeIfAbsent(buildingName, k -> new ArrayList<>()).add(teacherName)
                );
            }
        });
        return index;
    }

    /**
     * Build index: capability type -> list of rooms with that capability
     */
    private Map<String, List<Room>> buildRoomsByCapabilityIndex(PlaceRepository placeRepo) {
        return placeRepo.getRoomsByCapability();
    }

    /**
     * Build index: capability type -> count of rooms
     */
    private Map<String, Integer> buildRoomCountByCapabilityIndex() {
        return roomsByCapability.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    /**
     * Build index: capability type -> total available hours per week
     * Calculated as: sum of (place hours per week × number of rooms with that capability in that place)
     */
    private Map<String, Integer> buildTotalHoursByCapabilityIndex(PlaceRepository placeRepo) {
        Map<String, Integer> index = new HashMap<>();
        String[] capabilities = {"COURSE_ONLY", "SEMINAR_ONLY", "LAB_ONLY", "COURSE_SEMINAR", "COURSE_LAB", "SEMINAR_LAB", "ALL"};

        for (String capability : capabilities) {
            int totalHours = 0;
            for (Place place : places.values()) {
                int placeHoursPerWeek = placeRepo.calculatePlaceHoursPerWeek(place);
                int roomsWithCapability = (int) place.getRooms().values().stream()
                    .filter(room -> placeRepo.computeRoomCapability(room).equals(capability))
                    .count();
                totalHours += placeHoursPerWeek * roomsWithCapability;
            }
            index.put(capability, totalHours);
        }

        return index;
    }

    // ==================== VALIDATION SUPPORT METHODS ====================

    /**
     * Get all teachers capable of teaching a specific subject.
     */
    public List<Teacher> getCapableTeachers(String subjectName) {
        return teachers.values().stream()
            .filter(teacher -> teacher.canTeachSubject(subjectName))
            .collect(Collectors.toList());
    }

    /**
     * Get all teachers capable of teaching a subject AND speaking a language.
     */
    public List<Teacher> getCapableTeachersForLanguage(String subjectName, String language) {
        return getCapableTeachers(subjectName).stream()
            .filter(teacher -> teacher.speaksLanguage(language))
            .collect(Collectors.toList());
    }

    /**
     * Get total available hours for all teachers who can teach a subject.
     */
    public int getTotalCapacityForSubject(String subjectName) {
        return getCapableTeachers(subjectName).stream()
            .mapToInt(Teacher::getMaxHoursPerWeek)
            .sum();
    }

    /**
     * Print complete context summary for debugging.
     */
    public void printContextSummary() {
        System.out.println("\n========== TIMETABLE DATA CONTEXT SUMMARY ==========");
        System.out.println("Groups: " + groups.size());
        System.out.println("Teachers: " + teachers.size());
        System.out.println("Subjects: " + subjects.size());
        System.out.println("Places: " + places.size());
        System.out.println("Total Rooms: " + roomsByCapability.values().stream().mapToInt(List::size).sum());

        System.out.println("\n--- AGGREGATED STATISTICS ---");
        System.out.println("Total Teacher Hours/Week: " + totalTeacherHours);
        System.out.println("Total Required Hours/Week: " + totalRequiredHours);
        System.out.println("  - Courses: " + totalCourseHours);
        System.out.println("  - Seminars: " + totalSeminarHours);
        System.out.println("  - Laboratories: " + totalLaboratoryHours);

        System.out.println("\n--- ROOM CAPABILITY BREAKDOWN ---");
        roomCountByCapability.forEach((capability, count) ->
            System.out.println("  " + capability + ": " + count + " rooms (" + totalHoursByCapability.get(capability) + " hours available)")
        );

        System.out.println("\n=== Context initialization complete ===\n");
    }
}
