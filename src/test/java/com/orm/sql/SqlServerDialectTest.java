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

class SqlServerDialectTest {

    private SqlServerDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new SqlServerDialect();
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
        assertTrue(sql.contains("id BIGINT IDENTITY(1,1)"));
        assertTrue(sql.contains("name NVARCHAR(100) NOT NULL"));
        assertTrue(sql.contains("email NVARCHAR(255) UNIQUE"));
        assertTrue(sql.contains("balance DECIMAL(10,2) DEFAULT 0.00"));
        assertTrue(sql.contains("CONSTRAINT PK_users PRIMARY KEY (id)"));
    }

    @Test
    @DisplayName("Should generate ALTER TABLE statements for column modifications")
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
        String modifyColumnSql = dialect.modifyColumn("users", column);
        String dropColumnSql = dialect.dropColumn("users", "email");
        String renameColumnSql = dialect.renameColumn("users", "email", "email_address");

        // Then
        assertEquals(
            "ALTER TABLE users ADD email NVARCHAR(255) NOT NULL DEFAULT 'user@example.com'",
            addColumnSql
        );
        assertEquals(
            "ALTER TABLE users ALTER COLUMN email NVARCHAR(255) NOT NULL DEFAULT 'user@example.com'",
            modifyColumnSql
        );
        assertEquals("ALTER TABLE users DROP COLUMN email", dropColumnSql);
        assertEquals("EXEC sp_rename 'users.email', 'email_address', 'COLUMN'", renameColumnSql);
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
        String renameIndexSql = dialect.renameIndex("users", "idx_email", "idx_email_new");

        // Then
        assertEquals("CREATE UNIQUE INDEX idx_email ON users (email)", createIndexSql);
        assertEquals("DROP INDEX idx_email ON users", dropIndexSql);
        assertEquals("EXEC sp_rename 'users.idx_email', 'idx_email_new', 'INDEX'", renameIndexSql);
    }

    @Test
    @DisplayName("Should generate foreign key statements")
    void shouldGenerateForeignKeyStatements() {
        // When
        String addFkSql = dialect.addForeignKey(
            "orders", "fk_user_id", "user_id", "users", "id"
        );
        String dropFkSql = dialect.dropForeignKey("orders", "fk_user_id");

        // Then
        assertEquals(
            "ALTER TABLE orders ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(id)",
            addFkSql
        );
        assertEquals("ALTER TABLE orders DROP CONSTRAINT fk_user_id", dropFkSql);
    }

    @Test
    @DisplayName("Should map Java types to SQL Server types correctly")
    void shouldMapJavaTypesToSqlServerTypes() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setLength(100);
        column.setPrecision(10);
        column.setScale(2);

        // When & Then
        assertEquals("NVARCHAR(100)", dialect.mapJavaTypeToSqlType(String.class, column));
        assertEquals("INT", dialect.mapJavaTypeToSqlType(Integer.class, column));
        assertEquals("BIGINT", dialect.mapJavaTypeToSqlType(Long.class, column));
        assertEquals("BIT", dialect.mapJavaTypeToSqlType(Boolean.class, column));
        assertEquals("DECIMAL(10,2)", dialect.mapJavaTypeToSqlType(BigDecimal.class, column));
        assertEquals("DATETIME2", dialect.mapJavaTypeToSqlType(LocalDateTime.class, column));
        assertEquals("NVARCHAR(MAX)", dialect.mapJavaTypeToSqlType(String.class, 
            new ColumnMetadata() {{ setLength(10000); }}));
    }

    @Test
    @DisplayName("Should handle sequence generation type correctly")
    void shouldHandleSequenceGeneration() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setName("id");
        column.setFieldType(Long.class);
        column.setGenerationType(GenerationType.SEQUENCE);
        column.setTableName("users");
        
        // When
        String columnDef = dialect.getColumnDefinition(column);

        // Then
        assertTrue(columnDef.contains("DEFAULT NEXT VALUE FOR SEQ_users_id"));
    }

    @Test
    @DisplayName("Should handle safe table drops")
    void shouldHandleSafeTableDrops() {
        // When
        String dropTableSql = dialect.dropTable("users");

        // Then
        assertEquals("IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users", dropTableSql);
    }

    @Test
    @DisplayName("Should handle table and object renames using sp_rename")
    void shouldHandleRenames() {
        // When
        String renameTableSql = dialect.renameTable("old_users", "new_users");
        String renameColumnSql = dialect.renameColumn("users", "old_email", "new_email");
        String renameIndexSql = dialect.renameIndex("users", "old_idx", "new_idx");

        // Then
        assertEquals("EXEC sp_rename 'old_users', 'new_users'", renameTableSql);
        assertEquals("EXEC sp_rename 'users.old_email', 'new_email', 'COLUMN'", renameColumnSql);
        assertEquals("EXEC sp_rename 'users.old_idx', 'new_idx', 'INDEX'", renameIndexSql);
    }
} 