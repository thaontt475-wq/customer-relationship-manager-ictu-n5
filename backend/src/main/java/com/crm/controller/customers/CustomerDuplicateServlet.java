package com.crm.controller.customers;

import com.crm.controller.ServerForms;
import com.crm.model.CustomField;
import com.crm.dao.customfields.CustomFieldDAO;
import com.crm.service.customers.CustomerDuplicateService;
import com.crm.service.customers.CustomerDuplicateService.CustomerDetails;
import com.crm.service.customers.CustomerDuplicateService.DuplicateGroup;
import com.crm.service.customers.CustomerDuplicateService.MergeResult;
import com.crm.service.customers.CustomerDuplicateService.MergeStatus;
import com.crm.util.DBConnection;
import com.crm.util.Html;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-rendered controller for detecting duplicate customers, comparing two records, and merging them.
 */
@WebServlet({"/customers/duplicates", "/customers/duplicates/compare", "/customers/duplicates/merge"})
public class CustomerDuplicateServlet extends HttpServlet {

    private final CustomerDuplicateService duplicateService;
    private final CustomFieldDAO customFieldDAO;

    public CustomerDuplicateServlet() {
        this(new CustomerDuplicateService(), new CustomFieldDAO());
    }

    public CustomerDuplicateServlet(CustomerDuplicateService duplicateService, CustomFieldDAO customFieldDAO) {
        this.duplicateService = duplicateService != null ? duplicateService : new CustomerDuplicateService();
        this.customFieldDAO = customFieldDAO != null ? customFieldDAO : new CustomFieldDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        res.setHeader("Cache-Control", "no-store");

        String path = req.getServletPath();
        Long actor = ServerForms.actor(req);

        try {
            if ("/customers/duplicates/compare".equals(path)) {
                handleCompareView(req, res, actor);
            } else {
                handleListView(req, res, actor);
            }
        } catch (SQLException e) {
            getServletContext().log("Database error in CustomerDuplicateServlet", e);
            res.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        if (!ServerForms.checkCsrf(req, res)) return;

        String path = req.getServletPath();
        Long actor = ServerForms.actor(req);

        if ("/customers/duplicates/merge".equals(path)) {
            handleMergeSubmit(req, res, actor);
        } else {
            res.sendError(405);
        }
    }

    private void handleListView(HttpServletRequest req, HttpServletResponse res, Long actor)
            throws ServletException, IOException, SQLException {
        String q = req.getParameter("q");
        List<DuplicateGroup> duplicateGroups = duplicateService.findDuplicates(actor, q);
        req.setAttribute("duplicateGroups", duplicateGroups);
        req.setAttribute("query", q != null ? q : "");
        req.getRequestDispatcher("/jsp/customers/customer-duplicates.jsp").forward(req, res);
    }

    private void handleCompareView(HttpServletRequest req, HttpServletResponse res, Long actor)
            throws ServletException, IOException, SQLException {
        String id1Str = req.getParameter("id1");
        String id2Str = req.getParameter("id2");
        if (id1Str == null || id2Str == null) {
            res.sendRedirect(req.getContextPath() + "/customers/duplicates");
            return;
        }

        long id1 = ServerForms.positive(id1Str);
        long id2 = ServerForms.positive(id2Str);

        CustomerDetails c1 = duplicateService.getCustomerDetails(actor, id1);
        CustomerDetails c2 = duplicateService.getCustomerDetails(actor, id2);

        if (c1 == null || c2 == null) {
            req.setAttribute("errorMessage", "Không tìm thấy một trong hai khách hàng hoặc ngoài phạm vi dữ liệu.");
            handleListView(req, res, actor);
            return;
        }

        List<CustomField> customFields = List.of();
        try (Connection conn = DBConnection.getConnection()) {
            customFields = customFieldDAO.findAll(conn, "customers");
        }

        req.setAttribute("c1", c1);
        req.setAttribute("c2", c2);
        req.setAttribute("customFields", customFields);
        req.getRequestDispatcher("/jsp/customers/customer-compare.jsp").forward(req, res);
    }

    private void handleMergeSubmit(HttpServletRequest req, HttpServletResponse res, Long actor)
            throws ServletException, IOException {
        String masterIdStr = req.getParameter("masterId");
        String duplicateIdStr = req.getParameter("duplicateId");

        if (masterIdStr == null || duplicateIdStr == null) {
            res.sendError(400);
            return;
        }

        try {
            long masterId = ServerForms.positive(masterIdStr);
            long duplicateId = ServerForms.positive(duplicateIdStr);

            Map<String, String> overrideValues = new HashMap<>();
            List<CustomField> fields = List.of();
            try (Connection conn = DBConnection.getConnection()) {
                fields = customFieldDAO.findAll(conn, "customers");
            }
            for (CustomField field : fields) {
                String chosenValue = req.getParameter("field_" + field.getFieldName());
                if (chosenValue != null && !chosenValue.isBlank()) {
                    overrideValues.put(field.getFieldName(), chosenValue.trim());
                }
            }

            MergeResult result = duplicateService.mergeCustomers(actor, masterId, duplicateId, overrideValues);

            if (result.status() == MergeStatus.SUCCESS) {
                req.getSession().setAttribute("flashSuccess", result.message());
                res.sendRedirect(req.getContextPath() + "/customers?id=" + result.masterId());
            } else if (result.status() == MergeStatus.FORBIDDEN) {
                res.sendError(403);
            } else if (result.status() == MergeStatus.NOT_FOUND) {
                res.sendError(404);
            } else {
                req.setAttribute("errorMessage", result.message());
                handleCompareView(req, res, actor);
            }
        } catch (IllegalArgumentException e) {
            res.sendError(400);
        } catch (SQLException e) {
            getServletContext().log("Merge failed with SQL exception", e);
            res.sendError(500);
        }
    }
}
