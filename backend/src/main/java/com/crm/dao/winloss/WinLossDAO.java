package com.crm.dao.winloss;

import com.crm.model.Competitor;
import com.crm.model.WinLossReason;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WinLossDAO {
    public List<WinLossReason> findReasons(Connection connection, String type, boolean activeOnly)
            throws SQLException {
        String sql = "SELECT id, reason_type, reason_text, description, is_active, display_order, "
                + "created_at, updated_at FROM win_loss_reasons WHERE reason_type = ?"
                + (activeOnly ? " AND is_active = TRUE" : "")
                + " ORDER BY display_order, id";
        List<WinLossReason> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(mapReason(rs));
            }
        }
        return result;
    }

    public WinLossReason findReasonById(Connection connection, long id) throws SQLException {
        String sql = "SELECT id, reason_type, reason_text, description, is_active, display_order, "
                + "created_at, updated_at FROM win_loss_reasons WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapReason(rs) : null; }
        }
    }

    public boolean reasonExists(Connection connection, String type, String text, Long excludeId)
            throws SQLException {
        String sql = "SELECT 1 FROM win_loss_reasons WHERE reason_type = ? AND reason_text = ?"
                + (excludeId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.setString(2, text);
            if (excludeId != null) statement.setLong(3, excludeId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    public int nextReasonOrder(Connection connection, String type) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(display_order), 0) + 1 FROM win_loss_reasons WHERE reason_type = ?")) {
            statement.setString(1, type);
            try (ResultSet rs = statement.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public long insertReason(Connection connection, WinLossReason reason) throws SQLException {
        String sql = "INSERT INTO win_loss_reasons "
                + "(reason_type, reason_text, description, is_active, display_order) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindReason(statement, reason);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được ID lý do vừa tạo");
                return keys.getLong(1);
            }
        }
    }

    public int updateReason(Connection connection, WinLossReason reason) throws SQLException {
        String sql = "UPDATE win_loss_reasons SET reason_type = ?, reason_text = ?, description = ?, "
                + "is_active = ?, display_order = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindReason(statement, reason);
            statement.setLong(6, reason.getId());
            return statement.executeUpdate();
        }
    }

    private void bindReason(PreparedStatement statement, WinLossReason reason) throws SQLException {
        statement.setString(1, reason.getType());
        statement.setString(2, reason.getReasonText());
        statement.setString(3, reason.getDescription());
        statement.setBoolean(4, reason.isActive());
        statement.setInt(5, reason.getDisplayOrder());
    }

    public int deleteReason(Connection connection, long id) throws SQLException {
        return executeById(connection, "DELETE FROM win_loss_reasons WHERE id = ?", id);
    }

    public int deactivateReason(Connection connection, long id) throws SQLException {
        return executeById(connection, "UPDATE win_loss_reasons SET is_active = FALSE WHERE id = ?", id);
    }

    public List<Competitor> findCompetitors(Connection connection, boolean activeOnly) throws SQLException {
        String sql = "SELECT id, name, strengths, weaknesses, website, is_active, display_order, "
                + "created_at, updated_at FROM competitors"
                + (activeOnly ? " WHERE is_active = TRUE" : "") + " ORDER BY display_order, id";
        List<Competitor> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) result.add(mapCompetitor(rs));
        }
        return result;
    }

    public Competitor findCompetitorById(Connection connection, long id) throws SQLException {
        String sql = "SELECT id, name, strengths, weaknesses, website, is_active, display_order, "
                + "created_at, updated_at FROM competitors WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? mapCompetitor(rs) : null; }
        }
    }

    public boolean competitorExists(Connection connection, String name, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM competitors WHERE name = ?"
                + (excludeId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (excludeId != null) statement.setLong(2, excludeId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    public int nextCompetitorOrder(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(display_order), 0) + 1 FROM competitors");
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public long insertCompetitor(Connection connection, Competitor competitor) throws SQLException {
        String sql = "INSERT INTO competitors "
                + "(name, strengths, weaknesses, website, is_active, display_order) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindCompetitor(statement, competitor);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được ID đối thủ vừa tạo");
                return keys.getLong(1);
            }
        }
    }

    public int updateCompetitor(Connection connection, Competitor competitor) throws SQLException {
        String sql = "UPDATE competitors SET name = ?, strengths = ?, weaknesses = ?, website = ?, "
                + "is_active = ?, display_order = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCompetitor(statement, competitor);
            statement.setLong(7, competitor.getId());
            return statement.executeUpdate();
        }
    }

    private void bindCompetitor(PreparedStatement statement, Competitor competitor) throws SQLException {
        statement.setString(1, competitor.getName());
        statement.setString(2, competitor.getStrengths());
        statement.setString(3, competitor.getWeaknesses());
        statement.setString(4, competitor.getWebsite());
        statement.setBoolean(5, competitor.isActive());
        statement.setInt(6, competitor.getDisplayOrder());
    }

    public int deleteCompetitor(Connection connection, long id) throws SQLException {
        return executeById(connection, "DELETE FROM competitors WHERE id = ?", id);
    }

    public int deactivateCompetitor(Connection connection, long id) throws SQLException {
        return executeById(connection, "UPDATE competitors SET is_active = FALSE WHERE id = ?", id);
    }

    private int executeById(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate();
        }
    }

    private WinLossReason mapReason(ResultSet rs) throws SQLException {
        WinLossReason reason = new WinLossReason();
        reason.setId(rs.getLong("id"));
        reason.setType(rs.getString("reason_type"));
        reason.setReasonText(rs.getString("reason_text"));
        reason.setDescription(rs.getString("description"));
        reason.setActive(rs.getBoolean("is_active"));
        reason.setDisplayOrder(rs.getInt("display_order"));
        reason.setCreatedAt(rs.getTimestamp("created_at"));
        reason.setUpdatedAt(rs.getTimestamp("updated_at"));
        return reason;
    }

    private Competitor mapCompetitor(ResultSet rs) throws SQLException {
        Competitor competitor = new Competitor();
        competitor.setId(rs.getLong("id"));
        competitor.setName(rs.getString("name"));
        competitor.setStrengths(rs.getString("strengths"));
        competitor.setWeaknesses(rs.getString("weaknesses"));
        competitor.setWebsite(rs.getString("website"));
        competitor.setActive(rs.getBoolean("is_active"));
        competitor.setDisplayOrder(rs.getInt("display_order"));
        competitor.setCreatedAt(rs.getTimestamp("created_at"));
        competitor.setUpdatedAt(rs.getTimestamp("updated_at"));
        return competitor;
    }

}
