<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục sản phẩm & Bảng giá niêm yết - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Quản lý sản phẩm & Bảng giá (CRM-39) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/products/products.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Sản phẩm & Bảng giá -->
        <main class="product-page" id="productApp" role="main">
            <div class="product-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="product-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Kinh doanh</span>
                    <span class="separator">/</span>
                    <span class="active">Sản phẩm & Bảng giá niêm yết</span>
                </nav>

                <!-- Header màn hình & Role Simulator Context (AC 3) -->
                <header class="product-header">
                    <div class="product-header-info">
                        <h1>Danh mục sản phẩm, dịch vụ & Bảng giá niêm yết</h1>
                        <p>Quản lý danh mục hàng hóa chuẩn, giá sàn kiểm soát duyệt chiết khấu báo giá và bảo mật giá vốn theo phân quyền.</p>
                    </div>

                    <div class="product-header-actions">
                        <!-- Widget chuyển đổi vai trò người dùng mô phỏng (AC 3: Giám đốc kinh doanh vs Nhân viên) -->
                        <div class="role-context-box" title="Chuyển đổi vai trò người dùng để kiểm tra tính năng phân quyền Giá vốn">
                            <span class="role-context-label">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                    <circle cx="8.5" cy="7.5" r="4"></circle>
                                    <polyline points="17 11 19 13 23 9"></polyline>
                                </svg>
                                Vai trò:
                            </span>
                            <select id="roleContextSelect" class="role-context-select" aria-label="Chọn vai trò người dùng">
                                <option value="SALES_DIRECTOR" selected>Giám đốc kinh doanh (Toàn quyền giá vốn)</option>
                                <option value="SALES_REP">Nhân viên kinh doanh (Ẩn giá vốn)</option>
                            </select>
                        </div>

                        <span class="product-badge-jira">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                            S2-05 / CRM-39
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts / Banners) -->
                <div class="product-alerts" id="productAlertsArea" aria-live="polite">
                    <div class="product-alert product-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="product-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="product-alert-content">
                            <div class="product-alert-title" id="globalErrorTitle">Đã xảy ra lỗi</div>
                            <div id="globalErrorMessage"></div>
                        </div>
                        <button type="button" class="product-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="product-alert product-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="product-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="product-alert-content">
                            <div class="product-alert-title" id="globalSuccessTitle">Thành công</div>
                            <div id="globalSuccessMessage"></div>
                        </div>
                        <button type="button" class="product-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="product-alert product-alert-warning" id="roleNoticeAlert" style="display: none;" role="status">
                        <svg class="product-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="product-alert-content">
                            <div class="product-alert-title">Quyền xem Giá vốn (Cost Price)</div>
                            <div id="roleNoticeMessage">Bạn đang xem với vai trò Nhân viên kinh doanh. Dữ liệu Giá vốn đã được mã hóa bảo mật theo tiêu chí AC 3.</div>
                        </div>
                    </div>
                </div>

                <!-- Thống kê nhanh danh mục (Metric Stat Cards) -->
                <section class="product-stats-grid" aria-label="Thống kê tổng quan danh mục">
                    <div class="product-stat-card">
                        <div class="stat-icon-wrap stat-icon-blue">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                            </svg>
                        </div>
                        <div class="stat-content">
                            <span class="stat-value" id="statTotalCount">8</span>
                            <span class="stat-label">Tổng mặt hàng</span>
                        </div>
                    </div>

                    <div class="product-stat-card">
                        <div class="stat-icon-wrap stat-icon-green">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                        </div>
                        <div class="stat-content">
                            <span class="stat-value" id="statActiveCount">7</span>
                            <span class="stat-label">Đang kinh doanh</span>
                        </div>
                    </div>

                    <div class="product-stat-card">
                        <div class="stat-icon-wrap stat-icon-purple">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M21.5 2v6h-6"></path>
                                <path d="M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"></path>
                            </svg>
                        </div>
                        <div class="stat-content">
                            <span class="stat-value" id="statSubscriptionCount">3</span>
                            <span class="stat-label">Dịch vụ thuê bao (Subscription)</span>
                        </div>
                    </div>

                    <div class="product-stat-card">
                        <div class="stat-icon-wrap stat-icon-amber">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="6" x2="12" y2="12"></line>
                                <line x1="12" y1="12" x2="16" y2="14"></line>
                            </svg>
                        </div>
                        <div class="stat-content">
                            <span class="stat-value" id="statPriceBookCount">2</span>
                            <span class="stat-label">Bảng giá niêm yết áp dụng</span>
                        </div>
                    </div>
                </section>

                <!-- Thanh chuyển Tab (Danh mục sản phẩm vs Bảng giá) -->
                <nav class="product-tabs-nav" aria-label="Thanh chuyển đổi danh mục và bảng giá">
                    <button type="button" class="product-tab-btn active" id="tabBtnProducts" onclick="switchTab('products')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                            <polyline points="2 17 12 22 22 17"></polyline>
                            <polyline points="2 12 12 17 22 12"></polyline>
                        </svg>
                        <span>Danh mục sản phẩm & Dịch vụ</span>
                        <span class="product-tab-badge" id="badgeProductsTabCount">8</span>
                    </button>

                    <button type="button" class="product-tab-btn" id="tabBtnPriceBooks" onclick="switchTab('pricebooks')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                        </svg>
                        <span>Bảng giá niêm yết (Price Books)</span>
                        <span class="product-tab-badge" id="badgePriceBooksTabCount">2</span>
                    </button>
                </nav>

                <!-- =========================================================
                     TAB 1: DANH MỤC SẢN PHẨM & DỊCH VỤ
                     ========================================================= -->
                <div id="tabContentProducts">
                    <section class="product-card" style="position: relative;" aria-labelledby="productCardTitle">

                        <!-- Loading Overlay -->
                        <div class="product-loading-overlay" id="productLoadingOverlay" aria-hidden="true">
                            <div class="product-spinner"></div>
                        </div>

                        <!-- Toolbar & Bộ lọc -->
                        <div class="product-toolbar">
                            <div class="product-toolbar-left">
                                <!-- Ô tìm kiếm -->
                                <div class="product-search-wrap">
                                    <svg class="product-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                    </svg>
                                    <input type="search" id="productSearchInput" class="product-search-input"
                                           placeholder="Tìm theo mã hoặc tên sản phẩm..."
                                           aria-label="Tìm kiếm sản phẩm">
                                </div>

                                <!-- Bộ lọc loại sản phẩm (AC 1) -->
                                <select id="filterProductType" class="product-filter-select" aria-label="Lọc theo loại sản phẩm">
                                    <option value="">Tất cả loại sản phẩm</option>
                                    <option value="ONE_TIME">Sản phẩm một lần (One-Time)</option>
                                    <option value="SUBSCRIPTION">Dịch vụ thuê bao (Subscription)</option>
                                </select>

                                <!-- Bộ lọc trạng thái kinh doanh (AC 4) -->
                                <select id="filterProductActive" class="product-filter-select" aria-label="Lọc theo trạng thái kinh doanh">
                                    <option value="">Tất cả trạng thái</option>
                                    <option value="true">Đang kinh doanh (Active)</option>
                                    <option value="false">Ngừng kinh doanh (Inactive)</option>
                                </select>

                                <!-- Nút Đặt lại bộ lọc -->
                                <button type="button" class="btn btn-secondary btn-sm" id="btnResetProductFilter" title="Xóa bộ lọc">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <polyline points="1 4 1 10 7 10"></polyline>
                                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                    </svg>
                                    Đặt lại
                                </button>
                            </div>

                            <div class="product-toolbar-right">
                                <!-- Nút Thêm sản phẩm mới -->
                                <button type="button" class="btn btn-primary" id="btnOpenCreateProductModal">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <line x1="12" y1="5" x2="12" y2="19"></line>
                                        <line x1="5" y1="12" x2="19" y2="12"></line>
                                    </svg>
                                    <span>Thêm sản phẩm</span>
                                </button>
                            </div>
                        </div>

                        <!-- Bảng dữ liệu sản phẩm (Table Responsive) -->
                        <div class="product-table-responsive">
                            <table class="product-table" id="productTable" aria-label="Bảng dữ liệu danh mục sản phẩm">
                                <thead>
                                    <tr>
                                        <th scope="col" style="width: 50px;">#</th>
                                        <th scope="col">Mã SP</th>
                                        <th scope="col">Tên sản phẩm & Dịch vụ</th>
                                        <th scope="col">Loại hình</th>
                                        <th scope="col">ĐVT</th>
                                        <th scope="col" style="text-align: right;">Giá niêm yết</th>
                                        <th scope="col" style="text-align: right;" title="Ngưỡng tối thiểu duyệt chiết khấu báo giá (AC 2)">
                                            Giá sàn
                                            <span style="font-size: 0.75rem; color: #b45309;">(*)</span>
                                        </th>
                                        <th scope="col" style="text-align: right;" id="thCostPrice" title="Chỉ Giám đốc kinh doanh có quyền xem và sửa (AC 3)">
                                            Giá vốn
                                            <span style="font-size: 0.75rem; color: #2563eb;">(GĐKD)</span>
                                        </th>
                                        <th scope="col" style="text-align: center;" title="Số báo giá đã sử dụng sản phẩm này (AC 4)">Báo giá</th>
                                        <th scope="col" style="text-align: center;">Trạng thái</th>
                                        <th scope="col" style="text-align: center; width: 120px;">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody id="productTableBody">
                                    <!-- Render động từ JS -->
                                </tbody>
                            </table>
                        </div>

                        <!-- Empty State -->
                        <div class="product-empty-state" id="productEmptyState" style="display: none;">
                            <div class="empty-icon" aria-hidden="true">📦</div>
                            <div class="empty-title">Không tìm thấy sản phẩm nào</div>
                            <p class="empty-subtitle">Không có sản phẩm nào phù hợp với điều kiện tìm kiếm và bộ lọc của bạn.</p>
                            <button type="button" class="btn btn-secondary" onclick="document.getElementById('btnResetProductFilter').click()">
                                Xóa bộ lọc tìm kiếm
                            </button>
                        </div>

                        <!-- Phân trang (Pagination) -->
                        <div class="product-pagination" id="productPagination">
                            <div class="pagination-info" id="productPaginationInfo">
                                Hiển thị 8 trên tổng số 8 sản phẩm
                            </div>
                            <div class="pagination-controls" id="productPaginationControls">
                                <!-- Nút trang render từ JS -->
                            </div>
                        </div>

                    </section>
                </div>

                <!-- =========================================================
                     TAB 2: BẢNG GIÁ NIÊM YẾT (PRICE BOOKS)
                     ========================================================= -->
                <div id="tabContentPriceBooks" style="display: none;">
                    <section class="product-card" aria-labelledby="priceBookCardTitle">
                        <div class="product-toolbar">
                            <div class="product-toolbar-left">
                                <h3 id="priceBookCardTitle" style="margin: 0; font-size: 1.0625rem; font-weight: 700; color: #0f172a;">
                                    Danh sách Bảng giá niêm yết chuẩn
                                </h3>
                                <p style="margin: 0; font-size: 0.8125rem; color: #64748b;">
                                    Bảng giá chuẩn làm căn cứ xuất phát điểm cho mọi báo giá kinh doanh.
                                </p>
                            </div>
                            <div class="product-toolbar-right">
                                <button type="button" class="btn btn-primary" id="btnOpenCreatePriceBookModal">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <line x1="12" y1="5" x2="12" y2="19"></line>
                                        <line x1="5" y1="12" x2="19" y2="12"></line>
                                    </svg>
                                    <span>Tạo bảng giá mới</span>
                                </button>
                            </div>
                        </div>

                        <div class="pricebook-grid" id="priceBookGrid">
                            <!-- Render thẻ bảng giá từ JS -->
                        </div>
                    </section>
                </div>

            </div>
        </main>
    </div>

    <!-- =================================================================
         MODAL 1: THÊM / CHỈNH SỬA SẢN PHẨM (AC 1, AC 2, AC 3)
         ================================================================= -->
    <div class="crm-modal-overlay" id="productModal" role="dialog" aria-modal="true" aria-labelledby="productModalTitle">
        <div class="crm-modal-card">
            <header class="crm-modal-header">
                <h3 class="crm-modal-title" id="productModalTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                    </svg>
                    <span id="productModalHeading">Thêm sản phẩm mới</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseProductModal" aria-label="Đóng">&times;</button>
            </header>

            <form id="productForm" novalidate>
                <input type="hidden" id="formProductId" name="id">

                <div class="crm-modal-body">
                    <!-- AC 2: Ghi chú nghiệp vụ giá sàn & duyệt chiết khấu -->
                    <div class="business-rule-notice">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; margin-top: 1px;" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div>
                            <strong>Quy định nghiệp vụ (AC 2):</strong> Giá sàn là ngưỡng tối thiểu để hệ thống kiểm soát chiết khấu. Bất kỳ báo giá nào có giá bán thấp hơn Giá sàn sẽ tự động yêu cầu Giám đốc kinh doanh phê duyệt.
                        </div>
                    </div>

                    <!-- Hàng 1: Mã sản phẩm & Tên sản phẩm -->
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formProductCode" class="form-label">
                                <span>Mã sản phẩm <span class="required-mark">*</span></span>
                                <span class="form-hint">Duy nhất (VD: CRM-SUB-PRO)</span>
                            </label>
                            <input type="text" id="formProductCode" name="code" class="form-control"
                                   placeholder="VD: CRM-ENT-01" required uppercase>
                            <div class="form-feedback" id="feedbackProductCode"></div>
                        </div>

                        <div class="form-group">
                            <label for="formProductName" class="form-label">
                                <span>Tên sản phẩm / Dịch vụ <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formProductName" name="name" class="form-control"
                                   placeholder="VD: Thuê bao CRM Cloud Chuyên nghiệp" required>
                            <div class="form-feedback" id="feedbackProductName"></div>
                        </div>
                    </div>

                    <!-- Hàng 2: Loại sản phẩm & Đơn vị tính -->
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formProductType" class="form-label">
                                <span>Loại sản phẩm <span class="required-mark">*</span></span>
                            </label>
                            <select id="formProductType" name="type" class="form-control" required>
                                <option value="ONE_TIME">Sản phẩm một lần (One-Time)</option>
                                <option value="SUBSCRIPTION">Dịch vụ thuê bao (Subscription theo kỳ)</option>
                            </select>
                            <div class="form-feedback" id="feedbackProductType"></div>
                        </div>

                        <div class="form-group">
                            <label for="formProductUnit" class="form-label">
                                <span>Đơn vị tính (Unit) <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formProductUnit" name="unit" class="form-control"
                                   placeholder="VD: Gói, Bản quyền, User/Năm, Tháng..." required>
                            <div class="form-feedback" id="feedbackProductUnit"></div>
                        </div>
                    </div>

                    <!-- Hàng 3: Giá niêm yết & Giá sàn (AC 1 & AC 2) -->
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formListPrice" class="form-label">
                                <span>Giá niêm yết (VNĐ) <span class="required-mark">*</span></span>
                                <span class="form-hint">Giá chuẩn ban đầu</span>
                            </label>
                            <input type="number" id="formListPrice" name="listPrice" class="form-control"
                                   placeholder="VD: 2400000" min="0" step="1000" required>
                            <div class="form-feedback" id="feedbackListPrice"></div>
                        </div>

                        <div class="form-group">
                            <label for="formFloorPrice" class="form-label">
                                <span>Giá sàn (VNĐ) <span class="required-mark">*</span></span>
                                <span class="form-hint">Ngưỡng duyệt chiết khấu</span>
                            </label>
                            <input type="number" id="formFloorPrice" name="floorPrice" class="form-control"
                                   placeholder="VD: 1900000" min="0" step="1000" required>
                            <div class="form-feedback" id="feedbackFloorPrice"></div>
                        </div>
                    </div>

                    <!-- Hàng 4: Giá vốn (AC 3: Chỉ Giám đốc kinh doanh có quyền xem & sửa) -->
                    <div class="form-group" id="groupCostPrice">
                        <label for="formCostPrice" class="form-label">
                            <span>
                                Giá vốn (Cost Price - VNĐ)
                                <span class="required-mark">*</span>
                                <span style="font-size: 0.75rem; color: #2563eb; font-weight: 600;">(Chỉ GĐKD)</span>
                            </span>
                            <span class="form-hint">Cơ sở tính biên lợi nhuận</span>
                        </label>
                        <input type="number" id="formCostPrice" name="costPrice" class="form-control"
                               placeholder="VD: 800000" min="0" step="1000" required>
                        <div class="form-feedback" id="feedbackCostPrice"></div>
                    </div>

                    <!-- Hộp cảnh báo khi nhân viên kinh doanh không có quyền xem/sửa giá vốn (AC 3) -->
                    <div class="cost-price-director-only" id="noticeCostPriceHidden" style="display: none;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        <span>Trường <strong>Giá vốn (Cost Price)</strong> bị ẩn và chỉ được phép xem/sửa bởi người dùng có vai trò <strong>Giám đốc kinh doanh</strong>.</span>
                    </div>

                    <!-- Hàng 5: Trạng thái & Mô tả -->
                    <div class="form-group">
                        <label for="formProductActive" class="form-label">
                            <span>Trạng thái kinh doanh <span class="required-mark">*</span></span>
                        </label>
                        <select id="formProductActive" name="active" class="form-control" required>
                            <option value="true">Đang kinh doanh (Active)</option>
                            <option value="false">Ngừng kinh doanh (Inactive)</option>
                        </select>
                        <div class="form-feedback" id="feedbackProductActive"></div>
                    </div>

                    <div class="form-group">
                        <label for="formProductDescription" class="form-label">
                            <span>Mô tả sản phẩm</span>
                        </label>
                        <textarea id="formProductDescription" name="description" class="form-control" rows="3"
                                  placeholder="Mô tả phạm vi áp dụng, tính năng hoặc điều kiện dịch vụ..."></textarea>
                    </div>

                </div>

                <footer class="crm-modal-footer">
                    <button type="button" class="btn btn-secondary" id="btnCancelProductModal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveProduct">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        <span>Lưu sản phẩm</span>
                    </button>
                </footer>
            </form>
        </div>
    </div>

    <!-- =================================================================
         MODAL 2: TẠO BẢNG GIÁ NIÊM YẾT MỚI (PRICE BOOK MODAL)
         ================================================================= -->
    <div class="crm-modal-overlay" id="priceBookModal" role="dialog" aria-modal="true" aria-labelledby="priceBookModalTitle">
        <div class="crm-modal-card crm-modal-card-lg">
            <header class="crm-modal-header">
                <h3 class="crm-modal-title" id="priceBookModalTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                    </svg>
                    <span>Tạo Bảng giá niêm yết chuẩn</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnClosePriceBookModal" aria-label="Đóng">&times;</button>
            </header>

            <form id="priceBookForm" novalidate>
                <div class="crm-modal-body">
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formPriceBookName" class="form-label">
                                <span>Tên bảng giá <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formPriceBookName" name="name" class="form-control"
                                   placeholder="VD: Bảng giá chuẩn Năm 2026" required>
                            <div class="form-feedback" id="feedbackPriceBookName"></div>
                        </div>

                        <div class="form-group">
                            <label for="formPriceBookEffectiveFrom" class="form-label">
                                <span>Ngày có hiệu lực <span class="required-mark">*</span></span>
                            </label>
                            <input type="date" id="formPriceBookEffectiveFrom" name="effectiveFrom" class="form-control" required>
                            <div class="form-feedback" id="feedbackPriceBookEffective"></div>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="formPriceBookDescription" class="form-label">
                            <span>Ghi chú / Phạm vi áp dụng</span>
                        </label>
                        <input type="text" id="formPriceBookDescription" name="description" class="form-control"
                               placeholder="VD: Bảng giá niêm yết áp dụng cho mọi khách hàng khối Doanh nghiệp và Giáo dục">
                    </div>

                    <div class="form-group">
                        <label class="form-label">
                            <span>Danh sách mặt hàng & Đơn giá áp dụng trong bảng giá</span>
                            <span class="form-hint">Mặc định lấy theo giá niêm yết chuẩn</span>
                        </label>
                        <div style="max-height: 220px; overflow-y: auto; border: 1px solid #e2e8f0; border-radius: 8px;">
                            <table class="product-table" style="font-size: 0.8125rem;">
                                <thead>
                                    <tr>
                                        <th style="width: 30px;"><input type="checkbox" id="checkAllPriceBookLines" checked></th>
                                        <th>Mã SP</th>
                                        <th>Tên sản phẩm</th>
                                        <th style="text-align: right;">Đơn giá áp dụng (VNĐ)</th>
                                    </tr>
                                </thead>
                                <tbody id="priceBookLinesTbody">
                                    <!-- Render từ JS -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <footer class="crm-modal-footer">
                    <button type="button" class="btn btn-secondary" id="btnCancelPriceBookModal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSavePriceBook">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        <span>Lưu bảng giá</span>
                    </button>
                </footer>
            </form>
        </div>
    </div>

    <!-- =================================================================
         MODAL 3: CẢNH BÁO RÀNG BUỘC BÁO GIÁ & CHUYỂN NGỪNG KINH DOANH (AC 4)
         ================================================================= -->
    <div class="crm-modal-overlay" id="deactivateConfirmModal" role="dialog" aria-modal="true" aria-labelledby="deactivateModalTitle">
        <div class="crm-modal-card" style="max-width: 520px;">
            <header class="crm-modal-header" style="background-color: #fffbeb;">
                <h3 class="crm-modal-title" id="deactivateModalTitle" style="color: #92400e;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                        <line x1="12" y1="9" x2="12" y2="13"></line>
                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                    </svg>
                    <span>Cảnh báo ràng buộc Báo giá (AC 4)</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseDeactivateModal" aria-label="Đóng">&times;</button>
            </header>

            <div class="crm-modal-body">
                <p style="margin: 0; font-size: 0.9375rem; color: #334155; line-height: 1.5;" id="deactivateModalContent">
                    Sản phẩm này đã xuất hiện trong các báo giá kinh doanh. Theo tiêu chí nghiệm thu AC 4, hệ thống <strong>không cho phép xóa bỏ hoàn toàn</strong> để tránh mất tính toàn vẹn dữ liệu hợp đồng/báo giá.
                </p>
                <div style="padding: 12px 14px; border-radius: 8px; background-color: #f8fafc; border: 1px solid #e2e8f0; font-size: 0.875rem; color: #475569;">
                    Bạn có muốn chuyển trạng thái sản phẩm sang <strong>[Ngừng kinh doanh - Inactive]</strong> không? Sản phẩm sẽ không còn xuất hiện trong các báo giá mới nhưng lịch sử báo giá cũ vẫn được bảo toàn trọn vẹn.
                </div>
            </div>

            <footer class="crm-modal-footer">
                <button type="button" class="btn btn-secondary" id="btnCancelDeactivate">Giữ nguyên</button>
                <button type="button" class="btn btn-danger" id="btnConfirmDeactivate">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="15" y1="9" x2="9" y2="15"></line>
                        <line x1="9" y1="9" x2="15" y2="15"></line>
                    </svg>
                    <span>Xác nhận Ngừng kinh doanh</span>
                </button>
            </footer>
        </div>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- =================================================================
         JAVASCRIPT DOM THUẦN & FETCH API TÍCH HỢP (CRM-39)
         ================================================================= -->
    <script>
    (function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}' || '';

        // Dữ liệu 8 sản phẩm ban đầu chuẩn CRM B2B (Đầy đủ ONE_TIME, SUBSCRIPTION, Giá sàn, Giá vốn & Báo giá)
        var INITIAL_PRODUCTS = [
            {
                id: 1,
                code: 'CRM-ENT-01',
                name: 'CRM Doanh nghiệp Enterprise (Bản quyền trọn đời)',
                type: 'ONE_TIME',
                unit: 'Bản quyền',
                listPrice: 120000000,
                floorPrice: 95000000,
                costPrice: 40000000,
                active: true,
                quoteCount: 4,
                description: 'Bản quyền cài đặt tại hạ tầng On-premise của doanh nghiệp, đầy đủ phân hệ và tích hợp'
            },
            {
                id: 2,
                code: 'CRM-SUB-PRO',
                name: 'Thuê bao CRM Cloud Chuyên nghiệp (User/Năm)',
                type: 'SUBSCRIPTION',
                unit: 'User/Năm',
                listPrice: 2400000,
                floorPrice: 1900000,
                costPrice: 800000,
                active: true,
                quoteCount: 12,
                description: 'Dịch vụ đám mây tính theo đầu người hàng năm, hỗ trợ quản lý phễu cơ hội và lead'
            },
            {
                id: 3,
                code: 'CRM-SUB-STD',
                name: 'Thuê bao CRM Cloud Tiêu chuẩn (User/Tháng)',
                type: 'SUBSCRIPTION',
                unit: 'User/Tháng',
                listPrice: 250000,
                floorPrice: 200000,
                costPrice: 90000,
                active: true,
                quoteCount: 8,
                description: 'Gói thuê bao linh hoạt theo tháng dành cho doanh nghiệp vừa và nhỏ'
            },
            {
                id: 4,
                code: 'SRV-IMP-01',
                name: 'Dịch vụ Tư vấn & Triển khai Khởi động CRM',
                type: 'ONE_TIME',
                unit: 'Gói',
                listPrice: 35000000,
                floorPrice: 28000000,
                costPrice: 15000000,
                active: true,
                quoteCount: 5,
                description: 'Thiết lập quy trình nghiệp vụ bán hàng, chuẩn hóa dữ liệu khách hàng ban đầu'
            },
            {
                id: 5,
                code: 'SRV-TRN-02',
                name: 'Đào tạo Chuyển giao & Hướng dẫn sử dụng',
                type: 'ONE_TIME',
                unit: 'Buổi',
                listPrice: 5000000,
                floorPrice: 4000000,
                costPrice: 2000000,
                active: true,
                quoteCount: 3,
                description: 'Đào tạo trực tiếp 4 giờ cho đội ngũ kinh doanh và ban quản lý'
            },
            {
                id: 6,
                code: 'SRV-CUST-03',
                name: 'Dịch vụ Tùy biến Báo cáo & Tích hợp ERP/Kế toán',
                type: 'ONE_TIME',
                unit: 'Giờ công (Man-hour)',
                listPrice: 600000,
                floorPrice: 500000,
                costPrice: 300000,
                active: true,
                quoteCount: 2,
                description: 'Lập trình kết nối API đồng bộ dữ liệu hóa đơn, công nợ'
            },
            {
                id: 7,
                code: 'SRV-MAINT-YR',
                name: 'Gói Bảo hành & Hỗ trợ kỹ thuật 24/7 (Năm)',
                type: 'SUBSCRIPTION',
                unit: 'Năm',
                listPrice: 18000000,
                floorPrice: 15000000,
                costPrice: 6000000,
                active: true,
                quoteCount: 6,
                description: 'Dịch vụ hỗ trợ ưu tiên qua Hotline và giải quyết sự cố trong 2 giờ'
            },
            {
                id: 8,
                code: 'CRM-LEGACY-V1',
                name: 'Bản quyền CRM V1 Cũ (Ngừng phát triển)',
                type: 'ONE_TIME',
                unit: 'Bản quyền',
                listPrice: 50000000,
                floorPrice: 45000000,
                costPrice: 20000000,
                active: false,
                quoteCount: 9,
                description: 'Phiên bản cũ đã ngừng kinh doanh, chỉ duy trì cho hợp đồng lịch sử'
            }
        ];

        // Dữ liệu Bảng giá niêm yết chuẩn ban đầu
        var INITIAL_PRICE_BOOKS = [
            {
                id: 1,
                name: 'Bảng giá niêm yết chuẩn Năm 2026',
                effectiveFrom: '2026-01-01',
                active: true,
                itemCount: 7,
                description: 'Bảng giá áp dụng chuẩn cho toàn khối kinh doanh trên toàn quốc',
                lines: [
                    { productCode: 'CRM-ENT-01', price: 120000000 },
                    { productCode: 'CRM-SUB-PRO', price: 2400000 },
                    { productCode: 'CRM-SUB-STD', price: 250000 },
                    { productCode: 'SRV-IMP-01', price: 35000000 },
                    { productCode: 'SRV-TRN-02', price: 5000000 },
                    { productCode: 'SRV-CUST-03', price: 600000 },
                    { productCode: 'SRV-MAINT-YR', price: 18000000 }
                ]
            },
            {
                id: 2,
                name: 'Bảng giá Đối tác Chiến lược & Khối Giáo dục',
                effectiveFrom: '2026-06-01',
                active: true,
                itemCount: 4,
                description: 'Chính sách giá ưu đãi dành riêng cho các trường đại học và cơ quan đào tạo',
                lines: [
                    { productCode: 'CRM-SUB-PRO', price: 2000000 },
                    { productCode: 'CRM-SUB-STD', price: 210000 },
                    { productCode: 'SRV-IMP-01', price: 30000000 },
                    { productCode: 'SRV-TRN-02', price: 4200000 }
                ]
            }
        ];

        // State quản lý của ứng dụng
        var state = {
            role: 'SALES_DIRECTOR', // 'SALES_DIRECTOR' (xem/sửa giá vốn) hoặc 'SALES_REP' (ẩn giá vốn)
            currentTab: 'products',
            products: JSON.parse(JSON.stringify(INITIAL_PRODUCTS)),
            priceBooks: JSON.parse(JSON.stringify(INITIAL_PRICE_BOOKS)),
            keyword: '',
            filterType: '',
            filterActive: '',
            page: 1,
            size: 10,
            pendingDeactivateProduct: null
        };

        // DOM Elements
        var roleSelect = document.getElementById('roleContextSelect');
        var roleNoticeAlert = document.getElementById('roleNoticeAlert');
        var roleNoticeMessage = document.getElementById('roleNoticeMessage');
        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');
        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');

        var productTableBody = document.getElementById('productTableBody');
        var productEmptyState = document.getElementById('productEmptyState');
        var productPaginationInfo = document.getElementById('productPaginationInfo');
        var productPaginationControls = document.getElementById('productPaginationControls');
        var productLoadingOverlay = document.getElementById('productLoadingOverlay');

        var statTotalCount = document.getElementById('statTotalCount');
        var statActiveCount = document.getElementById('statActiveCount');
        var statSubscriptionCount = document.getElementById('statSubscriptionCount');
        var statPriceBookCount = document.getElementById('statPriceBookCount');
        var badgeProductsTabCount = document.getElementById('badgeProductsTabCount');
        var badgePriceBooksTabCount = document.getElementById('badgePriceBooksTabCount');

        var productSearchInput = document.getElementById('productSearchInput');
        var filterProductType = document.getElementById('filterProductType');
        var filterProductActive = document.getElementById('filterProductActive');
        var btnResetProductFilter = document.getElementById('btnResetProductFilter');

        // Modal Elements
        var productModal = document.getElementById('productModal');
        var productForm = document.getElementById('productForm');
        var productModalHeading = document.getElementById('productModalHeading');
        var btnOpenCreateProductModal = document.getElementById('btnOpenCreateProductModal');
        var btnCloseProductModal = document.getElementById('btnCloseProductModal');
        var btnCancelProductModal = document.getElementById('btnCancelProductModal');

        var formProductId = document.getElementById('formProductId');
        var formProductCode = document.getElementById('formProductCode');
        var formProductName = document.getElementById('formProductName');
        var formProductType = document.getElementById('formProductType');
        var formProductUnit = document.getElementById('formProductUnit');
        var formListPrice = document.getElementById('formListPrice');
        var formFloorPrice = document.getElementById('formFloorPrice');
        var formCostPrice = document.getElementById('formCostPrice');
        var formProductActive = document.getElementById('formProductActive');
        var formProductDescription = document.getElementById('formProductDescription');
        var groupCostPrice = document.getElementById('groupCostPrice');
        var noticeCostPriceHidden = document.getElementById('noticeCostPriceHidden');

        var deactivateConfirmModal = document.getElementById('deactivateConfirmModal');
        var deactivateModalContent = document.getElementById('deactivateModalContent');
        var btnCloseDeactivateModal = document.getElementById('btnCloseDeactivateModal');
        var btnCancelDeactivate = document.getElementById('btnCancelDeactivate');
        var btnConfirmDeactivate = document.getElementById('btnConfirmDeactivate');

        var priceBookModal = document.getElementById('priceBookModal');
        var priceBookForm = document.getElementById('priceBookForm');
        var btnOpenCreatePriceBookModal = document.getElementById('btnOpenCreatePriceBookModal');
        var btnClosePriceBookModal = document.getElementById('btnClosePriceBookModal');
        var btnCancelPriceBookModal = document.getElementById('btnCancelPriceBookModal');
        var priceBookGrid = document.getElementById('priceBookGrid');
        var priceBookLinesTbody = document.getElementById('priceBookLinesTbody');

        // Format tiền tệ VNĐ
        function formatVND(amount) {
            if (amount == null || isNaN(amount)) return '0 đ';
            return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
        }

        function escapeHtml(str) {
            if (str == null) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        // Thông báo
        function showSuccessAlert(msg) {
            globalSuccessMessage.textContent = msg;
            globalSuccessAlert.style.display = 'flex';
            globalErrorAlert.style.display = 'none';
            setTimeout(function () {
                globalSuccessAlert.style.display = 'none';
            }, 4500);
        }

        function showErrorAlert(msg) {
            globalErrorMessage.textContent = msg;
            globalErrorAlert.style.display = 'flex';
            globalSuccessAlert.style.display = 'none';
        }

        function hideAlerts() {
            globalSuccessAlert.style.display = 'none';
            globalErrorAlert.style.display = 'none';
        }

        // Cập nhật thống kê nhanh
        function updateStats() {
            var total = state.products.length;
            var active = state.products.filter(function (p) { return p.active; }).length;
            var subs = state.products.filter(function (p) { return p.type === 'SUBSCRIPTION'; }).length;
            var pbCount = state.priceBooks.length;

            statTotalCount.textContent = total;
            statActiveCount.textContent = active;
            statSubscriptionCount.textContent = subs;
            statPriceBookCount.textContent = pbCount;
            badgeProductsTabCount.textContent = total;
            badgePriceBooksTabCount.textContent = pbCount;
        }

        // Lọc sản phẩm
        function getFilteredProducts() {
            var kw = (state.keyword || '').trim().toLowerCase();
            return state.products.filter(function (p) {
                var matchKw = true;
                if (kw) {
                    var codeMatch = (p.code || '').toLowerCase().indexOf(kw) !== -1;
                    var nameMatch = (p.name || '').toLowerCase().indexOf(kw) !== -1;
                    matchKw = codeMatch || nameMatch;
                }
                var matchType = true;
                if (state.filterType) {
                    matchType = p.type === state.filterType;
                }
                var matchActive = true;
                if (state.filterActive !== '') {
                    var actBool = state.filterActive === 'true';
                    matchActive = p.active === actBool;
                }
                return matchKw && matchType && matchActive;
            });
        }

        // Render bảng sản phẩm
        function renderProductTable() {
            var filtered = getFilteredProducts();
            var isDirector = state.role === 'SALES_DIRECTOR';

            // Cập nhật hiển thị cột giá vốn trên Header
            var thCostPrice = document.getElementById('thCostPrice');
            if (thCostPrice) {
                if (isDirector) {
                    thCostPrice.innerHTML = 'Giá vốn <span style="font-size:0.75rem; color:#2563eb;">(GĐKD)</span>';
                } else {
                    thCostPrice.innerHTML = 'Giá vốn <span style="font-size:0.75rem; color:#94a3b8;">(Ẩn)</span>';
                }
            }

            if (filtered.length === 0) {
                productTableBody.innerHTML = '';
                productEmptyState.style.display = 'block';
                productPaginationInfo.textContent = 'Hiển thị 0 trên 0 sản phẩm';
                productPaginationControls.innerHTML = '';
                return;
            }

            productEmptyState.style.display = 'none';
            productTableBody.innerHTML = '';

            filtered.forEach(function (p, index) {
                var tr = document.createElement('tr');
                if (!p.active) {
                    tr.className = 'row-inactive';
                }

                // Cột Loại hình
                var typeBadgeHtml = p.type === 'SUBSCRIPTION'
                    ? '<span class="badge-type badge-type-subscription"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21.5 2v6h-6"></path><path d="M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"></path></svg> Thuê bao</span>'
                    : '<span class="badge-type badge-type-onetime"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg> Một lần</span>';

                // Cột Trạng thái
                var statusBadgeHtml = p.active
                    ? '<span class="badge-status badge-status-active"><span style="width:6px;height:6px;border-radius:50%;background:#10b981;display:inline-block;"></span> Đang kinh doanh</span>'
                    : '<span class="badge-status badge-status-inactive"><span style="width:6px;height:6px;border-radius:50%;background:#94a3b8;display:inline-block;"></span> Ngừng kinh doanh</span>';

                // Cột Giá vốn (AC 3: Phân quyền xem)
                var costPriceHtml = '';
                if (isDirector) {
                    costPriceHtml = '<span class="price-cost">' + formatVND(p.costPrice) + '</span>';
                } else {
                    costPriceHtml = '<span class="cost-masked" title="Chỉ Giám đốc kinh doanh có quyền xem giá vốn (AC 3)">••••••••</span>';
                }

                // Cột Báo giá (AC 4)
                var quoteCount = p.quoteCount || 0;
                var quoteCountHtml = quoteCount > 0
                    ? '<span class="quote-count-badge" title="Đã có ' + quoteCount + ' báo giá sử dụng sản phẩm này">' + quoteCount + ' báo giá</span>'
                    : '<span class="quote-count-badge quote-count-zero" title="Chưa có báo giá nào">0</span>';

                tr.innerHTML =
                    '<td><strong>' + (index + 1) + '</strong></td>' +
                    '<td><span class="col-code">' + escapeHtml(p.code) + '</span></td>' +
                    '<td>' +
                        '<div class="product-name-cell">' +
                            '<span class="product-title">' + escapeHtml(p.name) + '</span>' +
                            (p.description ? '<span class="product-desc-sub" title="' + escapeHtml(p.description) + '">' + escapeHtml(p.description) + '</span>' : '') +
                        '</div>' +
                    '</td>' +
                    '<td>' + typeBadgeHtml + '</td>' +
                    '<td><span style="color:#475569; font-weight:500;">' + escapeHtml(p.unit) + '</span></td>' +
                    '<td style="text-align: right;"><span class="price-value">' + formatVND(p.listPrice) + '</span></td>' +
                    '<td style="text-align: right;">' +
                        '<span class="price-value price-floor" title="Giá sàn tối thiểu để duyệt chiết khấu báo giá (AC 2)">' +
                            formatVND(p.floorPrice) +
                        '</span>' +
                        '<span class="price-floor-hint">Min chiết khấu</span>' +
                    '</td>' +
                    '<td style="text-align: right;">' + costPriceHtml + '</td>' +
                    '<td style="text-align: center;">' + quoteCountHtml + '</td>' +
                    '<td style="text-align: center;">' + statusBadgeHtml + '</td>' +
                    '<td>' +
                        '<div class="action-cell" style="justify-content: center;">' +
                            '<button type="button" class="btn-icon" data-action="edit" data-id="' + p.id + '" title="Chỉnh sửa sản phẩm">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>' +
                                    '<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>' +
                                '</svg>' +
                            '</button>' +
                            '<button type="button" class="btn-icon" data-action="toggle-active" data-id="' + p.id + '" title="' + (p.active ? 'Chuyển sang Ngừng kinh doanh' : 'Kích hoạt lại kinh doanh') + '">' +
                                (p.active
                                    ? '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>'
                                    : '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>'
                                ) +
                            '</button>' +
                            '<button type="button" class="btn-icon btn-icon-danger" data-action="delete" data-id="' + p.id + '" title="Xóa hoặc Ngừng kinh doanh (AC 4)">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<polyline points="3 6 5 6 21 6"></polyline>' +
                                    '<path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>' +
                                '</svg>' +
                            '</button>' +
                        '</div>' +
                    '</td>';

                productTableBody.appendChild(tr);
            });

            productPaginationInfo.textContent = 'Hiển thị ' + filtered.length + ' trên tổng số ' + state.products.length + ' sản phẩm';
            productPaginationControls.innerHTML = '<button type="button" class="page-btn active" disabled>1</button>';
        }

        // Render Bảng giá niêm yết (Price Books)
        function renderPriceBooks() {
            priceBookGrid.innerHTML = '';
            if (state.priceBooks.length === 0) {
                priceBookGrid.innerHTML = '<div style="grid-column: 1/-1; padding: 40px; text-align: center; color: #64748b;">Chưa có bảng giá niêm yết nào được tạo.</div>';
                return;
            }

            state.priceBooks.forEach(function (pb) {
                var card = document.createElement('div');
                card.className = 'pricebook-card';
                card.innerHTML =
                    '<div class="pricebook-card-header">' +
                        '<div>' +
                            '<h4 class="pricebook-title">' + escapeHtml(pb.name) + '</h4>' +
                            '<span class="pricebook-date">' +
                                '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>' +
                                'Hiệu lực từ: ' + escapeHtml(pb.effectiveFrom) +
                            '</span>' +
                        '</div>' +
                        '<span class="badge-status badge-status-active">Chuẩn áp dụng</span>' +
                    '</div>' +
                    '<div class="pricebook-card-body">' +
                        '<p class="pricebook-desc">' + escapeHtml(pb.description || 'Bảng giá chuẩn áp dụng cho báo giá kinh doanh') + '</p>' +
                        '<ul class="pricebook-meta-list">' +
                            '<li class="pricebook-meta-item">' +
                                '<span>Số lượng mặt hàng:</span>' +
                                '<strong>' + (pb.lines ? pb.lines.length : pb.itemCount || 0) + ' sản phẩm</strong>' +
                            '</li>' +
                            '<li class="pricebook-meta-item">' +
                                '<span>Trạng thái:</span>' +
                                '<span style="color:#10b981;font-weight:600;">Đang có hiệu lực</span>' +
                            '</li>' +
                        '</ul>' +
                    '</div>' +
                    '<div class="pricebook-card-footer">' +
                        '<button type="button" class="btn btn-secondary btn-sm" onclick="alert(\'Bảng giá chuẩn đã được gán áp dụng tự động cho module Báo giá!\')">Chi tiết bảng giá</button>' +
                    '</div>';
                priceBookGrid.appendChild(card);
            });
        }

        // Tải danh sách sản phẩm từ backend (GET /api/products)
        async function fetchProducts() {
            productLoadingOverlay.style.display = 'flex';
            hideAlerts();

            var queryParams = new URLSearchParams({
                keyword: state.keyword,
                active: state.filterActive,
                page: state.page,
                size: state.size
            });

            var endpoint = contextPath + '/api/products?' + queryParams.toString();

            try {
                var response = await fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });

                productLoadingOverlay.style.display = 'none';

                if (response.ok) {
                    var json = await response.json();
                    var list = (json && json.data && Array.isArray(json.data.items))
                        ? json.data.items
                        : (Array.isArray(json) ? json : null);

                    if (list && list.length > 0) {
                        state.products = list;
                    }
                } else {
                    // Fallback sang local initial data để bảo đảm UI luôn sống động và tương tác mượt mà
                    console.info('Backend /api/products trả mã ' + response.status + ' - Sử dụng fallback local data cho CRM-39.');
                }
            } catch (err) {
                productLoadingOverlay.style.display = 'none';
                console.info('Chưa kết nối Backend Servlet - Sử dụng dữ liệu mẫu chuẩn CRM-39:', err);
            }

            updateStats();
            renderProductTable();
        }

        // Tải danh sách bảng giá (GET /api/price-books)
        async function fetchPriceBooks() {
            var endpoint = contextPath + '/api/price-books';
            try {
                var res = await fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });
                if (res.ok) {
                    var data = await res.json();
                    if (Array.isArray(data)) {
                        state.priceBooks = data;
                    }
                }
            } catch (e) {
                console.info('Chưa kết nối API Bảng giá /api/price-books:', e);
            }
            renderPriceBooks();
        }

        // Chuyển đổi Tab
        window.switchTab = function (tabName) {
            state.currentTab = tabName;
            var tabBtnProducts = document.getElementById('tabBtnProducts');
            var tabBtnPriceBooks = document.getElementById('tabBtnPriceBooks');
            var tabContentProducts = document.getElementById('tabContentProducts');
            var tabContentPriceBooks = document.getElementById('tabContentPriceBooks');

            if (tabName === 'products') {
                tabBtnProducts.classList.add('active');
                tabBtnPriceBooks.classList.remove('active');
                tabContentProducts.style.display = 'block';
                tabContentPriceBooks.style.display = 'none';
            } else {
                tabBtnProducts.classList.remove('active');
                tabBtnPriceBooks.classList.add('active');
                tabContentProducts.style.display = 'none';
                tabContentPriceBooks.style.display = 'block';
                renderPriceBooks();
            }
        };

        // Chuyển đổi vai trò người dùng (AC 3: Giám đốc kinh doanh vs Nhân viên)
        roleSelect.addEventListener('change', function () {
            state.role = roleSelect.value;
            if (state.role === 'SALES_REP') {
                roleNoticeAlert.style.display = 'flex';
                roleNoticeMessage.textContent = 'Bạn đang xem với vai trò Nhân viên kinh doanh. Dữ liệu Giá vốn đã được mã hóa bảo mật theo tiêu chí AC 3.';
            } else {
                roleNoticeAlert.style.display = 'none';
            }
            renderProductTable();
        });

        // Tìm kiếm & Bộ lọc
        productSearchInput.addEventListener('input', function () {
            state.keyword = productSearchInput.value.trim();
            renderProductTable();
        });

        filterProductType.addEventListener('change', function () {
            state.filterType = filterProductType.value;
            renderProductTable();
        });

        filterProductActive.addEventListener('change', function () {
            state.filterActive = filterProductActive.value;
            renderProductTable();
        });

        btnResetProductFilter.addEventListener('click', function () {
            productSearchInput.value = '';
            filterProductType.value = '';
            filterProductActive.value = '';
            state.keyword = '';
            state.filterType = '';
            state.filterActive = '';
            renderProductTable();
        });

        // Mở Modal Thêm mới sản phẩm
        btnOpenCreateProductModal.addEventListener('click', function () {
            hideAlerts();
            productForm.reset();
            formProductId.value = '';
            productModalHeading.textContent = 'Thêm sản phẩm mới';

            // Xử lý quyền xem/sửa giá vốn (AC 3)
            var isDirector = state.role === 'SALES_DIRECTOR';
            if (isDirector) {
                groupCostPrice.style.display = 'flex';
                formCostPrice.disabled = false;
                noticeCostPriceHidden.style.display = 'none';
            } else {
                groupCostPrice.style.display = 'none';
                formCostPrice.disabled = true;
                noticeCostPriceHidden.style.display = 'flex';
            }

            clearFormErrors();
            productModal.style.display = 'flex';
        });

        // Đóng Modal Sản phẩm
        function closeProductModal() {
            productModal.style.display = 'none';
            clearFormErrors();
        }

        btnCloseProductModal.addEventListener('click', closeProductModal);
        btnCancelProductModal.addEventListener('click', closeProductModal);

        // Mở Modal Sửa sản phẩm
        function openEditProductModal(id) {
            var p = state.products.find(function (item) { return item.id === Number(id); });
            if (!p) return;

            hideAlerts();
            clearFormErrors();
            formProductId.value = p.id;
            formProductCode.value = p.code;
            formProductName.value = p.name;
            formProductType.value = p.type;
            formProductUnit.value = p.unit;
            formListPrice.value = p.listPrice;
            formFloorPrice.value = p.floorPrice;
            formProductActive.value = p.active ? 'true' : 'false';
            formProductDescription.value = p.description || '';

            // AC 3: Kiểm tra quyền xem/sửa giá vốn
            var isDirector = state.role === 'SALES_DIRECTOR';
            if (isDirector) {
                groupCostPrice.style.display = 'flex';
                formCostPrice.disabled = false;
                formCostPrice.value = p.costPrice != null ? p.costPrice : '';
                noticeCostPriceHidden.style.display = 'none';
            } else {
                groupCostPrice.style.display = 'none';
                formCostPrice.disabled = true;
                formCostPrice.value = '';
                noticeCostPriceHidden.style.display = 'flex';
            }

            productModalHeading.textContent = 'Chỉnh sửa sản phẩm: ' + p.code;
            productModal.style.display = 'flex';
        }

        function clearFormErrors() {
            document.querySelectorAll('.form-feedback').forEach(function (el) { el.textContent = ''; });
            document.querySelectorAll('.form-control').forEach(function (el) { el.classList.remove('is-invalid'); });
        }

        // Validate Form Sản phẩm (AC 1, AC 2, AC 3)
        function validateProductForm() {
            clearFormErrors();
            var valid = true;

            var code = formProductCode.value.trim();
            if (!code) {
                document.getElementById('feedbackProductCode').textContent = 'Vui lòng nhập mã sản phẩm.';
                formProductCode.classList.add('is-invalid');
                valid = false;
            }

            var name = formProductName.value.trim();
            if (!name) {
                document.getElementById('feedbackProductName').textContent = 'Vui lòng nhập tên sản phẩm.';
                formProductName.classList.add('is-invalid');
                valid = false;
            }

            var unit = formProductUnit.value.trim();
            if (!unit) {
                document.getElementById('feedbackProductUnit').textContent = 'Vui lòng nhập đơn vị tính.';
                formProductUnit.classList.add('is-invalid');
                valid = false;
            }

            var listPrice = parseFloat(formListPrice.value);
            if (isNaN(listPrice) || listPrice <= 0) {
                document.getElementById('feedbackListPrice').textContent = 'Giá niêm yết phải lớn hơn 0.';
                formListPrice.classList.add('is-invalid');
                valid = false;
            }

            var floorPrice = parseFloat(formFloorPrice.value);
            if (isNaN(floorPrice) || floorPrice <= 0) {
                document.getElementById('feedbackFloorPrice').textContent = 'Giá sàn phải lớn hơn 0.';
                formFloorPrice.classList.add('is-invalid');
                valid = false;
            }

            // AC 2: Giá sàn không được vượt quá Giá niêm yết
            if (!isNaN(listPrice) && !isNaN(floorPrice) && floorPrice > listPrice) {
                document.getElementById('feedbackFloorPrice').textContent = 'Giá sàn (' + formatVND(floorPrice) + ') không được lớn hơn Giá niêm yết (' + formatVND(listPrice) + '). Đây là ngưỡng tối thiểu duyệt chiết khấu!';
                formFloorPrice.classList.add('is-invalid');
                valid = false;
            }

            // AC 3: Nếu là Giám đốc kinh doanh thì kiểm tra giá vốn
            if (state.role === 'SALES_DIRECTOR') {
                var costPrice = parseFloat(formCostPrice.value);
                if (isNaN(costPrice) || costPrice < 0) {
                    document.getElementById('feedbackCostPrice').textContent = 'Giá vốn không được để trống và phải >= 0.';
                    formCostPrice.classList.add('is-invalid');
                    valid = false;
                }
            }

            return valid;
        }

        // Xử lý Submit Form Sản phẩm (POST/PUT /api/products)
        productForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            if (!validateProductForm()) return;

            var id = formProductId.value;
            var isEdit = Boolean(id);

            var payload = {
                code: formProductCode.value.trim().toUpperCase(),
                name: formProductName.value.trim(),
                type: formProductType.value,
                unit: formProductUnit.value.trim(),
                listPrice: parseFloat(formListPrice.value),
                floorPrice: parseFloat(formFloorPrice.value),
                active: formProductActive.value === 'true',
                description: formProductDescription.value.trim()
            };

            if (state.role === 'SALES_DIRECTOR') {
                payload.costPrice = parseFloat(formCostPrice.value);
            }

            var endpoint = isEdit ? (contextPath + '/api/products/' + id) : (contextPath + '/api/products');
            var method = isEdit ? 'PUT' : 'POST';
            var savedProduct = null;
            var localDemoOnly = false;

            try {
                var res = await fetch(endpoint, {
                    method: method,
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (!res.ok) {
                    showErrorAlert((isEdit ? 'Không thể cập nhật' : 'Không thể tạo') + ' sản phẩm trên máy chủ (Mã lỗi: ' + res.status + '). Dữ liệu local chưa thay đổi.');
                    return;
                }

                try {
                    var responseBody = await res.json();
                    savedProduct = responseBody && responseBody.data ? responseBody.data : responseBody;
                    if (savedProduct && savedProduct.item) savedProduct = savedProduct.item;
                } catch (ignored) {}
            } catch (err) {
                localDemoOnly = true;
                console.info('Fetch lỗi backend - Chỉ áp dụng cập nhật demo tại local state:', err);
            }

            if (isEdit) {
                var idx = state.products.findIndex(function (item) { return item.id === Number(id); });
                if (idx !== -1) {
                    payload.id = Number(id);
                    payload.quoteCount = state.products[idx].quoteCount;
                    if (payload.costPrice === undefined) {
                        payload.costPrice = state.products[idx].costPrice;
                    }
                    state.products[idx] = Object.assign({}, state.products[idx], payload, savedProduct || {});
                }
            } else {
                var serverId = savedProduct && savedProduct.id != null ? Number(savedProduct.id) : null;
                var newId = serverId != null
                    ? serverId
                    : (state.products.length > 0 ? Math.max.apply(null, state.products.map(function(p){ return p.id; })) + 1 : 1);
                payload.id = newId;
                payload.quoteCount = 0;
                if (payload.costPrice === undefined) payload.costPrice = 0;
                state.products.unshift(Object.assign({}, payload, savedProduct || {}));
            }

            if (localDemoOnly) {
                showErrorAlert('DEMO / Local only - chưa lưu server. Thay đổi sản phẩm chỉ tồn tại tạm thời trên trình duyệt.');
            } else {
                showSuccessAlert(isEdit
                    ? 'Đã cập nhật thông tin sản phẩm [' + payload.code + ']!'
                    : 'Đã thêm sản phẩm [' + payload.code + '] vào danh mục!');
            }

            closeProductModal();
            updateStats();
            renderProductTable();
        });

        // Thao tác dòng: Sửa, Đổi trạng thái, Xóa/Ngừng kinh doanh
        productTableBody.addEventListener('click', function (e) {
            var btn = e.target.closest('button[data-action]');
            if (!btn) return;

            var action = btn.getAttribute('data-action');
            var id = Number(btn.getAttribute('data-id'));

            if (action === 'edit') {
                openEditProductModal(id);
            } else if (action === 'toggle-active') {
                toggleProductActive(id);
            } else if (action === 'delete') {
                handleDeleteProduct(id);
            }
        });

        // Đổi trạng thái nhanh Active / Inactive
        async function toggleProductActive(id) {
            var p = state.products.find(function (item) { return item.id === id; });
            if (!p) return;

            var newStatus = !p.active;
            var endpoint = contextPath + '/api/products/' + id;

            try {
                var response = await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify({ active: newStatus })
                });

                if (!response.ok) {
                    showErrorAlert('Không thể đổi trạng thái sản phẩm trên máy chủ (Mã lỗi: ' + response.status + '). Trạng thái chưa thay đổi.');
                    return;
                }
            } catch (e) {
                console.error('Lỗi khi đổi trạng thái sản phẩm:', e);
                showErrorAlert('Không thể kết nối máy chủ để đổi trạng thái sản phẩm. Trạng thái chưa thay đổi.');
                return;
            }

            p.active = newStatus;
            showSuccessAlert('Đã chuyển trạng thái sản phẩm [' + p.code + '] sang: ' + (newStatus ? 'Đang kinh doanh' : 'Ngừng kinh doanh'));
            updateStats();
            renderProductTable();
        }

        // Xử lý Xóa / Ngừng kinh doanh (AC 4: Sản phẩm đã có trong báo giá không xóa được)
        function handleDeleteProduct(id) {
            var p = state.products.find(function (item) { return item.id === id; });
            if (!p) return;

            // AC 4: Nếu sản phẩm đã xuất hiện trong báo giá thì KHÔNG ĐƯỢC XÓA, chỉ được phép ngừng kinh doanh
            if (p.quoteCount && p.quoteCount > 0) {
                state.pendingDeactivateProduct = p;
                deactivateModalContent.innerHTML =
                    'Sản phẩm <strong>[' + escapeHtml(p.code) + ' - ' + escapeHtml(p.name) + ']</strong> đã xuất hiện trong <strong>' +
                    p.quoteCount + ' báo giá</strong>. Theo tiêu chí nghiệm thu <strong>AC 4</strong>, hệ thống <span style="color:#ef4444;font-weight:700;">không cho phép xóa bỏ hoàn toàn</span> để bảo đảm tính toàn vẹn dữ liệu hợp đồng/báo giá lịch sử.';
                deactivateConfirmModal.style.display = 'flex';
                return;
            }

            // Nếu chưa có báo giá nào, cho phép xóa hoặc xác nhận
            if (confirm('Sản phẩm [' + p.code + '] chưa phát sinh báo giá nào. Bạn có chắc chắn muốn xóa sản phẩm này không?')) {
                deleteProductDirectly(id);
            }
        }

        async function deleteProductDirectly(id) {
            var endpoint = contextPath + '/api/products/' + id;
            try {
                var response = await fetch(endpoint, {
                    method: 'DELETE',
                    headers: { 'Accept': 'application/json' }
                });

                if (!response.ok) {
                    showErrorAlert('Không thể xóa sản phẩm trên máy chủ (Mã lỗi: ' + response.status + '). Dữ liệu vẫn được giữ nguyên.');
                    return;
                }
            } catch (e) {
                console.error('Lỗi khi xóa sản phẩm:', e);
                showErrorAlert('Không thể kết nối máy chủ để xóa sản phẩm. Dữ liệu vẫn được giữ nguyên.');
                return;
            }
            state.products = state.products.filter(function (p) { return p.id !== id; });
            showSuccessAlert('Đã xóa sản phẩm thành công!');
            updateStats();
            renderProductTable();
        }

        // Xác nhận chuyển sang Ngừng kinh doanh (AC 4)
        btnConfirmDeactivate.addEventListener('click', async function () {
            if (!state.pendingDeactivateProduct) return;
            var p = state.pendingDeactivateProduct;

            var endpoint = contextPath + '/api/products/' + p.id;
            try {
                var response = await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify({ active: false })
                });

                if (!response.ok) {
                    showErrorAlert('Không thể ngừng kinh doanh sản phẩm trên máy chủ (Mã lỗi: ' + response.status + '). Trạng thái chưa thay đổi.');
                    return;
                }
            } catch (e) {
                console.error('Lỗi khi ngừng kinh doanh sản phẩm:', e);
                showErrorAlert('Không thể kết nối máy chủ để ngừng kinh doanh sản phẩm. Trạng thái chưa thay đổi.');
                return;
            }

            p.active = false;
            deactivateConfirmModal.style.display = 'none';
            state.pendingDeactivateProduct = null;
            showSuccessAlert('Đã chuyển sản phẩm [' + p.code + '] sang trạng thái [Ngừng kinh doanh] theo tiêu chuẩn AC 4!');
            updateStats();
            renderProductTable();
        });

        btnCloseDeactivateModal.addEventListener('click', function () {
            deactivateConfirmModal.style.display = 'none';
            state.pendingDeactivateProduct = null;
        });
        btnCancelDeactivate.addEventListener('click', function () {
            deactivateConfirmModal.style.display = 'none';
            state.pendingDeactivateProduct = null;
        });

        // Modal Tạo Bảng giá mới
        btnOpenCreatePriceBookModal.addEventListener('click', function () {
            priceBookForm.reset();
            var today = new Date().toISOString().substring(0, 10);
            document.getElementById('formPriceBookEffectiveFrom').value = today;

            // Render danh sách sản phẩm đang kinh doanh để chọn vào bảng giá
            priceBookLinesTbody.innerHTML = '';
            var activeProducts = state.products.filter(function (p) { return p.active; });
            activeProducts.forEach(function (p) {
                var row = document.createElement('tr');
                row.innerHTML =
                    '<td><input type="checkbox" class="pb-check-item" data-code="' + escapeHtml(p.code) + '" checked></td>' +
                    '<td><span class="col-code">' + escapeHtml(p.code) + '</span></td>' +
                    '<td>' + escapeHtml(p.name) + '</td>' +
                    '<td style="text-align: right;"><input type="number" class="form-control pb-price-input" style="width: 140px; display: inline-block; text-align: right;" value="' + p.listPrice + '" min="0" step="1000"></td>';
                priceBookLinesTbody.appendChild(row);
            });

            priceBookModal.style.display = 'flex';
        });

        function closePriceBookModal() {
            priceBookModal.style.display = 'none';
        }
        btnClosePriceBookModal.addEventListener('click', closePriceBookModal);
        btnCancelPriceBookModal.addEventListener('click', closePriceBookModal);

        // Submit Tạo Bảng giá (POST /api/price-books)
        priceBookForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            var name = document.getElementById('formPriceBookName').value.trim();
            var effectiveFrom = document.getElementById('formPriceBookEffectiveFrom').value;
            var description = document.getElementById('formPriceBookDescription').value.trim();

            if (!name) {
                alert('Vui lòng nhập tên bảng giá!');
                return;
            }
            if (!effectiveFrom) {
                alert('Vui lòng chọn ngày có hiệu lực!');
                return;
            }

            var lines = [];
            document.querySelectorAll('.pb-check-item:checked').forEach(function (chk) {
                var row = chk.closest('tr');
                var code = chk.getAttribute('data-code');
                var priceInput = row.querySelector('.pb-price-input');
                var price = parseFloat(priceInput.value) || 0;
                lines.push({ productCode: code, price: price });
            });

            if (lines.length === 0) {
                alert('Vui lòng chọn ít nhất 1 sản phẩm vào bảng giá!');
                return;
            }

            var payload = {
                name: name,
                effectiveFrom: effectiveFrom,
                description: description,
                lines: lines
            };

            var endpoint = contextPath + '/api/price-books';
            var savedPriceBook = null;
            var localDemoOnly = false;
            try {
                var response = await fetch(endpoint, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    showErrorAlert('Không thể tạo bảng giá trên máy chủ (Mã lỗi: ' + response.status + '). Dữ liệu local chưa thay đổi.');
                    return;
                }

                try {
                    var responseBody = await response.json();
                    savedPriceBook = responseBody && responseBody.data ? responseBody.data : responseBody;
                    if (savedPriceBook && savedPriceBook.item) savedPriceBook = savedPriceBook.item;
                } catch (ignored) {}
            } catch (err) {
                localDemoOnly = true;
                console.info('Backend API Bảng giá chưa hoạt động - Chỉ lưu demo tại local state:', err);
            }

            var serverId = savedPriceBook && savedPriceBook.id != null ? Number(savedPriceBook.id) : null;
            payload.id = serverId != null
                ? serverId
                : (state.priceBooks.length > 0 ? Math.max.apply(null, state.priceBooks.map(function(pb) { return pb.id; })) + 1 : 1);
            payload.active = true;
            payload.itemCount = lines.length;
            state.priceBooks.unshift(Object.assign({}, payload, savedPriceBook || {}));

            closePriceBookModal();
            if (localDemoOnly) {
                showErrorAlert('DEMO / Local only - chưa lưu server. Bảng giá chỉ tồn tại tạm thời trên trình duyệt.');
            } else {
                showSuccessAlert('Đã tạo mới Bảng giá niêm yết chuẩn [' + name + ']!');
            }
            updateStats();
            renderPriceBooks();
        });

        // Check All items in Price Book modal
        var checkAllPb = document.getElementById('checkAllPriceBookLines');
        if (checkAllPb) {
            checkAllPb.addEventListener('change', function () {
                var checked = checkAllPb.checked;
                document.querySelectorAll('.pb-check-item').forEach(function (chk) {
                    chk.checked = checked;
                });
            });
        }

        // Khởi động
        fetchProducts();
        fetchPriceBooks();

    })();
    </script>
</body>
</html>
