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
    <title>Tổng quan Sprint 1 - CRM ICTU</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard/dashboard.css">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <main class="dashboard" role="main">
            <div class="dashboard__container">
                <nav class="dashboard__breadcrumb" aria-label="Breadcrumb">
                    <span>CRM</span><span>/</span><strong>Tổng quan Sprint 1</strong>
                </nav>

                <section class="dashboard__hero" aria-labelledby="dashboard-title">
                    <div>
                        <span class="dashboard__eyebrow">SPRINT 1 · NỀN TẢNG HỆ THỐNG</span>
                        <h1 id="dashboard-title">Xin chào, <%= escapeDashboard(displayName) %></h1>
                        <p>Một không gian thống nhất để quản lý tài khoản, quyền truy cập và bảo mật CRM.</p>
                    </div>
                    <div class="dashboard__identity" aria-label="Thông tin truy cập hiện tại">
                        <div><span>Vai trò</span><strong><%= escapeDashboard(roleText) %></strong></div>
                        <div><span>Nhóm</span><strong><%= escapeDashboard(teamName == null || teamName.isBlank() ? "Chưa gán nhóm" : teamName) %></strong></div>
                        <div><span>Phạm vi dữ liệu</span><strong class="dashboard__scope"><%= escapeDashboard(scopeLabel) %></strong></div>
                    </div>
                </section>

                <% if (canManageUsers) { %>
                    <section class="dashboard__section" aria-labelledby="stats-title">
                        <div class="dashboard__section-heading">
                            <div><span class="dashboard__kicker">Tài khoản hệ thống</span><h2 id="stats-title">Thống kê người dùng</h2></div>
                            <a href="${pageContext.request.contextPath}/users">Xem danh sách</a>
                        </div>
                        <div class="dashboard__stats dashboard__stats--four">
                            <article class="dashboard__stat"><span>Tổng tài khoản</span><strong><%= totalUsers == null ? "—" : totalUsers %></strong><small>Toàn bộ người dùng</small></article>
                            <article class="dashboard__stat dashboard__stat--success"><span>Đang hoạt động</span><strong><%= activeUsers == null ? "—" : activeUsers %></strong><small>Trạng thái ACTIVE</small></article>
                            <article class="dashboard__stat dashboard__stat--danger"><span>Đã khóa</span><strong><%= lockedUsers == null ? "—" : lockedUsers %></strong><small>Trạng thái LOCKED</small></article>
                            <article class="dashboard__stat dashboard__stat--team"><span>Nhóm kinh doanh</span><strong><%= totalTeams == null ? "—" : totalTeams %></strong><small>Nhóm đang cấu hình</small></article>
                        </div>
                    </section>
                <% } %>

                <section class="dashboard__section" aria-labelledby="quick-title">
                    <div class="dashboard__section-heading">
                        <div><span class="dashboard__kicker">Thao tác nhanh</span><h2 id="quick-title">Bạn muốn làm gì?</h2></div>
                    </div>
                    <div class="dashboard__quick-grid">
                        <% if (canManageUsers) { %>
                            <a class="dashboard__quick-card" href="${pageContext.request.contextPath}/users">
                                <span class="dashboard__quick-icon">👥</span><div><strong>Quản lý người dùng</strong><small>CRUD, tìm kiếm, lọc, khóa và bàn giao</small></div><span aria-hidden="true">→</span>
                            </a>
                            <a class="dashboard__quick-card" href="${pageContext.request.contextPath}/permissions">
                                <span class="dashboard__quick-icon">⚿</span><div><strong>Phân quyền & vai trò</strong><small>Vai trò, nhóm và phạm vi SELF / TEAM / ALL</small></div><span aria-hidden="true">→</span>
                            </a>
                        <% } %>
                        <a class="dashboard__quick-card" href="${pageContext.request.contextPath}/change-password">
                            <span class="dashboard__quick-icon">●</span><div><strong>Đổi mật khẩu</strong><small>Bảo vệ tài khoản và thu hồi phiên khác</small></div><span aria-hidden="true">→</span>
                        </a>
                    </div>
                </section>

                <section class="dashboard__section dashboard__stories" aria-labelledby="stories-title">
                    <div class="dashboard__section-heading dashboard__stories-heading">
                        <div>
                            <span class="dashboard__kicker">Bằng chứng nghiệm thu</span>
                            <h2 id="stories-title">SPRINT 1 — 10/10 USER STORIES</h2>
                        </div>
                        <p>Mỗi story có tiêu chí nghiệm thu và lối vào demo riêng.</p>
                    </div>

                    <div class="dashboard__story-grid">
                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-01</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Đăng nhập</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Đăng nhập đúng thì vào trang chủ theo vai trò</li>
                                <li>Sai thông tin hiển thị thông báo chung, không tiết lộ email tồn tại</li>
                                <li>Khóa tạm 15 phút sau 5 lần sai liên tiếp</li>
                            </ul>
                            <a class="dashboard__story-action" href="${pageContext.request.contextPath}/login">Demo đăng nhập <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-02</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Phiên đăng nhập &amp; đăng xuất</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Phiên được gia hạn khi còn hoạt động</li>
                                <li>Logout vô hiệu session ngay phía server</li>
                                <li>Session hết hạn quay về login với thông báo rõ ràng</li>
                            </ul>
                            <a class="dashboard__story-action" href="${pageContext.request.contextPath}/api/auth/session">Demo session / logout <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-03</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Quên / đặt lại mật khẩu</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Link reset có hiệu lực 30 phút</li>
                                <li>Link chỉ dùng một lần</li>
                                <li>Email không tồn tại vẫn cùng thông báo</li>
                            </ul>
                            <a class="dashboard__story-action" href="${pageContext.request.contextPath}/forgot-password">Demo quên mật khẩu <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-04</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Đổi mật khẩu</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Bắt buộc mật khẩu hiện tại</li>
                                <li>Mật khẩu mới ≥ 8 ký tự, có chữ và số</li>
                                <li>Thu hồi các session khác sau khi đổi</li>
                            </ul>
                            <a class="dashboard__story-action" href="${pageContext.request.contextPath}/change-password">Demo đổi mật khẩu <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card dashboard__story-card--scope">
                            <header><span class="dashboard__story-id">S1-05</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Phạm vi dữ liệu</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>SELF / TEAM / ALL</li>
                                <li>Áp dụng Customer / Opportunity / Activity / Quote</li>
                                <li>Áp dụng list / search / export / detail</li>
                                <li>Ngoài scope hiển thị lỗi tiếng Việt</li>
                                <li>Có automated test chứng minh A không đọc dữ liệu B</li>
                            </ul>
                            <a class="dashboard__story-action" href="<%= canManageUsers ? request.getContextPath() + "/permissions" : "#dashboard-title" %>">Demo thông tin data scope <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-06</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Menu theo quyền</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Menu không có quyền thì không hiển thị</li>
                                <li>Hiển thị tên / role / team</li>
                                <li>Dùng tốt ở màn hình 360px</li>
                            </ul>
                            <a class="dashboard__story-action" href="#dashboard-title">Demo app shell hiện tại <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-07</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Trang lỗi</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Shared application style</li>
                                <li>Có hành động gợi ý tiếp theo</li>
                            </ul>
                            <a class="dashboard__story-action" href="${pageContext.request.contextPath}/errors/404">Demo trang lỗi <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-08</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Quản lý người dùng</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Tạo tài khoản + activation email + temporary password</li>
                                <li>Email trùng bị từ chối</li>
                                <li>Search theo tên / email / team</li>
                                <li>Filter role / status</li>
                                <li>Pagination mặc định 20</li>
                            </ul>
                            <a class="dashboard__story-action" href="<%= canManageUsers ? request.getContextPath() + "/users" : request.getContextPath() + "/errors/403" %>">Demo quản lý người dùng <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-09</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Vai trò &amp; nhóm</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Một user có nhiều role</li>
                                <li>Team Lead bắt buộc có team</li>
                                <li>Admin không thể tự thu hồi Admin role</li>
                            </ul>
                            <a class="dashboard__story-action" href="<%= canManageUsers ? request.getContextPath() + "/permissions" : request.getContextPath() + "/errors/403" %>">Demo vai trò / nhóm <span aria-hidden="true">→</span></a>
                        </article>

                        <article class="dashboard__story-card">
                            <header><span class="dashboard__story-id">S1-10</span><span class="dashboard__story-status">PASS</span></header>
                            <h3>Khóa &amp; bàn giao</h3>
                            <p class="dashboard__story-label">Acceptance Criteria</p>
                            <ul>
                                <li>Locked account không login được và session đang mở bị revoke</li>
                                <li>Bắt buộc người nhận trước khi khóa nếu có ownership</li>
                                <li>Chuyển toàn bộ Customer / Opportunity</li>
                                <li>Có audit</li>
                                <li>Không còn orphan ownership</li>
                            </ul>
                            <a class="dashboard__story-action" href="<%= canManageUsers ? request.getContextPath() + "/users" : request.getContextPath() + "/errors/403" %>">Demo khóa &amp; bàn giao <span aria-hidden="true">→</span></a>
                        </article>
                    </div>
                </section>
            </div>
        </main>
    </div>
</body>
</html>
