package com.crm.dao.audit;

import com.crm.model.AuditLog;
import com.crm.model.AuditLogFilter;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC data access for immutable CRM-37 audit log records.
 */
public class AuditLogDAO {

    public long insert(Connection conn, AuditLog auditLog) throws SQLException {
        String sql = "INSERT INTO audit_logs "
                + "(actor_user_id, action, object_type, object_id, before_value, after_value) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, auditLog.getActorUserId());
            stmt.setString(2, auditLog.getAction());
            stmt.setString(3, auditLog.getObjectType());
            stmt.setLong(4, auditLog.getObjectId());
            stmt.setString(5, toJson(auditLog.getBeforeValue()));
            stmt.setString(6, toJson(auditLog.getAfterValue()));

            int affected = stmt.executeUpdate();
            if (affected != 1) {
                throw new SQLException("Audit log insert affected " + affected + " rows");
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    auditLog.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Audit log insert did not return a generated ID");
    }

    public List<AuditLog> find(Connection conn, AuditLogFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, actor_user_id, action, object_type, object_id, "
                        + "before_value, after_value, created_at FROM audit_logs WHERE 1 = 1"
        );
        List<Object> parameters = new ArrayList<>();
        appendConditions(sql, parameters, filter);

        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        parameters.add(filter.getLimit());
        parameters.add(filter.getOffset());

        List<AuditLog> results = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            bind(stmt, parameters);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public int count(Connection conn, AuditLogFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM audit_logs WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendConditions(sql, parameters, filter);

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            bind(stmt, parameters);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private void appendConditions(StringBuilder sql, List<Object> parameters, AuditLogFilter filter) {
        if (filter.getUserId() != null) {
            sql.append(" AND actor_user_id = ?");
            parameters.add(filter.getUserId());
        }
        if (filter.getAction() != null) {
            sql.append(" AND action = ?");
            parameters.add(filter.getAction());
        }
        if (filter.getObjectType() != null) {
            sql.append(" AND object_type = ?");
            parameters.add(filter.getObjectType());
        }
        if (filter.getObjectId() != null) {
            sql.append(" AND object_id = ?");
            parameters.add(filter.getObjectId());
        }
        if (filter.getFrom() != null) {
            sql.append(" AND created_at >= ?");
            parameters.add(filter.getFrom());
        }
        if (filter.getTo() != null) {
            sql.append(" AND created_at <= ?");
            parameters.add(filter.getTo());
        }
    }

    private void bind(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            int index = i + 1;
            if (value instanceof Long number) {
                stmt.setLong(index, number);
            } else if (value instanceof Integer number) {
                stmt.setInt(index, number);
            } else if (value instanceof Timestamp timestamp) {
                stmt.setTimestamp(index, timestamp);
            } else {
                stmt.setString(index, String.valueOf(value));
            }
        }
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(rs.getLong("id"));
        auditLog.setActorUserId(rs.getLong("actor_user_id"));
        auditLog.setAction(rs.getString("action"));
        auditLog.setObjectType(rs.getString("object_type"));
        auditLog.setObjectId(rs.getLong("object_id"));
        auditLog.setBeforeValue(parseJson(rs.getString("before_value")));
        auditLog.setAfterValue(parseJson(rs.getString("after_value")));
        auditLog.setCreatedAt(rs.getTimestamp("created_at"));
        return auditLog;
    }

    private com.google.gson.JsonElement parseJson(String value) {
        return value == null ? JsonNull.INSTANCE : JsonParser.parseString(value);
    }

    private String toJson(com.google.gson.JsonElement value) {
        return value == null ? "null" : value.toString();
    }
}
