package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Model class representing a subject/course.
 */
public class Subject {
    private String name;

    @JsonProperty("MainTeacher")
    private String mainTeacher;

    @JsonProperty("Language")
    private String language;

    @JsonProperty("CoursesPerWeek")
    private int coursesPerWeek;

    @JsonProperty("CourseLenght")
    private int courseLenght;

    @JsonProperty("SeminarsPerWeek")
    private int seminarsPerWeek;

    @JsonProperty("SeminarLenght")
    private int seminarLenght;

    @JsonProperty("LaboratoriesPerWeek")
    private double laboratoriesPerWeek;

    @JsonProperty("LaboratoriesLenght")
    private int laboratoriesLenght;

    public Subject() {}

    public Subject(String name, String mainTeacher, String language,
                   int coursesPerWeek, int courseLenght,
                   int seminarsPerWeek, int seminarLenght,
                   double laboratoriesPerWeek, int laboratoriesLenght) {
        this.name = name;
        this.mainTeacher = mainTeacher;
        this.language = language;
        this.coursesPerWeek = coursesPerWeek;
        this.courseLenght = courseLenght;
        this.seminarsPerWeek = seminarsPerWeek;
        this.seminarLenght = seminarLenght;
        this.laboratoriesPerWeek = laboratoriesPerWeek;
        this.laboratoriesLenght = laboratoriesLenght;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMainTeacher() {
        return mainTeacher;
    }

    public void setMainTeacher(String mainTeacher) {
        this.mainTeacher = mainTeacher;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getCoursesPerWeek() {
        return coursesPerWeek;
    }

    public void setCoursesPerWeek(int coursesPerWeek) {
        this.coursesPerWeek = coursesPerWeek;
    }

    public int getCourseLenght() {
        return courseLenght;
    }

    public void setCourseLenght(int courseLenght) {
        this.courseLenght = courseLenght;
    }

    public int getSeminarsPerWeek() {
        return seminarsPerWeek;
    }

    public void setSeminarsPerWeek(int seminarsPerWeek) {
        this.seminarsPerWeek = seminarsPerWeek;
    }

    public int getSeminarLenght() {
        return seminarLenght;
    }

    public void setSeminarLenght(int seminarLenght) {
        this.seminarLenght = seminarLenght;
    }

    public double getLaboratoriesPerWeek() {
        return laboratoriesPerWeek;
    }

    public void setLaboratoriesPerWeek(double laboratoriesPerWeek) {
        this.laboratoriesPerWeek = laboratoriesPerWeek;
    }

    public int getLaboratoriesLenght() {
        return laboratoriesLenght;
    }

    public void setLaboratoriesLenght(int laboratoriesLenght) {
        this.laboratoriesLenght = laboratoriesLenght;
    }

    public int getTotalHoursPerWeek() {
        return (coursesPerWeek * courseLenght) +
               (seminarsPerWeek * seminarLenght) +
               (int) (laboratoriesPerWeek * laboratoriesLenght);
    }

    public int getCourseHours() {
        return coursesPerWeek * courseLenght;
    }

    public int getSeminarHours() {
        return seminarsPerWeek * seminarLenght;
    }

    public int getLaboratoryHours() {
        return (int) Math.ceil(laboratoriesPerWeek * laboratoriesLenght);
    }

    public int getTotalHours() {
        return getTotalHoursPerWeek();
    }

    @Override
    public String toString() {
        return "Subject{" +
                "name='" + name + '\'' +
                ", mainTeacher='" + mainTeacher + '\'' +
                ", language='" + language + '\'' +
                ", coursesPerWeek=" + coursesPerWeek +
                ", courseLenght=" + courseLenght +
                ", seminarsPerWeek=" + seminarsPerWeek +
                ", seminarLenght=" + seminarLenght +
                ", laboratoriesPerWeek=" + laboratoriesPerWeek +
                ", laboratoriesLenght=" + laboratoriesLenght +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return Objects.equals(name, subject.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}