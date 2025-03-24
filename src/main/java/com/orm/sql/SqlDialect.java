package com.orm.sql;

import com.orm.schema.TableMetadata;
import com.orm.schema.ColumnMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.ForeignKeyMetadata;

public interface SqlDialect {
    // Base SQL generation methods
    String createTableSql(TableMetadata table);
    String dropTableSql(TableMetadata table);
    String renameTableSql(String oldTableName, String newTableName);
    String addColumnSql(TableMetadata table, ColumnMetadata column);
    String dropColumnSql(TableMetadata table, String columnName);
    String modifyColumnSql(TableMetadata table, ColumnMetadata column);
    String renameColumnSql(TableMetadata table, String oldColumnName, String newColumnName);
    String createIndexSql(TableMetadata table, IndexMetadata index);
    String dropIndexSql(TableMetadata table, String indexName);
    String renameIndexSql(String tableName, String oldIndexName, String newIndexName);
    String addForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey);
    String dropForeignKeySql(TableMetadata table, String foreignKeyName);
    String quoteIdentifier(String identifier);
    String getColumnDefinition(ColumnMetadata column);
    String getPrimaryKeyColumns(TableMetadata table);
    
    // Deprecated methods (these will be removed in future versions)
    String createTable(TableMetadata table);
    String dropTable(String tableName);
    String renameTable(String oldName, String newName);
    String addColumn(String tableName, ColumnMetadata column);
    String dropColumn(String tableName, String columnName);
    String modifyColumn(String tableName, ColumnMetadata column);
    String renameColumn(String tableName, String oldName, String newName);
    String createIndex(String tableName, IndexMetadata index);
    String dropIndex(String tableName, String indexName);
    String renameIndex(String tableName, String oldName, String newName);
    String addForeignKey(String tableName, String constraintName, String columnName,
                       String referencedTable, String referencedColumn);
    String dropForeignKey(String tableName, String constraintName);
    String mapJavaTypeToSqlType(Class<?> javaType, ColumnMetadata column);

    // Older-style generate operations (for backward compatibility)
    String generateCreateTableSql(TableMetadata table);
    String generateDropTableSql(TableMetadata table);
    String generateRenameTableSql(TableMetadata table, String newName);
    String generateAddColumnSql(TableMetadata table, ColumnMetadata column);
    String generateDropColumnSql(TableMetadata table, ColumnMetadata column);
    String generateModifyColumnSql(TableMetadata table, ColumnMetadata oldColumn, ColumnMetadata newColumn);
    String generateRenameColumnSql(TableMetadata table, ColumnMetadata column, String newName);
    String generateCreateIndexSql(TableMetadata table, IndexMetadata index);
    String generateDropIndexSql(TableMetadata table, IndexMetadata index);
    String generateRenameIndexSql(TableMetadata table, IndexMetadata index, String newName);
    String generateAddForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey);
    String generateDropForeignKeySql(TableMetadata table, ForeignKeyMetadata foreignKey);
    String generateColumnDefinition(ColumnMetadata column);

    // Column definition helpers
    String getIdentityColumnDefinition(ColumnMetadata column);
    String getDefaultValue(ColumnMetadata column);
    String getColumnType(ColumnMetadata column);
    String getAutoIncrementClause(ColumnMetadata column);
    String getNullableClause(ColumnMetadata column);
    String getUniqueClause(ColumnMetadata column);
    String getPrimaryKeyClause(ColumnMetadata column);

    // Sequence operations
    String getSequenceNextValueSql(String sequenceName);
    String getCreateSequenceSql(String sequenceName);
    String getDropSequenceSql(String sequenceName);

    // Identifier formatting
    String getQuoteCharacter();
    String getEscapeCharacter();
    String getSchemaPrefix(String schema);
    String getCatalogPrefix(String catalog);
    String getTableName(TableMetadata table);
    String getColumnName(ColumnMetadata column);
    String getIndexName(IndexMetadata index);
    String getForeignKeyName(ForeignKeyMetadata foreignKey);
    String getSequenceName(String baseName);
    String getCommentClause(String comment);
}