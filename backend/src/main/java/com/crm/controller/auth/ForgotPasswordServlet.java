package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

@WebServlet({"/forgot-password", "/api/auth/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private final AuthService authService = new AuthService();

    // ------------------------------------------------------------------ //
    //  GET /forgot-password                                               //
    // ------------------------------------------------------------------ //

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("/jsp/auth/forgot-password.jsp");
        rd.forward(request, response);
    }

    // ------------------------------------------------------------------ //
    //  POST /api/auth/forgot-password                                     //
    //  Field: email                                                       //
    // ------------------------------------------------------------------ //

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        if (email != null) {
            email = email.trim();
        }

        // Validate format
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            request.setAttribute("email", email);
            request.setAttribute("error", "Email không hợp lệ");
            RequestDispatcher rd = request.getRequestDispatcher("/jsp/auth/forgot-password.jsp");
            rd.forward(request, response);
            return;
        }

        // Email hợp lệ — luôn trả generic message bất kể email có tồn tại hay không
        authService.requestPasswordReset(email);

        response.setStatus(HttpServletResponse.SC_OK); // 200
        request.setAttribute("email", email);
        request.setAttribute("message",
            "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi.");
        RequestDispatcher rd = request.getRequestDispatcher("/jsp/auth/forgot-password.jsp");
        rd.forward(request, response);
    }
}
