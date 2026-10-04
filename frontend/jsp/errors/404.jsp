<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.util.Html" %>
<%
String errorUri = (String) request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI);
String forwardUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
String requestedUri = "/leads";
if (errorUri != null && !errorUri.isBlank()) {
    requestedUri = errorUri;
} else if (forwardUri != null && !forwardUri.isBlank() && !forwardUri.contains("/errors/404") && !forwardUri.contains("404.jsp")) {
    requestedUri = forwardUri;
}
Object reqIdObj = request.getAttribute("requestId");
String requestId = (reqIdObj != null && !String.valueOf(reqIdObj).isBlank())
        ? String.valueOf(reqIdObj)
        : "a43cd815-e2f7-461a-a268-d497024bef49";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Không tìm thấy nội dung yêu cầu | CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/errors/errors.css">

    <!-- Khối CSS chuẩn doanh nghiệp nhúng trực tiếp tránh mất style -->
    <style>
        .error-page-wrapper {
            flex: 1;
            min-width: 0;
            width: 100%;
            background-color: #f6f8fb;
            padding: 32px;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            color: #1e293b;
            box-sizing: border-box;
        }
        .error-container {
            max-width: 900px;
            margin: 0 auto;
            width: 100%;
            box-sizing: border-box;
        }
        .error-breadcrumb {
            font-size: 13px;
            color: #64748b;
            margin-bottom: 16px;
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
            line-height: 1.4;
        }
        .error-breadcrumb a {
            color: #64748b;
            text-decoration: none;
            transition: color 150ms ease;
        }
        .error-breadcrumb a:hover {
            color: #2563eb;
        }
        .error-breadcrumb .sep {
            color: #cbd5e1;
        }
        .error-breadcrumb .current {
            color: #1e293b;
            font-weight: 500;
        }
        .error-header-title {
            font-size: 24px;
            font-weight: 700;
            color: #0f172a;
            margin: 0 0 6px 0;
            line-height: 1.3;
        }
        .error-header-sub {
            font-size: 13px;
            color: #64748b;
            margin: 0 0 24px 0;
            line-height: 1.5;
        }
        .incident-card {
            background-color: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.05);
            padding: 32px;
            box-sizing: border-box;
            overflow: hidden;
        }
        .badge-error {
            display: inline-block;
            background-color: #fff7ed;
            color: #ea580c;
            border: 1px solid #ffedd5;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 700;
            padding: 4px 10px;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            margin-bottom: 16px;
            line-height: 1.3;
        }
        .headline-error {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
            margin-bottom: 20px;
        }
        .error-number {
            font-size: 36px;
            font-weight: 800;
            color: #ea580c;
            line-height: 1;
            display: inline-block;
            vertical-align: middle;
            margin-right: 4px;
        }
        .error-title {
            font-size: 20px;
            font-weight: 700;
            color: #1e293b;
            display: inline-block;
            vertical-align: middle;
            margin: 0;
            line-height: 1.3;
        }
        .incident-box {
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 16px 20px;
            margin: 20px 0;
            font-size: 14px;
            line-height: 1.6;
            box-sizing: border-box;
        }
        .incident-reason {
            font-weight: 600;
            color: #334155;
            margin: 0 0 10px 0;
        }
        .incident-list {
            margin: 0;
            padding-left: 20px;
            color: #475569;
        }
        .incident-list li {
            margin-bottom: 6px;
        }
        .incident-list li:last-child {
            margin-bottom: 0;
        }
        .incident-url {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            color: #0f172a;
            font-weight: 600;
        }
        .incident-req-id {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            background-color: #e2e8f0;
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 12px;
            color: #475569;
            display: inline-block;
            word-break: break-all;
        }
        .error-actions-row {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
            margin-top: 24px;
        }
        .btn-primary {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #2563eb;
            color: #ffffff !important;
            font-weight: 600;
            font-size: 14px;
            padding: 10px 20px;
            border-radius: 6px;
            border: 1px solid #2563eb;
            text-decoration: none;
            box-shadow: 0 1px 2px rgba(37, 99, 235, 0.2);
            transition: background-color 150ms ease;
            cursor: pointer;
        }
        .btn-primary:hover {
            background-color: #1d4ed8;
            border-color: #1d4ed8;
        }
        .btn-secondary {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #ffffff;
            border: 1px solid #cbd5e1;
            color: #334155 !important;
            font-weight: 500;
            font-size: 14px;
            padding: 10px 18px;
            border-radius: 6px;
            text-decoration: none;
            transition: all 150ms ease;
            cursor: pointer;
        }
        .btn-secondary:hover {
            background-color: #f8fafc;
            border-color: #94a3b8;
            color: #0f172a !important;
        }
        .btn-ghost {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #eff6ff;
            border: 1px solid #bfdbfe;
            color: #1d4ed8 !important;
            font-weight: 600;
            font-size: 14px;
            padding: 10px 18px;
            border-radius: 6px;
            text-decoration: none;
            transition: all 150ms ease;
            cursor: pointer;
        }
        .btn-ghost:hover {
            background-color: #dbeafe;
            border-color: #93c5fd;
            color: #1e40af !important;
        }
        .footer-alert {
            background-color: #fffbeb;
            border: 1px solid #fef3c7;
            border-radius: 6px;
            padding: 12px 16px;
            margin-top: 24px;
            box-sizing: border-box;
            display: flex;
            align-items: flex-start;
            gap: 8px;
            line-height: 1.5;
        }
        .footer-alert-title {
            font-weight: 700;
            color: #b45309;
            font-size: 13px;
            flex-shrink: 0;
        }
        .footer-alert-text {
            color: #92400e;
            font-size: 13px;
        }
        @media (max-width: 640px) {
            .error-page-wrapper { padding: 16px; }
            .incident-card { padding: 20px 16px; }
            .error-actions-row { flex-direction: column; align-items: stretch; }
            .btn-primary, .btn-secondary, .btn-ghost { width: 100%; text-align: center; }
            .footer-alert { flex-direction: column; gap: 4px; }
        }
    </style>
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình kiểm soát lỗi 404 -->
        <main class="error-page-wrapper" role="main">
            <div class="error-container">

                <!-- 1. BREADCRUMB -->
                <nav class="error-breadcrumb" aria-label="Đường dẫn">
                    <a href="${pageContext.request.contextPath}/dashboard">Trang chủ</a>
                    <span class="sep">/</span>
                    <span>Hệ thống</span>
                    <span class="sep">/</span>
                    <span>Thông báo ngoại lệ</span>
                    <span class="sep">/</span>
                    <span class="current">Màn hình kiểm soát lỗi</span>
                </nav>

                <!-- 2. TIÊU ĐỀ TRANG -->
                <header>
                    <h1 class="error-header-title">Trung tâm thông báo Lỗi &amp; Giới hạn Quyền</h1>
                    <p class="error-header-sub">Xử lý chuẩn hóa theo Servlet Router (/errors/{code}) - Giữ nguyên layout Header &amp; Sidebar để người dùng dễ dàng chuyển hướng.</p>
                </header>

                <!-- 3. THẺ KIỂM SOÁT LỖI (INCIDENT CARD) -->
                <article class="incident-card" style="background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.05); padding: 32px;" aria-labelledby="errTitle">

                    <!-- Huy hiệu mã lỗi -->
                    <span class="badge-error" style="background: #fff7ed; color: #ea580c; border: 1px solid #ffedd5; border-radius: 4px; font-size: 12px; font-weight: 700; padding: 4px 10px; display: inline-block;">MÃ LỖI: HTTP 404 NOT FOUND</span>

                    <!-- Dòng tiêu đề lỗi -->
                    <div class="headline-error" style="margin-bottom: 20px;">
                        <span class="error-number" style="font-size: 36px; font-weight: 800; color: #ea580c; line-height: 1; display: inline-block; vertical-align: middle; margin-right: 12px;">404</span>
                        <h2 class="error-title" id="errTitle" style="font-size: 20px; font-weight: 700; color: #1e293b; display: inline-block; vertical-align: middle; margin: 0;">Không tìm thấy nội dung yêu cầu</h2>
                    </div>

                    <!-- Hộp chi tiết sự cố kỹ thuật (Incident Log Box) -->
                    <section class="incident-box" style="background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; padding: 16px 20px; margin: 20px 0;" aria-label="Chi tiết sự cố kỹ thuật">
                        <p class="incident-reason" style="font-weight: 600; color: #334155; margin-bottom: 8px;">Lý do: Đường dẫn không tồn tại hoặc bản ghi dữ liệu đã bị xóa khỏi hệ thống.</p>
                        <ul class="incident-list" style="margin: 0; padding-left: 20px; color: #475569;">
                            <li style="margin-bottom: 6px;">
                                URL truy cập: <span class="incident-url" style="font-family: monospace; color: #0f172a;"><%= Html.escape(requestedUri) %></span>
                            </li>
                            <li style="margin-bottom: 6px;">
                                Trạng thái bản ghi: Có thể đã được gộp (Merge) hoặc xóa vĩnh viễn.
                            </li>
                            <li>
                                Mã yêu cầu (Request ID): <span class="incident-req-id" style="font-family: monospace; background: #e2e8f0; padding: 2px 6px; border-radius: 4px; font-size: 12px; color: #475569;"><%= Html.escape(requestId) %></span>
                            </li>
                        </ul>
                    </section>

                    <!-- Dãy nút điều hướng doanh nghiệp (Thuần thẻ <a>) -->
                    <nav class="error-actions-row" style="display: flex; gap: 12px; margin-top: 24px;" aria-label="Điều hướng hỗ trợ">
                        <a href="${pageContext.request.contextPath}/dashboard" class="btn-primary" style="background: #2563eb; color: #ffffff; font-weight: 600; padding: 10px 20px; border-radius: 6px; text-decoration: none; border: 1px solid #2563eb;">&larr; Về trang chủ</a>
                        <a href="${pageContext.request.contextPath}/customers" class="btn-secondary" style="background: #ffffff; border: 1px solid #cbd5e1; color: #334155; font-weight: 500; padding: 10px 18px; border-radius: 6px; text-decoration: none;">Quay lại trang trước</a>
                        <a href="${pageContext.request.contextPath}/customers" class="btn-ghost" style="background: #eff6ff; border: 1px solid #bfdbfe; color: #1d4ed8; font-weight: 600; padding: 10px 18px; border-radius: 6px; text-decoration: none;">Xem danh sách Khách hàng</a>
                    </nav>

                    <!-- Banner cảnh báo an toàn doanh nghiệp (Footer Alert) -->
                    <aside class="footer-alert" style="background: #fffbeb; border: 1px solid #fef3c7; border-radius: 6px; padding: 12px 16px; margin-top: 24px;" role="note">
                        <span class="footer-alert-title" style="font-weight: 700; color: #b45309;">[Hỗ trợ điều hướng an toàn]:</span>
                        <span class="footer-alert-text" style="color: #92400e; font-size: 13px;">Không hiển thị stack trace lỗi kỹ thuật ra ngoài. Bảo vệ an toàn mã nguồn và thông tin máy chủ.</span>
                    </aside>

                </article>

            </div>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />
</body>
</html>
