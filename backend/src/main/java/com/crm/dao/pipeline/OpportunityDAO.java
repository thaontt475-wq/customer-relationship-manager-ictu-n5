package com.crm.dao.pipeline;

import com.crm.config.DatabaseConfig;
import com.crm.dto.pipeline.OpportunityWriteRequest;
import com.crm.service.permissions.DataScopeContext;

import java.sql.*;
import java.util.*;

public class OpportunityDAO {

    public List<Map<String, Object>> search(
            DataScopeContext scope,
            Long stageId,
            String status,
            String keyword,
            int page,
            int size
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    o.id,
                    o.name,
                    o.customer_id,
                    o.contact_name,
                    o.amount,
                    o.stage_id,
                    o.probability,
                    o.expected_close_date,
                    o.status,
                    o.win_reason_id,
                    o.loss_reason_id,
                    o.competitor_id,
                    o.lost_reason,
                    o.owner_user_id,
                    o.created_at,
                    c.name AS customer_name,
                    ps.name AS stage_name,
                    ps.order_no AS stage_order,
                    u.full_name AS owner_name,
                    comp.name AS competitor_name
                FROM opportunities o
                LEFT JOIN customers c ON c.id = o.customer_id
                LEFT JOIN pipeline_stages ps ON ps.id = o.stage_id
                LEFT JOIN users u ON u.id = o.owner_user_id
                LEFT JOIN competitors comp ON comp.id = o.competitor_id
                WHERE o.is_deleted = 0
                """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("o.owner_user_id", sql, params);
        }

        if (stageId != null) {
            sql.append(" AND o.stage_id = ?");
            params.add(stageId);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND o.status = ?");
            params.add(status.trim());
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(o.name) LIKE ? OR LOWER(o.contact_name) LIKE ? OR LOWER(c.name) LIKE ?)");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        sql.append(" ORDER BY o.id DESC");
        if (size > 0) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add((page - 1) * size);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOpportunity(rs));
                }
            }
        }
        return list;
    }

    public Map<String, Object> findById(long id) throws SQLException {
        String sql = """
                SELECT
                    o.id,
                    o.name,
                    o.customer_id,
                    o.contact_name,
                    o.amount,
                    o.stage_id,
                    o.probability,
                    o.expected_close_date,
                    o.status,
                    o.win_reason_id,
                    o.loss_reason_id,
                    o.competitor_id,
                    o.lost_reason,
                    o.owner_user_id,
                    o.created_at,
                    c.name AS customer_name,
                    ps.name AS stage_name,
                    ps.order_no AS stage_order,
                    u.full_name AS owner_name,
                    comp.name AS competitor_name
                FROM opportunities o
                LEFT JOIN customers c ON c.id = o.customer_id
                LEFT JOIN pipeline_stages ps ON ps.id = o.stage_id
                LEFT JOIN users u ON u.id = o.owner_user_id
                LEFT JOIN competitors comp ON comp.id = o.competitor_id
                WHERE o.id = ? AND o.is_deleted = 0
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapOpportunity(rs);
                }
            }
        }
        return null;
    }

    public long create(OpportunityWriteRequest req, long defaultOwnerId) throws SQLException {
        String sql = """
                INSERT INTO opportunities (
                    name, customer_id, contact_name, amount, stage_id, probability,
                    expected_close_date, status, win_reason_id, loss_reason_id, competitor_id,
                    lost_reason, owner_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        long ownerId = req.getOwnerUserId() != null ? req.getOwnerUserId() : defaultOwnerId;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, req.getName());
            stmt.setObject(2, req.getCustomerId(), Types.BIGINT);
            stmt.setString(3, req.getContactName());
            stmt.setBigDecimal(4, req.getAmount());
            stmt.setObject(5, req.getStageId(), Types.BIGINT);
            stmt.setObject(6, req.getProbability(), Types.INTEGER);
            stmt.setObject(7, req.getExpectedCloseDate() != null && !req.getExpectedCloseDate().isBlank() ? java.sql.Date.valueOf(req.getExpectedCloseDate()) : null, Types.DATE);
            stmt.setString(8, req.getStatus());
            stmt.setObject(9, req.getWinReasonId(), Types.BIGINT);
            stmt.setObject(10, req.getLossReasonId(), Types.BIGINT);
            stmt.setObject(11, req.getCompetitorId(), Types.BIGINT);
            stmt.setString(12, req.getLostReason());
            stmt.setLong(13, ownerId);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Không thể tạo cơ hội bán hàng");
    }

    public void update(long id, OpportunityWriteRequest req) throws SQLException {
        String sql = """
                UPDATE opportunities SET
                    name = ?, customer_id = ?, contact_name = ?, amount = ?, stage_id = ?,
                    probability = ?, expected_close_date = ?, status = ?, win_reason_id = ?,
                    loss_reason_id = ?, competitor_id = ?, lost_reason = ?
                WHERE id = ? AND is_deleted = 0
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, req.getName());
            stmt.setObject(2, req.getCustomerId(), Types.BIGINT);
            stmt.setString(3, req.getContactName());
            stmt.setBigDecimal(4, req.getAmount());
            stmt.setObject(5, req.getStageId(), Types.BIGINT);
            stmt.setObject(6, req.getProbability(), Types.INTEGER);
            stmt.setObject(7, req.getExpectedCloseDate() != null && !req.getExpectedCloseDate().isBlank() ? java.sql.Date.valueOf(req.getExpectedCloseDate()) : null, Types.DATE);
            stmt.setString(8, req.getStatus());
            stmt.setObject(9, req.getWinReasonId(), Types.BIGINT);
            stmt.setObject(10, req.getLossReasonId(), Types.BIGINT);
            stmt.setObject(11, req.getCompetitorId(), Types.BIGINT);
            stmt.setString(12, req.getLostReason());
            stmt.setLong(13, id);

            stmt.executeUpdate();
        }
    }

    public void updateStage(long id, long targetStageId, int probability) throws SQLException {
        String sql = "UPDATE opportunities SET stage_id = ?, probability = ? WHERE id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, targetStageId);
            stmt.setInt(2, probability);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public void closeDeal(long id, String status, Long winReasonId, Long lossReasonId, Long competitorId, String lostReason) throws SQLException {
        String sql = """
                UPDATE opportunities SET
                    status = ?, win_reason_id = ?, loss_reason_id = ?, competitor_id = ?, lost_reason = ?
                WHERE id = ? AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setObject(2, winReasonId, Types.BIGINT);
            stmt.setObject(3, lossReasonId, Types.BIGINT);
            stmt.setObject(4, competitorId, Types.BIGINT);
            stmt.setString(5, lostReason);
            stmt.setLong(6, id);
            stmt.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        String sql = "UPDATE opportunities SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Map<String, Object> mapOpportunity(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("name", rs.getString("name"));
        map.put("customerId", rs.getObject("customer_id"));
        map.put("customerName", rs.getString("customer_name"));
        map.put("contactName", rs.getString("contact_name"));
        map.put("amount", rs.getBigDecimal("amount"));
        map.put("stageId", rs.getObject("stage_id"));
        map.put("stageName", rs.getString("stage_name"));
        map.put("stageOrder", rs.getObject("stage_order"));
        map.put("probability", rs.getObject("probability"));
        map.put("expectedCloseDate", rs.getDate("expected_close_date"));
        map.put("status", rs.getString("status"));
        map.put("winReasonId", rs.getObject("win_reason_id"));
        map.put("lossReasonId", rs.getObject("loss_reason_id"));
        map.put("competitorId", rs.getObject("competitor_id"));
        map.put("competitorName", rs.getString("competitor_name"));
        map.put("lostReason", rs.getString("lost_reason"));
        map.put("ownerUserId", rs.getLong("owner_user_id"));
        map.put("ownerName", rs.getString("owner_name"));
        map.put("createdAt", rs.getTimestamp("created_at"));
        return map;
    }
}
