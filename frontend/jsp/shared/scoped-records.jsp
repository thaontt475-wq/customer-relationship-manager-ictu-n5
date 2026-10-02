<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.service.scope.ScopeRecord,com.crm.util.Html" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title><%= Html.escape(request.getAttribute("moduleTitle")) %> | CRM</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
</head><body class="crm-body"><jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/><main class="crm-main-content">
<h1><%= Html.escape(request.getAttribute("moduleTitle")) %></h1>
<% String route = request.getContextPath() + request.getServletPath();
ScopeRecord detail = (ScopeRecord) request.getAttribute("record");
if (detail != null) { %>
<p><%= Html.escape(detail.label()) %></p><p>Chủ sở hữu: <%= detail.ownerUserId() %></p>
<% if("/quotes".equals(request.getServletPath())) { %>
<form method="post" action="${pageContext.request.contextPath}/quotes/discount"><input type="hidden" name="csrfToken" value="<%=com.crm.controller.ServerForms.csrf(request)%>"><input type="hidden" name="id" value="<%=detail.id()%>"><label>Chiết khấu (%) <input type="number" min="0" max="100" step="0.01" name="discount" required></label><button type="submit">Lưu chiết khấu</button></form>
<% } %>
<a href="<%= Html.escape(route) %>">Quay lại danh sách</a>
<% } else { %>
<form method="get" action="<%= Html.escape(route) %>"><label>Tìm kiếm <input name="q" value="<%= Html.escape(request.getParameter("q")) %>"></label><button type="submit">Tìm kiếm</button></form>
<form method="get" action="<%= Html.escape(request.getContextPath() + "/api" + request.getServletPath() + "/export") %>"><input type="hidden" name="q" value="<%= Html.escape(request.getParameter("q")) %>"><button type="submit">Xuất Excel</button></form>
<ul><% for (ScopeRecord row : (List<ScopeRecord>)request.getAttribute("records")) { %>
<li><a href="<%= Html.escape(route) %>?id=<%= row.id() %>"><%= Html.escape(row.label()) %></a></li>
<% } %></ul>
<form method="get" action="<%= Html.escape(route) %>"><input type="hidden" name="q" value="<%= Html.escape(request.getParameter("q")) %>"><label>Trang <input type="number" name="page" min="1" max="<%= request.getAttribute("pageCount") %>" value="<%= request.getAttribute("pageNumber") %>"></label> / <%= request.getAttribute("pageCount") %> <button type="submit">Chuyển trang</button></form>
<% } %></main></div></body></html>
