package com.orm.schema;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class Schema {
    private List<TableMetadata> tables = new ArrayList<>();
    private String name;
    private String catalog;

    public void addTable(TableMetadata table) {
        tables.add(table);
    }

    public Optional<TableMetadata> getTable(String tableName) {
        return tables.stream()
                .filter(t -> t.getTableName().equals(tableName))
                .findFirst();
    }

    public boolean hasTable(String tableName) {
        return tables.stream()
                .anyMatch(t -> t.getTableName().equals(tableName));
    }

    public List<TableMetadata> getTables() {
        return tables;
    }

    public void setTables(List<TableMetadata> tables) {
        this.tables = tables;
    }
} 