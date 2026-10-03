package com.crm.controller.scope;

import com.crm.controller.ServerForms;
import com.crm.dao.categories.CategoryDAO;
import com.crm.dao.customfields.CustomFieldDAO;
import com.crm.dao.scope.ScopedEntityDAO;
import com.crm.model.Category;
import com.crm.model.CustomField;
import com.crm.service.customfields.CustomFieldService;
import com.crm.service.scope.*;
import com.crm.util.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Server-rendered entry points for the four scoped modules with Customer Add/Edit and Custom Fields support. */
@WebServlet({"/customers", "/opportunities", "/activities", "/quotes"})
public class ScopedEntityPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(ScopedEntityPageServlet.class.getName());
    private final DataScopeService service;
    private final CustomFieldService customFieldService;
    private final CustomFieldDAO customFieldDAO;
    private final CategoryDAO categoryDAO;

    public ScopedEntityPageServlet() {
        this(new DataScopeService(), new CustomFieldService(), new CustomFieldDAO(), new CategoryDAO());
    }

    public ScopedEntityPageServlet(DataScopeService service) {
        this(service, new CustomFieldService(), new CustomFieldDAO(), new CategoryDAO());
    }

    public ScopedEntityPageServlet(DataScopeService service, CustomFieldService customFieldService,
                                   CustomFieldDAO customFieldDAO, CategoryDAO categoryDAO) {
        this.service = service;
        this.customFieldService = customFieldService;
        this.customFieldDAO = customFieldDAO;
        this.categoryDAO = categoryDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        var type = ScopeEntityType.fromServletPath("/api" + req.getServletPath());
        res.setHeader("Cache-Control", "no-store");
        req.setAttribute("moduleTitle", switch (type) {
            case CUSTOMERS -> "Khách hàng";
            case OPPORTUNITIES -> "Cơ hội";
            case ACTIVITIES -> "Hoạt động";
            case QUOTES -> "Báo giá";
        });

        String action = req.getParameter("action");
        if ("/customers".equals(req.getServletPath()) && ("create".equals(action) || "edit".equals(action))) {
            handleCustomerFormGet(req, res);
            return;
        }

        if (req.getParameter("notice") != null) {
            req.setAttribute("notice", req.getParameter("notice"));
        }

        try {
            if (req.getParameter("id") != null) {
                var result = service.read(ServerForms.actor(req), type, ServerForms.positive(req.getParameter("id")));
                if (result.status() != DataScopeService.ReadStatus.SUCCESS) {
                    res.sendError(result.status() == DataScopeService.ReadStatus.FORBIDDEN ? 403 : 404);
                    return;
                }
                req.setAttribute("record", result.record());
            } else {
                var records = service.list(ServerForms.actor(req), type, req.getParameter("q"));
                int pages = Math.max(1, (records.size() + 19) / 20);
                int page = 1;
                try { page = Math.max(1, Math.min(pages, Integer.parseInt(req.getParameter("page")))); }
                catch (NumberFormatException ignored) { }
                int from = Math.min(records.size(), (page - 1) * 20);
                req.setAttribute("records", records.subList(from, Math.min(records.size(), from + 20)));
                req.setAttribute("pageNumber", page);
                req.setAttribute("pageCount", pages);
            }
            req.getRequestDispatcher("/jsp/shared/scoped-records.jsp").forward(req, res);
        } catch (IllegalArgumentException e) { res.sendError(400); }
        catch (SQLException e) {
            getServletContext().log("Cannot load scoped records", e);
            res.sendError(500);
        }
    }

    private void handleCustomerFormGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String action = req.getParameter("action");
        try (Connection conn = DBConnection.getConnection()) {
            List<CustomField> customFields = customFieldDAO.findActive(conn, "CUSTOMER");
            req.setAttribute("customFields", customFields);

            List<Category> industries = categoryDAO.findAll(conn, "INDUSTRY", null, true);
            List<Category> companySizes = categoryDAO.findAll(conn, "COMPANY_SIZE", null, true);
            req.setAttribute("industries", industries);
            req.setAttribute("companySizes", companySizes);

            if ("edit".equals(action)) {
                long id = ServerForms.positive(req.getParameter("id"));
                var readResult = service.read(ServerForms.actor(req), ScopeEntityType.CUSTOMERS, id);
                if (readResult.status() != DataScopeService.ReadStatus.SUCCESS) {
                    res.sendError(readResult.status() == DataScopeService.ReadStatus.FORBIDDEN ? 403 : 404);
                    return;
                }
                req.setAttribute("record", readResult.record());
                Map<String, String> customFieldValues = customFieldDAO.findValues(conn, "CUSTOMER", id);
                req.setAttribute("customFieldValues", customFieldValues);
            }
            req.getRequestDispatcher("/jsp/customers/customer-form.jsp").forward(req, res);
        } catch (IllegalArgumentException e) {
            res.sendError(400);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error loading customer form", e);
            res.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        if (!ServerForms.checkCsrf(req, res)) return;
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if ("/customers".equals(req.getServletPath())) {
            handleCustomerPost(req, res);
            return;
        }

        res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void handleCustomerPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String action = req.getParameter("action");
        String name = req.getParameter("name");
        Long userId = ServerForms.actor(req);

        Map<String, String> customFieldInputs = new HashMap<>();
        for (String paramName : req.getParameterMap().keySet()) {
            if (paramName.startsWith("cf_")) {
                String fieldName = paramName.substring(3);
                customFieldInputs.put(fieldName, req.getParameter(paramName));
            }
        }

        if (name == null || name.trim().isEmpty()) {
            forwardError(req, res, "Tên doanh nghiệp / Khách hàng không được để trống.", action);
            return;
        }

        if ("create".equals(action)) {
            try {
                ScopeRecord created = service.create(userId, ScopeEntityType.CUSTOMERS, name.trim());
                if (created != null && created.id() > 0 && !customFieldInputs.isEmpty()) {
                    try {
                        customFieldService.saveValues("CUSTOMER", created.id(), customFieldInputs);
                    } catch (Exception cfEx) {
                        try (Connection conn = DBConnection.getConnection()) {
                            new ScopedEntityDAO().delete(conn, ScopeEntityType.CUSTOMERS, created.id());
                        } catch (SQLException ignored) {}
                        throw cfEx;
                    }
                }
                String msg = URLEncoder.encode("Tạo khách hàng thành công.", StandardCharsets.UTF_8);
                res.sendRedirect(req.getContextPath() + "/customers?notice=" + msg);
            } catch (IllegalArgumentException e) {
                forwardError(req, res, e.getMessage(), action);
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Error creating customer", e);
                forwardError(req, res, "Lỗi hệ thống khi tạo khách hàng.", action);
            }
        } else if ("update".equals(action)) {
            try {
                long id = ServerForms.positive(req.getParameter("id"));
                var updateResult = service.update(userId, ScopeEntityType.CUSTOMERS, id, name.trim());
                if (updateResult.status() != DataScopeService.ReadStatus.SUCCESS) {
                    res.sendError(updateResult.status() == DataScopeService.ReadStatus.FORBIDDEN ? 403 : 404);
                    return;
                }
                if (!customFieldInputs.isEmpty()) {
                    customFieldService.saveValues("CUSTOMER", id, customFieldInputs);
                }
                String msg = URLEncoder.encode("Cập nhật khách hàng thành công.", StandardCharsets.UTF_8);
                res.sendRedirect(req.getContextPath() + "/customers?notice=" + msg);
            } catch (IllegalArgumentException e) {
                forwardError(req, res, e.getMessage(), action);
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Error updating customer", e);
                forwardError(req, res, "Lỗi hệ thống khi cập nhật khách hàng.", action);
            }
        } else {
            res.sendError(400, "Hành động không hợp lệ.");
        }
    }

    private void forwardError(HttpServletRequest req, HttpServletResponse res, String errorMessage, String action)
            throws ServletException, IOException {
        req.setAttribute("error", errorMessage);
        try (Connection conn = DBConnection.getConnection()) {
            List<CustomField> customFields = customFieldDAO.findActive(conn, "CUSTOMER");
            req.setAttribute("customFields", customFields);
            List<Category> industries = categoryDAO.findAll(conn, "INDUSTRY", null, true);
            List<Category> companySizes = categoryDAO.findAll(conn, "COMPANY_SIZE", null, true);
            req.setAttribute("industries", industries);
            req.setAttribute("companySizes", companySizes);

            Map<String, String> values = new HashMap<>();
            for (String paramName : req.getParameterMap().keySet()) {
                if (paramName.startsWith("cf_")) {
                    values.put(paramName.substring(3), req.getParameter(paramName));
                }
            }
            req.setAttribute("customFieldValues", values);

            if ("update".equals(action)) {
                try {
                    long id = ServerForms.positive(req.getParameter("id"));
                    var readResult = service.read(ServerForms.actor(req), ScopeEntityType.CUSTOMERS, id);
                    if (readResult.status() == DataScopeService.ReadStatus.SUCCESS) {
                        req.setAttribute("record", readResult.record());
                    }
                } catch (Exception ignored) {}
            }
            req.getRequestDispatcher("/jsp/customers/customer-form.jsp").forward(req, res);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error reloading customer form after failure", e);
            res.sendError(500);
        }
    }
}
