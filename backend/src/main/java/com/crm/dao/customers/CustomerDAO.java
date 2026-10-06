package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.service.permissions.DataScopeContext;

import java.sql.*;
import java.util.*;

public class CustomerDAO {

    public List<Map<String, Object>> search(
            DataScopeContext scope,
            String keyword,
            String status,
            int page,
            int size
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    c.id,
                    c.name,
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
                WHERE c.is_deleted = 0
                """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("c.owner_user_id", sql, params);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(c.name) LIKE ? OR LOWER(c.email) LIKE ? OR LOWER(c.phone) LIKE ? OR LOWER(c.tax_code) LIKE ?)");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND c.status = ?");
            params.add(status.trim());
        }

        sql.append(" ORDER BY c.id DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCustomer(rs));
                }
            }
        }
        return list;
    }

    public long count(
            DataScopeContext scope,
            String keyword,
            String status
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM customers c
                WHERE c.is_deleted = 0
                """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("c.owner_user_id", sql, params);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(c.name) LIKE ? OR LOWER(c.email) LIKE ? OR LOWER(c.phone) LIKE ? OR LOWER(c.tax_code) LIKE ?)");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND c.status = ?");
            params.add(status.trim());
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    public Map<String, Object> findById(long id) throws SQLException {
        String sql = """
                SELECT
                    c.id,
                    c.name,
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

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        String sql = """
                INSERT INTO customers (
                    name, tax_code, status, email, phone, website, address,
                    industry_id, company_size_id, owner_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        long ownerId = req.getOwnerUserId() != null ? req.getOwnerUserId() : defaultOwnerId;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        String sql = """
                UPDATE customers SET
                    name = ?, tax_code = ?, status = ?, email = ?, phone = ?,
                    website = ?, address = ?, industry_id = ?, company_size_id = ?,
                    owner_user_id = COALESCE(?, owner_user_id)
                WHERE id = ? AND is_deleted = 0
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            stmt.setLong(11, id);

            stmt.executeUpdate();
        }
    }

    public void softDelete(long id) throws SQLException {
        String sql = "UPDATE customers SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Map<String, Object> mapCustomer(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("name", rs.getString("name"));
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
