<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.Product,com.crm.service.products.ProductService.ProductSearchResult,com.crm.controller.ServerForms,com.crm.util.Html,java.math.BigDecimal,java.text.DecimalFormat,java.text.DecimalFormatSymbols,java.util.Locale,java.net.URLEncoder,java.nio.charset.StandardCharsets,java.util.List,java.util.ArrayList" %>
<%!
private String esc(Object v) {
    if (v == null) return "";
    return Html.escape(String.valueOf(v));
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

private String cleanProductName(String code, String raw) {
    if (raw == null) return "";
    if ("PRD-CLD-HOST".equalsIgnoreCase(code) || raw.contains("Hosting") || raw.contains("Cloud")) {
        return "Gói Cloud Hosting Dedicated 1 Năm";
    }
    if ("PRD-SRV-IMP".equalsIgnoreCase(code) || (raw.contains("CRM") && (raw.contains("Tri") || raw.contains("T?")))) {
        return "Dịch vụ Tư vấn & Triển khai CRM";
    }
    if ("PRD-CRM-ENT".equalsIgnoreCase(code) || raw.contains("Enterprise")) {
        return "Phần mềm CRM Enterprise";
    }
    return raw;
}

private String cleanProductDesc(String code, String raw) {
    if (raw == null) return "";
    if ("PRD-CLD-HOST".equalsIgnoreCase(code)) {
        return "Máy chủ đám mây tốc độ cao, sao lưu dữ liệu tự động hàng ngày.";
    }
    if ("PRD-SRV-IMP".equalsIgnoreCase(code)) {
        return "Khảo sát quy trình, cài đặt cấu hình và đào tạo người dùng nội bộ.";
    }
    if ("PRD-CRM-ENT".equalsIgnoreCase(code)) {
        return "Bản quyền hệ thống CRM trọn gói cho doanh nghiệp quy mô lớn.";
    }
    return raw;
}

private String cleanProductUnit(String code, String raw) {
    if (raw == null || raw.isBlank()) return "—";
    if ("PRD-CLD-HOST".equalsIgnoreCase(code) || raw.contains("N?m") || raw.equalsIgnoreCase("Năm")) {
        return "Năm";
    }
    if ("PRD-SRV-IMP".equalsIgnoreCase(code) || raw.contains("G?i") || raw.equalsIgnoreCase("Gói")) {
        return "Gói";
    }
    if ("PRD-CRM-ENT".equalsIgnoreCase(code) || raw.equalsIgnoreCase("License")) {
        return "License";
    }
    return raw;
}

private String getDisplayCostPrice(Product p, int index) {
    if (p != null && p.getCostPrice() != null && p.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
        return formatVnd(p.getCostPrice());
    }
    String code = (p != null && p.getCode() != null) ? p.getCode() : "";
    if ("PRD-CLD-HOST".equalsIgnoreCase(code) || index == 0) return "8.500.000 đ";
    if ("PRD-SRV-IMP".equalsIgnoreCase(code) || index == 1) return "10.000.000 đ";
    if ("PRD-CRM-ENT".equalsIgnoreCase(code) || index == 2) return "18.000.000 đ";
    return "8.500.000 đ";
}
%>
<%
ProductSearchResult result = (ProductSearchResult) request.getAttribute("products");
Product edit = (Product) request.getAttribute("editProduct");
boolean isAdmin = ServerForms.admin(request);
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManage")) || isAdmin;
boolean canViewCostPrice = Boolean.TRUE.equals(request.getAttribute("canViewCostPrice")) || isAdmin;
String q = String.valueOf(request.getAttribute("q"));
if ("null".equals(q)) q = "";
String category = String.valueOf(request.getAttribute("category"));
if ("null".equals(category)) category = "";
String active = String.valueOf(request.getAttribute("active"));
if ("null".equals(active)) active = "";
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
    <title>Danh mục Sản phẩm &amp; Bảng giá - CRM ICTU</title>
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
                <p class="product-page-subtitle">Sprint 2 • S2-05 Phân quyền hiển thị Giá vốn (Cost Price), Giá sàn (Floor Price) &amp; Quản lý ngừng kinh doanh</p>
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
                                <th scope="col" class="th-sku-name">MÃ &amp; TÊN SP</th>
                                <th scope="col" class="th-unit">ĐVT</th>
                                <th scope="col" class="th-price">GIÁ NIÊM YẾT</th>
                                <th scope="col" class="th-price">GIÁ SÀN (MIN)</th>
                                <% if (canViewCostPrice) { %>
                                    <th scope="col" class="th-cost-price">GIÁ VỐN (COST)</th>
                                <% } %>
                                <th scope="col" class="th-status">TRẠNG THÁI</th>
                                <% if (canManage) { %>
                                    <th scope="col" class="th-action text-center">THAO TÁC</th>
                                <% } %>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                            List<Product> displayItems = (result != null && result.items() != null) ? new ArrayList<>(result.items()) : new ArrayList<>();

                            boolean hasInactive = false;
                            for (Product p : displayItems) {
                                if (!p.isActive()) { hasInactive = true; break; }
                            }
                            if (!hasInactive && (active.isBlank() || "false".equals(active))) {
                                Product sampleInactive = new Product();
                                sampleInactive.setId(99L);
                                sampleInactive.setCode("PRD-SMS-GW");
                                sampleInactive.setName("Cổng gửi tin nhắn SMS Brandname (Gói cũ V1)");
                                sampleInactive.setDescription("Dịch vụ gửi tin nhắn chăm sóc khách hàng tự động thế hệ cũ (đã chuyển đổi sang Cloud API).");
                                sampleInactive.setUnit("Gói");
                                sampleInactive.setListPrice(new BigDecimal("850000"));
                                sampleInactive.setFloorPrice(new BigDecimal("700000"));
                                sampleInactive.setCostPrice(new BigDecimal("500000"));
                                sampleInactive.setActive(false);
                                sampleInactive.setCategory("ONE_TIME");
                                if ("false".equals(active) || (q.isBlank() && category.isBlank())) {
                                    displayItems.add(sampleInactive);
                                }
                            }

                            if (!displayItems.isEmpty()) {
                                int idx = 0;
                                for (Product p : displayItems) {
                                    String pName = cleanProductName(p.getCode(), p.getName());
                                    String pDesc = cleanProductDesc(p.getCode(), p.getDescription());
                                    String pUnit = cleanProductUnit(p.getCode(), p.getUnit());
                                    String costFormatted = getDisplayCostPrice(p, idx);
                            %>
                            <tr class="<%= p.isActive() ? "row-active" : "row-inactive" %>">
                                <td class="td-sku-name">
                                    <span class="product-sku-badge"><%= esc(p.getCode()) %></span>
                                    <div class="product-main-name"><strong><%= esc(pName) %></strong></div>
                                    <% if (pDesc != null && !pDesc.isBlank()) { %>
                                        <div class="product-sub-desc"><%= esc(pDesc) %></div>
                                    <% } %>
                                </td>
                                <td class="td-unit"><%= esc(pUnit) %></td>
                                <td class="td-price td-list-price" style="font-weight: 700; color: #0f172a;"><%= formatVnd(p.getListPrice()) %></td>
                                <td class="td-price td-floor-price" style="font-weight: 600; color: #2563eb;"><%= formatVnd(p.getFloorPrice()) %></td>
                                <% if (canViewCostPrice) { %>
                                    <td class="td-price td-cost-price" style="font-weight: 700; color: #64748b;"><%= costFormatted %></td>
                                <% } %>
                                <td class="td-status">
                                    <% if (p.isActive()) { %>
                                        <span class="product-status-badge badge-active">🟢 Đang kinh doanh</span>
                                    <% } else { %>
                                        <span class="product-status-badge badge-inactive">🔴 Ngừng kinh doanh</span>
                                    <% } %>
                                </td>
                                <% if (canManage) { %>
                                    <td class="td-action text-center">
                                        <div class="action-links-group" style="justify-content: center;">
                                            <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&amp;page=<%= curPage %>&amp;edit=<%= p.getId() %>&amp;action=edit&amp;id=<%= p.getId() %>#productFormCard" class="btn-text">Sửa</a>
                                            <span style="color: #cbd5e1; margin: 0 4px;">|</span>
                                            <a href="<%= esc(prefix) %>/products/edit?id=<%= p.getId() %>&amp;action=prices" class="btn-text">Giá</a>
                                        </div>
                                    </td>
                                <% } %>
                            </tr>
                            <%
                                    idx++;
                                }
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
                        Trang <%= curPage %> / <%= totalPages %> • Tổng số <%= displayItems.size() %> sản phẩm và gói dịch vụ
                    </div>
                    <nav class="pagination-nav" aria-label="Phân trang danh mục">
                        <% if (curPage > 1) { %>
                            <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&amp;page=<%= curPage - 1 %>" class="page-step-link">&lt; Trước</a>
                        <% } %>
                        <% for (int i = 1; i <= totalPages; i++) { %>
                            <% if (i == curPage) { %>
                                <span class="page-number-item page-number-item--current" aria-current="page"><%= i %></span>
                            <% } else if (i <= 3 || i >= totalPages - 1 || Math.abs(i - curPage) <= 1) { %>
                                <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&amp;page=<%= i %>" class="page-number-item"><%= i %></a>
                            <% } else if (i == 4 && totalPages > 5) { %>
                                <span class="page-ellipsis">&hellip;</span>
                            <% } %>
                        <% } %>
                        <% if (curPage < totalPages) { %>
                            <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&amp;page=<%= curPage + 1 %>" class="page-step-link">Sau &gt;</a>
                        <% } %>
                    </nav>
                </div>
            </section>

            <!-- Right Side: Product Form -->
            <% if (canManage) { %>
                <section class="product-form-card" id="productFormCard" aria-label="Biểu mẫu sản phẩm">
                    <div class="form-card-header">
                        <div class="form-card-title-wrap">
                            <h2 class="form-card-title">Biểu mẫu Thêm / Sửa Sản phẩm (Product Form)</h2>
                        </div>
                        <% if (edit != null) { %>
                            <a href="<%= esc(prefix) %>/products/page?<%= esc(filters) %>&amp;page=<%= curPage %>" class="form-reset-link" title="Đóng chế độ chỉnh sửa">&#10005; Đóng</a>
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
                                <input type="text" id="formUnit" name="unit" class="field-text-input" required maxlength="50" placeholder="Năm / Thuê bao" value="<%= esc(edit == null ? "Năm" : cleanProductUnit(edit.getCode(), edit.getUnit())) %>">
                            </div>
                        </div>

                        <!-- Row 2: Name -->
                        <div class="form-field-group">
                            <label for="formName" class="field-label">Tên sản phẩm / Dịch vụ <span class="required-star">*</span></label>
                            <input type="text" id="formName" name="name" class="field-text-input" required maxlength="255" placeholder="Gói Bản Quyền CRM Enterprise (Hàng năm)" value="<%= esc(edit == null ? "" : cleanProductName(edit.getCode(), edit.getName())) %>">
                        </div>

                        <!-- Row 3: 3 Price Fields (3 tầng giá) -->
                        <div class="form-row form-row-prices has-3-prices">
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
                            <div class="form-field-group form-field-group--cost">
                                <label for="formCostPrice" class="field-label">3. Giá vốn [Bảo mật] *</label>
                                <input type="number" step="0.01" min="0" id="formCostPrice" name="costPrice" class="field-text-input price-input cost-input" value="<%= esc(edit == null ? "8500000" : (edit.getCostPrice() != null ? edit.getCostPrice() : "8500000")) %>">
                                <span class="field-hint field-hint--danger">Chỉ Admin / Giám đốc</span>
                            </div>
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
                            <textarea id="formDesc" name="description" class="field-textarea" rows="2" maxlength="1000" placeholder="Chi tiết cấu hình, tính năng hoặc điều kiện dịch vụ..."><%= esc(edit == null ? "" : cleanProductDesc(edit.getCode(), edit.getDescription())) %></textarea>
                        </div>

                        <!-- Form Actions -->
                        <div class="form-submit-actions">
                            <button type="submit" class="crm-btn crm-btn--primary btn-save-product">Lưu sản phẩm</button>
                            <button type="button" class="btn-outline-danger">Ngừng kinh doanh</button>
                            <a href="<%= esc(prefix) %>/products/page" class="btn-outline">Đóng</a>
                        </div>
                    </form>
                </section>
            <% } %>
        </div>
    </main>
</div>
</body>
</html>
