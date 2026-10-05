<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
%>
<%
    String emailVal = (String) request.getAttribute("email");
    if (emailVal == null) {
        emailVal = request.getParameter("email");
    }
    String safeEmail = escapeHtml(emailVal != null ? emailVal : "");

    String errorMsg = (String) request.getAttribute("error");
    String messageMsg = (String) request.getAttribute("message");

    String resetLinkVal = (String) request.getAttribute("resetLink");
    Boolean resetSentSuccess = (Boolean) request.getAttribute("resetSentSuccess");
    String resetStatusMsg = (String) request.getAttribute("resetStatusMessage");
    String resetEmailVal = (String) request.getAttribute("resetEmail");

    boolean isSent = "1".equals(request.getParameter("sent"))
        || "true".equals(request.getParameter("sent"))
        || "success".equals(request.getParameter("state"))
        || (messageMsg != null && !messageMsg.trim().isEmpty())
        || (resetLinkVal != null && !resetLinkVal.trim().isEmpty());

    boolean hasError = (errorMsg != null && !errorMsg.trim().isEmpty());
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại Mật khẩu | Corporate CRM</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:ital,wght@0,300;0,400;0,500;0,600;0,700;0,800;1,400;1,500&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/forgot-password.css?v=20261005_5">
</head>
<body class="forgot-pwd-page">

    <main class="forgot-card-container">
        <section class="forgot-card" aria-labelledby="page-title">
            <%-- Top Brand Header (Hexagon Logo + Corporate) --%>
            <div class="brand-header">
                <svg class="brand-icon" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <path d="M14 2L24.3923 8V20L14 26L3.6077 20V8L14 2Z" fill="#1D4ED8"/>
                    <path d="M14 6L20.9282 10V18L14 22L7.0718 18V10L14 6Z" fill="#2563EB"/>
                    <path d="M16 10L11 14L16 18" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span class="brand-text">Corporate</span>
            </div>

            <%-- Pure CSS State Switcher Checkbox Hack --%>
            <input type="checkbox" id="toggle-state" class="state-toggle-checkbox" <%= isSent ? "checked" : "" %> aria-hidden="true" tabindex="-1">

            <%-- ========================================================
                 STATE 1: INITIAL FORM STATE
                 ======================================================== --%>
            <div class="forgot-state-initial">
                <h1 id="page-title" class="state-title">Đặt lại Mật khẩu</h1>
                <p class="state-desc">Nhập email liên kết với tài khoản để nhận hướng dẫn khôi phục</p>

                <form class="forgot-form" method="post" action="${pageContext.request.contextPath}/forgot-password">
                    <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">

                    <div class="field-group">
                        <label for="email" class="field-label">Email</label>
                        <input type="email"
                               id="email"
                               name="email"
                               class="form-input <%= hasError ? "has-error" : "" %>"
                               placeholder="name@company.com"
                               value="<%= safeEmail %>"
                               required
                               autocomplete="email">
                        <% if (hasError) { %>
                        <div class="field-error-notice" role="alert">
                            <span class="error-circle-icon" aria-hidden="true">!</span>
                            <span><%= escapeHtml(errorMsg) %></span>
                        </div>
                        <% } %>
                    </div>

                    <button type="submit" class="submit-btn">Gửi liên kết</button>

                    <div class="back-link-wrapper">
                        <a href="${pageContext.request.contextPath}/login" class="back-link">Quay lại đăng nhập</a>
                    </div>
                </form>
            </div>

            <%-- ========================================================
                 STATE 2: SUCCESS CONFIRMATION STATE
                 ======================================================== --%>
            <div class="forgot-state-success" role="status" aria-live="polite">
                <div class="success-icon-wrap" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                </div>

                <h2 class="success-title">Liên kết đặt lại mật khẩu đã được gửi!</h2>
                <p class="success-desc">Vui lòng kiểm tra hộp thư đến của bạn (hiệu lực 30 phút)</p>

                <% if (Boolean.TRUE.equals(resetSentSuccess)) { %>
                    <%-- Trạng thái chuẩn khi đã gửi email thành công qua Gmail SMTP --%>
                    <div class="smtp-alert smtp-alert-success">
                        <span class="smtp-alert-icon">✔</span>
                        <span>Đã gửi email hướng dẫn đặt lại mật khẩu tới <strong><%= escapeHtml(resetEmailVal != null && !resetEmailVal.isBlank() ? resetEmailVal : "hộp thư của bạn") %></strong>. Vui lòng kiểm tra hòm thư (kể cả mục Thư rác/Spam nếu chưa thấy).</span>
                    </div>
                <% } else if (resetLinkVal != null && !resetLinkVal.trim().isEmpty()) { %>
                    <%-- CHỈ hiển thị liên kết trực tiếp khi gửi mail thất bại hoặc chưa cấu hình SMTP (Chế độ Fallback nội bộ) --%>
                    <div class="smtp-alert smtp-alert-info">
                        <span class="smtp-alert-icon">⚡</span>
                        <span><%= escapeHtml(resetStatusMsg != null ? resetStatusMsg : "Dịch vụ SMTP cục bộ chưa cấu hình. Bạn có thể đặt lại mật khẩu ngay bằng liên kết trực tiếp bên dưới:") %></span>
                    </div>

                    <div class="direct-action-card">
                        <a href="<%= escapeHtml(resetLinkVal) %>" class="btn-direct-reset" id="btn-direct-reset">
                            👉 Đặt lại mật khẩu ngay (Nhấn vào đây)
                        </a>

                        <div class="copy-link-section">
                            <label for="resetLinkInput" class="copy-link-label">Hoặc copy đường dẫn đặt lại mật khẩu:</label>
                            <div class="copy-input-group">
                                <input type="text" id="resetLinkInput" class="copy-input" readonly value="<%= escapeHtml(resetLinkVal) %>">
                                <button type="button" class="btn-copy" onclick="copyResetLink(this)">Sao chép</button>
                            </div>
                        </div>
                    </div>

                    <details class="smtp-config-help">
                        <summary class="smtp-config-summary">⚙ Hướng dẫn cấu hình gửi email thật qua Gmail</summary>
                        <div class="smtp-config-content">
                            <p>Để nhận thư trực tiếp trong Gmail/Outlook của bạn:</p>
                            <ol>
                                <li>Mở tệp: <code>backend/src/main/resources/email.properties</code></li>
                                <li>Bật Xác thực 2 bước Google và tạo <strong>Mật khẩu ứng dụng (App Password)</strong> tại Google Account.</li>
                                <li>Điền email và mật khẩu ứng dụng 16 ký tự vào các trường <code>mail.smtp.username</code> và <code>mail.smtp.password</code>.</li>
                            </ol>
                        </div>
                    </details>
                <% } %>

                <div class="back-link-wrapper">
                    <a href="${pageContext.request.contextPath}/login" class="back-link">Quay lại đăng nhập</a>
                </div>
            </div>
        </section>

        <%-- Quick Demo Navigation for Reviewing Both Figma States --%>
        <nav class="demo-bar" aria-label="Xem trước trạng thái">
            <a href="${pageContext.request.contextPath}/forgot-password" class="demo-chip <%= !isSent ? "active" : "" %>">Form nhập email</a>
            <a href="${pageContext.request.contextPath}/forgot-password?sent=1" class="demo-chip <%= isSent ? "active" : "" %>">Xác nhận thành công</a>
        </nav>
    </main>

    <script>
    function copyResetLink(btn) {
        var copyText = document.getElementById("resetLinkInput");
        if (copyText) {
            copyText.select();
            copyText.setSelectionRange(0, 99999);
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(copyText.value).then(function() {
                    var oldText = btn.textContent;
                    btn.textContent = "Đã chép!";
                    btn.classList.add("copied");
                    setTimeout(function() {
                        btn.textContent = oldText;
                        btn.classList.remove("copied");
                    }, 2000);
                }).catch(function() {
                    fallbackCopy(copyText, btn);
                });
            } else {
                fallbackCopy(copyText, btn);
            }
        }
    }
    function fallbackCopy(copyText, btn) {
        try {
            document.execCommand("copy");
            var oldText = btn.textContent;
            btn.textContent = "Đã chép!";
            btn.classList.add("copied");
            setTimeout(function() {
                btn.textContent = oldText;
                btn.classList.remove("copied");
            }, 2000);
        } catch (e) {}
    }
    </script>
</body>
</html>

