package com.crm.dao.contracts;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ContractDAO {

    private static volatile boolean schemaChecked = false;

    public ContractDAO() {
        ensureSchema();
    }

    private void ensureSchema() {
        if (schemaChecked) return;
        synchronized (ContractDAO.class) {
            if (schemaChecked) return;
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 1. Create table contracts if not exists
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS contracts (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        contract_number VARCHAR(50) NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        customer_id BIGINT NOT NULL,
                        opportunity_id BIGINT DEFAULT NULL,
                        quote_id BIGINT DEFAULT NULL,
                        total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
                        signed_date DATE DEFAULT NULL,
                        start_date DATE DEFAULT NULL,
                        end_date DATE DEFAULT NULL,
                        payment_terms TEXT DEFAULT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                        notes TEXT DEFAULT NULL,
                        owner_user_id BIGINT NOT NULL,
                        is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uq_contracts_number (contract_number),
                        KEY idx_contracts_customer (customer_id),
                        KEY idx_contracts_opportunity (opportunity_id),
                        KEY idx_contracts_quote (quote_id),
                        KEY idx_contracts_owner (owner_user_id),
                        KEY idx_contracts_status (status)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

                // 2. Safe check columns if table already existed previously
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "title")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT 'Hợp đồng'");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "opportunity_id")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN opportunity_id BIGINT DEFAULT NULL");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "quote_id")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN quote_id BIGINT DEFAULT NULL");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "start_date")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN start_date DATE DEFAULT NULL");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "end_date")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN end_date DATE DEFAULT NULL");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "payment_terms")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN payment_terms TEXT DEFAULT NULL");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "notes")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN notes TEXT DEFAULT NULL");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "owner_user_id")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN owner_user_id BIGINT NOT NULL DEFAULT 1");
                    }
                }
                try (ResultSet rs = meta.getColumns(null, null, "contracts", "is_deleted")) {
                    if (!rs.next()) {
                        stmt.execute("ALTER TABLE contracts ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0");
                    }
                }

                // 3. Ensure permissions and scopes
                try {
                    stmt.execute("""
                        INSERT IGNORE INTO permissions (code, name) VALUES
                        ('contract.read', 'Xem hợp đồng'),
                        ('contract.create', 'Tạo hợp đồng'),
                        ('contract.update', 'Sửa hợp đồng'),
                        ('contract.delete', 'Xóa hợp đồng')
                    """);
                    stmt.execute("""
                        INSERT IGNORE INTO role_permissions(role_id, permission_id)
                        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                        WHERE r.code IN ('ADMIN', 'DIRECTOR') AND p.code IN ('contract.read', 'contract.create', 'contract.update', 'contract.delete')
                    """);
                    stmt.execute("""
                        INSERT IGNORE INTO role_permissions(role_id, permission_id)
                        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                        WHERE r.code IN ('TEAM_LEAD', 'SALES_REP', 'ACCOUNTANT') AND p.code IN ('contract.read', 'contract.create', 'contract.update')
                    """);
                    stmt.execute("""
                        INSERT INTO role_module_scopes(role_id, module_code, scope_type)
                        SELECT r.id, 'contract',
                            CASE r.code WHEN 'ADMIN' THEN 'ALL' WHEN 'DIRECTOR' THEN 'ALL'
                                WHEN 'TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
                        FROM roles r
                        WHERE r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD','SALES_REP','MARKETING','CUSTOMER_SUCCESS','ACCOUNTANT')
                        ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type)
                    """);
                } catch (Exception ignored) {
                    // Ignored if permissions tables differ in custom setups
                }

                schemaChecked = true;
            } catch (SQLException e) {
                // If tables already exist, allow search queries to continue
                schemaChecked = true;
            }
        }
    }

    public List<Map<String, Object>> search(
            DataScopeContext scope,
            String keyword,
            String status,
            Long customerId
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    ct.id,
                    ct.contract_number,
                    ct.title,
                    ct.customer_id,
                    c.name AS customer_name,
                    ct.opportunity_id,
                    o.title AS opportunity_title,
                    ct.quote_id,
                    q.quote_number,
                    ct.total_amount,
                    ct.signed_date,
                    ct.start_date,
                    ct.end_date,
                    ct.payment_terms,
                    ct.status,
                    ct.notes,
                    ct.owner_user_id,
                    u.full_name AS owner_name,
                    ct.created_at,
                    ct.updated_at
                FROM contracts ct
                LEFT JOIN customers c ON c.id = ct.customer_id
                LEFT JOIN opportunities o ON o.id = ct.opportunity_id
                LEFT JOIN quotes q ON q.id = ct.quote_id
                LEFT JOIN users u ON u.id = ct.owner_user_id
                WHERE ct.is_deleted = 0
                """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("ct.owner_user_id", sql, params);
        }

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            sql.append(" AND ct.status = ?");
            params.add(status.trim().toUpperCase());
        }

        if (customerId != null && customerId > 0) {
            sql.append(" AND ct.customer_id = ?");
            params.add(customerId);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(ct.contract_number) LIKE ? OR LOWER(ct.title) LIKE ? OR LOWER(c.name) LIKE ?)");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        sql.append(" ORDER BY ct.id DESC");

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapContract(rs));
                }
            }
        }
        return list;
    }

    public Map<String, Object> findById(long id) throws SQLException {
        String sql = """
                SELECT
                    ct.id,
                    ct.contract_number,
                    ct.title,
                    ct.customer_id,
                    c.name AS customer_name,
                    ct.opportunity_id,
                    o.title AS opportunity_title,
                    ct.quote_id,
                    q.quote_number,
                    ct.total_amount,
                    ct.signed_date,
                    ct.start_date,
                    ct.end_date,
                    ct.payment_terms,
                    ct.status,
                    ct.notes,
                    ct.owner_user_id,
                    u.full_name AS owner_name,
                    ct.created_at,
                    ct.updated_at
                FROM contracts ct
                LEFT JOIN customers c ON c.id = ct.customer_id
                LEFT JOIN opportunities o ON o.id = ct.opportunity_id
                LEFT JOIN quotes q ON q.id = ct.quote_id
                LEFT JOIN users u ON u.id = ct.owner_user_id
                WHERE ct.id = ? AND ct.is_deleted = 0
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapContract(rs);
                }
            }
        }
        return null;
    }

    public boolean existsByContractNumber(String contractNumber, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM contracts WHERE contract_number = ? AND is_deleted = 0" +
                     (excludeId != null ? " AND id <> ?" : "");
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, contractNumber);
            if (excludeId != null) {
                stmt.setLong(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long create(
            String contractNumber,
            String title,
            long customerId,
            Long opportunityId,
            Long quoteId,
            BigDecimal totalAmount,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            String paymentTerms,
            String status,
            String notes,
            long ownerUserId
    ) throws SQLException {
        String sql = """
                INSERT INTO contracts (
                    contract_number, title, customer_id, opportunity_id, quote_id,
                    total_amount, signed_date, start_date, end_date,
                    payment_terms, status, notes, owner_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, contractNumber);
            stmt.setString(2, title);
            stmt.setLong(3, customerId);
            stmt.setObject(4, opportunityId);
            stmt.setObject(5, quoteId);
            stmt.setBigDecimal(6, totalAmount != null ? totalAmount : BigDecimal.ZERO);
            stmt.setObject(7, signedDate);
            stmt.setObject(8, startDate);
            stmt.setObject(9, endDate);
            stmt.setString(10, paymentTerms);
            stmt.setString(11, status != null && !status.isBlank() ? status.toUpperCase() : "DRAFT");
            stmt.setString(12, notes);
            stmt.setLong(13, ownerUserId);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Không lấy được id hợp đồng tạo mới");
            }
        }
    }

    public void update(
            long id,
            String contractNumber,
            String title,
            long customerId,
            Long opportunityId,
            Long quoteId,
            BigDecimal totalAmount,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            String paymentTerms,
            String status,
            String notes
    ) throws SQLException {
        String sql = """
                UPDATE contracts SET
                    contract_number = ?,
                    title = ?,
                    customer_id = ?,
                    opportunity_id = ?,
                    quote_id = ?,
                    total_amount = ?,
                    signed_date = ?,
                    start_date = ?,
                    end_date = ?,
                    payment_terms = ?,
                    status = ?,
                    notes = ?
                WHERE id = ? AND is_deleted = 0
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contractNumber);
            stmt.setString(2, title);
            stmt.setLong(3, customerId);
            stmt.setObject(4, opportunityId);
            stmt.setObject(5, quoteId);
            stmt.setBigDecimal(6, totalAmount != null ? totalAmount : BigDecimal.ZERO);
            stmt.setObject(7, signedDate);
            stmt.setObject(8, startDate);
            stmt.setObject(9, endDate);
            stmt.setString(10, paymentTerms);
            stmt.setString(11, status != null && !status.isBlank() ? status.toUpperCase() : "DRAFT");
            stmt.setString(12, notes);
            stmt.setLong(13, id);

            stmt.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        String sql = "UPDATE contracts SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Map<String, Object> mapContract(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("contractNumber", rs.getString("contract_number"));
        map.put("title", rs.getString("title"));
        map.put("customerId", rs.getObject("customer_id"));
        map.put("customerName", rs.getString("customer_name"));
        map.put("opportunityId", rs.getObject("opportunity_id"));
        map.put("opportunityTitle", rs.getString("opportunity_title"));
        map.put("quoteId", rs.getObject("quote_id"));
        map.put("quoteNumber", rs.getString("quote_number"));
        map.put("totalAmount", rs.getBigDecimal("total_amount"));
        map.put("signedDate", rs.getString("signed_date"));
        map.put("startDate", rs.getString("start_date"));
        map.put("endDate", rs.getString("end_date"));
        map.put("paymentTerms", rs.getString("payment_terms"));
        map.put("status", rs.getString("status"));
        map.put("notes", rs.getString("notes"));
        map.put("ownerUserId", rs.getLong("owner_user_id"));
        map.put("ownerName", rs.getString("owner_name"));
        map.put("createdAt", rs.getTimestamp("created_at"));
        map.put("updatedAt", rs.getTimestamp("updated_at"));
        return map;
    }
}
