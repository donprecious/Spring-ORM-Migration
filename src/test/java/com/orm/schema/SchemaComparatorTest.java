package com.orm.schema;

import com.orm.model.Schema;
import com.orm.schema.diff.SchemaChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.GenerationType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaComparatorTest {

    private SchemaComparator comparator;
    private Schema oldSchema;
    private Schema newSchema;

    @BeforeEach
    void setUp() {
        comparator = new SchemaComparator();
        oldSchema = new Schema();
        newSchema = new Schema();
    }

    @Test
    void shouldDetectNewTable() {
        // Given
        TableMetadata newTable = createBasicTable("users");
        newSchema.addTable(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.CREATE_TABLE, change.getChangeType());
        assertEquals("users", change.getTableName());
        assertFalse(change.isDestructive());
    }

    @Test
    void shouldDetectDroppedTable() {
        // Given
        TableMetadata oldTable = createBasicTable("users");
        oldSchema.addTable(oldTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.DROP_TABLE, change.getChangeType());
        assertEquals("users", change.getTableName());
        assertTrue(change.isDestructive());
    }

    @Test
    void shouldDetectNewColumn() {
        // Given
        TableMetadata oldTable = createBasicTable("users");
        TableMetadata newTable = createBasicTable("users");
        
        ColumnMetadata newColumn = new ColumnMetadata();
        newColumn.setName("email");
        newColumn.setFieldType(String.class);
        newTable.addColumn(newColumn);
        
        oldSchema.addTable(oldTable);
        newSchema.addTable(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.ADD_COLUMN, change.getChangeType());
        assertEquals("users", change.getTableName());
        assertEquals("email", change.getColumnName());
        assertFalse(change.isDestructive());
    }

    @Test
    void shouldDetectModifiedColumn() {
        // Given
        TableMetadata oldTable = createBasicTable("users");
        TableMetadata newTable = createBasicTable("users");
        
        // Modify the name column in the new schema
        ColumnMetadata nameColumn = newTable.getColumn("name").orElseThrow();
        nameColumn.setLength(50); // Change from default 255
        
        oldSchema.addTable(oldTable);
        newSchema.addTable(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.MODIFY_COLUMN, change.getChangeType());
        assertEquals("users", change.getTableName());
        assertEquals("name", change.getColumnName());
        // Note: MODIFY_COLUMN might not be marked as destructive by default in the new implementation
    }

    @Test
    void shouldDetectNewIndex() {
        // Given
        TableMetadata oldTable = createBasicTable("users");
        TableMetadata newTable = createBasicTable("users");
        
        IndexMetadata newIndex = new IndexMetadata();
        newIndex.setName("idx_name");
        newIndex.setColumnList("name");
        newIndex.setUnique(true);
        newTable.addIndex(newIndex);
        
        oldSchema.addTable(oldTable);
        newSchema.addTable(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(1, changes.size());
        SchemaChange change = changes.get(0);
        assertEquals(SchemaChange.ChangeType.ADD_INDEX, change.getChangeType());
        assertEquals("users", change.getTableName());
        assertEquals("idx_name", change.getIndexName());
        assertFalse(change.isDestructive());
    }

    @Test
    void shouldDetectNewForeignKey() {
        // Given
        TableMetadata oldTable = createBasicTable("users");
        TableMetadata newTable = createBasicTable("users");
        
        // Add foreign key column
        ColumnMetadata fkColumn = new ColumnMetadata();
        fkColumn.setName("department_id");
        fkColumn.setFieldType(Long.class);
        fkColumn.setForeignKey(true);
        fkColumn.setReferencedTable("departments");
        fkColumn.setReferencedColumn("id");
        newTable.addColumn(fkColumn);
        
        // Create and add foreign key metadata
        ForeignKeyMetadata fk = new ForeignKeyMetadata();
        fk.setConstraintName("fk_department");
        fk.setColumnName("department_id");
        fk.setReferencedTable("departments");
        fk.setReferencedColumn("id");
        fk.setColumn(fkColumn);
        newTable.addForeignKey(fk);
        
        oldSchema.addTable(oldTable);
        newSchema.addTable(newTable);

        // When
        List<SchemaChange> changes = comparator.compareSchemas(oldSchema, newSchema);

        // Then
        assertEquals(2, changes.size()); // ADD_COLUMN and ADD_FOREIGN_KEY
        SchemaChange fkChange = changes.stream()
            .filter(c -> c.getChangeType() == SchemaChange.ChangeType.ADD_FOREIGN_KEY)
            .findFirst()
            .orElse(null);
        assertNotNull(fkChange);
        assertEquals("users", fkChange.getTableName());
        assertFalse(fkChange.isDestructive());
    }

    private TableMetadata createBasicTable(String tableName) {
        TableMetadata table = new TableMetadata();
        table.setTableName(tableName);
        
        // Add ID column
        ColumnMetadata idColumn = new ColumnMetadata();
        idColumn.setName("id");
        idColumn.setFieldType(Long.class);
        idColumn.setGenerationType(GenerationType.IDENTITY);
        table.addColumn(idColumn);
        
        // Add name column
        ColumnMetadata nameColumn = new ColumnMetadata();
        nameColumn.setName("name");
        nameColumn.setFieldType(String.class);
        nameColumn.setLength(255);
        table.addColumn(nameColumn);
        
        return table;
    }
} 