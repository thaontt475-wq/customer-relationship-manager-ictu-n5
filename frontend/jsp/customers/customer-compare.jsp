<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, com.crm.service.customers.CustomerDuplicateService.CustomerDetails, com.crm.model.CustomField, com.crm.util.Html, com.crm.controller.ServerForms" %>
<%
    CustomerDetails c1 = (CustomerDetails) request.getAttribute("c1");
    CustomerDetails c2 = (CustomerDetails) request.getAttribute("c2");
    List<CustomField> customFields = (List<CustomField>) request.getAttribute("customFields");
    String error = (String) request.getAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>So sánh & Gộp Khách hàng | CRM</title>
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
        <div class="crm-compare-container">
            <div class="crm-dup-header">
                <div class="crm-dup-title-group">
                    <div class="crm-breadcrumb">
                        <a href="${pageContext.request.contextPath}/customers">Khách hàng</a>
                        <span>/</span>
                        <a href="${pageContext.request.contextPath}/customers/duplicates">Trùng lặp</a>
                        <span>/</span>
                        <span aria-current="page">So sánh &amp; Gộp</span>
                    </div>
                    <h1>So sánh &amp; Gộp Khách hàng</h1>
                    <p class="crm-dup-subtitle">Chọn một khách hàng làm <strong>Bản ghi chính (Master)</strong>. Toàn bộ cơ hội, báo giá, hoạt động và các trường dữ liệu được chọn sẽ được chuyển sang bản ghi chính. Bản ghi phụ sẽ được xóa an toàn.</p>
                </div>
            </div>

            <% if (error != null && !error.isBlank()) { %>
                <div class="crm-alert crm-alert-danger" style="margin-bottom: var(--crm-space-4);">
                    <%= Html.escape(error) %>
                </div>
            <% } %>

            <div class="crm-compare-alert">
                <div class="crm-compare-alert-icon">&#9888;</div>
                <div>
                    <strong>Cảnh báo thao tác gộp:</strong> Thao tác gộp là không thể hoàn tác trực tiếp. Vui lòng kiểm tra kỹ bản ghi chính và các thông tin muốn giữ lại trước khi xác nhận.
                </div>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/customers/duplicates/merge">
                <input type="hidden" name="csrfToken" value="<%= ServerForms.csrf(request) %>">

                <div class="crm-compare-grid">
                    <div class="crm-compare-header-col">
                        <strong>Chọn bản ghi chính</strong>
                        <p class="crm-dup-meta" style="margin: 0;">Bản ghi sẽ tồn tại sau khi gộp</p>
                    </div>

                    <div class="crm-compare-card is-master" id="cardC1">
                        <label class="crm-compare-radio-label">
                            <input type="radio" name="masterId" value="<%= c1.id() %>" checked>
                            <div>
                                <span class="crm-compare-master-badge">Bản ghi A</span>
                                <h3 class="crm-compare-record-title">#<%= c1.id() %> - <%= Html.escape(c1.name()) %></h3>
                                <div class="crm-dup-meta">Tạo lúc: <%= c1.createdAt() != null ? c1.createdAt().toString().substring(0, 10) : "-" %></div>
                            </div>
                        </label>
                    </div>

                    <div class="crm-compare-card" id="cardC2">
                        <label class="crm-compare-radio-label">
                            <input type="radio" name="masterId" value="<%= c2.id() %>">
                            <div>
                                <span class="crm-compare-master-badge" style="background: var(--crm-muted);">Bản ghi B</span>
                                <h3 class="crm-compare-record-title">#<%= c2.id() %> - <%= Html.escape(c2.name()) %></h3>
                                <div class="crm-dup-meta">Tạo lúc: <%= c2.createdAt() != null ? c2.createdAt().toString().substring(0, 10) : "-" %></div>
                            </div>
                        </label>
                    </div>
                </div>

                <!-- Hidden field for duplicate id matching logic -->
                <input type="hidden" name="duplicateId" value="<%= c2.id() %>">

                <table class="crm-compare-table">
                    <thead>
                    <tr>
                        <th class="crm-compare-field-label">Trường dữ liệu</th>
                        <th class="crm-compare-cell">Giá trị bản ghi A (#<%= c1.id() %>)</th>
                        <th class="crm-compare-cell">Giá trị bản ghi B (#<%= c2.id() %>)</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td class="crm-compare-field-label">Tên khách hàng</td>
                        <td class="crm-compare-cell"><strong><%= Html.escape(c1.name()) %></strong></td>
                        <td class="crm-compare-cell"><strong><%= Html.escape(c2.name()) %></strong></td>
                    </tr>
                    <tr>
                        <td class="crm-compare-field-label">Người phụ trách</td>
                        <td class="crm-compare-cell"><%= Html.escape(c1.ownerName() != null ? c1.ownerName() : ("User #" + c1.ownerUserId())) %></td>
                        <td class="crm-compare-cell"><%= Html.escape(c2.ownerName() != null ? c2.ownerName() : ("User #" + c2.ownerUserId())) %></td>
                    </tr>
                    <tr>
                        <td class="crm-compare-field-label">Phòng ban / Nhóm</td>
                        <td class="crm-compare-cell"><%= Html.escape(c1.ownerTeamName() != null ? c1.ownerTeamName() : "-") %></td>
                        <td class="crm-compare-cell"><%= Html.escape(c2.ownerTeamName() != null ? c2.ownerTeamName() : "-") %></td>
                    </tr>
                    <tr>
                        <td class="crm-compare-field-label">Ngành nghề</td>
                        <td class="crm-compare-cell"><%= Html.escape(c1.industryName() != null ? c1.industryName() : "-") %></td>
                        <td class="crm-compare-cell"><%= Html.escape(c2.industryName() != null ? c2.industryName() : "-") %></td>
                    </tr>
                    <tr>
                        <td class="crm-compare-field-label">Quy mô doanh nghiệp</td>
                        <td class="crm-compare-cell"><%= Html.escape(c1.companySizeName() != null ? c1.companySizeName() : "-") %></td>
                        <td class="crm-compare-cell"><%= Html.escape(c2.companySizeName() != null ? c2.companySizeName() : "-") %></td>
                    </tr>

                    <% if (customFields != null) {
                        for (CustomField field : customFields) {
                            String fName = field.getFieldName();
                            String v1 = c1.customFields().get(fName);
                            String v2 = c2.customFields().get(fName);
                    %>
                    <tr>
                        <td class="crm-compare-field-label"><%= Html.escape(field.getFieldLabel()) %></td>
                        <td class="crm-compare-cell">
                            <label class="crm-compare-radio-label">
                                <input type="radio" name="field_<%= Html.escape(fName) %>" value="<%= Html.escape(v1 != null ? v1 : "") %>" checked>
                                <span><%= Html.escape(v1 != null && !v1.isBlank() ? v1 : "(Trống)") %></span>
                            </label>
                        </td>
                        <td class="crm-compare-cell">
                            <label class="crm-compare-radio-label">
                                <input type="radio" name="field_<%= Html.escape(fName) %>" value="<%= Html.escape(v2 != null ? v2 : "") %>">
                                <span><%= Html.escape(v2 != null && !v2.isBlank() ? v2 : "(Trống)") %></span>
                            </label>
                        </td>
                    </tr>
                    <% }
                    } %>

                    <tr>
                        <td class="crm-compare-field-label">Dữ liệu liên quan (tự động chuyển giao)</td>
                        <td class="crm-compare-cell">
                            <div><%= c1.opportunityCount() %> Cơ hội bán hàng</div>
                            <div><%= c1.activityCount() %> Hoạt động &amp; Lịch</div>
                        </td>
                        <td class="crm-compare-cell">
                            <div><%= c2.opportunityCount() %> Cơ hội bán hàng</div>
                            <div><%= c2.activityCount() %> Hoạt động &amp; Lịch</div>
                        </td>
                    </tr>
                    </tbody>
                </table>

                <div class="crm-compare-actions">
                    <a href="${pageContext.request.contextPath}/customers/duplicates" class="crm-btn crm-btn-secondary">Hủy bỏ</a>
                    <button type="submit" class="crm-btn crm-btn-danger">
                        Xác nhận gộp khách hàng
                    </button>
                </div>
            </form>
        </div>
    </main>
</div>
</body>
</html>
