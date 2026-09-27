package com.crm.dao.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserLockHandoverDAO {

    public void create(Connection conn, long targetUserId, long recipientUserId,
                       long lockedByUserId, String lockReason) throws SQLException {
        String sql = "INSERT INTO user_lock_handovers "
                + "(target_user_id, recipient_user_id, locked_by_user_id, lock_reason, created_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, targetUserId);
            stmt.setLong(2, recipientUserId);
            stmt.setLong(3, lockedByUserId);
            stmt.setString(4, lockReason);
            if (stmt.executeUpdate() != 1) {
                throw new SQLException("Unable to create user lock handover record");
            }
        }
    }
}
