<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.User, com.crm.model.Role, com.crm.util.Html, com.crm.controller.ServerForms" %>
<%@ page import="java.util.List" %>
<%
  User person = (User) request.getAttribute("profileUser");
  if (person == null) { response.sendError(500, "Thiếu dữ liệu hồ sơ"); return; }
  String err = (String) request.getAttribute("error");
  String signature = (String) request.getAttribute("signature");
  if (signature == null) signature = person.getSignature();

  List<Role> roles = person.getRoles();
  String rolesStr = "";
  if (roles != null && !roles.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Role r : roles) {
          if (sb.length() > 0) sb.append(", ");
          sb.append(r.getName());
      }
      rolesStr = sb.toString();
  }
  String prefix = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ cá nhân | CRM ICTU</title>

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
    <link rel="stylesheet" href="<%= Html.escape(prefix) %>/css/users/profile.css?v=20261005_4">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <main class="profile-page crm-page" role="main">
            <div class="profile-container">
                <nav class="profile-breadcrumb" aria-label="Breadcrumb">
                    <a href="<%= Html.escape(prefix) %>/dashboard">Trang chủ</a>
                    <span class="sep">/</span>
                    <span class="current">Hồ sơ cá nhân</span>
                </nav>

                <div class="crm-page-header">
                    <div class="crm-page-header-text">
                        <h1 class="crm-page-title">Hồ sơ cá nhân</h1>
                        <p class="crm-page-description">Cập nhật thông tin cá nhân và chữ ký email của bạn.</p>
                    </div>
                </div>

                <section class="crm-card profile-card">
                    <% if (err != null && !err.isBlank()) { %>
                        <div class="crm-alert crm-alert-error" role="alert">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd"/>
                            </svg>
                            <span><%= Html.escape(err) %></span>
                        </div>
                    <% } %>
                    <% if ("1".equals(request.getParameter("updated"))) { %>
                        <div class="crm-alert crm-alert-success" role="status">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd"/>
                            </svg>
                            <span>Cập nhật hồ sơ thành công.</span>
                        </div>
                    <% } %>

                    <form method="post" action="<%= Html.escape(prefix) %>/profile" class="crm-form">
                        <input type="hidden" name="csrfToken" value="<%= Html.escape(ServerForms.csrf(request)) %>">

                        <div class="profile-header-group">
                            <div class="profile-avatar-section">
                                <div class="avatar-preview">
                                    <img src="<%= Html.escape(prefix) %>/profile/avatar/thumbnail" alt="Ảnh đại diện" class="avatar-img" onerror="this.onerror=null; this.src='data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' viewBox=\'0 0 24 24\' fill=\'%2394a3b8\'%3E%3Cpath d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/%3E%3C/svg%3E';">
                                </div>
                                <div class="avatar-info-col">
                                    <div class="avatar-user-name"><%= Html.escape(person.getFullName() != null && !person.getFullName().isBlank() ? person.getFullName() : person.getUsername()) %></div>
                                    <div class="avatar-user-role"><%= Html.escape(rolesStr.isEmpty() ? "Người dùng CRM" : rolesStr) %></div>
                                    <div class="avatar-actions">
                                        <a href="<%= Html.escape(prefix) %>/profile/avatar" class="crm-btn crm-btn-secondary crm-btn-sm">
                                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                                                <circle cx="12" cy="13" r="4"></circle>
                                            </svg>
                                            Đổi ảnh đại diện
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="crm-form-grid">
                            <div class="crm-form-group">
                                <label class="crm-form-label" for="fullName">Họ và tên <span class="crm-required">*</span></label>
                                <input type="text" id="fullName" name="fullName" class="crm-form-control" value="<%= Html.escape(person.getFullName()) %>" required maxlength="150" placeholder="Nhập họ và tên">
                            </div>

                            <div class="crm-form-group">
                                <label class="crm-form-label" for="phone">Số điện thoại <span class="crm-required">*</span></label>
                                <input type="tel" id="phone" name="phone" class="crm-form-control" value="<%= Html.escape(person.getPhone()) %>" required autocomplete="tel" pattern="^0[35789][0-9]{8}$" title="Số điện thoại Việt Nam hợp lệ, ví dụ: 0912345678" placeholder="09xxxxxxxx">
                            </div>
                        </div>

                        <div class="crm-form-group">
                            <label class="crm-form-label" for="signature">Chữ ký email</label>
                            <textarea id="signature" name="signature" class="crm-form-control" rows="4" placeholder="Nhập chữ ký hiển thị dưới cuối email..."><%= Html.escape(signature) %></textarea>
                            <span class="crm-field-hint">Chữ ký sẽ tự động được gắn vào cuối các email gửi từ hệ thống CRM.</span>
                        </div>

                        <div class="profile-org-section">
                            <h3 class="section-title">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M3 21h18M3 7v14M21 7v14M6 7V3h12v4M9 11h2M9 15h2M13 11h2M13 15h2"/>
                                </svg>
                                Thông tin tổ chức
                            </h3>
                            <div class="crm-form-grid readonly-grid">
                                <div class="crm-form-group">
                                    <label class="crm-form-label" for="email">Email công ty</label>
                                    <input type="email" id="email" class="crm-form-control crm-readonly" value="<%= Html.escape(person.getEmail()) %>" readonly>
                                </div>

                                <div class="crm-form-group">
                                    <label class="crm-form-label" for="team">Nhóm / Phòng ban</label>
                                    <input type="text" id="team" class="crm-form-control crm-readonly" value="<%= Html.escape(person.getTeamName() != null ? person.getTeamName() : "Chưa phân nhóm") %>" readonly>
                                </div>

                                <div class="crm-form-group full-width">
                                    <label class="crm-form-label" for="role">Vai trò hệ thống</label>
                                    <input type="text" id="role" class="crm-form-control crm-readonly" value="<%= Html.escape(rolesStr.isEmpty() ? "Chưa cấp quyền" : rolesStr) %>" readonly>
                                </div>
                            </div>

                            <div class="crm-form-help">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="12" cy="12" r="10"></circle>
                                    <line x1="12" y1="16" x2="12" y2="12"></line>
                                    <line x1="12" y1="8" x2="12.01" y2="8"></line>
                                </svg>
                                <span>Ghi chú: Thông tin tổ chức (Email, Nhóm, Vai trò) được quản lý và phân quyền tập trung bởi Quản trị viên hệ thống.</span>
                            </div>
                        </div>

                        <div class="crm-form-actions">
                            <button type="submit" class="crm-btn crm-btn-primary">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                                    <polyline points="17 21 17 13 7 13 7 21"></polyline>
                                    <polyline points="7 3 7 8 15 8"></polyline>
                                </svg>
                                Lưu thay đổi
                            </button>
                            <a href="<%= Html.escape(prefix) %>/dashboard" class="crm-btn crm-btn-secondary">Hủy bỏ</a>
                        </div>
                    </form>
                </section>
            </div>
        </main>
    </div>
</body>
</html>
