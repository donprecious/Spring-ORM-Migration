package com.orm.util;

/**
 * Utility class for string manipulation.
 */
public class StringUtil {
    private StringUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts a string to snake case.
     *
     * @param input The input string
     * @return The snake case version of the string
     */
    public static String toSnakeCase(String input) {
        if (input == null) return null;
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        return input.replaceAll(regex, replacement).toLowerCase();
    }

    /**
     * Converts a string to camel case.
     *
     * @param input The input string
     * @return The camel case version of the string
     */
    public static String toCamelCase(String input) {
        if (input == null) return null;
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            
            if (currentChar == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(currentChar));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(currentChar));
                }
            }
        }
        
        return result.toString();
    }

    /**
     * Checks if a string is null or empty.
     *
     * @param str The string to check
     * @return true if the string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Gets the default value if the input is null or empty.
     *
     * @param input The input string
     * @param defaultValue The default value
     * @return The input string if not null or empty, otherwise the default value
     */
    public static String getDefaultIfEmpty(String input, String defaultValue) {
        return isNullOrEmpty(input) ? defaultValue : input;
    }
} 