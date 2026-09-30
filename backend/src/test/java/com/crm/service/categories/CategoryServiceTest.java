package com.crm.service.categories;

import com.crm.dao.categories.CategoryDAO;
import com.crm.model.Category;
import com.crm.model.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CRM-44 CategoryService:
 * 1. Display order sorting (display_order ASC)
 * 2. Dependency check and blocked deletion when category is referenced
 * 3. Field validation and code uniqueness per category type
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final long CATEGORY_ID = 5L;

    @Mock
    private CategoryDAO categoryDAO;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryDAO);
    }

    @Nested
    @DisplayName("Display Order Tests (Sắp xếp thứ tự)")
    class DisplayOrderTests {

        @Test
        @DisplayName("getCategories returns list from DAO for specific type")
        void getCategories_returnsSortedList() throws SQLException {
            Category c1 = new Category(1L, CategoryType.INDUSTRY, "IT", "Công nghệ thông tin", "Desc 1", 1, true);
            Category c2 = new Category(2L, CategoryType.INDUSTRY, "RETAIL", "Bán lẻ", "Desc 2", 2, true);
            Category c3 = new Category(3L, CategoryType.INDUSTRY, "OTHER", "Khác", "Desc 3", 99, true);

            when(categoryDAO.findAll(any(Connection.class), eq("INDUSTRY"), isNull(), isNull()))
                    .thenReturn(List.of(c1, c2, c3));

            List<Category> result = categoryService.getCategories(CategoryType.INDUSTRY, null, null);

            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).getDisplayOrder());
            assertEquals(2, result.get(1).getDisplayOrder());
            assertEquals(99, result.get(2).getDisplayOrder());
        }

        @Test
        @DisplayName("updateDisplayOrder successfully updates order in DAO")
        void updateDisplayOrder_succeeds() throws SQLException {
            when(categoryDAO.updateDisplayOrder(eq(CATEGORY_ID), eq(10))).thenReturn(1);

            boolean updated = categoryService.updateDisplayOrder(CATEGORY_ID, 10);

            assertTrue(updated);
            verify(categoryDAO).updateDisplayOrder(CATEGORY_ID, 10);
        }
    }

    @Nested
    @DisplayName("Dependency Check & Blocked Deletion Tests (Chặn xóa khi đã tham chiếu)")
    class BlockedDeletionTests {

        @Test
        @DisplayName("Deleting category referenced in business records throws CategoryInUseException")
        void deleteCategoryInUse_throwsCategoryInUseException() throws SQLException {
            Category existing = new Category(CATEGORY_ID, CategoryType.INDUSTRY, "IT", "Công nghệ thông tin", "", 1, true);

            when(categoryDAO.findById(any(Connection.class), eq(CATEGORY_ID))).thenReturn(existing);
            when(categoryDAO.isCategoryInUse(any(Connection.class), eq("INDUSTRY"), eq(CATEGORY_ID))).thenReturn(true);

            CategoryInUseException ex = assertThrows(CategoryInUseException.class,
                    () -> categoryService.deleteCategory(CATEGORY_ID));

            assertEquals(CATEGORY_ID, ex.getCategoryId());
            assertEquals("INDUSTRY", ex.getCategoryType());
            assertTrue(ex.getMessage().contains("đang được sử dụng trong hệ thống"));
            verify(categoryDAO, never()).delete(any(Connection.class), anyLong());
        }

        @Test
        @DisplayName("Deleting unused category succeeds")
        void deleteUnusedCategory_succeeds() throws SQLException, CategoryInUseException {
            Category existing = new Category(CATEGORY_ID, CategoryType.LEAD_SOURCE, "EVENT", "Sự kiện", "", 5, true);

            when(categoryDAO.findById(any(Connection.class), eq(CATEGORY_ID))).thenReturn(existing);
            when(categoryDAO.isCategoryInUse(any(Connection.class), eq("LEAD_SOURCE"), eq(CATEGORY_ID))).thenReturn(false);
            when(categoryDAO.delete(any(Connection.class), eq(CATEGORY_ID))).thenReturn(1);

            boolean result = categoryService.deleteCategory(CATEGORY_ID);

            assertTrue(result);
            verify(categoryDAO).delete(any(Connection.class), eq(CATEGORY_ID));
        }

        @Test
        @DisplayName("Database foreign key constraint violation is translated to CategoryInUseException")
        void fkViolation_translatedToCategoryInUseException() throws SQLException {
            Category existing = new Category(CATEGORY_ID, CategoryType.ACTIVITY_TYPE, "CALL", "Cuộc gọi", "", 1, true);

            when(categoryDAO.findById(any(Connection.class), eq(CATEGORY_ID))).thenReturn(existing);
            when(categoryDAO.isCategoryInUse(any(Connection.class), eq("ACTIVITY_TYPE"), eq(CATEGORY_ID))).thenReturn(false);
            when(categoryDAO.delete(any(Connection.class), eq(CATEGORY_ID)))
                    .thenThrow(new SQLIntegrityConstraintViolationException("FK constraint error 1451"));

            CategoryInUseException ex = assertThrows(CategoryInUseException.class,
                    () -> categoryService.deleteCategory(CATEGORY_ID));

            assertTrue(ex.getMessage().contains("ràng buộc dữ liệu liên quan"));
        }

        @Test
        @DisplayName("Deleting non-existent category returns false")
        void deleteNonExistentCategory_returnsFalse() throws SQLException, CategoryInUseException {
            when(categoryDAO.findById(any(Connection.class), eq(999L))).thenReturn(null);

            boolean result = categoryService.deleteCategory(999L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Validation and Uniqueness Tests")
    class ValidationTests {

        @Test
        @DisplayName("Creating category with null type throws IllegalArgumentException")
        void createCategory_nullType_throws() {
            Category invalid = new Category(null, null, "CODE", "Name", "", 0, true);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> categoryService.createCategory(invalid));
            assertTrue(ex.getMessage().contains("Loại danh mục"));
        }

        @Test
        @DisplayName("Creating category with blank code or name throws IllegalArgumentException")
        void createCategory_blankFields_throws() {
            Category blankCode = new Category(null, CategoryType.INDUSTRY, "   ", "Name", "", 0, true);
            assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(blankCode));

            Category blankName = new Category(null, CategoryType.INDUSTRY, "CODE", "   ", "", 0, true);
            assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(blankName));
        }

        @Test
        @DisplayName("Duplicate code in same category type throws IllegalArgumentException")
        void createCategory_duplicateCode_throws() throws SQLException {
            Category duplicate = new Category(null, CategoryType.COMPANY_SIZE, "SMALL", "Doanh nghiệp nhỏ", "", 1, true);

            when(categoryDAO.existsByTypeCode(any(Connection.class), eq("COMPANY_SIZE"), eq("SMALL"), isNull()))
                    .thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> categoryService.createCategory(duplicate));

            assertTrue(ex.getMessage().contains("đã tồn tại trong nhóm"));
        }

        @Test
        @DisplayName("getAllGrouped retrieves all 4 category types")
        void getAllGrouped_retrievesAllTypes() throws SQLException {
            when(categoryDAO.findAll(any(Connection.class), eq("INDUSTRY"), isNull(), isNull()))
                    .thenReturn(List.of(new Category(1L, CategoryType.INDUSTRY, "IT", "IT", "", 1, true)));
            when(categoryDAO.findAll(any(Connection.class), eq("COMPANY_SIZE"), isNull(), isNull()))
                    .thenReturn(List.of(new Category(2L, CategoryType.COMPANY_SIZE, "S", "Small", "", 1, true)));
            when(categoryDAO.findAll(any(Connection.class), eq("LEAD_SOURCE"), isNull(), isNull()))
                    .thenReturn(List.of(new Category(3L, CategoryType.LEAD_SOURCE, "WEB", "Web", "", 1, true)));
            when(categoryDAO.findAll(any(Connection.class), eq("ACTIVITY_TYPE"), isNull(), isNull()))
                    .thenReturn(List.of(new Category(4L, CategoryType.ACTIVITY_TYPE, "CALL", "Call", "", 1, true)));

            var result = categoryService.getAllGrouped(null);

            assertNotNull(result);
            assertEquals(1, result.industries().size());
            assertEquals(1, result.companySizes().size());
            assertEquals(1, result.leadSources().size());
            assertEquals(1, result.activityTypes().size());
        }
    }
}
