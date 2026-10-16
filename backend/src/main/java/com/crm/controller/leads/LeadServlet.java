package com.crm.controller.leads;

import com.crm.dao.leads.LeadAssignmentHistoryDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.ManualAssignRequest;
import com.crm.dto.leads.SalesRepDTO;
import com.crm.model.leads.AssignmentHistory;
import com.crm.model.leads.AssignmentRule;
import com.crm.model.leads.Lead;
import com.crm.service.leads.AssignmentRuleService;
import com.crm.service.leads.LeadAutoAssignmentService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import com.crm.service.permissions.AuthorizationService;

@WebServlet({"/api/leads", "/api/leads/*"})
public class LeadServlet extends HttpServlet {

    private final AssignmentRuleService ruleService;
    private final LeadAutoAssignmentService autoAssignmentService;
    private final LeadDAO leadDAO;
    private final LeadAssignmentHistoryDAO historyDAO;
    private final AuthorizationService authService;

    private static final Type RULE_LIST_TYPE = new TypeToken<List<AssignmentRule>>() {}.getType();

    public LeadServlet() {
        this.ruleService = new AssignmentRuleService();
        this.autoAssignmentService = new LeadAutoAssignmentService();
        this.leadDAO = new LeadDAO();
        this.historyDAO = new LeadAssignmentHistoryDAO();
        this.authService = new AuthorizationService();
    }

    public LeadServlet(AssignmentRuleService ruleService,
                       LeadAutoAssignmentService autoAssignmentService,
                       LeadDAO leadDAO,
                       LeadAssignmentHistoryDAO historyDAO) {
        this.ruleService = ruleService;
        this.autoAssignmentService = autoAssignmentService;
        this.leadDAO = leadDAO;
        this.historyDAO = historyDAO;
        this.authService = new AuthorizationService();
    }

