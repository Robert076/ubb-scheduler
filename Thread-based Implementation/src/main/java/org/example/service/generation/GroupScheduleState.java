package org.example.service.generation;

import org.example.model.Activity;
import org.example.model.Group;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe state for tracking group schedules during generation
 */
public class GroupScheduleState {
    private final Map<String, Map<String, List<Activity>>> groupSchedules;

    public GroupScheduleState(Map<String, Group> groups) {
        this.groupSchedules = new ConcurrentHashMap<>();

        groups.forEach((groupId, group) -> {
            Map<String, List<Activity>> schedule = new ConcurrentHashMap<>();
            String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
            for (String day : days) {
                schedule.put(day, new CopyOnWriteArrayList<>());
            }
            groupSchedules.put(groupId, schedule);
        });
    }

    public synchronized boolean isGroupAvailable(String groupId, String day, int hour) {
        Map<String, List<Activity>> schedule = groupSchedules.get(groupId);
        if (schedule == null) return true;
        
        String upperDay = day.toUpperCase();
        List<Activity> dayActivities = schedule.get(upperDay);
        if (dayActivities == null) return true;

        return dayActivities.stream()
                .noneMatch(a -> {
                    int start = a.startTime().getHour();
                    int end = a.endTime().getHour();
                    return hour >= start && hour < end;
                });
    }

    public synchronized void addActivity(String groupId, Activity activity) {
        if (groupId == null || activity == null) return;
        Map<String, List<Activity>> schedule = groupSchedules.get(groupId);
        if (schedule == null) return;
        
        String upperDay = activity.day().toUpperCase();
        List<Activity> dayActivities = schedule.get(upperDay);
        if (dayActivities != null) {
            dayActivities.add(activity);
        }
    }

    public synchronized List<Activity> getGroupActivities(String groupId) {
        List<Activity> all = new ArrayList<>();
        if (groupId == null) return all;
        
        Map<String, List<Activity>> schedule = groupSchedules.get(groupId);
        if (schedule != null) {
            schedule.values().forEach(all::addAll);
        }
        return all;
    }
}
