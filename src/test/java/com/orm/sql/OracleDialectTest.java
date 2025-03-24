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

class OracleDialectTest {

    private OracleDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new OracleDialect();
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
        assertTrue(sql.contains("id NUMBER(19) GENERATED ALWAYS AS IDENTITY"));
        assertTrue(sql.contains("name VARCHAR2(100) NOT NULL"));
        assertTrue(sql.contains("email VARCHAR2(255) UNIQUE"));
        assertTrue(sql.contains("balance NUMBER(10,2) DEFAULT 0.00"));
        assertTrue(sql.contains("CONSTRAINT pk_users PRIMARY KEY (id)"));
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
            "ALTER TABLE users ADD email VARCHAR2(255) NOT NULL DEFAULT 'user@example.com'",
            addColumnSql
        );
        assertEquals(
            "ALTER TABLE users MODIFY email VARCHAR2(255) NOT NULL DEFAULT 'user@example.com'",
            modifyColumnSql
        );
        assertEquals("ALTER TABLE users DROP COLUMN email", dropColumnSql);
        assertEquals("ALTER TABLE users RENAME COLUMN email TO email_address", renameColumnSql);
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
        assertEquals("DROP INDEX idx_email", dropIndexSql);
        assertEquals("ALTER INDEX idx_email RENAME TO idx_email_new", renameIndexSql);
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
    @DisplayName("Should map Java types to Oracle types correctly")
    void shouldMapJavaTypesToOracleTypes() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setLength(100);
        column.setPrecision(10);
        column.setScale(2);

        // When & Then
        assertEquals("VARCHAR2(100)", dialect.mapJavaTypeToSqlType(String.class, column));
        assertEquals("NUMBER(10)", dialect.mapJavaTypeToSqlType(Integer.class, column));
        assertEquals("NUMBER(19)", dialect.mapJavaTypeToSqlType(Long.class, column));
        assertEquals("NUMBER(1)", dialect.mapJavaTypeToSqlType(Boolean.class, column));
        assertEquals("NUMBER(10,2)", dialect.mapJavaTypeToSqlType(BigDecimal.class, column));
        assertEquals("TIMESTAMP", dialect.mapJavaTypeToSqlType(LocalDateTime.class, column));
        assertEquals("CLOB", dialect.mapJavaTypeToSqlType(String.class, 
            new ColumnMetadata() {{ setLength(5000); }}));
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
        assertTrue(columnDef.contains("DEFAULT seq_users_id.NEXTVAL"));
    }

    @Test
    @DisplayName("Should handle table drops with cascade constraints")
    void shouldHandleTableDropsWithCascade() {
        // When
        String dropTableSql = dialect.dropTable("users");

        // Then
        assertEquals("DROP TABLE users CASCADE CONSTRAINTS", dropTableSql);
    }
} 