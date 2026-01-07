package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Model class representing a building/place with its schedule and rooms.
 */
public class Place {
    private String name;

    @JsonProperty("Schedule")
    private Map<String, List<TimeSlot>> schedule;

    @JsonProperty("Rooms")
    private Map<String, Room> rooms;

    public Place() {}

    public Place(String name, Map<String, List<TimeSlot>> schedule, Map<String, Room> rooms) {
        this.name = name;
        this.schedule = schedule;
        this.rooms = rooms;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, List<TimeSlot>> getSchedule() {
        return schedule;
    }

    public void setSchedule(Map<String, List<TimeSlot>> schedule) {
        this.schedule = schedule;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public void setRooms(Map<String, Room> rooms) {
        this.rooms = rooms;
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<TimeSlot> getScheduleForDay(String day) {
        return schedule.get(day);
    }

    @Override
    public String toString() {
        return "Place{" +
                "name='" + name + '\'' +
                ", schedule=" + schedule +
                ", rooms=" + rooms +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Objects.equals(name, place.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}