package com.orm.schema;

import lombok.Data;

/**
 * Represents the metadata for a database relationship, including
 * foreign key constraints and relationship type.
 */
@Data
public class RelationshipMetadata {
    private String sourceTable;
    private String sourceField;
    private String foreignKeyName;
    private boolean nullable = true;
    private RelationshipType type;

    /**
     * Enum representing the types of relationships between tables.
     */
    public enum RelationshipType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }

    /**
     * Gets the default foreign key name if none is specified.
     *
     * @return The default foreign key name
     */
    public String getEffectiveForeignKeyName() {
        if (foreignKeyName != null && !foreignKeyName.isEmpty()) {
            return foreignKeyName;
        }
        return String.format("fk_%s_%s", sourceTable, sourceField);
    }

    /**
     * Checks if this is a bidirectional relationship.
     *
     * @return true if the relationship is bidirectional
     */
    public boolean isBidirectional() {
        return type == RelationshipType.ONE_TO_ONE ||
               type == RelationshipType.MANY_TO_MANY;
    }

    /**
     * Checks if this relationship requires a join table.
     *
     * @return true if a join table is required
     */
    public boolean requiresJoinTable() {
        return type == RelationshipType.MANY_TO_MANY;
    }

    /**
     * Gets the default join table name for many-to-many relationships.
     *
     * @param targetTable The target table name
     * @return The default join table name
     */
    public String getDefaultJoinTableName(String targetTable) {
        return String.format("%s_%s", sourceTable, targetTable);
    }
} 