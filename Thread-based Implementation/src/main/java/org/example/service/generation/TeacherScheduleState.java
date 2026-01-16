package org.example.service.generation;

import org.example.model.Activity;
import org.example.model.Teacher;
import org.example.model.TimeSlot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe state for tracking teacher schedules during generation
 */
public class TeacherScheduleState {
    private final Map<String, Map<String, List<Activity>>> teacherSchedules;
    private final Map<String, String> lastTeacherBuilding;

    public TeacherScheduleState(Map<String, Teacher> teachers) {
        this.teacherSchedules = new ConcurrentHashMap<>();
        this.lastTeacherBuilding = new ConcurrentHashMap<>();

        teachers.forEach((name, teacher) -> {
            Map<String, List<Activity>> schedule = new ConcurrentHashMap<>();
            String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
            for (String day : days) {
                schedule.put(day, new CopyOnWriteArrayList<>());
            }
            
            // Initial busy slots from teacher config
            if (teacher.getSchedule() != null) {
                teacher.getSchedule().forEach((day, slots) -> {
                    String upperDay = day.toUpperCase();
                    List<Activity> dayActivities = schedule.get(upperDay);
                    if (dayActivities != null) {
                        for (TimeSlot slot : slots) {
                            // Extract start hour. Assuming format "HH:mm"
                            try {
                                int startHour = Integer.parseInt(slot.getStart().split(":")[0]);
                                int endHour = Integer.parseInt(slot.getEnd().split(":")[0]);
                                for (int h = startHour; h < endHour; h++) {
                                    dayActivities.add(new Activity(
                                        "BUSY", "N/A", name, "N/A", upperDay,
                                        java.time.LocalTime.of(h, 0), java.time.LocalTime.of(h + 1, 0), "BUSY"
                                    ));
                                }
                            } catch (Exception e) {
                                // Ignore malformed slots
                            }
                        }
                    }
                });
            }

            teacherSchedules.put(name, schedule);
            // ConcurrentHashMap does not allow null values. Using empty string as placeholder.
            lastTeacherBuilding.put(name, "");
        });
    }

    public synchronized boolean isTeacherAvailable(String teacherName, String day, int hour) {
        if (teacherName == null) return true;
        Map<String, List<Activity>> schedule = teacherSchedules.get(teacherName);
        if (schedule == null) return true;
        
        // Normalize day to match keys initialized in constructor
        String upperDay = day.toUpperCase();
        List<Activity> dayActivities = schedule.get(upperDay);
        if (dayActivities == null) return true;

        boolean available = dayActivities.stream()
                .noneMatch(a -> {
                    int start = a.startTime().getHour();
                    int end = a.endTime().getHour();
                    return hour >= start && hour < end;
                });
                
        return available;
    }

    public synchronized void addActivity(String teacherName, Activity activity) {
        if (teacherName == null || activity == null) return;
        Map<String, List<Activity>> schedule = teacherSchedules.get(teacherName);
        if (schedule == null) return;
        
        String upperDay = activity.day().toUpperCase();
        List<Activity> dayActivities = schedule.get(upperDay);
        if (dayActivities != null) {
            dayActivities.add(activity);
        }
        
        // Update last building if this is a real room activity
        if (activity.roomId() != null && !activity.roomId().equals("N/A") && !activity.roomId().equals("BUSY")) {
            // Find building from room ID (assuming room ID contains building or we can find it)
            // For now, let's just use the activity room ID as a proxy for building/location
            lastTeacherBuilding.put(teacherName, activity.roomId());
        }
    }

    public synchronized List<Activity> getTeacherActivities(String teacherName) {
        List<Activity> all = new ArrayList<>();
        if (teacherName == null) return all;
        
        Map<String, List<Activity>> schedule = teacherSchedules.get(teacherName);
        if (schedule != null) {
            schedule.values().forEach(all::addAll);
        }
        return all;
    }

    public synchronized String getLastBuilding(String teacherName) {
        if (teacherName == null) return "";
        String building = lastTeacherBuilding.get(teacherName);
        return building != null ? building : "";
    }

    public synchronized void setLastBuilding(String teacherName, String buildingId) {
        if (teacherName != null && buildingId != null) {
            lastTeacherBuilding.put(teacherName, buildingId);
        }
    }
}
