package com.crm.controller.winloss;

import com.crm.controller.ServerForms;
import com.crm.model.Competitor;
import com.crm.model.WinLossReason;
import com.crm.service.winloss.WinLossService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Pure HTML forms for S2-10; retains existing JSON /api/winloss endpoints. */
@WebServlet("/winloss")
public class WinLossPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(WinLossPageServlet.class.getName());
    private final WinLossService service;
    public WinLossPageServlet() { this(new WinLossService()); }
    public WinLossPageServlet(WinLossService service) { this.service = service; }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        res.setHeader("Cache-Control", "no-store");
        String tab = tab(req.getParameter("tab"));
        req.setAttribute("tab", tab);
        req.setAttribute("canManage", ServerForms.admin(req));
        req.setAttribute("flash", flash(req.getParameter("result")));
        try {
            var reasons = service.getReasons(true);
            req.setAttribute("winReasons", reasons.winReasons());
            req.setAttribute("lossReasons", reasons.lossReasons());
            req.setAttribute("competitors", service.getCompetitors(true));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Unable to load win/loss configuration", e);
            res.sendError(500, "Không tải được cấu hình thắng/thua.");
            return;
        }
        req.getRequestDispatcher("/jsp/winloss/winloss.jsp").forward(req,res);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        if (!ServerForms.authorize(req,res,true)) return;
        if (!ServerForms.checkCsrf(req,res)) return;
        String tab = tab(req.getParameter("tab"));
        String operation = req.getParameter("operation");
        try {
            if ("delete".equals(operation) && !"yes".equals(req.getParameter("confirm")))
                throw new IllegalArgumentException("Bạn phải xác nhận trước khi xóa hoặc ngừng sử dụng.");
            if ("COMPETITOR".equals(tab)) {
                if ("delete".equals(operation)) {
                    service.deleteCompetitor(ServerForms.positive(req.getParameter("id")));
                    ServerForms.setToast(req, "success", "Xóa đối thủ thành công", "Thông tin đối thủ cạnh tranh đã được xóa.");
                } else {
                    Competitor c = new Competitor();
                    if ("update".equals(operation)) {
                        c.setId(ServerForms.positive(req.getParameter("id")));
                    } else if (!"create".equals(operation)) {
                        throw new IllegalArgumentException("Thao tác không hợp lệ.");
                    }
                    c.setName(req.getParameter("name"));
                    c.setStrengths(req.getParameter("strengths"));
                    c.setWeaknesses(req.getParameter("weaknesses"));
                    c.setWebsite(req.getParameter("website"));
                    c.setActive("true".equals(req.getParameter("active")));
                    c.setDisplayOrder(order(req.getParameter("displayOrder")));
                    if ("create".equals(operation)) {
                        service.createCompetitor(c);
                        ServerForms.setToast(req, "success", "Thêm đối thủ thành công", "Đối thủ cạnh tranh mới đã được tạo.");
                    } else {
                        service.updateCompetitor(c);
                        ServerForms.setToast(req, "success", "Cập nhật thành công", "Thông tin đối thủ cạnh tranh đã được lưu.");
                    }
                }
            } else {
                if ("delete".equals(operation)) {
                    // The submitted reason must belong to the selected tab; do not permit cross-group edits.
                    long id = ServerForms.positive(req.getParameter("id"));
                    var group = "WIN".equals(tab) ? service.getReasons(true).winReasons() : service.getReasons(true).lossReasons();
                    if (group.stream().noneMatch(r -> r.getId() != null && r.getId() == id))
                        throw new IllegalArgumentException("Lý do không thuộc nhóm đã chọn.");
                    service.deleteReason(id);
                    ServerForms.setToast(req, "success", "Xóa lý do thành công", "Lý do thắng/thua đã được gỡ bỏ.");
                } else {
                    if (!"create".equals(operation) && !"update".equals(operation))
                        throw new IllegalArgumentException("Thao tác không hợp lệ.");
                    WinLossReason r = new WinLossReason();
                    r.setType(tab);
                    r.setReasonText(req.getParameter("reasonText"));
                    r.setDescription(req.getParameter("description"));
                    r.setActive("true".equals(req.getParameter("active")));
                    r.setDisplayOrder(order(req.getParameter("displayOrder")));
                    if ("update".equals(operation)) {
                        long id = ServerForms.positive(req.getParameter("id"));
                        var group = "WIN".equals(tab) ? service.getReasons(true).winReasons() : service.getReasons(true).lossReasons();
                        if (group.stream().noneMatch(item -> item.getId() != null && item.getId() == id))
                            throw new IllegalArgumentException("Lý do không thuộc nhóm đã chọn.");
                        r.setId(id);
                        service.updateReason(r);
                        ServerForms.setToast(req, "success", "Cập nhật thành công", "Lý do thắng/thua đã được lưu.");
                    } else {
                        service.createReason(r);
                        ServerForms.setToast(req, "success", "Thêm lý do thành công", "Lý do mới đã được bổ sung vào hệ thống.");
                    }
                }
            }
            res.sendRedirect(req.getContextPath() + "/winloss?tab=" + tab + "&result=ok");
        } catch (IllegalArgumentException e) {
            showError(req,res,tab,e.getMessage(),400);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE,"Winloss HTML form failure",e);
            showError(req,res,tab,"Không thể lưu cấu hình; kiểm tra dữ liệu và thử lại.",500);
        }
    }

    private void showError(HttpServletRequest req, HttpServletResponse res, String tab, String message, int status)
            throws IOException, ServletException {
        req.setAttribute("formError", message);
        res.setStatus(status);
        req.setAttribute("tab",tab);
        req.setAttribute("canManage",true);
        res.setHeader("Cache-Control","no-store");
        try {
            var reasons = service.getReasons(true);
            req.setAttribute("winReasons",reasons.winReasons());
            req.setAttribute("lossReasons",reasons.lossReasons());
            req.setAttribute("competitors",service.getCompetitors(true));
            req.getRequestDispatcher("/jsp/winloss/winloss.jsp").forward(req,res);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE,"Unable to reload winloss after invalid input",e);
            res.sendError(500,"Không tải được cấu hình thắng/thua.");
        }
    }

    private static String tab(String value) {
        if ("LOSS".equalsIgnoreCase(value)) return "LOSS";
        if ("COMPETITOR".equalsIgnoreCase(value)) return "COMPETITOR";
        return "WIN";
    }
    private static int order(String value) {
        if (value == null || value.isBlank()) return 0;
        try { int n = Integer.parseInt(value); if (n >= 0) return n; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Thứ tự phải là số nguyên không âm.");
    }
    private static String flash(String value) { return "ok".equals(value) ? "Đã cập nhật dữ liệu thành công." : null; }
}
