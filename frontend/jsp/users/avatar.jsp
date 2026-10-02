<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.util.Html" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>áº¢nh Ä‘áº¡i diá»‡n | CRM</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/avatar.css">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <main class="avatar-page crm-page" role="main">
            <div class="crm-page-header">
                <h1 class="crm-page-title">Cáº­p nháº­t áº£nh Ä‘áº¡i diá»‡n</h1>
                <p class="crm-page-description">Quáº£n lÃ½ áº£nh Ä‘áº¡i diá»‡n hiá»ƒn thá»‹ trÃªn toÃ n há»‡ thá»‘ng CRM.</p>
            </div>

            <section class="crm-card avatar-card">
                <% if (request.getAttribute("message") != null) { %>
                    <div class="crm-alert crm-alert-error" role="alert">
                        <%= Html.escape((String) request.getAttribute("message")) %>
                    </div>
                <% } else if ("1".equals(request.getParameter("updated"))) { %>
                    <div class="crm-alert crm-alert-success" role="status">
                        ÄÃ£ cáº­p nháº­t áº£nh Ä‘áº¡i diá»‡n thÃ nh cÃ´ng.
                    </div>
                <% } %>

                <div class="avatar-current">
                    <% Boolean hasAvatar = (Boolean) request.getAttribute("hasAvatar");
                       if (Boolean.TRUE.equals(hasAvatar)) { %>
                        <img src="${pageContext.request.contextPath}/profile/avatar/image" alt="áº¢nh Ä‘áº¡i diá»‡n hiá»‡n táº¡i" class="avatar-img-large">
                    <% } else { %>
                        <div class="avatar-placeholder">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                        </div>
                    <% } %>
                </div>

                <div class="avatar-instructions">
                    <h3>YÃªu cáº§u áº£nh táº£i lÃªn:</h3>
                    <ul>
                        <li>Äá»‹nh dáº¡ng há»— trá»£: JPG, JPEG, PNG.</li>
                        <li>Dung lÆ°á»£ng tá»‘i Ä‘a: 2 MiB.</li>
                        <li>Há»‡ thá»‘ng tá»± Ä‘á»™ng cáº¯t vÃ  cÄƒn giá»¯a áº£nh thÃ nh tá»· lá»‡ 1:1 (vuÃ´ng).</li>
                    </ul>
                </div>

                <form method="post" action="${pageContext.request.contextPath}/profile/avatar" enctype="multipart/form-data" class="crm-form avatar-form">
                    <input type="hidden" name="csrfToken" value="<%= Html.escape((String) request.getAttribute("csrfToken")) %>">

                    <div class="crm-form-group">
                        <label class="crm-form-label" for="avatar">Chá»n áº£nh tá»« thiáº¿t bá»‹</label>
                        <input id="avatar" name="avatar" type="file" accept="image/jpeg,image/png" class="crm-form-control file-input" required>
                    </div>

                    <div class="crm-form-actions">
                        <button type="submit" class="crm-btn crm-btn-primary">Táº£i lÃªn & Cáº­p nháº­t</button>
                        <a href="${pageContext.request.contextPath}/profile" class="crm-btn crm-btn-secondary">Quay láº¡i Há»“ sÆ¡</a>
                    </div>
                </form>
            </section>
        </main>
    </div>
</body>
</html>
