package com.crm.service.permissions;

import com.crm.dto.permissions.MenuItem;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MenuService {
    // F/W/R/W*/R* all grant menu visibility; this does not authorize module actions
    // or enforce SELF/TEAM/ALL data scope. List order is the response order.
    private static final List<MenuRule> MENU_RULES = List.of(
        rule("SALES_CONFIG", "Danh mục & cấu hình bán hàng", "/permissions",
            "sales rep", "marketing", "cust. success", "accountant", "team lead", "director", "admin"),
        rule("CUSTOMERS", "Khách hàng & liên hệ", "/customers",
            "sales rep", "marketing", "cust. success", "accountant", "team lead", "director", "admin"),
        rule("LEADS", "Lead & phân bổ", null, "sales rep", "marketing", "team lead", "director", "admin"),
        rule("OPPORTUNITIES", "Cơ hội & pipeline", "/opportunities",
            "sales rep", "marketing", "cust. success", "accountant", "team lead", "director", "admin"),
        rule("ACTIVITIES", "Hoạt động & lịch làm việc", "/activities",
            "sales rep", "marketing", "cust. success", "team lead", "director", "admin"),
        rule("QUOTES_CONTRACTS", "Báo giá & hợp đồng", "/quotes",
            "sales rep", "cust. success", "accountant", "team lead", "director", "admin"),
        rule("KPI", "Chỉ tiêu & KPI", null, "sales rep", "accountant", "team lead", "director", "admin"),
        rule("REPORTS", "Báo cáo & dashboard", null,
            "sales rep", "marketing", "cust. success", "accountant", "team lead", "director", "admin"),
        rule("AUTOMATION", "Tự động hoá & thông báo", null,
            "sales rep", "marketing", "cust. success", "team lead", "director", "admin"),
        rule("USERS_AUDIT", "Người dùng & nhật ký", "/users", "director", "admin")
    );

    public List<MenuItem> getMenuItems(Collection<String> roles) {
        if (roles == null) {
            return List.of();
        }
        Set<String> normalizedRoles = roles.stream()
            .filter(role -> role != null)
            .map(role -> role.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

        // Visit each module once so overlapping roles cannot duplicate menu items.
        return MENU_RULES.stream()
            .filter(rule -> rule.roles().stream().anyMatch(normalizedRoles::contains))
            .map(MenuRule::item)
            .toList();
    }

    private static MenuRule rule(String code, String label, String url, String... roles) {
        return new MenuRule(new MenuItem(code, label, url, null, List.of()), Set.of(roles));
    }

    private record MenuRule(MenuItem item, Set<String> roles) { }
}
