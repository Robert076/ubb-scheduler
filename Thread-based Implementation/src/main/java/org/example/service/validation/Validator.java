package org.example.service.validation;

import org.example.context.TimetableDataContext;

/**
 * Validator interface - all validators must implement this.
 * Each validator performs a specific validation check.
 */
public interface Validator {
    /**
     * Perform validation using the timetable data context.
     * @param context TimetableDataContext with all loaded data and indices
     * @return ValidationResult with status, message, details, and execution time
     */
    ValidationResult validate(TimetableDataContext context);

    /**
     * Get the human-readable name of this validator.
     * @return validator name (e.g., "V0_TeacherDefinition")
     */
    String getValidatorName();
}
