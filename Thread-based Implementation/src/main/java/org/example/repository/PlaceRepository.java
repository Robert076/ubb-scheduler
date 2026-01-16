package org.example.repository;

import org.example.model.Place;
import org.example.model.Room;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

/**
 * Repository for loading and managing Place entities from configuration.
 * Data is loaded once and exposed as unmodifiable collections.
 * Provides utility methods for room capability categorization.
 */
public class PlaceRepository {

    private final Map<String, Place> places;

    public PlaceRepository() throws Exception {
        this.places = Collections.unmodifiableMap(loadPlaces());
    }

    private Map<String, Place> loadPlaces() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("places-config.json");

        if (inputStream == null) {
            throw new RuntimeException("places-config.json not found in resources!");
        }

        Map<String, Place> parsedPlaces = objectMapper.readValue(inputStream,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Place.class));

        // Set name for each place (from JSON key)
        parsedPlaces.forEach((name, place) -> {
            place.setName(name);
            if (place.getRooms() != null) {
                place.getRooms().forEach((roomId, room) -> room.setId(roomId));
            }
        });

        return parsedPlaces;
    }

    /**
     * Get all places as an unmodifiable map.
     */
    public Map<String, Place> getAllPlaces() {
        return places;
    }

    /**
     * Compute the capability type of a room based on its flags.
     * Returns one of: COURSE_ONLY, SEMINAR_ONLY, LAB_ONLY, COURSE_SEMINAR, COURSE_LAB, SEMINAR_LAB, ALL
     */
    public String computeRoomCapability(Room room) {
        boolean canCourse = !room.hasFlag("noCourse");
        boolean canSeminar = !room.hasFlag("noSeminar");
        boolean canLab = !room.hasFlag("noLaboratory");

        if (canCourse && canSeminar && canLab) return "ALL";
        if (canCourse && !canSeminar && !canLab) return "COURSE_ONLY";
        if (!canCourse && canSeminar && !canLab) return "SEMINAR_ONLY";
        if (!canCourse && !canSeminar && canLab) return "LAB_ONLY";
        if (canCourse && canSeminar && !canLab) return "COURSE_SEMINAR";
        if (canCourse && !canSeminar && canLab) return "COURSE_LAB";
        if (!canCourse && canSeminar && canLab) return "SEMINAR_LAB";
        return "NONE"; // should never happen if config is valid
    }

    /**
     * Get all rooms grouped by capability type.
     * Returns a map: capability -> list of rooms with that capability
     */
    public Map<String, List<Room>> getRoomsByCapability() {
        Map<String, List<Room>> groupedRooms = new HashMap<>();

        // Initialize capability categories
        String[] capabilities = {"COURSE_ONLY", "SEMINAR_ONLY", "LAB_ONLY", "COURSE_SEMINAR", "COURSE_LAB", "SEMINAR_LAB", "ALL"};
        for (String capability : capabilities) {
            groupedRooms.put(capability, new ArrayList<>());
        }

        // Populate rooms by capability
        places.values().forEach(place ->
                place.getRooms().values().forEach(room -> {
                    String capability = computeRoomCapability(room);
                    groupedRooms.get(capability).add(room);
                })
        );

        return groupedRooms;
    }

    /**
     * Get count of rooms by capability type.
     */
    public Map<String, Integer> getRoomCountByCapability() {
        Map<String, Integer> counts = new HashMap<>();
        getRoomsByCapability().forEach((capability, rooms) -> counts.put(capability, rooms.size()));
        return counts;
    }

    /**
     * Calculate total available hours per week for a specific capability type.
     * Returns the sum of (room capacity × available hours per week) for all rooms with that capability.
     */
    public int getTotalCapacityByCapability(String capability) {
        int totalCapacity = 0;

        for (Place place : places.values()) {
            int placeHoursPerWeek = calculatePlaceHoursPerWeek(place);

            for (Room room : place.getRooms().values()) {
                if (computeRoomCapability(room).equals(capability)) {
                    totalCapacity += placeHoursPerWeek * room.getCapacity();
                }
            }
        }

        return totalCapacity;
    }

    /**
     * Calculate total available hours per week for a place based on its schedule.
     */
    public int calculatePlaceHoursPerWeek(Place place) {
        int totalHours = 0;
        if (place.getSchedule() != null) {
            for (List<org.example.model.TimeSlot> daySlots : place.getSchedule().values()) {
                if (daySlots != null) {
                    for (org.example.model.TimeSlot slot : daySlots) {
                        totalHours += parseHourDuration(slot.getStart(), slot.getEnd());
                    }
                }
            }
        }
        return totalHours;
    }

    /**
     * Parse duration between two time strings (HH:MM format).
     * Returns number of hours.
     */
    private int parseHourDuration(String startTime, String endTime) {
        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);

            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            int durationMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
            return Math.max(0, durationMinutes / 60); // round down to whole hours
        } catch (Exception e) {
            System.err.println("Error parsing time duration: " + startTime + " - " + endTime);
            return 0;
        }
    }

    /**
     * Print all places and their rooms for debugging purposes.
     */
    public void printAllPlaces() {
        System.out.println("\n=== ALL PLACES ===");
        places.forEach((name, place) -> {
            System.out.println("\nPlace: " + name);
            System.out.println(" Rooms: " + place.getRooms().size());
            place.getRooms().forEach((roomId, room) -> {
                String capability = computeRoomCapability(room);
                System.out.println(" - " + roomId + " (Capacity: " + room.getCapacity() + ", Capability: " + capability + ")");
            });
        });
    }
}
