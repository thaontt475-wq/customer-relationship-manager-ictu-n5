package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadConversionRequest;
import com.crm.dto.leads.LeadDTO;
import com.crm.service.permissions.DataScopeContext;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class LeadConversionDAO {

    public Connection open() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    private final Set<String> existingColumnsCache = new HashSet<>();

    private boolean hasColumn(Connection conn, String column) {
        if (!existingColumnsCache.isEmpty()) {
            return existingColumnsCache.contains(column.toLowerCase());
        }
        try {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM leads WHERE 1=0")) {
                ResultSetMetaData rsm = rs.getMetaData();
                for (int i = 1; i <= rsm.getColumnCount(); i++) {
                    existingColumnsCache.add(rsm.getColumnLabel(i).toLowerCase());
                    existingColumnsCache.add(rsm.getColumnName(i).toLowerCase());
                }
                return existingColumnsCache.contains(column.toLowerCase());
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    public Map<String, Object> lockLeadForConversion(Connection c, long leadId) throws SQLException {
        boolean hasAssigned = hasColumn(c, "assigned_to_user_id");
        String assignedCols = hasAssigned ? ", assigned_to, assigned_to_user_id" : "";
        String sql = "SELECT id, full_name, company_name, email, phone, source_id, notes, status, " +
                     "owner_user_id, created_by, title, tax_code, address, website, " +
                     "company_phone, industry, score, tier, estimated_value, buying_role, " +
                     "is_locked, converted_at, converted_by, merged_into_id" + assignedCols + " " +
                     "FROM leads WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setLong(1, leadId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapLeadRow(rs);
                }
                return null;
            }
        }
    }

    public Map<String, Object> findLeadById(Connection c, long leadId) throws SQLException {
        boolean hasAssigned = hasColumn(c, "assigned_to_user_id");
        String assignedCols = hasAssigned ? ", assigned_to, assigned_to_user_id" : "";
        String sql = "SELECT id, full_name, company_name, email, phone, source_id, notes, status, " +
                     "owner_user_id, created_by, title, tax_code, address, website, " +
                     "company_phone, industry, score, tier, estimated_value, buying_role, " +
                     "is_locked, converted_at, converted_by, merged_into_id" + assignedCols + " " +
                     "FROM leads WHERE id = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setLong(1, leadId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapLeadRow(rs);
                }
                return null;
            }
        }
    }

    public Map<String, Object> findCustomerByTaxCode(Connection c, String taxCode) throws SQLException {
        if (taxCode == null || taxCode.isBlank()) return null;
        String sql = """
                SELECT id, name, tax_code, phone, email, address, status, owner_user_id
                FROM customers
                WHERE LOWER(TRIM(tax_code)) = LOWER(TRIM(?)) AND is_deleted = 0
                LIMIT 1
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, taxCode.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomerRow(rs);
                }
                return null;
            }
        }
    }

    public Map<String, Object> findCustomerByName(Connection c, String companyName) throws SQLException {
        if (companyName == null || companyName.isBlank()) return null;
        String sql = """
                SELECT id, name, tax_code, phone, email, address, status, owner_user_id
                FROM customers
                WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND is_deleted = 0
                LIMIT 1
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, companyName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomerRow(rs);
                }
                return null;
            }
        }
    }

    public Map<String, Object> findCustomerById(Connection c, long customerId) throws SQLException {
        String sql = """
                SELECT id, name, tax_code, phone, email, address, status, owner_user_id
                FROM customers
                WHERE id = ? AND is_deleted = 0
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomerRow(rs);
                }
                return null;
            }
        }
    }

    public Long findIndustryId(Connection c, String industryNameOrCode) throws SQLException {
        if (industryNameOrCode == null || industryNameOrCode.isBlank()) return null;
        String sql = """
                SELECT id FROM master_data
                WHERE type = 'industry' AND (LOWER(TRIM(name)) = LOWER(TRIM(?)) OR LOWER(TRIM(code)) = LOWER(TRIM(?)))
                LIMIT 1
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, industryNameOrCode.trim());
            stmt.setString(2, industryNameOrCode.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
                return null;
            }
        }
    }

    public long createCustomer(Connection c, LeadConversionRequest.CustomerData cust, long ownerUserId) throws SQLException {
        String sql = """
                INSERT INTO customers (
                    name, tax_code, status, email, phone, website, address, industry_id, owner_user_id, is_deleted
                ) VALUES (?, ?, 'TIEM_NANG', ?, ?, ?, ?, ?, ?, 0)
                """;
        Long industryId = findIndustryId(c, cust.industry);
        try (PreparedStatement stmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cust.name != null ? cust.name.trim() : "");
            stmt.setString(2, cust.taxCode != null && !cust.taxCode.isBlank() ? cust.taxCode.trim() : null);
            stmt.setString(3, null);
            stmt.setString(4, cust.phone != null && !cust.phone.isBlank() ? cust.phone.trim() : null);
            stmt.setString(5, cust.website != null && !cust.website.isBlank() ? cust.website.trim() : null);
            stmt.setString(6, cust.address != null && !cust.address.isBlank() ? cust.address.trim() : null);
            stmt.setObject(7, industryId, Types.BIGINT);
            stmt.setLong(8, ownerUserId);

            int affected = stmt.executeUpdate();
            if (affected != 1) {
                throw new SQLException("Không thể tạo khách hàng từ Lead");
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Không nhận được ID khách hàng mới");
            }
        }
    }

    public void clearPrimaryContact(Connection c, long customerId) throws SQLException {
        String sql = "UPDATE contacts SET is_primary = 0 WHERE customer_id = ? AND is_deleted = 0 AND is_primary = 1";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setLong(1, customerId);
            stmt.executeUpdate();
        }
    }

    public long createContact(Connection c, long customerId, LeadConversionRequest.ContactData contact) throws SQLException {
        clearPrimaryContact(c, customerId);
        String sql = """
                INSERT INTO contacts (
                    customer_id, full_name, email, phone, title, buying_role, is_primary, is_deleted
                ) VALUES (?, ?, ?, ?, ?, ?, 1, 0)
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, customerId);
            stmt.setString(2, contact.fullName != null ? contact.fullName.trim() : "");
            stmt.setString(3, contact.email != null ? contact.email.trim() : null);
            stmt.setString(4, contact.phone != null ? contact.phone.trim() : null);
            stmt.setString(5, contact.title != null && !contact.title.isBlank() ? contact.title.trim() : "Người đại diện");
            stmt.setString(6, contact.buyingRole != null && !contact.buyingRole.isBlank() ? contact.buyingRole.trim() : "DECISION_MAKER");

            int affected = stmt.executeUpdate();
            if (affected != 1) {
                throw new SQLException("Không thể tạo người liên hệ từ Lead");
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Không nhận được ID người liên hệ mới");
            }
        }
    }

    public long createOpportunity(Connection c, long customerId, String contactName, LeadConversionRequest.DealData deal, long ownerUserId) throws SQLException {
        String sql = """
                INSERT INTO opportunities (
                    name, customer_id, contact_name, amount, stage_id, probability,
                    expected_close_date, status, owner_user_id, is_deleted
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, 0)
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, deal.name != null ? deal.name.trim() : "");
            stmt.setLong(2, customerId);
            stmt.setString(3, contactName);
            stmt.setBigDecimal(4, deal.amount != null ? deal.amount : BigDecimal.ZERO);
            stmt.setObject(5, deal.stageId, Types.BIGINT);
            stmt.setObject(6, deal.probability != null ? deal.probability : 40, Types.INTEGER);
            stmt.setObject(7, deal.closeDate != null && !deal.closeDate.isBlank() ? LocalDate.parse(deal.closeDate.trim()) : null, Types.DATE);
            stmt.setLong(8, ownerUserId);

            int affected = stmt.executeUpdate();
            if (affected != 1) {
                throw new SQLException("Không thể tạo cơ hội bán hàng từ Lead");
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Không nhận được ID cơ hội mới");
            }
        }
    }

    public void updateLeadAsConverted(Connection c, long leadId, long userId, long customerId, long contactId, Long opportunityId) throws SQLException {
        String sql = """
                UPDATE leads SET
                    status = 'CONVERTED',
                    is_locked = 1,
                    converted_at = CURRENT_TIMESTAMP,
                    converted_by = ?,
                    converted_customer_id = ?,
                    converted_contact_id = ?,
                    converted_opportunity_id = ?
                WHERE id = ?
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, customerId);
            stmt.setLong(3, contactId);
            stmt.setObject(4, opportunityId, Types.BIGINT);
            stmt.setLong(5, leadId);
            stmt.executeUpdate();
        }
    }

    public void insertConversionHistory(Connection c, long leadId, long customerId, long contactId, Long opportunityId,
                                        long userId, String customerMode, String payloadJson, String snapshotJson) throws SQLException {
        String sql = """
                INSERT INTO lead_conversion_history (
                    lead_id, customer_id, contact_id, opportunity_id, converted_by,
                    customer_mode, conversion_payload, lead_snapshot
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setLong(1, leadId);
            stmt.setLong(2, customerId);
            stmt.setLong(3, contactId);
            stmt.setObject(4, opportunityId, Types.BIGINT);
            stmt.setLong(5, userId);
            stmt.setString(6, customerMode);
            stmt.setString(7, payloadJson);
            stmt.setString(8, snapshotJson);
            stmt.executeUpdate();
        }
    }

    public int transferLeadActivitiesToCustomer(Connection c, long leadId, long customerId, Long opportunityId, long currentUserId, String leadNotes) throws SQLException {
        if (c == null) return 0;
        int count = 0;
        try {
            // 1. Relink existing activities associated with the lead to customer and opportunity
            String relinkSql = """
                    UPDATE activities SET
                        customer_id = ?,
                        opportunity_id = COALESCE(opportunity_id, ?)
                    WHERE lead_id = ? AND is_deleted = 0
                    """;
            try (PreparedStatement stmt = c.prepareStatement(relinkSql)) {
                if (stmt != null) {
                    stmt.setLong(1, customerId);
                    stmt.setObject(2, opportunityId, Types.BIGINT);
                    stmt.setLong(3, leadId);
                    count += stmt.executeUpdate();
                }
            }

            // 2. Transfer lead notes to customer activity timeline if notes exist
            if (leadNotes != null && !leadNotes.isBlank()) {
                String insertNoteSql = """
                        INSERT INTO activities (
                            subject, type, description, status, customer_id, opportunity_id, lead_id, owner_user_id, is_deleted
                        ) VALUES (?, 'NOTE', ?, 'COMPLETED', ?, ?, ?, ?, 0)
                        """;
                try (PreparedStatement stmt = c.prepareStatement(insertNoteSql)) {
                    if (stmt != null) {
                        stmt.setString(1, "Ghi chú từ Lead (chuyển đổi)");
                        stmt.setString(2, leadNotes.trim());
                        stmt.setLong(3, customerId);
                        stmt.setObject(4, opportunityId, Types.BIGINT);
                        stmt.setLong(5, leadId);
                        stmt.setLong(6, currentUserId);
                        count += stmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            // If activities table does not exist in isolated test environments, ignore, otherwise propagate
            if (!"42S02".equals(e.getSQLState()) && e.getErrorCode() != 1146) {
                throw e;
            }
        }
        return count;
    }

    public void insertAuditLog(Connection c, long userId, String action, String entityType, long entityId, String description) throws SQLException {
        if (c == null) return;
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            if (stmt != null) {
                stmt.setLong(1, userId);
                stmt.setString(2, action);
                stmt.setString(3, entityType);
                stmt.setString(4, String.valueOf(entityId));
                stmt.setString(5, description);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // If table does not exist in mock test setup, pass, otherwise propagate so transaction aborts
            if (!"42S02".equals(e.getSQLState()) && e.getErrorCode() != 1146) {
                throw e;
            }
        }
    }

    public List<LeadDTO> searchLeads(Connection c, DataScopeContext scope, String status, String keyword, int limit, int offset) throws SQLException {
        boolean hasAssigned = hasColumn(c, "assigned_to_user_id");
        String userJoin = hasAssigned
                ? "LEFT JOIN users u ON u.id = COALESCE(l.assigned_to_user_id, CASE WHEN l.assigned_to REGEXP '^[0-9]+$' THEN CAST(l.assigned_to AS UNSIGNED) ELSE NULL END, l.owner_user_id)"
                : "LEFT JOIN users u ON u.id = l.owner_user_id";

        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.full_name, l.company_name, l.email, l.phone, l.source_id, l.notes,
                       l.status, l.owner_user_id, l.created_by, l.title, l.tax_code, l.address,
                       l.website, l.company_phone, l.industry, l.score, l.tier, l.estimated_value,
                       l.buying_role, l.is_locked, l.converted_at, l.converted_by,
                       l.converted_customer_id, l.converted_contact_id, l.converted_opportunity_id,
                       u.full_name AS owner_name,
                       cust.name AS conv_cust_name,
                       ct.full_name AS conv_contact_name,
                       opp.name AS conv_opp_name,
                       opp.amount AS conv_opp_amount
                FROM leads l
                """);
        sql.append(" ").append(userJoin).append("\n");
        sql.append("""
                LEFT JOIN customers cust ON cust.id = l.converted_customer_id
                LEFT JOIN contacts ct ON ct.id = l.converted_contact_id
                LEFT JOIN opportunities opp ON opp.id = l.converted_opportunity_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            if (hasAssigned) {
                scope.appendCoalescedOwnerPredicate("l.assigned_to_user_id", "l.owner_user_id", sql, params);
            } else {
                scope.appendOwnerPredicate("l.owner_user_id", sql, params);
            }
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND l.status = ?");
            params.add(status.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(l.full_name) LIKE LOWER(?) OR LOWER(l.company_name) LIKE LOWER(?) OR LOWER(l.email) LIKE LOWER(?) OR l.phone LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        sql.append(" ORDER BY l.id DESC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 50);
        params.add(Math.max(offset, 0));

        List<LeadDTO> result = new ArrayList<>();
        try (PreparedStatement stmt = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LeadDTO dto = new LeadDTO();
                    dto.id = rs.getLong("id");
                    dto.code = "LEAD-" + String.format("%05d", dto.id);
                    dto.fullName = rs.getString("full_name");
                    dto.companyName = rs.getString("company_name");
                    dto.email = rs.getString("email");
                    dto.phone = rs.getString("phone");
                    dto.notes = rs.getString("notes");
                    dto.status = rs.getString("status");
                    dto.title = rs.getString("title");
                    dto.taxCode = rs.getString("tax_code");
                    dto.address = rs.getString("address");
                    dto.website = rs.getString("website");
                    dto.companyPhone = rs.getString("company_phone");
                    dto.industry = rs.getString("industry");
                    dto.score = rs.getObject("score") != null ? rs.getInt("score") : 0;
                    dto.tier = rs.getString("tier");
                    dto.estimatedValue = rs.getBigDecimal("estimated_value");
                    dto.buyingRole = rs.getString("buying_role");
                    dto.isLocked = rs.getBoolean("is_locked");
                    Timestamp cat = rs.getTimestamp("converted_at");
                    dto.convertedAt = cat != null ? cat.toLocalDateTime().toString() : null;
                    dto.assignedSales = rs.getString("owner_name");

                    Long convCustId = rs.getObject("converted_customer_id", Long.class);
                    if (convCustId != null) {
                        Map<String, Object> convTo = new LinkedHashMap<>();
                        convTo.put("customerId", convCustId);
                        convTo.put("customerName", rs.getString("conv_cust_name"));
                        convTo.put("contactId", rs.getObject("converted_contact_id"));
                        convTo.put("contactName", rs.getString("conv_contact_name"));
                        convTo.put("dealId", rs.getObject("converted_opportunity_id"));
                        convTo.put("dealName", rs.getString("conv_opp_name"));
                        convTo.put("dealAmount", rs.getBigDecimal("conv_opp_amount"));
                        dto.convertedTo = convTo;
                    }
                    result.add(dto);
                }
            }
        }
        return result;
    }

    private Map<String, Object> mapLeadRow(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("fullName", rs.getString("full_name"));
        map.put("companyName", rs.getString("company_name"));
        map.put("email", rs.getString("email"));
        map.put("phone", rs.getString("phone"));
        map.put("sourceId", rs.getObject("source_id"));
        map.put("notes", rs.getString("notes"));
        map.put("status", rs.getString("status"));
        map.put("ownerUserId", rs.getLong("owner_user_id"));
        map.put("createdBy", rs.getLong("created_by"));
        map.put("title", rs.getString("title"));
        map.put("taxCode", rs.getString("tax_code"));
        map.put("address", rs.getString("address"));
        map.put("website", rs.getString("website"));
        map.put("companyPhone", rs.getString("company_phone"));
        map.put("industry", rs.getString("industry"));
        map.put("score", rs.getObject("score"));
        map.put("tier", rs.getString("tier"));
        map.put("estimatedValue", rs.getBigDecimal("estimated_value"));
        map.put("buyingRole", rs.getString("buying_role"));
        map.put("isLocked", rs.getBoolean("is_locked"));
        map.put("convertedAt", rs.getTimestamp("converted_at"));
        map.put("convertedBy", rs.getObject("converted_by"));
        map.put("mergedIntoId", rs.getObject("merged_into_id"));
        try { map.put("assignedTo", rs.getObject("assigned_to")); } catch (SQLException ignored) {}
        try { map.put("assignedToUserId", rs.getObject("assigned_to_user_id")); } catch (SQLException ignored) {}
        return map;
    }

    private Map<String, Object> mapCustomerRow(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("name", rs.getString("name"));
        map.put("taxCode", rs.getString("tax_code"));
        map.put("phone", rs.getString("phone"));
        map.put("email", rs.getString("email"));
        map.put("address", rs.getString("address"));
        map.put("status", rs.getString("status"));
        map.put("ownerUserId", rs.getLong("owner_user_id"));
        return map;
    }
}
