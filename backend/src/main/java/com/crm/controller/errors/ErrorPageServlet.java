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

@WebServlet({"/errors/403", "/errors/404"})
public class ErrorPageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int statusCode = resolveStatusCode(request);
        String originalUri = resolveOriginalUri(request);

        if (originalUri != null && originalUri.startsWith(request.getContextPath() + "/api/")) {
            writeApiError(response, statusCode);
            return;
        }

        switch (statusCode) {
            case HttpServletResponse.SC_FORBIDDEN -> {
                request.setAttribute("statusCode", 403);
                request.setAttribute("title", "Không có quyền truy cập");
                request.setAttribute(
                        "message",
                        "Bạn không có quyền thực hiện thao tác hoặc truy cập khu vực này."
                );
                request.setAttribute("actionLabel", "Quay lại trang trước");
                request.setAttribute("actionUrl", "javascript:history.back()");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                request.getRequestDispatcher("/jsp/errors/403.jsp")
                        .forward(request, response);
            }

            case HttpServletResponse.SC_NOT_FOUND -> {
                request.setAttribute("statusCode", 404);
                request.setAttribute("title", "Không tìm thấy trang");
                request.setAttribute(
                        "message",
                        "Đường dẫn bạn truy cập không tồn tại hoặc đã được thay đổi."
                );
                request.setAttribute("actionLabel", "Về trang chủ");
                request.setAttribute("actionUrl", request.getContextPath() + "/");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.getRequestDispatcher("/jsp/errors/404.jsp")
                        .forward(request, response);
            }

            default -> response.sendError(statusCode);
        }
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object status =
                request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status instanceof Integer code) {
            return code;
        }

        return "/errors/403".equals(request.getServletPath())
                ? HttpServletResponse.SC_FORBIDDEN
                : HttpServletResponse.SC_NOT_FOUND;
    }

    private String resolveOriginalUri(HttpServletRequest request) {
        Object uri =
                request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        return uri == null ? request.getRequestURI() : String.valueOf(uri);
    }

    private void writeApiError(HttpServletResponse response, int status)
            throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String message = status == HttpServletResponse.SC_FORBIDDEN
                ? "Không có quyền truy cập"
                : "Không tìm thấy tài nguyên";

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
