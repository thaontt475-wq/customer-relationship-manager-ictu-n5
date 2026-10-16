package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.Lead;

import java.sql.*;
import java.util.*;

public class LeadDAO {

    public Lead findById(long id) {
        String sql = "SELECT * FROM leads WHERE id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLead(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching lead by id: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<Lead> findUnassignedOrPendingQueue() {
        String sql = "SELECT * FROM leads WHERE (status = 'PENDING_ASSIGNMENT' OR assigned_to IS NULL) " +
                "AND is_deleted = 0 ORDER BY created_at DESC";
        List<Lead> leads = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                leads.add(mapResultSetToLead(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pending lead queue: " + e.getMessage());
            e.printStackTrace();
        }
        return leads;
    }

    public List<Lead> findAll(int page, int size, String status, String source) {
        StringBuilder sql = new StringBuilder("SELECT * FROM leads WHERE is_deleted = 0");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status.trim());
        }
        if (source != null && !source.isBlank()) {
            sql.append(" AND source = ?");
            params.add(source.trim());
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        int offset = Math.max(0, (page - 1) * size);
        params.add(size);
        params.add(offset);

        List<Lead> leads = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    leads.add(mapResultSetToLead(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching leads: " + e.getMessage());
            e.printStackTrace();
        }
        return leads;
    }

    public int count(String status, String source) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM leads WHERE is_deleted = 0");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status.trim());
        }
        if (source != null && !source.isBlank()) {
            sql.append(" AND source = ?");
            params.add(source.trim());
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
        } catch (SQLException e) {
            System.err.println("Error counting leads: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public long insert(Lead lead) {
        String sql = "INSERT INTO leads (full_name, company_name, email, phone, source, status, notes, " +
                "form_id, ip_address, assigned_to, is_deleted, region, region_code, industry, title, unassigned_reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, lead.getFullName());
            ps.setString(2, lead.getCompanyName());
            ps.setString(3, lead.getEmail());
            ps.setString(4, lead.getPhone());
            ps.setString(5, lead.getSource() != null ? lead.getSource() : "WEB_FORM");
            ps.setString(6, lead.getStatus() != null ? lead.getStatus() : "NEW");
            ps.setString(7, lead.getNotes());
            ps.setString(8, lead.getFormId());
            ps.setString(9, lead.getIpAddress());

            if (lead.getAssignedTo() != null) {
                ps.setLong(10, lead.getAssignedTo());
            } else {
                ps.setNull(10, Types.BIGINT);
            }

            ps.setString(11, lead.getRegion() != null ? lead.getRegion() : "Miền Bắc (Hà Nội)");
            ps.setString(12, lead.getRegionCode() != null ? lead.getRegionCode() : "NORTH");
            ps.setString(13, lead.getIndustry() != null ? lead.getIndustry() : "ALL");
            ps.setString(14, lead.getTitle() != null ? lead.getTitle() : "Khách hàng tiềm năng");
            ps.setString(15, lead.getUnassignedReason());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        lead.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting lead: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateAssignment(Connection conn, long leadId, Long assignedTo, String status, String reason) throws SQLException {
        String sql = "UPDATE leads SET assigned_to = ?, status = ?, unassigned_reason = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (assignedTo != null) {
                ps.setLong(1, assignedTo);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, status != null ? status : "ASSIGNED");
            ps.setString(3, reason);
            ps.setLong(4, leadId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateAssignment(long leadId, Long assignedTo, String status, String reason) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return updateAssignment(conn, leadId, assignedTo, status, reason);
        } catch (SQLException e) {
            System.err.println("Error updating lead assignment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<SalesRepDTO> getSalesRepsWithWorkload() {
        Map<String, SalesRepDTO> repsMap = new LinkedHashMap<>();

        // Standard reps configured in the CRM sales team
        repsMap.put("USR-101", new SalesRepDTO("USR-101", "Nguyễn Văn Hùng", "NH", "Senior Sales B2B", "Phòng Kinh doanh Miền Bắc", "NORTH", 8, 25, true));
        repsMap.put("USR-102", new SalesRepDTO("USR-102", "Trần Thu Hà", "TH", "Chuyên viên Bất động sản", "Phòng Kinh doanh Miền Bắc", "NORTH", 14, 25, true));
        repsMap.put("USR-103", new SalesRepDTO("USR-103", "Lê Hoàng Cường", "LC", "Trưởng nhóm Sales Miền Trung", "Chi nhánh Đà Nẵng", "CENTRAL", 6, 20, true));
        repsMap.put("USR-104", new SalesRepDTO("USR-104", "Phạm Thu Dung", "PD", "Senior Sales Enterprise", "Phòng Kinh doanh Miền Nam", "SOUTH", 12, 25, true));
        repsMap.put("USR-105", new SalesRepDTO("USR-105", "Vũ Hải Đăng", "VĐ", "Chuyên viên Giải pháp CNTT", "Phòng Kinh doanh Miền Nam", "SOUTH", 9, 25, true));
        repsMap.put("USR-106", new SalesRepDTO("USR-106", "Đỗ Minh Tuấn", "MT", "Key Account Manager", "Khối Khách hàng Lớn (VIP)", "NATIONWIDE", 5, 15, true));

        // Query database users and teams to enrich or add dynamic reps
        String sql = "SELECT u.id, u.full_name, u.email, t.name as team_name " +
                "FROM users u " +
                "LEFT JOIN teams t ON u.team_id = t.id " +
                "WHERE u.status = 'ACTIVE'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long uId = rs.getLong("id");
                String idKey = String.valueOf(uId);
                String fullName = rs.getString("full_name");
                String teamName = rs.getString("team_name");
                if (teamName == null) teamName = "Phòng Kinh doanh";

                String initials = getInitials(fullName);
                SalesRepDTO rep = new SalesRepDTO(
                        idKey,
                        fullName,
                        initials,
                        "Chuyên viên Kinh doanh",
                        teamName,
                        "NATIONWIDE",
                        0,
                        25,
                        true
                );
                repsMap.putIfAbsent(idKey, rep);
            }
        } catch (SQLException e) {
            System.err.println("Error querying active users for sales reps: " + e.getMessage());
        }

        // Count current leads assigned to each rep
        String countSql = "SELECT assigned_to, COUNT(*) as cnt FROM leads WHERE assigned_to IS NOT NULL AND is_deleted = 0 GROUP BY assigned_to";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long assignedTo = rs.getLong("assigned_to");
                int cnt = rs.getInt("cnt");
                String key = String.valueOf(assignedTo);
                if (repsMap.containsKey(key)) {
                    repsMap.get(key).setCurrentLeads(cnt);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying lead counts: " + e.getMessage());
        }

        return new ArrayList<>(repsMap.values());
    }

    private Lead mapResultSetToLead(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setId(rs.getLong("id"));
        lead.setFullName(rs.getString("full_name"));
        lead.setCompanyName(rs.getString("company_name"));
        lead.setEmail(rs.getString("email"));
        lead.setPhone(rs.getString("phone"));
        lead.setSource(rs.getString("source"));
        lead.setStatus(rs.getString("status"));
        lead.setNotes(rs.getString("notes"));
        lead.setFormId(rs.getString("form_id"));
        lead.setIpAddress(rs.getString("ip_address"));

        long assignedTo = rs.getLong("assigned_to");
        if (!rs.wasNull()) {
            lead.setAssignedTo(assignedTo);
            lead.setAssignedToRepId(String.valueOf(assignedTo));
        }

        lead.setDeleted(rs.getBoolean("is_deleted"));
        lead.setCreatedAt(rs.getTimestamp("created_at"));
        lead.setUpdatedAt(rs.getTimestamp("updated_at"));

        try {
            lead.setRegion(rs.getString("region"));
            lead.setRegionCode(rs.getString("region_code"));
            lead.setIndustry(rs.getString("industry"));
            lead.setTitle(rs.getString("title"));
            lead.setUnassignedReason(rs.getString("unassigned_reason"));
        } catch (SQLException ignored) {
            // In case columns were not queried
        }

        if (lead.getCreatedAt() != null) {
            lead.setTimeAgo(calculateTimeAgo(lead.getCreatedAt().getTime()));
        }

        return lead;
    }

    private String calculateTimeAgo(long timeMillis) {
        long diff = System.currentTimeMillis() - timeMillis;
        long minutes = diff / (60 * 1000);
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";
        long days = hours / 24;
        return days + " ngày trước";
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "NV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
