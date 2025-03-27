package com.orm.spring;

import com.orm.model.Schema;
import com.orm.schema.ColumnMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaExtractorTest {

    @Mock
    private Connection connection;

    @Mock
    private DataSource dataSource;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private ResultSet tablesRs;

    @Mock
    private ResultSet columnsRs;

    @Mock
    private ResultSet primaryKeysRs;

    @Mock
    private ResultSet indexesRs;

    @Mock
    private ResultSet foreignKeysRs;

    private SchemaExtractor extractor;

    @BeforeEach
    void setUp() throws Exception {
        extractor = new SchemaExtractor(dataSource);
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.getMetaData()).thenReturn(metaData);
    }

    @Test
    void shouldExtractTableMetadata() throws Exception {
        // Arrange
        setupTableMock("test_table");
        setupColumnMocks();
        setupPrimaryKeyMocks();
        setupIndexMocks();
        setupForeignKeyMocks();

        // Act
        Schema schema = extractor.extractSchema(dataSource);

        // Assert
        assertNotNull(schema);
        List<TableMetadata> tables = schema.getTables();
        assertEquals(1, tables.size());
        TableMetadata table = tables.get(0);
        assertEquals("test_table", table.getTableName());
        assertEquals(2, table.getColumns().size());
        assertEquals(1, table.getIndexes().size());
    }

    @Test
    void shouldHandleEmptySchema() throws Exception {
        // Arrange
        lenient().when(metaData.getTables(isNull(), isNull(), eq("%"), any())).thenReturn(tablesRs);
        lenient().when(tablesRs.next()).thenReturn(false);

        // Act
        Schema schema = extractor.extractSchema(dataSource);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.getTables().isEmpty());
    }

    @Test
    void shouldExtractColumnTypes() throws Exception {
        // Arrange
        setupTableMock("test_table");
        setupColumnWithTypeMocks();
        setupPrimaryKeyMocks();
        setupIndexMocks();
        setupForeignKeyMocks();

        // Act
        Schema schema = extractor.extractSchema(dataSource);

        // Assert
        assertNotNull(schema);
        List<TableMetadata> tables = schema.getTables();
        assertEquals(1, tables.size());
        TableMetadata table = tables.get(0);
        List<ColumnMetadata> columns = table.getColumns();
        assertEquals(2, columns.size());
        ColumnMetadata idColumn = columns.get(0);
        assertEquals("id", idColumn.getName());
        assertEquals(Long.class, idColumn.getFieldType());
    }

    @Test
    void shouldExtractIndexes() throws Exception {
        // Arrange
        setupTableMock("test_table");
        setupColumnMocks();
        setupPrimaryKeyMocks();
        setupDetailedIndexMocks();
        setupForeignKeyMocks();

        // Act
        Schema schema = extractor.extractSchema(dataSource);

        // Assert
        assertNotNull(schema);
        List<TableMetadata> tables = schema.getTables();
        assertEquals(1, tables.size());
        TableMetadata table = tables.get(0);
        List<IndexMetadata> indexes = table.getIndexes();
        assertEquals(1, indexes.size());
        IndexMetadata index = indexes.get(0);
        assertEquals("idx_name", index.getName());
        assertTrue(index.isUnique());
    }

    @Test
    void shouldHandleSQLException() throws Exception {
        // Arrange
        lenient().when(dataSource.getConnection()).thenThrow(new SQLException("Connection error"));

        // Act
        Schema schema = extractor.extractSchema(dataSource);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.getTables().isEmpty());
    }

    @Test
    void shouldIgnoreSystemTables() throws Exception {
        // Arrange
        lenient().when(metaData.getTables(isNull(), isNull(), eq("%"), any())).thenReturn(tablesRs);
        lenient().when(tablesRs.next()).thenReturn(true, false);
        lenient().when(tablesRs.getString("TABLE_NAME")).thenReturn("sys_table");
        lenient().when(metaData.getDatabaseProductName()).thenReturn("H2");
        
        // Mocking for extractColumns to handle system tables
        lenient().when(metaData.getColumns(isNull(), isNull(), eq("sys_table"), isNull())).thenReturn(columnsRs);
        lenient().when(columnsRs.next()).thenReturn(false);
        
        // Mocking for extractPrimaryKey to handle system tables
        lenient().when(metaData.getPrimaryKeys(isNull(), isNull(), eq("sys_table"))).thenReturn(primaryKeysRs);
        lenient().when(primaryKeysRs.next()).thenReturn(false);
        
        // Mocking for extractIndexes to handle system tables
        lenient().when(metaData.getIndexInfo(isNull(), isNull(), eq("sys_table"), eq(false), eq(false))).thenReturn(indexesRs);
        lenient().when(indexesRs.next()).thenReturn(false);
        
        // Mocking for extractForeignKeys to handle system tables
        lenient().when(metaData.getImportedKeys(isNull(), isNull(), eq("sys_table"))).thenReturn(foreignKeysRs);
        lenient().when(foreignKeysRs.next()).thenReturn(false);

        // Act
        Schema schema = extractor.extractSchema(dataSource);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.getTables().isEmpty());
    }

    private void setupTableMock(String tableName) throws Exception {
        lenient().when(metaData.getTables(isNull(), isNull(), eq("%"), any())).thenReturn(tablesRs);
        lenient().when(tablesRs.next()).thenReturn(true, false);
        lenient().when(tablesRs.getString("TABLE_NAME")).thenReturn(tableName);
    }

    private void setupColumnMocks() throws Exception {
        lenient().when(metaData.getColumns(isNull(), isNull(), anyString(), isNull())).thenReturn(columnsRs);
        lenient().when(columnsRs.next()).thenReturn(true, true, false);
        lenient().when(columnsRs.getString("COLUMN_NAME")).thenReturn("id", "name");
        lenient().when(columnsRs.getInt("DATA_TYPE")).thenReturn(Types.BIGINT, Types.VARCHAR);
        lenient().when(columnsRs.getString("TYPE_NAME")).thenReturn("BIGINT", "VARCHAR");
        lenient().when(columnsRs.getInt("COLUMN_SIZE")).thenReturn(0, 255);
        lenient().when(columnsRs.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls, DatabaseMetaData.columnNullable);
        lenient().when(columnsRs.getInt("DECIMAL_DIGITS")).thenReturn(0, 0);
        lenient().when(columnsRs.getString("COLUMN_DEF")).thenReturn(null, null);
    }

    private void setupColumnWithTypeMocks() throws Exception {
        lenient().when(metaData.getColumns(isNull(), isNull(), anyString(), isNull())).thenReturn(columnsRs);
        lenient().when(columnsRs.next()).thenReturn(true, true, false);
        lenient().when(columnsRs.getString("COLUMN_NAME")).thenReturn("id", "active");
        lenient().when(columnsRs.getInt("DATA_TYPE")).thenReturn(Types.BIGINT, Types.BOOLEAN);
        lenient().when(columnsRs.getString("TYPE_NAME")).thenReturn("BIGINT", "BOOLEAN");
        lenient().when(columnsRs.getInt("COLUMN_SIZE")).thenReturn(0, 0);
        lenient().when(columnsRs.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls, DatabaseMetaData.columnNullable);
        lenient().when(columnsRs.getInt("DECIMAL_DIGITS")).thenReturn(0, 0);
        lenient().when(columnsRs.getString("COLUMN_DEF")).thenReturn(null, null);
    }

    private void setupPrimaryKeyMocks() throws Exception {
        lenient().when(metaData.getPrimaryKeys(isNull(), isNull(), anyString())).thenReturn(primaryKeysRs);
        lenient().when(primaryKeysRs.next()).thenReturn(true, false);
        lenient().when(primaryKeysRs.getString("COLUMN_NAME")).thenReturn("id");
    }

    private void setupIndexMocks() throws Exception {
        lenient().when(metaData.getIndexInfo(isNull(), isNull(), anyString(), eq(false), eq(false))).thenReturn(indexesRs);
        lenient().when(indexesRs.next()).thenReturn(true, false);
        lenient().when(indexesRs.getString("INDEX_NAME")).thenReturn("idx_name");
        lenient().when(indexesRs.getString("COLUMN_NAME")).thenReturn("name");
        lenient().when(indexesRs.getBoolean("NON_UNIQUE")).thenReturn(false);
    }

    private void setupDetailedIndexMocks() throws Exception {
        lenient().when(metaData.getIndexInfo(isNull(), isNull(), anyString(), eq(false), eq(false))).thenReturn(indexesRs);
        lenient().when(indexesRs.next()).thenReturn(true, false);
        lenient().when(indexesRs.getString("INDEX_NAME")).thenReturn("idx_name");
        lenient().when(indexesRs.getString("COLUMN_NAME")).thenReturn("name");
        lenient().when(indexesRs.getBoolean("NON_UNIQUE")).thenReturn(false);
        lenient().when(indexesRs.getShort("TYPE")).thenReturn((short) 3); // TABLE_INDEX_CLUSTERED
        lenient().when(indexesRs.getString("FILTER_CONDITION")).thenReturn("name IS NOT NULL");
    }

    private void setupForeignKeyMocks() throws Exception {
        lenient().when(metaData.getImportedKeys(isNull(), isNull(), anyString())).thenReturn(foreignKeysRs);
        lenient().when(foreignKeysRs.next()).thenReturn(false);
    }
} 