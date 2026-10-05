<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // Check reset parameter
    if ("1".equals(request.getParameter("reset")) || "reset".equals(request.getParameter("action"))) {
        session.removeAttribute("login_demo_attempts");
    }

    Integer demoAttemptsObj = (Integer) session.getAttribute("login_demo_attempts");
    int demoAttempts = (demoAttemptsObj != null) ? demoAttemptsObj : 0;

    String errorAttr = (String) request.getAttribute("error");
    String errorParam = request.getParameter("error");
    String lockedParam = request.getParameter("locked");
    String expiredParam = request.getParameter("expired");
    String demoParam = request.getParameter("demo");

    boolean isPostError = (errorAttr != null && !errorAttr.isBlank());

    // Check session expired state
    boolean isExpired = "1".equals(expiredParam)
        || "expired".equals(demoParam)
        || (errorAttr != null && errorAttr.contains("hết hạn"));

    // Check demo overrides or post increments
    if ("1".equals(lockedParam) || "locked".equals(demoParam)) {
        demoAttempts = 5;
        session.setAttribute("login_demo_attempts", 5);
    } else if ("1".equals(errorParam) || "error".equals(demoParam)) {
        if (demoAttempts == 0) demoAttempts = 1;
        session.setAttribute("login_demo_attempts", demoAttempts);
    } else if (isPostError && !isExpired) {
        demoAttempts++;
        session.setAttribute("login_demo_attempts", demoAttempts);
    }

    // Determine states
    boolean isLocked = (demoAttempts >= 5);
    boolean isInvalidCreds = !isExpired && !isLocked && (demoAttempts > 0);

    String attemptsLabel = "(" + demoAttempts + "/5 attempts)";
    if ("1".equals(lockedParam)) {
        attemptsLabel = "(1/5 attempts)";
    }

    String emailParam = request.getParameter("email");
    if (emailParam == null) {
        emailParam = (String) request.getAttribute("email");
    }
    String safeEmail = com.crm.util.Html.escape(emailParam != null ? emailParam : "");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Màn hình Đăng nhập | CRM ICTU</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:ital,wght@0,300;0,400;0,500;0,600;0,700;0,800;1,400;1,500&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login.css?v=20261005_3">
