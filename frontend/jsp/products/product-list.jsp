<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.User,com.crm.util.SessionKey,java.util.Collection,java.util.List,java.util.Locale" %>
<%
    Object roleSource = session.getAttribute(SessionKey.ROLES);
    Collection<?> productRoles = roleSource instanceof Collection<?> ? (Collection<?>) roleSource : List.of();
    Object sessionUser = session.getAttribute(SessionKey.CURRENT_USER);
    if (!(roleSource instanceof Collection<?>) && sessionUser instanceof User && ((User) sessionUser).getRoles() != null) {
        productRoles = ((User) sessionUser).getRoles().stream().map(com.crm.model.Role::getName).toList();
    }
    boolean canManageProductCost = productRoles.stream().filter(String.class::isInstance)
        .map(value -> ((String) value).trim().toLowerCase(Locale.ROOT))
        .anyMatch(value -> List.of("admin", "director", "giám đốc", "giam doc", "quản trị viên", "quan tri vien").contains(value));
%>
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

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <!-- CSS riêng biệt của module Quản lý sản phẩm & Bảng giá (CRM-39) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/products/products.css">
</head>
<body class="crm-body products-page-shell">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Sản phẩm & Bảng giá -->
        <main class="product-page crm-page" id="productApp" data-context-path="${pageContext.request.contextPath}" data-can-manage-cost="<%= canManageProductCost %>" role="main">
            <div class="product-container crm-page-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="product-breadcrumb crm-breadcrumb" aria-label="Đường dẫn trang">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Kinh doanh</span>
                    <span class="separator">/</span>
                    <span class="active">Sản phẩm & Bảng giá niêm yết</span>
                </nav>

                <!-- Header màn hình & Role Simulator Context -->
                <header class="product-header crm-page-header">
                    <div class="product-header-info">
                        <h1 class="crm-page-title">Sản phẩm & Bảng giá</h1>
                        <p class="crm-page-description">Quản lý sản phẩm, dịch vụ và chính sách giá kinh doanh.</p>
                    </div>

                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts / Banners) -->
                <div class="product-alerts" id="productAlertsArea" aria-live="polite">
                    <div class="product-alert crm-alert product-alert-danger crm-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
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

                    <div class="product-alert crm-alert product-alert-success crm-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
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

                    <div class="product-alert crm-alert product-alert-warning crm-alert-warning" id="roleNoticeAlert" style="display: none;" role="status">
                        <svg class="product-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="product-alert-content">
                            <div class="product-alert-title">Quyền xem Giá vốn</div>
                            <div id="roleNoticeMessage">Bạn đang xem với vai trò Nhân viên kinh doanh. Giá vốn chỉ hiển thị cho tài khoản có quyền truy cập.</div>
                        </div>
                    </div>
                </div>

                <!-- Thanh chuyển Tab (Danh mục sản phẩm vs Bảng giá) -->
                <nav class="product-tabs-nav" aria-label="Thanh chuyển đổi danh mục và bảng giá">
                    <button type="button" class="product-tab-btn active" id="tabBtnProducts" onclick="switchTab('products')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                            <polyline points="2 17 12 22 22 17"></polyline>
                            <polyline points="2 12 12 17 22 12"></polyline>
                        </svg>
                        <span>Danh mục sản phẩm & Dịch vụ</span>
                        <span class="product-tab-badge" id="badgeProductsTabCount">—</span>
                    </button>

                    <button type="button" class="product-tab-btn" id="tabBtnPriceBooks" onclick="switchTab('pricebooks')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                        </svg>
                        <span>Bảng giá niêm yết</span>
                        <span class="product-tab-badge" id="badgePriceBooksTabCount">—</span>
                    </button>
                </nav>

                <!-- =========================================================
                     TAB 1: DANH MỤC SẢN PHẨM & DỊCH VỤ
                     ========================================================= -->
                <div id="tabContentProducts">
                    <section class="product-card crm-card" style="position: relative;" aria-label="Danh sách sản phẩm">

                        <!-- Loading Overlay -->
                        <div class="product-loading-overlay" id="productLoadingOverlay" aria-hidden="true">
                            <div class="product-spinner"></div>
                        </div>

                        <!-- Toolbar & Bộ lọc -->
                        <div class="product-toolbar crm-toolbar">
                            <div class="product-toolbar-left">
                                <!-- Ô tìm kiếm -->
                                <div class="product-search-wrap">
                                    <svg class="product-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                    </svg>
                                    <input type="search" id="productSearchInput" class="product-search-input crm-input"
                                           placeholder="Tìm theo mã hoặc tên sản phẩm..."
                                           aria-label="Tìm kiếm sản phẩm">
                                </div>

                                <!-- Bộ lọc loại sản phẩm (AC 1) -->
                                <select id="filterProductType" class="product-filter-select crm-select" aria-label="Lọc theo loại sản phẩm">
                                    <option value="">Tất cả loại sản phẩm</option>
                                    <option value="ONE_TIME">Sản phẩm một lần</option>
                                    <option value="SUBSCRIPTION">Dịch vụ thuê bao</option>
                                </select>

                                <!-- Bộ lọc trạng thái kinh doanh -->
                                <select id="filterProductActive" class="product-filter-select crm-select" aria-label="Lọc theo trạng thái kinh doanh">
                                    <option value="">Tất cả trạng thái</option>
                                    <option value="true">Đang sử dụng</option>
                                    <option value="false">Ngừng sử dụng</option>
                                </select>

                                <!-- Nút Đặt lại bộ lọc -->
                                <button type="button" class="btn crm-btn btn-secondary crm-btn-secondary btn-sm" id="btnResetProductFilter" title="Xóa bộ lọc">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <polyline points="1 4 1 10 7 10"></polyline>
                                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                    </svg>
                                    Đặt lại
                                </button>
                            </div>

                            <div class="product-toolbar-right">
                                <!-- Nút Thêm sản phẩm mới -->
                                <button type="button" class="btn crm-btn btn-primary crm-btn-primary" id="btnOpenCreateProductModal">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <line x1="12" y1="5" x2="12" y2="19"></line>
                                        <line x1="5" y1="12" x2="19" y2="12"></line>
                                    </svg>
                                    <span>Thêm sản phẩm</span>
                                </button>
                            </div>
                        </div>

                        <!-- Bảng dữ liệu sản phẩm (Table Responsive) -->
                        <div class="product-table-responsive crm-table-wrap" tabindex="0" role="region" aria-label="Bảng sản phẩm; có thể cuộn ngang">
                            <table class="product-table crm-table" id="productTable" aria-label="Bảng dữ liệu danh mục sản phẩm">
                                <thead>
                                    <tr>
                                        <th scope="col" style="width: 50px;">#</th>
                                        <th scope="col">Mã SP</th>
                                        <th scope="col">Tên sản phẩm & Dịch vụ</th>
                                        <th scope="col">Loại hình</th>
                                        <th scope="col">ĐVT</th>
                                        <th scope="col" style="text-align: right;">Giá niêm yết</th>
                                        <th scope="col" style="text-align: right;" title="Ngưỡng tối thiểu duyệt chiết khấu báo giá">
                                            Giá sàn
                                            <span style="font-size: 0.75rem; color: #b45309;">(*)</span>
                                        </th>
                                        <th scope="col" style="text-align: right;" id="thCostPrice" <%= canManageProductCost ? "" : "hidden" %> title="Giá vốn hiển thị theo quyền tài khoản">
                                            Giá vốn

                                        </th>
                                        <th scope="col" style="text-align: center;" title="Số báo giá đã sử dụng sản phẩm này">Báo giá</th>
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
                            <div class="empty-icon" aria-hidden="true"><svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="m3 7 9-5 9 5v10l-9 5-9-5zM3 7l9 5 9-5M12 12v10"/></svg></div>
                            <div class="empty-title">Không tìm thấy sản phẩm nào</div>
                            <p class="empty-subtitle">Không có sản phẩm nào phù hợp với điều kiện tìm kiếm và bộ lọc của bạn.</p>
                            <button type="button" class="btn crm-btn btn-secondary crm-btn-secondary" onclick="document.getElementById('btnResetProductFilter').click()">
                                Xóa bộ lọc tìm kiếm
                            </button>
                        </div>

                        <!-- Phân trang (Pagination) -->
                        <div class="product-pagination" id="productPagination">
                            <div class="pagination-info" id="productPaginationInfo">
                                Đang tải danh sách sản phẩm…
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
                    <section class="product-card crm-card" aria-labelledby="priceBookCardTitle">
                        <div class="product-toolbar crm-toolbar">
                            <div class="product-toolbar-left">
                                <h3 id="priceBookCardTitle" style="margin: 0; font-size: 1.0625rem; font-weight: 700; color: #0f172a;">
                                    Danh sách Bảng giá niêm yết chuẩn
                                </h3>
                                <p style="margin: 0; font-size: 0.8125rem; color: #64748b;">
                                    Bảng giá chuẩn làm căn cứ xuất phát điểm cho mọi báo giá kinh doanh.
                                </p>
                            </div>
                            <div class="product-toolbar-right">
                                <button type="button" class="btn crm-btn btn-primary crm-btn-primary" id="btnOpenCreatePriceBookModal">
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
                <p id="productFormError" class="form-feedback" role="alert"></p>
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
                            <strong>Quy định nghiệp vụ:</strong> Giá sàn là ngưỡng tối thiểu để hệ thống kiểm soát chiết khấu. Giá sàn không được vượt quá giá niêm yết.
                        </div>
                    </div>

                    <!-- Hàng 1: Mã sản phẩm & Tên sản phẩm -->
                    <div class="form-row-2">
                        <div class="form-group crm-form-group">
                            <label for="formProductCode" class="form-label crm-label">
                                <span>Mã sản phẩm <span class="required-mark">*</span></span>
                                <span class="form-hint">Duy nhất (VD: CRM-SUB-PRO)</span>
                            </label>
                            <input type="text" id="formProductCode" name="code" class="form-control crm-input"
                                   placeholder="Nhập thông tin" required uppercase>
                            <div class="form-feedback" id="feedbackProductCode"></div>
                        </div>

                        <div class="form-group crm-form-group">
                            <label for="formProductName" class="form-label crm-label">
                                <span>Tên sản phẩm / Dịch vụ <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formProductName" name="name" class="form-control crm-input"
                                   placeholder="Nhập thông tin" required>
                            <div class="form-feedback" id="feedbackProductName"></div>
                        </div>
                    </div>

                    <!-- Hàng 2: Loại sản phẩm & Đơn vị tính -->
                    <div class="form-row-2">
                        <div class="form-group crm-form-group">
                            <label for="formProductType" class="form-label crm-label">
                                <span>Loại sản phẩm <span class="required-mark">*</span></span>
                            </label>
                            <select id="formProductType" name="type" class="form-control crm-select" required>
                                <option value="ONE_TIME">Sản phẩm một lần</option>
                                <option value="SUBSCRIPTION">Dịch vụ thuê bao</option>
                            </select>
                            <div class="form-feedback" id="feedbackProductType"></div>
                        </div>

                        <div class="form-group crm-form-group">
                            <label for="formProductUnit" class="form-label crm-label">
                                <span>Đơn vị tính <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formProductUnit" name="unit" class="form-control crm-input"
                                   placeholder="Nhập thông tin" required>
                            <div class="form-feedback" id="feedbackProductUnit"></div>
                        </div>
                    </div>

                    <!-- Hàng 3: Giá niêm yết & Giá sàn (AC 1 & AC 2) -->
                    <div class="form-row-2">
                        <div class="form-group crm-form-group">
                            <label for="formListPrice" class="form-label crm-label">
                                <span>Giá niêm yết (VNĐ) <span class="required-mark">*</span></span>
                                <span class="form-hint">Giá chuẩn ban đầu</span>
                            </label>
                            <input type="number" id="formListPrice" name="listPrice" class="form-control crm-input"
                                   placeholder="Nhập thông tin" min="0" step="1000" required>
                            <div class="form-feedback" id="feedbackListPrice"></div>
                        </div>

                        <div class="form-group crm-form-group">
                            <label for="formFloorPrice" class="form-label crm-label">
                                <span>Giá sàn (VNĐ) <span class="required-mark">*</span></span>
                                <span class="form-hint">Ngưỡng duyệt chiết khấu</span>
                            </label>
                            <input type="number" id="formFloorPrice" name="floorPrice" class="form-control crm-input"
                                   placeholder="Nhập thông tin" min="0" step="1000" required>
                            <div class="form-feedback" id="feedbackFloorPrice"></div>
                        </div>
                    </div>

                    <!-- Hàng 4: Giá vốn (AC 3: Chỉ Giám đốc kinh doanh có quyền xem & sửa) -->
                    <div class="form-group crm-form-group" id="groupCostPrice">
                        <label for="formCostPrice" class="form-label crm-label">
                            <span>
                                Giá vốn (VNĐ)
                                <span class="required-mark">*</span>
                                <span style="font-size: 0.75rem; color: #2563eb; font-weight: 600;">(Theo phân quyền)</span>
                            </span>
                            <span class="form-hint">Cơ sở tính biên lợi nhuận</span>
                        </label>
                        <input type="number" id="formCostPrice" name="costPrice" class="form-control crm-input"
                               placeholder="Nhập thông tin" min="0" step="1000" required>
                        <div class="form-feedback" id="feedbackCostPrice"></div>
                    </div>

                    <!-- Hộp cảnh báo khi nhân viên kinh doanh không có quyền xem/sửa giá vốn -->
                    <div class="cost-price-director-only" id="noticeCostPriceHidden" style="display: none;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        <span>Trường <strong>Giá vốn</strong> bị ẩn và chỉ được phép xem/sửa bởi người dùng có vai trò <strong>Giám đốc kinh doanh</strong>.</span>
                    </div>

                    <!-- Hàng 5: Trạng thái & Mô tả -->
                    <div class="form-group crm-form-group">
                        <label for="formProductActive" class="form-label crm-label">
                            <span>Trạng thái kinh doanh <span class="required-mark">*</span></span>
                        </label>
                        <select id="formProductActive" name="active" class="form-control crm-select" required>
                            <option value="true">Đang sử dụng</option>
                            <option value="false">Ngừng sử dụng</option>
                        </select>
                        <div class="form-feedback" id="feedbackProductActive"></div>
                    </div>

                    <div class="form-group crm-form-group">
                        <label for="formProductDescription" class="form-label crm-label">
                            <span>Mô tả sản phẩm</span>
                        </label>
                        <textarea id="formProductDescription" name="description" class="form-control crm-textarea" rows="3"
                                  placeholder="Mô tả phạm vi áp dụng, tính năng hoặc điều kiện dịch vụ..."></textarea>
                    </div>

                </div>

                <footer class="crm-modal-footer">
                    <button type="button" class="btn crm-btn btn-secondary crm-btn-secondary" id="btnCancelProductModal">Hủy bỏ</button>
                    <button type="submit" class="btn crm-btn btn-primary crm-btn-primary" id="btnSaveProduct">
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
                <p id="priceBookFormError" class="form-feedback" role="alert"></p>
                <div class="crm-modal-body">
                    <div class="form-row-2">
                        <div class="form-group crm-form-group">
                            <label for="formPriceBookName" class="form-label crm-label">
                                <span>Tên bảng giá <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formPriceBookName" name="name" class="form-control crm-input"
                                   placeholder="Nhập thông tin" required>
                            <div class="form-feedback" id="feedbackPriceBookName"></div>
                        </div>

                        <div class="form-group crm-form-group">
                            <label for="formPriceBookEffectiveFrom" class="form-label crm-label">
                                <span>Ngày có hiệu lực <span class="required-mark">*</span></span>
                            </label>
                            <input type="date" id="formPriceBookEffectiveFrom" name="effectiveFrom" class="form-control crm-input" required>
                            <div class="form-feedback" id="feedbackPriceBookEffective"></div>
                        </div>
                    </div>

                    <div class="form-group crm-form-group">
                        <label for="formPriceBookDescription" class="form-label crm-label">
                            <span>Ghi chú / Phạm vi áp dụng</span>
                        </label>
                        <input type="text" id="formPriceBookDescription" name="description" class="form-control crm-input"
                               placeholder="Nhập thông tin">
                    </div>

                    <div class="form-group crm-form-group">
                        <label class="form-label crm-label">
                            <span>Danh sách mặt hàng & Đơn giá áp dụng trong bảng giá</span>
                            <span class="form-hint">Mặc định lấy theo giá niêm yết chuẩn</span>
                        </label>
                        <div class="crm-table-wrap pricebook-lines-wrap" tabindex="0" role="region" aria-label="Sản phẩm trong bảng giá; có thể cuộn ngang">
                            <table class="product-table crm-table" style="font-size: 0.8125rem;">
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
                    <button type="button" class="btn crm-btn btn-secondary crm-btn-secondary" id="btnCancelPriceBookModal">Hủy bỏ</button>
                    <button type="submit" class="btn crm-btn btn-primary crm-btn-primary" id="btnSavePriceBook">
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
         MODAL 3: CẢNH BÁO RÀNG BUỘC BÁO GIÁ & CHUYỂN NGỪNG KINH DOANH
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
                    <span>Cảnh báo ràng buộc Báo giá</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseDeactivateModal" aria-label="Đóng">&times;</button>
            </header>

            <div class="crm-modal-body">
                <p style="margin: 0; font-size: 0.9375rem; color: #334155; line-height: 1.5;" id="deactivateModalContent">
                    Sản phẩm này đã xuất hiện trong các báo giá kinh doanh. Để bảo vệ dữ liệu, hệ thống <strong>không cho phép xóa bỏ hoàn toàn</strong> để tránh mất tính toàn vẹn dữ liệu hợp đồng/báo giá.
                </p>
                <div style="padding: 12px 14px; border-radius: 8px; background-color: #f8fafc; border: 1px solid #e2e8f0; font-size: 0.875rem; color: #475569;">
                    Bạn có muốn chuyển trạng thái sản phẩm sang <strong>Ngừng sử dụng</strong> không? Sản phẩm sẽ không còn xuất hiện trong các báo giá mới nhưng lịch sử báo giá cũ vẫn được bảo toàn trọn vẹn.
                </div>
            </div>

            <footer class="crm-modal-footer">
                <button type="button" class="btn crm-btn btn-secondary crm-btn-secondary" id="btnCancelDeactivate">Giữ nguyên</button>
                <button type="button" class="btn crm-btn btn-danger crm-btn-danger" id="btnConfirmDeactivate">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="15" y1="9" x2="9" y2="15"></line>
                        <line x1="9" y1="9" x2="15" y2="15"></line>
                    </svg>
                    <span>Xác nhận Ngừng sử dụng</span>
                </button>
            </footer>
        </div>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- =================================================================
         JAVASCRIPT DOM THUẦN & FETCH API TÍCH HỢP (CRM-39)
         ================================================================= -->
    <script src="${pageContext.request.contextPath}/js/products/products.js" defer></script>
</body>
</html>
