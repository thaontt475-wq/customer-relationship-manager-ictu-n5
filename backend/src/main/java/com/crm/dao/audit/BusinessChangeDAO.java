package com.crm.dao.audit;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import com.crm.service.scope.ScopeRecord;

public class BusinessChangeDAO {
    public record QuoteDiscount(ScopeRecord record, BigDecimal discount) { }
    public record Target(long id, long userId, String name, LocalDate month, BigDecimal amount) { }

    public QuoteDiscount quote(Connection c, long id) throws SQLException {
        String sql = "SELECT q.id, q.quote_number, q.owner_user_id, u.team_id, q.discount_percent "
                   + "FROM quotes q JOIN users u ON u.id = q.owner_user_id WHERE q.id = ? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                Long team = r.getObject("team_id", Long.class);
                return new QuoteDiscount(
                        new ScopeRecord(id, r.getString("quote_number"), r.getLong("owner_user_id"), team),
                        r.getBigDecimal("discount_percent")
                );
            }
        }
    }

    public void discount(Connection c, long id, BigDecimal amount) throws SQLException {
        String sql = "UPDATE quotes SET discount_percent = ? WHERE id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setBigDecimal(1, amount);
            s.setLong(2, id);
            s.executeUpdate();
        }
    }

    public void lockUser(Connection c, long id) throws SQLException {
        String sql = "SELECT id FROM users WHERE id = ? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Người dùng không tồn tại.");
            }
        }
    }

    public Target target(Connection c, long user, LocalDate month) throws SQLException {
        String sql = "SELECT id, amount FROM sales_targets WHERE user_id = ? AND period_month = ? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, user);
            s.setDate(2, java.sql.Date.valueOf(month));
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? new Target(r.getLong(1), user, "", month, r.getBigDecimal(2)) : null;
            }
        }
    }

    public long saveTarget(Connection c, Target before, long user, LocalDate month, BigDecimal amount) throws SQLException {
        if (before != null) {
            String sql = "UPDATE sales_targets SET amount = ? WHERE id = ?";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setBigDecimal(1, amount);
                s.setLong(2, before.id());
                s.executeUpdate();
            }
            return before.id();
        }
        String sql = "INSERT INTO sales_targets(user_id, period_month, amount) VALUES (?, ?, ?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, user);
            s.setDate(2, java.sql.Date.valueOf(month));
            s.setBigDecimal(3, amount);
            s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    public List<Target> targets(Connection c) throws SQLException {
        List<Target> items = new ArrayList<>();
        String sql = "SELECT t.id, t.user_id, u.full_name, t.period_month, t.amount "
                   + "FROM sales_targets t JOIN users u ON u.id = t.user_id ORDER BY t.period_month DESC, t.id DESC";
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                items.add(new Target(
                        r.getLong(1),
                        r.getLong(2),
                        r.getString(3),
                        r.getDate(4).toLocalDate(),
                        r.getBigDecimal(5)
                ));
            }
        }
        return items;
    }
}
