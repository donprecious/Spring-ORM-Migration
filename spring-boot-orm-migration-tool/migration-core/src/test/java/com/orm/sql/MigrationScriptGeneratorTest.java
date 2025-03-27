package com.orm.sql;

import com.orm.migration.MigrationScript;
import com.orm.schema.ColumnMetadata;
import com.orm.schema.TableMetadata;
import com.orm.schema.diff.SchemaChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MigrationScriptGeneratorTest {

    @Mock
    private SqlDialect sqlDialect;

    private DefaultMigrationScriptGenerator generator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        generator = new DefaultMigrationScriptGenerator(sqlDialect);
    }

    @Test
    @DisplayName("Should generate migration script for single change")
    void shouldGenerateMigrationScriptForSingleChange() {
        // Given
        TableMetadata table = new TableMetadata();
        table.setTableName("users");
        
        SchemaChange change = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.CREATE_TABLE)
            .objectName("users")
            .objectType("TABLE")
            .table(table)
            .riskLevel(SchemaChange.RiskLevel.LOW)
            .build();

        when(sqlDialect.createTableSql(table))
            .thenReturn("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY)");

        // When
        MigrationScript script = generator.generateMigration(Collections.singletonList(change), "Create users table");

        // Then
        assertNotNull(script);
        assertTrue(script.getVersion().matches("\\d{14}"));
        List<String> upSql = script.getUpSql();
        assertTrue(upSql.stream().anyMatch(sql -> sql.equals("START TRANSACTION;")));
        assertTrue(upSql.stream().anyMatch(sql -> sql.contains("CREATE TABLE users")));
        assertTrue(upSql.stream().anyMatch(sql -> sql.equals("COMMIT;")));
        assertTrue(script.getDownSql().size() > 0);
        assertTrue(script.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("Should generate migration script with warnings for destructive changes")
    void shouldGenerateWarningsForDestructiveChanges() {
        // Given
        TableMetadata usersTable = new TableMetadata();
        usersTable.setTableName("users");
        
        ColumnMetadata emailColumn = new ColumnMetadata();
        emailColumn.setName("email");
        
        SchemaChange dropTable = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.DROP_TABLE)
            .objectName("users")
            .objectType("TABLE")
            .table(usersTable)
            .riskLevel(SchemaChange.RiskLevel.CRITICAL)
            .destructive(true)
            .build();

        SchemaChange dropColumn = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.DROP_COLUMN)
            .objectName("email")
            .objectType("COLUMN")
            .table(usersTable)
            .column(emailColumn)
            .riskLevel(SchemaChange.RiskLevel.CRITICAL)
            .destructive(true)
            .build();

        when(sqlDialect.dropTableSql(any(TableMetadata.class))).thenReturn("DROP TABLE users");
        when(sqlDialect.dropColumnSql(any(TableMetadata.class), eq("email"))).thenReturn("ALTER TABLE users DROP COLUMN email");

        // When
        MigrationScript script = generator.generateMigration(Arrays.asList(dropTable, dropColumn), "Drop users table and email column");

        // Then
        assertNotNull(script);
        assertTrue(script.hasWarnings());
        assertEquals(2, script.getWarnings().size());
        List<String> upSql = script.getUpSql();
        assertTrue(upSql.stream().anyMatch(sql -> sql.equals("START TRANSACTION;")));
        assertTrue(upSql.stream().anyMatch(sql -> sql.contains("DROP TABLE users")));
        assertTrue(upSql.stream().anyMatch(sql -> sql.contains("DROP COLUMN email")));
        assertTrue(script.getDownSql().size() > 0);
    }

    @Test
    @DisplayName("Should generate descriptive comments for each change")
    void shouldGenerateDescriptiveComments() {
        // Given
        TableMetadata usersTable = new TableMetadata();
        usersTable.setTableName("users");
        
        ColumnMetadata emailColumn = new ColumnMetadata();
        emailColumn.setName("email");
        
        SchemaChange addColumn = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.ADD_COLUMN)
            .objectName("email")
            .objectType("COLUMN")
            .table(usersTable)
            .column(emailColumn)
            .riskLevel(SchemaChange.RiskLevel.LOW)
            .build();

        when(sqlDialect.addColumnSql(any(TableMetadata.class), any(ColumnMetadata.class)))
            .thenReturn("ALTER TABLE users ADD COLUMN email VARCHAR(255)");

        // When
        MigrationScript script = generator.generateMigration(Collections.singletonList(addColumn), "Add email column to users table");

        // Then
        assertNotNull(script);
        List<String> upSql = script.getUpSql();
        assertTrue(upSql.stream().anyMatch(sql -> sql.contains("add column")));
    }

    @Test
    @DisplayName("Should handle multiple changes in correct order")
    void shouldHandleMultipleChangesInOrder() {
        // Given
        TableMetadata usersTable = new TableMetadata();
        usersTable.setTableName("users");
        
        ColumnMetadata emailColumn = new ColumnMetadata();
        emailColumn.setName("email");
        
        List<SchemaChange> changes = Arrays.asList(
            SchemaChange.builder()
                .changeType(SchemaChange.ChangeType.CREATE_TABLE)
                .objectName("users")
                .objectType("TABLE")
                .table(usersTable)
                .riskLevel(SchemaChange.RiskLevel.LOW)
                .build(),
            SchemaChange.builder()
                .changeType(SchemaChange.ChangeType.ADD_COLUMN)
                .objectName("email")
                .objectType("COLUMN")
                .table(usersTable)
                .column(emailColumn)
                .riskLevel(SchemaChange.RiskLevel.LOW)
                .build()
        );

        when(sqlDialect.createTableSql(any(TableMetadata.class)))
            .thenReturn("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY)");
        when(sqlDialect.addColumnSql(any(TableMetadata.class), any(ColumnMetadata.class)))
            .thenReturn("ALTER TABLE users ADD COLUMN email VARCHAR(255)");

        // When
        MigrationScript script = generator.generateMigration(changes, "Create users table and add email column");

        // Then
        assertNotNull(script);
        List<String> upSql = script.getUpSql();
        String createTableStatement = upSql.stream()
            .filter(sql -> sql.contains("CREATE TABLE"))
            .findFirst()
            .orElse("");
        String addColumnStatement = upSql.stream()
            .filter(sql -> sql.contains("ADD COLUMN"))
            .findFirst()
            .orElse("");
        
        assertFalse(createTableStatement.isEmpty());
        assertFalse(addColumnStatement.isEmpty());
        
        int createTableIndex = upSql.indexOf(createTableStatement);
        int addColumnIndex = upSql.indexOf(addColumnStatement);
        
        assertTrue(createTableIndex < addColumnIndex, "CREATE TABLE should come before ADD COLUMN");
    }

    @Test
    @DisplayName("Should return null for empty changes")
    void shouldReturnNullForEmptyChanges() {
        // When
        MigrationScript script = generator.generateMigration(Collections.emptyList(), "No changes");

        // Then
        assertNull(script);
    }
} 