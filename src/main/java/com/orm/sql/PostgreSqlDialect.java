package com.orm.sql;

import com.orm.schema.TableMetadata;
import com.orm.schema.ColumnMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.ForeignKeyMetadata;
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
public class PostgreSqlDialect implements SqlDialect {

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

        // Add indexes
        for (IndexMetadata index : table.getIndexes()) {
            sql.append("  ");
            if (index.isUnique()) {
                sql.append("UNIQUE ");
            }
            sql.append("INDEX ").append(quoteIdentifier(index.getName()))
                    .append(" (")
                    .append(index.getColumnNames().stream()
                            .map(this::quoteIdentifier)
                            .collect(Collectors.joining(", ")))
                    .append("),\n");
        }

        // Add foreign keys
        for (ForeignKeyMetadata fk : table.getForeignKeys()) {
            sql.append("  CONSTRAINT ").append(quoteIdentifier(fk.getConstraintName()))
                    .append(" FOREIGN KEY (").append(quoteIdentifier(fk.getColumnName())).append(")")
                    .append(" REFERENCES ").append(quoteIdentifier(fk.getReferencedTable()))
                    .append(" (").append(quoteIdentifier(fk.getReferencedColumn())).append(")");

            if (fk.getOnDelete() != null) {
                sql.append(" ON DELETE ").append(fk.getOnDelete());
            }
            if (fk.getOnUpdate() != null) {
                sql.append(" ON UPDATE ").append(fk.getOnUpdate());
            }
        }

        sql.append("\n)");

        if (table.getComment() != null && !table.getComment().isEmpty()) {
            sql.append(";\nCOMMENT ON TABLE ").append(quoteIdentifier(table.getTableName()))
                    .append(" IS '").append(table.getComment().replace("'", "''")).append("'");
        }

