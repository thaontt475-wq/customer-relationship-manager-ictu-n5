package com.crm.tools;

import com.crm.config.DatabaseConfig;
import java.sql.*;

/** Read-only development inventory. Never reads or prints password hashes. */
public final class InspectDevelopment {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT u.id, u.email, u.status, GROUP_CONCAT(r.code ORDER BY r.code) AS roles
                 FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id
                 LEFT JOIN roles r ON r.id = ur.role_id
                 WHERE u.status <> 'DELETED' GROUP BY u.id, u.email, u.status ORDER BY u.id
                 """); ResultSet rs = statement.executeQuery()) {
            System.out.println("Database: " + connection.getCatalog());
            while (rs.next()) {
                System.out.printf("id=%d email=%s status=%s roles=%s%n", rs.getLong("id"),
                        rs.getString("email"), rs.getString("status"), rs.getString("roles"));
            }
        }
    }
}
