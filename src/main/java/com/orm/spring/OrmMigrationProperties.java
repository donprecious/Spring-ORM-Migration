package com.orm.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the ORM migration tool.
 * These properties can be set in application.properties/yaml.
 */
@Data
@ConfigurationProperties(prefix = "orm.migration")
public class OrmMigrationProperties {

    /**
     * Whether to automatically run migrations on startup.
     */
    private boolean auto = false;

    /**
     * The database URL (optional if using Spring's DataSource).
     */
    private String url;

    /**
     * The base package to scan for entities.
     */
    private String basePackage;

    /**
     * The directory where migration scripts are stored.
     */
    private String scriptLocation = "db/migration";

    /**
     * Whether to validate entities against the database on startup.
     */
    private boolean validateOnStartup = true;

    /**
     * Whether to allow destructive changes in auto-migration mode.
     */
    private boolean allowDestructive = false;

    /**
     * The file name pattern for migration scripts.
     */
    private String filePattern = "V{timestamp}__{description}.sql";

    /**
     * Whether to generate both up and down scripts.
     */
    private boolean generateDownScripts = true;

    /**
     * Whether to format the generated SQL scripts.
     */
    private boolean formatSql = true;

    /**
     * The table name for storing migration history.
     */
    private String historyTable = "schema_history";

    /**
     * Whether to check for pending migrations on startup.
     */
    private boolean checkPendingOnStartup = true;

    /**
     * Whether to fail on pending migrations when checking.
     */
    private boolean failOnPending = true;

    /**
     * Whether to create the history table if it doesn't exist.
     */
    private boolean createHistoryTable = true;

    /**
     * The timeout in seconds for acquiring migration lock.
     */
    private int lockTimeout = 60;

    /**
     * Whether to ignore missing migrations.
     */
    private boolean ignoreMissingMigrations = false;

    /**
     * Whether to ignore future migrations.
     */
    private boolean ignoreFutureMigrations = false;

    /**
     * The comment prefix for migration scripts.
     */
    private String commentPrefix = "--";

    /**
     * The statement separator for migration scripts.
     */
    private String statementSeparator = ";";

    /**
     * Whether to use transaction for migrations.
     */
    private boolean useTransaction = true;

    /**
     * The encoding for migration scripts.
     */
    private String encoding = "UTF-8";

    /**
     * Whether to place a lock on the database during migration.
     */
    private boolean useLock = true;

    /**
     * Custom SQL to execute before each migration.
     */
    private String beforeMigrate;

    /**
     * Custom SQL to execute after each migration.
     */
    private String afterMigrate;
} 