package com.orm.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database migration script with both forward (up)
 * and rollback (down) SQL statements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationScript {
    private String description;
    private List<String> upSql;
    private List<String> downSql;
    private LocalDateTime createdAt;
    private LocalDateTime appliedAt;
    private boolean applied;
    private String checksum;
    private String version;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Creates a new migration script for a new migration.
     */
    public static MigrationScript createNew(String version, String description, String upScript, String downScript, LocalDateTime createdAt) {
        return MigrationScript.builder()
                .version(version)
                .description(description)
                .upSql(List.of(upScript))
                .downSql(List.of(downScript))
                .createdAt(createdAt)
                .warnings(new ArrayList<>())
                .build();
    }

    /**
     * Creates a migration script for an already applied migration.
     */
    public static MigrationScript createApplied(String version, String description, String upScript, String downScript, LocalDateTime appliedAt) {
        MigrationScript script = createNew(version, description, upScript, downScript, appliedAt);
        script.setAppliedAt(appliedAt);
        return script;
    }

    /**
     * Gets the complete up script with header comments.
     *
     * @return The complete up script
     */
    public String getCompleteUpScript() {
        return String.join("\n", upSql);
    }

    /**
     * Gets the complete down script with header comments.
     *
     * @return The complete down script
     */
    public String getCompleteDownScript() {
        return String.join("\n", downSql);
    }

    /**
     * Checks if this migration contains destructive changes.
     *
     * @return true if the migration contains destructive changes
     */
    public boolean hasDestructiveChanges() {
        return !warnings.isEmpty();
    }

    /**
     * Gets a list of warnings from the migration script.
     *
     * @return Array of warning messages
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Gets the version number from the file name.
     *
     * @return The version number
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the description part from the file name.
     *
     * @return The description from the file name
     */
    public String getFileDescription() {
        return description;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    /**
     * Checks if the migration has any warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUpSql(List<String> upSql) {
        this.upSql = upSql;
    }

    public void setDownSql(List<String> downSql) {
        this.downSql = downSql;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    /**
     * Gets the file name for this migration script.
     * 
     * @return The file name in format V{version}__{description}.sql
     */
    public String getFileName() {
        return String.format("V%s__%s.sql", 
            version != null ? version : "1", 
            description.replaceAll("\\s+", "_").toLowerCase());
    }

    /**
     * Convenience constructor for backward compatibility with tests
     */
    public MigrationScript(String version, String description, String upScript, String downScript, LocalDateTime createdAt) {
        this.version = version;
        this.description = description;
        this.upSql = upScript != null ? List.of(upScript) : List.of();
        this.downSql = downScript != null ? List.of(downScript) : List.of();
        this.createdAt = createdAt;
        this.appliedAt = null;
        this.applied = false;
        this.checksum = "";
        this.warnings = new ArrayList<>();
    }
} 