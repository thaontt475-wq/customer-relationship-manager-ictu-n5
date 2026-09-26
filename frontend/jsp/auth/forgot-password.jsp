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
    String safeEmail = escapeHtml(emailVal);

    String errorMsg = (String) request.getAttribute("error");
    if (errorMsg == null) {
        errorMsg = (String) request.getAttribute("errorMessage");
    }

    String successMsg = (String) request.getAttribute("message");
    if (successMsg == null) {
        successMsg = (String) request.getAttribute("successMessage");
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Quên mật khẩu | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css" />
</head>
<body class="auth-page">
    <main class="auth-shell">
        <section class="auth-card" aria-labelledby="forgot-title">
            <div class="auth-brand" aria-label="CRM brand">
                <div class="brand-mark">CRM</div>
                <span class="brand-name">CRM</span>
            </div>

            <header class="auth-header">
                <h1 id="forgot-title">Quên mật khẩu</h1>
                <p>Nhập địa chỉ email tài khoản của bạn để nhận hướng dẫn đặt lại mật khẩu.</p>
            </header>

            <form class="auth-form" method="post" action="${pageContext.request.contextPath}/forgot-password">
                <div class="field-group">
                    <label for="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        name="email"
                        value="<%= safeEmail %>"
                        placeholder="name@company.com"
                        required
                        autocomplete="email"
                    />
                </div>

                <button type="submit" class="auth-button">Gửi yêu cầu</button>

                <div class="auth-actions auth-actions--center">
                    <a href="${pageContext.request.contextPath}/login" class="back-link">
                        <span aria-hidden="true">&larr;</span> Quay lại đăng nhập
                    </a>
                </div>

                <% if (errorMsg != null && !errorMsg.trim().isEmpty()) { %>
                    <div class="auth-message auth-error" role="alert"><%= escapeHtml(errorMsg) %></div>
                <% } else if (successMsg != null && !successMsg.trim().isEmpty()) { %>
                    <div class="auth-message auth-success" role="status"><%= escapeHtml(successMsg) %></div>
                <% } else { %>
                    <div class="auth-message auth-message--empty"></div>
                <% } %>
            </form>
        </section>
    </main>
</body>
</html>