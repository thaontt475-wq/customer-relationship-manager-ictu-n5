package com.crm.controller.scope;

import com.crm.controller.ServerForms;
import com.crm.dao.categories.CategoryDAO;
import com.crm.dao.customfields.CustomFieldDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Category;
import com.crm.model.CustomField;
import com.crm.model.User;
import com.crm.service.scope.*;
import com.crm.util.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Server-rendered entry points for the four scoped modules. */
@WebServlet({"/customers", "/opportunities", "/activities", "/quotes"})
public class ScopedEntityPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(ScopedEntityPageServlet.class.getName());
    private final DataScopeService service;
    private final CustomFieldDAO customFieldDAO;
    private final CategoryDAO categoryDAO;
    private final UserDAO userDAO;

    public ScopedEntityPageServlet() {
        this(new DataScopeService(), new CustomFieldDAO(), new CategoryDAO(), new UserDAO());
    }

    public ScopedEntityPageServlet(DataScopeService service) {
        this(service, new CustomFieldDAO(), new CategoryDAO(), new UserDAO());
    }

    public ScopedEntityPageServlet(DataScopeService service, CustomFieldDAO customFieldDAO,
                                   CategoryDAO categoryDAO, UserDAO userDAO) {
        this.service = service;
        this.customFieldDAO = customFieldDAO != null ? customFieldDAO : new CustomFieldDAO();
        this.categoryDAO = categoryDAO != null ? categoryDAO : new CategoryDAO();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
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

        try {
            if (req.getParameter("id") != null) {
                long recordId = ServerForms.positive(req.getParameter("id"));
                var result = service.read(ServerForms.actor(req), type, recordId);
                if (result.status() != DataScopeService.ReadStatus.SUCCESS) {
                    res.sendError(result.status() == DataScopeService.ReadStatus.FORBIDDEN ? 403 : 404);
                    return;
                }
                req.setAttribute("record", result.record());

                // If viewing Customer detail, route to Customer 360 view with enriched data
                if (type == ScopeEntityType.CUSTOMERS) {
                    handleCustomer360Detail(req, res, result.record());
                    return;
                }
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
        } catch (IllegalArgumentException e) {
            res.sendError(400);
        } catch (SQLException e) {
            getServletContext().log("Cannot load scoped records", e);
            res.sendError(500);
        }
    }

    private void handleCustomer360Detail(HttpServletRequest req, HttpServletResponse res, ScopeRecord customerRecord)
            throws ServletException, IOException, SQLException {
        long customerId = customerRecord.id();
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Load Owner and Team details
            User owner = userDAO.findById(conn, customerRecord.ownerUserId());
            if (owner != null) {
                req.setAttribute("ownerName", owner.getFullName() != null ? owner.getFullName() : owner.getDisplayName());
                req.setAttribute("teamName", owner.getTeamName());
            }

            // 2. Load Industry and Company Size if present on customer table
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT industry_id, company_size_id FROM customers WHERE id = ?")) {
                stmt.setLong(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long indId = rs.getLong("industry_id");
                        if (!rs.wasNull()) {
                            Category ind = categoryDAO.findById(conn, indId);
                            if (ind != null) req.setAttribute("industryName", ind.getName());
                        }
                        long csId = rs.getLong("company_size_id");
                        if (!rs.wasNull()) {
                            Category cs = categoryDAO.findById(conn, csId);
                            if (cs != null) req.setAttribute("companySizeName", cs.getName());
                        }
                    }
                }
            } catch (SQLException ignored) {
                // columns may be optional or null
            }

            // 3. Load Active Custom Fields and Values for CUSTOMER
            List<CustomField> customFields = customFieldDAO.findActive(conn, "CUSTOMER");
            Map<String, String> customFieldValues = customFieldDAO.findValues(conn, "CUSTOMER", customerId);
            req.setAttribute("customFields", customFields);
            req.setAttribute("customFieldValues", customFieldValues);

            // 4. Load Scoped Related Deals / Opportunities
            List<ScopeRecord> relatedDeals = service.list(ServerForms.actor(req), ScopeEntityType.OPPORTUNITIES, null);
            req.setAttribute("relatedDeals", relatedDeals);

            // 5. Load Scoped Related Activities / Timeline
            List<ScopeRecord> relatedActivities = service.list(ServerForms.actor(req), ScopeEntityType.ACTIVITIES, null);
            req.setAttribute("relatedActivities", relatedActivities);

            req.getRequestDispatcher("/jsp/customers/customer-360.jsp").forward(req, res);
        }
    }
}
