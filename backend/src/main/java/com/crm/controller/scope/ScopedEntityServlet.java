package com.crm.controller.scope;

import com.crm.dto.permissions.MenuItem;
import com.crm.service.permissions.MenuService;
import com.crm.service.scope.DataScopeService;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({
        "/customers",
        "/customers/*",
        "/opportunities",
        "/opportunities/*",
        "/activities",
        "/activities/*",
        "/quotes",
        "/quotes/*",
        "/scope/records",
        "/scope/records/*",
        "/api/customers/*",
        "/api/opportunities/*",
        "/api/activities/*",
        "/api/quotes/*"
})
public class ScopedEntityServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ScopedEntityServlet.class.getName());
    private static final String SCOPE_RECORDS_JSP = "/jsp/scope/scoped-records.jsp";

    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    private final DataScopeService dataScopeService =
            new DataScopeService();

    private final MenuService menuService =
            new MenuService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String servletPath = request.getServletPath();
        boolean isApi = servletPath.startsWith("/api/");

        Long userId = currentUserId(request);

        if (userId == null) {
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                        "Yêu cầu đăng nhập", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        ScopeEntityType type = resolveEntityType(request);

        if (type == null) {
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy tài nguyên", null);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy danh mục dữ liệu.");
            }
            return;
        }

        if (isApi) {
            handleApiRequest(request, response, userId, type);
        } else {
            handleViewRequest(request, response, userId, type);
        }
    }

    private void handleViewRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            Long userId,
            ScopeEntityType type) throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        String action = request.getParameter("action");
        String search = request.getParameter("search");

        try {
            // Kiểm tra hành động xuất Excel / CSV
            if ("export".equalsIgnoreCase(action) || (pathInfo != null && "/export".equals(pathInfo))) {
                List<ScopeRecord> exportItems = dataScopeService.list(userId, type, search);
                exportExcelCsv(response, type, exportItems);
                return;
            }

            // Kiểm tra xem chi tiết bản ghi (nếu có viewId hoặc /<id>)
            Long viewId = parseViewId(request, pathInfo);
            if (viewId != null) {
                DataScopeService.ReadResult readResult = dataScopeService.read(userId, type, viewId);
                switch (readResult.status()) {
                    case SUCCESS -> request.setAttribute("viewRecord", readResult.record());
                    case FORBIDDEN -> {
                        request.setAttribute("accessDeniedError",
                                "Bạn không có quyền truy cập bản ghi này do phạm vi dữ liệu.");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    }
                    case NOT_FOUND -> {
                        request.setAttribute("notFoundError",
                                "Không tìm thấy bản ghi yêu cầu.");
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            }

            // Tải danh sách theo phạm vi và tìm kiếm
            List<ScopeRecord> items = dataScopeService.list(userId, type, search);
            DataScopeService.UserContextData userCtx = dataScopeService.getUserContext(userId);

            request.setAttribute("items", items);
            request.setAttribute("entityType", type);
            request.setAttribute("search", search != null ? search.trim() : "");
            request.setAttribute("userContext", userCtx);
            request.setAttribute("currentUser", userCtx != null ? userCtx.user() : null);

            // Nạp menu cho sidebar
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object rolesObj = session.getAttribute(SessionKey.ROLES);
                if (rolesObj instanceof Collection<?> roles) {
                    List<String> roleStrings = roles.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .toList();
                    List<MenuItem> menuItems = menuService.getMenuItems(roleStrings);
                    request.setAttribute("menuItems", menuItems);
                }
            }

            request.getRequestDispatcher(SCOPE_RECORDS_JSP).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn dữ liệu phạm vi", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Lỗi hệ thống khi kiểm tra phạm vi dữ liệu.");
        }
    }

    private void handleApiRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            Long userId,
            ScopeEntityType type) throws IOException {

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
                List<ScopeRecord> items = dataScopeService.list(
                        userId,
                        type,
                        request.getParameter("search")
                );

                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy danh sách thành công",
                        new ListData(items));
                return;
            }

            if ("/export".equals(pathInfo)) {
                exportExcelCsv(
                        response,
                        type,
                        dataScopeService.list(
                                userId,
                                type,
                                request.getParameter("search")
                        )
                );
                return;
            }

            Long id = parseId(pathInfo);

            if (id == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID bản ghi không hợp lệ", null);
                return;
            }

            DataScopeService.ReadResult result =
                    dataScopeService.read(userId, type, id);

            switch (result.status()) {
                case SUCCESS ->
                        writeJson(response, HttpServletResponse.SC_OK, true,
                                "Lấy dữ liệu thành công",
                                result.record());

                case NOT_FOUND ->
                        writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                                "Không tìm thấy bản ghi", null);

                case FORBIDDEN ->
                        writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                                "Bạn không có quyền truy cập bản ghi này do phạm vi dữ liệu.",
                                null);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi API phạm vi dữ liệu", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi kiểm tra phạm vi dữ liệu",
                    null);
        }
    }

    private ScopeEntityType resolveEntityType(HttpServletRequest request) {
        String servletPath = request.getServletPath();

        ScopeEntityType fromPath = ScopeEntityType.fromServletPath(servletPath);
        if (fromPath != null) {
            return fromPath;
        }

        if ("/scope/records".equals(servletPath)) {
            String typeParam = request.getParameter("type");
            return ScopeEntityType.fromTypeName(typeParam);
        }

        return null;
    }

    private Long parseViewId(HttpServletRequest request, String pathInfo) {
        String viewIdParam = request.getParameter("viewId");
        if (viewIdParam != null && !viewIdParam.isBlank()) {
            try {
                long id = Long.parseLong(viewIdParam.trim());
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {}
        }

        if (pathInfo != null && !pathInfo.isBlank() && !"/".equals(pathInfo) && !"/export".equals(pathInfo)) {
            return parseId(pathInfo);
        }

        return null;
    }

    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object value = session.getAttribute("userId");

        if (!(value instanceof Number number)) {
            return null;
        }

        long id = number.longValue();
        return id > 0 ? id : null;
    }

    private Long parseId(String pathInfo) {
        String value = pathInfo.startsWith("/")
                ? pathInfo.substring(1)
                : pathInfo;

        if (value.isBlank() || value.contains("/")) {
            return null;
        }

        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void exportExcelCsv(
            HttpServletResponse response,
            ScopeEntityType type,
            List<ScopeRecord> items) throws IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");

        String filename = "crm-" + type.slug() + "-export.csv";
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + filename + "\""
        );

        PrintWriter writer = response.getWriter();

        // Ghi BOM UTF-8 (\uFEFF) để Excel hiển thị tiếng Việt có dấu chuẩn xác 100%
        writer.write('\uFEFF');

        // Ghi Header cột
        writer.println("Mã,Tiêu đề / Số hiệu,Người phụ trách,Nhóm kinh doanh,Thời gian tạo");

        for (ScopeRecord item : items) {
            String owner = item.ownerName() != null ? item.ownerName() : "ID #" + item.ownerUserId();
            String team = item.ownerTeamName() != null ? item.ownerTeamName() : "Chưa gán nhóm";
            String date = item.createdAt() != null ? item.createdAt() : "";

            writer.printf("%d,%s,%s,%s,%s%n",
                    item.id(),
                    csv(item.label()),
                    csv(owner),
                    csv(team),
                    csv(date));
        }
        writer.flush();
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data) throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");

        GSON.toJson(
                new ApiResponse(success, message, data),
                response.getWriter()
        );
    }

    private record ListData(List<ScopeRecord> items) {}
    private record ApiResponse(
            boolean success,
            String message,
            Object data) {}
}