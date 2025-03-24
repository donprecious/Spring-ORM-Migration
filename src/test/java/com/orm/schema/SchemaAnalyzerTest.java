package com.orm.schema;

import com.orm.example.Department;
import com.orm.example.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaAnalyzerTest {

    @Mock
    private MetadataExtractor metadataExtractor;

    private SchemaAnalyzer schemaAnalyzer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        schemaAnalyzer = new SchemaAnalyzer(metadataExtractor);
    }

    @Test
    void shouldAnalyzeBasicEntity() {
        // Given
        Set<Class<?>> entityClasses = new HashSet<>();
        entityClasses.add(BasicEntity.class);

        // When
        List<TableMetadata> tables = schemaAnalyzer.analyzeEntities(entityClasses);

        // Then
        assertEquals(1, tables.size());
        TableMetadata table = tables.get(0);
        assertEquals("basic_entity", table.getTableName());
        // Skip the class name check as it's no longer stored
        
        // Verify columns
        assertEquals(3, table.getColumns().size());
        Optional<ColumnMetadata> primaryKey = table.getColumns().stream()
                .filter(ColumnMetadata::isPrimaryKey)
                .findFirst();
        assertTrue(primaryKey.isPresent());
        assertEquals("id", primaryKey.get().getName());
        
        // Verify the name column
        Optional<ColumnMetadata> nameColumnOpt = table.getColumns().stream()
                .filter(col -> "name".equals(col.getName()))
                .findFirst();
        assertTrue(nameColumnOpt.isPresent());
        ColumnMetadata nameColumn = nameColumnOpt.get();
        assertTrue(nameColumn.isString());
        assertEquals(100, nameColumn.getLength());
        assertFalse(nameColumn.isNullable());
    }

    @Test
    void shouldAnalyzeEntityWithRelationships() {
        // Given
        Set<Class<?>> entityClasses = new HashSet<>();
        entityClasses.add(ParentEntity.class);

        // When
        List<TableMetadata> tables = schemaAnalyzer.analyzeEntities(entityClasses);

        // Then
        assertEquals(1, tables.size());
        TableMetadata table = tables.get(0);
        
        // Verify relationships
        assertFalse(table.getRelationships().isEmpty());
        RelationshipMetadata oneToMany = table.getRelationships().get(0);
        assertEquals(RelationshipMetadata.RelationshipType.ONE_TO_MANY, oneToMany.getType());
        assertEquals("children", oneToMany.getSourceField());
    }

    @Test
    void shouldAnalyzeEntityWithIndexes() {
        // Given
        Set<Class<?>> entityClasses = new HashSet<>();
        entityClasses.add(IndexedEntity.class);

        // When
        List<TableMetadata> tables = schemaAnalyzer.analyzeEntities(entityClasses);

        // Then
        assertEquals(1, tables.size());
        TableMetadata table = tables.get(0);
        
        // Verify indexes
        assertFalse(table.getIndexes().isEmpty());
        IndexMetadata index = table.getIndexes().iterator().next();
        assertEquals("idx_email", index.getName());
        assertTrue(index.isUnique());
        assertTrue(index.getColumnNames().contains("email"));
    }

    @Test
    void shouldAnalyzeInheritedFields() {
        // Given
        Set<Class<?>> entityClasses = new HashSet<>();
        entityClasses.add(ChildEntity.class);

        // When
        List<TableMetadata> tables = schemaAnalyzer.analyzeEntities(entityClasses);

        // Then
        assertEquals(1, tables.size());
        TableMetadata table = tables.get(0);
        
        // Verify inherited fields are present
        assertTrue(table.getColumns().stream().anyMatch(col -> "id".equals(col.getName()))); // from parent
        assertTrue(table.getColumns().stream().anyMatch(col -> "name".equals(col.getName()))); // from parent
        assertTrue(table.getColumns().stream().anyMatch(col -> "child_field".equals(col.getName()))); // from child
    }

    // Test entities
    @Entity
    @Table(name = "basic_entity")
    static class BasicEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "name", length = 100, nullable = false)
        private String name;

        @Column(name = "active")
        private boolean active;
    }

    @Entity
    @Table(name = "parent_entity")
    static class ParentEntity {
        @Id
        private Long id;

        @OneToMany(mappedBy = "parent")
        private List<ChildEntity> children;
    }

    @Entity
    @Table(name = "indexed_entity", 
           indexes = @Index(name = "idx_email", columnList = "email", unique = true))
    static class IndexedEntity {
        @Id
        private Long id;

        @Column(name = "email")
        private String email;
    }

    @MappedSuperclass
    static class BaseEntity {
        @Id
        protected Long id;

        @Column(name = "name")
        protected String name;
    }

    @Entity
    @Table(name = "child_entity")
    static class ChildEntity extends BaseEntity {
        @Column(name = "child_field")
        private String childField;
    }
} 