<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.User" %>

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
List<User> users = (List<User>) request.getAttribute("users");
if (users == null) users = java.util.Collections.emptyList();

String keyword = request.getAttribute("q") == null
    ? "" : String.valueOf(request.getAttribute("q"));

String status = request.getAttribute("status") == null
    ? "" : String.valueOf(request.getAttribute("status"));

String role = request.getAttribute("role") == null
    ? "" : String.valueOf(request.getAttribute("role"));

int pageNumber = request.getAttribute("page") instanceof Number
    ? ((Number) request.getAttribute("page")).intValue() : 1;

int totalPages = request.getAttribute("totalPages") instanceof Number
    ? ((Number) request.getAttribute("totalPages")).intValue() : 0;

int totalItems = request.getAttribute("totalItems") instanceof Number
    ? ((Number) request.getAttribute("totalItems")).intValue() : 0;

String base = request.getContextPath();
String filterQuery = "&q=" + java.net.URLEncoder.encode(keyword, "UTF-8")
    + "&status=" + java.net.URLEncoder.encode(status, "UTF-8")
    + "&role=" + java.net.URLEncoder.encode(role, "UTF-8");
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width,initial-scale=1">

    <title>Quản lý người dùng | CRM ICTU</title>

    <link rel="stylesheet"
          href="<%=base%>/css/shared/common.css">

    <link rel="stylesheet"
          href="<%=base%>/css/shared/components.css">

    <link rel="stylesheet"
          href="<%=base%>/css/shared/layout.css">

    <link rel="stylesheet"
          href="<%=base%>/css/shared/header.css">

    <link rel="stylesheet"
          href="<%=base%>/css/shared/sidebar.css">

    <link rel="stylesheet"
          href="<%=base%>/css/users/users.css">

    <link rel="stylesheet"
          href="<%=base%>/css/users/users-admin.css">
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
        <span>Hệ thống</span>
        <span>/</span>
        <strong>Quản lý người dùng</strong>
    </nav>

    <header class="crm-page-header user-header">
        <h1 class="crm-page-title">
            Quản lý người dùng
        </h1>

        <p class="crm-page-description">
            Danh sách tài khoản, vai trò,
            nhóm kinh doanh và trạng thái.
        </p>
    </header>

    <section class="crm-card user-card">

        <div class="crm-card-header user-card-header">
            <div>
                <h2 class="crm-card-title">
                    Danh sách tài khoản
                </h2>

                <p class="user-card-subtitle">
                    Tổng số:
                    <strong><%=totalItems%></strong>
                    tài khoản
                </p>
            </div>
        </div>

        <div class="crm-card-body">

            <form method="get"
                  action="<%=base%>/users"
                  class="crm-toolbar user-toolbar">

                <input type="search"
                       name="q"
                       class="crm-input"
                       placeholder="Tìm theo tên hoặc email"
                       value="<%=esc(keyword)%>">

                <select name="status"
                        class="crm-select">

                    <option value="">
                        Tất cả trạng thái
                    </option>

                    <option value="ACTIVE"
                        <%= "ACTIVE".equals(status)
                        ? "selected" : "" %>>
                        Đang hoạt động
                    </option>

                    <option value="INACTIVE"
                        <%= "INACTIVE".equals(status)
                        ? "selected" : "" %>>
                        Ngừng hoạt động
                    </option>

                    <option value="LOCKED"
                        <%= "LOCKED".equals(status)
                        ? "selected" : "" %>>
                        Bị khóa
                    </option>
                </select>

                <select name="role"
                        class="crm-select">

                    <option value="">
                        Tất cả vai trò
                    </option>

                    <option value="Admin"
                        <%= "Admin".equals(role)
                        ? "selected" : "" %>>
                        Quản trị viên
                    </option>

                    <option value="Sales Rep"
                        <%= "Sales Rep".equals(role)
                        ? "selected" : "" %>>
                        Nhân viên kinh doanh
                    </option>
                </select>

                <button type="submit"
                        class="crm-btn crm-btn-primary">
                    Tìm kiếm
                </button>

                <a href="<%=base%>/users"
                   class="crm-btn crm-btn-secondary">
                    Đặt lại
                </a>

                <a href="<%=base%>/users/import"
                   class="crm-btn crm-btn-secondary">
                    Nhập từ Excel
                </a>
            </form>

            <div class="crm-table-wrap">
                <table class="crm-table">

                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Người dùng</th>
                            <th>Vai trò</th>
                            <th>Nhóm kinh doanh</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>

                    <tbody>
                    <% if (users.isEmpty()) { %>

                        <tr>
                            <td colspan="6">
                                <div class="crm-state">
                                    <h3>
                                        Không tìm thấy người dùng
                                    </h3>
                                    <p>
                                        Thử thay đổi bộ lọc
                                        hoặc từ khóa tìm kiếm.
                                    </p>
                                </div>
                            </td>
                        </tr>

                    <% } else {
                        for (User user : users) {
                    %>

                        <tr>
                            <td>
                                <%=user.getId()%>
                            </td>

                            <td>
                                <strong>
                                    <%=esc(user.getFullName())%>
                                </strong>
                                <br>
                                <small>
                                    <%=esc(user.getEmail())%>
                                </small>
                            </td>

                            <td>
                                <%=esc(user.getRole())%>
                            </td>

                            <td>
                                <%=esc(user.getTeamName())%>
                            </td>

                            <td>
                                <span class="crm-badge">
                                    <%=esc(user.getStatus())%>
                                </span>
                            </td>

                            <td>
                                <a class="crm-btn crm-btn-secondary"
                                   href="<%=base%>/users/detail?id=<%=user.getId()%>">
                                    Chi tiết
                                </a>
                            </td>
                        </tr>

                    <% }
                    } %>
                    </tbody>
                </table>
            </div>

            <nav class="crm-toolbar"
                 aria-label="Phân trang">

                <% if (pageNumber > 1) { %>
                    <a class="crm-btn crm-btn-secondary"
                       href="<%=base%>/users?page=<%=pageNumber-1%><%=esc(filterQuery)%>">
                        Trang trước
                    </a>
                <% } %>

                <span>
                    Trang <%=pageNumber%> /
                    <%=Math.max(totalPages,1)%>
                </span>

                <% if (pageNumber < totalPages) { %>
                    <a class="crm-btn crm-btn-secondary"
                       href="<%=base%>/users?page=<%=pageNumber+1%><%=esc(filterQuery)%>">
                        Trang sau
                    </a>
                <% } %>

            </nav>

        </div>
    </section>

</div>
</main>
</div>

<jsp:include page="/jsp/shared/footer.jsp"/>

</body>
</html>
