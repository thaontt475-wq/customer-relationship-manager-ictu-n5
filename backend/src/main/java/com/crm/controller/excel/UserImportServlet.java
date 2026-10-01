package com.crm.controller.excel;

import com.crm.dto.excel.ImportReportResult;
import com.crm.controller.ServerForms;
import com.crm.model.User;
import com.crm.service.excel.ExcelService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling Excel User Import operations (CRM-32):
 * - GET  /users/import                 — Forward to import JSP page
 * - GET  /api/users/import/template     — Download sample template (.xlsx / .csv)
 * - POST /api/users/import/preview      — Upload and validate Excel, return preview report
 * - POST /api/users/import/confirm      — Confirm import for valid rows, skipping error rows
 * - POST /api/users/import              — Direct upload & import in one step
 */
@WebServlet({
        "/users/import",
        "/users/import/*",
        "/api/users/import",
        "/api/users/import/*"
})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 10 * 1024 * 1024,        // 10 MB
        maxRequestSize = 20 * 1024 * 1024      // 20 MB
)
public class UserImportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(UserImportServlet.class.getName());
    private static final String USER_IMPORT_JSP = "/jsp/users/user-import.jsp";
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final ExcelService excelService;

    public UserImportServlet() {
        this.excelService = new ExcelService();
    }

    public UserImportServlet(ExcelService excelService) {
        this.excelService = excelService != null ? excelService : new ExcelService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        boolean isApi = isApiRequest(request);

        if (actorUserId == null) {
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
            }
            return;
        }

        if (!hasAdminOrDirectorRole(request)) {
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, false, "Không có quyền thực hiện chức năng này", null);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }

        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();

        // 1. Download template: /api/users/import/template or /users/import/template
        if ("/template".equals(pathInfo) || "/api/users/import/template".equals(servletPath)) {
            handleDownloadTemplate(request, response);
            return;
        }

        // 2. View page: /users/import
        if ("/users/import".equals(servletPath) && (pathInfo == null || "/".equals(pathInfo))) {
            request.setAttribute("notice", "Chọn tệp Excel để xem trước trước khi nhập.");
            request.getRequestDispatcher(USER_IMPORT_JSP).forward(request, response);
            return;
        }

        if (isApi) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false, "Endpoint không tồn tại", null);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        if (!hasAdminOrDirectorRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền thực hiện nhập người dùng", null);
            return;
        }

        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();

        // Dedicated HTML flow. JSON API behavior remains unchanged.
        if ("/users/import".equals(servletPath) &&
                ("/preview".equals(pathInfo) || "/execute".equals(pathInfo))) {
            if (!ServerForms.checkCsrf(request, response)) return;
            handleHtmlImport(request, response, actorUserId, pathInfo);
            return;
        }

        try {
            // 1. POST /api/users/import/preview or /users/import/preview
            if ("/preview".equals(pathInfo)) {
                handlePreview(request, response);
                return;
            }

            // 2. POST /api/users/import/confirm or /users/import/execute
            if ("/confirm".equals(pathInfo) || "/execute".equals(pathInfo)) {
                handleConfirm(request, response, actorUserId);
                return;
            }

            // 3. POST /api/users/import (direct upload and import)
            if (pathInfo == null || "/".equals(pathInfo)) {
                handleDirectImport(request, response, actorUserId);
                return;
            }

            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false, "Endpoint không tồn tại", null);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during Excel import", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi cơ sở dữ liệu khi nhập người dùng: " + e.getMessage(), null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during Excel import", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi xử lý tệp Excel: " + e.getMessage(), null);
        }
    }


    /** Native HTML preview/confirm with batch token kept on the server side. */
    private void handleHtmlImport(HttpServletRequest req, HttpServletResponse res, long actor,
                                  String path) throws ServletException, IOException {
        res.setHeader("Cache-Control", "no-store");
        try {
            if ("/preview".equals(path)) {
                req.getSession().removeAttribute("htmlUserImportToken");
                Part part = getUploadedFilePart(req);
                if (part == null || part.getSize() == 0) {
                    showHtmlImport(req, res, null, "Hãy chọn tệp Excel hoặc CSV.", 400);
                    return;
                }
                String name = part.getSubmittedFileName();
                if (!isSupportedExtension(name)) {
                    showHtmlImport(req, res, null, "Tệp phải có định dạng .xlsx, .xls hoặc .csv.", 400);
                    return;
                }
                try (InputStream in = part.getInputStream()) {
                    ImportReportResult result = excelService.parseAndValidate(in, name);
                    String token = result.getBatchToken();
                    if (token == null || token.isBlank()) {
                        showHtmlImport(req, res, result, "Không tạo được mã xem trước. Hãy tải lại tệp.", 400);
                        return;
                    }
                    req.getSession().setAttribute("htmlUserImportToken", token);
                    showHtmlImport(req, res, result, null, 200);
                }
            } else if ("/execute".equals(path)) {
                Object token = req.getSession().getAttribute("htmlUserImportToken");
                req.getSession().removeAttribute("htmlUserImportToken");
                if (!(token instanceof String value) || value.isBlank()) {
                    showHtmlImport(req, res, null, "Phiên xem trước không còn hiệu lực. Hãy tải lại tệp.", 400);
                    return;
                }
                ImportReportResult report = excelService.confirmImport(value);
                req.setAttribute("completed", Boolean.TRUE);
                showHtmlImport(req, res, report, null, 200);
            }
        } catch (IllegalStateException ex) {
            showHtmlImport(req, res, null, "Dữ liệu xem trước đã hết hạn. Hãy tải lại tệp.", 400);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "HTML Excel import failed", ex);
            showHtmlImport(req, res, null, "Không thể nhập dữ liệu. Kiểm tra kết nối và nhật ký máy chủ.", 500);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "HTML Excel import failed", ex);
            showHtmlImport(req, res, null, "Tệp không hợp lệ hoặc không thể xử lý.", 400);
        }
    }

    private void showHtmlImport(HttpServletRequest req, HttpServletResponse res,
                                ImportReportResult report, String error, int status)
            throws ServletException, IOException {
        req.setAttribute("report", report);
        req.setAttribute("error", error);
        res.setStatus(status);
        req.getRequestDispatcher(USER_IMPORT_JSP).forward(req, res);
    }

    // === Handlers ===

    private void handleDownloadTemplate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String format = request.getParameter("format");
        if (format == null || format.isBlank()) {
            format = "xlsx";
        }

        byte[] templateBytes = excelService.generateTemplate(format);

        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"mau_nhap_nguoi_dung.csv\"");
        } else {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"mau_nhap_nguoi_dung.xlsx\"");
        }

        response.setContentLength(templateBytes.length);
        try (OutputStream out = response.getOutputStream()) {
            out.write(templateBytes);
            out.flush();
        }
    }

    private void handlePreview(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {

        Part filePart = getUploadedFilePart(request);
        if (filePart == null || filePart.getSize() == 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Vui lòng chọn tệp Excel hoặc CSV để tải lên", null);
            return;
        }

        String fileName = filePart.getSubmittedFileName();
        if (!isSupportedExtension(fileName)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Định dạng tệp không được hỗ trợ. Vui lòng chọn tệp .xlsx, .xls hoặc .csv", null);
            return;
        }

        try (InputStream is = filePart.getInputStream()) {
            ImportReportResult previewResult = excelService.parseAndValidate(is, fileName);
            writeJson(response, HttpServletResponse.SC_OK, true, "Kiểm tra dữ liệu hoàn tất", previewResult);
        }
    }

    private void handleConfirm(HttpServletRequest request, HttpServletResponse response, long actorUserId)
            throws IOException, SQLException {

        ConfirmImportRequest body;
        try {
            body = GSON.fromJson(request.getReader(), ConfirmImportRequest.class);
        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "JSON không hợp lệ", null);
            return;
        }

        if (body == null || body.batchToken() == null || body.batchToken().isBlank()) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Thiếu mã xác nhận batchToken", null);
            return;
        }

        try {
            ImportReportResult finalReport = excelService.confirmImport(body.batchToken());
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Nhập danh sách người dùng thành công", finalReport);
        } catch (IllegalStateException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        }
    }

    private void handleDirectImport(HttpServletRequest request, HttpServletResponse response, long actorUserId)
            throws IOException, ServletException, SQLException {

        Part filePart = getUploadedFilePart(request);
        if (filePart == null || filePart.getSize() == 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Vui lòng chọn tệp Excel hoặc CSV để nhập", null);
            return;
        }

        String fileName = filePart.getSubmittedFileName();
        if (!isSupportedExtension(fileName)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Định dạng tệp không được hỗ trợ. Vui lòng chọn tệp .xlsx, .xls hoặc .csv", null);
            return;
        }

        try (InputStream is = filePart.getInputStream()) {
            ImportReportResult report = excelService.importDirect(is, fileName, actorUserId);
            writeJson(response, HttpServletResponse.SC_OK, true, "Nhập danh sách người dùng thành công", report);
        }
    }

    // === Helpers ===

    private Part getUploadedFilePart(HttpServletRequest request) {
        try {
            Part filePart = request.getPart("file");
            if (filePart != null) {
                return filePart;
            }
            // Fallback: look for any part with a filename
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null && !part.getSubmittedFileName().isBlank()) {
                    return part;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting uploaded file part", e);
        }
        return null;
    }

    private boolean isSupportedExtension(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.trim().toLowerCase(Locale.ROOT);
        return lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv");
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String acceptHeader = request.getHeader("Accept");
        return (servletPath != null && servletPath.startsWith("/api/"))
                || (acceptHeader != null && acceptHeader.contains("application/json"));
    }

    private Long extractActorUserId(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return null;
        }
        if (session == null) {
            return null;
        }
        try {
            Object directUserId = session.getAttribute("userId");
            Long parsedDirect = parseIdValue(directUserId);
            if (parsedDirect != null) {
                return parsedDirect;
            }
            Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
            if (currentUser instanceof User u && u.getId() > 0) {
                return u.getId();
            }
            if (currentUser instanceof Map<?, ?> map) {
                return parseIdValue(map.get("id"));
            }
            return parseIdValue(currentUser);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private Long parseIdValue(Object value) {
        if (value instanceof Number number) {
            long id = number.longValue();
            return id > 0 ? id : null;
        }
        if (value instanceof String text) {
            try {
                long parsed = Long.parseLong(text);
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private boolean hasAdminOrDirectorRole(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return false;
        }
        if (session == null) {
            return false;
        }
        try {
            Object rolesValue = session.getAttribute(SessionKey.ROLES);
            if (rolesValue instanceof Collection<?> roles) {
                return roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(r -> r.trim().toLowerCase(Locale.ROOT))
                        .anyMatch(r -> "admin".equals(r) || "director".equals(r));
            }
        } catch (IllegalStateException e) {
            return false;
        }
        return false;
    }

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    // === Request / Response records ===

    private record ConfirmImportRequest(String batchToken) { }
    private record ApiResponse(boolean success, String message, Object data) { }
}
