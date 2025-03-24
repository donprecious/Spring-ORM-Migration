package com.orm.spring;

import com.orm.migration.MigrationScript;
import com.orm.sql.MigrationScriptGenerator;
import com.orm.schema.Schema;
import com.orm.repository.MigrationRepository;
import com.orm.schema.SchemaAnalyzer;
import com.orm.schema.diff.SchemaChange;
import com.orm.sql.SqlDialect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing database migrations.
 * This class coordinates the schema analysis, comparison,
 * and migration script generation process.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationService {

    private final SchemaAnalyzer schemaAnalyzer;
    private final MigrationScriptGenerator scriptGenerator;
    private final MigrationRepository migrationRepository;
    private final SqlDialect sqlDialect;

    public MigrationScript generateMigration(Schema sourceSchema, Schema targetSchema, String description) {
        List<SchemaChange> changes = schemaAnalyzer.compareSchemas(sourceSchema, targetSchema);
        return scriptGenerator.generateMigration(changes, description);
    }

    /**
     * Simplified method to generate a migration with description and version.
     * This is used by the CLI when no explicit schemas are provided.
     */
    public MigrationScript generateMigration(String description, String version) {
        // Get current schema from database
        Schema sourceSchema = migrationRepository.extractCurrentSchema();
        // Generate target schema from entity classes
        Schema targetSchema = migrationRepository.extractSchemaFromEntities(); 
        
        List<SchemaChange> changes = schemaAnalyzer.compareSchemas(sourceSchema, targetSchema);
        MigrationScript script = scriptGenerator.generateMigration(changes, description);
        if (version != null && !version.isEmpty()) {
            script.setVersion(version);
        }
        return script;
    }
    
    /**
     * Validates the current schema against the entity schema
     */
    public List<SchemaChange> validateSchema() {
        Schema sourceSchema = migrationRepository.extractCurrentSchema();
        Schema targetSchema = migrationRepository.extractSchemaFromEntities();
        return schemaAnalyzer.compareSchemas(sourceSchema, targetSchema);
    }
    
    /**
     * Returns a list of pending migrations that haven't been applied
     */
    public List<MigrationScript> getPendingMigrations() {
        return migrationRepository.findPendingMigrations();
    }
    
    /**
     * Applies migrations up to the specified version
     */
    public List<MigrationScript> applyMigrations(String version, boolean dryRun, boolean force) {
        List<MigrationScript> pendingMigrations = getPendingMigrations();
        if (version != null && !version.isEmpty()) {
            pendingMigrations = pendingMigrations.stream()
                .filter(m -> m.getVersion().compareTo(version) <= 0)
                .toList();
        }
        
        if (!dryRun) {
            for (MigrationScript migration : pendingMigrations) {
                applyMigration(migration);
            }
        }
        
        return pendingMigrations;
    }
    
    /**
     * Applies all pending migrations
     */
    public List<MigrationScript> applyPendingMigrations() {
        return applyMigrations(null, false, false);
    }
    
    /**
     * Undoes the last applied migration
     */
    public MigrationScript undoLastMigration(boolean dryRun) {
        MigrationScript lastMigration = migrationRepository.findLastAppliedMigration();
        if (lastMigration == null) {
            return null;
        }
        
        if (!dryRun) {
            rollbackMigration(lastMigration);
        }
        
        return lastMigration;
    }
    
    /**
     * Reverts migrations back to the specified version
     */
    public List<MigrationScript> revertToVersion(String version, boolean dryRun) {
        List<MigrationScript> migrationsToRevert = migrationRepository.findMigrationsAfterVersion(version);
        
        if (!dryRun) {
            // Roll back in reverse order (newest first)
            for (MigrationScript migration : migrationsToRevert) {
                rollbackMigration(migration);
            }
        }
        
        return migrationsToRevert;
    }
    
    /**
     * Returns the migration history, limited to the specified count
     */
    public List<MigrationScript> getMigrationHistory(int limit) {
        return migrationRepository.findMigrationHistory(limit);
    }

    public void applyMigration(MigrationScript migration) {
        log.info("Applying migration: {}", migration.getDescription());
        migrationRepository.save(migration);
        
        if (migration.hasWarnings()) {
            log.warn("Migration has warnings:");
            migration.getWarnings().forEach(warning -> log.warn("- {}", warning));
        }
        
        try {
            // Execute the up SQL statements
            for (String sql : migration.getUpSql()) {
                log.debug("Executing SQL: {}", sql);
                migrationRepository.executeSql(sql);
            }
            
            migration.setApplied(true);
            migrationRepository.save(migration);
            log.info("Migration applied successfully");
            
        } catch (Exception e) {
            log.error("Failed to apply migration: {}", e.getMessage(), e);
            try {
                // Attempt rollback by executing down SQL statements
                log.info("Rolling back migration...");
                for (String sql : migration.getDownSql()) {
                    log.debug("Executing rollback SQL: {}", sql);
                    migrationRepository.executeSql(sql);
                }
                log.info("Rollback completed successfully");
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback migration: {}", rollbackEx.getMessage(), rollbackEx);
                throw new RuntimeException("Migration failed and rollback also failed", rollbackEx);
            }
            throw new RuntimeException("Migration failed", e);
        }
    }

    public void rollbackMigration(MigrationScript migration) {
        log.info("Rolling back migration: {}", migration.getDescription());
        
        if (!migration.isApplied()) {
            log.warn("Migration is not applied, skipping rollback");
            return;
        }
        
        try {
            // Execute the down SQL statements
            for (String sql : migration.getDownSql()) {
                log.debug("Executing rollback SQL: {}", sql);
                migrationRepository.executeSql(sql);
            }
            
            migration.setApplied(false);
            migrationRepository.save(migration);
            log.info("Migration rolled back successfully");
            
        } catch (Exception e) {
            log.error("Failed to rollback migration: {}", e.getMessage(), e);
            throw new RuntimeException("Rollback failed", e);
        }
    }

    /**
     * Compatibility method for tests
     */
    public List<MigrationScript> previewPendingMigrations() {
        return getPendingMigrations();
    }

    /**
     * Compatibility method for tests
     */
    public MigrationScript getLastAppliedMigration() {
        List<MigrationScript> history = getMigrationHistory(1);
        return history.isEmpty() ? null : history.get(0);
    }

    /**
     * Compatibility method for tests
     */
    public MigrationScript undoLastMigration() {
        return undoLastMigration(false);
    }

    /**
     * Compatibility method for tests
     */
    public List<MigrationScript> revertToVersion(String version) {
        return revertToVersion(version, false);
    }

    /**
     * Compatibility method for tests
     */
    public List<MigrationScript> getMigrationsToRevert(String version) {
        List<MigrationScript> appliedMigrations = getMigrationHistory(100);
        return appliedMigrations.stream()
            .filter(m -> m.getVersion().compareTo(version) > 0)
            .toList();
    }
}