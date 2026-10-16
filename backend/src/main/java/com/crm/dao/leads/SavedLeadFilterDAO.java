package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.model.leads.SavedLeadFilter;
import com.crm.util.JsonUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavedLeadFilterDAO {

    public List<SavedLeadFilter> findByUserId(long userId) {
        String sql = "SELECT * FROM saved_lead_filters WHERE user_id = ? ORDER BY is_default DESC, id DESC";
        List<SavedLeadFilter> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFilter(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching saved lead filters: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public SavedLeadFilter findById(long id) {
        String sql = "SELECT * FROM saved_lead_filters WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFilter(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding saved lead filter by id: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public long insert(SavedLeadFilter filter) {
        String sql = "INSERT INTO saved_lead_filters (user_id, name, filter_criteria, is_default) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, filter.getUserId());
            ps.setString(2, filter.getName());

            String criteriaJson = filter.getFilterCriteria();
            if (criteriaJson == null && filter.getFilter() != null) {
                criteriaJson = JsonUtil.getGson().toJson(filter.getFilter());
            }
            ps.setString(3, criteriaJson != null ? criteriaJson : "{}");
            ps.setBoolean(4, filter.isDefault());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        long genId = keys.getLong(1);
                        filter.setId(genId);
                        return genId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting saved lead filter: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean update(SavedLeadFilter filter) {
        String sql = "UPDATE saved_lead_filters SET name = ?, filter_criteria = ?, is_default = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, filter.getName());
            String criteriaJson = filter.getFilterCriteria();
            if (criteriaJson == null && filter.getFilter() != null) {
                criteriaJson = JsonUtil.getGson().toJson(filter.getFilter());
            }
            ps.setString(2, criteriaJson != null ? criteriaJson : "{}");
            ps.setBoolean(3, filter.isDefault());

            long id = parseId(filter.getId());
            ps.setLong(4, id);
            ps.setLong(5, filter.getUserId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating saved lead filter: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(long id, long userId) {
        String sql = "DELETE FROM saved_lead_filters WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting saved lead filter: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean setDefault(long id, long userId) {
        String clearSql = "UPDATE saved_lead_filters SET is_default = 0 WHERE user_id = ?";
        String setSql = "UPDATE saved_lead_filters SET is_default = 1 WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement clearPs = conn.prepareStatement(clearSql);
                 PreparedStatement setPs = conn.prepareStatement(setSql)) {

                clearPs.setLong(1, userId);
                clearPs.executeUpdate();

                setPs.setLong(1, id);
                setPs.setLong(2, userId);
                int affected = setPs.executeUpdate();

                conn.commit();
                return affected > 0;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("Error setting default saved lead filter: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private SavedLeadFilter mapResultSetToFilter(ResultSet rs) throws SQLException {
        SavedLeadFilter filter = new SavedLeadFilter();
        long id = rs.getLong("id");
        filter.setId("custom_filter_" + id);
        filter.setUserId(rs.getLong("user_id"));
        filter.setName(rs.getString("name"));
        String criteriaStr = rs.getString("filter_criteria");
        filter.setFilterCriteria(criteriaStr);

        try {
            if (criteriaStr != null && !criteriaStr.isBlank()) {
                filter.setFilter(JsonUtil.getGson().fromJson(criteriaStr, Object.class));
            }
        } catch (Exception ignored) {
        }

        filter.setDefault(rs.getBoolean("is_default"));
        filter.setSystem(false);
        filter.setCreatedAt(rs.getTimestamp("created_at"));
        filter.setUpdatedAt(rs.getTimestamp("updated_at"));
        return filter;
    }

    private long parseId(Object idObj) {
        if (idObj instanceof Number n) return n.longValue();
        if (idObj == null) return -1;
        String s = idObj.toString().replaceAll("[^0-9]", "");
        return s.isEmpty() ? -1 : Long.parseLong(s);
    }
}
