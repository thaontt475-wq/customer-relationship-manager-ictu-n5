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
            <p>Hệ thống tự động cắt vuông ở giữa ảnh, tạo ảnh chuẩn 512 × 512 và ảnh thu nhỏ 128 × 128.</p>

            <div id="clientErrorAlert" style="display: none; padding: 12px 16px; background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; color: #991b1b; font-size: 0.88rem; margin-bottom: 16px;">
                <span id="clientErrorMessage"></span>
            </div>

            <% if (request.getAttribute("message") != null) { %>
                <p role="alert" style="padding: 10px 14px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; color: #991b1b; font-size: 0.88rem;">${message}</p>
            <% } else if ("1".equals(request.getParameter("updated"))) { %>
                <p role="status" style="padding: 10px 14px; background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 8px; color: #065f46; font-size: 0.88rem;">Cập nhật ảnh đại diện thành công.</p>
            <% } %>

            <% if (Boolean.TRUE.equals(request.getAttribute("hasAvatar"))) { %>
                <div class="avatar-preview">
                    <figure>
                        <img src="${pageContext.request.contextPath}/profile/avatar/image" width="160" height="160" alt="Ảnh đại diện hiện tại">
                        <figcaption>Ảnh đại diện (512 × 512)</figcaption>
                    </figure>
                    <figure>
                        <img src="${pageContext.request.contextPath}/profile/avatar/thumbnail" width="96" height="96" alt="Ảnh thu nhỏ">
                        <figcaption>Thumbnail (128 × 128)</figcaption>
                    </figure>
                </div>
            <% } %>

            <form id="avatarForm" action="${pageContext.request.contextPath}/profile/avatar" method="post" enctype="multipart/form-data">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <label for="avatar" style="font-weight: 600; font-size: 0.92rem; color: #1e293b;">Chọn tệp ảnh mới (JPG, PNG tối đa 2MB)</label>
                <input id="avatar" name="avatar" type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" required style="padding: 8px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.88rem;">

                <!-- Xem trước bản thu nhỏ trước khi tải lên (AC 2) -->
                <div id="localPreviewWrap" style="display: none; align-items: center; gap: 16px; padding: 12px; background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px;">
                    <img id="localPreviewImg" src="" alt="Xem trước ảnh" style="width: 72px; height: 72px; border-radius: 50%; object-fit: cover; border: 2px solid #3b82f6;">
                    <div>
                        <div style="font-weight: 600; font-size: 0.88rem; color: #0f172a;">Xem trước bản thu nhỏ (Thumbnail Preview)</div>
                        <div id="localFileInfo" style="font-size: 0.78rem; color: #64748b; margin-top: 2px;"></div>
                    </div>
                </div>

                <div style="display: flex; gap: 12px; margin-top: 8px;">
                    <a href="${pageContext.request.contextPath}/profile" style="padding: 10px 18px; background: #ffffff; border: 1px solid #cbd5e1; border-radius: 8px; color: #334155; text-decoration: none; font-size: 0.9rem; font-weight: 600; display: inline-flex; align-items: center;">Quay lại Hồ sơ</a>
                    <button type="submit" id="btnSubmitAvatar">Lưu ảnh đại diện</button>
                </div>
            </form>
        </main>
    </div>

    <script>
    (function () {
        'use strict';
        var input = document.getElementById('avatar');
        var form = document.getElementById('avatarForm');
        var errAlert = document.getElementById('clientErrorAlert');
        var errMsg = document.getElementById('clientErrorMessage');
        var previewWrap = document.getElementById('localPreviewWrap');
        var previewImg = document.getElementById('localPreviewImg');
        var fileInfo = document.getElementById('localFileInfo');
        var btnSubmit = document.getElementById('btnSubmitAvatar');

        var MAX_SIZE = 2 * 1024 * 1024;
        var ALLOWED = ['jpg', 'jpeg', 'png'];

        function showError(msg) {
            errMsg.textContent = msg;
            errAlert.style.display = 'block';
        }

        function clearError() {
            errAlert.style.display = 'none';
            errMsg.textContent = '';
        }

        input.addEventListener('change', function () {
            clearError();
            if (!input.files || input.files.length === 0) {
                previewWrap.style.display = 'none';
                return;
            }
            var file = input.files[0];
            var ext = (file.name || '').split('.').pop().toLowerCase();

            if (ALLOWED.indexOf(ext) === -1) {
                showError('Định dạng tệp không hợp lệ. Hệ thống chỉ chấp nhận ảnh định dạng JPG, JPEG hoặc PNG.');
                input.value = '';
                previewWrap.style.display = 'none';
                return;
            }

            if (file.size > MAX_SIZE) {
                var mb = (file.size / (1024 * 1024)).toFixed(2);
                showError('Dung lượng tệp (' + mb + ' MB) vượt quá giới hạn 2MB (2.097.152 byte). Vui lòng chọn ảnh nhẹ hơn.');
                input.value = '';
                previewWrap.style.display = 'none';
                return;
            }

            var reader = new FileReader();
            reader.onload = function (e) {
                previewImg.src = e.target.result;
                fileInfo.textContent = file.name + ' (' + (file.size / 1024).toFixed(1) + ' KB)';
                previewWrap.style.display = 'flex';
            };
            reader.readAsDataURL(file);
        });

        form.addEventListener('submit', function (e) {
            clearError();
            if (!input.files || input.files.length === 0) {
                showError('Vui lòng chọn một tệp ảnh.');
                e.preventDefault();
                return;
            }
            var file = input.files[0];
            if (file.size > MAX_SIZE) {
                showError('Dung lượng tệp vượt quá giới hạn 2MB.');
                e.preventDefault();
                return;
            }
            btnSubmit.disabled = true;
            btnSubmit.textContent = 'Đang lưu ảnh...';
        });
    })();
    </script>
</body>
</html>
