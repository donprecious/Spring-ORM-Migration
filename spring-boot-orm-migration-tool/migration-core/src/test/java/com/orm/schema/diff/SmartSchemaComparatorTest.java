package com.orm.schema.diff;

import com.orm.schema.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SmartSchemaComparatorTest {

    private SmartSchemaComparator comparator;
    private Set<TableMetadata> oldSchema;
    private Set<TableMetadata> newSchema;

    @BeforeEach
    void setUp() {
        comparator = new SmartSchemaComparator();
        oldSchema = new HashSet<>();
        newSchema = new HashSet<>();
    }

    @Test
    @DisplayName("Should detect table creation")
    void shouldDetectTableCreation() {
        // Given
        TableMetadata newTable = createTable("users", Arrays.asList(
            createColumn("id", true, false),
            createColumn("name", false, false)
        ));
        newSchema.add(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.CREATE_TABLE, change.getChangeType());
        assertEquals("users", change.getObjectName());
        assertEquals(SchemaChange.RiskLevel.LOW, change.getRiskLevel());
        assertFalse(change.isRequiresConfirmation());
    }

    @Test
    @DisplayName("Should detect table deletion with warning")
    void shouldDetectTableDeletion() {
        // Given
        TableMetadata oldTable = createTable("users", Arrays.asList(
            createColumn("id", true, false),
            createColumn("name", false, false)
        ));
        oldSchema.add(oldTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.DROP_TABLE, change.getChangeType());
        assertEquals("users", change.getObjectName());
        assertEquals(SchemaChange.RiskLevel.CRITICAL, change.getRiskLevel());
        assertTrue(change.isRequiresConfirmation());
        assertTrue(change.isDestructive());
    }

    @Test
    @DisplayName("Should suggest table rename when names are similar")
    void shouldSuggestTableRename() {
        // Given
        TableMetadata oldTable = createTable("user", Arrays.asList(
            createColumn("id", true, false),
            createColumn("name", false, false)
        ));
        
        TableMetadata newTable = createTable("users", Arrays.asList(
            createColumn("id", true, false),
            createColumn("name", false, false)
        ));
        
        oldSchema.add(oldTable);
        newSchema.add(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.RENAME_TABLE, change.getChangeType());
        assertEquals("user", change.getObjectName());
        assertEquals("users", change.getNewName());
        assertEquals(SchemaChange.RiskLevel.MEDIUM, change.getRiskLevel());
    }

    @Test
    @DisplayName("Should detect column addition")
    void shouldDetectColumnAddition() {
        // Given
        TableMetadata oldTable = createTable("users", Arrays.asList(
            createColumn("id", true, false)
        ));
        
        TableMetadata newTable = createTable("users", Arrays.asList(
            createColumn("id", true, false),
            createColumn("name", false, false)
        ));
        
        oldSchema.add(oldTable);
        newSchema.add(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.ADD_COLUMN, change.getChangeType());
        assertEquals("name", change.getObjectName());
        assertEquals(SchemaChange.RiskLevel.LOW, change.getRiskLevel());
        assertFalse(change.isRequiresConfirmation());
    }

    @Test
    @DisplayName("Should detect column deletion with warning")
    void shouldDetectColumnDeletion() {
        // Given
        TableMetadata oldTable = createTable("users", Arrays.asList(
            createColumn("id", true, false),
            createColumn("name", false, false)
        ));
        
        TableMetadata newTable = createTable("users", Arrays.asList(
            createColumn("id", true, false)
        ));
        
        oldSchema.add(oldTable);
        newSchema.add(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.DROP_COLUMN, change.getChangeType());
        assertEquals("name", change.getObjectName());
        assertEquals(SchemaChange.RiskLevel.HIGH, change.getRiskLevel());
        assertTrue(change.isRequiresConfirmation());
        assertTrue(change.isDestructive());
    }

    @Test
    @DisplayName("Should suggest column rename when names are similar")
    void shouldSuggestColumnRename() {
        // Given
        TableMetadata oldTable = createTable("users", Arrays.asList(
            createColumn("id", true, false),
            createColumn("firstname", false, false)
        ));
        
        TableMetadata newTable = createTable("users", Arrays.asList(
            createColumn("id", true, false),
            createColumn("first_name", false, false)
        ));
        
        oldSchema.add(oldTable);
        newSchema.add(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.RENAME_COLUMN, change.getChangeType());
        assertEquals("firstname", change.getObjectName());
        assertEquals("first_name", change.getNewName());
        assertEquals(SchemaChange.RiskLevel.MEDIUM, change.getRiskLevel());
    }

    @Test
    @DisplayName("Should detect index changes")
    void shouldDetectIndexChanges() {
        // Given
        TableMetadata oldTable = createTableWithIndex("users", 
            Arrays.asList(
                createColumn("id", true, false),
                createColumn("name", false, false)
            ),
            Arrays.asList(
                createIndex("idx_name", "name", false)
            )
        );
        
        TableMetadata newTable = createTableWithIndex("users", 
            Arrays.asList(
                createColumn("id", true, false),
                createColumn("name", false, false)
            ),
            Arrays.asList(
                createIndex("idx_name_unique", "name", true)
            )
        );
        
        oldSchema.add(oldTable);
        newSchema.add(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(2, changes.size());
        // Verify we have the DROP_INDEX and ADD_INDEX changes
        assertTrue(changes.stream().anyMatch(c -> c.getChangeType() == SchemaChange.ChangeType.DROP_INDEX));
        assertTrue(changes.stream().anyMatch(c -> c.getChangeType() == SchemaChange.ChangeType.ADD_INDEX));
    }

    private TableMetadata createTable(String name, List<ColumnMetadata> columns) {
        TableMetadata table = new TableMetadata();
        table.setTableName(name);
        columns.forEach(table::addColumn);
        return table;
    }
    
    private TableMetadata createTableWithIndex(String name, 
                                         List<ColumnMetadata> columns,
                                         List<IndexMetadata> indexes) {
        TableMetadata table = createTable(name, columns);
        indexes.forEach(table::addIndex);
        return table;
    }
    
    private ColumnMetadata createColumn(String name, boolean isPrimaryKey, boolean isNullable) {
        ColumnMetadata column = new ColumnMetadata();
        column.setName(name);
        column.setFieldType(isPrimaryKey ? Long.class : String.class);
        column.setPrimaryKey(isPrimaryKey);
        column.setNullable(isNullable);
        return column;
    }
    
    private IndexMetadata createIndex(String name, String columnList, boolean isUnique) {
        IndexMetadata index = new IndexMetadata();
        index.setName(name);
        index.setColumnList(columnList);
        index.setUnique(isUnique);
        return index;
    }
} 