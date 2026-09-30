package com.crm.controller.categories;

import com.crm.model.Category;
import com.crm.model.CategoryType;
import com.crm.model.User;
import com.crm.service.categories.CategoryInUseException;
import com.crm.service.categories.CategoryService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling Common Master Data Categories (CRM-44):
 * 1. Industries (Ngành nghề)
 * 2. Company Sizes (Quy mô doanh nghiệp)
 * 3. Lead Sources (Nguồn Lead)
 * 4. Activity Types (Loại hoạt động)
 *
 * Supported Endpoints:
 * - GET    /api/categories                    — Get all 4 master data groups or filter by ?type=...
 * - GET    /api/categories/{type}             — Get categories of a specific type (sorted by display_order ASC)
 * - GET    /api/categories/{id}               — Get single category by ID
 * - POST   /api/categories                    — Create a new category
 * - PUT    /api/categories/{id}               — Update an existing category
 * - PUT    /api/categories/{id}/display-order — Update display order
 * - DELETE /api/categories/{id}               — Delete category (blocked with 409 if referenced in records)
 */
@WebServlet({
        "/api/categories",
        "/api/categories/*",
        "/api/master-data",
        "/api/master-data/*"
})
public class CategoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CategoryServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final CategoryService categoryService;

    public CategoryServlet() {
        this.categoryService = new CategoryService();
    }

    public CategoryServlet(CategoryService categoryService) {
        this.categoryService = categoryService != null ? categoryService : new CategoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        String pathInfo = request.getPathInfo();
        String typeParam = request.getParameter("type");
        if (typeParam == null) {
            typeParam = request.getParameter("category_type");
        }

        String activeParam = request.getParameter("active");
        Boolean activeOnly = null;
        if ("true".equalsIgnoreCase(activeParam) || "1".equals(activeParam)) {
            activeOnly = true;
        } else if ("false".equalsIgnoreCase(activeParam) || "0".equals(activeParam)) {
            activeOnly = false;
        }

        String keyword = request.getParameter("q");
        if (keyword == null) {
            keyword = request.getParameter("keyword");
        }

        try {
            // Case 1: Numeric ID in path (e.g. /api/categories/123)
            Long categoryId = parseNumericId(pathInfo);
            if (categoryId != null) {
                Category category = categoryService.getCategoryById(categoryId);
                if (category == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                            "Không tìm thấy danh mục với ID: " + categoryId, null);
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy thông tin danh mục thành công", category);
                return;
            }

            // Case 2: Specific category type in path or param (e.g. /api/categories/industries or ?type=INDUSTRY)
            CategoryType categoryType = null;
            if (pathInfo != null && !pathInfo.isBlank() && !"/".equals(pathInfo.trim())) {
                String subPath = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
                int slash = subPath.indexOf('/');
                if (slash != -1) {
                    subPath = subPath.substring(0, slash);
                }
                categoryType = CategoryType.fromString(subPath);
            }
            if (categoryType == null && typeParam != null) {
                categoryType = CategoryType.fromString(typeParam);
            }

            if (categoryType != null) {
                List<Category> list = categoryService.getCategories(categoryType, keyword, activeOnly);
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy danh sách " + categoryType.getDisplayName() + " thành công", list);
                return;
            }

            // Case 3: No type specified -> return all 4 groups grouped together
            var allGrouped = categoryService.getAllGrouped(activeOnly);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Lấy toàn bộ danh mục dùng chung thành công", allGrouped);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-44: Database error loading categories", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tải danh mục", null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // Check method override for HTML forms
        String methodOverride = request.getParameter("_method");
        if ("PUT".equalsIgnoreCase(methodOverride)) {
            doPut(request, response);
            return;
        } else if ("DELETE".equalsIgnoreCase(methodOverride)) {
            doDelete(request, response);
            return;
        }

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        Category category = parseCategoryPayload(request);
        if (category == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dữ liệu danh mục không hợp lệ", null);
            return;
        }

        try {
            Category created = categoryService.createCategory(category);
            writeJson(response, HttpServletResponse.SC_CREATED, true,
                    "Tạo danh mục mới thành công.", created);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-44: Database error creating category", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tạo danh mục mới", null);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        String pathInfo = request.getPathInfo();
        Long categoryId = parseNumericId(pathInfo);

        // Check if updating display order: /api/categories/{id}/display-order
        if (pathInfo != null && pathInfo.endsWith("/display-order") && categoryId != null) {
            handleDisplayOrderUpdate(request, response, categoryId);
            return;
        }

        Category category = parseCategoryPayload(request);
        if (category == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dữ liệu danh mục không hợp lệ", null);
            return;
        }

        if (categoryId != null && categoryId > 0) {
            category.setId(categoryId);
        }

        if (category.getId() == null || category.getId() <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu ID danh mục cần cập nhật", null);
            return;
        }

        try {
            Category updated = categoryService.updateCategory(category);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Cập nhật danh mục thành công.", updated);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-44: Database error updating category", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi cập nhật danh mục", null);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        Long categoryId = parseNumericId(request.getPathInfo());
        if (categoryId == null || categoryId <= 0) {
            String paramId = request.getParameter("id");
            if (paramId != null) {
                try {
                    categoryId = Long.parseLong(paramId.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (categoryId == null || categoryId <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu ID danh mục cần xóa", null);
            return;
        }

        try {
            boolean deleted = categoryService.deleteCategory(categoryId);
            if (!deleted) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy danh mục với ID: " + categoryId, null);
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Xóa danh mục thành công.", null);

        } catch (CategoryInUseException e) {
            // CRITICAL BUSINESS RULE 2: Block deletion of referenced category -> HTTP 409 Conflict
            LOGGER.log(Level.WARNING, "CRM-44: Blocked deletion of category in use: {0}", e.getMessage());
            writeJson(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-44: Database error deleting category", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi xóa danh mục", null);
        }
    }

    // === Helpers ===

    private void handleDisplayOrderUpdate(HttpServletRequest request, HttpServletResponse response, long categoryId)
            throws IOException {
        String orderParam = request.getParameter("displayOrder");
        if (orderParam == null) {
            orderParam = request.getParameter("order");
        }
        int newOrder = 0;
        if (orderParam != null) {
            try {
                newOrder = Integer.parseInt(orderParam.trim());
            } catch (NumberFormatException ignored) {}
        }
        try {
            boolean updated = categoryService.updateDisplayOrder(categoryId, newOrder);
            if (updated) {
                writeJson(response, HttpServletResponse.SC_OK, true, "Cập nhật thứ tự hiển thị thành công.", null);
            } else {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND, false, "Không tìm thấy danh mục với ID: " + categoryId, null);
            }
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Lỗi hệ thống", null);
        }
    }

    private Category parseCategoryPayload(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                return GSON.fromJson(request.getReader(), Category.class);
            } catch (JsonSyntaxException e) {
                return null;
            }
        }

        // Form urlencoded parsing
        Category c = new Category();
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.isBlank()) {
            try {
                c.setId(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String typeStr = request.getParameter("type");
        if (typeStr == null) {
            typeStr = request.getParameter("categoryType");
        }
        if (typeStr == null) {
            typeStr = request.getParameter("category_type");
        }
        c.setType(CategoryType.fromString(typeStr));

        c.setCode(request.getParameter("code"));
        c.setName(request.getParameter("name"));
        c.setDescription(request.getParameter("description"));

        String orderStr = request.getParameter("displayOrder");
        if (orderStr == null) {
            orderStr = request.getParameter("display_order");
        }
        if (orderStr != null && !orderStr.isBlank()) {
            try {
                c.setDisplayOrder(Integer.parseInt(orderStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String activeStr = request.getParameter("active");
        if (activeStr == null) {
            activeStr = request.getParameter("is_active");
        }
        if (activeStr != null) {
            c.setActive("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr) || "on".equalsIgnoreCase(activeStr));
        }

        return c;
    }

    private Long parseNumericId(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo.trim())) {
            return null;
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = clean.indexOf('/');
        if (slash != -1) {
            clean = clean.substring(0, slash);
        }
        try {
            long id = Long.parseLong(clean.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extractActorUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object directUserId = session.getAttribute("userId");
        if (directUserId instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        if (directUserId instanceof String text) {
            try {
                long parsed = Long.parseLong(text);
                if (parsed > 0) return parsed;
            } catch (NumberFormatException ignored) {}
        }
        Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        if (currentUser instanceof User u && u.getId() > 0) {
            return u.getId();
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    private record ApiResponse(boolean success, String message, Object data) {}
}
