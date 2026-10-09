package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.service.permissions.DataScopeContext;

import java.sql.*;
import java.util.*;

public class CustomerDAO {

    public Connection open() throws SQLException { return DatabaseConfig.getConnection(); }

    public List<Map<String, Object>> search(DataScopeContext scope, String keyword, String status,
                                             int page, int size) throws SQLException {
        try (Connection conn = open()) {
            return search(conn, scope, new CustomerSearchFilter(keyword, status, null, null, null, null), page, size);
        }
    }

    public long count(DataScopeContext scope, String keyword, String status) throws SQLException {
        try (Connection conn = open()) {
            return count(conn, scope, new CustomerSearchFilter(keyword, status, null, null, null, null));
        }
    }

    public List<Map<String, Object>> search(Connection conn, DataScopeContext scope,
                                            CustomerSearchFilter filter, int page, int size) throws SQLException {
        CustomerSearchFilter.pagination(page, size);
        StringBuilder sql = new StringBuilder("""
                SELECT c.id,c.name,c.region,c.tax_code,c.status,c.email,c.phone,c.website,c.address,
                       c.industry_id,c.company_size_id,c.owner_user_id,c.created_at,u.full_name AS owner_name
                FROM customers c LEFT JOIN users u ON u.id=c.owner_user_id
                WHERE c.is_deleted=0
                """);
        List<Object> params = new ArrayList<>();
        appendCriteria(scope, filter, sql, params);
        sql.append(" ORDER BY c.id DESC LIMIT ? OFFSET ?");
        params.add(size); params.add((long) (page - 1) * size);
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            bind(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(mapCustomer(rs));
            }
        }
        return result;
    }

    public long count(Connection conn, DataScopeContext scope, CustomerSearchFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM customers c WHERE c.is_deleted=0");
        List<Object> params = new ArrayList<>();
        appendCriteria(scope, filter, sql, params);
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            bind(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    /** COUNT and list share exactly the same scope and conditions. EXISTS avoids contact fan-out. */
    private void appendCriteria(DataScopeContext scope, CustomerSearchFilter filter,
                                StringBuilder sql, List<Object> params) {
        Objects.requireNonNull(scope, "Customer scope is required").appendOwnerPredicate("c.owner_user_id", sql, params);
        Objects.requireNonNull(filter, "Customer filter is required");
        if (filter.search() != null) {
            sql.append("""
                     AND (LOWER(c.name) LIKE ? ESCAPE '!' OR LOWER(c.email) LIKE ? ESCAPE '!'
                     OR LOWER(c.phone) LIKE ? ESCAPE '!' OR LOWER(c.tax_code) LIKE ? ESCAPE '!'
                     OR REPLACE(LOWER(c.tax_code),'-','') LIKE ? ESCAPE '!'
                     OR EXISTS (SELECT 1 FROM contacts ct WHERE ct.customer_id=c.id AND ct.is_deleted=0
                                AND LOWER(ct.phone) LIKE ? ESCAPE '!'))
                    """);
            String keyword = like(filter.search());
            params.add(keyword); params.add(keyword); params.add(keyword); params.add(keyword);
            params.add(like(filter.search().replace("-", ""))); params.add(keyword);
        }
        equalsFilter(sql, params, "c.status", filter.status());
        equalsFilter(sql, params, "c.industry_id", filter.industryId());
        if (filter.industry() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM master_data md WHERE md.id=c.industry_id AND md.type='industry' AND md.code=?)");
            params.add(filter.industry());
        }
        equalsFilter(sql, params, "c.company_size_id", filter.companySizeId());
        equalsFilter(sql, params, "c.region", filter.region());
        equalsFilter(sql, params, "c.owner_user_id", filter.ownerId());
    }

    private void equalsFilter(StringBuilder sql, List<Object> params, String column, Object value) {
        if (value != null) { sql.append(" AND ").append(column).append("=?"); params.add(value); }
    }

    private String like(String value) {
        return "%" + value.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private void bind(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
    }

    public Map<String, Object> findById(long id) throws SQLException {
        try (Connection conn = open()) { return findById(conn, id, false); }
    }

    public Map<String, Object> findById(Connection conn, long id, boolean lock) throws SQLException {
        String sql = """
                SELECT
                    c.id,
                    c.name,
                    c.region,
                    c.tax_code,
                    c.status,
                    c.email,
                    c.phone,
                    c.website,
                    c.address,
                    c.industry_id,
                    c.company_size_id,
                    c.owner_user_id,
                    c.created_at,
                    u.full_name AS owner_name
                FROM customers c
                LEFT JOIN users u ON u.id = c.owner_user_id
                WHERE c.id = ? AND c.is_deleted = 0
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql + (lock ? " FOR UPDATE" : ""))) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }
        }
        return null;
    }

    public long create(CustomerWriteRequest req, long defaultOwnerId) throws SQLException {
        try (Connection conn = open()) { return create(conn, req, defaultOwnerId); }
    }

    public long create(Connection conn, CustomerWriteRequest req, long ownerId) throws SQLException {
        String sql = """
                INSERT INTO customers (
                    name, tax_code, status, email, phone, website, address,
                    industry_id, company_size_id, owner_user_id, region
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, req.getName());
            stmt.setString(2, req.getTaxCode());
            stmt.setString(3, req.getStatus());
            stmt.setString(4, req.getEmail());
            stmt.setString(5, req.getPhone());
            stmt.setString(6, req.getWebsite());
            stmt.setString(7, req.getAddress());
            stmt.setObject(8, req.getIndustryId(), Types.BIGINT);
            stmt.setObject(9, req.getCompanySizeId(), Types.BIGINT);
            stmt.setLong(10, ownerId);
            stmt.setString(11, req.getRegion());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Không thể tạo bản ghi khách hàng");
    }

    public void update(long id, CustomerWriteRequest req) throws SQLException {
        try (Connection conn = open()) { update(conn, id, req); }
    }

    public void update(Connection conn, long id, CustomerWriteRequest req) throws SQLException {
        String sql = """
                UPDATE customers SET
                    name = ?, tax_code = ?, status = ?, email = ?, phone = ?,
                    website = ?, address = ?, industry_id = ?, company_size_id = ?,
                    owner_user_id = COALESCE(?, owner_user_id),
                    region = CASE WHEN ? THEN ? ELSE region END
                WHERE id = ? AND is_deleted = 0
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, req.getName());
            stmt.setString(2, req.getTaxCode());
            stmt.setString(3, req.getStatus());
            stmt.setString(4, req.getEmail());
            stmt.setString(5, req.getPhone());
            stmt.setString(6, req.getWebsite());
            stmt.setString(7, req.getAddress());
            stmt.setObject(8, req.getIndustryId(), Types.BIGINT);
            stmt.setObject(9, req.getCompanySizeId(), Types.BIGINT);
            stmt.setObject(10, req.getOwnerUserId(), Types.BIGINT);
            stmt.setBoolean(11, req.hasRegion());
            stmt.setString(12, req.getRegion());
            stmt.setLong(13, id);

            stmt.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        try (Connection conn = open()) { softDelete(conn, id); }
    }

    public void softDelete(Connection conn, long id) throws SQLException {
        String sql = "UPDATE customers SET is_deleted = 1 WHERE id = ? AND is_deleted = 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public boolean activeOwner(Connection conn, long id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM users WHERE id = ? AND status = 'ACTIVE' FOR SHARE")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    private Map<String, Object> mapCustomer(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("name", rs.getString("name"));
        map.put("region", rs.getString("region"));
        map.put("companyName", rs.getString("name"));
        map.put("taxCode", rs.getString("tax_code"));
        map.put("status", rs.getString("status"));
        map.put("email", rs.getString("email"));
        map.put("phone", rs.getString("phone"));
        map.put("website", rs.getString("website"));
        map.put("address", rs.getString("address"));
        map.put("industryId", rs.getObject("industry_id"));
        map.put("companySizeId", rs.getObject("company_size_id"));
        map.put("ownerUserId", rs.getLong("owner_user_id"));
        map.put("ownerName", rs.getString("owner_name"));
        map.put("owner", rs.getString("owner_name"));
        map.put("createdAt", rs.getTimestamp("created_at"));
        return map;
    }
}
