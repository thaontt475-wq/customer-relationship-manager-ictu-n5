<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%!
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>

<%
    String token = (String) request.getAttribute("token");
    String error = (String) request.getAttribute("error");
    String message = (String) request.getAttribute("message");
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
</head>
<body class="auth-page">
<main class="auth-shell">
    <section class="auth-card" aria-labelledby="reset-password-title">
        <div class="auth-brand">
            <div class="brand-mark" aria-hidden="true">CRM</div>
            <span class="brand-name">CRM</span>
        </div>

        <header class="auth-header">
            <h1 id="reset-password-title">Đặt lại mật khẩu</h1>
            <p>Mật khẩu phải có 8-72 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</p>
        </header>

        <% if (message != null && !message.isBlank()) { %>
            <div class="auth-message auth-success" role="status"><%= escapeHtml(message) %></div>
            <div class="auth-actions auth-actions--center">
                <a href="${pageContext.request.contextPath}/login" class="back-link">Quay lại đăng nhập</a>
            </div>
        <% } else if (token != null && !token.isBlank()) { %>
            <form class="auth-form" method="post"
                  action="${pageContext.request.contextPath}/reset-password">
                <input type="hidden" name="token" value="<%= escapeHtml(token) %>">

                <div class="field-group">
                    <label for="newPassword">Mật khẩu mới</label>
                    <input id="newPassword" type="password" name="newPassword"
                           autocomplete="new-password" minlength="8" maxlength="72" required>
                </div>

                <div class="field-group">
                    <label for="confirmPassword">Xác nhận mật khẩu mới</label>
                    <input id="confirmPassword" type="password" name="confirmPassword"
                           autocomplete="new-password" minlength="8" maxlength="72" required>
                </div>

                <button type="submit" class="auth-button">Đặt lại mật khẩu</button>

                <% if (error != null && !error.isBlank()) { %>
                    <div class="auth-message auth-error" role="alert"><%= escapeHtml(error) %></div>
                <% } %>
            </form>
        <% } else { %>
            <div class="auth-message auth-error" role="alert"><%= escapeHtml(error) %></div>
            <div class="auth-actions auth-actions--center">
                <a href="${pageContext.request.contextPath}/forgot-password" class="back-link">
                    Yêu cầu liên kết mới
                </a>
            </div>
        <% } %>
    </section>
</main>
</body>
</html>
