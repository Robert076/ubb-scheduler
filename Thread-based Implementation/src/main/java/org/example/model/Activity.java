package org.example.model;

import java.time.LocalTime;
import java.util.Objects;

public record Activity(
        String subjectName,
        String groupId,
        String teacherName,
        String roomId,
        String day,
        LocalTime startTime,
        LocalTime endTime,
        String activityType,
        String subgroup,
        String frequency
) {
    public Activity(String subjectName, String groupId, String teacherName, String roomId,
                    String day, LocalTime startTime, LocalTime endTime, String activityType) {
        this(subjectName, groupId, teacherName, roomId, day, startTime, endTime, activityType, "", "Weekly");
    }

    public Activity {
        Objects.requireNonNull(subjectName, "Subject name cannot be null");
        Objects.requireNonNull(groupId, "Group ID cannot be null");
        Objects.requireNonNull(teacherName, "Teacher name cannot be null");
        Objects.requireNonNull(roomId, "Room ID cannot be null");
        Objects.requireNonNull(day, "Day cannot be null");
        Objects.requireNonNull(activityType, "Activity type cannot be null");
        Objects.requireNonNull(subgroup, "Subgroup cannot be null");
        Objects.requireNonNull(frequency, "Frequency cannot be null");
    }

    public int getDurationHours() {
        return endTime.getHour() - startTime.getHour();
    }

    @Override
    public String toString() {
        return String.format("%s | %s-%s (%s) | %s | Group: %s | Teacher: %s | Room: %s",
                day, startTime, endTime, activityType, subjectName, groupId, teacherName, roomId);
    }
}
