package com.crm.service.leads;

import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.EmbedCodeResponse;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.dto.leads.LeadValidationException;
import com.crm.dto.leads.LeadValidationException.FieldError;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.dto.leads.WebFormLeadRequest;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.io.OutputStream;
import java.sql.Connection;
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
        var scoring = new com.crm.service.scoring.LeadScoringService();
        return new ScoringHook() {
            public void beforeCreate(Connection c) throws Exception { scoring.beforeCreate(c); }
            public void scoreCreated(Connection c, long id) throws Exception { scoring.scoreCreated(c, id); }
        };
    }

    public static final ScoringHook NO_SCORING = new ScoringHook() {
        public void beforeCreate(Connection c) {}
        public void scoreCreated(Connection c, long id) {}
    };

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{8,20}$");

    private final LeadDAO leadDAO;
    private final DataScopeService dataScopeService;
    private final ScopeResolver scopes;
    private final ScoringHook scoring;

    public LeadService() {
        this(new LeadDAO(), (user, action) -> new DataScopeService().resolve(user, "lead", action), productionScoring());
    }

    public LeadService(LeadDAO leadDAO, DataScopeService dataScopeService) {
        this(leadDAO, (user, action) -> dataScopeService.resolve(user, "lead", action), NO_SCORING);
    }

    public LeadService(LeadDAO leadDAO, ScopeResolver scopes) {
        this(leadDAO, scopes, NO_SCORING);
    }

    public LeadService(LeadDAO leadDAO, ScopeResolver scopes, ScoringHook scoring) {
        this.leadDAO = leadDAO;
        this.scopes = scopes;
        this.dataScopeService = new DataScopeService();
        this.scoring = scoring != null ? scoring : NO_SCORING;
    }

    // -------------------------------------------------------------------------
    // S4-02 & S4-05: Manual, Excel & Scoring Integration
    // -------------------------------------------------------------------------

    public DataScopeContext authorizeCreate(long user) throws Exception {
        return scopes.resolve(user, "create");
    }

    public List<Map<String, Object>> sources(long user) throws Exception {
        authorizeCreate(user);
        try (Connection c = leadDAO.open()) {
            return leadDAO.sources(c, false);
        }
    }

    public Map<String, Object> create(long user, LeadWriteRequest request) throws Exception {
        DataScopeContext scope = authorizeCreate(user);
        LeadWriteRequest lead = LeadValidation.normalize(request);
        try (Connection c = leadDAO.open()) {
            c.setAutoCommit(false);
            try {
                scoring.beforeCreate(c);
                requireSource(lead, leadDAO.sources(c, true));
                long id = leadDAO.insert(c, lead, user, null);
                scoring.scoreCreated(c, id);
                Map<String, Object> result = leadDAO.get(c, id, scope);
                c.commit();
                return result;
            } catch (Exception error) {
                c.rollback();
                throw error;
            }
        }
    }

    public Map<String, Object> get(long user, long id) throws Exception {
        if (id <= 0) throw new IllegalArgumentException("ID Lead phải dương");
        DataScopeContext scope = scopes.resolve(user, "read");
        try (Connection c = leadDAO.open()) {
            return leadDAO.get(c, id, scope);
        }
    }

    public Map<String, Object> update(long user, long id, LeadWriteRequest request) throws Exception {
        if (id <= 0) throw new IllegalArgumentException("ID Lead phải dương");
        DataScopeContext scope = scopes.resolve(user, "update");
        var lead = LeadValidation.normalize(request);
        try (Connection c = leadDAO.open()) {
            c.setAutoCommit(false);
            try {
                scoring.beforeCreate(c);
                if (leadDAO.get(c, id, scope, true) == null) {
                    throw new NoSuchElementException("Không tìm thấy Lead trong phạm vi dữ liệu");
                }
                requireSource(lead, leadDAO.sources(c, true));
                leadDAO.update(c, id, lead);
                scoring.scoreCreated(c, id);
                var result = leadDAO.get(c, id, scope);
                c.commit();
                return result;
            } catch (Exception error) {
                c.rollback();
                throw error;
            }
        }
    }

    public static void requireSource(LeadWriteRequest lead, List<Map<String, Object>> sources) {
        if (sources.stream().noneMatch(s -> ((Number) s.get("id")).longValue() == lead.sourceId())) {
            throw new LeadValidationException(List.of(new FieldError("sourceId", "Nguồn Lead không tồn tại, đã ngừng hoạt động hoặc sai loại")));
        }
    }

    // -------------------------------------------------------------------------
    // S4-01: Web Form Lead Capture & Embed Code
    // -------------------------------------------------------------------------

    public Lead createWebFormLead(WebFormLeadRequest request, String clientIp) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu gửi lên không được để trống.");
        }

        String fullName = request.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên khách hàng là bắt buộc.");
        }
        fullName = sanitize(fullName);
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

        long id = leadDAO.insert(lead);
        if (id > 0) {
            lead.setId(id);
        }
        return lead;
    }

    public EmbedCodeResponse generateEmbedCode(String formId, String baseUrl, String theme, String title) {
        if (formId == null || formId.isBlank()) {
            formId = "crm-web-lead-form";
        }
        formId = formId.trim().replaceAll("[^A-Za-z0-9_-]", "-");

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8080/crm";
        }
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

        String iframeSnippet = String.format(
                "<iframe src=\"%s\" width=\"100%%\" height=\"650\" frameborder=\"0\" " +
                "style=\"border:none; max-width:580px; width:100%%; border-radius:12px; " +
                "box-shadow:0 10px 25px -5px rgba(15,23,42,0.1); display:block; margin:0 auto;\" " +
                "title=\"%s\" loading=\"lazy\"></iframe>",
                formUrl, title
        );

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
        return input.replaceAll("(?i)<script.*?>.*?</script>", "").trim();
    }

    // -------------------------------------------------------------------------
    // S4-09: Multi-criteria Lead search, SLA calculation, and CRUD
    // -------------------------------------------------------------------------

    public Map<String, Object> searchLeads(LeadFilterRequest filter, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "read");

        int totalRecords = leadDAO.countWithFilters(filter, scope, currentUserId);
        List<Lead> items = leadDAO.findWithFilters(filter, scope, currentUserId);

        long now = System.currentTimeMillis();
        for (Lead lead : items) {
            evaluateSla(lead, now);
        }

        int pageSize = filter.getSize();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("leads", items);
        result.put("page", filter.getPage());
        result.put("size", pageSize);
        result.put("totalRecords", totalRecords);
        result.put("totalItems", totalRecords);
        result.put("totalPages", totalPages > 0 ? totalPages : 1);
        result.put("scope", scope != null ? scope.scopeType().name() : "ALL");
        result.put("stats", leadDAO.getLeadStats(scope, currentUserId));

        return result;
    }

    public Lead getLeadById(long id, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "read");
        Lead lead = leadDAO.findById(id);
        if (lead == null) return null;

        if (scope != null && !scope.canAccessOwner(lead.getAssignedTo() != null ? lead.getAssignedTo() : 0)) {
            if (lead.getAssignedTo() != null && scope.scopeType() == com.crm.service.permissions.ScopeType.SELF 
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
            long defaultSlaMin = lead.getSlaMinutesTotal() > 0 ? lead.getSlaMinutesTotal() : 120;
            lead.setSlaDeadline(new Timestamp(System.currentTimeMillis() + defaultSlaMin * 60 * 1000));
        }

        if (lead.getRating() == null || lead.getRating().isBlank()) {
            lead.setRating("WARM");
            lead.setTier("WARM");
        }

        long id = leadDAO.insert(lead);
        if (id > 0) {
            return getLeadById(id, currentUserId);
        }
        return null;
    }

    public boolean updateLead(Lead lead, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "update");
        Lead existing = leadDAO.findById(lead.getId());
        if (existing == null) {
            throw new NoSuchElementException("Không tìm thấy Lead để cập nhật");
        }
        if (existing.isLocked() || "CONVERTED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Lead đã được chuyển đổi hoặc bị khóa, không thể cập nhật");
        }
        long ownerId = existing.getAssignedTo() != null ? existing.getAssignedTo() : (existing.getOwnerUserId() != null ? existing.getOwnerUserId() : 0L);
        if (scope != null && !scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa Lead này");
        }
        return leadDAO.update(lead);
    }

    public boolean deleteLead(long id, long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "delete");
        Lead existing = leadDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Không tìm thấy Lead để xóa");
        }
        if (existing.isLocked() || "CONVERTED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("Lead đã được chuyển đổi hoặc bị khóa, không thể xóa");
        }
        long ownerId = existing.getAssignedTo() != null ? existing.getAssignedTo() : (existing.getOwnerUserId() != null ? existing.getOwnerUserId() : 0L);
        if (scope != null && !scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền xóa Lead này");
        }
        return leadDAO.delete(id);
    }

    public Map<String, Object> getLeadStats(long currentUserId) throws Exception {
        DataScopeContext scope = scopes.resolve(currentUserId, "read");
        return leadDAO.getLeadStats(scope, currentUserId);
    }

    public void evaluateSla(Lead lead, long now) {
        if (lead == null) return;

        boolean isClosed = "CONVERTED".equalsIgnoreCase(lead.getStatus()) || "LOST".equalsIgnoreCase(lead.getStatus());
        if (isClosed) {
            lead.setOverdueSla(false);
            lead.setSlaStatus("CLOSED");
            lead.setSlaRemainingMinutes(0L);
            lead.setSlaRemainingText("Đã đóng");
            return;
        }

        Timestamp deadline = lead.getSlaDeadline();
        if (deadline == null) {
            lead.setOverdueSla(false);
            lead.setSlaStatus("SAFE");
            lead.setSlaRemainingMinutes(null);
            lead.setSlaRemainingText("Không áp dụng");
            return;
        }

        long diffMs = deadline.getTime() - now;
        long diffMinutes = diffMs / (60 * 1000);

        lead.setSlaRemainingMinutes(diffMinutes);

        if (diffMs <= 0) {
            lead.setOverdueSla(true);
            lead.setSlaStatus("OVERDUE");
            long overdueMins = Math.abs(diffMinutes);
            if (overdueMins < 60) {
                lead.setSlaRemainingText("Quá hạn " + overdueMins + " phút");
            } else {
                long hours = overdueMins / 60;
                long mins = overdueMins % 60;
                lead.setSlaRemainingText("Quá hạn " + hours + "h " + (mins > 0 ? mins + "p" : ""));
            }
        } else {
            lead.setOverdueSla(false);
            if (diffMinutes <= 30) {
                lead.setSlaStatus("WARNING");
            } else {
                lead.setSlaStatus("SAFE");
            }
            if (diffMinutes < 60) {
                lead.setSlaRemainingText("Còn " + diffMinutes + " phút");
            } else {
                long hours = diffMinutes / 60;
                long mins = diffMinutes % 60;
                lead.setSlaRemainingText("Còn " + hours + "h " + (mins > 0 ? mins + "p" : ""));
            }
        }
    }
}
