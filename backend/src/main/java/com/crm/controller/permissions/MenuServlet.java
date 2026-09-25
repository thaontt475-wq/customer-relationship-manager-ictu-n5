package com.crm.controller.permissions;

import com.crm.dto.permissions.MenuItem;
import com.crm.service.permissions.MenuService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@WebServlet("/api/navigation/menu")
public class MenuServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final MenuService menuService = new MenuService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        HttpSession session = request.getSession(false);
        Object userId = null;
        Object sessionRoles = null;
        if (session != null) {
            try {
                userId = session.getAttribute("userId");
                sessionRoles = session.getAttribute("roles");
            } catch (IllegalStateException ignored) {
                // A concurrent logout may invalidate the session after getSession(false).
                writeUnauthorized(response);
                return;
            }
        }
        if (!(userId instanceof Long) || !(sessionRoles instanceof Collection<?> roleValues)) {
            writeUnauthorized(response);
            return;
        }

        List<String> roles = new ArrayList<>();
        for (Object value : roleValues) {
            if (!(value instanceof String role)) {
                writeUnauthorized(response);
                return;
            }
            roles.add(role);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        GSON.toJson(new MenuResponse(true, "Lấy menu thành công",
            new MenuData(menuService.getMenuItems(roles))), response.getWriter());
    }

    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        GSON.toJson(new MenuResponse(false, "Chưa đăng nhập", null), response.getWriter());
    }

    private record MenuResponse(boolean success, String message, MenuData data) { }
    private record MenuData(List<MenuItem> menuItems) { }
}
