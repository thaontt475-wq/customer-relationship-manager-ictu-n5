package com.crm.controller.products;

import com.crm.controller.ServerForms;
import com.crm.model.Product;
import com.crm.service.permissions.MenuService;
import com.crm.service.products.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** HTML presentation for S2-05. Existing /api/products JSON handlers remain unchanged. */
@WebServlet({"/products/page", "/products/create", "/products/edit"})
public class ProductPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(ProductPageServlet.class.getName());
    private final ProductService service;
    private final MenuService menuService;

    public ProductPageServlet() {
        this(new ProductService(), new MenuService());
    }

    public ProductPageServlet(ProductService service) {
        this(service, new MenuService());
    }

    public ProductPageServlet(ProductService service, MenuService menuService) {
        this.service = service;
        this.menuService = menuService != null ? menuService : new MenuService();
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String servletPath = req.getServletPath();

        // 1. GET /products/create
        if ("/products/create".equals(servletPath)) {
            if (!ServerForms.authorize(req, res, true)) return;
            res.setHeader("Cache-Control", "no-store");
            req.setAttribute("canManage", true);
            req.setAttribute("canViewCostPrice", service.canAccessCostPrice(ServerForms.roles(req)));
            try {
                req.setAttribute("menuItems", menuService.getMenuItems(ServerForms.roles(req)));
            } catch (Exception ignored) {}
            req.getRequestDispatcher("/jsp/products/product-create.jsp").forward(req, res);
            return;
        }

        // 2. GET /products/edit
        if ("/products/edit".equals(servletPath)) {
            if (!ServerForms.authorize(req, res, true)) return;
            res.setHeader("Cache-Control", "no-store");
            String idStr = req.getParameter("id");
            if (idStr == null || idStr.isBlank()) {
                res.sendError(400, "Thiếu mã sản phẩm.");
                return;
            }
            try {
                long editId = ServerForms.positive(idStr);
                Product edit = service.getProductById(editId, ServerForms.roles(req));
                if (edit == null) {
                    res.sendError(404, "Không tìm thấy sản phẩm.");
                    return;
                }
                req.setAttribute("canManage", true);
                req.setAttribute("canViewCostPrice", service.canAccessCostPrice(ServerForms.roles(req)));
                req.setAttribute("editProduct", edit);
                try {
                    req.setAttribute("menuItems", menuService.getMenuItems(ServerForms.roles(req)));
                } catch (Exception ignored) {}
                req.getRequestDispatcher("/jsp/products/product-edit.jsp").forward(req, res);
                return;
            } catch (IllegalArgumentException e) {
                res.sendError(400, e.getMessage());
                return;
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Cannot load product for edit", e);
                res.sendError(500, "Không tải được thông tin sản phẩm.");
                return;
            }
        }

        // 3. Default: /products/page
        if (!ServerForms.authorize(req, res, false)) return;
        res.setHeader("Cache-Control", "no-store");
        req.setAttribute("canManage", ServerForms.admin(req));
        req.setAttribute("canViewCostPrice", service.canAccessCostPrice(ServerForms.roles(req)));
        try {
            req.setAttribute("menuItems", menuService.getMenuItems(ServerForms.roles(req)));
        } catch (Exception ignored) {}
        req.setAttribute("q", ServerForms.value(req,"q",""));
        req.setAttribute("category", ServerForms.value(req,"category",""));
        req.setAttribute("active", ServerForms.value(req,"active",""));
        int page = page(req.getParameter("page"));
        try {
            Boolean active = "true".equals(req.getParameter("active")) ? Boolean.TRUE
                    : "false".equals(req.getParameter("active")) ? Boolean.FALSE : null;
            var result = service.searchProducts(req.getParameter("q"),
                    req.getParameter("category"), active, page, 20, ServerForms.roles(req));
            req.setAttribute("products", result);
            if (req.getParameter("edit") != null) {
                if (!ServerForms.admin(req)) { res.sendError(403); return; }
                Product edit = service.getProductById(ServerForms.positive(req.getParameter("edit")),
                        ServerForms.roles(req));
                if (edit == null) { res.sendError(404); return; }
                req.setAttribute("editProduct", edit);
            }
            if ("ok".equals(req.getParameter("result")))
                req.setAttribute("notice", "Đã lưu sản phẩm thành công.");
        } catch (IllegalArgumentException e) {
            res.sendError(400, "Tham số không hợp lệ."); return;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot load product HTML page", e);
            res.sendError(500, "Không tải được danh mục sản phẩm."); return;
        }
        req.getRequestDispatcher("/jsp/products/product-list.jsp").forward(req, res);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ServerForms.authorize(req,res,true)) return;
        if (!ServerForms.checkCsrf(req,res)) return;
        req.setCharacterEncoding("UTF-8");
        String servletPath = req.getServletPath();
        try {
            String operation = req.getParameter("operation");
            if (operation == null || operation.isBlank()) {
                if ("/products/create".equals(servletPath)) operation = "create";
                else if ("/products/edit".equals(servletPath)) operation = "update";
            }
            if ("disable".equals(operation)) {
                if (!"yes".equals(req.getParameter("confirm")))
                    throw new IllegalArgumentException("Bạn phải xác nhận ngừng kinh doanh.");
                Product existing = service.getProductById(ServerForms.positive(req.getParameter("id")),ServerForms.roles(req));
                if (existing == null) { res.sendError(404); return; }
                existing.setActive(false);
                service.updateProduct(existing, ServerForms.roles(req));
                ServerForms.setToast(req, "success", "Ngừng kinh doanh thành công", "Sản phẩm đã được chuyển sang trạng thái ngừng kinh doanh.");
            } else {
                if (!"create".equals(operation) && !"update".equals(operation))
                    throw new IllegalArgumentException("Thao tác không hợp lệ.");
                Product product = new Product();
                if ("update".equals(operation))
                    product.setId(ServerForms.positive(req.getParameter("id")));
                product.setCode(req.getParameter("code"));
                product.setName(req.getParameter("name"));
                product.setCategory(req.getParameter("category"));
                product.setUnit(req.getParameter("unit"));
                product.setDescription(req.getParameter("description"));
                product.setListPrice(money(req.getParameter("listPrice")));
                product.setFloorPrice(money(req.getParameter("floorPrice")));
                if (req.getParameter("costPrice") != null && !req.getParameter("costPrice").isBlank())
                    product.setCostPrice(money(req.getParameter("costPrice")));
                String activeParam = req.getParameter("active");
                boolean isActive = "true".equalsIgnoreCase(activeParam) || "1".equals(activeParam) || "on".equalsIgnoreCase(activeParam);
                product.setActive(isActive);
                if ("create".equals(operation)) {
                    service.createProduct(product,ServerForms.roles(req));
                    ServerForms.setToast(req, "success", "Tạo sản phẩm thành công", "Sản phẩm mới đã được thêm vào bảng giá.");
                } else {
                    service.updateProduct(product,ServerForms.roles(req));
                    ServerForms.setToast(req, "success", "Cập nhật thành công", "Thông tin sản phẩm đã được lưu.");
                }
            }
            res.sendRedirect(req.getContextPath() + "/products/page?result=ok");
        } catch (IllegalArgumentException e) {
            res.sendError(400, e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot save product HTML form",e);
            res.sendError(500,"Không lưu được sản phẩm. Kiểm tra dữ liệu và thử lại.");
        }
    }
    private static int page(String raw) {
        try { return Math.max(1,Math.min(100000,Integer.parseInt(raw))); }
        catch (NumberFormatException ignored) { return 1; }
    }
    private static BigDecimal money(String raw) {
        try { return new BigDecimal(raw == null || raw.isBlank() ? "0" : raw.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Giá tiền không hợp lệ."); }
    }
}
