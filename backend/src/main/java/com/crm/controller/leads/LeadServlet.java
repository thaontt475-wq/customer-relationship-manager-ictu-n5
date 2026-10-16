package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.*;
import com.crm.model.leads.AssignmentHistory;
import com.crm.model.leads.AssignmentRule;
import com.crm.model.leads.Lead;
import com.crm.model.leads.SavedLeadFilter;
import com.crm.service.importer.BoundedXlsx;
import com.crm.service.leads.*;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.IpRateLimiter;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

@WebServlet({"/api/leads/*", "/api/leads"})
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5500000, fileSizeThreshold = 0)
public class LeadServlet extends HttpServlet {

    private static final Type RULE_LIST_TYPE = new TypeToken<List<AssignmentRule>>() {}.getType();

    private final LeadService leadService;
    private final SavedLeadFilterService savedFilterService;
    private final LeadImportService importService;
    private final AssignmentRuleService ruleService;
    private final LeadAutoAssignmentService autoAssignmentService;
    private final com.crm.dao.leads.LeadDAO leadDAO;
    private final com.crm.dao.leads.LeadAssignmentHistoryDAO historyDAO;
    private final AuthorizationService authService;
    private final IpRateLimiter rateLimiter;
    private final LeadSlaService slaService;
    private final LeadConversionService conversionService;

    public LeadServlet() {
        this(new LeadService(), new SavedLeadFilterService(), new LeadImportService(),
                new AssignmentRuleService(), new LeadAutoAssignmentService(),
                new com.crm.dao.leads.LeadDAO(), new com.crm.dao.leads.LeadAssignmentHistoryDAO(),
                new AuthorizationService(), new IpRateLimiter(5, 60_000),
                new LeadSlaService(), new LeadConversionService());
    }

    public LeadServlet(LeadService leadService, IpRateLimiter rateLimiter) {
        this(leadService != null ? leadService : new LeadService(),
                new SavedLeadFilterService(), new LeadImportService(),
                new AssignmentRuleService(), new LeadAutoAssignmentService(),
                new com.crm.dao.leads.LeadDAO(), new com.crm.dao.leads.LeadAssignmentHistoryDAO(),
                new AuthorizationService(), rateLimiter != null ? rateLimiter : new IpRateLimiter(5, 60_000),
                new LeadSlaService(), new LeadConversionService());
    }

    public LeadServlet(LeadService leadService, SavedLeadFilterService savedFilterService) {
        this(leadService != null ? leadService : new LeadService(),
                savedFilterService != null ? savedFilterService : new SavedLeadFilterService(),
                new LeadImportService(), new AssignmentRuleService(), new LeadAutoAssignmentService(),
                new com.crm.dao.leads.LeadDAO(), new com.crm.dao.leads.LeadAssignmentHistoryDAO(),
                new AuthorizationService(), new IpRateLimiter(5, 60_000),
                new LeadSlaService(), new LeadConversionService());
    }

    public LeadServlet(LeadService leads, LeadImportService imports) {
        this(leads != null ? leads : new LeadService(),
                new SavedLeadFilterService(), imports != null ? imports : new LeadImportService(),
                new AssignmentRuleService(), new LeadAutoAssignmentService(),
                new com.crm.dao.leads.LeadDAO(), new com.crm.dao.leads.LeadAssignmentHistoryDAO(),
                new AuthorizationService(), new IpRateLimiter(5, 60_000),
                new LeadSlaService(), new LeadConversionService());
    }

    public LeadServlet(LeadSlaService slaService) {
        this(new LeadService(), new SavedLeadFilterService(), new LeadImportService(),
                new AssignmentRuleService(), new LeadAutoAssignmentService(),
                new com.crm.dao.leads.LeadDAO(), new com.crm.dao.leads.LeadAssignmentHistoryDAO(),
                new AuthorizationService(), new IpRateLimiter(5, 60_000),
                slaService != null ? slaService : new LeadSlaService(), new LeadConversionService());
    }

