package com.crm.controller.pipeline;

import com.crm.controller.ServerForms;
import com.crm.model.PipelineStage;
import com.crm.service.pipeline.PipelineService;
import com.crm.service.pipeline.StageInUseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** HTML presentation adapter for S2-09. Existing JSON API remains unchanged. */
@WebServlet({"/pipeline", "/pipeline/page"})
public class PipelinePageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(PipelinePageServlet.class.getName());
    private final PipelineService service;
    public PipelinePageServlet() { this(new PipelineService()); }
    public PipelinePageServlet(PipelineService service) { this.service = service; }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        res.setHeader("Cache-Control", "no-store");
        try {
            long pipelineId = req.getParameter("pipelineId") == null ? 1L
                    : ServerForms.positive(req.getParameter("pipelineId"));
            Boolean active = switch (ServerForms.value(req, "active", "")) {
                case "true" -> true;
                case "false" -> false;
                case "" -> null;
                default -> throw new IllegalArgumentException("Trạng thái không hợp lệ.");
            };
            List<PipelineStage> stages = service.getStages(pipelineId, active);
            PipelineStage edit = null;
            if (req.getParameter("edit") != null) {
                if (!ServerForms.admin(req)) { res.sendError(403); return; }
                edit = service.getStageById(ServerForms.positive(req.getParameter("edit")));
                if (edit == null || !Long.valueOf(pipelineId).equals(edit.getPipelineId())) {
                    res.sendError(404); return;
                }
            }
            req.setAttribute("stages", stages);
            req.setAttribute("editStage", edit);
            req.setAttribute("pipelineId", pipelineId);
            req.setAttribute("activeFilter", ServerForms.value(req, "active", ""));
            req.setAttribute("canManage", ServerForms.admin(req));
            if ("ok".equals(req.getParameter("result")))
                req.setAttribute("notice", "Cập nhật pipeline thành công.");
            req.getRequestDispatcher("/jsp/pipeline/pipeline-config.jsp").forward(req, res);
        } catch (IllegalArgumentException e) { res.sendError(400, "Tham số không hợp lệ."); }
        catch (SQLException e) { LOG.log(Level.SEVERE, "Cannot load pipeline", e); res.sendError(500, "Không tải được pipeline."); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");
        if (!ServerForms.authorize(req, res, true)) return;
        if (!ServerForms.checkCsrf(req, res)) return;
        try {
            long pipelineId = ServerForms.positive(req.getParameter("pipelineId"));
            String action = ServerForms.value(req, "action", "");
            switch (action) {
                case "create", "update" -> {
                    PipelineStage stage = new PipelineStage();
                    stage.setPipelineId(pipelineId);
                    stage.setCode(ServerForms.value(req, "code", ""));
                    stage.setName(ServerForms.value(req, "name", ""));
                    stage.setStageOrder(Integer.parseInt(req.getParameter("stageOrder")));
                    stage.setWinProbability(Integer.parseInt(req.getParameter("winProbability")));
                    stage.setRequirements(ServerForms.value(req, "requirements", ""));
                    String active = req.getParameter("active");
                    if (!"true".equals(active) && !"false".equals(active))
                        throw new IllegalArgumentException("Trạng thái không hợp lệ.");
                    stage.setActive("true".equals(active));
                    String type = ServerForms.value(req, "outcome", "normal");
                    if (!List.of("normal", "won", "lost").contains(type))
                        throw new IllegalArgumentException("Loại giai đoạn không hợp lệ.");
                    stage.setWon("won".equals(type));
                    stage.setLost("lost".equals(type));
                    if ("update".equals(action)) {
                        long id = ServerForms.positive(req.getParameter("id"));
                        PipelineStage existing = service.getStageById(id);
                        if (existing == null || !Long.valueOf(pipelineId).equals(existing.getPipelineId())) {
                            res.sendError(404); return;
                        }
                        stage.setId(id);
                        service.updateStage(stage);
                    } else service.createStage(stage);
                }
                case "delete" -> {
                    if (!"yes".equals(req.getParameter("confirm"))) {
                        res.sendError(400, "Cần xác nhận xóa giai đoạn."); return;
                    }
                    long id = ServerForms.positive(req.getParameter("id"));
                    PipelineStage existing = service.getStageById(id);
                    if (existing == null || !Long.valueOf(pipelineId).equals(existing.getPipelineId())) {
                        res.sendError(404); return;
                    }
                    String target = ServerForms.value(req, "targetStageId", "");
                    Long targetId = target.isEmpty() ? null : ServerForms.positive(target);
                    if (targetId != null) {
                        if (targetId == id) throw new IllegalArgumentException("Không thể chuyển sang cùng giai đoạn.");
                        PipelineStage recipient = service.getStageById(targetId);
                        if (recipient == null || !Long.valueOf(pipelineId).equals(recipient.getPipelineId()))
                            throw new IllegalArgumentException("Giai đoạn nhận phải thuộc cùng pipeline.");
                    }
                    if (!service.deleteStage(id, targetId)) { res.sendError(404); return; }
                }
                default -> { res.sendError(400, "Thao tác không hợp lệ."); return; }
            }
            res.sendRedirect(req.getContextPath() + "/pipeline/page?pipelineId=" + pipelineId + "&result=ok");
        } catch (StageInUseException e) { res.sendError(409, "Giai đoạn đang có cơ hội. Chọn giai đoạn nhận trước khi xóa."); }
        catch (IllegalArgumentException e) { res.sendError(400, "Dữ liệu không hợp lệ: " + e.getMessage()); }
        catch (SQLException e) { LOG.log(Level.SEVERE, "Cannot save pipeline", e); res.sendError(500, "Không cập nhật được pipeline."); }
    }
}
