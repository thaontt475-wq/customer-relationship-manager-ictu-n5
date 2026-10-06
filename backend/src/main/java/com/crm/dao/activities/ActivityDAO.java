package com.crm.dao.activities;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;

import java.sql.*;
import java.util.*;

public class ActivityDAO {

    public List<Map<String, Object>> search(
            DataScopeContext scope,
            Long customerId,
            Long opportunityId,
            String type,
            String status
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.id,
                    a.subject,
                    a.type,
                    a.description,
                    a.status,
                    a.due_date,
                    a.customer_id,
                    a.opportunity_id,
                    a.owner_user_id,
                    a.created_at,
                    u.full_name AS owner_name,
                    c.name AS customer_name,
                    o.name AS opportunity_name
                FROM activities a
                LEFT JOIN users u ON u.id = a.owner_user_id
                LEFT JOIN customers c ON c.id = a.customer_id
                LEFT JOIN opportunities o ON o.id = a.opportunity_id
                WHERE a.is_deleted = 0
                """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("a.owner_user_id", sql, params);
        }

        if (customerId != null) {
            sql.append(" AND a.customer_id = ?");
            params.add(customerId);
        }

        if (opportunityId != null) {
            sql.append(" AND a.opportunity_id = ?");
            params.add(opportunityId);
        }

        if (type != null && !type.isBlank()) {
            sql.append(" AND a.type = ?");
            params.add(type.trim());
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status.trim());
        }

        sql.append(" ORDER BY a.id DESC");

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapActivity(rs));
                }
            }
        }
        return list;
    }

    public Map<String, Object> findById(long id) throws SQLException {
        String sql = """
                SELECT
                    a.id,
                    a.subject,
                    a.type,
                    a.description,
                    a.status,
                    a.due_date,
                    a.customer_id,
                    a.opportunity_id,
                    a.owner_user_id,
                    a.created_at,
                    u.full_name AS owner_name,
                    c.name AS customer_name,
                    o.name AS opportunity_name
                FROM activities a
                LEFT JOIN users u ON u.id = a.owner_user_id
                LEFT JOIN customers c ON c.id = a.customer_id
                LEFT JOIN opportunities o ON o.id = a.opportunity_id
                WHERE a.id = ? AND a.is_deleted = 0
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapActivity(rs);
                }
            }
        }
        return null;
    }

    public long create(
            String subject,
            String type,
            String description,
            String status,
            Timestamp dueDate,
            Long customerId,
            Long opportunityId,
            long ownerId
    ) throws SQLException {
        String sql = """
                INSERT INTO activities (
                    subject, type, description, status, due_date, customer_id, opportunity_id, owner_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, subject);
            stmt.setString(2, type != null ? type : "CALL");
            stmt.setString(3, description);
            stmt.setString(4, status != null ? status : "COMPLETED");
            stmt.setTimestamp(5, dueDate);
            stmt.setObject(6, customerId, Types.BIGINT);
            stmt.setObject(7, opportunityId, Types.BIGINT);
            stmt.setLong(8, ownerId);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Không thể tạo hoạt động");
    }

    public void update(
            long id,
            String subject,
            String type,
            String description,
            String status,
            Timestamp dueDate
    ) throws SQLException {
        String sql = """
                UPDATE activities SET
                    subject = ?, type = ?, description = ?, status = ?, due_date = ?
                WHERE id = ? AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject);
            stmt.setString(2, type);
            stmt.setString(3, description);
            stmt.setString(4, status);
            stmt.setTimestamp(5, dueDate);
            stmt.setLong(6, id);
            stmt.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        String sql = "UPDATE activities SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Map<String, Object> mapActivity(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("subject", rs.getString("subject"));
        map.put("type", rs.getString("type"));
        map.put("description", rs.getString("description"));
        map.put("status", rs.getString("status"));
        map.put("dueDate", rs.getTimestamp("due_date"));
        map.put("customerId", rs.getObject("customer_id"));
        map.put("customerName", rs.getString("customer_name"));
        map.put("opportunityId", rs.getObject("opportunity_id"));
        map.put("opportunityName", rs.getString("opportunity_name"));
        map.put("ownerUserId", rs.getLong("owner_user_id"));
        map.put("ownerName", rs.getString("owner_name"));
        map.put("createdAt", rs.getTimestamp("created_at"));
        return map;
    }
}
