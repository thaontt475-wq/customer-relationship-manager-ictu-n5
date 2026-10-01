<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Không tìm thấy trang | CRM ICTU</title>

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

        <!-- Khu vực nội dung chính của trang báo lỗi 404 -->
        <main class="error-page crm-page" id="errorApp" role="main">
            <section class="err-card crm-card err-card--404" aria-labelledby="errTitle">

                <!-- Đồ họa Icon minh họa lỗi 404: Không tìm thấy trang / Mất phương hướng -->
                <div class="err-illustration err-illustration--404" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        <line x1="11" y1="8" x2="11" y2="12"></line>
                        <line x1="11" y1="14" x2="11.01" y2="14"></line>
                    </svg>
                </div>

                <!-- Mã lỗi lớn & Huy hiệu trạng thái -->
                <div class="err-code-display">
                    <span class="err-code-number">404</span>
                    <span class="err-badge">Không tìm thấy trang</span>
                </div>

                <!-- Tiêu đề & Thông điệp giải thích thân thiện -->
                <h1 class="err-title" id="errTitle">Đường dẫn không tồn tại</h1>
                <p class="err-description">
                    Trang bạn đang cố gắng truy cập không tồn tại, đã bị gỡ bỏ, đổi tên hoặc tạm thời không khả dụng.
                    Vui lòng kiểm tra lại tính chính xác của đường dẫn URL hoặc quay về bảng điều khiển trung tâm.
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
                        <span class="err-diag-value" id="diagTimestamp"><%= java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) %></span>
                    </div>
                    <div class="err-diag-item">
                        <span class="err-diag-label">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                            </svg>
                            Mã tra cứu yêu cầu:
                        </span>
                        <span class="err-diag-value" id="diagRequestId"><%= com.crm.util.Html.escape(request.getAttribute("requestId") == null ? "Chưa có mã tra cứu" : request.getAttribute("requestId")) %></span>
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
                        <span class="err-diag-value" id="diagPath"><%= com.crm.util.Html.escape(request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI) != null ? request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI) : request.getRequestURI()) %></span>
                    </div>
                </div>

                <!-- Các nút hành động hỗ trợ người dùng quay lại luồng làm việc -->
                <div class="err-actions">
                    <a href="${pageContext.request.contextPath}/dashboard" class="err-btn crm-btn err-btn-secondary crm-btn-secondary">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <polyline points="15 18 9 12 15 6"></polyline>
                        </svg>
                        <span>Về trang tổng quan</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/dashboard" class="err-btn crm-btn err-btn-primary crm-btn-primary">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                            <polyline points="9 22 9 12 15 12 15 22"></polyline>
                        </svg>
                        <span>Về bảng điều khiển</span>
                    </a>
                </div>

                <div class="err-footer-help">
                    Nếu bạn tin rằng đây là một liên kết hỏng của hệ thống, vui lòng báo cho bộ phận quản trị CRM.
                </div>

            </section>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />
</body>
</html>
