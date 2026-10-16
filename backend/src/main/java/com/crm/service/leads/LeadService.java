package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.EmbedCodeResponse;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.dto.leads.WebFormLeadRequest;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;
import com.crm.service.permissions.ScopeType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Pattern;

public class LeadService {

    @FunctionalInterface
    public interface ScopeResolver {
        DataScopeContext resolve(long user, String action) throws Exception;
    }

    public interface ScoringHook {
        void beforeCreate(Connection c) throws Exception;
        void scoreCreated(Connection c, long id) throws Exception;
    }

    public static ScoringHook productionScoring() {
        return new ScoringHook() {
            private final com.crm.service.scoring.LeadScoringService scoring = new com.crm.service.scoring.LeadScoringService();
            @Override
            public void beforeCreate(Connection c) throws Exception { scoring.beforeCreate(c); }
            @Override
            public void scoreCreated(Connection c, long id) throws Exception { scoring.scoreCreated(c, id); }
        };
    }

    public static final ScoringHook NO_SCORING = new ScoringHook() {
        @Override
        public void beforeCreate(Connection c) {}
        @Override
        public void scoreCreated(Connection c, long id) {}
    };

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\.\\s]{8,20}$");

    private final LeadDAO dao;
    private final ScopeResolver scopes;
    private final ScoringHook scoring;
    private final DataScopeService dataScopeService;

    public LeadService() {
        this(new LeadDAO());
    }

    public LeadService(LeadDAO dao) {
        this(dao, (u, a) -> new DataScopeService().resolve(u, "lead", a), productionScoring());
    }

    public LeadService(LeadDAO dao, ScopeResolver scopes) {
        this(dao, scopes, NO_SCORING);
    }

    public LeadService(LeadDAO dao, DataScopeService dataScopeService) {
        this(dao, (u, a) -> dataScopeService.resolve(u, "lead", a), NO_SCORING, dataScopeService);
    }

    public LeadService(LeadDAO dao, ScopeResolver scopes, ScoringHook scoring) {
        this(dao, scopes, scoring, new DataScopeService());
    }

    public LeadService(LeadDAO dao, ScopeResolver scopes, ScoringHook scoring, DataScopeService dataScopeService) {
        this.dao = dao;
        this.scopes = scopes != null ? scopes : (u, a) -> (dataScopeService != null ? dataScopeService : new DataScopeService()).resolve(u, "lead", a);
        this.scoring = scoring != null ? scoring : NO_SCORING;
        this.dataScopeService = dataScopeService != null ? dataScopeService : new DataScopeService();
    }

    public LeadDAO getLeadDAO() {
        return dao;
    }

    public DataScopeContext authorizeCreate(long user) throws Exception {
        return scopes.resolve(user, "create");
    }

    public List<Map<String, Object>> sources(long user) throws Exception {
        scopes.resolve(user, "read");
        try (Connection c = dao.open()) {
            return dao.sources(c, false);
        }
    }

    public Map<String, Object> create(long user, LeadWriteRequest request) throws Exception {
        scopes.resolve(user, "create");
        try (Connection c = dao.open()) {
            c.setAutoCommit(false);
            try {
                requireSource(request, dao.sources(c, true));
                scoring.beforeCreate(c);
                long id = dao.insert(c, request, user, null);
                scoring.scoreCreated(c, id);
                c.commit();
                DataScopeContext readScope = scopes.resolve(user, "read");
                return dao.get(c, id, readScope, false);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public Map<String, Object> get(long user, long id) throws Exception {
        DataScopeContext scope = scopes.resolve(user, "read");
        try (Connection c = dao.open()) {
            Map<String, Object> row = dao.get(c, id, scope, false);
            if (row == null) {
                // If lead exists but forbidden by scope -> 403 SecurityException
                Map<String, Object> unrestricted = dao.get(c, id, null, false);
                if (unrestricted != null) {
                    throw new SecurityException("Bạn không có quyền truy cập dữ liệu");
                }
            }
            return row;
        }
    }

    public Map<String, Object> update(long user, long id, LeadWriteRequest request) throws Exception {
        scopes.resolve(user, "update");
        try (Connection c = dao.open()) {
            c.setAutoCommit(false);
            try {
                DataScopeContext scope = scopes.resolve(user, "read");
                Map<String, Object> existing = dao.get(c, id, scope, true);
                if (existing == null) {
                    Map<String, Object> unrestricted = dao.get(c, id, null, false);
                    if (unrestricted != null) {
                        throw new SecurityException("Bạn không có quyền sửa dữ liệu");
                    }
                    return null;
                }

                // CRM-122: Prevent modification of converted leads
                boolean isLocked = Boolean.TRUE.equals(existing.get("isLocked")) || "CONVERTED".equalsIgnoreCase((String) existing.get("status"));
                if (isLocked) {
                    throw new IllegalStateException("Không thể chỉnh sửa Lead đã được chuyển đổi thành khách hàng");
                }

                requireSource(request, dao.sources(c, true));
                dao.update(c, id, request);
                c.commit();
                return dao.get(c, id, scope, false);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public static void requireSource(LeadWriteRequest lead, List<Map<String, Object>> sources) {
        boolean match = sources.stream().anyMatch(s -> Objects.equals(s.get("id"), lead.sourceId()));
        if (!match) {
            throw new IllegalArgumentException("Nguồn khách hàng tiềm năng không tồn tại");
        }
    }

    /**
     * S4-09 AC 1 & AC 2: Multi-criteria Lead search with SLA calculation and Data Scope.
     */
    public Map<String, Object> searchLeads(LeadFilterRequest filter, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "read");

        int totalRecords = dao.countWithFilters(filter, scope, currentUserId);
        List<Lead> items = dao.findWithFilters(filter, scope, currentUserId);

        // Process SLA calculations for every lead (AC 2)
        long now = System.currentTimeMillis();
        for (Lead lead : items) {
            evaluateSla(lead, now);
        }

        int pageSize = filter.getSize();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("leads", items); // Alias for flexible consumers
        result.put("page", filter.getPage());
        result.put("size", pageSize);
        result.put("totalRecords", totalRecords);
        result.put("totalItems", totalRecords);
        result.put("totalPages", totalPages > 0 ? totalPages : 1);
        result.put("scope", scope != null ? scope.scopeType().name() : "ALL");
        result.put("stats", dao.getLeadStats(scope, currentUserId));

        return result;
    }

    public Lead getLeadById(long id, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "read");
        Lead lead = dao.findById(id);
        if (lead == null) return null;

        if (scope != null && !scope.canAccessOwner(lead.getAssignedTo() != null ? lead.getAssignedTo() : 0)) {
            // Check if unassigned or allowed
            if (lead.getAssignedTo() != null && scope.scopeType() == ScopeType.SELF 
                && lead.getAssignedTo() != currentUserId) {
                throw new SecurityException("Bạn không có quyền truy cập dữ liệu Lead này");
            }
        }

        evaluateSla(lead, System.currentTimeMillis());
        return lead;
    }

    public Lead createLead(Lead lead, long currentUserId) throws Exception {
        scopes.resolve(currentUserId, "create");

        if (lead.getSlaDeadline() == null) {
            // Default SLA deadline: 120 minutes from creation
            long defaultSlaMin = lead.getSlaMinutesTotal() > 0 ? lead.getSlaMinutesTotal() : 120;
            lead.setSlaDeadline(new Timestamp(System.currentTimeMillis() + defaultSlaMin * 60 * 1000));
        }

        if (lead.getRating() == null || lead.getRating().isBlank()) {
            lead.setRating("WARM");
            lead.setTier("WARM");
        }

        long id = dao.insert(lead);
        if (id > 0) {
            return getLeadById(id, currentUserId);
        }
        return null;
    }

    public boolean updateLead(Lead lead, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "update");
        Lead existing = dao.findById(lead.getId());
        if (existing == null) {
            return false;
        }

        // AC Check: If lead was converted, lock update
        if (existing.isLocked() || "CONVERTED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Không thể chỉnh sửa Lead đã chuyển đổi thành công (CONVERTED)");
        }

        if (scope != null && !scope.canAccessOwner(existing.getAssignedTo() != null ? existing.getAssignedTo() : 0)) {
            if (existing.getAssignedTo() != null && scope.scopeType() == ScopeType.SELF 
                && existing.getAssignedTo() != currentUserId) {
                throw new SecurityException("Bạn không có quyền cập nhật Lead này");
            }
        }

        return dao.update(lead);
    }

    public boolean deleteLead(long id, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "delete");
        Lead existing = dao.findById(id);
        if (existing == null) return false;

        if (existing.isLocked() || "CONVERTED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Không thể xóa Lead đã chuyển đổi thành công (CONVERTED)");
        }

        if (scope != null && !scope.canAccessOwner(existing.getAssignedTo() != null ? existing.getAssignedTo() : 0)) {
            if (existing.getAssignedTo() != null && scope.scopeType() == ScopeType.SELF 
                && existing.getAssignedTo() != currentUserId) {
                throw new SecurityException("Bạn không có quyền xóa Lead này");
            }
        }

        return dao.delete(id);
    }

    public Map<String, Object> getLeadStats(long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "read");
        return dao.getLeadStats(scope, currentUserId);
    }

    /**
     * S4-09 AC 2: SLA Overdue and Time-to-Contact evaluation engine.
     */
    public void evaluateSla(Lead lead, long now) {
        if (lead == null) return;

        boolean isClosed = "CONVERTED".equalsIgnoreCase(lead.getStatus()) || "LOST".equalsIgnoreCase(lead.getStatus());
        if (isClosed) {
            lead.setOverdueSla(false);
            lead.setSlaStatus("CLOSED");
            lead.setSlaRemainingText("Đã kết thúc");
            lead.setSlaRemainingMinutes(null);
            return;
        }

        Timestamp deadline = lead.getSlaDeadline();
        if (deadline == null && "NEW".equalsIgnoreCase(lead.getStatus()) && lead.getCreatedAt() != null) {
            // If no explicit deadline, compute 2 hours (120 minutes) from created_at
            long createdMs = lead.getCreatedAt().getTime();
            long totalMs = (lead.getSlaMinutesTotal() > 0 ? lead.getSlaMinutesTotal() : 120) * 60 * 1000L;
            deadline = new Timestamp(createdMs + totalMs);
            lead.setSlaDeadline(deadline);
        }

        if (deadline == null) {
            lead.setOverdueSla(false);
            lead.setSlaStatus("NONE");
            lead.setSlaRemainingText(null);
            lead.setSlaRemainingMinutes(null);
            return;
        }

        long diffMs = deadline.getTime() - now;
        if (diffMs < 0) {
            lead.setOverdueSla(true);
            lead.setSlaStatus("OVERDUE");
            long overdueMin = Math.abs(diffMs / (60 * 1000));
            lead.setSlaRemainingMinutes(-overdueMin);

            long hours = overdueMin / 60;
            long mins = overdueMin % 60;
            lead.setSlaRemainingText(hours > 0 
                ? String.format("Quá hạn %dh %dm", hours, mins) 
                : String.format("Quá hạn %d phút", mins));
        } else {
            lead.setOverdueSla(false);
            long leftMin = diffMs / (60 * 1000);
            lead.setSlaRemainingMinutes(leftMin);
            lead.setSlaStatus(leftMin < 30 ? "WARNING" : "SAFE");

            long hours = leftMin / 60;
            long mins = leftMin % 60;
            lead.setSlaRemainingText(hours > 0 
                ? String.format("Còn %dh %dm", hours, mins) 
                : String.format("Còn %d phút", mins));
        }
    }

    /**
     * S4-01: Creates a new lead from a public web form submission with data sanitization and validation.
     */
    public Lead createWebFormLead(WebFormLeadRequest request, String clientIp) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu biểu mẫu không được để trống.");
        }

        String fullName = sanitize(request.getFullName());
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên là bắt buộc.");
        }
        if (fullName.length() > 150) {
            fullName = fullName.substring(0, 150);
        }

        String email = request.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Địa chỉ email không đúng định dạng.");
            }
            if (email.length() > 255) {
                email = email.substring(0, 255);
            }
        } else {
            email = null;
        }

        String phone = request.getPhone();
        if (phone != null && !phone.trim().isEmpty()) {
            phone = phone.trim().replaceAll("\\s+", "");
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ (từ 8-20 ký tự số).");
            }
            if (phone.length() > 50) {
                phone = phone.substring(0, 50);
            }
        } else {
            phone = null;
        }

        // At least one contact method required
        if (email == null && phone == null) {
            throw new IllegalArgumentException("Vui lòng cung cấp ít nhất số điện thoại hoặc email để liên hệ.");
        }

        String company = request.getResolvedCompany();
        if (company != null) {
            company = sanitize(company);
            if (company.length() > 255) {
                company = company.substring(0, 255);
            }
        }

        String notes = request.getResolvedNotes();
        if (notes != null) {
            notes = sanitize(notes);
            if (notes.length() > 2000) {
                notes = notes.substring(0, 2000);
            }
        }

        String source = request.getResolvedSource();
        if (source == null || source.isBlank()) {
            source = "WEB_FORM";
        }
        if (source.length() > 50) {
            source = source.substring(0, 50);
        }

        String formId = request.getFormId();
        if (formId != null && formId.length() > 100) {
            formId = formId.substring(0, 100);
        }

        Lead lead = new Lead();
        lead.setFullName(fullName);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setCompanyName(company);
        lead.setNotes(notes);
        lead.setSource(source);
        lead.setStatus("NEW");
        lead.setFormId(formId);
        lead.setIpAddress(clientIp);

        long id = dao.insert(lead);
        lead.setId(id);
        Lead created = dao.findById(id);
        return created != null ? created : lead;
    }

    /**
     * Generates HTML/JS Snippets for embedding web form into third-party websites.
     */
    public EmbedCodeResponse generateEmbedCode(String formId, String baseUrl, String theme, String title) {
        if (formId == null || formId.isBlank()) {
            formId = "crm-web-lead-form";
        }
        formId = formId.trim().replaceAll("[^A-Za-z0-9_-]", "-");

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8080/crm";
        }
        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (theme == null || (!"dark".equalsIgnoreCase(theme) && !"light".equalsIgnoreCase(theme))) {
            theme = "light";
        }

        if (title == null || title.isBlank()) {
            title = "Biểu mẫu Đăng ký Tư vấn | Corporate CRM";
        }
        title = title.trim().replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");

        String formUrl = baseUrl + "/lead-capture-form.html?formId=" + formId + "&theme=" + theme;
        String previewUrl = formUrl + "&preview=true";

        // 1. Iframe Snippet (most universal, zero CSS collision)
        String iframeSnippet = String.format(
                "<iframe src=\"%s\" width=\"100%%\" height=\"650\" frameborder=\"0\" " +
                "style=\"border:none; max-width:580px; width:100%%; border-radius:12px; " +
                "box-shadow:0 10px 25px -5px rgba(15,23,42,0.1); display:block; margin:0 auto;\" " +
                "title=\"%s\" loading=\"lazy\"></iframe>",
                formUrl, title
        );

        // 2. Responsive Container + Auto-resize Script Snippet
        String scriptSnippet = String.format(
                "<!-- Corporate CRM Web Form Embed -->\n" +
                "<div id=\"crm-form-container-%s\" style=\"max-width:580px; margin:0 auto;\">\n" +
                "  <iframe id=\"crm-iframe-%s\" src=\"%s\" width=\"100%%\" height=\"650\" frameborder=\"0\" " +
                "style=\"border:none; width:100%%; border-radius:12px;\" title=\"%s\"></iframe>\n" +
                "</div>\n" +
                "<script>\n" +
                "  window.addEventListener('message', function(e) {\n" +
                "    if (e.data && e.data.type === 'crm-form-resize') {\n" +
                "      var f = document.getElementById('crm-iframe-%s');\n" +
                "      if (f && e.data.height) f.style.height = e.data.height + 'px';\n" +
                "    }\n" +
                "  });\n" +
                "</script>",
                formId, formId, formUrl, title, formId
        );

        // 3. Raw Native HTML Form Snippet (for custom CSS integration)
        String htmlSnippet = String.format(
                "<form id=\"crm-native-form-%s\" action=\"%s/api/leads/web-form\" method=\"POST\" style=\"max-width:480px; font-family:sans-serif;\">\n" +
                "  <input type=\"hidden\" name=\"formId\" value=\"%s\" />\n" +
                "  <input type=\"hidden\" name=\"source\" value=\"WEB_FORM\" />\n" +
                "  <div style=\"margin-bottom:12px;\">\n" +
                "    <label style=\"display:block; font-weight:600; margin-bottom:4px;\">Họ và tên *</label>\n" +
                "    <input type=\"text\" name=\"fullName\" required style=\"width:100%%; padding:8px 12px; border:1px solid #cbd5e1; border-radius:6px;\" />\n" +
                "  </div>\n" +
                "  <div style=\"margin-bottom:12px;\">\n" +
                "    <label style=\"display:block; font-weight:600; margin-bottom:4px;\">Email *</label>\n" +
                "    <input type=\"email\" name=\"email\" required style=\"width:100%%; padding:8px 12px; border:1px solid #cbd5e1; border-radius:6px;\" />\n" +
                "  </div>\n" +
                "  <div style=\"margin-bottom:12px;\">\n" +
                "    <label style=\"display:block; font-weight:600; margin-bottom:4px;\">Số điện thoại *</label>\n" +
                "    <input type=\"tel\" name=\"phone\" required style=\"width:100%%; padding:8px 12px; border:1px solid #cbd5e1; border-radius:6px;\" />\n" +
                "  </div>\n" +
                "  <div style=\"margin-bottom:12px;\">\n" +
                "    <label style=\"display:block; font-weight:600; margin-bottom:4px;\">Tên công ty</label>\n" +
                "    <input type=\"text\" name=\"company\" style=\"width:100%%; padding:8px 12px; border:1px solid #cbd5e1; border-radius:6px;\" />\n" +
                "  </div>\n" +
                "  <div style=\"margin-bottom:16px;\">\n" +
                "    <label style=\"display:block; font-weight:600; margin-bottom:4px;\">Nhu cầu quan tâm</label>\n" +
                "    <textarea name=\"notes\" rows=\"3\" style=\"width:100%%; padding:8px 12px; border:1px solid #cbd5e1; border-radius:6px;\"></textarea>\n" +
                "  </div>\n" +
                "  <button type=\"submit\" style=\"width:100%%; background:#2563eb; color:#fff; font-weight:bold; padding:10px; border:none; border-radius:6px; cursor:pointer;\">Gửi thông tin</button>\n" +
                "</form>",
                formId, baseUrl, formId
        );

        return new EmbedCodeResponse(formId, formUrl, iframeSnippet, scriptSnippet, htmlSnippet, previewUrl);
    }

    private String sanitize(String input) {
        if (input == null) return null;
        // Strip script tags to prevent stored XSS
        return input.replaceAll("(?i)<script.*?>.*?</script>", "").trim();
    }
}
