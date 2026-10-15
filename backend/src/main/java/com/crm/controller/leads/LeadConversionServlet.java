package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.LeadConversionRequest;
import com.crm.dto.leads.LeadConversionResultDTO;
import com.crm.dto.leads.LeadConvertDuplicateCheckDTO;
import com.crm.dto.leads.LeadDTO;
import com.crm.service.leads.LeadConversionService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.*;

@WebServlet({
        "/api/leads/check-convert-duplicates",
        "/api/leads/convert",
        "/api/leads/*"
})
public class LeadConversionServlet extends HttpServlet {

    private final LeadConversionService conversionService;

    public LeadConversionServlet() {
        this(new LeadConversionService());
    }

    public LeadConversionServlet(LeadConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            HttpSession session = req.getSession(false);
            if (session == null || !(session.getAttribute("userId") instanceof Number id)) {
                ResponseUtil.json(resp, 401, ApiResponse.error("Chưa đăng nhập", null));
                return;
            }
            long currentUserId = id.longValue();

            String servletPath = req.getServletPath() != null ? req.getServletPath() : "";
            String pathInfo = req.getPathInfo() != null ? req.getPathInfo() : "";
            String fullPath = servletPath + pathInfo;
            String method = req.getMethod() != null ? req.getMethod().toUpperCase(Locale.ROOT) : "GET";

            // 1. GET /api/leads/{id}/check-convert-duplicates or /api/leads/check-convert-duplicates?leadId=...
            if (fullPath.endsWith("/check-convert-duplicates")) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                long leadId = extractLeadId(fullPath, "/check-convert-duplicates", req.getParameter("leadId"));
                LeadConvertDuplicateCheckDTO result = conversionService.checkConvertDuplicates(currentUserId, leadId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Kiểm tra trùng lặp thành công", result));
                return;
            }

            // 2. POST /api/leads/{id}/convert or /api/leads/convert
            if (fullPath.endsWith("/convert")) {
                if (!"POST".equals(method)) {
                    unsupported(resp, "POST");
                    return;
                }
                JsonObject bodyJson = readJson(req, 32768);
                long leadId = extractLeadId(fullPath, "/convert",
                        bodyJson.has("leadId") && !bodyJson.get("leadId").isJsonNull() ? bodyJson.get("leadId").getAsString() : req.getParameter("leadId"));

                LeadConversionRequest request = JsonUtil.getGson().fromJson(bodyJson, LeadConversionRequest.class);
                LeadConversionResultDTO result = conversionService.convert(currentUserId, leadId, request);
                ResponseUtil.json(resp, 200, ApiResponse.success("Chuyển đổi Lead thành công", result));
                return;
            }

            // 3. GET /api/leads/{id}
            if (pathInfo.matches("^/[^/]+$") && !"GET".equals(pathInfo) && !"/".equals(pathInfo)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                String rawId = pathInfo.substring(1);
                long leadId = parseLeadId(rawId);
                Map<String, Object> lead = conversionService.getLeadById(currentUserId, leadId);
                if (lead == null) {
                    ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy Lead", null));
                    return;
                }
                ResponseUtil.json(resp, 200, ApiResponse.success("Thông tin Lead", lead));
                return;
            }

            // 4. GET /api/leads or /api/leads/ (List leads for conversion)
            if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
                if (!"GET".equals(method)) {
                    unsupported(resp, "GET");
                    return;
                }
                String status = req.getParameter("status");
                String keyword = req.getParameter("keyword");
                int limit = parsePositiveInt(req.getParameter("limit"), 50);
                int offset = parsePositiveInt(req.getParameter("offset"), 0);

                List<LeadDTO> leads = conversionService.getLeads(currentUserId, status, keyword, limit, offset);
                ResponseUtil.json(resp, 200, ApiResponse.success("Danh sách Lead", leads));
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy API Lead", null));

        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException | JsonParseException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SQLException e) {
            log("Lead conversion database error", e);
            boolean isConflict = "23000".equals(e.getSQLState()) || "40001".equals(e.getSQLState()) || e.getErrorCode() == 1205;
            ResponseUtil.json(resp, isConflict ? 409 : 500, ApiResponse.error(
                    isConflict ? "Xung đột dữ liệu đồng thời; giao tác chuyển đổi đã được rollback"
                            : "Lỗi cơ sở dữ liệu khi chuyển đổi lead", null));
        } catch (Exception e) {
            log("Lead conversion unexpected error", e);
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ nội bộ: " + e.getMessage(), null));
        }
    }

    private long extractLeadId(String fullPath, String suffix, String fallbackParam) {
        if (fullPath.endsWith(suffix) && fullPath.length() > suffix.length()) {
            String prefix = fullPath.substring(0, fullPath.length() - suffix.length());
            int lastSlash = prefix.lastIndexOf('/');
            if (lastSlash >= 0) {
                String candidate = prefix.substring(lastSlash + 1).trim();
                if (!candidate.isEmpty() && !"leads".equalsIgnoreCase(candidate)) {
                    return parseLeadId(candidate);
                }
            }
        }
        if (fallbackParam != null && !fallbackParam.isBlank()) {
            return parseLeadId(fallbackParam);
        }
        throw new IllegalArgumentException("Thiếu mã định danh Lead để xử lý");
    }

    public static long parseLeadId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã định danh Lead");
        }
        String clean = raw.trim();
        if (clean.matches("^[0-9]+$")) {
            return Long.parseLong(clean);
        }
        String digits = clean.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            return Long.parseLong(digits);
        }
        throw new IllegalArgumentException("Mã định danh Lead không hợp lệ: " + raw);
    }

    private JsonObject readJson(HttpServletRequest req, int limit) throws IOException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            throw new IllegalArgumentException("Yêu cầu Content-Type: application/json");
        }
        char[] buffer = new char[limit + 1];
        int count = 0, n;
        Reader reader = req.getReader();
        while (count < buffer.length && (n = reader.read(buffer, count, buffer.length - count)) != -1) {
            count += n;
        }
        if (count > limit) {
            throw new IllegalArgumentException("Nội dung JSON vượt giới hạn cho phép");
        }
        if (count == 0) {
            throw new IllegalArgumentException("Nội dung yêu cầu JSON không được để trống");
        }
        return JsonParser.parseString(new String(buffer, 0, count)).getAsJsonObject();
    }

    private int parsePositiveInt(String val, int def) {
        if (val == null || val.isBlank()) return def;
        try {
            int v = Integer.parseInt(val.trim());
            return v >= 0 ? v : def;
        } catch (Exception e) {
            return def;
        }
    }

    private void unsupported(HttpServletResponse resp, String allow) throws IOException {
        resp.setHeader("Allow", allow);
        ResponseUtil.json(resp, 405, ApiResponse.error("Phương thức không được hỗ trợ", null));
    }
}
