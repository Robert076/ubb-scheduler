package org.example.repository;

import org.example.model.Place;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PlaceRepository {
    private Map<String, Place> places = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public PlaceRepository() throws Exception {
        loadPlaces();
    }

    private void loadPlaces() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("places-config.json");
        
        if (inputStream == null) {
            throw new RuntimeException("places-config.json not found in resources!");
        }

        // Parse JSON as Map<String, Place>
        Map<String, Place> parsedPlaces = objectMapper.readValue(inputStream, 
            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Place.class));

        // Set name for each place
        parsedPlaces.forEach((name, place) -> place.setName(name));
        
        this.places = parsedPlaces;
    }

    public Place getPlace(String name) {
        return places.get(name);
    }

    public Map<String, Place> getAllPlaces() {
        return new HashMap<>(places);
    }

    public void printAllPlaces() {
        System.out.println("\n=== ALL PLACES ===");
        places.forEach((name, place) -> {
            System.out.println("\nPlace: " + name);
            System.out.println("  Rooms: " + place.getRooms().size());
            place.getRooms().forEach((roomId, room) -> {
                System.out.println("    - " + roomId + " (Capacity: " + room.getCapacity() + ")");
            });
        });
    }
}