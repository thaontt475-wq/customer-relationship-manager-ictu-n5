<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${not empty code ? code : 'Thông báo lỗi'} - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">

    <!-- CSS riêng biệt của module Error Pages (CRM-27) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/errors/errors.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của trang báo lỗi động -->
        <main class="error-page" id="errorApp" role="main">
            <section class="err-card" id="dynamicErrCard" aria-labelledby="errTitle">

                <!-- Đồ họa Icon minh họa động theo mã lỗi -->
                <div class="err-illustration" id="dynamicErrIcon" aria-hidden="true">
                    <svg id="defaultIconSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                </div>

                <!-- Mã lỗi lớn & Huy hiệu trạng thái -->
                <div class="err-code-display">
                    <span class="err-code-number" id="dynamicErrCode">${not empty code ? code : (not empty param.code ? param.code : '500')}</span>
                    <span class="err-badge" id="dynamicErrBadge">Thông báo hệ thống</span>
                </div>

                <!-- Tiêu đề & Thông điệp giải thích thân thiện -->
                <h1 class="err-title" id="errTitle">${not empty title ? title : 'Đã xảy ra sự cố'}</h1>
                <p class="err-description" id="dynamicErrDesc">
                    ${not empty message ? message : 'Hệ thống đã ghi nhận yêu cầu của bạn nhưng gặp trở ngại trong quá trình phản hồi. Vui lòng kiểm tra lại thao tác hoặc quay về bảng điều khiển.'}
                </p>

                <!-- Hộp thông tin tra cứu kỹ thuật (Diagnostic Box) -->
                <div class="err-diagnostics" aria-label="Thông tin kỹ thuật">
                    <div class="err-diag-item">
                        <span class="err-diag-label">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <polyline points="12 6 12 12 16 14"></polyline>
                            </svg>
                            Thời gian ghi nhận:
                        </span>
                        <span class="err-diag-value" id="diagTimestamp">Đang cập nhật...</span>
                    </div>
                    <div class="err-diag-item">
                        <span class="err-diag-label">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                            </svg>
                            Mã tra cứu sự cố:
                        </span>
                        <span class="err-diag-value" id="diagRequestId">${not empty requestId ? requestId : 'CRM-ERR-DYN'}</span>
                    </div>
                    <div class="err-diag-item">
                        <span class="err-diag-label">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="2" y1="12" x2="22" y2="12"></line>
                                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
                            </svg>
                            Đường dẫn yêu cầu:
                        </span>
                        <span class="err-diag-value" id="diagPath">${pageContext.request.requestURI}</span>
                    </div>

                    <div class="err-diag-detail" id="serverErrorDetail" style="${empty error ? 'display: none;' : ''}">
                        <strong>Thông báo lỗi chi tiết:</strong>
                        <span>${error}</span>
                    </div>
                </div>

                <!-- Các nút hành động hỗ trợ người dùng quay lại luồng làm việc -->
                <div class="err-actions">
                    <button type="button" class="err-btn err-btn-secondary" onclick="window.history.back()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <polyline points="15 18 9 12 15 6"></polyline>
                        </svg>
                        <span>Quay lại trang trước</span>
                    </button>
                    <a href="${pageContext.request.contextPath}/" class="err-btn err-btn-primary">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                            <polyline points="9 22 9 12 15 12 15 22"></polyline>
                        </svg>
                        <span>Về bảng điều khiển</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/login" class="err-btn err-btn-login" id="dynamicLoginBtn" style="display: none;">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                            <polyline points="10 17 15 12 10 7"></polyline>
                            <line x1="15" y1="12" x2="3" y2="12"></line>
                        </svg>
                        <span>Đăng nhập lại</span>
                    </a>
                </div>

                <div class="err-footer-help">
                    Cần hỗ trợ kỹ thuật? Vui lòng gửi mã tra cứu sự cố cho Quản trị viên hệ thống CRM.
                </div>

            </section>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <script>
    (function () {
        'use strict';
        var codeEl = document.getElementById('dynamicErrCode');
        var cardEl = document.getElementById('dynamicErrCard');
        var iconEl = document.getElementById('dynamicErrIcon');
        var badgeEl = document.getElementById('dynamicErrBadge');
        var loginBtn = document.getElementById('dynamicLoginBtn');
        var titleEl = document.getElementById('errTitle');
        var descEl = document.getElementById('dynamicErrDesc');

        var code = codeEl ? codeEl.textContent.trim() : '500';

        // Đổi giao diện và icon tương ứng với mã lỗi nếu là dynamic error
        if (code === '401') {
            cardEl.classList.add('err-card--401');
            iconEl.classList.add('err-illustration--401');
            badgeEl.textContent = 'Hết phiên làm việc';
            if (loginBtn) loginBtn.style.display = 'inline-flex';
            if (titleEl && titleEl.textContent.trim() === 'Đã xảy ra sự cố') {
                titleEl.textContent = 'Phiên đăng nhập đã kết thúc';
            }
        } else if (code === '403') {
            cardEl.classList.add('err-card--403');
            iconEl.classList.add('err-illustration--403');
            badgeEl.textContent = 'Từ chối truy cập';
            if (loginBtn) loginBtn.style.display = 'inline-flex';
            if (titleEl && titleEl.textContent.trim() === 'Đã xảy ra sự cố') {
                titleEl.textContent = 'Không có quyền truy cập';
            }
        } else if (code === '404') {
            cardEl.classList.add('err-card--404');
            iconEl.classList.add('err-illustration--404');
            badgeEl.textContent = 'Không tìm thấy';
            if (titleEl && titleEl.textContent.trim() === 'Đã xảy ra sự cố') {
                titleEl.textContent = 'Đường dẫn không tồn tại';
            }
        } else {
            cardEl.classList.add('err-card--500');
            iconEl.classList.add('err-illustration--500');
            badgeEl.textContent = 'Lỗi máy chủ';
        }

        var timestampEl = document.getElementById('diagTimestamp');
        if (timestampEl) {
            var now = new Date();
            timestampEl.textContent = now.toLocaleString('vi-VN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        }

        var pathEl = document.getElementById('diagPath');
        if (pathEl && (!pathEl.textContent || pathEl.textContent.trim() === '')) {
            pathEl.textContent = window.location.pathname;
        }
    })();
    </script>
</body>
</html>
