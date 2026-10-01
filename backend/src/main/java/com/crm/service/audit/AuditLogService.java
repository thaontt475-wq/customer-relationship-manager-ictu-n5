package com.crm.service.audit;

import com.crm.dao.audit.AuditLogDAO;
import com.crm.model.AuditLog;
import com.crm.model.AuditLogFilter;
import com.crm.util.DBConnection;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Application service for CRM-37 change history.
 *
 * Business services should use the overload accepting their current JDBC connection so the
 * business mutation and its audit record commit or roll back together.
 */
public class AuditLogService {
    public static final String ACTION_DISCOUNT_CHANGED = "DISCOUNT_CHANGED";
    public static final String ACTION_TARGET_CHANGED = "TARGET_CHANGED";
    public static final String ACTION_OWNERSHIP_CHANGED = "OWNERSHIP_CHANGED";
    public static final String ACTION_ROLE_CHANGED = "ROLE_CHANGED";

    private static final int MAX_IDENTIFIER_LENGTH = 50;
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Gson GSON = new Gson();

    private final AuditLogDAO auditLogDAO;
    private final ConnectionProvider connectionProvider;

    public AuditLogService() {
        this(new AuditLogDAO(), DBConnection::getConnection);
    }

    public AuditLogService(AuditLogDAO auditLogDAO, ConnectionProvider connectionProvider) {
        this.auditLogDAO = Objects.requireNonNull(auditLogDAO, "auditLogDAO");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    /**
     * Record an audit entry as a standalone database operation.
     */
    public long recordChange(long actorUserId, String action, String objectType, long objectId,
                             Object beforeValue, Object afterValue) throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            return recordChange(conn, actorUserId, action, objectType, objectId, beforeValue, afterValue);
        }
    }

    /**
     * Record an audit entry inside an existing business transaction.
     */
    public long recordChange(Connection conn, long actorUserId, String action, String objectType,
                             long objectId, Object beforeValue, Object afterValue) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("Kết nối cơ sở dữ liệu không được để trống");
        }
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId phải là số dương");
        }
        if (objectId <= 0) {
            throw new IllegalArgumentException("objectId phải là số dương");
        }

        String normalizedAction = normalizeIdentifier(action, "action");
        String normalizedObjectType = normalizeIdentifier(objectType, "objectType");
        AuditLog auditLog = new AuditLog(
                actorUserId,
                normalizedAction,
                normalizedObjectType,
                objectId,
                toJsonTree(beforeValue),
                toJsonTree(afterValue)
        );
        return auditLogDAO.insert(conn, auditLog);
    }

    public long recordDiscountChange(Connection conn, long actorUserId, String objectType,
                                     long objectId, Object beforeValue, Object afterValue)
            throws SQLException {
        return recordChange(conn, actorUserId, ACTION_DISCOUNT_CHANGED, objectType, objectId,
                beforeValue, afterValue);
    }

    public long recordTargetChange(Connection conn, long actorUserId, String objectType,
                                   long objectId, Object beforeValue, Object afterValue)
            throws SQLException {
        return recordChange(conn, actorUserId, ACTION_TARGET_CHANGED, objectType, objectId,
                beforeValue, afterValue);
    }

    public long recordOwnershipChange(Connection conn, long actorUserId, String objectType,
                                      long objectId, Object beforeValue, Object afterValue)
            throws SQLException {
        return recordChange(conn, actorUserId, ACTION_OWNERSHIP_CHANGED, objectType, objectId,
                beforeValue, afterValue);
    }

    public long recordRoleChange(Connection conn, long actorUserId, long targetUserId,
                                 Object beforeValue, Object afterValue) throws SQLException {
        return recordChange(conn, actorUserId, ACTION_ROLE_CHANGED, "USER", targetUserId,
                beforeValue, afterValue);
    }

    public List<AuditLog> findLogs(AuditLogFilter filter) throws SQLException {
        AuditLogFilter normalized = validateAndNormalizeFilter(filter);
        try (Connection conn = connectionProvider.getConnection()) {
            return auditLogDAO.find(conn, normalized);
        }
    }

    AuditLogFilter validateAndNormalizeFilter(AuditLogFilter filter) {
        AuditLogFilter normalized = filter == null ? new AuditLogFilter() : filter;
        if (normalized.getUserId() != null && normalized.getUserId() <= 0) {
            throw new IllegalArgumentException("userId phải là số dương");
        }
        if (normalized.getObjectId() != null && normalized.getObjectId() <= 0) {
            throw new IllegalArgumentException("objectId phải là số dương");
        }
        if (normalized.getObjectType() != null && !normalized.getObjectType().isBlank()) {
            normalized.setObjectType(normalizeIdentifier(normalized.getObjectType(), "objectType"));
        } else {
            normalized.setObjectType(null);
        }
        if (normalized.getFrom() != null && normalized.getTo() != null
                && normalized.getFrom().after(normalized.getTo())) {
            throw new IllegalArgumentException("Thời điểm 'from' không được sau 'to'");
        }
        if (normalized.getLimit() <= 0) {
            normalized.setLimit(AuditLogFilter.DEFAULT_LIMIT);
        } else if (normalized.getLimit() > AuditLogFilter.MAX_LIMIT) {
            normalized.setLimit(AuditLogFilter.MAX_LIMIT);
        }
        return normalized;
    }

    private String normalizeIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_IDENTIFIER_LENGTH || !IDENTIFIER_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName
                    + " chỉ được chứa chữ cái, chữ số, dấu gạch dưới và tối đa 50 ký tự");
        }
        return normalized;
    }

    private JsonElement toJsonTree(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        return value instanceof JsonElement jsonElement ? jsonElement : GSON.toJsonTree(value);
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

}
