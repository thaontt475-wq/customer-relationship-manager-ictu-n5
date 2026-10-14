package com.crm.controller.permissions;

import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.MenuService;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller (Servlet) tiếp nhận request điều hướng menu và thông tin người dùng (CRM-26).
 * Kiến trúc: Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL 8.0.
 *
 * Tiêu chí đáp ứng:
 * - AC 1: Trả về danh sách menu chỉ bao gồm các chức năng được phân quyền.
 * - AC 2: Trả về thông tin người dùng gồm Tên, vai trò và nhóm kinh doanh.
 * - AC 3: Cung cấp đầy đủ cấu trúc menu tương thích hiển thị trên màn hình 360px và desktop.
 */
@WebServlet({"/api/navigation/menu", "/api/menu"})
public class MenuServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MenuServlet.class.getName());

    private final MenuService menuService;

    public MenuServlet() {
        this.menuService = new MenuService();
    }

    public MenuServlet(MenuService menuService) {
        this.menuService = menuService != null ? menuService : new MenuService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            ResponseUtil.json(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    ApiResponse.error("Chưa đăng nhập", null)
            );
            return;
        }

        try {
            long userId = ((Number) session.getAttribute("userId")).longValue();

            // Gọi Service để xử lý nghiệp vụ lấy dữ liệu menu và user profile
            Map<String, Object> data = menuService.getNavigationData(userId);

            // Gán attributes vào request để hỗ trợ server-side rendering / JSP forward nếu cần
            request.setAttribute("menuItems", data.get("menuItems"));
            request.setAttribute("userNavigationProfile", data.get("user"));
            request.setAttribute("currentUserProfile", data.get("userProfile"));

            ResponseUtil.json(
                    response,
                    HttpServletResponse.SC_OK,
                    ApiResponse.success("Lấy menu thành công", data)
            );

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi xử lý lấy menu điều hướng người dùng", e);
            ResponseUtil.json(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.error("Không thể lấy menu điều hướng", null)
            );
        }
    }
}