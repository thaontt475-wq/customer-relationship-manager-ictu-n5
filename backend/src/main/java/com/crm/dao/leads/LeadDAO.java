package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;

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

    // --- S4-01: Web Form Lead Capture Methods ---

    /**
     * Inserts a new lead into database.
     */
    public Lead insert(Lead lead) throws SQLException {
        String sql = """
            INSERT INTO leads (
                full_name, company_name, email, phone, source, status,
                notes, form_id, ip_address, assigned_to, is_deleted,
                region, region_code, industry, title, unassigned_reason
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

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
                ps.setObject(10, lead.getAssignedTo());
            } else {
                ps.setNull(10, Types.BIGINT);
            }
            ps.setBoolean(11, lead.isDeleted());
            ps.setString(12, lead.getRegion() != null ? lead.getRegion() : "Miền Bắc (Hà Nội)");
            ps.setString(13, lead.getRegionCode() != null ? lead.getRegionCode() : "NORTH");
            ps.setString(14, lead.getIndustry() != null ? lead.getIndustry() : "ALL");
            ps.setString(15, lead.getTitle() != null ? lead.getTitle() : "Khách hàng tiềm năng");
            ps.setString(16, lead.getUnassignedReason());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Thêm Lead thất bại, không có dòng nào được tạo.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    lead.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Thêm Lead thất bại, không lấy được ID.");
                }
            }
        }

        // Reload to get timestamp set by database
        return findById(lead.getId());
    }

    /**
     * Finds a lead by ID.
     */
    public Lead findById(Long id) throws SQLException {
        if (id == null) return null;
        String sql = "SELECT * FROM leads WHERE id = ? AND is_deleted = 0";
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

    public Lead findById(long id) {
        try {
            return findById((Long) id);
        } catch (SQLException e) {
            System.err.println("Error fetching lead by id: " + e.getMessage());
            return null;
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

    private Lead mapRow(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setId(rs.getLong("id"));
        lead.setFullName(rs.getString("full_name"));
        lead.setCompanyName(rs.getString("company_name"));
        lead.setEmail(rs.getString("email"));
        lead.setPhone(rs.getString("phone"));
        lead.setSource(rs.getString("source"));
        lead.setStatus(rs.getString("status"));

        try {
            lead.setNotes(rs.getString("notes"));
        } catch (SQLException ignored) {}
        try {
            lead.setFormId(rs.getString("form_id"));
        } catch (SQLException ignored) {}
        try {
            lead.setIpAddress(rs.getString("ip_address"));
        } catch (SQLException ignored) {}

        try {
            long assigned = rs.getLong("assigned_to");
            if (!rs.wasNull()) {
                lead.setAssignedTo(assigned);
                lead.setAssignedToRepId(String.valueOf(assigned));
            }
        } catch (Exception e) {
            String val = rs.getString("assigned_to");
            if (val != null) {
                try {
                    long assigned = Long.parseLong(val.trim());
                    lead.setAssignedTo(assigned);
                    lead.setAssignedToRepId(String.valueOf(assigned));
                } catch (NumberFormatException ignored) {}
            }
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
        } catch (SQLException ignored) {}

        if (lead.getCreatedAt() != null) {
            lead.setTimeAgo(calculateTimeAgo(lead.getCreatedAt().getTime()));
        }

        return lead;
    }

    private Lead mapResultSetToLead(ResultSet rs) throws SQLException {
        return mapRow(rs);
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
