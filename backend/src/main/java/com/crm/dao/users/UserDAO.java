package com.crm.dao.users;

import com.crm.model.users.User;
import com.crm.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public boolean existsByEmail(String email, Long excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)";

        if (excludeId != null) {
            sql += " AND id <> ?";
        }

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, email);

            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public long create(User user) throws SQLException {
        String sql =
                "INSERT INTO users " +
                "(email, password_hash, full_name, phone, team_id, active) " +
                "VALUES (?, ?, ?, ?, ?, TRUE)";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getPhone());

            if (user.getTeamId() == null) {
                ps.setNull(5, Types.BIGINT);
            } else {
                ps.setLong(5, user.getTeamId());
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Không lấy được ID User vừa tạo");
    }

    public Optional<User> findById(long id) throws SQLException {
        String sql =
                "SELECT id,email,password_hash,full_name,phone,team_id,active " +
                "FROM users WHERE id = ?";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql =
                "SELECT id,email,password_hash,full_name,phone,team_id,active " +
                "FROM users WHERE LOWER(email) = LOWER(?)";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<User> search(
            String keyword,
            Boolean active,
            int page,
            int size
    ) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT id,email,password_hash,full_name,phone,team_id,active " +
                "FROM users WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(
                    "AND (LOWER(email) LIKE LOWER(?) " +
                    "OR LOWER(full_name) LIKE LOWER(?)) "
            );

            String value = "%" + keyword.trim() + "%";
            params.add(value);
            params.add(value);
        }

        if (active != null) {
            sql.append("AND active = ? ");
            params.add(active);
        }

        sql.append("ORDER BY id DESC LIMIT ? OFFSET ?");

        params.add(size);
        params.add((page - 1) * size);

        List<User> users = new ArrayList<>();

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }

        return users;
    }

    public int count(String keyword, Boolean active) throws SQLException {
        StringBuilder sql =
                new StringBuilder("SELECT COUNT(*) FROM users WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(
                    "AND (LOWER(email) LIKE LOWER(?) " +
                    "OR LOWER(full_name) LIKE LOWER(?)) "
            );

            String value = "%" + keyword.trim() + "%";
            params.add(value);
            params.add(value);
        }

        if (active != null) {
            sql.append("AND active = ? ");
            params.add(active);
        }

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean update(User user) throws SQLException {
        String sql =
                "UPDATE users SET " +
                "email=?, full_name=?, phone=?, team_id=?, " +
                "updated_at=CURRENT_TIMESTAMP " +
                "WHERE id=?";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getPhone());

            if (user.getTeamId() == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, user.getTeamId());
            }

            ps.setLong(5, user.getId());

            return ps.executeUpdate() > 0;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));

        long teamId = rs.getLong("team_id");

        if (!rs.wasNull()) {
            user.setTeamId(teamId);
        }

        user.setActive(rs.getBoolean("active"));

        return user;
    }
}
