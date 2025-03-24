package com.orm.sql;

import com.orm.schema.ColumnMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.GenerationType;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDialectTest {

    private SqliteDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new SqliteDialect();
    }

    @Test
    @DisplayName("Should generate CREATE TABLE statement with various column types")
    void shouldGenerateCreateTable() {
        // Given
        TableMetadata table = new TableMetadata();
        table.setTableName("users");

        ColumnMetadata idColumn = new ColumnMetadata();
        idColumn.setName("id");
        idColumn.setFieldType(Long.class);
        idColumn.setNullable(false);
        idColumn.setGenerationType(GenerationType.IDENTITY);

        ColumnMetadata nameColumn = new ColumnMetadata();
        nameColumn.setName("name");
        nameColumn.setFieldType(String.class);
        nameColumn.setLength(100);
        nameColumn.setNullable(false);

        ColumnMetadata emailColumn = new ColumnMetadata();
        emailColumn.setName("email");
        emailColumn.setFieldType(String.class);
        emailColumn.setLength(255);
        emailColumn.setUnique(true);

        ColumnMetadata balanceColumn = new ColumnMetadata();
        balanceColumn.setName("balance");
        balanceColumn.setFieldType(BigDecimal.class);
        balanceColumn.setPrecision(10);
        balanceColumn.setScale(2);
        balanceColumn.setDefaultValue("0.00");

        table.setColumns(java.util.Arrays.asList(idColumn, nameColumn, emailColumn, balanceColumn));
        table.setPrimaryKey(idColumn);

        // When
        String sql = dialect.createTable(table);

        // Then
        assertTrue(sql.contains("CREATE TABLE users"));
        assertTrue(sql.contains("id INTEGER PRIMARY KEY AUTOINCREMENT"));
        assertTrue(sql.contains("name TEXT NOT NULL"));
        assertTrue(sql.contains("email TEXT UNIQUE"));
        assertTrue(sql.contains("balance REAL DEFAULT 0.00"));
    }

    @Test
    @DisplayName("Should generate ALTER TABLE statements for adding columns")
    void shouldGenerateAlterTableStatements() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setName("email");
        column.setFieldType(String.class);
        column.setLength(255);
        column.setNullable(false);
        column.setDefaultValue("'user@example.com'");

        // When
        String addColumnSql = dialect.addColumn("users", column);

        // Then
        assertEquals(
            "ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT 'user@example.com'",
            addColumnSql
        );
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for unsupported operations")
    void shouldThrowUnsupportedOperationException() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setName("email");
        column.setFieldType(String.class);

        // Then
        assertThrows(UnsupportedOperationException.class, () -> 
            dialect.dropColumn("users", "email"));
        assertThrows(UnsupportedOperationException.class, () -> 
            dialect.renameColumn("users", "email", "new_email"));
        assertThrows(UnsupportedOperationException.class, () -> 
            dialect.modifyColumn("users", column));
        assertThrows(UnsupportedOperationException.class, () -> 
            dialect.renameIndex("users", "idx_old", "idx_new"));
        assertThrows(UnsupportedOperationException.class, () -> 
            dialect.addForeignKey("orders", "fk_user", "user_id", "users", "id"));
        assertThrows(UnsupportedOperationException.class, () -> 
            dialect.dropForeignKey("orders", "fk_user"));
    }

    @Test
    @DisplayName("Should generate index statements")
    void shouldGenerateIndexStatements() {
        // Given
        IndexMetadata index = new IndexMetadata();
        index.setName("idx_email");
        index.setColumnList("email");
        index.setUnique(true);

        // When
        String createIndexSql = dialect.createIndex("users", index);
        String dropIndexSql = dialect.dropIndex("users", "idx_email");

        // Then
        assertEquals(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_email ON users (email)",
            createIndexSql
        );
        assertEquals("DROP INDEX IF EXISTS idx_email", dropIndexSql);
    }

    @Test
    @DisplayName("Should map Java types to SQLite types correctly")
    void shouldMapJavaTypesToSqliteTypes() {
        // Given
        ColumnMetadata column = new ColumnMetadata();

        // When & Then
        assertEquals("TEXT", dialect.mapJavaTypeToSqlType(String.class, column));
        assertEquals("INTEGER", dialect.mapJavaTypeToSqlType(Integer.class, column));
        assertEquals("INTEGER", dialect.mapJavaTypeToSqlType(Long.class, column));
        assertEquals("INTEGER", dialect.mapJavaTypeToSqlType(Boolean.class, column));
        assertEquals("REAL", dialect.mapJavaTypeToSqlType(BigDecimal.class, column));
        assertEquals("REAL", dialect.mapJavaTypeToSqlType(Double.class, column));
        assertEquals("TEXT", dialect.mapJavaTypeToSqlType(LocalDateTime.class, column));
    }

    @Test
    @DisplayName("Should handle table operations with IF EXISTS/IF NOT EXISTS")
    void shouldHandleTableOperations() {
        // When
        String dropTableSql = dialect.dropTable("users");
        String renameTableSql = dialect.renameTable("old_users", "new_users");

        // Then
        assertEquals("DROP TABLE IF EXISTS users", dropTableSql);
        assertEquals("ALTER TABLE old_users RENAME TO new_users", renameTableSql);
    }

    @Test
    @DisplayName("Should handle sequence generation type by falling back to autoincrement")
    void shouldHandleSequenceGeneration() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setName("id");
        column.setFieldType(Long.class);
        column.setGenerationType(GenerationType.SEQUENCE);
        
        // When
        String columnDef = dialect.getColumnDefinition(column);

        // Then
        assertTrue(columnDef.contains("PRIMARY KEY AUTOINCREMENT"));
    }
} 