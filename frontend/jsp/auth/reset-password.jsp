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
            <p>Mật khẩu phải có ít nhất 8 ký tự, gồm ít nhất một chữ và một số.</p>
        </header>

        <% if (message != null && !message.isBlank()) { %>
            <div class="auth-message auth-success" role="status"><%= escapeHtml(message) %></div>
            <div class="auth-actions auth-actions--center">
                <a href="${pageContext.request.contextPath}/login" class="auth-button auth-button--link">Đăng nhập bằng mật khẩu mới</a>
            </div>
        <% } else if (token != null && !token.isBlank()) { %>
            <form class="auth-form" method="post"
                  action="${pageContext.request.contextPath}/reset-password">
                <input type="hidden" name="token" value="<%= escapeHtml(token) %>">

                <div class="field-group">
                    <label for="newPassword">Mật khẩu mới</label>
                    <div class="password-field">
                        <input id="newPassword" type="password" name="newPassword"
                               autocomplete="new-password" minlength="8" maxlength="72"
                               pattern="(?=.*[A-Za-z])(?=.*[0-9]).{8,72}" required>
                        <button type="button" class="password-toggle" data-password-toggle="newPassword"
                                aria-label="Hiện mật khẩu mới">Hiện</button>
                    </div>
                </div>

                <div class="field-group">
                    <label for="confirmPassword">Xác nhận mật khẩu mới</label>
                    <div class="password-field">
                        <input id="confirmPassword" type="password" name="confirmPassword"
                               autocomplete="new-password" minlength="8" maxlength="72" required>
                        <button type="button" class="password-toggle" data-password-toggle="confirmPassword"
                                aria-label="Hiện xác nhận mật khẩu">Hiện</button>
                    </div>
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
<script>
    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
        button.addEventListener('click', function () {
            var input = document.getElementById(button.getAttribute('data-password-toggle'));
            var showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            button.textContent = showing ? 'Hiện' : 'Ẩn';
            button.setAttribute('aria-label', showing ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
        });
    });

    var resetForm = document.querySelector('.auth-form');
    if (resetForm) {
        resetForm.addEventListener('submit', function (event) {
            var password = document.getElementById('newPassword');
            var confirmation = document.getElementById('confirmPassword');
            if (password.value !== confirmation.value) {
                event.preventDefault();
                confirmation.setCustomValidity('Mật khẩu xác nhận không khớp.');
                confirmation.reportValidity();
            }
        });

        document.getElementById('confirmPassword').addEventListener('input', function () {
            this.setCustomValidity('');
        });
    }
</script>
</body>
</html>
