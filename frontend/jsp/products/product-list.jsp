<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.Product,com.crm.service.products.ProductService.ProductSearchResult,com.crm.controller.ServerForms,java.math.BigDecimal,java.text.DecimalFormat,java.text.DecimalFormatSymbols,java.util.Locale,java.net.URLEncoder,java.nio.charset.StandardCharsets" %>
<%!
private String esc(Object v) {
    if (v == null) return "";
    return v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String url(Object v) {
    return URLEncoder.encode(v == null ? "" : v.toString(), StandardCharsets.UTF_8);
}
private String formatVnd(BigDecimal amount) {
    if (amount == null) return "—";
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
    symbols.setGroupingSeparator('.');
    DecimalFormat df = new DecimalFormat("#,##0", symbols);
    return df.format(amount) + " đ";
}
%>
<%
ProductSearchResult result = (ProductSearchResult) request.getAttribute("products");
Product edit = (Product) request.getAttribute("editProduct");
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManage"));
boolean canViewCostPrice = Boolean.TRUE.equals(request.getAttribute("canViewCostPrice"));
String q = String.valueOf(request.getAttribute("q"));
String category = String.valueOf(request.getAttribute("category"));
String active = String.valueOf(request.getAttribute("active"));
String prefix = request.getContextPath();
String filters = "q=" + url(q) + "&category=" + url(category) + "&active=" + url(active);
int totalCount = result != null ? (int) result.total() : 0;
int curPage = result != null ? result.page() : 1;
int totalPages = result != null ? result.totalPages() : 1;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục Sản phẩm &amp; Bảng giá - CRM</title>
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
            <span class="crm-breadcrumb-item">Quản lý Bán hàng</span>
            <span class="crm-breadcrumb-sep" aria-hidden="true">/</span>
            <span class="crm-breadcrumb-item crm-breadcrumb-item--current">Sản phẩm &amp; Bảng giá</span>
        </nav>

        <!-- Page Header -->
        <div class="product-page-header">
            <div class="product-header-info">
                <h1 class="product-page-title">Danh mục Sản phẩm &amp; Chính sách Bảng giá (Price Books)</h1>
                <p class="product-page-subtitle">Sprint 2 • S2-05 (CRM-39) • Phân quyền hiển thị Giá vốn (Cost Price), Giá sàn (Floor Price) &amp; Quản lý ngừng kinh doanh</p>
            </div>
            <div class="product-header-actions">
                <% if (canManage) { %>
                    <a href="#productFormCard" class="crm-btn crm-btn--primary product-btn-add">+ Thêm sản phẩm</a>
                <% } %>
                <a href="<%= esc(prefix) %>/products/page" class="crm-btn crm-btn--secondary">Quản lý bảng giá</a>
            </div>
        </div>

        <% if (request.getAttribute("notice") != null) { %>
            <div class="product-notice-banner" role="status">
                <span class="notice-icon">&#10003;</span> <%= esc(request.getAttribute("notice")) %>
            </div>
        <% } %>

        <!-- Filter & Search Bar -->
        <section class="product-filter-section" aria-label="Bộ lọc tìm kiếm">
            <form method="get" action="<%= esc(prefix) %>/products/page" class="product-search-form">
                <div class="search-input-wrap">
                    <span class="search-icon" aria-hidden="true">&#128269;</span>
                    <input type="search" name="q" value="<%= esc(q) %>" class="search-text-input" placeholder="Tìm theo mã SKU, tên sản phẩm..." maxlength="255" aria-label="Tìm theo mã SKU hoặc tên">
                </div>
                <div class="filter-select-wrap">
                    <select name="active" class="filter-select" aria-label="Lọc theo trạng thái">
                        <option value="">Trạng thái: Tất cả</option>
                        <option value="true" <%= "true".equals(active) ? "selected" : "" %>>Đang kinh doanh</option>
                        <option value="false" <%= "false".equals(active) ? "selected" : "" %>>Ngừng kinh doanh</option>
                    </select>
                </div>
                <div class="filter-select-wrap">
                    <select name="category" class="filter-select" aria-label="Lọc theo loại sản phẩm">
                        <option value="">Loại: Tất cả</option>
                        <option value="ONE_TIME" <%= "ONE_TIME".equals(category) ? "selected" : "" %>>Một lần</option>
                        <option value="SUBSCRIPTION" <%= "SUBSCRIPTION".equals(category) ? "selected" : "" %>>Thuê bao</option>
                    </select>
                </div>
                <button type="submit" class="crm-btn crm-btn--primary filter-submit-btn">Tìm kiếm</button>
                <% if (!q.isBlank() || !active.isBlank() || !category.isBlank()) { %>
                    <a href="<%= esc(prefix) %>/products/page" class="filter-clear-link">Xóa lọc</a>
                <% } %>
            </form>
            <div class="product-security-pill">
                <span class="security-lock">&#128274;</span> Giá vốn: Chỉ Admin / Ban Giám Đốc
            </div>
        </section>

        <!-- Main Workspace: Table on Left + Form on Right -->
        <div class="product-workspace-grid <%= canManage ? "has-form" : "no-form" %>">
            <!-- Left Side: Table & Pagination -->
            <section class="product-table-card" aria-label="Danh sách sản phẩm">
                <div class="table-scroll-container">
                    <table class="product-data-table">
                        <thead>
                            <tr>
                                <th scope="col" class="th-sku-name">Mã &amp; Tên SP</th>
                                <th scope="col" class="th-unit">ĐVT</th>
                                <th scope="col" class="th-price">Giá niêm yết</th>
                                <th scope="col" class="th-price">Giá sàn (Min)</th>
                                <% if (canViewCostPrice) { %>
                                    <th scope="col" class="th-cost-price">Giá vốn (Cost)</th>
                                <% } %>
                                <th scope="col" class="th-status">Trạng thái</th>
                                <% if (canManage) { %>
                                    <th scope="col" class="th-action">Thao tác</th>
                                <% } %>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (result != null && !result.items().isEmpty()) {
                                for (Product p : result.items()) { %>
                                <tr class="<%= p.isActive() ? "row-active" : "row-inactive" %>">
                                    <td class="td-sku-name">
                                        <span class="product-sku-badge"><%= esc(p.getCode()) %></span>
                                        <div class="product-main-name"><%= esc(p.getName()) %></div>
                                        <% if (p.getDescription() != null && !p.getDescription().isBlank()) { %>
                                            <div class="product-sub-desc"><%= esc(p.getDescription()) %></div>
                                        <% } %>
                                    </td>
                                    <td class="td-unit"><%= esc(p.getUnit() == null || p.getUnit().isBlank() ? "—" : p.getUnit()) %></td>
                                    <td class="td-price td-list-price"><%= formatVnd(p.getListPrice()) %></td>
                                    <td class="td-price td-floor-price"><%= formatVnd(p.getFloorPrice()) %></td>
                                    <% if (canViewCostPrice) { %>
                                        <td class="td-price td-cost-price"><%= formatVnd(p.getCostPrice()) %></td>
                                    <% } %>
                                    <td class="td-status">
                                        <% if (p.isActive()) { %>
                                            <span class="crm-badge crm-badge--success">Đang kinh doanh</span>
                                        <% } else { %>
                                            <span class="crm-badge crm-badge--danger">Ngừng kinh doanh</span>
                                        <% } %>
                                    </td>
                                    <% if (canManage) { %>
                                        <td class="td-action">
                                            <div class="action-links-group">
                                                <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&page=<%= curPage %>&edit=<%= p.getId() %>#productFormCard" class="table-link-btn action-edit">Sửa</a>
                                                <a href="<%= esc(prefix) %>/products/edit?id=<%= p.getId() %>" class="table-link-btn action-price">Giá</a>
                                            </div>
                                        </td>
                                    <% } %>
                                </tr>
                            <%  }
                               } else { %>
                                <tr>
                                    <td colspan="<%= canViewCostPrice ? (canManage ? 7 : 6) : (canManage ? 6 : 5) %>" class="empty-table-msg">
                                        Không tìm thấy sản phẩm nào phù hợp với bộ lọc hiện tại.
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>

                <!-- Pagination -->
                <div class="product-pagination-bar">
                    <div class="pagination-summary">
                        Trang <%= curPage %> / <%= totalPages %> • Tổng số <%= totalCount %> sản phẩm và gói dịch vụ
                    </div>
                    <nav class="pagination-nav" aria-label="Phân trang danh mục">
                        <% if (curPage > 1) { %>
                            <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&page=<%= curPage - 1 %>" class="page-step-link">&lt; Trước</a>
                        <% } %>
                        <% for (int i = 1; i <= totalPages; i++) { %>
                            <% if (i == curPage) { %>
                                <span class="page-number-item page-number-item--current" aria-current="page"><%= i %></span>
                            <% } else if (i <= 3 || i >= totalPages - 1 || Math.abs(i - curPage) <= 1) { %>
                                <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&page=<%= i %>" class="page-number-item"><%= i %></a>
                            <% } else if (i == 4 && totalPages > 5) { %>
                                <span class="page-ellipsis">&hellip;</span>
                            <% } %>
                        <% } %>
                        <% if (curPage < totalPages) { %>
                            <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&page=<%= curPage + 1 %>" class="page-step-link">Sau &gt;</a>
                        <% } %>
                    </nav>
                </div>
            </section>

            <!-- Right Side: Product Form (CRM-39) -->
            <% if (canManage) { %>
                <section class="product-form-card" id="productFormCard" aria-label="Biểu mẫu sản phẩm">
                    <div class="form-card-header">
                        <div class="form-card-title-wrap">
                            <h2 class="form-card-title"><%= edit == null ? "Biểu mẫu Thêm Sản phẩm" : "Biểu mẫu Sửa Sản phẩm" %> (Product Form)</h2>
                            <span class="crm-badge-code">[CRM-39]</span>
                        </div>
                        <% if (edit != null) { %>
                            <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&page=<%= curPage %>" class="form-reset-link" title="Đóng chế độ chỉnh sửa">&#10005; Đóng</a>
                        <% } %>
                    </div>

                    <form method="post" action="<%= esc(prefix) %>/products/page" class="product-pure-form">
                        <input type="hidden" name="csrfToken" value="<%= esc(ServerForms.csrf(request)) %>">
                        <input type="hidden" name="operation" value="<%= edit == null ? "create" : "update" %>">
                        <% if (edit != null) { %>
                            <input type="hidden" name="id" value="<%= edit.getId() %>">
                        <% } %>

                        <!-- Row 1: Code + Unit -->
                        <div class="form-row form-row-2">
                            <div class="form-field-group">
                                <label for="formCode" class="field-label">Mã sản phẩm (SKU) <span class="required-star">*</span></label>
                                <input type="text" id="formCode" name="code" class="field-text-input" required maxlength="50" placeholder="CRM-ENTERPRISE-01" value="<%= esc(edit == null ? "" : edit.getCode()) %>">
                            </div>
                            <div class="form-field-group">
                                <label for="formUnit" class="field-label">Đơn vị tính (Unit) <span class="required-star">*</span></label>
                                <input type="text" id="formUnit" name="unit" class="field-text-input" required maxlength="50" placeholder="Năm / Thuê bao" value="<%= esc(edit == null ? "Năm" : edit.getUnit()) %>">
                            </div>
                        </div>

                        <!-- Row 2: Name -->
                        <div class="form-field-group">
                            <label for="formName" class="field-label">Tên sản phẩm / Dịch vụ <span class="required-star">*</span></label>
                            <input type="text" id="formName" name="name" class="field-text-input" required maxlength="255" placeholder="Gói Bản Quyền CRM Enterprise (Hàng năm)" value="<%= esc(edit == null ? "" : edit.getName()) %>">
                        </div>

                        <!-- Row 3: 3 Price Fields -->
                        <div class="form-row form-row-prices <%= canViewCostPrice ? "has-3-prices" : "has-2-prices" %>">
                            <div class="form-field-group">
                                <label for="formListPrice" class="field-label">1. Giá niêm yết <span class="required-star">*</span></label>
                                <input type="number" step="0.01" min="0" id="formListPrice" name="listPrice" class="field-text-input price-input" required value="<%= esc(edit == null ? "0" : edit.getListPrice()) %>">
                                <span class="field-hint">Công khai cho Sales</span>
                            </div>
                            <div class="form-field-group">
                                <label for="formFloorPrice" class="field-label">2. Giá sàn (Min) <span class="required-star">*</span></label>
                                <input type="number" step="0.01" min="0" id="formFloorPrice" name="floorPrice" class="field-text-input price-input" required value="<%= esc(edit == null ? "0" : edit.getFloorPrice()) %>">
                                <span class="field-hint">Ngưỡng chặn chiết khấu</span>
                            </div>
                            <% if (canViewCostPrice) { %>
                                <div class="form-field-group form-field-group--cost">
                                    <label for="formCostPrice" class="field-label">3. Giá vốn [Bảo mật] <span class="required-star">*</span></label>
                                    <input type="number" step="0.01" min="0" id="formCostPrice" name="costPrice" class="field-text-input price-input cost-input" value="<%= esc(edit == null ? "" : edit.getCostPrice()) %>">
                                    <span class="field-hint field-hint--danger">Chỉ Admin / Giám đốc</span>
                                </div>
                            <% } %>
                        </div>

                        <!-- Row 4: Category & Description -->
                        <div class="form-row form-row-2">
                            <div class="form-field-group">
                                <label for="formCategory" class="field-label">Loại sản phẩm</label>
                                <select id="formCategory" name="category" class="field-select">
                                    <option value="SUBSCRIPTION" <%= edit != null && "SUBSCRIPTION".equals(edit.getCategory()) ? "selected" : "" %>>Thuê bao (Subscription)</option>
                                    <option value="ONE_TIME" <%= edit != null && "ONE_TIME".equals(edit.getCategory()) ? "selected" : "" %>>Một lần (One-Time)</option>
                                </select>
                            </div>
                            <div class="form-field-group">
                                <label for="formActive" class="field-label">Trạng thái sản phẩm:</label>
                                <select id="formActive" name="active" class="field-select">
                                    <option value="true" <%= edit == null || edit.isActive() ? "selected" : "" %>>Đang bán (Active)</option>
                                    <option value="false" <%= edit != null && !edit.isActive() ? "selected" : "" %>>Ngừng kinh doanh (Inactive)</option>
                                </select>
                                <span class="field-hint">Bật / Tắt trạng thái trên toàn hệ thống</span>
                            </div>
                        </div>

                        <div class="form-field-group">
                            <label for="formDesc" class="field-label">Mô tả sản phẩm</label>
                            <textarea id="formDesc" name="description" class="field-textarea" rows="2" maxlength="1000" placeholder="Chi tiết cấu hình, tính năng hoặc điều kiện dịch vụ..."><%= esc(edit == null ? "" : edit.getDescription()) %></textarea>
                        </div>

                        <!-- Form Actions -->
                        <div class="form-submit-actions">
                            <button type="submit" class="crm-btn crm-btn--primary btn-save-product">Lưu sản phẩm</button>
                            <% if (edit != null) { %>
                                <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&page=<%= curPage %>" class="crm-btn crm-btn--secondary">Đóng</a>
                            <% } %>
                        </div>
                    </form>

                    <!-- Disable Form if Edit is Active -->
                    <% if (edit != null && edit.isActive()) { %>
                        <div class="product-disable-box">
                            <form method="post" action="<%= esc(prefix) %>/products/page" class="disable-sub-form">
                                <input type="hidden" name="csrfToken" value="<%= esc(ServerForms.csrf(request)) %>">
                                <input type="hidden" name="operation" value="disable">
                                <input type="hidden" name="id" value="<%= edit.getId() %>">
                                <div class="confirm-check-wrap">
                                    <input type="checkbox" id="confirmDisable" name="confirm" value="yes" required class="confirm-checkbox">
                                    <label for="confirmDisable" class="confirm-label">Xác nhận chuyển sang trạng thái <strong>Ngừng kinh doanh</strong></label>
                                </div>
                                <button type="submit" class="crm-btn crm-btn--danger btn-disable-product">Ngừng kinh doanh</button>
                            </form>
                        </div>
                    <% } %>
                </section>
            <% } %>
        </div>

        <!-- 4 Feature Summary Cards (from Mockup) -->
        <section class="product-spec-cards-container" aria-label="Nguyên tắc hệ thống sản phẩm">
            <div class="spec-card">
                <div class="spec-card-header">
                    <span class="spec-card-badge badge-blue">Pricing Model</span>
                    <h3 class="spec-card-title">1. Quản lý 3 tầng giá (Price Logic)</h3>
                </div>
                <ul class="spec-card-body">
                    <li><strong>Giá niêm yết:</strong> Đơn giá công bố bán tiêu chuẩn.</li>
                    <li><strong>Giá sàn (Floor Price):</strong> Ngưỡng giá tối thiểu Sales được phép chốt. Thấp hơn phải qua duyệt.</li>
                    <li><strong>Giá vốn (Cost Price):</strong> Phục vụ tính biên lợi nhuận (Margin) của từng thương vụ/báo giá.</li>
                </ul>
            </div>

            <div class="spec-card">
                <div class="spec-card-header">
                    <span class="spec-card-badge badge-red">Security AC</span>
                    <h3 class="spec-card-title">2. Phân quyền xem Giá vốn nhạy cảm</h3>
                </div>
                <ul class="spec-card-body">
                    <li><strong>Bảo mật thông tin biên lợi nhuận:</strong></li>
                    <li>Sales Rep: Ẩn hoàn toàn cột Giá vốn.</li>
                    <li>Admin / Director: Xem và chỉnh sửa đầy đủ.</li>
                    <li>Backend lọc thuộc tính costPrice trước khi trả về client để chống rò rỉ dữ liệu.</li>
                </ul>
            </div>

            <div class="spec-card">
                <div class="spec-card-header">
                    <span class="spec-card-badge badge-orange">Soft Inactive</span>
                    <h3 class="spec-card-title">3. Quản lý Trạng thái Ngừng bán</h3>
                </div>
                <ul class="spec-card-body">
                    <li><strong>Nguyên tắc toàn vẹn dữ liệu (Integrity):</strong> Không xóa cứng sản phẩm đã từng phát sinh báo giá.</li>
                    <li><strong>Chuyển trạng thái 'Ngừng kinh doanh' (Inactive):</strong> Không hiển thị khi tạo báo giá mới nhưng vẫn bảo toàn lịch sử trong hợp đồng cũ.</li>
                </ul>
            </div>

            <div class="spec-card">
                <div class="spec-card-header">
                    <span class="spec-card-badge badge-purple">System Unity</span>
                    <h3 class="spec-card-title">4. Đồng bộ Master Sidebar &amp; API</h3>
                </div>
                <ul class="spec-card-body">
                    <li>Master Sidebar duy trì 100% cấu trúc chuẩn.</li>
                    <li>Mục 'Quản lý Sản phẩm &amp; Bảng giá' giữ Active.</li>
                    <li>Ánh xạ API Contract chuẩn: GET /api/products, POST /api/products, GET /api/price-books.</li>
                    <li>Mọi thay đổi giá đều ghi nhận vào Audit Log.</li>
                </ul>
            </div>
        </section>
    </main>
</div>
</body>
</html>
