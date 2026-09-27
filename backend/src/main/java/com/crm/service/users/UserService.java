package com.crm.service.users;

import com.crm.dao.users.UserDAO;
import com.crm.dao.users.UserLockHandoverDAO;
import com.crm.model.User;
import com.crm.util.DBConnection;
import com.crm.util.SessionRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {
    private static final String ACTIVE = "ACTIVE";
    private static final String LOCKED = "LOCKED";

    private final UserDAO userDAO;
    private final OwnershipTransferService ownershipTransferService;
    private final UserLockHandoverDAO userLockHandoverDAO;

    public UserService() {
        this(new UserDAO(), new OwnershipTransferService(), new UserLockHandoverDAO());
    }

    UserService(UserDAO userDAO, OwnershipTransferService ownershipTransferService) {
        this(userDAO, ownershipTransferService, new UserLockHandoverDAO());
    }

    UserService(UserDAO userDAO, OwnershipTransferService ownershipTransferService,
                UserLockHandoverDAO userLockHandoverDAO) {
        this.userDAO = userDAO;
        this.ownershipTransferService = ownershipTransferService;
        this.userLockHandoverDAO = userLockHandoverDAO;
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
        return lockUser(targetUserId, actorUserId, null, lockReason);
    }

    public StatusChangeResult lockUser(long targetUserId, long actorUserId, Long recipientUserId,
                                       String lockReason) throws SQLException {
        String normalizedReason = lockReason == null ? "" : lockReason.trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 500) {
            return StatusChangeResult.INVALID_REASON;
        }
        if (actorUserId == targetUserId) {
            return StatusChangeResult.SELF_LOCK;
        }
        if (recipientUserId != null && recipientUserId == targetUserId) {
            return StatusChangeResult.SAME_USER;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LockedUsers lockedUsers = lockTargetAndRecipient(
                        conn, targetUserId, recipientUserId);
                User target = lockedUsers.target();
                if (target == null) {
                    conn.rollback();
                    return StatusChangeResult.TARGET_NOT_FOUND;
                }
                if (!ACTIVE.equals(target.getStatus())) {
                    conn.rollback();
                    return StatusChangeResult.INVALID_CURRENT_STATUS;
                }

                User recipient = lockedUsers.recipient();
                if (recipientUserId != null) {
                    if (recipient == null) {
                        conn.rollback();
                        return StatusChangeResult.RECIPIENT_NOT_FOUND;
                    }
                    if (!ACTIVE.equals(recipient.getStatus())) {
                        conn.rollback();
                        return StatusChangeResult.RECIPIENT_NOT_ACTIVE;
                    }
                }

                ownershipTransferService.lockOwnershipRows(conn, targetUserId);
                boolean hasOwnership = ownershipTransferService.hasOwnership(conn, targetUserId);
                if (hasOwnership && recipientUserId == null) {
                    conn.rollback();
                    return StatusChangeResult.RECIPIENT_REQUIRED;
                }

                if (recipientUserId != null) {
                    ownershipTransferService.transferAll(conn, targetUserId, recipientUserId);
                    if (!ownershipTransferService.isTransferComplete(conn, targetUserId)) {
                        conn.rollback();
                        return StatusChangeResult.TRANSFER_INCOMPLETE;
                    }
                }

                int affectedRows = userDAO.updateStatus(conn, targetUserId, ACTIVE, LOCKED);
                if (affectedRows != 1) {
                    conn.rollback();
                    return StatusChangeResult.UPDATE_CONFLICT;
                }

                if (recipientUserId != null) {
                    userLockHandoverDAO.create(conn, targetUserId, recipientUserId,
                            actorUserId, normalizedReason);
                }

                conn.commit();
                SessionRegistry.revokeAll(targetUserId);
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
                SessionRegistry.allowUser(targetUserId);
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
                ownershipTransferService.lockOwnershipRows(conn, sourceUserId);
                ownershipTransferService.transferAll(conn, sourceUserId, recipientUserId);
                if (!ownershipTransferService.isTransferComplete(conn, sourceUserId)) {
                    conn.rollback();
                    return TransferValidationResult.TRANSFER_INCOMPLETE;
                }
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

    private LockedUsers lockTargetAndRecipient(Connection conn, long targetUserId,
                                                Long recipientUserId) throws SQLException {
        if (recipientUserId == null) {
            return new LockedUsers(userDAO.findByIdForUpdate(conn, targetUserId), null);
        }

        long firstId = Math.min(targetUserId, recipientUserId);
        long secondId = Math.max(targetUserId, recipientUserId);
        User first = userDAO.findByIdForUpdate(conn, firstId);
        User second = userDAO.findByIdForUpdate(conn, secondId);
        return targetUserId == firstId
                ? new LockedUsers(first, second)
                : new LockedUsers(second, first);
    }

    private record LockedUsers(User target, User recipient) {
    }

    public enum StatusChangeResult {
        SUCCESS,
        INVALID_REASON,
        SELF_LOCK,
        TARGET_NOT_FOUND,
        INVALID_CURRENT_STATUS,
        RECIPIENT_REQUIRED,
        SAME_USER,
        RECIPIENT_NOT_FOUND,
        RECIPIENT_NOT_ACTIVE,
        UPDATE_CONFLICT,
        TRANSFER_INCOMPLETE
    }

    public enum TransferValidationResult {
        SUCCESS,
        SAME_USER,
        SOURCE_NOT_FOUND,
        RECIPIENT_NOT_FOUND,
        RECIPIENT_NOT_ACTIVE,
        NOT_SUPPORTED,
        TRANSFER_INCOMPLETE
    }
}
