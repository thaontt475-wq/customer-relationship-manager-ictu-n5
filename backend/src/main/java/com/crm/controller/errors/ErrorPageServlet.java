package com.crm.controller.errors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@WebServlet({"/errors/401", "/errors/403", "/errors/404", "/errors/500"})
public class ErrorPageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int statusCode = resolveStatusCode(request);
        String originalUri = resolveOriginalUri(request);

        if (originalUri != null
                && originalUri.startsWith(request.getContextPath() + "/api/")) {
            writeApiError(response, statusCode);
            return;
        }

        request.setAttribute("statusCode", statusCode);
        request.setAttribute("requestId", UUID.randomUUID().toString());

        switch (statusCode) {
            case HttpServletResponse.SC_UNAUTHORIZED -> {
                request.setAttribute("title", "Chưa đăng nhập");
                request.setAttribute(
                        "message",
                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn."
                );
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                request.getRequestDispatcher("/jsp/errors/401.jsp")
                        .forward(request, response);
            }

            case HttpServletResponse.SC_FORBIDDEN -> {
                request.setAttribute("title", "Không có quyền truy cập");
                request.setAttribute(
                        "message",
                        "Bạn không có quyền thực hiện thao tác hoặc truy cập khu vực này."
                );
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                request.getRequestDispatcher("/jsp/errors/403.jsp")
                        .forward(request, response);
            }

            case HttpServletResponse.SC_NOT_FOUND -> {
                request.setAttribute("title", "Không tìm thấy trang");
                request.setAttribute(
                        "message",
                        "Đường dẫn bạn truy cập không tồn tại hoặc đã được thay đổi."
                );
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.getRequestDispatcher("/jsp/errors/404.jsp")
                        .forward(request, response);
            }

            default -> {
                request.setAttribute("statusCode", 500);
                request.setAttribute("title", "Lỗi hệ thống");
                request.setAttribute(
                        "message",
                        "Hệ thống gặp sự cố. Vui lòng thử lại hoặc quay về trang trước."
                );
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                request.getRequestDispatcher("/jsp/errors/500.jsp")
                        .forward(request, response);
            }
        }
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status instanceof Number number) {
            return number.intValue();
        }

        return switch (request.getServletPath()) {
            case "/errors/401" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "/errors/403" -> HttpServletResponse.SC_FORBIDDEN;
            case "/errors/404" -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
    }

    private String resolveOriginalUri(HttpServletRequest request) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return uri == null ? request.getRequestURI() : String.valueOf(uri);
    }

    private void writeApiError(HttpServletResponse response, int status)
            throws IOException {

        int resolvedStatus = switch (status) {
            case 401, 403, 404, 500 -> status;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };

        String message = switch (resolvedStatus) {
            case 401 -> "Yêu cầu đăng nhập";
            case 403 -> "Không có quyền truy cập";
            case 404 -> "Không tìm thấy tài nguyên";
            default -> "Lỗi hệ thống";
        };

        response.setStatus(resolvedStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        GSON.toJson(
                new ApiError(false, message, null),
                response.getWriter()
        );
    }

    private record ApiError(
            boolean success,
            String message,
            Object data) {
    }
}