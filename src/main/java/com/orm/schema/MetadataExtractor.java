package com.orm.schema;

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for extracting metadata from entity classes.
 */
@Slf4j
@Component
public class MetadataExtractor {
    public TableMetadata extractTableMetadata(Class<?> entityClass) {
        log.debug("Extracting table metadata for entity: {}", entityClass.getName());
        TableMetadata metadata = new TableMetadata();
        metadata.setClassName(entityClass.getName());

        // Extract table name
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && StringUtils.hasText(tableAnnotation.name())) {
            metadata.setTableName(tableAnnotation.name());
        } else {
            metadata.setTableName(entityClass.getSimpleName().toLowerCase());
        }

        // Extract schema and catalog
        if (tableAnnotation != null) {
            if (StringUtils.hasText(tableAnnotation.schema())) {
                metadata.setSchema(tableAnnotation.schema());
            }
            if (StringUtils.hasText(tableAnnotation.catalog())) {
                metadata.setCatalog(tableAnnotation.catalog());
            }
        }

        // Extract columns
        extractColumns(entityClass, metadata);

        log.debug("Extracted table metadata: {}", metadata);
        return metadata;
    }

    private void extractColumns(Class<?> entityClass, TableMetadata metadata) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class)) {
                ColumnMetadata column = extractColumnMetadata(field);
                metadata.addColumn(column);

                if (field.isAnnotationPresent(Id.class)) {
                    column.setPrimaryKey(true);
                    column.setNullable(false);
                }
            }
        }
    }

    private ColumnMetadata extractColumnMetadata(Field field) {
        Column columnAnn = field.getAnnotation(Column.class);
        ColumnMetadata column = new ColumnMetadata();
        
        if (columnAnn != null) {
            column.setColumnName(columnAnn.name().isEmpty() ? field.getName() : columnAnn.name());
            column.setNullable(columnAnn.nullable());
            column.setUnique(columnAnn.unique());
            column.setLength(columnAnn.length());
            column.setPrecision(columnAnn.precision());
            column.setScale(columnAnn.scale());
            column.setInsertable(columnAnn.insertable());
            column.setUpdatable(columnAnn.updatable());
            column.setColumnDefinition(columnAnn.columnDefinition());
        } else {
            column.setColumnName(field.getName());
        }
        
        Id idAnn = field.getAnnotation(Id.class);
        if (idAnn != null) {
            column.setPrimaryKey(true);
            
            GeneratedValue genValue = field.getAnnotation(GeneratedValue.class);
            if (genValue != null) {
                column.setAutoIncrement(genValue.strategy() == GenerationType.IDENTITY || 
                                       genValue.strategy() == GenerationType.AUTO);
                column.setGenerationType(genValue.strategy());
            }
        }
        
        // Set type from field
        column.setType(field.getType().getName());
        column.setFieldName(field.getName());
        
        // Extract foreign key information
        if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null) {
                column.setForeignKey(true);
                column.setColumnName(joinColumn.name());
                column.setReferencedTable(field.getType().getSimpleName().toLowerCase());
                column.setReferencedColumn("id"); // Default to id for simplicity
            }
        }
        
        return column;
    }
} 