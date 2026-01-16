package org.example.service.validation;

import org.example.context.TimetableDataContext;
import java.util.*;

/**
 * Validation result object - contains status, message, details, and execution time.
 * Immutable after creation.
 */
public class ValidationResult {
    public enum Status {
        PASS("✓ PASS", "success"),
        WARN("⚠ WARN", "warning"),
        FAIL("✗ FAIL", "error");

        private final String display;
        private final String level;

        Status(String display, String level) {
            this.display = display;
            this.level = level;
        }

        public String getDisplay() { return display; }
        public String getLevel() { return level; }
    }

    private final String validatorName;
    private final Status status;
    private final String message;
    private final Map<String, Object> details;
    private final long executionTimeMs;

    public ValidationResult(String validatorName, Status status, String message,
                          Map<String, Object> details, long executionTimeMs) {
        this.validatorName = validatorName;
        this.status = status;
        this.message = message;
        this.details = Collections.unmodifiableMap(details != null ? new HashMap<>(details) : new HashMap<>());
        this.executionTimeMs = executionTimeMs;
    }

    public String getValidatorName() { return validatorName; }
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return details; }
    public long getExecutionTimeMs() { return executionTimeMs; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%dms)",
            status.getDisplay(),
            validatorName,
            message,
            executionTimeMs);
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n%s %s\n", status.getDisplay(), validatorName));
        sb.append(String.format("Message: %s\n", message));
        sb.append(String.format("Time: %dms\n", executionTimeMs));
        if (!details.isEmpty()) {
            sb.append("Details:\n");
            details.forEach((key, value) -> 
                sb.append(String.format("  - %s: %s\n", key, value)));
        }
        return sb.toString();
    }
}
