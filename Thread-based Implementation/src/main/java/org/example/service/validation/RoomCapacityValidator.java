package org.example.service.validation;

import org.example.context.TimetableDataContext;
import org.example.model.*;
import org.example.repository.PlaceRepository;
import java.util.*;
import java.util.concurrent.*;

/**
 * Room Capacity Validator
 * 
 * Validates that room space (by activity type) is sufficient to accommodate
 * all required courses, seminars, and laboratories.
 * 
 * Breakdown by capability: COURSE_ONLY, SEMINAR_ONLY, LAB_ONLY, COURSE_SEMINAR, etc.
 * 
 * Threading: PARALLEL by places
 * - Each place analysis runs independently
 * - Thread pool size = CPU cores
 * Complexity: O(places × rooms)
 */
public class RoomCapacityValidator implements Validator {
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final PlaceRepository placeRepository;

    public RoomCapacityValidator(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Override
    public String getValidatorName() {
        return "RoomCapacity";
    }

    /**
     * Activity type requirements - hours needed per activity type
     */
    private static class ActivityTypeRequirements {
        int courseHours;
        int seminarHours;
        int labHours;

        ActivityTypeRequirements(int courseHours, int seminarHours, int labHours) {
            this.courseHours = courseHours;
            this.seminarHours = seminarHours;
            this.labHours = labHours;
        }

        int totalHours() {
            return courseHours + seminarHours + labHours;
        }
    }

    /**
     * Activity type capacity - hours available per activity type
     */
    private static class ActivityTypeCapacity {
        Map<String, Integer> capabilityHours = new HashMap<>();

        void put(String capability, int hours) {
            capabilityHours.put(capability, hours);
        }

        int get(String capability) {
            return capabilityHours.getOrDefault(capability, 0);
        }

        int totalCourseCapacity() {
            return get("COURSE_ONLY") + get("COURSE_SEMINAR") + get("COURSE_LAB") + get("ALL");
        }

        int totalSeminarCapacity() {
            return get("SEMINAR_ONLY") + get("COURSE_SEMINAR") + get("SEMINAR_LAB") + get("ALL");
        }

        int totalLabCapacity() {
            return get("LAB_ONLY") + get("COURSE_LAB") + get("SEMINAR_LAB") + get("ALL");
        }
    }

    @Override
    public ValidationResult validate(TimetableDataContext context) {
        long startTime = System.currentTimeMillis();

        // Step 1: Calculate requirements
        ActivityTypeRequirements requirements = calculateActivityRequirements(context);

        // Step 2: Calculate capacity (parallel by place)
        ActivityTypeCapacity totalCapacity = calculateCapacityParallel(context);

        // Step 3: Check feasibility
        List<String> deficits = new ArrayList<>();
        
        if (totalCapacity.totalCourseCapacity() < requirements.courseHours) {
            deficits.add(String.format("Course capacity: need %dh, have %dh",
                requirements.courseHours, totalCapacity.totalCourseCapacity()));
        }
        if (totalCapacity.totalSeminarCapacity() < requirements.seminarHours) {
            deficits.add(String.format("Seminar capacity: need %dh, have %dh",
                requirements.seminarHours, totalCapacity.totalSeminarCapacity()));
        }
        if (totalCapacity.totalLabCapacity() < requirements.labHours) {
            deficits.add(String.format("Lab capacity: need %dh, have %dh",
                requirements.labHours, totalCapacity.totalLabCapacity()));
        }

        boolean pass = deficits.isEmpty();
        long executionTime = System.currentTimeMillis() - startTime;

        Map<String, Object> details = new HashMap<>();
        details.put("requirements", Map.of(
            "courses", requirements.courseHours,
            "seminars", requirements.seminarHours,
            "labs", requirements.labHours,
            "total", requirements.totalHours()
        ));
        details.put("capacity", Map.of(
            "courses", totalCapacity.totalCourseCapacity(),
            "seminars", totalCapacity.totalSeminarCapacity(),
            "labs", totalCapacity.totalLabCapacity()
        ));
        details.put("capabilityBreakdown", totalCapacity.capabilityHours);
        if (!deficits.isEmpty()) {
            details.put("deficits", deficits);
        }

        ValidationResult.Status status = pass ? ValidationResult.Status.PASS : ValidationResult.Status.FAIL;
        String message = pass
            ? "Room capacity is sufficient for all activities"
            : "Room capacity deficits: " + String.join(", ", deficits);

        return new ValidationResult(
            getValidatorName(),
            status,
            message,
            details,
            executionTime
        );
    }

    /**
     * Calculate total hours needed per activity type across all subjects and groups
     */
    private ActivityTypeRequirements calculateActivityRequirements(TimetableDataContext context) {
        int courseHours = 0;
        int seminarHours = 0;
        int labHours = 0;

        for (Group group : context.getGroups().values()) {
            for (String subjectName : group.getSubjects()) {
                Subject subject = context.getSubjects().get(subjectName);
                if (subject != null) {
                    courseHours += subject.getCoursesPerWeek() * subject.getCourseLenght();
                    seminarHours += subject.getSeminarsPerWeek() * subject.getSeminarLenght() * group.getSeminarySplit();
                    labHours += (int) (subject.getLaboratoriesPerWeek() * subject.getLaboratoriesLenght() * group.getLaboratorySplit());
                }
            }
        }

        return new ActivityTypeRequirements(courseHours, seminarHours, labHours);
    }

    /**
     * Calculate capacity in parallel by place
     */
    private ActivityTypeCapacity calculateCapacityParallel(TimetableDataContext context) {
        Map<String, ActivityTypeCapacity> placeCapacities = Collections.synchronizedMap(new HashMap<>());
        List<Future<?>> futures = new ArrayList<>();

        for (String placeName : context.getPlaces().keySet()) {
            Future<?> task = executor.submit(() -> {
                Place place = context.getPlaces().get(placeName);
                ActivityTypeCapacity capacity = analyzePlaceCapacity(place);
                placeCapacities.put(placeName, capacity);
            });
            futures.add(task);
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("V2 validation interrupted", e);
            }
        }

        ActivityTypeCapacity total = new ActivityTypeCapacity();
        String[] capabilities = {"COURSE_ONLY", "SEMINAR_ONLY", "LAB_ONLY", "COURSE_SEMINAR", "COURSE_LAB", "SEMINAR_LAB", "ALL"};
        
        for (String capability : capabilities) {
            int sumCapacity = placeCapacities.values().stream()
                .mapToInt(c -> c.get(capability))
                .sum();
            total.put(capability, sumCapacity);
        }

        return total;
    }

    /**
     * Analyze capacity for a single place
     */
    private ActivityTypeCapacity analyzePlaceCapacity(Place place) {
        ActivityTypeCapacity capacity = new ActivityTypeCapacity();
        int placeHoursPerWeek = placeRepository.calculatePlaceHoursPerWeek(place);

        String[] capabilities = {"COURSE_ONLY", "SEMINAR_ONLY", "LAB_ONLY", "COURSE_SEMINAR", "COURSE_LAB", "SEMINAR_LAB", "ALL"};
        for (String cap : capabilities) {
            capacity.put(cap, 0);
        }

        for (Room room : place.getRooms().values()) {
            String roomCapability = placeRepository.computeRoomCapability(room);
            int currentCapacity = capacity.get(roomCapability);
            capacity.put(roomCapability, currentCapacity + placeHoursPerWeek);
        }

        return capacity;
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
}
