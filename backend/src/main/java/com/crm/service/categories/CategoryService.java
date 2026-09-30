package com.crm.service.categories;

import com.crm.dao.categories.CategoryDAO;
import com.crm.model.Category;
import com.crm.model.CategoryType;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing Common Master Data Categories (CRM-44):
 * 1. Industries (Ngành nghề)
 * 2. Company Sizes (Quy mô doanh nghiệp)
 * 3. Lead Sources (Nguồn Lead)
 * 4. Activity Types (Loại hoạt động)
 *
 * Enforces business rules:
 * - Sorts items by display_order ASC for dropdown/select presentation.
 * - Blocks deletion when category is referenced in other records.
 */
public class CategoryService {
    private static final Logger LOGGER = Logger.getLogger(CategoryService.class.getName());

    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO != null ? categoryDAO : new CategoryDAO();
    }

    /**
     * Get categories by type with optional keyword search and active filter.
     * Guaranteed sorted by display_order ASC.
     */
    public List<Category> getCategories(CategoryType type, String keyword, Boolean activeOnly) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String typeCode = type != null ? type.getCode() : null;
            return categoryDAO.findAll(conn, typeCode, keyword, activeOnly);
        }
    }

    /**
     * Retrieve all 4 categories grouped together.
     */
    public MasterDataGroupedResult getAllGrouped(Boolean activeOnly) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Category> industries = categoryDAO.findAll(conn, CategoryType.INDUSTRY.getCode(), null, activeOnly);
            List<Category> companySizes = categoryDAO.findAll(conn, CategoryType.COMPANY_SIZE.getCode(), null, activeOnly);
            List<Category> leadSources = categoryDAO.findAll(conn, CategoryType.LEAD_SOURCE.getCode(), null, activeOnly);
            List<Category> activityTypes = categoryDAO.findAll(conn, CategoryType.ACTIVITY_TYPE.getCode(), null, activeOnly);

            return new MasterDataGroupedResult(industries, companySizes, leadSources, activityTypes);
        }
    }

    /**
     * Get single category by ID.
     */
    public Category getCategoryById(long id) throws SQLException {
        if (id <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return categoryDAO.findById(conn, id);
        }
    }

    /**
     * Create a new category.
     * Enforces required fields, code uniqueness within category type.
     */
    public Category createCategory(Category category) throws SQLException {
        if (category == null) {
            throw new IllegalArgumentException("Dữ liệu danh mục không được để trống.");
        }
        if (category.getType() == null) {
            throw new IllegalArgumentException("Loại danh mục không hợp lệ hoặc không được để trống.");
        }
        validateCategoryFields(category);

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (categoryDAO.existsByTypeCode(conn, category.getTypeCode(), category.getCode(), null)) {
                    conn.rollback();
                    throw new IllegalArgumentException("Mã danh mục '" + category.getCode() + "' đã tồn tại trong nhóm "
                            + category.getType().getDisplayName() + ".");
                }

                long newId = categoryDAO.insert(conn, category);
                if (newId <= 0) {
                    conn.rollback();
                    throw new SQLException("Không thể thêm danh mục mới.");
                }

                Category created = categoryDAO.findById(conn, newId);
                conn.commit();
                return created;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Update an existing category.
     * Enforces required fields, code uniqueness within category type.
     */
    public Category updateCategory(Category category) throws SQLException {
        if (category == null || category.getId() == null || category.getId() <= 0) {
            throw new IllegalArgumentException("ID danh mục không hợp lệ.");
        }
        validateCategoryFields(category);

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Category existing = categoryDAO.findById(conn, category.getId());
                if (existing == null) {
                    conn.rollback();
                    throw new IllegalArgumentException("Không tìm thấy danh mục với ID: " + category.getId());
                }

                // If type was omitted in update payload, preserve existing type
                if (category.getType() == null) {
                    category.setType(existing.getType());
                }

                if (categoryDAO.existsByTypeCode(conn, category.getTypeCode(), category.getCode(), category.getId())) {
                    conn.rollback();
                    throw new IllegalArgumentException("Mã danh mục '" + category.getCode() + "' đã được sử dụng bởi danh mục khác.");
                }

                int updated = categoryDAO.update(conn, category);
                if (updated <= 0) {
                    conn.rollback();
                    throw new SQLException("Không thể cập nhật danh mục.");
                }

                Category result = categoryDAO.findById(conn, category.getId());
                conn.commit();
                return result;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Update display order for a category.
     */
    public boolean updateDisplayOrder(long id, int newOrder) throws SQLException {
        if (id <= 0) {
            return false;
        }
        return categoryDAO.updateDisplayOrder(id, newOrder) > 0;
    }

    /**
     * Delete a category.
     * Business rule:
     * If the category is referenced by any records (Customers, Leads, Activities, etc.),
     * deletion is BLOCKED by throwing CategoryInUseException.
     */
    public boolean deleteCategory(long id) throws SQLException, CategoryInUseException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID danh mục không hợp lệ.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Category existing = categoryDAO.findById(conn, id);
                if (existing == null) {
                    conn.rollback();
                    return false;
                }

                // Check dependencies across all domain tables
                if (categoryDAO.isCategoryInUse(conn, existing.getTypeCode(), id)) {
                    conn.rollback();
                    throw new CategoryInUseException(existing.getTypeCode(), id,
                            "Không thể xóa danh mục '" + existing.getName() +
                                    "' vì đang được sử dụng trong hệ thống (Khách hàng, Lead hoặc Hoạt động).");
                }

                try {
                    int deleted = categoryDAO.delete(conn, id);
                    conn.commit();
                    return deleted > 0;
                } catch (SQLIntegrityConstraintViolationException fkEx) {
                    conn.rollback();
                    throw new CategoryInUseException(existing.getTypeCode(), id,
                            "Không thể xóa danh mục '" + existing.getName() +
                                    "' do có ràng buộc dữ liệu liên quan trong hệ thống.");
                }

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    private void validateCategoryFields(Category category) {
        if (category.getCode() == null || category.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã danh mục không được để trống.");
        }
        if (category.getCode().trim().length() > 50) {
            throw new IllegalArgumentException("Mã danh mục không được vượt quá 50 ký tự.");
        }

        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        if (category.getName().trim().length() > 255) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 255 ký tự.");
        }

        if (category.getDescription() != null && category.getDescription().trim().length() > 500) {
            throw new IllegalArgumentException("Mô tả danh mục không được vượt quá 500 ký tự.");
        }
    }

    public record MasterDataGroupedResult(
            List<Category> industries,
            List<Category> companySizes,
            List<Category> leadSources,
            List<Category> activityTypes
    ) {}
}
