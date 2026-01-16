package org.example.initialization;

import org.example.context.TimetableDataContext;
import org.example.repository.*;
import org.example.ui.ConsoleUI;
import org.example.service.validation.ValidationOrchestrator;

/**
 * TimetableInitializer - Orchestrates the entire initialization flow.
 * ZERO prints here - all output delegated to ConsoleUI.
 * This is pure SERVICE LOGIC ONLY.
 */
public class TimetableInitializer {
    private TimetableDataContext dataContext;
    private PlaceRepository placeRepository;

    /**
     * Run complete initialization pipeline
     */
    public void initialize() {
        try {
            // Show welcome
            ConsoleUI.printWelcome();

            // Load and initialize
            if (!loadAndInitialize()) {
                ConsoleUI.printError("Failed to load data and initialize context");
                System.exit(1);
            }

            // Run validations
            if (!validate()) {
                ConsoleUI.printError("Validation process failed");
                System.exit(1);
            }

        } catch (Exception e) {
            ConsoleUI.printError("Fatal error during initialization: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load repositories and build context
     * @return true if successful, false otherwise
     */
    private boolean loadAndInitialize() {
        try {
            // UI: show phase header
            ConsoleUI.printInitializationPhase();

            // UI: show loading message
            ConsoleUI.printLoadingRepositories();

            // SERVICE: Load repositories
            GroupRepository groupRepository = new GroupRepository();
            TeacherRepository teacherRepository = new TeacherRepository();
            SubjectRepository subjectRepository = new SubjectRepository();
            this.placeRepository = new PlaceRepository();

            // UI: show loaded success
            ConsoleUI.printRepositoriesLoaded();

            // UI: show building context message
            ConsoleUI.printBuildingContext();

            // SERVICE: Build context (FIRST TIME - with parameters)
            this.dataContext = TimetableDataContext.getInstance(
                    groupRepository,
                    teacherRepository,
                    subjectRepository,
                    placeRepository
            );

            // UI: show context built success
            ConsoleUI.printContextBuilt();

            // UI: show data summary
            ConsoleUI.printDataLoadedSummary(dataContext);

            return true;

        } catch (Exception e) {
            ConsoleUI.printError("Failed to load data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Run all validations
     * @return true if successful, false otherwise
     */
    private boolean validate() {
        try {
            // UI: show validation phase header
            ConsoleUI.printValidationPhase();

            // SERVICE: Create orchestrator and run validations
            // Use the already-initialized dataContext (stored in this.dataContext)
            ValidationOrchestrator orchestrator = new ValidationOrchestrator(
                    this.dataContext,
                    this.placeRepository
            );

            ValidationOrchestrator.ValidationReport report = orchestrator.runAllValidations();
            orchestrator.shutdown();

            // UI: Display results and get user decision
            boolean userWantsToProceed = ConsoleUI.displayValidationResults(report);

            if (!userWantsToProceed) {
                ConsoleUI.printUserDeclined();
                System.exit(0);
            }

            // User wants to proceed
            ConsoleUI.printUserConfirmed();
            ConsoleUI.printNextSteps();

            return true;

        } catch (Exception e) {
            ConsoleUI.printError("Validation failed: " + e.getMessage());
            return false;
        }
    }
}
