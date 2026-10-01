<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.User, com.crm.util.Html, com.crm.controller.ServerForms" %>
<%
  User person = (User) request.getAttribute("profileUser");
  if (person == null) { response.sendError(500, "Thiếu dữ liệu hồ sơ"); return; }
  String err = (String) request.getAttribute("error");
  String signature = (String) request.getAttribute("signature");
  if (signature == null) signature = person.getSignature();
%>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Hồ sơ cá nhân - CRM</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
<style>main.nojs-profile{padding:2rem;max-width:850px;width:100%;margin:auto}.nojs-profile form{display:grid;gap:1rem;background:white;padding:1.5rem;border-radius:12px}.nojs-profile label{display:grid;gap:.4rem}.nojs-profile input,.nojs-profile textarea{padding:.7rem;max-width:100%;border:1px solid #bbb;border-radius:6px}.nojs-profile button{padding:.7rem 1.3rem;cursor:pointer}.nojs-profile .note{padding:.8rem;background:#f6f6f6}</style>
</head><body class="crm-body"><jsp:include page="/jsp/shared/header.jsp" />
<div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp" />
<main class="nojs-profile"><h1>Hồ sơ cá nhân</h1>
<% if (err != null && !err.isBlank()) { %><p role="alert"><%= Html.escape(err) %></p><% } %>
<% if ("1".equals(request.getParameter("updated"))) { %><p role="status">Cập nhật hồ sơ thành công.</p><% } %>
<p class="note">Email, vai trò và nhóm chỉ do quản trị viên thay đổi.</p>
<form method="post" action="${pageContext.request.contextPath}/profile">
<input type="hidden" name="csrfToken" value="<%= Html.escape(ServerForms.csrf(request)) %>">
<label>Họ và tên <input type="text" name="fullName" value="<%= Html.escape(person.getFullName()) %>" required maxlength="150"></label>
<label>Email công ty <input type="email" value="<%= Html.escape(person.getEmail()) %>" readonly></label>
<label>Số điện thoại <input type="tel" name="phone" value="<%= Html.escape(person.getPhone()) %>" autocomplete="tel"></label>
<label>Chữ ký email <textarea name="signature" rows="5"><%= Html.escape(signature) %></textarea></label>
<div><button type="submit">Lưu thay đổi</button> <a href="${pageContext.request.contextPath}/profile/avatar">Đổi ảnh đại diện</a> · <a href="${pageContext.request.contextPath}/dashboard">Trang chủ</a></div>
</form></main></div></body></html>
