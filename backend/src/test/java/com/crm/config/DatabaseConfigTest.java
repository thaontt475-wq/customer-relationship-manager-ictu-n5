package com.crm.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {

    public static class MockDriver implements Driver {
        static {
            try {
                DriverManager.registerDriver(new MockDriver());
            } catch (SQLException ignored) {}
        }

        @Override
        public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) return null;
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        Class<?> ret = method.getReturnType();
                        if ("getTransactionIsolation".equals(name)) return Connection.TRANSACTION_READ_COMMITTED;
                        if ("getAutoCommit".equals(name)) return true;
                        if ("isClosed".equals(name)) return false;
                        if ("isValid".equals(name)) return true;
                        if (ret == boolean.class) return false;
                        if (ret == int.class) return 0;
                        if ("getMetaData".equals(name)) {
                            return Proxy.newProxyInstance(
                                    DatabaseMetaData.class.getClassLoader(),
                                    new Class<?>[]{DatabaseMetaData.class},
                                    (p2, m2, a2) -> {
                                        Class<?> ret2 = m2.getReturnType();
                                        if (ret2 == boolean.class) return false;
                                        if (ret2 == int.class) return 4;
                                        return null;
                                    }
                            );
                        }
                        if ("createStatement".equals(name) || "prepareStatement".equals(name)) {
                            return Proxy.newProxyInstance(
                                    PreparedStatement.class.getClassLoader(),
                                    new Class<?>[]{PreparedStatement.class},
                                    (p3, m3, a3) -> {
                                        Class<?> ret3 = m3.getReturnType();
                                        if (ret3 == boolean.class) return false;
                                        if (ret3 == int.class) return 0;
                                        return null;
                                    }
                            );
                        }
                        return null;
                    }
            );
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:mock:");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() { return 1; }

        @Override
        public int getMinorVersion() { return 0; }

        @Override
        public boolean jdbcCompliant() { return true; }

        @Override
        public Logger getParentLogger() { return Logger.getGlobal(); }
    }

    @BeforeEach
    @AfterEach
    public void cleanup() {
        System.clearProperty("crm.pool.enabled");
        System.clearProperty("catalina.base");
        System.clearProperty("db.driver");
        DatabaseConfig.closePool();
    }

    @Test
    public void testIsContainerManagedDetection() throws Exception {
        Method isManaged = DatabaseConfig.class.getDeclaredMethod("isContainerManaged");
        isManaged.setAccessible(true);

        assertFalse((Boolean) isManaged.invoke(null), "By default, should not be container-managed in tests");

        System.setProperty("crm.pool.enabled", "true");
        assertTrue((Boolean) isManaged.invoke(null), "Should be container-managed when crm.pool.enabled=true");

        System.clearProperty("crm.pool.enabled");
        System.setProperty("catalina.base", "C:/mock/tomcat");
        assertTrue((Boolean) isManaged.invoke(null), "Should be container-managed when catalina.base is present");
    }

    @Test
    public void testHikariPoolConfiguration() throws Exception {
        System.setProperty("db.driver", MockDriver.class.getName());
        Method getDsMethod = DatabaseConfig.class.getDeclaredMethod(
                "getDataSource", String.class, String.class, String.class
        );
        getDsMethod.setAccessible(true);

        String testUrl = "jdbc:mock://testdb";
        HikariDataSource ds = (HikariDataSource) getDsMethod.invoke(null, testUrl, "testuser", "testpass");

        assertNotNull(ds, "DataSource should not be null");
        assertEquals("NovaCRM-HikariPool", ds.getPoolName());
        assertEquals(30, ds.getMaximumPoolSize());
        assertEquals(5, ds.getMinimumIdle());
        assertEquals(30000, ds.getIdleTimeout());
        assertEquals(600000, ds.getMaxLifetime());
        assertEquals(10000, ds.getConnectionTimeout());

        HikariDataSource ds2 = (HikariDataSource) getDsMethod.invoke(null, testUrl, "testuser", "testpass");
        assertSame(ds, ds2, "Subsequent calls with same URL/user should return same DataSource instance");

        DatabaseConfig.closePool();
        Field dsField = DatabaseConfig.class.getDeclaredField("dataSource");
        dsField.setAccessible(true);
        assertNull(dsField.get(null), "dataSource field should be null after closePool");
        assertTrue(ds.isClosed(), "HikariDataSource should be closed");
    }
}
