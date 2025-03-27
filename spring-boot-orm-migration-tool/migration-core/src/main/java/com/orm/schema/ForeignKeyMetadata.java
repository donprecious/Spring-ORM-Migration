package com.orm.schema;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for a foreign key constraint, including referenced table and columns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForeignKeyMetadata {
    @NonNull
    private String constraintName;
    @NonNull
    private String columnName;
    @NonNull
    private String referencedTable;
    @NonNull
    private String referencedColumn;
    private ColumnMetadata column;
    
    @Builder.Default
    private List<String> columnNames = new ArrayList<>();
    @Builder.Default
    private List<String> referencedColumnNames = new ArrayList<>();

    private String tableName;
    private String schema;
    private String catalog;
    private String onUpdate;
    private String onDelete;
    private boolean deferrable;
    private boolean initiallyDeferred;
    private String comment;

    public static ForeignKeyMetadata from(ForeignKeyMetadata original) {
        return ForeignKeyMetadata.builder()
                .constraintName(original.getConstraintName())
                .columnName(original.getColumnName())
                .referencedTable(original.getReferencedTable())
                .referencedColumn(original.getReferencedColumn())
                .column(original.getColumn())
                .columnNames(new ArrayList<>(original.getColumnNames()))
                .referencedColumnNames(new ArrayList<>(original.getReferencedColumnNames()))
                .tableName(original.getTableName())
                .schema(original.getSchema())
                .catalog(original.getCatalog())
                .onUpdate(original.getOnUpdate())
                .onDelete(original.getOnDelete())
                .deferrable(original.isDeferrable())
                .initiallyDeferred(original.isInitiallyDeferred())
                .comment(original.getComment())
                .build();
    }

    public void addColumnMapping(String columnName, String referencedColumnName) {
        columnNames.add(columnName);
        referencedColumnNames.add(referencedColumnName);
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<String> getReferencedColumnNames() {
        return referencedColumnNames;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getReferencedTable() {
        return referencedTable;
    }

    public String getReferencedColumn() {
        return referencedColumn;
    }

    public ColumnMetadata getColumn() {
        return column;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    public void setReferencedColumn(String referencedColumn) {
        this.referencedColumn = referencedColumn;
    }

    public void setColumn(ColumnMetadata column) {
        this.column = column;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
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

    public String getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(String onUpdate) {
        this.onUpdate = onUpdate;
    }

    public String getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(String onDelete) {
        this.onDelete = onDelete;
    }

    public boolean isDeferrable() {
        return deferrable;
    }

    public void setDeferrable(boolean deferrable) {
        this.deferrable = deferrable;
    }

    public boolean isInitiallyDeferred() {
        return initiallyDeferred;
    }

    public void setInitiallyDeferred(boolean initiallyDeferred) {
        this.initiallyDeferred = initiallyDeferred;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getName() {
        return constraintName;
    }

    public void setName(String name) {
        this.constraintName = name;
    }
} 