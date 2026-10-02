package com.crm.controller.audit;

import com.crm.controller.ServerForms;
import com.crm.service.audit.BusinessChangeService;
import com.crm.service.users.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

@WebServlet({"/kpi", "/quotes/discount"})
public class BusinessChangeServlet extends HttpServlet {
    private final BusinessChangeService service;
    private final UserService userService;

    public BusinessChangeServlet() {
        this(new BusinessChangeService(), new UserService());
    }

    BusinessChangeServlet(BusinessChangeService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        if (!ServerForms.authorize(req, res, true)) {
            return;
        }
        if (!"/kpi".equals(req.getServletPath())) {
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            Long actor = ServerForms.actor(req);
            if (actor == null) {
                res.sendRedirect(req.getContextPath() + "/login?expired=1");
                return;
            }
            req.setAttribute("targets", service.targets(actor));
            req.setAttribute("users", userService.findAll());
            req.getRequestDispatcher("/jsp/audit/sales-targets.jsp").forward(req, res);
        } catch (SecurityException e) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            getServletContext().log("Cannot load targets", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        boolean isTarget = "/kpi".equals(req.getServletPath());
        if (!ServerForms.authorize(req, res, isTarget) || !ServerForms.checkCsrf(req, res)) {
            return;
        }
        Long actor = ServerForms.actor(req);
        if (actor == null) {
            res.sendRedirect(req.getContextPath() + "/login?expired=1");
            return;
        }

        try {
            if (isTarget) {
                String userParam = req.getParameter("userId");
                String monthParam = req.getParameter("month");
                String amountParam = req.getParameter("amount");

                if (userParam == null || userParam.isBlank()
                        || monthParam == null || monthParam.isBlank()
                        || amountParam == null || amountParam.isBlank()) {
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu thông tin chỉ tiêu.");
                    return;
                }

                long targetUserId = ServerForms.positive(userParam);
                LocalDate month = YearMonth.parse(monthParam.trim()).atDay(1);
                BigDecimal amount = new BigDecimal(amountParam.trim());

                service.setTarget(actor, targetUserId, month, amount);
                res.sendRedirect(req.getContextPath() + "/kpi?saved=1");
            } else {
                String idParam = req.getParameter("id");
                String discountParam = req.getParameter("discount");

                if (idParam == null || idParam.isBlank()
                        || discountParam == null || discountParam.isBlank()) {
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu thông tin chiết khấu.");
                    return;
                }

                long quoteId = ServerForms.positive(idParam);
                BigDecimal discount = new BigDecimal(discountParam.trim());

                service.changeDiscount(actor, quoteId, discount);
                res.sendRedirect(req.getContextPath() + "/quotes?id=" + quoteId);
            }
        } catch (SecurityException e) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException | DateTimeException | ArithmeticException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Business mutation rolled back", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
