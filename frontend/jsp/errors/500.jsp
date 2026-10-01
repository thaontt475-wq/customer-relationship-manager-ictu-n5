<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>500 - Lỗi máy chủ nội bộ | CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">


    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/errors/errors.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của trang báo lỗi 500 -->
        <main class="error-page crm-page" id="errorApp" role="main">
            <section class="err-card crm-card err-card--500" aria-labelledby="errTitle">

                <!-- Đồ họa Icon minh họa lỗi 500: Sự cố máy chủ / Gián đoạn hệ thống -->
                <div class="err-illustration err-illustration--500" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                        <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                        <line x1="6" y1="6" x2="6.01" y2="6"></line>
                        <line x1="6" y1="18" x2="6.01" y2="18"></line>
                        <line x1="12" y1="11" x2="12" y2="13"></line>
                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                    </svg>
                </div>

                <!-- Mã lỗi lớn & Huy hiệu trạng thái -->
                <div class="err-code-display">
                    <span class="err-code-number">500</span>
                    <span class="err-badge">Lỗi máy chủ</span>
                </div>

                <!-- Tiêu đề & Thông điệp giải thích thân thiện -->
                <h1 class="err-title" id="errTitle">Sự cố kỹ thuật từ máy chủ</h1>
                <p class="err-description">
                    Hệ thống đã gặp lỗi bất ngờ trong quá trình xử lý yêu cầu của bạn.
                    Thông tin lỗi đã được tự động ghi nhận vào nhật ký hệ thống để đội ngũ kỹ thuật sớm khắc phục.
                    Vui lòng thử tải lại trang hoặc quay lại sau ít phút.
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
                        <span class="err-diag-value" id="diagRequestId">${not empty requestId ? requestId : 'Chưa có mã tra cứu'}</span>
                    </div>
                    <div class="err-diag-item">
                        <span class="err-diag-label">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="2" y1="12" x2="22" y2="12"></line>
                                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
                            </svg>
                            Đường dẫn gặp lỗi:
                        </span>
                        <span class="err-diag-value" id="diagPath">${pageContext.request.requestURI}</span>
                    </div>

                    <!-- Hiển thị chi tiết lỗi nếu Backend gửi kèm qua attribute error/message -->
                    <div class="err-diag-detail" id="serverErrorDetail" style="${empty error and empty message ? 'display: none;' : ''}">
                        <strong>Thông báo từ hệ thống:</strong>
                        <span>${not empty error ? error : message}</span>
                    </div>
                </div>

                <!-- Các nút hành động hỗ trợ người dùng quay lại luồng làm việc -->
                <div class="err-actions">
                    <button type="button" class="err-btn crm-btn err-btn-reload crm-btn-primary" onclick="window.location.reload()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <polyline points="23 4 23 10 17 10"></polyline>
                            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                        </svg>
                        <span>Tải lại trang</span>
                    </button>
                    <button type="button" class="err-btn crm-btn err-btn-secondary crm-btn-secondary" onclick="window.history.back()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <polyline points="15 18 9 12 15 6"></polyline>
                        </svg>
                        <span>Quay lại trang trước</span>
                    </button>
                    <a href="${pageContext.request.contextPath}/dashboard" class="err-btn crm-btn err-btn-primary crm-btn-primary">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                            <polyline points="9 22 9 12 15 12 15 22"></polyline>
                        </svg>
                        <span>Về bảng điều khiển</span>
                    </a>
                </div>

                <div class="err-footer-help">
                    Nếu sự cố vẫn tiếp diễn, vui lòng sao chép mã tra cứu sự cố ở trên và gửi cho đội ngũ hỗ trợ kỹ thuật.
                </div>

            </section>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <script>
    (function () {
        'use strict';
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
