package com.crm.controller.products;

import com.crm.controller.ServerForms;
import com.crm.service.products.QuotePricingService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

@WebServlet({"/quotes/pricing", "/quotes/items", "/quotes/approve"})
public class QuotePricingServlet extends HttpServlet {
    private final QuotePricingService service;

    public QuotePricingServlet() {
        this(new QuotePricingService());
    }

    QuotePricingServlet(QuotePricingService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        if (!ServerForms.authorize(req, res, false)) return;
        try {
            long id = ServerForms.positive(req.getParameter("id"));
            req.setAttribute("quoteId", id);
            req.setAttribute("pricing", service.read(ServerForms.actor(req), id));
            req.getRequestDispatcher("/jsp/products/quote-pricing.jsp").forward(req, res);
        } catch (SecurityException e) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Quote pricing read failed", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!ServerForms.authorize(req, res, false) || !ServerForms.checkCsrf(req, res)) return;
        try {
            long id = ServerForms.positive(req.getParameter("id"));
            if ("/quotes/approve".equals(req.getServletPath())) {
                service.approve(ServerForms.actor(req), id);
            } else if ("/quotes/items".equals(req.getServletPath())) {
                service.addItem(ServerForms.actor(req), id, ServerForms.positive(req.getParameter("productId")),
                        new BigDecimal(req.getParameter("quantity")), new BigDecimal(req.getParameter("unitPrice")));
            } else {
                res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            res.sendRedirect(req.getContextPath() + "/quotes/pricing?id=" + id);
        } catch (SecurityException e) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Quote pricing transaction failed", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
