package com.crm.dao.users;

import com.crm.config.DatabaseConfig;
import com.crm.util.PasswordUtil;

import java.sql.*;
import java.util.*;

public class UserManagementDAO {

    public List<Map<String, Object>> search(
            String keyword,
            String status,
            int page,
            int size
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    u.id,
                    u.full_name,
                    u.email,
                    u.status,
                    u.data_scope,
                    u.team_id,
                    t.name AS team_name
                FROM users u
                LEFT JOIN teams t
                    ON t.id = u.team_id
                WHERE u.status <> 'DELETED'
                """);

        List<Object> params =
                new ArrayList<>();

        if (
                keyword != null &&
                !keyword.isBlank()
        ) {
            sql.append("""
                     AND (
                        LOWER(u.full_name) LIKE ?
                        OR LOWER(u.email) LIKE ?
                     )
                    """);

            String like =
                    "%" +
                    keyword.trim().toLowerCase() +
                    "%";

            params.add(like);
            params.add(like);
        }

        if (
                status != null &&
                !status.isBlank()
        ) {
            sql.append(" AND u.status = ?");
            params.add(
                    status.trim().toUpperCase()
            );
        }

        sql.append(
                " ORDER BY u.id DESC LIMIT ? OFFSET ?"
        );

        params.add(size);
        params.add(
                (page - 1) * size
        );

        List<Map<String, Object>> result =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }

        return result;
    }

    public long count(
            String keyword,
            String status
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                SELECT COUNT(*)
                FROM users
                WHERE status <> 'DELETED'
                """);

        List<Object> params =
                new ArrayList<>();

        if (
                keyword != null &&
                !keyword.isBlank()
        ) {

            sql.append("""
                     AND (
                        LOWER(full_name) LIKE ?
                        OR LOWER(email) LIKE ?
                     )
                    """);

            String like =
                    "%" +
                    keyword.trim().toLowerCase() +
                    "%";

            params.add(like);
            params.add(like);
        }

        if (
                status != null &&
                !status.isBlank()
        ) {
            sql.append(" AND status = ?");
            params.add(
                    status.trim().toUpperCase()
            );
        }

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }

            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public Map<String, Object> findById(
            long id
    ) throws SQLException {

        String sql = """
                SELECT
                    u.id,
                    u.full_name,
                    u.email,
                    u.status,
                    u.data_scope,
                    u.team_id,
                    t.name AS team_name
                FROM users u
                LEFT JOIN teams t
                    ON t.id = u.team_id
                WHERE u.id = ?
                  AND u.status <> 'DELETED'
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                return map(rs);
            }
        }
    }

    public boolean emailExists(
            String email,
            Long exceptId
    ) throws SQLException {

        String sql =
                exceptId == null
                        ? "SELECT 1 FROM users WHERE email = ?"
                        : "SELECT 1 FROM users WHERE email = ? AND id <> ?";

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    email
            );

            if (exceptId != null) {
                statement.setLong(
                        2,
                        exceptId
                );
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long create(
            String fullName,
            String email,
            String password,
            String status
    ) throws SQLException {

        String sql = """
                INSERT INTO users(
                    username,
                    full_name,
                    email,
                    password_hash,
                    status
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(1, email);
            statement.setString(2, fullName);
            statement.setString(3, email);
            statement.setString(
                    4,
                    PasswordUtil.hash(password)
            );
            statement.setString(5, status);

            statement.executeUpdate();

            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {

                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new SQLException(
                "Không lấy được user ID"
        );
    }

    public void update(
            long id,
            String fullName,
            String email,
            String status
    ) throws SQLException {

        String sql = """
                UPDATE users
                SET
                    full_name = ?,
                    email = ?,
                    session_version = session_version + IF(status <> ?, 1, 0),
                    status = ?
                WHERE id = ?
                  AND status <> 'DELETED'
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, status);
            statement.setString(4, status);
            statement.setLong(5, id);

            statement.executeUpdate();
        }
    }

    public void softDelete(
            long id
    ) throws SQLException {

        String sql = """
                UPDATE users
                SET
                    status = 'DELETED',
                    session_version = session_version + 1
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public void assignTeam(
            long userId,
            Long teamId
    ) throws SQLException {

        String sql =
                "UPDATE users SET team_id = ? WHERE id = ?";

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            if (teamId == null) {
                statement.setNull(
                        1,
                        Types.BIGINT
                );
            } else {
                statement.setLong(
                        1,
                        teamId
                );
            }

            statement.setLong(
                    2,
                    userId
            );

            statement.executeUpdate();
        }
    }

    public void changeStatus(
            long userId,
            String status
    ) throws SQLException {

        String sql = """
                UPDATE users
                SET
                    status = ?,
                    session_version = session_version + 1
                WHERE id = ?
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    status
            );

            statement.setLong(
                    2,
                    userId
            );

            statement.executeUpdate();
        }
    }

    private Map<String, Object> map(
            ResultSet rs
    ) throws SQLException {

        Map<String, Object> item =
                new LinkedHashMap<>();

        item.put(
                "id",
                rs.getLong("id")
        );

        item.put(
                "fullName",
                rs.getString("full_name")
        );

        item.put(
                "email",
                rs.getString("email")
        );

        item.put(
                "status",
                rs.getString("status")
        );

        item.put(
                "dataScope",
                rs.getString("data_scope")
        );

        Object teamId =
                rs.getObject("team_id");

        item.put(
                "teamId",
                teamId
        );

        item.put(
                "teamName",
                rs.getString("team_name")
        );

        return item;
    }
}
