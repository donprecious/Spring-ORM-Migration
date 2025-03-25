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

class PostgreSqlDialectTest {

    private PostgreSqlDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new PostgreSqlDialect();
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
        System.out.println("Generated PostgreSQL SQL: " + sql);

        // Then
        System.out.println("Contains CREATE TABLE users: " + sql.contains("CREATE TABLE users"));
        System.out.println("Contains id BIGSERIAL: " + sql.contains("id BIGSERIAL"));
        System.out.println("Contains name VARCHAR(100) NOT NULL: " + sql.contains("name VARCHAR(100) NOT NULL"));
        System.out.println("Contains email VARCHAR(255) UNIQUE: " + sql.contains("email VARCHAR(255) UNIQUE"));
        System.out.println("Contains balance NUMERIC(10,2) DEFAULT 0.00: " + sql.contains("balance NUMERIC(10,2) DEFAULT 0.00"));
        System.out.println("Contains PRIMARY KEY (id): " + sql.contains("PRIMARY KEY (id)"));
        
        assertTrue(sql.contains("CREATE TABLE users"));
        assertTrue(sql.contains("id BIGSERIAL"));
        assertTrue(sql.contains("name VARCHAR(100) NOT NULL"));
        assertTrue(sql.contains("email VARCHAR(255) UNIQUE"));
        assertTrue(sql.contains("balance NUMERIC(10,2) DEFAULT 0.00"));
        assertTrue(sql.contains("PRIMARY KEY (id)"));
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
        String alterSql = dialect.modifyColumn("users", column);

        // Then
        assertTrue(alterSql.contains("ALTER COLUMN email TYPE VARCHAR(255)"));
        assertTrue(alterSql.contains("ALTER COLUMN email SET NOT NULL"));
        assertTrue(alterSql.contains("ALTER COLUMN email SET DEFAULT 'user@example.com'"));
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
    @DisplayName("Should map Java types to PostgreSQL types correctly")
    void shouldMapJavaTypesToPostgreSqlTypes() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setLength(100);
        column.setPrecision(10);
        column.setScale(2);

        // When & Then
        assertEquals("VARCHAR(100)", dialect.mapJavaTypeToSqlType(String.class, column));
        assertEquals("INTEGER", dialect.mapJavaTypeToSqlType(Integer.class, column));
        assertEquals("BIGINT", dialect.mapJavaTypeToSqlType(Long.class, column));
        assertEquals("BOOLEAN", dialect.mapJavaTypeToSqlType(Boolean.class, column));
        assertEquals("NUMERIC(10,2)", dialect.mapJavaTypeToSqlType(BigDecimal.class, column));
        assertEquals("TIMESTAMP", dialect.mapJavaTypeToSqlType(LocalDateTime.class, column));
        assertEquals("TEXT", dialect.mapJavaTypeToSqlType(String.class, 
            new ColumnMetadata() {{ setLength(1000); }}));
    }

    @Test
    @DisplayName("Should handle sequence generation type correctly")
    void shouldHandleSequenceGeneration() {
        // Given
        ColumnMetadata column = new ColumnMetadata();
        column.setName("id");
        column.setFieldType(Long.class);
        column.setGenerationType(GenerationType.SEQUENCE);
        column.setSequenceName("users_id_seq");

        // When
        String columnDef = dialect.getColumnDefinition(column);

        // Then
        assertTrue(columnDef.contains("DEFAULT nextval('users_id_seq')"));
    }
} 