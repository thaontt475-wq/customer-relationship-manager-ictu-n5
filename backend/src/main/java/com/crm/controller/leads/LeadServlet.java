package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.*;
import com.crm.model.leads.AssignmentHistory;
import com.crm.model.leads.AssignmentRule;
import com.crm.model.leads.Lead;
import com.crm.model.leads.SavedLeadFilter;
import com.crm.service.importer.BoundedXlsx;
import com.crm.service.leads.*;
import com.crm.dao.leads.LeadAssignmentHistoryDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.service.permissions.AuthorizationService;
import com.crm.service.permissions.DataScopeContext;
import com.crm.util.IpRateLimiter;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

@WebServlet({"/api/leads/*", "/api/leads"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class LeadServlet extends HttpServlet {

    public interface ScopeResolver {
        DataScopeContext resolve(long user, String action) throws Exception;
    }

    private final LeadService leads;
    private final LeadImportService imports;
    private final IpRateLimiter rateLimiter;
    private final LeadDAO dao;

    private final AssignmentRuleService ruleService;
    private final LeadAutoAssignmentService autoAssignmentService;
    private final LeadAssignmentHistoryDAO historyDAO;
    private final AuthorizationService authService;
    private final LeadSlaService slaService;
    private final LeadConversionService conversionService;
    private final SavedLeadFilterService savedFilterService;

    public LeadServlet() {
        this(new LeadService(), new LeadImportService(), new IpRateLimiter(5, 60_000),
                new AssignmentRuleService(), new LeadAutoAssignmentService(), new LeadDAO(),
                new LeadAssignmentHistoryDAO(), new AuthorizationService(), new LeadSlaService(),
                new LeadConversionService(), new SavedLeadFilterService());
    }

    public LeadServlet(LeadConversionService conversionService) {
        this(new LeadService(), new LeadImportService(), new IpRateLimiter(5, 60_000),
                new AssignmentRuleService(), new LeadAutoAssignmentService(), new LeadDAO(),
                new LeadAssignmentHistoryDAO(), new AuthorizationService(), new LeadSlaService(),
                conversionService != null ? conversionService : new LeadConversionService(),
                new SavedLeadFilterService());
    }

    public LeadServlet(LeadSlaService slaService) {
        this(new LeadService(), new LeadImportService(), new IpRateLimiter(5, 60_000),
                new AssignmentRuleService(), new LeadAutoAssignmentService(), new LeadDAO(),
                new LeadAssignmentHistoryDAO(), new AuthorizationService(),
                slaService != null ? slaService : new LeadSlaService(), new LeadConversionService(),
                new SavedLeadFilterService());
    }

    public LeadServlet(LeadService leads, LeadImportService imports) {
        this(leads != null ? leads : new LeadService(),
                imports != null ? imports : new LeadImportService(),
                new IpRateLimiter(5, 60_000),
                new AssignmentRuleService(), new LeadAutoAssignmentService(), new LeadDAO(),
                new LeadAssignmentHistoryDAO(), new AuthorizationService(), new LeadSlaService(),
                new LeadConversionService(), new SavedLeadFilterService());
    }

    public LeadServlet(LeadService leads, SavedLeadFilterService savedFilterService) {
        this(leads != null ? leads : new LeadService(),
                new LeadImportService(),
                new IpRateLimiter(5, 60_000),
                new AssignmentRuleService(), new LeadAutoAssignmentService(), new LeadDAO(),
                new LeadAssignmentHistoryDAO(), new AuthorizationService(), new LeadSlaService(),
                new LeadConversionService(),
                savedFilterService != null ? savedFilterService : new SavedLeadFilterService());
    }

    public LeadServlet(LeadService leads, IpRateLimiter rateLimiter) {
        this(leads != null ? leads : new LeadService(),
                new LeadImportService(),
                rateLimiter != null ? rateLimiter : new IpRateLimiter(5, 60_000),
                new AssignmentRuleService(), new LeadAutoAssignmentService(), new LeadDAO(),
                new LeadAssignmentHistoryDAO(), new AuthorizationService(), new LeadSlaService(),
                new LeadConversionService(), new SavedLeadFilterService());
    }

    public LeadServlet(LeadService leads, LeadImportService imports, IpRateLimiter rateLimiter) {
        this(leads != null ? leads : new LeadService(),
                imports != null ? imports : new LeadImportService(),
                rateLimiter != null ? rateLimiter : new IpRateLimiter(5, 60_000),
                new AssignmentRuleService(), new LeadAutoAssignmentService(), new LeadDAO(),
                new LeadAssignmentHistoryDAO(), new AuthorizationService(), new LeadSlaService(),
                new LeadConversionService(), new SavedLeadFilterService());
    }

    public LeadServlet(AssignmentRuleService ruleService,
                       LeadAutoAssignmentService autoAssignmentService,
                       LeadDAO leadDAO,
                       LeadAssignmentHistoryDAO historyDAO) {
        this(new LeadService(), new LeadImportService(), new IpRateLimiter(5, 60_000),
                ruleService != null ? ruleService : new AssignmentRuleService(),
                autoAssignmentService != null ? autoAssignmentService : new LeadAutoAssignmentService(),
                leadDAO != null ? leadDAO : new LeadDAO(),
                historyDAO != null ? historyDAO : new LeadAssignmentHistoryDAO(),
                new AuthorizationService(), new LeadSlaService(),
                new LeadConversionService(), new SavedLeadFilterService());
    }

    public LeadServlet(AssignmentRuleService ruleService,
                       LeadAutoAssignmentService autoAssignmentService,
                       LeadDAO leadDAO,
                       LeadAssignmentHistoryDAO historyDAO,
                       AuthorizationService authService) {
        this(new LeadService(), new LeadImportService(), new IpRateLimiter(5, 60_000),
                ruleService != null ? ruleService : new AssignmentRuleService(),
                autoAssignmentService != null ? autoAssignmentService : new LeadAutoAssignmentService(),
                leadDAO != null ? leadDAO : new LeadDAO(),
                historyDAO != null ? historyDAO : new LeadAssignmentHistoryDAO(),
                authService != null ? authService : new AuthorizationService(), new LeadSlaService(),
                new LeadConversionService(), new SavedLeadFilterService());
    }

    public LeadServlet(LeadService leads, LeadImportService imports, IpRateLimiter rateLimiter,
                       AssignmentRuleService ruleService, LeadAutoAssignmentService autoAssignmentService,
                       LeadDAO leadDAO, LeadAssignmentHistoryDAO historyDAO, AuthorizationService authService,
                       LeadSlaService slaService) {
        this(leads, imports, rateLimiter, ruleService, autoAssignmentService, leadDAO, historyDAO, authService, slaService, new LeadConversionService(), new SavedLeadFilterService());
    }

    public LeadServlet(LeadService leads, LeadImportService imports, IpRateLimiter rateLimiter,
                       AssignmentRuleService ruleService, LeadAutoAssignmentService autoAssignmentService,
                       LeadDAO leadDAO, LeadAssignmentHistoryDAO historyDAO, AuthorizationService authService,
                       LeadSlaService slaService, LeadConversionService conversionService) {
        this(leads, imports, rateLimiter, ruleService, autoAssignmentService, leadDAO, historyDAO, authService, slaService, conversionService, new SavedLeadFilterService());
    }

    public LeadServlet(LeadService leads, LeadImportService imports, IpRateLimiter rateLimiter,
                       AssignmentRuleService ruleService, LeadAutoAssignmentService autoAssignmentService,
                       LeadDAO leadDAO, LeadAssignmentHistoryDAO historyDAO, AuthorizationService authService,
                       LeadSlaService slaService, LeadConversionService conversionService,
                       SavedLeadFilterService savedFilterService) {
        this.leads = leads != null ? leads : new LeadService();
        this.imports = imports != null ? imports : new LeadImportService();
        this.rateLimiter = rateLimiter != null ? rateLimiter : new IpRateLimiter(5, 60_000);
        this.ruleService = ruleService != null ? ruleService : new AssignmentRuleService();
        this.autoAssignmentService = autoAssignmentService != null ? autoAssignmentService : new LeadAutoAssignmentService();
        this.dao = leadDAO != null ? leadDAO : new LeadDAO();
        this.historyDAO = historyDAO != null ? historyDAO : new LeadAssignmentHistoryDAO();
        this.authService = authService != null ? authService : new AuthorizationService();
        this.slaService = slaService != null ? slaService : new LeadSlaService();
        this.conversionService = conversionService != null ? conversionService : new LeadConversionService();
        this.savedFilterService = savedFilterService != null ? savedFilterService : new SavedLeadFilterService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        service(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        service(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        service(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        service(req, resp);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = normalizePath(req.getPathInfo());
        String method = req.getMethod() != null ? req.getMethod().toUpperCase(Locale.ROOT) : "GET";

        // 1. Health check endpoint (Public GET)
        if ("/health".equalsIgnoreCase(path)) {
            if (!"GET".equals(method)) {
                unsupported(resp, "GET");
                return;
            }
            ResponseUtil.json(resp, 200, ApiResponse.success("Lead API service is running", null));
            return;
        }

        // 2. Public Web Form submission endpoints (Public POST)
        if ("POST".equals(method) && ("/web-form".equals(path) || "/web-form/submit".equals(path))) {
            handleWebFormSubmission(req, resp);
            return;
        }

        // 3. Check authentication for all management routes
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Number id)) {
            ResponseUtil.json(resp, 401, ApiResponse.error("Chưa đăng nhập", null));
            return;
        }
        long user = id.longValue();

        try {
            // --- S4-07 SLA Routes ---
            if ("/my-assigned".equals(path)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                new LeadSlaMonitorService().checkAndNotifyOverdueLeads();
                List<LeadSlaResponseDTO> list = slaService.getMyAssignedLeads(user);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách Lead được phân thành công", list));
                return;
            }

            if ("/check-sla".equals(path) || "/sla/trigger-check".equals(path)) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                int count = new LeadSlaMonitorService().checkAndNotifyOverdueLeads();
                ResponseUtil.json(resp, 200, ApiResponse.success("Đã kiểm tra SLA và phát thông báo", Map.of("notificationsSent", count)));
                return;
            }

            if (path.endsWith("/accept") && path.length() > "/accept".length()) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                long leadId = parseLeadId(path, "/accept");
                LeadSlaResponseDTO result = slaService.acceptLead(user, leadId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Tiếp nhận Lead thành công. Trạng thái chuyển sang Đang chăm sóc.", result));
                return;
            }

            if (path.endsWith("/reject") && path.length() > "/reject".length()) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                long leadId = parseLeadId(path, "/reject");
                String body = readBodyString(req);
                LeadRejectRequest rejectReq = null;
                try {
                    rejectReq = JsonUtil.getGson().fromJson(body, LeadRejectRequest.class);
                } catch (Exception ignored) {}

                if (rejectReq == null || rejectReq.getReason() == null || rejectReq.getReason().isBlank()
                        || rejectReq.getDetails() == null || rejectReq.getDetails().isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Lý do từ chối và giải trình chi tiết không được để trống", null));
                    return;
                }

                LeadSlaResponseDTO result = slaService.rejectLead(user, leadId, rejectReq.getReason().trim(), rejectReq.getDetails().trim());
                ResponseUtil.json(resp, 200, ApiResponse.success("Từ chối Lead thành công. Lead đã được chuyển về hàng chờ phân bổ.", result));
                return;
            }

            // --- S4-08 Lead Conversion Routes ---
            if (path.endsWith("/check-convert-duplicates")) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                long leadId = parseLeadId(path, "/check-convert-duplicates");
                if (leadId <= 0 && req.getParameter("leadId") != null) {
                    try { leadId = Long.parseLong(req.getParameter("leadId")); } catch (Exception ignored) {}
                }
                LeadConvertDuplicateCheckDTO dupCheck = conversionService.checkConvertDuplicates(user, leadId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Kiểm tra trùng lặp thành công", dupCheck));
                return;
            }

            if (path.endsWith("/convert")) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                long leadId = parseLeadId(path, "/convert");
                JsonObject bodyJson = json(req, 32768);
                if (bodyJson.has("existingCustomerId")) {
                    var el = bodyJson.get("existingCustomerId");
                    if (el == null || el.isJsonNull() || (el.isJsonPrimitive() && el.getAsString().isBlank())) {
                        bodyJson.remove("existingCustomerId");
                    }
                }
                if (leadId <= 0 && bodyJson.has("leadId") && !bodyJson.get("leadId").isJsonNull()) {
                    try {
                        String rawId = bodyJson.get("leadId").getAsString();
                        if (rawId.startsWith("LD-CONV-")) {
                            rawId = rawId.substring("LD-CONV-".length());
                        } else if (rawId.startsWith("LD-")) {
                            rawId = rawId.substring(3);
                        } else if (rawId.startsWith("LEAD-")) {
                            rawId = rawId.substring(5);
                        }
                        leadId = Long.parseLong(rawId);
                    } catch (Exception ignored) {}
                }
                LeadConversionRequest convReq = JsonUtil.getGson().fromJson(bodyJson, LeadConversionRequest.class);
                LeadConversionResultDTO result = conversionService.convert(user, leadId, convReq);
                ResponseUtil.json(resp, 200, ApiResponse.success("Chuyển đổi Lead thành công", result));
                return;
            }

            // --- S4-06 Auto Assignment Routes ---
            if ("/assignment-rules".equals(path)) {
                if ("GET".equals(method)) {
                    authService.requirePermission(req, "lead.assignment_rules");
                    List<AssignmentRule> rules = ruleService.getAllRules();
                    ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách quy tắc phân bổ thành công", rules));
                    return;
                } else if ("POST".equals(method)) {
                    handleSaveRules(req, resp);
                    return;
                } else {
                    unsupported(resp, "GET, POST");
                    return;
                }
            }

            if (path.startsWith("/assignment-rules/")) {
                String idStr = path.substring("/assignment-rules/".length());
                if ("PUT".equals(method)) {
                    handleUpdateSingleRule(req, resp, idStr);
                    return;
                } else if ("DELETE".equals(method)) {
                    handleDeleteSingleRule(req, resp, idStr);
                    return;
                } else {
                    unsupported(resp, "PUT, DELETE");
                    return;
                }
            }

            if ("/assignment-history".equals(path)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                authService.requirePermission(req, "lead.assign");
                List<AssignmentHistory> history = historyDAO.findAll(100);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy lịch sử phân bổ Lead thành công", history));
                return;
            }

            if (path.endsWith("/assignment-history") && path.length() > "/assignment-history".length()) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                String leadIdStr = path.substring(1, path.indexOf("/assignment-history"));
                authService.requirePermission(req, "lead.assign");
                try {
                    long leadId = Long.parseLong(leadIdStr);
                    List<AssignmentHistory> history = historyDAO.findByLeadId(leadId);
                    ResponseUtil.json(resp, 200, ApiResponse.success("Lấy lịch sử phân bổ Lead thành công", history));
                    return;
                } catch (NumberFormatException ignored) {}
            }

            if ("/queue".equals(path) || "/unassigned".equals(path)) {
                authService.requirePermission(req, "lead.assign");
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                List<Lead> queue = dao.findUnassignedOrPendingQueue();
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy hàng chờ phân bổ thành công", queue));
                return;
            }

            if ("/reps".equals(path) || "/sales-reps".equals(path)) {
                authService.requirePermission(req, "lead.assign");
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                List<SalesRepDTO> reps = dao.getSalesRepsWithWorkload();
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách chuyên viên kinh doanh thành công", reps));
                return;
            }

            if ("/assign".equals(path)) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                handleManualAssign(req, resp);
                return;
            }

            if ("/auto-assign".equals(path) || "/process-queue".equals(path)) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                handleProcessQueue(req, resp);
                return;
            }

            // --- S4-09 Saved Filters & Search Routes ---
            if ("/saved-filters".equals(path)) {
                if ("GET".equals(method)) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách bộ lọc thành công", savedFilterService.getAllFilters(user)));
                    return;
                } else if ("POST".equals(method)) {
                    String body = readBodyString(req);
                    SavedLeadFilter filter = JsonUtil.getGson().fromJson(body, SavedLeadFilter.class);
                    SavedLeadFilter saved = savedFilterService.saveCustomFilter(filter, user);
                    ResponseUtil.json(resp, 201, ApiResponse.success("Tạo bộ lọc thành công", saved));
                    return;
                } else {
                    unsupported(resp, "GET, POST");
                    return;
                }
            }

            if (path.startsWith("/saved-filters/")) {
                String idPart = path.substring("/saved-filters/".length());
                long filterId = parseIdSafe(idPart);
                if ("PUT".equals(method)) {
                    String body = readBodyString(req);
                    SavedLeadFilter filter = JsonUtil.getGson().fromJson(body, SavedLeadFilter.class);
                    filter.setId(filterId);
                    boolean updated = savedFilterService.updateCustomFilter(filter, user);
                    if (updated) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật bộ lọc thành công", filter));
                    } else {
                        ResponseUtil.json(resp, 400, ApiResponse.error("Không thể cập nhật bộ lọc", null));
                    }
                    return;
                } else if ("DELETE".equals(method)) {
                    boolean deleted = savedFilterService.deleteCustomFilter(filterId, user);
                    if (deleted) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Xóa bộ lọc thành công", null));
                    } else {
                        ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy bộ lọc để xóa", null));
                    }
                    return;
                } else {
                    unsupported(resp, "PUT, DELETE");
                    return;
                }
            }

            if ("/stats".equals(path)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                ResponseUtil.json(resp, 200, ApiResponse.success("Thống kê Lead thành công", leads.getLeadStats(user)));
                return;
            }

            if ("/filter".equals(path)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                LeadFilterRequest filter = parseFilterRequest(req);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lọc danh sách Lead thành công", leads.searchLeads(filter, user)));
                return;
            }

            // --- S4-01 / S4-02 / S4-09 Root Lead Routes ---
            if (path.isEmpty() || "/".equals(path)) {
                if ("POST".equals(method)) {
                    leads.authorizeCreate(user);
                    JsonObject jsonBody = json(req, 16384);
                    LeadWriteRequest writeReq = LeadWriteRequest.fromJson(jsonBody);
                    var lead = leads.create(user, writeReq);
                    ResponseUtil.json(resp, 201, ApiResponse.success("Tạo Lead thành công", lead));
                    return;
                } else if ("GET".equals(method)) {
                    handleLeadList(req, resp, user);
                    return;
                } else {
                    unsupported(resp, "GET, POST");
                    return;
                }
            }

            // S4-01: Embed code generator
            if ("/embed-code".equalsIgnoreCase(path)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                handleEmbedCode(req, resp, user);
                return;
            }

            // S4-02: Sources catalog
            if ("/sources".equals(path)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                ResponseUtil.json(resp, 200, ApiResponse.success("Nguồn Lead", leads.sources(user)));
                return;
            }

            // S4-02: Excel import template download
            if ("/import/template".equals(path)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                imports.template(user, out);
                resp.setHeader("Cache-Control", "no-store");
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition", "attachment; filename=\"lead_import_template.xlsx\"");
                resp.setContentLength(out.size());
                out.writeTo(resp.getOutputStream());
                return;
            }

            // S4-02: Excel import preview
            if ("/import/preview".equals(path)) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                imports.authorize(user);
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
                        ResponseUtil.json(resp, 200, ApiResponse.success("Preview Lead thành công", imports.preview(user, input)));
                        return;
                    }
                } finally {
                    if (file != null) file.delete();
                }
            }

            // S4-02: Excel import confirm
            if ("/import/confirm".equals(path)) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                imports.authorize(user);
                JsonObject body = json(req, 8192);
                if (!body.keySet().equals(Set.of("batchToken")) || !body.get("batchToken").isJsonPrimitive() || !body.getAsJsonPrimitive("batchToken").isString()) {
                    throw new IllegalArgumentException("Confirm chỉ nhận batchToken dạng chuỗi");
                }
                ResponseUtil.json(resp, 200, ApiResponse.success("Import Lead hoàn tất", imports.confirm(user, body.get("batchToken").getAsString())));
                return;
            }

            // S4-02, S4-05 & S4-09: Single Lead detail by ID (GET), update (PUT), delete (DELETE)
            if (path.matches("/[1-9][0-9]*")) {
                long leadId = Long.parseLong(path.substring(1));
                if ("PUT".equals(method)) {
                    JsonObject jsonBody = json(req, 16384);
                    LeadWriteRequest writeReq = LeadWriteRequest.fromJson(jsonBody);
                    var lead = leads.update(user, leadId, writeReq);
                    ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật Lead thành công", lead));
                    return;
                }
                if ("DELETE".equals(method)) {
                    boolean deleted = leads.deleteLead(leadId, user);
                    if (deleted) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Xóa Lead thành công", null));
                    } else {
                        ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy Lead để xóa", null));
                    }
                    return;
                }
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET, PUT, DELETE");
                    return;
                }
                Lead lead = leads.getLeadById(leadId, user);
                ResponseUtil.json(resp, lead == null ? 404 : 200, lead == null ? ApiResponse.error("Không tìm thấy Lead", null) : ApiResponse.success("Hồ sơ Lead", lead));
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy API Lead", null));

        } catch (NoSuchElementException error) {
            ResponseUtil.json(resp, 404, ApiResponse.error(error.getMessage(), null));
        } catch (LeadValidationException error) {
            ResponseUtil.json(resp, 400, ApiResponse.error(error.getMessage(), Map.of("errors", error.errors())));
        } catch (LeadImportService.Failure error) {
            if (error.status() == 500) log("Lead import failed", error);
            ResponseUtil.json(resp, error.status(), ApiResponse.error(error.getMessage(), error.data()));
        } catch (SecurityException error) {
            int status = error.getMessage() != null && error.getMessage().contains("Phiên") ? 401 : 403;
            ResponseUtil.json(resp, status, ApiResponse.error(error.getMessage(), null));
        } catch (JsonParseException | IllegalArgumentException error) {
            ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu yêu cầu không hợp lệ: " + error.getMessage(), null));
        } catch (IllegalStateException error) {
            if (error.getMessage() != null && (error.getMessage().contains("Upload") || error.getMessage().contains("vượt"))) {
                ResponseUtil.json(resp, 413, ApiResponse.error("Upload vượt dung lượng cho phép", null));
            } else {
                ResponseUtil.json(resp, 409, ApiResponse.error(error.getMessage(), null));
            }
        } catch (ServletException error) {
            ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu multipart không hợp lệ", null));
        } catch (SQLException error) {
            log("Lead database failure", error);
            ResponseUtil.json(resp, 500, ApiResponse.error("Không thể xử lý dữ liệu Lead; kiểm tra DB và migration 024", null));
        } catch (Exception error) {
            log("Lead request failed", error);
            ResponseUtil.json(resp, 500, ApiResponse.error("Không thể xử lý yêu cầu Lead", null));
        }
    }

    private void handleLeadList(HttpServletRequest request, HttpServletResponse response, long user) throws Exception {
        LeadFilterRequest filter = parseFilterRequest(request);
        Map<String, Object> result = leads.searchLeads(filter, user);

        if ("array".equalsIgnoreCase(request.getParameter("format"))) {
            response.setStatus(200);
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write(JsonUtil.getGson().toJson(result.get("items")));
        } else {
            ResponseUtil.json(response, 200, ApiResponse.success("Lấy danh sách Lead thành công", result));
        }
    }

    private void handleSaveRules(HttpServletRequest request, HttpServletResponse response) throws Exception {
        authService.requirePermission(request, "lead.assignment_rules");

        String body = readBodyString(request);
        if (body == null || body.isBlank()) {
            ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu quy tắc trống", null));
            return;
        }

        try {
            if (body.trim().startsWith("[")) {
                JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                List<AssignmentRule> rules = new ArrayList<>();
                for (JsonElement el : array) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    sanitizeRuleJson(obj);
                    rules.add(JsonUtil.getGson().fromJson(obj, AssignmentRule.class));
                }
                List<AssignmentRule> saved = ruleService.batchSaveRules(rules);
                ResponseUtil.json(response, 200, ApiResponse.success("Lưu danh sách quy tắc phân bổ thành công", saved));
            } else {
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                sanitizeRuleJson(obj);
                AssignmentRule rule = JsonUtil.getGson().fromJson(obj, AssignmentRule.class);
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

    private void sanitizeRuleJson(JsonObject obj) {
        if (obj.has("id")) {
            JsonElement idElem = obj.get("id");
            if (idElem == null || idElem.isJsonNull()) {
                obj.remove("id");
            } else if (idElem.isJsonPrimitive()) {
                String str = idElem.getAsString().trim();
                try {
                    long numericId = Long.parseLong(str);
                    if (numericId > 0) {
                        obj.addProperty("id", numericId);
                    } else {
                        obj.remove("id");
                    }
                } catch (NumberFormatException e) {
                    obj.remove("id");
                    if (!obj.has("ruleCode") || obj.get("ruleCode") == null || obj.get("ruleCode").isJsonNull() || obj.get("ruleCode").getAsString().isBlank()) {
                        obj.addProperty("ruleCode", str);
                    }
                }
            } else {
                obj.remove("id");
            }
        }
    }

    private void handleUpdateSingleRule(HttpServletRequest request, HttpServletResponse response, String idStr) throws Exception {
        authService.requirePermission(request, "lead.assignment_rules");

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
            ResponseUtil.json(response, 404, ApiResponse.error("Không tìm thấy quy tắc cần cập nhật", null));
        } catch (NumberFormatException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("ID quy tắc không hợp lệ", null));
        } catch (Exception e) {
            ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu cập nhật quy tắc không hợp lệ: " + e.getMessage(), null));
        }
    }

    private void handleDeleteSingleRule(HttpServletRequest request, HttpServletResponse response, String idStr) throws Exception {
        authService.requirePermission(request, "lead.assignment_rules");

        try {
            long ruleId = Long.parseLong(idStr);
            boolean deleted = ruleService.deleteRule(ruleId);
            if (deleted) {
                ResponseUtil.json(response, 200, ApiResponse.success("Xóa quy tắc phân bổ thành công", null));
            } else {
                ResponseUtil.json(response, 404, ApiResponse.error("Không tìm thấy quy tắc cần xóa", null));
            }
        } catch (NumberFormatException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("ID quy tắc không hợp lệ", null));
        }
    }

    private void handleManualAssign(HttpServletRequest request, HttpServletResponse response) throws Exception {
        authService.requirePermission(request, "lead.assign");

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

    private void handleProcessQueue(HttpServletRequest request, HttpServletResponse response) throws Exception {
        authService.requirePermission(request, "lead.auto_assign");

        int count = autoAssignmentService.processPendingQueue();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assignedCount", count);
        result.put("message", count > 0
                ? "Đã tự động phân bổ thành công " + count + " Lead từ hàng chờ tới đội ngũ Sales."
                : "Không có Lead nào khớp với các quy tắc đang bật.");

        ResponseUtil.json(response, 200, ApiResponse.success("Xử lý hàng chờ hoàn tất", result));
    }

    private void handleWebFormSubmission(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String clientIp = IpRateLimiter.extractClientIp(req);

        if (!rateLimiter.tryAcquire(clientIp)) {
            resp.setHeader("Retry-After", "60");
            ResponseUtil.json(resp, 429, ApiResponse.error(
                    "Quá nhiều yêu cầu từ địa chỉ IP của bạn. Vui lòng thử lại sau 1 phút.",
                    null
            ));
            return;
        }

        try {
            WebFormLeadRequest leadRequest = parseRequestBody(req);
            if (leadRequest == null) {
                ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu gửi lên không hợp lệ.", null));
                return;
            }

            Lead created = leads.createWebFormLead(leadRequest, clientIp);
            ResponseUtil.json(resp, 201, ApiResponse.success("Tiếp nhận thông tin Lead thành công", Map.of("id", created.getId(), "fullName", created.getFullName(), "status", "NEW")));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (LeadImportService.Failure e) {
            ResponseUtil.json(resp, e.status(), ApiResponse.error(e.getMessage(), null));
        } catch (JsonParseException | IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log("Error processing lead web-form submission", e);
            ResponseUtil.json(resp, 500, ApiResponse.error("Không thể lưu Lead. Vui lòng thử lại.", null));
        }
    }

    private void handleEmbedCode(HttpServletRequest request, HttpServletResponse response, long user) throws Exception {
        String formId = request.getParameter("formId");
        String theme = request.getParameter("theme");
        String title = request.getParameter("title");

        String baseUrl = resolveBaseUrl(request);
        EmbedCodeResponse embedCode = leads.registeredEmbedCode(user, formId, baseUrl, theme, title);

        ResponseUtil.json(response, 200, ApiResponse.success("Tạo mã nhúng biểu mẫu thành công", embedCode));
    }

    private WebFormLeadRequest parseRequestBody(HttpServletRequest request) throws Exception {
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            return JsonUtil.getGson().fromJson(json(request, 16384), WebFormLeadRequest.class);
        }
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded"))
            throw new IllegalArgumentException("Yêu cầu JSON hoặc application/x-www-form-urlencoded");
        if (request.getContentLengthLong() < 0)
            throw new IllegalArgumentException("Biểu mẫu phải có Content-Length");
        if (request.getContentLengthLong() > 16384)
            throw new LeadImportService.Failure(413, "Biểu mẫu vượt giới hạn cho phép", null);

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
        req.setRequestId(request.getParameter("requestId"));
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

        // Keyword
        String search = req.getParameter("search");
        if (search == null || search.isBlank()) search = req.getParameter("keyword");
        if (search != null) f.setKeyword(search);

        // Status
        String status = req.getParameter("status");
        if (status != null && !status.isBlank()) f.setStatus(status);

        // Tier / Rating
        String tier = req.getParameter("tier");
        if (tier == null || tier.isBlank()) tier = req.getParameter("rating");
        if (tier != null && !tier.isBlank()) f.setTier(tier);

        // Source
        String source = req.getParameter("source");
        if (source != null && !source.isBlank()) f.setSource(source);

        // Owner
        String owner = req.getParameter("owner_id");
        if (owner == null || owner.isBlank()) owner = req.getParameter("owner");
        if (owner == null || owner.isBlank()) owner = req.getParameter("assigned_to");
        if (owner != null && !owner.isBlank()) {
            f.setOwner(owner);
            if (owner.matches("\\d+")) {
                f.setOwnerId(Long.parseLong(owner));
            }
        }

        // Date range
        String dateRange = req.getParameter("dateRange");
        if (dateRange != null && !dateRange.isBlank()) f.setDateRange(dateRange);

        String from = req.getParameter("from");
        if (from == null || from.isBlank()) from = req.getParameter("fromDate");
        if (from == null || from.isBlank()) from = req.getParameter("created_from");
        if (from != null) f.setFrom(from);

        String to = req.getParameter("to");
        if (to == null || to.isBlank()) to = req.getParameter("toDate");
        if (to == null || to.isBlank()) to = req.getParameter("created_to");
        if (to != null) f.setTo(to);

        // SLA Overdue Only
        String overdue = req.getParameter("slaOverdueOnly");
        if (overdue == null || overdue.isBlank()) overdue = req.getParameter("is_overdue_sla");
        if ("true".equalsIgnoreCase(overdue) || "1".equals(overdue)) {
            f.setSlaOverdueOnly(true);
        }

        // Sort
        String sort = req.getParameter("sort");
        if (sort != null && !sort.isBlank()) f.setSort(sort);

        // Pagination
        f.setPage(parseInt(req.getParameter("page"), 1));
        int size = parseInt(req.getParameter("size"), 10);
        if (size <= 0) size = parseInt(req.getParameter("pageSize"), 10);
        f.setSize(size);

        return f;
    }

    private JsonObject json(HttpServletRequest req, int limit) throws Exception {
        if (req.getContentType() == null || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("application/json"))
            throw new IllegalArgumentException("Yêu cầu application/json");
        char[] chars = new char[limit + 1];
        int size = 0, count;
        Reader input = req.getReader();
        while (size < chars.length && (count = input.read(chars, size, chars.length - size)) != -1) size += count;
        if (size > limit) throw new LeadImportService.Failure(413, "JSON vượt giới hạn cho phép", null);
        var body = JsonParser.parseString(new String(chars, 0, size));
        if (!body.isJsonObject()) throw new IllegalArgumentException("Yêu cầu JSON object");
        return body.getAsJsonObject();
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

    private long parseLeadId(String path, String suffix) {
        String sub = path.substring(0, path.length() - suffix.length());
        while (sub.startsWith("/")) sub = sub.substring(1);
        while (sub.endsWith("/")) sub = sub.substring(0, sub.length() - 1);
        if (sub.toUpperCase().startsWith("LD-SLA-")) {
            sub = sub.substring(7);
        } else if (sub.toUpperCase().startsWith("LD-CONV-")) {
            sub = sub.substring(8);
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

    private long parseIdSafe(String idStr) {
        if (idStr == null || idStr.isBlank()) return -1;
        try {
            String digits = idStr.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? -1 : Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String normalizePath(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) return "";
        String p = pathInfo.trim();
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private void unsupported(HttpServletResponse resp, String allow) throws IOException {
        resp.setHeader("Allow", allow);
        ResponseUtil.json(resp, 405, ApiResponse.error("Phương thức không được hỗ trợ", null));
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
