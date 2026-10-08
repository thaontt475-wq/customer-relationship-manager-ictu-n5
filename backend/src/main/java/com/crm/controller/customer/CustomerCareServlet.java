package com.crm.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Customer Care Follow-up API Servlet
 * Author: Hoàng Văn Thắng
 */
@WebServlet(name = "CustomerCareServlet", urlPatterns = {"/api/customer/care"})
public class CustomerCareServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String nDaysParam = request.getParameter("nDays");
        int nDays = 15;
        if (nDaysParam != null && !nDaysParam.trim().isEmpty()) {
            try {
                nDays = Integer.parseInt(nDaysParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        String json = "{\n" +
            "  \"success\": true,\n" +
            "  \"filterNDays\": " + nDays + ",\n" +
            "  \"totalPending\": 7,\n" +
            "  \"overdue30Days\": 3,\n" +
            "  \"totalValueAtRisk\": 4850000000,\n" +
            "  \"contactedToday\": 2\n" +
            "}";

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(json);
    }
}
