package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;

import java.sql.*;
import java.util.*;

public class LeadDAO {

    public Connection open() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public void ensureTableExists() {
        // Handled via Flyway/SQL migrations
    }

    // -------------------------------------------------------------------------
    // S4-02: Manual & Excel Import Methods
    // -------------------------------------------------------------------------

    public List<Map<String, Object>> sources(Connection c, boolean lock) throws SQLException {
        String query = "SELECT id, code, name FROM master_data WHERE type='lead-source' AND active=TRUE ORDER BY id" + (lock ? " FOR SHARE" : "");
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(query); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                result.add(Map.of("id", r.getLong("id"), "code", r.getString("code"), "name", r.getString("name")));
            }
        }
        return result;
    }

    public long insert(Connection c, LeadWriteRequest lead, long user, String batch) throws SQLException {
        String sql = """
            INSERT INTO leads (full_name, company_name, email, phone, source_id, notes, status, owner_user_id, created_by, import_batch_hash)
            VALUES (?, ?, ?, ?, ?, ?, 'NEW', ?, ?, ?)
            """;
        try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, lead.fullName());
            p.setString(2, lead.companyName());
            p.setString(3, lead.email());
            p.setString(4, lead.phone());
            p.setLong(5, lead.sourceId());
            p.setString(6, lead.notes());
            p.setLong(7, user);
            p.setLong(8, user);
            p.setString(9, batch);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Missing Lead generated key");
                return keys.getLong(1);
            }
        }
    }

    public Map<String, Object> get(Connection c, long id, DataScopeContext scope) throws SQLException {
        return get(c, id, scope, false);
    }

    public Map<String, Object> get(Connection c, long id, DataScopeContext scope, boolean lock) throws SQLException {
        StringBuilder query = new StringBuilder("""
            SELECT l.id, l.full_name AS fullName, l.company_name AS companyName, l.email, l.phone, l.source_id AS sourceId,
            s.code AS sourceCode, s.name AS sourceName, l.notes, l.status, l.owner_user_id AS ownerUserId,
            l.created_by AS createdBy, l.created_at AS createdAt
            FROM leads l JOIN master_data s ON s.id=l.source_id WHERE l.id=?
            """);
        List<Object> values = new ArrayList<>();
        values.add(id);
        if (scope != null) {
            scope.appendOwnerPredicate("l.owner_user_id", query, values);
        }
        if (lock) query.append(" FOR UPDATE");
        try (PreparedStatement p = c.prepareStatement(query.toString())) {
            for (int i = 0; i < values.size(); i++) p.setObject(i + 1, values.get(i));
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                Map<String, Object> result = new LinkedHashMap<>();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
                    Object value = r.getObject(i);
                    result.put(r.getMetaData().getColumnLabel(i), value instanceof Timestamp date ? date.toLocalDateTime().toString() :
                            value instanceof java.time.temporal.TemporalAccessor ? value.toString() : value);
                }
                return result;
            }
        }
    }

    public void update(Connection c, long id, LeadWriteRequest lead) throws SQLException {
        try (var p = c.prepareStatement("UPDATE leads SET full_name=?, company_name=?, email=?, phone=?, source_id=?, notes=? WHERE id=?")) {
            p.setString(1, lead.fullName());
            p.setString(2, lead.companyName());
            p.setString(3, lead.email());
            p.setString(4, lead.phone());
            p.setLong(5, lead.sourceId());
            p.setString(6, lead.notes());
            p.setLong(7, id);
            p.executeUpdate();
        }
    }

    public Long receiptOwner(Connection c, String hash) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT owner_user_id FROM lead_import_batches WHERE token_hash=?")) {
            p.setString(1, hash);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getLong(1) : null;
            }
        }
    }

    public void receipt(Connection c, String hash, long user, int total, int success, int failure) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO lead_import_batches(token_hash, owner_user_id, total_rows, success_count, failure_count) VALUES (?, ?, ?, ?, ?)")) {
            p.setString(1, hash);
            p.setLong(2, user);
            p.setInt(3, total);
            p.setInt(4, success);
            p.setInt(5, failure);
            p.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // S4-06 & S4-07 & S4-09: Lead FindById, Assignment & SLA
    // -------------------------------------------------------------------------

    public Lead findById(long id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findById(conn, id, false);
        } catch (SQLException e) {
            System.err.println("Error finding lead by id: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Lead findById(Long id) {
        return id != null ? findById(id.longValue()) : null;
    }

    public Lead findById(Connection conn, long id) throws SQLException {
        return findById(conn, id, false);
    }

    public Lead findById(Connection conn, long id, boolean forUpdate) throws SQLException {
        String sql = """
            SELECT l.*, u.full_name AS owner_name, u.email AS owner_email
            FROM leads l
            LEFT JOIN users u ON (u.id = l.assigned_to_user_id OR u.id = l.owner_user_id OR (l.assigned_to REGEXP '^[0-9]+$' AND u.id = CAST(l.assigned_to AS UNSIGNED)))
            WHERE l.id = ? AND l.is_deleted = 0
            """ + (forUpdate ? " FOR UPDATE" : "");

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLead(rs);
                }
            }
        }
        return null;
    }

    public List<Lead> findMyAssignedLeads(long userId, DataScopeContext scope) throws SQLException {
        List<Lead> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM leads WHERE is_deleted = 0 AND ");

        if (scope != null && scope.scopeType() == com.crm.service.permissions.ScopeType.ALL) {
            sql.append("(assigned_to_user_id IS NOT NULL OR assigned_to IS NOT NULL OR status IN ('PENDING_RESPONSE', 'CONTACTING', 'REJECTED')) ");
        } else if (scope != null && scope.scopeType() == com.crm.service.permissions.ScopeType.TEAM) {
            Set<Long> owners = scope.allowedOwnerIds();
            if (owners == null || owners.isEmpty()) {
                sql.append("(assigned_to_user_id = ").append(userId).append(" OR assigned_to = '").append(userId).append("') ");
            } else {
                sql.append("(assigned_to_user_id IN (");
                int i = 0;
                for (Long oid : owners) {
                    if (i++ > 0) sql.append(",");
                    sql.append(oid);
                }
                sql.append(") OR assigned_to IN (");
                i = 0;
                for (Long oid : owners) {
                    if (i++ > 0) sql.append(",");
                    sql.append("'").append(oid).append("'");
                }
                sql.append(")) ");
            }
        } else {
            sql.append("(assigned_to_user_id = ? OR assigned_to = ? OR assigned_to = ?) ");
        }

        sql.append("ORDER BY FIELD(status, 'PENDING_RESPONSE', 'CONTACTING', 'REJECTED', 'PENDING_ASSIGNMENT'), id DESC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (scope == null || scope.scopeType() == com.crm.service.permissions.ScopeType.SELF) {
                stmt.setLong(1, userId);
                stmt.setString(2, String.valueOf(userId));
                stmt.setString(3, "USR-" + userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLead(rs));
                }
            }
        }
        return list;
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

    public List<Lead> findUnassignedOrPendingQueue() {
        String sql = "SELECT * FROM leads WHERE (status = 'PENDING_ASSIGNMENT' OR assigned_to IS NULL) AND is_deleted = 0 ORDER BY created_at DESC";
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

    public boolean updateAssignment(Connection conn, long leadId, Long assignedTo, String status, String reason) throws SQLException {
        try {
            String sql = "UPDATE leads SET assigned_to = ?, assigned_to_user_id = ?, status = ?, unassigned_reason = ?, assigned_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (assignedTo != null) {
                    ps.setString(1, String.valueOf(assignedTo));
                    ps.setLong(2, assignedTo);
                } else {
                    ps.setNull(1, Types.VARCHAR);
                    ps.setNull(2, Types.BIGINT);
                }
                ps.setString(3, status != null ? status : "ASSIGNED");
                ps.setString(4, reason);
                ps.setLong(5, leadId);

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            String fallbackSql = "UPDATE leads SET assigned_to = ?, status = ?, unassigned_reason = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                if (assignedTo != null) {
                    ps.setObject(1, assignedTo);
                } else {
                    ps.setNull(1, Types.BIGINT);
                }
                ps.setString(2, status != null ? status : "ASSIGNED");
                ps.setString(3, reason);
                ps.setLong(4, leadId);

                return ps.executeUpdate() > 0;
            }
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

        repsMap.put("USR-101", new SalesRepDTO("USR-101", "Nguyễn Văn Hùng", "NH", "Senior Sales B2B", "Phòng Kinh doanh Miền Bắc", "NORTH", 8, 25, true));
        repsMap.put("USR-102", new SalesRepDTO("USR-102", "Trần Thu Hà", "TH", "Chuyên viên Bất động sản", "Phòng Kinh doanh Miền Bắc", "NORTH", 14, 25, true));
        repsMap.put("USR-103", new SalesRepDTO("USR-103", "Lê Hoàng Cường", "LC", "Trưởng nhóm Sales Miền Trung", "Chi nhánh Đà Nẵng", "CENTRAL", 6, 20, true));
        repsMap.put("USR-104", new SalesRepDTO("USR-104", "Phạm Thu Dung", "PD", "Senior Sales Enterprise", "Phòng Kinh doanh Miền Nam", "SOUTH", 12, 25, true));
        repsMap.put("USR-105", new SalesRepDTO("USR-105", "Vũ Hải Đăng", "VĐ", "Chuyên viên Giải pháp CNTT", "Phòng Kinh doanh Miền Nam", "SOUTH", 9, 25, true));
        repsMap.put("USR-106", new SalesRepDTO("USR-106", "Đỗ Minh Tuấn", "MT", "Key Account Manager", "Khối Khách hàng Lớn (VIP)", "NATIONWIDE", 5, 15, true));

        String sql = "SELECT u.id, u.full_name, u.email, t.name as team_name FROM users u LEFT JOIN teams t ON u.team_id = t.id WHERE u.status = 'ACTIVE'";

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
                        idKey, fullName, initials, "Chuyên viên Kinh doanh", teamName, "NATIONWIDE", 0, 25, true
                );
                repsMap.putIfAbsent(idKey, rep);
            }
        } catch (SQLException e) {
            System.err.println("Error querying active users for sales reps: " + e.getMessage());
        }

        String countSql = "SELECT assigned_to, COUNT(*) as cnt FROM leads WHERE assigned_to IS NOT NULL AND is_deleted = 0 GROUP BY assigned_to";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String assignedToStr = rs.getString("assigned_to");
                int cnt = rs.getInt("cnt");
                if (assignedToStr != null && repsMap.containsKey(assignedToStr)) {
                    repsMap.get(assignedToStr).setCurrentLeads(cnt);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying lead counts: " + e.getMessage());
        }

        return new ArrayList<>(repsMap.values());
    }

    // -------------------------------------------------------------------------
    // S4-01 & General Query Methods
    // -------------------------------------------------------------------------

    public List<Lead> findAll(int page, int size, String status, String source, DataScopeContext scope) throws SQLException {
        List<Lead> list = new ArrayList<>();
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

        if (scope != null) {
            scope.appendOwnerPredicate("assigned_to", sql, params);
        }
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        int offset = Math.max(0, (page - 1) * size);
        params.add(size);
        params.add(offset);

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
        }
        return list;
    }

    public List<Lead> findAll(int page, int size, String status, String source) {
        try {
            return findAll(page, size, status, source, null);
        } catch (SQLException e) {
            System.err.println("Error finding all leads: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public int count(String status, String source, DataScopeContext scope) throws SQLException {
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

        if (scope != null) {
            scope.appendOwnerPredicate("assigned_to", sql, params);
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

    public int count(String status, String source) {
        try {
            return count(status, source, null);
        } catch (SQLException e) {
            System.err.println("Error counting leads: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Insert, Update, Delete with full unified fields
    // -------------------------------------------------------------------------

    public long insert(Connection conn, Lead lead) throws SQLException {
        String sql = """
            INSERT INTO leads (
                code, full_name, title, company_name, industry, tax_code, email, phone, 
                source_id, source, source_name, rating, tier, score, status, 
                owner_user_id, assigned_to, assigned_to_user_id, assigned_at, 
                estimated_value, sla_deadline, sla_total_minutes, sla_minutes_total, 
                last_contacted_at, notes, form_id, ip_address, region, region_code, 
                unassigned_reason, is_locked, created_by, is_deleted, created_at, updated_at
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, 
                ?, ?, ?, ?, ?, ?, ?, 
                ?, ?, ?, ?, 
                ?, ?, ?, ?, 
                ?, ?, ?, ?, ?, ?, 
                ?, ?, ?, 0, ?, ?
            )
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp createdAt = lead.getCreatedAt() != null ? lead.getCreatedAt() : now;
            Timestamp assignedAt = lead.getAssignedAt();
            if (assignedAt == null && (lead.getAssignedTo() != null || lead.getAssignedToUserId() != null)) {
                assignedAt = now;
            }

            ps.setString(1, lead.getCode());
            ps.setString(2, lead.getFullName());
            ps.setString(3, lead.getTitle() != null ? lead.getTitle() : "Khách hàng tiềm năng");
            ps.setString(4, lead.getCompanyName());
            ps.setString(5, lead.getIndustry() != null ? lead.getIndustry() : "ALL");
            ps.setString(6, lead.getTaxCode());
            ps.setString(7, lead.getEmail());
            ps.setString(8, lead.getPhone());

            ps.setObject(9, lead.getSourceId(), Types.BIGINT);
            ps.setString(10, lead.getSource() != null ? lead.getSource() : "WEB_FORM");
            ps.setString(11, lead.getSourceName());
            ps.setString(12, lead.getRating() != null ? lead.getRating() : "WARM");
            ps.setString(13, lead.getTier() != null ? lead.getTier() : "WARM");
            ps.setInt(14, lead.getScore() != null ? lead.getScore() : 50);
            ps.setString(15, lead.getStatus() != null ? lead.getStatus() : "NEW");

            ps.setObject(16, lead.getOwnerUserId(), Types.BIGINT);
            if (lead.getAssignedTo() != null) {
                ps.setString(17, String.valueOf(lead.getAssignedTo()));
                ps.setLong(18, lead.getAssignedTo());
            } else {
                ps.setNull(17, Types.VARCHAR);
                ps.setNull(18, Types.BIGINT);
            }
            ps.setTimestamp(19, assignedAt);

            ps.setDouble(20, lead.getEstimatedValue());
            ps.setTimestamp(21, lead.getSlaDeadline());
            int slaMin = lead.getSlaTotalMinutes() != null && lead.getSlaTotalMinutes() > 0 ? lead.getSlaTotalMinutes() : 120;
            ps.setInt(22, slaMin);
            ps.setInt(23, slaMin);

            ps.setTimestamp(24, lead.getLastContactedAt());
            ps.setString(25, lead.getNotes());
            ps.setString(26, lead.getFormId());
            ps.setString(27, lead.getIpAddress());
            ps.setString(28, lead.getRegion() != null ? lead.getRegion() : "Miền Bắc (Hà Nội)");
            ps.setString(29, lead.getRegionCode() != null ? lead.getRegionCode() : "NORTH");
            ps.setString(30, lead.getUnassignedReason());
            ps.setBoolean(31, lead.isLocked());
            ps.setObject(32, lead.getCreatedBy(), Types.BIGINT);
            ps.setTimestamp(33, createdAt);
            ps.setTimestamp(34, lead.getUpdatedAt() != null ? lead.getUpdatedAt() : now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        long genId = keys.getLong(1);
                        lead.setId(genId);
                        if (lead.getCode() == null || lead.getCode().isBlank()) {
                            String code = String.format("LEAD-2026-%03d", genId);
                            lead.setCode(code);
                            updateCode(conn, genId, code);
                        }
                        return genId;
                    }
                }
            }
        }
        return -1;
    }

    public long insert(Lead lead) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return insert(conn, lead);
        } catch (SQLException e) {
            System.err.println("Error inserting lead: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public boolean update(Lead lead) {
        String sql = """
            UPDATE leads SET 
                code = ?, full_name = ?, title = ?, company_name = ?, industry = ?, tax_code = ?,
                email = ?, phone = ?, source = ?, source_name = ?, rating = ?, tier = ?, score = ?,
                status = ?, assigned_to = ?, assigned_to_user_id = ?, estimated_value = ?, 
                sla_deadline = ?, sla_minutes_total = ?, sla_total_minutes = ?,
                last_contacted_at = ?, notes = ?, region = ?, region_code = ?, unassigned_reason = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND is_deleted = 0
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindLeadParams(ps, lead);
            ps.setLong(27, lead.getId());
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
        try (Connection conn = DatabaseConfig.getConnection()) {
            return updateCode(conn, id, code);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateCode(Connection conn, long id, String code) {
        String sql = "UPDATE leads SET code = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // S4-09: Filter, Search & Stats
    // -------------------------------------------------------------------------

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

    private void buildFilterPredicates(StringBuilder sql, List<Object> params,
                                       LeadFilterRequest filter, DataScopeContext scope, Long currentUserId) {
        if (scope != null) {
            scope.appendOwnerPredicate("l.assigned_to", sql, params);
        }

        String status = filter.getStatus();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                sql.append(" AND l.status IN ('IN_PROGRESS', 'CONTACTED')");
            } else {
                sql.append(" AND l.status = ?");
                params.add(status.trim().toUpperCase());
            }
        }

        String tier = filter.getTier();
        if (tier != null && !tier.isBlank() && !"ALL".equalsIgnoreCase(tier)) {
            sql.append(" AND (l.tier = ? OR l.rating = ?)");
            params.add(tier.trim().toUpperCase());
            params.add(tier.trim().toUpperCase());
        }

        String source = filter.getSource();
        if (source != null && !source.isBlank() && !"ALL".equalsIgnoreCase(source)) {
            if ("WEBSITE".equalsIgnoreCase(source) || "WEB_FORM".equalsIgnoreCase(source)) {
                sql.append(" AND (l.source = 'WEBSITE' OR l.source = 'WEB_FORM')");
            } else {
                sql.append(" AND l.source = ?");
                params.add(source.trim().toUpperCase());
            }
        }

        String owner = filter.getOwner();
        Long ownerId = filter.getOwnerId();
        if (ownerId != null && ownerId > 0) {
            sql.append(" AND (l.assigned_to = ? OR l.assigned_to_user_id = ?)");
            params.add(ownerId);
            params.add(ownerId);
        } else if (owner != null && !owner.isBlank() && !"ALL".equalsIgnoreCase(owner)) {
            if ("UNASSIGNED".equalsIgnoreCase(owner)) {
                sql.append(" AND l.assigned_to IS NULL");
            } else if ("CURRENT_USER".equalsIgnoreCase(owner)) {
                if (currentUserId != null) {
                    sql.append(" AND (l.assigned_to = ? OR l.assigned_to_user_id = ?)");
                    params.add(currentUserId);
                    params.add(currentUserId);
                }
            } else if (owner.matches("\\d+")) {
                sql.append(" AND (l.assigned_to = ? OR l.assigned_to_user_id = ?)");
                params.add(Long.parseLong(owner));
                params.add(Long.parseLong(owner));
            } else {
                sql.append(" AND LOWER(u.full_name) LIKE ?");
                params.add("%" + owner.trim().toLowerCase() + "%");
            }
        }

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
        ps.setString(3, lead.getTitle() != null ? lead.getTitle() : "Khách hàng tiềm năng");
        ps.setString(4, lead.getCompanyName());
        ps.setString(5, lead.getIndustry() != null ? lead.getIndustry() : "ALL");
        ps.setString(6, lead.getTaxCode());
        ps.setString(7, lead.getEmail());
        ps.setString(8, lead.getPhone());
        ps.setString(9, lead.getSource() != null ? lead.getSource() : "WEB_FORM");
        ps.setString(10, lead.getSourceName());
        ps.setString(11, lead.getRating() != null ? lead.getRating() : "WARM");
        ps.setString(12, lead.getTier() != null ? lead.getTier() : "WARM");
        ps.setInt(13, lead.getScore() != null ? lead.getScore() : 50);
        ps.setString(14, lead.getStatus() != null ? lead.getStatus() : "NEW");

        if (lead.getAssignedTo() != null) {
            ps.setString(15, String.valueOf(lead.getAssignedTo()));
            ps.setLong(16, lead.getAssignedTo());
        } else {
            ps.setNull(15, Types.VARCHAR);
            ps.setNull(16, Types.BIGINT);
        }

        ps.setDouble(17, lead.getEstimatedValue());

        if (lead.getSlaDeadline() != null) {
            ps.setTimestamp(18, lead.getSlaDeadline());
        } else {
            ps.setNull(18, Types.TIMESTAMP);
        }

        int slaMin = lead.getSlaTotalMinutes() != null && lead.getSlaTotalMinutes() > 0 ? lead.getSlaTotalMinutes() : 120;
        ps.setInt(19, slaMin);
        ps.setInt(20, slaMin);

        if (lead.getLastContactedAt() != null) {
            ps.setTimestamp(21, lead.getLastContactedAt());
        } else {
            ps.setNull(21, Types.TIMESTAMP);
        }

        ps.setString(22, lead.getNotes());
        ps.setString(23, lead.getFormId());
        ps.setString(24, lead.getIpAddress());
        ps.setString(25, lead.getRegion() != null ? lead.getRegion() : "Miền Bắc (Hà Nội)");
        ps.setString(26, lead.getRegionCode() != null ? lead.getRegionCode() : "NORTH");
        ps.setString(27, lead.getUnassignedReason());
    }

    private Lead mapResultSetToLead(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setId(rs.getLong("id"));
        lead.setCode(getColStr(rs, "code"));
        lead.setFullName(getColStr(rs, "full_name"));
        lead.setTitle(getColStr(rs, "title"));
        lead.setCompanyName(getColStr(rs, "company_name"));
        lead.setIndustry(getColStr(rs, "industry"));
        lead.setTaxCode(getColStr(rs, "tax_code"));
        lead.setEmail(getColStr(rs, "email"));
        lead.setPhone(getColStr(rs, "phone"));
        lead.setSourceId(getCol(rs, "source_id", Long.class));
        lead.setSource(getColStr(rs, "source"));
        lead.setSourceName(getColStr(rs, "source_name"));
        lead.setRating(getColStr(rs, "rating"));
        lead.setTier(getColStr(rs, "tier"));
        lead.setScore(getCol(rs, "score", Integer.class));
        lead.setStatus(getColStr(rs, "status"));
        lead.setOwnerUserId(getCol(rs, "owner_user_id", Long.class));

        String assignedToStr = getColStr(rs, "assigned_to");
        Long assignedToUid = getCol(rs, "assigned_to_user_id", Long.class);
        if (assignedToUid != null && assignedToUid > 0) {
            lead.setAssignedTo(assignedToUid);
        } else if (assignedToStr != null && !assignedToStr.isBlank()) {
            lead.setAssignedTo(assignedToStr);
        } else if (lead.getOwnerUserId() != null && lead.getOwnerUserId() > 0) {
            lead.setAssignedTo(lead.getOwnerUserId());
        }

        lead.setOwner(getColStr(rs, "owner_name"));
        lead.setEstimatedValue(getColDouble(rs, "estimated_value"));
        lead.setAssignedAt(getColTs(rs, "assigned_at"));
        lead.setSlaDeadline(getColTs(rs, "sla_deadline"));

        Integer slaTot = getCol(rs, "sla_total_minutes", Integer.class);
        if (slaTot == null || slaTot <= 0) slaTot = getCol(rs, "sla_minutes_total", Integer.class);
        lead.setSlaTotalMinutes(slaTot != null ? slaTot : 120);

        lead.setAcceptedAt(getColTs(rs, "accepted_at"));
        lead.setRejectedAt(getColTs(rs, "rejected_at"));
        lead.setRejectReason(getColStr(rs, "reject_reason"));
        lead.setRejectDetails(getColStr(rs, "reject_details"));
        lead.setSlaResult(getColStr(rs, "sla_result"));
        lead.setResponseTimeText(getColStr(rs, "response_time_text"));
        lead.setContactingStage(getColStr(rs, "contacting_stage"));
        lead.setEmployeeCount(getCol(rs, "employee_count", Integer.class));
        lead.setLocked(getColBool(rs, "is_locked"));
        lead.setCreatedBy(getCol(rs, "created_by", Long.class));
        lead.setLastContactedAt(getColTs(rs, "last_contacted_at"));
        lead.setNotes(getColStr(rs, "notes"));
        lead.setFormId(getColStr(rs, "form_id"));
        lead.setIpAddress(getColStr(rs, "ip_address"));
        lead.setRegion(getColStr(rs, "region"));
        lead.setRegionCode(getColStr(rs, "region_code"));
        lead.setUnassignedReason(getColStr(rs, "unassigned_reason"));
        lead.setDeleted(getColBool(rs, "is_deleted"));
        lead.setCreatedAt(getColTs(rs, "created_at"));
        lead.setUpdatedAt(getColTs(rs, "updated_at"));

        if (lead.getCreatedAt() != null) {
            lead.setTimeAgo(calculateTimeAgo(lead.getCreatedAt().getTime()));
        }

        return lead;
    }

    private static <T> T getCol(ResultSet rs, String col, Class<T> type) {
        try {
            return rs.getObject(col, type);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static String getColStr(ResultSet rs, String col) {
        try {
            return rs.getString(col);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static Timestamp getColTs(ResultSet rs, String col) {
        try {
            return rs.getTimestamp(col);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static boolean getColBool(ResultSet rs, String col) {
        try {
            return rs.getBoolean(col);
        } catch (SQLException ignored) {
            return false;
        }
    }

    private static double getColDouble(ResultSet rs, String col) {
        try {
            return rs.getDouble(col);
        } catch (SQLException ignored) {
            return 0.0;
        }
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
