package com.crm.dao.quotes;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class QuoteDAO {

    public List<Map<String, Object>> search(
            DataScopeContext scope,
            String keyword,
            String status
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    q.id,
                    q.quote_number,
                    q.title,
                    q.customer_id,
                    q.opportunity_id,
                    q.discount_percent,
                    q.subtotal,
                    q.total_amount,
                    q.status,
                    q.requires_approval,
                    q.owner_user_id,
                    q.created_at,
                    c.name AS customer_name,
                    u.full_name AS owner_name
                FROM quotes q
                LEFT JOIN customers c ON c.id = q.customer_id
                LEFT JOIN users u ON u.id = q.owner_user_id
                WHERE q.is_deleted = 0
                """);

        List<Object> params = new ArrayList<>();
        if (scope != null) {
            scope.appendOwnerPredicate("q.owner_user_id", sql, params);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND q.status = ?");
            params.add(status.trim());
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(q.quote_number) LIKE ? OR LOWER(q.title) LIKE ? OR LOWER(c.name) LIKE ?)");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        sql.append(" ORDER BY q.id DESC");

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQuote(rs));
                }
            }
        }
        return list;
    }

    public Map<String, Object> findById(long id) throws SQLException {
        String sql = """
                SELECT
                    q.id,
                    q.quote_number,
                    q.title,
                    q.customer_id,
                    q.opportunity_id,
                    q.discount_percent,
                    q.subtotal,
                    q.total_amount,
                    q.status,
                    q.requires_approval,
                    q.owner_user_id,
                    q.created_at,
                    c.name AS customer_name,
                    u.full_name AS owner_name
                FROM quotes q
                LEFT JOIN customers c ON c.id = q.customer_id
                LEFT JOIN users u ON u.id = q.owner_user_id
                WHERE q.id = ? AND q.is_deleted = 0
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> quote = mapQuote(rs);
                    quote.put("items", findItemsByQuoteId(conn, id));
                    return quote;
                }
            }
        }
        return null;
    }

    public List<Map<String, Object>> findItemsByQuoteId(Connection conn, long quoteId) throws SQLException {
        String sql = """
                SELECT
                    qi.id,
                    qi.quote_id,
                    qi.product_id,
                    qi.quantity,
                    qi.unit_price,
                    qi.discount_percent,
                    qi.amount,
                    p.code AS product_code,
                    p.name AS product_name,
                    p.unit AS product_unit,
                    p.floor_price,
                    p.list_price
                FROM quote_items qi
                JOIN products p ON p.id = qi.product_id
                WHERE qi.quote_id = ?
                """;

        List<Map<String, Object>> items = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, quoteId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("productId", rs.getLong("product_id"));
                    item.put("productCode", rs.getString("product_code"));
                    item.put("productName", rs.getString("product_name"));
                    item.put("unit", rs.getString("product_unit"));
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("unitPrice", rs.getBigDecimal("unit_price"));
                    item.put("discountPercent", rs.getBigDecimal("discount_percent"));
                    item.put("amount", rs.getBigDecimal("amount"));
                    item.put("floorPrice", rs.getBigDecimal("floor_price"));
                    item.put("listPrice", rs.getBigDecimal("list_price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public long create(
            String quoteNumber,
            String title,
            Long customerId,
            Long opportunityId,
            BigDecimal discountPercent,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            String status,
            boolean requiresApproval,
            long ownerId,
            List<Map<String, Object>> items
    ) throws SQLException {
        String sql = """
                INSERT INTO quotes (
                    quote_number, title, customer_id, opportunity_id, discount_percent,
                    subtotal, total_amount, status, requires_approval, owner_user_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            long quoteId;
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, quoteNumber);
                    stmt.setString(2, title);
                    stmt.setObject(3, customerId, Types.BIGINT);
                    stmt.setObject(4, opportunityId, Types.BIGINT);
                    stmt.setBigDecimal(5, discountPercent);
                    stmt.setBigDecimal(6, subtotal);
                    stmt.setBigDecimal(7, totalAmount);
                    stmt.setString(8, status);
                    stmt.setBoolean(9, requiresApproval);
                    stmt.setLong(10, ownerId);
                    stmt.executeUpdate();
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            quoteId = rs.getLong(1);
                        } else {
                            throw new SQLException("Không lấy được id báo giá");
                        }
                    }
                }

                if (items != null && !items.isEmpty()) {
                    String itemSql = """
                            INSERT INTO quote_items (quote_id, product_id, quantity, unit_price, discount_percent, amount)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                        for (Map<String, Object> it : items) {
                            stmt.setLong(1, quoteId);
                            stmt.setLong(2, ((Number) it.get("productId")).longValue());
                            stmt.setInt(3, it.get("quantity") != null ? ((Number) it.get("quantity")).intValue() : 1);
                            BigDecimal up = it.get("unitPrice") != null ? new BigDecimal(String.valueOf(it.get("unitPrice"))) : BigDecimal.ZERO;
                            BigDecimal dp = it.get("discountPercent") != null ? new BigDecimal(String.valueOf(it.get("discountPercent"))) : BigDecimal.ZERO;
                            BigDecimal amt = it.get("amount") != null ? new BigDecimal(String.valueOf(it.get("amount"))) : up;
                            stmt.setBigDecimal(4, up);
                            stmt.setBigDecimal(5, dp);
                            stmt.setBigDecimal(6, amt);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }
                conn.commit();
                return quoteId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void softDelete(long id) throws SQLException {
        String sql = "UPDATE quotes SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public void updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE quotes SET status = ?, requires_approval = CASE WHEN ? = 'APPROVED' THEN 0 ELSE requires_approval END WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, status);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    private Map<String, Object> mapQuote(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("quoteNumber", rs.getString("quote_number"));
        map.put("title", rs.getString("title"));
        map.put("customerId", rs.getObject("customer_id"));
        map.put("customerName", rs.getString("customer_name"));
        map.put("opportunityId", rs.getObject("opportunity_id"));
        map.put("discountPercent", rs.getBigDecimal("discount_percent"));
        map.put("subtotal", rs.getBigDecimal("subtotal"));
        map.put("totalAmount", rs.getBigDecimal("total_amount"));
        map.put("status", rs.getString("status"));
        map.put("requiresApproval", rs.getBoolean("requires_approval"));
        map.put("ownerUserId", rs.getLong("owner_user_id"));
        map.put("ownerName", rs.getString("owner_name"));
        map.put("createdAt", rs.getTimestamp("created_at"));
        return map;
    }
}
