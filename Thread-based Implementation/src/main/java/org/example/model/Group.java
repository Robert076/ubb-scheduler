package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Model class representing a student group/class.
 */
public class Group {
    private String id;

    @JsonProperty("Size")
    private int size;

    @JsonProperty("Language")
    private String language;

    @JsonProperty("Subjects")
    private List<String> subjects;

    @JsonProperty("SeminarySplit")
    private int seminarySplit;

    @JsonProperty("LaboratorySplit")
    private int laboratorySplit;

    public Group() {}

    public Group(String id, int size, String language, List<String> subjects,
                 int seminarySplit, int laboratorySplit) {
        this.id = id;
        this.size = size;
        this.language = language;
        this.subjects = subjects;
        this.seminarySplit = seminarySplit;
        this.laboratorySplit = laboratorySplit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public int getSeminarySplit() {
        return seminarySplit;
    }

    public void setSeminarySplit(int seminarySplit) {
        this.seminarySplit = seminarySplit;
    }

    public int getLaboratorySplit() {
        return laboratorySplit;
    }

    public void setLaboratorySplit(int laboratorySplit) {
        this.laboratorySplit = laboratorySplit;
    }

    public int getSeminaryGroupSize() {
        return size / seminarySplit;
    }

    public int getLaboratoryGroupSize() {
        return size / laboratorySplit;
    }

    public boolean hasSubject(String subjectName) {
        return subjects != null && subjects.contains(subjectName);
    }

    @Override
    public String toString() {
        return "Group{" +
                "id='" + id + '\'' +
                ", size=" + size +
                ", language='" + language + '\'' +
                ", subjects=" + subjects +
                ", seminarySplit=" + seminarySplit +
                ", laboratorySplit=" + laboratorySplit +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}