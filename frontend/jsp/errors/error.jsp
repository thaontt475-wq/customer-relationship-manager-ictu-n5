<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    Object rawErrorCode = request.getAttribute("statusCode");
    String errorCode = rawErrorCode == null ? "500" : String.valueOf(rawErrorCode);
    if (!java.util.Set.of("401", "403", "404", "500").contains(errorCode)) errorCode = "500";
    String errBadge = switch(errorCode) {
        case "401" -> "Hết phiên làm việc";
        case "403" -> "Từ chối truy cập";
        case "404" -> "Không tìm thấy";
        default -> "Lỗi máy chủ";
    };
    String errTitle = switch(errorCode) {
        case "401" -> "Phiên đăng nhập đã kết thúc";
        case "403" -> "Không có quyền truy cập";
        case "404" -> "Đường dẫn không tồn tại";
        default -> "Đã xảy ra sự cố";
    };
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= errorCode %> - CRM ICTU</title>

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

        <!-- Khu vực nội dung chính của trang báo lỗi động -->
        <main class="error-page crm-page" id="errorApp" role="main">
            <section class="err-card crm-card err-card--<%= errorCode %>" id="dynamicErrCard" aria-labelledby="errTitle">

                <!-- Đồ họa Icon minh họa động theo mã lỗi -->
                <div class="err-illustration err-illustration--<%= errorCode %>" id="dynamicErrIcon" aria-hidden="true">
                    <svg id="defaultIconSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                </div>

                <!-- Mã lỗi lớn & Huy hiệu trạng thái -->
                <div class="err-code-display">
                    <span class="err-code-number" id="dynamicErrCode"><%= errorCode %></span>
                    <span class="err-badge" id="dynamicErrBadge"><%= errBadge %></span>
                </div>

                <!-- Tiêu đề & Thông điệp giải thích thân thiện -->
                <h1 class="err-title" id="errTitle"><%= com.crm.util.Html.escape(request.getAttribute("title") == null ? errTitle : request.getAttribute("title")) %></h1>
                <p class="err-description" id="dynamicErrDesc">
                    <%= com.crm.util.Html.escape(request.getAttribute("message") == null ? "Hệ thống gặp sự cố. Vui lòng quay về trang tổng quan." : request.getAttribute("message")) %>
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
                            Mã tra cứu sự cố:
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

                    <div class="err-diag-detail" id="serverErrorDetail" style="${empty error ? 'display: none;' : ''}">
                        <strong>Thông báo lỗi chi tiết:</strong>
                        <span><%= com.crm.util.Html.escape(request.getAttribute("error")) %></span>
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
                    <a href="${pageContext.request.contextPath}/login" class="err-btn crm-btn err-btn-login crm-btn-primary" id="dynamicLoginBtn" style="<%= ("401".equals(errorCode) || "403".equals(errorCode)) ? "" : "display: none;" %>">
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
</body>
</html>
