package com.crm.controller.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/auth/session")
public class SessionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        HttpSession session = null;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException ignored) {
            // Concurrent invalidation
        }

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            GSON.toJson(new SessionResponse(false, "Chưa đăng nhập", null), response.getWriter());
            return;
        }

        /*
         * TODO / BLOCKER (CRM-21 Integration):
         * CRM-21 (Login BE) chưa chốt authentication session contract.
         * Chưa có quy ước thống nhất về session attribute lưu thông tin người dùng
         * (ví dụ: 'currentUser', 'userId', 'AUTH_USER', hoặc User DTO).
         *
         * Nguyên tắc an toàn theo CRM-22:
         * 1. Tuyệt đối không coi chỉ cần HttpSession tồn tại là người dùng đã đăng nhập.
         * 2. Không tự ý bịa đặt (invent) session attribute names (user, userId, currentUser, authenticatedUser, roles...).
         * 3. Không hard-code user hoặc role.
         *
         * Khi CRM-21 hoàn tất và thống nhất Session Contract:
         * - Lấy session attribute hợp lệ theo contract của CRM-21.
         * - Nếu tồn tại authenticated user hợp lệ:
         *       response.setStatus(HttpServletResponse.SC_OK);
         *       GSON.toJson(new SessionResponse(true, "Phiên đăng nhập hợp lệ", userData), response.getWriter());
         * - Nếu không hợp lệ:
         *       response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         *       GSON.toJson(new SessionResponse(false, "Chưa đăng nhập", null), response.getWriter());
         *
         * Hiện tại: Vì chưa thể xác định authenticated user một cách chính xác, phản hồi SC_UNAUTHORIZED (401)
         * để không giả vờ biết user đã đăng nhập khi Login contract chưa tồn tại.
         */
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        GSON.toJson(new SessionResponse(false, "Chưa xác thực: Chờ CRM-21 chốt authentication session contract", null), response.getWriter());
    }

    private record SessionResponse(boolean success, String message, Object data) { }
}