    public LeadServlet(AssignmentRuleService ruleService, LeadAutoAssignmentService autoAssignmentService,
                       com.crm.dao.leads.LeadDAO leadDAO, com.crm.dao.leads.LeadAssignmentHistoryDAO historyDAO,
                       AuthorizationService authService) {
        this(new LeadService(), new SavedLeadFilterService(), new LeadImportService(),
                ruleService != null ? ruleService : new AssignmentRuleService(),
                autoAssignmentService != null ? autoAssignmentService : new LeadAutoAssignmentService(),
                leadDAO != null ? leadDAO : new com.crm.dao.leads.LeadDAO(),
                historyDAO != null ? historyDAO : new com.crm.dao.leads.LeadAssignmentHistoryDAO(),
                authService != null ? authService : new AuthorizationService(),
                new IpRateLimiter(5, 60_000), new LeadSlaService(), new LeadConversionService());
    }

    public LeadServlet(LeadService leadService, SavedLeadFilterService savedFilterService, LeadImportService importService,
                       AssignmentRuleService ruleService, LeadAutoAssignmentService autoAssignmentService,
                       com.crm.dao.leads.LeadDAO leadDAO, com.crm.dao.leads.LeadAssignmentHistoryDAO historyDAO,
                       AuthorizationService authService, IpRateLimiter rateLimiter,
                       LeadSlaService slaService, LeadConversionService conversionService) {
        this.leadService = leadService;
        this.savedFilterService = savedFilterService;
        this.importService = importService;
        this.ruleService = ruleService;
        this.autoAssignmentService = autoAssignmentService;
        this.leadDAO = leadDAO;
        this.historyDAO = historyDAO;
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.slaService = slaService;
        this.conversionService = conversionService;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = normalizePath(req.getPathInfo());
        String method = req.getMethod() != null ? req.getMethod().toUpperCase(Locale.ROOT) : "GET";

        // 1. Health check route (Public)
        if ("/health".equals(path)) {
            ResponseUtil.json(resp, 200, ApiResponse.success("Lead API service is running", null));
            return;
        }

        // 2. Web Form Public Lead Submission Route
        if ("POST".equals(method) && ("/web-form".equals(path) || "/web-form/submit".equals(path))) {
            handlePublicWebForm(req, resp);
            return;
        }

        // 3. Fallback check for anonymous POST /api/leads
        HttpSession session = req.getSession(false);
        Long sessionUserId = extractUserId(session);

        if ("POST".equals(method) && (path.isEmpty() || "/".equals(path)) && sessionUserId == null) {
            handlePublicWebForm(req, resp);
            return;
        }

        if (sessionUserId == null) {
            ResponseUtil.json(resp, 401, ApiResponse.error("Chưa đăng nhập", null));
            return;
        }

        try {
            super.service(req, resp);
        } catch (SecurityException e) {
            int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
            ResponseUtil.json(resp, status, ApiResponse.error(e.getMessage(), null));
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = normalizePath(req.getPathInfo());

        try {
            // 1. Saved Filters API: GET /api/leads/saved-filters (AC 3)
            if ("/saved-filters".equals(path)) {
                var filters = savedFilterService.getAllFilters(currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách bộ lọc lưu sẵn thành công", filters));
                return;
            }

            // 2. Statistics API: GET /api/leads/stats
            if ("/stats".equals(path)) {
                var stats = leadService.getLeadStats(currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thống kê Lead thành công", stats));
                return;
            }

            // 3. S4-02: Sources: GET /api/leads/sources
            if ("/sources".equals(path)) {
                ResponseUtil.json(resp, 200, ApiResponse.success("Nguồn Lead", leadService.sources(currentUserId)));
                return;
            }

            // 4. S4-02: Import template: GET /api/leads/import/template
            if ("/import/template".equals(path)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                importService.template(currentUserId, out);
                resp.setHeader("Cache-Control", "no-store");
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition", "attachment; filename=\"lead_import_template.xlsx\"");
                resp.setContentLength(out.size());
                out.writeTo(resp.getOutputStream());
                return;
            }

            // 5. S4-01: Generate Embed Code API: GET /api/leads/embed-code
            if ("/embed-code".equals(path)) {
                handleEmbedCode(req, resp);
                return;
            }

            // 6. S4-06: Assignment Rules: GET /api/leads/assignment-rules
            if ("/assignment-rules".equals(path)) {
                List<AssignmentRule> rules = ruleService.getAllRules();
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách quy tắc phân bổ thành công", rules));
                return;
            }

            // 7. S4-06: Assignment History: GET /api/leads/assignment-history
            if ("/assignment-history".equals(path)) {
                List<AssignmentHistory> history = historyDAO.findAll(100);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy lịch sử phân bổ thành công", history));
                return;
            }

            // Specific Lead Assignment History: GET /api/leads/{id}/assignment-history
            if (path.endsWith("/assignment-history") && path.length() > "/assignment-history".length()) {
                String leadIdStr = path.substring(1, path.indexOf("/assignment-history"));
                try {
                    long leadId = Long.parseLong(leadIdStr);
                    List<AssignmentHistory> history = historyDAO.findByLeadId(leadId);
                    ResponseUtil.json(resp, 200, ApiResponse.success("Lấy lịch sử phân bổ Lead thành công", history));
                    return;
                } catch (NumberFormatException ignored) {}
            }

            // 8. S4-06: Unassigned / Pending Queue: GET /api/leads/queue or GET /api/leads/unassigned
            if ("/queue".equals(path) || "/unassigned".equals(path)) {
                List<Lead> queue = leadDAO.findUnassignedOrPendingQueue();
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy hàng chờ phân bổ thành công", queue));
                return;
            }

            // 9. S4-06: Sales Reps: GET /api/leads/reps or /sales-reps
            if ("/reps".equals(path) || "/sales-reps".equals(path)) {
                List<SalesRepDTO> reps = leadDAO.getSalesRepsWithWorkload();
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách chuyên viên kinh doanh thành công", reps));
                return;
            }

            // 10. S4-07: My Assigned Leads with SLA: GET /api/leads/my-assigned
            if ("/my-assigned".equals(path)) {
                new LeadSlaMonitorService().checkAndNotifyOverdueLeads();
                List<LeadSlaResponseDTO> list = slaService.getMyAssignedLeads(currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách Lead được phân thành công", list));
                return;
            }

            // 11. S4-08: Check Convert Duplicates: GET /api/leads/{id}/check-convert-duplicates or /check-convert-duplicates
            if (path.endsWith("/check-convert-duplicates")) {
                long leadId = parseLeadId(path, "/check-convert-duplicates", req.getParameter("leadId"));
                var checkResult = conversionService.checkConvertDuplicates(currentUserId, leadId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Kiểm tra trùng lặp thành công", checkResult));
                return;
            }

            // 12. Lead Detail API: GET /api/leads/{id}
            if (path.matches("^/\\d+$")) {
                long id = Long.parseLong(path.substring(1));
                Lead lead = leadService.getLeadById(id, currentUserId);
                if (lead != null) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Chi tiết Lead", lead));
                } else {
                    ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy Lead với ID " + id, null));
                }
                return;
            }

            // 13. Advanced Multi-Criteria Search & Filter API: GET /api/leads
            if (path.isEmpty() || "/".equals(path)) {
                LeadFilterRequest filter = parseFilterRequest(req);
                Map<String, Object> result = leadService.searchLeads(filter, currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách Lead thành công", result));
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại: " + path, null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing GET /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi xử lý yêu cầu danh sách Lead: " + e.getMessage(), null));
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = normalizePath(req.getPathInfo());
        String body = readBodyString(req);

        try {
            // 1. Create Saved Filter API: POST /api/leads/saved-filters (AC 3)
            if ("/saved-filters".equals(path)) {
                if (body == null || body.isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu bộ lọc không được để trống", null));
                    return;
                }

                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String name = json.has("name") ? json.get("name").getAsString() : "Bộ lọc chưa đặt tên";
                boolean isDefault = json.has("setAsDefault") && json.get("setAsDefault").getAsBoolean();

                SavedLeadFilter filter = new SavedLeadFilter();
                filter.setName(name);
                filter.setDefault(isDefault);

                if (json.has("filter")) {
                    filter.setFilter(json.get("filter"));
                    filter.setFilterCriteria(json.get("filter").toString());
                } else if (json.has("filterCriteria")) {
                    filter.setFilterCriteria(json.get("filterCriteria").getAsString());
                }

                SavedLeadFilter saved = savedFilterService.saveCustomFilter(filter, currentUserId);
                if (saved != null) {
                    ResponseUtil.json(resp, 201, ApiResponse.success("Lưu bộ lọc thành công", saved));
                } else {
                    ResponseUtil.json(resp, 500, ApiResponse.error("Không thể lưu bộ lọc", null));
                }
                return;
            }

            // 2. S4-02: Import Preview: POST /api/leads/import/preview
            if ("/import/preview".equals(path)) {
                importService.authorize(currentUserId);
                if (req.getContentType() == null || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
                    throw new IllegalArgumentException("Preview yêu cầu multipart/form-data, trường file");
                }
                Part file = null;
                try {
                    file = req.getPart("file");
                    if (file == null || file.getSubmittedFileName() == null || !file.getSubmittedFileName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                        throw new IllegalArgumentException("Chỉ chấp nhận file .xlsx");
                    }
                    if (file.getSize() > BoundedXlsx.MAX_BYTES) {
                        ResponseUtil.json(resp, 413, ApiResponse.error("File Excel tối đa 5MB", null));
                        return;
                    }
                    try (InputStream input = file.getInputStream()) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Preview Lead thành công", importService.preview(currentUserId, input)));
                        return;
                    }
                } finally {
                    if (file != null) file.delete();
                }
            }

            // 3. S4-02: Import Confirm: POST /api/leads/import/confirm
            if ("/import/confirm".equals(path)) {
                importService.authorize(currentUserId);
                JsonObject jsonBody = JsonParser.parseString(body).getAsJsonObject();
                if (!jsonBody.keySet().equals(Set.of("batchToken")) || !jsonBody.get("batchToken").isJsonPrimitive() || !jsonBody.getAsJsonPrimitive("batchToken").isString()) {
                    throw new IllegalArgumentException("Confirm chỉ nhận batchToken dạng chuỗi");
                }
                ResponseUtil.json(resp, 200, ApiResponse.success("Import Lead hoàn tất", importService.confirm(currentUserId, jsonBody.get("batchToken").getAsString())));
                return;
            }

            // 4. S4-06: Assignment Rules Save: POST /api/leads/assignment-rules
            if ("/assignment-rules".equals(path)) {
                handleSaveRules(req, resp, body);
                return;
            }

            // 5. S4-06: Manual Assign: POST /api/leads/assign
            if ("/assign".equals(path)) {
                handleManualAssign(req, resp, body);
                return;
            }

            // 6. S4-06: Auto Assign Queue Trigger: POST /api/leads/auto-assign
            if ("/auto-assign".equals(path)) {
                handleProcessQueue(req, resp);
                return;
            }

            // 7. S4-07: Check SLA Trigger: POST /api/leads/check-sla or /sla/trigger-check
            if ("/check-sla".equals(path) || "/sla/trigger-check".equals(path)) {
                int count = new LeadSlaMonitorService().checkAndNotifyOverdueLeads();
                ResponseUtil.json(resp, 200, ApiResponse.success("Đã kiểm tra SLA và phát thông báo", Map.of("notificationsSent", count)));
                return;
            }

            // 8. S4-07: Accept Lead: POST /api/leads/{id}/accept
            if (path.endsWith("/accept") && path.length() > "/accept".length()) {
                long leadId = parseLeadIdFromPath(path, "/accept");
                LeadSlaResponseDTO result = slaService.acceptLead(currentUserId, leadId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Tiếp nhận Lead thành công. Trạng thái chuyển sang Đang chăm sóc.", result));
                return;
            }

            // 9. S4-07: Reject Lead: POST /api/leads/{id}/reject
            if (path.endsWith("/reject") && path.length() > "/reject".length()) {
                long leadId = parseLeadIdFromPath(path, "/reject");
                LeadRejectRequest rejectReq = null;
                try {
                    rejectReq = JsonUtil.getGson().fromJson(body, LeadRejectRequest.class);
                } catch (Exception ignored) {}

                if (rejectReq == null || rejectReq.getReason() == null || rejectReq.getReason().isBlank()
                        || rejectReq.getDetails() == null || rejectReq.getDetails().isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Lý do từ chối và giải trình chi tiết không được để trống", null));
                    return;
                }

                LeadSlaResponseDTO result = slaService.rejectLead(currentUserId, leadId, rejectReq.getReason().trim(), rejectReq.getDetails().trim());
                ResponseUtil.json(resp, 200, ApiResponse.success("Từ chối Lead thành công. Lead đã được chuyển về hàng chờ phân bổ.", result));
                return;
            }

            // 10. S4-08: Lead Conversion: POST /api/leads/{id}/convert or /convert
            if (path.endsWith("/convert")) {
                long leadId = parseLeadId(path, "/convert", req.getParameter("leadId"));
                JsonObject bodyJson = (body != null && !body.isBlank()) ? JsonParser.parseString(body).getAsJsonObject() : new JsonObject();
                if (leadId <= 0 && bodyJson.has("leadId") && !bodyJson.get("leadId").isJsonNull()) {
                    leadId = bodyJson.get("leadId").getAsLong();
                }
                LeadConversionRequest request = JsonUtil.getGson().fromJson(bodyJson, LeadConversionRequest.class);
                var convertResult = conversionService.convert(currentUserId, leadId, request);
                ResponseUtil.json(resp, 200, ApiResponse.success("Chuyển đổi Lead thành công", convertResult));
                return;
            }

            // 11. Standard Create Lead API: POST /api/leads
            if (path.isEmpty() || "/".equals(path)) {
                if (body == null || body.isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu Lead không được để trống", null));
                    return;
                }

                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                // If S4-02 LeadWriteRequest (contains sourceId)
                if (json.has("sourceId") && !json.get("sourceId").isJsonNull()) {
                    leadService.authorizeCreate(currentUserId);
                    var createdMap = leadService.create(currentUserId, LeadWriteRequest.fromJson(json));
                    ResponseUtil.json(resp, 201, ApiResponse.success("Tạo Lead thành công", createdMap));
                    return;
                }

                // Standard full Lead entity creation with auto-assignment
                Lead lead = JsonUtil.getGson().fromJson(body, Lead.class);
                if (lead == null || lead.getFullName() == null || lead.getFullName().isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Họ và tên khách hàng là bắt buộc", null));
                    return;
                }

                Lead created = leadService.createLead(lead, currentUserId);
                if (created != null) {
                    try {
                        autoAssignmentService.assignLead(created);
                    } catch (Exception e) {
                        System.err.println("Warning: Auto-assignment evaluation failed: " + e.getMessage());
                    }
                    ResponseUtil.json(resp, 201, ApiResponse.success("Tạo Lead thành công", created));
                } else {
                    ResponseUtil.json(resp, 500, ApiResponse.error("Không thể tạo Lead", null));
                }
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại: " + path, null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
            ResponseUtil.json(resp, status, ApiResponse.error(e.getMessage(), null));
        } catch (LeadValidationException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), Map.of("errors", e.errors())));
        } catch (JsonSyntaxException | IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu yêu cầu không hợp lệ: " + e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing POST /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi tạo dữ liệu: " + e.getMessage(), null));
        }
    }

    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = normalizePath(req.getPathInfo());
        String body = readBodyString(req);

