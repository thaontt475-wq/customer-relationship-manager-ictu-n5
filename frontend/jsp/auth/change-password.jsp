<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi mật khẩu - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">

    <!-- CSS riêng biệt của module Đổi mật khẩu (CRM-24) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/change-password.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Đổi mật khẩu -->
        <main class="change-password-page" id="changePasswordApp">
            <div class="cp-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="cp-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/dashboard">CRM</a>
                    <span class="separator">/</span>
                    <span>Tài khoản</span>
                    <span class="separator">/</span>
                    <span class="active">Đổi mật khẩu</span>
                </nav>

                <!-- Header màn hình -->
                <header class="cp-header">
                    <div class="cp-header-info">
                        <h1>Đổi mật khẩu</h1>
                        <p>Cập nhật mật khẩu định kỳ giúp bảo vệ tài khoản và đảm bảo an toàn cho dữ liệu khách hàng.</p>
                    </div>
                    <div class="cp-header-badges">
                        <span class="cp-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                            </svg>
                            S1-04 / CRM-24
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alert / Banner) -->
                <div class="cp-alerts-area" id="cpAlertsArea" aria-live="polite">
                    <!-- Banner thông báo lỗi -->
                    <div class="cp-alert cp-alert-danger" id="cpErrorAlert" style="display: none;" role="alert">
                        <svg class="cp-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="cp-alert-content">
                            <div class="cp-alert-title" id="cpErrorTitle">Đã xảy ra lỗi</div>
                            <p class="cp-alert-msg" id="cpErrorMessage"></p>
                        </div>
                        <button type="button" class="cp-alert-close" id="cpErrorCloseBtn" aria-label="Đóng thông báo lỗi">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </div>

                    <!-- Banner thông báo thành công -->
                    <div class="cp-alert cp-alert-success" id="cpSuccessAlert" style="display: none;" role="status">
                        <svg class="cp-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="cp-alert-content">
                            <div class="cp-alert-title" id="cpSuccessTitle">Thao tác thành công</div>
                            <p class="cp-alert-msg" id="cpSuccessMessage">Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được thu hồi.</p>
                            <p class="cp-alert-msg" id="cpRedirectNotice" style="margin-top: 6px; font-weight: 600; color: #047857;"></p>
                        </div>
                    </div>
                </div>

                <!-- Bố cục lưới 2 cột: Form đổi mật khẩu & Hướng dẫn bảo mật -->
                <div class="cp-grid">

                    <!-- Cột 1: Form đổi mật khẩu -->
                    <div class="cp-card">
                        <div class="cp-card-header">
                            <h2 class="cp-card-title">
                                <svg class="cp-card-title-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M21 2l-2 2m-1.5 1.5L14 9l-1.5-1.5L11 9l-1.5-1.5L8 9 3 14l3 3 5-5 1.5 1.5L14 12l1.5 1.5L17 12l1.5 1.5 2-2z"></path>
                                    <circle cx="7.5" cy="16.5" r="1.5"></circle>
                                </svg>
                                Thiết lập mật khẩu mới
                            </h2>
                            <p class="cp-card-desc">Vui lòng điền đầy đủ thông tin bên dưới để tiến hành đổi mật khẩu đăng nhập.</p>
                        </div>

                        <form class="cp-form" id="changePasswordForm" method="post" novalidate>

                            <!-- Trường 1: Mật khẩu hiện tại -->
                            <div class="cp-field">
                                <label for="currentPassword" class="cp-label">
                                    <span>Mật khẩu hiện tại <span class="cp-required" aria-hidden="true">*</span></span>
                                </label>
                                <div class="cp-input-wrap">
                                    <svg class="cp-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                    </svg>
                                    <input type="password" id="currentPassword" name="currentPassword" class="cp-input"
                                           placeholder="Nhập mật khẩu hiện tại của bạn"
                                           autocomplete="current-password" required>
                                    <button type="button" class="cp-toggle-pwd" data-target="currentPassword"
                                            aria-label="Hiện mật khẩu hiện tại" title="Hiện mật khẩu">
                                        <svg class="icon-eye" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                            <circle cx="12" cy="12" r="3"></circle>
                                        </svg>
                                        <svg class="icon-eye-off" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: none;" aria-hidden="true">
                                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                            <line x1="1" y1="1" x2="23" y2="23"></line>
                                        </svg>
                                    </button>
                                </div>
                                <div class="cp-field-feedback text-danger" id="currentPasswordFeedback"></div>
                            </div>

                            <!-- Trường 2: Mật khẩu mới -->
                            <div class="cp-field">
                                <label for="newPassword" class="cp-label">
                                    <span>Mật khẩu mới <span class="cp-required" aria-hidden="true">*</span></span>
                                </label>
                                <div class="cp-input-wrap">
                                    <svg class="cp-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M12 2a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1V7a5 5 0 0 0-5-5z"></path>
                                    </svg>
                                    <input type="password" id="newPassword" name="newPassword" class="cp-input"
                                           placeholder="Tối thiểu 8 ký tự, gồm cả chữ và số"
                                           autocomplete="new-password" minlength="8" required>
                                    <button type="button" class="cp-toggle-pwd" data-target="newPassword"
                                            aria-label="Hiện mật khẩu mới" title="Hiện mật khẩu">
                                        <svg class="icon-eye" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                            <circle cx="12" cy="12" r="3"></circle>
                                        </svg>
                                        <svg class="icon-eye-off" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: none;" aria-hidden="true">
                                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                            <line x1="1" y1="1" x2="23" y2="23"></line>
                                        </svg>
                                    </button>
                                </div>
                                <div class="cp-field-feedback text-danger" id="newPasswordFeedback"></div>

                                <!-- Password Strength Meter (Thanh đo độ mạnh mật khẩu thời gian thực) -->
                                <div class="cp-strength-card" id="cpStrengthCard">
                                    <div class="cp-strength-header">
                                        <span class="cp-strength-title">Độ mạnh mật khẩu:</span>
                                        <span class="cp-strength-status" id="cpStrengthLabel">Chưa nhập</span>
                                    </div>
                                    <div class="cp-strength-bars" aria-hidden="true">
                                        <div class="cp-strength-bar" id="cpBar1"></div>
                                        <div class="cp-strength-bar" id="cpBar2"></div>
                                        <div class="cp-strength-bar" id="cpBar3"></div>
                                    </div>
                                    <!-- Tiêu chí mật khẩu trực quan -->
                                    <ul class="cp-criteria-list" aria-label="Tiêu chí mật khẩu">
                                        <li class="cp-criterion-item" id="critLength">
                                            <svg class="cp-criterion-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                <circle cx="12" cy="12" r="9"></circle>
                                            </svg>
                                            <span>Tối thiểu 8 ký tự</span>
                                        </li>
                                        <li class="cp-criterion-item" id="critLetterNumber">
                                            <svg class="cp-criterion-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                <circle cx="12" cy="12" r="9"></circle>
                                            </svg>
                                            <span>Bao gồm cả chữ cái và chữ số</span>
                                        </li>
                                        <li class="cp-criterion-item" id="critNotOld">
                                            <svg class="cp-criterion-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                <circle cx="12" cy="12" r="9"></circle>
                                            </svg>
                                            <span>Không trùng với mật khẩu hiện tại</span>
                                        </li>
                                    </ul>
                                </div>
                            </div>

                            <!-- Trường 3: Xác nhận mật khẩu mới -->
                            <div class="cp-field">
                                <label for="confirmPassword" class="cp-label">
                                    <span>Xác nhận mật khẩu mới <span class="cp-required" aria-hidden="true">*</span></span>
                                </label>
                                <div class="cp-input-wrap">
                                    <svg class="cp-input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="8.5" cy="7.5" r="4"></circle>
                                        <polyline points="17 11 19 13 23 9"></polyline>
                                    </svg>
                                    <input type="password" id="confirmPassword" name="confirmPassword" class="cp-input"
                                           placeholder="Nhập lại mật khẩu mới vừa đặt"
                                           autocomplete="new-password" minlength="8" required>
                                    <button type="button" class="cp-toggle-pwd" data-target="confirmPassword"
                                            aria-label="Hiện xác nhận mật khẩu" title="Hiện mật khẩu">
                                        <svg class="icon-eye" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                            <circle cx="12" cy="12" r="3"></circle>
                                        </svg>
                                        <svg class="icon-eye-off" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: none;" aria-hidden="true">
                                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                            <line x1="1" y1="1" x2="23" y2="23"></line>
                                        </svg>
                                    </button>
                                </div>
                                <div class="cp-field-feedback" id="confirmPasswordFeedback"></div>
                            </div>

                            <!-- Nút thao tác -->
                            <div class="cp-actions">
                                <button type="reset" class="cp-btn cp-btn-secondary" id="cpResetBtn">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <polyline points="1 4 1 10 7 10"></polyline>
                                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                    </svg>
                                    Nhập lại
                                </button>
                                <button type="submit" class="cp-btn cp-btn-primary" id="cpSubmitBtn">
                                    <span class="cp-spinner" id="cpSubmitSpinner" aria-hidden="true"></span>
                                    <svg id="cpSubmitIcon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                                        <polyline points="17 21 17 13 7 13 7 21"></polyline>
                                        <polyline points="7 3 7 8 15 8"></polyline>
                                    </svg>
                                    <span id="cpSubmitText">Đổi mật khẩu</span>
                                </button>
                            </div>

                        </form>
                    </div>

                    <!-- Cột 2: Thẻ hướng dẫn bảo mật & Lưu ý -->
                    <aside class="cp-tips-card" aria-label="Hướng dẫn bảo mật tài khoản">
                        <div class="cp-tips-header">
                            <div class="cp-tips-icon-wrap" aria-hidden="true">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                                </svg>
                            </div>
                            <h2>Nguyên tắc bảo mật</h2>
                        </div>

                        <ul class="cp-tips-list">
                            <li class="cp-tip-item">
                                <span class="cp-tip-dot" aria-hidden="true"></span>
                                <span>Bắt buộc nhập đúng mật khẩu hiện tại để xác thực chủ tài khoản.</span>
                            </li>
                            <li class="cp-tip-item">
                                <span class="cp-tip-dot" aria-hidden="true"></span>
                                <span>Mật khẩu mới tối thiểu <strong>8 ký tự</strong> và phải có cả <strong>chữ cái và chữ số</strong>.</span>
                            </li>
                            <li class="cp-tip-item">
                                <span class="cp-tip-dot" aria-hidden="true"></span>
                                <span>Không sử dụng lại mật khẩu cũ hoặc thông tin dễ đoán như ngày sinh, số điện thoại.</span>
                            </li>
                            <li class="cp-tip-item">
                                <span class="cp-tip-dot" aria-hidden="true"></span>
                                <span>Khuyến khích kết hợp chữ hoa, chữ thường và ký tự đặc biệt (@, #, $, %, ...) để đạt độ mạnh tối ưu.</span>
                            </li>
                        </ul>

                        <div class="cp-notice-box" role="note">
                            <svg class="cp-notice-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="16" x2="12" y2="12"></line>
                                <line x1="12" y1="8" x2="12.01" y2="8"></line>
                            </svg>
                            <div>
                                <strong>Lưu ý bảo mật:</strong> Để đảm bảo an toàn tuyệt đối, ngay sau khi đổi mật khẩu thành công, toàn bộ các phiên đăng nhập khác của bạn trên mọi thiết bị sẽ tự động được thu hồi.
                            </div>
                        </div>
                    </aside>

                </div>

            </div>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- Script xử lý logic tương tác, validation & gọi API Contract CRM-24 -->
    <script>
    document.addEventListener('DOMContentLoaded', function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}';
        var form = document.getElementById('changePasswordForm');
        var currentPwdInput = document.getElementById('currentPassword');
        var newPwdInput = document.getElementById('newPassword');
        var confirmPwdInput = document.getElementById('confirmPassword');

        var currentFeedback = document.getElementById('currentPasswordFeedback');
        var newFeedback = document.getElementById('newPasswordFeedback');
        var confirmFeedback = document.getElementById('confirmPasswordFeedback');

        var errorAlert = document.getElementById('cpErrorAlert');
        var errorTitle = document.getElementById('cpErrorTitle');
        var errorMessage = document.getElementById('cpErrorMessage');
        var errorCloseBtn = document.getElementById('cpErrorCloseBtn');

        var successAlert = document.getElementById('cpSuccessAlert');
        var successMessage = document.getElementById('cpSuccessMessage');
        var redirectNotice = document.getElementById('cpRedirectNotice');

        var submitBtn = document.getElementById('cpSubmitBtn');
        var submitSpinner = document.getElementById('cpSubmitSpinner');
        var submitIcon = document.getElementById('cpSubmitIcon');
        var submitText = document.getElementById('cpSubmitText');
        var resetBtn = document.getElementById('cpResetBtn');

        var bar1 = document.getElementById('cpBar1');
        var bar2 = document.getElementById('cpBar2');
        var bar3 = document.getElementById('cpBar3');
        var strengthLabel = document.getElementById('cpStrengthLabel');

        var critLength = document.getElementById('critLength');
        var critLetterNumber = document.getElementById('critLetterNumber');
        var critNotOld = document.getElementById('critNotOld');

        var checkSvgHtml = '<svg class="cp-criterion-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"></polyline></svg>';
        var circleSvgHtml = '<svg class="cp-criterion-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"></circle></svg>';

        // 1. Chức năng Toggle Ẩn / Hiện mật khẩu (Eye Icon)
        var toggleButtons = document.querySelectorAll('.cp-toggle-pwd');
        toggleButtons.forEach(function (btn) {
            btn.addEventListener('click', function () {
                var targetId = btn.getAttribute('data-target');
                var targetInput = document.getElementById(targetId);
                if (!targetInput) return;

                var eyeIcon = btn.querySelector('.icon-eye');
                var eyeOffIcon = btn.querySelector('.icon-eye-off');

                if (targetInput.type === 'password') {
                    targetInput.type = 'text';
                    if (eyeIcon) eyeIcon.style.display = 'none';
                    if (eyeOffIcon) eyeOffIcon.style.display = 'block';
                    btn.setAttribute('aria-label', 'Ẩn mật khẩu');
                    btn.setAttribute('title', 'Ẩn mật khẩu');
                } else {
                    targetInput.type = 'password';
                    if (eyeIcon) eyeIcon.style.display = 'block';
                    if (eyeOffIcon) eyeOffIcon.style.display = 'none';
                    btn.setAttribute('aria-label', 'Hiện mật khẩu');
                    btn.setAttribute('title', 'Hiện mật khẩu');
                }
            });
        });

        // 2. Hàm đánh giá độ mạnh mật khẩu (Password Strength Meter) theo thời gian thực
        function evaluatePasswordStrength(val, oldVal) {
            if (!val || val.length === 0) {
                return {
                    score: 0,
                    label: 'Chưa nhập',
                    hasLength: false,
                    hasLetterAndNumber: false,
                    notOld: true
                };
            }

            var hasLength = val.length >= 8;
            var hasLetter = /[a-zA-Z]/.test(val);
            var hasNumber = /[0-9]/.test(val);
            var hasLetterAndNumber = hasLetter && hasNumber;
            var notOld = (oldVal && oldVal.length > 0) ? (val !== oldVal) : true;

            var hasUpper = /[A-Z]/.test(val);
            var hasLower = /[a-z]/.test(val);
            var hasSpecial = /[^a-zA-Z0-9]/.test(val);

            var score = 1; // Mặc định Yếu nếu đã nhập
            var label = 'Yếu';

            if (hasLength && hasLetterAndNumber) {
                // Đã đạt chuẩn cơ bản
                score = 2;
                label = 'Trung bình';

                var bonusPoints = 0;
                if (hasUpper && hasLower) bonusPoints++;
                if (hasSpecial) bonusPoints++;
                if (val.length >= 10) bonusPoints++;

                if (bonusPoints >= 2) {
                    score = 3;
                    label = 'Mạnh';
                }
            }

            return {
                score: score,
                label: label,
                hasLength: hasLength,
                hasLetterAndNumber: hasLetterAndNumber,
                notOld: notOld
            };
        }

        // Cập nhật giao diện thanh đo độ mạnh mật khẩu
        function updateStrengthMeter() {
            var val = newPwdInput.value;
            var oldVal = currentPwdInput.value;
            var result = evaluatePasswordStrength(val, oldVal);

            // Cập nhật nhãn trạng thái
            strengthLabel.textContent = result.label;
            strengthLabel.className = 'cp-strength-status';
            if (result.score === 1) strengthLabel.classList.add('status-weak');
            else if (result.score === 2) strengthLabel.classList.add('status-medium');
            else if (result.score === 3) strengthLabel.classList.add('status-strong');

            // Reset thanh bar
            bar1.className = 'cp-strength-bar';
            bar2.className = 'cp-strength-bar';
            bar3.className = 'cp-strength-bar';

            if (result.score === 1) {
                bar1.classList.add('is-weak');
            } else if (result.score === 2) {
                bar1.classList.add('is-medium');
                bar2.classList.add('is-medium');
            } else if (result.score === 3) {
                bar1.classList.add('is-strong');
                bar2.classList.add('is-strong');
                bar3.classList.add('is-strong');
            }

            // Cập nhật checklist tiêu chí
            updateCriterion(critLength, result.hasLength);
            updateCriterion(critLetterNumber, result.hasLetterAndNumber);
            updateCriterion(critNotOld, (val.length > 0 && oldVal.length > 0) ? (val !== oldVal) : false);
        }

        function updateCriterion(elem, isMet) {
            if (!elem) return;
            var iconContainer = elem.querySelector('.cp-criterion-icon');
            if (isMet) {
                elem.classList.add('is-met');
                if (iconContainer) {
                    iconContainer.outerHTML = checkSvgHtml;
                }
            } else {
                elem.classList.remove('is-met');
                if (iconContainer) {
                    iconContainer.outerHTML = circleSvgHtml;
                }
            }
        }

        // Kiểm tra khớp mật khẩu xác nhận
        function checkConfirmPassword() {
            var newPass = newPwdInput.value;
            var confirmPass = confirmPwdInput.value;

            if (!confirmPass) {
                confirmPwdInput.classList.remove('is-valid', 'is-invalid');
                confirmFeedback.textContent = '';
                confirmFeedback.className = 'cp-field-feedback';
                return;
            }

            if (confirmPass === newPass) {
                confirmPwdInput.classList.remove('is-invalid');
                confirmPwdInput.classList.add('is-valid');
                confirmFeedback.textContent = 'Mật khẩu xác nhận trùng khớp.';
                confirmFeedback.className = 'cp-field-feedback text-success';
            } else {
                confirmPwdInput.classList.remove('is-valid');
                confirmPwdInput.classList.add('is-invalid');
                confirmFeedback.textContent = 'Mật khẩu xác nhận không trùng khớp.';
                confirmFeedback.className = 'cp-field-feedback text-danger';
            }
        }

        // Lắng nghe sự kiện người dùng gõ
        newPwdInput.addEventListener('input', function () {
            updateStrengthMeter();
            if (confirmPwdInput.value) {
                checkConfirmPassword();
            }
            if (newPwdInput.value) {
                newPwdInput.classList.remove('is-invalid');
                newFeedback.textContent = '';
            }
        });

        currentPwdInput.addEventListener('input', function () {
            updateStrengthMeter();
            if (currentPwdInput.value) {
                currentPwdInput.classList.remove('is-invalid');
                currentFeedback.textContent = '';
            }
        });

        confirmPwdInput.addEventListener('input', checkConfirmPassword);

        // 3. Quản lý hiển thị Alert / Banner
        function showError(title, msg) {
            errorTitle.textContent = title || 'Lỗi';
            errorMessage.textContent = msg || 'Đã xảy ra lỗi.';
            errorAlert.style.display = 'flex';
            successAlert.style.display = 'none';
            errorAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        function showSuccess(msg) {
            successMessage.textContent = msg || 'Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được thu hồi.';
            successAlert.style.display = 'flex';
            errorAlert.style.display = 'none';
            successAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        function hideAlerts() {
            errorAlert.style.display = 'none';
            successAlert.style.display = 'none';
        }

        if (errorCloseBtn) {
            errorCloseBtn.addEventListener('click', function () {
                errorAlert.style.display = 'none';
            });
        }

        // 4. Trạng thái Loading của nút Submit
        function setLoading(loading) {
            if (loading) {
                submitBtn.disabled = true;
                resetBtn.disabled = true;
                currentPwdInput.disabled = true;
                newPwdInput.disabled = true;
                confirmPwdInput.disabled = true;
                submitSpinner.style.display = 'inline-block';
                submitIcon.style.display = 'none';
                submitText.textContent = 'Đang xử lý...';
            } else {
                submitBtn.disabled = false;
                resetBtn.disabled = false;
                currentPwdInput.disabled = false;
                newPwdInput.disabled = false;
                confirmPwdInput.disabled = false;
                submitSpinner.style.display = 'none';
                submitIcon.style.display = 'inline-block';
                submitText.textContent = 'Đổi mật khẩu';
            }
        }

        // Reset form
        resetBtn.addEventListener('click', function () {
            hideAlerts();
            currentPwdInput.classList.remove('is-valid', 'is-invalid');
            newPwdInput.classList.remove('is-valid', 'is-invalid');
            confirmPwdInput.classList.remove('is-valid', 'is-invalid');
            currentFeedback.textContent = '';
            newFeedback.textContent = '';
            confirmFeedback.textContent = '';
            setTimeout(function () {
                updateStrengthMeter();
            }, 50);
        });

        // 5. Xử lý gửi Form & API Contract CRM-24
        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            hideAlerts();

            var currentPassword = currentPwdInput.value.trim();
            var newPassword = newPwdInput.value;
            var confirmPassword = confirmPwdInput.value;
            var isValid = true;

            // Kiểm tra mật khẩu hiện tại
            if (!currentPassword) {
                currentPwdInput.classList.add('is-invalid');
                currentFeedback.textContent = 'Vui lòng nhập mật khẩu hiện tại.';
                isValid = false;
            } else {
                currentPwdInput.classList.remove('is-invalid');
                currentFeedback.textContent = '';
            }

            // Kiểm tra mật khẩu mới
            var hasLetter = /[a-zA-Z]/.test(newPassword);
            var hasNumber = /[0-9]/.test(newPassword);

            if (!newPassword) {
                newPwdInput.classList.add('is-invalid');
                newFeedback.textContent = 'Vui lòng nhập mật khẩu mới.';
                isValid = false;
            } else if (newPassword.length < 8) {
                newPwdInput.classList.add('is-invalid');
                newFeedback.textContent = 'Mật khẩu mới phải có tối thiểu 8 ký tự.';
                isValid = false;
            } else if (!hasLetter || !hasNumber) {
                newPwdInput.classList.add('is-invalid');
                newFeedback.textContent = 'Mật khẩu mới phải bao gồm cả chữ cái và chữ số.';
                isValid = false;
            } else if (newPassword === currentPassword) {
                newPwdInput.classList.add('is-invalid');
                newFeedback.textContent = 'Mật khẩu mới không được trùng với mật khẩu hiện tại.';
                isValid = false;
            } else {
                newPwdInput.classList.remove('is-invalid');
                newFeedback.textContent = '';
            }

            // Kiểm tra xác nhận mật khẩu
            if (!confirmPassword) {
                confirmPwdInput.classList.add('is-invalid');
                confirmFeedback.textContent = 'Vui lòng xác nhận lại mật khẩu mới.';
                confirmFeedback.className = 'cp-field-feedback text-danger';
                isValid = false;
            } else if (confirmPassword !== newPassword) {
                confirmPwdInput.classList.add('is-invalid');
                confirmFeedback.textContent = 'Mật khẩu xác nhận không trùng khớp.';
                confirmFeedback.className = 'cp-field-feedback text-danger';
                isValid = false;
            }

            if (!isValid) {
                showError('Thông tin chưa hợp lệ', 'Vui lòng kiểm tra lại các trường thông tin được đánh dấu đỏ.');
                return;
            }

            // Gửi dữ liệu lên API Contract CRM-24:
            // POST ${pageContext.request.contextPath}/api/auth/change-password
            // Payload: { currentPassword, newPassword, confirmPassword }
            setLoading(true);

            var endpoint = contextPath + '/api/auth/change-password';
            var payload = {
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            };

            try {
                var response = await fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                var resData = null;
                var contentType = response.headers.get('content-type');
                if (contentType && contentType.indexOf('application/json') !== -1) {
                    resData = await response.json();
                } else {
                    var rawText = await response.text();
                    try {
                        resData = JSON.parse(rawText);
                    } catch (ignore) {
                        resData = { message: rawText };
                    }
                }

                // Xử lý kết quả trả về theo Response JSON: { success: true/false, message: "..." }
                if (response.ok && resData && resData.success !== false) {
                    // Thành công theo AC: "Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được thu hồi."
                    var succMsg = (resData && resData.message)
                        ? resData.message
                        : 'Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được thu hồi.';

                    showSuccess(succMsg);

                    // Vô hiệu hóa toàn bộ form
                    submitBtn.disabled = true;
                    resetBtn.disabled = true;
                    currentPwdInput.disabled = true;
                    newPwdInput.disabled = true;
                    confirmPwdInput.disabled = true;
                    submitSpinner.style.display = 'none';
                    submitIcon.style.display = 'inline-block';
                    submitText.textContent = 'Đã hoàn tất';

                    // Đếm ngược 2 giây và chuyển hướng về /login theo AC
                    var countdown = 2;
                    redirectNotice.textContent = 'Đang chuyển hướng về trang đăng nhập sau ' + countdown + ' giây...';

                    var redirectTimer = setInterval(function () {
                        countdown--;
                        if (countdown > 0) {
                            redirectNotice.textContent = 'Đang chuyển hướng về trang đăng nhập sau ' + countdown + ' giây...';
                        } else {
                            clearInterval(redirectTimer);
                            window.location.href = contextPath + '/login';
                        }
                    }, 1000);

                } else {
                    // Xử lý các mã lỗi từ Backend (HTTP 400, 401, 500, 404, ...)
                    setLoading(false);
                    var errTitle = 'Đổi mật khẩu không thành công';
                    var errMsg = '';

                    if (resData && resData.message && resData.message.trim() !== '') {
                        errMsg = resData.message;
                    } else if (response.status === 400) {
                        errMsg = 'Yêu cầu không hợp lệ. Vui lòng kiểm tra lại mật khẩu cũ và tiêu chuẩn mật khẩu mới.';
                    } else if (response.status === 401) {
                        errMsg = 'Mật khẩu hiện tại không chính xác hoặc phiên làm việc đã hết hạn.';
                    } else if (response.status === 500) {
                        errMsg = 'Lỗi hệ thống từ máy chủ (500). Vui lòng thử lại sau hoặc liên hệ quản trị viên.';
                    } else if (response.status === 404) {
                        errMsg = 'Endpoint /api/auth/change-password chưa sẵn sàng trên máy chủ backend.';
                    } else {
                        errMsg = 'Đã xảy ra lỗi trong quá trình xử lý (Mã trạng thái: ' + response.status + ').';
                    }

                    showError(errTitle, errMsg);
                }

            } catch (err) {
                setLoading(false);
                console.error('Lỗi khi gọi API đổi mật khẩu:', err);
                showError('Lỗi kết nối máy chủ', 'Không thể kết nối đến máy chủ hoặc endpoint backend chưa sẵn sàng. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.');
            }
        });

    });
    </script>
</body>
</html>
