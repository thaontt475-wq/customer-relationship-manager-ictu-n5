package com.crm.dao.permissions;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class PermissionDAO {

    public List<Map<String, Object>> findRoles() throws SQLException {
        List<Map<String, Object>> roles = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, code, name FROM roles ORDER BY code");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> role = new LinkedHashMap<>();
                role.put("id", rs.getLong("id"));
                role.put("code", rs.getString("code"));
                role.put("name", rs.getString("name"));
                roles.add(role);
            }
        }
        return roles;
    }

    public Set<String> findByUserId(
            long userId
    ) throws SQLException {

        String sql = """
                SELECT DISTINCT p.code
                FROM permissions p
                JOIN role_permissions rp
                    ON rp.permission_id = p.id
                JOIN user_roles ur
                    ON ur.role_id = rp.role_id
                WHERE ur.user_id = ?
                """;

        Set<String> result =
                new HashSet<>();

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {
                    result.add(
                            rs.getString("code")
                    );
                }
            }
        }

        return result;
    }

    public List<Map<String, Object>> findRolesByUserId(
            long userId
    ) throws SQLException {

        String sql = """
                SELECT r.id, r.code, r.name
                FROM roles r
                JOIN user_roles ur
                    ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.code
                """;

        List<Map<String, Object>> roles =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    Map<String, Object> role =
                            new LinkedHashMap<>();

                    role.put(
                            "id",
                            rs.getLong("id")
                    );

                    role.put(
                            "code",
                            rs.getString("code")
                    );

                    role.put(
                            "name",
                            rs.getString("name")
                    );

                    roles.add(role);
                }
            }
        }

        return roles;
    }

    public boolean roleExists(
            long roleId
    ) throws SQLException {

        String sql =
                "SELECT 1 FROM roles WHERE id = ?";

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, roleId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void assignRoles(
            long userId,
            List<Long> roleIds,
            String dataScope,
            long currentUserId
    ) throws SQLException {

        try (
                Connection connection =
                        DatabaseConfig.getConnection()
        ) {

            connection.setAutoCommit(false);

            try {

                validateAssignment(connection, userId, roleIds, currentUserId);

                try (
                        PreparedStatement delete =
                                connection.prepareStatement(
                                        "DELETE FROM user_roles WHERE user_id = ?"
                                )
                ) {

                    delete.setLong(
                            1,
                            userId
                    );

                    delete.executeUpdate();
                }

                String insertSql = """
                        INSERT INTO user_roles(
                            user_id,
                            role_id
                        )
                        VALUES (?, ?)
                        """;

                try (
                        PreparedStatement insert =
                                connection.prepareStatement(
                                        insertSql
                                )
                ) {

                    for (Long roleId : new LinkedHashSet<>(roleIds)) {

                        insert.setLong(
                                1,
                                userId
                        );

                        insert.setLong(
                                2,
                                roleId
                        );

                        insert.addBatch();
                    }

                    insert.executeBatch();
                }

                try (
                        PreparedStatement update =
                                connection.prepareStatement(
                                        "UPDATE users SET data_scope = ?, session_version = session_version + 1 WHERE id = ?"
                                )
                ) {

                    update.setString(
                            1,
                            dataScope
                    );

                    update.setLong(
                            2,
                            userId
                    );

                    update.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {

                connection.rollback();

                throw e;
            }
        }
    }

    private void validateAssignment(Connection connection, long userId,
                                    List<Long> roleIds, long currentUserId) throws SQLException {
        // Lock the ADMIN row so two concurrent requests cannot remove the last admins.
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT id FROM roles WHERE code = 'ADMIN' FOR UPDATE");
             ResultSet rs = lock.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("Thiếu vai trò ADMIN");
        }
        Long teamId;
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT team_id FROM users WHERE id = ? FOR UPDATE")) {
            lock.setLong(1, userId);
            try (ResultSet rs = lock.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("User không tồn tại");
                teamId = rs.getObject(1, Long.class);
            }
        }
        Set<String> codes = new HashSet<>();
        try (PreparedStatement role = connection.prepareStatement("SELECT code FROM roles WHERE id = ?")) {
            for (Long roleId : new LinkedHashSet<>(roleIds)) {
                role.setLong(1, roleId);
                try (ResultSet rs = role.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Role không hợp lệ");
                    codes.add(rs.getString(1));
                }
            }
        }
        if (codes.contains("TEAM_LEAD") && teamId == null) {
            throw new IllegalArgumentException("Trưởng nhóm kinh doanh phải được gán vào một nhóm.");
        }
        if (hasRole(connection, userId, "ADMIN") && !codes.contains("ADMIN")) {
            if (userId == currentUserId) {
                throw new IllegalArgumentException("Bạn không thể tự thu hồi quyền quản trị của chính mình.");
            }
            try (PreparedStatement count = connection.prepareStatement("""
                    SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur
                    JOIN roles r ON r.id = ur.role_id WHERE r.code = 'ADMIN'
                    """); ResultSet rs = count.executeQuery()) {
                rs.next();
                if (rs.getLong(1) <= 1) {
                    throw new IllegalArgumentException("Không thể thu hồi quyền của quản trị viên cuối cùng.");
                }
            }
        }
    }

    public boolean hasRole(long userId, String code) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            return hasRole(connection, userId, code);
        }
    }

    private boolean hasRole(Connection connection, long userId, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.code = ?
                """)) {
            statement.setLong(1, userId);
            statement.setString(2, code);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public String findDataScope(
            long userId
    ) throws SQLException {

        String sql =
                "SELECT data_scope FROM users WHERE id = ?";

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                return rs.getString(
                        "data_scope"
                );
            }
        }
    }
}
