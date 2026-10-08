package com.crm.dao.pipeline;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class PipelineStageDAO {

    public Map<String, Object> findById(long id) throws SQLException {
        String sql = """
                SELECT id, pipeline_id, code, name, stage_order, win_probability, requirements, is_won, is_lost, is_active
                FROM pipeline_stages
                WHERE id = ? AND is_active = 1
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", rs.getLong("id"));
                    map.put("pipelineId", rs.getLong("pipeline_id"));
                    map.put("code", rs.getString("code"));
                    map.put("name", rs.getString("name"));
                    map.put("stageOrder", rs.getInt("stage_order"));
                    map.put("winProbability", rs.getInt("win_probability"));
                    map.put("requirements", rs.getString("requirements"));
                    map.put("isWon", rs.getBoolean("is_won"));
                    map.put("isLost", rs.getBoolean("is_lost"));
                    map.put("isActive", rs.getBoolean("is_active"));
                    return map;
                }
            }
        }
        return null;
    }
}
