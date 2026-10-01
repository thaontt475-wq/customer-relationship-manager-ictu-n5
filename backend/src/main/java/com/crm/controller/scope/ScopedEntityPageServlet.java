package com.crm.controller.scope;

import com.crm.controller.ServerForms;
import com.crm.service.scope.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/** Server-rendered entry points for the four scoped modules. */
@WebServlet({"/customers", "/opportunities", "/activities", "/quotes"})
public class ScopedEntityPageServlet extends HttpServlet {
    private final DataScopeService service;
    public ScopedEntityPageServlet() { this(new DataScopeService()); }
    public ScopedEntityPageServlet(DataScopeService service) { this.service = service; }
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        var type = ScopeEntityType.fromServletPath("/api" + req.getServletPath());
        res.setHeader("Cache-Control", "no-store");
        req.setAttribute("moduleTitle", switch (type) {
            case CUSTOMERS -> "Khách hàng";
            case OPPORTUNITIES -> "Cơ hội";
            case ACTIVITIES -> "Hoạt động";
            case QUOTES -> "Báo giá";
        });
        try {
            if (req.getParameter("id") != null) {
                var result = service.read(ServerForms.actor(req), type, ServerForms.positive(req.getParameter("id")));
                if (result.status() != DataScopeService.ReadStatus.SUCCESS) {
                    res.sendError(result.status() == DataScopeService.ReadStatus.FORBIDDEN ? 403 : 404);
                    return;
                }
                req.setAttribute("record", result.record());
            } else {
                var records = service.list(ServerForms.actor(req), type, req.getParameter("q"));
                int pages = Math.max(1, (records.size() + 19) / 20);
                int page = 1;
                try { page = Math.max(1, Math.min(pages, Integer.parseInt(req.getParameter("page")))); }
                catch (NumberFormatException ignored) { }
                int from = Math.min(records.size(), (page - 1) * 20);
                req.setAttribute("records", records.subList(from, Math.min(records.size(), from + 20)));
                req.setAttribute("pageNumber", page);
                req.setAttribute("pageCount", pages);
            }
            req.getRequestDispatcher("/jsp/shared/scoped-records.jsp").forward(req, res);
        } catch (IllegalArgumentException e) { res.sendError(400); }
        catch (SQLException e) {
            getServletContext().log("Cannot load scoped records", e);
            res.sendError(500);
        }
    }
}
