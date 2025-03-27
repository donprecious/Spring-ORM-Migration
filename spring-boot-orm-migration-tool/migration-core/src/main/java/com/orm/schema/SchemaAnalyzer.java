package com.orm.schema;

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import java.lang.reflect.Field;
import java.util.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.orm.sql.SqlDialect;
import com.orm.schema.diff.SchemaChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Analyzes JPA entities to extract database schema metadata.
 * This class scans entity classes and their annotations to build
 * a complete picture of the database schema.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaAnalyzer {
    
    private final String basePackage;
    private final MetadataExtractor metadataExtractor;
    private final Map<String, TableMetadata> currentSchema = new HashMap<>();
    private SqlDialect sqlDialect;

    @Autowired
    public SchemaAnalyzer(MetadataExtractor metadataExtractor) {
        this.basePackage = "com.orm"; // Default package to scan
        this.metadataExtractor = metadataExtractor;
        this.sqlDialect = null; // Will be set later via setter
    }
    
    @Autowired(required = false)
    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    /**
     * Analyzes a set of entity classes to extract table metadata.
     *
     * @param entityClasses The classes to analyze
     * @return List of table metadata objects
     */
    public List<TableMetadata> analyzeEntities(Set<Class<?>> entityClasses) {
        List<TableMetadata> tables = new ArrayList<>();
        
        for (Class<?> entityClass : entityClasses) {
            if (isEntity(entityClass)) {
                tables.add(analyzeEntity(entityClass));
            }
        }
        
        return tables;
    }

    /**
     * Analyzes a single entity class to extract table metadata.
     *
     * @param entityClass The class to analyze
     * @return Table metadata object
     */
    public TableMetadata analyzeEntity(Class<?> entityClass) {
        if (!isEntity(entityClass)) {
            throw new IllegalArgumentException("Class is not a JPA entity: " + entityClass.getName());
        }

        TableMetadata table = new TableMetadata();
        table.setTableName(extractTableName(entityClass));
        table.setClassName(entityClass.getName());
        
        List<ColumnMetadata> columns = new ArrayList<>();
        Map<String, ColumnMetadata> columnMap = new HashMap<>();
        
        // Analyze fields including those from superclasses
        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (isColumnField(field)) {
                    ColumnMetadata column = analyzeField(field);
                    column.setTableName(table.getTableName());
                    columns.add(column);
                    columnMap.put(column.getName(), column);
                    
                    if (isPrimaryKey(field)) {
                        table.setPrimaryKey(column);
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        table.setColumns(columns);
        
        // Process relationships after all columns are analyzed
        processRelationships(entityClass, table, columnMap);
        
        // Process indexes
        processIndexes(entityClass, table);
        
        return table;
    }

    private boolean isEntity(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entity.class);
    }

    private String extractTableName(Class<?> entityClass) {
        Table tableAnn = entityClass.getAnnotation(Table.class);
        if (tableAnn != null && !tableAnn.name().isEmpty()) {
            return tableAnn.name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    private boolean isColumnField(Field field) {
        // Exclude static, transient, and @Transient fields
        if (field.isAnnotationPresent(Transient.class)) {
            return false;
        }
        int modifiers = field.getModifiers();
        return !java.lang.reflect.Modifier.isStatic(modifiers) &&
               !java.lang.reflect.Modifier.isTransient(modifiers);
    }

    private boolean isPrimaryKey(Field field) {
        return field.isAnnotationPresent(Id.class);
    }

    private ColumnMetadata analyzeField(Field field) {
        ColumnMetadata column = new ColumnMetadata();
        
        // Basic column info
        column.setName(extractColumnName(field));
        column.setFieldName(field.getName());
        column.setFieldType(field.getType());
        
        // Process @Column annotation
        if (field.isAnnotationPresent(Column.class)) {
            Column columnAnn = field.getAnnotation(Column.class);
            column.setNullable(columnAnn.nullable());
            column.setUnique(columnAnn.unique());
            column.setLength(columnAnn.length());
            column.setPrecision(columnAnn.precision());
            column.setScale(columnAnn.scale());
            if (!columnAnn.columnDefinition().isEmpty()) {
                column.setColumnDefinition(columnAnn.columnDefinition());
            }
        }
        
        // Process @GeneratedValue
        if (field.isAnnotationPresent(GeneratedValue.class)) {
            GeneratedValue genAnn = field.getAnnotation(GeneratedValue.class);
            column.setGenerationType(genAnn.strategy());
            if (genAnn.generator() != null && !genAnn.generator().isEmpty()) {
                column.setSequenceName(genAnn.generator());
            }
        }
        
        // Process primary key field
        if (field.isAnnotationPresent(Id.class)) {
            column.setPrimaryKey(true);
            column.setNullable(false);
        }
        
        return column;
    }

    private String extractColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column columnAnn = field.getAnnotation(Column.class);
            if (!columnAnn.name().isEmpty()) {
                return columnAnn.name();
            }
        }
        return field.getName().toLowerCase();
    }

    private void processRelationships(Class<?> entityClass, TableMetadata table, 
                                    Map<String, ColumnMetadata> columnMap) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(OneToMany.class) ||
                field.isAnnotationPresent(ManyToOne.class) ||
                field.isAnnotationPresent(OneToOne.class) ||
                field.isAnnotationPresent(ManyToMany.class)) {
                
                RelationshipMetadata relationship = new RelationshipMetadata();
                relationship.setSourceTable(table.getTableName());
                relationship.setSourceField(field.getName());
                
                // Set the relationship type based on the annotation
                if (field.isAnnotationPresent(OneToMany.class)) {
                    relationship.setType(RelationshipMetadata.RelationshipType.ONE_TO_MANY);
                } else if (field.isAnnotationPresent(ManyToOne.class)) {
                    relationship.setType(RelationshipMetadata.RelationshipType.MANY_TO_ONE);
                } else if (field.isAnnotationPresent(OneToOne.class)) {
                    relationship.setType(RelationshipMetadata.RelationshipType.ONE_TO_ONE);
                } else if (field.isAnnotationPresent(ManyToMany.class)) {
                    relationship.setType(RelationshipMetadata.RelationshipType.MANY_TO_MANY);
                }
                
                if (field.isAnnotationPresent(JoinColumn.class)) {
                    JoinColumn joinAnn = field.getAnnotation(JoinColumn.class);
                    relationship.setForeignKeyName(joinAnn.name());
                    relationship.setNullable(joinAnn.nullable());
                    
                    // Update the corresponding column metadata
                    ColumnMetadata column = columnMap.get(joinAnn.name());
                    if (column != null) {
                        column.setForeignKey(true);
                        column.setReferencedTable(extractReferencedTable(field));
                        column.setReferencedColumn(joinAnn.referencedColumnName());
                    }
                }
                
                table.addRelationship(relationship);
            }
        }
    }

    private String extractReferencedTable(Field field) {
        Class<?> targetEntity = null;
        
        if (field.isAnnotationPresent(OneToMany.class)) {
            targetEntity = field.getAnnotation(OneToMany.class).targetEntity();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            targetEntity = field.getAnnotation(ManyToOne.class).targetEntity();
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            targetEntity = field.getAnnotation(OneToOne.class).targetEntity();
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            targetEntity = field.getAnnotation(ManyToMany.class).targetEntity();
        }
        
        if (targetEntity != null && targetEntity != void.class) {
            Table tableAnn = targetEntity.getAnnotation(Table.class);
            if (tableAnn != null && !tableAnn.name().isEmpty()) {
                return tableAnn.name();
            }
            return targetEntity.getSimpleName().toLowerCase();
        }
        
        return null;
    }

    private void processIndexes(Class<?> entityClass, TableMetadata table) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table tableAnn = entityClass.getAnnotation(Table.class);
            
            // Process @Index annotations
            for (Index indexAnn : tableAnn.indexes()) {
                IndexMetadata index = new IndexMetadata();
                index.setName(indexAnn.name());
                index.setColumnList(String.join(",", indexAnn.columnList()));
                index.setUnique(indexAnn.unique());
                table.addIndex(index);
            }
            
            // Process @UniqueConstraint annotations
            for (UniqueConstraint uniqueAnn : tableAnn.uniqueConstraints()) {
                IndexMetadata index = new IndexMetadata();
                index.setName(uniqueAnn.name());
                index.setColumnList(String.join(",", uniqueAnn.columnNames()));
                index.setUnique(true);
                table.addIndex(index);
            }
        }
    }

    public TableMetadata getTableMetadata(String tableName) {
        return currentSchema.get(tableName);
    }

    public boolean hasTable(String tableName) {
        return currentSchema.containsKey(tableName);
    }

    public Schema analyzeEntities(List<Class<?>> entities) {
        Schema schema = new Schema();
        for (Class<?> entity : entities) {
            TableMetadata table = analyzeEntity(entity);
            schema.addTable(table);
        }
        return schema;
    }

    public Schema analyzeDatabase(DataSource dataSource) {
        Schema schema = new Schema();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schemaName = conn.getSchema();

            // Get all tables
            try (ResultSet tables = metaData.getTables(catalog, schemaName, null, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    TableMetadata table = analyzeTable(metaData, catalog, schemaName, tableName);
                    schema.addTable(table);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to analyze database schema", e);
        }
        return schema;
    }

    private TableMetadata analyzeTable(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        TableMetadata table = TableMetadata.builder()
                .tableName(tableName)
                .columns(new ArrayList<>())
                .indexes(new ArrayList<>())
                .foreignKeys(new ArrayList<>())
                .build();

        // Get columns
        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, null)) {
            while (columns.next()) {
                ColumnMetadata column = analyzeColumn(columns);
                table.getColumns().add(column);
            }
        }

        // Get primary keys
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (primaryKeys.next()) {
                String columnName = primaryKeys.getString("COLUMN_NAME");
                table.getColumns().stream()
                        .filter(col -> col.getColumnName().equals(columnName))
                        .findFirst()
                        .ifPresent(col -> col.setPrimaryKey(true));
            }
        }

        // Get indexes
        try (ResultSet indexes = metaData.getIndexInfo(catalog, schema, tableName, false, false)) {
            Map<String, IndexMetadata> indexMap = new HashMap<>();
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName == null) {
                    continue;
                }

                final String idxName = indexName;
                final boolean isUnique;
                try {
                    isUnique = !indexes.getBoolean("NON_UNIQUE");
                } catch (SQLException e) {
                    continue; // Skip this index if we can't determine uniqueness
                }

                IndexMetadata index = indexMap.computeIfAbsent(idxName, k -> IndexMetadata.builder()
                        .indexName(k)
                        .unique(isUnique)
                        .columns(new ArrayList<>())
                        .build());

                String columnName = indexes.getString("COLUMN_NAME");
                index.getColumns().add(columnName);
            }
            table.getIndexes().addAll(indexMap.values());
        }

        // Get foreign keys
        try (ResultSet foreignKeys = metaData.getImportedKeys(catalog, schema, tableName)) {
            Map<String, ForeignKeyMetadata> fkMap = new HashMap<>();
            while (foreignKeys.next()) {
                String fkName = foreignKeys.getString("FK_NAME");
                if (fkName == null) continue;
                
                final String columnName;
                final String refTable;
                final String refColumn;
                
                try {
                    columnName = foreignKeys.getString("FKCOLUMN_NAME");
                    refTable = foreignKeys.getString("PKTABLE_NAME");
                    refColumn = foreignKeys.getString("PKCOLUMN_NAME");
                } catch (SQLException e) {
                    continue; // Skip this FK if we can't get the required data
                }
                
                ForeignKeyMetadata fk = fkMap.computeIfAbsent(fkName, k -> ForeignKeyMetadata.builder()
                        .constraintName(k)
                        .columnName(columnName)
                        .referencedTable(refTable)
                        .referencedColumn(refColumn)
                        .build());
                
                table.getForeignKeys().add(fk);
            }
        }

        return table;
    }

    private ColumnMetadata analyzeColumn(ResultSet rs) throws SQLException {
        Integer columnSize = rs.getInt("COLUMN_SIZE");
        Integer decimalDigits = rs.getInt("DECIMAL_DIGITS");
        if (rs.wasNull()) {
            columnSize = null;
        }
        if (rs.wasNull()) {
            decimalDigits = null;
        }
        
        return ColumnMetadata.builder()
                .columnName(rs.getString("COLUMN_NAME"))
                .type(rs.getString("TYPE_NAME"))
                .nullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                .defaultValue(rs.getString("COLUMN_DEF"))
                .length(columnSize)
                .precision(decimalDigits)
                .build();
    }

    private IndexMetadata analyzeIndex(Index indexAnn) {
        return IndexMetadata.builder()
                .indexName(indexAnn.name())
                .unique(indexAnn.unique())
                .columns(Arrays.asList(indexAnn.columnList().split(",")))
                .build();
    }

    public List<SchemaChange> compareSchemas(Schema sourceSchema, Schema targetSchema) {
        List<SchemaChange> changes = new ArrayList<>();
        
        // Check for tables that exist in source but not in target (dropped tables)
        for (TableMetadata sourceTable : sourceSchema.getTables()) {
            if (!targetSchema.hasTable(sourceTable.getTableName())) {
                SchemaChange change = SchemaChange.builder()
                    .changeType(SchemaChange.ChangeType.DROP_TABLE)
                    .table(sourceTable)
                    .build();
                changes.add(change);
            }
        }
        
        // Check for tables that exist in target but not in source (new tables)
        for (TableMetadata targetTable : targetSchema.getTables()) {
            if (!sourceSchema.hasTable(targetTable.getTableName())) {
                SchemaChange change = SchemaChange.builder()
                    .changeType(SchemaChange.ChangeType.CREATE_TABLE)
                    .table(targetTable)
                    .build();
                changes.add(change);
            }
        }
        
        // Compare tables that exist in both schemas
        for (TableMetadata sourceTable : sourceSchema.getTables()) {
            String tableName = sourceTable.getTableName();
            if (targetSchema.hasTable(tableName)) {
                TableMetadata targetTable = targetSchema.getTable(tableName).get();
                compareColumns(sourceTable, targetTable, changes);
                compareIndexes(sourceTable, targetTable, changes);
                compareForeignKeys(sourceTable, targetTable, changes);
            }
        }
        
        return changes;
    }

    private void compareColumns(TableMetadata sourceTable, TableMetadata targetTable, List<SchemaChange> changes) {
        // Check for columns that exist in source but not in target (dropped columns)
        for (ColumnMetadata sourceColumn : sourceTable.getColumns()) {
            if (!targetTable.hasColumn(sourceColumn.getName())) {
                SchemaChange change = SchemaChange.builder()
                    .changeType(SchemaChange.ChangeType.DROP_COLUMN)
                    .table(targetTable)
                    .column(sourceColumn)
                    .build();
                changes.add(change);
            }
        }
        
        // Check for columns that exist in target but not in source (new columns)
        for (ColumnMetadata targetColumn : targetTable.getColumns()) {
            if (!sourceTable.hasColumn(targetColumn.getName())) {
                SchemaChange change = SchemaChange.builder()
                    .changeType(SchemaChange.ChangeType.ADD_COLUMN)
                    .table(targetTable)
                    .column(targetColumn)
                    .build();
                changes.add(change);
            }
        }
        
        // Compare columns that exist in both tables
        for (ColumnMetadata sourceColumn : sourceTable.getColumns()) {
            String columnName = sourceColumn.getName();
            if (targetTable.hasColumn(columnName)) {
                targetTable.getColumn(columnName).ifPresent(targetColumn -> {
                    if (!columnsEqual(sourceColumn, targetColumn)) {
                        SchemaChange change = SchemaChange.builder()
                            .changeType(SchemaChange.ChangeType.MODIFY_COLUMN)
                            .table(targetTable)
                            .column(sourceColumn)
                            .newColumn(targetColumn)
                            .build();
                        changes.add(change);
                    }
                });
            }
        }
    }

    private void compareIndexes(TableMetadata sourceTable, TableMetadata targetTable, List<SchemaChange> changes) {
        for (IndexMetadata sourceIndex : sourceTable.getIndexes()) {
            IndexMetadata targetIndex = targetTable.getIndex(sourceIndex.getIndexName())
                    .orElse(null);

            if (targetIndex == null) {
                // Index was dropped
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.DROP_INDEX)
                        .table(sourceTable)
                        .index(sourceIndex)
                        .build());
            } else if (!indexesEqual(sourceIndex, targetIndex)) {
                // Index was modified - drop and recreate
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.DROP_INDEX)
                        .table(sourceTable)
                        .index(sourceIndex)
                        .build());
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.ADD_INDEX)
                        .table(targetTable)
                        .index(targetIndex)
                        .build());
            }
        }

        // Check for new indexes
        for (IndexMetadata targetIndex : targetTable.getIndexes()) {
            if (!sourceTable.hasIndex(targetIndex.getIndexName())) {
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.ADD_INDEX)
                        .table(targetTable)
                        .index(targetIndex)
                        .build());
            }
        }
    }

    private void compareForeignKeys(TableMetadata sourceTable, TableMetadata targetTable, List<SchemaChange> changes) {
        for (ForeignKeyMetadata sourceFk : sourceTable.getForeignKeys()) {
            ForeignKeyMetadata targetFk = targetTable.getForeignKey(sourceFk.getConstraintName())
                    .orElse(null);

            if (targetFk == null) {
                // Foreign key was dropped
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.DROP_FOREIGN_KEY)
                        .table(sourceTable)
                        .foreignKey(sourceFk)
                        .build());
            } else if (!foreignKeysEqual(sourceFk, targetFk)) {
                // Foreign key was modified - drop and recreate
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.DROP_FOREIGN_KEY)
                        .table(sourceTable)
                        .foreignKey(sourceFk)
                        .build());
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.ADD_FOREIGN_KEY)
                        .table(targetTable)
                        .foreignKey(targetFk)
                        .build());
            }
        }

        // Check for new foreign keys
        for (ForeignKeyMetadata targetFk : targetTable.getForeignKeys()) {
            if (!sourceTable.hasForeignKey(targetFk.getConstraintName())) {
                changes.add(SchemaChange.builder()
                        .changeType(SchemaChange.ChangeType.ADD_FOREIGN_KEY)
                        .table(targetTable)
                        .foreignKey(targetFk)
                        .build());
            }
        }
    }

    private boolean columnsEqual(ColumnMetadata source, ColumnMetadata target) {
        return source.getType().equals(target.getType()) &&
               source.isNullable() == target.isNullable() &&
               source.isUnique() == target.isUnique() &&
               source.isPrimaryKey() == target.isPrimaryKey() &&
               source.isAutoIncrement() == target.isAutoIncrement() &&
               Objects.equals(source.getLength(), target.getLength()) &&
               Objects.equals(source.getPrecision(), target.getPrecision()) &&
               Objects.equals(source.getScale(), target.getScale()) &&
               Objects.equals(source.getDefaultValue(), target.getDefaultValue());
    }

    private boolean indexesEqual(IndexMetadata source, IndexMetadata target) {
        return source.isUnique() == target.isUnique() &&
               source.getColumns().equals(target.getColumns());
    }

    private boolean foreignKeysEqual(ForeignKeyMetadata source, ForeignKeyMetadata target) {
        return Objects.equals(source.getColumnName(), target.getColumnName()) &&
               Objects.equals(source.getReferencedTable(), target.getReferencedTable()) &&
               Objects.equals(source.getReferencedColumn(), target.getReferencedColumn()) &&
               Objects.equals(source.getOnUpdate(), target.getOnUpdate()) &&
               Objects.equals(source.getOnDelete(), target.getOnDelete());
    }
}

class SchemaAnalysisException extends RuntimeException {
    public SchemaAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
} 