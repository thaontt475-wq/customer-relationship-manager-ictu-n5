package com.crm.dao.campaigns;

import com.crm.config.DatabaseConfig;
import com.crm.model.campaigns.Campaign;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CampaignDAO {

    public List<Campaign> findPaged(String keyword, String status, int offset, int limit) throws SQLException {
        List<Campaign> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT c.*, u.full_name AS owner_name,
                   (SELECT COUNT(*) FROM campaign_leads cl WHERE cl.campaign_id = c.id) AS lead_count
            FROM campaigns c
            LEFT JOIN users u ON u.id = c.owner_id
            WHERE c.is_deleted = 0
        """);

        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (c.name LIKE ? OR c.code LIKE ?)");
            params.add("%" + keyword.trim() + "%");
            params.add("%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND c.status = ?");
            params.add(status.trim());
        }

        sql.append(" ORDER BY c.id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public int countTotal(String keyword, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM campaigns WHERE is_deleted = 0");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (name LIKE ? OR code LIKE ?)");
            params.add("%" + keyword.trim() + "%");
            params.add("%" + keyword.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status.trim());
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Campaign findById(long id) throws SQLException {
        String sql = """
            SELECT c.*, u.full_name AS owner_name,
                   (SELECT COUNT(*) FROM campaign_leads cl WHERE cl.campaign_id = c.id) AS lead_count
            FROM campaigns c
            LEFT JOIN users u ON u.id = c.owner_id
            WHERE c.id = ? AND c.is_deleted = 0
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean existsByCode(String code, Long excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM campaigns WHERE code = ? AND is_deleted = 0"
                + (excludeId != null ? " AND id <> ?" : "");
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public long insert(Campaign c) throws SQLException {
        String sql = """
            INSERT INTO campaigns(code, name, type, status, start_date, end_date, budget, actual_cost, expected_revenue, description, owner_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getCode());
            ps.setString(2, c.getName());
            ps.setString(3, c.getType());
            ps.setString(4, c.getStatus());
            ps.setDate(5, c.getStartDate() != null ? Date.valueOf(c.getStartDate()) : null);
            ps.setDate(6, c.getEndDate() != null ? Date.valueOf(c.getEndDate()) : null);
            ps.setBigDecimal(7, c.getBudget());
            ps.setBigDecimal(8, c.getActualCost());
            ps.setBigDecimal(9, c.getExpectedRevenue());
            ps.setString(10, c.getDescription());
            ps.setLong(11, c.getOwnerId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Thêm chiến dịch thất bại, không nhận được ID.");
    }

    public boolean update(Campaign c) throws SQLException {
        String sql = """
            UPDATE campaigns
            SET code = ?, name = ?, type = ?, status = ?, start_date = ?, end_date = ?,
                budget = ?, actual_cost = ?, expected_revenue = ?, description = ?
            WHERE id = ? AND is_deleted = 0
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCode());
            ps.setString(2, c.getName());
            ps.setString(3, c.getType());
            ps.setString(4, c.getStatus());
            ps.setDate(5, c.getStartDate() != null ? Date.valueOf(c.getStartDate()) : null);
            ps.setDate(6, c.getEndDate() != null ? Date.valueOf(c.getEndDate()) : null);
            ps.setBigDecimal(7, c.getBudget());
            ps.setBigDecimal(8, c.getActualCost());
            ps.setBigDecimal(9, c.getExpectedRevenue());
            ps.setString(10, c.getDescription());
            ps.setLong(11, c.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(long id) throws SQLException {
        String sql = "UPDATE campaigns SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Campaign mapRow(ResultSet rs) throws SQLException {
        Campaign c = new Campaign();
        c.setId(rs.getLong("id"));
        c.setCode(rs.getString("code"));
        c.setName(rs.getString("name"));
        c.setType(rs.getString("type"));
        c.setStatus(rs.getString("status"));

        Date start = rs.getDate("start_date");
        if (start != null) c.setStartDate(start.toLocalDate());

        Date end = rs.getDate("end_date");
        if (end != null) c.setEndDate(end.toLocalDate());

        c.setBudget(rs.getBigDecimal("budget"));
        c.setActualCost(rs.getBigDecimal("actual_cost"));
        c.setExpectedRevenue(rs.getBigDecimal("expected_revenue"));
        c.setDescription(rs.getString("description"));
        c.setOwnerId(rs.getLong("owner_id"));
        c.setOwnerName(rs.getString("owner_name"));
        c.setLeadCount(rs.getInt("lead_count"));
        c.setDeleted(rs.getBoolean("is_deleted"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) c.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) c.setUpdatedAt(updatedAt.toLocalDateTime());

        return c;
    }
}
