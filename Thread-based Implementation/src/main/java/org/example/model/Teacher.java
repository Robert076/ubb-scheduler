package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Model class representing a teacher.
 */
public class Teacher {
    private String name;

    @JsonProperty("Schedule")
    private Map<String, List<TimeSlot>> schedule;

    @JsonProperty("MaxHoursPerWeek")
    private int maxHoursPerWeek;

    @JsonProperty("PreferredBuildings")
    private List<String> preferredBuildings;

    @JsonProperty("Subjects")
    private Map<String, SubjectCapability> subjects;

    @JsonProperty("Languages")
    private List<String> languages;

    public Teacher() {}

    public Teacher(String name, Map<String, List<TimeSlot>> schedule,
                   int maxHoursPerWeek, List<String> preferredBuildings,
                   Map<String, SubjectCapability> subjects, List<String> languages) {
        this.name = name;
        this.schedule = schedule;
        this.maxHoursPerWeek = maxHoursPerWeek;
        this.preferredBuildings = preferredBuildings;
        this.subjects = subjects;
        this.languages = languages;
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

    public int getMaxHoursPerWeek() {
        return maxHoursPerWeek;
    }

    public void setMaxHoursPerWeek(int maxHoursPerWeek) {
        this.maxHoursPerWeek = maxHoursPerWeek;
    }

    public List<String> getPreferredBuildings() {
        return preferredBuildings;
    }

    public void setPreferredBuildings(List<String> preferredBuildings) {
        this.preferredBuildings = preferredBuildings;
    }

    public Map<String, SubjectCapability> getSubjects() {
        return subjects;
    }

    public void setSubjects(Map<String, SubjectCapability> subjects) {
        this.subjects = subjects;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public boolean canTeachSubject(String subjectName) {
        return subjects != null && subjects.containsKey(subjectName);
    }

    public boolean canTeachCourse(String subjectName) {
        SubjectCapability capability = subjects.get(subjectName);
        return capability != null && capability.isCanCourse();
    }

    public boolean canTeachSeminar(String subjectName) {
        SubjectCapability capability = subjects.get(subjectName);
        return capability != null && capability.isCanSeminary();
    }

    public boolean canTeachLaboratory(String subjectName) {
        SubjectCapability capability = subjects.get(subjectName);
        return capability != null && capability.isCanLaboratory();
    }

    public boolean canWorkInBuilding(String buildingName) {
        return preferredBuildings != null && preferredBuildings.contains(buildingName);
    }

    public boolean speaksLanguage(String language) {
        return languages != null && languages.contains(language);
    }

    public List<String> getTeachesSubjects() {
        return subjects != null ? new ArrayList<>(subjects.keySet()) : new ArrayList<>();
    }

    public int getTotalHours() {
        return maxHoursPerWeek;
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "name='" + name + '\'' +
                ", maxHoursPerWeek=" + maxHoursPerWeek +
                ", preferredBuildings=" + preferredBuildings +
                ", subjects=" + subjects +
                ", languages=" + languages +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Teacher teacher = (Teacher) o;
        return Objects.equals(name, teacher.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}