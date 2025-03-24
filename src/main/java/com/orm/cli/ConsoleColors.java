package com.orm.cli;

/**
 * Utility class for ANSI color codes to enhance CLI output.
 */
public class ConsoleColors {
    // Reset
    public static final String RESET = "\033[0m";

    // Regular Colors
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String RED = "\033[0;31m";
    public static final String BLUE = "\033[0;34m";
    public static final String CYAN = "\033[0;36m";

    // Bold
    public static final String BOLD_GREEN = "\033[1;32m";
    public static final String BOLD_YELLOW = "\033[1;33m";
    public static final String BOLD_RED = "\033[1;31m";
    public static final String BOLD_BLUE = "\033[1;34m";

    private ConsoleColors() {} // Prevent instantiation

    /**
     * Wraps text with color and reset codes.
     */
    public static String color(String text, String color) {
        return color + text + RESET;
    }

    /**
     * Formats a success message in green.
     */
    public static String success(String text) {
        return color(text, GREEN);
    }

    /**
     * Formats a warning message in yellow.
     */
    public static String warning(String text) {
        return color(text, YELLOW);
    }

    /**
     * Formats an error message in red.
     */
    public static String error(String text) {
        return color(text, RED);
    }

    /**
     * Formats an info message in blue.
     */
    public static String info(String text) {
        return color(text, BLUE);
    }

    /**
     * Formats a heading in bold blue.
     */
    public static String heading(String text) {
        return color(text, BOLD_BLUE);
    }
} 