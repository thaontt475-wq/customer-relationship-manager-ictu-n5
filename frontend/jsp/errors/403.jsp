<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>403 - Không có quyền truy cập | CRM ICTU</title>

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

        <!-- Khu vực nội dung chính của trang báo lỗi 403 -->
        <main class="error-page crm-page" id="errorApp" role="main">
            <section class="err-card crm-card err-card--403" aria-labelledby="errTitle">

                <!-- Đồ họa Icon minh họa lỗi 403: Không đủ quyền hạn / Truy cập bị chặn -->
                <div class="err-illustration err-illustration--403" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                        <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                    </svg>
                </div>

                <!-- Mã lỗi lớn & Huy hiệu trạng thái -->
                <div class="err-code-display">
                    <span class="err-code-number">403</span>
                    <span class="err-badge">Từ chối truy cập</span>
                </div>

                <!-- Tiêu đề & Thông điệp giải thích thân thiện -->
                <h1 class="err-title" id="errTitle">Không có quyền truy cập tài nguyên</h1>
                <p class="err-description">
                    Tài khoản của bạn hiện tại chưa được cấp quyền hạn để xem hoặc chỉnh sửa dữ liệu tại trang này.
                    Vui lòng liên hệ với Quản trị viên  của tổ chức nếu bạn cho rằng đây là một sự nhầm lẫn về phân quyền.
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
                            Mã tra cứu yêu cầu:
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
                            Đường dẫn truy cập:
                        </span>
                        <span class="err-diag-value" id="diagPath">${pageContext.request.requestURI}</span>
                    </div>
                </div>

                <!-- Các nút hành động hỗ trợ người dùng quay lại luồng làm việc -->
                <div class="err-actions">
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
                    <a href="${pageContext.request.contextPath}/login" class="err-btn crm-btn err-btn-secondary crm-btn-secondary" title="Đổi sang tài khoản có quyền truy cập">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                            <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                        <span>Đổi tài khoản khác</span>
                    </a>
                </div>

                <div class="err-footer-help">
                    Cần quyền truy cập? Liên hệ Quản trị viên hệ thống để kiểm tra vai trò người dùng và phạm vi dữ liệu.
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
