<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">

<header class="crm-header" role="banner">
    <div class="crm-header__container">
        <!-- Brand / Hệ thống & Nút Toggle Mobile Sidebar -->
        <div class="crm-header__brand">
            <!-- Nút Hamburger Menu điều hướng Mobile (AC 3) -->
            <button type="button" class="crm-header__menu-toggle" id="crmHeaderToggleBtn" aria-label="Mở menu điều hướng" title="Mở menu điều hướng">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <line x1="3" y1="12" x2="21" y2="12"></line>
                    <line x1="3" y1="6" x2="21" y2="6"></line>
                    <line x1="3" y1="18" x2="21" y2="18"></line>
                </svg>
            </button>

            <a href="${pageContext.request.contextPath}/" class="crm-header__brand-link" title="Trang chủ CRM">
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
            <a href="${pageContext.request.contextPath}/profile" class="crm-header__user" title="Hồ sơ cá nhân" id="crmHeaderUserWidget" style="text-decoration: none; color: inherit;">
                <div class="crm-header__avatar" aria-hidden="true">
                    <span id="crmHeaderAvatarText">${sessionScope.displayName != null && !sessionScope.displayName.isEmpty() ? sessionScope.displayName.substring(0, 1).toUpperCase() : "U"}</span>
                    <img src="${pageContext.request.contextPath}/profile/avatar/thumbnail" alt="" width="32" height="32"
                         onload="this.previousElementSibling.hidden=true" onerror="this.hidden=true">
                </div>
                <div class="crm-header__user-details">
                    <div class="crm-header__user-name" id="crmHeaderUserName">${sessionScope.displayName != null ? sessionScope.displayName : "Tài khoản"}</div>
                    <div class="crm-header__user-meta">
                        <span class="crm-header__role-badge" id="crmHeaderUserRole" style="display: none;">Vai trò</span>
                        <span class="crm-header__team-name" id="crmHeaderUserTeam" style="display: none;">Nhóm</span>
                        <span class="crm-header__status-text" id="crmHeaderStatusText">Đang hoạt động</span>
                    </div>
                </div>
            </a>

            <a href="${pageContext.request.contextPath}/profile/avatar" class="crm-header__avatar-link" title="Đổi ảnh đại diện">Đổi ảnh</a>

            <!-- Form Đăng xuất (Gửi POST tới endpoint chính thức /api/auth/logout) -->
            <form class="crm-header__logout-form" method="post" action="${pageContext.request.contextPath}/api/auth/logout">
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
