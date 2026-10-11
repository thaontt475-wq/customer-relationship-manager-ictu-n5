package com.crm.controller.customers;

import com.crm.dto.common.ApiResponse;
import com.crm.service.customers.CustomerLookupService;
import com.crm.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/customer-lookups")
public class CustomerLookupServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Number id)) {
            ResponseUtil.json(response, 401, ApiResponse.error("Chưa đăng nhập", null));
            return;
        }
        try {
            ResponseUtil.json(response, 200, ApiResponse.success("Danh mục khách hàng", new CustomerLookupService().list(id.longValue())));
        } catch (SecurityException error) {
            ResponseUtil.json(response, 403, ApiResponse.error(error.getMessage(), null));
        } catch (Exception error) {
            log("Customer lookups failed", error);
            ResponseUtil.json(response, 500, ApiResponse.error("Không thể tải danh mục khách hàng", null));
        }
    }
}
