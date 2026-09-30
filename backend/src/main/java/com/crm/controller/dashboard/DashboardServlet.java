package com.crm.controller.dashboard;

import com.crm.model.User;
import com.crm.service.teams.TeamService;
import com.crm.service.users.UserService;
import com.crm.util.SessionKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());
    private static final String DASHBOARD_JSP = "/jsp/dashboard/dashboard.jsp";

    private final UserService userService = new UserService();
    private final TeamService teamService = new TeamService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Long userId = resolveUserId(session);
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/login?expired=1");
            return;
        }

        List<String> roles = resolveRoles(session);
        boolean canManageUsers = roles.stream()
                .map(role -> role.trim().toLowerCase(Locale.ROOT))
                .anyMatch(role -> "admin".equals(role) || "director".equals(role));

        try {
            User currentUser = userService.findById(userId);
            if (currentUser == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            request.setAttribute("currentUser", currentUser);
            request.setAttribute("currentRoles", roles);
            request.setAttribute("canManageUsers", canManageUsers);

            if (canManageUsers) {
                request.setAttribute("totalUsers",
                        userService.searchUsers(null, null, null, 1, 1).totalItems());
                request.setAttribute("activeUsers",
                        userService.searchUsers(null, null, "ACTIVE", 1, 1).totalItems());
                request.setAttribute("lockedUsers",
                        userService.searchUsers(null, null, "LOCKED", 1, 1).totalItems());
                request.setAttribute("totalTeams", teamService.findAllTeams().size());
            }

            request.getRequestDispatcher(DASHBOARD_JSP).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load Sprint 1 dashboard", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Long resolveUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value;
        try {
            value = session.getAttribute("userId");
        } catch (IllegalStateException e) {
            return null;
        }
        if (value instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        return null;
    }

    private List<String> resolveRoles(HttpSession session) {
        Object value;
        try {
            value = session.getAttribute(SessionKey.ROLES);
        } catch (IllegalStateException e) {
            return List.of();
        }
        if (!(value instanceof Iterable<?> roles)) {
            return List.of();
        }
        List<String> resolved = new ArrayList<>();
        for (Object role : roles) {
            if (role instanceof String text && !text.isBlank()) {
                resolved.add(text);
            }
        }
        return List.copyOf(resolved);
    }
}
