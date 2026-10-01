package com.crm.dao.users;

import com.crm.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public User findForLogin(String email) throws SQLException {
        String sql = "SELECT u.id, u.email, u.password_hash, "
                + "COALESCE(u.display_name, u.full_name) AS display_name, u.active, u.status, "
                + "r.id AS role_id, r.name AS role_name FROM users u "
                + "LEFT JOIN user_roles ur ON ur.user_id = u.id "
                + "LEFT JOIN roles r ON r.id = ur.role_id WHERE u.email = ? ORDER BY r.id";
        try (Connection conn = com.crm.util.DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                User user = null;
                List<com.crm.model.Role> roles = new ArrayList<>();
                while (rs.next()) {
                    if (user == null) {
                        user = new User();
                        user.setId(rs.getLong("id"));
                        user.setEmail(rs.getString("email"));
                        user.setPasswordHash(rs.getString("password_hash"));
                        user.setDisplayName(rs.getString("display_name"));
                        user.setActive(rs.getBoolean("active"));
                        user.setStatus(rs.getString("status"));
                    }
                    long roleId = rs.getLong("role_id");
                    if (!rs.wasNull()) roles.add(new com.crm.model.Role(roleId, rs.getString("role_name")));
                }
                if (user != null) user.setRoles(roles);
                return user;
            }
        }
    }


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

    public String findPasswordHashById(Connection conn, long userId) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("password_hash") : null;
            }
        }
    }

    public List<String> findRoleNamesByUserId(Connection conn, long userId) throws SQLException {
        String sql = "SELECT r.name FROM user_roles ur "
                + "JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? ORDER BY r.name";
        List<String> roles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString("name"));
                }
            }
        }
        return roles;
    }
    public void updatePasswordHash(Connection conn, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    public List<User> findAll(Connection conn) throws SQLException {
        String sql = "SELECT u.id, u.username, u.email, u.full_name, u.display_name, u.active, u.phone, u.signature, u.status, u.team_id, u.data_scope, "
                    + "t.name AS team_name, u.created_at, u.updated_at, u.last_login_at "
                    + "FROM users u LEFT JOIN teams t ON t.id = u.team_id ORDER BY u.full_name, u.email";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapViewUser(rs));
            }
        }
        return users;
    }

    public User findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT u.id, u.username, u.email, u.full_name, u.display_name, u.active, u.phone, u.signature, u.status, u.team_id, u.data_scope, "
                    + "t.name AS team_name, u.created_at, u.updated_at, u.last_login_at "
                    + "FROM users u LEFT JOIN teams t ON t.id = u.team_id WHERE u.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapViewUser(rs) : null;
            }
        }
    }

    public User findByIdForUpdate(Connection conn, long id) throws SQLException {
        String sql = "SELECT u.id, u.username, u.email, u.full_name, u.display_name, u.active, u.phone, u.signature, u.status, u.team_id, u.data_scope, "
                    + "t.name AS team_name, u.created_at, u.updated_at, u.last_login_at "
                    + "FROM users u LEFT JOIN teams t ON t.id = u.team_id WHERE u.id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapViewUser(rs) : null;
            }
        }
    }

    public List<User> findActiveRecipientsExcluding(Connection conn, long excludedUserId) throws SQLException {
        String sql = "SELECT u.id, u.username, u.email, u.full_name, u.display_name, u.active, u.phone, u.status, u.team_id, u.data_scope, "
                    + "t.name AS team_name, u.created_at, u.updated_at, u.last_login_at "
                    + "FROM users u LEFT JOIN teams t ON t.id = u.team_id "
                    + "WHERE u.status = 'ACTIVE' AND u.id <> ? ORDER BY u.full_name, u.email";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, excludedUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapViewUser(rs));
                }
            }
        }
        return users;
    }

    public int delete(Connection conn, long userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate();
        }
    }
    public int updateStatus(Connection conn, long userId, String expectedStatus, String newStatus)
            throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setLong(2, userId);
            stmt.setString(3, expectedStatus);
            return stmt.executeUpdate();
        }
    }

    public User findByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, full_name, phone, status "
                + "FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
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

    public boolean emailExistsExcludingUser(Connection conn, String email, long excludedUserId)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) AND id <> ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setLong(2, excludedUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean usernameExistsExcludingUser(Connection conn, String username, long excludedUserId)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) AND id <> ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setLong(2, excludedUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long create(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users "
                + "(username, email, password_hash, full_name, display_name, active, phone, status, team_id, data_scope) "
                + "VALUES (?, ?, ?, ?, ?, TRUE, ?, 'ACTIVE', ?, 'SELF')";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getFullName());
            stmt.setString(6, user.getPhone());

            if (user.getTeamId() == null) {
                stmt.setNull(7, java.sql.Types.BIGINT);
            } else {
                stmt.setLong(7, user.getTeamId());
            }

            if (stmt.executeUpdate() != 1) {
                throw new SQLException("Unable to create user");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Missing generated user id");
                }
                return keys.getLong(1);
            }
        }
    }

    public int updateProfile(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users u "
                + "LEFT JOIN teams managed ON managed.leader_user_id = u.id "
                + "SET u.username = ?, u.email = ?, u.full_name = ?, "
                + "u.display_name = ?, u.phone = ?, u.team_id = ? "
                + "WHERE u.id = ? AND (managed.id IS NULL OR managed.id = ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getPhone());

            if (user.getTeamId() == null) {
                stmt.setNull(6, java.sql.Types.BIGINT);
            } else {
                stmt.setLong(6, user.getTeamId());
            }

            stmt.setLong(7, user.getId());
            if (user.getTeamId() == null) {
                stmt.setNull(8, java.sql.Types.BIGINT);
            } else {
                stmt.setLong(8, user.getTeamId());
            }
            return stmt.executeUpdate();
        }
    }

    /**
     * Update user self profile (CRM-35).
     * ONLY updates the 3 allowed fields: full_name, phone, signature.
     * Email, team_id, and roles are NEVER modified.
     */
    public int updateUserSelfProfile(Connection conn, long userId, String fullName, String phone, String signature)
            throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, signature = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, phone);
            stmt.setString(3, signature);
            stmt.setLong(4, userId);
            return stmt.executeUpdate();
        }
    }

    public int updateUserSelfProfile(long userId, String fullName, String phone, String signature) throws SQLException {
        try (Connection conn = com.crm.util.DBConnection.getConnection()) {
            return updateUserSelfProfile(conn, userId, fullName, phone, signature);
        }
    }

    /**
     * Retrieve complete user profile including roles list (CRM-35).
     */
    public User findUserProfileWithRoles(Connection conn, long userId) throws SQLException {
        User user = findById(conn, userId);
        if (user == null) {
            return null;
        }

        String sql = "SELECT r.id, r.name FROM user_roles ur "
                + "JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = ? ORDER BY r.name";

        List<com.crm.model.Role> roles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(new com.crm.model.Role(rs.getLong("id"), rs.getString("name")));
                }
            }
        }
        user.setRoles(roles);
        return user;
    }

    public User findUserProfileWithRoles(long userId) throws SQLException {
        try (Connection conn = com.crm.util.DBConnection.getConnection()) {
            return findUserProfileWithRoles(conn, userId);
        }
    }

    public long countSearch(Connection conn, String keyword, String role, String status)
            throws SQLException {

        SearchClause clause = buildSearchClause(keyword, role, status);
        String sql = "SELECT COUNT(*) FROM users u "
                + "LEFT JOIN teams t ON t.id = u.team_id "
                + clause.sql();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindSearchParameters(stmt, clause.parameters());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public List<User> search(Connection conn, String keyword, String role, String status,
                             int limit, int offset) throws SQLException {

        SearchClause clause = buildSearchClause(keyword, role, status);

        String sql = "SELECT u.id, u.username, u.email, u.full_name, u.display_name, u.active, u.phone, u.status, "
                + "u.team_id, u.data_scope, t.name AS team_name, "
                + "u.created_at, u.updated_at, u.last_login_at, "
                + "(SELECT GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') "
                + " FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + " WHERE ur.user_id = u.id) AS role_name "
                + "FROM users u LEFT JOIN teams t ON t.id = u.team_id "
                + clause.sql()
                + " ORDER BY u.full_name, u.email LIMIT ? OFFSET ?";

        List<User> users = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = bindSearchParameters(stmt, clause.parameters());
            stmt.setInt(index++, limit);
            stmt.setInt(index, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User user = mapViewUser(rs);
                    user.setRole(rs.getString("role_name"));
                    users.add(user);
                }
            }
        }

        return users;
    }

    private SearchClause buildSearchClause(String keyword, String role, String status) {
        StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(u.full_name, '')) LIKE ?")
                    .append(" OR LOWER(u.email) LIKE ?")
                    .append(" OR LOWER(COALESCE(t.name, '')) LIKE ?)");

            String pattern = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }

        if (role != null && !role.isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM user_roles ur2 ")
                    .append("JOIN roles r2 ON r2.id = ur2.role_id ")
                    .append("WHERE ur2.user_id = u.id AND LOWER(r2.name) = LOWER(?))");
            parameters.add(role.trim());
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(u.status) = UPPER(?)");
            parameters.add(status.trim());
        }

        return new SearchClause(sql.toString(), parameters);
    }

    private int bindSearchParameters(PreparedStatement stmt, List<Object> parameters)
            throws SQLException {
        int index = 1;
        for (Object parameter : parameters) {
            stmt.setObject(index++, parameter);
        }
        return index;
    }

    private record SearchClause(String sql, List<Object> parameters) {
    }
    private User mapViewUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setDisplayName(rs.getString("display_name"));
        user.setActive(rs.getBoolean("active"));
        user.setPhone(rs.getString("phone"));
        try {
            user.setSignature(rs.getString("signature"));
        } catch (SQLException ignored) {
        }
        user.setStatus(rs.getString("status"));
        long teamId = rs.getLong("team_id");
        user.setTeamId(rs.wasNull() ? null : teamId);
        user.setTeamName(rs.getString("team_name"));
        user.setDataScope(rs.getString("data_scope"));
        user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        user.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        user.setLastLoginAt(toLocalDateTime(rs.getTimestamp("last_login_at")));
        return user;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
