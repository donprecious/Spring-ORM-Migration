package com.orm.sql;

/**
 * Enumeration of supported database types.
 * Each type includes information about its JDBC URL pattern and driver class.
 */
public enum DatabaseType {
    MYSQL("jdbc:mysql:", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("jdbc:postgresql:", "org.postgresql.Driver"),
    ORACLE("jdbc:oracle:", "oracle.jdbc.OracleDriver"),
    SQL_SERVER("jdbc:sqlserver:", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    SQLITE("jdbc:sqlite:", "org.sqlite.JDBC");

    private final String jdbcUrlPrefix;
    private final String driverClassName;

    DatabaseType(String jdbcUrlPrefix, String driverClassName) {
        this.jdbcUrlPrefix = jdbcUrlPrefix;
        this.driverClassName = driverClassName;
    }

    /**
     * Gets the JDBC URL prefix for this database type.
     *
     * @return The JDBC URL prefix
     */
    public String getJdbcUrlPrefix() {
        return jdbcUrlPrefix;
    }

    /**
     * Gets the driver class name for this database type.
     *
     * @return The fully qualified driver class name
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * Determines the database type from a JDBC URL.
     *
     * @param jdbcUrl The JDBC URL to analyze
     * @return The corresponding database type
     * @throws UnsupportedDatabaseException if the database type cannot be determined
     */
    public static DatabaseType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new UnsupportedDatabaseException("JDBC URL cannot be null or empty");
        }

        for (DatabaseType type : values()) {
            if (jdbcUrl.toLowerCase().startsWith(type.jdbcUrlPrefix.toLowerCase())) {
                return type;
            }
        }

        throw new UnsupportedDatabaseException("Unsupported database type for JDBC URL: " + jdbcUrl);
    }
} 