package com.crm.service.scope;

import com.crm.dao.scope.ScopedEntityDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DataScopeService {

    private final ScopedEntityDAO scopedEntityDAO;
    private final UserDAO userDAO;
    private final ScopeAccessPolicy accessPolicy;

    public DataScopeService() {
        this(new ScopedEntityDAO(), new UserDAO(), new ScopeAccessPolicy());
    }

    DataScopeService(
            ScopedEntityDAO scopedEntityDAO,
            UserDAO userDAO,
            ScopeAccessPolicy accessPolicy) {
        this.scopedEntityDAO = scopedEntityDAO;
        this.userDAO = userDAO;
        this.accessPolicy = accessPolicy;
    }

    public List<ScopeRecord> list(
            long userId,
            ScopeEntityType type,
            String search) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            ScopeContext actor = loadContext(conn, userId);
            return scopedEntityDAO.findVisible(conn, type, actor, search);
        }
    }

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

    private ScopeContext loadContext(Connection conn, long userId)
            throws SQLException {

        User user = userDAO.findById(conn, userId);

        if (user == null) {
            throw new SQLException("Authenticated user not found");
        }

        String scope = user.getDataScope() == null
                || user.getDataScope().isBlank()
                ? "SELF"
                : user.getDataScope();

        return new ScopeContext(
                user.getId(),
                user.getTeamId(),
                scope
        );
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
}