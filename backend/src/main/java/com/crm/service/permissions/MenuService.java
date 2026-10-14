package com.crm.service.permissions;

import com.crm.dao.permissions.MenuDAO;
import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service xử lý logic nghiệp vụ cho phân quyền menu và thông tin điều hướng (CRM-26).
 * Đảm bảo:
 * - AC 1: Mục menu không thuộc quyền của người dùng thì KHÔNG hiển thị.
 * - AC 2: Cung cấp đầy đủ thông tin người dùng: Tên, vai trò và nhóm kinh doanh.
 * - AC 3: Dữ liệu phân tách rõ ràng (workspace/admin) hỗ trợ hiển thị trên màn hình 360px (mobile) và desktop.
 */
public class MenuService {

    private final MenuDAO menuDAO;

    public MenuService() {
        this.menuDAO = new MenuDAO();
    }

    public MenuService(MenuDAO menuDAO) {
        this.menuDAO = Objects.requireNonNullElseGet(menuDAO, MenuDAO::new);
    }

    /**
     * Lấy toàn bộ dữ liệu điều hướng cho người dùng theo User ID.
     * Bao gồm danh sách menu đã lọc theo quyền và thông tin UserNavigationProfile.
     */
    public Map<String, Object> getNavigationData(long userId) {
        Set<String> permissions = menuDAO.findPermissionsByUserId(userId);
        List<String> roleCodes = menuDAO.findRoleCodesByUserId(userId);

        // Fallback: Nếu role là ADMIN, đảm bảo có toàn bộ quyền quản trị
        if (roleCodes.contains("ADMIN")) {
            permissions = supplementAdminPermissions(permissions);
        }

        List<Map<String, String>> menuItems = getMenu(permissions);
        UserNavigationProfile profile = getUserNavigationProfile(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("menuItems", menuItems);

        if (profile != null) {
            result.put("user", profile.toMap());
            result.put("userProfile", profile.toMap());
        } else {
            result.put("user", null);
            result.put("userProfile", null);
        }

        result.put("permissions", permissions);
        return result;
    }

    /**
     * Lấy thông tin người dùng (Tên, vai trò, nhóm kinh doanh) từ DAO (AC 2).
     */
    public UserNavigationProfile getUserNavigationProfile(long userId) {
        return menuDAO.findUserNavigationProfile(userId);
    }

    /**
     * Lấy danh sách menu đã được lọc theo permissions của người dùng (AC 1).
     */
    public List<Map<String, String>> getMenuByUserId(long userId) {
        Set<String> permissions = menuDAO.findPermissionsByUserId(userId);
        List<String> roleCodes = menuDAO.findRoleCodesByUserId(userId);
        if (roleCodes.contains("ADMIN")) {
            permissions = supplementAdminPermissions(permissions);
        }
        return getMenu(permissions);
    }

    /**
     * Lọc danh sách menu dựa trên tập hợp permissions.
     * Mục menu không thuộc quyền của người dùng thì KHÔNG được thêm vào danh sách (AC 1).
     */
    public List<Map<String, String>> getMenu(Set<String> permissions) {
        if (permissions == null) {
            permissions = Set.of();
        }

        List<Map<String, String>> menu = new ArrayList<>();

        /*
         * Dashboard luôn hiển thị cho người dùng đã đăng nhập vào hệ thống.
         */
        menu.add(item("DASHBOARD", "Tổng quan", "dashboard.html", "workspace", "dashboard"));

        /* =====================================================
           WORKSPACE (Không gian làm việc)
        ===================================================== */

        if (permissions.contains("customer.read")) {
            menu.add(item("CUSTOMERS", "Khách hàng", "customers.html", "workspace", "customer"));
        }

        if (permissions.contains("contact.read")) {
            menu.add(item("CONTACTS", "Người liên hệ", "contacts.html", "workspace", "contact"));
        }

        if (permissions.contains("opportunity.read")) {
            menu.add(item("OPPORTUNITIES", "Cơ hội", "pipeline.html", "workspace", "opportunity"));
        }

        if (permissions.contains("activity.read")) {
            menu.add(item("ACTIVITIES", "Hoạt động", "activities.html", "workspace", "activity"));
            menu.add(item("CALENDAR", "Lịch", "calendar.html", "workspace", "calendar"));
        }

        if (permissions.contains("task.read")) {
            menu.add(item("TASKS", "Công việc", "tasks.html", "workspace", "task"));
        }

        if (permissions.contains("quote.read")) {
            menu.add(item("QUOTATIONS", "Báo giá", "quotations.html", "workspace", "quote"));
        }

        if (permissions.contains("contract.read")) {
            menu.add(item("CONTRACTS", "Hợp đồng", "contracts.html", "workspace", "contract"));
        }

        if (permissions.contains("support.read")) {
            menu.add(item("SUPPORT_TICKETS", "Chăm sóc & Hỗ trợ", "support-tickets.html", "workspace", "support"));
        }

        if (permissions.contains("customer.read")) {
            menu.add(item("PERIODIC_CARE", "Chăm sóc định kỳ", "periodic-care.html", "workspace", "calendar"));
        }

        if (permissions.contains("product.read")) {
            menu.add(item("PRODUCTS", "Sản phẩm", "products.html", "workspace", "product"));
        }

        /* =====================================================
           ADMIN (Khu vực Quản trị - Chỉ hiển thị khi có quyền)
        ===================================================== */

        if (permissions.contains("user.read")) {
            menu.add(item("USERS", "Người dùng", "users.html", "admin", "users"));
        }

        if (permissions.contains("user.import")) {
            menu.add(item("USER_IMPORT", "Nhập người dùng Excel", "import-users.html", "admin", "import"));
        }

        if (permissions.contains("audit.read")) {
            menu.add(item("AUDIT", "Nhật ký hệ thống", "audit-log.html", "admin", "audit"));
        }

        if (permissions.contains("organization.read")) {
            menu.add(item("ORGANIZATION", "Tổ chức", "organization.html", "admin", "organization"));
        }

        if (permissions.contains("masterdata.read")) {
            menu.add(item("MASTER_DATA", "Danh mục dùng chung", "master-data.html", "admin", "master"));
        }

        if (permissions.contains("customfield.read")) {
            menu.add(item("CUSTOM_FIELDS", "Trường tùy chỉnh", "custom-fields.html", "admin", "custom"));
        }

        if (permissions.contains("pipeline.read")) {
            menu.add(item("PIPELINE_SETTINGS", "Cấu hình Pipeline", "pipeline-settings.html", "admin", "settings"));
        }

        if (permissions.contains("winloss.read") || permissions.contains("competitor.read")) {
            menu.add(item("WIN_LOSS", "Lý do thắng/thua & Đối thủ", "win-loss-settings.html", "admin", "winLoss"));
        }

        if (permissions.contains("role.read") || permissions.contains("role.manage")) {
            menu.add(item("ROLES", "Phân quyền vai trò", "permissions.html", "admin", "role"));
        }

        return menu;
    }

    /**
     * Chuyển đổi danh sách Menu sang dạng danh sách DTO MenuItem.
     */
    public List<MenuItem> getMenuItems(Set<String> permissions) {
        List<Map<String, String>> rawMenu = getMenu(permissions);
        List<MenuItem> items = new ArrayList<>();
        for (Map<String, String> m : rawMenu) {
            items.add(new MenuItem(
                    m.get("code"),
                    m.get("label"),
                    m.get("path"),
                    m.get("section"),
                    m.get("icon")
            ));
        }
        return items;
    }

    private Set<String> supplementAdminPermissions(Set<String> existing) {
        Set<String> full = new java.util.HashSet<>(existing);
        full.addAll(List.of(
                "customer.read", "contact.read", "opportunity.read", "activity.read",
                "task.read", "quote.read", "contract.read", "support.read", "product.read",
                "user.read", "user.import", "audit.read", "organization.read",
                "masterdata.read", "customfield.read", "pipeline.read", "winloss.read",
                "competitor.read", "role.read", "role.manage"
        ));
        return full;
    }

    private Map<String, String> item(
            String code,
            String label,
            String path,
            String section,
            String icon
    ) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("name", code);
        item.put("label", label);
        item.put("path", path);
        item.put("section", section);
        item.put("icon", icon);
        return item;
    }
}