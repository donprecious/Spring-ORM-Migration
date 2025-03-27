package com.orm.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SqlDialectFactoryTest {

    private SqlDialectFactory factory;
    private MySqlDialect mySqlDialect;
    private PostgreSqlDialect postgreSqlDialect;
    private OracleDialect oracleDialect;
    private SqlServerDialect sqlServerDialect;
    private SqliteDialect sqliteDialect;

    @BeforeEach
    void setUp() {
        mySqlDialect = mock(MySqlDialect.class);
        postgreSqlDialect = mock(PostgreSqlDialect.class);
        oracleDialect = mock(OracleDialect.class);
        sqlServerDialect = mock(SqlServerDialect.class);
        sqliteDialect = mock(SqliteDialect.class);

        factory = new SqlDialectFactory(
            Optional.of(mySqlDialect),
            Optional.of(postgreSqlDialect),
            Optional.of(oracleDialect),
            Optional.of(sqlServerDialect),
            Optional.of(sqliteDialect)
        );
    }

    @Test
    @DisplayName("Should return correct dialect for database type")
    void shouldReturnCorrectDialectForDatabaseType() {
        assertEquals(mySqlDialect, factory.getDialect(DatabaseType.MYSQL));
        assertEquals(postgreSqlDialect, factory.getDialect(DatabaseType.POSTGRESQL));
        assertEquals(oracleDialect, factory.getDialect(DatabaseType.ORACLE));
        assertEquals(sqlServerDialect, factory.getDialect(DatabaseType.SQL_SERVER));
        assertEquals(sqliteDialect, factory.getDialect(DatabaseType.SQLITE));
    }

    @Test
    @DisplayName("Should return correct dialect for JDBC URL")
    void shouldReturnCorrectDialectForJdbcUrl() {
        assertEquals(mySqlDialect, 
            factory.getDialectFromJdbcUrl("jdbc:mysql://localhost:3306/testdb"));
        assertEquals(postgreSqlDialect, 
            factory.getDialectFromJdbcUrl("jdbc:postgresql://localhost:5432/testdb"));
        assertEquals(oracleDialect, 
            factory.getDialectFromJdbcUrl("jdbc:oracle:thin:@localhost:1521:testdb"));
        assertEquals(sqlServerDialect, 
            factory.getDialectFromJdbcUrl("jdbc:sqlserver://localhost:1433;database=testdb"));
        assertEquals(sqliteDialect, 
            factory.getDialectFromJdbcUrl("jdbc:sqlite:test.db"));
    }

    @Test
    @DisplayName("Should throw exception for unsupported database type")
    void shouldThrowExceptionForUnsupportedDatabaseType() {
        SqlDialectFactory emptyFactory = new SqlDialectFactory(
            Optional.empty(), Optional.empty(), Optional.empty(), 
            Optional.empty(), Optional.empty()
        );

        assertThrows(UnsupportedDatabaseException.class, () -> 
            emptyFactory.getDialect(DatabaseType.MYSQL));
    }

    @Test
    @DisplayName("Should throw exception for invalid JDBC URL")
    void shouldThrowExceptionForInvalidJdbcUrl() {
        assertThrows(UnsupportedDatabaseException.class, () -> 
            factory.getDialectFromJdbcUrl("jdbc:unsupported:test"));
        assertThrows(UnsupportedDatabaseException.class, () -> 
            factory.getDialectFromJdbcUrl(""));
        assertThrows(UnsupportedDatabaseException.class, () -> 
            factory.getDialectFromJdbcUrl(null));
    }

    @Test
    @DisplayName("Should correctly check dialect availability")
    void shouldCheckDialectAvailability() {
        assertTrue(factory.hasDialect(DatabaseType.MYSQL));
        assertTrue(factory.hasDialect(DatabaseType.POSTGRESQL));
        assertTrue(factory.hasDialect(DatabaseType.ORACLE));
        assertTrue(factory.hasDialect(DatabaseType.SQL_SERVER));
        assertTrue(factory.hasDialect(DatabaseType.SQLITE));

        SqlDialectFactory emptyFactory = new SqlDialectFactory(
            Optional.empty(), Optional.empty(), Optional.empty(), 
            Optional.empty(), Optional.empty()
        );
        assertFalse(emptyFactory.hasDialect(DatabaseType.MYSQL));
    }

    @Test
    @DisplayName("Should return all supported databases")
    void shouldReturnAllSupportedDatabases() {
        DatabaseType[] supported = factory.getSupportedDatabases();
        assertEquals(5, supported.length);
        assertTrue(containsAll(supported, DatabaseType.values()));
    }

    private boolean containsAll(DatabaseType[] array, DatabaseType[] values) {
        for (DatabaseType value : values) {
            boolean found = false;
            for (DatabaseType element : array) {
                if (element == value) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
} 