        try {
            // 1. S4-06: Update Assignment Rule: PUT /api/leads/assignment-rules/{id}
            if (path.startsWith("/assignment-rules/")) {
                try {
                    authService.requirePermission(req, "lead.assignment_rules");
                } catch (SecurityException e) {
                    int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
                    ResponseUtil.json(resp, status, ApiResponse.error(e.getMessage(), null));
                    return;
                }
                String idPart = path.substring("/assignment-rules/".length());
                long ruleId = parseIdSafe(idPart);
                AssignmentRule rule = JsonUtil.getGson().fromJson(body, AssignmentRule.class);
                rule.setId(ruleId);
                ruleService.updateRule(rule);
                ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật quy tắc phân bổ thành công", rule));
                return;
            }

            // 2. Set Default Filter API: PUT /api/leads/saved-filters/{id}/default
            if (path.startsWith("/saved-filters/") && path.endsWith("/default")) {
                String idPart = path.substring("/saved-filters/".length(), path.indexOf("/default"));
                long filterId = parseIdSafe(idPart);
                boolean success = savedFilterService.setDefaultFilter(filterId, currentUserId);
                if (success) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Đặt bộ lọc mặc định thành công", null));
                } else {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Không thể đặt bộ lọc mặc định", null));
                }
                return;
            }

