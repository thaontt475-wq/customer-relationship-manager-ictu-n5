package com.crm.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {

    private static final HikariDataSource DATA_SOURCE;

    static {
        try {
            Properties properties = new Properties();

            try (InputStream input =
                         DBConnection.class
                                 .getClassLoader()
                                 .getResourceAsStream("db.properties")) {

                if (input != null) {
                    properties.load(input);
                }
            }

            String url = envOrDefault(
                    "DB_URL",
                    properties.getProperty(
                            "db.url",
                            "jdbc:postgresql://localhost:5432/crm_db"
                    )
            );

            String username = envOrDefault(
                    "DB_USERNAME",
                    properties.getProperty("db.username", "postgres")
            );

            String password = envOrDefault(
                    "DB_PASSWORD",
                    properties.getProperty("db.password", "")
            );

            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);

            config.setDriverClassName("org.postgresql.Driver");

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            config.setPoolName("CRM-HikariPool");

            DATA_SOURCE = new HikariDataSource(config);

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection()
            throws SQLException {

        return DATA_SOURCE.getConnection();
    }

    private static String envOrDefault(
            String name,
            String defaultValue) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}