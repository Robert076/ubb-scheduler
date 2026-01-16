package org.example.ui;

import org.example.context.TimetableDataContext;
import org.example.service.generation.*;
import org.example.service.validation.ValidationOrchestrator;
import org.example.service.validation.ValidationResult;
import java.util.*;

/**
 * ConsoleUI - Handles ALL console output and user interaction.
 * This is the ONLY place that prints to console.
 * No service logic here - ONLY presentation.
 */
public class ConsoleUI {
    private static final String HEADER_COLOR = "\u001B[36m";    // Cyan
    private static final String SUCCESS_COLOR = "\u001B[32m";   // Green
    private static final String WARNING_COLOR = "\u001B[33m";   // Yellow
    private static final String ERROR_COLOR = "\u001B[31m";     // Red
    private static final String INFO_COLOR = "\u001B[34m";      // Blue
    private static final String DEBUG_COLOR = "\u001B[35m";     // Magenta
    private static final String RESET_COLOR = "\u001B[0m";      // Reset

    private static final Scanner scanner = new Scanner(System.in);

    // ==================== WELCOME & BANNERS ====================

    public static void printWelcome() {
        System.out.println();
        System.out.println(HEADER_COLOR + "╔════════════════════════════════════════════╗" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "║  UNIVERSITY SCHEDULE MANAGER v1.0          ║" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "║  Timetable Generator & Validator           ║" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "╚════════════════════════════════════════════╝" + RESET_COLOR);
        System.out.println();
    }

    // ==================== INITIALIZATION PHASE ====================

    public static void printInitializationPhase() {
        System.out.println(INFO_COLOR + "\n[PHASE 1] INITIALIZATION" + RESET_COLOR);
        System.out.println(INFO_COLOR + "━".repeat(50) + RESET_COLOR);
    }

    public static void printLoadingRepositories() {
        System.out.println(INFO_COLOR + "ℹ Loading repositories from config files..." + RESET_COLOR);
    }

    public static void printRepositoriesLoaded() {
        System.out.println(SUCCESS_COLOR + "✓ Repositories loaded" + RESET_COLOR);
    }

    public static void printBuildingContext() {
        System.out.println(INFO_COLOR + "ℹ Building centralized data context..." + RESET_COLOR);
    }

    public static void printContextBuilt() {
        System.out.println(SUCCESS_COLOR + "✓ Context built with pre-computed indices" + RESET_COLOR);
    }

    public static void printDataLoadedSummary(TimetableDataContext context) {
        System.out.println(SUCCESS_COLOR + "✓ Data loaded successfully:" + RESET_COLOR);
        System.out.println("  • Groups:   " + context.getGroups().size());
        System.out.println("  • Teachers: " + context.getTeachers().size());
        System.out.println("  • Subjects: " + context.getSubjects().size());
        System.out.println("  • Places:   " + context.getPlaces().size());
        System.out.println();
    }

    // ==================== VALIDATION PHASE ====================

    public static void printValidationPhase() {
        System.out.println(INFO_COLOR + "\n[PHASE 2] VALIDATION" + RESET_COLOR);
        System.out.println(INFO_COLOR + "━".repeat(50) + RESET_COLOR);
    }

    /**
     * Display validation results and prompt user for confirmation
     * @return true if user wants to proceed, false otherwise
     */
    public static boolean displayValidationResults(ValidationOrchestrator.ValidationReport report) {
        System.out.println();
        System.out.println(HEADER_COLOR + "╔════════════════════════════════════════════╗" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "║     VALIDATION RESULTS SUMMARY             ║" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "╚════════════════════════════════════════════╝" + RESET_COLOR);
        System.out.println();

        int passCount = 0, warnCount = 0, failCount = 0;

        for (var result : report.getResults()) {
            String statusStr = switch (result.getStatus()) {
                case PASS -> SUCCESS_COLOR + "✓ PASS" + RESET_COLOR;
                case WARN -> WARNING_COLOR + "⚠ WARN" + RESET_COLOR;
                case FAIL -> ERROR_COLOR + "✗ FAIL" + RESET_COLOR;
            };

            System.out.println(statusStr + " | " + result.getValidatorName() + " (" + result.getExecutionTimeMs() + "ms)");
            System.out.println("      " + result.getMessage());

            // Print detailed information if validator has details
            if (result.getDetails() != null && !result.getDetails().isEmpty()) {
                printValidationDetails(result.getDetails());
            }

            System.out.println();

            switch (result.getStatus()) {
                case PASS: passCount++; break;
                case WARN: warnCount++; break;
                case FAIL: failCount++; break;
            }
        }

        System.out.println("─".repeat(50));
        System.out.println(SUCCESS_COLOR + "✓ PASS: " + passCount + RESET_COLOR);
        System.out.println(WARNING_COLOR + "⚠ WARN: " + warnCount + RESET_COLOR);
        System.out.println(ERROR_COLOR + "✗ FAIL: " + failCount + RESET_COLOR);
        System.out.println("Total Time: " + report.getTotalTimeMs() + "ms");
        System.out.println("─".repeat(50));
        System.out.println();

        // Check overall status
        boolean canProceed = report.getOverallStatus() != ValidationResult.Status.FAIL;

        // Print recommendation
        String recommendation = switch (report.getOverallStatus()) {
            case PASS -> SUCCESS_COLOR + "✓ All checks passed! System is ready for timetable generation." + RESET_COLOR;
            case WARN -> WARNING_COLOR + "⚠ Some warnings detected. You can still generate the schedule." + RESET_COLOR;
            case FAIL -> ERROR_COLOR + "✗ Critical issues found. Fix them before proceeding." + RESET_COLOR;
        };

        System.out.println(recommendation);
        System.out.println();

        // If cannot proceed, return false
        if (!canProceed) {
            System.out.println(HEADER_COLOR + "╔════════════════════════════════════════════╗" + RESET_COLOR);
            System.out.println(HEADER_COLOR + "║   VALIDATION INCOMPLETE - CANNOT PROCEED    ║" + RESET_COLOR);
            System.out.println(HEADER_COLOR + "╚════════════════════════════════════════════╝" + RESET_COLOR);
            System.out.println();
            return false;
        }

        // Can proceed - ask user confirmation
        System.out.println(HEADER_COLOR + "╔════════════════════════════════════════════╗" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "║   READY FOR TIMETABLE GENERATION            ║" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "╚════════════════════════════════════════════╝" + RESET_COLOR);
        System.out.println();

        return promptUserConfirmation();
    }

