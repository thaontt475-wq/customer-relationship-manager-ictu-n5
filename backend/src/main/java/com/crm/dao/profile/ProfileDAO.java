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
                    id,
                    full_name,
                    email,
                    phone,
                    email_signature,
                    avatar_url,
                    avatar_thumbnail_url,
                    status
                FROM users
                WHERE id = ?
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
                    phone = ?,
                    email_signature = ?
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
                    phone
            );

            statement.setString(
                    3,
                    emailSignature
            );

            statement.setLong(
                    4,
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