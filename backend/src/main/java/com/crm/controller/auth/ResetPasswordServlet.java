package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import com.crm.util.PasswordUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet({"/reset-password", "/api/auth/reset-password"})
public class ResetPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final AuthService authService = new AuthService();

    // ------------------------------------------------------------------ //
    //  GET /reset-password?token=<rawToken>                               //
    // ------------------------------------------------------------------ //

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");

        if (token != null && !token.isBlank() && authService.validateResetToken(token)) {
            // Token hợp lệ — hiển thị form đặt lại mật khẩu
            response.setStatus(HttpServletResponse.SC_OK);
            request.setAttribute("token", token);
        } else {
            // Token không hợp lệ hoặc đã hết hạn
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error",
                "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        RequestDispatcher rd = request.getRequestDispatcher("/jsp/auth/reset-password.jsp");
        rd.forward(request, response);
    }

    // ------------------------------------------------------------------ //
    //  POST /api/auth/reset-password                                      //
    //  Fields: token, newPassword, confirmPassword                        //
    // ------------------------------------------------------------------ //

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // A. token null/blank
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error",
                "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            forward(request, response);
            return;
        }

        // B. Mật khẩu null hoặc không khớp
        if (newPassword == null || confirmPassword == null
                || !newPassword.equals(confirmPassword)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("token", token);
            request.setAttribute("error", "Mật khẩu không khớp.");
            forward(request, response);
            return;
        }

        // C. Không đúng policy mật khẩu
        if (!PasswordUtil.isValidPassword(newPassword)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("token", token);
            request.setAttribute("error",
                "Mật khẩu phải dài 8-72 ký tự và có chữ hoa, chữ thường, chữ số, ký tự đặc biệt.");
            forward(request, response);
            return;
        }

        // D. Thực hiện reset
        boolean success = authService.resetPassword(token, newPassword);

        if (success) {
            response.setStatus(HttpServletResponse.SC_OK);
            request.setAttribute("message",
                "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.");
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error",
                "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        forward(request, response);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("/jsp/auth/reset-password.jsp");
        rd.forward(request, response);
    }
}
