package com.orm.sql;

/**
 * Exception thrown when an unsupported database type is encountered
 * or when a database operation is not supported by a specific dialect.
 */
public class UnsupportedDatabaseException extends RuntimeException {

    /**
     * Constructs a new UnsupportedDatabaseException with the specified message.
     *
     * @param message The detail message
     */
    public UnsupportedDatabaseException(String message) {
        super(message);
    }

    /**
     * Constructs a new UnsupportedDatabaseException with the specified message and cause.
     *
     * @param message The detail message
     * @param cause The cause of the exception
     */
    public UnsupportedDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
} 