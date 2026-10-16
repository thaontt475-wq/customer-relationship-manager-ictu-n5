package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;

import java.sql.*;
import java.time.temporal.TemporalAccessor;
import java.util.*;

public class LeadDAO {

    public LeadDAO() {
        ensureTableExists();
    }

    public Connection open() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    /**
     * Self-healing table check: ensures table and columns exist in database.
     */
    public void ensureTableExists() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS leads (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                full_name VARCHAR(150) NOT NULL,
                company_name VARCHAR(255) NULL,
                email VARCHAR(255) NULL,
                phone VARCHAR(50) NULL,
                source VARCHAR(50) NULL DEFAULT 'WEB_FORM',
                status VARCHAR(50) NOT NULL DEFAULT 'NEW',
                notes TEXT NULL,
                form_id VARCHAR(100) NULL,
                ip_address VARCHAR(50) NULL,
                assigned_to BIGINT NULL,
                source_id BIGINT NULL,
                owner_user_id BIGINT NULL,
                created_by BIGINT NULL,
                import_batch_hash VARCHAR(64) NULL,
                region VARCHAR(100) NULL DEFAULT 'Miền Bắc (Hà Nội)',
                region_code VARCHAR(50) NULL DEFAULT 'NORTH',
                industry VARCHAR(100) NULL DEFAULT 'ALL',
                title VARCHAR(150) NULL DEFAULT 'Khách hàng tiềm năng',
                unassigned_reason VARCHAR(255) NULL,
                is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_leads_status (status),
                INDEX idx_leads_source (source),
                INDEX idx_leads_assigned (assigned_to),
                INDEX idx_leads_is_deleted (is_deleted)
            );
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);

            // Ensure columns exist
            ensureColumnExists(conn, "notes", "TEXT NULL");
            ensureColumnExists(conn, "source", "VARCHAR(50) NULL DEFAULT 'WEB_FORM'");
            ensureColumnExists(conn, "form_id", "VARCHAR(100) NULL");
            ensureColumnExists(conn, "ip_address", "VARCHAR(50) NULL");
            ensureColumnExists(conn, "assigned_to", "BIGINT NULL");
            ensureColumnExists(conn, "source_id", "BIGINT NULL");
            ensureColumnExists(conn, "owner_user_id", "BIGINT NULL");
            ensureColumnExists(conn, "created_by", "BIGINT NULL");
            ensureColumnExists(conn, "import_batch_hash", "VARCHAR(64) NULL");
            ensureColumnExists(conn, "region", "VARCHAR(100) NULL DEFAULT 'Miền Bắc (Hà Nội)'");
            ensureColumnExists(conn, "region_code", "VARCHAR(50) NULL DEFAULT 'NORTH'");
            ensureColumnExists(conn, "industry", "VARCHAR(100) NULL DEFAULT 'ALL'");
            ensureColumnExists(conn, "title", "VARCHAR(150) NULL DEFAULT 'Khách hàng tiềm năng'");
            ensureColumnExists(conn, "unassigned_reason", "VARCHAR(255) NULL");
            ensureColumnExists(conn, "is_deleted", "TINYINT(1) NOT NULL DEFAULT 0");

            // S4-07 SLA columns
            ensureColumnExists(conn, "assigned_to_user_id", "BIGINT NULL");
            ensureColumnExists(conn, "assigned_at", "DATETIME NULL");
            ensureColumnExists(conn, "sla_total_minutes", "INT NULL DEFAULT 120");
            ensureColumnExists(conn, "sla_deadline", "DATETIME NULL");
            ensureColumnExists(conn, "accepted_at", "DATETIME NULL");
            ensureColumnExists(conn, "rejected_at", "DATETIME NULL");
            ensureColumnExists(conn, "reject_reason", "VARCHAR(100) NULL");
            ensureColumnExists(conn, "reject_details", "TEXT NULL");
            ensureColumnExists(conn, "sla_result", "VARCHAR(50) NULL");
            ensureColumnExists(conn, "response_time_text", "VARCHAR(255) NULL");
            ensureColumnExists(conn, "contacting_stage", "VARCHAR(255) NULL");
            ensureColumnExists(conn, "is_locked", "TINYINT(1) NOT NULL DEFAULT 0");
            ensureColumnExists(conn, "employee_count", "INT NULL DEFAULT 0");
            ensureColumnExists(conn, "score", "INT NULL DEFAULT 0");
        } catch (SQLException e) {
            System.err.println("Notice: LeadDAO ensureTableExists check: " + e.getMessage());
        }
    }

    private void ensureColumnExists(Connection conn, String column, String definition) {
        try {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, "leads", column)) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE leads ADD COLUMN `" + column + "` " + definition);
                    }
                }
            }
        } catch (SQLException ignored) {
        }
    }

    // --- S4-02 & S4-05: Lead Manual, Excel Import & Scoring Methods ---

    public List<Map<String, Object>> sources(Connection c, boolean lock) throws SQLException {
        String query = "SELECT id,code,name FROM master_data WHERE type='lead-source' AND active=TRUE ORDER BY id" + (lock ? " FOR SHARE" : "");
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(query); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                result.add(Map.of("id", r.getLong("id"), "code", r.getString("code"), "name", r.getString("name")));
            }
        }
        return result;
    }

    public long insert(Connection c, LeadWriteRequest lead, long user, String batch) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("""
            INSERT INTO leads(full_name,company_name,email,phone,source_id,notes,status,owner_user_id,created_by,import_batch_hash)
            VALUES (?,?,?,?,?,?,'NEW',?,?,?)
            """, Statement.RETURN_GENERATED_KEYS)) {
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
            SELECT l.id,l.full_name AS fullName,l.company_name AS companyName,l.email,l.phone,l.source_id AS sourceId,
            s.code AS sourceCode,s.name AS sourceName,l.notes,l.status,l.owner_user_id AS ownerUserId,
            l.created_by AS createdBy,l.created_at AS createdAt
            FROM leads l JOIN master_data s ON s.id=l.source_id WHERE l.id=?
            """);
        List<Object> values = new ArrayList<>();
        values.add(id);
        scope.appendOwnerPredicate("l.owner_user_id", query, values);
        if (lock) query.append(" FOR UPDATE");
        try (PreparedStatement p = c.prepareStatement(query.toString())) {
            for (int i = 0; i < values.size(); i++) p.setObject(i + 1, values.get(i));
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                Map<String, Object> result = new LinkedHashMap<>();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
                    Object value = r.getObject(i);
                    result.put(r.getMetaData().getColumnLabel(i), value instanceof Timestamp date ? date.toLocalDateTime().toString() :
                            value instanceof TemporalAccessor ? value.toString() : value);
                }
                return result;
            }
        }
    }

    public void update(Connection c, long id, LeadWriteRequest lead) throws SQLException {
        try (var p = c.prepareStatement("UPDATE leads SET full_name=?,company_name=?,email=?,phone=?,source_id=?,notes=? WHERE id=?")) {
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
        try (PreparedStatement p = c.prepareStatement("INSERT INTO lead_import_batches(token_hash,owner_user_id,total_rows,success_count,failure_count) VALUES (?,?,?,?,?)")) {
            p.setString(1, hash);
            p.setLong(2, user);
            p.setInt(3, total);
            p.setInt(4, success);
            p.setInt(5, failure);
            p.executeUpdate();
        }
    }

    // --- S4-01 & S4-07: Lead Insert & Find Methods ---

    public long insert(Lead lead) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return insert(conn, lead);
        }
    }

    public long insert(Connection conn, Lead lead) throws SQLException {
        String sql = """
            INSERT INTO leads
            (full_name, company_name, email, phone, title, source_id, source, region, region_code, industry,
             employee_count, score, notes, status, form_id, ip_address, owner_user_id, assigned_to, assigned_to_user_id,
             assigned_at, sla_total_minutes, sla_deadline, is_locked, is_deleted, created_by, unassigned_reason, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, lead.getFullName());
            stmt.setString(2, lead.getCompanyName());
            stmt.setString(3, lead.getEmail());
            stmt.setString(4, lead.getPhone());
            stmt.setString(5, lead.getTitle() != null ? lead.getTitle() : "Khách hàng tiềm năng");
            stmt.setObject(6, lead.getSourceId(), Types.BIGINT);
            stmt.setString(7, lead.getSource() != null ? lead.getSource() : "WEB_FORM");
            stmt.setString(8, lead.getRegion() != null ? lead.getRegion() : "Miền Bắc (Hà Nội)");
            stmt.setString(9, lead.getRegionCode() != null ? lead.getRegionCode() : "NORTH");
            stmt.setString(10, lead.getIndustry() != null ? lead.getIndustry() : "ALL");
            stmt.setInt(11, lead.getEmployeeCount() != null ? lead.getEmployeeCount() : 0);
            stmt.setInt(12, lead.getScore() != null ? lead.getScore() : 0);
            stmt.setString(13, lead.getNotes());
            stmt.setString(14, lead.getStatus() != null ? lead.getStatus() : "NEW");
            stmt.setString(15, lead.getFormId());
            stmt.setString(16, lead.getIpAddress());
            stmt.setObject(17, lead.getOwnerUserId(), Types.BIGINT);
            if (lead.getAssignedTo() != null) {
                stmt.setObject(18, lead.getAssignedTo(), Types.BIGINT);
            } else {
                stmt.setNull(18, Types.BIGINT);
            }
            stmt.setObject(19, lead.getAssignedToUserId(), Types.BIGINT);
            stmt.setTimestamp(20, lead.getAssignedAt());
            stmt.setInt(21, lead.getSlaTotalMinutes() != null ? lead.getSlaTotalMinutes() : 120);
            stmt.setTimestamp(22, lead.getSlaDeadline());
            stmt.setBoolean(23, lead.isLocked());
            stmt.setBoolean(24, lead.isDeleted());
            stmt.setObject(25, lead.getCreatedBy(), Types.BIGINT);
            stmt.setString(26, lead.getUnassignedReason());
            stmt.setTimestamp(27, lead.getCreatedAt() != null ? lead.getCreatedAt() : new Timestamp(System.currentTimeMillis()));

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

    public Lead findById(long id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findById(conn, id, false);
        } catch (SQLException e) {
            System.err.println("Error fetching lead by id: " + e.getMessage());
            return null;
        }
    }

    public Lead findById(Long id) throws SQLException {
        if (id == null) return null;
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

    // --- S4-07: SLA Response Methods ---

    public List<Lead> findMyAssignedLeads(long userId, DataScopeContext scope) throws SQLException {
        List<Lead> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM leads WHERE is_deleted = 0 AND ");

        if (scope != null && scope.scopeType() == ScopeType.ALL) {
            sql.append("(assigned_to_user_id IS NOT NULL OR assigned_to IS NOT NULL OR status IN ('PENDING_RESPONSE', 'CONTACTING', 'REJECTED')) ");
        } else if (scope != null && scope.scopeType() == ScopeType.TEAM) {
            Set<Long> owners = scope.allowedOwnerIds();
            if (owners == null || owners.isEmpty()) {
                sql.append("(assigned_to_user_id = ").append(userId).append(" OR assigned_to = ").append(userId).append(") ");
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
                    sql.append(oid);
                }
                sql.append(")) ");
            }
        } else {
            sql.append("(assigned_to_user_id = ? OR assigned_to = ? OR assigned_to = ?) ");
        }

        sql.append("ORDER BY FIELD(status, 'PENDING_RESPONSE', 'CONTACTING', 'REJECTED', 'PENDING_ASSIGNMENT'), id DESC");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (scope == null || scope.scopeType() == ScopeType.SELF) {
                stmt.setLong(1, userId);
                stmt.setLong(2, userId);
                stmt.setLong(3, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
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

    /**
     * Lists leads with pagination and optional status filter.
     */
    public List<Lead> findAll(int page, int size, String status, String source,
            DataScopeContext scope) throws SQLException {
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

        scope.appendOwnerPredicate("assigned_to", sql, params);
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
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Counts leads with optional filters.
     */
    public int count(String status, String source,
            DataScopeContext scope) throws SQLException {
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

        scope.appendOwnerPredicate("assigned_to", sql, params);
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

    // --- S4-06: Lead Auto-Assignment Methods ---

    public List<Lead> findUnassignedOrPendingQueue() {
        String sql = "SELECT * FROM leads WHERE (status = 'PENDING_ASSIGNMENT' OR assigned_to IS NULL) " +
                "AND is_deleted = 0 ORDER BY created_at DESC";
        List<Lead> leads = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                leads.add(mapRow(rs));
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
                    leads.add(mapRow(rs));
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

    public Lead mapRow(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setId(rs.getLong("id"));
        lead.setFullName(rs.getString("full_name"));
        lead.setCompanyName(rs.getString("company_name"));
        lead.setEmail(rs.getString("email"));
        lead.setPhone(rs.getString("phone"));
        lead.setSource(rs.getString("source"));
        lead.setStatus(rs.getString("status"));

        try { lead.setTitle(rs.getString("title")); } catch (SQLException ignored) {}
        try { lead.setSourceId(rs.getObject("source_id", Long.class)); } catch (SQLException ignored) {}
        try { lead.setRegion(rs.getString("region")); } catch (SQLException ignored) {}
        try { lead.setRegionCode(rs.getString("region_code")); } catch (SQLException ignored) {}
        try { lead.setIndustry(rs.getString("industry")); } catch (SQLException ignored) {}
        try { lead.setEmployeeCount(rs.getObject("employee_count", Integer.class)); } catch (SQLException ignored) {}
        try { lead.setScore(rs.getObject("score", Integer.class)); } catch (SQLException ignored) {}
        try { lead.setNotes(rs.getString("notes")); } catch (SQLException ignored) {}
        try { lead.setFormId(rs.getString("form_id")); } catch (SQLException ignored) {}
        try { lead.setIpAddress(rs.getString("ip_address")); } catch (SQLException ignored) {}
        try { lead.setOwnerUserId(rs.getObject("owner_user_id", Long.class)); } catch (SQLException ignored) {}

        try {
            long assigned = rs.getLong("assigned_to");
            if (!rs.wasNull()) {
                lead.setAssignedTo(assigned);
            }
        } catch (Exception e) {
            String val = rs.getString("assigned_to");
            if (val != null) {
                lead.setAssignedTo(val);
            }
        }

        try {
            Long assignedUserId = rs.getObject("assigned_to_user_id", Long.class);
            if (assignedUserId != null) {
                lead.setAssignedToUserId(assignedUserId);
            }
        } catch (SQLException ignored) {}

        try { lead.setAssignedAt(rs.getTimestamp("assigned_at")); } catch (SQLException ignored) {}
        try { lead.setSlaTotalMinutes(rs.getObject("sla_total_minutes", Integer.class)); } catch (SQLException ignored) {}
        try { lead.setSlaDeadline(rs.getTimestamp("sla_deadline")); } catch (SQLException ignored) {}
        try { lead.setAcceptedAt(rs.getTimestamp("accepted_at")); } catch (SQLException ignored) {}
        try { lead.setRejectedAt(rs.getTimestamp("rejected_at")); } catch (SQLException ignored) {}
        try { lead.setRejectReason(rs.getString("reject_reason")); } catch (SQLException ignored) {}
        try { lead.setRejectDetails(rs.getString("reject_details")); } catch (SQLException ignored) {}
        try { lead.setSlaResult(rs.getString("sla_result")); } catch (SQLException ignored) {}
        try { lead.setResponseTimeText(rs.getString("response_time_text")); } catch (SQLException ignored) {}
        try { lead.setContactingStage(rs.getString("contacting_stage")); } catch (SQLException ignored) {}
        try { lead.setUnassignedReason(rs.getString("unassigned_reason")); } catch (SQLException ignored) {}
        try { lead.setLocked(rs.getBoolean("is_locked")); } catch (SQLException ignored) {}
        try { lead.setDeleted(rs.getBoolean("is_deleted")); } catch (SQLException ignored) {}
        try { lead.setCreatedBy(rs.getObject("created_by", Long.class)); } catch (SQLException ignored) {}
        try { lead.setCreatedAt(rs.getTimestamp("created_at")); } catch (SQLException ignored) {}
        try { lead.setUpdatedAt(rs.getTimestamp("updated_at")); } catch (SQLException ignored) {}

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
