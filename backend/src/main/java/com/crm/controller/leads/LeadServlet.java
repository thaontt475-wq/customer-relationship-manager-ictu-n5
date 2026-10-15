package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.LeadRejectRequest;
import com.crm.dto.leads.LeadSlaResponseDTO;
import com.crm.model.leads.Lead;
import com.crm.service.leads.LeadSlaMonitorService;
import com.crm.service.leads.LeadSlaService;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.JsonParseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet({"/api/leads", "/api/leads/*"})
public class LeadServlet extends HttpServlet {

    private final LeadSlaService slaService;

    public LeadServlet() {
        this(new LeadSlaService());
    }

    public LeadServlet(LeadSlaService slaService) {
        this.slaService = slaService;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = normalizePath(req.getPathInfo());
        if ("/health".equals(path)) {
            ResponseUtil.json(resp, 200, ApiResponse.success("Lead API service is running", null));
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Number)) {
            ResponseUtil.json(resp, 401, ApiResponse.error("Chưa đăng nhập", null));
            return;
        }

        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = AuthorizationService.currentUserId(req);
            String path = normalizePath(req.getPathInfo());

            if ("/my-assigned".equals(path)) {
                new LeadSlaMonitorService().checkAndNotifyOverdueLeads();
                List<LeadSlaResponseDTO> list = slaService.getMyAssignedLeads(currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách Lead được phân thành công", list));
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại: " + path, null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log("Lỗi xử lý yêu cầu GET Lead", e);
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ nội bộ", null));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = AuthorizationService.currentUserId(req);
            String path = normalizePath(req.getPathInfo());

            // POST /api/leads/check-sla or /api/leads/sla/trigger-check
            if ("/check-sla".equals(path) || "/sla/trigger-check".equals(path)) {
                int count = new LeadSlaMonitorService().checkAndNotifyOverdueLeads();
                ResponseUtil.json(resp, 200, ApiResponse.success("Đã kiểm tra SLA và phát thông báo", Map.of("notificationsSent", count)));
                return;
            }

            // POST /api/leads/{id}/accept
            if (path.endsWith("/accept") && path.length() > "/accept".length()) {
                long leadId = parseLeadId(path, "/accept");
                LeadSlaResponseDTO result = slaService.acceptLead(currentUserId, leadId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Tiếp nhận Lead thành công. Trạng thái chuyển sang Đang chăm sóc.", result));
                return;
            }

            // POST /api/leads/{id}/reject
            if (path.endsWith("/reject") && path.length() > "/reject".length()) {
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

                LeadSlaResponseDTO result = slaService.rejectLead(currentUserId, leadId, rejectReq.getReason().trim(), rejectReq.getDetails().trim());
                ResponseUtil.json(resp, 200, ApiResponse.success("Từ chối Lead thành công. Lead đã được chuyển về hàng chờ phân bổ.", result));
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại: " + path, null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException | JsonParseException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            log("Lỗi xử lý yêu cầu POST Lead", e);
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ nội bộ", null));
        }
    }

    private long parseLeadId(String path, String suffix) {
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
}
