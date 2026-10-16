package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.LeadAttachCustomerRequest;
import com.crm.dto.leads.LeadDuplicatePairDTO;
import com.crm.dto.leads.LeadMergeRequest;
import com.crm.service.leads.LeadDuplicateMergeService;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet({"/api/leads/duplicates", "/api/leads/merge", "/api/leads/attach-customer", "/api/leads/link-customer"})
public class LeadDuplicateMergeServlet extends HttpServlet {

    private final LeadDuplicateMergeService service;

    public LeadDuplicateMergeServlet() {
        this(new LeadDuplicateMergeService());
    }

    public LeadDuplicateMergeServlet(LeadDuplicateMergeService service) {
        this.service = service;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            HttpSession session = req.getSession(false);
            if (session == null || !(session.getAttribute("userId") instanceof Number)) {
                ResponseUtil.json(resp, 401, ApiResponse.error("Chưa đăng nhập", null));
                return;
            }
            long userId = AuthorizationService.currentUserId(req);
            String route = req.getServletPath();

            if ("/api/leads/duplicates".equals(route)) {
                if (!"GET".equalsIgnoreCase(req.getMethod())) {
                    unsupported(resp, "GET");
                    return;
                }
                List<LeadDuplicatePairDTO> duplicates = service.getDuplicates(userId);
                if ("envelope".equalsIgnoreCase(req.getParameter("format"))
                        || "true".equalsIgnoreCase(req.getParameter("envelope"))) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách lead trùng lặp thành công", duplicates));
                } else {
                    ResponseUtil.rawJson(resp, 200, duplicates);
                }

            } else if ("/api/leads/merge".equals(route)) {
                if (!"POST".equalsIgnoreCase(req.getMethod())) {
                    unsupported(resp, "POST");
                    return;
                }
                JsonObject json = readJsonBody(req, resp);
                if (json == null) return;

                LeadMergeRequest body = new LeadMergeRequest();
                body.masterId = parseJsonId(json, "masterId");
                body.duplicateId = parseJsonId(json, "duplicateId");
                body.retainHistory = json.has("retainHistory") ? json.get("retainHistory").getAsBoolean() : true;

                if (json.has("mergedData") && json.get("mergedData").isJsonObject()) {
                    body.mergedData = JsonUtil.getGson().fromJson(
                            json.getAsJsonObject("mergedData"),
                            new TypeToken<Map<String, Object>>() {}.getType()
                    );
                } else {
                    throw new IllegalArgumentException("Thiếu dữ liệu mergedData");
                }

                Map<String, Object> result = service.merge(userId, body);
                ResponseUtil.json(resp, 200, ApiResponse.success("Gộp Lead thành công", result));

            } else if ("/api/leads/attach-customer".equals(route) || "/api/leads/link-customer".equals(route)) {
                if (!"POST".equalsIgnoreCase(req.getMethod())) {
                    unsupported(resp, "POST");
                    return;
                }
                JsonObject json = readJsonBody(req, resp);
                if (json == null) return;

                LeadAttachCustomerRequest body = new LeadAttachCustomerRequest();
                body.leadId = parseJsonId(json, "leadId");
                body.customerId = parseJsonId(json, "customerId");
                body.notes = json.has("notes") && !json.get("notes").isJsonNull() ? json.get("notes").getAsString() : null;

                Map<String, Object> result = service.attachToCustomer(userId, body);
                ResponseUtil.json(resp, 200, ApiResponse.success("Liên kết Lead vào Khách hàng thành công", result));

            } else {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy API", null));
            }

        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (JsonParseException | IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SQLException e) {
            log("Lead merge database error", e);
            boolean conflict = "23000".equals(e.getSQLState()) || "40001".equals(e.getSQLState()) || e.getErrorCode() == 1205;
            ResponseUtil.json(resp, conflict ? 409 : 500, ApiResponse.error(
                    conflict ? "Dữ liệu xung đột đồng thời; giao tác merge đã tự động rollback"
                            : "Lỗi cơ sở dữ liệu khi xử lý gộp lead", null));
        } catch (Exception e) {
            log("Lead duplicate merge error", e);
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ nội bộ: " + e.getMessage(), null));
        }
    }

    private JsonObject readJsonBody(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            throw new IllegalArgumentException("Yêu cầu Content-Type: application/json");
        }
        char[] buffer = new char[16385];
        int count = 0, n;
        Reader reader = req.getReader();
        while (count < buffer.length && (n = reader.read(buffer, count, buffer.length - count)) != -1) {
            count += n;
        }
        if (count > 16384) {
            ResponseUtil.json(resp, 413, ApiResponse.error("Nội dung JSON tối đa 16KB", null));
            return null;
        }
        if (count == 0) {
            throw new IllegalArgumentException("Nội dung yêu cầu JSON không được để trống");
        }
        JsonObject json = JsonUtil.getGson().fromJson(new String(buffer, 0, count), JsonObject.class);
        if (json == null) {
            throw new IllegalArgumentException("JSON không hợp lệ");
        }
        return json;
    }

    private long parseJsonId(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            throw new IllegalArgumentException(key + " là bắt buộc");
        }
        JsonElement el = json.get(key);
        if (el.isJsonPrimitive()) {
            String val = el.getAsString().trim();
            if (val.startsWith("LD-") || val.startsWith("ld-")) {
                val = val.substring(3);
            } else if (val.startsWith("CUST-") || val.startsWith("cust-")) {
                val = val.substring(5);
            }
            if (val.matches("[1-9][0-9]*")) {
                return Long.parseLong(val);
            }
        }
        throw new IllegalArgumentException(key + " phải là số nguyên dương");
    }

    private void unsupported(HttpServletResponse resp, String allow) throws IOException {
        resp.setHeader("Allow", allow);
        ResponseUtil.json(resp, 405, ApiResponse.error("Phương thức không được hỗ trợ", null));
    }
}
