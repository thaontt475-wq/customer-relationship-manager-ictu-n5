<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Đăng nhập | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css" />
</head>
<body class="auth-page">
    <main class="auth-shell">
        <section class="auth-card" aria-labelledby="login-title">
            <div class="auth-brand" aria-label="CRM brand">
                <div class="brand-mark">CRM</div>
                <span class="brand-name">CRM</span>
            </div>

            <header class="auth-header">
                <h1 id="login-title">Đăng nhập</h1>
                <p>Quản lý khách hàng, cơ hội và hoạt động bán hàng hiệu quả hơn.</p>
            </header>

            <form class="auth-form" method="post" action="${pageContext.request.contextPath}/login">
                <div class="field-group">
                    <label for="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        name="email"
                        placeholder="name@company.com"
                        required
                        autocomplete="email"
                    />
                </div>

                <div class="field-group">
                    <label for="password">Mật khẩu</label>
                    <input
                        id="password"
                        type="password"
                        name="password"
                        placeholder="••••••••"
                        required
                        autocomplete="current-password"
                    />
                </div>

                <div class="auth-actions">
                    <a href="${pageContext.request.contextPath}/forgot-password" class="forgot-link">Quên mật khẩu?</a>
                </div>

                <button type="submit" class="auth-button">Đăng nhập</button>

                <div class="auth-message ${empty requestScope.error ? 'auth-message--empty' : 'auth-error'}">${requestScope.error}</div>
            </form>
        </section>
    </main>
</body>
</html>
