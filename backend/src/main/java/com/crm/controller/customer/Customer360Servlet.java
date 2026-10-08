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

        String countParam = request.getParameter("activities");
        boolean generate500Activities = "500".equals(countParam) || "true".equalsIgnoreCase(request.getParameter("benchmark"));

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"success\": true,\n");
        sb.append("  \"customerId\": \"").append(customerId).append("\",\n");
        sb.append("  \"name\": \"Tập đoàn Công nghệ VNG\",\n");
        sb.append("  \"code\": \"CUST-2026-089\",\n");
        sb.append("  \"industry\": \"Công nghệ & Game\",\n");
        sb.append("  \"parentCompany\": \"NONE\",\n");
        sb.append("  \"owner\": \"Hoàng Văn Thắng\",\n");
        sb.append("  \"healthScore\": 95,\n");
        sb.append("  \"phone\": \"028 3962 3888\",\n");
        sb.append("  \"email\": \"contact@vng.com.vn\",\n");
        sb.append("  \"taxCode\": \"0303538466\",\n");
        sb.append("  \"address\": \"Z06 Đường số 13, KCX Tân Thuận, Quận 7, TP. HCM\",\n");
        sb.append("  \"website\": \"https://vng.com.vn\",\n");
        sb.append("  \"signedTotalValue\": 1250000000,\n");
        sb.append("  \"openDealsValue\": 655000000,\n");
        sb.append("  \"groupTotalValue\": 5480000000,\n");
        sb.append("  \"revenue\": 1250000000,\n");
        sb.append("  \"subsidiariesCount\": 3,\n");
        sb.append("  \"openDealsCount\": 3,\n");
        sb.append("  \"activitiesCount\": ").append(generate500Activities ? 500 : 4).append(",\n");
        sb.append("  \"activities\": [\n");

        int totalActs = generate500Activities ? 500 : 4;
        String[] types = {"meeting", "call", "email", "note", "care"};
        String[] titles = {
            "Họp trao đổi kỹ thuật tích hợp hệ thống",
            "Cuộc gọi chăm sóc khách hàng định kỳ",
            "Gửi báo giá đề xuất giải pháp doanh nghiệp",
            "Ghi chú rà soát tiến độ triển khai dự án",
            "Thăm hỏi và đánh giá mức độ hài lòng khách hàng VIP"
        };

        for (int i = 0; i < totalActs; i++) {
            String type = types[i % types.length];
            String title = titles[i % titles.length] + (generate500Activities ? " #" + (500 - i) : "");
            String time = (i == 0) ? "Hôm nay, 14:30" : (i == 1) ? "Hôm qua, 09:15" : (i + " ngày trước");
            sb.append("    {\n");
            sb.append("      \"id\": ").append(i + 1).append(",\n");
            sb.append("      \"type\": \"").append(type).append("\",\n");
            sb.append("      \"title\": \"").append(title).append("\",\n");
            sb.append("      \"author\": \"Hoàng Văn Thắng\",\n");
            sb.append("      \"time\": \"").append(time).append("\",\n");
            sb.append("      \"body\": \"Nội dung chi tiết hoạt động tương tác khách hàng do phụ trách Hoàng Văn Thắng thực hiện.\"\n");
            sb.append("    }");
            if (i < totalActs - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(sb.toString());
    }
}
