<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, com.crm.service.customers.CustomerDuplicateService.DuplicateGroup, com.crm.service.customers.CustomerDuplicateService.CustomerDetails, com.crm.util.Html" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phát hiện & Gộp Khách hàng trùng | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customers/customers.css">
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-main-content">
        <div class="crm-dup-page">
            <div class="crm-dup-header">
                <div class="crm-dup-title-group">
                    <div class="crm-breadcrumb">
                        <a href="${pageContext.request.contextPath}/customers">Khách hàng</a>
                        <span>/</span>
                        <span aria-current="page">Khách hàng trùng</span>
                    </div>
                    <h1>Phát hiện &amp; Gộp khách hàng trùng</h1>
                    <p class="crm-dup-subtitle">Tự động đối chiếu thông tin doanh nghiệp (Tên, Mã số thuế) để phát hiện các bản ghi trùng lặp trong phạm vi dữ liệu của bạn.</p>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/customers" class="crm-btn crm-btn-secondary">Quay lại danh sách</a>
                </div>
            </div>

            <div class="crm-dup-toolbar">
                <form class="crm-dup-search-form" method="get" action="${pageContext.request.contextPath}/customers/duplicates">
                    <input type="text" name="q" class="crm-dup-search-input" placeholder="Tìm kiếm theo tên hoặc mã số thuế..." value="<%= Html.escape(request.getAttribute("query")) %>">
                    <button type="submit" class="crm-btn crm-btn-primary">Tìm kiếm</button>
                    <% if (request.getAttribute("query") != null && !String.valueOf(request.getAttribute("query")).isBlank()) { %>
                        <a href="${pageContext.request.contextPath}/customers/duplicates" class="crm-btn crm-btn-secondary">Xóa lọc</a>
                    <% } %>
                </form>
            </div>

            <%
                List<DuplicateGroup> groups = (List<DuplicateGroup>) request.getAttribute("duplicateGroups");
                if (groups == null || groups.isEmpty()) {
            %>
                <div class="crm-empty-duplicates">
                    <div class="crm-empty-duplicates-icon" aria-hidden="true">&#10003;</div>
                    <div class="crm-empty-duplicates-title">Không phát hiện khách hàng trùng lặp</div>
                    <div class="crm-empty-duplicates-desc">Hệ thống không tìm thấy bản ghi khách hàng nào có cùng tên doanh nghiệp hoặc mã số thuế trong phạm vi quyền hạn của bạn.</div>
                    <a href="${pageContext.request.contextPath}/customers" class="crm-btn crm-btn-primary">Xem danh sách khách hàng</a>
                </div>
            <% } else { %>
                <% for (DuplicateGroup group : groups) { %>
                    <div class="crm-dup-group">
                        <div class="crm-dup-group-header">
                            <div>
                                <span class="crm-dup-group-tag">Phát hiện theo: <%= Html.escape(group.matchCriteria()) %></span>
                                <span class="crm-dup-group-value"><%= Html.escape(group.matchValue()) %></span>
                            </div>
                            <div class="crm-dup-meta">
                                Phát hiện <strong><%= group.records().size() %></strong> bản ghi trùng
                            </div>
                        </div>
                        <div class="crm-dup-table-wrap">
                            <table class="crm-dup-table">
                                <thead>
                                <tr>
                                    <th>Mã KH</th>
                                    <th>Tên Khách hàng</th>
                                    <th>Người phụ trách</th>
                                    <th>Phòng ban</th>
                                    <th>Ngành nghề</th>
                                    <th>Liên kết liên quan</th>
                                    <th>Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                <%
                                    List<CustomerDetails> records = group.records();
                                    for (int i = 0; i < records.size(); i++) {
                                        CustomerDetails r = records.get(i);
                                %>
                                <tr>
                                    <td><strong>#<%= r.id() %></strong></td>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/customers?id=<%= r.id() %>" class="crm-dup-record-name">
                                            <%= Html.escape(r.name()) %>
                                        </a>
                                        <%
                                            String tax = r.customFields().get("tax_code");
                                            if (tax != null && !tax.isBlank()) {
                                        %>
                                            <div class="crm-dup-meta">MST: <%= Html.escape(tax) %></div>
                                        <% } %>
                                    </td>
                                    <td><%= Html.escape(r.ownerName() != null ? r.ownerName() : ("User #" + r.ownerUserId())) %></td>
                                    <td><%= Html.escape(r.ownerTeamName() != null ? r.ownerTeamName() : "-") %></td>
                                    <td><%= Html.escape(r.industryName() != null ? r.industryName() : "-") %></td>
                                    <td>
                                        <span class="crm-dup-badge"><%= r.opportunityCount() %> Cơ hội</span>
                                        <span class="crm-dup-badge"><%= r.activityCount() %> Hoạt động</span>
                                    </td>
                                    <td>
                                        <% if (records.size() >= 2) {
                                            long otherId = (i == 0) ? records.get(1).id() : records.get(0).id();
                                        %>
                                            <a href="${pageContext.request.contextPath}/customers/duplicates/compare?id1=<%= r.id() %>&id2=<%= otherId %>" class="crm-btn crm-btn-primary" style="font-size: 0.75rem; padding: 4px 10px; min-height: 32px;">
                                                So sánh &amp; Gộp
                                            </a>
                                        <% } %>
                                    </td>
                                </tr>
                                <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                <% } %>
            <% } %>
        </div>
    </main>
</div>
</body>
</html>
