package com.orm;

import com.orm.schema.SchemaAnalyzer;
import com.orm.schema.TableMetadata;
import com.orm.spring.SchemaExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

@Slf4j
@SpringBootApplication
public class OrmMigrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrmMigrationApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(SchemaAnalyzer schemaAnalyzer) {
        return (args) -> {
            log.info("Starting schema analysis...");
            List<TableMetadata> tables = schemaAnalyzer.analyzeEntities(new HashSet<>());
            
            log.info("Found {} tables:", tables.size());
            for (TableMetadata table : tables) {
                log.info("Table: {}", table.getTableName());
                log.info("  Columns: {}", table.getColumns().size());
                log.info("  Indexes: {}", table.getIndexes().size());
                log.info("  Foreign Keys: {}", table.getForeignKeys().size());
            }
        };
    }
}