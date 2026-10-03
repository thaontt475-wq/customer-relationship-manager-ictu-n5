<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map,com.crm.service.scope.ScopeRecord,com.crm.model.CustomField,com.crm.model.Category,com.crm.controller.ServerForms" %>
<%!
private String esc(Object v) {
    if (v == null) return "";
    return v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
%>
<%
ScopeRecord record = (ScopeRecord) request.getAttribute("record");
boolean isEdit = (record != null);
String title = isEdit ? "Chỉnh sửa khách hàng" : "Thêm mới khách hàng";
String prefix = request.getContextPath();
String error = (String) request.getAttribute("error");
String success = (String) request.getAttribute("success");
List<Category> industries = (List<Category>) request.getAttribute("industries");
List<Category> companySizes = (List<Category>) request.getAttribute("companySizes");
String nameVal = record != null ? record.label() : (request.getParameter("name") != null ? request.getParameter("name") : "");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= esc(title) %> | CRM System</title>
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/components.css">
    <style>
        .cust-form-shell { width: 100%; max-width: 900px; margin: 0 auto; padding: 24px 16px; }
        .cust-card { background: var(--crm-surface, #fff); border: 1px solid var(--crm-border, #e3e8ef); border-radius: 10px; padding: 24px; margin-bottom: 20px; box-shadow: var(--crm-shadow-sm, 0 1px 3px rgba(0,0,0,0.05)); }
        .cust-form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; margin-bottom: 20px; }
        .cust-form-group { display: flex; flex-direction: column; gap: 6px; }
        .cust-form-group label { font-weight: 500; font-size: 13px; color: var(--crm-text-secondary, #334155); }
        .cust-alert { padding: 12px 16px; border-radius: 6px; margin-bottom: 18px; font-size: 14px; }
        .cust-alert-danger { background: #fee2e2; color: #b91c1c; border: 1px solid #f87171; }
        .cust-alert-success { background: #dcfce7; color: #15803d; border: 1px solid #86efac; }
        .cust-actions { display: flex; gap: 12px; align-items: center; justify-content: flex-end; margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--crm-border, #e3e8ef); }
        @media (max-width: 768px) {
            .cust-form-shell { padding: 16px 8px; }
            .cust-card { padding: 16px; }
            .cust-actions { flex-direction: column-reverse; align-items: stretch; }
            .cust-actions button, .cust-actions a { width: 100%; text-align: center; }
        }
    </style>
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-page cust-form-shell" role="main">
        <!-- Breadcrumb navigation -->
        <nav class="crm-breadcrumb" aria-label="Breadcrumb" style="margin-bottom: 16px;">
            <a href="<%= esc(prefix) %>/dashboard">Trang chủ</a>
            <span aria-hidden="true">&rsaquo;</span>
            <a href="<%= esc(prefix) %>/customers">Khách hàng</a>
            <span aria-hidden="true">&rsaquo;</span>
            <span aria-current="page"><%= esc(title) %></span>
        </nav>

        <div class="crm-page-header" style="margin-bottom: 20px;">
            <h1 class="crm-page-title"><%= esc(title) %></h1>
            <p class="crm-page-description">Vui lòng điền đầy đủ các thông tin doanh nghiệp và trường dữ liệu tùy chỉnh mở rộng bên dưới.</p>
        </div>

        <% if (error != null && !error.isBlank()) { %>
            <div class="cust-alert cust-alert-danger" role="alert"><%= esc(error) %></div>
        <% } %>
        <% if (success != null && !success.isBlank()) { %>
            <div class="cust-alert cust-alert-success" role="status"><%= esc(success) %></div>
        <% } %>

        <form method="post" action="<%= esc(prefix) %>/customers">
            <input type="hidden" name="csrfToken" value="<%= esc(ServerForms.csrf(request)) %>">
            <input type="hidden" name="action" value="<%= isEdit ? "update" : "create" %>">
            <% if (isEdit) { %>
                <input type="hidden" name="id" value="<%= record.id() %>">
            <% } %>

            <div class="cust-card">
                <h2 class="crm-card-title" style="margin-bottom: 16px; font-size: 16px; font-weight: 600;">Thông tin cơ bản</h2>
                <div class="cust-form-grid">
                    <div class="cust-form-group" style="grid-column: 1 / -1;">
                        <label for="customerName">Tên doanh nghiệp / Khách hàng <span style="color: var(--crm-danger, #ef4444);">*</span></label>
                        <input type="text" id="customerName" name="name" class="crm-input" required maxlength="255" value="<%= esc(nameVal) %>" placeholder="Ví dụ: Công ty TNHH Giải Pháp Công Nghệ...">
                    </div>

                    <div class="cust-form-group">
                        <label for="industryId">Ngành nghề kinh doanh</label>
                        <select id="industryId" name="industryId" class="crm-select">
                            <option value="">-- Chọn ngành nghề --</option>
                            <% if (industries != null) {
                                for (Category cat : industries) { %>
                                <option value="<%= cat.getId() %>"><%= esc(cat.getName()) %></option>
                            <%  }
                            } %>
                        </select>
                    </div>

                    <div class="cust-form-group">
                        <label for="companySizeId">Quy mô doanh nghiệp</label>
                        <select id="companySizeId" name="companySizeId" class="crm-select">
                            <option value="">-- Chọn quy mô --</option>
                            <% if (companySizes != null) {
                                for (Category size : companySizes) { %>
                                <option value="<%= size.getId() %>"><%= esc(size.getName()) %></option>
                            <%  }
                            } %>
                        </select>
                    </div>
                </div>
            </div>

            <!-- Custom Field Section Integration -->
            <div class="cust-card">
                <jsp:include page="/jsp/customfields/custom-field-section.jsp"/>
            </div>

            <div class="cust-actions">
                <a href="<%= esc(prefix) %>/customers" class="crm-btn">Hủy bỏ</a>
                <button type="submit" class="crm-btn crm-btn-primary"><%= isEdit ? "Lưu thay đổi" : "Tạo khách hàng" %></button>
            </div>
        </form>
    </main>
</div>
</body>
</html>