            // 3. Update Saved Filter API: PUT /api/leads/saved-filters/{id}
            if (path.startsWith("/saved-filters/")) {
                String idPart = path.substring("/saved-filters/".length());
                long filterId = parseIdSafe(idPart);

                SavedLeadFilter filter = JsonUtil.getGson().fromJson(body, SavedLeadFilter.class);
                filter.setId(filterId);
                boolean updated = savedFilterService.updateCustomFilter(filter, currentUserId);
                if (updated) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật bộ lọc thành công", filter));
                } else {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Không thể cập nhật bộ lọc", null));
                }
                return;
            }

            // 4. Update Lead API: PUT /api/leads/{id}
            if (path.matches("^/\\d+$")) {
                String idPart = path.substring(1);
                long leadId = parseIdSafe(idPart);
                if (leadId > 0) {
                    Lead lead = JsonUtil.getGson().fromJson(body, Lead.class);
                    lead.setId(leadId);
                    boolean updated = leadService.updateLead(lead, currentUserId);
                    if (updated) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật Lead thành công", lead));
                    } else {
                        ResponseUtil.json(resp, 400, ApiResponse.error("Không thể cập nhật Lead", null));
                    }
                    return;
                }
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại: " + path, null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing PUT /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi cập nhật dữ liệu: " + e.getMessage(), null));
        }
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = normalizePath(req.getPathInfo());

        try {
            // 1. S4-06: Delete Assignment Rule: DELETE /api/leads/assignment-rules/{id}
            if (path.startsWith("/assignment-rules/")) {
                try {
                    authService.requirePermission(req, "lead.assignment_rules");
                } catch (SecurityException e) {
                    int status = e.getMessage() != null && e.getMessage().contains("Phiên") ? 401 : 403;
                    ResponseUtil.json(resp, status, ApiResponse.error(e.getMessage(), null));
                    return;
                }
                String idPart = path.substring("/assignment-rules/".length());
                long ruleId = parseIdSafe(idPart);
                ruleService.deleteRule(ruleId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Xóa quy tắc phân bổ thành công", null));
                return;
            }

            // 2. Delete Saved Filter API: DELETE /api/leads/saved-filters/{id} (AC 3)
            if (path.startsWith("/saved-filters/")) {
                String idPart = path.substring("/saved-filters/".length());
                long filterId = parseIdSafe(idPart);
                boolean deleted = savedFilterService.deleteCustomFilter(filterId, currentUserId);
                if (deleted) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Xóa bộ lọc thành công", null));
                } else {
                    ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy bộ lọc để xóa", null));
                }
                return;
            }

            // 3. Delete Lead API: DELETE /api/leads/{id}
            if (path.matches("^/\\d+$")) {
                String idPart = path.substring(1);
                long leadId = parseIdSafe(idPart);
                if (leadId > 0) {
                    boolean deleted = leadService.deleteLead(leadId, currentUserId);
                    if (deleted) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Xóa Lead thành công", null));
                    } else {
                        ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy Lead để xóa", null));
                    }
                    return;
                }
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại: " + path, null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing DELETE /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi xóa dữ liệu: " + e.getMessage(), null));
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private void handlePublicWebForm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String clientIp = IpRateLimiter.extractClientIp(req);
        if (rateLimiter != null && !rateLimiter.tryAcquire(clientIp)) {
            resp.setHeader("Retry-After", "60");
            ResponseUtil.json(resp, 429, ApiResponse.error(
                    "Quá nhiều yêu cầu từ địa chỉ IP của bạn. Vui lòng thử lại sau 1 phút.", null));
            return;
        }

        try {
            WebFormLeadRequest leadRequest = parseWebFormRequestBody(req);
            if (leadRequest == null) {
                ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu gửi lên không hợp lệ.", null));
                return;
            }

            Lead created = leadService.createWebFormLead(leadRequest, clientIp);
            ResponseUtil.json(resp, 201, ApiResponse.success("Tiếp nhận thông tin Lead thành công", created));

        } catch (JsonParseException | IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing lead web-form submission: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Không thể lưu Lead. Vui lòng thử lại.", null));
        }
    }

    private void handleEmbedCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String formId = request.getParameter("formId");
        String theme = request.getParameter("theme");
        String title = request.getParameter("title");

        String baseUrl = resolveBaseUrl(request);
        EmbedCodeResponse embedCode = leadService.generateEmbedCode(formId, baseUrl, theme, title);

        ResponseUtil.json(response, 200, ApiResponse.success("Tạo mã nhúng biểu mẫu thành công", embedCode));
    }

    private void handleSaveRules(HttpServletRequest request, HttpServletResponse response, String body) throws IOException {
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

        if (body == null || body.isBlank()) {
            ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu quy tắc trống", null));
            return;
        }

        try {
            if (body.trim().startsWith("[")) {
                List<AssignmentRule> rules = JsonUtil.getGson().fromJson(body, RULE_LIST_TYPE);
                List<AssignmentRule> saved = ruleService.batchSaveRules(rules);
                ResponseUtil.json(response, 200, ApiResponse.success("Lưu danh sách quy tắc phân bổ thành công", saved));
            } else {
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

    private void handleManualAssign(HttpServletRequest request, HttpServletResponse response, String body) throws IOException {
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

    private WebFormLeadRequest parseWebFormRequestBody(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            if (sb.length() == 0) return null;
            return JsonUtil.getGson().fromJson(sb.toString(), WebFormLeadRequest.class);
        }

        WebFormLeadRequest req = new WebFormLeadRequest();
        req.setFullName(request.getParameter("fullName"));
        req.setEmail(request.getParameter("email"));
        req.setPhone(request.getParameter("phone"));
        req.setCompany(request.getParameter("company"));
        req.setCompanyName(request.getParameter("companyName"));
        req.setInterest(request.getParameter("interest"));
        req.setNote(request.getParameter("note"));
        req.setNotes(request.getParameter("notes"));
        req.setSource(request.getParameter("source"));
        req.setFormId(request.getParameter("formId"));
        req.setUtmSource(request.getParameter("utm_source"));
        req.setUtmMedium(request.getParameter("utm_medium"));
        req.setUtmCampaign(request.getParameter("utm_campaign"));
        return req;
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String envBase = System.getenv("CRM_BACKEND_PUBLIC_URL");
        if (envBase != null && !envBase.isBlank()) {
            return envBase.trim().replaceAll("/+$", "");
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            scheme = forwardedProto.split(",")[0].trim();
        }
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            host = forwardedHost.split(",")[0].trim();
            if (host.contains(":")) {
                String[] parts = host.split(":");
                host = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            }
        }

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(host);
        if (("http".equalsIgnoreCase(scheme) && port != 80) ||
                ("https".equalsIgnoreCase(scheme) && port != 443)) {
            url.append(":").append(port);
        }
        if (contextPath != null && !contextPath.isBlank() && !"/".equals(contextPath)) {
            url.append(contextPath);
        }
        return url.toString();
    }

    private LeadFilterRequest parseFilterRequest(HttpServletRequest req) {
        LeadFilterRequest f = new LeadFilterRequest();

        String search = req.getParameter("search");
        if (search == null || search.isBlank()) search = req.getParameter("keyword");
        if (search != null) f.setKeyword(search);

        String status = req.getParameter("status");
        if (status != null && !status.isBlank()) f.setStatus(status);

        String tier = req.getParameter("tier");
        if (tier == null || tier.isBlank()) tier = req.getParameter("rating");
        if (tier != null && !tier.isBlank()) f.setTier(tier);

        String source = req.getParameter("source");
        if (source != null && !source.isBlank()) f.setSource(source);

        String owner = req.getParameter("owner");
        if (owner != null && !owner.isBlank()) f.setOwner(owner);

        String ownerIdStr = req.getParameter("ownerId");
        if (ownerIdStr != null && !ownerIdStr.isBlank()) {
            try {
                f.setOwnerId(Long.parseLong(ownerIdStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String from = req.getParameter("effectiveFrom");
        if (from == null || from.isBlank()) from = req.getParameter("fromDate");
        if (from != null && !from.isBlank()) f.setEffectiveFrom(from);

        String to = req.getParameter("effectiveTo");
        if (to == null || to.isBlank()) to = req.getParameter("toDate");
        if (to != null && !to.isBlank()) f.setEffectiveTo(to);

        String dateRange = req.getParameter("dateRange");
        if (dateRange != null && !dateRange.isBlank()) f.setDateRange(dateRange);

        String sort = req.getParameter("sort");
        if (sort != null && !sort.isBlank()) f.setSort(sort);

        String slaOverdueStr = req.getParameter("slaOverdueOnly");
        if (slaOverdueStr != null && ("true".equalsIgnoreCase(slaOverdueStr) || "1".equals(slaOverdueStr))) {
            f.setSlaOverdueOnly(true);
        }

        String pageStr = req.getParameter("page");
        if (pageStr != null && !pageStr.isBlank()) {
            try {
                f.setPage(Math.max(1, Integer.parseInt(pageStr.trim())));
            } catch (NumberFormatException ignored) {}
        }

        String sizeStr = req.getParameter("size");
        if (sizeStr != null && !sizeStr.isBlank()) {
            try {
                f.setSize(Math.min(100, Math.max(1, Integer.parseInt(sizeStr.trim()))));
            } catch (NumberFormatException ignored) {}
        }

        return f;
    }

    private long parseLeadIdFromPath(String path, String suffix) {
        String sub = path.substring(0, path.length() - suffix.length());
        while (sub.startsWith("/")) sub = sub.substring(1);
        while (sub.endsWith("/")) sub = sub.substring(0, sub.length() - 1);
        if (sub.toUpperCase().startsWith("LD-SLA-")) {
            sub = sub.substring(7);
        } else if (sub.toUpperCase().startsWith("LD-")) {
            sub = sub.substring(3);
        } else if (sub.toUpperCase().startsWith("LEAD-")) {
            sub = sub.substring(5);
        }
        try {
            return Long.parseLong(sub);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mã Lead không hợp lệ: " + sub);
        }
    }

    private long parseLeadId(String path, String suffix, String fallbackParam) {
        if (fallbackParam != null && !fallbackParam.isBlank()) {
            try {
                return Long.parseLong(fallbackParam.trim());
            } catch (NumberFormatException ignored) {}
        }
        return parseLeadIdFromPath(path, suffix);
    }

    private String normalizePath(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) return "";
        String p = pathInfo.trim();
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private Long extractUserId(HttpSession session) {
        if (session != null && session.getAttribute("userId") instanceof Number id) {
            return id.longValue();
        }
        return null;
    }

    private long requireUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Long uid = extractUserId(session);
        if (uid != null) {
            return uid;
        }
        throw new SecurityException("Chưa đăng nhập");
    }

    private long parseIdSafe(String idPart) {
        try {
            return Long.parseLong(idPart.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return -1L;
        }
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
}
