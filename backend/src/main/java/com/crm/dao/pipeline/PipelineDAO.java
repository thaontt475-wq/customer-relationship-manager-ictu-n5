package com.crm.dao.pipeline;

import com.crm.model.PipelineStage;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Pipeline Stages (CRM-47).
 */
public class PipelineDAO {
    private static final Logger LOGGER = Logger.getLogger(PipelineDAO.class.getName());

    public PipelineStage findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT id, pipeline_id, code, name, stage_order, win_probability, requirements, "
                + "is_won, is_lost, is_active, created_at, updated_at "
                + "FROM pipeline_stages WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PipelineStage stage = mapRowToStage(rs);
                    stage.setOpportunityCount(countOpportunitiesByStage(conn, id));
                    return stage;
                }
            }
        }
        return null;
    }

    public PipelineStage findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public PipelineStage findByCode(Connection conn, long pipelineId, String code) throws SQLException {
        if (code == null) {
            return null;
        }
        String sql = "SELECT id, pipeline_id, code, name, stage_order, win_probability, requirements, "
                + "is_won, is_lost, is_active, created_at, updated_at "
                + "FROM pipeline_stages WHERE pipeline_id = ? AND code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pipelineId);
            stmt.setString(2, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToStage(rs);
                }
            }
        }
        return null;
    }

    public boolean existsByCode(Connection conn, long pipelineId, String code, Long excludeId) throws SQLException {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        StringBuilder sql = new StringBuilder("SELECT 1 FROM pipeline_stages WHERE pipeline_id = ? AND code = ?");
        if (excludeId != null && excludeId > 0) {
            sql.append(" AND id <> ?");
        }
        sql.append(" LIMIT 1");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, pipelineId);
            stmt.setString(2, code.trim());
            if (excludeId != null && excludeId > 0) {
                stmt.setLong(3, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByCode(long pipelineId, String code, Long excludeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return existsByCode(conn, pipelineId, code, excludeId);
        }
    }

    /**
     * Retrieve all stages for a pipeline, sorted strictly by stage_order ASC, id ASC.
     */
    public List<PipelineStage> findAll(Connection conn, long pipelineId, Boolean activeOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, pipeline_id, code, name, stage_order, win_probability, requirements, "
                        + "is_won, is_lost, is_active, created_at, updated_at "
                        + "FROM pipeline_stages WHERE pipeline_id = ?"
        );
        if (activeOnly != null) {
            sql.append(" AND is_active = ?");
        }

        // CRITICAL BUSINESS RULE 2: Order strictly by stage_order ASC
        sql.append(" ORDER BY stage_order ASC, id ASC");

        List<PipelineStage> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setLong(1, pipelineId);
            if (activeOnly != null) {
                stmt.setBoolean(2, activeOnly);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToStage(rs));
                }
            }
        }

        // Populate opportunity count for each stage
        for (PipelineStage stage : list) {
            stage.setOpportunityCount(countOpportunitiesByStage(conn, stage.getId()));
        }

        return list;
    }

    public List<PipelineStage> findAll(long pipelineId, Boolean activeOnly) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn, pipelineId, activeOnly);
        }
    }

    public long insert(Connection conn, PipelineStage stage) throws SQLException {
        String sql = "INSERT INTO pipeline_stages (pipeline_id, code, name, stage_order, win_probability, "
                + "requirements, is_won, is_lost, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, stage.getPipelineId() != null ? stage.getPipelineId() : 1L);
            stmt.setString(2, stage.getCode().trim());
            stmt.setString(3, stage.getName().trim());
            stmt.setInt(4, stage.getStageOrder());
            stmt.setInt(5, stage.getWinProbability());
            stmt.setString(6, stage.getRequirements() != null ? stage.getRequirements().trim() : null);
            stmt.setBoolean(7, stage.isWon());
            stmt.setBoolean(8, stage.isLost());
            stmt.setBoolean(9, stage.isActive());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        long id = keys.getLong(1);
                        stage.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public long insert(PipelineStage stage) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return insert(conn, stage);
        }
    }

    public int update(Connection conn, PipelineStage stage) throws SQLException {
        String sql = "UPDATE pipeline_stages SET code = ?, name = ?, stage_order = ?, win_probability = ?, "
                + "requirements = ?, is_won = ?, is_lost = ?, is_active = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, stage.getCode().trim());
            stmt.setString(2, stage.getName().trim());
            stmt.setInt(3, stage.getStageOrder());
            stmt.setInt(4, stage.getWinProbability());
            stmt.setString(5, stage.getRequirements() != null ? stage.getRequirements().trim() : null);
            stmt.setBoolean(6, stage.isWon());
            stmt.setBoolean(7, stage.isLost());
            stmt.setBoolean(8, stage.isActive());
            stmt.setLong(9, stage.getId());

            return stmt.executeUpdate();
        }
    }

    public int update(PipelineStage stage) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, stage);
        }
    }

    public int updateStageOrder(Connection conn, long id, int stageOrder) throws SQLException {
        String sql = "UPDATE pipeline_stages SET stage_order = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, stageOrder);
            stmt.setLong(2, id);
            return stmt.executeUpdate();
        }
    }

    public int updateStageOrder(long id, int stageOrder) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return updateStageOrder(conn, id, stageOrder);
        }
    }

    public int delete(Connection conn, long id) throws SQLException {
        String sql = "DELETE FROM pipeline_stages WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate();
        }
    }

    public int delete(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        }
    }

    /**
     * Counts how many opportunities are currently assigned to this stage.
     */
    public long countOpportunitiesByStage(Connection conn, long stageId) {
        if (stageId <= 0) {
            return 0;
        }
        if (!tableHasColumn(conn, "opportunities", "stage_id")) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM opportunities WHERE stage_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, stageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Failed to count opportunities for stage " + stageId, e);
        }
        return 0;
    }

    public long countOpportunitiesByStage(long stageId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return countOpportunitiesByStage(conn, stageId);
        }
    }

    /**
     * Checks if a stage is currently in use by any opportunities.
     */
    public boolean isStageInUse(Connection conn, long stageId) {
        return countOpportunitiesByStage(conn, stageId) > 0;
    }

    public boolean isStageInUse(long stageId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return isStageInUse(conn, stageId);
        }
    }

    /**
     * Reassigns all opportunities from an old stage to a target stage
     * (used when safely retiring/deleting an old stage).
     */
    public int reassignOpportunities(Connection conn, long fromStageId, long toStageId) throws SQLException {
        if (!tableHasColumn(conn, "opportunities", "stage_id")) {
            return 0;
        }
        String sql = "UPDATE opportunities SET stage_id = ? WHERE stage_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, toStageId);
            stmt.setLong(2, fromStageId);
            return stmt.executeUpdate();
        }
    }

    public int reassignOpportunities(long fromStageId, long toStageId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return reassignOpportunities(conn, fromStageId, toStageId);
        }
    }

    private boolean tableHasColumn(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, columnName)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private PipelineStage mapRowToStage(ResultSet rs) throws SQLException {
        PipelineStage s = new PipelineStage();
        s.setId(rs.getLong("id"));
        s.setPipelineId(rs.getLong("pipeline_id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setStageOrder(rs.getInt("stage_order"));
        s.setWinProbability(rs.getInt("win_probability"));
        s.setRequirements(rs.getString("requirements"));
        s.setWon(rs.getBoolean("is_won"));
        s.setLost(rs.getBoolean("is_lost"));
        s.setActive(rs.getBoolean("is_active"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setUpdatedAt(rs.getTimestamp("updated_at"));
        return s;
    }
}