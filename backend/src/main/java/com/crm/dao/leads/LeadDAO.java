package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;

import java.sql.*;
import java.util.*;

public class LeadDAO {

    public Lead findById(long id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findById(conn, id, false);
        }
    }

    public Lead findById(Connection conn, long id) throws SQLException {
        return findById(conn, id, false);
    }

    public Lead findById(Connection conn, long id, boolean forUpdate) throws SQLException {
        String sql = "SELECT * FROM leads WHERE id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Lead> findMyAssignedLeads(long userId, DataScopeContext scope) throws SQLException {
        List<Lead> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM leads WHERE ");

        if (scope != null && scope.scopeType() == ScopeType.ALL) {
            // ALL: Can view all leads that are assigned or in workflow
            sql.append("(assigned_to_user_id IS NOT NULL OR status IN ('PENDING_RESPONSE', 'CONTACTING', 'REJECTED')) ");
        } else if (scope != null && scope.scopeType() == ScopeType.TEAM) {
            // TEAM: Can view leads assigned to anyone in user's team
            Set<Long> owners = scope.allowedOwnerIds();
            if (owners == null || owners.isEmpty()) {
                sql.append("assigned_to_user_id = ").append(userId).append(" ");
            } else {
                sql.append("assigned_to_user_id IN (");
                int i = 0;
                for (Long oid : owners) {
                    if (i++ > 0) sql.append(",");
                    sql.append(oid);
                }
                sql.append(") ");
            }
        } else {
            // SELF: Only leads assigned to this specific user
            sql.append("(assigned_to_user_id = ? OR assigned_to = ? OR assigned_to = ?) ");
        }

        sql.append("ORDER BY FIELD(status, 'PENDING_RESPONSE', 'CONTACTING', 'REJECTED', 'PENDING_ASSIGNMENT'), id DESC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (scope == null || scope.scopeType() == ScopeType.SELF) {
                stmt.setLong(1, userId);
                stmt.setString(2, String.valueOf(userId));
                stmt.setString(3, "USR-" + userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public long insert(Lead lead) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return insert(conn, lead);
        }
    }

    public long insert(Connection conn, Lead lead) throws SQLException {
        String sql = """
            INSERT INTO leads
            (full_name, company_name, email, phone, title, source_id, source, region, region_code, industry,
             employee_count, score, notes, status, owner_user_id, assigned_to, assigned_to_user_id,
             assigned_at, sla_total_minutes, sla_deadline, is_locked, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, lead.getFullName());
            stmt.setString(2, lead.getCompanyName());
            stmt.setString(3, lead.getEmail());
            stmt.setString(4, lead.getPhone());
            stmt.setString(5, lead.getTitle() != null ? lead.getTitle() : "Khách hàng tiềm năng");
            stmt.setObject(6, lead.getSourceId(), Types.BIGINT);
            stmt.setString(7, lead.getSource() != null ? lead.getSource() : "WEB_FORM");
            stmt.setString(8, lead.getRegion());
            stmt.setString(9, lead.getRegionCode());
            stmt.setString(10, lead.getIndustry());
            stmt.setInt(11, lead.getEmployeeCount() != null ? lead.getEmployeeCount() : 0);
            stmt.setInt(12, lead.getScore() != null ? lead.getScore() : 0);
            stmt.setString(13, lead.getNotes());
            stmt.setString(14, lead.getStatus() != null ? lead.getStatus() : "PENDING_RESPONSE");
            stmt.setObject(15, lead.getOwnerUserId(), Types.BIGINT);
            stmt.setString(16, lead.getAssignedTo());
            stmt.setObject(17, lead.getAssignedToUserId(), Types.BIGINT);
            stmt.setTimestamp(18, lead.getAssignedAt() != null ? lead.getAssignedAt() : new Timestamp(System.currentTimeMillis()));
            stmt.setInt(19, lead.getSlaTotalMinutes() != null ? lead.getSlaTotalMinutes() : 120);
            stmt.setTimestamp(20, lead.getSlaDeadline());
            stmt.setBoolean(21, lead.isLocked());
            stmt.setObject(22, lead.getCreatedBy(), Types.BIGINT);
            stmt.setTimestamp(23, lead.getCreatedAt() != null ? lead.getCreatedAt() : new Timestamp(System.currentTimeMillis()));

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    lead.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public void updateAccept(Connection conn, Lead lead) throws SQLException {
        String sql = """
            UPDATE leads
            SET status = ?, accepted_at = ?, sla_result = ?, response_time_text = ?, contacting_stage = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, lead.getStatus());
            stmt.setTimestamp(2, lead.getAcceptedAt());
            stmt.setString(3, lead.getSlaResult());
            stmt.setString(4, lead.getResponseTimeText());
            stmt.setString(5, lead.getContactingStage());
            stmt.setLong(6, lead.getId());
            stmt.executeUpdate();
        }
    }

    public void updateReject(Connection conn, Lead lead) throws SQLException {
        String sql = """
            UPDATE leads
            SET status = ?, assigned_to = NULL, assigned_to_user_id = NULL, rejected_at = ?, reject_reason = ?, reject_details = ?, unassigned_reason = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, lead.getStatus());
            stmt.setTimestamp(2, lead.getRejectedAt());
            stmt.setString(3, lead.getRejectReason());
            stmt.setString(4, lead.getRejectDetails());
            stmt.setString(5, lead.getUnassignedReason());
            stmt.setLong(6, lead.getId());
            stmt.executeUpdate();
        }
    }

    private Lead mapRow(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setId(rs.getLong("id"));
        lead.setFullName(rs.getString("full_name"));
        lead.setCompanyName(rs.getString("company_name"));
        lead.setEmail(rs.getString("email"));
        lead.setPhone(rs.getString("phone"));
        lead.setTitle(rs.getString("title"));
        lead.setSourceId(rs.getObject("source_id", Long.class));
        lead.setSource(rs.getString("source"));
        lead.setRegion(rs.getString("region"));
        lead.setRegionCode(rs.getString("region_code"));
        lead.setIndustry(rs.getString("industry"));
        lead.setEmployeeCount(rs.getObject("employee_count", Integer.class));
        lead.setScore(rs.getObject("score", Integer.class));
        lead.setNotes(rs.getString("notes"));
        lead.setStatus(rs.getString("status"));
        lead.setOwnerUserId(rs.getObject("owner_user_id", Long.class));
        lead.setAssignedTo(rs.getString("assigned_to"));
        lead.setAssignedToUserId(rs.getObject("assigned_to_user_id", Long.class));
        lead.setAssignedAt(rs.getTimestamp("assigned_at"));
        lead.setSlaTotalMinutes(rs.getObject("sla_total_minutes", Integer.class));
        lead.setSlaDeadline(rs.getTimestamp("sla_deadline"));
        lead.setAcceptedAt(rs.getTimestamp("accepted_at"));
        lead.setRejectedAt(rs.getTimestamp("rejected_at"));
        lead.setRejectReason(rs.getString("reject_reason"));
        lead.setRejectDetails(rs.getString("reject_details"));
        lead.setSlaResult(rs.getString("sla_result"));
        lead.setResponseTimeText(rs.getString("response_time_text"));
        lead.setContactingStage(rs.getString("contacting_stage"));
        lead.setUnassignedReason(rs.getString("unassigned_reason"));
        lead.setLocked(rs.getBoolean("is_locked"));
        lead.setCreatedBy(rs.getObject("created_by", Long.class));
        lead.setCreatedAt(rs.getTimestamp("created_at"));
        lead.setUpdatedAt(rs.getTimestamp("updated_at"));
        return lead;
    }
}
