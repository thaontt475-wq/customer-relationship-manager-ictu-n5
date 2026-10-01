<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.User, com.crm.model.Role, java.util.List, java.util.stream.Collectors" %>
<%
    User user = (User) request.getAttribute("profileUser");
    String userId = user != null ? String.valueOf(user.getId()) : "";
    String fullName = user != null && user.getFullName() != null ? user.getFullName() : "";
    String username = user != null && user.getUsername() != null ? user.getUsername() : "";
    String email = user != null && user.getEmail() != null ? user.getEmail() : "";
    String phone = user != null && user.getPhone() != null ? user.getPhone() : "";
    String signature = request.getAttribute("signature") != null ? String.valueOf(request.getAttribute("signature")) : "";
    String teamName = user != null && user.getTeamName() != null ? user.getTeamName() : "Chưa phân nhóm";
    String status = user != null && user.getStatus() != null ? user.getStatus() : "ACTIVE";

    String rolesText = "Chưa phân vai trò";
    if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
        rolesText = user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));
    }

    String avatarChar = !fullName.isEmpty() ? fullName.substring(0, 1).toUpperCase() : "U";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ cá nhân - CRM ICTU</title>

    <!-- CSS dùng chung -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS module Users -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/users.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/profile.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Nội dung chính -->
        <main class="user-page crm-page" id="profileApp" role="main">
            <div class="user-container crm-page-container">

                <!-- Breadcrumb -->
                <nav class="user-breadcrumb crm-breadcrumb" aria-label="Điều hướng">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Tài khoản</span>
                    <span class="separator">/</span>
                    <span class="active">Hồ sơ cá nhân</span>
                </nav>

                <!-- Header -->
                <header class="user-header crm-page-header">
                    <div class="user-header-info">
                        <h1>Hồ sơ cá nhân</h1>
                        <p>Xem thông tin tài khoản, cập nhật số điện thoại liên hệ và chữ ký email dùng khi gửi báo giá cho khách hàng.</p>
                    </div>

                </header>

                <!-- Alerts -->
                <div class="user-alerts" id="profileAlertsArea" aria-live="polite">
                    <div class="user-alert user-alert-danger" id="profileErrorAlert" style="display: none;" role="alert">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title">Lỗi cập nhật</div>
                            <div id="profileErrorMessage"></div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="user-alert user-alert-success" id="profileSuccessAlert" style="display: none;" role="status">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title">Thành công</div>
                            <div id="profileSuccessMessage">Cập nhật hồ sơ cá nhân thành công.</div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>
                </div>

                <!-- Bố cục Profile: Cột trái (Thông tin tóm tắt) & Cột phải (Form chỉnh sửa) -->
                <div class="user-profile-layout">

                    <!-- Cột trái: Thẻ thông tin cá nhân -->
                    <aside class="user-profile-sidebar" aria-label="Tóm tắt tài khoản">
                        <div class="user-card crm-card">
                            <div class="user-info-summary">
                                <div class="user-avatar-wrapper">
                                    <div class="user-avatar-lg" id="sidebarAvatar" aria-hidden="true">
                                        <img id="sidebarAvatarImg" src="${pageContext.request.contextPath}/profile/avatar/image" alt="Ảnh đại diện" class="user-avatar-img"
                                             onload="this.style.display='block'; var c=document.getElementById('sidebarAvatarChar'); if(c) c.style.display='none';"
                                             onerror="this.style.display='none'; var c=document.getElementById('sidebarAvatarChar'); if(c) c.style.display='inline';">
                                        <span id="sidebarAvatarChar"><%= avatarChar %></span>
                                    </div>
                                    <button type="button" class="user-avatar-edit-badge" id="btnOpenAvatarModal" title="Đổi ảnh đại diện" aria-label="Đổi ảnh đại diện">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                                            <circle cx="12" cy="13" r="4"></circle>
                                        </svg>
                                    </button>
                                </div>
                                <div style="margin-bottom: 8px;">
                                    <button type="button" class="user-avatar-btn-trigger" id="btnTriggerAvatarModal">
                                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                                            <circle cx="12" cy="13" r="4"></circle>
                                        </svg>
                                        <span>Đổi ảnh đại diện</span>
                                    </button>
                                </div>
                                <h2 id="sidebarFullName"><%= fullName.isEmpty() ? "Chưa có tên" : fullName %></h2>
                                <div class="email" id="sidebarEmail"><%= email %></div>
                                <div>
                                    <span class="status-badge status-badge--active">
                                        <span class="status-dot" aria-hidden="true"></span>
                                        Đang hoạt động
                                    </span>
                                </div>
                            </div>

                            <div class="user-details-list">
                                <div class="user-details-item">
                                    <span class="user-details-label">Mã ID</span>
                                    <span class="user-details-value">#<%= userId %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Tên đăng nhập</span>
                                    <span class="user-details-value"><%= username %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Vai trò</span>
                                    <span class="user-details-value" id="sidebarRoles"><%= rolesText %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Nhóm kinh doanh</span>
                                    <span class="user-details-value" id="sidebarTeam"><%= teamName %></span>
                                </div>
                                <div class="user-details-item">
                                    <span class="user-details-label">Số điện thoại</span>
                                    <span class="user-details-value" id="sidebarPhone"><%= phone.isEmpty() ? "Chưa cập nhật" : phone %></span>
                                </div>
                            </div>
                        </div>
                    </aside>

                    <!-- Cột phải: Form chỉnh sửa hồ sơ -->
                    <section class="user-profile-main" aria-labelledby="profileEditTitle">
                        <div class="user-card crm-card">
                            <div class="user-card-header" style="margin-bottom: 20px;">
                                <div>
                                    <h2 id="profileEditTitle" class="user-card-title">
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                        </svg>
                                        Chỉnh sửa thông tin hồ sơ
                                    </h2>
                                    <p class="user-card-subtitle">Cập nhật họ tên, số điện thoại và mẫu chữ ký email tự động chèn vào báo giá gửi khách hàng</p>
                                </div>
                            </div>

                            <form id="profileForm" novalidate>

                                <div style="margin-bottom: 22px; padding: 14px 16px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;">
                                    <h3 style="font-size: 0.92rem; font-weight: 700; color: #334155; margin: 0 0 12px 0;">
                                        Thông tin hệ thống do Quản trị viên quản lý (Chỉ đọc)
                                    </h3>

                                    <div class="profile-readonly-fields">
                                        <div class="modal-field crm-form-group" style="margin: 0;">
                                            <label class="modal-label crm-label" for="readonlyEmail">
                                                Địa chỉ Email
                                                <span class="profile-readonly-badge">
                                                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                                    Chỉ đọc
                                                </span>
                                            </label>
                                            <input type="email" id="readonlyEmail" class="modal-input crm-input" value="<%= email %>" readonly disabled style="background-color: #f1f5f9; cursor: not-allowed; color: #475569;">
                                        </div>

                                        <div class="modal-field crm-form-group" style="margin: 0;">
                                            <label class="modal-label crm-label" for="readonlyRoles">
                                                Vai trò hệ thống
                                                <span class="profile-readonly-badge">
                                                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                                    Chỉ đọc
                                                </span>
                                            </label>
                                            <input type="text" id="readonlyRoles" class="modal-input crm-input" value="<%= rolesText %>" readonly disabled style="background-color: #f1f5f9; cursor: not-allowed; color: #475569;">
                                        </div>

                                        <div class="modal-field crm-form-group" style="margin: 0;">
                                            <label class="modal-label crm-label" for="readonlyTeam">
                                                Nhóm kinh doanh
                                                <span class="profile-readonly-badge">
                                                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                                    Chỉ đọc
                                                </span>
                                            </label>
                                            <input type="text" id="readonlyTeam" class="modal-input crm-input" value="<%= teamName %>" readonly disabled style="background-color: #f1f5f9; cursor: not-allowed; color: #475569;">
                                        </div>
                                    </div>
                                </div>


                                <div class="modal-field crm-form-group">
                                    <label for="profileFullName" class="modal-label crm-label">
                                        Họ và tên <span class="modal-required">*</span>
                                    </label>
                                    <input type="text" id="profileFullName" name="fullName" class="modal-input crm-input"
                                           value="<%= fullName %>" placeholder="Ví dụ: Nguyễn Văn A" required>
                                    <div class="modal-field-feedback" id="feedbackProfileFullName" style="display: block; color: #dc2626; font-size: 0.83rem; margin-top: 4px;"></div>
                                </div>

                                <div class="modal-field crm-form-group">
                                    <label for="profilePhone" class="modal-label crm-label">
                                        Số điện thoại di động
                                        <span style="font-weight: normal; font-size: 0.82rem; color: #64748b;">(Định dạng Việt Nam, ví dụ: 0912345678 hoặc +84912345678)</span>
                                    </label>
                                    <input type="tel" id="profilePhone" name="phone" class="modal-input crm-input"
                                           value="<%= phone %>" placeholder="Ví dụ: 0912345678">
                                    <div class="modal-field-feedback" id="feedbackProfilePhone" style="display: block; color: #dc2626; font-size: 0.83rem; margin-top: 4px;"></div>
                                </div>

                                <div class="modal-field crm-form-group">
                                    <label for="profileSignature" class="modal-label crm-label">
                                        Chữ ký Email (Email Signature)
                                        <span style="font-weight: normal; font-size: 0.82rem; color: #64748b;">(Tự động chèn ở cuối báo giá gửi khách)</span>
                                    </label>
                                    <textarea id="profileSignature" name="signature" class="modal-input crm-input" rows="5"
                                              placeholder="Ví dụ:&#10;Trân trọng,&#10;<%= !fullName.isEmpty() ? fullName : "Nguyễn Văn A" %> - Bộ phận Kinh doanh&#10;Công ty CRM ICTU&#10;SĐT: <%= !phone.isEmpty() ? phone : "0912345678" %> | Email: <%= !email.isEmpty() ? email : "email@example.com" %>"><%= signature %></textarea>

                                    <div class="signature-preview-title" style="margin-top: 10px;">Xem trước chữ ký email</div>
                                    <div class="signature-preview-box" id="signaturePreview"><%= signature.isEmpty() ? "(Chưa thiết lập chữ ký email)" : signature %></div>
                                </div>

                                <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; padding-top: 18px; border-top: 1px solid #e2e8f0;">
                                    <button type="reset" class="btn crm-btn btn-secondary crm-btn-secondary" id="btnResetProfile">Đặt lại</button>
                                    <button type="submit" class="btn crm-btn btn-primary crm-btn-primary" id="btnSaveProfile">
                                        <span id="saveProfileSpinner" class="user-spinner" style="width: 14px; height: 14px; display: none; margin-right: 4px;" aria-hidden="true"></span>
                                        <span id="saveProfileBtnText">Lưu thay đổi</span>
                                    </button>
                                </div>
                            </form>
                        </div>
                    </section>

                </div>

            </div>
        </main>


        <div class="avatar-modal-backdrop" id="avatarUploadModal" role="dialog" aria-modal="true" aria-labelledby="avatarModalTitle">
            <div class="avatar-modal-card">
                <div class="avatar-modal-header">
                    <h2 class="avatar-modal-title" id="avatarModalTitle">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                            <circle cx="12" cy="13" r="4"></circle>
                        </svg>
                        <span>Cập nhật ảnh đại diện người dùng</span>
                    </h2>
                    <button type="button" class="avatar-modal-close" id="btnCloseAvatarModal" aria-label="Đóng">&times;</button>
                </div>

                <div class="avatar-modal-body">

                    <div class="avatar-modal-alert avatar-modal-alert-danger" id="avatarModalErrorAlert" role="alert" style="display: none;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" style="flex-shrink:0; margin-top:1px;">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div id="avatarModalErrorMessage" style="flex:1;"></div>
                        <button type="button" style="background:none;border:none;cursor:pointer;color:inherit;font-size:1.1rem;line-height:1;padding:0;" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <!-- 1. Vùng Dropzone chọn ảnh -->
                    <div class="avatar-dropzone" id="avatarDropzone">
                        <input type="file" id="avatarFileInput" accept=".jpg,.jpeg,.png,image/jpeg,image/png" style="display: none;">
                        <svg class="avatar-dropzone-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                            <circle cx="8.5" cy="8.5" r="1.5"></circle>
                            <polyline points="21 15 16 10 5 21"></polyline>
                        </svg>
                        <div class="avatar-dropzone-title">Nhấp để chọn ảnh hoặc kéo thả ảnh vào đây</div>
                        <div class="avatar-dropzone-hint">
                            Chấp nhận tệp định dạng <strong>JPG, JPEG hoặc PNG</strong>.<br>
                            Dung lượng tối đa không quá <strong>2MB (2.097.152 byte)</strong>.
                        </div>
                        <div class="avatar-dropzone-badge">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
                            Tự động cắt vuông 1:1 chuẩn xác
                        </div>
                    </div>


                    <div class="avatar-cropper-wrapper" id="avatarCropperWrapper">
                        <div class="avatar-crop-stage" id="avatarCropStage" title="Kéo chuột để di chuyển vị trí cắt ảnh">
                            <canvas id="avatarCropCanvas" class="avatar-crop-canvas"></canvas>
                            <div class="avatar-crop-mask"></div>
                        </div>

                        <!-- Thanh điều khiển Zoom -->
                        <div class="avatar-crop-controls">
                            <button type="button" class="avatar-control-btn" id="btnZoomOut" title="Thu nhỏ">-</button>
                            <label for="avatarZoomSlider">Thu phóng:</label>
                            <input type="range" id="avatarZoomSlider" class="avatar-zoom-slider" min="1" max="3" step="0.05" value="1">
                            <button type="button" class="avatar-control-btn" id="btnZoomIn" title="Phóng to">+</button>
                            <button type="button" class="avatar-control-btn" id="btnResetCrop" title="Đặt lại vị trí ban đầu">Đặt lại</button>
                            <button type="button" class="avatar-control-btn" id="btnChangeImage" title="Chọn ảnh khác">Chọn ảnh khác</button>
                        </div>


                        <div class="avatar-previews-bar">
                            <div class="avatar-preview-item">
                                <div class="avatar-preview-circle-lg">
                                    <canvas id="previewCanvasLg" width="64" height="64"></canvas>
                                </div>
                                <span>Hồ sơ (64px)</span>
                            </div>
                            <div class="avatar-preview-item">
                                <div class="avatar-preview-circle-md">
                                    <canvas id="previewCanvasMd" width="44" height="44"></canvas>
                                </div>
                                <span>Sidebar (44px)</span>
                            </div>
                            <div class="avatar-preview-item">
                                <div class="avatar-preview-circle-sm">
                                    <canvas id="previewCanvasSm" width="32" height="32"></canvas>
                                </div>
                                <span>Header (32px)</span>
                            </div>
                        </div>
                    </div>

                    <!-- Thanh tiến trình tải lên (Progress Bar) -->
                    <div class="avatar-progress-wrap" id="avatarProgressWrap">
                        <div class="avatar-progress-bar-bg">
                            <div class="avatar-progress-bar-fill" id="avatarProgressBarFill"></div>
                        </div>
                        <div class="avatar-progress-meta">
                            <span id="avatarProgressStatus">Đang xử lý và tải ảnh lên...</span>
                            <span id="avatarProgressPercent">0%</span>
                        </div>
                    </div>
                </div>

                <div class="avatar-modal-footer">
                    <button type="button" class="btn crm-btn btn-secondary crm-btn-secondary" id="btnCancelAvatarModal">Hủy bỏ</button>
                    <button type="button" class="btn crm-btn btn-primary crm-btn-primary" id="btnSaveAvatar" disabled>
                        <span id="avatarSaveSpinner" class="user-spinner" style="width: 14px; height: 14px; display: none; margin-right: 4px;" aria-hidden="true"></span>
                        <span id="avatarSaveBtnText">Lưu ảnh đại diện</span>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Footer dùng chung -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <script>
    document.addEventListener('DOMContentLoaded', function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}';
        var profileForm = document.getElementById('profileForm');
        var inputFullName = document.getElementById('profileFullName');
        var inputPhone = document.getElementById('profilePhone');
        var inputSignature = document.getElementById('profileSignature');
        var signaturePreview = document.getElementById('signaturePreview');

        var feedbackName = document.getElementById('feedbackProfileFullName');
        var feedbackPhone = document.getElementById('feedbackProfilePhone');

        var errorAlert = document.getElementById('profileErrorAlert');
        var errorMessage = document.getElementById('profileErrorMessage');
        var successAlert = document.getElementById('profileSuccessAlert');
        var successMessage = document.getElementById('profileSuccessMessage');

        var btnSave = document.getElementById('btnSaveProfile');
        var saveSpinner = document.getElementById('saveProfileSpinner');
        var saveBtnText = document.getElementById('saveProfileBtnText');

        var sidebarFullName = document.getElementById('sidebarFullName');
        var sidebarPhone = document.getElementById('sidebarPhone');
        var sidebarAvatar = document.getElementById('sidebarAvatar');

        // Real-time signature preview
        inputSignature.addEventListener('input', function () {
            var val = inputSignature.value.trim();
            signaturePreview.textContent = val || '(Chưa thiết lập chữ ký email)';
        });

        // Regex số điện thoại Việt Nam chuẩn (AC 5 & AC 6)
        var vietnamesePhoneRegex = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;

        // Tự động đồng bộ thông tin tài khoản từ session nếu chưa có từ server-side rendering
        (async function loadSessionProfile() {
            try {
                var resp = await fetch(contextPath + '/api/auth/session');
                if (resp.ok) {
                    var sData = await resp.json();
                    if (sData && sData.data && sData.data.currentUser) {
                        var u = sData.data.currentUser;
                        if (!inputFullName.value && u.fullName) {
                            inputFullName.value = u.fullName;
                            inputFullName.defaultValue = u.fullName;
                        }
                        if (!inputPhone.value && u.phone) {
                            inputPhone.value = u.phone;
                            inputPhone.defaultValue = u.phone;
                        }
                        var rEmail = document.getElementById('readonlyEmail');
                        if (rEmail && !rEmail.value && u.email) rEmail.value = u.email;
                        var rTeam = document.getElementById('readonlyTeam');
                        if (rTeam && (!rTeam.value || rTeam.value === 'Chưa phân nhóm') && (u.teamName || u.team)) {
                            rTeam.value = u.teamName || u.team;
                        }
                        var rRoles = document.getElementById('readonlyRoles');
                        if (rRoles && (!rRoles.value || rRoles.value === 'Chưa phân vai trò') && sData.data.roles) {
                            var roleList = sData.data.roles;
                            if (Array.isArray(roleList) && roleList.length > 0) {
                                rRoles.value = roleList.map(function(r) { return typeof r === 'string' ? r : (r.name || r.code); }).join(', ');
                            }
                        }
                        if (sidebarFullName && u.fullName) sidebarFullName.textContent = u.fullName;
                        if (sidebarAvatar && u.fullName) sidebarAvatar.textContent = u.fullName.trim().charAt(0).toUpperCase() || 'U';
                        if (sidebarPhone && u.phone) sidebarPhone.textContent = u.phone;
                    }
                }
            } catch (e) {
                // Ignore silent fallback
            }
        })();

        function showAlert(isSuccess, msg) {
            if (isSuccess) {
                successMessage.textContent = msg;
                successAlert.style.display = 'flex';
                errorAlert.style.display = 'none';
                setTimeout(function () {
                    successAlert.style.display = 'none';
                }, 5000);
            } else {
                errorMessage.textContent = msg;
                errorAlert.style.display = 'flex';
                successAlert.style.display = 'none';
            }
        }

        function clearErrors() {
            feedbackName.textContent = '';
            feedbackPhone.textContent = '';
            inputFullName.classList.remove('is-invalid');
            inputPhone.classList.remove('is-invalid');
            errorAlert.style.display = 'none';
            successAlert.style.display = 'none';
        }

        profileForm.addEventListener('reset', function () {
            window.setTimeout(function () {
                clearErrors();
                var signature = inputSignature.value.trim();
                signaturePreview.textContent = signature || '(Chưa thiết lập chữ ký email)';
            }, 0);
        });

        profileForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            clearErrors();

            var fullName = inputFullName.value.trim();
            var phone = inputPhone.value.trim();
            var signature = inputSignature.value.trim();
            var isValid = true;

            // 1. Kiểm tra Họ và tên
            if (!fullName) {
                inputFullName.classList.add('is-invalid');
                feedbackName.textContent = 'Vui lòng nhập họ và tên.';
                isValid = false;
            }

            // 2. Kiểm tra Số điện thoại Việt Nam (AC 5, AC 6)
            if (phone !== '') {
                if (!vietnamesePhoneRegex.test(phone)) {
                    inputPhone.classList.add('is-invalid');
                    feedbackPhone.textContent = 'Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam hợp lệ (ví dụ: 0912345678 hoặc +84912345678).';
                    isValid = false;
                }
            }

            if (!isValid) return;

            // AC 2, 3, 8: Chỉ gửi họ tên, số điện thoại, chữ ký (email/group/role không được gửi hoặc backend sẽ ignore)
            var payload = {
                fullName: fullName,
                phone: phone,
                signature: signature
            };

            btnSave.disabled = true;
            saveSpinner.style.display = 'inline-block';
            saveBtnText.textContent = 'Đang lưu...';

            try {
                // BE CONTRACT NEEDED: /profile và /api/profile.
                var response = await fetch(contextPath + '/api/profile', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                btnSave.disabled = false;
                saveSpinner.style.display = 'none';
                saveBtnText.textContent = 'Lưu thay đổi';

                var resData = null;
                var contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    resData = await response.json();
                }

                if (response.ok && resData && resData.success === true) {
                    showAlert(true, (resData && resData.message) ? resData.message : 'Cập nhật hồ sơ cá nhân thành công.');

                    // AC 7: Dữ liệu trên giao diện cập nhật theo dữ liệu mới
                    var d = resData.data || {};
                    var savedFullName = typeof d.fullName === 'string' ? d.fullName : fullName;
                    var savedPhone = typeof d.phone === 'string' ? d.phone : phone;
                    var savedSignature = typeof d.signature === 'string' ? d.signature : signature;

                    inputFullName.value = savedFullName;
                    inputPhone.value = savedPhone;
                    inputSignature.value = savedSignature;
                    inputFullName.defaultValue = savedFullName;
                    inputPhone.defaultValue = savedPhone;
                    inputSignature.defaultValue = savedSignature;
                    signaturePreview.textContent = savedSignature.trim() || '(Chưa thiết lập chữ ký email)';

                    sidebarFullName.textContent = savedFullName;
                    sidebarAvatar.textContent = savedFullName.trim().charAt(0).toUpperCase() || 'U';
                    sidebarPhone.textContent = savedPhone || 'Chưa cập nhật';

                    // Cập nhật tên trên Header nếu có; không thay đổi Email, Team hoặc Role.
                    var headerNameEl = document.querySelector('.crm-header__user-name');
                    if (headerNameEl) headerNameEl.textContent = savedFullName;

                } else {
                    var errorMsg = (resData && resData.message)
                        ? resData.message
                        : 'API cập nhật hồ sơ cá nhân chưa khả dụng. Vui lòng thử lại sau.';
                    if (errorMsg.includes('Số điện thoại')) {
                        inputPhone.classList.add('is-invalid');
                        feedbackPhone.textContent = errorMsg;
                    } else if (errorMsg.includes('Họ và tên')) {
                        inputFullName.classList.add('is-invalid');
                        feedbackName.textContent = errorMsg;
                    } else {
                        showAlert(false, response.ok ? errorMsg : errorMsg + ' (Mã lỗi: ' + response.status + ')');
                    }
                }

            } catch (err) {
                btnSave.disabled = false;
                saveSpinner.style.display = 'none';
                saveBtnText.textContent = 'Lưu thay đổi';
                console.error('Lỗi khi cập nhật hồ sơ:', err);
                showAlert(false, 'Không thể kết nối đến máy chủ backend để cập nhật hồ sơ.');
            }
        });

        // ======================================================================
        // AVATAR MANAGER (CRM-36 / S2-03)
        // Cắt vuông 1:1, bản thu nhỏ preview, validate 2MB/JPG/PNG, đồng bộ Header & Sidebar
        // ======================================================================
        (function initAvatarManager() {
            var avatarModal = document.getElementById('avatarUploadModal');
            var btnOpenAvatarModal = document.getElementById('btnOpenAvatarModal');
            var btnTriggerAvatarModal = document.getElementById('btnTriggerAvatarModal');
            var btnCloseAvatarModal = document.getElementById('btnCloseAvatarModal');
            var btnCancelAvatarModal = document.getElementById('btnCancelAvatarModal');

            var avatarDropzone = document.getElementById('avatarDropzone');
            var avatarFileInput = document.getElementById('avatarFileInput');
            var avatarCropperWrapper = document.getElementById('avatarCropperWrapper');
            var avatarCropStage = document.getElementById('avatarCropStage');
            var avatarCropCanvas = document.getElementById('avatarCropCanvas');
            var avatarZoomSlider = document.getElementById('avatarZoomSlider');
            var btnZoomIn = document.getElementById('btnZoomIn');
            var btnZoomOut = document.getElementById('btnZoomOut');
            var btnResetCrop = document.getElementById('btnResetCrop');
            var btnChangeImage = document.getElementById('btnChangeImage');

            var previewCanvasLg = document.getElementById('previewCanvasLg');
            var previewCanvasMd = document.getElementById('previewCanvasMd');
            var previewCanvasSm = document.getElementById('previewCanvasSm');

            var avatarProgressWrap = document.getElementById('avatarProgressWrap');
            var avatarProgressBarFill = document.getElementById('avatarProgressBarFill');
            var avatarProgressStatus = document.getElementById('avatarProgressStatus');
            var avatarProgressPercent = document.getElementById('avatarProgressPercent');

            var avatarModalErrorAlert = document.getElementById('avatarModalErrorAlert');
            var avatarModalErrorMessage = document.getElementById('avatarModalErrorMessage');

            var btnSaveAvatar = document.getElementById('btnSaveAvatar');
            var avatarSaveSpinner = document.getElementById('avatarSaveSpinner');
            var avatarSaveBtnText = document.getElementById('avatarSaveBtnText');

            var sidebarAvatarImg = document.getElementById('sidebarAvatarImg');
            var sidebarAvatarChar = document.getElementById('sidebarAvatarChar');

            // Giới hạn file: tối đa 2MB (2.097.152 byte) theo AC 1 & AC 3
            var MAX_FILE_SIZE = 2 * 1024 * 1024;
            var ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png'];

            // State quản lý cropper
            var loadedImage = null;
            var rawFile = null;
            var scale = 1;
            var minScale = 1;
            var baseSquareSize = 0;
            var offsetX = 0;
            var offsetY = 0;
            var isDragging = false;
            var startDragX = 0;
            var startDragY = 0;
            var cachedCsrfToken = null;

            function showAvatarError(msg) {
                avatarModalErrorMessage.textContent = msg;
                avatarModalErrorAlert.style.display = 'flex';
            }

            function hideAvatarError() {
                avatarModalErrorAlert.style.display = 'none';
                avatarModalErrorMessage.textContent = '';
            }

            function openModal() {
                hideAvatarError();
                resetCropper();
                avatarModal.style.display = 'flex';
                fetchCsrfToken();
            }

            function closeModal() {
                avatarModal.style.display = 'none';
                resetCropper();
            }

            if (btnOpenAvatarModal) btnOpenAvatarModal.addEventListener('click', openModal);
            if (btnTriggerAvatarModal) btnTriggerAvatarModal.addEventListener('click', openModal);
            if (btnCloseAvatarModal) btnCloseAvatarModal.addEventListener('click', closeModal);
            if (btnCancelAvatarModal) btnCancelAvatarModal.addEventListener('click', closeModal);

            avatarModal.addEventListener('click', function (e) {
                if (e.target === avatarModal) closeModal();
            });

            // Lấy CSRF token từ endpoint avatar metadata
            async function fetchCsrfToken() {
                if (cachedCsrfToken) return cachedCsrfToken;
                try {
                    var resp = await fetch(contextPath + '/api/users/me/avatar');
                    if (resp.ok) {
                        var resBody = await resp.json();
                        if (resBody && resBody.data && resBody.data.csrfToken) {
                            cachedCsrfToken = resBody.data.csrfToken;
                        }
                    }
                } catch (e) {
                    console.info('Không thể lấy csrfToken trước:', e);
                }
                return cachedCsrfToken;
            }

            // Click vào dropzone để mở file dialog
            avatarDropzone.addEventListener('click', function () {
                avatarFileInput.click();
            });

            // Xử lý kéo thả tệp (Drag and Drop)
            avatarDropzone.addEventListener('dragover', function (e) {
                e.preventDefault();
                avatarDropzone.classList.add('dragover');
            });

            avatarDropzone.addEventListener('dragleave', function (e) {
                e.preventDefault();
                avatarDropzone.classList.remove('dragover');
            });

            avatarDropzone.addEventListener('drop', function (e) {
                e.preventDefault();
                avatarDropzone.classList.remove('dragover');
                if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                    handleFileSelection(e.dataTransfer.files[0]);
                }
            });

            avatarFileInput.addEventListener('change', function () {
                if (avatarFileInput.files && avatarFileInput.files.length > 0) {
                    handleFileSelection(avatarFileInput.files[0]);
                }
            });

            btnChangeImage.addEventListener('click', function () {
                resetCropper();
                avatarFileInput.click();
            });

            // AC 1 & AC 3: Validate định dạng và dung lượng tối đa 2MB ngay Client với thông báo Tiếng Việt
            function handleFileSelection(file) {
                hideAvatarError();
                if (!file) return;

                // 1. Kiểm tra đuôi file
                var fileName = file.name || '';
                var ext = fileName.split('.').pop().toLowerCase();
                var isValidExt = ALLOWED_EXTENSIONS.indexOf(ext) !== -1;
                var isValidType = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg';

                if (!isValidExt || (!isValidType && file.type)) {
                    showAvatarError('Định dạng tệp không hợp lệ. Hệ thống chỉ chấp nhận ảnh định dạng JPG, JPEG hoặc PNG.');
                    avatarFileInput.value = '';
                    return;
                }

                // 2. Kiểm tra dung lượng tối đa 2MB (2.097.152 byte)
                if (file.size > MAX_FILE_SIZE) {
                    var sizeMB = (file.size / (1024 * 1024)).toFixed(2);
                    showAvatarError('Dung lượng tệp (' + sizeMB + ' MB) vượt quá giới hạn tối đa 2MB (2.097.152 byte). Vui lòng chọn ảnh nhẹ hơn.');
                    avatarFileInput.value = '';
                    return;
                }

                // 3. Kiểm tra tệp rỗng
                if (file.size === 0) {
                    showAvatarError('Tệp ảnh rỗng hoặc không có dữ liệu. Vui lòng chọn tệp hợp lệ.');
                    avatarFileInput.value = '';
                    return;
                }

                rawFile = file;

                // Đọc file thành Image
                var reader = new FileReader();
                reader.onload = function (e) {
                    var img = new Image();
                    img.onload = function () {
                        // Giới hạn an toàn: không quá 16 triệu pixel (theo quy chuẩn backend)
                        if (img.width * img.height > 16000000) {
                            showAvatarError('Kích thước điểm ảnh quá lớn (vượt quá 16 triệu pixel). Vui lòng chọn ảnh nhỏ hơn.');
                            return;
                        }
                        setupCropper(img);
                    };
                    img.onerror = function () {
                        showAvatarError('Không thể đọc dữ liệu ảnh. Vui lòng chọn tệp ảnh khác.');
                    };
                    img.src = e.target.result;
                };
                reader.onerror = function () {
                    showAvatarError('Đã xảy ra lỗi khi đọc tệp từ thiết bị.');
                };
                reader.readAsDataURL(file);
            }

            // AC 2: Thiết lập Canvas Cropper cắt vuông 1:1
            function setupCropper(img) {
                loadedImage = img;
                avatarDropzone.style.display = 'none';
                avatarCropperWrapper.style.display = 'flex';
                btnSaveAvatar.disabled = false;

                // Chiều dài khung cắt vuông tại giao diện
                var maskSize = 220;
                baseSquareSize = Math.min(img.width, img.height);
                minScale = maskSize / baseSquareSize;
                scale = minScale;

                avatarZoomSlider.min = minScale;
                avatarZoomSlider.max = minScale * 3;
                avatarZoomSlider.step = minScale * 0.05;
                avatarZoomSlider.value = minScale;

                // Căn giữa ảnh mặc định
                offsetX = (maskSize - img.width * scale) / 2;
                offsetY = (maskSize - img.height * scale) / 2;

                avatarCropCanvas.width = maskSize;
                avatarCropCanvas.height = maskSize;

                renderCropAndPreviews();
            }

            function resetCropper() {
                loadedImage = null;
                rawFile = null;
                avatarFileInput.value = '';
                avatarDropzone.style.display = 'block';
                avatarCropperWrapper.style.display = 'none';
                avatarProgressWrap.style.display = 'none';
                btnSaveAvatar.disabled = true;
                hideAvatarError();
            }

            // Vẽ Canvas Crop và 3 bản thu nhỏ Previews (AC 2)
            function renderCropAndPreviews() {
                if (!loadedImage) return;

                var ctx = avatarCropCanvas.getContext('2d');
                var w = avatarCropCanvas.width;
                var h = avatarCropCanvas.height;

                ctx.clearRect(0, 0, w, h);
                ctx.save();
                ctx.drawImage(loadedImage, offsetX, offsetY, loadedImage.width * scale, loadedImage.height * scale);
                ctx.restore();

                // Đồng bộ 3 bản thu nhỏ (AC 2)
                drawThumbnail(previewCanvasLg, 64);
                drawThumbnail(previewCanvasMd, 44);
                drawThumbnail(previewCanvasSm, 32);
            }

            function drawThumbnail(targetCanvas, size) {
                var ctx = targetCanvas.getContext('2d');
                ctx.clearRect(0, 0, size, size);
                ctx.drawImage(avatarCropCanvas, 0, 0, avatarCropCanvas.width, avatarCropCanvas.height, 0, 0, size, size);
            }

            // Điều khiển Zoom Slider
            avatarZoomSlider.addEventListener('input', function () {
                var newScale = parseFloat(avatarZoomSlider.value);
                zoomAtCenter(newScale);
            });

            btnZoomIn.addEventListener('click', function () {
                var step = minScale * 0.2;
                var newScale = Math.min(parseFloat(avatarZoomSlider.max), scale + step);
                avatarZoomSlider.value = newScale;
                zoomAtCenter(newScale);
            });

            btnZoomOut.addEventListener('click', function () {
                var step = minScale * 0.2;
                var newScale = Math.max(parseFloat(avatarZoomSlider.min), scale - step);
                avatarZoomSlider.value = newScale;
                zoomAtCenter(newScale);
            });

            btnResetCrop.addEventListener('click', function () {
                if (!loadedImage) return;
                scale = minScale;
                avatarZoomSlider.value = minScale;
                var maskSize = avatarCropCanvas.width;
                offsetX = (maskSize - loadedImage.width * scale) / 2;
                offsetY = (maskSize - loadedImage.height * scale) / 2;
                renderCropAndPreviews();
            });

            function zoomAtCenter(newScale) {
                if (!loadedImage) return;
                var maskSize = avatarCropCanvas.width;
                var centerX = maskSize / 2;
                var centerY = maskSize / 2;

                var ratio = newScale / scale;
                offsetX = centerX - (centerX - offsetX) * ratio;
                offsetY = centerY - (centerY - offsetY) * ratio;
                scale = newScale;

                clampOffset();
                renderCropAndPreviews();
            }

            function clampOffset() {
                if (!loadedImage) return;
                var maskSize = avatarCropCanvas.width;
                var imgW = loadedImage.width * scale;
                var imgH = loadedImage.height * scale;

                if (imgW >= maskSize) {
                    if (offsetX > 0) offsetX = 0;
                    if (offsetX + imgW < maskSize) offsetX = maskSize - imgW;
                } else {
                    offsetX = (maskSize - imgW) / 2;
                }

                if (imgH >= maskSize) {
                    if (offsetY > 0) offsetY = 0;
                    if (offsetY + imgH < maskSize) offsetY = maskSize - imgH;
                } else {
                    offsetY = (maskSize - imgH) / 2;
                }
            }

            // Xử lý kéo chuột để di chuyển vùng crop (Pan)
            avatarCropStage.addEventListener('mousedown', function (e) {
                if (!loadedImage) return;
                isDragging = true;
                startDragX = e.clientX - offsetX;
                startDragY = e.clientY - offsetY;
            });

            window.addEventListener('mousemove', function (e) {
                if (!isDragging || !loadedImage) return;
                offsetX = e.clientX - startDragX;
                offsetY = e.clientY - startDragY;
                clampOffset();
                renderCropAndPreviews();
            });

            window.addEventListener('mouseup', function () {
                isDragging = false;
            });

            // Touch events cho mobile
            avatarCropStage.addEventListener('touchstart', function (e) {
                if (!loadedImage || !e.touches[0]) return;
                isDragging = true;
                startDragX = e.touches[0].clientX - offsetX;
                startDragY = e.touches[0].clientY - offsetY;
            }, { passive: true });

            window.addEventListener('touchmove', function (e) {
                if (!isDragging || !loadedImage || !e.touches[0]) return;
                offsetX = e.touches[0].clientX - startDragX;
                offsetY = e.touches[0].clientY - startDragY;
                clampOffset();
                renderCropAndPreviews();
            }, { passive: true });

            window.addEventListener('touchend', function () {
                isDragging = false;
            });

            // Xuất ảnh vuông 512x512 chất lượng cao (AC 2)
            function generateCroppedBlob() {
                return new Promise(function (resolve, reject) {
                    if (!loadedImage) {
                        reject(new Error('Chưa có ảnh được chọn.'));
                        return;
                    }

                    var exportCanvas = document.createElement('canvas');
                    exportCanvas.width = 512;
                    exportCanvas.height = 512;
                    var ctx = exportCanvas.getContext('2d');

                    var maskSize = avatarCropCanvas.width;
                    var factor = 512 / maskSize;

                    ctx.save();
                    ctx.drawImage(
                        loadedImage,
                        offsetX * factor,
                        offsetY * factor,
                        loadedImage.width * scale * factor,
                        loadedImage.height * scale * factor
                    );
                    ctx.restore();

                    exportCanvas.toBlob(function (blob) {
                        if (blob) resolve(blob);
                        else reject(new Error('Không thể tạo file ảnh từ canvas.'));
                    }, 'image/png');
                });
            }

            // Gửi upload FormData lên backend với Progress Bar (AC 1, AC 2, AC 4)
            btnSaveAvatar.addEventListener('click', async function () {
                if (!loadedImage) return;
                hideAvatarError();

                btnSaveAvatar.disabled = true;
                avatarSaveSpinner.style.display = 'inline-block';
                avatarSaveBtnText.textContent = 'Đang lưu ảnh...';

                avatarProgressWrap.style.display = 'block';
                avatarProgressBarFill.style.width = '10%';
                avatarProgressPercent.textContent = '10%';
                avatarProgressStatus.textContent = 'Đang cắt vuông và chuẩn bị tệp...';

                try {
                    var croppedBlob = await generateCroppedBlob();
                    var csrfToken = await fetchCsrfToken();

                    avatarProgressBarFill.style.width = '30%';
                    avatarProgressPercent.textContent = '30%';
                    avatarProgressStatus.textContent = 'Đang gửi ảnh lên máy chủ...';

                    var formData = new FormData();
                    formData.append('avatar', croppedBlob, 'avatar.png');
                    if (csrfToken) formData.append('csrfToken', csrfToken);

                    // Upload via XMLHttpRequest để bắt sự kiện tiến trình (Progress Bar)
                    var uploadSuccess = false;
                    var uploadResult = null;

                    // Thử endpoint chuẩn Jira: /api/users/profile/avatar trước, sau đó fallback /api/users/me/avatar
                    var primaryEndpoint = contextPath + '/api/users/profile/avatar';
                    var fallbackEndpoint = contextPath + '/api/users/me/avatar';

                    uploadResult = await doUploadXHR(primaryEndpoint, formData, csrfToken);

                    // Nếu 404 hoặc 405 (do backend đã map /api/users/me/avatar theo CRM-36.md), thử fallback
                    if (uploadResult.status === 404 || uploadResult.status === 405) {
                        uploadResult = await doUploadXHR(fallbackEndpoint, formData, csrfToken);
                    }

                    if (uploadResult.ok) {
                        uploadSuccess = true;
                    } else {
                        // Xử lý lỗi trả về từ máy chủ
                        var errorMsg = uploadResult.message || 'Không thể lưu ảnh đại diện trên máy chủ.';
                        showAvatarError(errorMsg);
                        btnSaveAvatar.disabled = false;
                        avatarSaveSpinner.style.display = 'none';
                        avatarSaveBtnText.textContent = 'Lưu ảnh đại diện';
                        avatarProgressWrap.style.display = 'none';
                        return;
                    }

                    // AC 4: CẬP NHẬT ĐỒNG BỘ AVATAR TRÊN HEADER & SIDEBAR MÀ KHÔNG RELOAD TRANG
                    avatarProgressBarFill.style.width = '100%';
                    avatarProgressPercent.textContent = '100%';
                    avatarProgressStatus.textContent = 'Cập nhật thành công!';

                    var timestamp = Date.now();
                    var newThumbnailUrl = contextPath + '/profile/avatar/thumbnail?t=' + timestamp;
                    var newImageUrl = contextPath + '/profile/avatar/image?t=' + timestamp;

                    // 1. Cập nhật Avatar trên trang Hồ sơ cá nhân (Profile page)
                    if (sidebarAvatarImg) {
                        sidebarAvatarImg.src = newImageUrl;
                        sidebarAvatarImg.style.display = 'block';
                    }
                    if (sidebarAvatarChar) {
                        sidebarAvatarChar.style.display = 'none';
                    }

                    // 2. Cập nhật Avatar trên thanh Header
                    var headerAvatarImgs = document.querySelectorAll('.crm-header__avatar img, #crmHeaderUserWidget img');
                    headerAvatarImgs.forEach(function (img) {
                        img.src = newThumbnailUrl;
                        img.hidden = false;
                        img.style.display = 'block';
                        if (img.previousElementSibling) {
                            img.previousElementSibling.hidden = true;
                            img.previousElementSibling.style.display = 'none';
                        }
                    });

                    // 3. Cập nhật Avatar trên thanh Sidebar
                    var sidebarAvatarEl = document.getElementById('crmSidebarAvatarText');
                    if (sidebarAvatarEl) {
                        sidebarAvatarEl.innerHTML = '<img src="' + newThumbnailUrl + '" alt="Ảnh đại diện" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">';
                    }

                    setTimeout(function () {
                        closeModal();
                        showAlert(true, 'Cập nhật ảnh đại diện người dùng thành công!');
                    }, 400);

                } catch (err) {
                    console.error('Lỗi khi tải ảnh lên:', err);
                    showAvatarError('Đã xảy ra lỗi kết nối với máy chủ khi tải ảnh lên.');
                    btnSaveAvatar.disabled = false;
                    avatarSaveSpinner.style.display = 'none';
                    avatarSaveBtnText.textContent = 'Lưu ảnh đại diện';
                    avatarProgressWrap.style.display = 'none';
                }
            });

            function doUploadXHR(url, formData, csrfToken) {
                return new Promise(function (resolve) {
                    var xhr = new XMLHttpRequest();
                    xhr.open('POST', url, true);
                    if (csrfToken) {
                        xhr.setRequestHeader('X-CSRF-Token', csrfToken);
                    }
                    xhr.setRequestHeader('Accept', 'application/json');

                    xhr.upload.onprogress = function (e) {
                        if (e.lengthComputable) {
                            var pct = Math.round(30 + (e.loaded / e.total) * 60);
                            avatarProgressBarFill.style.width = pct + '%';
                            avatarProgressPercent.textContent = pct + '%';
                        }
                    };

                    xhr.onload = function () {
                        var isOk = xhr.status >= 200 && xhr.status < 300;
                        var msg = '';
                        try {
                            var data = JSON.parse(xhr.responseText);
                            msg = data.message || '';
                        } catch (e) {
                            msg = xhr.statusText;
                        }
                        resolve({ ok: isOk, status: xhr.status, message: msg });
                    };

                    xhr.onerror = function () {
                        resolve({ ok: false, status: 0, message: 'Lỗi mạng hoặc không thể kết nối tới server.' });
                    };

                    xhr.send(formData);
                });
            }

            // Tự động kiểm tra trạng thái avatar người dùng khi nạp trang
            (async function checkExistingAvatar() {
                try {
                    var resp = await fetch(contextPath + '/api/users/me/avatar');
                    if (resp.ok) {
                        var body = await resp.json();
                        if (body && body.data && body.data.hasAvatar) {
                            if (sidebarAvatarImg) {
                                sidebarAvatarImg.src = contextPath + '/profile/avatar/image?t=' + Date.now();
                                sidebarAvatarImg.style.display = 'block';
                            }
                            if (sidebarAvatarChar) {
                                sidebarAvatarChar.style.display = 'none';
                            }
                        }
                    }
                } catch (ignored) {}
            })();

        })();
    });
    </script>
</body>
</html>
