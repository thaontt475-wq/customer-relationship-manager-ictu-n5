package com.crm.controller.contracts;

import com.crm.dto.common.ApiResponse;
import com.crm.service.contracts.ContractService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet({
        "/api/contracts",
        "/api/contracts/*"
})
public class ContractServlet extends HttpServlet {

    private final ContractService contractService = new ContractService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            String path = req.getPathInfo();

            if (path == null || path.equals("/") || path.isEmpty()) {
                String keyword = req.getParameter("keyword");
                String status = req.getParameter("status");
                String customerIdStr = req.getParameter("customerId");
                Long customerId = customerIdStr != null && !customerIdStr.isBlank() ? Long.parseLong(customerIdStr) : null;

                var items = contractService.search(currentUserId, keyword, status, customerId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách hợp đồng thành công", items));
                return;
            }

            long id = parseId(path);
            var item = contractService.getById(currentUserId, id);
            if (item == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy hợp đồng", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thông tin hợp đồng thành công", item));
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

            Map<String, Object> body = JsonUtil.getGson().fromJson(
                    req.getReader(),
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType()
            );
            if (body == null) {
                throw new IllegalArgumentException("Thiếu dữ liệu hợp đồng");
            }

            String contractNumber = (String) body.get("contractNumber");
            String title = (String) body.get("title");
            Long customerId = parseLong(body.get("customerId"));
            Long opportunityId = parseLong(body.get("opportunityId"));
            Long quoteId = parseLong(body.get("quoteId"));
            BigDecimal totalAmount = parseBigDecimal(body.get("totalAmount"));
            LocalDate signedDate = parseDate(body.get("signedDate"));
            LocalDate startDate = parseDate(body.get("startDate"));
            LocalDate endDate = parseDate(body.get("endDate"));
            String paymentTerms = (String) body.get("paymentTerms");
            String status = (String) body.get("status");
            String notes = (String) body.get("notes");

            var created = contractService.create(
                    currentUserId,
                    contractNumber,
                    title,
                    customerId,
                    opportunityId,
                    quoteId,
                    totalAmount,
                    signedDate,
                    startDate,
                    endDate,
                    paymentTerms,
                    status,
                    notes
            );

            ResponseUtil.json(resp, 201, ApiResponse.success("Tạo hợp đồng thành công", created));
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

            Map<String, Object> body = JsonUtil.getGson().fromJson(
                    req.getReader(),
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType()
            );
            if (body == null) {
                throw new IllegalArgumentException("Thiếu dữ liệu cập nhật");
            }

            String contractNumber = (String) body.get("contractNumber");
            String title = (String) body.get("title");
            Long customerId = parseLong(body.get("customerId"));
            Long opportunityId = parseLong(body.get("opportunityId"));
            Long quoteId = parseLong(body.get("quoteId"));
            BigDecimal totalAmount = parseBigDecimal(body.get("totalAmount"));
            LocalDate signedDate = parseDate(body.get("signedDate"));
            LocalDate startDate = parseDate(body.get("startDate"));
            LocalDate endDate = parseDate(body.get("endDate"));
            String paymentTerms = (String) body.get("paymentTerms");
            String status = (String) body.get("status");
            String notes = (String) body.get("notes");

            var updated = contractService.update(
                    currentUserId,
                    id,
                    contractNumber,
                    title,
                    customerId,
                    opportunityId,
                    quoteId,
                    totalAmount,
                    signedDate,
                    startDate,
                    endDate,
                    paymentTerms,
                    status,
                    notes
            );

            ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật hợp đồng thành công", updated));
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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            long id = parseId(req.getPathInfo());

            contractService.delete(currentUserId, id);
            ResponseUtil.json(resp, 200, ApiResponse.success("Xóa hợp đồng thành công", null));
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
        return ((Number) session.getAttribute("userId")).longValue();
    }

    private long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new IllegalArgumentException("Thiếu id hợp đồng");
        }
        String clean = pathInfo.replaceAll("^/+", "").replaceAll("/+$", "");
        int slash = clean.indexOf('/');
        String idStr = slash >= 0 ? clean.substring(0, slash) : clean;
        return Long.parseLong(idStr);
    }

    private Long parseLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        String s = String.valueOf(val).trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        String s = String.valueOf(val).trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(Object val) {
        if (val == null) return null;
        String s = String.valueOf(val).trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        if (s.length() >= 10) s = s.substring(0, 10);
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
