package com.crm.controller.users;

import com.crm.dto.common.ApiResponse;
import com.crm.service.importer.UserImportService;
import com.crm.service.permissions.PermissionService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@WebServlet("/api/users/import/*")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5500000)
public class UserImportServlet extends HttpServlet {
    private static final long MAX_FILE_SIZE = 5242880;
    private final UserImportService service = new UserImportService();
    private final PermissionService permissions = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            require(request);
            if (!"/template".equals(request.getPathInfo())) {
                ResponseUtil.json(response, 404, ApiResponse.error("Không tìm thấy API", null));
                return;
            }
            String type = request.getParameter("type");
            boolean isInvalid = "invalid".equalsIgnoreCase(type) || "error".equalsIgnoreCase(type);
            String filename = isInvalid ? "mau_kiem_thu_nguoi_dung_co_loi.xlsx" : "mau_nguoi_dung_hop_le.xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            service.writeTemplate(response.getOutputStream(), isInvalid ? "invalid" : "valid");
        } catch (SecurityException e) {
            forbidden(response);
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Không thể tải file mẫu", null));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            long ownerUserId = require(request);
            String path = request.getPathInfo();
            if ("/preview".equals(path)) {
                Part file = request.getPart("file");
                if (file == null || file.getSubmittedFileName() == null
                        || !file.getSubmittedFileName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                    throw new IllegalArgumentException("Chỉ chấp nhận file .xlsx");
                }
                if (file.getSize() <= 0) throw new IllegalArgumentException("File không có dữ liệu");
                if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File không được vượt quá 5MB");
                try (InputStream input = file.getInputStream()) {
                    ResponseUtil.json(response, 200, ApiResponse.success("Preview import thành công", service.preview(input, ownerUserId)));
                }
                return;
            }
            if ("/confirm".equals(path)) {
                JsonObject body = JsonUtil.getGson().fromJson(request.getReader(), JsonObject.class);
                if (body == null || !body.has("batchToken") || body.get("batchToken").isJsonNull()
                        || !body.get("batchToken").isJsonPrimitive()
                        || !body.get("batchToken").getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Thiếu batchToken hợp lệ");
                }
                ResponseUtil.json(response, 200, ApiResponse.success("Import user hoàn tất", service.confirm(body.get("batchToken").getAsString(), ownerUserId)));
                return;
            }
            ResponseUtil.json(response, 404, ApiResponse.error("Không tìm thấy API", null));
        } catch (SecurityException e) {
            forbidden(response);
        } catch (JsonParseException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("JSON không hợp lệ", null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(response, 400, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("File hoặc request vượt quá dung lượng cho phép", null));
        } catch (ServletException e) {
            ResponseUtil.json(response, 400, ApiResponse.error("Dữ liệu upload không hợp lệ", null));
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Lỗi import user", null));
        }
    }

    private long require(HttpServletRequest request) throws Exception {
        long userId = ((Number) request.getSession(false).getAttribute("userId")).longValue();
        if (!permissions.hasPermission(userId, "user.import")) throw new SecurityException();
        return userId;
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        ResponseUtil.json(response, 403, ApiResponse.error("Không có quyền", null));
    }
}
