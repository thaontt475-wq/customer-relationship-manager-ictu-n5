<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ảnh đại diện | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/avatar.css">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <main class="avatar-page" role="main">
            <h1>Ảnh đại diện người dùng</h1>
            <p>Chọn JPG/JPEG hoặc PNG tối đa 2 MiB (2.097.152 byte), tối đa 16 triệu điểm ảnh.</p>
            <p>Ảnh sẽ được xử lý sau khi nhấn Lưu. Hệ thống tự động cắt vuông ở giữa ảnh, tạo ảnh chuẩn 512 × 512 và ảnh thu nhỏ 128 × 128.</p>
            <% if (request.getAttribute("message") != null) { %>
            <p role="alert"><%= com.crm.util.Html.escape(request.getAttribute("message")) %></p>
            <% } else if ("1".equals(request.getParameter("updated"))) { %>
            <p role="status">Đã cập nhật ảnh đại diện.</p>
            <% } %>
            <form method="post" action="${pageContext.request.contextPath}/profile/avatar" enctype="multipart/form-data">
                <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                <label for="avatar">Chọn ảnh JPG hoặc PNG</label>
                <input id="avatar" name="avatar" type="file" accept="image/jpeg,image/png" required>
                <div style="display: flex; gap: 12px; margin-top: 8px;">
                    <a href="${pageContext.request.contextPath}/profile" style="padding: 10px 18px; background: #ffffff; border: 1px solid #cbd5e1; border-radius: 8px; color: #334155; text-decoration: none; font-size: 0.9rem; font-weight: 600; display: inline-flex; align-items: center;">Quay lại Hồ sơ</a>
                    <button type="submit" id="btnSubmitAvatar">Lưu ảnh đại diện</button>
                </div>
            </form>
        </main>
    </div>
</body>
</html>
