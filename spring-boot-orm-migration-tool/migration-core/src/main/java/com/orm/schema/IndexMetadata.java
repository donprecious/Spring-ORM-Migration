package com.orm.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents metadata for a database index, including its name, columns, and uniqueness constraint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMetadata {
    private String name;
    private boolean unique;
    private List<String> columnNames;
    private String tableName;
    private String schema;
    private String catalog;
    private String comment;
    private String type;

    /**
     * Creates a new index with the specified properties.
     *
     * @param name The name of the index
     * @param columnList The list of column names
     * @param unique Whether the index enforces uniqueness
     * @return A new IndexMetadata instance
     */
    public static IndexMetadata createNew(String name, String columnList, boolean unique) {
        return IndexMetadata.builder()
                .name(name)
                .columnNames(Arrays.asList(columnList.split(",")))
                .unique(unique)
                .build();
    }

    /**
     * Creates a copy of this index with a new name.
     *
     * @param newName The new index name
     * @return A new IndexMetadata instance
     */
    public static IndexMetadata createCopy(String newName, IndexMetadata original) {
        return IndexMetadata.builder()
                .name(newName)
                .columnNames(new ArrayList<>(original.getColumnNames()))
                .unique(original.isUnique())
                .build();
    }

    /**
     * Checks if this index covers the specified columns.
     *
     * @param columns The columns to check
     * @return true if this index covers all the specified columns
     */
    public boolean coversColumns(List<String> columns) {
        return columnNames.containsAll(columns);
    }

    public static IndexMetadata from(IndexMetadata original) {
        return IndexMetadata.builder()
                .name(original.getName())
                .columnNames(new ArrayList<>(original.getColumnNames()))
                .unique(original.isUnique())
                .build();
    }

    public void setColumnList(String columnList) {
        if (columnList != null) {
            this.columnNames = Arrays.asList(columnList.split(","));
        }
    }

    public String getColumnList() {
        if (columnNames == null || columnNames.isEmpty()) {
            return "";
        }
        return String.join(",", columnNames);
    }

    public List<String> getColumnNames() {
        return columnNames != null ? columnNames : new ArrayList<>();
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addColumn(String columnName) {
        if (columnNames == null) {
            columnNames = new ArrayList<>();
        }
        columnNames.add(columnName);
    }

    public void setColumns(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public void setColumns(String... columnNames) {
        this.columnNames = new ArrayList<>(Arrays.asList(columnNames));
    }

    public List<String> getColumns() {
        return getColumnNames();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getIndexName() {
        return name;
    }

    public void setIndexName(String indexName) {
        this.name = indexName;
    }

    public static class IndexMetadataBuilder {
        private String name;
        private boolean unique;
        private List<String> columnNames = new ArrayList<>();
        private String tableName;
        private String schema;
        private String catalog;
        private String comment;
        private String type;

        IndexMetadataBuilder() {
        }

        public IndexMetadataBuilder name(String name) {
            this.name = name;
            return this;
        }

        public IndexMetadataBuilder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public IndexMetadataBuilder columnNames(List<String> columnNames) {
            this.columnNames = columnNames;
            return this;
        }

        public IndexMetadataBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public IndexMetadataBuilder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public IndexMetadataBuilder catalog(String catalog) {
            this.catalog = catalog;
            return this;
        }

        public IndexMetadataBuilder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public IndexMetadataBuilder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Alias for name(String) to maintain compatibility with both naming conventions.
         * @param indexName the index name
         * @return the builder instance
         */
        public IndexMetadataBuilder indexName(String indexName) {
            return this.name(indexName);
        }

        /**
         * Sets the columns for this index using a List of Strings.
         * @param columns list of column names
         * @return the builder instance
         */
        public IndexMetadataBuilder columns(List<String> columns) {
            this.columnNames = new ArrayList<>(columns);
            return this;
        }

        /**
         * Sets the columns for this index using an ArrayList of Objects.
         * @param columns list of column names as Objects
         * @return the builder instance
         */
        @SuppressWarnings("unchecked")
        public IndexMetadataBuilder columnsFromObjects(ArrayList<Object> columns) {
            this.columnNames = new ArrayList<>();
            for (Object column : columns) {
                this.columnNames.add(column.toString());
            }
            return this;
        }

        public IndexMetadata build() {
            return new IndexMetadata(name, unique, columnNames, tableName, schema, catalog, comment, type);
        }
    }
} 