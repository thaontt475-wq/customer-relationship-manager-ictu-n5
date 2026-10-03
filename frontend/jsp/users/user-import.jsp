<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.dto.excel.ImportReportResult, com.crm.dto.excel.ImportRowData, com.crm.dto.excel.ImportErrorDetail" %>
<%@ page import="com.crm.util.Html" %>
<%
    ImportReportResult report = (ImportReportResult) request.getAttribute("report");
    boolean completed = Boolean.TRUE.equals(request.getAttribute("completed"));
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhập người dùng từ Excel | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/user-import.css">
</head>
<body class="crm-body users-admin">
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <main class="import-page crm-page" role="main">
            <div class="import-header crm-page-header">
                <nav class="crm-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/users">Quản lý người dùng</a>
                    <span class="separator">/</span>
                    <span class="current" aria-current="page">Nhập Excel</span>
                </nav>
                <div class="import-header-info">
                    <h1 class="crm-page-title">Nhập danh sách người dùng từ Excel</h1>
                    <p class="crm-page-description">Tải lên danh sách người dùng. Hệ thống sẽ kiểm tra và xem trước lỗi trước khi nhập chính thức.</p>
                </div>
            </div>

            <% if (error != null && !error.isBlank()) { %>
                <div class="import-alert import-alert-danger" role="alert">
                    <svg class="import-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    <span><%= Html.escape(error) %></span>
                </div>
            <% } %>

            <% if (report == null && !completed) { %>
                <section class="crm-card">
                    <div class="import-template-box" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 24px; border-bottom: 1px solid var(--border-color);">
                        <div>
                            <h2 style="font-size: 1.1rem; margin-bottom: 8px;">Bước 1: Tải tệp mẫu</h2>
                            <p style="color: var(--text-secondary); font-size: 0.9rem;">Tải tệp mẫu để xem cấu trúc dữ liệu yêu cầu trước khi nhập.</p>
                        </div>
                        <div class="import-template-actions" style="display: flex; gap: 12px;">
                            <a class="import-btn import-btn-excel" href="${pageContext.request.contextPath}/api/users/import/template?format=xlsx">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                                Tải mẫu XLSX
                            </a>
                            <a class="import-btn import-btn-secondary" href="${pageContext.request.contextPath}/api/users/import/template?format=csv">Tải mẫu CSV</a>
                        </div>
                    </div>

                    <form action="${pageContext.request.contextPath}/users/import/preview" method="post" enctype="multipart/form-data">
                        <input type="hidden" name="csrfToken" value="<%= Html.escape(com.crm.controller.ServerForms.csrf(request)) %>">
                        <h2 style="font-size: 1.1rem; margin-bottom: 16px;">Bước 2: Tải lên dữ liệu</h2>
                        
                        <div class="import-dropzone" style="border: 2px dashed var(--border-color); border-radius: 8px; padding: 40px; text-align: center; margin-bottom: 24px;">
                            <label for="file" style="display: block; cursor: pointer;">
                                <svg class="import-dropzone-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin: 0 auto 12px;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                                <div class="import-dropzone-text">Chọn tệp Excel hoặc CSV</div>
                                <div class="import-dropzone-hint" style="margin-top: 8px;">Hỗ trợ .xlsx, .xls, .csv (Tối đa 10MB)</div>
                                <input id="file" name="file" type="file" accept=".xlsx,.xls,.csv" required style="margin-top: 16px; padding: 8px;">
                            </label>
                        </div>
                        <div style="text-align: right;">
                            <button type="submit" class="import-btn import-btn-primary">Xem trước dữ liệu</button>
                        </div>
                    </form>
                </section>
            <% } %>

            <% if (report != null) { %>
                <section class="crm-card">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
                        <h2 style="font-size: 1.25rem;"><%= completed ? "Báo cáo Kết quả Nhập" : "Bước 3: Xem trước dữ liệu" %></h2>
                        <% if (completed) { %>
                            <a href="${pageContext.request.contextPath}/users" class="import-btn import-btn-primary">Đến trang Quản lý</a>
                        <% } else { %>
                            <a href="${pageContext.request.contextPath}/users/import" class="import-btn import-btn-secondary">Tải lại tệp khác</a>
                        <% } %>
                    </div>

                    <div class="import-stats-grid" style="margin-bottom: 32px;">
                        <div class="import-stat-card total">
                            <span class="import-stat-label">Tổng số dòng đã đọc</span>
                            <span class="import-stat-value"><%= report.getTotalRows() %></span>
                        </div>
                        <div class="import-stat-card <%= completed ? "success" : "valid" %>">
                            <span class="import-stat-label"><%= completed ? "Nhập thành công" : "Dòng hợp lệ (Sẵn sàng nhập)" %></span>
                            <span class="import-stat-value"><%= completed ? report.getSuccessRows() : report.getValidCount() %></span>
                        </div>
                        <div class="import-stat-card error">
                            <span class="import-stat-label"><%= completed ? "Nhập thất bại" : "Dòng lỗi (Sẽ bị bỏ qua)" %></span>
                            <span class="import-stat-value"><%= completed ? report.getFailedRows() : report.getErrorCount() %></span>
                        </div>
                    </div>

                    <% if (!completed) { %>
                        <% if (report.getErrors() != null && !report.getErrors().isEmpty()) { %>
                            <h3 style="font-size: 1.1rem; margin-bottom: 12px; color: var(--danger);">Các dòng lỗi cần kiểm tra lại</h3>
                            <div class="import-table-wrap" style="margin-bottom: 32px;">
                                <table class="import-table">
                                    <thead>
                                        <tr>
                                            <th style="width: 80px;">Dòng</th>
                                            <th>Trường dữ liệu</th>
                                            <th>Chi tiết lỗi</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <% for (ImportErrorDetail errRow : report.getErrors()) { %>
                                            <tr class="row-error">
                                                <td><strong>#<%= errRow.getRowNumber() %></strong></td>
                                                <td><span class="import-tag import-tag-danger"><%= Html.escape(errRow.getField()) %></span></td>
                                                <td><%= Html.escape(errRow.getMessage()) %></td>
                                            </tr>
                                        <% } %>
                                    </tbody>
                                </table>
                            </div>
                        <% } %>

                        <% if (report.getValidRows() != null && !report.getValidRows().isEmpty()) { %>
                            <h3 style="font-size: 1.1rem; margin-bottom: 12px; color: var(--success);">Các dòng hợp lệ có thể nhập</h3>
                            <div class="import-table-wrap" style="margin-bottom: 32px;">
                                <table class="import-table">
                                    <thead>
                                        <tr>
                                            <th>Dòng</th>
                                            <th>Họ tên</th>
                                            <th>Email</th>
                                            <th>Tài khoản</th>
                                            <th>Vai trò</th>
                                            <th>Nhóm</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <% for (ImportRowData validRow : report.getValidRows()) { %>
                                            <tr class="row-valid">
                                                <td>#<%= validRow.getRowNum() %></td>
                                                <td><%= Html.escape(validRow.getFullName()) %></td>
                                                <td><%= Html.escape(validRow.getEmail()) %></td>
                                                <td><%= Html.escape(validRow.getUsername()) %></td>
                                                <td><%= Html.escape(validRow.getRole()) %></td>
                                                <td><%= Html.escape(validRow.getTeam()) %></td>
                                            </tr>
                                        <% } %>
                                    </tbody>
                                </table>
                            </div>
                        <% } %>

                        <% if (report.getValidCount() > 0) { %>
                            <div style="background: var(--bg-secondary); padding: 20px; border-radius: 8px; border: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <h3 style="font-size: 1.05rem; margin-bottom: 4px;">Xác nhận nhập dữ liệu</h3>
                                    <p style="color: var(--text-secondary); font-size: 0.9rem; margin: 0;">Chỉ <%= report.getValidCount() %> dòng hợp lệ sẽ được nhập vào hệ thống. Các dòng lỗi bị bỏ qua.</p>
                                </div>
                                <form action="${pageContext.request.contextPath}/users/import/execute" method="post">
                                    <input type="hidden" name="csrfToken" value="<%= Html.escape(com.crm.controller.ServerForms.csrf(request)) %>">
                                    <button type="submit" class="import-btn import-btn-success">Xác nhận nhập</button>
                                </form>
                            </div>
                        <% } else { %>
                            <div class="import-alert import-alert-danger" role="alert">
                                Không có dòng dữ liệu nào hợp lệ. Vui lòng chỉnh sửa tệp Excel và tải lại.
                            </div>
                        <% } %>

                    <% } else { %>
                        <div class="import-report-box" style="margin-top: 32px;">
                            <div class="import-report-icon">✓</div>
                            <h3 class="import-report-title">Hoàn tất nhập dữ liệu</h3>
                            <p class="import-report-desc">Đã xử lý xong tệp dữ liệu. Các tài khoản hợp lệ đã được tạo và có thể sử dụng hệ thống ngay lập tức.</p>
                            <a href="${pageContext.request.contextPath}/users" class="import-btn import-btn-primary" style="margin-top: 16px;">Về danh sách người dùng</a>
                        </div>
                    <% } %>
                </section>
            <% } %>

        </main>
    </div>
</body>
</html>
