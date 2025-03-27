package com.orm.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "orm.migration")
public class MigrationProperties {
    private String basePackage;
    private String migrationsPath = "migrations";
    private String dialect = "mysql";
    private boolean autoMigrate = false;
    private boolean validateOnStartup = true;
    private boolean generateSql = true;
    private boolean backupBeforeMigration = true;
    private String backupPath = "backups";
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private int batchSize = 100;
    private int lockTimeout = 60;
    private boolean failOnMissingMigrations = true;
    private boolean cleanDisabled = true;
    private String placeholderPrefix = "${";
    private String placeholderSuffix = "}";
    private Map<String, String> placeholders = new HashMap<>();
} 