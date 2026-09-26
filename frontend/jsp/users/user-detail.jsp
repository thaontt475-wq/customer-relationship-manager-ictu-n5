<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.lang.reflect.Method" %>
<%--
  User Detail & Account Lock / Handover View (CRM-30 / S1-10: Khóa tài khoản và bàn giao dữ liệu)
  API Base đã thống nhất: /api/users
  BLOCKER: Chờ Backend chốt endpoint khóa tài khoản và bàn giao dữ liệu.
  Request attributes:
    - user / targetUser: Thông tin tài khoản đang xem.
    - availableRecipients / activeUsers / users: Danh sách nhân sự khả dụng để nhận bàn giao.
    - error: Thông báo lỗi.
    - message: Thông báo thành công.
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
        if (obj == null) return "";
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

    // Lấy thông tin user mục tiêu
    Object targetUser = request.getAttribute("user");
    if (targetUser == null) {
        targetUser = request.getAttribute("targetUser");
    }

    String userId = getProp(targetUser, "id");
    if (userId.isEmpty()) {
        userId = request.getParameter("id");
    }
    if (userId == null) userId = "";

    String fullName = getProp(targetUser, "fullName");
    if (fullName.isEmpty()) fullName = getProp(targetUser, "name");
    if (fullName.isEmpty()) fullName = getProp(targetUser, "username");
    if (fullName.isEmpty() && !userId.isEmpty()) fullName = "Người dùng #" + userId;
    if (fullName.isEmpty()) fullName = "Thông tin tài khoản";

    String username = getProp(targetUser, "username");
    String email = getProp(targetUser, "email");
    String phone = getProp(targetUser, "phone");
    if (phone.isEmpty()) phone = getProp(targetUser, "phoneNumber");
    String role = getProp(targetUser, "role");
    if (role.isEmpty()) role = getProp(targetUser, "roleName");
    String department = getProp(targetUser, "department");
    String createdAt = getProp(targetUser, "createdAt");
    String lastLogin = getProp(targetUser, "lastLogin");

    String status = getProp(targetUser, "status");
    boolean isLocked = "LOCKED".equalsIgnoreCase(status)
                    || "0".equals(status)
                    || "Đã khóa".equalsIgnoreCase(status)
                    || "DISABLED".equalsIgnoreCase(status);

    String avatarLetter = !fullName.isEmpty() ? fullName.substring(0, 1).toUpperCase() : "U";

    // Danh sách nhân sự có thể nhận bàn giao dữ liệu
    Object rawRecipients = request.getAttribute("availableRecipients");
    if (rawRecipients == null) {
        rawRecipients = request.getAttribute("activeUsers");
    }
    if (rawRecipients == null) {
        rawRecipients = request.getAttribute("users");
    }
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
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="${pageContext.request.contextPath}/users">Quản lý người dùng</a>
                    <span class="separator">/</span>
                    <span class="active">Chi tiết tài khoản #<%= escapeHtml(userId) %></span>
                </nav>

                <!-- Header màn hình -->
                <header class="user-header">
                    <div class="user-header-info">
                        <h1>Chi tiết tài khoản & Quản lý trạng thái</h1>
                        <p>Xem thông tin định danh, kiểm tra trạng thái hoạt động và thực hiện khóa tài khoản / bàn giao dữ liệu nghiệp vụ.</p>
                    </div>
                    <div class="user-header-badges">
                        <span class="user-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            </svg>
                            CRM-30 / S1-10
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

                <!-- Bố cục Profile & Thao tác Bàn giao -->
                <div class="user-profile-layout">

                    <!-- Cột trái: Thẻ thông tin cá nhân -->
                    <aside class="user-profile-sidebar" aria-label="Thông tin người dùng">
                        <div class="user-card">
                            <div class="user-info-summary">
                                <div class="user-avatar-lg<%= isLocked ? " user-avatar-lg--locked" : "" %>" aria-hidden="true">
                                    <%= escapeHtml(avatarLetter) %>
                                </div>
                                <h2><%= escapeHtml(fullName) %></h2>
                                <div class="email"><%= escapeHtml(email.isEmpty() ? "Chưa có email" : email) %></div>
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
                                    <span class="user-details-value"><%= escapeHtml(role.isEmpty() ? "Chưa phân vai trò" : role) %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Phòng ban</span>
                                    <span class="user-details-value"><%= escapeHtml(department.isEmpty() ? "Kinh doanh / CRM" : department) %></span>
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
                                        <h3 id="lock-action-title" class="lock-warning-title">Khóa tài khoản & Bàn giao dữ liệu</h3>
                                        <p class="lock-warning-desc">
                                            Hành động này sẽ <strong>chặn ngay lập tức quyền truy cập</strong> của người dùng vào hệ thống CRM.
                                            Để bảo toàn tính liên tục trong vận hành, vui lòng chọn nhân sự tiếp nhận toàn bộ dữ liệu phụ trách của tài khoản này.
                                        </p>
                                    </div>
                                </div>

                                <%-- BLOCKER: Chờ Backend chốt endpoint khóa tài khoản và bàn giao dữ liệu. --%>
                                <!-- Container UI Khóa & Bàn giao (Trạng thái chờ Backend Integration, không submit form) -->
                                <div class="handover-form">
                                    <!-- ID tài khoản bị khóa -->
                                    <input type="hidden" name="userId" value="<%= escapeHtml(userId) %>">

                                    <!-- Thông tin người bàn giao -->
                                    <div class="form-group">
                                        <label class="form-label">Tài khoản bị khóa (Bàn giao dữ liệu):</label>
                                        <div class="user-assignee-display">
                                            <%= escapeHtml(fullName) %> <%= !email.isEmpty() ? "(" + escapeHtml(email) + ")" : "" %>
                                            <span class="user-assignee-id">[ID: #<%= escapeHtml(userId) %>]</span>
                                        </div>
                                    </div>

                                    <!-- Chọn người tiếp nhận bàn giao -->
                                    <div class="form-group">
                                        <label for="recipientId" class="form-label">
                                            Người nhận bàn giao dữ liệu <span class="form-label-required">*</span>
                                        </label>
                                        <select class="form-select" id="recipientId" name="recipientId" required>
                                            <option value="">-- Chọn nhân sự tiếp nhận bàn giao dữ liệu --</option>
                                            <% if (recipients != null && !recipients.isEmpty()) {
                                                for (Object rItem : recipients) {
                                                    if (rItem == null) continue;
                                                    String rId = getProp(rItem, "id");
                                                    // QUY TẮC AN TOÀN: Không cho phép chọn chính tài khoản đang bị khóa làm người nhận
                                                    if (!userId.isEmpty() && userId.equals(rId)) {
                                                        continue;
                                                    }
                                                    String rName = getProp(rItem, "fullName");
                                                    if (rName.isEmpty()) rName = getProp(rItem, "name");
                                                    if (rName.isEmpty()) rName = getProp(rItem, "username");
                                                    String rEmail = getProp(rItem, "email");
                                                    String rRole = getProp(rItem, "role");
                                                    if (rRole.isEmpty()) rRole = getProp(rItem, "roleName");
                                            %>
                                                <option value="<%= escapeHtml(rId) %>">
                                                    <%= escapeHtml(rName) %> <%= !rEmail.isEmpty() ? "(" + escapeHtml(rEmail) + ")" : "" %> <%= !rRole.isEmpty() ? " - " + escapeHtml(rRole) : "" %>
                                                </option>
                                            <%   }
                                               } else { %>
                                                <option value="" disabled>Chưa có danh sách nhân sự khả dụng từ Backend (Cần BE API)</option>
                                            <% } %>
                                        </select>
                                        <div class="form-hint">Dữ liệu phụ trách sẽ được chuyển giao quyền quản lý (Owner) sang tài khoản này.</div>
                                    </div>

                                    <!-- Thông tin phạm vi dữ liệu bàn giao (Không invent request parameters) -->
                                    <div class="form-group">
                                        <label class="form-label">Phạm vi dữ liệu chuyển quyền tiếp quản:</label>
                                        <div class="handover-scope-note">
                                            Phạm vi bàn giao: Toàn bộ Khách hàng, Cơ hội bán hàng, Báo giá, Hợp đồng và Hoạt động phụ trách sẽ được chuyển giao theo quy định nghiệp vụ do Backend xử lý.
                                        </div>
                                    </div>

                                    <!-- Lý do khóa -->
                                    <div class="form-group">
                                        <label for="lockReason" class="form-label">
                                            Lý do khóa tài khoản <span class="form-label-required">*</span>
                                        </label>
                                        <textarea class="form-textarea" id="lockReason" name="lockReason" placeholder="Nhập lý do khóa tài khoản (Ví dụ: Nghỉ việc, chuyển công tác, điều chuyển nội bộ...)" required></textarea>
                                    </div>

                                    <!-- Bước xác nhận an toàn (Confirmation Step) -->
                                    <div class="confirmation-box">
                                        <input type="checkbox" id="confirmLockCheckbox" name="confirmLock" required>
                                        <label for="confirmLockCheckbox">
                                            Tôi xác nhận đã kiểm tra kỹ: Tài khoản <strong><%= escapeHtml(fullName) %></strong> sẽ bị khóa quyền truy cập ngay lập tức, và toàn bộ dữ liệu nghiệp vụ sẽ được chuyển giao sang nhân sự tiếp nhận.
                                        </label>
                                    </div>

                                    <!-- Nút hành động (Trạng thái chờ Backend API) -->
                                    <div class="form-actions">
                                        <a href="${pageContext.request.contextPath}/users" class="btn btn-secondary">
                                            Hủy bỏ
                                        </a>
                                        <button type="button" class="btn btn-danger" disabled title="Chờ Backend API">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                            </svg>
                                            Xác nhận Khóa & Bàn giao dữ liệu
                                        </button>
                                    </div>
                                </div>
                            </div>

                        <% } else { %>
                            <!-- TRƯỜNG HỢP 2: Tài khoản ĐÃ BỊ KHÓA (Chỉ hiển thị thông tin trạng thái, không tạo form/nút mở khóa) -->
                            <div class="locked-account-card">
                                <div class="lock-warning-header">
                                    <div class="locked-account-icon" aria-hidden="true">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                        </svg>
                                    </div>
                                    <div>
                                        <h3 class="locked-account-title">Tài khoản này hiện đang bị KHÓA</h3>
                                        <p class="lock-warning-desc">
                                            Tài khoản đã bị ngắt quyền truy cập vào hệ thống CRM. Mọi dữ liệu phụ trách đã được chuyển giao theo quy trình quản trị.
                                        </p>
                                    </div>
                                </div>
                            </div>
                        <% } %>

                    </section>
                </div>

            </div>
        </main>
    </div>
</body>
</html>