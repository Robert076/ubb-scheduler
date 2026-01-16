package org.example.service.generation;

import org.example.model.Activity;
import org.example.model.Place;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe state for tracking room schedules during generation
 */
public class RoomScheduleState {
    private final Map<String, Map<String, List<Activity>>> roomSchedules;

    public RoomScheduleState(Map<String, Place> places) {
        this.roomSchedules = new ConcurrentHashMap<>();
        
        places.forEach((placeName, place) -> {
            if (place.getRooms() != null) {
                place.getRooms().forEach((roomId, room) -> {
                    Map<String, List<Activity>> schedule = new ConcurrentHashMap<>();
                    String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
                    for (String day : days) {
                        schedule.put(day, new CopyOnWriteArrayList<>());
                    }
                    roomSchedules.put(roomId, schedule);
                });
            }
        });
        
        initializeBusySlots(places);
    }

    private void initializeBusySlots(Map<String, Place> places) {
        places.forEach((placeName, place) -> {
            if (place.getRooms() == null) return;
            
            place.getRooms().forEach((roomId, room) -> {
                Map<String, List<Activity>> roomSchedule = roomSchedules.get(roomId);
                if (roomSchedule == null) return;

                String[] allDays = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
                for (String day : allDays) {
                    List<Activity> dayActivities = roomSchedule.get(day);
                    
                    // Get available slots for this day from place config
                    List<org.example.model.TimeSlot> availableSlots = null;
                    if (place.getSchedule() != null) {
                        // Config might use "Monday" or "MONDAY"
                        availableSlots = place.getSchedule().get(day);
                        if (availableSlots == null) {
                            // Try title case
                            String titleCaseDay = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
                            availableSlots = place.getSchedule().get(titleCaseDay);
                        }
                    }

                    boolean[] isBusy = new boolean[24];
                    Arrays.fill(isBusy, true); // Assume busy by default

                    if (availableSlots != null) {
                        for (org.example.model.TimeSlot slot : availableSlots) {
                            try {
                                int start = Integer.parseInt(slot.getStart().split(":")[0]);
                                int end = Integer.parseInt(slot.getEnd().split(":")[0]);
                                for (int h = start; h < end; h++) {
                                    if (h >= 0 && h < 24) isBusy[h] = false; // It's available
                                }
                            } catch (Exception e) {}
                        }
                    }

                    // Add busy activities for all non-available hours between 8 and 20
                    for (int h = 8; h < 20; h++) {
                        if (isBusy[h]) {
                            dayActivities.add(new Activity(
                                "CLOSED", "N/A", "N/A", roomId, day,
                                java.time.LocalTime.of(h, 0), java.time.LocalTime.of(h + 1, 0), "CLOSED"
                            ));
                        }
                    }
                }
            });
        });
    }

    public synchronized boolean isRoomAvailable(String roomId, String day, int hour) {
        Map<String, List<Activity>> daySchedules = roomSchedules.get(roomId);
        if (daySchedules == null) return true;
        
        String upperDay = day.toUpperCase();
        List<Activity> dayActivities = daySchedules.get(upperDay);
        if (dayActivities == null) return true;

        return dayActivities.stream()
                .noneMatch(a -> {
                    int start = a.startTime().getHour();
                    int end = a.endTime().getHour();
                    return hour >= start && hour < end;
                });
    }

    public synchronized void addActivity(String roomId, Activity activity) {
        String upperDay = activity.day().toUpperCase();
        roomSchedules.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(upperDay, k -> new CopyOnWriteArrayList<>())
                .add(activity);
    }

    public synchronized List<Activity> getRoomActivities(String roomId) {
        List<Activity> all = new ArrayList<>();
        roomSchedules.getOrDefault(roomId, new ConcurrentHashMap<>()).values().forEach(all::addAll);
        return all;
    }
}