    public LeadServlet(AssignmentRuleService ruleService,
                       LeadAutoAssignmentService autoAssignmentService,
                       LeadDAO leadDAO,
                       LeadAssignmentHistoryDAO historyDAO,
                       AuthorizationService authService) {
        this.ruleService = ruleService;
        this.autoAssignmentService = autoAssignmentService;
        this.leadDAO = leadDAO;
        this.historyDAO = historyDAO;
        this.authService = authService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = normalizePath(request.getPathInfo());

        // 1. Assignment Rules: GET /api/leads/assignment-rules
        if ("/assignment-rules".equals(path)) {
            List<AssignmentRule> rules = ruleService.getAllRules();
            ResponseUtil.json(response, 200, ApiResponse.success("Lấy danh sách quy tắc phân bổ thành công", rules));
            return;
        }

        // 2. Assignment History: GET /api/leads/assignment-history
        if ("/assignment-history".equals(path)) {
            List<AssignmentHistory> history = historyDAO.findAll(100);
            ResponseUtil.json(response, 200, ApiResponse.success("Lấy lịch sử phân bổ thành công", history));
            return;
        }

        // Specific Lead Assignment History: GET /api/leads/{id}/assignment-history
        if (path.endsWith("/assignment-history") && path.length() > "/assignment-history".length()) {
            String leadIdStr = path.substring(1, path.indexOf("/assignment-history"));
            try {
                long leadId = Long.parseLong(leadIdStr);
                List<AssignmentHistory> history = historyDAO.findByLeadId(leadId);
                ResponseUtil.json(response, 200, ApiResponse.success("Lấy lịch sử phân bổ Lead thành công", history));
                return;
            } catch (NumberFormatException ignored) {}
        }

        // 3. Unassigned / Pending Queue: GET /api/leads/queue or GET /api/leads/unassigned
        if ("/queue".equals(path) || "/unassigned".equals(path)) {
            List<Lead> queue = leadDAO.findUnassignedOrPendingQueue();
            ResponseUtil.json(response, 200, ApiResponse.success("Lấy hàng chờ phân bổ thành công", queue));
            return;
        }

        // 4. Sales Reps: GET /api/leads/reps or /sales-reps
        if ("/reps".equals(path) || "/sales-reps".equals(path)) {
            List<SalesRepDTO> reps = leadDAO.getSalesRepsWithWorkload();
            ResponseUtil.json(response, 200, ApiResponse.success("Lấy danh sách chuyên viên kinh doanh thành công", reps));
            return;
        }

        // 5. Health Check
        if ("/health".equals(path)) {
            ResponseUtil.json(response, 200, ApiResponse.success("Lead API service is running", null));
            return;
        }

        // 6. Default: List leads
        try {
            int page = parseInt(request.getParameter("page"), 1);
            int size = parseInt(request.getParameter("size"), 20);
            String status = request.getParameter("status");
            String source = request.getParameter("source");

            List<Lead> leads = leadDAO.findAll(page, size, status, source);
            int total = leadDAO.count(status, source);

            if ("wrapper".equalsIgnoreCase(request.getParameter("format"))) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("items", leads);
                data.put("total", total);
                data.put("page", page);
                data.put("size", size);
                data.put("totalPages", (int) Math.ceil((double) total / size));
                ResponseUtil.json(response, 200, ApiResponse.success("Lấy danh sách Lead thành công", data));
            } else {
                ResponseUtil.json(response, 200, ApiResponse.success("Lấy danh sách Lead thành công", leads));
            }
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Lỗi khi tải danh sách Lead: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = normalizePath(request.getPathInfo());

        // 1. Batch or single save Assignment Rules: POST /api/leads/assignment-rules
        if ("/assignment-rules".equals(path)) {
            handleSaveRules(request, response);
            return;
        }

        // 2. Manual Assign: POST /api/leads/assign
        if ("/assign".equals(path)) {
            handleManualAssign(request, response);
            return;
        }

        // 3. Trigger Auto Assignment on Queue: POST /api/leads/auto-assign or /process-queue
        if ("/auto-assign".equals(path) || "/process-queue".equals(path)) {
            handleProcessQueue(request, response);
            return;
        }

        // 4. Create New Lead: POST /api/leads
        if ("".equals(path) || "/".equals(path)) {
            handleCreateLead(request, response);
            return;
        }

        ResponseUtil.json(response, 404, ApiResponse.error("Endpoint không tồn tại: " + path, null));
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = normalizePath(request.getPathInfo());

        // Update single rule: PUT /api/leads/assignment-rules/{id}
        if (path.startsWith("/assignment-rules/")) {
            try {
                authService.requirePermission(request, "lead.assignment_rules");
            } catch (SecurityException e) {
                int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
                ResponseUtil.json(response, status, ApiResponse.error(e.getMessage(), null));
                return;
            } catch (Exception e) {
                ResponseUtil.json(response, 500, ApiResponse.error("Lỗi xác thực phân quyền: " + e.getMessage(), null));
                return;
            }

            String idStr = path.substring("/assignment-rules/".length());
            try {
                long ruleId = Long.parseLong(idStr);
                String body = readBodyString(request);
                AssignmentRule rule = JsonUtil.getGson().fromJson(body, AssignmentRule.class);
                if (rule != null) {
                    rule.setId(ruleId);
                    boolean ok = ruleService.updateRule(rule);
                    if (ok) {
                        ResponseUtil.json(response, 200, ApiResponse.success("Cập nhật quy tắc phân bổ thành công", rule));
                        return;
                    }
                }
            } catch (Exception e) {
                ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu cập nhật quy tắc không hợp lệ: " + e.getMessage(), null));
                return;
            }
        }

        // Batch update rules: PUT /api/leads/assignment-rules
        if ("/assignment-rules".equals(path)) {
            handleSaveRules(request, response);
            return;
        }

        ResponseUtil.json(response, 404, ApiResponse.error("Endpoint không tồn tại", null));
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = normalizePath(request.getPathInfo());

        if (path.startsWith("/assignment-rules/")) {
            try {
                authService.requirePermission(request, "lead.assignment_rules");
            } catch (SecurityException e) {
                int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
                ResponseUtil.json(response, status, ApiResponse.error(e.getMessage(), null));
                return;
            } catch (Exception e) {
                ResponseUtil.json(response, 500, ApiResponse.error("Lỗi xác thực phân quyền: " + e.getMessage(), null));
                return;
            }

            String idStr = path.substring("/assignment-rules/".length());
            try {
                long ruleId = Long.parseLong(idStr);
                boolean deleted = ruleService.deleteRule(ruleId);
                if (deleted) {
                    ResponseUtil.json(response, 200, ApiResponse.success("Xóa quy tắc phân bổ thành công", null));
                } else {
                    ResponseUtil.json(response, 404, ApiResponse.error("Không tìm thấy quy tắc cần xóa", null));
                }
                return;
            } catch (NumberFormatException e) {
                ResponseUtil.json(response, 400, ApiResponse.error("ID quy tắc không hợp lệ", null));
                return;
            }
        }

        ResponseUtil.json(response, 404, ApiResponse.error("Endpoint không tồn tại", null));
    }

    private void handleSaveRules(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            authService.requirePermission(request, "lead.assignment_rules");
        } catch (SecurityException e) {
            int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
            ResponseUtil.json(response, status, ApiResponse.error(e.getMessage(), null));
            return;
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Lỗi xác thực phân quyền: " + e.getMessage(), null));
            return;
        }

        String body = readBodyString(request);
        if (body == null || body.isBlank()) {
            ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu quy tắc trống", null));
            return;
        }

