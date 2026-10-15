package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.model.leads.Lead;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeadDAO {

    public LeadDAO() {
        ensureTableExists();
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

            // Ensure columns notes, form_id, ip_address exist (in case table was previously created with minimal columns)
            ensureColumnExists(conn, "notes", "TEXT NULL");
            ensureColumnExists(conn, "form_id", "VARCHAR(100) NULL");
            ensureColumnExists(conn, "ip_address", "VARCHAR(50) NULL");
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
                ps.setLong(10, lead.getAssignedTo());
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
            com.crm.service.permissions.DataScopeContext scope) throws SQLException {
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
            com.crm.service.permissions.DataScopeContext scope) throws SQLException {
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

        long assigned = rs.getLong("assigned_to");
        if (!rs.wasNull()) {
            lead.setAssignedTo(assigned);
        }
        lead.setDeleted(rs.getBoolean("is_deleted"));
        lead.setCreatedAt(rs.getTimestamp("created_at"));
        lead.setUpdatedAt(rs.getTimestamp("updated_at"));
        return lead;
    }
}
