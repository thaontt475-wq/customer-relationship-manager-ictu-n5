<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ==========================================================================
  FEATURE S2-05: Quản lý danh mục sản phẩm/dịch vụ và bảng giá
  Phụ trách Frontend/View: Tiệp
  
  BE CONTRACT NEEDED:
    - GET  /products             : Lấy danh sách sản phẩm (params: search, type, status, sortBy)
    - POST /products             : Tạo mới sản phẩm (body: code, name, type, unit, listPrice, floorPrice, costPrice, status, description)
    - POST /products/update      : Cập nhật sản phẩm (body: id, code, name, type, unit, listPrice, floorPrice, costPrice, status, description)
    - POST /products/delete      : Ngừng KD / Xóa SP (body: id; nếu đã có báo giá/hợp đồng thì chuyển INACTIVE)
    - GET  /price-lists          : Lấy danh sách bảng giá
    
  PHÂN QUYỀN GIAO DIỆN (ROLE GUARDS):
    - Giá vốn (Cost Price): Chỉ Giám đốc kinh doanh (Director/Admin) mới thấy số liệu thực tế. Nhân viên (Sales Rep) bị ẩn/che.
    - Báo giá dưới Giá sàn: Phải gửi Giám đốc kinh doanh duyệt.
  ==========================================================================
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục sản phẩm & Bảng giá - CRM ICTU</title>

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

        <!-- Khu vực nội dung chính của màn hình Quản lý sản phẩm & Bảng giá -->
        <main class="prod-page" id="productApp" role="main">
            <div class="prod-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="prod-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Kinh doanh</span>
                    <span class="separator">/</span>
                    <span class="active">Sản phẩm & Bảng giá</span>
                </nav>

                <!-- Header màn hình -->
                <header class="prod-header">
                    <div class="prod-header-info">
                        <h1>Danh mục sản phẩm/dịch vụ & Bảng giá</h1>
                        <p>Quản lý danh sách hàng hóa, dịch vụ định kỳ, kiểm soát giá niêm yết, giá sàn phê duyệt và bảng giá áp dụng.</p>
                    </div>
                    <div class="prod-header-badges">
                        <span class="prod-badge-tag">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                            </svg>
                            S2-05 / Catalog & Pricing
                        </span>
                        <span class="prod-badge-tag" style="background: #f0fdf4; color: #16a34a; border-color: #bbf7d0;">
                            Kiểm soát giá sàn
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts / Banners) -->
                <div class="prod-alerts" id="prodAlertsArea" aria-live="polite">
                    <%-- Hiển thị thông báo server-side nếu có --%>
                    <% if (request.getAttribute("errorMessage") != null) { %>
                        <div class="prod-alert prod-alert-danger" role="alert">
                            <svg class="prod-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <div class="prod-alert-content">
                                <div class="prod-alert-title">Lỗi hệ thống</div>
                                <div><%= request.getAttribute("errorMessage") %></div>
                            </div>
                            <button type="button" class="prod-alert-close" onclick="this.parentElement.remove();">&times;</button>
                        </div>
                    <% } %>

                    <% if (request.getAttribute("successMessage") != null) { %>
                        <div class="prod-alert prod-alert-success" role="status">
                            <svg class="prod-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                            <div class="prod-alert-content">
                                <div class="prod-alert-title">Thành công</div>
                                <div><%= request.getAttribute("successMessage") %></div>
                            </div>
                            <button type="button" class="prod-alert-close" onclick="this.parentElement.remove();">&times;</button>
                        </div>
                    <% } %>

                    <!-- Dynamic Client Alerts -->
                    <div class="prod-alert prod-alert-danger" id="clientErrorAlert" style="display: none;" role="alert">
                        <svg class="prod-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="prod-alert-content">
                            <div class="prod-alert-title" id="clientErrorTitle">Lỗi xử lý</div>
                            <div id="clientErrorMessage"></div>
                        </div>
                        <button type="button" class="prod-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>

                    <div class="prod-alert prod-alert-success" id="clientSuccessAlert" style="display: none;" role="status">
                        <svg class="prod-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="prod-alert-content">
                            <div class="prod-alert-title" id="clientSuccessTitle">Thao tác thành công</div>
                            <div id="clientSuccessMessage"></div>
                        </div>
                        <button type="button" class="prod-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>
                </div>

                <!-- Thống kê sơ bộ (KPI Counters) -->
                <section class="prod-stats-grid" aria-label="Thống kê tổng quan sản phẩm">
                    <div class="prod-stat-card">
                        <div class="prod-stat-icon prod-stat-icon--blue" aria-hidden="true">📦</div>
                        <div class="prod-stat-data">
                            <span class="prod-stat-val" id="statTotalProducts">8</span>
                            <span class="prod-stat-lbl">Tổng số sản phẩm / DV</span>
                        </div>
                    </div>
                    <div class="prod-stat-card">
                        <div class="prod-stat-icon prod-stat-icon--green" aria-hidden="true">🔄</div>
                        <div class="prod-stat-data">
                            <span class="prod-stat-val" id="statActiveProducts">7</span>
                            <span class="prod-stat-lbl">Đang kinh doanh</span>
                        </div>
                    </div>
                    <div class="prod-stat-card">
                        <div class="prod-stat-icon prod-stat-icon--purple" aria-hidden="true">📅</div>
                        <div class="prod-stat-data">
                            <span class="prod-stat-val" id="statSubscriptionCount">4</span>
                            <span class="prod-stat-lbl">Dịch vụ thuê bao định kỳ</span>
                        </div>
                    </div>
                    <div class="prod-stat-card">
                        <div class="prod-stat-icon prod-stat-icon--amber" aria-hidden="true">🏷️</div>
                        <div class="prod-stat-data">
                            <span class="prod-stat-val" id="statPriceListsCount">3</span>
                            <span class="prod-stat-lbl">Bảng giá đang áp dụng</span>
                        </div>
                    </div>
                </section>

                <!-- Navigation Tabs: Danh mục sản phẩm vs Quản lý bảng giá -->
                <div class="prod-nav-tabs" role="tablist" aria-label="Phân hệ Quản lý sản phẩm và bảng giá">
                    <button type="button" class="prod-tab-btn active" id="tabBtnProducts" role="tab" aria-selected="true" aria-controls="tabPaneProducts" onclick="switchTab('products')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                            <line x1="12" y1="22.08" x2="12" y2="12"></line>
                        </svg>
                        <span>Danh mục sản phẩm & dịch vụ</span>
                        <span class="prod-tab-badge" id="tabBadgeProdCount">8</span>
                    </button>
                    <button type="button" class="prod-tab-btn" id="tabBtnPriceLists" role="tab" aria-selected="false" aria-controls="tabPanePriceLists" onclick="switchTab('pricelists')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <line x1="12" y1="1" x2="12" y2="23"></line>
                            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                        </svg>
                        <span>Quản lý bảng giá</span>
                        <span class="prod-tab-badge" id="tabBadgePriceListCount">3</span>
                    </button>
                </div>

                <!-- TAB 1: DANH MỤC SẢN PHẨM / DỊCH VỤ -->
                <div id="tabPaneProducts" role="tabpanel" aria-labelledby="tabBtnProducts">
                    <section class="prod-card" aria-label="Danh sách sản phẩm">
                        
                        <!-- Toolbar tìm kiếm, lọc & thêm mới -->
                        <div class="prod-toolbar">
                            <div class="prod-toolbar-left">
                                <!-- Ô tìm kiếm -->
                                <div class="prod-search-wrap">
                                    <svg class="prod-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                    </svg>
                                    <input type="search" id="prodSearchInput" class="prod-search-input"
                                           placeholder="Tìm theo mã hoặc tên sản phẩm..."
                                           aria-label="Tìm kiếm sản phẩm">
                                </div>

                                <!-- Lọc theo loại -->
                                <select id="prodTypeFilter" class="prod-filter-select" aria-label="Lọc theo loại">
                                    <option value="">Tất cả loại sản phẩm/DV</option>
                                    <option value="SUBSCRIPTION">Dịch vụ định kỳ (Subscription)</option>
                                    <option value="ONE_TIME">Sản phẩm dùng một lần</option>
                                    <option value="LICENSE">Bản quyền phần mềm</option>
                                    <option value="HARDWARE">Thiết bị phần cứng</option>
                                    <option value="CONSULTING">Tư vấn & Triển khai</option>
                                </select>

                                <!-- Lọc theo trạng thái -->
                                <select id="prodStatusFilter" class="prod-filter-select" aria-label="Lọc theo trạng thái kinh doanh">
                                    <option value="">Tất cả trạng thái</option>
                                    <option value="ACTIVE">Đang kinh doanh</option>
                                    <option value="INACTIVE">Ngừng kinh doanh</option>
                                </select>

                                <!-- Nút đặt lại bộ lọc -->
                                <button type="button" class="btn-prod btn-prod-secondary" id="btnResetFilters" title="Đặt lại bộ lọc" onclick="resetFilters()">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="1 4 1 10 7 10"></polyline>
                                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                    </svg>
                                    <span>Đặt lại</span>
                                </button>
                            </div>

                            <div class="prod-toolbar-right">
                                <!-- Chế độ giả lập phân quyền xem Giá vốn (Dành cho việc kiểm thử UI theo S2-05) -->
                                <div style="display: flex; align-items: center; gap: 8px; font-size: 0.82rem; color: var(--prod-text-sub); background: #f1f5f9; padding: 4px 10px; border-radius: 6px;">
                                    <span>Vai trò xem:</span>
                                    <select id="roleViewSelector" onchange="toggleRoleView(this.value)" style="border: none; background: transparent; font-weight: 600; cursor: pointer; color: var(--prod-primary);">
                                        <option value="DIRECTOR">Giám đốc kinh doanh (Xem giá vốn)</option>
                                        <option value="SALES">Nhân viên kinh doanh (Ẩn giá vốn)</option>
                                    </select>
                                </div>

                                <!-- Nút Thêm sản phẩm mới -->
                                <button type="button" class="btn-prod btn-prod-primary" id="btnOpenCreateModal" onclick="openCreateModal()">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <line x1="12" y1="5" x2="12" y2="19"></line>
                                        <line x1="5" y1="12" x2="19" y2="12"></line>
                                    </svg>
                                    <span>Thêm sản phẩm</span>
                                </button>
                            </div>
                        </div>

                        <!-- Bảng dữ liệu sản phẩm -->
                        <div class="prod-table-responsive">
                            <table class="prod-table" id="prodTable" aria-label="Bảng danh mục sản phẩm">
                                <thead>
                                    <tr>
                                        <th scope="col" style="width: 100px;">Mã SP</th>
                                        <th scope="col">Tên sản phẩm / Dịch vụ</th>
                                        <th scope="col">Loại</th>
                                        <th scope="col">ĐVT</th>
                                        <th scope="col" style="text-align: right;">Giá niêm yết</th>
                                        <th scope="col" style="text-align: right;">Giá sàn (Duyệt)</th>
                                        <th scope="col" id="colHeaderCostPrice" style="text-align: right;">Giá vốn</th>
                                        <th scope="col" style="text-align: center;">Trạng thái</th>
                                        <th scope="col" style="text-align: center; width: 120px;">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody id="prodTableBody">
                                    <!-- Render động qua JavaScript -->
                                </tbody>
                            </table>
                        </div>

                        <!-- Empty State khi không tìm thấy kết quả -->
                        <div class="prod-empty-state" id="prodEmptyState" style="display: none;">
                            <div class="prod-empty-icon" aria-hidden="true">🔍</div>
                            <div class="prod-empty-title">Không tìm thấy sản phẩm nào</div>
                            <p class="prod-empty-desc">Không có sản phẩm nào phù hợp với điều kiện tìm kiếm hoặc bộ lọc hiện tại.</p>
                            <button type="button" class="btn-prod btn-prod-secondary" onclick="resetFilters()">Xóa bộ lọc</button>
                        </div>

                    </section>
                </div>

                <!-- TAB 2: QUẢN LÝ BẢNG GIÁ (PRICE LISTS) -->
                <div id="tabPanePriceLists" role="tabpanel" aria-labelledby="tabBtnPriceLists" style="display: none;">
                    <section class="prod-card" aria-label="Danh sách bảng giá">
                        <div class="prod-toolbar">
                            <div>
                                <h3 style="margin: 0; font-size: 1.05rem; color: var(--prod-text-main);">Chính sách & Bảng giá áp dụng</h3>
                                <p style="margin: 4px 0 0 0; font-size: 0.85rem; color: var(--prod-text-muted);">Các bảng giá quy định mức chiết khấu và đơn giá theo từng đối tượng khách hàng</p>
                            </div>
                            <div class="prod-toolbar-right">
                                <button type="button" class="btn-prod btn-prod-primary" onclick="alert('BE CẦN CUNG CẤP: API tạo bảng giá mới (POST /price-lists). Giao diện FE đã sẵn sàng tiếp nhận.');">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <line x1="12" y1="5" x2="12" y2="19"></line>
                                        <line x1="5" y1="12" x2="19" y2="12"></line>
                                    </svg>
                                    <span>Tạo bảng giá mới</span>
                                </button>
                            </div>
                        </div>

                        <div class="prod-pricelists-container">
                            <div class="prod-pricelist-grid" id="priceListsGrid">
                                <!-- Price list cards will be rendered dynamically -->
                            </div>
                        </div>
                    </section>
                </div>

            </div>
        </main>
    </div>

    <!-- MODAL 1: THÊM SẢN PHẨM MỚI -->
    <div class="prod-modal-overlay" id="modalCreateProduct" role="dialog" aria-modal="true" aria-labelledby="modalCreateTitle">
        <div class="prod-modal">
            <div class="prod-modal-header">
                <h3 class="prod-modal-title" id="modalCreateTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Thêm sản phẩm / dịch vụ mới
                </h3>
                <button type="button" class="prod-modal-close" onclick="closeCreateModal()" aria-label="Đóng">&times;</button>
            </div>
            <form id="formCreateProduct" onsubmit="handleCreateProduct(event)">
                <div class="prod-modal-body">
                    <div class="prod-form-row">
                        <div class="prod-form-group">
                            <label class="prod-label" for="createProdCode">Mã sản phẩm <span class="required">*</span></label>
                            <input type="text" id="createProdCode" class="prod-input" placeholder="VD: SP-009, DV-010" required>
                        </div>
                        <div class="prod-form-group">
                            <label class="prod-label" for="createProdType">Loại sản phẩm <span class="required">*</span></label>
                            <select id="createProdType" class="prod-select" required>
                                <option value="SUBSCRIPTION">Dịch vụ định kỳ (Subscription)</option>
                                <option value="ONE_TIME">Sản phẩm dùng 1 lần</option>
                                <option value="LICENSE">Bản quyền phần mềm</option>
                                <option value="HARDWARE">Thiết bị phần cứng</option>
                                <option value="CONSULTING">Tư vấn & Triển khai</option>
                            </select>
                        </div>
                    </div>

                    <div class="prod-form-group">
                        <label class="prod-label" for="createProdName">Tên sản phẩm / Dịch vụ <span class="required">*</span></label>
                        <input type="text" id="createProdName" class="prod-input" placeholder="Nhập tên gọi sản phẩm hoặc gói dịch vụ..." required>
                    </div>

                    <div class="prod-form-row">
                        <div class="prod-form-group">
                            <label class="prod-label" for="createProdUnit">Đơn vị tính <span class="required">*</span></label>
                            <input type="text" id="createProdUnit" class="prod-input" placeholder="VD: Gói/Năm, License, Giờ, Chiếc" required>
                        </div>
                        <div class="prod-form-group">
                            <label class="prod-label" for="createProdStatus">Trạng thái kinh doanh</label>
                            <select id="createProdStatus" class="prod-select">
                                <option value="ACTIVE">Đang kinh doanh</option>
                                <option value="INACTIVE">Ngừng kinh doanh</option>
                            </select>
                        </div>
                    </div>

                    <div class="prod-form-row">
                        <div class="prod-form-group">
                            <label class="prod-label" for="createListPrice">Giá niêm yết <span class="required">*</span></label>
                            <div class="prod-input-group">
                                <input type="number" id="createListPrice" class="prod-input" min="0" step="1000" placeholder="25000000" required>
                                <span class="prod-input-suffix">₫</span>
                            </div>
                            <span class="prod-hint">Giá bán công khai chuẩn</span>
                        </div>
                        <div class="prod-form-group">
                            <label class="prod-label" for="createFloorPrice">Giá sàn (Duyệt) <span class="required">*</span></label>
                            <div class="prod-input-group">
                                <input type="number" id="createFloorPrice" class="prod-input" min="0" step="1000" placeholder="20000000" required>
                                <span class="prod-input-suffix">₫</span>
                            </div>
                            <span class="prod-hint">Ngưỡng tối thiểu được chào giá</span>
                        </div>
                    </div>

                    <div class="prod-hint-warning">
                        ⚠️ <strong>Quy tắc giá sàn:</strong> Báo giá có đơn giá dưới mức Giá sàn sẽ bắt buộc phải gửi Giám đốc kinh doanh phê duyệt trước khi gửi khách hàng.
                    </div>

                    <div class="prod-form-group" id="groupCreateCostPrice">
                        <label class="prod-label" for="createCostPrice">Giá vốn (Chỉ Giám đốc kinh doanh)</label>
                        <div class="prod-input-group">
                            <input type="number" id="createCostPrice" class="prod-input" min="0" step="1000" placeholder="12000000">
                            <span class="prod-input-suffix">₫</span>
                        </div>
                        <span class="prod-hint">Thông tin bảo mật chi phí vốn phục vụ tính toán lợi nhuận gộp</span>
                    </div>

                    <div class="prod-form-group">
                        <label class="prod-label" for="createProdDesc">Mô tả sản phẩm</label>
                        <textarea id="createProdDesc" class="prod-textarea" rows="3" placeholder="Nhập mô tả tính năng, phạm vi hỗ trợ..."></textarea>
                    </div>
                </div>
                <div class="prod-modal-footer">
                    <button type="button" class="btn-prod btn-prod-secondary" onclick="closeCreateModal()">Hủy</button>
                    <button type="submit" class="btn-prod btn-prod-primary">Lưu sản phẩm</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 2: CHỈNH SỬA SẢN PHẨM -->
    <div class="prod-modal-overlay" id="modalEditProduct" role="dialog" aria-modal="true" aria-labelledby="modalEditTitle">
        <div class="prod-modal">
            <div class="prod-modal-header">
                <h3 class="prod-modal-title" id="modalEditTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                    Chỉnh sửa sản phẩm / dịch vụ
                </h3>
                <button type="button" class="prod-modal-close" onclick="closeEditModal()" aria-label="Đóng">&times;</button>
            </div>
            <form id="formEditProduct" onsubmit="handleUpdateProduct(event)">
                <input type="hidden" id="editProdId">
                <div class="prod-modal-body">
                    <div class="prod-form-row">
                        <div class="prod-form-group">
                            <label class="prod-label" for="editProdCode">Mã sản phẩm</label>
                            <input type="text" id="editProdCode" class="prod-input" readonly style="background-color: #f1f5f9; cursor: not-allowed;">
                        </div>
                        <div class="prod-form-group">
                            <label class="prod-label" for="editProdType">Loại sản phẩm <span class="required">*</span></label>
                            <select id="editProdType" class="prod-select" required>
                                <option value="SUBSCRIPTION">Dịch vụ định kỳ (Subscription)</option>
                                <option value="ONE_TIME">Sản phẩm dùng 1 lần</option>
                                <option value="LICENSE">Bản quyền phần mềm</option>
                                <option value="HARDWARE">Thiết bị phần cứng</option>
                                <option value="CONSULTING">Tư vấn & Triển khai</option>
                            </select>
                        </div>
                    </div>

                    <div class="prod-form-group">
                        <label class="prod-label" for="editProdName">Tên sản phẩm / Dịch vụ <span class="required">*</span></label>
                        <input type="text" id="editProdName" class="prod-input" required>
                    </div>

                    <div class="prod-form-row">
                        <div class="prod-form-group">
                            <label class="prod-label" for="editProdUnit">Đơn vị tính <span class="required">*</span></label>
                            <input type="text" id="editProdUnit" class="prod-input" required>
                        </div>
                        <div class="prod-form-group">
                            <label class="prod-label" for="editProdStatus">Trạng thái kinh doanh</label>
                            <select id="editProdStatus" class="prod-select">
                                <option value="ACTIVE">Đang kinh doanh</option>
                                <option value="INACTIVE">Ngừng kinh doanh</option>
                            </select>
                        </div>
                    </div>

                    <div class="prod-form-row">
                        <div class="prod-form-group">
                            <label class="prod-label" for="editListPrice">Giá niêm yết <span class="required">*</span></label>
                            <div class="prod-input-group">
                                <input type="number" id="editListPrice" class="prod-input" min="0" step="1000" required>
                                <span class="prod-input-suffix">₫</span>
                            </div>
                        </div>
                        <div class="prod-form-group">
                            <label class="prod-label" for="editFloorPrice">Giá sàn (Duyệt) <span class="required">*</span></label>
                            <div class="prod-input-group">
                                <input type="number" id="editFloorPrice" class="prod-input" min="0" step="1000" required>
                                <span class="prod-input-suffix">₫</span>
                            </div>
                        </div>
                    </div>

                    <div class="prod-hint-warning">
                        ⚠️ Thay đổi giá sàn sẽ cập nhật ngưỡng kiểm soát chiết khấu cho các báo giá tạo mới.
                    </div>

                    <div class="prod-form-group" id="groupEditCostPrice">
                        <label class="prod-label" for="editCostPrice">Giá vốn</label>
                        <div class="prod-input-group">
                            <input type="number" id="editCostPrice" class="prod-input" min="0" step="1000">
                            <span class="prod-input-suffix">₫</span>
                        </div>
                    </div>

                    <div class="prod-form-group">
                        <label class="prod-label" for="editProdDesc">Mô tả sản phẩm</label>
                        <textarea id="editProdDesc" class="prod-textarea" rows="3"></textarea>
                    </div>
                </div>
                <div class="prod-modal-footer">
                    <button type="button" class="btn-prod btn-prod-secondary" onclick="closeEditModal()">Hủy</button>
                    <button type="submit" class="btn-prod btn-prod-primary">Lưu cập nhật</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 3: XEM CHI TIẾT SẢN PHẨM -->
    <div class="prod-modal-overlay" id="modalDetailProduct" role="dialog" aria-modal="true" aria-labelledby="modalDetailTitle">
        <div class="prod-modal">
            <div class="prod-modal-header">
                <h3 class="prod-modal-title" id="modalDetailTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    Chi tiết sản phẩm / dịch vụ
                </h3>
                <button type="button" class="prod-modal-close" onclick="closeDetailModal()" aria-label="Đóng">&times;</button>
            </div>
            <div class="prod-modal-body">
                <div class="prod-detail-grid">
                    <span class="prod-detail-label">Mã sản phẩm:</span>
                    <span class="prod-detail-val" id="detailProdCode" style="font-weight: 700; color: var(--prod-primary);"></span>

                    <span class="prod-detail-label">Tên sản phẩm:</span>
                    <span class="prod-detail-val" id="detailProdName" style="font-weight: 600;"></span>

                    <span class="prod-detail-label">Phân loại:</span>
                    <span class="prod-detail-val" id="detailProdType"></span>

                    <span class="prod-detail-label">Đơn vị tính:</span>
                    <span class="prod-detail-val" id="detailProdUnit"></span>

                    <span class="prod-detail-label">Trạng thái:</span>
                    <span class="prod-detail-val" id="detailProdStatus"></span>

                    <span class="prod-detail-label">Giá niêm yết:</span>
                    <span class="prod-detail-val" id="detailListPrice" style="font-weight: 700; font-size: 1.05rem; color: var(--prod-text-main);"></span>

                    <span class="prod-detail-label">Giá sàn duyệt:</span>
                    <span class="prod-detail-val" id="detailFloorPrice" style="font-weight: 700; color: #b45309;"></span>

                    <span class="prod-detail-label">Giá vốn:</span>
                    <span class="prod-detail-val" id="detailCostPrice"></span>

                    <span class="prod-detail-label">Mô tả:</span>
                    <span class="prod-detail-val" id="detailProdDesc" style="line-height: 1.5; color: var(--prod-text-sub);"></span>
                </div>

                <div class="prod-hint-warning" style="margin-top: 8px;">
                    ℹ️ <strong>Chính sách bảng giá liên kết:</strong> Sản phẩm này hiện nằm trong <strong>Bảng giá chuẩn</strong> và <strong>Bảng giá Doanh nghiệp VIP</strong>. Mọi thay đổi về giá sẽ có hiệu lực ngay trong bảng báo giá tạo mới.
                </div>
            </div>
            <div class="prod-modal-footer">
                <button type="button" class="btn-prod btn-prod-secondary" onclick="closeDetailModal()">Đóng</button>
                <button type="button" class="btn-prod btn-prod-primary" id="btnDetailToEdit">Chỉnh sửa</button>
            </div>
        </div>
    </div>

    <!-- MODAL 4: XÁC NHẬN NGỪNG KINH DOANH / XÓA SẢN PHẨM -->
    <div class="prod-modal-overlay" id="modalDeleteProduct" role="dialog" aria-modal="true" aria-labelledby="modalDeleteTitle">
        <div class="prod-modal" style="max-width: 480px;">
            <div class="prod-modal-header">
                <h3 class="prod-modal-title" id="modalDeleteTitle" style="color: var(--prod-danger);">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    Xác nhận thao tác sản phẩm
                </h3>
                <button type="button" class="prod-modal-close" onclick="closeDeleteModal()" aria-label="Đóng">&times;</button>
            </div>
            <div class="prod-modal-body">
                <p style="margin: 0; line-height: 1.5; font-size: 0.92rem;">
                    Bạn đang yêu cầu xóa / ngừng kinh doanh sản phẩm: <strong id="deleteProdNameDisplay"></strong> (<span id="deleteProdCodeDisplay"></span>).
                </p>

                <div class="prod-hint-warning" style="margin-top: 12px; background: #fef2f2; border-color: #fecaca; color: #991b1b;">
                    🛡️ <strong>Ràng buộc toàn vẹn dữ liệu:</strong>
                    Nếu sản phẩm đã được sử dụng trong bất kỳ Báo giá hoặc Hợp đồng nào, hệ thống Backend sẽ <strong>không cho phép xóa vĩnh viễn</strong> mà sẽ tự động chuyển sang trạng thái <strong>Ngừng kinh doanh</strong> để lưu giữ lịch sử kiểm toán.
                </div>
            </div>
            <div class="prod-modal-footer">
                <button type="button" class="btn-prod btn-prod-secondary" onclick="closeDeleteModal()">Hủy</button>
                <button type="button" class="btn-prod btn-prod-danger" id="btnConfirmDelete" onclick="confirmDeleteProduct()">Xác nhận thực hiện</button>
            </div>
        </div>
    </div>

    <!-- JavaScript Xử lý Tương tác Frontend (S2-05) -->
    <script>
        // Dữ liệu ban đầu (Mock Catalog Data theo đặc tả S2-05)
        // Khi BE cung cấp API /products, dữ liệu sẽ được đồng bộ trực tiếp từ server
        let productsData = [
            {
                id: 1,
                code: "SP-001",
                name: "Gói CRM Cloud Enterprise (1 Năm)",
                type: "SUBSCRIPTION",
                unit: "Gói / Năm",
                listPrice: 24000000,
                floorPrice: 19200000, // Giá sàn giảm tối đa 20%
                costPrice: 9600000,
                status: "ACTIVE",
                description: "Hệ thống CRM toàn diện trên đám mây, không giới hạn người dùng, tính năng tự động hóa quy trình bán hàng."
            },
            {
                id: 2,
                code: "SP-002",
                name: "Gói CRM Cloud Starter (1 Năm)",
                type: "SUBSCRIPTION",
                unit: "Gói / Năm",
                listPrice: 9600000,
                floorPrice: 8000000,
                costPrice: 4000000,
                status: "ACTIVE",
                description: "Gói cơ bản cho doanh nghiệp nhỏ (tối đa 5 tài khoản), quản lý đầu mối và cơ hội kinh doanh."
            },
            {
                id: 3,
                code: "SP-003",
                name: "Bản quyền Phần mềm CRM On-Premise",
                type: "LICENSE",
                unit: "License vĩnh viễn",
                listPrice: 85000000,
                floorPrice: 70000000,
                costPrice: 35000000,
                status: "ACTIVE",
                description: "Cài đặt trực tiếp trên hạ tầng máy chủ của khách hàng, tích hợp bảo mật nội bộ."
            },
            {
                id: 4,
                code: "DV-004",
                name: "Dịch vụ Tư vấn & Triển khai Onsite",
                type: "CONSULTING",
                unit: "Gói / Dự án",
                listPrice: 30000000,
                floorPrice: 25000000,
                costPrice: 18000000,
                status: "ACTIVE",
                description: "Chuyên gia CRM khảo sát thực địa, chuẩn hóa dữ liệu khách hàng và đào tạo vận hành nội bộ."
            },
            {
                id: 5,
                code: "DV-005",
                name: "Gói Bảo trì & Hỗ trợ kỹ thuật 24/7",
                type: "SUBSCRIPTION",
                unit: "Gói / Năm",
                listPrice: 15000000,
                floorPrice: 12000000,
                costPrice: 6000000,
                status: "ACTIVE",
                description: "Cam kết SLA phản hồi dưới 30 phút, bảo trì định kỳ và backup dữ liệu hàng ngày."
            },
            {
                id: 6,
                code: "TB-006",
                name: "Máy chủ Server Dell PowerEdge R750",
                type: "HARDWARE",
                unit: "Chiếc",
                listPrice: 65000000,
                floorPrice: 58000000,
                costPrice: 48000000,
                status: "ACTIVE",
                description: "Thiết bị phần cứng máy chủ phục vụ triển khai CRM On-Premise."
            },
            {
                id: 7,
                code: "DV-007",
                name: "Gói Tích hợp Tổng đài VoIP Call Center",
                type: "ONE_TIME",
                unit: "Lần tích hợp",
                listPrice: 8000000,
                floorPrice: 6500000,
                costPrice: 3500000,
                status: "ACTIVE",
                description: "Kết nối hệ thống CRM với tổng đài nghe gọi tự động và ghi âm cuộc gọi."
            },
            {
                id: 8,
                code: "SP-008",
                name: "Gói CRM Marketing Tự động (Cũ)",
                type: "SUBSCRIPTION",
                unit: "Gói / Tháng",
                listPrice: 3000000,
                floorPrice: 2500000,
                costPrice: 1500000,
                status: "INACTIVE",
                description: "Phiên bản cũ đã ngừng kinh doanh, chuyển tiếp sang giải pháp Marketing Hub 2026."
            }
        ];

        // Dữ liệu bảng giá mẫu (Price Lists)
        let priceListsData = [
            {
                id: 1,
                name: "Bảng giá tiêu chuẩn (Standard List Price)",
                code: "PL-STD-2026",
                target: "Toàn bộ khách hàng phổ thông",
                validFrom: "01/01/2026",
                validTo: "31/12/2026",
                discountPolicy: "Áp dụng giá niêm yết chuẩn, chiết khấu tối đa theo giá sàn",
                status: "ACTIVE",
                totalProducts: 8
            },
            {
                id: 2,
                name: "Bảng giá Khách hàng Doanh nghiệp VIP",
                code: "PL-VIP-2026",
                target: "Khách hàng hợp đồng > 100 triệu",
                validFrom: "01/01/2026",
                validTo: "31/12/2026",
                discountPolicy: "Chiết khấu cố định 10% - 15% trên giá niêm yết",
                status: "ACTIVE",
                totalProducts: 7
            },
            {
                id: 3,
                name: "Bảng giá Đối tác & Đại lý phân phối",
                code: "PL-PARTNER-2026",
                target: "Đại lý cấp 1 và đối tác công nghệ",
                validFrom: "15/02/2026",
                validTo: "31/12/2026",
                discountPolicy: "Chiết khấu sát giá sàn (tối đa 25%), yêu cầu cam kết doanh số",
                status: "ACTIVE",
                totalProducts: 6
            }
        ];

        let currentRole = "DIRECTOR"; // Mặc định hiển thị đầy đủ (có thể đổi sang SALES để test ẩn giá vốn)
        let deletingProductId = null;

        // Định dạng tiền tệ VNĐ
        function formatVND(amount) {
            if (amount === null || amount === undefined || isNaN(amount)) return "0 ₫";
            return new Intl.NumberFormat('vi-VN').format(amount) + " ₫";
        }

        // Nhãn phân loại sản phẩm
        function getTypeBadge(type) {
            switch(type) {
                case "SUBSCRIPTION":
                    return '<span class="prod-badge prod-badge-type-subscription">🔄 Dịch vụ định kỳ</span>';
                case "LICENSE":
                    return '<span class="prod-badge prod-badge-type-license">🔑 Bản quyền PM</span>';
                case "HARDWARE":
                    return '<span class="prod-badge prod-badge-type-hardware">🖥️ Thiết bị</span>';
                case "CONSULTING":
                    return '<span class="prod-badge prod-badge-type-consulting">💼 Tư vấn triển khai</span>';
                default:
                    return '<span class="prod-badge" style="background: #f1f5f9; color: #475569;">📦 Dùng 1 lần</span>';
            }
        }

        // Render bảng sản phẩm
        function renderProductTable() {
            const tableBody = document.getElementById("prodTableBody");
            const emptyState = document.getElementById("prodEmptyState");
            const searchKeyword = (document.getElementById("prodSearchInput").value || "").trim().toLowerCase();
            const typeFilter = document.getElementById("prodTypeFilter").value;
            const statusFilter = document.getElementById("prodStatusFilter").value;

            // Lọc dữ liệu
            let filtered = productsData.filter(item => {
                const matchKeyword = !searchKeyword || 
                    item.code.toLowerCase().includes(searchKeyword) || 
                    item.name.toLowerCase().includes(searchKeyword);
                const matchType = !typeFilter || item.type === typeFilter;
                const matchStatus = !statusFilter || item.status === statusFilter;
                return matchKeyword && matchType && matchStatus;
            });

            // Cập nhật counters
            updateStats();

            if (filtered.length === 0) {
                tableBody.innerHTML = "";
                emptyState.style.display = "flex";
                return;
            }

            emptyState.style.display = "none";
            let html = "";

            filtered.forEach(p => {
                const statusBadge = p.status === "ACTIVE" 
                    ? '<span class="prod-badge prod-badge-active">● Đang kinh doanh</span>'
                    : '<span class="prod-badge prod-badge-inactive">○ Ngừng KD</span>';

                // Kiểm soát hiển thị Giá vốn theo quyền
                let costPriceDisplay = "";
                if (currentRole === "DIRECTOR") {
                    costPriceDisplay = '<span class="prod-cost-val">' + formatVND(p.costPrice) + '</span>';
                } else {
                    costPriceDisplay = '<span class="prod-cost-masked" title="Chỉ Giám đốc kinh doanh mới có quyền xem giá vốn">••••••</span>';
                }

                html += `
                    <tr>
                        <td style="font-weight: 700; color: var(--prod-primary);">${p.code}</td>
                        <td>
                            <div style="font-weight: 600; color: var(--prod-text-main);">${p.name}</div>
                            <div style="font-size: 0.78rem; color: var(--prod-text-muted);">${p.description ? p.description.substring(0, 60) + '...' : ''}</div>
                        </td>
                        <td>${getTypeBadge(p.type)}</td>
                        <td style="color: var(--prod-text-sub);">${p.unit}</td>
                        <td style="text-align: right; font-weight: 600; color: var(--prod-text-main);">
                            ${formatVND(p.listPrice)}
                        </td>
                        <td style="text-align: right;">
                            <div style="font-weight: 700; color: #b45309;">${formatVND(p.floorPrice)}</div>
                            <span class="prod-floor-badge" title="Mọi chiết khấu dưới mức giá sàn này bắt buộc Giám đốc kinh doanh duyệt">⚠️ Ngưỡng duyệt</span>
                        </td>
                        <td style="text-align: right;">
                            ${costPriceDisplay}
                        </td>
                        <td style="text-align: center;">${statusBadge}</td>
                        <td style="text-align: center;">
                            <div style="display: inline-flex; gap: 4px;">
                                <button type="button" class="btn-prod-icon" title="Xem chi tiết" onclick="openDetailModal(${p.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                                <button type="button" class="btn-prod-icon btn-prod-icon--edit" title="Chỉnh sửa" onclick="openEditModal(${p.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                    </svg>
                                </button>
                                <button type="button" class="btn-prod-icon btn-prod-icon--danger" title="Ngừng kinh doanh / Xóa" onclick="openDeleteModal(${p.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="3 6 5 6 21 6"></polyline>
                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                    </svg>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            });

            tableBody.innerHTML = html;
        }

        // Render Cards Bảng giá (Tab 2)
        function renderPriceLists() {
            const grid = document.getElementById("priceListsGrid");
            let html = "";
            priceListsData.forEach(pl => {
                html += `
                    <div class="prod-pricelist-card">
                        <div class="prod-pricelist-head">
                            <div>
                                <h4 class="prod-pricelist-title">${pl.name}</h4>
                                <span class="prod-badge prod-badge-type-subscription">${pl.code}</span>
                            </div>
                            <span class="prod-badge prod-badge-active">Đang áp dụng</span>
                        </div>
                        <div class="prod-pricelist-meta">
                            <div class="prod-pricelist-meta-item">
                                <strong>Đối tượng:</strong> <span>${pl.target}</span>
                            </div>
                            <div class="prod-pricelist-meta-item">
                                <strong>Hiệu lực:</strong> <span>${pl.validFrom} - ${pl.validTo}</span>
                            </div>
                            <div class="prod-pricelist-meta-item">
                                <strong>Quy định:</strong> <span>${pl.discountPolicy}</span>
                            </div>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: auto; padding-top: 10px; border-top: 1px solid var(--prod-border-subtle);">
                            <span style="font-size: 0.8rem; color: var(--prod-text-muted);">Sản phẩm: <strong>${pl.totalProducts}</strong></span>
                            <button type="button" class="btn-prod btn-prod-secondary" style="padding: 5px 10px; font-size: 0.8rem;" onclick="alert('Đang xem chính sách của ' + '${pl.name}' + ' (BE sẽ hỗ trợ API chi tiết bảng giá)');">
                                Xem chi tiết
                            </button>
                        </div>
                    </div>
                `;
            });
            grid.innerHTML = html;
        }

        // Cập nhật thống kê sơ bộ
        function updateStats() {
            const total = productsData.length;
            const active = productsData.filter(p => p.status === "ACTIVE").length;
            const subs = productsData.filter(p => p.type === "SUBSCRIPTION").length;

            document.getElementById("statTotalProducts").textContent = total;
            document.getElementById("statActiveProducts").textContent = active;
            document.getElementById("statSubscriptionCount").textContent = subs;
            document.getElementById("statPriceListsCount").textContent = priceListsData.length;
            document.getElementById("tabBadgeProdCount").textContent = total;
            document.getElementById("tabBadgePriceListCount").textContent = priceListsData.length;
        }

        // Chuyển đổi Tab
        function switchTab(tab) {
            const btnProd = document.getElementById("tabBtnProducts");
            const btnPrice = document.getElementById("tabBtnPriceLists");
            const paneProd = document.getElementById("tabPaneProducts");
            const panePrice = document.getElementById("tabPanePriceLists");

            if (tab === "products") {
                btnProd.classList.add("active");
                btnProd.setAttribute("aria-selected", "true");
                btnPrice.classList.remove("active");
                btnPrice.setAttribute("aria-selected", "false");
                paneProd.style.display = "block";
                panePrice.style.display = "none";
            } else {
                btnPrice.classList.add("active");
                btnPrice.setAttribute("aria-selected", "true");
                btnProd.classList.remove("active");
                btnProd.setAttribute("aria-selected", "false");
                panePrice.style.display = "block";
                paneProd.style.display = "none";
            }
        }

        // Chuyển đổi quyền xem (Giả lập để test phân quyền hiển thị Giá vốn)
        function toggleRoleView(role) {
            currentRole = role;
            const groupCost = document.getElementById("groupCreateCostPrice");
            const groupEditCost = document.getElementById("groupEditCostPrice");
            if (role === "SALES") {
                if (groupCost) groupCost.style.display = "none";
                if (groupEditCost) groupEditCost.style.display = "none";
            } else {
                if (groupCost) groupCost.style.display = "flex";
                if (groupEditCost) groupEditCost.style.display = "flex";
            }
            renderProductTable();
        }

        // Đặt lại bộ lọc
        function resetFilters() {
            document.getElementById("prodSearchInput").value = "";
            document.getElementById("prodTypeFilter").value = "";
            document.getElementById("prodStatusFilter").value = "";
            renderProductTable();
        }

        // Modal Controls
        function openCreateModal() {
            document.getElementById("formCreateProduct").reset();
            document.getElementById("modalCreateProduct").classList.add("active");
        }

        function closeCreateModal() {
            document.getElementById("modalCreateProduct").classList.remove("active");
        }

        function openEditModal(id) {
            const prod = productsData.find(p => p.id === id);
            if (!prod) return;

            document.getElementById("editProdId").value = prod.id;
            document.getElementById("editProdCode").value = prod.code;
            document.getElementById("editProdName").value = prod.name;
            document.getElementById("editProdType").value = prod.type;
            document.getElementById("editProdUnit").value = prod.unit;
            document.getElementById("editProdStatus").value = prod.status;
            document.getElementById("editListPrice").value = prod.listPrice;
            document.getElementById("editFloorPrice").value = prod.floorPrice;
            document.getElementById("editCostPrice").value = prod.costPrice;
            document.getElementById("editProdDesc").value = prod.description || "";

            document.getElementById("modalEditProduct").classList.add("active");
        }

        function closeEditModal() {
            document.getElementById("modalEditProduct").classList.remove("active");
        }

        function openDetailModal(id) {
            const prod = productsData.find(p => p.id === id);
            if (!prod) return;

            document.getElementById("detailProdCode").textContent = prod.code;
            document.getElementById("detailProdName").textContent = prod.name;
            document.getElementById("detailProdType").innerHTML = getTypeBadge(prod.type);
            document.getElementById("detailProdUnit").textContent = prod.unit;
            document.getElementById("detailProdStatus").innerHTML = prod.status === "ACTIVE" 
                ? '<span class="prod-badge prod-badge-active">Đang kinh doanh</span>'
                : '<span class="prod-badge prod-badge-inactive">Ngừng kinh doanh</span>';
            document.getElementById("detailListPrice").textContent = formatVND(prod.listPrice);
            document.getElementById("detailFloorPrice").textContent = formatVND(prod.floorPrice);

            if (currentRole === "DIRECTOR") {
                document.getElementById("detailCostPrice").textContent = formatVND(prod.costPrice);
            } else {
                document.getElementById("detailCostPrice").innerHTML = '<span class="prod-cost-masked">•••••• (Chỉ Giám đốc)</span>';
            }

            document.getElementById("detailProdDesc").textContent = prod.description || "Không có mô tả chi tiết.";

            document.getElementById("btnDetailToEdit").onclick = function() {
                closeDetailModal();
                openEditModal(prod.id);
            };

            document.getElementById("modalDetailProduct").classList.add("active");
        }

        function closeDetailModal() {
            document.getElementById("modalDetailProduct").classList.remove("active");
        }

        function openDeleteModal(id) {
            const prod = productsData.find(p => p.id === id);
            if (!prod) return;

            deletingProductId = id;
            document.getElementById("deleteProdNameDisplay").textContent = prod.name;
            document.getElementById("deleteProdCodeDisplay").textContent = prod.code;
            document.getElementById("modalDeleteProduct").classList.add("active");
        }

        function closeDeleteModal() {
            deletingProductId = null;
            document.getElementById("modalDeleteProduct").classList.remove("active");
        }

        // Thông báo UI
        function showSuccessToast(title, message) {
            const alertBox = document.getElementById("clientSuccessAlert");
            document.getElementById("clientSuccessTitle").textContent = title;
            document.getElementById("clientSuccessMessage").textContent = message;
            alertBox.style.display = "flex";
            setTimeout(() => { alertBox.style.display = "none"; }, 5000);
        }

        function showErrorToast(title, message) {
            const alertBox = document.getElementById("clientErrorAlert");
            document.getElementById("clientErrorTitle").textContent = title;
            document.getElementById("clientErrorMessage").textContent = message;
            alertBox.style.display = "flex";
        }

        // Xử lý Thêm mới (Form Submission)
        function handleCreateProduct(e) {
            e.preventDefault();
            const listPrice = parseFloat(document.getElementById("createListPrice").value);
            const floorPrice = parseFloat(document.getElementById("createFloorPrice").value);

            // Client Validation: Giá sàn không được lớn hơn Giá niêm yết
            if (floorPrice > listPrice) {
                showErrorToast("Lỗi kiểm tra giá sàn", "Giá sàn (" + formatVND(floorPrice) + ") không thể lớn hơn Giá niêm yết (" + formatVND(listPrice) + "). Vui lòng kiểm tra lại.");
                return;
            }

            const newProduct = {
                id: Date.now(),
                code: document.getElementById("createProdCode").value.trim().toUpperCase(),
                name: document.getElementById("createProdName").value.trim(),
                type: document.getElementById("createProdType").value,
                unit: document.getElementById("createProdUnit").value.trim(),
                listPrice: listPrice,
                floorPrice: floorPrice,
                costPrice: parseFloat(document.getElementById("createCostPrice").value) || 0,
                status: document.getElementById("createProdStatus").value,
                description: document.getElementById("createProdDesc").value.trim()
            };

            // Thêm vào danh sách giao diện
            productsData.unshift(newProduct);
            closeCreateModal();
            renderProductTable();
            showSuccessToast("Thêm sản phẩm thành công", "Đã lưu sản phẩm mới " + newProduct.code + " vào danh mục. (BE Contract: POST /products)");
        }

        // Xử lý Cập nhật (Form Submission)
        function handleUpdateProduct(e) {
            e.preventDefault();
            const id = parseInt(document.getElementById("editProdId").value);
            const listPrice = parseFloat(document.getElementById("editListPrice").value);
            const floorPrice = parseFloat(document.getElementById("editFloorPrice").value);

            if (floorPrice > listPrice) {
                showErrorToast("Lỗi kiểm tra giá sàn", "Giá sàn không thể lớn hơn Giá niêm yết. Vui lòng kiểm tra lại.");
                return;
            }

            const index = productsData.findIndex(p => p.id === id);
            if (index !== -1) {
                productsData[index].name = document.getElementById("editProdName").value.trim();
                productsData[index].type = document.getElementById("editProdType").value;
                productsData[index].unit = document.getElementById("editProdUnit").value.trim();
                productsData[index].listPrice = listPrice;
                productsData[index].floorPrice = floorPrice;
                productsData[index].costPrice = parseFloat(document.getElementById("editCostPrice").value) || productsData[index].costPrice;
                productsData[index].status = document.getElementById("editProdStatus").value;
                productsData[index].description = document.getElementById("editProdDesc").value.trim();

                closeEditModal();
                renderProductTable();
                showSuccessToast("Cập nhật thành công", "Đã cập nhật thông tin sản phẩm " + productsData[index].code + ".");
            }
        }

        // Xử lý Xác nhận Xóa / Ngừng KD
        function confirmDeleteProduct() {
            if (!deletingProductId) return;
            const index = productsData.findIndex(p => p.id === deletingProductId);
            if (index !== -1) {
                const prod = productsData[index];
                // Mô phỏng kiểm tra nghiệp vụ: Chuyển sang Ngừng kinh doanh để an toàn
                prod.status = "INACTIVE";
                closeDeleteModal();
                renderProductTable();
                showSuccessToast("Đã chuyển trạng thái", "Sản phẩm " + prod.code + " đã được chuyển sang trạng thái 'Ngừng kinh doanh' để bảo toàn dữ liệu báo giá/hợp đồng liên quan.");
            }
        }

        // Tích hợp tìm kiếm theo thời gian thực (Real-time Filter)
        document.getElementById("prodSearchInput").addEventListener("input", renderProductTable);
        document.getElementById("prodTypeFilter").addEventListener("change", renderProductTable);
        document.getElementById("prodStatusFilter").addEventListener("change", renderProductTable);

        // Khởi tạo ban đầu
        window.addEventListener("DOMContentLoaded", () => {
            renderProductTable();
            renderPriceLists();
        });
    </script>
</body>
</html>