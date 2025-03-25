package com.orm.sql;

import com.orm.migration.MigrationScript;
import com.orm.schema.diff.SchemaChange;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of the MigrationScriptGenerator interface.
 * Generates SQL migration scripts based on schema changes.
 */
@Component
public class DefaultMigrationScriptGenerator implements MigrationScriptGenerator {
    
    private final SqlDialect sqlDialect;
    
    public DefaultMigrationScriptGenerator(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }
    
    @Override
    public MigrationScript generateMigration(List<SchemaChange> changes, String description) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        
        List<SchemaChange> sortedChanges = sortChanges(changes);
        List<String> upSqlStatements = new ArrayList<>();
        List<String> downSqlStatements = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Add headers
        upSqlStatements.add("-- Migration: " + description);
        upSqlStatements.add("-- Generated: " + LocalDateTime.now());
        upSqlStatements.add("");
        
        upSqlStatements.add("START TRANSACTION;");
        downSqlStatements.add("START TRANSACTION;");
        
        for (SchemaChange change : sortedChanges) {
            processChange(change, upSqlStatements, downSqlStatements);
            
            if (change.isDestructive()) {
                warnings.add("Warning: Destructive change - " + getChangeDescription(change));
            }
        }
        
        upSqlStatements.add("COMMIT;");
        downSqlStatements.add("COMMIT;");
        
        String version = generateVersion();
        String fileName = generateFileName(version, description, sortedChanges);
        
        MigrationScript script = new MigrationScript();
        script.setVersion(version);
        script.setDescription(description);
        script.setUpSql(upSqlStatements);
        script.setDownSql(downSqlStatements);
        script.setCreatedAt(LocalDateTime.now());
        script.setWarnings(warnings);
        
        return script;
    }
    
    private List<SchemaChange> sortChanges(List<SchemaChange> changes) {
        return changes.stream()
                .sorted(Comparator.comparingInt(SchemaChange::getOrderPriority))
                .collect(Collectors.toList());
    }
    
    private void processChange(SchemaChange change, List<String> upSqlStatements, List<String> downSqlStatements) {
        String comment = "-- " + getChangeDescription(change);
        upSqlStatements.add(comment);
        
        switch (change.getChangeType()) {
            case CREATE_TABLE:
                upSqlStatements.add(sqlDialect.createTableSql(change.getTable()));
                downSqlStatements.add(sqlDialect.dropTableSql(change.getTable()));
                break;
            case DROP_TABLE:
                upSqlStatements.add(sqlDialect.dropTableSql(change.getTable()));
                // Rollback would require full table definition
                downSqlStatements.add("-- Cannot automatically restore dropped table: " + change.getTable().getTableName());
                break;
            case RENAME_TABLE:
                upSqlStatements.add(sqlDialect.renameTableSql(change.getTable().getTableName(), change.getNewTableName()));
                downSqlStatements.add(sqlDialect.renameTableSql(change.getNewTableName(), change.getTable().getTableName()));
                break;
            case ADD_COLUMN:
                upSqlStatements.add(sqlDialect.addColumnSql(change.getTable(), change.getColumn()));
                downSqlStatements.add(sqlDialect.dropColumnSql(change.getTable(), change.getColumn().getName()));
                break;
            case DROP_COLUMN:
                upSqlStatements.add(sqlDialect.dropColumnSql(change.getTable(), change.getColumn().getName()));
                // Rollback would require full column definition
                downSqlStatements.add("-- Cannot automatically restore dropped column: " + change.getColumn().getName());
                break;
            case MODIFY_COLUMN:
                upSqlStatements.add(sqlDialect.modifyColumnSql(change.getTable(), change.getNewColumn()));
                // Rollback to original column definition
                downSqlStatements.add(sqlDialect.modifyColumnSql(change.getTable(), change.getColumn()));
                break;
            case RENAME_COLUMN:
                upSqlStatements.add(sqlDialect.renameColumnSql(change.getTable(), change.getColumn().getName(), change.getNewColumnName()));
                downSqlStatements.add(sqlDialect.renameColumnSql(change.getTable(), change.getNewColumnName(), change.getColumn().getName()));
                break;
            case ADD_INDEX:
                upSqlStatements.add(sqlDialect.createIndexSql(change.getTable(), change.getIndex()));
                downSqlStatements.add(sqlDialect.dropIndexSql(change.getTable(), change.getIndex().getName()));
                break;
            case DROP_INDEX:
                upSqlStatements.add(sqlDialect.dropIndexSql(change.getTable(), change.getIndex().getName()));
                // Rollback would require full index definition
                downSqlStatements.add("-- Cannot automatically restore dropped index: " + change.getIndex().getName());
                break;
            case RENAME_INDEX:
                upSqlStatements.add(sqlDialect.renameIndexSql(change.getTable().getTableName(), change.getIndex().getName(), change.getNewIndexName()));
                downSqlStatements.add(sqlDialect.renameIndexSql(change.getTable().getTableName(), change.getNewIndexName(), change.getIndex().getName()));
                break;
            case ADD_FOREIGN_KEY:
                upSqlStatements.add(sqlDialect.addForeignKeySql(change.getTable(), change.getForeignKey()));
                downSqlStatements.add(sqlDialect.dropForeignKeySql(change.getTable(), change.getForeignKey().getConstraintName()));
                break;
            case DROP_FOREIGN_KEY:
                upSqlStatements.add(sqlDialect.dropForeignKeySql(change.getTable(), change.getForeignKey().getConstraintName()));
                // Rollback would require full foreign key definition
                downSqlStatements.add("-- Cannot automatically restore dropped foreign key: " + change.getForeignKey().getConstraintName());
                break;
            default:
                upSqlStatements.add("-- Unsupported change type: " + change.getChangeType());
                downSqlStatements.add("-- Unsupported change type: " + change.getChangeType());
        }
        
        upSqlStatements.add("");
    }
    
    private String getChangeDescription(SchemaChange change) {
        String description = change.getDescription();
        if (description != null) {
            return description.toLowerCase();
        }
        return "unknown change";
    }
    
    private String generateVersion() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return formatter.format(LocalDateTime.now());
    }
    
    private String generateFileName(String version, String description, List<SchemaChange> changes) {
        if (changes.isEmpty()) {
            return "V" + version + "__" + sanitizeFileName(description) + ".sql";
        }
        
        SchemaChange primaryChange = changes.get(0);
        String action = primaryChange.getChangeType().toString().toLowerCase().replace('_', '_');
        String object = primaryChange.getObjectType().toLowerCase();
        String name = primaryChange.getObjectName().toLowerCase();
        
        return "V" + version + "__" + action + "_" + object + "_" + name + ".sql";
    }
    
    private String sanitizeFileName(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
} 