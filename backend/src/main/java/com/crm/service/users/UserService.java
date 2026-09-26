package com.crm.service.users;

import com.crm.dao.users.UserDAO;
import com.crm.dao.users.UserLockHandoverDAO;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {
    private static final String ACTIVE = "ACTIVE";
    private static final String LOCKED = "LOCKED";

    private final UserDAO userDAO;
    private final UserLockHandoverDAO handoverDAO;

    public UserService() {
        this(new UserDAO(), new UserLockHandoverDAO());
    }

    UserService(UserDAO userDAO, UserLockHandoverDAO handoverDAO) {
        this.userDAO = userDAO;
        this.handoverDAO = handoverDAO;
    }

    public List<User> findAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return userDAO.findAll(conn);
        }
    }

    public User findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return userDAO.findById(conn, id);
        }
    }

    public List<User> findAvailableRecipients(long targetUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return userDAO.findActiveRecipientsExcluding(conn, targetUserId);
        }
    }

    public LockHandoverResult lockAndHandover(long targetUserId, long recipientUserId,
                                               long actorUserId, String lockReason) throws SQLException {
        String normalizedReason = lockReason == null ? "" : lockReason.trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 500) {
            return LockHandoverResult.INVALID_REASON;
        }
        if (targetUserId == recipientUserId) {
            return LockHandoverResult.SAME_RECIPIENT;
        }
        if (actorUserId == targetUserId) {
            return LockHandoverResult.SELF_LOCK;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long firstId = Math.min(targetUserId, recipientUserId);
                long secondId = Math.max(targetUserId, recipientUserId);
                User first = userDAO.findByIdForUpdate(conn, firstId);
                User second = userDAO.findByIdForUpdate(conn, secondId);

                User target = targetUserId == firstId ? first : second;
                User recipient = recipientUserId == firstId ? first : second;

                if (target == null) {
                    conn.rollback();
                    return LockHandoverResult.TARGET_NOT_FOUND;
                }
                if (recipient == null) {
                    conn.rollback();
                    return LockHandoverResult.RECIPIENT_NOT_FOUND;
                }
                if (!ACTIVE.equals(target.getStatus())) {
                    conn.rollback();
                    return LockHandoverResult.TARGET_NOT_ACTIVE;
                }
                if (!ACTIVE.equals(recipient.getStatus())) {
                    conn.rollback();
                    return LockHandoverResult.RECIPIENT_NOT_ACTIVE;
                }

                int affectedRows = userDAO.updateStatus(conn, targetUserId, ACTIVE, LOCKED);
                if (affectedRows != 1) {
                    conn.rollback();
                    return LockHandoverResult.UPDATE_CONFLICT;
                }

                handoverDAO.create(conn, targetUserId, recipientUserId, actorUserId, normalizedReason);
                conn.commit();
                return LockHandoverResult.SUCCESS;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    public enum LockHandoverResult {
        SUCCESS,
        INVALID_REASON,
        SAME_RECIPIENT,
        SELF_LOCK,
        TARGET_NOT_FOUND,
        RECIPIENT_NOT_FOUND,
        TARGET_NOT_ACTIVE,
        RECIPIENT_NOT_ACTIVE,
        UPDATE_CONFLICT
    }

}
