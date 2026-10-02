package com.crm.dao.products;

import com.crm.model.QuoteItem;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

public class QuotePricingDAO {
    public List<QuoteItem> items(Connection c, long quote) throws SQLException {
        List<QuoteItem> items = new ArrayList<>();
        String sql = "SELECT id, product_id, product_code, product_name, unit, quantity, unit_price, list_price_snapshot, floor_price_snapshot "
                   + "FROM quote_items WHERE quote_id = ? ORDER BY id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, quote);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    items.add(new QuoteItem(
                            r.getLong(1),
                            r.getLong(2),
                            r.getString(3),
                            r.getString(4),
                            r.getString(5),
                            r.getBigDecimal(6),
                            r.getBigDecimal(7),
                            r.getBigDecimal(8),
                            r.getBigDecimal(9)
                    ));
                }
            }
        }
        return items;
    }

    public long add(Connection c, long quote, long product, BigDecimal quantity, BigDecimal unitPrice) throws SQLException {
        // Lock product while taking a snapshot; never select or expose cost_price.
        String sql = "SELECT code, name, unit, list_price, floor_price, is_active FROM products WHERE id = ? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, product);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next() || !r.getBoolean(6)) {
                    throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
                }
                String insertSql = "INSERT INTO quote_items(quote_id, product_id, product_code, product_name, unit, quantity, unit_price, list_price_snapshot, floor_price_snapshot) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insert = c.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, quote);
                    insert.setLong(2, product);
                    insert.setString(3, r.getString(1));
                    insert.setString(4, r.getString(2));
                    insert.setString(5, r.getString(3));
                    insert.setBigDecimal(6, quantity);
                    insert.setBigDecimal(7, unitPrice);
                    insert.setBigDecimal(8, r.getBigDecimal(4));
                    insert.setBigDecimal(9, r.getBigDecimal(5));
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        keys.next();
                        return keys.getLong(1);
                    }
                }
            }
        }
    }

    public String status(Connection c, long quote) throws SQLException {
        String sql = "SELECT status FROM quote_pricing_approvals WHERE quote_id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, quote);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getString(1) : "DRAFT";
            }
        }
    }

    public void status(Connection c, long quote, String status, Long actor) throws SQLException {
        String sql = "INSERT INTO quote_pricing_approvals(quote_id, status, approved_by, approved_at) "
                   + "VALUES (?, ?, ?, IF(? IS NULL, NULL, CURRENT_TIMESTAMP)) "
                   + "ON DUPLICATE KEY UPDATE status = VALUES(status), approved_by = VALUES(approved_by), approved_at = VALUES(approved_at)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, quote);
            s.setString(2, status);
            s.setObject(3, actor);
            s.setObject(4, actor);
            s.executeUpdate();
        }
    }
}
