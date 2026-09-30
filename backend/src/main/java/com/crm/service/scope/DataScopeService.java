package com.crm.service.scope;

import com.crm.dao.scope.DataScopeHelper;
import com.crm.dao.scope.ScopedEntityDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Role;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service managing Data Scope authorization (SELF, TEAM, ALL) across the 4 core domain modules (CRM-25):
 * 1. Customers
 * 2. Opportunities
 * 3. Activities
 * 4. Quotes
 */
public class DataScopeService {

    private final ScopedEntityDAO scopedEntityDAO;
    private final UserDAO userDAO;
    private final ScopeAccessPolicy accessPolicy;

    public DataScopeService() {
        this(new ScopedEntityDAO(), new UserDAO(), new ScopeAccessPolicy());
    }

    public DataScopeService(
            ScopedEntityDAO scopedEntityDAO,
            UserDAO userDAO,
            ScopeAccessPolicy accessPolicy) {
        this.scopedEntityDAO = scopedEntityDAO != null ? scopedEntityDAO : new ScopedEntityDAO();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
        this.accessPolicy = accessPolicy != null ? accessPolicy : new ScopeAccessPolicy();
    }

    /**
     * List scoped records visible to the user, with optional search query.
     */
    public List<ScopeRecord> list(
            long userId,
            ScopeEntityType type,
            String search) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            ScopeContext actor = loadContext(conn, userId);
            return scopedEntityDAO.findVisible(conn, type, actor, search);
        }
    }

    /**
     * Count scoped records visible to the user.
     */
    public long count(
            long userId,
            ScopeEntityType type,
            String search) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            ScopeContext actor = loadContext(conn, userId);
            return scopedEntityDAO.countVisible(conn, type, actor, search);
        }
    }

    /**
     * Read single scoped record by ID, verifying data scope permissions.
     * Returns:
     * - SUCCESS (200) if user has permission
     * - FORBIDDEN (403) if record exists but belongs outside user's data scope
     * - NOT_FOUND (404) if record does not exist
     */
    public ReadResult read(
            long userId,
            ScopeEntityType type,
            long recordId) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            ScopeContext actor = loadContext(conn, userId);
            ScopeRecord record = scopedEntityDAO.findById(conn, type, recordId);

            if (record == null) {
                return new ReadResult(ReadStatus.NOT_FOUND, null);
            }

            if (!accessPolicy.canAccess(actor, record)) {
                return new ReadResult(ReadStatus.FORBIDDEN, null);
            }

            return new ReadResult(ReadStatus.SUCCESS, record);
        }
    }

    /**
     * Create a new scoped record owned by the current user.
     */
    public ScopeRecord create(
            long userId,
            ScopeEntityType type,
            String label) throws SQLException {

        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên/tiêu đề bản ghi không được để trống.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            long newId = scopedEntityDAO.insert(conn, type, label.trim(), userId);
            if (newId <= 0) {
                throw new SQLException("Không thể tạo bản ghi mới.");
            }
            return scopedEntityDAO.findById(conn, type, newId);
        }
    }

    /**
     * Update an existing scoped record after verifying data scope permissions.
     */
    public OperationResult update(
            long userId,
            ScopeEntityType type,
            long recordId,
            String newLabel) throws SQLException {

        if (newLabel == null || newLabel.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên/tiêu đề bản ghi không được để trống.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            ScopeContext actor = loadContext(conn, userId);
            ScopeRecord existing = scopedEntityDAO.findById(conn, type, recordId);

            if (existing == null) {
                return new OperationResult(ReadStatus.NOT_FOUND, null);
            }

            if (!accessPolicy.canAccess(actor, existing)) {
                return new OperationResult(ReadStatus.FORBIDDEN, null);
            }

            scopedEntityDAO.update(conn, type, recordId, newLabel.trim());
            ScopeRecord updated = scopedEntityDAO.findById(conn, type, recordId);
            return new OperationResult(ReadStatus.SUCCESS, updated);
        }
    }

    /**
     * Delete an existing scoped record after verifying data scope permissions.
     */
    public ReadStatus delete(
            long userId,
            ScopeEntityType type,
            long recordId) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            ScopeContext actor = loadContext(conn, userId);
            ScopeRecord existing = scopedEntityDAO.findById(conn, type, recordId);

            if (existing == null) {
                return ReadStatus.NOT_FOUND;
            }

            if (!accessPolicy.canAccess(actor, existing)) {
                return ReadStatus.FORBIDDEN;
            }

            scopedEntityDAO.delete(conn, type, recordId);
            return ReadStatus.SUCCESS;
        }
    }

    /**
     * Load ScopeContext for a user, resolving effective data scope (SELF, TEAM, ALL).
     */
    public ScopeContext loadContext(Connection conn, long userId) throws SQLException {
        User user = userDAO.findUserProfileWithRoles(conn, userId);
        if (user == null) {
            user = userDAO.findById(conn, userId);
        }

        if (user == null) {
            throw new SQLException("Authenticated user not found: " + userId);
        }

        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).toList()
                : List.of();

        String effectiveScope = DataScopeHelper.resolveEffectiveScope(user.getDataScope(), roleNames);

        return new ScopeContext(
                user.getId(),
                user.getTeamId(),
                effectiveScope
        );
    }

    public ScopeContext loadContext(long userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return loadContext(conn, userId);
        }
    }

    public enum ReadStatus {
        SUCCESS,
        NOT_FOUND,
        FORBIDDEN
    }

    public record ReadResult(
            ReadStatus status,
            ScopeRecord record) {
    }

    public record OperationResult(
            ReadStatus status,
            ScopeRecord record) {
    }
}