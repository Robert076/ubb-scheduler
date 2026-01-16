package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty; /**
 * Model class representing a subject capability for a teacher.
 */
public class SubjectCapability {
    @JsonProperty("canCourse")
    private boolean canCourse;

    @JsonProperty("canSeminary")
    private boolean canSeminary;

    @JsonProperty("canLaboratory")
    private boolean canLaboratory;

    public SubjectCapability() {}

    public SubjectCapability(boolean canCourse, boolean canSeminary, boolean canLaboratory) {
        this.canCourse = canCourse;
        this.canSeminary = canSeminary;
        this.canLaboratory = canLaboratory;
    }

    public boolean isCanCourse() {
        return canCourse;
    }

    public void setCanCourse(boolean canCourse) {
        this.canCourse = canCourse;
    }

    public boolean isCanSeminary() {
        return canSeminary;
    }

    public void setCanSeminary(boolean canSeminary) {
        this.canSeminary = canSeminary;
    }

    public boolean isCanLaboratory() {
        return canLaboratory;
    }

    public void setCanLaboratory(boolean canLaboratory) {
        this.canLaboratory = canLaboratory;
    }

    @Override
    public String toString() {
        return "SubjectCapability{" +
                "canCourse=" + canCourse +
                ", canSeminary=" + canSeminary +
                ", canLaboratory=" + canLaboratory +
                '}';
    }
}
