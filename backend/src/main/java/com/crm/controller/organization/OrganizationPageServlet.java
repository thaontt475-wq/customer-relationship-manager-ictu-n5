package com.crm.controller.organization;

import com.crm.controller.ServerForms;
import com.crm.model.Organization;
import com.crm.service.organization.OrganizationService;
import com.crm.service.organization.OrganizationService.OrganizationException;
import com.crm.service.organization.OrganizationService.UnitInput;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Server-rendered S2-06; existing /api/organization/units remains unchanged. */
@WebServlet({"/organization", "/organization/page"})
public class OrganizationPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(OrganizationPageServlet.class.getName());
    private final OrganizationService service;
    public OrganizationPageServlet() { this(new OrganizationService()); }
    public OrganizationPageServlet(OrganizationService service) { this.service = service; }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        res.setHeader("Cache-Control", "no-store");
        try {
            List<Organization> units = service.getUnits();
            String q = ServerForms.value(req,"q","").toLowerCase(Locale.ROOT);
            String region = ServerForms.value(req,"region","");
            Long editId = optionalId(req.getParameter("edit"));
            Organization editUnit = null;
            if (editId != null) {
                if (!ServerForms.admin(req)) { res.sendError(403); return; }
                for (Organization unit : units) if (unit.getId() == editId) editUnit = unit;
                if (editUnit == null) { res.sendError(404); return; }
            }
            List<Organization> filtered = units.stream().filter(unit ->
                    (region.isEmpty() || region.equals(unit.getRegion())) &&
                    (q.isEmpty() || (safe(unit.getName()) + " " + safe(unit.getManagerName()))
                            .toLowerCase(Locale.ROOT).contains(q)))
                    .toList();
            req.setAttribute("units", units);
            req.setAttribute("filtered", filtered);
            req.setAttribute("editUnit", editUnit);
            req.setAttribute("canManage", ServerForms.admin(req));
            req.setAttribute("keyword", ServerForms.value(req,"q",""));
            req.setAttribute("regionFilter", region);
            if ("ok".equals(req.getParameter("result"))) req.setAttribute("notice", "Đã lưu đơn vị thành công.");
            req.getRequestDispatcher("/jsp/organization/organization.jsp").forward(req, res);
        } catch (IllegalArgumentException e) {
            res.sendError(400,"Tham số không hợp lệ.");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE,"Cannot render organization HTML page",e);
            res.sendError(500,"Không tải được cơ cấu tổ chức.");
        }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ServerForms.authorize(req,res,true)) return;
        if (!ServerForms.checkCsrf(req,res)) return;
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        try {
            if (!"create".equals(action) && !"update".equals(action)) {
                res.sendError(400,"Thao tác không hợp lệ."); return;
            }
            String name = req.getParameter("name");
            Long parentId = optionalId(req.getParameter("parentId"));
            Long managerId = optionalId(req.getParameter("managerId"));
            String region = req.getParameter("region");
            boolean active = "true".equals(req.getParameter("active"));
            UnitInput input = new UnitInput(name,parentId,managerId,region,active);
            if ("update".equals(action)) {
                service.updateUnit(ServerForms.positive(req.getParameter("id")),input);
            } else {
                service.createUnit(input);
            }
            res.sendRedirect(req.getContextPath() + "/organization/page?result=ok");
        } catch (IllegalArgumentException e) {
            res.sendError(400,"Dữ liệu không hợp lệ.");
        } catch (OrganizationException e) {
            // No user-entered data is rendered; service supplies a localized domain message.
            res.sendError(400,e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE,"Cannot save organization HTML form",e);
            res.sendError(500,"Không lưu được đơn vị.");
        }
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static Long optionalId(String value) {
        return value == null || value.isBlank() ? null : ServerForms.positive(value);
    }
}
