package com.crm.service.products;

import com.crm.dao.products.ProductDAO;
import com.crm.model.Product;
import com.crm.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service implementing business logic for CRM-39 (Sản phẩm & bảng giá):
 * 1. Phân quyền hiển thị Giá Vốn: Trường cost_price CHỈ CÓ "Giám đốc" (Director/Admin)
 *    mới được phép xem và chỉnh sửa. Nếu người dùng không phải Giám đốc, ẩn/bỏ qua thông tin
 *    giá vốn trong response (set null) và không cho phép cập nhật.
 * 2. Validation Giá Sàn: list_price >= floor_price.
 * 3. Chặn xóa sản phẩm đã sử dụng: Nếu sản phẩm đã được tham chiếu trong chứng từ/giao dịch
 *    (Báo giá, Hợp đồng, Cơ hội, Đơn hàng), ném ProductInUseException để trả về HTTP 409/400.
 */
public class ProductService {
    private static final Logger LOGGER = Logger.getLogger(ProductService.class.getName());

    private static final Set<String> DIRECTOR_ADMIN_ROLES = Set.of(
            "admin",
            "director",
            "giám đốc",
            "giam doc",
            "quản trị viên",
            "quan tri vien"
    );

    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO != null ? productDAO : new ProductDAO();
    }

    /**
     * Checks if the given roles include Director or Admin privileges.
     */
    public boolean isDirectorOrAdmin(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> r.trim().toLowerCase(Locale.ROOT))
                .anyMatch(DIRECTOR_ADMIN_ROLES::contains);
    }

    /**
     * Get product by ID with cost_price masking based on user role.
     */
    public Product getProductById(long id, Collection<String> userRoles) throws SQLException {
        if (id <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            Product product = productDAO.findById(conn, id);
            if (product != null) {
                applyCostPriceAuthorization(product, userRoles);
            }
            return product;
        }
    }

    /**
     * Search products with filtering, pagination and cost_price masking.
     */
    public ProductSearchResult searchProducts(String keyword, String category, Boolean activeOnly,
                                              int page, int size, Collection<String> userRoles) throws SQLException {
        int currentPage = Math.max(1, page);
        int pageSize = size > 0 ? Math.min(100, size) : 10;
        int offset = (currentPage - 1) * pageSize;

        try (Connection conn = DBConnection.getConnection()) {
            long total = productDAO.countSearch(conn, keyword, category, activeOnly);
            List<Product> items = productDAO.search(conn, keyword, category, activeOnly, offset, pageSize);

            boolean isDirector = isDirectorOrAdmin(userRoles);
            for (Product p : items) {
                if (!isDirector) {
                    p.setCostPrice(null);
                }
            }

            int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / pageSize);
            return new ProductSearchResult(items, total, currentPage, pageSize, totalPages);
        }
    }

    /**
     * Create a new product.
     * Enforces:
     * - Required fields: code, name
     * - Unique product code
     * - Validation: list_price >= floor_price >= 0
     * - Cost price authorization: Only Director/Admin can set cost_price; otherwise forced to 0.
     */
    public Product createProduct(Product product, Collection<String> userRoles) throws SQLException {
        if (product == null) {
            throw new IllegalArgumentException("Thông tin sản phẩm không được rỗng.");
        }

        validateBasicProduct(product);

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // Check code uniqueness
                if (productDAO.existsByCode(conn, product.getCode(), null)) {
                    conn.rollback();
                    throw new IllegalArgumentException("Mã sản phẩm '" + product.getCode() + "' đã tồn tại trong hệ thống.");
                }

                // Cost price authorization
                boolean isDirector = isDirectorOrAdmin(userRoles);
                if (!isDirector) {
                    // Non-directors cannot set cost price
                    product.setCostPrice(BigDecimal.ZERO);
                } else {
                    if (product.getCostPrice() == null || product.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
                        product.setCostPrice(BigDecimal.ZERO);
                    }
                }

                long newId = productDAO.insert(conn, product);
                if (newId <= 0) {
                    conn.rollback();
                    throw new SQLException("Không thể tạo sản phẩm mới.");
                }

                Product created = productDAO.findById(conn, newId);
                conn.commit();

                if (created != null && !isDirector) {
                    created.setCostPrice(null);
                }
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
     * Update an existing product.
     * Enforces:
     * - Product must exist
     * - Required fields: code, name
     * - Unique product code (excluding current ID)
     * - Validation: list_price >= floor_price >= 0
     * - Cost price authorization: Only Director/Admin can modify cost_price.
     *   If non-director, cost_price is strictly preserved from the database.
     */
    public Product updateProduct(Product product, Collection<String> userRoles) throws SQLException {
        if (product == null || product.getId() == null || product.getId() <= 0) {
            throw new IllegalArgumentException("ID sản phẩm không hợp lệ.");
        }

        validateBasicProduct(product);

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Product existing = productDAO.findById(conn, product.getId());
                if (existing == null) {
                    conn.rollback();
                    throw new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + product.getId());
                }

                // Check code uniqueness
                if (productDAO.existsByCode(conn, product.getCode(), product.getId())) {
                    conn.rollback();
                    throw new IllegalArgumentException("Mã sản phẩm '" + product.getCode() + "' đã được sử dụng bởi sản phẩm khác.");
                }

                // Cost price authorization
                boolean isDirector = isDirectorOrAdmin(userRoles);
                if (!isDirector) {
                    // Strictly preserve existing cost price from database
                    product.setCostPrice(existing.getCostPrice());
                } else {
                    // Director is modifying; ensure not null and >= 0
                    if (product.getCostPrice() == null) {
                        product.setCostPrice(existing.getCostPrice());
                    } else if (product.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
                        conn.rollback();
                        throw new IllegalArgumentException("Giá vốn không được nhỏ hơn 0.");
                    }
                }

                int updated = productDAO.update(conn, product);
                if (updated <= 0) {
                    conn.rollback();
                    throw new SQLException("Không thể cập nhật sản phẩm.");
                }

                Product result = productDAO.findById(conn, product.getId());
                conn.commit();

                if (result != null && !isDirector) {
                    result.setCostPrice(null);
                }
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
     * Delete a product.
     * Business rule:
     * If the product has been referenced by any transactions/documents (Quotes, Deals/Opportunities,
     * Contracts, Orders), deletion is strictly BLOCKED by throwing ProductInUseException.
     * If not referenced, deletion proceeds normally.
     */
    public boolean deleteProduct(long productId) throws SQLException, ProductInUseException {
        if (productId <= 0) {
            throw new IllegalArgumentException("ID sản phẩm không hợp lệ.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Product existing = productDAO.findById(conn, productId);
                if (existing == null) {
                    conn.rollback();
                    return false;
                }

                // Check business references
                if (productDAO.isProductInUse(conn, productId)) {
                    conn.rollback();
                    throw new ProductInUseException(productId,
                            "Không thể xóa sản phẩm '" + existing.getName() + " (" + existing.getCode() +
                                    ")' vì sản phẩm đã được sử dụng trong các giao dịch/chứng từ (Báo giá, Hợp đồng, Cơ hội, Đơn hàng).");
                }

                try {
                    int deleted = productDAO.delete(conn, productId);
                    conn.commit();
                    return deleted > 0;
                } catch (SQLIntegrityConstraintViolationException fkEx) {
                    // Foreign key constraint violation fallback
                    conn.rollback();
                    throw new ProductInUseException(productId,
                            "Không thể xóa sản phẩm '" + existing.getName() +
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

    /**
     * Soft delete: deactivates product instead of physical removal.
     */
    public boolean deactivateProduct(long productId) throws SQLException {
        if (productId <= 0) {
            return false;
        }
        return productDAO.softDelete(productId) > 0;
    }

    private void applyCostPriceAuthorization(Product product, Collection<String> userRoles) {
        if (product == null) {
            return;
        }
        if (!isDirectorOrAdmin(userRoles)) {
            // Mask cost price for non-director users
            product.setCostPrice(null);
        }
    }

    private void validateBasicProduct(Product product) {
        if (product.getCode() == null || product.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống.");
        }
        if (product.getCode().trim().length() > 50) {
            throw new IllegalArgumentException("Mã sản phẩm không được vượt quá 50 ký tự.");
        }

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        if (product.getName().trim().length() > 255) {
            throw new IllegalArgumentException("Tên sản phẩm không được vượt quá 255 ký tự.");
        }

        // Validate prices
        BigDecimal listPrice = product.getListPrice() != null ? product.getListPrice() : BigDecimal.ZERO;
        BigDecimal floorPrice = product.getFloorPrice() != null ? product.getFloorPrice() : BigDecimal.ZERO;

        if (listPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá niêm yết không được nhỏ hơn 0.");
        }
        if (floorPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá sàn không được nhỏ hơn 0.");
        }

        // CRITICAL BUSINESS RULE 2: list_price >= floor_price
        if (listPrice.compareTo(floorPrice) < 0) {
            throw new IllegalArgumentException("Giá niêm yết (" + listPrice + ") không được nhỏ hơn giá sàn (" + floorPrice + ").");
        }
    }

    public record ProductSearchResult(
            List<Product> items,
            long total,
            int page,
            int size,
            int totalPages
    ) {}
}