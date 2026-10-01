<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.User" %>
<%!
    private String escapeDashboard(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    User currentUser = (User) request.getAttribute("currentUser");
    List<String> currentRoles = (List<String>) request.getAttribute("currentRoles");
    boolean canManageUsers = Boolean.TRUE.equals(request.getAttribute("canManageUsers"));
    String displayName = currentUser == null ? "Người dùng" : currentUser.getDisplayName();
    if (displayName == null || displayName.isBlank()) displayName = currentUser == null ? "Người dùng" : currentUser.getFullName();
    if (displayName == null || displayName.isBlank()) displayName = currentUser == null ? "Người dùng" : currentUser.getUsername();
    String teamName = currentUser == null ? null : currentUser.getTeamName();
    String dataScope = currentUser == null ? "SELF" : currentUser.getDataScope();
    if (dataScope == null || dataScope.isBlank()) dataScope = "SELF";
    String roleText = currentRoles == null || currentRoles.isEmpty()
            ? "Chưa phân vai trò" : String.join(", ", currentRoles);
    Object totalUsers = request.getAttribute("totalUsers");
    Object activeUsers = request.getAttribute("activeUsers");
    Object lockedUsers = request.getAttribute("lockedUsers");
    Object totalTeams = request.getAttribute("totalTeams");
    String scopeLabel;
    switch (dataScope.toUpperCase(java.util.Locale.ROOT)) {
        case "TEAM": scopeLabel = "Nhóm của tôi"; break;
        case "ALL": scopeLabel = "Tất cả"; break;
        default: scopeLabel = "Của tôi";
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tổng quan - CRM ICTU</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard/dashboard.css">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />
    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />
        <main class="dashboard crm-page" role="main" aria-labelledby="dashboard-title">
            <div class="dashboard__container crm-page-container">
                <nav class="crm-breadcrumb" aria-label="Đường dẫn trang">
                    <span>CRM</span><span aria-hidden="true">/</span><span aria-current="page">Tổng quan</span>
                </nav>
                <header class="crm-page-header">
                    <div>
                        <h1 id="dashboard-title" class="crm-page-title">Tổng quan</h1>
                        <p class="crm-page-description">Thông tin tài khoản và các thao tác thường dùng.</p>
                    </div>
                    <% if (canManageUsers) { %>
                        <a class="crm-btn crm-btn-primary" href="${pageContext.request.contextPath}/users"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="9" cy="7" r="4"/><path d="M2 21v-2a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v2M20 7v6M17 10h6"/></svg> Quản lý người dùng</a>
                    <% } %>
                </header>
                <section class="dashboard__welcome crm-card" aria-labelledby="welcome-title">
                    <div class="dashboard__greeting">
                        <h2 id="welcome-title">Xin chào, <%= escapeDashboard(displayName) %></h2>
                        <p><%= canManageUsers ? "Theo dõi tài khoản và quản lý quyền truy cập trong hệ thống." : "Thông tin truy cập và công cụ dành cho tài khoản của bạn." %></p>
                    </div>
                    <dl class="dashboard__identity" aria-label="Thông tin truy cập hiện tại">
                        <div><dt>Vai trò</dt><dd><%= escapeDashboard(roleText) %></dd></div>
                        <div><dt>Nhóm</dt><dd><%= escapeDashboard(teamName == null || teamName.isBlank() ? "Chưa gán nhóm" : teamName) %></dd></div>
                        <div><dt>Phạm vi dữ liệu</dt><dd><%= escapeDashboard(scopeLabel) %></dd></div>
                    </dl>
                </section>
                <% if (canManageUsers) { %>
                    <section class="dashboard__section" aria-labelledby="stats-title">
                        <div class="dashboard__section-heading">
                            <h2 id="stats-title" class="crm-card-title">Thống kê người dùng</h2>
                            <a href="${pageContext.request.contextPath}/users">Xem danh sách <span aria-hidden="true">&rarr;</span></a>
                        </div>
                        <div class="dashboard__stats">
                            <article class="dashboard__stat crm-card">
                                <div class="dashboard__stat-heading"><span>Tổng tài khoản</span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="9" cy="7" r="4"/><path d="M2 21v-2a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v2M20 7v6M17 10h6"/></svg></div>
                                <strong><%= totalUsers == null ? "—" : totalUsers %></strong><small>Toàn bộ người dùng</small>
                            </article>
                            <article class="dashboard__stat crm-card">
                                <div class="dashboard__stat-heading"><span>Đang hoạt động</span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg></div>
                                <strong><%= activeUsers == null ? "—" : activeUsers %></strong><small>Tài khoản có thể truy cập</small>
                            </article>
                            <article class="dashboard__stat crm-card">
                                <div class="dashboard__stat-heading"><span>Đã khóa</span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/></svg></div>
                                <strong><%= lockedUsers == null ? "—" : lockedUsers %></strong><small>Tài khoản tạm ngừng truy cập</small>
                            </article>
                            <article class="dashboard__stat crm-card">
                                <div class="dashboard__stat-heading"><span>Nhóm kinh doanh</span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="8" y="2" width="8" height="6" rx="1"/><rect x="2" y="16" width="8" height="6" rx="1"/><rect x="14" y="16" width="8" height="6" rx="1"/><path d="M12 8v4M6 16v-4h12v4"/></svg></div>
                                <strong><%= totalTeams == null ? "—" : totalTeams %></strong><small>Nhóm đang cấu hình</small>
                            </article>
                        </div>
                    </section>
                <% } %>
                <section class="dashboard__section" aria-labelledby="quick-title">
                    <div class="dashboard__section-heading"><h2 id="quick-title" class="crm-card-title">Thao tác nhanh</h2></div>
                    <div class="dashboard__quick-grid">
                        <% if (canManageUsers) { %>
                            <a class="dashboard__quick-card crm-card" href="${pageContext.request.contextPath}/users">
                                <span class="dashboard__quick-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="9" cy="7" r="4"/><path d="M2 21v-2a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v2M20 7v6M17 10h6"/></svg></span>
                                <div><strong>Quản lý người dùng</strong><small>Tìm kiếm, quản lý và bàn giao tài khoản</small></div><span class="dashboard__quick-arrow" aria-hidden="true">&rarr;</span>
                            </a>
                            <a class="dashboard__quick-card crm-card" href="${pageContext.request.contextPath}/permissions">
                                <span class="dashboard__quick-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3 3 7v5c0 5 9 9 9 9s9-4 9-9V7l-9-4Z"/><path d="m8 12 3 3 5-6"/></svg></span>
                                <div><strong>Phân quyền &amp; vai trò</strong><small>Quản lý vai trò, nhóm và phạm vi dữ liệu</small></div><span class="dashboard__quick-arrow" aria-hidden="true">&rarr;</span>
                            </a>
                        <% } %>
                            <a class="dashboard__quick-card crm-card" href="${pageContext.request.contextPath}/change-password">
                                <span class="dashboard__quick-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/></svg></span>
                                <div><strong>Đổi mật khẩu</strong><small>Bảo vệ tài khoản của bạn</small></div><span class="dashboard__quick-arrow" aria-hidden="true">&rarr;</span>
                            </a>
                    </div>
                </section>
            </div>
        </main>
    </div>
</body>
</html>
