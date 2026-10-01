package com.crm.controller.customfields;

import com.crm.controller.ServerForms;
import com.crm.model.CustomField;
import com.crm.service.customfields.CustomFieldService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** HTML adapter for S2-08. Preserves existing JSON REST controller. */
@WebServlet({"/customfields", "/customfields/page"})
public class CustomFieldPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(CustomFieldPageServlet.class.getName());
    private final CustomFieldService service;
    public CustomFieldPageServlet() { this(new CustomFieldService()); }
    public CustomFieldPageServlet(CustomFieldService service) { this.service = service; }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, true)) return;
        res.setHeader("Cache-Control", "no-store");
        try {
            String entity = entity(req.getParameter("entity"));
            List<CustomField> fields = service.getDefinitions(entity);
            CustomField edit = null;
            if (req.getParameter("edit") != null) {
                edit = service.getDefinition(ServerForms.positive(req.getParameter("edit")));
                if (edit == null || !entity.equals(edit.getEntityType())) { res.sendError(404); return; }
            }
            req.setAttribute("fields", fields);
            req.setAttribute("entity", entity);
            req.setAttribute("editField", edit);
            if ("ok".equals(req.getParameter("result"))) req.setAttribute("notice", "Đã lưu thay đổi.");
            req.getRequestDispatcher("/jsp/customfields/custom-field-list.jsp").forward(req, res);
        } catch (IllegalArgumentException e) { res.sendError(400, "Tham số không hợp lệ."); }
          catch (SQLException e) { LOG.log(Level.SEVERE, "Custom field page load error", e); res.sendError(500); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!ServerForms.authorize(req, res, true)) return;
        if (!ServerForms.checkCsrf(req, res)) return;
        req.setCharacterEncoding("UTF-8");
        try {
            String action = req.getParameter("action");
            String entity = entity(req.getParameter("entity"));
            if ("delete".equals(action)) {
                if (!"yes".equals(req.getParameter("confirm"))) { res.sendError(400,"Xác nhận thao tác."); return; }
                long id = ServerForms.positive(req.getParameter("id"));
                CustomField existing = service.getDefinition(id);
                if (existing == null || !entity.equals(existing.getEntityType())) { res.sendError(404); return; }
                service.deleteDefinition(id);
            } else if ("create".equals(action) || "update".equals(action)) {
                CustomField field = new CustomField();
                field.setEntityType(entity);
                field.setFieldName(req.getParameter("fieldName"));
                field.setFieldLabel(req.getParameter("fieldLabel"));
                field.setFieldType(req.getParameter("fieldType"));
                field.setRequired("on".equals(req.getParameter("required")));
                field.setActive("on".equals(req.getParameter("active")));
                field.setInForm("on".equals(req.getParameter("inForm")));
                field.setInFilter("on".equals(req.getParameter("inFilter")));
                field.setInExport("on".equals(req.getParameter("inExport")));
                field.setSortOrder(Integer.parseInt(req.getParameter("sortOrder")));
                String options = req.getParameter("options");
                List<String> values = new ArrayList<>();
                if (options != null && !options.isBlank()) {
                    Arrays.stream(options.split("\\R")).map(String::trim).filter(s -> !s.isBlank()).forEach(values::add);
                }
                field.setOptions(values);
                if ("update".equals(action)) {
                    long id = ServerForms.positive(req.getParameter("id"));
                    CustomField existing = service.getDefinition(id);
                    if (existing == null || !entity.equals(existing.getEntityType())) { res.sendError(404); return; }
                    // These keys are immutable; preserve authoritative database values.
                    field.setEntityType(existing.getEntityType());
                    field.setFieldName(existing.getFieldName());
                    service.updateDefinition(id, field);
                } else service.createDefinition(field);
            } else { res.sendError(400,"Thao tác không hợp lệ."); return; }
            res.sendRedirect(req.getContextPath() + "/customfields/page?entity=" + entity + "&result=ok");
        } catch (CustomFieldService.NotFoundException e) { res.sendError(404,"Không tìm thấy trường tùy chỉnh."); }
          catch (IllegalArgumentException | IllegalStateException e) { res.sendError(400, e.getMessage()); }
          catch (SQLException e) { LOG.log(Level.SEVERE,"Custom field form update failed",e); res.sendError(500); }
    }
    private static String entity(String raw) {
        String value = raw == null ? "CUSTOMER" : raw.toUpperCase(Locale.ROOT).trim();
        if (!"CUSTOMER".equals(value) && !"OPPORTUNITY".equals(value))
            throw new IllegalArgumentException("Đối tượng không hợp lệ.");
        return value;
    }
}
