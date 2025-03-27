package com.orm.spring;

import com.orm.migration.MigrationGenerator;
import com.orm.repository.MigrationRepository;
import com.orm.schema.MetadataExtractor;
import com.orm.schema.SchemaAnalyzer;
import com.orm.schema.SchemaComparator;
import com.orm.sql.MigrationScriptGenerator;
import com.orm.sql.MySqlDialect;
import com.orm.sql.OracleDialect;
import com.orm.sql.PostgreSqlDialect;
import com.orm.sql.SqlDialect;
import com.orm.sql.SqlDialectFactory;
import com.orm.sql.SqlServerDialect;
import com.orm.sql.SqliteDialect;
import com.orm.sql.UnsupportedDatabaseException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import javax.sql.DataSource;

/**
 * Auto-configuration for the ORM migration tool.
 * This class sets up all necessary beans and configures
 * the migration tool based on application properties.
 */
@AutoConfiguration
@EnableConfigurationProperties(OrmMigrationProperties.class)
@ComponentScan("com.orm")
public class OrmMigrationAutoConfiguration {

    /**
     * Configures the SQL dialect based on the database type.
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlDialect sqlDialect(DataSource dataSource, OrmMigrationProperties properties) {
        final String url;
        String configUrl = properties.getUrl();
        if (configUrl == null) {
            // Try to get URL from datasource if not specified in properties
            try {
                url = dataSource.getConnection().getMetaData().getURL();
            } catch (Exception e) {
                throw new IllegalStateException("Could not determine database URL", e);
            }
        } else {
            url = configUrl;
        }

        return SqlDialectFactory.createFromJdbcUrl(url)
            .orElseThrow(() -> new UnsupportedDatabaseException(
                "Unsupported database URL: " + url
            ));
    }

    /**
     * Configures the metadata extractor.
     */
    @Bean
    @ConditionalOnMissingBean
    public MetadataExtractor metadataExtractor() {
        return new MetadataExtractor();
    }

    /**
     * Configures the schema analyzer.
     */
    @Bean
    @ConditionalOnMissingBean
    public SchemaAnalyzer schemaAnalyzer(MetadataExtractor metadataExtractor) {
        return new SchemaAnalyzer(metadataExtractor);
    }

    /**
     * Configures the schema comparator.
     */
    @Bean
    @ConditionalOnMissingBean
    public SchemaComparator schemaComparator() {
        return new SchemaComparator();
    }

    /**
     * Configures the migration generator.
     */
    @Bean
    @ConditionalOnMissingBean
    public MigrationGenerator migrationGenerator(SqlDialect sqlDialect) {
        return new MigrationGenerator(sqlDialect);
    }

    /**
     * Configures the migration service.
     */
    @Bean
    @ConditionalOnMissingBean
    public MigrationService migrationService(
            SchemaAnalyzer analyzer,
            MigrationScriptGenerator scriptGenerator,
            MigrationRepository migrationRepository,
            SqlDialect sqlDialect) {
        return new MigrationService(analyzer, scriptGenerator, migrationRepository, sqlDialect);
    }

    /**
     * Configures the migration runner if auto-migration is enabled.
     */
    @Bean
    @ConditionalOnProperty(name = "orm.migration.auto", havingValue = "true")
    public MigrationRunner migrationRunner(
            MigrationService migrationService,
            OrmMigrationProperties properties) {
        return new MigrationRunner(migrationService, properties);
    }
} 