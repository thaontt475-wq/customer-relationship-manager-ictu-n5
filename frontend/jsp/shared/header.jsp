<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
private String headerEsc(Object v) {
    if (v == null) return "";
    return String.valueOf(v).replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
}
%>
<%
Object headerName = request.getAttribute("currentUserDisplayName");
if (headerName == null || String.valueOf(headerName).isBlank()) {
    headerName = session == null ? null : session.getAttribute("displayName");
}
String headerDisplayName = headerName == null || String.valueOf(headerName).isBlank() ? "Tài khoản" : String.valueOf(headerName);
Object headerRole = request.getAttribute("currentUserRoleLabel");
Object headerTeam = request.getAttribute("currentUserTeamName");

jakarta.servlet.http.Cookie[] headerCookies = request.getCookies();
boolean headerIsDark = false;
if (headerCookies != null) {
    for (jakarta.servlet.http.Cookie c : headerCookies) {
        if ("crm_theme".equals(c.getName()) && "dark".equals(c.getValue())) {
            headerIsDark = true;
            break;
        }
    }
}
%>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:ital,wght@0,300;0,400;0,500;0,600;0,700;0,800;1,400;1,500&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css?v=20261005_3">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css?v=20261005_3">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/responsive.css?v=20261005_3">

<!-- Checkbox hack cho Chế độ Sáng / Tối (Lưu trạng thái qua Cookie/localStorage) -->
<input type="checkbox" id="crm-theme-toggle" class="crm-theme-cb" <%= headerIsDark ? "checked" : "" %> hidden>
<script>
(function() {
    var cb = document.getElementById('crm-theme-toggle');
    if (!cb) return;
    try {
        var savedTheme = localStorage.getItem('crm_theme');
        if (savedTheme === 'dark' && !cb.checked) {
            cb.checked = true;
            document.cookie = "crm_theme=dark;path=/;max-age=31536000;SameSite=Lax";
        } else if (savedTheme === 'light' && cb.checked) {
            cb.checked = false;
            document.cookie = "crm_theme=light;path=/;max-age=31536000;SameSite=Lax";
        }
    } catch (e) {}
    cb.addEventListener('change', function() {
        var mode = this.checked ? 'dark' : 'light';
        try {
            localStorage.setItem('crm_theme', mode);
        } catch (e) {}
        document.cookie = "crm_theme=" + mode + ";path=/;max-age=31536000;SameSite=Lax";
    });
})();
</script>

<!-- Checkbox hack cho Mobile Drawer Menu (Pure CSS) -->
<input type="checkbox" id="crm-mobile-drawer-cb" class="crm-mobile-drawer-cb" hidden>

