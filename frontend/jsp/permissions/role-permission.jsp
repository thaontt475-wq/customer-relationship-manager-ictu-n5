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
    List<?> teams = (List<?>) request.getAttribute("teams");
    Object selectedUser = request.getAttribute("selectedUser");
    List<?> userRoles = (List<?>) request.getAttribute("userRoles");
    String currentDataScope = (String) request.getAttribute("dataScope");
    String error = (String) request.getAttribute("error");
    String successMessage = (String) request.getAttribute("successMessage");

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
    <title>Phân quyền & Nhóm kinh doanh - CRM</title>

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

        <!-- Khu vực nội dung chính của màn hình Phân quyền & Nhóm kinh doanh -->
        <main class="permission-page" id="permissionApp">
            <div class="permission-container">

                <!-- Breadcrumb -->
                <nav class="permission-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="#">Hệ thống</a>
                    <span class="separator">/</span>
                    <span class="active">Phân quyền & Nhóm kinh doanh</span>
                </nav>

                <!-- Header màn hình -->
                <header class="permission-header">
                    <div class="permission-header-info">
                        <h1>Phân quyền & Nhóm kinh doanh</h1>
                        <p>Cấu hình người dùng, phân bổ nhóm bán hàng (CRM-29), vai trò hệ thống và phạm vi dữ liệu sở hữu (CRM-25).</p>
                    </div>
                    <div class="permission-header-badges">
                        <span class="permission-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            </svg>
                            CRM-25 & CRM-29
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
                            <button type="button" class="permission-alert-close" onclick="this.parentElement.remove();">&times;</button>
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
                            <button type="button" class="permission-alert-close" onclick="this.parentElement.remove();">&times;</button>
                        </div>
                    <% } %>

                    <!-- Banner thông báo động điều khiển bởi JavaScript -->
                    <div class="permission-alert permission-alert-danger" id="clientErrorAlert" style="display: none;">
                        <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="permission-alert-content">
                            <div class="permission-alert-title">Thông báo lỗi</div>
                            <div id="clientErrorText"></div>
                        </div>
                        <button type="button" class="permission-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>

                    <div class="permission-alert permission-alert-success" id="clientSuccessAlert" style="display: none;">
                        <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="permission-alert-content">
                            <div class="permission-alert-title">Thành công</div>
                            <div id="clientSuccessText"></div>
                        </div>
                        <button type="button" class="permission-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>
                </div>

                <!-- Form phân quyền & nhóm chính -->
                <form id="permissionForm" onsubmit="return false;">

                    <!-- BƯỚC 1: Chọn người dùng -->
                    <section class="permission-card" id="userCardSection">
                        <div class="permission-card-header">
                            <div class="permission-card-title-group">
                                <span class="permission-card-step">1</span>
                                <div>
                                    <h2>Chọn người dùng</h2>
                                    <div class="permission-card-subtitle">Lựa chọn nhân sự cần thiết lập nhóm kinh doanh và quyền hạn</div>
                                </div>
                            </div>
                        </div>
                        <div class="permission-card-body">
                            <div class="permission-user-selector">
                                <div class="permission-form-group">
                                    <label for="userSelect" class="permission-label">Tài khoản người dùng <span style="color: var(--perm-danger);">*</span></label>
                                    <div class="permission-select-wrapper">
                                        <select id="userSelect" name="userId" class="permission-select" required>
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

                    <!-- BƯỚC 2: Gán nhóm kinh doanh (Sales Team) - CRM-29 -->
                    <section class="permission-card" id="teamCardSection">
                        <div class="permission-card-header">
                            <div class="permission-card-title-group">
                                <span class="permission-card-step">2</span>
                                <div>
                                    <h2>Gán nhóm kinh doanh (Sales Team)</h2>
                                    <div class="permission-card-subtitle">Phân bổ nhân sự vào nhóm kinh doanh phụ trách</div>
                                </div>
                            </div>
                        </div>
                        <div class="permission-card-body">
                            <div class="permission-team-layout">
                                <!-- Cảnh báo dành riêng cho vai trò Trưởng nhóm (Team Lead) khi chưa có nhóm -->
                                <div class="permission-lead-warning" id="teamLeadWarning" style="display: none;">
                                    <svg class="permission-lead-warning-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                        <line x1="12" y1="9" x2="12" y2="13"></line>
                                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                                    </svg>
                                    <div class="permission-lead-warning-content">
                                        <div class="permission-lead-warning-title">Yêu cầu ràng buộc: Vai trò Trưởng nhóm (Team Lead)</div>
                                        <div class="permission-lead-warning-desc">
                                            Tài khoản này đang giữ vai trò Quản lý / Trưởng nhóm nhưng hiện tại chưa được gán vào nhóm kinh doanh nào.
                                            Vui lòng chọn và gán nhóm quản lý phụ trách cho nhân sự này.
                                        </div>
                                    </div>
                                </div>

                                <!-- Thẻ hiển thị nhóm hiện tại -->
                                <div class="permission-current-team-card">
                                    <div class="permission-current-team-left">
                                        <div class="permission-current-team-icon">
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                <circle cx="9" cy="7" r="4"></circle>
                                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                            </svg>
                                        </div>
                                        <div class="permission-current-team-info">
                                            <span class="permission-current-team-label">Nhóm kinh doanh hiện tại</span>
                                            <span class="permission-current-team-name" id="currentTeamDisplay">Chưa phân nhóm</span>
                                        </div>
                                    </div>
                                    <div class="permission-status-hint">
                                        <span class="permission-status-dot" id="teamStatusDot"></span>
                                        <span id="teamStatusText">Chưa chọn người dùng</span>
                                    </div>
                                </div>

                                <!-- Form chọn nhóm và nút gán nhóm -->
                                <div class="permission-team-assign-form">
                                    <div class="permission-team-select-group">
                                        <label for="teamSelect" class="permission-label">Chọn nhóm kinh doanh mới <span style="color: var(--perm-danger);">*</span></label>
                                        <div class="permission-select-wrapper" style="max-width: 100%;">
                                            <select id="teamSelect" name="teamId" class="permission-select">
                                                <option value="">-- Chọn nhóm kinh doanh --</option>
                                                <% if (teams != null && !teams.isEmpty()) { %>
                                                    <% for (Object t : teams) {
                                                        String tId = getProperty(t, "id", "teamId");
                                                        String tName = getProperty(t, "name", "teamName", "title");
                                                    %>
                                                        <option value="<%= escapeHtml(tId) %>"><%= escapeHtml(tName) %></option>
                                                    <% } %>
                                                <% } %>
                                            </select>
                                        </div>
                                    </div>
                                    <button type="button" class="permission-btn permission-btn-primary" id="btnAssignTeam">
                                        <span class="permission-spinner" id="btnAssignTeamSpinner" style="display: none;"></span>
                                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" id="btnAssignTeamIcon">
                                            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                            <circle cx="9" cy="7" r="4"></circle>
                                            <polyline points="16 11 18 13 22 9"></polyline>
                                        </svg>
                                        <span id="btnAssignTeamText">Gán nhóm</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </section>

                    <!-- BƯỚC 3: Chọn vai trò (Roles) -->
                    <section class="permission-card" id="rolesCardSection">
                        <div class="permission-card-header">
                            <div class="permission-card-title-group">
                                <span class="permission-card-step">3</span>
                                <div>
                                    <h2>Vai trò hệ thống (Roles)</h2>
                                    <div class="permission-card-subtitle">Có thể chọn một hoặc nhiều vai trò chức năng cho tài khoản</div>
                                </div>
                            </div>
                            <div class="permission-quick-actions">
                                <button type="button" class="permission-btn-subtle" id="btnSelectAllRoles">Chọn tất cả</button>
                                <button type="button" class="permission-btn-subtle" id="btnDeselectAllRoles">Bỏ chọn tất cả</button>
                            </div>
                        </div>
                        <div class="permission-card-body">
                            <!-- Overlay hiển thị khi đang tải dữ liệu từ API -->
                            <div class="permission-loading-overlay" id="rolesLoadingOverlay">
                                <div class="permission-loading-box">
                                    <span class="permission-spinner permission-spinner-dark"></span>
                                    <span>Đang đồng bộ dữ liệu phân quyền...</span>
                                </div>
                            </div>

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
                                    <!-- Danh mục vai trò chuẩn CRM định sẵn của hệ thống khi danh sách roles chưa nạp từ DB -->
                                    <label class="permission-role-item" for="role_1">
                                        <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_1" value="1">
                                        <div class="permission-role-details">
                                            <div class="permission-role-top">
                                                <span class="permission-role-title">Quản trị hệ thống (Admin)</span>
                                                <span class="permission-role-code">ROLE_ADMIN</span>
                                            </div>
                                            <p class="permission-role-desc">Toàn quyền cấu hình hệ thống, quản lý tài khoản người dùng, phân quyền và cài đặt quy trình chung.</p>
                                        </div>
                                    </label>

                                    <label class="permission-role-item" for="role_2">
                                        <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_2" value="2">
                                        <div class="permission-role-details">
                                            <div class="permission-role-top">
                                                <span class="permission-role-title">Quản lý kinh doanh (Sales Manager)</span>
                                                <span class="permission-role-code">ROLE_SALES_MANAGER</span>
                                            </div>
                                            <p class="permission-role-desc">Quản trị đội ngũ bán hàng, phân bổ khách hàng tiềm năng, theo dõi pipeline cơ hội và duyệt báo giá hợp đồng.</p>
                                        </div>
                                    </label>

                                    <label class="permission-role-item" for="role_3">
                                        <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_3" value="3">
                                        <div class="permission-role-details">
                                            <div class="permission-role-top">
                                                <span class="permission-role-title">Nhân viên kinh doanh (Sales Rep)</span>
                                                <span class="permission-role-code">ROLE_SALES_REP</span>
                                            </div>
                                            <p class="permission-role-desc">Trực tiếp tiếp cận khách hàng tiềm năng, tạo lập giao dịch, chăm sóc cơ hội và ghi nhận tương tác bán hàng.</p>
                                        </div>
                                    </label>

                                    <label class="permission-role-item" for="role_4">
                                        <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_4" value="4">
                                        <div class="permission-role-details">
                                            <div class="permission-role-top">
                                                <span class="permission-role-title">Chăm sóc khách hàng (Customer Support)</span>
                                                <span class="permission-role-code">ROLE_SUPPORT</span>
                                            </div>
                                            <p class="permission-role-desc">Tiếp nhận yêu cầu, giải quyết sự vụ khiếu nại, hỗ trợ kỹ thuật và duy trì quan hệ sau bán hàng.</p>
                                        </div>
                                    </label>
                                <% } %>
                            </div>
                        </div>
                    </section>

                    <!-- BƯỚC 4: Phạm vi dữ liệu sở hữu (Data Scope) -->
                    <section class="permission-card" id="dataScopeCardSection">
                        <div class="permission-card-header">
                            <div class="permission-card-title-group">
                                <span class="permission-card-step">4</span>
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
                            <button type="button" class="permission-btn permission-btn-secondary" id="btnResetPermissions">
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

    <!-- Script xử lý tương tác phía Frontend -->
    <script>
        (function() {
            'use strict';

            // Đường dẫn gốc ứng dụng (Context Path)
            const contextPath = '${pageContext.request.contextPath}';

            // Các thành phần giao diện DOM người dùng & phân quyền (CRM-25)
            const userSelect = document.getElementById('userSelect');
            const userSummaryCard = document.getElementById('userSummaryCard');
            const userAvatarText = document.getElementById('userAvatarText');
            const userNameDisplay = document.getElementById('userNameDisplay');
            const userEmailDisplay = document.getElementById('userEmailDisplay');
            const userTeamDisplay = document.getElementById('userTeamDisplay');
            const activeRoleCountDisplay = document.getElementById('activeRoleCountDisplay');
            const rolesGridContainer = document.getElementById('rolesGridContainer');
            const rolesLoadingOverlay = document.getElementById('rolesLoadingOverlay');
            const btnSelectAllRoles = document.getElementById('btnSelectAllRoles');
            const btnDeselectAllRoles = document.getElementById('btnDeselectAllRoles');
            const btnSavePermissions = document.getElementById('btnSavePermissions');
            const btnSaveSpinner = document.getElementById('btnSaveSpinner');
            const btnSaveIcon = document.getElementById('btnSaveIcon');
            const btnSaveText = document.getElementById('btnSaveText');
            const btnResetPermissions = document.getElementById('btnResetPermissions');
            const clientErrorAlert = document.getElementById('clientErrorAlert');
            const clientErrorText = document.getElementById('clientErrorText');
            const clientSuccessAlert = document.getElementById('clientSuccessAlert');
            const clientSuccessText = document.getElementById('clientSuccessText');
            const permissionStatusDot = document.getElementById('permissionStatusDot');
            const permissionStatusText = document.getElementById('permissionStatusText');

            // Các thành phần giao diện DOM nhóm kinh doanh (CRM-29)
            const teamSelect = document.getElementById('teamSelect');
            const currentTeamDisplay = document.getElementById('currentTeamDisplay');
            const teamStatusDot = document.getElementById('teamStatusDot');
            const teamStatusText = document.getElementById('teamStatusText');
            const btnAssignTeam = document.getElementById('btnAssignTeam');
            const btnAssignTeamSpinner = document.getElementById('btnAssignTeamSpinner');
            const btnAssignTeamIcon = document.getElementById('btnAssignTeamIcon');
            const btnAssignTeamText = document.getElementById('btnAssignTeamText');
            const teamLeadWarning = document.getElementById('teamLeadWarning');

            // Bộ nhớ đệm lưu trạng thái ban đầu để khôi phục khi nhấn "Đặt lại"
            let initialUserState = null;
            let currentUserTeamName = '';

            // Xóa thông báo lỗi / thành công trên UI
            function clearAlerts() {
                clientErrorAlert.style.display = 'none';
                clientErrorText.textContent = '';
                clientSuccessAlert.style.display = 'none';
                clientSuccessText.textContent = '';
                const serverErr = document.getElementById('serverErrorAlert');
                if (serverErr) serverErr.style.display = 'none';
                const serverSucc = document.getElementById('serverSuccessAlert');
                if (serverSucc) serverSucc.style.display = 'none';
            }

            // Hiển thị thông báo lỗi
            function showError(message) {
                clearAlerts();
                clientErrorText.textContent = message || 'Đã xảy ra lỗi trong quá trình thực hiện.';
                clientErrorAlert.style.display = 'flex';
                clientErrorAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                setStatus('Lỗi thao tác', false);
            }

            // Hiển thị thông báo thành công
            function showSuccess(message) {
                clearAlerts();
                clientSuccessText.textContent = message || 'Cập nhật thành công!';
                clientSuccessAlert.style.display = 'flex';
                clientSuccessAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                setStatus('Đã đồng bộ thành công', true);
            }

            // Cập nhật trạng thái thanh status bar dưới chân trang
            function setStatus(text, isActive) {
                if (permissionStatusText) permissionStatusText.textContent = text;
                if (permissionStatusDot) {
                    if (isActive) {
                        permissionStatusDot.classList.add('active');
                    } else {
                        permissionStatusDot.classList.remove('active');
                    }
                }
            }

            // Cập nhật số lượng vai trò được chọn
            function updateRoleCountBadge() {
                const checkedBoxes = document.querySelectorAll('input[name="roleIds"]:checked');
                const count = checkedBoxes.length;
                if (activeRoleCountDisplay) {
                    activeRoleCountDisplay.textContent = count + ' vai trò';
                }
            }

            // Cập nhật hiển thị thẻ người dùng được chọn
            function updateUserSummary(name, email, team) {
                if (!name) {
                    userSummaryCard.style.display = 'none';
                    return;
                }
                userSummaryCard.style.display = 'flex';
                userNameDisplay.textContent = name;
                userEmailDisplay.textContent = email || 'Chưa có email';
                userTeamDisplay.textContent = team || 'Chưa phân nhóm';

                // Ký tự đại diện Avatar
                const initials = name.trim().split(' ').map(function(w) { return w.charAt(0); }).join('').toUpperCase();
                userAvatarText.textContent = initials.substring(0, 2) || 'U';

                updateRoleCountBadge();
            }

            // =================================================================
            // XỬ LÝ NHÓM KINH DOANH (CRM-29)
            // =================================================================

            // Kiểm tra xem người dùng đang chọn có đảm nhiệm vai trò Trưởng nhóm (Team Lead) hay không
            function isCurrentSelectedUserTeamLead() {
                const checkedBoxes = document.querySelectorAll('input[name="roleIds"]:checked');
                for (let i = 0; i < checkedBoxes.length; i++) {
                    const cb = checkedBoxes[i];
                    const parent = cb.closest('.permission-role-item');
                    if (!parent) continue;
                    const title = (parent.querySelector('.permission-role-title')?.textContent || '').toUpperCase();
                    const code = (parent.querySelector('.permission-role-code')?.textContent || '').toUpperCase();
                    if (code.includes('MANAGER') || code.includes('LEAD') || code.includes('LEADER') ||
                        title.includes('TRƯỞNG NHÓM') || title.includes('QUẢN LÝ') || title.includes('LEAD')) {
                        return true;
                    }
                }
                return false;
            }

            // Kiểm tra và hiển thị validation rõ ràng khi Team Lead chưa có Team
            function checkTeamLeadValidation() {
                if (!userSelect.value) {
                    if (teamLeadWarning) teamLeadWarning.style.display = 'none';
                    if (teamSelect) teamSelect.classList.remove('warning-border');
                    if (teamStatusText) teamStatusText.textContent = 'Chưa chọn người dùng';
                    if (teamStatusDot) teamStatusDot.classList.remove('active');
                    return;
                }

                const isLead = isCurrentSelectedUserTeamLead();
                const hasAssignedTeam = currentUserTeamName && currentUserTeamName !== 'Chưa phân nhóm' && currentUserTeamName !== '';
                const hasSelectedTeam = teamSelect && teamSelect.value !== '';

                if (isLead && !hasAssignedTeam && !hasSelectedTeam) {
                    if (teamLeadWarning) teamLeadWarning.style.display = 'flex';
                    if (teamSelect) teamSelect.classList.add('warning-border');
                    if (teamStatusText) teamStatusText.textContent = 'Cần gán nhóm cho Team Lead';
                    if (teamStatusDot) teamStatusDot.classList.remove('active');
                } else {
                    if (teamLeadWarning) teamLeadWarning.style.display = 'none';
                    if (teamSelect) teamSelect.classList.remove('warning-border');
                    if (teamStatusText) {
                        teamStatusText.textContent = hasAssignedTeam ? 'Đã phân nhóm: ' + currentUserTeamName : 'Chưa phân nhóm';
                    }
                    if (teamStatusDot) {
                        if (hasAssignedTeam) teamStatusDot.classList.add('active');
                        else teamStatusDot.classList.remove('active');
                    }
                }
            }

            // Gọi API GET /api/teams để tải danh sách nhóm kinh doanh từ hệ thống
            function fetchTeamsList() {
                const endpoint = contextPath + '/api/teams';
                fetch(endpoint, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
                .then(function(res) {
                    if (!res.ok) {
                        throw new Error('Endpoint GET /api/teams trả về mã lỗi HTTP ' + res.status);
                    }
                    return res.json();
                })
                .then(function(json) {
                    const teamsData = (json && json.data !== undefined) ? json.data : json;
                    if (Array.isArray(teamsData) && teamsData.length > 0) {
                        const currentVal = teamSelect.value;
                        teamSelect.innerHTML = '<option value="">-- Chọn nhóm kinh doanh --</option>';
                        teamsData.forEach(function(item) {
                            const opt = document.createElement('option');
                            opt.value = item.id !== undefined ? item.id : item.teamId;
                            opt.textContent = item.name || item.teamName || ('Nhóm #' + opt.value);
                            teamSelect.appendChild(opt);
                        });
                        if (currentVal) {
                            teamSelect.value = currentVal;
                        }
                    }
                })
                .catch(function(err) {
                    // Không hard-code data production; nếu chưa có option nào ngoài mặc định thì báo rõ ràng
                    if (teamSelect.options.length <= 1) {
                        const opt = document.createElement('option');
                        opt.value = '';
                        opt.disabled = true;
                        opt.textContent = '-- Chưa có danh sách nhóm từ hệ thống (GET /api/teams chưa sẵn sàng) --';
                        teamSelect.appendChild(opt);
                    }
                });
            }

            // Gọi API POST /api/users/{userId}/team khi nhấn Gán nhóm
            function assignUserTeam() {
                const userId = userSelect.value;
                if (!userId) {
                    showError('Vui lòng chọn người dùng trước khi thực hiện gán nhóm kinh doanh.');
                    userSelect.focus();
                    return;
                }

                const teamIdVal = teamSelect.value;
                if (!teamIdVal) {
                    showError('Vui lòng chọn nhóm kinh doanh cần gán cho người dùng.');
                    teamSelect.focus();
                    return;
                }

                // Kiểm tra validation bắt buộc khi tài khoản là Team Lead
                if (isCurrentSelectedUserTeamLead() && !teamIdVal) {
                    showError('Validation: Tài khoản giữ vai trò Trưởng nhóm (Team Lead) bắt buộc phải được gán vào một nhóm kinh doanh cụ thể.');
                    teamSelect.focus();
                    return;
                }

                const payload = {
                    teamId: isNaN(teamIdVal) ? teamIdVal : Number(teamIdVal)
                };

                btnAssignTeam.disabled = true;
                btnAssignTeamSpinner.style.display = 'inline-block';
                btnAssignTeamIcon.style.display = 'none';
                btnAssignTeamText.textContent = 'Đang gán nhóm...';
                clearAlerts();
                setStatus('Đang gửi yêu cầu gán nhóm kinh doanh...', false);

                const endpoint = contextPath + '/api/users/' + encodeURIComponent(userId) + '/team';

                fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                })
                .then(function(response) {
                    const contentType = response.headers.get('content-type') || '';
                    if (contentType.indexOf('application/json') !== -1) {
                        return response.json().then(function(json) {
                            return { ok: response.ok, status: response.status, body: json };
                        });
                    } else {
                        return response.text().then(function(text) {
                            return { ok: response.ok, status: response.status, rawText: text };
                        });
                    }
                })
                .then(function(res) {
                    if (!res.ok) {
                        let errMsg = '';
                        if (res.body && res.body.message) {
                            errMsg = res.body.message;
                        } else if (res.status === 404) {
                            errMsg = 'Endpoint POST /api/users/{userId}/team chưa sẵn sàng phía Backend (Mã lỗi HTTP 404 Not Found).';
                        } else if (res.status === 403) {
                            errMsg = 'Không có quyền thực hiện gán nhóm kinh doanh (HTTP 403 Forbidden).';
                        } else if (res.status === 500) {
                            errMsg = 'Máy chủ gặp sự cố nội bộ khi gán nhóm kinh doanh (HTTP 500 Internal Server Error).';
                        } else {
                            errMsg = 'Gán nhóm kinh doanh thất bại (HTTP ' + res.status + ').';
                        }
                        throw new Error(errMsg);
                    }

                    const result = res.body;
                    if (result && result.success === false) {
                        throw new Error(result.message || 'Gán nhóm kinh doanh thất bại từ máy chủ.');
                    }

                    const successMsg = (result && result.message) ? result.message : 'Gán người dùng vào nhóm kinh doanh thành công!';
                    showSuccess(successMsg);

                    // Cập nhật tên nhóm mới hiển thị trên giao diện
                    const selectedOpt = teamSelect.options[teamSelect.selectedIndex];
                    const newTeamName = selectedOpt ? selectedOpt.text : 'Đã phân nhóm';
                    currentUserTeamName = newTeamName;
                    currentTeamDisplay.textContent = newTeamName;
                    userTeamDisplay.textContent = newTeamName;

                    // Cập nhật data-team trên option của userSelect
                    const currentSelectedUserOpt = userSelect.options[userSelect.selectedIndex];
                    if (currentSelectedUserOpt) {
                        currentSelectedUserOpt.setAttribute('data-team', newTeamName);
                    }

                    checkTeamLeadValidation();
                })
                .catch(function(err) {
                    showError(err.message || 'Lỗi không xác định khi gán nhóm kinh doanh.');
                })
                .finally(function() {
                    btnAssignTeam.disabled = false;
                    btnAssignTeamSpinner.style.display = 'none';
                    btnAssignTeamIcon.style.display = 'inline-block';
                    btnAssignTeamText.textContent = 'Gán nhóm';
                });
            }

            // =================================================================
            // XỬ LÝ PHÂN QUYỀN VAI TRÒ & PHẠM VI DỮ LIỆU (CRM-25)
            // =================================================================

            // Lắng nghe thay đổi trạng thái checkbox vai trò để đổi style class
            function bindRoleCheckboxes() {
                const checkboxes = document.querySelectorAll('input[name="roleIds"]');
                checkboxes.forEach(function(cb) {
                    cb.addEventListener('change', function() {
                        const parentLabel = cb.closest('.permission-role-item');
                        if (parentLabel) {
                            if (cb.checked) {
                                parentLabel.classList.add('checked');
                            } else {
                                parentLabel.classList.remove('checked');
                            }
                        }
                        updateRoleCountBadge();
                        checkTeamLeadValidation();
                        setStatus('Có thay đổi chưa lưu', false);
                    });
                });
            }

            // Lắng nghe thay đổi chọn phạm vi dữ liệu Data Scope
            function bindDataScopeRadios() {
                const radios = document.querySelectorAll('input[name="dataScope"]');
                radios.forEach(function(radio) {
                    radio.addEventListener('change', function() {
                        document.querySelectorAll('.permission-scope-card').forEach(function(card) {
                            card.classList.remove('selected');
                        });
                        const parentCard = radio.closest('.permission-scope-card');
                        if (parentCard && radio.checked) {
                            parentCard.classList.add('selected');
                        }
                        setStatus('Có thay đổi chưa lưu', false);
                    });
                });
            }

            // Áp dụng dữ liệu phân quyền lên Form từ API
            function applyUserPermissionsToForm(data) {
                if (!data) return;

                // Chuẩn hóa roles từ backend: roles có thể là mảng ID [1, 2] hoặc mảng Object [{id: 1}, ...]
                const roleList = Array.isArray(data.roles) ? data.roles : [];
                const roleIdSet = new Set();
                roleList.forEach(function(item) {
                    if (typeof item === 'object' && item !== null) {
                        const val = item.id !== undefined ? item.id : (item.roleId !== undefined ? item.roleId : item.code);
                        if (val !== undefined && val !== null) roleIdSet.add(String(val));
                    } else if (item !== undefined && item !== null) {
                        roleIdSet.add(String(item));
                    }
                });

                // Cập nhật các checkbox vai trò
                const checkboxes = document.querySelectorAll('input[name="roleIds"]');
                checkboxes.forEach(function(cb) {
                    const isChecked = roleIdSet.has(String(cb.value));
                    cb.checked = isChecked;
                    const parentLabel = cb.closest('.permission-role-item');
                    if (parentLabel) {
                        if (isChecked) parentLabel.classList.add('checked');
                        else parentLabel.classList.remove('checked');
                    }
                });

                // Cập nhật phạm vi dữ liệu Data Scope (SELF | TEAM | ALL)
                const dataScopeVal = (data.dataScope || 'SELF').toUpperCase();
                const targetRadio = document.querySelector('input[name="dataScope"][value="' + dataScopeVal + '"]');
                if (targetRadio) {
                    targetRadio.checked = true;
                    document.querySelectorAll('.permission-scope-card').forEach(function(card) {
                        card.classList.remove('selected');
                    });
                    const parentCard = targetRadio.closest('.permission-scope-card');
                    if (parentCard) parentCard.classList.add('selected');
                }

                // Cập nhật team nếu API trả về
                if (data.team) {
                    const teamName = typeof data.team === 'object' ? (data.team.name || data.team.teamName || '') : String(data.team);
                    const teamId = typeof data.team === 'object' ? (data.team.id || data.team.teamId || '') : '';
                    if (teamName) {
                        currentUserTeamName = teamName;
                        userTeamDisplay.textContent = teamName;
                        currentTeamDisplay.textContent = teamName;

                        for (let i = 0; i < teamSelect.options.length; i++) {
                            const opt = teamSelect.options[i];
                            if ((teamId && String(opt.value) === String(teamId)) || (opt.text === teamName)) {
                                teamSelect.selectedIndex = i;
                                break;
                            }
                        }
                    }
                }

                updateRoleCountBadge();
                checkTeamLeadValidation();
            }

            // Gọi API GET /api/permissions/users/{userId} khi đổi người dùng
            function loadUserPermissions(userId) {
                if (!userId) {
                    userSummaryCard.style.display = 'none';
                    currentUserTeamName = '';
                    currentTeamDisplay.textContent = 'Chưa phân nhóm';
                    teamSelect.value = '';

                    // Reset form vai trò về mặc định
                    document.querySelectorAll('input[name="roleIds"]').forEach(function(cb) {
                        cb.checked = false;
                        const parent = cb.closest('.permission-role-item');
                        if (parent) parent.classList.remove('checked');
                    });
                    const selfRadio = document.querySelector('input[name="dataScope"][value="SELF"]');
                    if (selfRadio) {
                        selfRadio.checked = true;
                        document.querySelectorAll('.permission-scope-card').forEach(function(card) {
                            card.classList.remove('selected');
                        });
                        const parentCard = selfRadio.closest('.permission-scope-card');
                        if (parentCard) parentCard.classList.add('selected');
                    }
                    setStatus('Chờ chọn người dùng', false);
                    checkTeamLeadValidation();
                    return;
                }

                // Hiển thị thông tin người dùng từ data-attributes của option
                const selectedOption = userSelect.options[userSelect.selectedIndex];
                const userName = selectedOption ? selectedOption.getAttribute('data-name') : '';
                const userEmail = selectedOption ? selectedOption.getAttribute('data-email') : '';
                const userTeam = selectedOption ? selectedOption.getAttribute('data-team') : '';
                currentUserTeamName = userTeam || '';
                currentTeamDisplay.textContent = currentUserTeamName || 'Chưa phân nhóm';
                updateUserSummary(userName, userEmail, userTeam);

                // Khớp chọn nhóm trong teamSelect nếu trùng tên
                if (currentUserTeamName) {
                    for (let i = 0; i < teamSelect.options.length; i++) {
                        if (teamSelect.options[i].text === currentUserTeamName) {
                            teamSelect.selectedIndex = i;
                            break;
                        }
                    }
                } else {
                    teamSelect.value = '';
                }

                // Bật overlay loading
                if (rolesLoadingOverlay) rolesLoadingOverlay.classList.add('active');
                clearAlerts();
                setStatus('Đang tải dữ liệu phân quyền...', false);

                const endpoint = contextPath + '/api/permissions/users/' + encodeURIComponent(userId);

                fetch(endpoint, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
                .then(function(response) {
                    if (!response.ok) {
                        if (response.status === 404) {
                            throw new Error('Endpoint GET /api/permissions/users/{userId} chưa sẵn sàng trên Backend (Mã lỗi HTTP 404 Not Found).');
                        } else if (response.status === 500) {
                            throw new Error('Lỗi máy chủ nội bộ khi lấy phân quyền người dùng (HTTP 500 Internal Server Error).');
                        } else {
                            throw new Error('Không thể tải phân quyền người dùng (Mã lỗi HTTP: ' + response.status + ').');
                        }
                    }
                    return response.json();
                })
                .then(function(resData) {
                    const payload = (resData && resData.data !== undefined) ? resData.data : resData;
                    applyUserPermissionsToForm(payload);
                    initialUserState = JSON.parse(JSON.stringify(payload));
                    setStatus('Đã tải thông tin phân quyền người dùng', true);
                })
                .catch(function(err) {
                    showError(err.message);
                })
                .finally(function() {
                    if (rolesLoadingOverlay) rolesLoadingOverlay.classList.remove('active');
                    checkTeamLeadValidation();
                });
            }

            // Gọi API POST /api/permissions/assign khi nhấn Lưu phân quyền
            function savePermissions() {
                const userId = userSelect.value;
                if (!userId) {
                    showError('Vui lòng chọn người dùng cần phân quyền trước khi lưu.');
                    userSelect.focus();
                    return;
                }

                // Thu thập danh sách roleIds
                const selectedBoxes = document.querySelectorAll('input[name="roleIds"]:checked');
                const roleIds = [];
                selectedBoxes.forEach(function(box) {
                    const val = box.value;
                    roleIds.push(isNaN(val) ? val : Number(val));
                });

                // Thu thập dataScope
                const scopeRadio = document.querySelector('input[name="dataScope"]:checked');
                const dataScope = scopeRadio ? scopeRadio.value : 'SELF';

                // Chuẩn bị Request JSON theo đúng API Contract
                const requestPayload = {
                    userId: isNaN(userId) ? userId : Number(userId),
                    roleIds: roleIds,
                    dataScope: dataScope
                };

                // Trạng thái Loading trên nút bấm
                btnSavePermissions.disabled = true;
                btnSaveSpinner.style.display = 'inline-block';
                btnSaveIcon.style.display = 'none';
                btnSaveText.textContent = 'Đang lưu phân quyền...';
                clearAlerts();
                setStatus('Đang gửi yêu cầu lưu phân quyền...', false);

                const endpoint = contextPath + '/api/permissions/assign';

                fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(requestPayload)
                })
                .then(function(response) {
                    const contentType = response.headers.get('content-type') || '';
                    if (contentType.indexOf('application/json') !== -1) {
                        return response.json().then(function(json) {
                            return { ok: response.ok, status: response.status, body: json };
                        });
                    } else {
                        return response.text().then(function(text) {
                            return { ok: response.ok, status: response.status, rawText: text };
                        });
                    }
                })
                .then(function(res) {
                    if (!res.ok) {
                        let errMsg = '';
                        if (res.body && res.body.message) {
                            errMsg = res.body.message;
                        } else if (res.status === 404) {
                            errMsg = 'Endpoint POST /api/permissions/assign chưa sẵn sàng phía Backend (Mã lỗi HTTP 404 Not Found).';
                        } else if (res.status === 500) {
                            errMsg = 'Máy chủ gặp sự cố nội bộ khi lưu phân quyền (HTTP 500 Internal Server Error).';
                        } else {
                            errMsg = 'Yêu cầu lưu phân quyền thất bại (HTTP ' + res.status + ').';
                        }
                        throw new Error(errMsg);
                    }

                    const result = res.body;
                    if (result && result.success === false) {
                        throw new Error(result.message || 'Lưu phân quyền thất bại từ hệ thống.');
                    }

                    const message = (result && result.message) ? result.message : 'Lưu phân quyền và phạm vi dữ liệu thành công!';
                    showSuccess(message);

                    initialUserState = {
                        roles: roleIds,
                        dataScope: dataScope
                    };
                })
                .catch(function(err) {
                    showError(err.message || 'Lỗi không xác định khi lưu phân quyền.');
                })
                .finally(function() {
                    btnSavePermissions.disabled = false;
                    btnSaveSpinner.style.display = 'none';
                    btnSaveIcon.style.display = 'inline-block';
                    btnSaveText.textContent = 'Lưu phân quyền';
                });
            }

            // Gán các sự kiện
            function initEvents() {
                // Đổi người dùng
                userSelect.addEventListener('change', function() {
                    loadUserPermissions(this.value);
                });

                // Đổi nhóm kinh doanh trong dropdown
                if (teamSelect) {
                    teamSelect.addEventListener('change', function() {
                        checkTeamLeadValidation();
                        setStatus('Có thay đổi nhóm chưa lưu', false);
                    });
                }

                // Nút Gán nhóm kinh doanh (CRM-29)
                if (btnAssignTeam) {
                    btnAssignTeam.addEventListener('click', function(e) {
                        e.preventDefault();
                        assignUserTeam();
                    });
                }

                // Nút chọn tất cả vai trò
                if (btnSelectAllRoles) {
                    btnSelectAllRoles.addEventListener('click', function() {
                        document.querySelectorAll('input[name="roleIds"]').forEach(function(cb) {
                            cb.checked = true;
                            const parent = cb.closest('.permission-role-item');
                            if (parent) parent.classList.add('checked');
                        });
                        updateRoleCountBadge();
                        checkTeamLeadValidation();
                        setStatus('Có thay đổi chưa lưu', false);
                    });
                }

                // Nút bỏ chọn tất cả vai trò
                if (btnDeselectAllRoles) {
                    btnDeselectAllRoles.addEventListener('click', function() {
                        document.querySelectorAll('input[name="roleIds"]').forEach(function(cb) {
                            cb.checked = false;
                            const parent = cb.closest('.permission-role-item');
                            if (parent) parent.classList.remove('checked');
                        });
                        updateRoleCountBadge();
                        checkTeamLeadValidation();
                        setStatus('Có thay đổi chưa lưu', false);
                    });
                }

                // Gửi form lưu phân quyền
                document.getElementById('permissionForm').addEventListener('submit', function(e) {
                    e.preventDefault();
                    savePermissions();
                });

                // Nút Lưu
                btnSavePermissions.addEventListener('click', function(e) {
                    e.preventDefault();
                    savePermissions();
                });

                // Nút Đặt lại
                if (btnResetPermissions) {
                    btnResetPermissions.addEventListener('click', function() {
                        clearAlerts();
                        if (userSelect.value && initialUserState) {
                            applyUserPermissionsToForm(initialUserState);
                            setStatus('Đã khôi phục trạng thái gần nhất', true);
                        } else if (userSelect.value) {
                            loadUserPermissions(userSelect.value);
                        } else {
                            setStatus('Sẵn sàng thiết lập phân quyền', false);
                        }
                    });
                }

                bindRoleCheckboxes();
                bindDataScopeRadios();

                // Tải danh sách nhóm từ GET /api/teams
                fetchTeamsList();

                // Kiểm tra xem đã có người dùng được chọn sẵn từ Server (qua request attribute selectedUser) chưa
                if (userSelect.value) {
                    const opt = userSelect.options[userSelect.selectedIndex];
                    if (opt) {
                        currentUserTeamName = opt.getAttribute('data-team') || '';
                        currentTeamDisplay.textContent = currentUserTeamName || 'Chưa phân nhóm';
                        updateUserSummary(
                            opt.getAttribute('data-name'),
                            opt.getAttribute('data-email'),
                            currentUserTeamName
                        );
                        checkTeamLeadValidation();
                    }
                }
            }

            // Khởi chạy khi DOM sẵn sàng
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initEvents);
            } else {
                initEvents();
            }
        })();
    </script>
</body>
</html>