    /**
     * Print detailed information from validation results
     */
    private static void printValidationDetails(Map<String, Object> details) {
        if (details.containsKey("invalidSubjects")) {
            @SuppressWarnings("unchecked")
            List<String> invalidItems = (List<String>) details.get("invalidSubjects");
            System.out.println(ERROR_COLOR + "      Invalid items:" + RESET_COLOR);
            for (String item : invalidItems) {
                System.out.println(ERROR_COLOR + "        • " + item + RESET_COLOR);
            }
        }
    }

    // ==================== GENERATION PHASE ====================

    public static void printGenerationPhase() {
        System.out.println(INFO_COLOR + "\n[PHASE 3] TIMETABLE GENERATION" + RESET_COLOR);
        System.out.println(INFO_COLOR + "━".repeat(50) + RESET_COLOR);
    }

    /**
     * Display generation results with detailed metrics
     */
    public static void displayGenerationResults(GenerationResult result) {
        System.out.println();
        System.out.println(HEADER_COLOR + "╔════════════════════════════════════════════╗" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "║     GENERATION RESULTS SUMMARY             ║" + RESET_COLOR);
        System.out.println(HEADER_COLOR + "╚════════════════════════════════════════════╝" + RESET_COLOR);
        System.out.println();

        if (result.success()) {
            System.out.println(SUCCESS_COLOR + "✓ GENERATION COMPLETED SUCCESSFULLY" + RESET_COLOR);
        } else {
            System.out.println(ERROR_COLOR + "✗ GENERATION FAILED: " + result.errorMessage() + RESET_COLOR);
        }

        System.out.println();
        System.out.println("📊 STATISTICS:");
        System.out.println("  • Total Activities Scheduled: " + result.getTotalActivities());
        System.out.println("  • Success Rate: " + String.format("%.1f%%", result.getSuccessRate()));
        System.out.println("  • Total Time: " + result.getTotalTimeMs() + "ms");

        // Display phase metrics
        System.out.println();
        System.out.println("⏱️  PHASE BREAKDOWN:");
        for (Map.Entry<String, Long> entry : result.metrics().getPhaseTimes().entrySet()) {
            System.out.println("  • " + entry.getKey() + ": " + entry.getValue() + "ms");
        }

        // Display subject metrics
        System.out.println();
        System.out.println("📚 SUBJECT SCHEDULING:");
        for (TimetableGenerator.SubjectGenerationResult subjectResult : result.subjectResults()) {
            String status = subjectResult.success() ? SUCCESS_COLOR + "✓" : ERROR_COLOR + "✗";
            System.out.println(status + " " + RESET_COLOR + subjectResult.subjectName() +
                    " - " + subjectResult.scheduledHours() + " hours (" + subjectResult.executionTimeMs() + "ms)");
        }

        // Display detailed metrics
        System.out.println();
        System.out.println("🔍 DETAILED METRICS:");
        for (Map.Entry<String, String> entry : result.metrics().getMetrics().entrySet()) {
            System.out.println("  • " + entry.getKey() + ": " + entry.getValue());
        }

        if (!result.metrics().getErrors().isEmpty()) {
            System.out.println();
            System.out.println(ERROR_COLOR + "⚠️  ERRORS ENCOUNTERED:" + RESET_COLOR);
            for (String error : result.metrics().getErrors()) {
                System.out.println("  • " + error);
            }
        }

        System.out.println();
        System.out.println("─".repeat(50));

        if (result.success()) {
            System.out.println(SUCCESS_COLOR + "✓ Timetable successfully generated!" + RESET_COLOR);
        } else {
            System.out.println(ERROR_COLOR + "✗ Timetable generation had issues. Check above." + RESET_COLOR);
        }

        System.out.println();
    }

    // ==================== SUCCESS & FAILURE ====================

    public static void printUserConfirmed() {
        System.out.println(SUCCESS_COLOR + "✓ User confirmed - Ready for timetable generation" + RESET_COLOR);
    }

    public static void printNextSteps() {
        System.out.println(INFO_COLOR + "ℹ Next: Export schedule or view detailed timetable" + RESET_COLOR);
        System.out.println();
    }

    public static void printUserDeclined() {
        System.out.println(INFO_COLOR + "ℹ User declined to proceed. Exiting..." + RESET_COLOR);
        System.out.println();
    }

    // ==================== GENERIC MESSAGES ====================

    public static void printError(String message) {
        System.out.println(ERROR_COLOR + "✗ ERROR: " + message + RESET_COLOR);
    }

    public static void printInfo(String message) {
        System.out.println(INFO_COLOR + "ℹ " + message + RESET_COLOR);
    }

    public static void printSuccess(String message) {
        System.out.println(SUCCESS_COLOR + "✓ " + message + RESET_COLOR);
    }

    // ==================== PRIVATE METHODS ====================

    private static boolean promptUserConfirmation() {
        System.out.print(INFO_COLOR + "Do you want to proceed with timetable generation? (y/n): " + RESET_COLOR);
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }
}
