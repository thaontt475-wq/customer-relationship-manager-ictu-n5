<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.controller.ServerForms" %>
<%!
private String esc(Object v) {
    if (v == null) return "";
    return v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
%>
<%
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManage"));
boolean canViewCostPrice = Boolean.TRUE.equals(request.getAttribute("canViewCostPrice"));
String prefix = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thêm sản phẩm mới - CRM</title>
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/components.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/products/products.css">
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-page products-page-shell" id="mainContent">
        <!-- Breadcrumb -->
        <nav class="crm-breadcrumb" aria-label="Đường dẫn trang">
            <a href="<%= esc(prefix) %>/products/page" class="crm-breadcrumb-item">Sản phẩm &amp; Bảng giá</a>
            <span class="crm-breadcrumb-sep" aria-hidden="true">/</span>
            <span class="crm-breadcrumb-item crm-breadcrumb-item--current">Thêm sản phẩm mới</span>
        </nav>

        <div class="product-page-header">
            <div class="product-header-info">
                <h1 class="product-page-title">Biểu mẫu Thêm Sản phẩm (Product Form)</h1>
                <p class="product-page-subtitle">Khai báo thông tin sản phẩm và chính sách 3 tầng giá (Giá niêm yết, Giá sàn, Giá vốn)</p>
            </div>
            <div class="product-header-actions">
                <a href="<%= esc(prefix) %>/products/page" class="crm-btn crm-btn--secondary">&larr; Quay lại danh sách</a>
            </div>
        </div>

        <div class="product-standalone-form-wrap">
            <section class="product-form-card" aria-label="Biểu mẫu tạo sản phẩm">
                <form method="post" action="<%= esc(prefix) %>/products/page" class="product-pure-form">
                    <input type="hidden" name="csrfToken" value="<%= esc(ServerForms.csrf(request)) %>">
                    <input type="hidden" name="operation" value="create">

                    <!-- Row 1: Code + Unit -->
                    <div class="form-row form-row-2">
                        <div class="form-field-group">
                            <label for="pCode" class="field-label">Mã sản phẩm (SKU) <span class="required-star">*</span></label>
                            <input type="text" id="pCode" name="code" class="field-text-input" required maxlength="50" placeholder="CRM-ENTERPRISE-01">
                        </div>
                        <div class="form-field-group">
                            <label for="pUnit" class="field-label">Đơn vị tính (Unit) <span class="required-star">*</span></label>
                            <input type="text" id="pUnit" name="unit" class="field-text-input" required maxlength="50" placeholder="Năm / Thuê bao" value="Năm">
                        </div>
                    </div>

                    <!-- Row 2: Name -->
                    <div class="form-field-group">
                        <label for="pName" class="field-label">Tên sản phẩm / Dịch vụ <span class="required-star">*</span></label>
                        <input type="text" id="pName" name="name" class="field-text-input" required maxlength="255" placeholder="Gói Bản Quyền CRM Enterprise (Hàng năm)">
                    </div>

                    <!-- Row 3: 3 Price Fields -->
                    <div class="form-row form-row-prices <%= canViewCostPrice ? "has-3-prices" : "has-2-prices" %>">
                        <div class="form-field-group">
                            <label for="pListPrice" class="field-label">1. Giá niêm yết <span class="required-star">*</span></label>
                            <input type="number" step="0.01" min="0" id="pListPrice" name="listPrice" class="field-text-input price-input" required value="0">
                            <span class="field-hint">Công khai cho Sales</span>
                        </div>
                        <div class="form-field-group">
                            <label for="pFloorPrice" class="field-label">2. Giá sàn (Min) <span class="required-star">*</span></label>
                            <input type="number" step="0.01" min="0" id="pFloorPrice" name="floorPrice" class="field-text-input price-input" required value="0">
                            <span class="field-hint">Ngưỡng chặn chiết khấu</span>
                        </div>
                        <% if (canViewCostPrice) { %>
                            <div class="form-field-group form-field-group--cost">
                                <label for="pCostPrice" class="field-label">3. Giá vốn [Bảo mật] <span class="required-star">*</span></label>
                                <input type="number" step="0.01" min="0" id="pCostPrice" name="costPrice" class="field-text-input price-input cost-input">
                                <span class="field-hint field-hint--danger">Chỉ Admin / Giám đốc</span>
                            </div>
                        <% } %>
                    </div>

                    <!-- Row 4: Category & Status -->
                    <div class="form-row form-row-2">
                        <div class="form-field-group">
                            <label for="pCategory" class="field-label">Loại sản phẩm</label>
                            <select id="pCategory" name="category" class="field-select">
                                <option value="SUBSCRIPTION">Thuê bao (Subscription)</option>
                                <option value="ONE_TIME">Một lần (One-Time)</option>
                            </select>
                        </div>
                        <div class="form-field-group">
                            <label for="pActive" class="field-label">Trạng thái sản phẩm:</label>
                            <select id="pActive" name="active" class="field-select">
                                <option value="true" selected>Đang bán (Active)</option>
                                <option value="false">Ngừng kinh doanh (Inactive)</option>
                            </select>
                            <span class="field-hint">Bật / Tắt trạng thái trên toàn bộ hệ thống</span>
                        </div>
                    </div>

                    <!-- Description -->
                    <div class="form-field-group">
                        <label for="pDesc" class="field-label">Mô tả sản phẩm</label>
                        <textarea id="pDesc" name="description" class="field-textarea" rows="3" maxlength="1000" placeholder="Chi tiết cấu hình, tính năng hoặc điều kiện dịch vụ..."></textarea>
                    </div>

                    <!-- Actions -->
                    <div class="form-submit-actions">
                        <button type="submit" class="crm-btn crm-btn--primary">Lưu sản phẩm</button>
                        <a href="<%= esc(prefix) %>/products/page" class="crm-btn crm-btn--secondary">Hủy bỏ</a>
                    </div>
                </form>
            </section>
        </div>
    </main>
</div>
</body>
</html>