<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ==========================================================================
  FEATURE S2-05: Màn hình Chỉnh sửa sản phẩm / dịch vụ (Standalone View)
  Phụ trách Frontend/View: Tiệp
  BE CONTRACT:
    - GET  ${pageContext.request.contextPath}/products/edit?id={id}
    - POST ${pageContext.request.contextPath}/products/update
    - Parameters: id, code, name, type, unit, listPrice, floorPrice, costPrice, status, description
  ==========================================================================
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chỉnh sửa sản phẩm - CRM ICTU</title>

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
                    <span class="active">Chỉnh sửa sản phẩm</span>
                </nav>

                <!-- Header -->
                <header class="prod-header">
                    <div class="prod-header-info">
                        <h1>Chỉnh sửa sản phẩm / dịch vụ</h1>
                        <p>Cập nhật giá niêm yết, giá sàn và trạng thái hoạt động kinh doanh của sản phẩm.</p>
                    </div>
                </header>

                <!-- Form Card -->
                <section class="prod-card" style="padding: 24px;">
                    <form action="${pageContext.request.contextPath}/products/update" method="post" id="editForm" onsubmit="return validateEditForm(event)">
                        <input type="hidden" name="id" value="${product != null ? product.id : param.id}">

                        <div style="display: flex; flex-direction: column; gap: 16px;">
                            
                            <div class="prod-form-row">
                                <div class="prod-form-group">
                                    <label class="prod-label" for="editCode">Mã sản phẩm</label>
                                    <input type="text" id="editCode" name="code" class="prod-input" value="${product != null ? product.code : 'SP-001'}" readonly style="background: #f1f5f9; cursor: not-allowed;">
                                </div>
                                <div class="prod-form-group">
                                    <label class="prod-label" for="editType">Loại sản phẩm <span class="required">*</span></label>
                                    <select id="editType" name="type" class="prod-select" required>
                                        <option value="SUBSCRIPTION" selected>Dịch vụ định kỳ (Subscription)</option>
                                        <option value="ONE_TIME">Sản phẩm dùng 1 lần</option>
                                        <option value="LICENSE">Bản quyền phần mềm</option>
                                        <option value="HARDWARE">Thiết bị phần cứng</option>
                                        <option value="CONSULTING">Tư vấn & Triển khai</option>
                                    </select>
                                </div>
                            </div>

                            <div class="prod-form-group">
                                <label class="prod-label" for="editName">Tên sản phẩm / Dịch vụ <span class="required">*</span></label>
                                <input type="text" id="editName" name="name" class="prod-input" value="${product != null ? product.name : 'Gói CRM Cloud Enterprise (1 Năm)'}" required>
                            </div>

                            <div class="prod-form-row">
                                <div class="prod-form-group">
                                    <label class="prod-label" for="editUnit">Đơn vị tính <span class="required">*</span></label>
                                    <input type="text" id="editUnit" name="unit" class="prod-input" value="${product != null ? product.unit : 'Gói / Năm'}" required>
                                </div>
                                <div class="prod-form-group">
                                    <label class="prod-label" for="editStatus">Trạng thái kinh doanh</label>
                                    <select id="editStatus" name="status" class="prod-select">
                                        <option value="ACTIVE" selected>Đang kinh doanh</option>
                                        <option value="INACTIVE">Ngừng kinh doanh</option>
                                    </select>
                                </div>
                            </div>

                            <div class="prod-form-row">
                                <div class="prod-form-group">
                                    <label class="prod-label" for="editListPrice">Giá niêm yết <span class="required">*</span></label>
                                    <div class="prod-input-group">
                                        <input type="number" id="editListPrice" name="listPrice" class="prod-input" min="0" step="1000" value="${product != null ? product.listPrice : '24000000'}" required>
                                        <span class="prod-input-suffix">₫</span>
                                    </div>
                                </div>
                                <div class="prod-form-group">
                                    <label class="prod-label" for="editFloorPrice">Giá sàn (Duyệt) <span class="required">*</span></label>
                                    <div class="prod-input-group">
                                        <input type="number" id="editFloorPrice" name="floorPrice" class="prod-input" min="0" step="1000" value="${product != null ? product.floorPrice : '19200000'}" required>
                                        <span class="prod-input-suffix">₫</span>
                                    </div>
                                </div>
                            </div>

                            <div class="prod-hint-warning">
                                ⚠️ Cập nhật giá sàn sẽ điều chỉnh ngưỡng bắt buộc duyệt chiết khấu cho các báo giá tạo mới.
                            </div>

                            <div class="prod-form-group">
                                <label class="prod-label" for="editCostPrice">Giá vốn (Chỉ Giám đốc kinh doanh)</label>
                                <div class="prod-input-group">
                                    <input type="number" id="editCostPrice" name="costPrice" class="prod-input" min="0" step="1000" value="${product != null ? product.costPrice : '9600000'}">
                                    <span class="prod-input-suffix">₫</span>
                                </div>
                            </div>

                            <div class="prod-form-group">
                                <label class="prod-label" for="editDesc">Mô tả sản phẩm</label>
                                <textarea id="editDesc" name="description" class="prod-textarea" rows="4">${product != null ? product.description : 'Hệ thống CRM toàn diện trên đám mây, không giới hạn người dùng.'}</textarea>
                            </div>

                            <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; padding-top: 16px; border-top: 1px solid var(--prod-border);">
                                <a href="${pageContext.request.contextPath}/products" class="btn-prod btn-prod-secondary">Hủy bỏ</a>
                                <button type="submit" class="btn-prod btn-prod-primary">Lưu cập nhật</button>
                            </div>

                        </div>
                    </form>
                </section>

            </div>
        </main>
    </div>

    <script>
        function validateEditForm(e) {
            const listPrice = parseFloat(document.getElementById("editListPrice").value);
            const floorPrice = parseFloat(document.getElementById("editFloorPrice").value);

            if (floorPrice > listPrice) {
                alert("Lỗi: Giá sàn không thể lớn hơn Giá niêm yết!");
                e.preventDefault();
                return false;
            }
            return true;
        }
    </script>
</body>
</html>