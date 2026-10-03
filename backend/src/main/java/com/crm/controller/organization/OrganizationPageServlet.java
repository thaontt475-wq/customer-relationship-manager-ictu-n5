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
import com.crm.dao.teams.UserTeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.User;
import com.crm.util.DBConnection;
import java.io.IOException;
import java.sql.Connection;
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

            boolean isCreateMode = "create".equalsIgnoreCase(req.getParameter("mode"))
                    || "1".equals(req.getParameter("create"));
            if (isCreateMode && !ServerForms.admin(req)) {
                res.sendError(403);
                return;
            }

            Long selectedId = optionalId(req.getParameter("selected"));
            Organization selectedUnit = editUnit;
            if (selectedUnit == null && selectedId != null) {
                for (Organization unit : units) {
                    if (unit.getId() == selectedId) {
                        selectedUnit = unit;
                        break;
                    }
                }
            }

            List<Organization> filtered = units.stream().filter(unit ->
                    (region.isEmpty() || region.equals(unit.getRegion())) &&
                    (q.isEmpty() || (safe(unit.getName()) + " " + safe(unit.getManagerName()))
                            .toLowerCase(Locale.ROOT).contains(q)))
                    .toList();

            if (selectedUnit == null && !filtered.isEmpty() && !isCreateMode) {
                selectedUnit = filtered.get(0);
            }

            List<User> allUsers = List.of();
            try (Connection conn = DBConnection.getConnection()) {
                allUsers = new UserDAO().findAll(conn);
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Unable to load all users for organization page", e);
            }

            req.setAttribute("units", units);
            req.setAttribute("filtered", filtered);
            req.setAttribute("selectedUnit", selectedUnit);
            req.setAttribute("editUnit", editUnit != null ? editUnit : selectedUnit);
            req.setAttribute("isCreateMode", isCreateMode);
            req.setAttribute("allUsers", allUsers);
            req.setAttribute("canManage", ServerForms.admin(req));
            req.setAttribute("keyword", ServerForms.value(req,"q",""));
            req.setAttribute("regionFilter", region);

            String result = req.getParameter("result");
            if ("ok".equals(result) || "updated".equals(result)) req.setAttribute("notice", "Đã lưu cấu hình đơn vị thành công.");
            else if ("created".equals(result)) req.setAttribute("notice", "Đã tạo nhóm kinh doanh mới thành công.");
            else if ("deactivated".equals(result)) req.setAttribute("notice", "Đã giải thể / vô hiệu hóa đơn vị thành công.");
            else if ("member_assigned".equals(result)) req.setAttribute("notice", "Đã phân bổ nhân sự vào nhóm thành công.");
            else if ("member_removed".equals(result)) req.setAttribute("notice", "Đã gỡ nhân sự khỏi nhóm thành công.");

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
            if ("deactivate".equals(action)) {
                long id = ServerForms.positive(req.getParameter("id"));
                service.deactivateUnit(id);
                ServerForms.setToast(req, "success", "Giải thể thành công", "Đơn vị đã được chuyển sang trạng thái ngừng hoạt động.");
                res.sendRedirect(req.getContextPath() + "/organization/page?result=deactivated");
                return;
            }
            if ("assign_member".equals(action)) {
                long unitId = ServerForms.positive(req.getParameter("unitId"));
                long userId = ServerForms.positive(req.getParameter("userId"));
                try (Connection conn = DBConnection.getConnection()) {
                    new UserTeamDAO().assignUserToTeam(conn, userId, unitId);
                }
                ServerForms.setToast(req, "success", "Phân bổ thành công", "Đã thêm nhân sự vào nhóm kinh doanh.");
                res.sendRedirect(req.getContextPath() + "/organization/page?selected=" + unitId + "&result=member_assigned");
                return;
            }
            if ("remove_member".equals(action)) {
                long unitId = ServerForms.positive(req.getParameter("unitId"));
                long userId = ServerForms.positive(req.getParameter("userId"));
                try (Connection conn = DBConnection.getConnection()) {
                    new UserTeamDAO().removeUserFromTeam(conn, userId);
                }
                ServerForms.setToast(req, "success", "Đã gỡ nhân sự", "Nhân sự đã được đưa ra khỏi nhóm.");
                res.sendRedirect(req.getContextPath() + "/organization/page?selected=" + unitId + "&result=member_removed");
                return;
            }
            if (!"create".equals(action) && !"update".equals(action)) {
                res.sendError(400,"Thao tác không hợp lệ."); return;
            }
            String name = req.getParameter("name");
            Long parentId = optionalId(req.getParameter("parentId"));
            Long managerId = optionalId(req.getParameter("managerId"));
            String region = req.getParameter("region");
            boolean active = "true".equals(req.getParameter("active"));
            UnitInput input = new UnitInput(name,parentId,managerId,region,active);
            long targetId;
            if ("update".equals(action)) {
                targetId = ServerForms.positive(req.getParameter("id"));
                service.updateUnit(targetId, input);
                ServerForms.setToast(req, "success", "Cập nhật thành công", "Thông tin đơn vị đã được lưu.");
                res.sendRedirect(req.getContextPath() + "/organization/page?selected=" + targetId + "&result=updated");
            } else {
                Organization created = service.createUnit(input);
                ServerForms.setToast(req, "success", "Tạo nhóm thành công", "Nhóm kinh doanh mới đã được kích hoạt.");
                res.sendRedirect(req.getContextPath() + "/organization/page?selected=" + created.getId() + "&result=created");
            }
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
