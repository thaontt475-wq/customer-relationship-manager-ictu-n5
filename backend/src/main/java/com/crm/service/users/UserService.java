package com.crm.service.users;

import com.crm.dao.users.UserDAO;
import com.crm.dao.users.UserLockHandoverDAO;
import com.crm.model.User;
import com.crm.service.email.EmailService;
import com.crm.util.PasswordUtil;
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
    private final EmailService emailService = new EmailService();
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
            User user = userDAO.findById(conn, id);
            if (user != null) {
                user.setRole(String.join(", ", userDAO.findRoleNamesByUserId(conn, id)));
            }
            return user;
        }
    }

    public List<User> findAvailableRecipients(long targetUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return userDAO.findActiveRecipientsExcluding(conn, targetUserId);
        }
    }

    public CreateUserResult createUser(
            String username,
            String email,
            String fullName,
            String phone,
            Long teamId) throws SQLException {

        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);
        String normalizedFullName = normalize(fullName);
        String normalizedPhone = normalizeNullable(phone);

        if (normalizedEmail == null || normalizedFullName == null) {
            return new CreateUserResult(CreateUserStatus.INVALID_INPUT, null);
        }

        if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return new CreateUserResult(CreateUserStatus.INVALID_EMAIL, null);
        }
        if (normalizedUsername == null) {
            normalizedUsername = generateUsernameFromEmail(normalizedEmail);
        }

        if (teamId != null && teamId <= 0) {
            return new CreateUserResult(CreateUserStatus.INVALID_INPUT, null);
        }

        String temporaryPassword = generateTemporaryPassword();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (userDAO.findByEmail(conn, normalizedEmail) != null) {
                    conn.rollback();
                    return new CreateUserResult(CreateUserStatus.DUPLICATE_EMAIL, null);
                }

                if (userDAO.findByUsername(conn, normalizedUsername) != null) {
                    conn.rollback();
                    return new CreateUserResult(CreateUserStatus.DUPLICATE_USERNAME, null);
                }

                User user = new User();
                user.setUsername(normalizedUsername);
                user.setEmail(normalizedEmail);
                user.setFullName(normalizedFullName);
                user.setPhone(normalizedPhone);
                user.setTeamId(teamId);
                user.setPasswordHash(PasswordUtil.hashPassword(temporaryPassword));

                long userId = userDAO.create(conn, user);
                conn.commit();

                emailService.sendAccountActivationEmail(
                        normalizedEmail,
                        normalizedFullName,
                        normalizedUsername,
                        temporaryPassword
                );

                return new CreateUserResult(CreateUserStatus.SUCCESS, userId);

            } catch (SQLException | RuntimeException ex) {
                rollback(conn, ex);
                throw ex;
            }
        }
    }

    public UpdateUserStatus updateUser(
            long userId,
            String username,
            String email,
            String fullName,
            String phone,
            Long teamId) throws SQLException {

        if (userId <= 0) {
            return UpdateUserStatus.INVALID_INPUT;
        }

        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);
        String normalizedFullName = normalize(fullName);
        String normalizedPhone = normalizeNullable(phone);

        if (normalizedEmail == null || normalizedFullName == null) {
            return UpdateUserStatus.INVALID_INPUT;
        }

        if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return UpdateUserStatus.INVALID_EMAIL;
        }

        if (teamId != null && teamId <= 0) {
            return UpdateUserStatus.INVALID_INPUT;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                User existingUser = userDAO.findById(conn, userId);
                if (existingUser == null) {
                    conn.rollback();
                    return UpdateUserStatus.NOT_FOUND;
                }

                if (normalizedUsername == null) {
                    normalizedUsername = existingUser.getUsername();
                }

                if (normalizedPhone == null) {
                    normalizedPhone = existingUser.getPhone();
                }

                if (teamId == null) {
                    teamId = existingUser.getTeamId();
                }

                if (userDAO.emailExistsExcludingUser(conn, normalizedEmail, userId)) {
                    conn.rollback();
                    return UpdateUserStatus.DUPLICATE_EMAIL;
                }

                if (userDAO.usernameExistsExcludingUser(conn, normalizedUsername, userId)) {
                    conn.rollback();
                    return UpdateUserStatus.DUPLICATE_USERNAME;
                }

                User user = new User();
                user.setId(userId);
                user.setUsername(normalizedUsername);
                user.setEmail(normalizedEmail);
                user.setFullName(normalizedFullName);
                user.setPhone(normalizedPhone);
                user.setTeamId(teamId);

                int affected = userDAO.updateProfile(conn, user);

                if (affected != 1) {
                    conn.rollback();
                    return UpdateUserStatus.UPDATE_CONFLICT;
                }

                conn.commit();
                return UpdateUserStatus.SUCCESS;

            } catch (SQLException | RuntimeException ex) {
                rollback(conn, ex);
                throw ex;
            }
        }
    }

    public UserPage searchUsers(
            String keyword,
            String role,
            String status,
            int requestedPage,
            int requestedSize) throws SQLException {

        int page = Math.max(requestedPage, 1);
        int size = requestedSize <= 0 ? 20 : Math.min(requestedSize, 100);
        int offset = (page - 1) * size;

        try (Connection conn = DBConnection.getConnection()) {
            long totalItems = userDAO.countSearch(conn, keyword, role, status);
            List<User> items = userDAO.search(
                    conn,
                    keyword,
                    role,
                    status,
                    size,
                    offset
            );

            long totalPages = totalItems == 0
                    ? 0
                    : (totalItems + size - 1) / size;

            return new UserPage(
                    items,
                    page,
                    size,
                    totalItems,
                    totalPages
            );
        }
    }

    private String generateUsernameFromEmail(String email) {
        String normalizedEmail = email.toLowerCase(java.util.Locale.ROOT);
        int atIndex = normalizedEmail.indexOf('@');
        String localPart = atIndex > 0
                ? normalizedEmail.substring(0, atIndex)
                : normalizedEmail;

        String base = localPart.replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }

        String suffix = Integer.toUnsignedString(
                normalizedEmail.hashCode(),
                36
        );

        int maxBaseLength = Math.max(1, 100 - suffix.length() - 1);
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }

        return base + "-" + suffix;
    }
    private String generateTemporaryPassword() {
        String random = java.util.UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        return "Tmp!9" + random;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public enum CreateUserStatus {
        SUCCESS,
        INVALID_INPUT,
        INVALID_EMAIL,
        DUPLICATE_EMAIL,
        DUPLICATE_USERNAME
    }

    public record CreateUserResult(
            CreateUserStatus status,
            Long userId) {
    }

    public enum UpdateUserStatus {
        SUCCESS,
        INVALID_INPUT,
        INVALID_EMAIL,
        DUPLICATE_EMAIL,
        DUPLICATE_USERNAME,
        NOT_FOUND,
        UPDATE_CONFLICT
    }

    public record UserPage(
            List<User> items,
            int page,
            int size,
            long totalItems,
            long totalPages) {
    }
    public DeleteUserStatus deleteUser(long userId, long actorUserId)
            throws SQLException {

        if (userId <= 0) {
            return DeleteUserStatus.NOT_FOUND;
        }

        if (userId == actorUserId) {
            return DeleteUserStatus.SELF_DELETE;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (userDAO.findById(conn, userId) == null) {
                    conn.rollback();
                    return DeleteUserStatus.NOT_FOUND;
                }

                int affected = userDAO.delete(conn, userId);

                if (affected != 1) {
                    conn.rollback();
                    return DeleteUserStatus.DELETE_CONFLICT;
                }

                conn.commit();
                return DeleteUserStatus.SUCCESS;

            } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                conn.rollback();
                return DeleteUserStatus.REFERENCED_DATA;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    public enum DeleteUserStatus {
        SUCCESS,
        NOT_FOUND,
        SELF_DELETE,
        REFERENCED_DATA,
        DELETE_CONFLICT
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
