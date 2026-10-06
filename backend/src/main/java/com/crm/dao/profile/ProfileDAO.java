package com.crm.dao.profile;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProfileDAO {

    public Map<String, Object> findByUserId(
            long userId
    ) throws SQLException {

        String sql = """
                SELECT
                    u.id,
                    COALESCE(u.full_name, u.display_name, '') AS full_name,
                    u.email,
                    u.phone,
                    COALESCE(u.email_signature, u.signature, '') AS email_signature,
                    u.avatar_url,
                    u.avatar_thumbnail_url,
                    u.status,
                    t.name AS team_name,
                    GROUP_CONCAT(r.name SEPARATOR ', ') AS role_names
                FROM users u
                LEFT JOIN teams t ON t.id = u.team_id
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE u.id = ?
                GROUP BY u.id, u.full_name, u.display_name, u.email, u.phone, u.email_signature, u.signature, u.avatar_url, u.avatar_thumbnail_url, u.status, t.name
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    userId
            );

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                if (!rs.next()) {
                    return null;
                }

                Map<String, Object> profile =
                        new LinkedHashMap<>();

                profile.put(
                        "id",
                        rs.getLong("id")
                );

                profile.put(
                        "fullName",
                        rs.getString("full_name")
                );

                profile.put(
                        "email",
                        rs.getString("email")
                );

                profile.put(
                        "phone",
                        rs.getString("phone")
                );

                profile.put(
                        "emailSignature",
                        rs.getString("email_signature")
                );

                profile.put(
                        "avatarUrl",
                        rs.getString("avatar_url")
                );

                profile.put(
                        "avatarThumbnailUrl",
                        rs.getString(
                                "avatar_thumbnail_url"
                        )
                );

                profile.put(
                        "status",
                        rs.getString("status")
                );

                profile.put(
                        "teamName",
                        rs.getString("team_name")
                );

                profile.put(
                        "roleNames",
                        rs.getString("role_names")
                );

                return profile;
            }
        }
    }


    public void update(
            long userId,
            String fullName,
            String phone,
            String emailSignature
    ) throws SQLException {

        String sql = """
                UPDATE users
                SET
                    full_name = ?,
                    display_name = ?,
                    phone = ?,
                    email_signature = ?,
                    signature = ?
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
                    fullName
            );

            statement.setString(
                    2,
                    fullName
            );

            statement.setString(
                    3,
                    phone
            );

            statement.setString(
                    4,
                    emailSignature
            );

            statement.setString(
                    5,
                    emailSignature
            );

            statement.setLong(
                    6,
                    userId
            );

            statement.executeUpdate();
        }
    }


    public void updateAvatar(
            long userId,
            String avatarUrl,
            String thumbnailUrl
    ) throws SQLException {

        String sql = """
                UPDATE users
                SET
                    avatar_url = ?,
                    avatar_thumbnail_url = ?
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
                    avatarUrl
            );

            statement.setString(
                    2,
                    thumbnailUrl
            );

            statement.setLong(
                    3,
                    userId
            );

            statement.executeUpdate();
        }
    }
}