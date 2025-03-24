package com.orm.spring;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans the classpath for JPA entity classes.
 */
@Slf4j
@Component
public class EntityScanner {
    /**
     * Scans for entity classes in the specified base package.
     *
     * @param basePackage The base package to scan
     * @return Set of discovered entity classes
     */
    public Set<Class<?>> scanEntities(String basePackage) {
        log.debug("Scanning for entities in package: {}", basePackage);

        if (!StringUtils.hasText(basePackage)) {
            throw new IllegalArgumentException("Base package must be specified");
        }

        Set<Class<?>> entities = new HashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = createEntityScanner();

        try {
            for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
                Class<?> entityClass = loadClass(bd.getBeanClassName());
                entities.add(entityClass);
                log.debug("Found entity class: {}", entityClass.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan for entities", e);
        }

        log.info("Found {} entities in package {}", entities.size(), basePackage);
        return entities;
    }

    private ClassPathScanningCandidateComponentProvider createEntityScanner() {
        ClassPathScanningCandidateComponentProvider scanner = 
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        return scanner;
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load entity class: " + className, e);
        }
    }
} 