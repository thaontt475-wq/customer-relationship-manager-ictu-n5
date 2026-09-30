package com.crm.controller.organization;

import com.crm.model.User;
import com.crm.service.organization.OrganizationService;
import com.crm.service.organization.OrganizationService.OrganizationException;
import com.crm.service.organization.OrganizationService.UnitInput;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({
        "/api/organization/units",
        "/api/organization/units/*"
})
public class OrganizationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(OrganizationServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final OrganizationService organizationService;

    public OrganizationServlet() {
        this(new OrganizationService());
    }

    public OrganizationServlet(OrganizationService organizationService) {
        this.organizationService = organizationService != null
                ? organizationService
                : new OrganizationService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (!isAuthenticated(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    false, "Yêu cầu đăng nhập", null);
            return;
        }
        if (hasPath(request)) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                    false, "Endpoint không tồn tại", null);
            return;
        }

        try {
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Lấy cây cơ cấu tổ chức thành công",
                    new UnitList(organizationService.getUnits()));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-42: Unable to load organization units", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false, "Lỗi hệ thống khi tải cơ cấu tổ chức", null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (!isAuthenticated(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    false, "Yêu cầu đăng nhập", null);
            return;
        }
        if (!hasPermissionAdminRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    false, "Không có quyền quản lý cơ cấu tổ chức", null);
            return;
        }
        if (hasPath(request)) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                    false, "Endpoint không tồn tại", null);
            return;
        }

        UnitRequest body = readBody(request, response);
        if (body == null) {
            return;
        }

        try {
            Object created = organizationService.createUnit(body.toInput());
            writeJson(response, HttpServletResponse.SC_CREATED, true,
                    "Tạo đơn vị thành công", created);
        } catch (OrganizationException e) {
            writeBusinessError(response, e);
        } catch (SQLIntegrityConstraintViolationException e) {
            writeJson(response, HttpServletResponse.SC_CONFLICT,
                    false, "Dữ liệu đơn vị xung đột với dữ liệu hiện có", null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-42: Unable to create organization unit", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false, "Lỗi hệ thống khi tạo đơn vị", null);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (!isAuthenticated(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    false, "Yêu cầu đăng nhập", null);
            return;
        }
        if (!hasPermissionAdminRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    false, "Không có quyền quản lý cơ cấu tổ chức", null);
            return;
        }

        Long unitId = parseUnitId(request.getPathInfo());
        if (unitId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    false, "ID đơn vị không hợp lệ", null);
            return;
        }

        UnitRequest body = readBody(request, response);
        if (body == null) {
            return;
        }

        try {
            Object updated = organizationService.updateUnit(unitId, body.toInput());
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Cập nhật đơn vị thành công", updated);
        } catch (OrganizationException e) {
            writeBusinessError(response, e);
        } catch (SQLIntegrityConstraintViolationException e) {
            writeJson(response, HttpServletResponse.SC_CONFLICT,
                    false, "Dữ liệu đơn vị xung đột với dữ liệu hiện có", null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-42: Unable to update organization unit " + unitId, e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false, "Lỗi hệ thống khi cập nhật đơn vị", null);
        }
    }

    private UnitRequest readBody(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            UnitRequest body = GSON.fromJson(request.getReader(), UnitRequest.class);
            if (body == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        false, "Dữ liệu đơn vị không được để trống", null);
            }
            return body;
        } catch (JsonSyntaxException | JsonIOException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    false, "JSON không hợp lệ", null);
            return null;
        }
    }

    private void writeBusinessError(HttpServletResponse response, OrganizationException error)
            throws IOException {
        int status = switch (error.getCode()) {
            case VALIDATION, INVALID_REGION -> HttpServletResponse.SC_BAD_REQUEST;
            case UNIT_NOT_FOUND, PARENT_NOT_FOUND, LEADER_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case INVALID_LEADER, SELF_PARENT, CYCLE, DUPLICATE_NAME,
                    USER_ALREADY_IN_TEAM, LEADER_ALREADY_ASSIGNED,
                    UPDATE_CONFLICT -> HttpServletResponse.SC_CONFLICT;
        };
        writeJson(response, status, false, error.getMessage(),
                Map.of("code", error.getCode().name()));
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return false;
        }
        if (session == null) {
            return false;
        }

        Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        if (currentUser instanceof User user) {
            return user.getId() > 0;
        }
        if (currentUser instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (id instanceof Number number && number.longValue() > 0) {
                return true;
            }
        }
        if (currentUser != null) {
            return true;
        }

        Object userId = session.getAttribute("userId");
        if (userId instanceof Number number) {
            return number.longValue() > 0;
        }
        if (userId instanceof String text) {
            try {
                return Long.parseLong(text) > 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean hasPermissionAdminRole(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return false;
        }

        if (session == null) {
            return false;
        }

        Object rolesValue;
        try {
            rolesValue = session.getAttribute(SessionKey.ROLES);
        } catch (IllegalStateException e) {
            return false;
        }

        if (!(rolesValue instanceof java.util.Collection<?> roles)) {
            return false;
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.trim().toLowerCase(java.util.Locale.ROOT))
                .anyMatch(role -> "admin".equals(role) || "director".equals(role));
    }

    private boolean hasPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo != null && !pathInfo.isBlank() && !"/".equals(pathInfo.trim());
    }

    private Long parseUnitId(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo.trim())) {
            return null;
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (clean.isBlank() || clean.contains("/")) {
            return null;
        }
        try {
            long id = Long.parseLong(clean);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    private record UnitRequest(
            String name,
            Long parentId,
            Long managerId,
            String managerName,
            String managerRole,
            String region,
            Boolean active) {

        UnitInput toInput() {
            return new UnitInput(name, parentId, managerId, region, active);
        }
    }

    private record UnitList(List<com.crm.model.Organization> items) {
    }

    private record ApiResponse(boolean success, String message, Object data) {
    }
}
