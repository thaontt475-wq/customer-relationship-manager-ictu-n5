package com.crm.controller.pipeline;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.pipeline.OpportunityWriteRequest;
import com.crm.service.pipeline.OpportunityService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet({
        "/api/opportunities",
        "/api/opportunities/*"
})
public class OpportunityServlet extends HttpServlet {

    private final OpportunityService opportunityService = new OpportunityService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            String path = req.getPathInfo();

            if (path == null || path.equals("/")) {
                String keyword = req.getParameter("keyword");
                String status = req.getParameter("status");
                Long stageId = longParam(req, "stageId");
                int page = intParam(req, "page", 1);
                int size = intParam(req, "size", 100);

                var items = opportunityService.search(currentUserId, stageId, status, keyword, page, size);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách cơ hội thành công", items));
                return;
            }

            long id = parseId(path);
            var item = opportunityService.getById(currentUserId, id);
            if (item == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy cơ hội", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thông tin cơ hội thành công", item));
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

            if (path == null || path.equals("/")) {
                OpportunityWriteRequest body = JsonUtil.getGson().fromJson(req.getReader(), OpportunityWriteRequest.class);
                var created = opportunityService.create(currentUserId, body);
                ResponseUtil.json(resp, 201, ApiResponse.success("Tạo cơ hội thành công", created));
                return;
            }

            String[] segments = segments(path);
            if (segments.length == 2) {
                long targetId = Long.parseLong(segments[0]);
                String action = segments[1];

                if ("stage".equals(action)) {
                    Map<String, Object> body = JsonUtil.getGson().fromJson(req.getReader(), Map.class);
                    if (body == null || body.get("stageId") == null) {
                        throw new IllegalArgumentException("Thiếu stageId");
                    }
                    long targetStageId = ((Number) body.get("stageId")).longValue();
                    var updated = opportunityService.changeStage(currentUserId, targetId, targetStageId);
                    ResponseUtil.json(resp, 200, ApiResponse.success("Chuyển giai đoạn thành công", updated));
                    return;
                }

                if ("close".equals(action)) {
                    Map<String, Object> body = JsonUtil.getGson().fromJson(req.getReader(), Map.class);
                    if (body == null || body.get("status") == null) {
                        throw new IllegalArgumentException("Thiếu trạng thái đóng cơ hội (WON/LOST)");
                    }
                    String status = String.valueOf(body.get("status"));
                    Long winReasonId = body.get("winReasonId") != null ? ((Number) body.get("winReasonId")).longValue() : null;
                    Long lossReasonId = body.get("lossReasonId") != null ? ((Number) body.get("lossReasonId")).longValue() : null;
                    Long competitorId = body.get("competitorId") != null ? ((Number) body.get("competitorId")).longValue() : null;
                    String lostReason = body.get("lostReason") != null ? String.valueOf(body.get("lostReason")) : null;

                    var closed = opportunityService.closeDeal(currentUserId, targetId, status, winReasonId, lossReasonId, competitorId, lostReason);
                    ResponseUtil.json(resp, 200, ApiResponse.success("Đóng cơ hội thành công", closed));
                    return;
                }
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại", null));
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
            OpportunityWriteRequest body = JsonUtil.getGson().fromJson(req.getReader(), OpportunityWriteRequest.class);

            var updated = opportunityService.update(currentUserId, id, body);
            if (updated == null) {
                ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy cơ hội", null));
                return;
            }

            ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật cơ hội thành công", updated));
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

            opportunityService.delete(currentUserId, id);
            ResponseUtil.json(resp, 200, ApiResponse.success("Xóa cơ hội thành công", null));
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
            throw new IllegalArgumentException("Thiếu opportunity id");
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = clean.indexOf('/');
        String idStr = slash >= 0 ? clean.substring(0, slash) : clean;
        return Long.parseLong(idStr);
    }

    private String[] segments(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) return new String[0];
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return clean.split("/");
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
