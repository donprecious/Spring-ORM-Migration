package com.orm.schema.diff;

import com.orm.schema.TableMetadata;
import com.orm.schema.ColumnMetadata;
import com.orm.schema.IndexMetadata;
import com.orm.schema.ForeignKeyMetadata;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaChange {
    public enum SchemaChangeType {
        CREATE_TABLE,
        DROP_TABLE,
        RENAME_TABLE,
        ADD_COLUMN,
        DROP_COLUMN,
        MODIFY_COLUMN,
        RENAME_COLUMN,
        CREATE_INDEX,
        DROP_INDEX,
        RENAME_INDEX,
        ADD_FOREIGN_KEY,
        DROP_FOREIGN_KEY
    }
    
    public enum ChangeType {
        ADD,
        REMOVE,
        MODIFY,
        RENAME,
        ADD_COLUMN,
        DROP_COLUMN,
        MODIFY_COLUMN,
        RENAME_COLUMN,
        ADD_TABLE,
        DROP_TABLE,
        RENAME_TABLE,
        CREATE_TABLE,
        ADD_INDEX,
        DROP_INDEX,
        RENAME_INDEX,
        ADD_FOREIGN_KEY,
        DROP_FOREIGN_KEY
    }
    
    public enum RiskLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private SchemaChangeType type;
    private TableMetadata table;
    
    @Builder.Default
    private ColumnMetadata column = null;
    
    @Builder.Default
    private ColumnMetadata newColumn = null;
    
    @Builder.Default
    private IndexMetadata index = null;
    
    @Builder.Default
    private ForeignKeyMetadata foreignKey = null;
    
    @Builder.Default
    private boolean isReversible = true;
    
    @Builder.Default
    private List<String> impactedObjects = new ArrayList<>();

    @Builder.Default
    private boolean destructive = false;
    
    @Builder.Default
    private boolean dataLoss = false;
    
    @Builder.Default
    private boolean requiresDowntime = false;

    @Builder.Default
    private boolean requiresWarning = false;
    
    @Builder.Default
    private ChangeType changeType = null;
    
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;
    
    @Builder.Default
    private String objectType = null;
    
    @Builder.Default
    private String objectName = null;

    @Builder.Default
    private String newTableName = null;
    
    @Builder.Default
    private String newColumnName = null;
    
    @Builder.Default
    private String newIndexName = null;
    
    @Builder.Default
    private String oldValue = null;
    
    @Builder.Default
    private String newValue = null;
    
    @Builder.Default
    private boolean requiresConfirmation = false;
    
    @Builder.Default
    private String confirmationMessage = null;

    private String newName;

    public String getTableName() {
        return table != null ? table.getTableName() : null;
    }
    
    public String getColumnName() {
        return column != null ? column.getName() : null;
    }
    
    public String getIndexName() {
        return index != null ? index.getName() : null;
    }
    
    public String getForeignKeyName() {
        return foreignKey != null ? foreignKey.getConstraintName() : null;
    }
    
    public ColumnMetadata getOriginalColumn() {
        return column;
    }
    
    public String getNewTableName() {
        return newTableName != null ? newTableName : newValue;
    }
    
    public String getNewColumnName() {
        return newColumnName != null ? newColumnName : newValue;
    }
    
    public String getNewIndexName() {
        return newIndexName != null ? newIndexName : newValue;
    }
    
    public SchemaChangeType getType() {
        return type;
    }
    
    public ChangeType getChangeType() {
        return changeType;
    }
    
    public String getObjectType() {
        if (objectType != null) {
            return objectType;
        }
        
        if (changeType == null) {
            return null;
        }
        
        return switch (changeType) {
            case CREATE_TABLE, DROP_TABLE, RENAME_TABLE, ADD_TABLE -> "TABLE";
            case ADD_COLUMN, DROP_COLUMN, MODIFY_COLUMN, RENAME_COLUMN -> "COLUMN";
            case ADD_INDEX, DROP_INDEX, RENAME_INDEX -> "INDEX";
            case ADD_FOREIGN_KEY, DROP_FOREIGN_KEY -> "FOREIGN KEY";
            default -> null;
        };
    }
    
    public String getObjectName() {
        if (objectName != null) {
            return objectName;
        }
        
        if (changeType == null) {
            return null;
        }
        
        return switch (changeType) {
            case CREATE_TABLE, DROP_TABLE, RENAME_TABLE, ADD_TABLE -> getTableName();
            case ADD_COLUMN, DROP_COLUMN, MODIFY_COLUMN, RENAME_COLUMN -> column != null ? column.getName() : null;
            case ADD_INDEX, DROP_INDEX, RENAME_INDEX -> index != null ? index.getName() : null;
            case ADD_FOREIGN_KEY, DROP_FOREIGN_KEY -> foreignKey != null ? foreignKey.getConstraintName() : null;
            default -> null;
        };
    }

    public String getDescription() {
        if (changeType == null) {
            return null;
        }
        
        return switch (changeType) {
            case CREATE_TABLE -> "Create table " + getTableName();
            case DROP_TABLE -> "Drop table " + getTableName();
            case RENAME_TABLE -> "Rename table " + getTableName() + " to " + getNewTableName();
            case ADD_COLUMN -> "Add column " + (column != null ? column.getName() : "") + " to table " + getTableName();
            case DROP_COLUMN -> "Drop column " + (column != null ? column.getName() : "") + " from table " + getTableName();
            case MODIFY_COLUMN -> "Modify column " + (column != null ? column.getName() : "") + " in table " + getTableName();
            case RENAME_COLUMN -> "Rename column " + (column != null ? column.getName() : "") + " to " + getNewColumnName() + " in table " + getTableName();
            case ADD_INDEX -> "Create index " + (index != null ? index.getName() : "") + " on table " + getTableName();
            case DROP_INDEX -> "Drop index " + (index != null ? index.getName() : "") + " from table " + getTableName();
            case RENAME_INDEX -> "Rename index " + (index != null ? index.getName() : "") + " to " + getNewIndexName() + " on table " + getTableName();
            case ADD_FOREIGN_KEY -> "Add foreign key " + (foreignKey != null ? foreignKey.getConstraintName() : "") + " to table " + getTableName();
            case DROP_FOREIGN_KEY -> "Drop foreign key " + (foreignKey != null ? foreignKey.getConstraintName() : "") + " from table " + getTableName();
            default -> null;
        };
    }

    public boolean isDataLossRisk() {
        if (changeType == null) {
            return false;
        }
        
        return switch (changeType) {
            case DROP_TABLE, DROP_COLUMN -> true;
            case MODIFY_COLUMN -> column != null && newColumn != null &&
                (column.isNullable() != newColumn.isNullable() || 
                !column.getType().equals(newColumn.getType()));
            default -> false;
        };
    }

    public String getWarning() {
        if (changeType == null) {
            return null;
        }
        
        return switch (changeType) {
            case DROP_TABLE -> "WARNING: This change will permanently delete the table '" + getTableName() + "' and all its data";
            case DROP_COLUMN -> "WARNING: This change will permanently delete the column '" + 
                (column != null ? column.getName() : "") + "' and its data from table '" + getTableName() + "'";
            case MODIFY_COLUMN -> "WARNING: This change may result in data loss or truncation";
            default -> null;
        };
    }

    public int getOrderPriority() {
        if (changeType == null) {
            return 13;
        }
        
        return switch (changeType) {
            case DROP_FOREIGN_KEY -> 1;
            case DROP_INDEX -> 2;
            case DROP_COLUMN -> 3;
            case DROP_TABLE -> 4;
            case RENAME_TABLE -> 5;
            case CREATE_TABLE, ADD_TABLE -> 6;
            case ADD_COLUMN -> 7;
            case RENAME_COLUMN -> 8;
            case MODIFY_COLUMN -> 9;
            case ADD_INDEX -> 10;
            case RENAME_INDEX -> 11;
            case ADD_FOREIGN_KEY -> 12;
            default -> 13;
        };
    }

    public boolean requiresWarning() {
        return isDataLossRisk() || destructive;
    }

    public boolean isDestructive() {
        return destructive;
    }
    
    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }
} 