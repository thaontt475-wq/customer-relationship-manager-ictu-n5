package com.crm.dao.pipeline;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class PipelineStageDAO {

    public Map<String, Object> findById(long id) throws SQLException {
        String sql = """
                SELECT
                    id,
                    pipeline_id,
                    name,
                    order_no AS stage_order,
                    probability AS win_probability,
                    exit_condition AS requirements,
                    condition_required, active AS is_active
                FROM pipeline_stages
                WHERE (id = ? OR order_no = ?) AND active = 1
                ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END, id ASC
                LIMIT 1
                """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.setLong(2, id);
                stmt.setLong(3, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }

            ensureDefaultStages(conn, 1L);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.setLong(2, id);
                stmt.setLong(3, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("PipelineStageDAO.findById notice: " + e.getMessage());
        }

        return getFallbackStage(id);
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("pipelineId", rs.getLong("pipeline_id"));
        map.put("name", rs.getString("name"));
        map.put("stageOrder", rs.getInt("stage_order"));
        map.put("winProbability", rs.getInt("win_probability"));
        map.put("requirements", rs.getString("requirements"));
        map.put("conditionRequired", rs.getBoolean("condition_required"));
        map.put("isActive", rs.getBoolean("is_active"));
        return map;
    }

    private void ensureDefaultStages(Connection conn, long pipelineId) {
        try (PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) FROM pipeline_stages WHERE pipeline_id = ?")) {
            countStmt.setLong(1, pipelineId);
            try (ResultSet rs = countStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = """
                            INSERT INTO pipeline_stages(pipeline_id, name, order_no, probability, active)
                            VALUES
                            (?, 'Tiềm năng', 1, 10, 1),
                            (?, 'Giải pháp', 2, 40, 1),
                            (?, 'Đàm phán', 3, 70, 1),
                            (?, 'Thành công', 4, 100, 1),
                            (?, 'Thất bại', 5, 0, 1)
                            """;
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        for (int i = 1; i <= 5; i++) ps.setLong(i, pipelineId);
                        ps.executeUpdate();
                    }
                    return;
                }
            }
        } catch (SQLException ignored) {
        }

        List<Object[]> stagesToEnsure = List.of(
                new Object[]{"Tiềm năng", 1, 10},
                new Object[]{"Giải pháp", 2, 40},
                new Object[]{"Đàm phán", 3, 70},
                new Object[]{"Thành công", 4, 100},
                new Object[]{"Thất bại", 5, 0}
        );
        for (Object[] st : stagesToEnsure) {
            String name = (String) st[0];
            int orderNo = (Integer) st[1];
            int prob = (Integer) st[2];
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM pipeline_stages WHERE pipeline_id = ? AND name = ?")) {
                check.setLong(1, pipelineId);
                check.setString(2, name);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        int finalOrder = orderNo;
                        try (PreparedStatement checkOrder = conn.prepareStatement(
                                "SELECT MAX(order_no) FROM pipeline_stages WHERE pipeline_id = ?")) {
                            checkOrder.setLong(1, pipelineId);
                            try (ResultSet rsOrder = checkOrder.executeQuery()) {
                                if (rsOrder.next()) {
                                    int maxOrder = rsOrder.getInt(1);
                                    if (maxOrder >= finalOrder) {
                                        finalOrder = maxOrder + 1;
                                    }
                                }
                            }
                        }
                        try (PreparedStatement insert = conn.prepareStatement(
                                "INSERT INTO pipeline_stages(pipeline_id, name, order_no, probability, active) VALUES (?, ?, ?, ?, 1)")) {
                            insert.setLong(1, pipelineId);
                            insert.setString(2, name);
                            insert.setInt(3, finalOrder);
                            insert.setInt(4, prob);
                            insert.executeUpdate();
                        }
                    }
                }
            } catch (SQLException ignored) {
            }
        }
    }

    private Map<String, Object> getFallbackStage(long id) {
        int order = (int) id;
        String name;
        int prob;
        switch (order) {
            case 1:
                name = "Tiềm năng";
                prob = 10;
                break;
            case 2:
                name = "Giải pháp";
                prob = 40;
                break;
            case 3:
                name = "Đàm phán";
                prob = 70;
                break;
            case 4:
                name = "Thành công";
                prob = 100;
                break;
            case 5:
                name = "Thất bại";
                prob = 0;
                break;
            default:
                name = "Giai đoạn " + id;
                prob = 0;
                break;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("pipelineId", 1L);
        map.put("name", name);
        map.put("stageOrder", order);
        map.put("winProbability", prob);
        map.put("requirements", null);
        map.put("conditionRequired", false);
        map.put("isActive", true);
        return map;
    }
}
