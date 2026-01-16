package org.example;

import org.example.initialization.TimetableInitializer;

/**
 * Main entry point for the Timetable Generator application.
 * Minimal orchestration - all output and logic delegated to service layer.
 */
public class Main {

    public static void main(String[] args) {
        TimetableInitializer initializer = new TimetableInitializer();
        initializer.initialize();
    }
}
