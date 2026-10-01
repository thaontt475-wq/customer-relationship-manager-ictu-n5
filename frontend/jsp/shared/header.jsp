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
%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">

<header class="crm-header" role="banner">
    <div class="crm-header__container">
        <!-- Brand / Hệ thống & Nút Toggle Mobile Sidebar -->
        <div class="crm-header__brand">
            <!-- Nút Hamburger Menu điều hướng Mobile (AC 3) -->
            <a href="#crmSidebar" class="crm-header__menu-toggle" aria-label="Mở menu điều hướng" title="Mở menu điều hướng">&#9776;</a>

            <a href="${pageContext.request.contextPath}/dashboard" class="crm-header__brand-link" title="Trang chủ CRM">
                <span class="crm-header__brand-mark" aria-hidden="true">CRM</span>
                <div class="crm-header__brand-info">
                    <span class="crm-header__brand-title">CRM System</span>
                    <span class="crm-header__brand-subtitle">Quản trị Khách hàng</span>
                </div>
            </a>
        </div>

        <!-- User Profile & Logout Actions (AC 2) -->
        <div class="crm-header__actions">
            <!-- User Info Widget hiển thị Tên, Vai trò và Nhóm kinh doanh (AC 2) -->
            <a href="${pageContext.request.contextPath}/profile" class="crm-header__user" title="Hồ sơ cá nhân" aria-label="Hồ sơ cá nhân" id="crmHeaderUserWidget">
                <div class="crm-header__avatar" aria-hidden="true">
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
            </a>

            <a href="${pageContext.request.contextPath}/profile/avatar" class="crm-header__avatar-link" title="Đổi ảnh đại diện">Đổi ảnh</a>

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
