package com.orm.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory class for creating and managing SQL dialect instances.
 * This factory provides centralized dialect selection based on database type
 * and manages the lifecycle of dialect instances.
 */
@Component
public class SqlDialectFactory {

    private final Map<DatabaseType, SqlDialect> dialects;

    @Autowired
    public SqlDialectFactory(Optional<MySqlDialect> mySqlDialect,
                           Optional<PostgreSqlDialect> postgreSqlDialect,
                           Optional<OracleDialect> oracleDialect,
                           Optional<SqlServerDialect> sqlServerDialect,
                           Optional<SqliteDialect> sqliteDialect) {
        dialects = new HashMap<>();
        
        mySqlDialect.ifPresent(dialect -> dialects.put(DatabaseType.MYSQL, dialect));
        postgreSqlDialect.ifPresent(dialect -> dialects.put(DatabaseType.POSTGRESQL, dialect));
        oracleDialect.ifPresent(dialect -> dialects.put(DatabaseType.ORACLE, dialect));
        sqlServerDialect.ifPresent(dialect -> dialects.put(DatabaseType.SQL_SERVER, dialect));
        sqliteDialect.ifPresent(dialect -> dialects.put(DatabaseType.SQLITE, dialect));
    }

    /**
     * Gets a dialect instance for the specified database type.
     *
     * @param databaseType The type of database to get a dialect for
     * @return The appropriate SQL dialect instance
     * @throws UnsupportedDatabaseException if the database type is not supported
     */
    public SqlDialect getDialect(DatabaseType databaseType) {
        SqlDialect dialect = dialects.get(databaseType);
        if (dialect == null) {
            throw new UnsupportedDatabaseException("No dialect found for database type: " + databaseType);
        }
        return dialect;
    }

    /**
     * Gets a dialect instance based on a JDBC URL.
     *
     * @param jdbcUrl The JDBC URL to determine the dialect from
     * @return The appropriate SQL dialect instance
     * @throws UnsupportedDatabaseException if the database type cannot be determined or is not supported
     */
    public SqlDialect getDialectFromJdbcUrl(String jdbcUrl) {
        DatabaseType type = DatabaseType.fromJdbcUrl(jdbcUrl);
        return getDialect(type);
    }

    /**
     * Checks if a dialect is available for the specified database type.
     *
     * @param databaseType The database type to check
     * @return true if a dialect is available, false otherwise
     */
    public boolean hasDialect(DatabaseType databaseType) {
        return dialects.containsKey(databaseType);
    }

    /**
     * Gets all supported database types.
     *
     * @return Array of supported database types
     */
    public DatabaseType[] getSupportedDatabases() {
        return dialects.keySet().toArray(new DatabaseType[0]);
    }

    /**
     * Creates a dialect instance based on a JDBC URL.
     *
     * @param jdbcUrl The JDBC URL to determine the dialect from
     * @return Optional containing the appropriate SQL dialect instance, or empty if not supported
     */
    public static Optional<SqlDialect> createFromJdbcUrl(String jdbcUrl) {
        try {
            DatabaseType type = DatabaseType.fromJdbcUrl(jdbcUrl);
            switch (type) {
                case MYSQL:
                    return Optional.of(new MySqlDialect());
                case POSTGRESQL:
                    return Optional.of(new PostgreSqlDialect());
                case ORACLE:
                    return Optional.of(new OracleDialect());
                case SQL_SERVER:
                    return Optional.of(new SqlServerDialect());
                case SQLITE:
                    return Optional.of(new SqliteDialect());
                default:
                    return Optional.empty();
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
} 