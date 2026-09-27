package com.crm.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {
    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in == null) throw new SQLException("db.properties not found on classpath");
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("Failed to load db.properties", e);
        }
        String url = System.getenv().getOrDefault("CRM_DB_URL", props.getProperty("db.url"));
        String username = System.getenv().getOrDefault("CRM_DB_USERNAME", props.getProperty("db.username"));
        String password = System.getenv().getOrDefault("CRM_DB_PASSWORD", props.getProperty("db.password"));
        if (url == null || url.isBlank() || !url.startsWith("jdbc:mysql:"))
            throw new SQLException("Missing or invalid db.url / CRM_DB_URL: expected jdbc:mysql URL");
        if (username == null || username.isBlank())
            throw new SQLException("Missing db.username / CRM_DB_USERNAME");
        if (password == null)
            throw new SQLException("Missing db.password / CRM_DB_PASSWORD (empty allowed for local use)");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver missing from application classpath", e);
        }
        return DriverManager.getConnection(url, username, password);
    }
}
