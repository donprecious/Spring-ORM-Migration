package com.orm.sql;

import com.orm.schema.ColumnMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.TableMetadata;
import com.orm.schema.ForeignKeyMetadata;
import jakarta.persistence.GenerationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SqlServerDialect implements SqlDialect {

    @Override
    public String createTableSql(TableMetadata table) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(quoteIdentifier(table.getTableName())).append(" (\n");

        // Add columns
        for (ColumnMetadata column : table.getColumns()) {
            sql.append("  ").append(quoteIdentifier(column.getName()))
                .append(" ").append(getColumnDefinition(column)).append(",\n");
        }

        // Add primary key
        if (!table.getPrimaryKeyColumns().isEmpty()) {
            sql.append("  PRIMARY KEY (");
            
            sql.append(table.getPrimaryKeyColumns().stream()
                .map(ColumnMetadata::getName)
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", ")));
                
            sql.append("),\n");
        }

        // Remove trailing comma and newline
        if (sql.toString().endsWith(",\n")) {
            sql.setLength(sql.length() - 2);
            sql.append("\n");
        }

        sql.append(")");
        
        return sql.toString();
    }

    @Override
    public String dropTableSql(TableMetadata table) {
        return "IF OBJECT_ID('" + quoteIdentifier(table.getTableName()) + "', 'U') IS NOT NULL DROP TABLE " + quoteIdentifier(table.getTableName());
    }

    @Override
    public String renameTableSql(String oldTableName, String newTableName) {
        return "EXEC sp_rename '" + quoteIdentifier(oldTableName) + "', '" + quoteIdentifier(newTableName) + "'";
    }

    @Override
    public String addColumnSql(TableMetadata table, ColumnMetadata column) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) + 
               " ADD " + quoteIdentifier(column.getName()) + " " + getColumnDefinition(column);
    }

    @Override
    public String dropColumnSql(TableMetadata table, String columnName) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) + 
               " DROP COLUMN " + quoteIdentifier(columnName);
    }

    @Override
    public String modifyColumnSql(TableMetadata table, ColumnMetadata column) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) + 
               " ALTER COLUMN " + quoteIdentifier(column.getName()) + " " + getColumnDefinition(column);
    }

    @Override
    public String renameColumnSql(TableMetadata table, String oldColumnName, String newColumnName) {
        return "EXEC sp_rename '" + quoteIdentifier(table.getTableName()) + "." + 
               quoteIdentifier(oldColumnName) + "', '" + newColumnName + "', 'COLUMN'";
    }

    @Override
    public String createIndexSql(TableMetadata table, IndexMetadata index) {
        StringBuilder sql = new StringBuilder("CREATE ");
        if (index.isUnique()) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ").append(quoteIdentifier(index.getName()))
                .append(" ON ").append(quoteIdentifier(table.getTableName()))
                .append(" (")
                .append(index.getColumnNames().stream()
                        .map(this::quoteIdentifier)
                        .collect(Collectors.joining(", ")))
                .append(")");
        return sql.toString();
    }

    @Override
    public String dropIndexSql(TableMetadata table, String indexName) {
        return String.format("DROP INDEX %s ON %s", 
            quoteIdentifier(indexName), quoteIdentifier(table.getTableName()));
    }

    @Override
    public String addForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) +
                " ADD CONSTRAINT " + quoteIdentifier(foreignKey.getConstraintName()) +
                " FOREIGN KEY (" + quoteIdentifier(foreignKey.getColumnName()) + ")" +
                " REFERENCES " + quoteIdentifier(foreignKey.getReferencedTable()) +
                "(" + quoteIdentifier(foreignKey.getReferencedColumn()) + ")";
    }

    @Override
    public String dropForeignKeySql(TableMetadata table, String foreignKeyName) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) + 
               " DROP CONSTRAINT " + quoteIdentifier(foreignKeyName);
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public String getPrimaryKeyColumns(TableMetadata table) {
        if (table.getPrimaryKeyColumns() == null || table.getPrimaryKeyColumns().isEmpty()) {
            return "";
        }
        
        return table.getPrimaryKeyColumns().stream()
                .map(ColumnMetadata::getName)
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String createTable(TableMetadata table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(quoteIdentifier(table.getTableName())).append(" (\n");
        
        List<String> columnDefinitions = new ArrayList<>();
        for (ColumnMetadata column : table.getColumns()) {
            columnDefinitions.add("    " + quoteIdentifier(column.getName()) + " " + getColumnDefinition(column));
        }
        
        if (!table.getPrimaryKeyColumns().isEmpty()) {
            List<String> pkColumns = table.getPrimaryKeyColumns().stream()
                .map(ColumnMetadata::getName)
                .map(this::quoteIdentifier)
                .collect(Collectors.toList());
            columnDefinitions.add("    PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
        }
        
        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");
        
        return sql.toString();
    }

    @Override
    public String dropTable(String tableName) {
        return "IF OBJECT_ID('" + tableName + "', 'U') IS NOT NULL DROP TABLE " + quoteIdentifier(tableName);
    }

    @Override
    public String renameTable(String oldName, String newName) {
        return "EXEC sp_rename '" + quoteIdentifier(oldName) + "', '" + quoteIdentifier(newName) + "'";
    }

    @Override
    public String addColumn(String tableName, ColumnMetadata column) {
        return "ALTER TABLE " + quoteIdentifier(tableName) + " ADD " + 
            quoteIdentifier(column.getName()) + " " + getColumnDefinition(column);
    }

    @Override
    public String dropColumn(String tableName, String columnName) {
        return "ALTER TABLE " + quoteIdentifier(tableName) + " DROP COLUMN " + quoteIdentifier(columnName);
    }

    @Override
    public String renameColumn(String tableName, String oldName, String newName) {
        return "EXEC sp_rename '" + quoteIdentifier(tableName) + "." + quoteIdentifier(oldName) + "', '" + newName + "', 'COLUMN'";
    }

    @Override
    public String modifyColumn(String tableName, ColumnMetadata column) {
        return "ALTER TABLE " + quoteIdentifier(tableName) + " ALTER COLUMN " + 
            quoteIdentifier(column.getName()) + " " + getColumnDefinition(column);
    }

    @Override
    public String createIndex(String tableName, IndexMetadata index) {
        StringBuilder sql = new StringBuilder("CREATE ");
        if (index.isUnique()) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ").append(quoteIdentifier(index.getName()))
           .append(" ON ").append(quoteIdentifier(tableName))
           .append(" (").append(index.getColumnNames().stream()
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", "))).append(")");
        return sql.toString();
    }

    @Override
    public String dropIndex(String tableName, String indexName) {
        return "DROP INDEX " + quoteIdentifier(indexName) + " ON " + quoteIdentifier(tableName);
    }

    @Override
    public String renameIndex(String tableName, String oldName, String newName) {
        return "EXEC sp_rename '" + quoteIdentifier(tableName) + "." + quoteIdentifier(oldName) + "', '" + 
               quoteIdentifier(newName) + "', 'INDEX'";
    }

    @Override
    public String addForeignKey(String tableName, String constraintName, String columnName,
                              String referencedTable, String referencedColumn) {
        return "ALTER TABLE " + quoteIdentifier(tableName) +
               " ADD CONSTRAINT " + quoteIdentifier(constraintName) +
               " FOREIGN KEY (" + quoteIdentifier(columnName) + ")" +
               " REFERENCES " + quoteIdentifier(referencedTable) + "(" + quoteIdentifier(referencedColumn) + ")";
    }

    @Override
    public String dropForeignKey(String tableName, String constraintName) {
        return "ALTER TABLE " + quoteIdentifier(tableName) + " DROP CONSTRAINT " + quoteIdentifier(constraintName);
    }

    @Override
    public String mapJavaTypeToSqlType(Class<?> javaType, ColumnMetadata column) {
        if (javaType == String.class) {
            Integer size = column.getSize();
            int length = (size != null && size > 0) ? size : 255;
            return length > 8000 ? "NVARCHAR(MAX)" : "NVARCHAR(" + length + ")";
        } else if (javaType == Integer.class || javaType == int.class) {
            return "INT";
        } else if (javaType == Long.class || javaType == long.class) {
            return "BIGINT";
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return "BIT";
        } else if (javaType == BigDecimal.class) {
            Integer precision = column.getPrecision();
            Integer scale = column.getScale();
            int p = (precision != null && precision > 0) ? precision : 19;
            int s = (scale != null && scale >= 0) ? scale : 4;
            return String.format("DECIMAL(%d,%d)", p, s);
        } else if (javaType == LocalDateTime.class) {
            return "DATETIME2";
        } else if (javaType == LocalDate.class) {
            return "DATE";
        } else if (javaType != null && javaType.isEnum()) {
            return "NVARCHAR(50)";
        }
        return "NVARCHAR(255)"; // default
    }

    @Override
    public String getColumnDefinition(ColumnMetadata column) {
        StringBuilder def = new StringBuilder();
        
        if (column.getType() != null && !column.getType().isEmpty()) {
            def.append(column.getType());
        } else if (column.getFieldType() != null) {
            def.append(mapJavaTypeToSqlType(column.getFieldType(), column));
        } else {
            def.append("NVARCHAR(255)");
        }
        
        if (!column.isNullable()) {
            def.append(" NOT NULL");
        }
        
        if (column.isUnique()) {
            def.append(" UNIQUE");
        }
        
        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            def.append(" DEFAULT ").append(column.getDefaultValue());
        }
        
        if (column.isAutoIncrement()) {
            def.append(" IDENTITY(1,1)");
        }
        
        return def.toString();
    }

    @Override
    public String generateCreateTableSql(TableMetadata table) {
        return createTableSql(table);
    }

    @Override
    public String generateDropTableSql(TableMetadata table) {
        return dropTableSql(table);
    }

    @Override
    public String generateRenameTableSql(TableMetadata table, String newName) {
        return renameTableSql(table.getTableName(), newName);
    }

    @Override
    public String generateAddColumnSql(TableMetadata table, ColumnMetadata column) {
        return addColumnSql(table, column);
    }

    @Override
    public String generateDropColumnSql(TableMetadata table, ColumnMetadata column) {
        return dropColumnSql(table, column.getName());
    }

    @Override
    public String generateModifyColumnSql(TableMetadata table, ColumnMetadata oldColumn, ColumnMetadata newColumn) {
        return modifyColumnSql(table, newColumn);
    }

    @Override
    public String generateRenameColumnSql(TableMetadata table, ColumnMetadata column, String newName) {
        return renameColumnSql(table, column.getName(), newName);
    }

    @Override
    public String generateCreateIndexSql(TableMetadata table, IndexMetadata index) {
        return createIndexSql(table, index);
    }

    @Override
    public String generateDropIndexSql(TableMetadata table, IndexMetadata index) {
        return dropIndexSql(table, index.getName());
    }

    @Override
    public String generateRenameIndexSql(TableMetadata table, IndexMetadata index, String newName) {
        return "EXEC sp_rename '" + quoteIdentifier(table.getTableName()) + "." + 
               quoteIdentifier(index.getName()) + "', '" + quoteIdentifier(newName) + "', 'INDEX'";
    }

    @Override
    public String generateAddForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey) {
        return addForeignKeySql(table, foreignKey);
    }

    @Override
    public String generateDropForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey) {
        return dropForeignKeySql(table, foreignKey.getConstraintName());
    }

    @Override
    public String generateColumnDefinition(ColumnMetadata column) {
        return getColumnDefinition(column);
    }

    @Override
    public String getIdentityColumnDefinition(ColumnMetadata column) {
        return getColumnDefinition(column) + " IDENTITY(1,1)";
    }

    @Override
    public String getDefaultValue(ColumnMetadata column) {
        if (column.getDefaultValue() == null) {
            return "";
        }
        return String.format("DEFAULT %s", column.getDefaultValue());
    }

    @Override
    public String getColumnType(ColumnMetadata column) {
        return column.getType();
    }

    @Override
    public String getAutoIncrementClause(ColumnMetadata column) {
        return column.isAutoIncrement() ? "IDENTITY(1,1)" : "";
    }

    @Override
    public String getNullableClause(ColumnMetadata column) {
        return column.isNullable() ? "NULL" : "NOT NULL";
    }

    @Override
    public String getUniqueClause(ColumnMetadata column) {
        return column.isUnique() ? "UNIQUE" : "";
    }

    @Override
    public String getPrimaryKeyClause(ColumnMetadata column) {
        return column.isPrimaryKey() ? "PRIMARY KEY" : "";
    }

    @Override
    public String getSequenceNextValueSql(String sequenceName) {
        return "SELECT NEXT VALUE FOR " + quoteIdentifier(sequenceName);
    }

    @Override
    public String getCreateSequenceSql(String sequenceName) {
        return "CREATE SEQUENCE " + quoteIdentifier(sequenceName);
    }

    @Override
    public String getDropSequenceSql(String sequenceName) {
        return "DROP SEQUENCE " + quoteIdentifier(sequenceName);
    }

    @Override
    public String getQuoteCharacter() {
        return "[";
    }

    @Override
    public String getEscapeCharacter() {
        return "\\";
    }

    @Override
    public String getSchemaPrefix(String schema) {
        return schema != null ? quoteIdentifier(schema) + "." : "";
    }

    @Override
    public String getCatalogPrefix(String catalog) {
        return catalog != null ? quoteIdentifier(catalog) + "." : "";
    }

    @Override
    public String getTableName(TableMetadata table) {
        return quoteIdentifier(table.getTableName());
    }

    @Override
    public String getColumnName(ColumnMetadata column) {
        return quoteIdentifier(column.getName());
    }

    @Override
    public String getIndexName(IndexMetadata index) {
        return quoteIdentifier(index.getName());
    }

    @Override
    public String getForeignKeyName(ForeignKeyMetadata foreignKey) {
        return quoteIdentifier(foreignKey.getConstraintName());
    }

    @Override
    public String getSequenceName(String baseName) {
        return quoteIdentifier(baseName + "_SEQ");
    }

    @Override
    public String getCommentClause(String comment) {
        // SQL Server doesn't support inline comments in DDL
        // Comments would need to be added separately with sp_addextendedproperty
        return "";
    }

    @Override
    public String renameIndexSql(String tableName, String oldIndexName, String newIndexName) {
        return String.format("EXEC sp_rename '%s.%s', '%s', 'INDEX'", 
            tableName, oldIndexName, newIndexName);
    }
} 