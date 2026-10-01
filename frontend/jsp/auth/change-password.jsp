<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.util.Html" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đổi mật khẩu | CRM</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/change-password.css">
</head><body class="crm-body"><jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="change-password-page crm-page" id="changePasswordApp"><div class="cp-container crm-page-container">
<header class="crm-page-header"><div><h1 class="crm-page-title">Đổi mật khẩu</h1><p class="crm-page-description">Bảo vệ tài khoản và dữ liệu của bạn.</p></div></header>
<% if(request.getAttribute("error") != null) { %><p class="crm-alert crm-alert-danger" role="alert"><%= Html.escape(request.getAttribute("error")) %></p><% } %>
<% if(request.getAttribute("message") != null) { %><p class="crm-alert crm-alert-success" role="status"><%= Html.escape(request.getAttribute("message")) %></p><% } %>
<section class="cp-card crm-card"><form id="changePasswordForm" class="cp-form" method="post" action="${pageContext.request.contextPath}/change-password">
<div class="crm-form-group"><label class="crm-label" for="currentPassword">Mật khẩu hiện tại</label><input class="crm-input" id="currentPassword" name="currentPassword" type="password" autocomplete="current-password" required></div>
<div class="crm-form-group"><label class="crm-label" for="newPassword">Mật khẩu mới</label><input class="crm-input" id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" pattern="(?=.*[A-Za-z])(?=.*[0-9]).{8,72}" required aria-describedby="passwordPolicy"><p id="passwordPolicy" class="crm-field-help">Từ 8 đến 72 ký tự, có chữ cái và chữ số.</p></div>
<div class="crm-form-group"><label class="crm-label" for="confirmPassword">Xác nhận mật khẩu mới</label><input class="crm-input" id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required></div>
<div class="cp-actions"><button class="crm-btn crm-btn-primary" type="submit">Lưu mật khẩu</button><a class="crm-btn crm-btn-secondary" href="${pageContext.request.contextPath}/profile">Hủy</a></div>
</form><p class="crm-field-help">Sau khi đổi mật khẩu, các phiên đăng nhập khác sẽ được thu hồi.</p></section>
</div></main></div><jsp:include page="/jsp/shared/footer.jsp"/></body></html>
