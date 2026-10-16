package com.crm.controller.leads;

import com.crm.dao.leads.LeadDAO;
import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.*;
import com.crm.model.leads.Lead;
import com.crm.service.importer.BoundedXlsx;
import com.crm.service.leads.*;
import com.crm.service.permissions.DataScopeService;
import com.crm.util.*;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/api/leads/*")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5500000, fileSizeThreshold = 0)
public class LeadServlet extends HttpServlet {

    private final LeadService leads;
    private final LeadImportService imports;
    private final IpRateLimiter rateLimiter;
    private final LeadDAO dao;
    private final LeadService.ScopeResolver scopes;

    public LeadServlet() {
        this(new LeadService(), new LeadImportService(), new IpRateLimiter(5, 60_000));
    }

    public LeadServlet(LeadService leads, LeadImportService imports) {
        this(leads != null ? leads : new LeadService(),
                imports != null ? imports : new LeadImportService(),
                new IpRateLimiter(5, 60_000));
    }

    public LeadServlet(LeadService leads, IpRateLimiter rateLimiter) {
        this(leads != null ? leads : new LeadService(),
                new LeadImportService(),
                rateLimiter != null ? rateLimiter : new IpRateLimiter(5, 60_000));
    }

    public LeadServlet(LeadService leads, LeadImportService imports, IpRateLimiter rateLimiter) {
        this.leads = leads;
        this.imports = imports;
        this.rateLimiter = rateLimiter;
        this.dao = new LeadDAO();
        this.scopes = (user, action) -> new DataScopeService().resolve(user, "lead", action);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
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

        // 2. Public Web Form submission endpoints
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
            // Root path: POST = S4-02 manual lead create, GET = S4-01 lead list
            if (path == null || path.equals("/")) {
                if ("POST".equals(method)) {
                    leads.authorizeCreate(user);
                    ResponseUtil.json(resp, 201, ApiResponse.success(
                            "Tạo Lead thành công",
                            leads.create(user, LeadWriteRequest.fromJson(json(req, 16384)))
                    ));
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
                handleEmbedCode(req, resp);
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

            // S4-02 & S4-05: Single Lead detail by ID (GET) and update (PUT)
            if (path.matches("/[1-9][0-9]*")) {
                long leadId = Long.parseLong(path.substring(1));
                if ("PUT".equals(method)) {
                    var lead = leads.update(user, leadId, LeadWriteRequest.fromJson(json(req, 16384)));
                    ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật Lead thành công", lead));
                    return;
                }
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET, PUT");
                    return;
                }
                var lead = leads.get(user, leadId);
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
            ResponseUtil.json(resp, 403, ApiResponse.error(error.getMessage(), null));
        } catch (JsonSyntaxException | IllegalArgumentException error) {
            ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu yêu cầu không hợp lệ: " + error.getMessage(), null));
        } catch (IllegalStateException error) {
            ResponseUtil.json(resp, 413, ApiResponse.error("Upload vượt dung lượng cho phép", null));
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
        int page = parseInt(request.getParameter("page"), 1);
        int size = parseInt(request.getParameter("size"), 20);
        String status = request.getParameter("status");
        String source = request.getParameter("source");

        var scope = scopes.resolve(user, "read");
        if (page < 1 || size < 1 || size > 100) {
            ResponseUtil.json(response, 400, ApiResponse.error("Phân trang Lead không hợp lệ", null));
            return;
        }

        List<Lead> items = dao.findAll(page, size, status, source, scope);
        int total = dao.count(status, source, scope);

        if ("wrapper".equalsIgnoreCase(request.getParameter("format"))) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            data.put("totalPages", (int) Math.ceil((double) total / size));
            ResponseUtil.json(response, 200, ApiResponse.success("Lấy danh sách Lead thành công", data));
        } else {
            response.setStatus(200);
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write(JsonUtil.getGson().toJson(items));
        }
    }

    private void handleWebFormSubmission(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String clientIp = IpRateLimiter.extractClientIp(req);

        // Anti-Spam & Rate Limiting check
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
            ResponseUtil.json(resp, 201, ApiResponse.success("Tiếp nhận thông tin Lead thành công", created));
        } catch (JsonParseException | IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log("Error processing lead web-form submission", e);
            ResponseUtil.json(resp, 500, ApiResponse.error("Không thể lưu Lead. Vui lòng thử lại.", null));
        }
    }

    private void handleEmbedCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String formId = request.getParameter("formId");
        String theme = request.getParameter("theme");
        String title = request.getParameter("title");

        String baseUrl = resolveBaseUrl(request);
        EmbedCodeResponse embedCode = leads.generateEmbedCode(formId, baseUrl, theme, title);

        ResponseUtil.json(response, 200, ApiResponse.success("Tạo mã nhúng biểu mẫu thành công", embedCode));
    }

    private WebFormLeadRequest parseRequestBody(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();

        // 1. JSON Payload
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

        // 2. Standard Form URL Encoded Payload
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

    private JsonObject json(HttpServletRequest req, int limit) throws Exception {
        if (req.getContentType() == null || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("application/json"))
            throw new IllegalArgumentException("Yêu cầu application/json");
        char[] chars = new char[limit + 1];
        int size = 0, count;
        Reader input = req.getReader();
        while (size < chars.length && (count = input.read(chars, size, chars.length - size)) != -1) size += count;
        if (size > limit) throw new LeadImportService.Failure(413, "JSON vượt giới hạn cho phép", null);
        var body = JsonParser.parseString(new String(chars, 0, size));
        if (!body.isJsonObject()) throw new IllegalArgumentException("Body phải là JSON object");
        return body.getAsJsonObject();
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
