# SQL Dialects

This package contains SQL dialect implementations for various database systems. Each dialect implements the `SqlDialect` interface and provides database-specific SQL generation for schema operations.

## Dialect Factory

The `SqlDialectFactory` provides centralized management of SQL dialects:

```java
@Autowired
private SqlDialectFactory dialectFactory;

// Get dialect by database type
SqlDialect dialect = dialectFactory.getDialect(DatabaseType.MYSQL);

// Get dialect from JDBC URL
SqlDialect dialect = dialectFactory.getDialectFromJdbcUrl("jdbc:mysql://localhost:3306/db");

// Check dialect availability
boolean hasDialect = dialectFactory.hasDialect(DatabaseType.POSTGRESQL);

// Get all supported databases
DatabaseType[] supported = dialectFactory.getSupportedDatabases();
```

### Supported JDBC URLs

| Database   | JDBC URL Pattern                         |
| ---------- | ---------------------------------------- |
| MySQL      | jdbc:mysql://host:port/database          |
| PostgreSQL | jdbc:postgresql://host:port/database     |
| Oracle     | jdbc:oracle:thin:@host:port:sid          |
| SQL Server | jdbc:sqlserver://host:port;database=name |
| SQLite     | jdbc:sqlite:path/to/database.db          |

## Supported Databases

### MySQL Dialect

- **Class**: `MySqlDialect`
- **Features**:
  - Native `BOOLEAN` type support
  - `AUTO_INCREMENT` for identity columns
  - Full `ALTER TABLE` support
  - Sequences via `AUTO_INCREMENT`
  - `VARCHAR` with configurable length
  - `DECIMAL` for precise numeric data
  - Native `DATETIME` type

### PostgreSQL Dialect

- **Class**: `PostgreSqlDialect`
- **Features**:
  - `SERIAL`/`BIGSERIAL` for auto-incrementing columns
  - Native sequence support
  - Rich data type system
  - Separate `ALTER COLUMN` statements for modifications
  - `TEXT` type for unlimited string length
  - Native boolean type
  - `NUMERIC` for precise decimal numbers

### Oracle Dialect

- **Class**: `OracleDialect`
- **Features**:
  - `NUMBER` type with precision
  - Identity columns with `GENERATED ALWAYS AS IDENTITY`
  - Native sequence support
  - `VARCHAR2` for string data
  - `CLOB` for large text
  - `CASCADE CONSTRAINTS` for table drops
  - Rich constraint management

### SQL Server Dialect

- **Class**: `SqlServerDialect`
- **Features**:
  - Unicode support with `NVARCHAR`
  - `IDENTITY(1,1)` for auto-incrementing
  - Safe table operations with `IF OBJECT_ID` checks
  - `sp_rename` for object renaming
  - `BIT` type for boolean values
  - `DATETIME2` for high-precision timestamps
  - `NVARCHAR(MAX)` for large strings

### SQLite Dialect

- **Class**: `SqliteDialect`
- **Features**:
  - Simple type system (TEXT, INTEGER, REAL, BLOB)
  - `AUTOINCREMENT` for identity columns
  - Safe operations with `IF EXISTS`/`IF NOT EXISTS`
  - ISO8601 text storage for dates
  - Integer storage for boolean values
- **Limitations**:
  - No direct column drops (requires table recreation)
  - No direct column modifications
  - No direct foreign key alterations
  - Limited ALTER TABLE support

## Type Mappings

| Java Type     | MySQL        | PostgreSQL      | Oracle           | SQL Server   | SQLite  |
| ------------- | ------------ | --------------- | ---------------- | ------------ | ------- |
| String        | VARCHAR(n)   | VARCHAR(n)/TEXT | VARCHAR2(n)/CLOB | NVARCHAR(n)  | TEXT    |
| Integer       | INT          | INTEGER         | NUMBER(10)       | INT          | INTEGER |
| Long          | BIGINT       | BIGINT          | NUMBER(19)       | BIGINT       | INTEGER |
| Boolean       | BOOLEAN      | BOOLEAN         | NUMBER(1)        | BIT          | INTEGER |
| BigDecimal    | DECIMAL(p,s) | NUMERIC(p,s)    | NUMBER(p,s)      | DECIMAL(p,s) | REAL    |
| LocalDateTime | DATETIME     | TIMESTAMP       | TIMESTAMP        | DATETIME2    | TEXT    |
| LocalDate     | DATE         | DATE            | DATE             | DATE         | TEXT    |
| Enum          | VARCHAR(50)  | VARCHAR(50)     | VARCHAR2(50)     | NVARCHAR(50) | TEXT    |

## Usage Example

```java
@Autowired
private SqlDialectFactory dialectFactory;

// Get the appropriate dialect
SqlDialect sqlDialect = dialectFactory.getDialectFromJdbcUrl(jdbcUrl);

// Create a table
TableMetadata table = new TableMetadata();
table.setTableName("users");
// ... set up columns ...
String createTableSql = sqlDialect.createTable(table);

// Add a column
ColumnMetadata column = new ColumnMetadata();
column.setName("email");
column.setFieldType(String.class);
column.setLength(255);
String addColumnSql = sqlDialect.addColumn("users", column);

// Create an index
IndexMetadata index = new IndexMetadata();
index.setName("idx_email");
index.setColumnList("email");
index.setUnique(true);
String createIndexSql = sqlDialect.createIndex("users", index);
```

## Testing

Each dialect comes with a comprehensive test suite that verifies:

- Table creation with various column types
- Column modifications (where supported)
- Index operations
- Foreign key management
- Type mapping accuracy
- Identity/sequence generation
- Database-specific features
- Proper handling of unsupported operations

## Best Practices

1. **Type Selection**:

   - Use appropriate length for VARCHAR/NVARCHAR columns
   - Consider TEXT/CLOB for large string data
   - Use DECIMAL/NUMERIC for financial calculations

2. **Identity Columns**:

   - Prefer IDENTITY/AUTO_INCREMENT for simple scenarios
   - Use sequences when more control is needed
   - Be aware of SQLite's limitations

3. **Schema Changes**:

   - Always use IF EXISTS/IF NOT EXISTS where supported
   - Be cautious with destructive operations
   - Consider database-specific limitations

4. **Unicode Support**:

   - Use NVARCHAR in SQL Server for Unicode
   - PostgreSQL handles Unicode in standard types
   - SQLite stores everything as Unicode

5. **Dialect Selection**:
   - Use `SqlDialectFactory` for dialect management
   - Prefer JDBC URL-based dialect selection
   - Handle unsupported databases gracefully

## Contributing

When adding support for a new database:

1. Implement the `SqlDialect` interface
2. Follow the existing pattern for type mapping
3. Add comprehensive tests
4. Document database-specific features and limitations
5. Update this README with the new dialect information
6. Add the new dialect to `SqlDialectFactory` and `DatabaseType`
