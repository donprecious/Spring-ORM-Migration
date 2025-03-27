package com.orm.schema;

import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents metadata for a database column, including its type, constraints, and other properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ColumnMetadata {
    private String columnName;
    private String tableName;
    private String fieldName;
    private String type;
    private boolean nullable;
    private boolean unique;
    private boolean primaryKey;
    private String defaultValue;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private String columnDefinition;
    private String referencedTable;
    private String referencedColumn;
    private boolean insertable;
    private boolean updatable;
    private boolean autoIncrement;
    private String comment;
    private boolean isForeignKey;
    private GenerationType generationType;
    private String sequenceName;
    private String name;
    private Integer size;
    private Integer decimalDigits;
    private String enumValues;
    private String schema;
    private String catalog;

    public void setForeignKey(boolean isForeignKey) {
        this.isForeignKey = isForeignKey;
    }

    public void setGenerationType(GenerationType generationType) {
        this.generationType = generationType;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public GenerationType getGenerationType() {
        return generationType;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public String getTableName() {
        return tableName;
    }

    public boolean isForeignKey() {
        return isForeignKey;
    }

    // Convenience methods
    public String getName() {
        return columnName != null ? columnName : fieldName;
    }
    
    public void setName(String name) {
        this.fieldName = name;
        if (this.columnName == null) {
            this.columnName = name;
        }
    }
    
    public Class<?> getFieldType() {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Type not found: " + type, e);
        }
    }
    
    public void setFieldType(Class<?> fieldType) {
        this.type = fieldType.getName();
    }

    // For numeric types
    public boolean hasScale() {
        return scale > 0;
    }

    // For string types
    public boolean hasLength() {
        return length > 0;
    }

    /**
     * Checks if this column is numeric.
     *
     * @return true if the column type is numeric
     */
    public boolean isNumeric() {
        return Number.class.isAssignableFrom(getFieldType()) ||
               getFieldType() == int.class ||
               getFieldType() == long.class ||
               getFieldType() == double.class ||
               getFieldType() == float.class;
    }

    /**
     * Checks if this column is a string type.
     *
     * @return true if the column type is string
     */
    public boolean isString() {
        return String.class.isAssignableFrom(getFieldType()) ||
               CharSequence.class.isAssignableFrom(getFieldType());
    }

    /**
     * Checks if this column is a temporal type.
     *
     * @return true if the column type is temporal
     */
    public boolean isTemporal() {
        return java.util.Date.class.isAssignableFrom(getFieldType()) ||
               java.time.temporal.Temporal.class.isAssignableFrom(getFieldType());
    }

    /**
     * Checks if this column is auto-generated.
     *
     * @return true if the column has a generation strategy
     */
    public boolean isGenerated() {
        return autoIncrement;
    }

    /**
     * Gets the effective maximum length for string columns.
     *
     * @return The effective length, considering the column definition
     */
    public int getEffectiveLength() {
        if (columnDefinition != null && columnDefinition.toLowerCase().contains("varchar")) {
            try {
                int start = columnDefinition.indexOf('(');
                int end = columnDefinition.indexOf(')');
                if (start > 0 && end > start) {
                    return Integer.parseInt(columnDefinition.substring(start + 1, end));
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors and return default length
            }
        }
        return length != null ? length : 255;
    }

    public String getColumnDefinition() {
        StringBuilder definition = new StringBuilder();
        definition.append(type.toUpperCase());

        if (size != null) {
            if (decimalDigits != null) {
                definition.append("(").append(size).append(",").append(decimalDigits).append(")");
            } else {
                definition.append("(").append(size).append(")");
            }
        }

        if (!nullable) {
            definition.append(" NOT NULL");
        }

        if (defaultValue != null && !defaultValue.isEmpty()) {
            definition.append(" DEFAULT ").append(defaultValue);
        }

        if (autoIncrement) {
            definition.append(" AUTO_INCREMENT");
        }

        if (comment != null && !comment.isEmpty()) {
            definition.append(" COMMENT '").append(comment).append("'");
        }

        return definition.toString();
    }

    public boolean equals(ColumnMetadata other) {
        if (other == null) {
            return false;
        }

        return name.equals(other.name) &&
                type.equals(other.type) &&
                ((size == null && other.size == null) || (size != null && size.equals(other.size))) &&
                ((decimalDigits == null && other.decimalDigits == null) || (decimalDigits != null && decimalDigits.equals(other.decimalDigits))) &&
                nullable == other.nullable &&
                ((defaultValue == null && other.defaultValue == null) || (defaultValue != null && defaultValue.equals(other.defaultValue))) &&
                autoIncrement == other.autoIncrement &&
                ((comment == null && other.comment == null) || (comment != null && comment.equals(other.comment))) &&
                primaryKey == other.primaryKey &&
                unique == other.unique &&
                ((enumValues == null && other.enumValues == null) || (enumValues != null && enumValues.equals(other.enumValues)));
    }

    public boolean hasChanged(ColumnMetadata other) {
        return !equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColumnMetadata that = (ColumnMetadata) o;

        if (nullable != that.nullable) return false;
        if (unique != that.unique) return false;
        if (primaryKey != that.primaryKey) return false;
        if (length != null ? !length.equals(that.length) : that.length != null) return false;
        if (precision != null ? !precision.equals(that.precision) : that.precision != null) return false;
        if (scale != null ? !scale.equals(that.scale) : that.scale != null) return false;
        if (insertable != that.insertable) return false;
        if (updatable != that.updatable) return false;
        if (autoIncrement != that.autoIncrement) return false;
        if (!columnName.equals(that.columnName)) return false;
        if (!type.equals(that.type)) return false;
        if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null)
            return false;
        if (columnDefinition != null ? !columnDefinition.equals(that.columnDefinition) : that.columnDefinition != null)
            return false;
        if (referencedTable != null ? !referencedTable.equals(that.referencedTable) : that.referencedTable != null)
            return false;
        if (referencedColumn != null ? !referencedColumn.equals(that.referencedColumn) : that.referencedColumn != null)
            return false;
        if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
        if (isForeignKey != that.isForeignKey) return false;
        if (generationType != that.generationType) return false;
        if (sequenceName != null ? !sequenceName.equals(that.sequenceName) : that.sequenceName != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (size != null ? !size.equals(that.size) : that.size != null) return false;
        if (decimalDigits != null ? !decimalDigits.equals(that.decimalDigits) : that.decimalDigits != null) return false;
        if (enumValues != null ? !enumValues.equals(that.enumValues) : that.enumValues != null) return false;
        if (schema != null ? !schema.equals(that.schema) : that.schema != null) return false;
        if (catalog != null ? !catalog.equals(that.catalog) : that.catalog != null) return false;
        return tableName != null ? tableName.equals(that.tableName) : that.tableName == null;
    }

    @Override
    public int hashCode() {
        int result = columnName.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (nullable ? 1 : 0);
        result = 31 * result + (unique ? 1 : 0);
        result = 31 * result + (primaryKey ? 1 : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (length != null ? length : 0);
        result = 31 * result + (precision != null ? precision : 0);
        result = 31 * result + (scale != null ? scale : 0);
        result = 31 * result + (insertable ? 1 : 0);
        result = 31 * result + (updatable ? 1 : 0);
        result = 31 * result + (autoIncrement ? 1 : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (isForeignKey ? 1 : 0);
        result = 31 * result + (generationType != null ? generationType.hashCode() : 0);
        result = 31 * result + (sequenceName != null ? sequenceName.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (size != null ? size : 0);
        result = 31 * result + (decimalDigits != null ? decimalDigits : 0);
        result = 31 * result + (enumValues != null ? enumValues.hashCode() : 0);
        result = 31 * result + (schema != null ? schema.hashCode() : 0);
        result = 31 * result + (catalog != null ? catalog.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getLength() {
        return length != null ? length : 0;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getPrecision() {
        return precision != null ? precision : 0;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale != null ? scale : 0;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public boolean isInsertable() {
        return insertable;
    }

    public void setInsertable(boolean insertable) {
        this.insertable = insertable;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public String getReferencedTable() {
        return referencedTable;
    }

    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    public String getReferencedColumn() {
        return referencedColumn;
    }

    public void setReferencedColumn(String referencedColumn) {
        this.referencedColumn = referencedColumn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSize() {
        return size != null ? size : 0;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getDecimalDigits() {
        return decimalDigits != null ? decimalDigits : 0;
    }

    public void setDecimalDigits(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    /**
     * Custom builder that provides additional methods for convenience.
     */
    public static class ColumnMetadataBuilder {
        /**
         * Sets the type field based on the Class type.
         * @param fieldType the class representing the field type
         * @return the builder instance
         */
        public ColumnMetadataBuilder fieldType(Class<?> fieldType) {
            return this.type(fieldType.getName());
        }
    }
}