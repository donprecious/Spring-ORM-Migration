package com.orm.spring;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.ReflectionUtils;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityScannerTest {

    private EntityScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new EntityScanner();
    }

    @Test
    void shouldScanEntitiesInPackage() {
        // Act
        Set<Class<?>> entities = scanner.scanEntities("com.orm.spring.EntityScannerTest");

        // Assert
        assertEquals(2, entities.size());
        assertTrue(entities.contains(TestEntity.class));
        assertTrue(entities.contains(AnotherTestEntity.class));
    }

    @Test
    void shouldIgnoreNonEntityClasses() {
        // Act
        Set<Class<?>> entities = scanner.scanEntities("com.orm.spring.EntityScannerTest");

        // Assert
        assertFalse(entities.contains(NonEntityClass.class));
    }

    @Test
    void shouldHandleEmptyPackage() {
        // Act
        Set<Class<?>> entities = scanner.scanEntities("com.orm.spring.empty");

        // Assert
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldHandleInvalidPackage() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            scanner.scanEntities("invalid.package.name"));
    }

    @Test
    void shouldHandleNestedEntities() {
        // Act
        Set<Class<?>> entities = scanner.scanEntities("com.orm.spring.EntityScannerTest");

        // Assert
        assertTrue(entities.contains(OuterEntity.NestedEntity.class));
    }

    @Test
    void shouldHandleInheritedEntities() {
        // Act
        Set<Class<?>> entities = scanner.scanEntities("com.orm.spring.EntityScannerTest");

        // Assert
        assertTrue(entities.contains(ChildEntity.class));
    }

    // Test entities
    @Entity
    @Table(name = "test_entity")
    private static class TestEntity {
        @Id
        private Long id;
        private String name;
    }

    @Entity
    @Table(name = "another_test_entity")
    private static class AnotherTestEntity {
        @Id
        private Long id;
        private String description;
    }

    private static class NonEntityClass {
        private String someField;
    }

    @Entity
    private static class OuterEntity {
        @Id
        private Long id;

        @Entity
        private static class NestedEntity {
            @Id
            private Long id;
        }
    }

    @Entity
    private static class BaseEntity {
        @Id
        private Long id;
    }

    @Entity
    private static class ChildEntity extends BaseEntity {
        private String additionalField;
    }
} 