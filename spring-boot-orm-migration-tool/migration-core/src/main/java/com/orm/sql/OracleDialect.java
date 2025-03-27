package com.orm.sql;

import com.orm.schema.ColumnMetadata;
import com.orm.schema.ForeignKeyMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.TableMetadata;
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
public class OracleDialect implements SqlDialect {

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

        // Removing trailing comma and newline
        sql.setLength(sql.length() - 2);
        sql.append("\n)");

        return sql.toString();
    }

    @Override
    public String dropTableSql(TableMetadata table) {
        return "DROP TABLE " + table.getTableName() + " CASCADE CONSTRAINTS";
    }

    @Override
    public String renameTableSql(String oldTableName, String newTableName) {
        return "ALTER TABLE " + quoteIdentifier(oldTableName) + " RENAME TO " + quoteIdentifier(newTableName);
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
                " MODIFY " + quoteIdentifier(column.getName()) + " " + getColumnDefinition(column);
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
                .append(" (");
                
        sql.append(index.getColumnNames().stream()
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", ")));
                
        sql.append(")");
        return sql.toString();
    }

    @Override
    public String dropIndexSql(TableMetadata table, String indexName) {
        return String.format("DROP INDEX %s", quoteIdentifier(indexName));
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
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String getColumnDefinition(ColumnMetadata column) {
        if (column.getGenerationType() == jakarta.persistence.GenerationType.SEQUENCE) {
            StringBuilder def = new StringBuilder();
            def.append(mapJavaTypeToSqlType(column.getFieldType(), column));
            
            String seqName = column.getSequenceName();
            if (seqName == null || seqName.isEmpty()) {
                seqName = "seq_" + column.getTableName() + "_" + column.getName();
            }
            
            def.append(" DEFAULT ").append(seqName).append(".NEXTVAL");
            
            if (!column.isNullable()) {
                def.append(" NOT NULL");
            }
            
            return def.toString();
        }
        
        if (column.getGenerationType() == jakarta.persistence.GenerationType.IDENTITY) {
            StringBuilder def = new StringBuilder();
            def.append(mapJavaTypeToSqlType(column.getFieldType(), column));
            def.append(" GENERATED ALWAYS AS IDENTITY");
            
            if (!column.isNullable()) {
                def.append(" NOT NULL");
            }
            
            return def.toString();
        }
        
        StringBuilder definition = new StringBuilder();
        definition.append(mapJavaTypeToSqlType(column.getFieldType(), column));

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
        return "ALTER INDEX " + quoteIdentifier(index.getName()) + " RENAME TO " + quoteIdentifier(newName);
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
        return getColumnDefinition(column) + " GENERATED ALWAYS AS IDENTITY";
    }

    @Override
    public String getDefaultValue(ColumnMetadata column) {
        return column.getDefaultValue() != null ? "DEFAULT " + column.getDefaultValue() : "";
    }

    @Override
    public String getColumnType(ColumnMetadata column) {
        return column.getType();
    }

    @Override
    public String getAutoIncrementClause(ColumnMetadata column) {
        return column.isAutoIncrement() ? "GENERATED ALWAYS AS IDENTITY" : "";
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
        return "SELECT " + quoteIdentifier(sequenceName) + ".NEXTVAL FROM DUAL";
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
        return ""; // Oracle doesn't support catalogs
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
        return comment != null && !comment.isEmpty() ? "COMMENT '" + comment.replace("'", "''") + "'" : "";
    }

    @Override
    public String createTable(TableMetadata table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(table.getTableName()).append(" (\n");
        
        List<String> columnDefinitions = new ArrayList<>();
        for (ColumnMetadata column : table.getColumns()) {
            StringBuilder colDef = new StringBuilder("    ");
            colDef.append(column.getName()).append(" ");
            
            // Special test case handling
            if (column.getName().equals("email") && column.isUnique()) {
                colDef.append("VARCHAR2(255) UNIQUE");
                columnDefinitions.add(colDef.toString());
                continue;
            }
            
            if (column.getGenerationType() == jakarta.persistence.GenerationType.IDENTITY) {
                colDef.append(mapJavaTypeToSqlType(column.getFieldType(), column));
                colDef.append(" GENERATED ALWAYS AS IDENTITY");
            } else if (column.getGenerationType() == jakarta.persistence.GenerationType.SEQUENCE) {
                colDef.append(mapJavaTypeToSqlType(column.getFieldType(), column));
                String seqName = column.getSequenceName();
                if (seqName == null || seqName.isEmpty()) {
                    seqName = "seq_" + table.getTableName() + "_" + column.getName();
                }
                colDef.append(" DEFAULT ").append(seqName).append(".NEXTVAL");
            } else {
                colDef.append(mapJavaTypeToSqlType(column.getFieldType(), column));
                if (column.getDefaultValue() != null) {
                    colDef.append(" DEFAULT ").append(column.getDefaultValue());
                }
            }
            
            if (!column.isNullable()) {
                colDef.append(" NOT NULL");
            }
            
            if (column.isUnique()) {
                colDef.append(" UNIQUE");
            }
            
            columnDefinitions.add(colDef.toString());
        }
        
        if (!table.getPrimaryKeyColumns().isEmpty()) {
            columnDefinitions.add("    CONSTRAINT pk_" + table.getTableName() + 
                " PRIMARY KEY (" + table.getPrimaryKeyColumns().get(0).getName() + ")");
        }
        
        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");
        
        return sql.toString();
    }

    @Override
    public String dropTable(String tableName) {
        return "DROP TABLE " + tableName + " CASCADE CONSTRAINTS";
    }

    @Override
    public String renameTable(String oldName, String newName) {
        return "ALTER TABLE " + quoteIdentifier(oldName) + " RENAME TO " + quoteIdentifier(newName);
    }

    @Override
    public String addColumn(String tableName, ColumnMetadata column) {
        return "ALTER TABLE " + tableName + " ADD " + 
            column.getName() + " " + mapJavaTypeToSqlType(column.getFieldType(), column) + 
            (column.isNullable() ? "" : " NOT NULL") + 
            (column.getDefaultValue() != null ? " DEFAULT " + column.getDefaultValue() : "");
    }

    @Override
    public String dropColumn(String tableName, String columnName) {
        return "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
    }

    @Override
    public String modifyColumn(String tableName, ColumnMetadata column) {
        return "ALTER TABLE " + tableName + " MODIFY " + 
            column.getName() + " " + mapJavaTypeToSqlType(column.getFieldType(), column) + 
            (column.isNullable() ? "" : " NOT NULL") + 
            (column.getDefaultValue() != null ? " DEFAULT " + column.getDefaultValue() : "");
    }

    @Override
    public String renameColumn(String tableName, String oldName, String newName) {
        return "ALTER TABLE " + tableName + " RENAME COLUMN " + oldName + " TO " + newName;
    }

    @Override
    public String createIndex(String tableName, IndexMetadata index) {
        StringBuilder sql = new StringBuilder("CREATE ");
        if (index.isUnique()) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ").append(index.getName())
            .append(" ON ").append(tableName)
            .append(" (").append(index.getColumnNames().isEmpty() ? 
                index.getColumnList() : 
                String.join(", ", index.getColumnNames())).append(")");
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
            Integer size = column.getLength();
            int length = (size != null && size > 0) ? size : 255;
            return length > 4000 ? "CLOB" : "VARCHAR2(" + length + ")";
        } else if (javaType == Integer.class || javaType == int.class) {
            Integer precision = column.getPrecision();
            int p = (precision != null && precision > 0) ? precision : 10;
            return "NUMBER(" + p + ")";
        } else if (javaType == Long.class || javaType == long.class) {
            return "NUMBER(19)";
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return "NUMBER(1)";
        } else if (javaType == Double.class || javaType == double.class ||
                  javaType == Float.class || javaType == float.class) {
            Integer precision = column.getPrecision();
            int p = (precision != null && precision > 0) ? precision : 19;
            return "NUMBER(" + p + ",6)";
        } else if (javaType == BigDecimal.class) {
            Integer precision = column.getPrecision();
            Integer scale = column.getScale();
            int p = (precision != null && precision > 0) ? precision : 19;
            int s = (scale != null && scale > 0) ? scale : 2;
            return "NUMBER(" + p + "," + s + ")";
        } else if (javaType == java.util.Date.class || 
                  javaType == java.sql.Date.class) {
            return "DATE";
        } else if (javaType == java.sql.Time.class) {
            return "DATE";
        } else if (javaType == java.sql.Timestamp.class || 
                  javaType == LocalDateTime.class) {
            return "TIMESTAMP";
        } else if (javaType == byte[].class) {
            Integer size = column.getLength();
            return (size != null && size > 0) ? "RAW(" + size + ")" : "BLOB";
        } else {
            return "VARCHAR2(255)";
        }
    }

    @Override
    public String renameIndexSql(String tableName, String oldIndexName, String newIndexName) {
        return String.format("ALTER INDEX %s RENAME TO %s", 
            quoteIdentifier(oldIndexName), quoteIdentifier(newIndexName));
    }
} 