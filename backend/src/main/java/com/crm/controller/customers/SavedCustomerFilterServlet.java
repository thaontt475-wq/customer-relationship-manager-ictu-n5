package com.crm.controller.customers;

import com.crm.dao.customers.SavedCustomerFilterDAO.SavedFilter;
import com.crm.dto.common.ApiResponse;
import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.service.customers.SavedCustomerFilterService;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.*;
import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;

/** Proposed CUSTOMER contract; group confirmation is still required. */
@WebServlet({"/api/saved-filters", "/api/saved-filters/*"})
public class SavedCustomerFilterServlet extends HttpServlet {
    private final SavedCustomerFilterService service;
    public SavedCustomerFilterServlet() { this(new SavedCustomerFilterService()); }
    public SavedCustomerFilterServlet(SavedCustomerFilterService service) { this.service = service; }

    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getSession(false) == null || !(req.getSession(false).getAttribute("userId") instanceof Number)) {
                ResponseUtil.json(resp, 401, ApiResponse.error("Chưa đăng nhập", null)); return;
            }
            long user = AuthorizationService.currentUserId(req);
            String path = req.getPathInfo(); boolean collection = path == null || path.equals("/");
            if (!collection && !path.matches("/[1-9][0-9]*/?")) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy API bộ lọc", null)); return;
            }
            Object data; int status = 200;
            switch (req.getMethod()) {
                case "GET" -> {
                    if (collection) {
                        entity(req.getParameter("entity"));
                        int page = integer(req.getParameter("page"), "page", 1);
                        int size = integer(req.getParameter("size"), "size", 20);
                        data = service.list(user, page, size).stream().map(this::response).toList();
                    } else data = response(service.get(user, pathId(path)));
                }
                case "POST" -> {
                    if (!collection) { unsupported(resp, "GET, DELETE"); return; }
                    JsonObject json = body(req);
                    entity(string(json, "entity"));
                    if (!json.has("criteria") || !json.get("criteria").isJsonObject()) throw new IllegalArgumentException("criteria phải là object");
                    data = response(service.create(user, string(json, "name"), criteria(json.getAsJsonObject("criteria"))));
                    status = 201;
                }
                case "DELETE" -> {
                    if (collection) { unsupported(resp, "GET, POST"); return; }
                    service.delete(user, pathId(path)); data = null;
                }
                default -> { unsupported(resp, collection ? "GET, POST" : "GET, DELETE"); return; }
            }
            ResponseUtil.json(resp, status, ApiResponse.success("Xử lý bộ lọc thành công", data));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (JsonParseException | IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error("Bộ lọc không hợp lệ: " + e.getMessage(), null));
        } catch (Exception e) {
            log("Saved Customer filter failed", e);
            ResponseUtil.json(resp, 500, ApiResponse.error("Không thể xử lý bộ lọc", null));
        }
    }

    private JsonObject body(HttpServletRequest req) throws IOException {
        char[] chars = new char[16385]; int count = 0, n; Reader reader = req.getReader();
        while (count < chars.length && (n = reader.read(chars, count, chars.length - count)) != -1) count += n;
        if (count > 16384) throw new IllegalArgumentException("JSON tối đa 16384 ký tự");
        JsonObject json = JsonUtil.getGson().fromJson(new String(chars, 0, count), JsonObject.class);
        if (json == null) throw new IllegalArgumentException("Thiếu JSON");
        return json;
    }
    private CustomerSearchFilter criteria(JsonObject json) {
        Set<String> allowed = Set.of("keyword", "status", "industry", "size", "region", "ownerId");
        for (String key : json.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Điều kiện không hỗ trợ: " + key);
        String industry = scalar(json, "industry"); Long industryId = null;
        if (industry != null && industry.matches("[0-9]+")) { industryId = CustomerSearchFilter.id(industry, "industry"); industry = null; }
        return new CustomerSearchFilter(string(json, "keyword"), string(json, "status"), industryId,
                CustomerSearchFilter.id(scalar(json, "size"), "size"), string(json, "region"),
                CustomerSearchFilter.id(scalar(json, "ownerId"), "ownerId"), industry);
    }
    private String string(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        if (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isString()) throw new IllegalArgumentException(key + " phải là chuỗi");
        return json.get(key).getAsString();
    }
    private String scalar(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return null;
        if (!json.get(key).isJsonPrimitive() || json.getAsJsonPrimitive(key).isBoolean()) throw new IllegalArgumentException(key + " không hợp lệ");
        return json.get(key).getAsString();
    }
    private Map<String, Object> response(SavedFilter saved) {
        var filter = saved.filter(); Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("keyword", filter.search()); criteria.put("status", filter.status());
        criteria.put("industry", filter.industryId() == null ? filter.industry() : filter.industryId());
        criteria.put("size", filter.companySizeId()); criteria.put("region", filter.region()); criteria.put("ownerId", filter.ownerId());
        return Map.of("id", saved.id(), "entity", "CUSTOMER", "name", saved.name(), "criteria", criteria, "createdAt", saved.createdAt());
    }
    private void entity(String entity) {
        if (!"CUSTOMER".equals(entity)) throw new IllegalArgumentException("entity phải là CUSTOMER");
    }
    private long pathId(String path) { return Long.parseLong(path.replace("/", "")); }
    private int integer(String value, String field, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        Long id = CustomerSearchFilter.id(value, field);
        if (id > Integer.MAX_VALUE) throw new IllegalArgumentException(field + " vượt giới hạn");
        return id.intValue();
    }
    private void unsupported(HttpServletResponse resp, String allow) throws IOException {
        resp.setHeader("Allow", allow); ResponseUtil.json(resp, 405, ApiResponse.error("HTTP method không hỗ trợ", null));
    }
}
