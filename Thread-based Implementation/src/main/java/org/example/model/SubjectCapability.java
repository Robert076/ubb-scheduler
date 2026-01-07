package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty; /**
 * Model class representing a subject capability for a teacher.
 */
public class SubjectCapability {
    @JsonProperty("canSeminary")
    private boolean canSeminary;

    @JsonProperty("canLaboratory")
    private boolean canLaboratory;

    public SubjectCapability() {}

    public SubjectCapability(boolean canSeminary, boolean canLaboratory) {
        this.canSeminary = canSeminary;
        this.canLaboratory = canLaboratory;
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
                "canSeminary=" + canSeminary +
                ", canLaboratory=" + canLaboratory +
                '}';
    }
}
