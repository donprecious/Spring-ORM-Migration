package com.orm.sql;

import com.orm.migration.MigrationScript;
import com.orm.schema.diff.SchemaChange;
import java.util.List;

/**
 * Interface for generating SQL migration scripts based on schema changes.
 */
public interface MigrationScriptGenerator {
    /**
     * Generate a migration script from a list of schema changes.
     * 
     * @param changes list of schema changes
     * @param description migration description
     * @return a migration script, or null if no changes provided
     */
    MigrationScript generateMigration(List<SchemaChange> changes, String description);
} 