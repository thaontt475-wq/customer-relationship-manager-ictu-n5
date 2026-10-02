package com.crm.dao.products;

import com.crm.model.Product;
import com.crm.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Product and Price Book management (CRM-39).
 */
public class ProductDAO {
    private static final Logger LOGGER = Logger.getLogger(ProductDAO.class.getName());

    /**
     * Tables that may reference a product. If any records exist in these tables for a given product_id,
     * the product is considered "in use" and deletion MUST be blocked.
     */
    private static final String[] REFERENCE_TABLES = {
            "quote_items",
            "opportunity_products",
            "deal_products",
            "contract_items",
            "order_items"
    };

    public Product findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT id, code, name, category, unit, list_price, floor_price, cost_price, "
                + "description, is_active, created_at, updated_at "
                + "FROM products WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProduct(rs);
                }
            }
        }
        return null;
    }

    public Product findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public Product findByCode(Connection conn, String code) throws SQLException {
        if (code == null) {
            return null;
        }
        String sql = "SELECT id, code, name, category, unit, list_price, floor_price, cost_price, "
                + "description, is_active, created_at, updated_at "
                + "FROM products WHERE code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProduct(rs);
                }
            }
        }
        return null;
    }

    public Product findByCode(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByCode(conn, code);
        }
    }

    public boolean existsByCode(Connection conn, String code, Long excludeId) throws SQLException {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        StringBuilder sql = new StringBuilder("SELECT 1 FROM products WHERE code = ?");
        if (excludeId != null && excludeId > 0) {
            sql.append(" AND id <> ?");
        }
        sql.append(" LIMIT 1");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, code.trim());
            if (excludeId != null && excludeId > 0) {
                stmt.setLong(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByCode(String code, Long excludeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return existsByCode(conn, code, excludeId);
        }
    }

    public List<Product> search(Connection conn, String keyword, String category, Boolean activeOnly,
                                int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, code, name, category, unit, list_price, floor_price, cost_price, "
                        + "description, is_active, created_at, updated_at FROM products WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (code LIKE ? OR name LIKE ? OR description LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (category != null && !category.trim().isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }

        if (activeOnly != null) {
            sql.append(" AND is_active = ?");
            params.add(activeOnly);
        }

        sql.append(" ORDER BY id DESC");

        if (limit > 0) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(Math.max(0, offset));
        }

        List<Product> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToProduct(rs));
                }
            }
        }
        return list;
    }

    public List<Product> search(String keyword, String category, Boolean activeOnly, int offset, int limit)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return search(conn, keyword, category, activeOnly, offset, limit);
        }
    }

    public long countSearch(Connection conn, String keyword, String category, Boolean activeOnly)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (code LIKE ? OR name LIKE ? OR description LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (category != null && !category.trim().isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }

        if (activeOnly != null) {
            sql.append(" AND is_active = ?");
            params.add(activeOnly);
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
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

    public long countSearch(String keyword, String category, Boolean activeOnly) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return countSearch(conn, keyword, category, activeOnly);
        }
    }

    public long insert(Connection conn, Product product) throws SQLException {
        String sql = "INSERT INTO products (code, name, category, unit, list_price, floor_price, cost_price, "
                + "description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, product.getCode().trim());
            stmt.setString(2, product.getName().trim());
            stmt.setString(3, product.getCategory() != null ? product.getCategory().trim() : null);
            stmt.setString(4, product.getUnit() != null ? product.getUnit().trim() : null);
            stmt.setBigDecimal(5, product.getListPrice() != null ? product.getListPrice() : BigDecimal.ZERO);
            stmt.setBigDecimal(6, product.getFloorPrice() != null ? product.getFloorPrice() : BigDecimal.ZERO);
            stmt.setBigDecimal(7, product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
            stmt.setString(8, product.getDescription() != null ? product.getDescription().trim() : null);
            stmt.setBoolean(9, product.isActive());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        long id = keys.getLong(1);
                        product.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public long insert(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return insert(conn, product);
        }
    }

    public int update(Connection conn, Product product) throws SQLException {
        String sql = "UPDATE products SET code = ?, name = ?, category = ?, unit = ?, list_price = ?, "
                + "floor_price = ?, cost_price = ?, description = ?, is_active = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getCode().trim());
            stmt.setString(2, product.getName().trim());
            stmt.setString(3, product.getCategory() != null ? product.getCategory().trim() : null);
            stmt.setString(4, product.getUnit() != null ? product.getUnit().trim() : null);
            stmt.setBigDecimal(5, product.getListPrice() != null ? product.getListPrice() : BigDecimal.ZERO);
            stmt.setBigDecimal(6, product.getFloorPrice() != null ? product.getFloorPrice() : BigDecimal.ZERO);
            stmt.setBigDecimal(7, product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
            stmt.setString(8, product.getDescription() != null ? product.getDescription().trim() : null);
            stmt.setBoolean(9, product.isActive());
            stmt.setLong(10, product.getId());

            return stmt.executeUpdate();
        }
    }

    public int update(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, product);
        }
    }

    public int delete(Connection conn, long productId) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, productId);
            return stmt.executeUpdate();
        }
    }

    public int delete(long productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, productId);
        }
    }

    public int softDelete(Connection conn, long productId) throws SQLException {
        String sql = "UPDATE products SET is_active = FALSE WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, productId);
            return stmt.executeUpdate();
        }
    }

    public int softDelete(long productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return softDelete(conn, productId);
        }
    }

    /**
     * Checks if a product has been referenced by any business transaction / document
     * (such as Quotes, Deals/Opportunities, Contracts, or Orders).
     *
     * @param conn      Active database connection
     * @param productId Target product ID
     * @return true if the product is referenced in any transaction table, false otherwise
     */
    public boolean isProductInUse(Connection conn, long productId) throws SQLException {
        if (productId <= 0) {
            return false;
        }

        if (!tableExists(conn, "quote_items")) {
            throw new SQLException("Missing quote_items: cannot safely verify product references. Apply CRM-51.");
        }

        for (String table : REFERENCE_TABLES) {
            if (tableExists(conn, table)) {
                String sql = "SELECT 1 FROM " + table + " WHERE product_id = ? LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, productId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            LOGGER.log(Level.INFO, "Product {0} is referenced in table {1}",
                                    new Object[]{productId, table});
                            return true;
                        }
                    }
                } catch (SQLException e) {
                    throw new SQLException("Cannot verify product references in " + table, e);
                }
            }
        }
        return false;
    }

    public boolean isProductInUse(long productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return isProductInUse(conn, productId);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private Product mapRowToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setCode(rs.getString("code"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setUnit(rs.getString("unit"));
        p.setListPrice(rs.getBigDecimal("list_price"));
        p.setFloorPrice(rs.getBigDecimal("floor_price"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setDescription(rs.getString("description"));
        p.setActive(rs.getBoolean("is_active"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }
}