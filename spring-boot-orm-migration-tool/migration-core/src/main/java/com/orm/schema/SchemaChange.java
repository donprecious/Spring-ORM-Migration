package com.orm.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaChange {
    private SchemaChangeType type;
    private TableMetadata table;
    private ColumnMetadata column;
    private ColumnMetadata oldColumn;
    private IndexMetadata index;
    private ForeignKeyMetadata foreignKey;
    private String description;
}