<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.util.Html" %>
<%
    String prefix = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ảnh đại diện | CRM ICTU</title>

    <!-- Google Fonts Plus Jakarta Sans / Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:ital,wght@0,300;0,400;0,500;0,600;0,700;0,800;1,400;1,500&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">

    <!-- CSS dùng chung của hệ thống -->
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/shared/common.css?v=20261005_4">
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/shared/layout.css?v=20261005_4">
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/shared/header.css?v=20261005_4">
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/shared/sidebar.css?v=20261005_4">
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/shared/responsive.css?v=20261005_4">
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/shared/components.css?v=20261005_4">
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/users/avatar.css?v=20261005_4">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <main class="avatar-page crm-page" role="main">
            <div class="avatar-container">
                <nav class="avatar-breadcrumb" aria-label="Breadcrumb">
                    <a href="<%= Html.escape(prefix) %>/dashboard">Trang chủ</a>
                    <span class="sep">/</span>
                    <a href="<%= Html.escape(prefix) %>/profile">Hồ sơ cá nhân</a>
                    <span class="sep">/</span>
                    <span class="current">Ảnh đại diện</span>
                </nav>

                <div class="crm-page-header">
                    <div class="crm-page-header-text">
                        <h1 class="crm-page-title">Cập nhật ảnh đại diện</h1>
                        <p class="crm-page-description">Quản lý ảnh đại diện hiển thị trên toàn hệ thống CRM.</p>
                    </div>
                </div>

                <section class="crm-card avatar-card">
                    <% if (request.getAttribute("message") != null) { %>
                        <div class="crm-alert crm-alert-error" role="alert">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd"/>
                            </svg>
                            <span><%= Html.escape((String) request.getAttribute("message")) %></span>
                        </div>
                    <% } else if ("1".equals(request.getParameter("updated"))) { %>
                        <div class="crm-alert crm-alert-success" role="status">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd"/>
                            </svg>
                            <span>Đã cập nhật ảnh đại diện thành công.</span>
                        </div>
                    <% } %>

                    <div class="avatar-current-section">
                        <div class="avatar-current-wrapper">
                            <% Boolean hasAvatar = (Boolean) request.getAttribute("hasAvatar");
                               if (Boolean.TRUE.equals(hasAvatar)) { %>
                                <img src="<%= Html.escape(prefix) %>/profile/avatar/image" alt="Ảnh đại diện hiện tại" class="avatar-img-large" onerror="this.onerror=null; this.src='data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 24 24\' fill=\'%2394a3b8\'%3E%3Cpath d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/%3E%3C/svg%3E';">
                            <% } else { %>
                                <div class="avatar-placeholder-large" title="Chưa có ảnh đại diện">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="12" cy="7" r="4"></circle>
                                    </svg>
                                </div>
                            <% } %>
                        </div>
                        <div class="avatar-current-label">Ảnh đại diện hiện tại</div>
                    </div>

                    <div class="avatar-instructions">
                        <h3 class="instructions-title">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="16" x2="12" y2="12"></line>
                                <line x1="12" y1="8" x2="12.01" y2="8"></line>
                            </svg>
                            Yêu cầu ảnh tải lên:
                        </h3>
                        <ul>
                            <li>Định dạng hỗ trợ: <strong>JPG, JPEG, PNG</strong>.</li>
                            <li>Dung lượng tối đa: <strong>2 MiB</strong>.</li>
                            <li>Hệ thống tự động cắt và căn giữa ảnh thành tỷ lệ <strong>1:1 (vuông)</strong>.</li>
                        </ul>
                    </div>

                    <form method="post" action="<%= Html.escape(prefix) %>/profile/avatar" enctype="multipart/form-data" class="crm-form avatar-form">
                        <input type="hidden" name="csrfToken" value="<%= Html.escape((String) request.getAttribute("csrfToken")) %>">

                        <div class="crm-form-group">
                            <label class="crm-form-label" for="avatar">Chọn ảnh từ thiết bị <span class="crm-required">*</span></label>
                            <input id="avatar" name="avatar" type="file" accept="image/jpeg,image/png" class="crm-form-control file-input" required>
                        </div>

                        <div class="crm-form-actions">
                            <button type="submit" class="crm-btn crm-btn-primary">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                    <polyline points="17 8 12 3 7 8"></polyline>
                                    <line x1="12" y1="3" x2="12" y2="15"></line>
                                </svg>
                                Tải lên & Cập nhật
                            </button>
                            <a href="<%= Html.escape(prefix) %>/profile" class="crm-btn crm-btn-secondary">Quay lại Hồ sơ</a>
                        </div>
                    </form>
                </section>
            </div>
        </main>
    </div>
</body>
</html>
