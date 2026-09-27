package com.crm.controller.scope;

import com.crm.service.scope.DataScopeService;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

@WebServlet({
        "/api/customers/*",
        "/api/opportunities/*",
        "/api/activities/*",
        "/api/quotes/*"
})
public class ScopedEntityServlet extends HttpServlet {

    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    private final DataScopeService dataScopeService =
            new DataScopeService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Long userId = currentUserId(request);

        if (userId == null) {
            writeJson(response, 401, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        ScopeEntityType type =
                ScopeEntityType.fromServletPath(
                        request.getServletPath()
                );

        if (type == null) {
            writeJson(response, 404, false,
                    "Không tìm thấy tài nguyên", null);
            return;
        }

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null
                    || pathInfo.isBlank()
                    || "/".equals(pathInfo)) {

                List<ScopeRecord> items =
                        dataScopeService.list(
                                userId,
                                type,
                                request.getParameter("search")
                        );

                writeJson(response, 200, true,
                        "Lấy danh sách thành công",
                        new ListData(items));
                return;
            }

            if ("/export".equals(pathInfo)) {
                exportCsv(
                        response,
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
                writeJson(response, 400, false,
                        "ID bản ghi không hợp lệ", null);
                return;
            }

            DataScopeService.ReadResult result =
                    dataScopeService.read(userId, type, id);

            switch (result.status()) {
                case SUCCESS ->
                        writeJson(response, 200, true,
                                "Lấy dữ liệu thành công",
                                result.record());

                case NOT_FOUND ->
                        writeJson(response, 404, false,
                                "Không tìm thấy bản ghi", null);

                case FORBIDDEN ->
                        writeJson(response, 403, false,
                                "Bạn không có quyền truy cập bản ghi này do phạm vi dữ liệu.",
                                null);
            }

        } catch (SQLException e) {
            writeJson(response, 500, false,
                    "Lỗi hệ thống khi kiểm tra phạm vi dữ liệu",
                    null);
        }
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

    private void exportCsv(
            HttpServletResponse response,
            List<ScopeRecord> items) throws IOException {

        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"crm-export.csv\""
        );

        response.getWriter().println(
                "id,label,ownerUserId"
        );

        for (ScopeRecord item : items) {
            response.getWriter()
                    .printf("%d,%s,%d%n",
                            item.id(),
                            csv(item.label()),
                            item.ownerUserId());
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }

        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data) throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );
        response.setContentType(
                "application/json; charset=UTF-8"
        );

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