        try {
            if (body.trim().startsWith("[")) {
                // Batch list of rules
                List<AssignmentRule> rules = JsonUtil.getGson().fromJson(body, RULE_LIST_TYPE);
                List<AssignmentRule> saved = ruleService.batchSaveRules(rules);
                ResponseUtil.json(response, 200, ApiResponse.success("Lưu danh sách quy tắc phân bổ thành công", saved));
            } else {
                // Single rule
                AssignmentRule rule = JsonUtil.getGson().fromJson(body, AssignmentRule.class);
                if (rule.getId() != null && rule.getId() > 0) {
                    ruleService.updateRule(rule);
                    ResponseUtil.json(response, 200, ApiResponse.success("Cập nhật quy tắc phân bổ thành công", rule));
                } else {
                    AssignmentRule created = ruleService.createRule(rule);
                    ResponseUtil.json(response, 201, ApiResponse.success("Tạo quy tắc phân bổ thành công", created));
                }
            }
        } catch (Exception e) {
            ResponseUtil.json(response, 400, ApiResponse.error("Lỗi lưu quy tắc phân bổ: " + e.getMessage(), null));
        }
    }

    private void handleManualAssign(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            authService.requirePermission(request, "lead.assign");
        } catch (SecurityException e) {
            int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
            ResponseUtil.json(response, status, ApiResponse.error(e.getMessage(), null));
            return;
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Lỗi xác thực phân quyền: " + e.getMessage(), null));
            return;
        }

        String body = readBodyString(request);
        ManualAssignRequest req = JsonUtil.getGson().fromJson(body, ManualAssignRequest.class);

        if (req == null) {
            ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu phân bổ không hợp lệ", null));
            return;
        }

        Long leadId = req.getParsedLeadId();
        if (leadId == null) {
            ResponseUtil.json(response, 400, ApiResponse.error("Mã Lead không hợp lệ", null));
            return;
        }

        if (req.getRepId() == null || req.getRepId().isBlank()) {
            ResponseUtil.json(response, 400, ApiResponse.error("Chưa chọn nhân viên phụ trách", null));
            return;
        }

        boolean success = autoAssignmentService.assignLeadManual(leadId, req.getRepId(), req.getNote());
        if (success) {
            ResponseUtil.json(response, 200, ApiResponse.success("Phân công Lead thành công", null));
        } else {
            ResponseUtil.json(response, 500, ApiResponse.error("Không thể phân công Lead", null));
        }
    }

    private void handleProcessQueue(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            authService.requirePermission(request, "lead.auto_assign");
        } catch (SecurityException e) {
            int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
            ResponseUtil.json(response, status, ApiResponse.error(e.getMessage(), null));
            return;
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Lỗi xác thực phân quyền: " + e.getMessage(), null));
            return;
        }

        int count = autoAssignmentService.processPendingQueue();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assignedCount", count);
        result.put("message", count > 0
                ? "Đã tự động phân bổ thành công " + count + " Lead từ hàng chờ tới đội ngũ Sales."
                : "Không có Lead nào khớp với các quy tắc đang bật.");

        ResponseUtil.json(response, 200, ApiResponse.success("Xử lý hàng chờ hoàn tất", result));
    }

    private void handleCreateLead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String body = readBodyString(request);
        Lead lead;

        if (body != null && !body.isBlank()) {
            lead = JsonUtil.getGson().fromJson(body, Lead.class);
        } else {
            lead = new Lead();
            lead.setFullName(request.getParameter("fullName"));
            lead.setEmail(request.getParameter("email"));
            lead.setPhone(request.getParameter("phone"));
            lead.setCompanyName(request.getParameter("companyName"));
            lead.setRegion(request.getParameter("region"));
            lead.setRegionCode(request.getParameter("regionCode"));
            lead.setIndustry(request.getParameter("industry"));
            lead.setNotes(request.getParameter("notes"));
            lead.setSource(request.getParameter("source"));
        }

        if (lead == null || lead.getFullName() == null || lead.getFullName().isBlank()) {
            ResponseUtil.json(response, 400, ApiResponse.error("Họ tên Lead không được để trống", null));
            return;
        }

        // Default attributes
        if (lead.getStatus() == null || lead.getStatus().isBlank()) {
            lead.setStatus("NEW");
        }
        if (lead.getSource() == null || lead.getSource().isBlank()) {
            lead.setSource("WEB_FORM");
        }

        // Insert lead
        long leadId = leadDAO.insert(lead);
        if (leadId <= 0) {
            ResponseUtil.json(response, 500, ApiResponse.error("Không thể lưu Lead vào hệ thống", null));
            return;
        }
        lead.setId(leadId);

        // Auto execute assignment immediately upon lead creation (AC 4)
        boolean assigned = autoAssignmentService.assignLead(lead);

        String message = assigned
                ? "Tạo Lead và phân bổ tự động thành công (Phụ trách: " + lead.getAssignedToRepId() + ")"
                : "Tạo Lead thành công. Lead đã được chuyển vào Hàng chờ (Pending Assignment)";

        ResponseUtil.json(response, 201, ApiResponse.success(message, lead));
    }

    private String normalizePath(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) return "";
        String p = pathInfo.trim();
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private String readBodyString(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
