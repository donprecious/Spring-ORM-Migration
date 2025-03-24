# Spring ORM Migration

A streamlined, intelligent database schema migration solution for Java applications.

## 🔍 The Problem

Database schema migrations are a critical but often challenging aspect of application development. Current solutions in the Java ecosystem have several limitations:

- **Manual Code Writing**: Most tools require manually writing SQL scripts for every schema change
- **Limited Intelligence**: Existing solutions don't intelligently detect or suggest schema changes
- **Difficult Tracking**: Tracking what changes were made and why is often left to commit messages or comments
- **Painful Rollbacks**: Rolling back migrations can be error-prone and complicated
- **Testing Complexity**: Testing migrations before applying them to production is cumbersome

## 🚀 Our Solution

Spring ORM Migration aims to revolutionize database schema management in Java applications by providing:

1. **Smart Schema Comparison**: Automatically detect differences between your JPA entities and database schema
2. **Migration Script Generation**: Generate SQL scripts based on detected changes
3. **Risk Analysis**: Assess the impact and risk level of each schema change
4. **Flexible Execution**: Apply migrations immediately or schedule them for later
5. **Simplified Rollbacks**: Easily undo migrations with automatically generated down scripts
6. **CLI Interface**: Execute migrations from the command line or integrate with your CI/CD pipeline

## ✨ Key Features

- **Smart Schema Comparator**: Intelligently detects schema changes, including column renames and table restructuring
- **Customizable Risk Assessment**: Configure what changes are considered high, medium, or low risk
- **Migration History**: Track all changes with detailed metadata
- **Preview Mode**: See what SQL would be executed without applying changes
- **Interactive CLI**: User-friendly command-line interface
- **Spring Boot Integration**: Seamless integration with Spring Boot applications
- **Test-Friendly Design**: Utilities for easy migration testing

## 🛠️ Technical Architecture

- **Schema Analysis**: Extract metadata from JPA entities and database
- **Schema Comparison**: Compare entity and database schemas to detect changes
- **Migration Generation**: Generate SQL scripts for schema changes
- **Migration Execution**: Apply migrations to the database
- **Migration History**: Track applied migrations

## 📋 Usage Example

### JPA Entity Example

Here's an example of a JPA entity:

```java
// Initial version of the User entity
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and setters
}
```

After making changes to the entity:

```java
// Updated version of the User entity
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100) // Changed length from 50 to 100
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name") // Added new field
    private String fullName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at") // Added new field
    private LocalDateTime updatedAt;

    // Getters and setters
}
```

### Generate Migration Command

To generate a migration based on these changes:

```bash
./migrate generate --description "Update user table structure"
```

### Command Output

```
$ ./migrate generate --description "Update user table structure"

Analyzing JPA entities...
Comparing with database schema...

Found 3 changes to 'users' table:
✓ ALTER COLUMN 'username': Changed length from 50 to 100 [LOW RISK]
✓ ADD COLUMN 'full_name': VARCHAR(255) [LOW RISK]
✓ ADD COLUMN 'updated_at': TIMESTAMP [LOW RISK]

Migration script generated successfully:
/migrations/V1_2_0__Update_user_table_structure.sql

-- Generated SQL Preview:
ALTER TABLE users MODIFY COLUMN username VARCHAR(100) NOT NULL;
ALTER TABLE users ADD COLUMN full_name VARCHAR(255);
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP;

Apply this migration now? [y/N]: n
Migration saved. Run 'migrate apply' to apply pending migrations.
```

### Apply Migration Command

```bash
./migrate apply
```

### Apply Command Output

```
$ ./migrate apply

Found 1 pending migration:
- V1_2_0__Update_user_table_structure.sql [LOW RISK]

Applying migrations...
✓ V1_2_0__Update_user_table_structure.sql applied successfully

Summary:
- 1 migration applied
- 0 migrations skipped
- 0 migrations failed

Migration history updated.
```

### View Migration Status

```bash
# View migration status
./migrate status

# Output
$ ./migrate status

Migration Status:
✓ V1_0_0__Initial_schema.sql - APPLIED (2025-03-10 14:30:22)
✓ V1_1_0__Add_role_column_to_users.sql - APPLIED (2025-03-15 09:12:04)
✓ V1_2_0__Update_user_table_structure.sql - APPLIED (2025-03-24 10:15:33)
```

## 🚧 Development Status

**Note:** This project is still in active development and not yet ready for production use.

Current focus areas:

- Improving schema comparison algorithms
- Enhancing migration script generation
- Building comprehensive test coverage
- Creating documentation and examples

## 📚 Documentation

Comprehensive documentation will be available once the project reaches beta status.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
