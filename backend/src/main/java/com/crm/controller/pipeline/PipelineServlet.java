package com.crm.controller.pipeline;

import com.crm.model.PipelineStage;
import com.crm.model.User;
import com.crm.service.pipeline.PipelineService;
import com.crm.service.pipeline.PipelineService.TransitionContext;
import com.crm.service.pipeline.PipelineService.TransitionValidationResult;
import com.crm.service.pipeline.StageInUseException;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling Pipeline Stages and Transition Rules (CRM-47):
 * - GET    /api/pipeline/stages                     — List stages sorted by stage_order ASC
 * - GET    /api/pipeline/stages/{id}                — Get single stage details
 * - POST   /api/pipeline/stages                     — Create new stage
 * - POST   /api/pipeline/stages/validate-transition — Validate opportunity stage transition
 * - PUT    /api/pipeline/stages/{id}                — Update stage (data integrity: preserves opportunities)
 * - PUT    /api/pipeline/stages/reorder             — Reorder stages
 * - DELETE /api/pipeline/stages/{id}                — Delete stage (blocked with 409 if opportunities exist)
 */
@WebServlet({
        "/api/pipeline/stages",
        "/api/pipeline/stages/*",
        "/api/stages",
        "/api/stages/*"
})
public class PipelineServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PipelineServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final PipelineService pipelineService;

    public PipelineServlet() {
        this.pipelineService = new PipelineService();
    }

    public PipelineServlet(PipelineService pipelineService) {
        this.pipelineService = pipelineService != null ? pipelineService : new PipelineService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        String pathInfo = request.getPathInfo();
        Long stageId = parseNumericId(pathInfo);

        try {
            if (stageId != null) {
                // View single stage
                PipelineStage stage = pipelineService.getStageById(stageId);
                if (stage == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                            "Không tìm thấy giai đoạn với ID: " + stageId, null);
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy thông tin giai đoạn thành công", stage);
            } else {
                // List stages
                long pipelineId = parseLongParam(request.getParameter("pipelineId"), 1L);
                String activeStr = request.getParameter("active");
                Boolean activeOnly = null;
                if ("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr)) {
                    activeOnly = true;
                } else if ("false".equalsIgnoreCase(activeStr) || "0".equals(activeStr)) {
                    activeOnly = false;
                }

                List<PipelineStage> stages = pipelineService.getStages(pipelineId, activeOnly);
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy danh sách giai đoạn bán hàng thành công", stages);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-47: Database error loading stages", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tải danh sách giai đoạn", null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // Check method override for HTML forms
        String methodOverride = request.getParameter("_method");
        if ("PUT".equalsIgnoreCase(methodOverride)) {
            doPut(request, response);
            return;
        } else if ("DELETE".equalsIgnoreCase(methodOverride)) {
            doDelete(request, response);
            return;
        }

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        String pathInfo = request.getPathInfo();

        // Sub-action: Validate Stage Transition (Rule 3)
        if (pathInfo != null && pathInfo.contains("/validate-transition")) {
            handleValidateTransition(request, response);
            return;
        }

        PipelineStage stage = parseStagePayload(request);
        if (stage == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dữ liệu giai đoạn không hợp lệ", null);
            return;
        }

        try {
            PipelineStage created = pipelineService.createStage(stage);
            writeJson(response, HttpServletResponse.SC_CREATED, true,
                    "Tạo giai đoạn bán hàng mới thành công.", created);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-47: Database error creating stage", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tạo giai đoạn", null);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        String pathInfo = request.getPathInfo();

        // Sub-action: Reorder stages
        if (pathInfo != null && pathInfo.contains("/reorder")) {
            handleReorderStages(request, response);
            return;
        }

        PipelineStage stage = parseStagePayload(request);
        if (stage == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dữ liệu giai đoạn không hợp lệ", null);
            return;
        }

        Long pathId = parseNumericId(pathInfo);
        if (pathId != null && pathId > 0) {
            stage.setId(pathId);
        }

        if (stage.getId() == null || stage.getId() <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu ID giai đoạn cần cập nhật", null);
            return;
        }

        try {
            PipelineStage updated = pipelineService.updateStage(stage);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Cập nhật giai đoạn bán hàng thành công.", updated);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-47: Database error updating stage", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi cập nhật giai đoạn", null);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        Long stageId = parseNumericId(request.getPathInfo());
        if (stageId == null || stageId <= 0) {
            String paramId = request.getParameter("id");
            if (paramId != null) {
                try {
                    stageId = Long.parseLong(paramId.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (stageId == null || stageId <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu ID giai đoạn cần xóa", null);
            return;
        }

        // Optional target stage ID for safe migration of opportunities
        Long targetStageId = null;
        String reassignParam = request.getParameter("targetStageId");
        if (reassignParam == null) {
            reassignParam = request.getParameter("reassignTo");
        }
        if (reassignParam != null && !reassignParam.isBlank()) {
            try {
                targetStageId = Long.parseLong(reassignParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        try {
            boolean deleted = pipelineService.deleteStage(stageId, targetStageId);
            if (!deleted) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy giai đoạn với ID: " + stageId, null);
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Xóa giai đoạn thành công.", null);

        } catch (StageInUseException e) {
            // CRITICAL BUSINESS RULE 4: Block deletion when opportunities are present -> HTTP 409 Conflict
            LOGGER.log(Level.WARNING, "CRM-47: Blocked deletion of stage in use: {0}", e.getMessage());
            writeJson(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage(),
                    Map.of("stageId", e.getStageId(), "opportunityCount", e.getOpportunityCount()));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-47: Database error deleting stage", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi xóa giai đoạn", null);
        }
    }

    // === Transition Validation Handler ===

    private void handleValidateTransition(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            TransitionRequestDTO dto = GSON.fromJson(request.getReader(), TransitionRequestDTO.class);
            if (dto == null || dto.targetStageId() <= 0) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu thông tin giai đoạn đích", null);
                return;
            }

            PipelineStage currentStage = dto.currentStageId() > 0 ? pipelineService.getStageById(dto.currentStageId()) : null;
            PipelineStage targetStage = pipelineService.getStageById(dto.targetStageId());

            if (targetStage == null) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND, false, "Không tìm thấy giai đoạn đích", null);
                return;
            }

            TransitionContext context = new TransitionContext(
                    dto.amount(),
                    dto.contactName(),
                    dto.lostReason(),
                    dto.requirementsConfirmed()
            );

            TransitionValidationResult result = pipelineService.validateTransition(currentStage, targetStage, context);

            if (result.valid()) {
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Điều kiện chuyển giai đoạn hợp lệ.", result);
            } else {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Không đủ điều kiện để chuyển sang giai đoạn '" + targetStage.getName() + "'.", result);
            }

        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "JSON không hợp lệ", null);
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Lỗi kiểm tra điều kiện", null);
        }
    }

    private void handleReorderStages(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            ReorderRequestDTO dto = GSON.fromJson(request.getReader(), ReorderRequestDTO.class);
            if (dto == null || dto.stageIds() == null || dto.stageIds().isEmpty()) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu danh sách thứ tự giai đoạn", null);
                return;
            }

            long pipelineId = dto.pipelineId() > 0 ? dto.pipelineId() : 1L;
            boolean success = pipelineService.reorderStages(pipelineId, dto.stageIds());

            if (success) {
                writeJson(response, HttpServletResponse.SC_OK, true, "Cập nhật thứ tự giai đoạn thành công.", null);
            } else {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Không thể cập nhật thứ tự", null);
            }
        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "JSON không hợp lệ", null);
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Lỗi hệ thống khi sắp xếp", null);
        }
    }

    // === Helpers ===

    private PipelineStage parseStagePayload(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                return GSON.fromJson(request.getReader(), PipelineStage.class);
            } catch (JsonSyntaxException e) {
                return null;
            }
        }

        // Form urlencoded parsing
        PipelineStage s = new PipelineStage();
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.isBlank()) {
            try {
                s.setId(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String pipelineIdStr = request.getParameter("pipelineId");
        if (pipelineIdStr != null && !pipelineIdStr.isBlank()) {
            try {
                s.setPipelineId(Long.parseLong(pipelineIdStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        s.setCode(request.getParameter("code"));
        s.setName(request.getParameter("name"));

        String orderStr = request.getParameter("stageOrder");
        if (orderStr == null) {
            orderStr = request.getParameter("order");
        }
        if (orderStr != null && !orderStr.isBlank()) {
            try {
                s.setStageOrder(Integer.parseInt(orderStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String probStr = request.getParameter("winProbability");
        if (probStr == null) {
            probStr = request.getParameter("probability");
        }
        if (probStr != null && !probStr.isBlank()) {
            try {
                s.setWinProbability(Integer.parseInt(probStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        s.setRequirements(request.getParameter("requirements"));

        String wonStr = request.getParameter("isWon");
        if (wonStr == null) wonStr = request.getParameter("won");
        if (wonStr != null) {
            s.setWon("true".equalsIgnoreCase(wonStr) || "1".equals(wonStr));
        }

        String lostStr = request.getParameter("isLost");
        if (lostStr == null) lostStr = request.getParameter("lost");
        if (lostStr != null) {
            s.setLost("true".equalsIgnoreCase(lostStr) || "1".equals(lostStr));
        }

        String activeStr = request.getParameter("active");
        if (activeStr == null) activeStr = request.getParameter("is_active");
        if (activeStr != null) {
            s.setActive("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr) || "on".equalsIgnoreCase(activeStr));
        }

        return s;
    }

    private Long parseNumericId(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo.trim())) {
            return null;
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = clean.indexOf('/');
        if (slash != -1) {
            clean = clean.substring(0, slash);
        }
        try {
            long id = Long.parseLong(clean.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongParam(String param, long defaultValue) {
        if (param == null || param.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(param.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long extractActorUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object directUserId = session.getAttribute("userId");
        if (directUserId instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        if (directUserId instanceof String text) {
            try {
                long parsed = Long.parseLong(text);
                if (parsed > 0) return parsed;
            } catch (NumberFormatException ignored) {}
        }
        Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        if (currentUser instanceof User u && u.getId() > 0) {
            return u.getId();
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    private record ApiResponse(boolean success, String message, Object data) {}

    private record TransitionRequestDTO(
            long currentStageId,
            long targetStageId,
            BigDecimal amount,
            String contactName,
            String lostReason,
            boolean requirementsConfirmed
    ) {}

    private record ReorderRequestDTO(long pipelineId, List<Long> stageIds) {}
}