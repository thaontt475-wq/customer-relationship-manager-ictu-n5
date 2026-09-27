package com.crm.service.users;

import com.crm.dao.users.UserDAO;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {
    private static final String ACTIVE = "ACTIVE";
    private static final String LOCKED = "LOCKED";

    private final UserDAO userDAO;
    private final OwnershipTransferService ownershipTransferService;

    public UserService() {
        this(new UserDAO(), new OwnershipTransferService());
    }

    UserService(UserDAO userDAO, OwnershipTransferService ownershipTransferService) {
        this.userDAO = userDAO;
        this.ownershipTransferService = ownershipTransferService;
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

    public StatusChangeResult lockUser(long targetUserId, long actorUserId, String lockReason)
            throws SQLException {
        String normalizedReason = lockReason == null ? "" : lockReason.trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 500) {
            return StatusChangeResult.INVALID_REASON;
        }
        if (actorUserId == targetUserId) {
            return StatusChangeResult.SELF_LOCK;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User target = userDAO.findByIdForUpdate(conn, targetUserId);
                if (target == null) {
                    conn.rollback();
                    return StatusChangeResult.TARGET_NOT_FOUND;
                }
                if (!ACTIVE.equals(target.getStatus())) {
                    conn.rollback();
                    return StatusChangeResult.INVALID_CURRENT_STATUS;
                }

                int affectedRows = userDAO.updateStatus(conn, targetUserId, ACTIVE, LOCKED);
                if (affectedRows != 1) {
                    conn.rollback();
                    return StatusChangeResult.UPDATE_CONFLICT;
                }

                conn.commit();
                return StatusChangeResult.SUCCESS;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    public StatusChangeResult unlockUser(long targetUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User target = userDAO.findByIdForUpdate(conn, targetUserId);
                if (target == null) {
                    conn.rollback();
                    return StatusChangeResult.TARGET_NOT_FOUND;
                }
                if (!LOCKED.equals(target.getStatus())) {
                    conn.rollback();
                    return StatusChangeResult.INVALID_CURRENT_STATUS;
                }

                int affectedRows = userDAO.updateStatus(conn, targetUserId, LOCKED, ACTIVE);
                if (affectedRows != 1) {
                    conn.rollback();
                    return StatusChangeResult.UPDATE_CONFLICT;
                }

                conn.commit();
                return StatusChangeResult.SUCCESS;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    public TransferValidationResult validateTransfer(long sourceUserId, long recipientUserId)
            throws SQLException {
        if (sourceUserId == recipientUserId) {
            return TransferValidationResult.SAME_USER;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long firstId = Math.min(sourceUserId, recipientUserId);
                long secondId = Math.max(sourceUserId, recipientUserId);
                User first = userDAO.findByIdForUpdate(conn, firstId);
                User second = userDAO.findByIdForUpdate(conn, secondId);
                User source = sourceUserId == firstId ? first : second;
                User recipient = recipientUserId == firstId ? first : second;

                if (source == null) {
                    conn.rollback();
                    return TransferValidationResult.SOURCE_NOT_FOUND;
                }
                if (recipient == null) {
                    conn.rollback();
                    return TransferValidationResult.RECIPIENT_NOT_FOUND;
                }
                if (!ACTIVE.equals(recipient.getStatus())) {
                    conn.rollback();
                    return TransferValidationResult.RECIPIENT_NOT_ACTIVE;
                }
                if (!ownershipTransferService.isSupported()) {
                    conn.rollback();
                    return TransferValidationResult.NOT_SUPPORTED;
                }

                // Future schema implementations must execute their transfer in this transaction.
                ownershipTransferService.transferAll(conn, sourceUserId, recipientUserId);
                conn.commit();
                return TransferValidationResult.SUCCESS;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    private void rollback(Connection conn, Exception originalException) {
        try {
            conn.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    public enum StatusChangeResult {
        SUCCESS,
        INVALID_REASON,
        SELF_LOCK,
        TARGET_NOT_FOUND,
        INVALID_CURRENT_STATUS,
        UPDATE_CONFLICT
    }

    public enum TransferValidationResult {
        SUCCESS,
        SAME_USER,
        SOURCE_NOT_FOUND,
        RECIPIENT_NOT_FOUND,
        RECIPIENT_NOT_ACTIVE,
        NOT_SUPPORTED
    }
}
