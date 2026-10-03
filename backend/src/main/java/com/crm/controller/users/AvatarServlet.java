package com.crm.controller.users;

import com.crm.controller.ServerForms;
import com.crm.model.User;
import com.crm.service.users.AvatarException;
import com.crm.service.users.AvatarService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@WebServlet({"/profile/avatar", "/profile/avatar/image", "/profile/avatar/thumbnail", "/api/users/me/avatar"})
@MultipartConfig(maxFileSize = 2097152, maxRequestSize = 2162688, fileSizeThreshold = 0)
public class AvatarServlet extends HttpServlet {
    private static final String TOKEN = "htmlFormToken";
    private AvatarService service;

    public AvatarServlet() {
    }

    public AvatarServlet(AvatarService service) {
        this.service = service;
    }

    @Override public void init() throws ServletException {
        if (this.service != null) return;
        String directory = System.getenv("CRM_AVATAR_DIR");
        if (directory == null || directory.isBlank()) {
            String base = System.getProperty("catalina.base");
            if (base == null) throw new ServletException("Configure CRM_AVATAR_DIR or catalina.base");
            directory = Path.of(base, "data", "avatars").toString();
        }
        this.service = new AvatarService(Path.of(directory));
    }

    private Long resolveUserId(HttpServletRequest req) {
        Object attr = req.getAttribute("avatarUserId");
        if (attr instanceof Number num && num.longValue() > 0) return num.longValue();
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object direct = session.getAttribute("userId");
            if (direct instanceof Number num && num.longValue() > 0) return num.longValue();
            Object actorIdObj = session.getAttribute("actorUserId");
            if (actorIdObj instanceof Number num && num.longValue() > 0) return num.longValue();
            Object cur = session.getAttribute(SessionKey.CURRENT_USER);
            if (cur instanceof User u && u.getId() > 0) return u.getId();
        }
        return null;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        Long userId = resolveUserId(req);
        String path = req.getServletPath();
        if (userId == null) {
            if (path != null && path.startsWith("/api/")) {
                respond(req, res, 401, false, "Yêu cầu đăng nhập.");
            } else {
                res.sendRedirect(req.getContextPath() + "/login?expired=1");
            }
            return;
        }

        try {
            if (path.endsWith("/image") || path.endsWith("/thumbnail")) {
                byte[] bytes = service.read(userId, path.endsWith("/thumbnail"));
                if (bytes == null) { res.setStatus(404); return; }
                res.setContentType("image/png");
                res.setHeader("X-Content-Type-Options", "nosniff");
                res.setContentLength(bytes.length);
                res.getOutputStream().write(bytes);
                return;
            }
            String token = token(req);
            boolean exists = service.find(userId) != null;
            if (path.startsWith("/api/")) {
                json(res, 200, true, "Thông tin ảnh đại diện", Map.of("csrfToken", token, "hasAvatar", exists,
                        "imageUrl", req.getContextPath() + "/profile/avatar/image",
                        "thumbnailUrl", req.getContextPath() + "/profile/avatar/thumbnail"));
            } else {
                req.setAttribute("hasAvatar", exists);
                show(req, res);
            }
        } catch (SQLException | IOException e) {
            getServletContext().log("Avatar read failed", e);
            respond(req, res, 500, false, "Không thể tải ảnh đại diện lúc này. Vui lòng thử lại.");
        }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        if (!req.getServletPath().equals("/profile/avatar") && !req.getServletPath().equals("/api/users/me/avatar")) {
            res.setStatus(405); return;
        }
        req.setCharacterEncoding("UTF-8");
        String type = req.getContentType();
        if (type == null || !type.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
            respond(req, res, 415, false, "Yêu cầu multipart/form-data với trường avatar."); return;
        }

        Long userId = resolveUserId(req);
        if (userId == null) {
            respond(req, res, 401, false, "Yêu cầu đăng nhập.");
            return;
        }

        java.util.Collection<Part> parts = null;
        try {
            parts = req.getParts();
            String supplied = req.getHeader("X-CSRF-Token");
            if (supplied == null) supplied = req.getParameter("csrfToken");
            HttpSession session = req.getSession(false);
            Object expected = session != null ? session.getAttribute(TOKEN) : null;
            if (!(expected instanceof String value) || supplied == null || !MessageDigest.isEqual(
                    value.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                respond(req, res, 403, false, "Phiên xác nhận không hợp lệ. Vui lòng tải lại trang."); return;
            }
            var files = parts.stream().filter(p -> p.getSubmittedFileName() != null).toList();
            if (files.size() != 1 || !"avatar".equals(files.getFirst().getName())) {
                respond(req, res, 400, false, "Vui lòng gửi đúng một file ở trường avatar."); return;
            }
            Part file = files.getFirst();
            try (var input = file.getInputStream()) {
                service.upload(userId, input, file.getSubmittedFileName(), file.getSize());
            }
            if (session != null) {
                session.setAttribute("hasUserAvatar", Boolean.TRUE);
            }
            if (!req.getServletPath().startsWith("/api/")) {
                ServerForms.setToast(req, "success", "Cập nhật thành công", "Ảnh đại diện đã được cập nhật.");
                res.sendRedirect(req.getContextPath() + "/profile/avatar?updated=1");
                return;
            }
            respond(req, res, 200, true, "Cập nhật ảnh đại diện thành công.");
        } catch (IllegalStateException e) {
            respond(req, res, 413, false, "Ảnh tối đa 2 MiB; tổng yêu cầu tối đa 2 MiB + 64 KiB.");
        } catch (AvatarException e) {
            respond(req, res, e.getStatus(), false, e.getMessage());
        } catch (SQLException | IOException e) {
            getServletContext().log("Avatar upload failed", e);
            respond(req, res, 500, false, "Không thể lưu ảnh đại diện lúc này. Vui lòng thử lại.");
        } catch (ServletException e) {
            respond(req, res, 400, false, "Dữ liệu upload không hợp lệ.");
        } finally {
            if (parts != null) for (Part part : parts) {
                try { part.delete(); } catch (IOException e) { getServletContext().log("Multipart cleanup failed", e); }
            }
        }
    }

    private String token(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return UUID.randomUUID().toString();
        synchronized (session) {
            String token = (String) session.getAttribute(TOKEN);
            if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute(TOKEN, token); }
            return token;
        }
    }
    private void show(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setAttribute("csrfToken", token(req));
        req.getRequestDispatcher("/jsp/users/avatar.jsp").forward(req, res);
    }
    private void respond(HttpServletRequest req, HttpServletResponse res, int status, boolean success, String message)
            throws IOException, ServletException {
        if (req.getServletPath().startsWith("/api/")) {
            json(res, status, success, message, success ? Map.of(
                    "imageUrl", req.getContextPath() + "/profile/avatar/image",
                    "thumbnailUrl", req.getContextPath() + "/profile/avatar/thumbnail") : null);
        } else {
            res.setStatus(status);
            req.setAttribute("message", message);
            show(req, res);
        }
    }
    private void json(HttpServletResponse res, int status, boolean success, String message, Object data) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        new Gson().toJson(new ApiResponse(success, message, data), res.getWriter());
    }
    private record ApiResponse(boolean success, String message, Object data) { }
}
