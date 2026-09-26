<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  Shared Header Component (CRM-22 / S1-02: Session + Logout)
  UI Shell an toàn cho Topbar, User Profile và Nút Đăng xuất.
  API Contract chính thức:
    - Session API: GET ${pageContext.request.contextPath}/api/auth/session
    - Logout API: POST ${pageContext.request.contextPath}/api/auth/logout
  LƯU Ý TÍCH HỢP BE:
    API logout trả JSON. BE Thái cần phối hợp xử lý chuyển hướng (redirect) về /login
    hoặc Filter điều hướng người dùng sau khi session bị hủy.
--%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">

<header class="crm-header" role="banner">
    <div class="crm-header__container">
        <!-- Brand / Hệ thống -->
        <div class="crm-header__brand">
            <a href="${pageContext.request.contextPath}/" class="crm-header__brand-link" title="Trang chủ CRM">
                <span class="crm-header__brand-mark" aria-hidden="true">CRM</span>
                <div class="crm-header__brand-info">
                    <span class="crm-header__brand-title">CRM System</span>
                    <span class="crm-header__brand-subtitle">Quản trị Khách hàng</span>
                </div>
            </a>
        </div>

        <!-- User Profile & Logout Actions -->
        <div class="crm-header__actions">
            <!-- User Info Widget (Hiển thị nhãn generic an toàn, không tự suy đoán session attribute) -->
            <div class="crm-header__user" title="Tài khoản người dùng">
                <div class="crm-header__avatar" aria-hidden="true">
                    <span>U</span>
                </div>
                <div class="crm-header__user-details">
                    <div class="crm-header__user-name">Tài khoản</div>
                    <div class="crm-header__user-meta">
                        <span class="crm-header__status-text">Đang hoạt động</span>
                    </div>
                </div>
            </div>

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