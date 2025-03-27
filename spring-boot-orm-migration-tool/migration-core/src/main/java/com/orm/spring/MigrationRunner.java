package com.orm.spring;

import com.orm.migration.MigrationScript;
import com.orm.schema.diff.SchemaChange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles automatic migration execution on application startup.
 * This runner is only active when orm.migration.auto=true.
 */
@Slf4j
@Component
public class MigrationRunner {

    private final MigrationService migrationService;
    private final OrmMigrationProperties properties;

    @Autowired
    public MigrationRunner(MigrationService migrationService, OrmMigrationProperties properties) {
        this.migrationService = migrationService;
        this.properties = properties;
    }

    @EventListener
    public void onApplicationEvent(ApplicationStartedEvent event) {
        log.info("Checking database migrations on startup");

        try {
            // Validate schema if enabled
            if (properties.isValidateOnStartup()) {
                List<SchemaChange> changes = migrationService.validateSchema();
                if (!changes.isEmpty()) {
                    handleValidationFailure();
                }
            }

            // Check for pending migrations
            if (properties.isCheckPendingOnStartup()) {
                handlePendingMigrations();
            }

        } catch (Exception e) {
            handleMigrationError(e);
        }
    }

    private void handleValidationFailure() {
        String message = "Schema validation failed";
        if (properties.isFailOnPending()) {
            throw new IllegalStateException(message);
        } else {
            log.warn(message);
        }
    }

    private void handlePendingMigrations() {
        try {
            List<MigrationScript> applied = migrationService.applyPendingMigrations();
            if (!applied.isEmpty()) {
                log.info("Successfully applied {} pending migrations", applied.size());
            }
        } catch (Exception e) {
            String message = "Failed to apply pending migrations";
            if (properties.isFailOnPending()) {
                throw new IllegalStateException(message, e);
            } else {
                log.error(message + ": " + e.getMessage(), e);
            }
        }
    }

    private void handleMigrationError(Exception e) {
        String message = "Migration error on startup";
        if (properties.isFailOnPending()) {
            throw new IllegalStateException(message, e);
        } else {
            log.error(message, e);
        }
    }
} 