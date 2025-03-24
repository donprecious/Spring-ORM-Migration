package com.orm.repository;

import com.orm.migration.MigrationScript;
import com.orm.schema.Schema;
import java.util.List;

public interface MigrationRepository {
    void save(MigrationScript script);
    void executeSql(String sql);
    List<MigrationScript> findAll();
    List<MigrationScript> findApplied();
    
    /**
     * Returns a list of migrations that have not been applied yet
     */
    List<MigrationScript> findPendingMigrations();
    
    /**
     * Returns the last applied migration
     */
    MigrationScript findLastAppliedMigration();
    
    /**
     * Returns migrations applied after the specified version
     */
    List<MigrationScript> findMigrationsAfterVersion(String version);
    
    /**
     * Returns migration history, limited to the specified count
     */
    List<MigrationScript> findMigrationHistory(int limit);
    
    /**
     * Extracts the current schema from the database
     */
    Schema extractCurrentSchema();
    
    /**
     * Extracts schema from entity classes
     */
    Schema extractSchemaFromEntities();
}