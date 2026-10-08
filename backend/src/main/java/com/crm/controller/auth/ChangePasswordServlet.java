package com.crm.controller.auth;

import com.crm.dto.common.ApiResponse;
import com.crm.service.auth.SessionService;
import com.crm.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/auth/change-password")
public class ChangePasswordServlet extends HttpServlet {
    private record PasswordRequest(String currentPassword, String newPassword, String confirmPassword) {}

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            PasswordRequest body = JsonUtil.getGson().fromJson(request.getReader(), PasswordRequest.class);
            if (body == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
            new SessionService().changePassword(
                    ((Number) request.getSession(false).getAttribute("userId")).longValue(),
                    body.currentPassword(), body.newPassword(), body.confirmPassword());
            request.getSession(false).invalidate();
            ResponseUtil.json(response, 200, ApiResponse.success("Đổi mật khẩu thành công. Vui lòng đăng nhập lại", null));
        } catch (com.google.gson.JsonParseException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("JSON không hợp lệ", null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(response, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Không thể đổi mật khẩu", null));
        }
    }
}

