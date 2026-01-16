package org.example.service.generation;

import org.example.context.TimetableDataContext;
import org.example.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SubjectScheduler - Schedules all activities for a single subject
 * Uses backtracking with constraint satisfaction
 */
public class SubjectScheduler {
    private final String subjectName;
    private final Subject subject;
    private final TimetableDataContext context;
    private final TeacherScheduleState teacherState;
    private final RoomScheduleState roomState;
    private final GroupScheduleState groupState;

    private final List<Activity> scheduledActivities = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random();

    public SubjectScheduler(String subjectName, Subject subject, TimetableDataContext context,
                            TeacherScheduleState teacherState, RoomScheduleState roomState,
                            GroupScheduleState groupState) {
        this.subjectName = subjectName;
        this.subject = subject;
        this.context = context;
        this.teacherState = teacherState;
        this.roomState = roomState;
        this.groupState = groupState;
    }

    /**
     * Main scheduling method - returns list of scheduled activities
     */
    public List<Activity> schedule() {
        try {
            // Get groups that take this subject
            List<Group> groupsForSubject = getGroupsForSubject();
            if (groupsForSubject.isEmpty()) {
                return new ArrayList<>();
            }

            // Get teachers who can teach this subject
            List<Teacher> capableTeachers = getCapableTeachers();
            if (capableTeachers.isEmpty()) {
                return new ArrayList<>();
            }

            // Schedule each activity type
            boolean coursesOk = scheduleCourses(groupsForSubject, capableTeachers);
            boolean seminarsOk = scheduleSeminars(groupsForSubject, capableTeachers);
            boolean labsOk = scheduleLaboratories(groupsForSubject, capableTeachers);
            
            if (!coursesOk) {
                String msg = "Subject " + subjectName + ": Failed to schedule courses completely.";
                System.err.println(msg);
            }
            if (!seminarsOk) {
                String msg = "Subject " + subjectName + ": Failed to schedule seminars completely.";
                System.err.println(msg);
            }
            if (!labsOk) {
                String msg = "Subject " + subjectName + ": Failed to schedule laboratories completely.";
                System.err.println(msg);
            }

            // Important: We need to filter scheduledActivities to avoid duplicates in reporting
            // If we have course with 3 groups, we added 3 group activities + 1 teacher activity
            // But for 'required hours' count we should probably count teacher slots.
            
            return new ArrayList<>(scheduledActivities);
        } catch (Exception e) {
            String subjectInfo = (subjectName != null) ? subjectName : "unknown";
            System.err.println("Error scheduling subject " + subjectInfo + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Schedule all course activities for this subject
     */
    private boolean scheduleCourses(List<Group> groups, List<Teacher> teachers) {
        int courseHours = subject.getCourseHours();
        int courseLength = subject.getCourseLenght();
        if (courseLength <= 0) return true; // No courses to schedule
        
        int numCourses = courseHours / courseLength;
        boolean allScheduled = true;

        for (int i = 0; i < numCourses; i++) {
            Teacher teacher = selectTeacher(teachers);
            
            // Courses are attended by ALL groups of that subject together (usually)
            // But we must mark all groups as busy in that slot.
            
            String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
            boolean scheduled = false;
            
            // Try to find a slot where teacher, room AND ALL groups are free
            for (int attempt = 0; attempt < 100 && !scheduled; attempt++) {
                String day = days[random.nextInt(days.length)];
                int startHour = 8 + random.nextInt(12 - courseLength);
                
                if (canScheduleCourseForAllGroups(teacher, groups, day, startHour, courseLength)) {
                    scheduled = scheduleCourseForAllGroups(teacher, groups, day, startHour, courseLength);
                }
            }
            
            if (!scheduled) {
                // Backtracking
                for (String day : days) {
                    for (int hour = 8; hour <= 20 - courseLength; hour++) {
                        if (canScheduleCourseForAllGroups(teacher, groups, day, hour, courseLength)) {
                            scheduled = scheduleCourseForAllGroups(teacher, groups, day, hour, courseLength);
                            if (scheduled) break;
                        }
                    }
                    if (scheduled) break;
                }
            }
            
            if (!scheduled) allScheduled = false;
        }
        return allScheduled;
    }

    private boolean canScheduleCourseForAllGroups(Teacher teacher, List<Group> groups, String day, int hour, int duration) {
        if (teacher == null || teacher.getName() == null) return false;
        
        for (int h = hour; h < hour + duration; h++) {
            if (!teacherState.isTeacherAvailable(teacher.getName(), day, h)) return false;
        }
        
        for (Group group : groups) {
            if (group == null || group.getId() == null) continue;
            for (int h = hour; h < hour + duration; h++) {
                if (!groupState.isGroupAvailable(group.getId(), day, h)) return false;
            }
        }
        
        // Total students attending the course
        int totalStudents = groups.stream().filter(Objects::nonNull).mapToInt(Group::getSize).sum();
        Room room = getAvailableRoomWithCapacity(day, hour, "COURSE", totalStudents);
        if (room == null) return false;

        // Check room availability for the whole duration
        for (int h = hour + 1; h < hour + duration; h++) {
            if (!roomState.isRoomAvailable(room.getId(), day, h)) return false;
        }

        return true;
    }

    private boolean scheduleCourseForAllGroups(Teacher teacher, List<Group> groups, String day, int hour, int duration) {
        // Total students attending the course
        int totalStudents = groups.stream().filter(Objects::nonNull).mapToInt(Group::getSize).sum();
        Room room = getAvailableRoomWithCapacity(day, hour, "COURSE", totalStudents);
        if (room == null || room.getId() == null) return false;
        
        for (Group group : groups) {
            if (group == null || group.getId() == null) continue;
            Activity activity = new Activity(
                    subjectName,
                    group.getId(),
                    teacher.getName(),
                    room.getId(),
                    day,
                    java.time.LocalTime.of(hour, 0),
                    java.time.LocalTime.of(hour + duration, 0),
                    "COURSE"
            );
            
            groupState.addActivity(group.getId(), activity);
        }
        
        Activity teacherActivity = new Activity(
                subjectName,
                "ALL_GROUPS",
                teacher.getName(),
                room.getId(),
                day,
                java.time.LocalTime.of(hour, 0),
                java.time.LocalTime.of(hour + duration, 0),
                "COURSE"
        );
        teacherState.addActivity(teacher.getName(), teacherActivity);
        roomState.addActivity(room.getId(), teacherActivity);
        
        // Count all hours scheduled
        for (int h = 0; h < duration; h++) {
            scheduledActivities.add(teacherActivity); 
        }
        
        return true;
    }

    private Room getAvailableRoomWithCapacity(String day, int hour, String activityType, int minCapacity) {
        if (context == null || context.getPlaces() == null) return null;
        
        // Prefer rooms with specific capability for the activity type to avoid wasting multi-purpose rooms
        List<Room> allMatchingRooms = new ArrayList<>();
        
        for (Place place : context.getPlaces().values()) {
            if (place == null || place.getRooms() == null) continue;

            for (Room room : place.getRooms().values()) {
                if (room == null || room.getId() == null) continue;
                if (room.getCapacity() < minCapacity) continue;
                
                if ("COURSE".equals(activityType) && room.hasFlag("noCourse")) continue;
                if ("SEMINAR".equals(activityType) && room.hasFlag("noSeminar")) continue;
                if ("LABORATORY".equals(activityType) && room.hasFlag("noLaboratory")) continue;

                if (roomState != null && roomState.isRoomAvailable(room.getId(), day, hour)) {
                    allMatchingRooms.add(room);
                }
            }
        }
        
        if (allMatchingRooms.isEmpty()) return null;
        
        // Sort rooms by capacity to pick the smallest one that fits (Best Fit)
        allMatchingRooms.sort(Comparator.comparingInt(Room::getCapacity));
        
        return allMatchingRooms.get(0);
    }

    /**
     * Schedule all seminar activities for this subject
     */
    private boolean scheduleSeminars(List<Group> groups, List<Teacher> teachers) {
        int seminarHours = subject.getSeminarHours();
        boolean allScheduled = true;

        for (Group group : groups) {
            int seminarSplits = group.getSeminarySplit();
            if (seminarSplits <= 0) continue;
            int hoursPerSplit = seminarHours / seminarSplits;

            for (int i = 0; i < seminarSplits; i++) {
                for (int h = 0; h < hoursPerSplit; h++) {
                    Teacher teacher = selectTeacher(teachers);

                    int duration = subject.getSeminarLenght();
                    if (!tryScheduleActivityWithDuration(teacher, group, "SEMINAR", duration)) {
                        if (!scheduleWithBacktrackingAndDuration(teacher, group, "SEMINAR", duration)) {
                            allScheduled = false;
                        }
                    }
                }
            }
        }
        return allScheduled;
    }

    /**
     * Schedule all laboratory activities for this subject
     */
    private boolean scheduleLaboratories(List<Group> groups, List<Teacher> teachers) {
        int labHours = subject.getLaboratoryHours();
        boolean allScheduled = true;

        for (Group group : groups) {
            int labSplits = group.getLaboratorySplitCount();
            if (labSplits <= 0) continue;
            int hoursPerSplit = labHours / labSplits;

            for (int i = 0; i < labSplits; i++) {
                for (int h = 0; h < hoursPerSplit; h++) {
                    Teacher teacher = selectTeacher(teachers);

                    // Laboratories often come in 2-hour blocks
                    int duration = subject.getLaboratoriesLenght();
                    if (!tryScheduleActivityWithDuration(teacher, group, "LABORATORY", duration)) {
                        if (!scheduleWithBacktrackingAndDuration(teacher, group, "LABORATORY", duration)) {
                            allScheduled = false;
                        }
                    }
                }
            }
        }
        return allScheduled;
    }

    /**
     * Try to schedule activity with constraint checking
     */
    private boolean tryScheduleActivityWithDuration(Teacher teacher, Group group, String activityType, int duration) {
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};

        // Try random time slots
        for (int attempt = 0; attempt < 100; attempt++) {
            String day = days[random.nextInt(days.length)];
            int startHour = 8 + random.nextInt(12 - duration); // Adjusted for duration
            int endHour = startHour + duration;

            if (canScheduleAt(teacher, group, day, startHour, endHour, activityType)) {
                return scheduleActivity(teacher, group, activityType, day, startHour, endHour);
            }
        }
        return false;
    }

    /**
     * Backtracking - try all possible time slots
     */
    private boolean scheduleWithBacktrackingAndDuration(Teacher teacher, Group group, String activityType, int duration) {
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};

        for (String day : days) {
            for (int hour = 8; hour <= 20 - duration; hour++) {
                if (canScheduleAt(teacher, group, day, hour, hour + duration, activityType)) {
                    return scheduleActivity(teacher, group, activityType, day, hour, hour + duration);
                }
            }
        }
        return false;
    }

    /**
     * Try to schedule activity with constraint checking
     */
    private boolean tryScheduleActivity(Teacher teacher, Group group, String activityType) {
        return tryScheduleActivityWithDuration(teacher, group, activityType, 1);
    }

    /**
     * Backtracking - try all possible time slots
     */
    private boolean scheduleWithBacktracking(Teacher teacher, Group group, String activityType) {
        return scheduleWithBacktrackingAndDuration(teacher, group, activityType, 1);
    }

    /**
     * Check if we can schedule activity at given time
     */
    private boolean canScheduleAt(Teacher teacher, Group group, String day, int startHour, int endHour, String activityType) {
        // Check teacher availability
        if (teacher == null || teacher.getName() == null || teacherState == null) {
            return false;
        }

        // Check availability for the whole duration
        for (int h = startHour; h < endHour; h++) {
            if (!teacherState.isTeacherAvailable(teacher.getName(), day, h)) {
                return false;
            }
        }
        
        // Check teacher specific capability for the activity type
        if ("SEMINAR".equals(activityType) && !teacher.canTeachSeminar(subjectName)) {
            return false;
        }
        if ("LABORATORY".equals(activityType) && !teacher.canTeachLaboratory(subjectName)) {
            return false;
        }

        // Check group availability for the whole duration
        if (group == null || group.getId() == null || groupState == null) {
            return false;
        }
        for (int h = startHour; h < endHour; h++) {
            if (!groupState.isGroupAvailable(group.getId(), day, h)) {
                return false;
            }
        }

        // Check room availability and capability
        if (context == null || context.getPlaces() == null) {
            return false;
        }
        
        int minCapacity = group.getSize();
        if ("SEMINAR".equals(activityType)) {
            minCapacity = group.getSeminaryGroupSize();
        } else if ("LABORATORY".equals(activityType)) {
            minCapacity = group.getLaboratoryGroupSize();
        }

        // Check room availability for the whole duration
        Room room = getAvailableRoomWithCapacity(day, startHour, activityType, minCapacity);
        if (room == null) return false;

        for (int h = startHour + 1; h < endHour; h++) {
            if (!roomState.isRoomAvailable(room.getId(), day, h)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Actually schedule the activity
     */
    private boolean scheduleActivity(Teacher teacher, Group group, String activityType,
                                     String day, int startHour, int endHour) {
        try {
            if (teacher == null || group == null) return false;

            int minCapacity = group.getSize();
            if ("SEMINAR".equals(activityType)) {
                minCapacity = group.getSeminaryGroupSize();
            } else if ("LABORATORY".equals(activityType)) {
                minCapacity = group.getLaboratoryGroupSize();
            }

            // Get a room
            Room room = getAvailableRoomWithCapacity(day, startHour, activityType, minCapacity);
            if (room == null || room.getId() == null) {
                return false;
            }

            // Create activity
            Activity activity = new Activity(
                    subjectName,
                    group.getId(),
                    teacher.getName(),
                    room.getId(),
                    day,
                    java.time.LocalTime.of(startHour, 0),
                    java.time.LocalTime.of(endHour, 0),
                    activityType
            );

            // Record in state
            teacherState.addActivity(teacher.getName(), activity);
            groupState.addActivity(group.getId(), activity);
            roomState.addActivity(room.getId(), activity);

            scheduledActivities.add(activity);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get an available room for the time slot
     */
    private Room getAvailableRoom(String day, int hour, String activityType) {
        if (context == null || context.getPlaces() == null) return null;
        for (Place place : context.getPlaces().values()) {
            if (place == null || place.getRooms() == null) continue;

            for (Room room : place.getRooms().values()) {
                if (room == null || room.getId() == null) continue;
                
                // Check room capability
                if ("COURSE".equals(activityType) && room.hasFlag("noCourse")) continue;
                if ("SEMINAR".equals(activityType) && room.hasFlag("noSeminar")) continue;
                if ("LABORATORY".equals(activityType) && room.hasFlag("noLaboratory")) continue;

                if (roomState != null && roomState.isRoomAvailable(room.getId(), day, hour)) {
                    return room;
                }
            }
        }
        return null;
    }

    /**
     * Select a teacher randomly from capable list
     */
    private Teacher selectTeacher(List<Teacher> teachers) {
        if (teachers.isEmpty()) {
            throw new IllegalArgumentException("No capable teachers for subject " + subjectName);
        }
        return teachers.get(random.nextInt(teachers.size()));
    }

    /**
     * Get all groups that take this subject
     */
    private List<Group> getGroupsForSubject() {
        return context.getGroups().values().stream()
                .filter(g -> g.hasSubject(subjectName))
                .collect(Collectors.toList());
    }

    /**
     * Get all teachers capable of teaching this subject
     */
    private List<Teacher> getCapableTeachers() {
        return context.getTeachers().values().stream()
                .filter(t -> t.canTeachSubject(subjectName))
                .collect(Collectors.toList());
    }
}
