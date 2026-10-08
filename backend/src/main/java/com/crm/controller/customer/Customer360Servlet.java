package com.crm.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Customer 360 API Servlet
 * Author: Hoàng Văn Thắng
 */
@WebServlet(name = "Customer360Servlet", urlPatterns = {"/api/customer/360"})
public class Customer360Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String customerId = request.getParameter("id");
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = "CUST-001";
        }

        // Mock JSON response matching frontend Customer360DB
        String json = "{\n" +
            "  \"success\": true,\n" +
            "  \"customerId\": \"" + customerId + "\",\n" +
            "  \"name\": \"Tập đoàn Công nghệ VNG\",\n" +
            "  \"parentCompany\": \"NONE\",\n" +
            "  \"groupTotalValue\": 5480000000,\n" +
            "  \"revenue\": 1250000000,\n" +
            "  \"subsidiariesCount\": 3,\n" +
            "  \"openDealsCount\": 3\n" +
            "}";

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(json);
    }
}
