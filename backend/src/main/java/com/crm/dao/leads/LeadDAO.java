package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;

import java.sql.*;
import java.util.*;

public class LeadDAO {

    public List<Lead> findWithFilters(LeadFilterRequest filter, DataScopeContext scope, Long currentUserId) {
        StringBuilder sql = new StringBuilder("""
            SELECT l.*, u.full_name AS owner_name, u.email AS owner_email
            FROM leads l
            LEFT JOIN users u ON (u.id = l.assigned_to_user_id OR u.id = l.owner_user_id OR (l.assigned_to REGEXP '^[0-9]+$' AND u.id = CAST(l.assigned_to AS UNSIGNED)))
            WHERE l.is_deleted = 0
            """);

        List<Object> params = new ArrayList<>();
        buildFilterPredicates(sql, params, filter, scope, currentUserId);
        buildOrderClause(sql, filter.getSort());

        int page = filter.getPage();
        int size = filter.getSize();
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        List<Lead> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLead(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying leads with filters: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public int countWithFilters(LeadFilterRequest filter, DataScopeContext scope, Long currentUserId) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM leads l
            LEFT JOIN users u ON (u.id = l.assigned_to_user_id OR u.id = l.owner_user_id OR (l.assigned_to REGEXP '^[0-9]+$' AND u.id = CAST(l.assigned_to AS UNSIGNED)))
            WHERE l.is_deleted = 0
            """);

        List<Object> params = new ArrayList<>();
        buildFilterPredicates(sql, params, filter, scope, currentUserId);

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
            System.err.println("Error counting leads with filters: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public Map<String, Object> getLeadStats(DataScopeContext scope, Long currentUserId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
            SELECT 
                COUNT(*) AS total,
                SUM(CASE WHEN (l.tier = 'HOT' OR l.rating = 'HOT') THEN 1 ELSE 0 END) AS hot_count,
                SUM(CASE WHEN (l.tier = 'WARM' OR l.rating = 'WARM') THEN 1 ELSE 0 END) AS warm_count,
                SUM(CASE WHEN (l.tier = 'COLD' OR l.rating = 'COLD') THEN 1 ELSE 0 END) AS cold_count,
                SUM(CASE WHEN l.sla_deadline IS NOT NULL AND l.sla_deadline < NOW() AND l.status NOT IN ('CONVERTED', 'LOST') THEN 1 ELSE 0 END) AS overdue_count,
                SUM(CASE WHEN l.assigned_to IS NULL THEN 1 ELSE 0 END) AS unassigned_count,
                SUM(CASE WHEN l.status = 'NEW' THEN 1 ELSE 0 END) AS new_count,
                SUM(CASE WHEN l.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_count,
                SUM(CASE WHEN l.status = 'QUALIFIED' THEN 1 ELSE 0 END) AS qualified_count,
                SUM(CASE WHEN l.status = 'CONVERTED' THEN 1 ELSE 0 END) AS converted_count,
                COALESCE(SUM(l.estimated_value), 0) AS total_estimated_value
            FROM leads l
            WHERE l.is_deleted = 0
            """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("l.assigned_to", sql, params);
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("total", rs.getInt("total"));
                    stats.put("hotCount", rs.getInt("hot_count"));
                    stats.put("warmCount", rs.getInt("warm_count"));
                    stats.put("coldCount", rs.getInt("cold_count"));
                    stats.put("overdueCount", rs.getInt("overdue_count"));
                    stats.put("unassignedCount", rs.getInt("unassigned_count"));
                    stats.put("newCount", rs.getInt("new_count"));
                    stats.put("inProgressCount", rs.getInt("in_progress_count"));
                    stats.put("qualifiedCount", rs.getInt("qualified_count"));
                    stats.put("convertedCount", rs.getInt("converted_count"));
                    stats.put("totalEstimatedValue", rs.getDouble("total_estimated_value"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching lead statistics: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    public Lead findById(long id) {
        String sql = """
            SELECT l.*, u.full_name AS owner_name, u.email AS owner_email
            FROM leads l
            LEFT JOIN users u ON (u.id = l.assigned_to_user_id OR u.id = l.owner_user_id OR (l.assigned_to REGEXP '^[0-9]+$' AND u.id = CAST(l.assigned_to AS UNSIGNED)))
            WHERE l.id = ? AND l.is_deleted = 0
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLead(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding lead by id: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public long insert(Lead lead) {
        String sql = """
            INSERT INTO leads (code, full_name, title, company_name, industry, tax_code, email, phone, 
                               source, source_name, rating, tier, score, status, assigned_to, estimated_value, 
                               sla_deadline, sla_minutes_total, last_contacted_at, notes, form_id, ip_address, 
                               region, region_code, unassigned_reason, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindLeadParams(ps, lead);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        long genId = keys.getLong(1);
                        lead.setId(genId);
                        if (lead.getCode() == null || lead.getCode().isBlank()) {
                            String code = String.format("LEAD-2026-%03d", genId);
                            lead.setCode(code);
                            updateCode(genId, code);
                        }
                        return genId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting lead: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean update(Lead lead) {
        String sql = """
            UPDATE leads SET 
                code = ?, full_name = ?, title = ?, company_name = ?, industry = ?, tax_code = ?,
                email = ?, phone = ?, source = ?, source_name = ?, rating = ?, tier = ?, score = ?,
                status = ?, assigned_to = ?, estimated_value = ?, sla_deadline = ?, sla_minutes_total = ?,
                last_contacted_at = ?, notes = ?, region = ?, region_code = ?, unassigned_reason = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND is_deleted = 0
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindLeadParams(ps, lead);
            ps.setLong(26, lead.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating lead: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(long id) {
        String sql = "UPDATE leads SET is_deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error soft-deleting lead: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateCode(long id, String code) {
        String sql = "UPDATE leads SET code = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private void buildFilterPredicates(StringBuilder sql, List<Object> params,
                                       LeadFilterRequest filter, DataScopeContext scope, Long currentUserId) {
        // 1. Data Scope
        if (scope != null) {
            scope.appendOwnerPredicate("l.assigned_to", sql, params);
        }

        // 2. Status
        String status = filter.getStatus();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                sql.append(" AND l.status IN ('IN_PROGRESS', 'CONTACTED')");
            } else {
                sql.append(" AND l.status = ?");
                params.add(status.trim().toUpperCase());
            }
        }

        // 3. Tier / Rating (HOT / WARM / COLD)
        String tier = filter.getTier();
        if (tier != null && !tier.isBlank() && !"ALL".equalsIgnoreCase(tier)) {
            sql.append(" AND (l.tier = ? OR l.rating = ?)");
            params.add(tier.trim().toUpperCase());
            params.add(tier.trim().toUpperCase());
        }

        // 4. Source
        String source = filter.getSource();
        if (source != null && !source.isBlank() && !"ALL".equalsIgnoreCase(source)) {
            if ("WEBSITE".equalsIgnoreCase(source) || "WEB_FORM".equalsIgnoreCase(source)) {
                sql.append(" AND (l.source = 'WEBSITE' OR l.source = 'WEB_FORM')");
            } else {
                sql.append(" AND l.source = ?");
                params.add(source.trim().toUpperCase());
            }
        }

        // 5. Owner / Assigned Rep
        String owner = filter.getOwner();
        Long ownerId = filter.getOwnerId();
        if (ownerId != null && ownerId > 0) {
            sql.append(" AND l.assigned_to = ?");
            params.add(ownerId);
        } else if (owner != null && !owner.isBlank() && !"ALL".equalsIgnoreCase(owner)) {
            if ("UNASSIGNED".equalsIgnoreCase(owner)) {
                sql.append(" AND l.assigned_to IS NULL");
            } else if ("CURRENT_USER".equalsIgnoreCase(owner)) {
                if (currentUserId != null) {
                    sql.append(" AND l.assigned_to = ?");
                    params.add(currentUserId);
                }
            } else if (owner.matches("\\d+")) {
                sql.append(" AND l.assigned_to = ?");
                params.add(Long.parseLong(owner));
            } else {
                sql.append(" AND LOWER(u.full_name) LIKE ?");
                params.add("%" + owner.trim().toLowerCase() + "%");
            }
        }

        // 6. Date Range / fromDate / toDate
        String from = filter.getEffectiveFrom();
        String to = filter.getEffectiveTo();
        if (from != null && !from.isBlank()) {
            sql.append(" AND l.created_at >= ?");
            params.add(from.contains(":") ? from : from + " 00:00:00");
        }
        if (to != null && !to.isBlank()) {
            sql.append(" AND l.created_at <= ?");
            params.add(to.contains(":") ? to : to + " 23:59:59");
        }

        String dateRange = filter.getDateRange();
        if (dateRange != null && !"ALL".equalsIgnoreCase(dateRange) && (from == null && to == null)) {
            switch (dateRange.toUpperCase()) {
                case "TODAY" -> sql.append(" AND l.created_at >= CURRENT_DATE()");
                case "YESTERDAY" -> sql.append(" AND l.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY) AND l.created_at < CURRENT_DATE()");
                case "THIS_WEEK" -> sql.append(" AND l.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL WEEKDAY(CURRENT_DATE()) DAY)");
                case "THIS_MONTH" -> sql.append(" AND l.created_at >= DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')");
                case "LAST_30_DAYS" -> sql.append(" AND l.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            }
        }

        // 7. Keyword / Search
        String kw = filter.getKeyword();
        if (kw != null && !kw.isBlank()) {
            sql.append("""
                 AND (
                    LOWER(l.full_name) LIKE ? OR
                    LOWER(COALESCE(l.company_name, '')) LIKE ? OR
                    LOWER(COALESCE(l.phone, '')) LIKE ? OR
                    LOWER(COALESCE(l.email, '')) LIKE ? OR
                    LOWER(COALESCE(l.code, '')) LIKE ? OR
                    LOWER(COALESCE(l.notes, '')) LIKE ?
                 )
                """);
            String likeParam = "%" + kw.toLowerCase() + "%";
            for (int i = 0; i < 6; i++) {
                params.add(likeParam);
            }
        }

        // 8. SLA Overdue Only (S4-09 AC 2)
        if (filter.isSlaOverdueOnly()) {
            sql.append(" AND l.sla_deadline IS NOT NULL AND l.sla_deadline < NOW() AND l.status NOT IN ('CONVERTED', 'LOST')");
        }
    }

    private void buildOrderClause(StringBuilder sql, String sort) {
        if (sort == null || sort.isBlank()) {
            sql.append(" ORDER BY l.id DESC");
            return;
        }

        switch (sort.toUpperCase()) {
            case "SLA_URGENT" -> sql.append("""
                 ORDER BY 
                    CASE 
                        WHEN l.status IN ('CONVERTED', 'LOST') THEN 3 
                        WHEN l.sla_deadline IS NOT NULL AND l.sla_deadline < NOW() THEN 1 
                        WHEN l.sla_deadline IS NOT NULL THEN 2 
                        ELSE 3 
                    END ASC,
                    l.sla_deadline ASC, 
                    l.id DESC
                """);
            case "SCORE_DESC" -> sql.append(" ORDER BY l.score DESC, l.id DESC");
            case "NEWEST" -> sql.append(" ORDER BY l.created_at DESC, l.id DESC");
            case "OLDEST" -> sql.append(" ORDER BY l.created_at ASC, l.id ASC");
            case "VALUE_DESC" -> sql.append(" ORDER BY l.estimated_value DESC, l.id DESC");
            default -> sql.append(" ORDER BY l.id DESC");
        }
    }

    private void bindLeadParams(PreparedStatement ps, Lead lead) throws SQLException {
        ps.setString(1, lead.getCode());
        ps.setString(2, lead.getFullName());
        ps.setString(3, lead.getTitle());
        ps.setString(4, lead.getCompanyName());
        ps.setString(5, lead.getIndustry());
        ps.setString(6, lead.getTaxCode());
        ps.setString(7, lead.getEmail());
        ps.setString(8, lead.getPhone());
        ps.setString(9, lead.getSource() != null ? lead.getSource() : "WEB_FORM");
        ps.setString(10, lead.getSourceName());
        ps.setString(11, lead.getRating());
        ps.setString(12, lead.getTier());
        ps.setInt(13, lead.getScore());
        ps.setString(14, lead.getStatus() != null ? lead.getStatus() : "NEW");

        if (lead.getAssignedTo() != null && lead.getAssignedTo() > 0) {
            ps.setString(15, String.valueOf(lead.getAssignedTo()));
        } else {
            ps.setNull(15, Types.VARCHAR);
        }

        ps.setDouble(16, lead.getEstimatedValue());

        if (lead.getSlaDeadline() != null) {
            ps.setTimestamp(17, lead.getSlaDeadline());
        } else {
            ps.setNull(17, Types.TIMESTAMP);
        }

        ps.setInt(18, lead.getSlaMinutesTotal() > 0 ? lead.getSlaMinutesTotal() : 120);

        if (lead.getLastContactedAt() != null) {
            ps.setTimestamp(19, lead.getLastContactedAt());
        } else {
            ps.setNull(19, Types.TIMESTAMP);
        }

        ps.setString(20, lead.getNotes());
        ps.setString(21, lead.getFormId());
        ps.setString(22, lead.getIpAddress());
        ps.setString(23, lead.getRegion());
        ps.setString(24, lead.getRegionCode());
        ps.setString(25, lead.getUnassignedReason());
    }

    private Lead mapResultSetToLead(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setId(rs.getLong("id"));
        lead.setCode(rs.getString("code"));
        lead.setFullName(rs.getString("full_name"));
        lead.setTitle(rs.getString("title"));
        lead.setCompanyName(rs.getString("company_name"));
        lead.setIndustry(rs.getString("industry"));
        lead.setTaxCode(rs.getString("tax_code"));
        lead.setEmail(rs.getString("email"));
        lead.setPhone(rs.getString("phone"));
        lead.setSource(rs.getString("source"));
        lead.setSourceName(rs.getString("source_name"));
        lead.setRating(rs.getString("rating"));
        lead.setTier(rs.getString("tier"));
        lead.setScore(rs.getInt("score"));
        lead.setStatus(rs.getString("status"));

        String assignedToStr = rs.getString("assigned_to");
        if (assignedToStr != null && !assignedToStr.isBlank()) {
            try {
                String digits = assignedToStr.replaceAll("[^0-9]", "");
                if (!digits.isBlank()) {
                    lead.setAssignedTo(Long.parseLong(digits));
                }
            } catch (Exception ignored) {
            }
        }
        if (lead.getAssignedTo() == null || lead.getAssignedTo() <= 0) {
            try {
                long uid = rs.getLong("assigned_to_user_id");
                if (!rs.wasNull() && uid > 0) {
                    lead.setAssignedTo(uid);
                }
            } catch (SQLException ignored) {
            }
        }
        if (lead.getAssignedTo() == null || lead.getAssignedTo() <= 0) {
            try {
                long uid = rs.getLong("owner_user_id");
                if (!rs.wasNull() && uid > 0) {
                    lead.setAssignedTo(uid);
                }
            } catch (SQLException ignored) {
            }
        }

        try {
            lead.setOwner(rs.getString("owner_name"));
        } catch (SQLException ignored) {
        }

        lead.setEstimatedValue(rs.getDouble("estimated_value"));
        lead.setSlaDeadline(rs.getTimestamp("sla_deadline"));
        lead.setSlaMinutesTotal(rs.getInt("sla_minutes_total"));
        lead.setLastContactedAt(rs.getTimestamp("last_contacted_at"));
        lead.setNotes(rs.getString("notes"));
        lead.setFormId(rs.getString("form_id"));
        lead.setIpAddress(rs.getString("ip_address"));
        lead.setRegion(rs.getString("region"));
        lead.setRegionCode(rs.getString("region_code"));
        lead.setUnassignedReason(rs.getString("unassigned_reason"));
        lead.setDeleted(rs.getBoolean("is_deleted"));
        lead.setCreatedAt(rs.getTimestamp("created_at"));
        lead.setUpdatedAt(rs.getTimestamp("updated_at"));

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
}
