package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class LeadDuplicateMergeDAO {

    public Connection open() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    private final Map<String, Boolean> columnCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Boolean> tableCache = new java.util.concurrent.ConcurrentHashMap<>();

    public boolean hasColumn(Connection c, String table, String column) {
        String key = (table + "." + column).toLowerCase(Locale.ROOT);
        return columnCache.computeIfAbsent(key, k -> {
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SHOW COLUMNS FROM " + table)) {
                while (rs.next()) {
                    if (column.equalsIgnoreCase(rs.getString("Field"))) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
                try (Statement s = c.createStatement();
                     ResultSet rs = s.executeQuery("SELECT * FROM " + table + " WHERE 1=0")) {
                    ResultSetMetaData md = rs.getMetaData();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        if (column.equalsIgnoreCase(md.getColumnLabel(i)) || column.equalsIgnoreCase(md.getColumnName(i))) {
                            return true;
                        }
                    }
                } catch (Exception ignored2) {}
            }
            return false;
        });
    }

    public boolean hasTable(Connection c, String table) {
        String key = table.toLowerCase(Locale.ROOT);
        return tableCache.computeIfAbsent(key, k -> {
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT 1 FROM " + table + " WHERE 1=0")) {
                return true;
            } catch (Exception ignored) {
                return false;
            }
        });
    }

    public List<Map<String, Object>> findActiveLeads(Connection c, DataScopeContext scope, int limit) throws SQLException {
        boolean hasMergedInto = hasColumn(c, "leads", "merged_into_id");
        boolean hasTitle = hasColumn(c, "leads", "title");
        boolean hasCustomerId = hasColumn(c, "leads", "customer_id");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id, l.full_name AS fullName, l.company_name AS companyName, l.email, l.phone, ");
        sql.append("l.source_id AS sourceId, l.notes, l.status, l.owner_user_id AS ownerUserId, ");
        sql.append("l.created_by AS createdBy, l.created_at AS createdAt, l.updated_at AS updatedAt, ");
        if (hasTitle) {
            sql.append("l.title, ");
        }
        if (hasMergedInto) {
            sql.append("l.merged_into_id AS mergedIntoId, ");
        }
        if (hasCustomerId) {
            sql.append("l.customer_id AS customerId, ");
        }
        sql.append("s.name AS sourceName, s.code AS sourceCode ");
        sql.append("FROM leads l ");
        sql.append("LEFT JOIN master_data s ON s.id = l.source_id ");
        sql.append("WHERE l.status != 'MERGED' ");
        if (hasMergedInto) {
            sql.append("AND l.merged_into_id IS NULL ");
        }

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("l.owner_user_id", sql, params);
        }
        sql.append(" ORDER BY l.id ASC LIMIT ?");
        params.add(limit > 0 ? limit : 1000);

        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    results.add(mapLeadRow(rs, hasTitle, hasMergedInto));
                }
            }
        }
        return results;
    }

    public List<Map<String, Object>> findActiveCustomers(Connection c, DataScopeContext customerScope, int limit) throws SQLException {
        if (!hasTable(c, "customers")) {
            return Collections.emptyList();
        }
        boolean hasIsDeleted = hasColumn(c, "customers", "is_deleted");
        boolean hasMergedInto = hasColumn(c, "customers", "merged_into_id");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.id, c.name, c.email, c.phone, c.address, c.status, c.owner_user_id AS ownerUserId, c.created_at AS createdAt ");
        sql.append("FROM customers c WHERE 1=1 ");
        if (hasIsDeleted) {
            sql.append("AND c.is_deleted = 0 ");
        }
        if (hasMergedInto) {
            sql.append("AND c.merged_into_id IS NULL ");
        }
        sql.append("AND c.status != 'DA_GOP' ");

        List<Object> params = new ArrayList<>();
        if (customerScope != null) {
            customerScope.appendOwnerPredicate("c.owner_user_id", sql, params);
        }
        sql.append(" ORDER BY c.id ASC LIMIT ?");
        params.add(limit > 0 ? limit : 1000);

        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("name", rs.getString("name"));
                    row.put("email", rs.getString("email"));
                    row.put("phone", rs.getString("phone"));
                    row.put("address", rs.getString("address"));
                    row.put("status", rs.getString("status"));
                    row.put("ownerUserId", rs.getLong("ownerUserId"));
                    Timestamp cat = rs.getTimestamp("createdAt");
                    row.put("createdAt", cat != null ? cat.toInstant().toString() : null);
                    results.add(row);
                }
            }
        }
        return results;
    }

    public Map<String, Object> findLeadById(Connection c, long leadId) throws SQLException {
        boolean hasMergedInto = hasColumn(c, "leads", "merged_into_id");
        boolean hasTitle = hasColumn(c, "leads", "title");
        boolean hasCustomerId = hasColumn(c, "leads", "customer_id");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id, l.full_name AS fullName, l.company_name AS companyName, l.email, l.phone, ");
        sql.append("l.source_id AS sourceId, l.notes, l.status, l.owner_user_id AS ownerUserId, ");
        sql.append("l.created_by AS createdBy, l.created_at AS createdAt, l.updated_at AS updatedAt, ");
        if (hasTitle) {
            sql.append("l.title, ");
        }
        if (hasMergedInto) {
            sql.append("l.merged_into_id AS mergedIntoId, ");
        }
        if (hasCustomerId) {
            sql.append("l.customer_id AS customerId, ");
        }
        sql.append("s.name AS sourceName, s.code AS sourceCode ");
        sql.append("FROM leads l LEFT JOIN master_data s ON s.id = l.source_id WHERE l.id = ?");

        try (PreparedStatement p = c.prepareStatement(sql.toString())) {
            p.setLong(1, leadId);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) return null;
                return mapLeadRow(rs, hasTitle, hasMergedInto);
            }
        }
    }

    public Map<String, Object> findCustomerById(Connection c, long customerId) throws SQLException {
        if (!hasTable(c, "customers")) return null;
        String sql = "SELECT * FROM customers WHERE id = ?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, customerId);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    map.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                return map;
            }
        }
    }

    public List<Map<String, Object>> lockLeadsForUpdate(Connection c, long id1, long id2) throws SQLException {
        long minId = Math.min(id1, id2);
        long maxId = Math.max(id1, id2);

        boolean hasMergedInto = hasColumn(c, "leads", "merged_into_id");
        boolean hasTitle = hasColumn(c, "leads", "title");
        boolean hasCustomerId = hasColumn(c, "leads", "customer_id");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id, l.full_name AS fullName, l.company_name AS companyName, l.email, l.phone, ");
        sql.append("l.source_id AS sourceId, l.notes, l.status, l.owner_user_id AS ownerUserId, ");
        sql.append("l.created_by AS createdBy, l.created_at AS createdAt, l.updated_at AS updatedAt, ");
        if (hasTitle) {
            sql.append("l.title, ");
        }
        if (hasMergedInto) {
            sql.append("l.merged_into_id AS mergedIntoId, ");
        }
        if (hasCustomerId) {
            sql.append("l.customer_id AS customerId, ");
        }
        sql.append("s.name AS sourceName, s.code AS sourceCode ");
        sql.append("FROM leads l LEFT JOIN master_data s ON s.id = l.source_id ");
        sql.append("WHERE l.id IN (?, ?) ORDER BY l.id ASC FOR UPDATE");

        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql.toString())) {
            p.setLong(1, minId);
            p.setLong(2, maxId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLeadRow(rs, hasTitle, hasMergedInto));
                }
            }
        }
        return list;
    }

    public void updateMasterLead(Connection c, long masterId, Map<String, Object> fields, long userId) throws SQLException {
        boolean hasTitle = hasColumn(c, "leads", "title");

        StringBuilder sql = new StringBuilder("UPDATE leads SET full_name = ?, company_name = ?, email = ?, phone = ?, notes = ?");
        if (fields.containsKey("sourceId")) {
            sql.append(", source_id = ?");
        }
        if (hasTitle && fields.containsKey("title")) {
            sql.append(", title = ?");
        }
        sql.append(", updated_at = CURRENT_TIMESTAMP WHERE id = ?");

        try (PreparedStatement p = c.prepareStatement(sql.toString())) {
            int idx = 1;
            p.setString(idx++, (String) fields.get("fullName"));
            p.setString(idx++, (String) fields.get("companyName"));
            p.setString(idx++, (String) fields.get("email"));
            p.setString(idx++, (String) fields.get("phone"));
            p.setString(idx++, (String) fields.get("notes"));
            if (fields.containsKey("sourceId")) {
                p.setLong(idx++, ((Number) fields.get("sourceId")).longValue());
            }
            if (hasTitle && fields.containsKey("title")) {
                p.setString(idx++, (String) fields.get("title"));
            }
            p.setLong(idx++, masterId);
            p.executeUpdate();
        }
    }

    public void archiveDuplicateLead(Connection c, long duplicateId, long masterId, long userId) throws SQLException {
        boolean hasMergedInto = hasColumn(c, "leads", "merged_into_id");
        boolean hasMergedAt = hasColumn(c, "leads", "merged_at");
        boolean hasMergedBy = hasColumn(c, "leads", "merged_by");

        StringBuilder sql = new StringBuilder("UPDATE leads SET status = 'MERGED'");
        if (hasMergedInto) {
            sql.append(", merged_into_id = ?");
        }
        if (hasMergedAt) {
            sql.append(", merged_at = CURRENT_TIMESTAMP");
        }
        if (hasMergedBy) {
            sql.append(", merged_by = ?");
        }
        sql.append(", updated_at = CURRENT_TIMESTAMP WHERE id = ?");

        try (PreparedStatement p = c.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasMergedInto) {
                p.setLong(idx++, masterId);
            }
            if (hasMergedBy) {
                p.setLong(idx++, userId);
            }
            p.setLong(idx++, duplicateId);
            p.executeUpdate();
        }
    }

    public void insertMergeHistory(
            Connection c,
            long masterId,
            long duplicateId,
            long userId,
            String beforeMaster,
            String beforeDuplicate,
            String afterMaster,
            String overrides,
            boolean retainHistory
    ) throws SQLException {
        if (!hasTable(c, "lead_merge_history")) return;

        String sql = """
            INSERT INTO lead_merge_history
            (master_lead_id, duplicate_lead_id, performed_by, before_master, before_duplicate, after_master, field_overrides, retain_history)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, masterId);
            p.setLong(2, duplicateId);
            p.setLong(3, userId);
            p.setString(4, beforeMaster);
            p.setString(5, beforeDuplicate);
            p.setString(6, afterMaster);
            p.setString(7, overrides);
            p.setBoolean(8, retainHistory);
            p.executeUpdate();
        }
    }

    public void linkLeadToCustomer(Connection c, long leadId, long customerId, long userId, String notes) throws SQLException {
        boolean hasCustomerId = hasColumn(c, "leads", "customer_id");
        if (hasCustomerId) {
            try (PreparedStatement p = c.prepareStatement("UPDATE leads SET customer_id = ?, status = 'CONVERTED', updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                p.setLong(1, customerId);
                p.setLong(2, leadId);
                p.executeUpdate();
            }
        } else {
            try (PreparedStatement p = c.prepareStatement("UPDATE leads SET status = 'CONVERTED', updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                p.setLong(1, leadId);
                p.executeUpdate();
            }
        }

        if (hasTable(c, "lead_customer_links")) {
            String sql = "INSERT INTO lead_customer_links(lead_id, customer_id, linked_by, notes) VALUES (?, ?, ?, ?)";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, leadId);
                p.setLong(2, customerId);
                p.setLong(3, userId);
                p.setString(4, notes);
                p.executeUpdate();
            }
        }
    }

    public Long resolveSourceId(Connection c, Object sourceVal) throws SQLException {
        if (sourceVal == null) return null;
        if (sourceVal instanceof Number num) return num.longValue();
        String str = sourceVal.toString().trim();
        if (str.matches("[1-9][0-9]*")) {
            try { return Long.parseLong(str); } catch (Exception ignored) {}
        }
        String sql = "SELECT id FROM master_data WHERE type = 'lead-source' AND (code = ? OR name = ?) LIMIT 1";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, str);
            p.setString(2, str);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return null;
    }

    public void insertAuditLog(
            Connection c,
            long userId,
            String entityType,
            String entityId,
            String action,
            String description,
            String beforeVal,
            String afterVal
    ) throws SQLException {
        if (!hasTable(c, "audit_logs")) return;

        String sql = "INSERT INTO audit_logs(user_id, entity_type, entity_id, action, description, before_value, after_value) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            p.setString(2, entityType);
            p.setString(3, entityId);
            p.setString(4, action);
            p.setString(5, description);
            p.setString(6, beforeVal);
            p.setString(7, afterVal);
            p.executeUpdate();
        }
    }

    private String getColStr(ResultSet rs, String... names) {
        for (String name : names) {
            try {
                String val = rs.getString(name);
                if (val != null) return val;
            } catch (SQLException ignored) {}
        }
        return null;
    }

    private Long getColLong(ResultSet rs, String... names) {
        for (String name : names) {
            try {
                Object obj = rs.getObject(name);
                if (obj instanceof Number n) return n.longValue();
            } catch (SQLException ignored) {}
        }
        return null;
    }

    private Timestamp getColTimestamp(ResultSet rs, String... names) {
        for (String name : names) {
            try {
                Timestamp ts = rs.getTimestamp(name);
                if (ts != null) return ts;
            } catch (SQLException ignored) {}
        }
        return null;
    }

    private Map<String, Object> mapLeadRow(ResultSet rs, boolean hasTitle, boolean hasMergedInto) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", getColLong(rs, "id"));
        map.put("fullName", getColStr(rs, "fullName", "full_name"));
        map.put("companyName", getColStr(rs, "companyName", "company_name"));
        map.put("email", getColStr(rs, "email"));
        map.put("phone", getColStr(rs, "phone"));
        map.put("sourceId", getColLong(rs, "sourceId", "source_id"));
        map.put("notes", getColStr(rs, "notes"));
        map.put("status", getColStr(rs, "status"));
        map.put("ownerUserId", getColLong(rs, "ownerUserId", "owner_user_id"));
        map.put("createdBy", getColLong(rs, "createdBy", "created_by"));
        map.put("title", getColStr(rs, "title"));
        map.put("mergedIntoId", getColLong(rs, "mergedIntoId", "merged_into_id"));
        map.put("customerId", getColLong(rs, "customerId", "customer_id"));

        String sourceName = getColStr(rs, "sourceName", "source_name");
        String sourceCode = getColStr(rs, "sourceCode", "source_code");
        map.put("sourceName", sourceName);
        map.put("sourceCode", sourceCode);
        map.put("source", sourceName != null ? sourceName : (sourceCode != null ? sourceCode : "Trực tiếp"));

        Timestamp cat = getColTimestamp(rs, "createdAt", "created_at");
        map.put("createdAt", cat != null ? cat.toInstant().toString() : null);

        Timestamp uat = getColTimestamp(rs, "updatedAt", "updated_at");
        map.put("updatedAt", uat != null ? uat.toInstant().toString() : null);

        return map;
    }
}
