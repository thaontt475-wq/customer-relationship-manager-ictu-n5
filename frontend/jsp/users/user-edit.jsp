<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.User,com.crm.model.Role,com.crm.model.Team" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return String.valueOf(value)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
}
%>
<%
    String error = (String) request.getAttribute("error");
    User userToEdit = (User) request.getAttribute("user");
    List<Role> availableRoles = (List<Role>) request.getAttribute("availableRoles");
    List<Team> availableTeams = (List<Team>) request.getAttribute("availableTeams");
    List<Long> currentRoleIds = (List<Long>) request.getAttribute("currentRoleIds");
    if (currentRoleIds == null) currentRoleIds = java.util.Collections.emptyList();
    String base = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Sửa tài khoản | CRM ICTU</title>
    <link rel="stylesheet" href="<%=base%>/css/shared/common.css">
    <link rel="stylesheet" href="<%=base%>/css/shared/components.css">
    <link rel="stylesheet" href="<%=base%>/css/shared/layout.css">
    <link rel="stylesheet" href="<%=base%>/css/shared/header.css">
    <link rel="stylesheet" href="<%=base%>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%=base%>/css/users/users.css">
    <link rel="stylesheet" href="<%=base%>/css/users/users-admin.css">
</head>
<body class="crm-body users-admin">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
<jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="crm-page user-page">
<div class="crm-page-container user-container">
    <nav class="crm-breadcrumb">
        <a href="<%=base%>/dashboard">CRM</a>
        <span>/</span>
        <a href="<%=base%>/users">Quản lý người dùng</a>
        <span>/</span>
        <strong>Sửa tài khoản</strong>
    </nav>
    <header class="crm-page-header user-header">
        <h1 class="crm-page-title">Sửa tài khoản <%=esc(userToEdit != null ? userToEdit.getUsername() : "")%></h1>
    </header>
    <section class="crm-card user-card">
        <div class="crm-card-body">
            <% if (error != null) { %>
                <div class="crm-alert crm-alert-danger"><%=error%></div>
            <% } %>
            <% if (userToEdit != null) { %>
            <form action="<%=base%>/users/edit" method="post" class="crm-form">
                <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                <input type="hidden" name="id" value="<%=userToEdit.getId()%>">
                <div class="crm-form-group">
                    <label class="crm-label">Họ và tên <span style="color:red">*</span></label>
                    <input type="text" name="fullName" class="crm-input" value="<%=esc(userToEdit.getFullName())%>" required>
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Email <span style="color:red">*</span></label>
                    <input type="email" name="email" class="crm-input" value="<%=esc(userToEdit.getEmail())%>" required>
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Tên đăng nhập</label>
                    <input type="text" name="username" class="crm-input" value="<%=esc(userToEdit.getUsername())%>">
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Số điện thoại</label>
                    <input type="text" name="phone" class="crm-input" value="<%=esc(userToEdit.getPhone())%>">
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Nhóm kinh doanh</label>
                    <select name="teamId" class="crm-select">
                        <option value="">-- Chọn nhóm --</option>
                        <% if (availableTeams != null) {
                            for (Team team : availableTeams) { 
                                boolean selected = userToEdit.getTeamId() != null && userToEdit.getTeamId().equals(team.getId());
                        %>
                                <option value="<%=team.getId()%>" <%=selected ? "selected" : ""%>><%=team.getName()%></option>
                        <% } } %>
                    </select>
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Vai trò</label>
                    <select name="roleIds" class="crm-select" multiple size="5">
                        <% if (availableRoles != null) {
                            for (Role role : availableRoles) { 
                                boolean selected = currentRoleIds.contains(role.getId());
                        %>
                                <option value="<%=role.getId()%>" <%=selected ? "selected" : ""%>><%=role.getName()%></option>
                        <% } } %>
                    </select>
                    <small>Giữ Ctrl (hoặc Cmd) để chọn nhiều vai trò</small>
                </div>
                <div class="crm-form-actions">
                    <a href="<%=base%>/users" class="crm-btn crm-btn-secondary">Hủy bỏ</a>
                    <button type="submit" class="crm-btn crm-btn-primary">Lưu thay đổi</button>
                </div>
            </form>
            <% } else { %>
                <div class="crm-alert crm-alert-danger">Không tìm thấy người dùng.</div>
            <% } %>
        </div>
    </section>
</div>
</main>
</div>
<jsp:include page="/jsp/shared/footer.jsp"/>
</body>
</html>