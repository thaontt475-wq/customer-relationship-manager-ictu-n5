package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.EmbedCodeResponse;
import com.crm.dto.leads.WebFormLeadRequest;
import com.crm.model.leads.Lead;
import com.crm.service.leads.LeadService;
import com.crm.util.IpRateLimiter;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "LeadServlet", urlPatterns = {"/api/leads", "/api/leads/*"})
public class LeadServlet extends HttpServlet {

    private final LeadService leadService;
    private final IpRateLimiter rateLimiter;

    public LeadServlet() {
        this.leadService = new LeadService();
        this.rateLimiter = IpRateLimiter.getInstance();
    }

    public LeadServlet(LeadService leadService, IpRateLimiter rateLimiter) {
        this.leadService = leadService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        String action = request.getParameter("action");

        // 1. Generate Embed Code API
        if ("/embed-code".equalsIgnoreCase(pathInfo) || "embed-code".equalsIgnoreCase(action)) {
            handleEmbedCode(request, response);
            return;
        }

        // 2. Health check endpoint
        if ("/health".equalsIgnoreCase(pathInfo)) {
            ResponseUtil.json(response, 200, ApiResponse.success("Lead API service is running", null));
            return;
        }

        // 3. List leads (with pagination for management views)
        try {
            int page = parseInt(request.getParameter("page"), 1);
            int size = parseInt(request.getParameter("size"), 20);
            String status = request.getParameter("status");
            String source = request.getParameter("source");

            com.crm.dao.leads.LeadDAO dao = new com.crm.dao.leads.LeadDAO();
            List<Lead> leads = dao.findAll(page, size, status, source);
            int total = dao.count(status, source);

            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("items", leads);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            data.put("totalPages", (int) Math.ceil((double) total / size));

            ResponseUtil.json(response, 200, ApiResponse.success("Lấy danh sách Lead thành công", data));
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Lỗi khi tải danh sách Lead: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = IpRateLimiter.extractClientIp(request);

        // Anti-Spam & Rate Limiting check: max 5 requests / 1 minute per IP
        if (!rateLimiter.tryAcquire(clientIp)) {
            response.setHeader("Retry-After", "60");
            ResponseUtil.json(response, 429, ApiResponse.error(
                    "Quá nhiều yêu cầu từ địa chỉ IP của bạn. Vui lòng thử lại sau 1 phút.",
                    null
            ));
            return;
        }

        try {
            WebFormLeadRequest leadRequest = parseRequestBody(request);

            if (leadRequest == null) {
                ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu gửi lên không hợp lệ.", null));
                return;
            }

            Lead created = leadService.createWebFormLead(leadRequest, clientIp);

            ResponseUtil.json(response, 201, ApiResponse.success("Tiếp nhận thông tin Lead thành công", created));

        } catch (IllegalArgumentException e) {
            ResponseUtil.json(response, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing lead web-form submission: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(response, 500, ApiResponse.error("Lỗi hệ thống khi lưu trữ thông tin Lead: " + e.getMessage(), null));
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
        // Support custom backend base override if configured
        String envBase = System.getenv("CRM_BACKEND_PUBLIC_URL");
        if (envBase != null && !envBase.isBlank()) {
            return envBase.trim().replaceAll("/+$", "");
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();

        // Check proxy forwarding headers
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            scheme = forwardedProto.split(",")[0].trim();
        }
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            host = forwardedHost.split(",")[0].trim();
            // X-Forwarded-Host may include port
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

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
