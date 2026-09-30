package com.crm.service.permissions;

import com.crm.dao.permissions.MenuDAO;
import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Service processing role-based menu filtering and user navigation profile lookup (CRM-26).
 * Follows Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL 8.0 architecture.
 */
public final class MenuService {
    private final MenuDAO menuDAO;

    public MenuService() {
        this.menuDAO = new MenuDAO();
    }

    public MenuService(MenuDAO menuDAO) {
        this.menuDAO = menuDAO != null ? menuDAO : new MenuDAO();
    }

    // SVG Icons for clean server-rendered sidebar navigation
    private static final String SVG_CUSTOMERS = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2\"></path><circle cx=\"9\" cy=\"7\" r=\"4\"></circle><path d=\"M23 21v-2a4 4 0 0 0-3-3.87\"></path><path d=\"M16 3.13a4 4 0 0 1 0 7.75\"></path></svg>";
    private static final String SVG_LEADS = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3\"></polygon></svg>";
    private static final String SVG_OPPORTUNITIES = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><line x1=\"18\" y1=\"20\" x2=\"18\" y2=\"10\"></line><line x1=\"12\" y1=\"20\" x2=\"12\" y2=\"4\"></line><line x1=\"6\" y1=\"20\" x2=\"6\" y2=\"14\"></line></svg>";
    private static final String SVG_ACTIVITIES = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"3\" y=\"4\" width=\"18\" height=\"18\" rx=\"2\" ry=\"2\"></rect><line x1=\"16\" y1=\"2\" x2=\"16\" y2=\"6\"></line><line x1=\"8\" y1=\"2\" x2=\"8\" y2=\"6\"></line><line x1=\"3\" y1=\"10\" x2=\"21\" y2=\"10\"></line></svg>";
    private static final String SVG_QUOTES = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z\"></path><polyline points=\"14 2 14 8 20 8\"></polyline><line x1=\"16\" y1=\"13\" x2=\"8\" y2=\"13\"></line><line x1=\"16\" y1=\"17\" x2=\"8\" y2=\"17\"></line><polyline points=\"10 9 9 9 8 9\"></polyline></svg>";
    private static final String SVG_SALES_CONFIG = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z\"></path><polyline points=\"3.27 6.96 12 12.01 20.73 6.96\"></polyline><line x1=\"12\" y1=\"22.08\" x2=\"12\" y2=\"12\"></line></svg>";
    private static final String SVG_KPI = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"12\" cy=\"8\" r=\"7\"></circle><polyline points=\"8.21 13.89 7 23 12 20 17 23 15.79 13.88\"></polyline></svg>";
    private static final String SVG_REPORTS = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M21.21 15.89A10 10 0 1 1 8 2.83\"></path><path d=\"M22 12A10 10 0 0 0 12 2v10z\"></path></svg>";
    private static final String SVG_AUTOMATION = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"13 2 3 14 12 14 11 22 21 10 12 10 13 2\"></polygon></svg>";
    private static final String SVG_USERS_AUDIT = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z\"></path></svg>";

    /**
     * Standard role-based menu rules for CRM-26.
     * Roles normalized: admin, director, sales_manager, sales_rep, accountant, team_lead.
     */
    private static final List<MenuRule> MENU_RULES = List.of(
            rule("CUSTOMERS", "Khách hàng", "/customers", SVG_CUSTOMERS, List.of(),
                    "director", "admin", "sales_manager", "sales_rep", "accountant", "team_lead"),
            rule("LEADS", "Đầu mối", "/leads", SVG_LEADS, List.of(),
                    "director", "admin", "sales_manager", "sales_rep", "team_lead"),
            rule("OPPORTUNITIES", "Cơ hội bán hàng", "/pipeline", SVG_OPPORTUNITIES, List.of(),
                    "director", "admin", "sales_manager", "sales_rep", "team_lead"),
            rule("ACTIVITIES", "Hoạt động & Lịch", "/activities", SVG_ACTIVITIES, List.of(),
                    "director", "admin", "sales_manager", "sales_rep", "accountant", "team_lead"),
            rule("QUOTES_CONTRACTS", "Báo giá & hợp đồng", "/quotes", SVG_QUOTES, List.of(),
                    "director", "admin", "sales_manager", "sales_rep", "accountant", "team_lead"),
            rule("SALES_CONFIG", "Cấu hình bán hàng", "/products", SVG_SALES_CONFIG, List.of(),
                    "director", "admin", "sales_manager", "team_lead"),
            rule("KPI", "Chỉ tiêu (KPI)", "/kpi", SVG_KPI, List.of(),
                    "director", "admin", "sales_manager", "team_lead"),
            rule("REPORTS", "Báo cáo & Thống kê", "/winloss", SVG_REPORTS, List.of(),
                    "director", "admin", "sales_manager", "accountant", "team_lead"),
            rule("AUTOMATION", "Tự động hóa", "/automation", SVG_AUTOMATION, List.of(),
                    "director", "admin"),
            rule("USERS_AUDIT", "Người dùng & hệ thống", "/users", SVG_USERS_AUDIT,
                    List.of(
                            new MenuItem("USERS_MGT", "Quản lý người dùng", "/users", null, List.of()),
                            new MenuItem("PERMISSIONS_MGT", "Phân quyền & Dữ liệu", "/permissions", null, List.of()),
                            new MenuItem("AUDIT_LOG", "Nhật ký kiểm toán", "/audit", null, List.of())
                    ),
                    "director", "admin")
    );

