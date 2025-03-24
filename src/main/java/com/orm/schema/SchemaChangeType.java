package com.orm.schema;

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
    ADD_FOREIGN_KEY,
    DROP_FOREIGN_KEY
} 