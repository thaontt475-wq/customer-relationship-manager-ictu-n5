<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.service.scope.ScopeRecord,com.crm.util.Html,java.net.URLEncoder,java.nio.charset.StandardCharsets" %>
<%!
private String esc(Object v) {
    if (v == null) return "";
    return v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String url(Object v) {
    return URLEncoder.encode(v == null ? "" : v.toString(), StandardCharsets.UTF_8);
}
%>
<%
String moduleTitle = (String) request.getAttribute("moduleTitle");
if (moduleTitle == null || moduleTitle.isBlank()) {
    moduleTitle = "Khách hàng";
}
String route = request.getContextPath() + request.getServletPath();
String q = request.getParameter("q");
if (q == null) q = "";
ScopeRecord detail = (ScopeRecord) request.getAttribute("record");
List<ScopeRecord> records = (List<ScopeRecord>) request.getAttribute("records");
Integer pageNumObj = (Integer) request.getAttribute("pageNumber");
Integer pageCountObj = (Integer) request.getAttribute("pageCount");
int pageNumber = pageNumObj == null ? 1 : pageNumObj;
int pageCount = pageCountObj == null ? 1 : pageCountObj;
String prefix = request.getContextPath();
String notice = (String) request.getAttribute("notice");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= esc(moduleTitle) %> | CRM System</title>
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/components.css">
    <style>
        .cust-shell { width: 100%; max-width: 1280px; margin: 0 auto; padding: 24px 16px; }
        .cust-card { background: var(--crm-surface, #fff); border: 1px solid var(--crm-border, #e3e8ef); border-radius: 10px; padding: 20px; margin-bottom: 20px; box-shadow: var(--crm-shadow-sm, 0 1px 3px rgba(0,0,0,0.05)); }
        .cust-toolbar { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; justify-content: space-between; margin-bottom: 16px; }
        .cust-filter-form { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; flex: 1; }
        .cust-filter-form input[type="search"], .cust-filter-form input[type="text"] { min-width: 220px; max-width: 380px; flex: 1; }
        .cust-table-wrap { overflow-x: auto; width: 100%; border: 1px solid var(--crm-border, #e3e8ef); border-radius: 8px; }
        .cust-table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }
        .cust-table th { background: var(--crm-surface-subtle, #f1f4f8); color: var(--crm-text-secondary, #58677c); padding: 12px 14px; font-weight: 600; border-bottom: 1px solid var(--crm-border, #e3e8ef); white-space: nowrap; }
        .cust-table td { padding: 12px 14px; border-bottom: 1px solid var(--crm-border, #e3e8ef); vertical-align: middle; }
        .cust-table tbody tr:hover { background: var(--crm-bg, #f6f8fb); }
        .cust-pagination { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; margin-top: 18px; padding-top: 14px; border-top: 1px solid var(--crm-border, #e3e8ef); }
        .cust-empty { text-align: center; padding: 48px 16px; color: var(--crm-muted, #64748b); }
        .cust-empty-icon { font-size: 36px; display: block; margin-bottom: 8px; }
        .cust-btn-group { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
        .cust-notice { padding: 12px 16px; background: #dcfce7; color: #15803d; border: 1px solid #86efac; border-radius: 6px; margin-bottom: 16px; font-size: 14px; }
        @media (max-width: 768px) {
            .cust-shell { padding: 16px 8px; }
            .cust-toolbar { flex-direction: column; align-items: stretch; }
            .cust-filter-form { flex-direction: column; align-items: stretch; }
            .cust-filter-form input[type="search"], .cust-filter-form input[type="text"] { max-width: 100%; }
            .cust-btn-group { width: 100%; }
            .cust-btn-group form, .cust-btn-group a { flex: 1; text-align: center; }
            .cust-pagination { flex-direction: column; align-items: stretch; text-align: center; }
        }
    </style>
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-page cust-shell" role="main">
        <!-- Breadcrumb navigation -->
        <nav class="crm-breadcrumb" aria-label="Breadcrumb">
            <a href="<%= esc(prefix) %>/dashboard">Trang chủ</a>
            <span aria-hidden="true">&rsaquo;</span>
            <span aria-current="page"><%= esc(moduleTitle) %></span>
        </nav>

        <!-- Page Header -->
        <div class="crm-page-header">
            <div>
                <h1 class="crm-page-title"><%= esc(moduleTitle) %></h1>
                <p class="crm-page-description">Quản lý danh sách doanh nghiệp và phân quyền theo phạm vi dữ liệu (Data Scope).</p>
            </div>
            <div class="cust-btn-group">
                <% if ("/customers".equals(request.getServletPath())) { %>
                <a href="<%= esc(prefix) %>/customers?action=create" class="crm-btn crm-btn-primary" style="display: inline-flex; align-items: center; gap: 6px;">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                    Thêm khách hàng
                </a>
                <% } %>
                <form method="get" action="<%= esc(prefix + "/api" + request.getServletPath() + "/export") %>">
                    <input type="hidden" name="q" value="<%= esc(q) %>">
                    <button type="submit" class="crm-btn" title="Xuất dữ liệu Excel">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                        Xuất Excel
                    </button>
                </form>
            </div>
        </div>

        <% if (notice != null && !notice.isBlank()) { %>
        <div class="cust-notice" role="status"><%= esc(notice) %></div>
        <% } %>

        <% if (detail != null) { %>
        <!-- Detail View (When ID is present) -->
        <section class="cust-card" aria-labelledby="custDetailHeading">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
                <h2 id="custDetailHeading" class="crm-card-title">Chi tiết: <%= esc(detail.label()) %></h2>
                <% if ("/customers".equals(request.getServletPath())) { %>
                <div>
                    <a href="<%= esc(prefix) %>/customers?action=edit&amp;id=<%= detail.id() %>" class="crm-btn crm-btn-primary">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>
                        Chỉnh sửa
                    </a>
                </div>
                <% } %>
            </div>
            <div style="margin: 16px 0; display: grid; gap: 8px;">
                <p><strong>Mã định danh:</strong> <%= detail.id() %></p>
                <p><strong>Tên:</strong> <%= esc(detail.label()) %></p>
                <p><strong>Người phụ trách (Owner User ID):</strong> <%= detail.ownerUserId() %></p>
                <% if (detail.ownerTeamId() != null) { %>
                <p><strong>Nhóm kinh doanh (Team ID):</strong> <%= detail.ownerTeamId() %></p>
                <% } %>
            </div>
            <% if ("/quotes".equals(request.getServletPath())) { %>
            <div style="margin: 16px 0; padding: 12px; background: var(--crm-surface-subtle); border-radius: 6px;">
                <p><a href="<%= esc(prefix) %>/quotes/pricing?id=<%= detail.id() %>" class="crm-btn crm-btn-primary">Sản phẩm và phê duyệt chiết khấu</a></p>
                <form method="post" action="<%= esc(prefix) %>/quotes/discount" style="margin-top: 12px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                    <input type="hidden" name="csrfToken" value="<%= esc(com.crm.controller.ServerForms.csrf(request)) %>">
                    <input type="hidden" name="id" value="<%= detail.id() %>">
                    <label>Chiết khấu (%) <input type="number" min="0" max="100" step="0.01" name="discount" required class="crm-input" style="width: 120px; display: inline-block;"></label>
                    <button type="submit" class="crm-btn">Lưu chiết khấu</button>
                </form>
            </div>
            <% } %>
            <div style="margin-top: 16px;">
                <a href="<%= esc(route) %>" class="crm-btn">&larr; Quay lại danh sách</a>
            </div>
        </section>
        <% } else { %>
        <!-- Search & Filter Card -->
        <section class="cust-card" aria-label="Bộ lọc tìm kiếm">
            <div class="cust-toolbar">
                <form method="get" action="<%= esc(route) %>" class="cust-filter-form">
                    <input type="search" name="q" value="<%= esc(q) %>" placeholder="Tìm kiếm theo tên <%= esc(moduleTitle).toLowerCase() %>..." class="crm-input" maxlength="255" aria-label="Từ khóa tìm kiếm">
                    <button type="submit" class="crm-btn crm-btn-primary">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                        Tìm kiếm
                    </button>
                    <% if (!q.isBlank()) { %>
                    <a href="<%= esc(route) %>" class="crm-btn">Xóa tìm kiếm</a>
                    <% } %>
                </form>
            </div>

            <!-- Table of scoped records -->
            <div class="cust-table-wrap">
                <table class="cust-table" aria-label="Danh sách <%= esc(moduleTitle) %>">
                    <thead>
                        <tr>
                            <th style="width: 80px;">STT</th>
                            <th>Tên / Tiêu đề</th>
                            <th style="width: 160px;">Người phụ trách</th>
                            <th style="width: 140px;">Nhóm phụ trách</th>
                            <th style="width: 120px; text-align: center;">Trạng thái</th>
                            <th style="width: 160px; text-align: right;">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                    <%
                    if (records != null && !records.isEmpty()) {
                        int idx = (pageNumber - 1) * 20 + 1;
                        for (ScopeRecord row : records) {
                    %>
                        <tr>
                            <td><%= idx++ %></td>
                            <td>
                                <strong><%= esc(row.label()) %></strong>
                            </td>
                            <td>User #<%= row.ownerUserId() %></td>
                            <td><%= row.ownerTeamId() != null ? "Nhóm #" + row.ownerTeamId() : "<span style='color: var(--crm-muted);'>Chưa phân nhóm</span>" %></td>
                            <td style="text-align: center;">
                                <span class="crm-badge crm-badge-success">Hoạt động</span>
                            </td>
                            <td style="text-align: right; white-space: nowrap;">
                                <a href="<%= esc(route) %>?id=<%= row.id() %>" class="crm-btn" style="padding: 4px 8px; font-size: 12px; min-height: 28px;">Chi tiết</a>
                                <% if ("/customers".equals(request.getServletPath())) { %>
                                <a href="<%= esc(prefix) %>/customers?action=edit&amp;id=<%= row.id() %>" class="crm-btn" style="padding: 4px 8px; font-size: 12px; min-height: 28px; margin-left: 4px;">Sửa</a>
                                <% } %>
                            </td>
                        </tr>
                    <%
                        }
                    } else {
                    %>
                        <tr>
                            <td colspan="6">
                                <div class="cust-empty">
                                    <span class="cust-empty-icon" aria-hidden="true">&#128196;</span>
                                    <p>Không tìm thấy bản ghi nào phù hợp.</p>
                                    <% if (!q.isBlank()) { %>
                                    <p style="font-size: 12px; margin-top: 4px;">Thử tìm kiếm với từ khóa khác hoặc <a href="<%= esc(route) %>">xem tất cả</a>.</p>
                                    <% } %>
                                </div>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>

            <!-- Pagination bar -->
            <% if (pageCount > 1 || (records != null && !records.isEmpty())) { %>
            <nav class="cust-pagination" aria-label="Phân trang danh sách">
                <div>
                    <span>Trang <strong><%= pageNumber %></strong> / <%= pageCount %></span>
                </div>
                <div style="display: flex; gap: 8px; align-items: center;">
                    <% if (pageNumber > 1) { %>
                    <a href="<%= esc(route) %>?q=<%= esc(url(q)) %>&page=<%= pageNumber - 1 %>" class="crm-btn" aria-label="Trang trước">&larr; Trước</a>
                    <% } %>

                    <% if (pageNumber < pageCount) { %>
                    <a href="<%= esc(route) %>?q=<%= esc(url(q)) %>&page=<%= pageNumber + 1 %>" class="crm-btn" aria-label="Trang sau">Sau &rarr;</a>
                    <% } %>

                    <form method="get" action="<%= esc(route) %>" style="display: inline-flex; align-items: center; gap: 6px; margin-left: 12px;">
                        <input type="hidden" name="q" value="<%= esc(q) %>">
                        <label style="font-size: 13px; color: var(--crm-muted);">Tới trang:</label>
                        <input type="number" name="page" min="1" max="<%= pageCount %>" value="<%= pageNumber %>" class="crm-input" style="width: 70px; min-height: 32px; padding: 2px 6px;">
                        <button type="submit" class="crm-btn" style="min-height: 32px; padding: 2px 10px;">Đi</button>
                    </form>
                </div>
            </nav>
            <% } %>
        </section>
        <% } %>
    </main>
</div>
</body>
</html>
