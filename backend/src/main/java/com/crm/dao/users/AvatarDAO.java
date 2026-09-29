package com.crm.dao.users;

import com.crm.model.UserAvatar;
import java.sql.Connection;
import java.sql.SQLException;

public class AvatarDAO {
    public boolean lockActiveUser(Connection conn, long userId) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT id FROM users WHERE id = ? AND active = TRUE AND status = 'ACTIVE' FOR UPDATE")) {
            stmt.setLong(1, userId);
            try (var rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    public UserAvatar find(Connection conn, long userId) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT image_path, thumbnail_path FROM user_avatars WHERE user_id = ?")) {
            stmt.setLong(1, userId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? new UserAvatar(rs.getString(1), rs.getString(2)) : null;
            }
        }
    }

    public void save(Connection conn, long userId, UserAvatar avatar) throws SQLException {
        try (var stmt = conn.prepareStatement("INSERT INTO user_avatars (user_id, image_path, thumbnail_path) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE image_path = ?, thumbnail_path = ?")) {
            stmt.setLong(1, userId);
            stmt.setString(2, avatar.imagePath());
            stmt.setString(3, avatar.thumbnailPath());
            stmt.setString(4, avatar.imagePath());
            stmt.setString(5, avatar.thumbnailPath());
            stmt.executeUpdate();
        }
    }
}
