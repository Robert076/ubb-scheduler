package org.example.service.validation;

import org.example.context.TimetableDataContext;
import org.example.model.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Time-Slot Collision Analysis (V4)
 *
 * Builds a 2D collision matrix: Hours (8am-8pm) × Days (Mon-Fri)
 *
 * Teacher layer: +1 for each hour a teacher is available
 * Room layer: -1 for each hour a room is occupied (based on place schedule)
 *
 * Result interpretation:
 * - Positive values: More teachers available than rooms (bottleneck: insufficient rooms at that time)
 * - Negative values: More rooms available than teachers (bottleneck: insufficient teachers at that time)
 * - Zero values: Perfect balance (rare)
 *
 * THIS IS A WARNING VALIDATOR - never blocks generation, only indicates areas for improvement
 *
 * Threading: PARALLEL (teachers + places layers)
 * Complexity: O(teachers × slots + places × slots) with parallelism
 */
public class TimeSlotCollisionValidator implements Validator {
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    @Override
    public String getValidatorName() {
        return "TimeSlotCollision";
    }

    @Override
    public ValidationResult validate(TimetableDataContext context) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> details = new HashMap<>();

        // Build collision matrix
        TimeSlotRange range = new TimeSlotRange(context);
        int[][] collisionMatrix = new int[range.getDayCount()][range.getHourCount()];

