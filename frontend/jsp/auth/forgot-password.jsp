<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

    <%-- CRM-23 / S1-03 - Quên mật khẩu qua email API: POST /api/auth/forgot-password Request attributes: - email -
        error - message --%>

        <%! private String escapeHtml(String input) { if (input==null) { return "" ; } return input
            .replace("&", "&amp;" ) .replace("<", "&lt;" ) .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
            }
            %>

            <% String emailVal=(String) request.getAttribute("email"); String errorMsg=(String)
                request.getAttribute("error"); String messageMsg=(String) request.getAttribute("message"); String
                safeEmail=escapeHtml(emailVal); %>

                <!DOCTYPE html>
                <html lang="vi">

                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">

                    <title>Quên mật khẩu | CRM</title>

                    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/auth.css">
                </head>

                <body class="auth-page">

                    <main class="auth-shell">

                        <section class="auth-card" aria-labelledby="forgot-password-title">

                            <div class="auth-brand">
                                <div class="brand-mark" aria-hidden="true">
                                    CRM
                                </div>

                                <span class="brand-name">
                                    CRM
                                </span>
                            </div>

                            <header class="auth-header">

                                <h1 id="forgot-password-title">
                                    Quên mật khẩu
                                </h1>

                                <p>
                                    Nhập địa chỉ email tài khoản của bạn để nhận
                                    hướng dẫn đặt lại mật khẩu.
                                </p>

                            </header>

                            <form class="auth-form" method="post"
                                action="${pageContext.request.contextPath}/api/auth/forgot-password">

                                <div class="field-group">

                                    <label for="email">
                                        Email
                                    </label>

                                    <input id="email" type="email" name="email" value="<%= safeEmail %>"
                                        placeholder="name@company.com" autocomplete="email" required>

                                </div>

                                <button type="submit" class="auth-button">
                                    Gửi yêu cầu
                                </button>

                                <% if (errorMsg !=null && !errorMsg.trim().isEmpty()) { %>

                                    <div class="auth-message auth-error" role="alert">
                                        <%= escapeHtml(errorMsg) %>
                                    </div>

                                    <% } else if (messageMsg !=null && !messageMsg.trim().isEmpty()) { %>

                                        <div class="auth-message auth-success" role="status">
                                            <%= escapeHtml(messageMsg) %>
                                        </div>

                                        <% } %>

                                            <div class="auth-actions auth-actions--center">

                                                <a href="${pageContext.request.contextPath}/login" class="back-link">
                                                    <span aria-hidden="true">&larr;</span>
                                                    Quay lại đăng nhập
                                                </a>

                                            </div>

                            </form>

                        </section>

                    </main>

                </body>

                </html>