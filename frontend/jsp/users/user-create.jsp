<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.User,com.crm.model.Role,com.crm.model.Team" %>
<%
    String error = (String) request.getAttribute("error");
    List<Role> availableRoles = (List<Role>) request.getAttribute("availableRoles");
    List<Team> availableTeams = (List<Team>) request.getAttribute("availableTeams");
    String base = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Thêm tài khoản | CRM ICTU</title>
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
        <strong>Thêm tài khoản mới</strong>
    </nav>
    <header class="crm-page-header user-header">
        <h1 class="crm-page-title">Thêm tài khoản mới</h1>
    </header>
    <section class="crm-card user-card">
        <div class="crm-card-body">
            <% if (error != null) { %>
                <div class="crm-alert crm-alert-danger"><%=error%></div>
            <% } %>
            <form action="<%=base%>/users/create" method="post" class="crm-form">
                <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                <div class="crm-form-group">
                    <label class="crm-label">Họ và tên <span style="color:red">*</span></label>
                    <input type="text" name="fullName" class="crm-input" required>
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Email <span style="color:red">*</span></label>
                    <input type="email" name="email" class="crm-input" required>
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Tên đăng nhập (Để trống sẽ tự tạo từ email)</label>
                    <input type="text" name="username" class="crm-input">
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Số điện thoại</label>
                    <input type="text" name="phone" class="crm-input">
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Nhóm kinh doanh</label>
                    <select name="teamId" class="crm-select">
                        <option value="">-- Chọn nhóm --</option>
                        <% if (availableTeams != null) {
                            for (Team team : availableTeams) { %>
                                <option value="<%=team.getId()%>"><%=team.getName()%></option>
                        <% } } %>
                    </select>
                </div>
                <div class="crm-form-group">
                    <label class="crm-label">Vai trò</label>
                    <select name="roleIds" class="crm-select" multiple size="5">
                        <% if (availableRoles != null) {
                            for (Role role : availableRoles) { %>
                                <option value="<%=role.getId()%>"><%=role.getName()%></option>
                        <% } } %>
                    </select>
                    <small>Giữ Ctrl (hoặc Cmd) để chọn nhiều vai trò</small>
                </div>
                <div class="crm-form-actions">
                    <a href="<%=base%>/users" class="crm-btn crm-btn-secondary">Hủy bỏ</a>
                    <button type="submit" class="crm-btn crm-btn-primary">Tạo tài khoản</button>
                </div>
            </form>
        </div>
    </section>
</div>
</main>
</div>
<jsp:include page="/jsp/shared/footer.jsp"/>
</body>
</html>