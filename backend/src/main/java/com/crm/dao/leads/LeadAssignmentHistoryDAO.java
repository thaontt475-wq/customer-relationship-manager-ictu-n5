package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.model.leads.AssignmentHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeadAssignmentHistoryDAO {

    public long insert(AssignmentHistory history) {
        String sql = "INSERT INTO lead_assignment_history (lead_id, lead_name, lead_phone, company_name, " +
                "rep_id, rep_name, rep_dept, method, note, assigned_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (history.getLeadId() != null) {
                ps.setLong(1, history.getLeadId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, history.getLeadName());
            ps.setString(3, history.getLeadPhone());
            ps.setString(4, history.getCompanyName());
            ps.setString(5, history.getRepId());
            ps.setString(6, history.getRepName());
            ps.setString(7, history.getRepDept());
            ps.setString(8, history.getMethod());
            ps.setString(9, history.getNote());

            if (history.getAssignedAt() != null) {
                ps.setTimestamp(10, history.getAssignedAt());
            } else {
                ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            }

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        history.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting lead assignment history: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public List<AssignmentHistory> findAll(int limit) {
        String sql = "SELECT * FROM lead_assignment_history ORDER BY assigned_at DESC, id DESC LIMIT ?";
        List<AssignmentHistory> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit > 0 ? limit : 100);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching lead assignment history: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<AssignmentHistory> findByLeadId(long leadId) {
        String sql = "SELECT * FROM lead_assignment_history WHERE lead_id = ? ORDER BY assigned_at DESC, id DESC";
        List<AssignmentHistory> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, leadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching lead assignment history for lead " + leadId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    private AssignmentHistory mapResultSet(ResultSet rs) throws SQLException {
        AssignmentHistory history = new AssignmentHistory();
        history.setId(rs.getLong("id"));

        long leadId = rs.getLong("lead_id");
        if (!rs.wasNull()) {
            history.setLeadId(leadId);
        }

        history.setLeadName(rs.getString("lead_name"));
        history.setLeadPhone(rs.getString("lead_phone"));
        history.setCompanyName(rs.getString("company_name"));
        history.setRepId(rs.getString("rep_id"));
        history.setRepName(rs.getString("rep_name"));
        history.setRepDept(rs.getString("rep_dept"));
        history.setMethod(rs.getString("method"));
        history.setNote(rs.getString("note"));
        history.setAssignedAt(rs.getTimestamp("assigned_at"));
        return history;
    }
}