<header class="crm-header" role="banner">
    <div class="crm-header__container">
        <!-- Brand / Logo & Nút Hamburger Mobile -->
        <div class="crm-header__brand">
            <!-- Nút Hamburger Menu điều hướng Mobile (Pure CSS Checkbox Hack) -->
            <label for="crm-mobile-drawer-cb" class="crm-header__menu-toggle" aria-label="Mở menu điều hướng" title="Mở menu điều hướng">&#9776;</label>

            <a href="${pageContext.request.contextPath}/dashboard" class="crm-header__brand-link" title="Trang chủ CRM">
                <span class="crm-header__brand-mark" aria-hidden="true">CRM</span>
                <div class="crm-header__brand-info">
                    <span class="crm-header__brand-title">CRM System</span>
                    <span class="crm-header__brand-subtitle">Quản trị Khách hàng</span>
                </div>
            </a>
        </div>

        <!-- Global Search Bar ở giữa (Desktop PC >= 1024px) -->
        <div class="crm-header__search" role="search">
            <svg class="crm-header__search-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input type="search" class="crm-header__search-input" placeholder="Tìm kiếm nhanh khách hàng, cơ hội, tác vụ..." aria-label="Tìm kiếm toàn hệ thống">
            <kbd class="crm-header__search-kbd">Ctrl K</kbd>
        </div>

        <!-- User Profile & Action Buttons -->
        <div class="crm-header__actions">
            <!-- Nút chuyển đổi Giao diện Sáng / Tối (Mặt trời / Mặt trăng) -->
            <label for="crm-theme-toggle" class="crm-header__icon-btn crm-header__theme-btn" title="Chuyển chế độ Sáng / Tối" aria-label="Chuyển chế độ Sáng / Tối">
                <!-- Icon Mặt trời (Hiển thị khi chế độ Tối) -->
                <svg class="crm-icon crm-icon-sun" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="5"></circle>
                    <line x1="12" y1="1" x2="12" y2="3"></line>
                    <line x1="12" y1="21" x2="12" y2="23"></line>
                    <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                    <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                    <line x1="1" y1="12" x2="3" y2="12"></line>
                    <line x1="21" y1="12" x2="23" y2="12"></line>
                    <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                    <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                </svg>
                <!-- Icon Mặt trăng (Hiển thị khi chế độ Sáng) -->
                <svg class="crm-icon crm-icon-moon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
                </svg>
            </label>

            <!-- Icon Chuông thông báo kèm chấm đỏ (Badge) -->
            <div class="crm-header__bell-wrapper">
                <a href="${pageContext.request.contextPath}/audit" class="crm-header__icon-btn crm-header__bell-btn" title="Thông báo hệ thống" aria-label="Thông báo hệ thống">
                    <svg class="crm-icon crm-icon-bell" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                    </svg>
                    <span class="crm-header__bell-badge" aria-label="3 thông báo mới">3</span>
                </a>
            </div>

            <!-- User Info Widget hiển thị Tên, Vai trò và Avatar -->
            <a href="${pageContext.request.contextPath}/profile" class="crm-header__user" title="Hồ sơ cá nhân" aria-label="Hồ sơ cá nhân" id="crmHeaderUserWidget">
                <div class="crm-header__avatar" aria-hidden="true" style="overflow: hidden;">
                    <img src="${pageContext.request.contextPath}/profile/avatar/thumbnail"
                         alt="Avatar"
                         class="crm-header__avatar-img"
                         onload="this.style.display='block'; var s=this.nextElementSibling; if(s) s.style.display='none';"
                         onerror="this.style.display='none'; var s=this.nextElementSibling; if(s) s.style.display='grid';"
                         style="display: none; width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">
                    <span id="crmHeaderAvatarText"><%= headerEsc(headerDisplayName.substring(0, 1).toUpperCase(java.util.Locale.ROOT)) %></span>
                </div>
                <div class="crm-header__user-details">
                    <div class="crm-header__user-name" id="crmHeaderUserName"><%= headerEsc(headerDisplayName) %></div>
                    <div class="crm-header__user-meta">
                        <span class="crm-header__role-badge" id="crmHeaderUserRole"><%= headerEsc(headerRole == null ? "Người dùng" : headerRole) %></span>
                        <span class="crm-header__team-name" id="crmHeaderUserTeam"><%= headerEsc(headerTeam == null ? "Chưa phân nhóm" : headerTeam) %></span>
                        <span class="crm-header__status-text" id="crmHeaderStatusText">Đang hoạt động</span>
                    </div>
                </div>
                <svg class="crm-header__user-chevron" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
            </a>

            <!-- Form Đăng xuất (Gửi POST tới endpoint chính thức /api/auth/logout) -->
            <form class="crm-header__logout-form" method="post" action="${pageContext.request.contextPath}/api/auth/logout">
                <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                <input type="hidden" name="redirectToLogin" value="true">
                <button type="submit" class="crm-header__logout-btn" title="Đăng xuất khỏi hệ thống" aria-label="Đăng xuất">
                    <svg class="crm-header__logout-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                        <polyline points="16 17 21 12 16 7"></polyline>
                        <line x1="21" y1="12" x2="9" y2="12"></line>
                    </svg>
                    <span class="crm-header__logout-text">Đăng xuất</span>
                </button>
            </form>
        </div>
    </div>
</header>

