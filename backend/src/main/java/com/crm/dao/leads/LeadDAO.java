package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;

import java.sql.*;
import java.util.*;

public class LeadDAO {

    public Connection open() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public List<Map<String, Object>> sources(Connection c, boolean lock) throws SQLException {
        String query = "SELECT id,code,name FROM master_data WHERE type='lead-source' AND active=TRUE ORDER BY id" + (lock ? " FOR SHARE" : "");
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(query); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", r.getLong("id"));
                map.put("code", r.getString("code"));
                map.put("name", r.getString("name"));
                list.add(map);
            }
            return list;
        }
    }

    public long insert(Connection c, LeadWriteRequest lead, long user, String batch) throws SQLException {
        StringBuilder cols = new StringBuilder("INSERT INTO leads (full_name,phone,source_id,owner_user_id,created_by");
        StringBuilder vals = new StringBuilder("VALUES (?,?,?,?,?");
        List<Object> args = new ArrayList<>(Arrays.asList(lead.fullName(), lead.phone(), lead.sourceId(), user, user));

        if (lead.companyName() != null) { cols.append(",company_name"); vals.append(",?"); args.add(lead.companyName()); }
        if (lead.email() != null) { cols.append(",email"); vals.append(",?"); args.add(lead.email()); }
        if (lead.notes() != null) { cols.append(",notes"); vals.append(",?"); args.add(lead.notes()); }
        if (batch != null) { cols.append(",import_batch_hash"); vals.append(",?"); args.add(batch); }
        cols.append(",status) ").append(vals).append(",'NEW')");

        try (PreparedStatement p = c.prepareStatement(cols.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < args.size(); i++) p.setObject(i + 1, args.get(i));
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                if (r.next()) {
                    long genId = r.getLong(1);
                    String code = String.format("LEAD-2026-%03d", genId);
                    updateCode(c, genId, code);
                    return genId;
                }
                throw new SQLException("Cannot generate lead id");
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
                result.put("isLocked", "CONVERTED".equalsIgnoreCase(String.valueOf(result.get("status"))));
                return result;
            }
        }
    }

    public void update(Connection c, long id, LeadWriteRequest lead) throws SQLException {
        String sql = "UPDATE leads SET full_name=?,company_name=?,email=?,phone=?,source_id=?,notes=? WHERE id=?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, lead.fullName());
            p.setString(2, lead.companyName());
            p.setString(3, lead.email());
            p.setString(4, lead.phone());
            p.setObject(5, lead.sourceId());
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

    public long insert(Lead lead) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return insert(conn, lead);
        } catch (SQLException e) {
            System.err.println("Error inserting lead: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public long insert(Connection conn, Lead lead) throws SQLException {
        boolean hasOwner = hasColumn(conn, "owner_user_id");
        boolean hasAssignedToUser = hasColumn(conn, "assigned_to_user_id");
        boolean hasCreatedBy = hasColumn(conn, "created_by");

        StringBuilder cols = new StringBuilder("INSERT INTO leads (code, full_name, title, company_name, industry, tax_code, email, phone, " +
                "source, source_name, rating, tier, score, status, assigned_to, estimated_value, " +
                "sla_deadline, sla_minutes_total, last_contacted_at, notes, form_id, ip_address, " +
                "region, region_code, unassigned_reason");
        StringBuilder vals = new StringBuilder("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");

        if (hasOwner) {
            cols.append(", owner_user_id");
            vals.append(", ?");
        }
        if (hasAssignedToUser) {
            cols.append(", assigned_to_user_id");
            vals.append(", ?");
        }
        if (hasCreatedBy) {
            cols.append(", created_by");
            vals.append(", ?");
        }
        cols.append(", is_deleted) ").append(vals).append(", 0)");

        try (PreparedStatement stmt = conn.prepareStatement(cols.toString(), Statement.RETURN_GENERATED_KEYS)) {
            bindLeadParams(stmt, lead);
            int idx = 26;
            if (hasOwner) {
                Long owner = lead.getOwnerUserId() != null ? lead.getOwnerUserId() : lead.getAssignedTo();
                stmt.setObject(idx++, owner, Types.BIGINT);
            }
            if (hasAssignedToUser) {
                Long rep = lead.getAssignedToUserId() != null ? lead.getAssignedToUserId() : lead.getAssignedTo();
                stmt.setObject(idx++, rep, Types.BIGINT);
            }
            if (hasCreatedBy) {
                stmt.setObject(idx++, lead.getCreatedBy(), Types.BIGINT);
            }

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    lead.setId(id);
                    if (lead.getCode() == null || lead.getCode().isBlank()) {
                        String code = String.format("LEAD-2026-%03d", id);
                        lead.setCode(code);
                        updateCode(conn, id, code);
                    }
                    return id;
                }
            }
        }
        return -1;
    }

    public boolean update(Lead lead) {
        String sql = """
            UPDATE leads SET 
                code = ?, full_name = ?, title = ?, company_name = ?, industry = ?, tax_code = ?,
                email = ?, phone = ?, source = ?, source_name = ?, rating = ?, tier = ?, score = ?,
                status = ?, assigned_to = ?, estimated_value = ?, sla_deadline = ?, sla_minutes_total = ?,
                last_contacted_at = ?, notes = ?, form_id = ?, ip_address = ?, region = ?, region_code = ?, unassigned_reason = ?,
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
        String sql = """
            SELECT l.*, u.full_name AS owner_name, u.email AS owner_email
            FROM leads l
            LEFT JOIN users u ON (u.id = l.assigned_to_user_id OR u.id = l.owner_user_id OR (l.assigned_to REGEXP '^[0-9]+$' AND u.id = CAST(l.assigned_to AS UNSIGNED)))
            WHERE l.id = ? AND l.is_deleted = 0
            """ + (forUpdate ? " FOR UPDATE" : "");
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

    // --- S4-09: Filter and Search Methods ---

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
                    list.add(mapRow(rs));
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
            scope.appendCoalescedOwnerPredicate("l.assigned_to", "l.owner_user_id", sql, params);
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
        // 1. Data Scope
        if (scope != null) {
            scope.appendCoalescedOwnerPredicate("l.assigned_to", "l.owner_user_id", sql, params);
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

        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
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
        try { lead.setId(rs.getLong("id")); } catch (SQLException ignored) {}
        try { lead.setCode(rs.getString("code")); } catch (SQLException ignored) {}
        try { lead.setFullName(rs.getString("full_name")); } catch (SQLException ignored) {}
        try { lead.setTitle(rs.getString("title")); } catch (SQLException ignored) {}
        try { lead.setCompanyName(rs.getString("company_name")); } catch (SQLException ignored) {}
        try { lead.setIndustry(rs.getString("industry")); } catch (SQLException ignored) {}
        try { lead.setTaxCode(rs.getString("tax_code")); } catch (SQLException ignored) {}
        try { lead.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        try { lead.setPhone(rs.getString("phone")); } catch (SQLException ignored) {}
        try { lead.setSourceId(rs.getObject("source_id", Long.class)); } catch (SQLException ignored) {}
        try { lead.setSource(rs.getString("source")); } catch (SQLException ignored) {}
        try { lead.setSourceName(rs.getString("source_name")); } catch (SQLException ignored) {}
        try { lead.setRegion(rs.getString("region")); } catch (SQLException ignored) {}
        try { lead.setRegionCode(rs.getString("region_code")); } catch (SQLException ignored) {}
        try { lead.setEmployeeCount(rs.getObject("employee_count", Integer.class)); } catch (SQLException ignored) {}
        try { lead.setScore(rs.getObject("score", Integer.class)); } catch (SQLException ignored) {}
        try { lead.setRating(rs.getString("rating")); } catch (SQLException ignored) {}
        try { lead.setTier(rs.getString("tier")); } catch (SQLException ignored) {}
        try { lead.setStatus(rs.getString("status")); } catch (SQLException ignored) {}
        try { lead.setNotes(rs.getString("notes")); } catch (SQLException ignored) {}
        try { lead.setFormId(rs.getString("form_id")); } catch (SQLException ignored) {}
        try { lead.setIpAddress(rs.getString("ip_address")); } catch (SQLException ignored) {}
        try { lead.setOwnerUserId(rs.getObject("owner_user_id", Long.class)); } catch (SQLException ignored) {}

        try {
            long assigned = rs.getLong("assigned_to");
            if (!rs.wasNull()) {
                lead.setAssignedTo(assigned);
            } else {
                lead.setAssignedTo((Long) null);
            }
        } catch (Exception e) {
            String val = rs.getString("assigned_to");
            lead.setAssignedTo(val);
        }

        try {
            Long assignedUserId = rs.getObject("assigned_to_user_id", Long.class);
            if (assignedUserId != null) {
                lead.setAssignedToUserId(assignedUserId);
            } else {
                if (lead.getAssignedTo() == null) {
                    lead.setAssignedToUserId(null);
                }
            }
        } catch (SQLException ignored) {}

        try { lead.setOwner(rs.getString("owner_name")); } catch (SQLException ignored) {}
        try { lead.setEstimatedValue(rs.getDouble("estimated_value")); } catch (SQLException ignored) {}
        try { lead.setAssignedAt(rs.getTimestamp("assigned_at")); } catch (SQLException ignored) {}
        try { lead.setSlaTotalMinutes(rs.getObject("sla_total_minutes", Integer.class)); } catch (SQLException ignored) {}
        try { lead.setSlaMinutesTotal(rs.getInt("sla_minutes_total")); } catch (SQLException ignored) {}
        try { lead.setSlaDeadline(rs.getTimestamp("sla_deadline")); } catch (SQLException ignored) {}
        try { lead.setLastContactedAt(rs.getTimestamp("last_contacted_at")); } catch (SQLException ignored) {}
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

    public Lead mapResultSetToLead(ResultSet rs) throws SQLException {
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
