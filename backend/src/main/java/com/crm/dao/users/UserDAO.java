package com.crm.dao.users;

import com.crm.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public User findByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, full_name, phone, status FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setFullName(rs.getString("full_name"));
                    user.setPhone(rs.getString("phone"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        }
        return null;
    }

    public void updatePasswordHash(Connection conn, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }
}
