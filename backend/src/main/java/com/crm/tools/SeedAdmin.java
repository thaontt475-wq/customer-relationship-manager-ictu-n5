package com.crm.tools;

import com.crm.config.DatabaseConfig;
import com.crm.util.PasswordUtil;
import java.sql.*;

/** Explicit development bootstrap; never resets an existing account. */
public final class SeedAdmin {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConfig.getConnection()) {
            try (PreparedStatement existing = connection.prepareStatement("""
                    SELECT u.id FROM users u JOIN user_roles ur ON ur.user_id = u.id
                    JOIN roles r ON r.id = ur.role_id WHERE u.status = 'ACTIVE' AND r.code = 'ADMIN' LIMIT 1
                    """); ResultSet rs = existing.executeQuery()) {
                if (rs.next()) {
                    System.out.println("ACTIVE ADMIN already exists; seed skipped.");
                    return;
                }
            }
            String email = System.getenv("CRM_SEED_ADMIN_EMAIL");
            String password = System.getenv("CRM_SEED_ADMIN_PASSWORD");
            if (email == null || email.isBlank() || password == null || password.length() < 8
                    || !password.matches("(?s).*[A-Z].*") || !password.matches("(?s).*[a-z].*")
                    || !password.matches("(?s).*[0-9].*")) {
                throw new IllegalArgumentException("Set CRM_SEED_ADMIN_EMAIL and a strong CRM_SEED_ADMIN_PASSWORD in ENV");
            }
            connection.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO users(full_name, email, password_hash, status, data_scope) VALUES ('CRM Administrator', ?, ?, 'ACTIVE', 'ALL')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, email.trim().toLowerCase(java.util.Locale.ROOT));
                    insert.setString(2, PasswordUtil.hash(password));
                    insert.executeUpdate();
                    try (ResultSet rs = insert.getGeneratedKeys()) { rs.next(); id = rs.getLong(1); }
                }
                try (PreparedStatement assign = connection.prepareStatement(
                        "INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE code = 'ADMIN'")) {
                    assign.setLong(1, id);
                    if (assign.executeUpdate() != 1) throw new SQLException("ADMIN role missing");
                }
                connection.commit();
                System.out.println("Development administrator created; credentials are not logged.");
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
