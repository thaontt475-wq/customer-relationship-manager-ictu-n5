package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import com.crm.util.SessionKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/jsp/auth/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("error", "Vui lòng nhập email và mật khẩu");
            doGet(request, response);
            return;
        }
        AuthService.LoginResult result;
        try {
            result = authService.login(email, password);
        } catch (SQLException e) {
            getServletContext().log("Login failed due to a server error", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("error", "Không thể đăng nhập lúc này. Vui lòng thử lại sau.");
            doGet(request, response);
            return;
        }
        if (result == null) {
            request.setAttribute("error", "Email hoặc mật khẩu không đúng");
            doGet(request, response);
            return;
        }
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", Long.valueOf(result.userId()));
        session.setAttribute(SessionKey.ROLES, result.roles());
        session.setAttribute(SessionKey.CURRENT_USER, Map.of("id", result.userId()));
        if (result.displayName() != null) session.setAttribute("displayName", result.displayName());
        response.sendRedirect(request.getContextPath() + "/html/index.html");
    }
}
