package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public class Room {
    private String id;

    @JsonProperty("Capacity")
    private int capacity;

    @JsonProperty("Flags")
    private List<String> flags;

    public Room() {}

    public Room(String id, int capacity, List<String> flags) {
        this.id = id;
        this.capacity = capacity;
        this.flags = flags;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getFlags() {
        return flags;
    }

    public void setFlags(List<String> flags) {
        this.flags = flags;
    }

    public boolean hasFlag(String flag) {
        return flags != null && flags.contains(flag);
    }

    @Override
    public String toString() {
        return "Room{" +
                "capacity=" + capacity +
                ", flags=" + flags +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return capacity == room.capacity && Objects.equals(flags, room.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capacity, flags);
    }
}
