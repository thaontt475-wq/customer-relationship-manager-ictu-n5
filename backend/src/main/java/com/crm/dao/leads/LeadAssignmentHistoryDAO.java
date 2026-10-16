package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.model.leads.AssignmentHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeadAssignmentHistoryDAO {

    public long insert(Connection conn, AssignmentHistory history) throws SQLException {
        try {
            String sql = """
                INSERT INTO lead_assignment_history
                (lead_id, lead_name, lead_phone, company_name, rep_id, rep_name, rep_dept, method, action_type, note, assigned_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setObject(1, history.getLeadId(), Types.BIGINT);
                stmt.setString(2, history.getLeadName());
                stmt.setString(3, history.getLeadPhone());
                stmt.setString(4, history.getCompanyName());
                stmt.setString(5, history.getRepId());
                stmt.setString(6, history.getRepName());
                stmt.setString(7, history.getRepDept());
                stmt.setString(8, history.getMethod());
                stmt.setString(9, history.getActionType() != null ? history.getActionType() : "ASSIGN");
                stmt.setString(10, history.getNote());
                stmt.setTimestamp(11, history.getAssignedAt() != null ? history.getAssignedAt() : new Timestamp(System.currentTimeMillis()));

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        history.setId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            String fallbackSql = """
                INSERT INTO lead_assignment_history
                (lead_id, lead_name, lead_phone, company_name, rep_id, rep_name, rep_dept, method, note, assigned_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement stmt = conn.prepareStatement(fallbackSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setObject(1, history.getLeadId(), Types.BIGINT);
                stmt.setString(2, history.getLeadName());
                stmt.setString(3, history.getLeadPhone());
                stmt.setString(4, history.getCompanyName());
                stmt.setString(5, history.getRepId());
                stmt.setString(6, history.getRepName());
                stmt.setString(7, history.getRepDept());
                stmt.setString(8, history.getMethod());
                stmt.setString(9, history.getNote());
                stmt.setTimestamp(10, history.getAssignedAt() != null ? history.getAssignedAt() : new Timestamp(System.currentTimeMillis()));

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        history.setId(id);
                        return id;
                    }
                }
            }
        }
        return history.getId() != null ? history.getId() : 1L;
    }

    public long insert(AssignmentHistory history) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            insert(conn, history);
            return history.getId() != null ? history.getId() : 1L;
        } catch (SQLException e) {
            System.err.println("Error inserting lead assignment history: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public List<AssignmentHistory> findAll(int limit) {
        String sql = "SELECT * FROM lead_assignment_history ORDER BY assigned_at DESC, id DESC LIMIT ?";
        List<AssignmentHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 100);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching lead assignment history: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<AssignmentHistory> findByLeadId(long leadId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findByLeadId(conn, leadId);
        }
    }

    public List<AssignmentHistory> findByLeadId(Connection conn, long leadId) throws SQLException {
        List<AssignmentHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM lead_assignment_history WHERE lead_id = ? ORDER BY assigned_at DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, leadId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private AssignmentHistory mapRow(ResultSet rs) throws SQLException {
        AssignmentHistory h = new AssignmentHistory();
        h.setId(rs.getLong("id"));
        h.setLeadId(rs.getObject("lead_id", Long.class));
        h.setLeadName(rs.getString("lead_name"));
        h.setLeadPhone(rs.getString("lead_phone"));
        h.setCompanyName(rs.getString("company_name"));
        h.setRepId(rs.getString("rep_id"));
        h.setRepName(rs.getString("rep_name"));
        h.setRepDept(rs.getString("rep_dept"));
        h.setMethod(rs.getString("method"));
        try {
            h.setActionType(rs.getString("action_type"));
        } catch (SQLException ignored) {}
        h.setNote(rs.getString("note"));
        h.setAssignedAt(rs.getTimestamp("assigned_at"));
        return h;
    }
}
