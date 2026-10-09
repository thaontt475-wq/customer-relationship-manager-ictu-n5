package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.util.JsonUtil;
import java.sql.*;
import java.util.*;

public class SavedCustomerFilterDAO {
    public record SavedFilter(long id, String name, CustomerSearchFilter filter, String createdAt) {}
    public Connection open() throws SQLException { return DatabaseConfig.getConnection(); }

    public SavedFilter create(long user, String name, CustomerSearchFilter filter) throws SQLException {
        try (Connection conn = open()) {
            conn.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO saved_customer_filters(user_id,name,criteria) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setLong(1, user); stmt.setString(2, name); stmt.setString(3, JsonUtil.getGson().toJson(filter));
                    if (stmt.executeUpdate() != 1) throw new SQLException("Saved filter insert failed");
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Saved filter ID missing");
                        id = keys.getLong(1);
                    }
                }
                SavedFilter result = find(conn, user, id);
                conn.commit(); return result;
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    public List<SavedFilter> list(long user, int page, int size) throws SQLException {
        CustomerSearchFilter.pagination(page, size);
        try (Connection conn = open(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT id,name,criteria,created_at FROM saved_customer_filters WHERE user_id=? ORDER BY id DESC LIMIT ? OFFSET ?")) {
            stmt.setLong(1, user); stmt.setInt(2, size); stmt.setLong(3, (long) (page - 1) * size);
            List<SavedFilter> result = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) { while (rs.next()) result.add(map(rs)); }
            return result;
        }
    }

    public SavedFilter find(long user, long id) throws SQLException {
        try (Connection conn = open()) { return find(conn, user, id); }
    }

    private SavedFilter find(Connection conn, long user, long id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id,name,criteria,created_at FROM saved_customer_filters WHERE id=? AND user_id=?")) {
            stmt.setLong(1, id); stmt.setLong(2, user);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new NoSuchElementException("Không tìm thấy bộ lọc đã lưu");
                return map(rs);
            }
        }
    }

    public void delete(long user, long id) throws SQLException {
        try (Connection conn = open(); PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM saved_customer_filters WHERE id=? AND user_id=?")) {
            stmt.setLong(1, id); stmt.setLong(2, user);
            if (stmt.executeUpdate() != 1) throw new NoSuchElementException("Không tìm thấy bộ lọc đã lưu");
        }
    }

    private SavedFilter map(ResultSet rs) throws SQLException {
        return new SavedFilter(rs.getLong("id"), rs.getString("name"),
                JsonUtil.getGson().fromJson(rs.getString("criteria"), CustomerSearchFilter.class),
                rs.getTimestamp("created_at").toLocalDateTime().toString());
    }
}
