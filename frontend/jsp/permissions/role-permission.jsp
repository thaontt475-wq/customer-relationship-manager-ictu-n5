<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map, java.util.Set, java.util.HashSet" %>
<%!
    /**
     * Phương thức tiện ích lấy giá trị thuộc tính an toàn từ Object (Map, Bean hoặc Record)
     * Tránh phụ thuộc cứng vào cấu trúc class model backend khi chưa hoàn thiện.
     */
    private String getProperty(Object obj, String... propNames) {
        if (obj == null) return "";
        if (obj instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
            for (String prop : propNames) {
                Object val = map.get(prop);
                if (val != null) return String.valueOf(val);
            }
            return "";
        }
        Class<?> clazz = obj.getClass();
        for (String prop : propNames) {
            String getterName = "get" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
            try {
                java.lang.reflect.Method method = clazz.getMethod(getterName);
                Object val = method.invoke(obj);
                if (val != null) return String.valueOf(val);
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(prop);
                field.setAccessible(true);
                Object val = field.get(obj);
                if (val != null) return String.valueOf(val);
            } catch (Exception ignored) {}
        }
        return obj.toString();
    }

    /**
     * Escape ký tự đặc biệt HTML chống lỗi hiển thị và bảo vệ XSS
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
%>
<%
    // Nhận các attribute từ Controller / Servlet chuyển tiếp tới View
    List<?> users = (List<?>) request.getAttribute("users");
    List<?> roles = (List<?>) request.getAttribute("roles");
    Object selectedUser = request.getAttribute("selectedUser");
    List<?> userRoles = (List<?>) request.getAttribute("userRoles");
    String currentDataScope = (String) request.getAttribute("dataScope");
    String error = (String) request.getAttribute("error");
    String successMessage = (String) request.getAttribute("message");

    String selectedUserId = getProperty(selectedUser, "id", "userId");
    if (currentDataScope == null || currentDataScope.trim().isEmpty()) {
        currentDataScope = "SELF";
    }

    // Tập hợp ID các vai trò đã được gán cho người dùng hiện tại
    Set<String> assignedRoleIds = new HashSet<String>();
    if (userRoles != null) {
        for (Object ur : userRoles) {
            if (ur instanceof Number || ur instanceof String) {
                assignedRoleIds.add(String.valueOf(ur));
            } else {
                assignedRoleIds.add(getProperty(ur, "id", "roleId", "code"));
            }
        }
    }

    boolean hasRolesFromBackend = (roles != null && !roles.isEmpty());
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phân quyền & Phạm vi dữ liệu sở hữu - CRM</title>

    <!-- CSS dùng chung của hệ thống (nếu có) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">

    <!-- CSS riêng của module Phân quyền (Permissions) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/permissions/permissions.css">
</head>
<body class="crm-body">

    <!-- Include Header dùng chung -->
    <jsp:include page="../shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Include Sidebar dùng chung -->
        <jsp:include page="../shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Phân quyền -->
        <main class="permission-page" id="permissionApp">
            <div class="permission-container">

                <!-- Breadcrumb -->
                <nav class="permission-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="#">Hệ thống</a>
                    <span class="separator">/</span>
                    <span class="active">Phân quyền & Phạm vi dữ liệu</span>
                </nav>

                <!-- Header màn hình -->
                <header class="permission-header">
                    <div class="permission-header-info">
                        <h1>Phân quyền & Phạm vi dữ liệu sở hữu</h1>
                        <p>Cấu hình vai trò hệ thống (Roles) và phạm vi truy cập dữ liệu (Data Scope) cho người dùng trong CRM.</p>
                    </div>
                    <div class="permission-header-badges">
                        <span class="permission-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            </svg>
                            S1-05 / CRM-25
                        </span>
                    </div>
                </header>

                <!-- Khu vực thông báo (Alerts) -->
                <div class="permission-alerts" id="permissionAlertsArea">
                    <%-- Thông báo lỗi từ Server (nếu có) --%>
                    <% if (error != null && !error.trim().isEmpty()) { %>
                        <div class="permission-alert permission-alert-danger" id="serverErrorAlert">
                            <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <div class="permission-alert-content">
                                <div class="permission-alert-title">Thông báo lỗi</div>
                                <div><%= escapeHtml(error) %></div>
                            </div>
                        </div>
                    <% } %>

                    <%-- Thông báo thành công từ Server (nếu có) --%>
                    <% if (successMessage != null && !successMessage.trim().isEmpty()) { %>
                        <div class="permission-alert permission-alert-success" id="serverSuccessAlert">
                            <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                            <div class="permission-alert-content">
                                <div class="permission-alert-title">Thành công</div>
                                <div><%= escapeHtml(successMessage) %></div>
                            </div>
                        </div>
                    <% } %>

                </div>

                <!-- Form phân quyền chính -->
                <form id="permissionForm" action="${pageContext.request.contextPath}/permissions/assign" method="post">
                    <input type="hidden" name="userId" value="<%= selectedUser != null ? escapeHtml(getProperty(selectedUser, "id", "userId")) : "" %>">

                    <!-- BƯỚC 1: Chọn người dùng -->
                    <section class="permission-card" id="userCardSection">
                        <div class="permission-card-header">
                            <div class="permission-card-title-group">
                                <span class="permission-card-step">1</span>
                                <div>
                                    <h2>Chọn người dùng</h2>
                                    <div class="permission-card-subtitle">Lựa chọn nhân sự cần thiết lập quyền và phạm vi dữ liệu</div>
                                </div>
                            </div>
                        </div>
                        <div class="permission-card-body">
                            <div class="permission-user-selector">
                                <div class="permission-form-group">
                                    <label for="userSelect" class="permission-label">Tài khoản người dùng <span style="color: var(--perm-danger);">*</span></label>
                                    <div class="permission-select-wrapper">
                                        <select id="userSelect" name="viewUserId" class="permission-select" required>
                                            <option value="">-- Chọn người dùng cần phân quyền --</option>
                                            <% if (users != null && !users.isEmpty()) { %>
                                                <% for (Object u : users) {
                                                    String uId = getProperty(u, "id", "userId");
                                                    String uName = getProperty(u, "fullName", "name", "username");
                                                    String uEmail = getProperty(u, "email");
                                                    String uTeam = getProperty(u, "team", "department");
                                                    boolean isSelected = (selectedUserId != null && !selectedUserId.isEmpty() && selectedUserId.equals(uId));
                                                %>
                                                    <option value="<%= escapeHtml(uId) %>"
                                                            data-name="<%= escapeHtml(uName) %>"
                                                            data-email="<%= escapeHtml(uEmail) %>"
                                                            data-team="<%= escapeHtml(uTeam) %>"
                                                            <%= isSelected ? "selected" : "" %>>
                                                        <%= escapeHtml(uName) %> <%= (uEmail.isEmpty() ? "" : "(" + escapeHtml(uEmail) + ")") %>
                                                    </option>
                                                <% } %>
                                            <% } else { %>
                                                <option value="" disabled>-- Chưa có danh sách người dùng từ hệ thống --</option>
                                            <% } %>
                                        </select>
                                        <div class="permission-buttons" style="margin-top: 12px;">
                                            <button type="submit" formaction="${pageContext.request.contextPath}/permissions" formmethod="get" class="permission-btn permission-btn-secondary">Tải phân quyền</button>
                                        </div>
                                    </div>
                                </div>

                                <!-- Thẻ hiển thị tóm tắt thông tin người dùng được chọn -->
                                <div class="permission-user-summary" id="userSummaryCard" style="display: none;">
                                    <div class="permission-user-avatar" id="userAvatarText">U</div>
                                    <div class="permission-user-meta">
                                        <div class="permission-user-name" id="userNameDisplay">Họ và tên người dùng</div>
                                        <div class="permission-user-subdetails">
                                            <span id="userEmailDisplay">email@crm.vn</span>
                                            <span class="permission-user-tag">
                                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="9" cy="7" r="4"></circle>
                                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                </svg>
                                                Phòng ban / Nhóm: <strong id="userTeamDisplay" style="margin-left: 4px;">Chưa phân nhóm</strong>
                                            </span>
                                            <span class="permission-user-tag">
                                                Đang chọn: <strong id="activeRoleCountDisplay" style="margin-left: 4px;">0 vai trò</strong>
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>

                    <!-- BƯỚC 2: Chọn vai trò (Roles) -->
                    <section class="permission-card" id="rolesCardSection">
                        <div class="permission-card-header">
                            <div class="permission-card-title-group">
                                <span class="permission-card-step">2</span>
                                <div>
                                    <h2>Vai trò hệ thống (Roles)</h2>
                                    <div class="permission-card-subtitle">Có thể chọn một hoặc nhiều vai trò chức năng cho tài khoản</div>
                                </div>
                            </div>
                        </div>
                        <div class="permission-card-body">

                            <div class="permission-role-grid" id="rolesGridContainer">
                                <% if (hasRolesFromBackend) { %>
                                    <% for (Object r : roles) {
                                        String rId = getProperty(r, "id", "roleId", "code");
                                        String rName = getProperty(r, "name", "roleName", "title");
                                        String rCode = getProperty(r, "code", "roleCode");
                                        String rDesc = getProperty(r, "description", "desc");
                                        boolean isRoleChecked = assignedRoleIds.contains(rId) || assignedRoleIds.contains(rCode);
                                    %>
                                        <label class="permission-role-item <%= isRoleChecked ? "checked" : "" %>" for="role_<%= escapeHtml(rId) %>">
                                            <input type="checkbox"
                                                   class="permission-role-checkbox"
                                                   name="roleIds"
                                                   id="role_<%= escapeHtml(rId) %>"
                                                   value="<%= escapeHtml(rId) %>"
                                                   <%= isRoleChecked ? "checked" : "" %>>
                                            <div class="permission-role-details">
                                                <div class="permission-role-top">
                                                    <span class="permission-role-title"><%= escapeHtml(rName) %></span>
                                                    <% if (!rCode.isEmpty()) { %>
                                                        <span class="permission-role-code"><%= escapeHtml(rCode) %></span>
                                                    <% } %>
                                                </div>
                                                <p class="permission-role-desc"><%= escapeHtml(rDesc.isEmpty() ? "Vai trò nghiệp vụ hệ thống CRM" : rDesc) %></p>
                                            </div>
                                        </label>
                                    <% } %>
                                <% } else { %>
                                        <div class="permission-alert permission-alert-danger">
                                            <div class="permission-alert-content">
                                                <div class="permission-alert-title">Chưa có vai trò trong CSDL</div>
                                                <div>Hãy thêm dữ liệu vào bảng roles trước khi phân quyền.</div>
                                            </div>
                                        </div>
                                <% } %>
                            </div>
                        </div>
                    </section>

                    <!-- BƯỚC 3: Phạm vi dữ liệu sở hữu (Data Scope) -->
                    <section class="permission-card" id="dataScopeCardSection">
                        <div class="permission-card-header">
                            <div class="permission-card-title-group">
                                <span class="permission-card-step">3</span>
                                <div>
                                    <h2>Phạm vi dữ liệu sở hữu (Data Scope)</h2>
                                    <div class="permission-card-subtitle">Quy định giới hạn bản ghi dữ liệu người dùng được phép xem, sửa hoặc thao tác</div>
                                </div>
                            </div>
                        </div>
                        <div class="permission-card-body">
                            <div class="permission-scope-grid" id="dataScopeGrid">
                                <!-- Option 1: SELF -->
                                <label class="permission-scope-card <%= "SELF".equalsIgnoreCase(currentDataScope) ? "selected" : "" %>" for="scope_self">
                                    <div class="permission-scope-top">
                                        <div class="permission-scope-icon-wrap">
                                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                <circle cx="12" cy="7" r="4"></circle>
                                            </svg>
                                        </div>
                                        <input type="radio"
                                               class="permission-scope-radio"
                                               name="dataScope"
                                               id="scope_self"
                                               value="SELF"
                                               <%= "SELF".equalsIgnoreCase(currentDataScope) ? "checked" : "" %>>
                                    </div>
                                    <div class="permission-scope-name">Cá nhân (SELF)</div>
                                    <div class="permission-scope-desc">
                                        Chỉ xem và thao tác trên dữ liệu (khách hàng, giao dịch, cơ hội) do chính tài khoản tạo ra hoặc được phân công phụ trách trực tiếp.
                                    </div>
                                    <span class="permission-scope-badge">Mức bảo mật hẹp</span>
                                </label>

                                <!-- Option 2: TEAM -->
                                <label class="permission-scope-card <%= "TEAM".equalsIgnoreCase(currentDataScope) ? "selected" : "" %>" for="scope_team">
                                    <div class="permission-scope-top">
                                        <div class="permission-scope-icon-wrap">
                                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                <circle cx="9" cy="7" r="4"></circle>
                                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                            </svg>
                                        </div>
                                        <input type="radio"
                                               class="permission-scope-radio"
                                               name="dataScope"
                                               id="scope_team"
                                               value="TEAM"
                                               <%= "TEAM".equalsIgnoreCase(currentDataScope) ? "checked" : "" %>>
                                    </div>
                                    <div class="permission-scope-name">Nhóm / Phòng ban (TEAM)</div>
                                    <div class="permission-scope-desc">
                                        Được quyền xem và thao tác trên toàn bộ dữ liệu của tất cả các thành viên trực thuộc cùng đội nhóm / phòng ban làm việc.
                                    </div>
                                    <span class="permission-scope-badge">Cộng tác nhóm</span>
                                </label>

                                <!-- Option 3: ALL -->
                                <label class="permission-scope-card <%= "ALL".equalsIgnoreCase(currentDataScope) ? "selected" : "" %>" for="scope_all">
                                    <div class="permission-scope-top">
                                        <div class="permission-scope-icon-wrap">
                                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <circle cx="12" cy="12" r="10"></circle>
                                                <line x1="2" y1="12" x2="22" y2="12"></line>
                                                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
                                            </svg>
                                        </div>
                                        <input type="radio"
                                               class="permission-scope-radio"
                                               name="dataScope"
                                               id="scope_all"
                                               value="ALL"
                                               <%= "ALL".equalsIgnoreCase(currentDataScope) ? "checked" : "" %>>
                                    </div>
                                    <div class="permission-scope-name">Toàn hệ thống (ALL)</div>
                                    <div class="permission-scope-desc">
                                        Toàn quyền truy cập, xem và xử lý toàn bộ dữ liệu khách hàng và cơ hội trên tất cả các phòng ban trong toàn bộ doanh nghiệp.
                                    </div>
                                    <span class="permission-scope-badge">Toàn quyền dữ liệu</span>
                                </label>
                            </div>
                        </div>
                    </section>

                    <!-- THANH HÀNH ĐỘNG (ACTION BAR) -->
                    <footer class="permission-actions-bar">
                        <div class="permission-status-hint">
                            <span class="permission-status-dot" id="permissionStatusDot"></span>
                            <span id="permissionStatusText">Sẵn sàng thiết lập phân quyền</span>
                        </div>
                        <div class="permission-buttons">
                            <button type="reset" class="permission-btn permission-btn-secondary" id="btnResetPermissions">
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="1 4 1 10 7 10"></polyline>
                                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                </svg>
                                Đặt lại
                            </button>
                            <button type="submit" class="permission-btn permission-btn-primary" id="btnSavePermissions">
                                <span class="permission-spinner" id="btnSaveSpinner" style="display: none;"></span>
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" id="btnSaveIcon">
                                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                                    <polyline points="7 3 7 8 15 8"></polyline>
                                </svg>
                                <span id="btnSaveText">Lưu phân quyền</span>
                            </button>
                        </div>
                    </footer>
                </form>

            </div>
        </main>
    </div>

    <!-- Include Footer dùng chung -->
    <jsp:include page="../shared/footer.jsp" />

</body>
</html>
