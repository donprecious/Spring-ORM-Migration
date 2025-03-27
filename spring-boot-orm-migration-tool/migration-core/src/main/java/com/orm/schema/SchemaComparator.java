package com.orm.schema;

import com.orm.model.Schema;
import com.orm.schema.diff.SchemaChange;
import com.orm.schema.diff.SchemaChange.SchemaChangeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Compares two database schemas and identifies the differences between them.
 * This is used to generate migration scripts for schema updates.
 */
@Slf4j
@Component
public class SchemaComparator {

    /**
     * Compares two schemas and returns a list of changes.
     *
     * @param currentSchema The original schema
     * @param targetSchema The new schema
     * @return List of schema changes
     */
    public List<SchemaChange> compareSchemas(Schema currentSchema, Schema targetSchema) {
        List<SchemaChange> changes = new ArrayList<>();

        // Find dropped tables
        for (TableMetadata currentTable : currentSchema.getTables()) {
            if (!targetSchema.hasTable(currentTable.getTableName())) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.DROP_TABLE)
                        .changeType(SchemaChange.ChangeType.DROP_TABLE)
                        .table(currentTable)
                        .objectName(currentTable.getTableName())
                        .objectType("TABLE")
                        .destructive(true)
                        .dataLoss(true)
                        .build());
            }
        }

        // Find new and modified tables
        for (TableMetadata targetTable : targetSchema.getTables()) {
            Optional<TableMetadata> currentTableOpt = currentSchema.getTable(targetTable.getTableName());
            
            if (currentTableOpt.isEmpty()) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.CREATE_TABLE)
                        .changeType(SchemaChange.ChangeType.CREATE_TABLE)
                        .table(targetTable)
                        .objectName(targetTable.getTableName())
                        .objectType("TABLE")
                        .build());
            } else {
                changes.addAll(compareColumns(currentTableOpt.get(), targetTable));
                changes.addAll(compareIndexes(currentTableOpt.get(), targetTable));
                changes.addAll(compareForeignKeys(currentTableOpt.get(), targetTable));
            }
        }

        return changes;
    }

    private List<SchemaChange> compareColumns(TableMetadata currentTable, TableMetadata targetTable) {
        List<SchemaChange> changes = new ArrayList<>();

        // Find dropped columns
        for (ColumnMetadata currentColumn : currentTable.getColumns()) {
            if (!targetTable.hasColumn(currentColumn.getName())) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.DROP_COLUMN)
                        .changeType(SchemaChange.ChangeType.DROP_COLUMN)
                        .table(currentTable)
                        .column(currentColumn)
                        .objectName(currentColumn.getName())
                        .objectType("COLUMN")
                        .destructive(true)
                        .dataLoss(true)
                        .build());
            }
        }

        // Find new and modified columns
        for (ColumnMetadata targetColumn : targetTable.getColumns()) {
            Optional<ColumnMetadata> currentColumnOpt = currentTable.getColumn(targetColumn.getName());
            
            if (currentColumnOpt.isEmpty()) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.ADD_COLUMN)
                        .changeType(SchemaChange.ChangeType.ADD_COLUMN)
                        .table(targetTable)
                        .column(targetColumn)
                        .objectName(targetColumn.getName())
                        .objectType("COLUMN")
                        .build());
            } else {
                ColumnMetadata currentColumn = currentColumnOpt.get();
                if (!columnsEqual(currentColumn, targetColumn)) {
                    changes.add(SchemaChange.builder()
                            .type(SchemaChangeType.MODIFY_COLUMN)
                            .changeType(SchemaChange.ChangeType.MODIFY_COLUMN)
                            .table(targetTable)
                            .column(currentColumn)
                            .newColumn(targetColumn)
                            .objectName(currentColumn.getName())
                            .objectType("COLUMN")
                            .build());
                }
            }
        }

        return changes;
    }

    private List<SchemaChange> compareIndexes(TableMetadata currentTable, TableMetadata targetTable) {
        List<SchemaChange> changes = new ArrayList<>();

        // Find dropped indexes
        for (IndexMetadata currentIndex : currentTable.getIndexes()) {
            if (!targetTable.hasIndex(currentIndex.getName())) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.DROP_INDEX)
                        .changeType(SchemaChange.ChangeType.DROP_INDEX)
                        .table(currentTable)
                        .index(currentIndex)
                        .objectName(currentIndex.getName())
                        .objectType("INDEX")
                        .build());
            }
        }

        // Find new indexes
        for (IndexMetadata targetIndex : targetTable.getIndexes()) {
            if (!currentTable.hasIndex(targetIndex.getName())) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.CREATE_INDEX)
                        .changeType(SchemaChange.ChangeType.ADD_INDEX)
                        .table(targetTable)
                        .index(targetIndex)
                        .objectName(targetIndex.getName())
                        .objectType("INDEX")
                        .build());
            }
        }

        return changes;
    }

    private List<SchemaChange> compareForeignKeys(TableMetadata currentTable, TableMetadata targetTable) {
        List<SchemaChange> changes = new ArrayList<>();

        // Find dropped foreign keys
        for (ForeignKeyMetadata currentFk : currentTable.getForeignKeys()) {
            if (!targetTable.hasForeignKey(currentFk.getName())) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.DROP_FOREIGN_KEY)
                        .changeType(SchemaChange.ChangeType.DROP_FOREIGN_KEY)
                        .table(currentTable)
                        .foreignKey(currentFk)
                        .objectName(currentFk.getName())
                        .objectType("FOREIGN_KEY")
                        .build());
            }
        }

        // Find new foreign keys
        for (ForeignKeyMetadata targetFk : targetTable.getForeignKeys()) {
            if (!currentTable.hasForeignKey(targetFk.getName())) {
                changes.add(SchemaChange.builder()
                        .type(SchemaChangeType.ADD_FOREIGN_KEY)
                        .changeType(SchemaChange.ChangeType.ADD_FOREIGN_KEY)
                        .table(targetTable)
                        .foreignKey(targetFk)
                        .objectName(targetFk.getName())
                        .objectType("FOREIGN_KEY")
                        .build());
            }
        }

        return changes;
    }

    private boolean columnsEqual(ColumnMetadata col1, ColumnMetadata col2) {
        if (col1 == null || col2 == null) {
            return false;
        }
        
        return col1.getName().equals(col2.getName()) &&
               java.util.Objects.equals(col1.getFieldType(), col2.getFieldType()) &&
               col1.isNullable() == col2.isNullable() &&
               col1.getLength() == col2.getLength() &&
               col1.getPrecision() == col2.getPrecision() &&
               col1.getScale() == col2.getScale() &&
               col1.isUnique() == col2.isUnique() &&
               col1.isPrimaryKey() == col2.isPrimaryKey() &&
               col1.isAutoIncrement() == col2.isAutoIncrement() &&
               java.util.Objects.equals(col1.getColumnDefinition(), col2.getColumnDefinition());
    }
} 