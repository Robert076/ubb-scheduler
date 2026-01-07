package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Model class representing a time slot (start and end time).
 */
public class TimeSlot {
    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    public TimeSlot() {}

    public TimeSlot(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return start + " - " + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(start, timeSlot.start) && Objects.equals(end, timeSlot.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
