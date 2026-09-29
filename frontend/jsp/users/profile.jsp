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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS module Users -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/users.css">
    <style>
        .profile-readonly-badge {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 0.75rem;
            color: #64748b;
            background: #f1f5f9;
            padding: 2px 8px;
            border-radius: 4px;
            margin-left: 6px;
        }
        .signature-preview-box {
            margin-top: 10px;
            padding: 14px 16px;
            background-color: #f8fafc;
            border: 1px dashed #cbd5e1;
            border-radius: 8px;
            font-size: 0.88rem;
            color: #334155;
            white-space: pre-wrap;
            min-height: 80px;
            line-height: 1.5;
        }
        .signature-preview-title {
            font-size: 0.8rem;
            font-weight: 600;
            color: #64748b;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: 6px;
        }
    </style>
</head>
<body class="crm-body">

    <!-- Header dùng chung -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Nội dung chính -->
        <main class="user-page" id="profileApp" role="main">
            <div class="user-container">

                <!-- Breadcrumb -->
                <nav class="user-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Tài khoản</span>
                    <span class="separator">/</span>
                    <span class="active">Hồ sơ cá nhân</span>
                </nav>

                <!-- Header -->
                <header class="user-header">
                    <div class="user-header-info">
                        <h1>Hồ sơ cá nhân &amp; Chữ ký email</h1>
                        <p>Xem thông tin tài khoản, cập nhật số điện thoại liên hệ và chữ ký số dùng khi gửi báo giá cho khách hàng.</p>
                    </div>
                    <div class="user-header-badges">
                        <span class="user-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                            S2-02 / CRM-35
                        </span>
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
                        <div class="user-card">
                            <div class="user-info-summary">
                                <div class="user-avatar-lg" id="sidebarAvatar" aria-hidden="true">
                                    <%= avatarChar %>
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
                        <div class="user-card">
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
                                <!-- Phần 1: Các trường cố định (Readonly / Disabled) theo AC 3 & AC 4 -->
                                <div style="margin-bottom: 22px; padding: 14px 16px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;">
                                    <h3 style="font-size: 0.92rem; font-weight: 700; color: #334155; margin: 0 0 12px 0;">
                                        Thông tin hệ thống do Quản trị viên quản lý (Chỉ đọc)
                                    </h3>

                                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px;">
                                        <div class="modal-field" style="margin: 0;">
                                            <label class="modal-label" for="readonlyEmail">
                                                Địa chỉ Email
                                                <span class="profile-readonly-badge">
                                                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                                    Chỉ đọc
                                                </span>
                                            </label>
                                            <input type="email" id="readonlyEmail" class="modal-input" value="<%= email %>" readonly disabled style="background-color: #f1f5f9; cursor: not-allowed; color: #475569;">
                                        </div>

                                        <div class="modal-field" style="margin: 0;">
                                            <label class="modal-label" for="readonlyRoles">
                                                Vai trò hệ thống
                                                <span class="profile-readonly-badge">
                                                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                                    Chỉ đọc
                                                </span>
                                            </label>
                                            <input type="text" id="readonlyRoles" class="modal-input" value="<%= rolesText %>" readonly disabled style="background-color: #f1f5f9; cursor: not-allowed; color: #475569;">
                                        </div>

                                        <div class="modal-field" style="margin: 0;">
                                            <label class="modal-label" for="readonlyTeam">
                                                Nhóm kinh doanh
                                                <span class="profile-readonly-badge">
                                                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                                    Chỉ đọc
                                                </span>
                                            </label>
                                            <input type="text" id="readonlyTeam" class="modal-input" value="<%= teamName %>" readonly disabled style="background-color: #f1f5f9; cursor: not-allowed; color: #475569;">
                                        </div>
                                    </div>
                                </div>

                                <!-- Phần 2: Các trường được phép chỉnh sửa theo AC 2 -->
                                <div class="modal-field">
                                    <label for="profileFullName" class="modal-label">
                                        Họ và tên <span class="modal-required">*</span>
                                    </label>
                                    <input type="text" id="profileFullName" name="fullName" class="modal-input"
                                           value="<%= fullName %>" placeholder="Ví dụ: Nguyễn Văn A" required>
                                    <div class="modal-field-feedback" id="feedbackProfileFullName" style="display: block; color: #dc2626; font-size: 0.83rem; margin-top: 4px;"></div>
                                </div>

                                <div class="modal-field">
                                    <label for="profilePhone" class="modal-label">
                                        Số điện thoại di động
                                        <span style="font-weight: normal; font-size: 0.82rem; color: #64748b;">(Định dạng Việt Nam, ví dụ: 0912345678 hoặc +84912345678)</span>
                                    </label>
                                    <input type="tel" id="profilePhone" name="phone" class="modal-input"
                                           value="<%= phone %>" placeholder="Ví dụ: 0912345678">
                                    <div class="modal-field-feedback" id="feedbackProfilePhone" style="display: block; color: #dc2626; font-size: 0.83rem; margin-top: 4px;"></div>
                                </div>

                                <div class="modal-field">
                                    <label for="profileSignature" class="modal-label">
                                        Chữ ký Email (Email Signature)
                                        <span style="font-weight: normal; font-size: 0.82rem; color: #64748b;">(Tự động chèn ở cuối báo giá gửi khách)</span>
                                    </label>
                                    <textarea id="profileSignature" name="signature" class="modal-input" rows="5"
                                              placeholder="Ví dụ:&#10;Trân trọng,&#10;<%= !fullName.isEmpty() ? fullName : "Nguyễn Văn A" %> - Bộ phận Kinh doanh&#10;Công ty CRM ICTU&#10;SĐT: <%= !phone.isEmpty() ? phone : "0912345678" %> | Email: <%= !email.isEmpty() ? email : "email@example.com" %>"><%= signature %></textarea>
                                    
                                    <div class="signature-preview-title" style="margin-top: 10px;">Xem trước chữ ký email</div>
                                    <div class="signature-preview-box" id="signaturePreview"><%= signature.isEmpty() ? "(Chưa thiết lập chữ ký email)" : signature %></div>
                                </div>

                                <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; padding-top: 18px; border-top: 1px solid #e2e8f0;">
                                    <button type="reset" class="btn btn-secondary" id="btnResetProfile">Đặt lại</button>
                                    <button type="submit" class="btn btn-primary" id="btnSaveProfile">
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
    });
    </script>
</body>
</html>
