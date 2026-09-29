<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ==========================================================================
  FEATURE S2-05: Màn hình Thêm mới sản phẩm / dịch vụ (Standalone View)
  Phụ trách Frontend/View: Tiệp
  BE CONTRACT:
    - POST ${pageContext.request.contextPath}/products
    - Parameters: code, name, type, unit, listPrice, floorPrice, costPrice, status, description
  ==========================================================================
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thêm sản phẩm mới - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Sản phẩm & Bảng giá (S2-05) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/products/products.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính -->
        <main class="prod-page" role="main">
            <div class="prod-container" style="max-width: 800px;">

                <!-- Breadcrumb -->
                <nav class="prod-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="${pageContext.request.contextPath}/products">Sản phẩm & Bảng giá</a>
                    <span class="separator">/</span>
                    <span class="active">Thêm sản phẩm mới</span>
                </nav>

                <!-- Header -->
                <header class="prod-header">
                    <div class="prod-header-info">
                        <h1>Thêm sản phẩm / dịch vụ mới</h1>
                        <p>Khai báo thông tin hàng hóa, dịch vụ và định cấu hình giá sàn kiểm soát duyệt chiết khấu.</p>
                    </div>
                </header>

                <!-- Alerts -->
                <div class="prod-alerts" id="alertsArea">
                    <% if (request.getAttribute("errorMessage") != null) { %>
                        <div class="prod-alert prod-alert-danger" role="alert">
                            <svg class="prod-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <div class="prod-alert-content">
                                <div class="prod-alert-title">Lỗi từ máy chủ</div>
                                <div><%= request.getAttribute("errorMessage") %></div>
                            </div>
                        </div>
                    <% } %>

                    <div class="prod-alert prod-alert-danger" id="clientError" style="display: none;" role="alert">
                        <svg class="prod-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                        </svg>
                        <div class="prod-alert-content">
                            <div class="prod-alert-title" id="clientErrorTitle">Lỗi dữ liệu</div>
                            <div id="clientErrorMsg"></div>
                        </div>
                    </div>
                </div>

                <!-- Form Card -->
                <section class="prod-card" style="padding: 24px;">
                    <form action="${pageContext.request.contextPath}/products" method="post" id="createForm" onsubmit="return validateForm(event)">
                        <div style="display: flex; flex-direction: column; gap: 16px;">
                            
                            <div class="prod-form-row">
                                <div class="prod-form-group">
                                    <label class="prod-label" for="code">Mã sản phẩm <span class="required">*</span></label>
                                    <input type="text" id="code" name="code" class="prod-input" placeholder="VD: SP-010" required>
                                </div>
                                <div class="prod-form-group">
                                    <label class="prod-label" for="type">Loại sản phẩm <span class="required">*</span></label>
                                    <select id="type" name="type" class="prod-select" required>
                                        <option value="SUBSCRIPTION">Dịch vụ định kỳ (Subscription)</option>
                                        <option value="ONE_TIME">Sản phẩm dùng 1 lần</option>
                                        <option value="LICENSE">Bản quyền phần mềm</option>
                                        <option value="HARDWARE">Thiết bị phần cứng</option>
                                        <option value="CONSULTING">Tư vấn & Triển khai</option>
                                    </select>
                                </div>
                            </div>

                            <div class="prod-form-group">
                                <label class="prod-label" for="name">Tên sản phẩm / Dịch vụ <span class="required">*</span></label>
                                <input type="text" id="name" name="name" class="prod-input" placeholder="Nhập tên sản phẩm..." required>
                            </div>

                            <div class="prod-form-row">
                                <div class="prod-form-group">
                                    <label class="prod-label" for="unit">Đơn vị tính <span class="required">*</span></label>
                                    <input type="text" id="unit" name="unit" class="prod-input" placeholder="VD: Gói/Năm, License, Chiếc" required>
                                </div>
                                <div class="prod-form-group">
                                    <label class="prod-label" for="status">Trạng thái kinh doanh</label>
                                    <select id="status" name="status" class="prod-select">
                                        <option value="ACTIVE" selected>Đang kinh doanh</option>
                                        <option value="INACTIVE">Ngừng kinh doanh</option>
                                    </select>
                                </div>
                            </div>

                            <div class="prod-form-row">
                                <div class="prod-form-group">
                                    <label class="prod-label" for="listPrice">Giá niêm yết <span class="required">*</span></label>
                                    <div class="prod-input-group">
                                        <input type="number" id="listPrice" name="listPrice" class="prod-input" min="0" step="1000" placeholder="25000000" required>
                                        <span class="prod-input-suffix">₫</span>
                                    </div>
                                    <span class="prod-hint">Giá bán tiêu chuẩn công bố tới khách hàng</span>
                                </div>
                                <div class="prod-form-group">
                                    <label class="prod-label" for="floorPrice">Giá sàn (Ngưỡng duyệt) <span class="required">*</span></label>
                                    <div class="prod-input-group">
                                        <input type="number" id="floorPrice" name="floorPrice" class="prod-input" min="0" step="1000" placeholder="20000000" required>
                                        <span class="prod-input-suffix">₫</span>
                                    </div>
                                    <span class="prod-hint">Giá tối thiểu Sales Rep được phép chào giá</span>
                                </div>
                            </div>

                            <div class="prod-hint-warning">
                                ⚠️ <strong>Quy định nghiệp vụ (S2-05):</strong> Báo giá có đơn giá dưới Giá sàn sẽ phải chuyển Giám đốc kinh doanh duyệt.
                            </div>

                            <div class="prod-form-group">
                                <label class="prod-label" for="costPrice">Giá vốn (Chỉ Giám đốc kinh doanh)</label>
                                <div class="prod-input-group">
                                    <input type="number" id="costPrice" name="costPrice" class="prod-input" min="0" step="1000" placeholder="12000000">
                                    <span class="prod-input-suffix">₫</span>
                                </div>
                                <span class="prod-hint">Dữ liệu bảo mật chỉ phục vụ tính toán tỷ suất lợi nhuận gộp</span>
                            </div>

                            <div class="prod-form-group">
                                <label class="prod-label" for="description">Mô tả sản phẩm / dịch vụ</label>
                                <textarea id="description" name="description" class="prod-textarea" rows="4" placeholder="Nhập mô tả tính năng, cam kết SLA..."></textarea>
                            </div>

                            <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; padding-top: 16px; border-top: 1px solid var(--prod-border);">
                                <a href="${pageContext.request.contextPath}/products" class="btn-prod btn-prod-secondary">Hủy bỏ</a>
                                <button type="submit" class="btn-prod btn-prod-primary">Lưu sản phẩm</button>
                            </div>

                        </div>
                    </form>
                </section>

            </div>
        </main>
    </div>

    <script>
        function validateForm(e) {
            const listPrice = parseFloat(document.getElementById("listPrice").value);
            const floorPrice = parseFloat(document.getElementById("floorPrice").value);

            if (floorPrice > listPrice) {
                e.preventDefault();
                const errBox = document.getElementById("clientError");
                document.getElementById("clientErrorTitle").textContent = "Lỗi kiểm tra giá sàn";
                document.getElementById("clientErrorMsg").textContent = "Giá sàn không thể lớn hơn Giá niêm yết. Vui lòng điều chỉnh lại.";
                errBox.style.display = "flex";
                return false;
            }
            return true;
        }
    </script>
</body>
</html>