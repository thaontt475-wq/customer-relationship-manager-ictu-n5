package com.crm.service.permissions;

import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuServiceTest {
    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuService();
    }

    @Test
    @DisplayName("Admin receives complete Sprint 1 navigation (Legacy)")
    void adminReceivesCompleteSprintOneNavigation() {
        List<String> codes = codes(menuService.getSprint1MenuItems(List.of("Admin")));

        assertEquals(List.of("DASHBOARD", "USERS", "PERMISSIONS", "CHANGE_PASSWORD", "LOGOUT"),
                codes);
    }

    @Test
    @DisplayName("Regular user does not receive administrative Sprint 1 navigation")
    void regularUserDoesNotReceiveAdministrativeNavigation() {
        List<String> codes = codes(menuService.getSprint1MenuItems(List.of("Sales Rep")));

        assertTrue(codes.contains("DASHBOARD"));
        assertTrue(codes.contains("CHANGE_PASSWORD"));
        assertTrue(codes.contains("LOGOUT"));
        assertFalse(codes.contains("USERS"));
        assertFalse(codes.contains("PERMISSIONS"));
    }

    @Test
    @DisplayName("AC 1: Sales Rep only sees permitted menu items, restricted menus are hidden")
    void salesRepOnlySeesPermittedMenuItems() {
        List<MenuItem> items = menuService.getMenuItems(List.of("Sales Rep"));
        List<String> codes = codes(items);

        // Sales Rep should see sales features
        assertTrue(codes.contains("CUSTOMERS"), "Sales Rep should see CUSTOMERS");
        assertTrue(codes.contains("LEADS"), "Sales Rep should see LEADS");
        assertTrue(codes.contains("OPPORTUNITIES"), "Sales Rep should see OPPORTUNITIES");
        assertTrue(codes.contains("ACTIVITIES"), "Sales Rep should see ACTIVITIES");
        assertTrue(codes.contains("QUOTES_CONTRACTS"), "Sales Rep should see QUOTES_CONTRACTS");

        // Sales Rep should NOT see admin / management features
        assertFalse(codes.contains("SALES_CONFIG"), "Sales Rep must NOT see SALES_CONFIG");
        assertFalse(codes.contains("KPI"), "Sales Rep must NOT see KPI");
        assertFalse(codes.contains("AUTOMATION"), "Sales Rep must NOT see AUTOMATION");
        assertFalse(codes.contains("USERS_AUDIT"), "Sales Rep must NOT see USERS_AUDIT");
    }

    @Test
    @DisplayName("AC 1: Accountant only sees permitted accounting & reporting menu items")
    void accountantOnlySeesPermittedMenuItems() {
        List<MenuItem> items = menuService.getMenuItems(List.of("Accountant"));
        List<String> codes = codes(items);

        assertTrue(codes.contains("CUSTOMERS"));
        assertTrue(codes.contains("ACTIVITIES"));
        assertTrue(codes.contains("QUOTES_CONTRACTS"));
        assertTrue(codes.contains("REPORTS"));

        assertFalse(codes.contains("LEADS"));
        assertFalse(codes.contains("OPPORTUNITIES"));
        assertFalse(codes.contains("SALES_CONFIG"));
        assertFalse(codes.contains("KPI"));
        assertFalse(codes.contains("AUTOMATION"));
        assertFalse(codes.contains("USERS_AUDIT"));
    }

    @Test
    @DisplayName("AC 1: Admin receives full system menu navigation")
    void adminReceivesFullNavigation() {
        List<MenuItem> items = menuService.getMenuItems(List.of("Admin"));
        List<String> codes = codes(items);

        assertTrue(codes.contains("CUSTOMERS"));
        assertTrue(codes.contains("SALES_CONFIG"));
        assertTrue(codes.contains("AUTOMATION"));
        assertTrue(codes.contains("USERS_AUDIT"));

        // Verify submenus under USERS_AUDIT
        MenuItem usersAudit = items.stream()
                .filter(i -> "USERS_AUDIT".equals(i.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(usersAudit);
        List<String> subCodes = usersAudit.getChildren().stream().map(MenuItem::getCode).toList();
        assertTrue(subCodes.contains("USERS_MGT"));
        assertTrue(subCodes.contains("PERMISSIONS_MGT"));
        assertTrue(subCodes.contains("AUDIT_LOG"));
    }

    @Test
    @DisplayName("AC 1: Empty roles collection returns empty list")
    void emptyRolesReturnsEmptyList() {
        assertTrue(menuService.getMenuItems(null).isEmpty());
        assertTrue(menuService.getMenuItems(List.of()).isEmpty());
        assertTrue(menuService.getMenuItems(List.of(" ")).isEmpty());
    }

    @Test
    @DisplayName("AC 2: UserNavigationProfile maintains Name, Role and Team information")
    void userNavigationProfileMaintainsRequiredFields() {
        UserNavigationProfile profile = new UserNavigationProfile(
                100L,
                "Tiến Nguyễn",
                "Nguyễn Văn Tiến",
                "Admin",
                "Đội Kỹ Thuật"
        );

        assertEquals(100L, profile.getUserId());
        assertEquals("Tiến Nguyễn", profile.getDisplayName());
        assertEquals("Nguyễn Văn Tiến", profile.getFullName());
        assertEquals("Admin", profile.getRole());
        assertEquals("Đội Kỹ Thuật", profile.getTeamName());
        assertTrue(profile.getRoles().contains("Admin"));
    }

    @Test
    @DisplayName("AC 2: UserNavigationProfile defaults teamName when not specified")
    void userNavigationProfileDefaultsWhenNull() {
        UserNavigationProfile profile = new UserNavigationProfile(
                101L,
                null,
                null,
                null,
                null
        );

        assertEquals("Người dùng", profile.getDisplayName());
        assertEquals("Người dùng", profile.getRole());
        assertEquals("Chưa phân nhóm", profile.getTeamName());
    }

    @Test
    @DisplayName("Menu authorization route checker blocks unauthorized paths")
    void authorizationChecksPaths() {
        List<String> salesRoles = List.of("Sales Rep");
        List<String> adminRoles = List.of("Admin");

        // Sales Rep is authorized for /customers, but NOT /users or /permissions
        assertTrue(menuService.isAuthorized(salesRoles, "/customers"));
        assertTrue(menuService.isAuthorized(salesRoles, "/leads"));
        assertFalse(menuService.isAuthorized(salesRoles, "/users"));
        assertFalse(menuService.isAuthorized(salesRoles, "/permissions"));
        assertFalse(menuService.isAuthorized(salesRoles, "/audit"));

        // Admin is authorized for all
        assertTrue(menuService.isAuthorized(adminRoles, "/users"));
        assertTrue(menuService.isAuthorized(adminRoles, "/permissions"));
        assertTrue(menuService.isAuthorized(adminRoles, "/audit"));
        assertTrue(menuService.isAuthorized(adminRoles, "/customers"));
    }

    private List<String> codes(List<MenuItem> items) {
        return items.stream().map(MenuItem::getCode).toList();
    }
}
