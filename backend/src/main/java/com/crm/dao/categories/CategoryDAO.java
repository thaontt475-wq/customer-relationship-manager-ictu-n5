package com.crm.dao.categories;

import com.crm.model.Category;
import com.crm.model.CategoryType;
import com.crm.util.DBConnection;

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
 * Data Access Object for Common Master Data Categories (CRM-44):
 * - Industries (Ngành nghề)
 * - Company Sizes (Quy mô doanh nghiệp)
 * - Lead Sources (Nguồn Lead)
 * - Activity Types (Loại hoạt động)
 */
public class CategoryDAO {
    private static final Logger LOGGER = Logger.getLogger(CategoryDAO.class.getName());

    public Category findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT id, type, code, name, description, display_order, is_active, created_at, updated_at "
                + "FROM categories WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCategory(rs);
                }
            }
        }
        return null;
    }

    public Category findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public Category findByTypeCode(Connection conn, String type, String code) throws SQLException {
        if (type == null || code == null) {
            return null;
        }
        String sql = "SELECT id, type, code, name, description, display_order, is_active, created_at, updated_at "
                + "FROM categories WHERE type = ? AND code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.trim().toUpperCase());
            stmt.setString(2, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCategory(rs);
                }
            }
        }
        return null;
    }

    public Category findByTypeCode(String type, String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByTypeCode(conn, type, code);
        }
    }

    public boolean existsByTypeCode(Connection conn, String type, String code, Long excludeId) throws SQLException {
        if (type == null || code == null) {
            return false;
        }
        StringBuilder sql = new StringBuilder("SELECT 1 FROM categories WHERE type = ? AND code = ?");
        if (excludeId != null && excludeId > 0) {
            sql.append(" AND id <> ?");
        }
        sql.append(" LIMIT 1");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, type.trim().toUpperCase());
            stmt.setString(2, code.trim());
            if (excludeId != null && excludeId > 0) {
                stmt.setLong(3, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByTypeCode(String type, String code, Long excludeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return existsByTypeCode(conn, type, code, excludeId);
        }
    }

    /**
     * Search and retrieve categories, sorted by display_order ASC, name ASC, id ASC.
     */
    public List<Category> findAll(Connection conn, String type, String keyword, Boolean activeOnly)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT id, type, code, name, description, display_order, is_active, created_at, updated_at "
                        + "FROM categories WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (type != null && !type.trim().isEmpty()) {
            sql.append(" AND type = ?");
            params.add(type.trim().toUpperCase());
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (code LIKE ? OR name LIKE ? OR description LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (activeOnly != null) {
            sql.append(" AND is_active = ?");
            params.add(activeOnly);
        }

        // CRITICAL BUSINESS RULE 1: Sort by display_order ASC
        sql.append(" ORDER BY display_order ASC, name ASC, id ASC");

        List<Category> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToCategory(rs));
                }
            }
        }
        return list;
    }

    public List<Category> findAll(String type, String keyword, Boolean activeOnly) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn, type, keyword, activeOnly);
        }
    }

    public long insert(Connection conn, Category category) throws SQLException {
        String sql = "INSERT INTO categories (type, code, name, description, display_order, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getTypeCode());
            stmt.setString(2, category.getCode().trim());
            stmt.setString(3, category.getName().trim());
            stmt.setString(4, category.getDescription() != null ? category.getDescription().trim() : null);
            stmt.setInt(5, category.getDisplayOrder());
            stmt.setBoolean(6, category.isActive());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        long id = keys.getLong(1);
                        category.setId(id);
                        return id;
                    }
                }
            }
        }
        return 0;
    }

    public long insert(Category category) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return insert(conn, category);
        }
    }

    public int update(Connection conn, Category category) throws SQLException {
        String sql = "UPDATE categories SET code = ?, name = ?, description = ?, display_order = ?, is_active = ? "
                + "WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getCode().trim());
            stmt.setString(2, category.getName().trim());
            stmt.setString(3, category.getDescription() != null ? category.getDescription().trim() : null);
            stmt.setInt(4, category.getDisplayOrder());
            stmt.setBoolean(5, category.isActive());
            stmt.setLong(6, category.getId());

            return stmt.executeUpdate();
        }
    }

    public int update(Category category) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return update(conn, category);
        }
    }

    public int updateDisplayOrder(Connection conn, long id, int displayOrder) throws SQLException {
        String sql = "UPDATE categories SET display_order = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, displayOrder);
            stmt.setLong(2, id);
            return stmt.executeUpdate();
        }
    }

    public int updateDisplayOrder(long id, int displayOrder) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return updateDisplayOrder(conn, id, displayOrder);
        }
    }

    public int delete(Connection conn, long id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate();
        }
    }

    public int delete(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return delete(conn, id);
        }
    }

    /**
     * Checks if a category is in use by business transaction tables:
     * - INDUSTRY: customers.industry_id, leads.industry_id
     * - COMPANY_SIZE: customers.company_size_id, leads.company_size_id
     * - LEAD_SOURCE: leads.lead_source_id, customers.lead_source_id
     * - ACTIVITY_TYPE: activities.activity_type_id
     *
     * @param conn        Active database connection
     * @param type        Category type (e.g. INDUSTRY, COMPANY_SIZE, LEAD_SOURCE, ACTIVITY_TYPE)
     * @param categoryId  Target category ID
     * @return true if referenced in any table, false otherwise
     */
    public boolean isCategoryInUse(Connection conn, String type, long categoryId) throws SQLException {
        if (categoryId <= 0) {
            return false;
        }

        String cleanType = type != null ? type.trim().toUpperCase() : "";

        switch (cleanType) {
            case "INDUSTRY":
                if (checkReference(conn, "customers", "industry_id", categoryId)) return true;
                if (checkReference(conn, "leads", "industry_id", categoryId)) return true;
                break;

            case "COMPANY_SIZE":
                if (checkReference(conn, "customers", "company_size_id", categoryId)) return true;
                if (checkReference(conn, "leads", "company_size_id", categoryId)) return true;
                break;

            case "LEAD_SOURCE":
                if (checkReference(conn, "leads", "lead_source_id", categoryId)) return true;
                if (checkReference(conn, "customers", "lead_source_id", categoryId)) return true;
                if (checkReference(conn, "opportunities", "lead_source_id", categoryId)) return true;
                break;

            case "ACTIVITY_TYPE":
                if (checkReference(conn, "activities", "activity_type_id", categoryId)) return true;
                break;

            default:
                // Check all known reference columns generically
                if (checkReference(conn, "customers", "industry_id", categoryId)) return true;
                if (checkReference(conn, "customers", "company_size_id", categoryId)) return true;
                if (checkReference(conn, "leads", "lead_source_id", categoryId)) return true;
                if (checkReference(conn, "leads", "industry_id", categoryId)) return true;
                if (checkReference(conn, "leads", "company_size_id", categoryId)) return true;
                if (checkReference(conn, "activities", "activity_type_id", categoryId)) return true;
                break;
        }

        return false;
    }

    public boolean isCategoryInUse(String type, long categoryId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return isCategoryInUse(conn, type, categoryId);
        }
    }

    private boolean checkReference(Connection conn, String tableName, String columnName, long categoryId) {
        if (!tableHasColumn(conn, tableName, columnName)) {
            return false;
        }
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + columnName + " = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LOGGER.log(Level.INFO, "Category {0} is referenced by {1}.{2}",
                            new Object[]{categoryId, tableName, columnName});
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Failed to check reference in " + tableName + "." + columnName, e);
        }
        return false;
    }

    private boolean tableHasColumn(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, columnName)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Category mapRowToCategory(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setTypeCode(rs.getString("type"));
        c.setCode(rs.getString("code"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setDisplayOrder(rs.getInt("display_order"));
        c.setActive(rs.getBoolean("is_active"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }
}
