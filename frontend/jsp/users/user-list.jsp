<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.lang.reflect.Method" %>
<%--
  User List View (CRM-30 / S1-10: Khóa tài khoản và bàn giao dữ liệu)
  API Base đã thống nhất: /api/users

  CONTRACT GIAO DIỆN & BACKEND:
    - Request attributes:
        + users (List<?>): Danh sách người dùng
        + error (String): Thông báo lỗi
        + message (String): Thông báo thành công
    - Property names (User Model):
        + id, fullName, email, role, status
    - Status values:
        + ACTIVE: Đang hoạt động
        + LOCKED: Đã khóa

  BLOCKER:
    - Chờ Backend hoàn tất UserServlet / UserService / UserDAO trả dữ liệu qua attribute "users".
--%>
<%!
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    private String getProp(Object obj, String propName) {
        if (obj == null || propName == null) return "";
        if (obj instanceof java.util.Map<?, ?> map) {
            Object v = map.get(propName);
            return v != null ? v.toString() : "";
        }
        try {
            String getter = "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
            Method m = obj.getClass().getMethod(getter);
            Object v = m.invoke(obj);
            return v != null ? v.toString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }
%>
<%
    String errorMsg = (String) request.getAttribute("error");
    String messageMsg = (String) request.getAttribute("message");

    // Sử dụng duy nhất 1 request attribute "users", không fallback đoán tên khác
    Object rawUsers = request.getAttribute("users");
    List<?> users = (rawUsers instanceof List<?>) ? (List<?>) rawUsers : null;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý người dùng & Khóa tài khoản - CRM</title>

    <!-- CSS dùng chung của hệ thống -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng của module Users -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/users.css">
</head>
<body class="crm-body">

    <!-- Include Header dùng chung -->
    <jsp:include page="../shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Include Sidebar dùng chung -->
        <jsp:include page="../shared/sidebar.jsp" />

        <!-- Nội dung chính của màn hình Quản lý người dùng -->
        <main class="user-page" id="userApp">
            <div class="user-container">

                <!-- Breadcrumb -->
                <nav class="user-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="#">Hệ thống</a>
                    <span class="separator">/</span>
                    <span class="active">Quản lý người dùng</span>
                </nav>

                <!-- Header màn hình -->
                <header class="user-header">
                    <div class="user-header-info">
                        <h1>Quản lý người dùng & Tài khoản</h1>
                        <p>Theo dõi trạng thái hoạt động, quản lý phân quyền và thực hiện khóa tài khoản / bàn giao dữ liệu nhân sự.</p>
                    </div>
                    <div class="user-header-badges">
                        <span class="user-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                            </svg>
                            S1-10 / CRM-30: Khóa & Bàn giao
                        </span>
                    </div>
                </header>

                <!-- Khu vực thông báo (Alerts) -->
                <% if (errorMsg != null && !errorMsg.trim().isEmpty()) { %>
                    <div class="user-alert user-alert-danger" role="alert">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title">Thông báo lỗi</div>
                            <div><%= escapeHtml(errorMsg) %></div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.remove();" aria-label="Đóng">&times;</button>
                    </div>
                <% } %>

                <% if (messageMsg != null && !messageMsg.trim().isEmpty()) { %>
                    <div class="user-alert user-alert-success" role="status">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title">Thành công</div>
                            <div><%= escapeHtml(messageMsg) %></div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.remove();" aria-label="Đóng">&times;</button>
                    </div>
                <% } %>

                <!-- Card danh sách người dùng -->
                <section class="user-card" aria-labelledby="user-table-title">
                    <div class="user-card-header">
                        <div>
                            <h2 id="user-table-title" class="user-card-title">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                    <circle cx="9" cy="7" r="4"></circle>
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                </svg>
                                Danh sách tài khoản người dùng
                            </h2>
                            <p class="user-card-subtitle">Hiển thị thông tin định danh và trạng thái tài khoản trên hệ thống</p>
                        </div>
                    </div>

                    <!-- Bảng dữ liệu người dùng -->
                    <div class="user-table-responsive">
                        <table class="user-table" aria-label="Bảng danh sách người dùng">
                            <thead>
                                <tr>
                                    <th scope="col" class="table-col-id">ID</th>
                                    <th scope="col">Người dùng</th>
                                    <th scope="col">Vai trò</th>
                                    <th scope="col" class="table-col-status">Trạng thái</th>
                                    <th scope="col" class="table-col-actions">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (users == null || users.isEmpty()) { %>
                                    <tr>
                                        <td colspan="5">
                                            <div class="user-empty-state">
                                                <div class="user-empty-icon" aria-hidden="true">👥</div>
                                                <div class="user-empty-text">Chưa có dữ liệu tài khoản</div>
                                                <p class="table-empty-desc">Danh sách người dùng sẽ hiển thị khi Backend cung cấp dữ liệu qua API base <code>/api/users</code>.</p>
                                            </div>
                                        </td>
                                    </tr>
                                <% } else { %>
                                    <% for (Object item : users) {
                                        if (item == null) continue;
                                        String uid = getProp(item, "id");
                                        String fullName = getProp(item, "fullName");
                                        String email = getProp(item, "email");
                                        String role = getProp(item, "role");
                                        String status = getProp(item, "status");
                                        boolean isLocked = "LOCKED".equalsIgnoreCase(status);
                                        String avatarLetter = !fullName.isEmpty() ? fullName.substring(0, 1).toUpperCase() : "U";
                                    %>
                                        <tr>
                                            <td><strong>#<%= escapeHtml(uid) %></strong></td>
                                            <td>
                                                <div class="user-info-cell">
                                                    <div class="user-avatar-sm<%= isLocked ? " user-avatar-sm--locked" : "" %>" aria-hidden="true">
                                                        <%= escapeHtml(avatarLetter) %>
                                                    </div>
                                                    <div>
                                                        <div class="user-name-text"><%= escapeHtml(!fullName.isEmpty() ? fullName : "Chưa có tên") %></div>
                                                        <div class="user-email-text"><%= escapeHtml(!email.isEmpty() ? email : "Chưa có email") %></div>
                                                    </div>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="role-badge"><%= escapeHtml(!role.isEmpty() ? role : "Chưa phân vai trò") %></span>
                                            </td>
                                            <td>
                                                <% if (isLocked) { %>
                                                    <span class="status-badge status-badge--locked">
                                                        <span class="status-dot" aria-hidden="true"></span>
                                                        Đã khóa
                                                    </span>
                                                <% } else { %>
                                                    <span class="status-badge status-badge--active">
                                                        <span class="status-dot" aria-hidden="true"></span>
                                                        Đang hoạt động
                                                    </span>
                                                <% } %>
                                            </td>
                                            <td class="table-col-actions">
                                                <div class="user-actions-group">
                                                    <a href="${pageContext.request.contextPath}/users/detail?id=<%= escapeHtml(uid) %>" class="btn btn-sm btn-secondary" title="Xem thông tin chi tiết và quản lý bàn giao">
                                                        Chi tiết
                                                    </a>
                                                    <% if (!isLocked) { %>
                                                        <a href="${pageContext.request.contextPath}/users/detail?id=<%= escapeHtml(uid) %>#lock-handover-area" class="btn btn-sm btn-outline-danger" title="Khóa tài khoản và chuyển quyền bàn giao dữ liệu">
                                                            Khóa
                                                        </a>
                                                    <% } %>
                                                </div>
                                            </td>
                                        </tr>
                                    <% } %>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>
        </main>
    </div>
</body>
</html>