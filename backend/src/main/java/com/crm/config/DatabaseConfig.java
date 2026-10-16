package com.crm.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConfig {

    private static final Properties PROPERTIES =
            new Properties();

    static {
        try (
                InputStream input =
                        DatabaseConfig.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        "db.properties"
                                )
        ) {

            if (input == null) {
                throw new RuntimeException(
                        "Không tìm thấy db.properties"
                );
            }

            PROPERTIES.load(input);

            Class.forName(
                    PROPERTIES.getProperty(
                            "db.driver"
                    )
            );

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConfig() {
    }

    public static Connection getConnection()
            throws SQLException {

        String envPassword =
                System.getenv("CRM_DB_PASSWORD");

        String password =
                envPassword != null
                        ? envPassword
                        : PROPERTIES.getProperty(
                                "db.password",
                                ""
                        );

        return DriverManager.getConnection(
                System.getenv("CRM_DB_URL") != null
                        ? System.getenv("CRM_DB_URL")
                        : PROPERTIES.getProperty("db.url"),
                System.getenv("CRM_DB_USERNAME") != null
                        ? System.getenv("CRM_DB_USERNAME")
                        : PROPERTIES.getProperty("db.username"),
                password
        );
    }
}