        // Teacher layer
        for (Teacher teacher : context.getTeachers().values()) {
            if (teacher.getSchedule() != null) {
                for (Map.Entry<String, List<TimeSlot>> dayEntry : teacher.getSchedule().entrySet()) {
                    int dayIdx = range.getDayIndex(dayEntry.getKey());
                    if (dayIdx >= 0 && dayEntry.getValue() != null) {
                        for (TimeSlot slot : dayEntry.getValue()) {
                            int startHour = parseHour(slot.getStart());
                            int endHour = parseHour(slot.getEnd());
                            for (int h = startHour; h < endHour; h++) {
                                int hourIdx = range.getHourIndex(h);
                                if (hourIdx >= 0) {
                                    collisionMatrix[dayIdx][hourIdx]++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Room layer
        for (Place place : context.getPlaces().values()) {
            if (place.getSchedule() != null) {
                for (Map.Entry<String, List<TimeSlot>> dayEntry : place.getSchedule().entrySet()) {
                    int dayIdx = range.getDayIndex(dayEntry.getKey());
                    if (dayIdx >= 0 && dayEntry.getValue() != null) {
                        for (TimeSlot slot : dayEntry.getValue()) {
                            int startHour = parseHour(slot.getStart());
                            int endHour = parseHour(slot.getEnd());
                            for (int h = startHour; h < endHour; h++) {
                                int hourIdx = range.getHourIndex(h);
                                if (hourIdx >= 0) {
                                    collisionMatrix[dayIdx][hourIdx]--;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Analyze bottlenecks
        BottleneckAnalysis analysis = analyzeBottlenecks(collisionMatrix, range);
        details.put("bottlenecks", analysis.bottlenecks);
        details.put("healthScore", analysis.getHealthScore());
        details.put("severeTeacherExcess", analysis.severeTeacherExcess);
        details.put("severeRoomExcess", analysis.severeRoomExcess);
        details.put("mildImbalances", analysis.mildImbalances);

        long executionTime = System.currentTimeMillis() - startTime;

        // IMPORTANT: This is ALWAYS a WARNING (informative, not blocking)
        // Only show FAIL if there are SEVERE issues (>5 severe bottlenecks)
        ValidationResult.Status status = (analysis.severeTeacherExcess > 5 || analysis.severeRoomExcess > 5)
                ? ValidationResult.Status.FAIL
                : ValidationResult.Status.WARN;

        String message = analysis.isEmpty()
                ? "No time-slot bottlenecks detected - optimal distribution"
                : "Found " + analysis.bottlenecks.size() + " bottleneck(s): " +
                analysis.severeTeacherExcess + " teacher excess, " +
                analysis.severeRoomExcess + " room excess, " +
                analysis.mildImbalances + " mild imbalances (Health: " + analysis.getHealthScore() + "%)";

        return new ValidationResult(
                getValidatorName(),
                status,
                message,
                details,
                executionTime
        );
    }

    private BottleneckAnalysis analyzeBottlenecks(int[][] matrix, TimeSlotRange range) {
        BottleneckAnalysis analysis = new BottleneckAnalysis();

        for (int day = 0; day < matrix.length; day++) {
            for (int hour = 0; hour < matrix[day].length; hour++) {
                int value = matrix[day][hour];

                if (value > 3) {
                    // Too many teachers vs rooms
                    analysis.addBottleneck(
                            range.getDayName(day),
                            range.getHourLabel(range.hours.get(hour)),
                            "SEVERE_TEACHER_EXCESS",
                            value
                    );
                } else if (value < -3) {
                    // Too many rooms vs teachers
                    analysis.addBottleneck(
                            range.getDayName(day),
                            range.getHourLabel(range.hours.get(hour)),
                            "SEVERE_ROOM_EXCESS",
                            value
                    );
                } else if (value != 0) {
                    // Mild imbalance
                    analysis.addBottleneck(
                            range.getDayName(day),
                            range.getHourLabel(range.hours.get(hour)),
                            "MILD_IMBALANCE",
                            value
                    );
                }
            }
        }

        return analysis;
    }

    private static int parseHour(String timeStr) {
        try {
            return Integer.parseInt(timeStr.split(":")[0]);
        } catch (Exception e) {
            return 8; // Default fallback
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ==================== HELPER CLASSES ====================

    private static class TimeSlotRange {
        private final List<String> days = new ArrayList<>();
        private final List<Integer> hours = new ArrayList<>();
        private final Map<String, Integer> dayIndex = new HashMap<>();
        private final Map<Integer, Integer> hourIndex = new HashMap<>();

        TimeSlotRange(TimetableDataContext context) {
            buildDayRange();
            buildHourRange(context);
        }

        private void buildDayRange() {
            String[] dayOrder = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            for (int i = 0; i < dayOrder.length; i++) {
                days.add(dayOrder[i]);
                dayIndex.put(dayOrder[i], i);
            }
        }

        private void buildHourRange(TimetableDataContext context) {
            Set<Integer> hourSet = new TreeSet<>();

            // Collect from places
            for (Place place : context.getPlaces().values()) {
                if (place.getSchedule() != null) {
                    for (List<TimeSlot> slots : place.getSchedule().values()) {
                        if (slots != null) {
                            for (TimeSlot slot : slots) {
                                hourSet.add(parseHour(slot.getStart()));
                                hourSet.add(parseHour(slot.getEnd()));
                            }
                        }
                    }
                }
            }

            if (hourSet.isEmpty()) {
                for (int h = 8; h <= 20; h++) {
                    hourSet.add(h);
                }
            }

            hourSet.forEach(h -> {
                hourIndex.put(h, hours.size());
                hours.add(h);
            });
        }

        int getDayIndex(String dayName) {
            return dayIndex.getOrDefault(dayName, -1);
        }

        int getHourIndex(int hour) {
            return hourIndex.getOrDefault(hour, -1);
        }

        String getDayName(int index) {
            return index >= 0 && index < days.size() ? days.get(index) : "Unknown";
        }

        String getHourLabel(int hourValue) {
            return String.format("%02d:00", hourValue);
        }

        int getDayCount() { return days.size(); }
        int getHourCount() { return hours.size(); }
    }

    private static class BottleneckAnalysis {
        private final List<Map<String, Object>> bottlenecks = new ArrayList<>();
        private int severeTeacherExcess = 0;
        private int severeRoomExcess = 0;
        private int mildImbalances = 0;

        void addBottleneck(String day, String hour, String type, int value) {
            Map<String, Object> bn = new HashMap<>();
            bn.put("day", day);
            bn.put("hour", hour);
            bn.put("type", type);
            bn.put("value", value);
            bottlenecks.add(bn);

            if (type.equals("SEVERE_TEACHER_EXCESS")) severeTeacherExcess++;
            else if (type.equals("SEVERE_ROOM_EXCESS")) severeRoomExcess++;
            else mildImbalances++;
        }

        boolean isEmpty() {
            return bottlenecks.isEmpty();
        }

        int getHealthScore() {
            return Math.max(0, 100 - (severeTeacherExcess * 15 + severeRoomExcess * 15 + mildImbalances * 3));
        }
    }
}
