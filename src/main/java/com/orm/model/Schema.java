package com.orm.model;

import com.orm.schema.TableMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schema {
    private List<TableMetadata> tables;

    public Optional<TableMetadata> getTable(String tableName) {
        if (tables == null) {
            return Optional.empty();
        }
        return tables.stream()
                .filter(t -> t.getTableName().equals(tableName))
                .findFirst();
    }

    public boolean hasTable(String tableName) {
        if (tables == null) {
            return false;
        }
        return tables.stream()
                .anyMatch(t -> t.getTableName().equals(tableName));
    }

    public void addTable(TableMetadata table) {
        if (tables == null) {
            tables = new ArrayList<>();
        }
        tables.add(table);
    }

    public void removeTable(String tableName) {
        if (tables != null) {
            tables.removeIf(t -> t.getTableName().equalsIgnoreCase(tableName));
        }
    }

    public List<TableMetadata> getTables() {
        return tables != null ? tables : new ArrayList<>();
    }
} 