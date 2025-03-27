package com.orm.schema;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents metadata for a database table, including its columns, relationships, and constraints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableMetadata {
    private String name;
    private String schema;
    private String catalog;
    private String comment;
    @Builder.Default private List<ColumnMetadata> columns = new ArrayList<>();
    @Builder.Default private List<ColumnMetadata> primaryKeyColumns = new ArrayList<>();
    @Builder.Default private List<IndexMetadata> indexes = new ArrayList<>();
    @Builder.Default private List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
    @Builder.Default private List<RelationshipMetadata> relationships = new ArrayList<>();

    public void addColumn(ColumnMetadata column) {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    public void addPrimaryKeyColumn(ColumnMetadata column) {
        if (primaryKeyColumns == null) {
            primaryKeyColumns = new ArrayList<>();
        }
        primaryKeyColumns.add(column);
    }

    public void addIndex(IndexMetadata index) {
        if (indexes == null) {
            indexes = new ArrayList<>();
        }
        indexes.add(index);
    }

    public void addForeignKey(ForeignKeyMetadata foreignKey) {
        if (foreignKeys == null) {
            foreignKeys = new ArrayList<>();
        }
        foreignKeys.add(foreignKey);
    }

    public void addRelationship(RelationshipMetadata relationship) {
        if (relationships == null) {
            relationships = new ArrayList<>();
        }
        relationships.add(relationship);
    }

    public Optional<ColumnMetadata> getColumn(String columnName) {
        if (columns == null) {
            return Optional.empty();
        }
        return columns.stream()
                .filter(c -> c.getName().equals(columnName))
                .findFirst();
    }

    public Optional<IndexMetadata> getIndex(String indexName) {
        if (indexes == null) {
            return Optional.empty();
        }
        return indexes.stream()
                .filter(i -> i.getName().equals(indexName))
                .findFirst();
    }

    public Optional<ForeignKeyMetadata> getForeignKey(String foreignKeyName) {
        if (foreignKeys == null) {
            return Optional.empty();
        }
        return foreignKeys.stream()
                .filter(fk -> fk.getName().equals(foreignKeyName))
                .findFirst();
    }

    public String getTableName() {
        return name;
    }

    public void setTableName(String tableName) {
        this.name = tableName;
    }

    public void setPrimaryKey(ColumnMetadata column) {
        if (primaryKeyColumns == null) {
            primaryKeyColumns = new ArrayList<>();
        }
        primaryKeyColumns.add(column);
    }

    public List<ColumnMetadata> getPrimaryKey() {
        return primaryKeyColumns;
    }

    public List<ColumnMetadata> getPrimaryKeyColumns() {
        return primaryKeyColumns != null ? primaryKeyColumns : new ArrayList<>();
    }

    public void setClassName(String className) {
        // This method is kept for backward compatibility
    }

    public List<ColumnMetadata> getColumns() {
        return columns != null ? columns : new ArrayList<>();
    }

    public List<IndexMetadata> getIndexes() {
        return indexes != null ? indexes : new ArrayList<>();
    }

    public List<ForeignKeyMetadata> getForeignKeys() {
        return foreignKeys != null ? foreignKeys : new ArrayList<>();
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean hasColumn(String columnName) {
        return columns.stream()
                .anyMatch(col -> col.getName().equals(columnName));
    }

    public boolean hasIndex(String indexName) {
        return indexes.stream()
                .anyMatch(idx -> idx.getName().equals(indexName));
    }

    public boolean hasForeignKey(String foreignKeyName) {
        return foreignKeys.stream()
                .anyMatch(fk -> fk.getName().equals(foreignKeyName));
    }

    public List<ColumnMetadata> getForeignKeyColumns() {
        List<ColumnMetadata> fkColumns = new ArrayList<>();
        for (ForeignKeyMetadata fk : foreignKeys) {
            for (String columnName : fk.getColumnNames()) {
                ColumnMetadata column = getColumn(columnName)
                    .orElse(null);
                if (column != null) {
                    fkColumns.add(column);
                }
            }
        }
        return fkColumns;
    }

    public boolean hasForeignKeyConstraint(String columnName) {
        return foreignKeys.stream()
                .anyMatch(fk -> fk.getColumnNames().contains(columnName));
    }

    public List<IndexMetadata> getUniqueIndexes() {
        return indexes.stream()
                .filter(IndexMetadata::isUnique)
                .toList();
    }

    /**
     * Custom builder that provides additional methods for convenience.
     */
    public static class TableMetadataBuilder {
        /**
         * Alias for name(String) to maintain compatibility with both naming conventions.
         * @param tableName the table name
         * @return the builder instance
         */
        public TableMetadataBuilder tableName(String tableName) {
            return this.name(tableName);
        }
    }
}