package com.crm.service.permissions;

import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CRM-26: Kiểm thử Phân quyền Menu Điều hướng và Thông tin Người dùng")
class MenuServiceTest {

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuService();
    }

    @Test
    @DisplayName("AC 1: Người dùng không có quyền quản trị thì KHÔNG thấy các menu admin")
    void nonAdminUserCannotSeeAdminMenus() {
        // Giả lập user chỉ có quyền xem khách hàng và cơ hội
        Set<String> salesPermissions = Set.of(
                "customer.read",
                "opportunity.read",
                "activity.read",
                "contact.read"
        );

        List<Map<String, String>> menu = menuService.getMenu(salesPermissions);
        List<String> codes = menu.stream().map(m -> m.get("code")).toList();

        // Menu được cấp quyền
        assertTrue(codes.contains("DASHBOARD"), "Dashboard luôn hiển thị");
        assertTrue(codes.contains("CUSTOMERS"), "Hiển thị menu Khách hàng khi có customer.read");
        assertTrue(codes.contains("CONTACTS"), "Hiển thị menu Người liên hệ khi có contact.read");
        assertTrue(codes.contains("OPPORTUNITIES"), "Hiển thị menu Cơ hội khi có opportunity.read");
        assertTrue(codes.contains("ACTIVITIES"), "Hiển thị menu Hoạt động khi có activity.read");

        // Menu không thuộc quyền -> KHÔNG HIỂN THỊ (AC 1)
        assertFalse(codes.contains("USERS"), "Không được hiển thị menu Người dùng khi thiếu user.read");
        assertFalse(codes.contains("USER_IMPORT"), "Không được hiển thị menu Nhập người dùng khi thiếu user.import");
        assertFalse(codes.contains("AUDIT"), "Không được hiển thị menu Nhật ký khi thiếu audit.read");
        assertFalse(codes.contains("ORGANIZATION"), "Không được hiển thị menu Tổ chức khi thiếu organization.read");
        assertFalse(codes.contains("MASTER_DATA"), "Không được hiển thị menu Master Data khi thiếu masterdata.read");
        assertFalse(codes.contains("CUSTOM_FIELDS"), "Không được hiển thị menu Custom Fields khi thiếu customfield.read");
        assertFalse(codes.contains("PIPELINE_SETTINGS"), "Không được hiển thị menu Cấu hình Pipeline khi thiếu pipeline.read");
        assertFalse(codes.contains("WIN_LOSS"), "Không được hiển thị menu Win/Loss khi thiếu winloss.read");
        assertFalse(codes.contains("ROLES"), "Không được hiển thị menu Phân quyền khi thiếu role.read");
    }

    @Test
    @DisplayName("AC 1: Người dùng có quyền quản trị thì thấy đầy đủ menu admin tương ứng")
    void adminUserReceivesAdminMenus() {
        Set<String> adminPermissions = Set.of(
                "user.read",
                "user.import",
                "audit.read",
                "organization.read",
                "masterdata.read",
                "customfield.read",
                "pipeline.read",
                "winloss.read",
                "role.read"
        );

        List<Map<String, String>> menu = menuService.getMenu(adminPermissions);
        List<String> codes = menu.stream().map(m -> m.get("code")).toList();

        assertTrue(codes.contains("DASHBOARD"));
        assertTrue(codes.contains("USERS"));
        assertTrue(codes.contains("USER_IMPORT"));
        assertTrue(codes.contains("AUDIT"));
        assertTrue(codes.contains("ORGANIZATION"));
        assertTrue(codes.contains("MASTER_DATA"));
        assertTrue(codes.contains("CUSTOM_FIELDS"));
        assertTrue(codes.contains("PIPELINE_SETTINGS"));
        assertTrue(codes.contains("WIN_LOSS"));
        assertTrue(codes.contains("ROLES"));
    }

    @Test
    @DisplayName("AC 1: Phân quyền các module hợp đồng, báo giá, hỗ trợ sau bán")
    void specializedModulesMenuVisibility() {
        Set<String> permissions = Set.of("contract.read", "quote.read", "support.read");
        List<Map<String, String>> menu = menuService.getMenu(permissions);
        List<String> codes = menu.stream().map(m -> m.get("code")).toList();

        assertTrue(codes.contains("CONTRACTS"), "Hiển thị menu Hợp đồng khi có contract.read");
        assertTrue(codes.contains("QUOTATIONS"), "Hiển thị menu Báo giá khi có quote.read");
        assertTrue(codes.contains("SUPPORT_TICKETS"), "Hiển thị menu Hỗ trợ khi có support.read");
        assertFalse(codes.contains("USERS"), "Không hiển thị menu Users");
    }

    @Test
    @DisplayName("AC 1: Null hoặc rỗng permissions chỉ hiển thị Dashboard mặc định")
    void emptyPermissionsOnlyReturnsDashboard() {
        List<Map<String, String>> nullResult = menuService.getMenu(null);
        assertEquals(1, nullResult.size());
        assertEquals("DASHBOARD", nullResult.get(0).get("code"));

        List<Map<String, String>> emptyResult = menuService.getMenu(Set.of());
        assertEquals(1, emptyResult.size());
        assertEquals("DASHBOARD", emptyResult.get(0).get("code"));
    }

    @Test
    @DisplayName("AC 2: UserNavigationProfile lưu trữ đầy đủ Tên, Vai trò và Nhóm kinh doanh")
    void userNavigationProfileContainsRequiredInformation() {
        UserNavigationProfile profile = new UserNavigationProfile(
                101L,
                "Nguyễn Văn Tiến",
                "Tiến Nguyễn",
                "Nhân viên kinh doanh",
                List.of("Nhân viên kinh doanh", "Kinh doanh Miền Bắc"),
                5L,
                "Đội Bán Hàng 1"
        );

        assertEquals(101L, profile.getUserId());
        assertEquals("Nguyễn Văn Tiến", profile.getFullName());
        assertEquals("Tiến Nguyễn", profile.getDisplayName());
        assertEquals("Nhân viên kinh doanh", profile.getRole());
        assertEquals(2, profile.getRoles().size());
        assertEquals(5L, profile.getTeamId());
        assertEquals("Đội Bán Hàng 1", profile.getTeamName());

        Map<String, Object> map = profile.toMap();
        assertEquals(101L, map.get("userId"));
        assertEquals("Nguyễn Văn Tiến", map.get("fullName"));
        assertEquals("Tiến Nguyễn", map.get("displayName"));
        assertEquals("Nhân viên kinh doanh", map.get("role"));
        assertEquals("Đội Bán Hàng 1", map.get("teamName"));
    }

    @Test
    @DisplayName("AC 2: Xử lý fallback an toàn khi Tên hiển thị hoặc Nhóm kinh doanh bị null/trống")
    void userNavigationProfileFallbackDefaults() {
        UserNavigationProfile profile = new UserNavigationProfile(
                202L,
                "Trần Thị Bình",
                null,
                null,
                null,
                null,
                ""
        );

        assertEquals("Trần Thị Bình", profile.getDisplayName(), "Tự fallback sang fullName khi displayName null");
        assertEquals("Người dùng", profile.getRole(), "Fallback sang vai trò mặc định");
        assertEquals("Chưa thuộc nhóm", profile.getTeamName(), "Fallback sang Chưa thuộc nhóm khi teamName trống");
        assertTrue(profile.getRoles().isEmpty());
    }

    @Test
    @DisplayName("AC 3: Cấu trúc Menu Items hợp lệ cho cả màn hình Desktop và Mobile 360px")
    void menuItemsStructureCompatibleWith360pxLayout() {
        Set<String> allSamplePermissions = Set.of(
                "customer.read", "contact.read", "contract.read", "user.read"
        );

        List<MenuItem> items = menuService.getMenuItems(allSamplePermissions);
        assertFalse(items.isEmpty());

        for (MenuItem item : items) {
            assertNotNull(item.getCode(), "Mã menu không được rỗng");
            assertNotNull(item.getLabel(), "Tên hiển thị menu không được rỗng");
            assertNotNull(item.getPath(), "Đường dẫn không được rỗng");
            assertTrue("workspace".equals(item.getSection()) || "admin".equals(item.getSection()),
                    "Section phải là workspace hoặc admin để tương thích layout");
            assertNotNull(item.getIcon(), "Icon không được rỗng để hiển thị tốt trên 360px");

            Map<String, Object> map = item.toMap();
            assertTrue(map.containsKey("name"));
            assertTrue(map.containsKey("path"));
            assertTrue(map.containsKey("icon"));
        }
    }
}
