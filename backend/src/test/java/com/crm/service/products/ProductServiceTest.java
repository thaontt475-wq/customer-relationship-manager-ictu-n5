package com.crm.service.products;

import com.crm.dao.products.ProductDAO;
import com.crm.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CRM-39 ProductService:
 * 1. Phân quyền hiển thị và cập nhật Giá Vốn (cost_price)
 * 2. Validation Giá Sàn (list_price >= floor_price)
 * 3. Chặn xóa sản phẩm đã sử dụng trong giao dịch/chứng từ
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final long PRODUCT_ID = 10L;

    @Mock
    private ProductDAO productDAO;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productDAO);
    }

    @Nested
    @DisplayName("Cost Price Authorization Tests (Phân quyền Giá Vốn)")
    class CostPriceAuthorizationTests {

        @ParameterizedTest
        @ValueSource(strings = {"director", "ROLE_DIRECTOR", "Giám đốc", "GIAM DOC", "Giám đốc kinh doanh"})
        @DisplayName("Only Director can view cost_price")
        void directorOrAdmin_canViewCostPrice(String role) throws SQLException {
            Product raw = new Product(PRODUCT_ID, "PRD-01", "CRM Enterprise", "Software", "Gói",
                    new BigDecimal("10000000"), new BigDecimal("8000000"), new BigDecimal("5000000"),
                    "Desc", true);

            when(productDAO.findById(any(Connection.class), eq(PRODUCT_ID))).thenReturn(raw);

            Product result = productService.getProductById(PRODUCT_ID, List.of(role));

            assertNotNull(result);
            assertEquals(new BigDecimal("5000000"), result.getCostPrice());
        }

        @ParameterizedTest
        @ValueSource(strings = {"admin", "ROLE_ADMIN", "Quản trị viên", "sales rep", "marketing", "cust. success", "accountant", "team lead"})
        @DisplayName("Non-directors have cost_price masked to null")
        void nonDirector_hasCostPriceMasked(String role) throws SQLException {
            Product raw = new Product(PRODUCT_ID, "PRD-01", "CRM Enterprise", "Software", "Gói",
                    new BigDecimal("10000000"), new BigDecimal("8000000"), new BigDecimal("5000000"),
                    "Desc", true);

            when(productDAO.findById(any(Connection.class), eq(PRODUCT_ID))).thenReturn(raw);

            Product result = productService.getProductById(PRODUCT_ID, List.of(role));

            assertNotNull(result);
            assertNull(result.getCostPrice(), "cost_price must be masked to null for non-director roles");
        }

        @Test
        @DisplayName("Admin does not gain cost-price rights")
        void adminDeniedCostPrice() {
            assertFalse(productService.canAccessCostPrice(List.of("admin")));
            assertFalse(productService.canAccessCostPrice(List.of("ROLE_ADMIN")));
            assertTrue(productService.canAccessCostPrice(List.of("admin", "director")));
        }

        @Test
        @DisplayName("Non-director updating product cannot tamper with existing cost_price")
        void nonDirector_updatingProduct_preservesExistingCostPrice() throws SQLException {
            Product existingInDb = new Product(PRODUCT_ID, "PRD-01", "Old Name", "Software", "Gói",
                    new BigDecimal("10000000"), new BigDecimal("8000000"), new BigDecimal("5000000"),
                    "Old Desc", true);

            Product userUpdate = new Product(PRODUCT_ID, "PRD-01", "New Name", "Software", "Gói",
                    new BigDecimal("12000000"), new BigDecimal("9000000"), new BigDecimal("1000"), // Tampered cost
                    "New Desc", true);

            when(productDAO.findById(any(Connection.class), eq(PRODUCT_ID))).thenReturn(existingInDb);
            when(productDAO.existsByCode(any(Connection.class), eq("PRD-01"), eq(PRODUCT_ID))).thenReturn(false);
            when(productDAO.update(any(Connection.class), any(Product.class))).thenReturn(1);

            productService.updateProduct(userUpdate, List.of("sales rep"));

            // Verify that cost_price passed to DAO was preserved from existingInDb (5000000, not 1000)
            verify(productDAO).update(any(Connection.class), argThat(p ->
                    new BigDecimal("5000000").equals(p.getCostPrice())
            ));
        }

        @Test
        @DisplayName("Director updating product can update cost_price")
        void director_updatingProduct_canUpdateCostPrice() throws SQLException {
            Product existingInDb = new Product(PRODUCT_ID, "PRD-01", "Old Name", "Software", "Gói",
                    new BigDecimal("10000000"), new BigDecimal("8000000"), new BigDecimal("5000000"),
                    "Old Desc", true);

            Product userUpdate = new Product(PRODUCT_ID, "PRD-01", "New Name", "Software", "Gói",
                    new BigDecimal("12000000"), new BigDecimal("9000000"), new BigDecimal("6000000"),
                    "New Desc", true);

            when(productDAO.findById(any(Connection.class), eq(PRODUCT_ID))).thenReturn(existingInDb);
            when(productDAO.existsByCode(any(Connection.class), eq("PRD-01"), eq(PRODUCT_ID))).thenReturn(false);
            when(productDAO.update(any(Connection.class), any(Product.class))).thenReturn(1);

            productService.updateProduct(userUpdate, List.of("director"));

            verify(productDAO).update(any(Connection.class), argThat(p ->
                    new BigDecimal("6000000").equals(p.getCostPrice())
            ));
        }
    }

    @Nested
    @DisplayName("Floor Price Validation Tests (list_price >= floor_price)")
    class FloorPriceValidationTests {

        @Test
        @DisplayName("List price lower than floor price throws IllegalArgumentException")
        void listPriceLowerThanFloorPrice_throwsException() {
            Product invalid = new Product(null, "PRD-02", "Service A", "Service", "Giờ",
                    new BigDecimal("500000"), new BigDecimal("800000"), BigDecimal.ZERO,
                    "Invalid pricing", true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> productService.createProduct(invalid, List.of("admin")));

            assertTrue(ex.getMessage().contains("Giá niêm yết") && ex.getMessage().contains("không được nhỏ hơn giá sàn"));
        }

        @Test
        @DisplayName("List price equal to floor price succeeds")
        void listPriceEqualToFloorPrice_succeeds() throws SQLException {
            Product valid = new Product(null, "PRD-03", "Service B", "Service", "Giờ",
                    new BigDecimal("500000"), new BigDecimal("500000"), BigDecimal.ZERO,
                    "Valid pricing", true);

            Product created = new Product(11L, "PRD-03", "Service B", "Service", "Giờ",
                    new BigDecimal("500000"), new BigDecimal("500000"), BigDecimal.ZERO,
                    "Valid pricing", true);

            when(productDAO.existsByCode(any(Connection.class), eq("PRD-03"), isNull())).thenReturn(false);
            when(productDAO.insert(any(Connection.class), any(Product.class))).thenReturn(11L);
            when(productDAO.findById(any(Connection.class), eq(11L))).thenReturn(created);

            Product result = productService.createProduct(valid, List.of("admin"));

            assertNotNull(result);
            assertEquals(new BigDecimal("500000"), result.getListPrice());
            assertEquals(new BigDecimal("500000"), result.getFloorPrice());
        }

        @Test
        @DisplayName("Negative floor price throws IllegalArgumentException")
        void negativeFloorPrice_throwsException() {
            Product invalid = new Product(null, "PRD-04", "Product C", "Goods", "Cái",
                    new BigDecimal("100000"), new BigDecimal("-50000"), BigDecimal.ZERO,
                    "Negative floor", true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> productService.createProduct(invalid, List.of("admin")));

            assertTrue(ex.getMessage().contains("Giá sàn không được nhỏ hơn 0"));
        }

        @Test
        @DisplayName("Blank code or name throws IllegalArgumentException")
        void blankCodeOrName_throwsException() {
            Product noCode = new Product(null, "", "Product Name", "Goods", "Cái",
                    BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, "Desc", true);
            assertThrows(IllegalArgumentException.class, () -> productService.createProduct(noCode, List.of("admin")));

            Product noName = new Product(null, "PRD-05", "   ", "Goods", "Cái",
                    BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, "Desc", true);
            assertThrows(IllegalArgumentException.class, () -> productService.createProduct(noName, List.of("admin")));
        }

        @Test
        @DisplayName("Duplicate product code throws IllegalArgumentException")
        void duplicateCode_throwsException() throws SQLException {
            Product duplicate = new Product(null, "PRD-EXIST", "Product Dup", "Goods", "Cái",
                    new BigDecimal("200000"), new BigDecimal("100000"), BigDecimal.ZERO, "Desc", true);

            when(productDAO.existsByCode(any(Connection.class), eq("PRD-EXIST"), isNull())).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> productService.createProduct(duplicate, List.of("admin")));

            assertTrue(ex.getMessage().contains("đã tồn tại"));
        }
    }

    @Nested
    @DisplayName("Product Deletion Tests (Chặn xóa sản phẩm đã tham chiếu)")
    class ProductDeletionTests {

        @Test
        @DisplayName("Deleting product referenced in transactions throws ProductInUseException")
        void deleteReferencedProduct_throwsProductInUseException() throws SQLException {
            Product existing = new Product(PRODUCT_ID, "PRD-REF", "Product Used", "Goods", "Cái",
                    new BigDecimal("100000"), new BigDecimal("80000"), BigDecimal.ZERO, "Desc", true);

            when(productDAO.findById(any(Connection.class), eq(PRODUCT_ID))).thenReturn(existing);
            when(productDAO.isProductInUse(any(Connection.class), eq(PRODUCT_ID))).thenReturn(true);

            ProductInUseException ex = assertThrows(ProductInUseException.class,
                    () -> productService.deleteProduct(PRODUCT_ID));

            assertEquals(PRODUCT_ID, ex.getProductId());
            assertTrue(ex.getMessage().contains("đã được sử dụng trong các giao dịch/chứng từ"));
            verify(productDAO, never()).delete(any(Connection.class), anyLong());
        }

        @Test
        @DisplayName("Deleting unreferenced product succeeds")
        void deleteUnreferencedProduct_succeeds() throws SQLException, ProductInUseException {
            Product existing = new Product(PRODUCT_ID, "PRD-CLEAN", "Product Unused", "Goods", "Cái",
                    new BigDecimal("100000"), new BigDecimal("80000"), BigDecimal.ZERO, "Desc", true);

            when(productDAO.findById(any(Connection.class), eq(PRODUCT_ID))).thenReturn(existing);
            when(productDAO.isProductInUse(any(Connection.class), eq(PRODUCT_ID))).thenReturn(false);
            when(productDAO.delete(any(Connection.class), eq(PRODUCT_ID))).thenReturn(1);

            boolean result = productService.deleteProduct(PRODUCT_ID);

            assertTrue(result);
            verify(productDAO).delete(any(Connection.class), eq(PRODUCT_ID));
        }

        @Test
        @DisplayName("Database foreign key constraint violation is translated to ProductInUseException")
        void fkConstraintViolation_translatedToProductInUseException() throws SQLException {
            Product existing = new Product(PRODUCT_ID, "PRD-FK", "Product FK", "Goods", "Cái",
                    new BigDecimal("100000"), new BigDecimal("80000"), BigDecimal.ZERO, "Desc", true);

            when(productDAO.findById(any(Connection.class), eq(PRODUCT_ID))).thenReturn(existing);
            when(productDAO.isProductInUse(any(Connection.class), eq(PRODUCT_ID))).thenReturn(false);
            when(productDAO.delete(any(Connection.class), eq(PRODUCT_ID)))
                    .thenThrow(new SQLIntegrityConstraintViolationException("FK violation error 1451"));

            ProductInUseException ex = assertThrows(ProductInUseException.class,
                    () -> productService.deleteProduct(PRODUCT_ID));

            assertTrue(ex.getMessage().contains("ràng buộc dữ liệu liên quan"));
        }

        @Test
        @DisplayName("Deleting non-existent product returns false")
        void deleteNonExistentProduct_returnsFalse() throws SQLException, ProductInUseException {
            when(productDAO.findById(any(Connection.class), eq(999L))).thenReturn(null);

            boolean result = productService.deleteProduct(999L);

            assertFalse(result);
        }
    }
}
