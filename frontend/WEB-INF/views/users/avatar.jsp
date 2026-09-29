<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ảnh đại diện | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/avatar.css">
</head>
<body>
<jsp:include page="/jsp/shared/header.jsp" />
<main class="avatar-page">
    <h1>Ảnh đại diện</h1>
    <p>Chọn JPG/JPEG hoặc PNG tối đa 2 MiB (2.097.152 byte), tối đa 16 triệu điểm ảnh.</p>
    <p>Hệ thống cắt vuông ở giữa ảnh, tạo ảnh 512 × 512 và ảnh thu nhỏ 128 × 128.</p>
    <% if (request.getAttribute("message") != null) { %>
        <p role="alert">${message}</p>
    <% } else if ("1".equals(request.getParameter("updated"))) { %>
        <p role="status">Cập nhật ảnh đại diện thành công.</p>
    <% } %>
    <% if (Boolean.TRUE.equals(request.getAttribute("hasAvatar"))) { %>
        <div class="avatar-preview">
            <figure><img src="${pageContext.request.contextPath}/profile/avatar/image" width="256" height="256" alt="Ảnh đại diện hiện tại"><figcaption>Ảnh đại diện</figcaption></figure>
            <figure><img src="${pageContext.request.contextPath}/profile/avatar/thumbnail" width="128" height="128" alt="Ảnh thu nhỏ"><figcaption>Thumbnail 128 × 128</figcaption></figure>
        </div>
    <% } %>
    <form action="${pageContext.request.contextPath}/profile/avatar" method="post" enctype="multipart/form-data">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <label for="avatar">Chọn ảnh mới</label>
        <input id="avatar" name="avatar" type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" required>
        <button type="submit">Lưu ảnh đại diện</button>
    </form>
</main>
</body>
</html>
