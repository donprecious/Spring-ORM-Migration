package com.orm.spring;

import com.orm.model.Schema;
import com.orm.schema.ColumnMetadata;
import com.orm.schema.ForeignKeyMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.TableMetadata;
import com.orm.util.StringUtil;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Extracts database schema metadata using JDBC metadata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaExtractor {
    private final DataSource dataSource;

    /**
     * Extracts the current database schema.
     *
     * @param dataSource The data source
     * @return The extracted schema
     */
    public Schema extractSchema(DataSource dataSource) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String[] types = {"TABLE"};
            
            try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (!isSystemTable(tableName, metaData)) {
                        TableMetadata table = TableMetadata.builder()
                                .tableName(tableName)
                                .build();
                        
                        extractColumns(metaData, table);
                        extractPrimaryKey(metaData, table);
                        extractIndexes(metaData, table);
                        extractForeignKeys(metaData, table);
                        
                        tables.add(table);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to extract schema", e);
            // Return empty schema on error, don't throw exception
            return new Schema(new ArrayList<>());
        }
        return new Schema(tables);
    }

    private void extractColumns(DatabaseMetaData metaData, TableMetadata table) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, table.getTableName(), null)) {
            while (rs.next()) {
                ColumnMetadata column = extractColumnMetadata(rs);
                table.addColumn(column);
            }
        }
    }

    private void extractPrimaryKey(DatabaseMetaData metaData, TableMetadata table) throws SQLException {
        try (ResultSet rs = metaData.getPrimaryKeys(null, null, table.getTableName())) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                Optional<ColumnMetadata> columnOpt = table.getColumn(columnName);
                if (columnOpt.isPresent()) {
                    ColumnMetadata column = columnOpt.get();
                    column.setPrimaryKey(true);
                    table.setPrimaryKey(column);
                }
            }
        }
    }

    private void extractIndexes(DatabaseMetaData metaData, TableMetadata table) throws SQLException {
        try (ResultSet rs = metaData.getIndexInfo(null, null, table.getTableName(), false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;

                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                IndexMetadata idx = IndexMetadata.builder()
                        .name(indexName)
                        .unique(!nonUnique)
                        .build();
                
                table.addIndex(idx);
            }
        }
    }

    private void extractForeignKeys(DatabaseMetaData metaData, TableMetadata table) throws SQLException {
        try (ResultSet rs = metaData.getImportedKeys(null, null, table.getTableName())) {
            while (rs.next()) {
                String columnName = rs.getString("FKCOLUMN_NAME");
                String pkTableName = rs.getString("PKTABLE_NAME");
                String pkColumnName = rs.getString("PKCOLUMN_NAME");
                
                Optional<ColumnMetadata> columnOpt = table.getColumn(columnName);
                if (columnOpt.isPresent()) {
                    ColumnMetadata column = columnOpt.get();
                    column.setForeignKey(true);
                    column.setReferencedTable(pkTableName);
                    column.setReferencedColumn(pkColumnName);
                }
            }
        }
    }

    private ColumnMetadata extractColumnMetadata(ResultSet rs) throws SQLException {
        String columnName = rs.getString("COLUMN_NAME");
        int dataType = rs.getInt("DATA_TYPE");
        String typeName = rs.getString("TYPE_NAME");
        int columnSize = rs.getInt("COLUMN_SIZE");
        int decimalDigits = rs.getInt("DECIMAL_DIGITS");
        int nullable = rs.getInt("NULLABLE");
        String defaultValue = rs.getString("COLUMN_DEF");
        
        ColumnMetadata metadata = ColumnMetadata.builder()
                .columnName(columnName)
                .name(columnName)
                .fieldName(columnName)
                .fieldType(getJavaType(dataType))
                .nullable(nullable == DatabaseMetaData.columnNullable)
                .length(columnSize)
                .precision(decimalDigits)
                .columnDefinition(typeName)
                .defaultValue(defaultValue)
                .build();
        
        if (typeName.contains("IDENTITY") || typeName.contains("AUTO_INCREMENT")) {
            metadata.setGenerationType(jakarta.persistence.GenerationType.IDENTITY);
        }
        
        return metadata;
    }

    private Class<?> getJavaType(int sqlType) {
        switch (sqlType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                return String.class;
            case Types.NUMERIC:
            case Types.DECIMAL:
                return java.math.BigDecimal.class;
            case Types.BIT:
                return Boolean.class;
            case Types.TINYINT:
                return Byte.class;
            case Types.SMALLINT:
                return Short.class;
            case Types.INTEGER:
                return Integer.class;
            case Types.BIGINT:
                return Long.class;
            case Types.REAL:
                return Float.class;
            case Types.FLOAT:
            case Types.DOUBLE:
                return Double.class;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return byte[].class;
            case Types.DATE:
                return java.sql.Date.class;
            case Types.TIME:
                return java.sql.Time.class;
            case Types.TIMESTAMP:
                return java.sql.Timestamp.class;
            default:
                return Object.class;
        }
    }

    private boolean isSystemTable(String tableName, DatabaseMetaData metaData) {
        try {
            // Common system table patterns
            return tableName.startsWith("SYS_") ||
                   tableName.startsWith("SYSTEM_") ||
                   tableName.startsWith("pg_") ||  // PostgreSQL
                   tableName.startsWith("mysql_") || // MySQL
                   tableName.startsWith("sqlite_") || // SQLite
                   tableName.startsWith("sys_") ||   // Add lowercase sys_ prefix
                   tableName.equals("schema_history");
        } catch (Exception e) {
            log.warn("Error checking system table: {}", e.getMessage());
            return false;
        }
    }
} 