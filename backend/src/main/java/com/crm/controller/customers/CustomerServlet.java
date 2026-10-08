package com.crm.controller.customers;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.service.customers.CustomerService;
import com.crm.service.customers.Customer360Service;
import com.crm.service.customers.CustomerRelationService;
import com.crm.service.customers.CustomerCareService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.NoSuchElementException;

@WebServlet({
        "/api/customers",
        "/api/customers/*"
})
public class CustomerServlet extends HttpServlet {

    private final CustomerService customerService = new CustomerService();
    private final Customer360Service customer360 = new Customer360Service();
    private final CustomerRelationService relations = new CustomerRelationService();
    private final CustomerCareService care = new CustomerCareService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            String path = req.getPathInfo();

            if (path == null || path.equals("/")) {
                String keyword = req.getParameter("keyword");
                String status = req.getParameter("status");
                int page = intParam(req, "page", 1);
                int size = intParam(req, "size", 20);

                var result = customerService.search(currentUserId, keyword, status, page, size);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách khách hàng thành công", result));
                return;
            }

            if ("/care-list".equals(path)) {
                int days = intParam(req, "days", 30);
                int page = intParam(req, "page", 1);
                int size = intParam(req, "size", 20);
                ResponseUtil.json(resp, 200, ApiResponse.success("Danh sách cần chăm sóc", care.list(currentUserId, days, page, size)));
                return;
            }
            if (path != null && path.matches("/\\d+/360/?")) {
                var data = customer360.getCustomer360(currentUserId, parseId(path));
                if (data == null) ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy khách hàng", null));
                else ResponseUtil.json(resp, 200, ApiResponse.success("Customer 360", data));
                return;
            }
            if (path != null && path.matches("/\\d+/children/?")) {
                var data = relations.children(currentUserId, parseId(path));
                if (data == null) ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy khách hàng", null));
                else ResponseUtil.json(resp, 200, ApiResponse.success("Danh sách công ty con", data));
                return;
            }
            if (path != null && !path.matches("/\\d+/?")) throw new IllegalArgumentException("Đường dẫn khách hàng không hợp lệ");
            long id = parseId(path);
            var customer = customerService.getById(currentUserId, id);
            if (customer == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy khách hàng", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thông tin khách hàng thành công", customer));
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
            String path = req.getPathInfo();
            if (path != null && path.matches("/\\d+/mark-contacted/?")) {
                java.util.Map<?,?> body = JsonUtil.getGson().fromJson(req.getReader(), java.util.Map.class);
                String note = body == null || body.get("note") == null ? null : String.valueOf(body.get("note"));
                var data = care.markContacted(currentUserId, parseId(path), note);
                if (data == null) ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy khách hàng", null));
                else ResponseUtil.json(resp, 201, ApiResponse.success("Đã ghi nhận liên hệ", data));
                return;
            }
            if (path != null && !path.equals("/")) throw new IllegalArgumentException("Đường dẫn không hợp lệ");
            CustomerWriteRequest body = JsonUtil.getGson().fromJson(req.getReader(), CustomerWriteRequest.class);

            var created = customerService.create(currentUserId, body);
            ResponseUtil.json(resp, 201, ApiResponse.success("Tạo khách hàng thành công", created));
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
            String path = req.getPathInfo();
            if (path != null && path.matches("/\\d+/parent/?")) {
                java.util.Map<?,?> body = JsonUtil.getGson().fromJson(req.getReader(), java.util.Map.class);
                if (body == null || !body.containsKey("parentCustomerId")) throw new IllegalArgumentException("Thiếu parentCustomerId");
                Object raw = body.get("parentCustomerId");
                Long parentId = raw == null ? null : ((Number)raw).longValue();
                var data = relations.setParent(currentUserId, parseId(path), parentId);
                if (data == null) ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy khách hàng", null));
                else ResponseUtil.json(resp, 200, ApiResponse.success("Đã cập nhật công ty mẹ", data));
                return;
            }
            if (path != null && !path.matches("/\\d+/?")) throw new IllegalArgumentException("Đường dẫn không hợp lệ");
            long id = parseId(path);
            CustomerWriteRequest body = JsonUtil.getGson().fromJson(req.getReader(), CustomerWriteRequest.class);

            var updated = customerService.update(currentUserId, id, body);
            if (updated == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy khách hàng", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật khách hàng thành công", updated));
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

            customerService.delete(currentUserId, id);
            ResponseUtil.json(resp, 200, ApiResponse.success("Xóa khách hàng thành công", null));
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
            throw new IllegalArgumentException("Thiếu customer id");
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = clean.indexOf('/');
        String idStr = slash >= 0 ? clean.substring(0, slash) : clean;
        return Long.parseLong(idStr);
    }

    private int intParam(HttpServletRequest req, String name, int fallback) {
        String val = req.getParameter(name);
        if (val == null || val.isBlank()) return fallback;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
