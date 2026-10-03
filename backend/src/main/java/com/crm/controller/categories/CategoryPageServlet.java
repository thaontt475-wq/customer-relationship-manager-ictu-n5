package com.crm.controller.categories;

import com.crm.controller.ServerForms;
import com.crm.model.Category;
import com.crm.model.CategoryType;
import com.crm.service.categories.CategoryService;
import com.crm.service.categories.CategoryInUseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/** HTML form adapter for S2-07; the existing JSON API is unchanged. */
@WebServlet({"/configuration", "/configuration/page"})
public class CategoryPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(CategoryPageServlet.class.getName());
    private final CategoryService service;
    public CategoryPageServlet() { this(new CategoryService()); }
    public CategoryPageServlet(CategoryService service) { this.service = service; }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        if (!ServerForms.authorize(req,res,false)) return;
        res.setHeader("Cache-Control", "no-store");
        try {
            String rawType = ServerForms.value(req,"type","INDUSTRY");
            CategoryType type = CategoryType.fromString(rawType);
            if (type == null) { res.sendError(400,"Nhóm danh mục không hợp lệ."); return; }
            String status = ServerForms.value(req,"active","");
            Boolean active = switch(status) {
                case "true" -> Boolean.TRUE;
                case "false" -> Boolean.FALSE;
                case "" -> null;
                default -> throw new IllegalArgumentException("Trạng thái không hợp lệ.");
            };
            List<Category> categories = service.getCategories(type,ServerForms.value(req,"q",""),active);
            Category edit = null;
            if (req.getParameter("edit") != null) {
                if (!ServerForms.admin(req)) {res.sendError(403);return;}
                edit = service.getCategoryById(ServerForms.positive(req.getParameter("edit")));
                if (edit == null || edit.getType() != type) {res.sendError(404);return;}
            }
            req.setAttribute("categories",categories);
            req.setAttribute("currentType",type);
            req.setAttribute("keyword",ServerForms.value(req,"q",""));
            req.setAttribute("statusFilter",status);
            req.setAttribute("editCategory",edit);
            req.setAttribute("canManage",ServerForms.admin(req));
            if ("ok".equals(req.getParameter("result")))
                req.setAttribute("notice","Cập nhật danh mục thành công.");
            req.getRequestDispatcher("/jsp/configuration/configuration.jsp").forward(req,res);
        } catch (IllegalArgumentException e) {res.sendError(400,"Tham số không hợp lệ.");}
        catch (SQLException e) {LOG.log(Level.SEVERE,"Cannot load category HTML page",e);res.sendError(500,"Không tải được danh mục.");}
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        if (!ServerForms.authorize(req,res,true)) return;
        if (!ServerForms.checkCsrf(req,res)) return;
        req.setCharacterEncoding("UTF-8");
        try {
            String action = req.getParameter("action");
            CategoryType type = CategoryType.fromString(req.getParameter("type"));
            if (type == null) {res.sendError(400,"Nhóm danh mục không hợp lệ.");return;}
            switch(action == null ? "" : action) {
                case "create", "update" -> {
                    Category data = new Category();
                    data.setType(type);
                    data.setCode(req.getParameter("code"));
                    data.setName(req.getParameter("name"));
                    data.setDescription(req.getParameter("description"));
                    data.setDisplayOrder(Integer.parseInt(req.getParameter("displayOrder")));
                    if (data.getDisplayOrder() < 0) throw new IllegalArgumentException("Thứ tự không hợp lệ.");
                    String rawActive = req.getParameter("active");
                    if (!"true".equals(rawActive) && !"false".equals(rawActive))
                        throw new IllegalArgumentException("Trạng thái không hợp lệ.");
                    data.setActive("true".equals(rawActive));
                    if ("update".equals(action)) {
                        data.setId(ServerForms.positive(req.getParameter("id")));
                        Category previous = service.getCategoryById(data.getId());
                        if (previous == null || previous.getType() != type) {res.sendError(404);return;}
                        service.updateCategory(data);
                        ServerForms.setToast(req, "success", "Cập nhật thành công", "Danh mục đã được lưu.");
                    } else {
                        service.createCategory(data);
                        ServerForms.setToast(req, "success", "Tạo danh mục thành công", "Danh mục mới đã được thêm.");
                    }
                }
                case "delete" -> {
                    long id = ServerForms.positive(req.getParameter("id"));
                    Category previous = service.getCategoryById(id);
                    if (previous == null || previous.getType() != type) {res.sendError(404);return;}
                    if (!"yes".equals(req.getParameter("confirm"))) {res.sendError(400,"Cần xác nhận xóa.");return;}
                    if (!service.deleteCategory(id)) {res.sendError(404);return;}
                    ServerForms.setToast(req, "success", "Xóa danh mục thành công", "Danh mục đã được xóa.");
                }
                default -> {res.sendError(400,"Thao tác không hợp lệ.");return;}
            }
            res.sendRedirect(req.getContextPath()+"/configuration/page?type="+type.name()+"&result=ok");
        } catch (CategoryInUseException e) {res.sendError(409,"Danh mục đang được sử dụng nên không thể xóa.");}
        catch (IllegalArgumentException e) {res.sendError(400,"Dữ liệu danh mục không hợp lệ: "+e.getMessage());}
        catch (SQLException e) {LOG.log(Level.SEVERE,"Cannot save category HTML form",e);res.sendError(500,"Không cập nhật được danh mục.");}
    }
}