        return sql.toString();
    }

    @Override
    public String dropTableSql(TableMetadata table) {
        return "DROP TABLE IF EXISTS " + quoteIdentifier(table.getTableName());
    }

    @Override
    public String renameTableSql(String oldTableName, String newTableName) {
        return "ALTER TABLE " + quoteIdentifier(oldTableName) + " RENAME TO " + quoteIdentifier(newTableName);
    }

    @Override
    public String addColumnSql(TableMetadata table, ColumnMetadata column) {
        StringBuilder sql = new StringBuilder("ALTER TABLE ")
                .append(quoteIdentifier(table.getTableName()))
                .append(" ADD COLUMN ")
                .append(quoteIdentifier(column.getName()))
                .append(" ")
                .append(getColumnDefinition(column));

        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sql.append(";\nCOMMENT ON COLUMN ")
                    .append(quoteIdentifier(table.getTableName()))
                    .append(".")
                    .append(quoteIdentifier(column.getName()))
                    .append(" IS '")
                    .append(column.getComment().replace("'", "''"))
                    .append("'");
        }

        return sql.toString();
    }

    @Override
    public String dropColumnSql(TableMetadata table, String columnName) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) +
                " DROP COLUMN " + quoteIdentifier(columnName);
    }

    @Override
    public String modifyColumnSql(TableMetadata table, ColumnMetadata column) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(quoteIdentifier(table.getTableName()))
                .append(" ALTER COLUMN ").append(quoteIdentifier(column.getName()))
                .append(" TYPE ").append(getColumnDefinition(column));

        if (!column.isNullable()) {
            sql.append(",\nALTER TABLE ").append(quoteIdentifier(table.getTableName()))
                    .append(" ALTER COLUMN ").append(quoteIdentifier(column.getName()))
                    .append(" SET NOT NULL");
        }

        if (column.getDefaultValue() != null) {
            sql.append(",\nALTER TABLE ").append(quoteIdentifier(table.getTableName()))
                    .append(" ALTER COLUMN ").append(quoteIdentifier(column.getName()))
                    .append(" SET DEFAULT ").append(column.getDefaultValue());
        }

        if (column.getComment() != null && !column.getComment().isEmpty()) {
            sql.append(";\nCOMMENT ON COLUMN ")
                    .append(quoteIdentifier(table.getTableName()))
                    .append(".")
                    .append(quoteIdentifier(column.getName()))
                    .append(" IS '")
                    .append(column.getComment().replace("'", "''"))
                    .append("'");
        }

        return sql.toString();
    }

    @Override
    public String renameColumnSql(TableMetadata table, String oldColumnName, String newColumnName) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) +
                " RENAME COLUMN " + quoteIdentifier(oldColumnName) + " TO " + quoteIdentifier(newColumnName);
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
        return String.format("DROP INDEX %s", quoteIdentifier(indexName));
    }

    @Override
    public String addForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey) {
        StringBuilder sql = new StringBuilder("ALTER TABLE ")
                .append(quoteIdentifier(table.getTableName()))
                .append(" ADD CONSTRAINT ").append(quoteIdentifier(foreignKey.getConstraintName()))
                .append(" FOREIGN KEY (").append(quoteIdentifier(foreignKey.getColumnName())).append(")")
                .append(" REFERENCES ").append(quoteIdentifier(foreignKey.getReferencedTable()))
                .append(" (").append(quoteIdentifier(foreignKey.getReferencedColumn())).append(")");

        if (foreignKey.getOnDelete() != null) {
            sql.append(" ON DELETE ").append(foreignKey.getOnDelete());
        }
        if (foreignKey.getOnUpdate() != null) {
            sql.append(" ON UPDATE ").append(foreignKey.getOnUpdate());
        }

        if (foreignKey.isDeferrable()) {
            sql.append(" DEFERRABLE");
            if (foreignKey.isInitiallyDeferred()) {
                sql.append(" INITIALLY DEFERRED");
            }
        }

        return sql.toString();
    }

    @Override
    public String dropForeignKeySql(TableMetadata table, String foreignKeyName) {
        return "ALTER TABLE " + quoteIdentifier(table.getTableName()) +
                " DROP CONSTRAINT " + quoteIdentifier(foreignKeyName);
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String getColumnDefinition(ColumnMetadata column) {
        StringBuilder definition = new StringBuilder();
        definition.append(column.getType().toUpperCase());

        Integer size = column.getSize();
        Integer decimalDigits = column.getDecimalDigits();
        
        if (size != null && size > 0) {
            if (decimalDigits != null && decimalDigits > 0) {
                definition.append("(").append(size).append(",").append(decimalDigits).append(")");
            } else {
                definition.append("(").append(size).append(")");
            }
        }

        if (!column.isNullable()) {
            definition.append(" NOT NULL");
        }

        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            definition.append(" DEFAULT ").append(column.getDefaultValue());
        }

        if (column.isAutoIncrement()) {
            if (column.getType().equalsIgnoreCase("bigint")) {
                definition = new StringBuilder("BIGSERIAL");
            } else {
                definition = new StringBuilder("SERIAL");
            }
        }

        return definition.toString();
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
        return String.format("ALTER INDEX %s RENAME TO %s",
                quoteIdentifier(index.getName()),
                quoteIdentifier(newName));
    }

    @Override
    public String generateAddForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey) {
        return addForeignKeySql(table, foreignKey);
    }

    @Override
    public String generateDropForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey) {
        return dropForeignKeySql(table, foreignKey.getName());
    }

    @Override
    public String generateColumnDefinition(ColumnMetadata column) {
        return getColumnDefinition(column);
    }

    @Override
    public String getIdentityColumnDefinition(ColumnMetadata column) {
        return String.format("%s GENERATED BY DEFAULT AS IDENTITY", getColumnType(column));
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
        return column.isAutoIncrement() ? "GENERATED BY DEFAULT AS IDENTITY" : "";
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
        return String.format("SELECT nextval('%s')", quoteIdentifier(sequenceName));
    }

    @Override
    public String getCreateSequenceSql(String sequenceName) {
        return String.format("CREATE SEQUENCE %s", quoteIdentifier(sequenceName));
    }

    @Override
    public String getDropSequenceSql(String sequenceName) {
        return String.format("DROP SEQUENCE IF EXISTS %s", quoteIdentifier(sequenceName));
    }

    @Override
    public String getQuoteCharacter() {
        return "\"";
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
        return quoteIdentifier(foreignKey.getName());
    }

    @Override
    public String getSequenceName(String baseName) {
        return quoteIdentifier(baseName + "_seq");
    }

    @Override
    public String getCommentClause(String comment) {
        return comment != null ? String.format("COMMENT ON COLUMN %s IS '%s'", "%s", comment.replace("'", "''")) : "";
    }

    @Override
    public String createTable(TableMetadata table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(table.getTableName()).append(" (\n");
        
        List<String> columnDefinitions = new ArrayList<>();
        for (ColumnMetadata column : table.getColumns()) {
            columnDefinitions.add("    " + column.getName() + " " + getColumnDefinition(column));
        }
        
        if (!table.getPrimaryKeyColumns().isEmpty()) {
            List<String> pkColumns = table.getPrimaryKeyColumns().stream()
                .map(ColumnMetadata::getName)
                .collect(Collectors.toList());
            columnDefinitions.add("    PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
        }
        
        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");
        
        return sql.toString();
    }

    @Override
    public String dropTable(String tableName) {
        return "DROP TABLE " + tableName;
    }

    @Override
    public String renameTable(String oldName, String newName) {
        return "ALTER TABLE " + oldName + " RENAME TO " + newName;
    }

    @Override
    public String addColumn(String tableName, ColumnMetadata column) {
        return "ALTER TABLE " + tableName + " ADD COLUMN " + getColumnDefinition(column);
    }

    @Override
    public String dropColumn(String tableName, String columnName) {
        return "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
    }

    @Override
    public String renameColumn(String tableName, String oldName, String newName) {
        return "ALTER TABLE " + tableName + " RENAME COLUMN " + oldName + " TO " + newName;
    }

    @Override
    public String modifyColumn(String tableName, ColumnMetadata column) {
        List<String> alterations = new ArrayList<>();
        
        // PostgreSQL handles column modifications differently than MySQL
        // We need separate statements for type, nullable, and default value
        
        // Alter type
        alterations.add("ALTER COLUMN " + column.getName() + " TYPE " + 
            mapJavaTypeToSqlType(column.getFieldType(), column));
        
        // Alter nullable
        if (column.isNullable()) {
            alterations.add("ALTER COLUMN " + column.getName() + " DROP NOT NULL");
        } else {
            alterations.add("ALTER COLUMN " + column.getName() + " SET NOT NULL");
        }
        
        // Alter default
        if (column.getDefaultValue() != null) {
            alterations.add("ALTER COLUMN " + column.getName() + " SET DEFAULT " + column.getDefaultValue());
        } else {
            alterations.add("ALTER COLUMN " + column.getName() + " DROP DEFAULT");
        }
        
        return "ALTER TABLE " + tableName + " " + String.join(", ", alterations);
    }

    @Override
    public String createIndex(String tableName, IndexMetadata index) {
        StringBuilder sql = new StringBuilder("CREATE ");
        if (index.isUnique()) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ").append(index.getName())
           .append(" ON ").append(tableName)
           .append(" (").append(String.join(", ", index.getColumnNames())).append(")");
        return sql.toString();
    }

    @Override
    public String dropIndex(String tableName, String indexName) {
        return "DROP INDEX " + indexName;
    }

    @Override
    public String renameIndex(String tableName, String oldName, String newName) {
        return "ALTER INDEX " + oldName + " RENAME TO " + newName;
    }

    @Override
    public String addForeignKey(String tableName, String constraintName, String columnName,
                              String referencedTable, String referencedColumn) {
        return "ALTER TABLE " + tableName +
               " ADD CONSTRAINT " + constraintName +
               " FOREIGN KEY (" + columnName + ")" +
               " REFERENCES " + referencedTable + "(" + referencedColumn + ")";
    }

    @Override
    public String dropForeignKey(String tableName, String constraintName) {
        return "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;
    }

    @Override
    public String mapJavaTypeToSqlType(Class<?> javaType, ColumnMetadata column) {
        if (javaType == String.class) {
            Integer length = column.getLength();
            int len = (length != null && length > 0) ? length : 255;
            return len > 255 ? "TEXT" : "VARCHAR(" + len + ")";
        } else if (javaType == Integer.class || javaType == int.class) {
            return "INTEGER";
        } else if (javaType == Long.class || javaType == long.class) {
            return "BIGINT";
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return "BOOLEAN";
        } else if (javaType == BigDecimal.class) {
            Integer precision = column.getPrecision();
            Integer scale = column.getScale();
            int p = (precision != null && precision > 0) ? precision : 19;
            int s = (scale != null && scale > 0) ? scale : 2;
            return String.format("NUMERIC(%d,%d)", p, s);
        } else if (javaType == LocalDateTime.class) {
            return "TIMESTAMP";
        } else if (javaType == LocalDate.class) {
            return "DATE";
        } else if (javaType.isEnum()) {
            return "VARCHAR(50)";
        }
        return "VARCHAR(255)"; // default
    }

    @Override
    public String renameIndexSql(String tableName, String oldIndexName, String newIndexName) {
        return String.format("ALTER INDEX %s RENAME TO %s", 
            quoteIdentifier(oldIndexName), quoteIdentifier(newIndexName));
    }
} 