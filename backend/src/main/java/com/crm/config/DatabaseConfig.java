package com.crm.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConfig {

    private static final Properties PROPERTIES = new Properties();
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/crm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "crm_user";

    private static volatile HikariDataSource dataSource;
    private static String currentPoolUrl;
    private static String currentPoolUser;

    static {
        try (
                InputStream input =
                        DatabaseConfig.class
                                .getClassLoader()
                                .getResourceAsStream("db.properties")
        ) {
            if (input != null) {
                PROPERTIES.load(input);
            }
            String driver = getPropertyOrEnv("db.driver", null, DEFAULT_DRIVER);
            Class.forName(driver);
        } catch (Exception e) {
            try {
                Class.forName(DEFAULT_DRIVER);
            } catch (ClassNotFoundException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    private DatabaseConfig() {
    }

    private static String getPropertyOrEnv(String propKey, String envKey, String fallback) {
        // 1. System property (-Dkey=value)
        if (propKey != null) {
            String sysVal = System.getProperty(propKey);
            if (isValidValue(sysVal)) {
                return sysVal.trim();
            }
        }
        // 2. Environment variable
        if (envKey != null) {
            String envVal = System.getenv(envKey);
            if (isValidValue(envVal)) {
                return envVal.trim();
            }
        }
        // 3. Loaded properties file
        if (propKey != null) {
            String fileVal = PROPERTIES.getProperty(propKey);
            if (isValidValue(fileVal)) {
                return fileVal.trim();
            }
        }
        return fallback;
    }

    private static boolean isValidValue(String val) {
        return val != null && !val.isBlank() && !val.startsWith("${");
    }

    public static String getUrl() {
        String sys = System.getProperty("crm.db.url");
        if (!isValidValue(sys)) sys = System.getProperty("db.url");
        if (isValidValue(sys)) return sys.trim();
        return getPropertyOrEnv("crm.db.url", "CRM_DB_URL", PROPERTIES.getProperty("db.url", DEFAULT_URL));
    }

    public static String getUsername() {
        String sys = System.getProperty("crm.db.username");
        if (!isValidValue(sys)) sys = System.getProperty("db.username");
        if (isValidValue(sys)) return sys.trim();
        return getPropertyOrEnv("crm.db.username", "CRM_DB_USERNAME", PROPERTIES.getProperty("db.username", DEFAULT_USER));
    }

    public static String getPassword() {
        String sys = System.getProperty("crm.db.password");
        if (!isValidValue(sys)) sys = System.getProperty("db.password");
        if (isValidValue(sys)) return sys.trim();
        return getPropertyOrEnv("crm.db.password", "CRM_DB_PASSWORD", PROPERTIES.getProperty("db.password", ""));
    }

    private static HikariDataSource getDataSource(String url, String username, String password) {
        if (dataSource == null || !url.equals(currentPoolUrl) || !username.equals(currentPoolUser)) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null || !url.equals(currentPoolUrl) || !username.equals(currentPoolUser)) {
                    if (dataSource != null && !dataSource.isClosed()) {
                        dataSource.close();
                    }
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(url);
                    config.setUsername(username);
                    config.setPassword(password != null ? password : "");
                    config.setDriverClassName(getPropertyOrEnv("db.driver", null, DEFAULT_DRIVER));
                    config.setMaximumPoolSize(30);
                    config.setMinimumIdle(5);
                    config.setIdleTimeout(30000);
                    config.setMaxLifetime(600000);
                    config.setConnectionTimeout(10000);
                    config.setPoolName("NovaCRM-HikariPool");

                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.addDataSourceProperty("useServerPrepStmts", "true");

                    dataSource = new HikariDataSource(config);
                    currentPoolUrl = url;
                    currentPoolUser = username;
                }
            }
        }
        return dataSource;
    }

    private static boolean isContainerManaged() {
        return System.getProperty("catalina.base") != null
                || "true".equalsIgnoreCase(System.getProperty("crm.pool.enabled"))
                || "true".equalsIgnoreCase(System.getenv("CRM_POOL_ENABLED"));
    }

    public static Connection getConnection() throws SQLException {
        String url = getUrl();
        String username = getUsername();
        String password = getPassword();

        try {
            if (isContainerManaged()) {
                return getDataSource(url, username, password).getConnection();
            }
            return java.sql.DriverManager.getConnection(url, username, password != null ? password : "");
        } catch (SQLException e) {
            String msg = e.getMessage();
            int errCode = e.getErrorCode();
            if (errCode == 1045 || (msg != null && msg.contains("Access denied"))) {
                throw new SQLException(
                        "Lỗi xác thực MySQL: Tài khoản '" + username + "' bị từ chối kết nối. "
                        + "Vui lòng cấu hình biến môi trường CRM_DB_USERNAME và CRM_DB_PASSWORD "
                        + "hoặc chạy scripts/setup-dev.ps1. Xem hướng dẫn tại SETUP_TEAM.md. Gốc: " + msg,
                        e.getSQLState(),
                        e.getErrorCode(),
                        e
                );
            }
            throw e;
        }
    }

    public static synchronized void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }
}

