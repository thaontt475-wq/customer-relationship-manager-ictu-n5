package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadWriteRequest;
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
                notes, form_id, ip_address, assigned_to, is_deleted
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            }
        } catch (Exception e) {
            String val = rs.getString("assigned_to");
            if (val != null) {
                try {
                    lead.setAssignedTo(Long.parseLong(val.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        lead.setDeleted(rs.getBoolean("is_deleted"));
        lead.setCreatedAt(rs.getTimestamp("created_at"));
        lead.setUpdatedAt(rs.getTimestamp("updated_at"));
        return lead;
    }
}
