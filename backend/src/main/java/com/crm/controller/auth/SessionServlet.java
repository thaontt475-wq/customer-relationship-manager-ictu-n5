package com.crm.controller.auth;

import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

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
            writeUnauthorized(response);
            return;
        }

        Object currentUser = null;
        Object roles = null;
        try {
            currentUser = session.getAttribute(SessionKey.CURRENT_USER);
            roles = session.getAttribute(SessionKey.ROLES);
        } catch (IllegalStateException ignored) {
            writeUnauthorized(response);
            return;
        }

        if (currentUser == null) {
            writeUnauthorized(response);
            return;
        }

        Object resolvedRoles = (roles != null) ? roles : List.of();
        Object expiresAt = resolveExpiresAt(session);

        SessionData data = new SessionData(currentUser, resolvedRoles, expiresAt);
        response.setStatus(HttpServletResponse.SC_OK);
        GSON.toJson(new SessionResponse(true, "Lấy thông tin phiên làm việc thành công", data), response.getWriter());
    }

    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        GSON.toJson(new SessionResponse(false, "Chưa đăng nhập", null), response.getWriter());
    }

    private static Object resolveExpiresAt(HttpSession session) {
        try {
            Object customExpiresAt = session.getAttribute(SessionKey.EXPIRES_AT);
            if (customExpiresAt != null) {
                return customExpiresAt;
            }
            int maxInactive = session.getMaxInactiveInterval();
            if (maxInactive > 0) {
                return Instant.ofEpochMilli(session.getLastAccessedTime() + ((long) maxInactive * 1000L)).toString();
            }
        } catch (IllegalStateException ignored) {
            // Concurrent invalidation
        }
        return null;
    }

    private record SessionResponse(boolean success, String message, SessionData data) { }
    private record SessionData(Object currentUser, Object roles, Object expiresAt) { }
}
