<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.AuditLog,com.crm.util.Html" %>
<%
    Object rawLogs = request.getAttribute("auditLogs");
    List<?> logs = rawLogs instanceof List<?> ? (List<?>) rawLogs : java.util.List.of();
    Object error = request.getAttribute("auditError");
%>
<!DOCTYPE html>
<html lang="vi"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Nhật ký thay đổi - CRM ICTU</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/audit/audit.css">
</head><body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="audit-page crm-page"><div class="audit-container crm-page-container">
<nav class="audit-breadcrumb crm-breadcrumb" aria-label="Đường dẫn"><a href="${pageContext.request.contextPath}/dashboard">CRM</a> / Nhật ký</nav>
<header class="audit-header crm-page-header"><h1 class="crm-page-title">Nhật ký thay đổi</h1>
<a class="crm-btn crm-btn-secondary" href="${pageContext.request.contextPath}/audit">Làm mới</a></header>
<section class="audit-card crm-card"><div class="crm-card-header"><h2 class="crm-card-title">Lịch sử thay đổi</h2><span class="crm-badge">Chỉ đọc</span></div>
<div class="crm-card-body">
<form class="audit-filter-bar crm-toolbar" method="get" action="${pageContext.request.contextPath}/audit">
<div class="crm-form-group"><label for="actor" class="crm-label">Người thực hiện (ID)</label>
<input id="actor" class="crm-input" name="userId" type="number" min="1" value="<%= Html.escape(request.getParameter("userId")) %>"></div>
<div class="crm-form-group"><label for="action" class="crm-label">Hành động</label>
<input id="action" class="crm-input" name="action" maxlength="50" value="<%= Html.escape(request.getParameter("action")) %>" placeholder="UPDATE, DELETE..."></div>
<div class="crm-form-group"><label for="objectType" class="crm-label">Đối tượng</label>
<input id="objectType" class="crm-input" name="objectType" maxlength="50" value="<%= Html.escape(request.getParameter("objectType")) %>" placeholder="USER, CUSTOMER..."></div>
<div class="crm-form-group"><label for="objectId" class="crm-label">Mã bản ghi</label>
<input id="objectId" class="crm-input" name="objectId" type="number" min="1" value="<%= Html.escape(request.getParameter("objectId")) %>"></div>
<div class="crm-form-group"><label for="from" class="crm-label">Từ ngày</label>
<input id="from" class="crm-input" name="from" type="date" value="<%= Html.escape(request.getParameter("from")) %>"></div>
<div class="crm-form-group"><label for="to" class="crm-label">Đến ngày</label>
<input id="to" class="crm-input" name="to" type="date" value="<%= Html.escape(request.getParameter("to")) %>"></div>
<button class="crm-btn crm-btn-primary" type="submit">Lọc</button>
<a class="crm-btn crm-btn-secondary" href="${pageContext.request.contextPath}/audit">Xóa bộ lọc</a>
</form>
<% if (error != null) { %><p class="crm-alert crm-alert-danger" role="alert"><%= Html.escape(error) %></p><% } %>
<div class="audit-table-responsive crm-table-wrap" role="region" aria-label="Bảng nhật ký" tabindex="0">
<table class="audit-table crm-table"><thead><tr><th>Thời điểm</th><th>Người thực hiện</th><th>Loại đối tượng</th><th>Mã bản ghi</th><th>Hành động</th><th>Trước</th><th>Sau</th></tr></thead>
<tbody>
<% for (Object item : logs) { if (!(item instanceof AuditLog log)) continue; %>
<tr><td><%= Html.escape(log.getCreatedAt()) %></td><td><%= log.getActorUserId() %></td>
<td><%= Html.escape(log.getObjectType()) %></td><td><%= log.getObjectId() %></td>
<td><%= Html.escape(log.getAction()) %></td><td><pre><%= Html.escape(log.getBeforeValue()) %></pre></td>
<td><pre><%= Html.escape(log.getAfterValue()) %></pre></td></tr>
<% } %>
</tbody></table></div>
<% if (logs.isEmpty() && error == null) { %><p>Chưa có nhật ký phù hợp.</p><% } %>

<% 
    int currentPage = request.getAttribute("currentPage") != null ? (Integer) request.getAttribute("currentPage") : 1;
    int totalPages = request.getAttribute("totalPages") != null ? (Integer) request.getAttribute("totalPages") : 1;
    if (totalPages > 0) { 
        String queryString = request.getQueryString() != null ? request.getQueryString() : "";
        queryString = queryString.replaceAll("&?page=\\d+", "");
        if (!queryString.isEmpty()) queryString = "&" + queryString;
%>
<div class="crm-pagination" style="margin-top: 20px; display: flex; gap: 10px; align-items: center;">
    <% if (currentPage > 1) { %>
        <a class="crm-btn crm-btn-secondary" href="?page=<%= currentPage - 1 %><%= Html.escape(queryString) %>">Trang trước</a>
    <% } %>
    <span>Trang <%= currentPage %> / <%= totalPages %></span>
    <% if (currentPage < totalPages) { %>
        <a class="crm-btn crm-btn-secondary" href="?page=<%= currentPage + 1 %><%= Html.escape(queryString) %>">Trang sau</a>
    <% } %>
</div>
<% } %>
</div></section></div></main></div><jsp:include page="/jsp/shared/footer.jsp"/>
</body></html>
