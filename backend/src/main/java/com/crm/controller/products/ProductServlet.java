package com.crm.controller.products;

import com.crm.model.Product;
import com.crm.model.User;
import com.crm.service.products.ProductInUseException;
import com.crm.service.products.ProductService;
import com.crm.service.products.ProductService.ProductSearchResult;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling Product and Price Book operations for CRM-39:
 * - GET    /api/products           — List/Search products (paginated, cost_price masked for non-directors)
 * - GET    /api/products/{id}      — Get single product details
 * - POST   /api/products           — Create new product (cost_price restricted to directors)
 * - PUT    /api/products/{id}      — Update product (floor_price validated, cost_price protected)
 * - DELETE /api/products/{id}      — Delete product (blocked with 409 if referenced in quotes/deals/contracts/orders)
 */
@WebServlet({
        "/products",
        "/products/*",
        "/api/products",
        "/api/products/*"
})
public class ProductServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ProductServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final ProductService productService;

    public ProductServlet() {
        this.productService = new ProductService();
    }

    public ProductServlet(ProductService productService) {
        this.productService = productService != null ? productService : new ProductService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        boolean isApi = isApiRequest(request);
        if (!isApi && (request.getPathInfo() == null || "/".equals(request.getPathInfo()))) {
            response.sendRedirect(request.getContextPath() + "/products/page");
            return;
        }

        if (actorUserId == null) {
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
            }
            return;
        }

        Collection<String> roles = extractUserRoles(request);
        Long productId = extractIdFromPath(request);

        try {
            if (productId != null) {
                // View single product details
                Product product = productService.getProductById(productId, roles);
                if (product == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                            "Không tìm thấy sản phẩm với ID: " + productId, null);
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy thông tin sản phẩm thành công", product);
            } else {
                // Search / List products
                String keyword = request.getParameter("q");
                if (keyword == null || keyword.isBlank()) {
                    keyword = request.getParameter("keyword");
                }
                String category = request.getParameter("category");
                String activeStr = request.getParameter("active");
                Boolean activeOnly = null;
                if ("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr)) {
                    activeOnly = true;
                } else if ("false".equalsIgnoreCase(activeStr) || "0".equals(activeStr)) {
                    activeOnly = false;
                }

                int page = parseIntParam(request.getParameter("page"), 1);
                int size = parseIntParam(request.getParameter("size"), 10);
                if (size <= 0) {
                    size = parseIntParam(request.getParameter("limit"), 10);
                }

                ProductSearchResult result = productService.searchProducts(keyword, category, activeOnly, page, size, roles);
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy danh sách sản phẩm thành công", result);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-39: Database error loading products", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tải thông tin sản phẩm", null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // Check method override for HTML forms (_method=PUT or _method=DELETE)
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

        Collection<String> roles = extractUserRoles(request);
        Product product = parseProductPayload(request);
        if (product == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dữ liệu sản phẩm không hợp lệ", null);
            return;
        }

        try {
            Product created = productService.createProduct(product, roles);
            writeJson(response, HttpServletResponse.SC_CREATED, true,
                    "Tạo sản phẩm mới thành công.", created);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-39: Database error creating product", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tạo sản phẩm mới", null);
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

        Collection<String> roles = extractUserRoles(request);
        Product product = parseProductPayload(request);
        if (product == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dữ liệu sản phẩm không hợp lệ", null);
            return;
        }

        Long pathId = extractIdFromPath(request);
        if (pathId != null && pathId > 0) {
            product.setId(pathId);
        }

        if (product.getId() == null || product.getId() <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu ID sản phẩm cần cập nhật", null);
            return;
        }

        try {
            Product updated = productService.updateProduct(product, roles);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Cập nhật sản phẩm thành công.", updated);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-39: Database error updating product", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi cập nhật sản phẩm", null);
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

        Long productId = extractIdFromPath(request);
        if (productId == null || productId <= 0) {
            String paramId = request.getParameter("id");
            if (paramId != null) {
                try {
                    productId = Long.parseLong(paramId.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (productId == null || productId <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu ID sản phẩm cần xóa", null);
            return;
        }

        try {
            boolean deleted = productService.deleteProduct(productId);
            if (!deleted) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy sản phẩm với ID: " + productId, null);
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Xóa sản phẩm thành công.", null);

        } catch (ProductInUseException e) {
            // CRITICAL BUSINESS RULE 3: Block deletion of referenced products -> HTTP 409 Conflict
            LOGGER.log(Level.WARNING, "CRM-39: Blocked deletion of product in use: {0}", e.getMessage());
            writeJson(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-39: Database error deleting product", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi xóa sản phẩm", null);
        }
    }

    // === Helpers ===

    private Product parseProductPayload(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                return GSON.fromJson(request.getReader(), Product.class);
            } catch (JsonSyntaxException e) {
                return null;
            }
        }

        // Parse from form-urlencoded
        Product p = new Product();
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.isBlank()) {
            try {
                p.setId(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException ignored) {}
        }
        p.setCode(request.getParameter("code"));
        p.setName(request.getParameter("name"));
        p.setCategory(request.getParameter("category"));
        p.setUnit(request.getParameter("unit"));

        String listPriceStr = request.getParameter("listPrice");
        if (listPriceStr == null) {
            listPriceStr = request.getParameter("list_price");
        }
        if (listPriceStr != null && !listPriceStr.isBlank()) {
            try {
                p.setListPrice(new BigDecimal(listPriceStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String floorPriceStr = request.getParameter("floorPrice");
        if (floorPriceStr == null) {
            floorPriceStr = request.getParameter("floor_price");
        }
        if (floorPriceStr != null && !floorPriceStr.isBlank()) {
            try {
                p.setFloorPrice(new BigDecimal(floorPriceStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String costPriceStr = request.getParameter("costPrice");
        if (costPriceStr == null) {
            costPriceStr = request.getParameter("cost_price");
        }
        if (costPriceStr != null && !costPriceStr.isBlank()) {
            try {
                p.setCostPrice(new BigDecimal(costPriceStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        p.setDescription(request.getParameter("description"));
        String activeStr = request.getParameter("active");
        if (activeStr == null) {
            activeStr = request.getParameter("is_active");
        }
        if (activeStr != null) {
            p.setActive("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr) || "on".equalsIgnoreCase(activeStr));
        }

        return p;
    }

    private Long extractIdFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo.trim())) {
            return null;
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slashIdx = clean.indexOf('/');
        if (slashIdx != -1) {
            clean = clean.substring(0, slashIdx);
        }
        try {
            long id = Long.parseLong(clean.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getContentType();

        return (servletPath != null && servletPath.startsWith("/api/"))
                || (acceptHeader != null && acceptHeader.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
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

    private Collection<String> extractUserRoles(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return List.of();
        }
        Object rolesObj = session.getAttribute(SessionKey.ROLES);
        if (rolesObj instanceof Collection<?> col) {
            return col.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        if (currentUser instanceof User u && u.getRoles() != null) {
            return u.getRoles().stream().map(com.crm.model.Role::getName).toList();
        }
        return List.of();
    }

    private int parseIntParam(String param, int defaultValue) {
        if (param == null || param.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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