</head>
<body class="login-page">

    <%-- State: Session Timeout Toast Notification (Top-Right) --%>
    <% if (isExpired) { %>
    <aside class="session-timeout-toast" role="status" aria-live="polite">
        <span class="toast-circle-icon" aria-hidden="true">!</span>
        <div class="toast-content">
            <div class="toast-text-title">Phiên đăng nhập đã hết hạn.</div>
            <div class="toast-text-desc">Vui lòng đăng nhập lại.</div>
        </div>
    </aside>
    <% } %>

    <main class="login-container">
        <section class="login-card" aria-labelledby="login-heading">
            <header class="login-header">
                <div class="login-brand-logo" aria-hidden="true">
                    <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <circle cx="24" cy="24" r="23" stroke="#2563eb" stroke-width="2" fill="#eff6ff"/>
                        <path d="M24 10C16.268 10 10 16.268 10 24C10 27.866 11.567 31.366 14.1 33.9L16.928 31.072C15.116 29.26 14 26.76 14 24C14 18.477 18.477 14 24 14C27.53 14 30.63 15.84 32.4 18.62L35.77 16.37C33.19 12.51 28.88 10 24 10Z" fill="#1d4ed8"/>
                        <path d="M24 38C31.732 38 38 31.732 38 24C38 20.134 36.433 16.634 33.9 14.1L31.072 16.928C32.884 18.74 34 21.24 34 24C34 29.523 29.523 34 24 34C20.47 34 17.37 32.16 15.6 29.38L12.23 31.63C14.81 35.49 19.12 38 24 38Z" fill="#1d4ed8"/>
                        <circle cx="24" cy="24" r="4" fill="#2563eb"/>
                    </svg>
                </div>
                <h1 id="login-heading" class="login-title">Màn hình Đăng nhập</h1>
            </header>

            <form class="login-form" method="post" action="${pageContext.request.contextPath}/login" novalidate>
                <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">

                <div class="field-group">
                    <label for="email" class="field-label">Email</label>
                    <input type="email"
                           id="email"
                           name="email"
                           class="login-input"
                           placeholder="name@company.com"
                           value="<%= safeEmail %>"
                           required
                           autocomplete="email">
                </div>

                <div class="field-group">
                    <label for="password" class="field-label">Mật khẩu</label>
                    <div class="password-wrapper <%= isInvalidCreds ? "has-error" : "" %>">
                        <input type="checkbox" id="toggle-pwd" class="pwd-toggle-checkbox" tabindex="-1" aria-label="Hiện hoặc ẩn mật khẩu">
                        <input type="text"
                               id="password"
                               name="password"
                               class="login-input pwd-masked-input"
                               placeholder="••••••••"
                               required
                               autocomplete="current-password">
                        <label for="toggle-pwd" class="pwd-toggle-label" title="Hiện hoặc ẩn mật khẩu">
                            <%-- Eye-off icon (default when hidden) --%>
                            <svg class="eye-svg eye-off" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                <line x1="1" y1="1" x2="23" y2="23"></line>
                            </svg>
                            <%-- Eye-on icon (when visible) --%>
                            <svg class="eye-svg eye-on" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                        </label>
                    </div>

                    <%-- State: Error Notice for Invalid Email/Password with attempts count --%>
                    <% if (isInvalidCreds) { %>
                    <div class="field-error-notice" role="alert">
                        <span class="error-circle-icon" aria-hidden="true">!</span>
                        <span class="error-notice-text">Sai email hoặc mật khẩu <%= attemptsLabel %></span>
                    </div>
                    <% } %>
                </div>

                <%-- State: Account Lockout Warning Card --%>
                <% if (isLocked) { %>
                <div class="lockout-alert" role="alert">
                    <div class="lockout-title">Tài khoản bị khóa tạm thời 15 phút</div>
                    <div class="lockout-subtitle"><%= attemptsLabel %></div>
                    <div class="unlock-action">
                        <a href="${pageContext.request.contextPath}/login?reset=1">Thử lại / Mở khóa demo</a>
                    </div>
                </div>
                <% } %>

                <button type="submit"
                        class="login-submit-btn <%= isLocked ? "is-disabled" : "" %>"
                        <%= isLocked ? "disabled aria-disabled=\"true\"" : "" %>>
                    Đăng nhập hệ thống
                </button>

                <div class="login-footer-links">
                    <a href="${pageContext.request.contextPath}/forgot-password" class="forgot-pwd-link">Quên mật khẩu?</a>
                </div>
            </form>
        </section>

        <%-- Quick Demo Navigation for Testing All UI States --%>
        <nav class="demo-bar" aria-label="Các trạng thái giao diện demo">
            <span class="demo-label">Xem nhanh:</span>
            <a href="${pageContext.request.contextPath}/login?reset=1" class="demo-chip <%= (!isInvalidCreds && !isLocked && !isExpired) ? "active" : "" %>">Mặc định</a>
            <a href="${pageContext.request.contextPath}/login?demo=error" class="demo-chip <%= (isInvalidCreds) ? "active" : "" %>">Sai MK (1/5)</a>
            <a href="${pageContext.request.contextPath}/login?demo=locked" class="demo-chip <%= (isLocked) ? "active" : "" %>">Khóa tài khoản (5/5)</a>
            <a href="${pageContext.request.contextPath}/login?demo=expired" class="demo-chip <%= (isExpired) ? "active" : "" %>">Hết hạn phiên</a>
            <% if (demoAttempts > 0) { %>
            <a href="${pageContext.request.contextPath}/login?reset=1" class="demo-chip demo-chip-reset">↺ Reset lần thử (<%= demoAttempts %>)</a>
            <% } %>
        </nav>
    </main>

</body>
</html>
