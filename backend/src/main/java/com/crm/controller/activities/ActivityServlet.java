package com.crm.controller.activities;

import com.crm.dto.common.ApiResponse;
import com.crm.service.activities.ActivityService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet({
        "/api/activities",
        "/api/activities/*"
})
public class ActivityServlet extends HttpServlet {

    private final ActivityService activityService = new ActivityService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            String path = req.getPathInfo();

            if (path == null || path.equals("/")) {
                Long customerId = longParam(req, "customerId");
                Long opportunityId = longParam(req, "opportunityId");
                String type = req.getParameter("type");
                String status = req.getParameter("status");

                var items = activityService.search(currentUserId, customerId, opportunityId, type, status);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách hoạt động thành công", items));
                return;
            }

            long id = parseId(path);
            var item = activityService.getById(currentUserId, id);
            if (item == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy hoạt động", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thông tin hoạt động thành công", item));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            Map<String, Object> body = JsonUtil.getGson().fromJson(req.getReader(), Map.class);
            if (body == null) {
                throw new IllegalArgumentException("Thiếu dữ liệu body");
            }

            String subject = (String) body.get("subject");
            String type = (String) body.get("type");
            String description = (String) body.get("description");
            String status = (String) body.get("status");
            String dueDateStr = (String) body.get("dueDate");
            Timestamp dueDate = parseTimestamp(dueDateStr);

            Long customerId = body.get("customerId") != null ? ((Number) body.get("customerId")).longValue() : null;
            Long opportunityId = body.get("opportunityId") != null ? ((Number) body.get("opportunityId")).longValue() : null;

            var created = activityService.create(currentUserId, subject, type, description, status, dueDate, customerId, opportunityId);
            ResponseUtil.json(resp, 201, ApiResponse.success("Tạo hoạt động thành công", created));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            long id = parseId(req.getPathInfo());
            Map<String, Object> body = JsonUtil.getGson().fromJson(req.getReader(), Map.class);

            String subject = (String) body.get("subject");
            String type = (String) body.get("type");
            String description = (String) body.get("description");
            String status = (String) body.get("status");
            String dueDateStr = (String) body.get("dueDate");
            Timestamp dueDate = parseTimestamp(dueDateStr);

            var updated = activityService.update(currentUserId, id, subject, type, description, status, dueDate);
            if (updated == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy hoạt động", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật hoạt động thành công", updated));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            long id = parseId(req.getPathInfo());

            activityService.delete(currentUserId, id);
            ResponseUtil.json(resp, 200, ApiResponse.success("Xóa hoạt động thành công", null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    private long requireUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new SecurityException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
        return (Long) session.getAttribute("userId");
    }

    private long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new IllegalArgumentException("Thiếu activity id");
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = clean.indexOf('/');
        String idStr = slash >= 0 ? clean.substring(0, slash) : clean;
        return Long.parseLong(idStr);
    }

    private Long longParam(HttpServletRequest req, String name) {
        String val = req.getParameter(name);
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Timestamp parseTimestamp(String str) {
        if (str == null || str.isBlank()) return null;
        str = str.trim().replace('T', ' ');
        if (str.length() == 10) {
            str = str + " 00:00:00";
        } else if (str.length() == 16) {
            str = str + ":00";
        }
        try {
            return Timestamp.valueOf(str);
        } catch (Exception e) {
            return null;
        }
    }
}
