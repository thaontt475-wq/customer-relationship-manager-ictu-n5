package com.crm.controller.quotes;

import com.crm.dto.common.ApiResponse;
import com.crm.service.quotes.QuoteService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet({
        "/api/quotes",
        "/api/quotes/*"
})
public class QuoteServlet extends HttpServlet {

    private final QuoteService quoteService = new QuoteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            String path = req.getPathInfo();

            if (path == null || path.equals("/")) {
                String keyword = req.getParameter("keyword");
                String status = req.getParameter("status");

                var items = quoteService.search(currentUserId, keyword, status);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách báo giá thành công", items));
                return;
            }

            long id = parseId(path);
            var item = quoteService.getById(currentUserId, id);
            if (item == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy báo giá", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thông tin báo giá thành công", item));
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

            Map<String, Object> body = JsonUtil.getGson().fromJson(req.getReader(), Map.class);
            if (body == null) {
                throw new IllegalArgumentException("Thiếu dữ liệu");
            }

            if (path != null && path.contains("/status")) {
                String clean = path.replace("/status", "").replaceAll("^/+", "");
                long quoteId = Long.parseLong(clean);
                String status = (String) body.get("status");
                var updated = quoteService.updateStatus(currentUserId, quoteId, status);
                ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật trạng thái báo giá thành công", updated));
                return;
            }

            String title = (String) body.get("title");
            Long customerId = body.get("customerId") != null ? ((Number) body.get("customerId")).longValue() : null;
            Long opportunityId = body.get("opportunityId") != null ? ((Number) body.get("opportunityId")).longValue() : null;
            BigDecimal discountPercent = body.get("discountPercent") != null ? new BigDecimal(String.valueOf(body.get("discountPercent"))) : BigDecimal.ZERO;
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

            var created = quoteService.create(currentUserId, title, customerId, opportunityId, discountPercent, items);
            ResponseUtil.json(resp, 201, ApiResponse.success("Tạo báo giá thành công", created));
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

            quoteService.delete(currentUserId, id);
            ResponseUtil.json(resp, 200, ApiResponse.success("Xóa báo giá thành công", null));
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
            throw new IllegalArgumentException("Thiếu quote id");
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = clean.indexOf('/');
        String idStr = slash >= 0 ? clean.substring(0, slash) : clean;
        return Long.parseLong(idStr);
    }
}
