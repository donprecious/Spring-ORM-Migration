package com.orm.migration;

import com.orm.schema.*;
import com.orm.schema.diff.SchemaChange;
import com.orm.sql.MySqlDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.persistence.GenerationType;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MigrationGeneratorTest {

    @Mock
    private MySqlDialect sqlDialect;

    private MigrationGenerator generator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        generator = new MigrationGenerator(sqlDialect);

        // Setup common mock responses
        when(sqlDialect.createTable(any(TableMetadata.class))).thenReturn("CREATE TABLE test (id INT)");
        when(sqlDialect.dropTable(anyString())).thenReturn("DROP TABLE test");
        when(sqlDialect.addColumn(anyString(), any(ColumnMetadata.class))).thenReturn("ALTER TABLE test ADD COLUMN name VARCHAR(255)");
        when(sqlDialect.dropColumn(anyString(), anyString())).thenReturn("ALTER TABLE test DROP COLUMN name");
        when(sqlDialect.createIndex(anyString(), any(IndexMetadata.class))).thenReturn("CREATE INDEX idx_test ON test (name)");
        when(sqlDialect.dropIndex(anyString(), anyString())).thenReturn("DROP INDEX idx_test ON test");
    }

    @Test
    void shouldGenerateMigrationScriptForSingleChange() {
        // Given
        List<SchemaChange> changes = new ArrayList<>();
        TableMetadata usersTable = createBasicTable("users");
        
        SchemaChange change = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.CREATE_TABLE)
            .objectName("users")
            .objectType("TABLE")
            .table(usersTable)
            .build();
        
        changes.add(change);

        // When
        MigrationScript script = generator.generateMigration(changes, "Create users table");

        // Then
        assertNotNull(script);
        assertTrue(script.getVersion().matches("\\d{14}"));
        assertTrue(script.getUpSql().stream().anyMatch(sql -> sql.contains("CREATE TABLE")));
        assertTrue(script.getDownSql().stream().anyMatch(sql -> sql.contains("DROP TABLE")));
        assertTrue(script.getWarnings().isEmpty());
    }

    @Test
    void shouldGenerateWarningsForDestructiveChanges() {
        // Given
        List<SchemaChange> changes = new ArrayList<>();
        TableMetadata usersTable = createBasicTable("users");
        
        SchemaChange change = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.DROP_TABLE)
            .objectName("users")
            .objectType("TABLE")
            .table(usersTable)
            .destructive(true)
            .build();
            
        changes.add(change);

        // When
        MigrationScript script = generator.generateMigration(changes, "Drop users table");

        // Then
        assertNotNull(script);
        assertTrue(script.hasWarnings());
        assertTrue(script.getUpSql().stream().anyMatch(sql -> sql.contains("DROP TABLE")));
        assertEquals(1, script.getWarnings().size());
    }

    @Test
    void shouldGenerateDescriptiveComments() {
        // Given
        List<SchemaChange> changes = new ArrayList<>();
        TableMetadata usersTable = createBasicTable("users");
        ColumnMetadata emailColumn = createEmailColumn();
        
        SchemaChange change = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.ADD_COLUMN)
            .objectName("email")
            .objectType("COLUMN")
            .table(usersTable)
            .column(emailColumn)
            .build();
            
        changes.add(change);

        // When
        MigrationScript script = generator.generateMigration(changes, "Add email to users");

        // Then
        assertNotNull(script);
        assertTrue(script.getUpSql().stream().anyMatch(sql -> sql.contains("Add column")));
    }

    @Test
    void shouldHandleMultipleChangesInOrder() {
        // Given
        List<SchemaChange> changes = new ArrayList<>();
        TableMetadata usersTable = createBasicTable("users");
        ColumnMetadata emailColumn = createEmailColumn();
        
        SchemaChange createTable = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.CREATE_TABLE)
            .objectName("users")
            .objectType("TABLE")
            .table(usersTable)
            .build();
            
        SchemaChange addColumn = SchemaChange.builder()
            .changeType(SchemaChange.ChangeType.ADD_COLUMN)
            .objectName("email")
            .objectType("COLUMN")
            .table(usersTable)
            .column(emailColumn)
            .build();
            
        changes.add(createTable);
        changes.add(addColumn);

        // When
        MigrationScript script = generator.generateMigration(changes, "Create users table with email");

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
        
        assertTrue(createTableIndex < addColumnIndex);
    }

    @Test
    void shouldReturnNullForEmptyChanges() {
        // Given
        List<SchemaChange> changes = new ArrayList<>();

        // When
        MigrationScript script = generator.generateMigration(changes, "Empty migration");

        // Then
        assertNull(script);
    }

    private TableMetadata createBasicTable(String tableName) {
        TableMetadata table = new TableMetadata();
        table.setTableName(tableName);
        
        ColumnMetadata idColumn = new ColumnMetadata();
        idColumn.setName("id");
        idColumn.setFieldType(Long.class);
        idColumn.setGenerationType(GenerationType.IDENTITY);
        table.addColumn(idColumn);
        
        return table;
    }

    private ColumnMetadata createEmailColumn() {
        ColumnMetadata column = new ColumnMetadata();
        column.setName("email");
        column.setFieldType(String.class);
        column.setLength(255);
        column.setNullable(false);
        column.setUnique(true);
        return column;
    }
}