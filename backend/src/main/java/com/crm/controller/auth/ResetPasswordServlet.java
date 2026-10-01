package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import com.crm.util.PasswordUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet({"/reset-password", "/api/auth/reset-password"})
public class ResetPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String API_PATH = "/api/auth/reset-password";
    private static final String INVALID_TOKEN_MESSAGE =
        "Liên kết đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng.";
    private static final String PASSWORD_POLICY_MESSAGE =
        "Mật khẩu phải có ít nhất 8 ký tự, gồm ít nhất một chữ và một số.";
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final AuthService authService = new AuthService();

    // ------------------------------------------------------------------ //
    //  GET /reset-password?token=<rawToken>                               //
    // ------------------------------------------------------------------ //

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (isApiRequest(request)) {
            writeJson(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, false,
                "Phương thức không được hỗ trợ");
            return;
        }

        if ("1".equals(request.getParameter("saved"))) {
            request.setAttribute("message", "Đặt lại mật khẩu thành công");
            forwardView(request, response);
            return;
        }

        String token = request.getParameter("token");

        if (token != null && !token.isBlank() && authService.validateResetToken(token)) {
            response.setStatus(HttpServletResponse.SC_OK);
            request.setAttribute("token", token);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", INVALID_TOKEN_MESSAGE);
        }

        forwardView(request, response);
    }

    // ------------------------------------------------------------------ //
    //  POST /api/auth/reset-password                                      //
    //  Fields: token, newPassword, confirmPassword                        //
    // ------------------------------------------------------------------ //

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String token = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (token == null || token.isBlank()) {
            handleFailure(request, response, INVALID_TOKEN_MESSAGE);
            return;
        }

        if (newPassword == null || confirmPassword == null
                || !newPassword.equals(confirmPassword)) {
            handleFailure(request, response, "Mật khẩu không khớp.");
            return;
        }

        if (!PasswordUtil.isValidPassword(newPassword)) {
            handleFailure(request, response, PASSWORD_POLICY_MESSAGE);
            return;
        }

        boolean success = authService.resetPassword(token, newPassword);

        if (success) {
            if (isApiRequest(request)) {
                writeJson(response, HttpServletResponse.SC_OK, true, "Đặt lại mật khẩu thành công");
                return;
            }

            response.sendRedirect(request.getContextPath() + "/reset-password?saved=1");
            return;
        }

        handleFailure(request, response, INVALID_TOKEN_MESSAGE);
    }

    private void handleFailure(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        if (isApiRequest(request)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, message);
            return;
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("token", request.getParameter("token"));
        request.setAttribute("error", message);
        forwardView(request, response);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return API_PATH.equals(request.getServletPath());
    }

    private void forwardView(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/jsp/auth/reset-password.jsp").forward(request, response);
    }

    private void writeJson(HttpServletResponse response, int status, boolean success, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, null), response.getWriter());
    }

    private record ApiResponse(boolean success, String message, Object data) {
    }
}
