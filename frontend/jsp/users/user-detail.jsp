<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.lang.reflect.Method" %>
<%--
  User Detail & Account Lock / Handover View (CRM-30 / S1-10: Khóa tài khoản và bàn giao dữ liệu)
  API Base đã thống nhất: /api/users

  CONTRACT GIAO DIỆN & BACKEND:
    - Request attributes:
        + user (Object): Thông tin tài khoản cần xem / khóa
        + availableRecipients (List<?>): Danh sách nhân sự khả dụng để nhận bàn giao
        + error (String): Thông báo lỗi
        + message (String): Thông báo thành công
    - Property names (User Model):
        + id, username, fullName, email, phone, role, department, createdAt, lastLogin, status
    - Status values:
        + ACTIVE: Đang hoạt động
        + LOCKED: Đã khóa

  TRẠNG THÁI TÍCH HỢP:
    - Form view gọi cùng UserService lock/handover transaction với API CRM-30.
    - Customer và Opportunity ownership được bàn giao trước khi khóa commit.
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
        if (obj instanceof java.util.Map<?, ?>) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
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

    // Lấy thông tin user mục tiêu từ duy nhất 1 attribute: "user"
    Object targetUser = request.getAttribute("user");

    String userId = getProp(targetUser, "id");
    String fullName = getProp(targetUser, "fullName");
    String username = getProp(targetUser, "username");
    String email = getProp(targetUser, "email");
    String phone = getProp(targetUser, "phone");
    String role = getProp(targetUser, "role");
    String teamName = getProp(targetUser, "teamName");
    String dataScope = getProp(targetUser, "dataScope");
    String createdAt = getProp(targetUser, "createdAt");
    String lastLogin = getProp(targetUser, "lastLoginAt");

    String status = getProp(targetUser, "status");
    boolean isLocked = "LOCKED".equalsIgnoreCase(status);

    String avatarLetter = !fullName.isEmpty() ? fullName.substring(0, 1).toUpperCase() : "U";

    // Danh sách nhân sự nhận bàn giao từ duy nhất 1 attribute: "availableRecipients"
    Object rawRecipients = request.getAttribute("availableRecipients");
    List<?> recipients = (rawRecipients instanceof List<?>) ? (List<?>) rawRecipients : null;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi tiết tài khoản & Khóa bàn giao - CRM</title>

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

        <!-- Nội dung chính màn hình Chi tiết tài khoản & Khóa bàn giao -->
        <main class="user-page" id="userDetailApp">
            <div class="user-container">

                <!-- Breadcrumb -->
                <nav class="user-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/dashboard">CRM</a>
                    <span class="separator">/</span>
                    <a href="${pageContext.request.contextPath}/users">Quản lý người dùng</a>
                    <span class="separator">/</span>
                    <span class="active">Chi tiết người dùng</span>
                </nav>

                <!-- Header màn hình -->
                <header class="user-header">
                    <div class="user-header-info">
                        <h1><%= escapeHtml(!fullName.isEmpty() ? fullName : "Chi tiết người dùng") %></h1>
                        <p>Thông tin tài khoản, vai trò, nhóm, phạm vi dữ liệu và trạng thái truy cập.</p>
                    </div>
                    <div class="user-header-badges">
                        <span class="user-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            </svg>
                            Quản trị tài khoản
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
                    </div>
                <% } %>

                <!-- Bố cục Profile & Thao tác Bàn giao -->
                <div class="user-profile-layout">

                    <!-- Cột trái: Thẻ thông tin cá nhân -->
                    <aside class="user-profile-sidebar" aria-label="Thông tin người dùng">
                        <div class="user-card">
                            <div class="user-info-summary">
                                <div class="user-avatar-lg<%= isLocked ? " user-avatar-lg--locked" : "" %>" aria-hidden="true">
                                    <%= escapeHtml(avatarLetter) %>
                                </div>
                                <h2><%= escapeHtml(!fullName.isEmpty() ? fullName : "Chưa có tên") %></h2>
                                <div class="email"><%= escapeHtml(!email.isEmpty() ? email : "Chưa có email") %></div>
                                <div>
                                    <% if (isLocked) { %>
                                        <span class="status-badge status-badge--locked">
                                            <span class="status-dot" aria-hidden="true"></span>
                                            Đã khóa tài khoản
                                        </span>
                                    <% } else { %>
                                        <span class="status-badge status-badge--active">
                                            <span class="status-dot" aria-hidden="true"></span>
                                            Đang hoạt động
                                        </span>
                                    <% } %>
                                </div>
                            </div>

                            <div class="user-details-list">
                                <div class="user-details-item">
                                    <span class="user-details-label">Mã ID</span>
                                    <span class="user-details-value">#<%= escapeHtml(userId.isEmpty() ? "---" : userId) %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Tên tài khoản</span>
                                    <span class="user-details-value"><%= escapeHtml(username.isEmpty() ? "---" : username) %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Vai trò</span>
                                    <span class="user-details-value"><%= escapeHtml(!role.isEmpty() ? role : "Chưa phân vai trò") %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Nhóm kinh doanh</span>
                                    <span class="user-details-value"><%= escapeHtml(!teamName.isEmpty() ? teamName : "Chưa gán nhóm") %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Phạm vi dữ liệu</span>
                                    <span class="user-details-value scope-badge scope-badge--<%= escapeHtml(dataScope.toLowerCase(java.util.Locale.ROOT)) %>"><%= "ALL".equalsIgnoreCase(dataScope) ? "Tất cả" : ("TEAM".equalsIgnoreCase(dataScope) ? "Nhóm của tôi" : "Của tôi") %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Số điện thoại</span>
                                    <span class="user-details-value"><%= escapeHtml(phone.isEmpty() ? "---" : phone) %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Ngày tạo</span>
                                    <span class="user-details-value"><%= escapeHtml(createdAt.isEmpty() ? "---" : createdAt) %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Đăng nhập cuối</span>
                                    <span class="user-details-value"><%= escapeHtml(lastLogin.isEmpty() ? "---" : lastLogin) %></span>
                                </div>
                            </div>

                            <div class="user-back-link-wrapper">
                                <a href="${pageContext.request.contextPath}/users" class="btn btn-secondary btn-block">
                                    &larr; Quay lại danh sách
                                </a>
                            </div>
                        </div>
                    </aside>

                    <!-- Cột phải: Khu vực Khóa tài khoản & Bàn giao dữ liệu (CRM-30) -->
                    <section class="user-profile-main" id="lock-handover-area" aria-labelledby="lock-action-title">

                        <% if (!isLocked) { %>
                            <!-- TRƯỜNG HỢP 1: Tài khoản đang hoạt động -> Hiển thị Card Khóa & Bàn giao dữ liệu -->
                            <div class="lock-warning-card lock-warning-card--active">
                                <div class="lock-warning-header">
                                    <div class="lock-warning-icon" aria-hidden="true">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                        </svg>
                                    </div>
                                    <div>
                                        <h3 id="lock-action-title" class="lock-warning-title">Khóa tài khoản và bàn giao</h3>
                                        <p class="lock-warning-desc">
                                            Phiên đăng nhập hiện tại sẽ bị thu hồi ngay sau khi giao dịch khóa thành công.
                                        </p>
                                    </div>
                                </div>

                                <form class="handover-form" method="post" action="${pageContext.request.contextPath}/users/lock-handover">
                                    <!-- ID tài khoản bị khóa -->
                                    <input type="hidden" name="userId" value="<%= escapeHtml(userId) %>">

                                    <!-- Thông tin người bàn giao -->
                                    <div class="form-group">
                                        <label class="form-label">Tài khoản bị khóa</label>
                                        <div class="user-assignee-display">
                                            <%= escapeHtml(!fullName.isEmpty() ? fullName : (!username.isEmpty() ? username : "Tài khoản")) %> <%= !email.isEmpty() ? "(" + escapeHtml(email) + ")" : "" %>
                                        </div>
                                    </div>

                                    <!-- Chọn người tiếp nhận bàn giao -->
                                    <div class="form-group">
                                        <label for="recipientId" class="form-label">
                                            Người tiếp nhận
                                        </label>
                                        <select class="form-select" id="recipientId" name="recipientId" required>
                                            <option value="">-- Chọn người tiếp nhận --</option>
                                            <% if (recipients != null && !recipients.isEmpty()) {
                                                for (Object rItem : recipients) {
                                                    if (rItem == null) continue;
                                                    String rId = getProp(rItem, "id");
                                                    // QUY TẮC AN TOÀN: Không cho phép chọn chính tài khoản đang bị khóa làm người nhận
                                                    if (!userId.isEmpty() && userId.equals(rId)) {
                                                        continue;
                                                    }
                                                    String rName = getProp(rItem, "fullName");
                                                    String rEmail = getProp(rItem, "email");
                                                    String rRole = getProp(rItem, "role");
                                            %>
                                                <option value="<%= escapeHtml(rId) %>">
                                                    <%= escapeHtml(!rName.isEmpty() ? rName : "ID #" + rId) %> <%= !rEmail.isEmpty() ? "(" + escapeHtml(rEmail) + ")" : "" %> <%= !rRole.isEmpty() ? " - " + escapeHtml(rRole) : "" %>
                                                </option>
                                            <%   }
                                               } else { %>
                                                <option value="" disabled>Không có tài khoản ACTIVE phù hợp</option>
                                            <% } %>
                                        </select>
                                        <div class="form-hint">Bắt buộc khi người dùng đang phụ trách ít nhất một Khách hàng hoặc Cơ hội.</div>
                                    </div>

                                    <!-- Thông tin phạm vi dữ liệu bàn giao (Không invent request parameters) -->
                                    <div class="form-group">
                                        <label class="form-label">Dữ liệu được bàn giao</label>
                                        <div class="handover-scope-note">
                                            Khách hàng và Cơ hội đang phụ trách sẽ được chuyển cho người tiếp nhận trong cùng giao dịch khóa tài khoản.
                                        </div>
                                    </div>

                                    <!-- Lý do khóa -->
                                    <div class="form-group">
                                        <label for="lockReason" class="form-label">
                                            Lý do khóa tài khoản <span class="form-label-required">*</span>
                                        </label>
                                        <textarea class="form-textarea" id="lockReason" name="reason" maxlength="500" placeholder="Ví dụ: Nhân viên nghỉ việc, chuyển công tác..." required></textarea>
                                    </div>

                                    <!-- Bước xác nhận an toàn (Confirmation Step) -->
                                    <div class="confirmation-box">
                                        <input type="checkbox" id="confirmLockCheckbox" name="confirm" value="true" required>
                                        <label for="confirmLockCheckbox">
                                            Tôi xác nhận khóa tài khoản <strong><%= escapeHtml(!fullName.isEmpty() ? fullName : (!username.isEmpty() ? username : "này")) %></strong> và bàn giao toàn bộ ownership liên quan.
                                        </label>
                                    </div>

                                    <!-- Nút hành động (Trạng thái chờ Backend API) -->
                                    <div class="form-actions">
                                        <a href="${pageContext.request.contextPath}/users" class="btn btn-secondary">
                                            Hủy bỏ
                                        </a>
                                        <button type="submit" class="btn btn-danger">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                            </svg>
                                            Khóa và bàn giao
                                        </button>
                                    </div>
                                </form>
                            </div>

                        <% } else { %>
                            <!-- Tài khoản đã khóa: cho phép quản trị viên mở lại truy cập. -->
                            <div class="locked-account-card">
                                <div class="lock-warning-header">
                                    <div class="locked-account-icon" aria-hidden="true">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                        </svg>
                                    </div>
                                    <div>
                                        <h3 class="locked-account-title">Tài khoản đang bị khóa</h3>
                                        <p class="lock-warning-desc">
                                            Người dùng không thể đăng nhập và các phiên trước đó đã bị thu hồi.
                                        </p>
                                    </div>
                                </div>
                                <form method="post" action="${pageContext.request.contextPath}/users/unlock" class="form-actions">
                                    <input type="hidden" name="userId" value="<%= escapeHtml(userId) %>">
                                    <a href="${pageContext.request.contextPath}/users" class="btn btn-secondary">Quay lại danh sách</a>
                                    <button type="submit" class="btn btn-primary">Mở khóa tài khoản</button>
                                </form>
                            </div>
                        <% } %>

                    </section>
                </div>

            </div>
        </main>
    </div>
</body>
</html>
