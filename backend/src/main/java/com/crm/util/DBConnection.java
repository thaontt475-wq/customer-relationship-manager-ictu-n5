package com.crm.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final String PROPERTIES_FILE = "/db.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                PROPS.load(in);
            } else {
                throw new ExceptionInInitializerError("db.properties not found on classpath");
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load db.properties: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("CRM_DB_URL", PROPS.getProperty("db.url"));
        String username = System.getenv().getOrDefault("CRM_DB_USERNAME", PROPS.getProperty("db.username"));
        String password = System.getenv().getOrDefault("CRM_DB_PASSWORD", PROPS.getProperty("db.password"));
        return DriverManager.getConnection(url, username, password);
    }
}
