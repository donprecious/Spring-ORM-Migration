package com.demo.todo.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class SQLiteConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName(properties.getDriverClassName())
                .url(properties.getUrl())
                .build();

        // Create the database file if it doesn't exist
        try (Connection conn = dataSource.getConnection()) {
            // The connection will create the file if it doesn't exist
            System.out.println("SQLite database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing SQLite database: " + e.getMessage());
        }

        return dataSource;
    }
} 