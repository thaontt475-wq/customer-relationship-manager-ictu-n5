package com.crm.controller;

import com.crm.model.Role;
import com.crm.model.User;
import com.crm.util.SessionKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/** Shared presentation boundary for HTML forms; JSON API controllers are unaffected. */
public final class ServerForms {
    private ServerForms() { }
    public static Long actor(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("userId");
        if (id instanceof Number number && number.longValue() > 0) return number.longValue();
        return null;
    }
    public static List<String> roles(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return List.of();
        List<String> roles = new ArrayList<>();
        addRoles(roles, session.getAttribute(SessionKey.ROLES));
        Object user = session.getAttribute(SessionKey.CURRENT_USER);
        if (user instanceof User value) addRoles(roles, value.getRoles());
        return roles;
    }
    private static void addRoles(List<String> roles, Object value) {
        if (value instanceof Collection<?> values) values.forEach(item -> addRoles(roles, item));
        else if (value instanceof Role role) roles.add(role.getName());
        else if (value instanceof String name) roles.add(name);
    }
    public static boolean admin(HttpServletRequest request) {
        return roles(request).stream().filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", ""))
                .anyMatch(Set.of("ADMIN", "ADMINISTRATOR", "SYSTEM_ADMIN", "DIRECTOR",
                        "GIÁM ĐỐC", "GIAM DOC", "QUẢN TRỊ VIÊN", "QUAN TRI VIEN")::contains);
    }
    public static boolean authorize(HttpServletRequest request, HttpServletResponse response, boolean adminOnly)
            throws IOException {
        if (actor(request) == null) {
            response.sendRedirect(request.getContextPath() + "/login?expired=1");
            return false;
        }
        if (adminOnly && !admin(request)) { response.sendError(403); return false; }
        return true;
    }
    public static String csrf(HttpServletRequest request) {
        var session = request.getSession();
        synchronized (session) {
            String token = (String) session.getAttribute("htmlFormToken");
            if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute("htmlFormToken", token); }
            return token;
        }
    }
    public static boolean checkCsrf(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var session = request.getSession(false);
        Object token = session == null ? null : session.getAttribute("htmlFormToken");
        if (!(token instanceof String) || !token.equals(request.getParameter("csrfToken"))) {
            response.sendError(403); return false;
        }
        return true;
    }
    public static long positive(String value) {
        try { long result = Long.parseLong(value); if (result > 0) return result; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Mã bản ghi không hợp lệ.");
    }
    public static String value(HttpServletRequest request, String key, String fallback) {
        String value = request.getParameter(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
