<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Đăng nhập CRM | CRM ICTU</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css" />
</head>
<body class="auth-page">
    <main class="auth-shell">
        <section class="auth-card" aria-labelledby="login-title">
            <div class="auth-brand" aria-label="Thương hiệu CRM">
                <div class="brand-mark">CRM</div>
                <span class="brand-name">CRM ICTU</span>
            </div>

            <header class="auth-header">
                <h1 id="login-title">Đăng nhập CRM</h1>
                <p>Đăng nhập để tiếp tục công việc của bạn.</p>
            </header>

            <form class="auth-form" method="post" action="${pageContext.request.contextPath}/login">
<input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                <div class="field-group crm-form-group">
                    <label for="email">Email</label>
                    <input class="crm-input"
                        id="email"
                        type="email"
                        name="email"
                        placeholder="Nhập địa chỉ email"
                        required
                        autocomplete="email"
                    />
                </div>

                <div class="field-group crm-form-group">
                    <label for="password">Mật khẩu</label>
                    <div class="password-field">
                        <input class="crm-input"
                            id="password"
                            type="password"
                            name="password"
                            placeholder="••••••••"
                            required
                            autocomplete="current-password"
                        />
                    </div>
                </div>

                <div class="auth-actions">
                    <a href="${pageContext.request.contextPath}/forgot-password" class="forgot-link">Quên mật khẩu?</a>
                </div>

                <button type="submit" class="auth-button crm-btn crm-btn-primary">Đăng nhập</button>

                <div role="alert" class="auth-message ${empty requestScope.error ? 'auth-message--empty' : 'auth-error'}"><%= com.crm.util.Html.escape(request.getAttribute("error")) %></div>
            </form>
        </section>
    </main>
</body>
</html>