<!-- Backdrop overlay khi mở mobile sidebar drawer (Pure CSS Checkbox Hack) -->
<label for="crm-mobile-drawer-cb" class="crm-mobile-drawer__backdrop" aria-hidden="true"></label>

<!-- Mobile Drawer Menu trượt (AC Mobile: Cấu hình, Sản phẩm, Đăng xuất) -->
<aside class="crm-mobile-drawer" id="crmMobileDrawer" aria-label="Menu mở rộng mobile">
    <div class="crm-mobile-drawer__header">
        <div class="crm-mobile-drawer__brand">
            <span class="crm-header__brand-mark" aria-hidden="true">CRM</span>
            <span class="crm-mobile-drawer__brand-title">CRM System</span>
        </div>
        <label for="crm-mobile-drawer-cb" class="crm-mobile-drawer__close" aria-label="Đóng menu" title="Đóng menu">&#10005;</label>
    </div>

    <div class="crm-mobile-drawer__body">
        <!-- Thông tin tóm tắt người dùng -->
        <div class="crm-mobile-drawer__user-card">
            <div class="crm-header__avatar" aria-hidden="true" style="overflow: hidden;">
                <img src="${pageContext.request.contextPath}/profile/avatar/thumbnail"
                     alt="Avatar"
                     class="crm-header__avatar-img"
                     onload="this.style.display='block'; var s=this.nextElementSibling; if(s) s.style.display='none';"
                     onerror="this.style.display='none'; var s=this.nextElementSibling; if(s) s.style.display='grid';"
                     style="display: none; width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">
                <span><%= headerEsc(headerDisplayName.substring(0, 1).toUpperCase(java.util.Locale.ROOT)) %></span>
            </div>
            <div class="crm-mobile-drawer__user-info">
                <div class="crm-mobile-drawer__user-name"><%= headerEsc(headerDisplayName) %></div>
                <div class="crm-mobile-drawer__user-role"><%= headerEsc(headerRole == null ? "Người dùng" : headerRole) %></div>
            </div>
        </div>

        <!-- Danh sách tính năng phụ theo yêu cầu Mobile Drawer -->
        <div class="crm-mobile-drawer__nav">
            <span class="crm-mobile-drawer__section-title">Tính năng hệ thống</span>
            <ul class="crm-mobile-drawer__menu">
                <li>
                    <a href="${pageContext.request.contextPath}/products" class="crm-mobile-drawer__link">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="m7.5 4.27 9 5.15M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/>
                            <path d="m3.3 7 8.7 5 8.7-5M12 22V12"/>
                        </svg>
                        <span>Sản phẩm</span>
                    </a>
                </li>
                <li>
                    <a href="${pageContext.request.contextPath}/configuration" class="crm-mobile-drawer__link">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <circle cx="12" cy="12" r="3"/>
                            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                        </svg>
                        <span>Cấu hình</span>
                    </a>
                </li>
                <li>
                    <a href="${pageContext.request.contextPath}/profile" class="crm-mobile-drawer__link">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                        <span>Hồ sơ cá nhân</span>
                    </a>
                </li>
                <li>
                    <a href="${pageContext.request.contextPath}/audit" class="crm-mobile-drawer__link">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                            <polyline points="14 2 14 8 20 8"/>
                            <line x1="16" y1="13" x2="8" y2="13"/>
                            <line x1="16" y1="17" x2="8" y2="17"/>
                            <polyline points="10 9 9 9 8 9"/>
                        </svg>
                        <span>Nhật ký hoạt động</span>
                    </a>
                </li>
            </ul>
        </div>

        <div class="crm-mobile-drawer__footer">
            <form method="post" action="${pageContext.request.contextPath}/api/auth/logout">
                <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                <input type="hidden" name="redirectToLogin" value="true">
                <button type="submit" class="crm-mobile-drawer__logout-btn">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                        <polyline points="16 17 21 12 16 7"/>
                        <line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                    <span>Đăng xuất</span>
                </button>
            </form>
        </div>
    </div>
</aside>
