package com.crm.controller.auth;

import com.crm.dto.common.ApiResponse;
import com.crm.util.ResponseUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Container errors keep the API's JSON contract without exposing stack traces. */
@WebServlet("/api/error")
public class ApiErrorServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer value ? value : 404;
        ResponseUtil.json(response, status, ApiResponse.error(
                status == 404 ? "Không tìm thấy API" : "Lỗi hệ thống", null));
    }
}
