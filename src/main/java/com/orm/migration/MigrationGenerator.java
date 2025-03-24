package com.orm.migration;

import com.orm.schema.diff.SchemaChange;
import com.orm.schema.TableMetadata;
import com.orm.schema.ColumnMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.ForeignKeyMetadata;
import com.orm.sql.SqlDialect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates SQL migration scripts from schema changes
 */
@Component
public class MigrationGenerator {

    private final SqlDialect sqlDialect;
    
    public MigrationGenerator(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }
    
    /**
     * Generate a migration script from a list of schema changes
     * 
     * @param changes list of schema changes
     * @param description migration description
     * @return a migration script
     */
    public MigrationScript generateMigration(List<SchemaChange> changes, String description) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        
        List<String> upSql = new ArrayList<>();
        List<String> downSql = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean hasDestructiveChanges = false;
        
        upSql.add("-- Migration: " + description);
        upSql.add("-- Generated: " + LocalDateTime.now());
        upSql.add("");
        upSql.add("START TRANSACTION;");
        
        downSql.add("-- Rollback for: " + description);
        downSql.add("");
        downSql.add("START TRANSACTION;");
        
        for (SchemaChange change : changes) {
            processChange(change, upSql, downSql);
            
            if (change.isDestructive()) {
                hasDestructiveChanges = true;
                warnings.add("WARNING: " + change.getDescription() + " - This change may result in data loss");
            }
        }
        
        upSql.add("");
        upSql.add("COMMIT;");
        
        downSql.add("");
        downSql.add("COMMIT;");
        
        String version = generateVersion();
        String fileName = generateFileName(version, description);
        
        MigrationScript script = new MigrationScript();
        script.setVersion(version);
        script.setDescription(description);
        script.setUpSql(upSql);
        script.setDownSql(downSql);
        script.setCreatedAt(LocalDateTime.now());
        script.setWarnings(warnings);
        
        return script;
    }
    
    private void processChange(SchemaChange change, List<String> upSql, List<String> downSql) {
        upSql.add("-- " + change.getDescription());
        
        switch(change.getChangeType()) {
            case CREATE_TABLE:
                upSql.add(sqlDialect.createTable(change.getTable()));
                downSql.add(sqlDialect.dropTable(change.getTable().getTableName()));
                break;
            case DROP_TABLE:
                upSql.add(sqlDialect.dropTable(change.getTable().getTableName()));
                // Can't generate creation script for dropped tables automatically
                downSql.add("-- Cannot automatically generate CREATE TABLE script for dropped table: " + change.getTable().getTableName());
                break;
            case ADD_COLUMN:
                upSql.add(sqlDialect.addColumn(change.getTable().getTableName(), change.getColumn()));
                downSql.add(sqlDialect.dropColumn(change.getTable().getTableName(), change.getColumn().getName()));
                break;
            case DROP_COLUMN:
                upSql.add(sqlDialect.dropColumn(change.getTable().getTableName(), change.getColumn().getName()));
                // Can't generate add column script for dropped columns automatically
                downSql.add("-- Cannot automatically generate ADD COLUMN script for dropped column: " + change.getColumn().getName());
                break;
            case ADD_INDEX:
                upSql.add(sqlDialect.createIndex(change.getTable().getTableName(), change.getIndex()));
                downSql.add(sqlDialect.dropIndex(change.getTable().getTableName(), change.getIndex().getName()));
                break;
            case DROP_INDEX:
                upSql.add(sqlDialect.dropIndex(change.getTable().getTableName(), change.getIndex().getName()));
                // Can't generate create index script for dropped indexes automatically
                downSql.add("-- Cannot automatically generate CREATE INDEX script for dropped index: " + change.getIndex().getName());
                break;
            default:
                upSql.add("-- Unsupported change type: " + change.getChangeType());
                downSql.add("-- No rollback available for unsupported change type: " + change.getChangeType());
        }
        
        upSql.add("");
    }
    
    private String generateVersion() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return formatter.format(LocalDateTime.now());
    }
    
    private String generateFileName(String version, String description) {
        String sanitizedDesc = description.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        return "V" + version + "__" + sanitizedDesc + ".sql";
    }
} 