package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@WebServlet({"/forgot-password", "/api/auth/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String API_PATH = "/api/auth/forgot-password";
    static final String GENERIC_MESSAGE =
        "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi.";
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final AuthService authService = new AuthService();

    // ------------------------------------------------------------------ //
    //  GET /forgot-password                                               //
    // ------------------------------------------------------------------ //

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (isApiRequest(request)) {
            writeJson(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, false,
                "Phương thức không được hỗ trợ");
            return;
        }
        forwardView(request, response);
    }

    // ------------------------------------------------------------------ //
    //  POST /api/auth/forgot-password                                     //
    //  Field: email                                                       //
    // ------------------------------------------------------------------ //

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String email = request.getParameter("email");
        if (email != null) {
            email = email.trim();
        }

        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            if (isApiRequest(request)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Email không hợp lệ");
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("email", email);
            request.setAttribute("error", "Email không hợp lệ");
            forwardView(request, response);
            return;
        }

        authService.requestPasswordReset(email);

        if (isApiRequest(request)) {
            writeJson(response, HttpServletResponse.SC_OK, true, GENERIC_MESSAGE);
            return;
        }

        request.setAttribute("email", email);
        request.setAttribute("message", GENERIC_MESSAGE);
        forwardView(request, response);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return API_PATH.equals(request.getServletPath());
    }

    private void forwardView(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/jsp/auth/forgot-password.jsp").forward(request, response);
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