    /**
     * Filter menu items based on collection of roles (CRM-26 AC 1).
     * Items not permitted for user roles are NOT returned.
     */
    public List<MenuItem> getMenuItems(Collection<String> roles) {
        if (roles == null) {
            return List.of();
        }
        Set<String> normalizedRoles = normalizeRoles(roles);
        if (normalizedRoles.isEmpty()) {
            return List.of();
        }

        return MENU_RULES.stream()
                .filter(rule -> rule.isPermitted(normalizedRoles))
                .map(MenuRule::item)
                .toList();
    }

    /**
     * Query user roles from DB via MenuDAO (JDBC) and return filtered menu items (CRM-26 AC 1).
     */
    public List<MenuItem> getMenuItemsByUserId(long userId) {
        if (userId <= 0) {
            return List.of();
        }
        List<String> dbRoles = menuDAO.findRoleNamesByUserId(userId);
        return getMenuItems(dbRoles);
    }

    /**
     * Query user navigation profile (Name, Roles, Team) from DB via MenuDAO (JDBC) (CRM-26 AC 2).
     */
    public UserNavigationProfile getUserNavigationProfile(long userId) {
        if (userId <= 0) {
            return null;
        }
        UserNavigationProfile profile = menuDAO.findUserNavigationProfile(userId);
        if (profile == null) {
            List<String> roles = menuDAO.findRoleNamesByUserId(userId);
            String roleLabel = roles.isEmpty() ? "Người dùng" : String.join(", ", roles);
            return new UserNavigationProfile(userId, "Người dùng", "", roleLabel, "Chưa phân nhóm", roles);
        }
        return profile;
    }

    /**
     * Check if a user possessing given roles is authorized to access a specific route or menu code.
     */
    public boolean isAuthorized(Collection<String> roles, String targetPathOrCode) {
        if (targetPathOrCode == null || targetPathOrCode.isBlank()) {
            return true;
        }
        Set<String> normalizedRoles = normalizeRoles(roles);
        if (normalizedRoles.contains("admin") || normalizedRoles.contains("director")) {
            return true;
        }

        String path = targetPathOrCode.trim().toLowerCase(Locale.ROOT);

        for (MenuRule rule : MENU_RULES) {
            MenuItem item = rule.item();
            boolean matchCode = item.getCode().equalsIgnoreCase(path);
            boolean matchUrl = item.getUrl() != null && path.startsWith(item.getUrl().toLowerCase(Locale.ROOT));

            boolean matchChild = false;
            for (MenuItem child : item.getChildren()) {
                if (child.getCode().equalsIgnoreCase(path) ||
                        (child.getUrl() != null && path.startsWith(child.getUrl().toLowerCase(Locale.ROOT)))) {
                    matchChild = true;
                    break;
                }
            }

            if (matchCode || matchUrl || matchChild) {
                return rule.isPermitted(normalizedRoles);
            }
        }

        // Paths not governed by menu rules default to true (e.g. /profile, /change-password)
        return true;
    }

    /**
     * Server-rendered Sprint 1 navigation preserved for backward compatibility and test passing.
     */
    public List<MenuItem> getSprint1MenuItems(Collection<String> roles) {
        Set<String> normalizedRoles = normalizeRoles(roles);
        boolean permissionAdmin = normalizedRoles.contains("admin")
                || normalizedRoles.contains("director");

        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("DASHBOARD", "Tổng quan", "/dashboard", null, List.of()));
        if (permissionAdmin) {
            items.add(new MenuItem("USERS", "Người dùng", "/users", null, List.of()));
            items.add(new MenuItem("PERMISSIONS", "Phân quyền", "/permissions", null, List.of()));
        }
        items.add(new MenuItem("CHANGE_PASSWORD", "Đổi mật khẩu", "/change-password", null, List.of()));
        items.add(new MenuItem("LOGOUT", "Đăng xuất", "/api/auth/logout", null, List.of()));
        return List.copyOf(items);
    }

    /**
     * Server-rendered Sprint 1 navigation query by user ID.
     */
    public List<MenuItem> getSprint1MenuItemsByUserId(long userId) {
        List<String> dbRoles = menuDAO.findRoleNamesByUserId(userId);
        return getSprint1MenuItems(dbRoles);
    }

    /**
     * Normalizes roles into standard lowercase and underscore formats.
     * Maps both "Sales Rep" and "sales_rep", "Team Lead" and "team_lead", etc.
     */
    public Set<String> normalizeRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        for (String role : roles) {
            if (role != null && !role.isBlank()) {
                String clean = role.trim().toLowerCase(Locale.ROOT);
                normalized.add(clean);
                String underscored = clean.replace(' ', '_').replace('-', '_');
                normalized.add(underscored);
                String spaced = clean.replace('_', ' ').replace('-', ' ');
                normalized.add(spaced);

                // Team Lead maps also as sales_manager permissions if needed
                if ("team_lead".equals(underscored) || "team lead".equals(spaced)) {
                    normalized.add("sales_manager");
                }
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static MenuRule rule(String code, String label, String url, String icon,
                                 List<MenuItem> children, String... roles) {
        return new MenuRule(new MenuItem(code, label, url, icon, children), Set.of(roles));
    }

    private record MenuRule(MenuItem item, Set<String> permittedRoles) {
        boolean isPermitted(Set<String> roles) {
            for (String role : roles) {
                if (permittedRoles.contains(role)) {
                    return true;
                }
            }
            return false;
        }
    }
}
