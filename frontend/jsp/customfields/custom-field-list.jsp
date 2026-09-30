<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Khai báo trường tuỳ chỉnh (Custom Fields) - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Khai báo trường tuỳ chỉnh (CRM-46) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customfields/customfields.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Khai báo trường tuỳ chỉnh -->
        <main class="cf-page" id="customFieldsApp" role="main">
            <div class="cf-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="cf-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Thiết lập hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Khai báo trường tuỳ chỉnh (Custom Fields)</span>
                </nav>

                <!-- Header màn hình -->
                <header class="cf-header">
                    <div class="cf-header-info">
                        <h1>Khai báo trường tuỳ chỉnh (Custom Fields)</h1>
                        <p>Định nghĩa các cột dữ liệu mở rộng cho Khách hàng và Cơ hội để đưa các cột nhân viên đang tự thêm trong Excel vào hệ thống CRM chuẩn hóa.</p>
                    </div>

                    <div class="cf-header-badges">
                        <span class="cf-badge cf-badge-excel" title="Đồng bộ các cột tự thêm từ file Excel của nhân viên vào biểu mẫu chuẩn">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="8" y1="13" x2="16" y2="13"></line>
                                <line x1="8" y1="17" x2="16" y2="17"></line>
                                <polyline points="10 9 9 9 8 9"></polyline>
                            </svg>
                            Chuẩn hóa cột Excel
                        </span>
                        <span class="cf-badge cf-badge-scope" title="Tích hợp linh hoạt vào Biểu mẫu, Bộ lọc và Xuất báo cáo Excel">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                            Linh hoạt Form & Filter & Export
                        </span>
                        <span class="cf-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            S2-08 / CRM-46
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts) -->
                <div class="cf-alerts" id="cfAlertsArea" aria-live="polite">
                    <div class="cf-alert cf-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="cf-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="cf-alert-content">
                            <div class="cf-alert-title" id="globalErrorTitle">Đã xảy ra lỗi</div>
                            <div id="globalErrorMessage"></div>
                        </div>
                        <button type="button" class="cf-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="cf-alert cf-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="cf-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="cf-alert-content">
                            <div class="cf-alert-title" id="globalSuccessTitle">Thành công</div>
                            <div id="globalSuccessMessage"></div>
                        </div>
                        <button type="button" class="cf-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="cf-alert cf-alert-info" id="globalInfoAlert" style="display: none;" role="status">
                        <svg class="cf-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="16" x2="12" y2="12"></line>
                            <line x1="12" y1="8" x2="12.01" y2="8"></line>
                        </svg>
                        <div class="cf-alert-content">
                            <div class="cf-alert-title" id="globalInfoTitle">Thông tin</div>
                            <div id="globalInfoMessage"></div>
                        </div>
                        <button type="button" class="cf-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>
                </div>

                <!-- Banner Nghiệp vụ Chuẩn hóa Cột Excel vào Hệ thống (Mô tả Jira CRM-46) -->
                <aside class="cf-scope-banner" role="region" aria-label="Mục tiêu nghiệp vụ CRM-46">
                    <svg class="cf-scope-banner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    <div>
                        <div class="cf-scope-banner-title">Mục tiêu chuẩn hóa trường tuỳ chỉnh (CRM-46)</div>
                        <p class="cf-scope-banner-desc">
                            Trong thực tế kinh doanh, nhân viên thường tự thêm các cột riêng lẻ trong Excel (ví dụ: Mã số thuế, Phân hạng đối tác VIP, Đối thủ cạnh tranh, Thời hạn đấu thầu...).
                            Chức năng này cho phép Quản trị viên khai báo các trường đó trực tiếp vào hệ thống với 4 kiểu dữ liệu chuẩn (Văn bản, Số, Ngày, Danh sách chọn), tự động hiển thị trong <strong>Biểu mẫu nhập liệu (Form)</strong>, <strong>Bộ lọc tìm kiếm (Filter)</strong> và <strong>Bản xuất Excel</strong>.
                        </p>
                    </div>
                </aside>

                <!-- Thống kê nhanh chỉ số trường tuỳ chỉnh -->
                <section class="cf-stats-grid" aria-label="Thống kê trường tuỳ chỉnh">
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon-wrap stat-icon-blue">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <line x1="3" y1="9" x2="21" y2="9"></line>
                                <line x1="9" y1="21" x2="9" y2="9"></line>
                            </svg>
                        </div>
                        <div class="cf-stat-content">
                            <span class="cf-stat-value" id="statTotalFields">0</span>
                            <span class="cf-stat-label">Tổng số trường khai báo</span>
                        </div>
                    </div>

                    <div class="cf-stat-card">
                        <div class="cf-stat-icon-wrap stat-icon-amber">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                        </div>
                        <div class="cf-stat-content">
                            <span class="cf-stat-value" id="statRequiredFields">0</span>
                            <span class="cf-stat-label">Trường bắt buộc nhập (Required)</span>
                        </div>
                    </div>

                    <div class="cf-stat-card">
                        <div class="cf-stat-icon-wrap stat-icon-indigo">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polyline points="6 9 12 15 18 9"></polyline>
                            </svg>
                        </div>
                        <div class="cf-stat-content">
                            <span class="cf-stat-value" id="statDropdownFields">0</span>
                            <span class="cf-stat-label">Danh sách chọn (Dropdown)</span>
                        </div>
                    </div>

                    <div class="cf-stat-card">
                        <div class="cf-stat-icon-wrap stat-icon-emerald">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                        </div>
                        <div class="cf-stat-content">
                            <span class="cf-stat-value" id="statActiveFields">0</span>
                            <span class="cf-stat-label">Đang kích hoạt (Active)</span>
                        </div>
                    </div>
                </section>

                <!-- Thanh chuyển Tab Đối tượng áp dụng (AC 4: CUSTOMER vs OPPORTUNITY) & Toggle chế độ xem -->
                <nav class="cf-entity-tabs" aria-label="Thanh chọn đối tượng áp dụng">
                    <div class="cf-entity-tabs-group">
                        <button type="button" class="cf-entity-tab-btn active" id="tabBtnCustomer" data-entity="CUSTOMER">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                            <span>Khách hàng (CUSTOMER)</span>
                            <span class="cf-tab-badge" id="badgeCustomerCount">0</span>
                        </button>

                        <button type="button" class="cf-entity-tab-btn" id="tabBtnOpportunity" data-entity="OPPORTUNITY">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <line x1="18" y1="20" x2="18" y2="10"></line>
                                <line x1="12" y1="20" x2="12" y2="4"></line>
                                <line x1="6" y1="20" x2="6" y2="14"></line>
                            </svg>
                            <span>Cơ hội bán hàng (OPPORTUNITY)</span>
                            <span class="cf-tab-badge" id="badgeOpportunityCount">0</span>
                        </button>
                    </div>

                    <!-- Nút chuyển đổi giao diện: Bảng cấu hình vs Mô phỏng trực quan (AC 3) -->
                    <div class="cf-view-toggle-group" role="group" aria-label="Chế độ hiển thị">
                        <button type="button" class="cf-view-btn active" id="btnViewTable" title="Xem danh sách quản lý cấu hình các trường">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <line x1="8" y1="6" x2="21" y2="6"></line>
                                <line x1="8" y1="12" x2="21" y2="12"></line>
                                <line x1="8" y1="18" x2="21" y2="18"></line>
                                <line x1="3" y1="6" x2="3.01" y2="6"></line>
                                <line x1="3" y1="12" x2="3.01" y2="12"></line>
                                <line x1="3" y1="18" x2="3.01" y2="18"></line>
                            </svg>
                            Bảng cấu hình
                        </button>
                        <button type="button" class="cf-view-btn" id="btnViewSimulator" title="Xem trước thực tế hiển thị trong Form, Bộ lọc và Excel">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                            Mô phỏng hiển thị (AC 3)
                        </button>
                    </div>
                </nav>

                <!-- KHU VỰC 1: BẢNG QUẢN LÝ CẤU HÌNH TRƯỜNG TUỲ CHỈNH -->
                <section class="cf-card" id="cfTableSection" style="position: relative;" aria-label="Bảng quản lý trường tuỳ chỉnh">

                    <!-- Loading Overlay -->
                    <div class="cf-loading-overlay" id="cfLoadingOverlay" aria-hidden="true">
                        <div class="cf-spinner"></div>
                    </div>

                    <!-- Toolbar & Bộ lọc -->
                    <div class="cf-toolbar">
                        <div class="cf-toolbar-left">
                            <div class="cf-search-wrap">
                                <svg class="cf-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="search" id="fieldSearchInput" class="cf-search-input"
                                       placeholder="Tìm theo nhãn hoặc mã trường..."
                                       aria-label="Tìm kiếm trường tuỳ chỉnh">
                            </div>

                            <select id="filterFieldType" class="cf-filter-select" aria-label="Lọc theo kiểu dữ liệu">
                                <option value="">Tất cả kiểu dữ liệu</option>
                                <option value="TEXT">Văn bản (TEXT)</option>
                                <option value="NUMBER">Số (NUMBER)</option>
                                <option value="DATE">Ngày tháng (DATE)</option>
                                <option value="DROPDOWN">Danh sách chọn (DROPDOWN)</option>
                            </select>

                            <select id="filterRequired" class="cf-filter-select" aria-label="Lọc theo tính bắt buộc">
                                <option value="">Tất cả tính chất</option>
                                <option value="true">Bắt buộc (Required: Có)</option>
                                <option value="false">Tùy chọn (Required: Không)</option>
                            </select>

                            <select id="filterActiveStatus" class="cf-filter-select" aria-label="Lọc theo trạng thái">
                                <option value="">Tất cả trạng thái</option>
                                <option value="true">Đang kích hoạt (Active)</option>
                                <option value="false">Ngừng kích hoạt (Inactive)</option>
                            </select>

                            <button type="button" class="btn btn-secondary btn-sm" id="btnResetFilter" title="Đặt lại bộ lọc tìm kiếm">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="1 4 1 10 7 10"></polyline>
                                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                </svg>
                                Đặt lại
                            </button>
                        </div>

                        <div class="cf-toolbar-right">
                            <button type="button" class="btn btn-primary" id="btnOpenCreateModal">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                <span id="btnCreateText">Thêm trường cho Khách hàng</span>
                            </button>
                        </div>
                    </div>

                    <!-- Bảng dữ liệu trường tuỳ chỉnh (Render bằng JS DOM) -->
                    <div class="cf-table-container">
                        <table class="cf-table" id="cfTable" aria-label="Danh sách trường tuỳ chỉnh">
                            <thead>
                                <tr>
                                    <th style="width: 70px; text-align: center;">Thứ tự</th>
                                    <th style="width: 170px;">Mã hệ thống</th>
                                    <th>Nhãn hiển thị</th>
                                    <th style="width: 130px;">Kiểu dữ liệu</th>
                                    <th style="width: 110px; text-align: center;">Bắt buộc</th>
                                    <th style="width: 240px;">Tùy chọn danh sách</th>
                                    <th style="width: 170px;">Phạm vi hiển thị</th>
                                    <th style="width: 120px; text-align: center;">Trạng thái</th>
                                    <th style="width: 110px; text-align: right;">Hành động</th>
                                </tr>
                            </thead>
                            <tbody id="cfTableBody">
                                <!-- JavaScript render dữ liệu động tại đây -->
                            </tbody>
                        </table>

                        <!-- Empty state khi không có bản ghi nào -->
                        <div class="cf-empty-state" id="cfEmptyState" style="display: none;">
                            <svg class="cf-empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <line x1="9" y1="9" x2="15" y2="15"></line>
                                <line x1="15" y1="9" x2="9" y2="15"></line>
                            </svg>
                            <h3>Chưa có trường tuỳ chỉnh nào</h3>
                            <p>Hãy thêm trường tuỳ chỉnh mới để mở rộng thuộc tính cho đối tượng này.</p>
                            <button type="button" class="btn btn-primary btn-sm" id="btnEmptyCreate">
                                Thêm trường đầu tiên
                            </button>
                        </div>
                    </div>
                </section>

                <!-- KHU VỰC 2: MÔ PHỎNG HIỂN THỊ THỰC TẾ (LIVE PREVIEW SIMULATOR - AC 3) -->
                <section class="cf-preview-wrapper" id="cfPreviewSection" aria-label="Mô phỏng hiển thị thực tế trường tuỳ chỉnh">
                    <div class="cf-preview-header">
                        <div class="cf-preview-title">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                            <span>Mô phỏng hiển thị thực tế (AC 3: Form - Filter - Excel Export)</span>
                        </div>
                        <div class="cf-preview-tabs">
                            <button type="button" class="cf-preview-tab-btn active" id="simTabForm" data-mode="form">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                    <polyline points="14 2 14 8 20 8"></polyline>
                                </svg>
                                1. Biểu mẫu nhập liệu (Form)
                            </button>
                            <button type="button" class="cf-preview-tab-btn" id="simTabFilter" data-mode="filter">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
                                </svg>
                                2. Khung bộ lọc tìm kiếm (Filter)
                            </button>
                            <button type="button" class="cf-preview-tab-btn" id="simTabExcel" data-mode="excel">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                    <line x1="8" y1="13" x2="16" y2="13"></line>
                                    <line x1="8" y1="17" x2="16" y2="17"></line>
                                </svg>
                                3. Bảng xuất Excel (Export)
                            </button>
                        </div>
                    </div>

                    <div class="cf-preview-body">
                        <!-- Mode 1: Biểu mẫu Form Simulator -->
                        <div id="simBodyForm" style="display: block;">
                            <div style="margin-bottom: 16px; padding: 12px 16px; background: #eff6ff; border-radius: 8px; font-size: 0.875rem; color: #1e40af;">
                                💡 <strong>Giao diện mô phỏng Form:</strong> Minh họa cách các trường tuỳ chỉnh đang kích hoạt xuất hiện liền mạch bên dưới các trường chuẩn của đối tượng <strong id="simEntityName">Khách hàng</strong>.
                            </div>
                            <div class="cf-sim-form-grid" id="simFormFieldsGrid">
                                <!-- Rendered dynamically by JS -->
                            </div>
                        </div>

                        <!-- Mode 2: Bộ lọc Filter Simulator -->
                        <div id="simBodyFilter" style="display: none;">
                            <div style="margin-bottom: 16px; padding: 12px 16px; background: #eff6ff; border-radius: 8px; font-size: 0.875rem; color: #1e40af;">
                                🔍 <strong>Giao diện mô phỏng Bộ lọc:</strong> Cho phép nhân viên kinh doanh lọc nhanh danh sách bản ghi theo từng giá trị của trường tuỳ chỉnh (tìm văn bản, khoảng số, khoảng ngày hoặc chọn option dropdown).
                            </div>
                            <div class="cf-sim-filter-box" id="simFilterFieldsBox">
                                <!-- Rendered dynamically by JS -->
                            </div>
                        </div>

                        <!-- Mode 3: Bản xuất Excel Sheet Simulator -->
                        <div id="simBodyExcel" style="display: none;">
                            <div style="margin-bottom: 16px; padding: 12px 16px; background: #ecfdf5; border-radius: 8px; font-size: 0.875rem; color: #065f46;">
                                📊 <strong>Giao diện mô phỏng Bản xuất Excel:</strong> Các cột trường tuỳ chỉnh (màu xanh lá) được tích hợp tự động vào cuối bảng xuất dữ liệu Excel để nhân viên tiếp tục đối chiếu với báo cáo tổng hợp.
                            </div>
                            <div class="cf-sim-excel-sheet">
                                <div class="cf-sim-excel-header">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                    </svg>
                                    <span id="simExcelTitle">Danh_sach_Khach_hang_CustomFields.xlsx</span>
                                </div>
                                <div style="overflow-x: auto;">
                                    <table class="cf-sim-excel-table" id="simExcelTable">
                                        <!-- Rendered dynamically by JS -->
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

            </div>
        </main>
    </div>

    <!-- MODAL 1: TẠO MỚI / CHỈNH SỬA TRƯỜNG TUỲ CHỈNH -->
    <div class="cf-modal-backdrop" id="cfFormModal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
        <div class="cf-modal-card">
            <div class="cf-modal-header">
                <h2 class="cf-modal-title" id="modalTitle">Thêm trường tuỳ chỉnh</h2>
                <button type="button" class="cf-modal-close" id="btnCloseModal" aria-label="Đóng">&times;</button>
            </div>

            <form id="cfFieldForm" novalidate>
                <input type="hidden" id="formFieldId" value="">

                <div class="cf-modal-body">
                    <!-- Đối tượng áp dụng (AC 4) -->
                    <div class="cf-form-group">
                        <label class="cf-form-label" for="formEntityType">
                            Đối tượng áp dụng <span class="required-indicator">*</span>
                        </label>
                        <select id="formEntityType" class="cf-form-select" required>
                            <option value="CUSTOMER">Khách hàng (CUSTOMER)</option>
                            <option value="OPPORTUNITY">Cơ hội bán hàng (OPPORTUNITY)</option>
                        </select>
                        <span class="cf-form-help">Chọn đối tượng mà trường này sẽ xuất hiện trong biểu mẫu nhập liệu.</span>
                    </div>

                    <div class="cf-form-row">
                        <!-- Nhãn hiển thị -->
                        <div class="cf-form-group">
                            <label class="cf-form-label" for="formFieldLabel">
                                Nhãn hiển thị <span class="required-indicator">*</span>
                            </label>
                            <input type="text" id="formFieldLabel" class="cf-form-input"
                                   placeholder="Ví dụ: Mã số thuế, Phân hạng VIP..."
                                   required maxlength="100">
                            <span class="cf-form-help">Tên thân thiện xuất hiện trên nhãn Form và tiêu đề cột Excel.</span>
                        </div>

                        <!-- Mã hệ thống / Field Name -->
                        <div class="cf-form-group">
                            <label class="cf-form-label" for="formFieldName">
                                Mã hệ thống <span class="required-indicator">*</span>
                            </label>
                            <input type="text" id="formFieldName" class="cf-form-input"
                                   placeholder="Ví dụ: tax_code, vip_tier..."
                                   required maxlength="50" pattern="^[a-z][a-z0-9_]*$">
                            <span class="cf-form-help">Chữ thường, số, dấu gạch dưới (tự động gợi ý từ nhãn).</span>
                        </div>
                    </div>

                    <!-- Kiểu dữ liệu (AC 1) -->
                    <div class="cf-form-group">
                        <label class="cf-form-label" for="formFieldType">
                            Kiểu dữ liệu <span class="required-indicator">*</span>
                        </label>
                        <select id="formFieldType" class="cf-form-select" required>
                            <option value="TEXT">Văn bản (TEXT) - Nhập chuỗi ký tự tự do</option>
                            <option value="NUMBER">Số (NUMBER) - Nhập số nguyên hoặc số thực</option>
                            <option value="DATE">Ngày tháng (DATE) - Chọn ngày/tháng/năm</option>
                            <option value="DROPDOWN">Danh sách chọn (DROPDOWN) - Chọn từ danh sách định sẵn</option>
                        </select>
                        <span class="cf-form-help">Kiểu dữ liệu quyết định định dạng nhập liệu và xác thực của trường.</span>
                    </div>

                    <!-- Khu vực cấu hình Options cho DROPDOWN (AC 1) -->
                    <div class="cf-options-builder" id="cfOptionsBuilderArea">
                        <label class="cf-form-label">
                            Các giá trị của danh sách chọn (Options) <span class="required-indicator">*</span>
                        </label>
                        <div class="cf-opt-input-row">
                            <input type="text" id="newOptionInput" class="cf-form-input"
                                   placeholder="Nhập giá trị lựa chọn (ví dụ: Khách VIP) rồi ấn Thêm..."
                                   style="flex: 1;">
                            <button type="button" class="btn btn-secondary btn-sm" id="btnAddOption">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                Thêm
                            </button>
                        </div>
                        <div class="cf-opt-chips-list" id="optChipsContainer">
                            <!-- Các tag options render tại đây -->
                        </div>
                        <span class="cf-form-help" style="margin-top: 6px; display: block;">
                            Nhập ít nhất 2 giá trị. Nhấn Enter để thêm nhanh giá trị tiếp theo.
                        </span>
                    </div>

                    <!-- Thuộc tính Bắt buộc (AC 2) -->
                    <div class="cf-form-group">
                        <label class="cf-switch-wrap" for="formIsRequired">
                            <input type="checkbox" id="formIsRequired" class="cf-switch-input">
                            <span class="cf-switch"></span>
                            <div class="cf-switch-label-group">
                                <span class="cf-switch-title">Bắt buộc nhập dữ liệu (Required)</span>
                                <span class="cf-switch-desc">Nếu bật, người dùng không thể lưu bản ghi nếu để trống trường này.</span>
                            </div>
                        </label>
                    </div>

                    <!-- Phạm vi hiển thị (AC 3: Form, Filter, Excel Export) -->
                    <div class="cf-form-group">
                        <label class="cf-form-label">Phạm vi xuất hiện linh hoạt (AC 3)</label>
                        <div class="cf-checkbox-group">
                            <label class="cf-checkbox-label">
                                <input type="checkbox" id="formInForm" class="cf-checkbox-input" checked>
                                <span>Biểu mẫu nhập liệu (Form)</span>
                            </label>
                            <label class="cf-checkbox-label">
                                <input type="checkbox" id="formInFilter" class="cf-checkbox-input" checked>
                                <span>Bộ lọc tìm kiếm (Filter)</span>
                            </label>
                            <label class="cf-checkbox-label">
                                <input type="checkbox" id="formInExport" class="cf-checkbox-input" checked>
                                <span>Bản xuất Excel (Export)</span>
                            </label>
                        </div>
                    </div>

                    <!-- Trạng thái kích hoạt -->
                    <div class="cf-form-group" style="margin-bottom: 0;">
                        <label class="cf-switch-wrap" for="formIsActive">
                            <input type="checkbox" id="formIsActive" class="cf-switch-input" checked>
                            <span class="cf-switch"></span>
                            <div class="cf-switch-label-group">
                                <span class="cf-switch-title">Kích hoạt sử dụng trường</span>
                                <span class="cf-switch-desc">Tạm ẩn trường trên hệ thống mà không làm mất dữ liệu đã nhập.</span>
                            </div>
                        </label>
                    </div>

                </div>

                <div class="cf-modal-footer">
                    <button type="button" class="btn btn-secondary" id="btnCancelModal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSubmitForm">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        Lưu trường tuỳ chỉnh
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 2: CẢNH BÁO CHẶN XÓA & CHUYỂN SANG INACTIVE KHI ĐÃ CÓ DỮ LIỆU NHẬP -->
    <div class="cf-modal-backdrop" id="cfInUseNoticeModal" role="dialog" aria-modal="true" aria-labelledby="inUseModalTitle">
        <div class="cf-modal-card cf-inuse-modal-card">
            <div class="cf-modal-body" style="padding: 24px; text-align: center;">
                <div class="cf-inuse-icon-wrap" style="margin: 0 auto 16px;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                </div>

                <h3 class="cf-modal-title" id="inUseModalTitle" style="margin-bottom: 10px;">Không thể xóa trường đang có dữ liệu</h3>
                <p id="inUseModalContent" style="font-size: 0.875rem; color: #475569; line-height: 1.6; margin-bottom: 20px;">
                    <!-- Nội dung cảnh báo động -->
                </p>

                <div style="display: flex; justify-content: center; gap: 12px;">
                    <button type="button" class="btn btn-secondary" id="btnCancelInUse">Hủy thao tác</button>
                    <button type="button" class="btn btn-danger" id="btnConfirmDeactivate">
                        Chuyển sang Ngừng kích hoạt (Inactive)
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- JavaScript thuần xử lý State, DOM Rendering, Modal & API Fallback -->
    <script>
    (function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}';

        // 1. Dữ liệu mẫu chuẩn nghiệp vụ cho Khách hàng và Cơ hội (Local fallback state)
        var DEFAULT_CUSTOM_FIELDS = {
            'CUSTOMER': [
                {
                    id: 1,
                    entityType: 'CUSTOMER',
                    fieldName: 'tax_code',
                    fieldLabel: 'Mã số thuế doanh nghiệp',
                    fieldType: 'TEXT',
                    isRequired: false,
                    options: [],
                    sortOrder: 1,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 42
                },
                {
                    id: 2,
                    entityType: 'CUSTOMER',
                    fieldName: 'annual_revenue',
                    fieldLabel: 'Doanh thu năm gần nhất (VNĐ)',
                    fieldType: 'NUMBER',
                    isRequired: false,
                    options: [],
                    sortOrder: 2,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 28
                },
                {
                    id: 3,
                    entityType: 'CUSTOMER',
                    fieldName: 'incorporation_date',
                    fieldLabel: 'Ngày thành lập / Cấp GPKD',
                    fieldType: 'DATE',
                    isRequired: false,
                    options: [],
                    sortOrder: 3,
                    active: true,
                    inForm: true,
                    inFilter: false,
                    inExport: true,
                    usageCount: 19
                },
                {
                    id: 4,
                    entityType: 'CUSTOMER',
                    fieldName: 'customer_tier',
                    fieldLabel: 'Phân hạng đối tác VIP',
                    fieldType: 'DROPDOWN',
                    isRequired: true,
                    options: ['Hạng Kim Cương', 'Hạng Vàng', 'Hạng Bạc', 'Khách hàng Thân thiết', 'Khách hàng Tiêu chuẩn'],
                    sortOrder: 4,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 56
                },
                {
                    id: 5,
                    entityType: 'CUSTOMER',
                    fieldName: 'delivery_region',
                    fieldLabel: 'Khu vực giao hàng chính',
                    fieldType: 'DROPDOWN',
                    options: ['Miền Bắc', 'Miền Trung', 'Miền Nam', 'Khu vực Tây Nguyên', 'Xuất khẩu Quốc tế'],
                    sortOrder: 5,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 35
                }
            ],
            'OPPORTUNITY': [
                {
                    id: 101,
                    entityType: 'OPPORTUNITY',
                    fieldName: 'primary_competitor',
                    fieldLabel: 'Đối thủ cạnh tranh trực tiếp',
                    fieldType: 'TEXT',
                    isRequired: false,
                    options: [],
                    sortOrder: 1,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 24
                },
                {
                    id: 102,
                    entityType: 'OPPORTUNITY',
                    fieldName: 'expected_margin_pct',
                    fieldLabel: 'Tỷ suất lợi nhuận kỳ vọng (%)',
                    fieldType: 'NUMBER',
                    isRequired: false,
                    options: [],
                    sortOrder: 2,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 18
                },
                {
                    id: 103,
                    entityType: 'OPPORTUNITY',
                    fieldName: 'bidding_deadline',
                    fieldLabel: 'Hạn chót mở thầu / Ký hợp đồng',
                    fieldType: 'DATE',
                    isRequired: true,
                    options: [],
                    sortOrder: 3,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 31
                },
                {
                    id: 104,
                    entityType: 'OPPORTUNITY',
                    fieldName: 'budget_status',
                    fieldLabel: 'Tình trạng phê duyệt ngân sách',
                    fieldType: 'DROPDOWN',
                    options: ['Đã duyệt 100%', 'Đang trình duyệt HĐQT', 'Chưa có ngân sách chính thức', 'Ngân sách bổ sung'],
                    sortOrder: 4,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 45
                },
                {
                    id: 105,
                    entityType: 'OPPORTUNITY',
                    fieldName: 'decision_maker_role',
                    fieldLabel: 'Vai trò người quyết định chính',
                    fieldType: 'DROPDOWN',
                    options: ['Tổng Giám đốc (CEO)', 'Giám đốc Công nghệ (CIO/CTO)', 'Giám đốc Tài chính (CFO)', 'Trưởng phòng Thu mua'],
                    sortOrder: 5,
                    active: true,
                    inForm: true,
                    inFilter: true,
                    inExport: true,
                    usageCount: 33
                }
            ]
        };

        // State quản lý của ứng dụng
        var state = {
            currentEntity: 'CUSTOMER',     // 'CUSTOMER' | 'OPPORTUNITY'
            currentView: 'table',          // 'table' | 'simulator'
            simulatorMode: 'form',         // 'form' | 'filter' | 'excel'
            data: {
                CUSTOMER: JSON.parse(JSON.stringify(DEFAULT_CUSTOM_FIELDS.CUSTOMER)),
                OPPORTUNITY: JSON.parse(JSON.stringify(DEFAULT_CUSTOM_FIELDS.OPPORTUNITY))
            },
            filters: {
                keyword: '',
                type: '',
                required: '',
                status: ''
            },
            currentEditingId: null,
            pendingOptions: [],
            pendingDeactivateItem: null,
            isBackendConnected: false
        };

        // DOM Elements
        var tabBtnCustomer = document.getElementById('tabBtnCustomer');
        var tabBtnOpportunity = document.getElementById('tabBtnOpportunity');
        var badgeCustomerCount = document.getElementById('badgeCustomerCount');
        var badgeOpportunityCount = document.getElementById('badgeOpportunityCount');

        var btnViewTable = document.getElementById('btnViewTable');
        var btnViewSimulator = document.getElementById('btnViewSimulator');
        var cfTableSection = document.getElementById('cfTableSection');
        var cfPreviewSection = document.getElementById('cfPreviewSection');

        var simTabForm = document.getElementById('simTabForm');
        var simTabFilter = document.getElementById('simTabFilter');
        var simTabExcel = document.getElementById('simTabExcel');
        var simBodyForm = document.getElementById('simBodyForm');
        var simBodyFilter = document.getElementById('simBodyFilter');
        var simBodyExcel = document.getElementById('simBodyExcel');
        var simEntityName = document.getElementById('simEntityName');
        var simExcelTitle = document.getElementById('simExcelTitle');
        var simFormFieldsGrid = document.getElementById('simFormFieldsGrid');
        var simFilterFieldsBox = document.getElementById('simFilterFieldsBox');
        var simExcelTable = document.getElementById('simExcelTable');

        var cfTableBody = document.getElementById('cfTableBody');
        var cfEmptyState = document.getElementById('cfEmptyState');
        var cfLoadingOverlay = document.getElementById('cfLoadingOverlay');

        var fieldSearchInput = document.getElementById('fieldSearchInput');
        var filterFieldType = document.getElementById('filterFieldType');
        var filterRequired = document.getElementById('filterRequired');
        var filterActiveStatus = document.getElementById('filterActiveStatus');
        var btnResetFilter = document.getElementById('btnResetFilter');

        var btnOpenCreateModal = document.getElementById('btnOpenCreateModal');
        var btnEmptyCreate = document.getElementById('btnEmptyCreate');
        var btnCreateText = document.getElementById('btnCreateText');

        // Modal Elements
        var cfFormModal = document.getElementById('cfFormModal');
        var modalTitle = document.getElementById('modalTitle');
        var btnCloseModal = document.getElementById('btnCloseModal');
        var btnCancelModal = document.getElementById('btnCancelModal');
        var cfFieldForm = document.getElementById('cfFieldForm');
        var formFieldId = document.getElementById('formFieldId');
        var formEntityType = document.getElementById('formEntityType');
        var formFieldLabel = document.getElementById('formFieldLabel');
        var formFieldName = document.getElementById('formFieldName');
        var formFieldType = document.getElementById('formFieldType');
        var cfOptionsBuilderArea = document.getElementById('cfOptionsBuilderArea');
        var newOptionInput = document.getElementById('newOptionInput');
        var btnAddOption = document.getElementById('btnAddOption');
        var optChipsContainer = document.getElementById('optChipsContainer');
        var formIsRequired = document.getElementById('formIsRequired');
        var formInForm = document.getElementById('formInForm');
        var formInFilter = document.getElementById('formInFilter');
        var formInExport = document.getElementById('formInExport');
        var formIsActive = document.getElementById('formIsActive');

        // In-Use Delete Modal Elements
        var cfInUseNoticeModal = document.getElementById('cfInUseNoticeModal');
        var inUseModalContent = document.getElementById('inUseModalContent');
        var btnCancelInUse = document.getElementById('btnCancelInUse');
        var btnConfirmDeactivate = document.getElementById('btnConfirmDeactivate');

        // Stats Elements
        var statTotalFields = document.getElementById('statTotalFields');
        var statRequiredFields = document.getElementById('statRequiredFields');
        var statDropdownFields = document.getElementById('statDropdownFields');
        var statActiveFields = document.getElementById('statActiveFields');

        // Alerts Elements
        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessTitle = document.getElementById('globalSuccessTitle');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');

        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorTitle = document.getElementById('globalErrorTitle');
        var globalErrorMessage = document.getElementById('globalErrorMessage');

        var globalInfoAlert = document.getElementById('globalInfoAlert');
        var globalInfoTitle = document.getElementById('globalInfoTitle');
        var globalInfoMessage = document.getElementById('globalInfoMessage');

        function showSuccessAlert(msg, title) {
            globalSuccessTitle.textContent = title || 'Thành công';
            globalSuccessMessage.textContent = msg;
            globalSuccessAlert.style.display = 'flex';
            globalErrorAlert.style.display = 'none';
            setTimeout(function() { globalSuccessAlert.style.display = 'none'; }, 4500);
        }

        function showErrorAlert(msg, title) {
            globalErrorTitle.textContent = title || 'Đã xảy ra lỗi';
            globalErrorMessage.textContent = msg;
            globalErrorAlert.style.display = 'flex';
            globalSuccessAlert.style.display = 'none';
        }

        function showInfoAlert(msg, title) {
            globalInfoTitle.textContent = title || 'Thông tin hệ thống';
            globalInfoMessage.textContent = msg;
            globalInfoAlert.style.display = 'flex';
            setTimeout(function() { globalInfoAlert.style.display = 'none'; }, 6000);
        }

        function escapeHtml(text) {
            if (text == null) return '';
            return String(text)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        }

        function slugifyFieldName(text) {
            if (!text) return '';
            var str = text.toLowerCase().trim();
            // Xóa dấu tiếng Việt
            str = str.replace(/[àáạảãâầấậẩẫăằắặẳẵ]/g, 'a')
                     .replace(/[èéẹẻẽêềếệểễ]/g, 'e')
                     .replace(/[ìíịỉĩ]/g, 'i')
                     .replace(/[òóọỏõôồốộổỗơờớợởỡ]/g, 'o')
                     .replace(/[ùúụủũưừứựửữ]/g, 'u')
                     .replace(/[ỳýỵỷỹ]/g, 'y')
                     .replace(/đ/g, 'd');
            // Thay ký tự đặc biệt bằng gạch dưới
            str = str.replace(/[^a-z0-9]/g, '_')
                     .replace(/_+/g, '_')
                     .replace(/^_+|_+$/g, '');
            return str;
        }

        // ======================================================================
        // 2. Fetch API & Đồng bộ dữ liệu với Backend
        // ======================================================================
        async function fetchCustomFields(entity) {
            cfLoadingOverlay.style.display = 'flex';
            var endpoint = contextPath + '/api/custom-fields?entity=' + encodeURIComponent(entity);

            try {
                var res = await fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });

                if (res.ok) {
                    var responseData = await res.json();
                    var items = [];
                    if (Array.isArray(responseData)) {
                        items = responseData;
                    } else if (responseData && Array.isArray(responseData.data)) {
                        items = responseData.data;
                    }

                    if (items.length > 0) {
                        state.data[entity] = items.map(function(item, idx) {
                            return normalizeFieldItem(item, entity, idx + 1);
                        });
                        state.isBackendConnected = true;
                    }
                } else {
                    console.info('Backend API chưa sẵn sàng hoặc trả về lỗi ' + res.status + '. Sử dụng Local State để kiểm thử giao diện.');
                }
            } catch (err) {
                console.info('Không thể kết nối Backend API. Sử dụng dữ liệu cục bộ (Local State):', err);
            } finally {
                cfLoadingOverlay.style.display = 'none';
                updateStatsAndBadges();
                renderTable();
                renderSimulator();
            }
        }

        function normalizeFieldItem(item, entity, defaultOrder) {
            var optionsArr = [];
            if (Array.isArray(item.options)) {
                optionsArr = item.options;
            } else if (typeof item.options === 'string' && item.options.trim() !== '') {
                try {
                    optionsArr = JSON.parse(item.options);
                } catch (e) {
                    optionsArr = item.options.split(',').map(function(s) { return s.trim(); });
                }
            }

            return {
                id: item.id != null ? Number(item.id) : Date.now(),
                entityType: item.entityType || entity,
                fieldName: item.fieldName || ('field_' + defaultOrder),
                fieldLabel: item.fieldLabel || item.name || 'Trường ' + defaultOrder,
                fieldType: item.fieldType || 'TEXT',
                isRequired: item.isRequired === true || item.required === true,
                options: optionsArr,
                sortOrder: item.sortOrder != null ? Number(item.sortOrder) : defaultOrder,
                active: item.active !== false,
                inForm: item.inForm !== false,
                inFilter: item.inFilter !== false,
                inExport: item.inExport !== false,
                usageCount: item.usageCount != null ? Number(item.usageCount) : 0
            };
        }

        // ======================================================================
        // 3. Render Bảng danh sách trường tuỳ chỉnh (AC 1, AC 2, AC 3, AC 4)
        // ======================================================================
        function getCurrentList() {
            var list = state.data[state.currentEntity] || [];
            return list.slice().sort(function (a, b) {
                return (a.sortOrder || 0) - (b.sortOrder || 0);
            });
        }

        function getFilteredList() {
            var list = getCurrentList();
            var kw = state.filters.keyword.trim().toLowerCase();
            var type = state.filters.type;
            var req = state.filters.required;
            var status = state.filters.status;

            return list.filter(function (it) {
                if (kw) {
                    var matchName = (it.fieldName || '').toLowerCase().indexOf(kw) !== -1;
                    var matchLabel = (it.fieldLabel || '').toLowerCase().indexOf(kw) !== -1;
                    if (!matchName && !matchLabel) return false;
                }
                if (type && it.fieldType !== type) return false;
                if (req !== '') {
                    var isReq = (req === 'true');
                    if (it.isRequired !== isReq) return false;
                }
                if (status !== '') {
                    var isActive = (status === 'true');
                    if (it.active !== isActive) return false;
                }
                return true;
            });
        }

        function renderTable() {
            var items = getFilteredList();
            var fullList = getCurrentList();
            cfTableBody.innerHTML = '';

            if (items.length === 0) {
                cfEmptyState.style.display = 'block';
                return;
            }
            cfEmptyState.style.display = 'none';

            var fragment = document.createDocumentFragment();

            items.forEach(function (field, index) {
                var tr = document.createElement('tr');
                if (!field.active) {
                    tr.classList.add('cf-row-inactive');
                }

                // 1. Thứ tự & Buttons Di chuyển Lên/Xuống
                var tdOrder = document.createElement('td');
                tdOrder.style.textAlign = 'center';
                var orderWrap = document.createElement('div');
                orderWrap.className = 'cf-order-group';
                orderWrap.style.justifyContent = 'center';

                var btnUp = document.createElement('button');
                btnUp.type = 'button';
                btnUp.className = 'cf-order-btn';
                btnUp.title = 'Di chuyển lên';
                btnUp.setAttribute('data-action', 'move-up');
                btnUp.setAttribute('data-id', field.id);
                btnUp.innerHTML = '&#9650;';
                if (index === 0) btnUp.disabled = true;

                var spanOrder = document.createElement('span');
                spanOrder.style.fontWeight = '600';
                spanOrder.style.fontSize = '0.8125rem';
                spanOrder.style.minWidth = '18px';
                spanOrder.textContent = field.sortOrder || (index + 1);

                var btnDown = document.createElement('button');
                btnDown.type = 'button';
                btnDown.className = 'cf-order-btn';
                btnDown.title = 'Di chuyển xuống';
                btnDown.setAttribute('data-action', 'move-down');
                btnDown.setAttribute('data-id', field.id);
                btnDown.innerHTML = '&#9660;';
                if (index === items.length - 1) btnDown.disabled = true;

                orderWrap.appendChild(btnUp);
                orderWrap.appendChild(spanOrder);
                orderWrap.appendChild(btnDown);
                tdOrder.appendChild(orderWrap);
                tr.appendChild(tdOrder);

                // 2. Mã hệ thống (fieldName)
                var tdName = document.createElement('td');
                tdName.innerHTML = '<code style="background:#f1f5f9;padding:3px 7px;border-radius:4px;font-size:0.8125rem;color:#0f172a;font-weight:600;">' +
                                   escapeHtml(field.fieldName) + '</code>';
                tr.appendChild(tdName);

                // 3. Nhãn hiển thị (fieldLabel)
                var tdLabel = document.createElement('td');
                tdLabel.innerHTML = '<div style="font-weight:600;color:#0f172a;">' + escapeHtml(field.fieldLabel) + '</div>' +
                                    (field.usageCount ? '<div style="font-size:0.75rem;color:#64748b;margin-top:2px;">Đang lưu ' + field.usageCount + ' bản ghi</div>' : '');
                tr.appendChild(tdLabel);

                // 4. Kiểu dữ liệu (fieldType - AC 1)
                var tdType = document.createElement('td');
                var typeClass = 'type-' + (field.fieldType || 'text').toLowerCase();
                var typeLabel = field.fieldType;
                if (field.fieldType === 'TEXT') typeLabel = 'Văn bản (TEXT)';
                else if (field.fieldType === 'NUMBER') typeLabel = 'Số (NUMBER)';
                else if (field.fieldType === 'DATE') typeLabel = 'Ngày (DATE)';
                else if (field.fieldType === 'DROPDOWN') typeLabel = 'Danh sách (SELECT)';

                tdType.innerHTML = '<span class="cf-type-badge ' + typeClass + '">' + escapeHtml(typeLabel) + '</span>';
                tr.appendChild(tdType);

                // 5. Bắt buộc nhập (isRequired - AC 2)
                var tdReq = document.createElement('td');
                tdReq.style.textAlign = 'center';
                if (field.isRequired) {
                    tdReq.innerHTML = '<span class="cf-required-badge req-yes">Bắt buộc</span>';
                } else {
                    tdReq.innerHTML = '<span class="cf-required-badge req-no">Tùy chọn</span>';
                }
                tr.appendChild(tdReq);

                // 6. Options của Dropdown (AC 1)
                var tdOpts = document.createElement('td');
                if (field.fieldType === 'DROPDOWN' && Array.isArray(field.options) && field.options.length > 0) {
                    var optDiv = document.createElement('div');
                    optDiv.className = 'cf-options-preview';
                    var previewLimit = 3;
                    var slice = field.options.slice(0, previewLimit);
                    slice.forEach(function (opt) {
                        var chip = document.createElement('span');
                        chip.className = 'cf-option-chip';
                        chip.textContent = opt;
                        chip.title = opt;
                        optDiv.appendChild(chip);
                    });
                    if (field.options.length > previewLimit) {
                        var moreSpan = document.createElement('span');
                        moreSpan.className = 'cf-option-more';
                        moreSpan.textContent = '+' + (field.options.length - previewLimit) + ' mục';
                        optDiv.appendChild(moreSpan);
                    }
                    tdOpts.appendChild(optDiv);
                } else {
                    tdOpts.innerHTML = '<span style="color:#94a3b8;font-size:0.8125rem;">—</span>';
                }
                tr.appendChild(tdOpts);

                // 7. Phạm vi hiển thị (AC 3: Form, Filter, Excel)
                var tdScope = document.createElement('td');
                var scopeDiv = document.createElement('div');
                scopeDiv.className = 'cf-scope-pills';

                var pillForm = document.createElement('span');
                pillForm.className = 'cf-scope-pill' + (field.inForm !== false ? ' scope-active' : '');
                pillForm.textContent = 'Form';
                scopeDiv.appendChild(pillForm);

                var pillFilter = document.createElement('span');
                pillFilter.className = 'cf-scope-pill' + (field.inFilter !== false ? ' scope-active' : '');
                pillFilter.textContent = 'Filter';
                scopeDiv.appendChild(pillFilter);

                var pillExcel = document.createElement('span');
                pillExcel.className = 'cf-scope-pill' + (field.inExport !== false ? ' scope-active' : '');
                pillExcel.textContent = 'Excel';
                scopeDiv.appendChild(pillExcel);

                tdScope.appendChild(scopeDiv);
                tr.appendChild(tdScope);

                // 8. Trạng thái Active / Inactive
                var tdStatus = document.createElement('td');
                tdStatus.style.textAlign = 'center';
                if (field.active) {
                    tdStatus.innerHTML = '<span class="cf-status-badge cf-status-active">Đang dùng</span>';
                } else {
                    tdStatus.innerHTML = '<span class="cf-status-badge cf-status-inactive">Ngừng dùng</span>';
                }
                tr.appendChild(tdStatus);

                // 9. Hành động (Edit, Delete, Toggle Active)
                var tdActions = document.createElement('td');
                tdActions.style.textAlign = 'right';
                var actDiv = document.createElement('div');
                actDiv.className = 'cf-actions-group';
                actDiv.style.justifyContent = 'flex-end';

                // Nút Sửa
                var btnEdit = document.createElement('button');
                btnEdit.type = 'button';
                btnEdit.className = 'cf-action-btn btn-edit';
                btnEdit.title = 'Chỉnh sửa cấu hình trường';
                btnEdit.setAttribute('data-action', 'edit');
                btnEdit.setAttribute('data-id', field.id);
                btnEdit.innerHTML = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>';
                actDiv.appendChild(btnEdit);

                // Nút Bật / Tắt Active nhanh
                var btnToggle = document.createElement('button');
                btnToggle.type = 'button';
                btnToggle.className = 'cf-action-btn';
                btnToggle.title = field.active ? 'Tạm ngừng kích hoạt trường' : 'Kích hoạt lại trường';
                btnToggle.setAttribute('data-action', 'toggle-active');
                btnToggle.setAttribute('data-id', field.id);
                btnToggle.innerHTML = field.active
                    ? '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line></svg>'
                    : '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>';
                actDiv.appendChild(btnToggle);

                // Nút Xóa
                var btnDel = document.createElement('button');
                btnDel.type = 'button';
                btnDel.className = 'cf-action-btn btn-del';
                btnDel.title = 'Xóa trường tuỳ chỉnh';
                btnDel.setAttribute('data-action', 'delete');
                btnDel.setAttribute('data-id', field.id);
                btnDel.innerHTML = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>';
                actDiv.appendChild(btnDel);

                tdActions.appendChild(actDiv);
                tr.appendChild(tdActions);

                fragment.appendChild(tr);
            });

            cfTableBody.appendChild(fragment);
        }

        // ======================================================================
        // 4. Render Live Preview Simulator (AC 3)
        // ======================================================================
        function renderSimulator() {
            var entity = state.currentEntity;
            var list = getCurrentList().filter(function(it) { return it.active; });
            var entityTitle = entity === 'CUSTOMER' ? 'Khách hàng' : 'Cơ hội bán hàng';

            simEntityName.textContent = entityTitle;
            simExcelTitle.textContent = 'Danh_sach_' + (entity === 'CUSTOMER' ? 'Khach_hang' : 'Co_hoi') + '_Export_CustomFields.xlsx';

            // 4.1 Mode Form Simulator
            simFormFieldsGrid.innerHTML = '';

            // Thêm vài trường chuẩn làm mẫu ngữ cảnh
            var standardDiv = document.createElement('div');
            standardDiv.className = 'cf-sim-field-group';
            standardDiv.innerHTML = '<label class="cf-sim-label">Tên ' + entityTitle + ' chuẩn <span class="req-star">*</span></label>' +
                                    '<input type="text" class="cf-sim-input" value="' + (entity === 'CUSTOMER' ? 'Tập đoàn Công nghệ Alpha VN' : 'Dự án Nâng cấp ERP Toàn diện') + '" readonly>' +
                                    '<span class="cf-sim-hint">Trường hệ thống mặc định</span>';
            simFormFieldsGrid.appendChild(standardDiv);

            var standardDiv2 = document.createElement('div');
            standardDiv2.className = 'cf-sim-field-group';
            standardDiv2.innerHTML = '<label class="cf-sim-label">Người phụ trách <span class="req-star">*</span></label>' +
                                     '<input type="text" class="cf-sim-input" value="Nguyễn Văn Thắng (Sale Leader)" readonly>' +
                                     '<span class="cf-sim-hint">Trường hệ thống mặc định</span>';
            simFormFieldsGrid.appendChild(standardDiv2);

            // Thêm các custom fields có inForm !== false
            var formFields = list.filter(function(f) { return f.inForm !== false; });
            if (formFields.length === 0) {
                var emptyNotice = document.createElement('div');
                emptyNotice.style.gridColumn = '1 / -1';
                emptyNotice.style.padding = '20px';
                emptyNotice.style.color = '#64748b';
                emptyNotice.style.textAlign = 'center';
                emptyNotice.textContent = 'Không có trường tuỳ chỉnh nào được cấu hình hiển thị trong Form.';
                simFormFieldsGrid.appendChild(emptyNotice);
            } else {
                formFields.forEach(function(f) {
                    var fGroup = document.createElement('div');
                    fGroup.className = 'cf-sim-field-group';

                    var labelHtml = '<label class="cf-sim-label" for="sim_input_' + f.id + '">' +
                                    escapeHtml(f.fieldLabel) +
                                    (f.isRequired ? ' <span class="req-star">*</span>' : '') +
                                    '<span class="custom-tag">Trường tuỳ chỉnh</span>' +
                                    '</label>';

                    var inputHtml = '';
                    if (f.fieldType === 'DROPDOWN') {
                        inputHtml = '<select class="cf-sim-select" id="sim_input_' + f.id + '">' +
                                    '<option value="">-- Chọn ' + escapeHtml(f.fieldLabel) + ' --</option>';
                        if (Array.isArray(f.options)) {
                            f.options.forEach(function(opt) {
                                inputHtml += '<option value="' + escapeHtml(opt) + '">' + escapeHtml(opt) + '</option>';
                            });
                        }
                        inputHtml += '</select>';
                    } else if (f.fieldType === 'NUMBER') {
                        inputHtml = '<input type="number" class="cf-sim-input" id="sim_input_' + f.id + '" placeholder="Nhập số giá trị ' + escapeHtml(f.fieldLabel) + '...">';
                    } else if (f.fieldType === 'DATE') {
                        inputHtml = '<input type="date" class="cf-sim-input" id="sim_input_' + f.id + '">';
                    } else {
                        inputHtml = '<input type="text" class="cf-sim-input" id="sim_input_' + f.id + '" placeholder="Nhập ' + escapeHtml(f.fieldLabel) + '...">';
                    }

                    var hintHtml = '<span class="cf-sim-hint">Mã trường: ' + escapeHtml(f.fieldName) + ' | Kiểu: ' + f.fieldType + '</span>';

                    fGroup.innerHTML = labelHtml + inputHtml + hintHtml;
                    simFormFieldsGrid.appendChild(fGroup);
                });
            }

            // 4.2 Mode Filter Simulator
            simFilterFieldsBox.innerHTML = '';
            var filterFields = list.filter(function(f) { return f.inFilter !== false; });
            if (filterFields.length === 0) {
                simFilterFieldsBox.innerHTML = '<span style="color:#64748b;font-size:0.875rem;">Không có trường tuỳ chỉnh nào được đưa vào Bộ lọc tìm kiếm.</span>';
            } else {
                filterFields.forEach(function(f) {
                    var wrap = document.createElement('div');
                    wrap.style.display = 'flex';
                    wrap.style.flexDirection = 'column';
                    wrap.style.gap = '4px';

                    var lbl = document.createElement('span');
                    lbl.style.fontSize = '0.75rem';
                    lbl.style.fontWeight = '600';
                    lbl.style.color = '#475569';
                    lbl.textContent = f.fieldLabel;
                    wrap.appendChild(lbl);

                    if (f.fieldType === 'DROPDOWN') {
                        var sel = document.createElement('select');
                        sel.className = 'cf-filter-select';
                        sel.style.fontSize = '0.8125rem';
                        sel.innerHTML = '<option value="">Tất cả ' + escapeHtml(f.fieldLabel) + '</option>';
                        if (Array.isArray(f.options)) {
                            f.options.forEach(function(opt) {
                                sel.innerHTML += '<option value="' + escapeHtml(opt) + '">' + escapeHtml(opt) + '</option>';
                            });
                        }
                        wrap.appendChild(sel);
                    } else if (f.fieldType === 'DATE') {
                        var dateInp = document.createElement('input');
                        dateInp.type = 'date';
                        dateInp.className = 'cf-search-input';
                        dateInp.style.width = '160px';
                        dateInp.style.padding = '6px 10px';
                        wrap.appendChild(dateInp);
                    } else if (f.fieldType === 'NUMBER') {
                        var numInp = document.createElement('input');
                        numInp.type = 'number';
                        numInp.className = 'cf-search-input';
                        numInp.placeholder = 'Lọc theo ' + f.fieldLabel;
                        numInp.style.width = '170px';
                        numInp.style.padding = '6px 10px';
                        wrap.appendChild(numInp);
                    } else {
                        var textInp = document.createElement('input');
                        textInp.type = 'text';
                        textInp.className = 'cf-search-input';
                        textInp.placeholder = 'Tìm ' + f.fieldLabel + '...';
                        textInp.style.width = '180px';
                        textInp.style.padding = '6px 10px';
                        wrap.appendChild(textInp);
                    }
                    simFilterFieldsBox.appendChild(wrap);
                });
            }

            // 4.3 Mode Excel Table Simulator
            simExcelTable.innerHTML = '';
            var excelFields = list.filter(function(f) { return f.inExport !== false; });

            var thead = document.createElement('thead');
            var trHead = document.createElement('tr');
            trHead.innerHTML = '<th>STT</th>' +
                               '<th>Tên ' + entityTitle + ' (Chuẩn)</th>' +
                               '<th>Người phụ trách (Chuẩn)</th>' +
                               '<th>Ngày tạo (Chuẩn)</th>';

            excelFields.forEach(function(f) {
                var th = document.createElement('th');
                th.className = 'custom-col';
                th.textContent = f.fieldLabel + ' [Custom]';
                th.title = 'Trường tuỳ chỉnh: ' + f.fieldName;
                trHead.appendChild(th);
            });
            thead.appendChild(trHead);
            simExcelTable.appendChild(thead);

            var tbody = document.createElement('tbody');
            var sampleRows = [
                {
                    name: entity === 'CUSTOMER' ? 'Công ty TNHH Vận tải Hoàng Phát' : 'Gói thầu Phần mềm Quản trị Kho Vận',
                    owner: 'Trần Thị Thu Thảo',
                    created: '2026-09-15',
                    customVals: ['0108923412', '15,000,000,000', '2018-05-20', 'Hạng Kim Cương', 'Miền Bắc']
                },
                {
                    name: entity === 'CUSTOMER' ? 'Tập đoàn Cơ khí Bách Khoa' : 'Hợp đồng Nâng cấp Bảo mật Dữ liệu',
                    owner: 'Phạm Đức Tiệp',
                    created: '2026-09-22',
                    customVals: ['0315487920', '8,500,000,000', '2020-11-10', 'Hạng Vàng', 'Miền Nam']
                },
                {
                    name: entity === 'CUSTOMER' ? 'Công ty Dược phẩm Quốc tế Tâm Đức' : 'Triển khai CRM cho Đội ngũ Trình dược viên',
                    owner: 'Nguyễn Văn Thắng',
                    created: '2026-09-28',
                    customVals: ['0401889241', '32,000,000,000', '2015-02-14', 'Hạng Kim Cương', 'Miền Trung']
                }
            ];

            sampleRows.forEach(function(row, idx) {
                var tr = document.createElement('tr');
                var html = '<td>' + (idx + 1) + '</td>' +
                           '<td style="font-weight:600;">' + escapeHtml(row.name) + '</td>' +
                           '<td>' + escapeHtml(row.owner) + '</td>' +
                           '<td>' + escapeHtml(row.created) + '</td>';

                excelFields.forEach(function(f, cIdx) {
                    var val = (row.customVals && row.customVals[cIdx]) ? row.customVals[cIdx] : '—';
                    if (f.fieldType === 'DROPDOWN' && Array.isArray(f.options) && f.options.length > 0) {
                        val = f.options[idx % f.options.length];
                    }
                    html += '<td style="background:#f0fdf4;color:#166534;font-weight:500;">' + escapeHtml(val) + '</td>';
                });
                tr.innerHTML = html;
                tbody.appendChild(tr);
            });
            simExcelTable.appendChild(tbody);
        }

        // ======================================================================
        // 5. Cập nhật Thống kê & Badges
        // ======================================================================
        function updateStatsAndBadges() {
            var custCount = (state.data.CUSTOMER || []).length;
            var oppCount = (state.data.OPPORTUNITY || []).length;

            badgeCustomerCount.textContent = custCount;
            badgeOpportunityCount.textContent = oppCount;

            var currentList = getCurrentList();
            var total = currentList.length;
            var reqCount = currentList.filter(function(it) { return it.isRequired; }).length;
            var dropdownCount = currentList.filter(function(it) { return it.fieldType === 'DROPDOWN'; }).length;
            var activeCount = currentList.filter(function(it) { return it.active; }).length;

            statTotalFields.textContent = total;
            statRequiredFields.textContent = reqCount;
            statDropdownFields.textContent = dropdownCount;
            statActiveFields.textContent = activeCount;

            btnCreateText.textContent = state.currentEntity === 'CUSTOMER'
                ? 'Thêm trường cho Khách hàng'
                : 'Thêm trường cho Cơ hội';
        }

        // ======================================================================
        // 6. Xử lý Chuyển Tab (Khách hàng vs Cơ hội) & Chuyển chế độ xem
        // ======================================================================
        function switchEntity(entity) {
            if (state.currentEntity === entity) return;
            state.currentEntity = entity;

            if (entity === 'CUSTOMER') {
                tabBtnCustomer.classList.add('active');
                tabBtnOpportunity.classList.remove('active');
            } else {
                tabBtnOpportunity.classList.add('active');
                tabBtnCustomer.classList.remove('active');
            }

            fieldSearchInput.value = '';
            filterFieldType.value = '';
            filterRequired.value = '';
            filterActiveStatus.value = '';
            state.filters = { keyword: '', type: '', required: '', status: '' };

            updateStatsAndBadges();
            renderTable();
            renderSimulator();
        }

        tabBtnCustomer.addEventListener('click', function () { switchEntity('CUSTOMER'); });
        tabBtnOpportunity.addEventListener('click', function () { switchEntity('OPPORTUNITY'); });

        btnViewTable.addEventListener('click', function () {
            state.currentView = 'table';
            btnViewTable.classList.add('active');
            btnViewSimulator.classList.remove('active');
            cfTableSection.style.display = 'block';
            cfPreviewSection.style.display = 'none';
        });

        btnViewSimulator.addEventListener('click', function () {
            state.currentView = 'simulator';
            btnViewSimulator.classList.add('active');
            btnViewTable.classList.remove('active');
            cfTableSection.style.display = 'none';
            cfPreviewSection.style.display = 'block';
            renderSimulator();
        });

        // Tabs bên trong Simulator
        function switchSimulatorTab(mode) {
            state.simulatorMode = mode;
            [simTabForm, simTabFilter, simTabExcel].forEach(function(btn) {
                btn.classList.toggle('active', btn.getAttribute('data-mode') === mode);
            });
            simBodyForm.style.display = (mode === 'form' ? 'block' : 'none');
            simBodyFilter.style.display = (mode === 'filter' ? 'block' : 'none');
            simBodyExcel.style.display = (mode === 'excel' ? 'block' : 'none');
        }

        simTabForm.addEventListener('click', function() { switchSimulatorTab('form'); });
        simTabFilter.addEventListener('click', function() { switchSimulatorTab('filter'); });
        simTabExcel.addEventListener('click', function() { switchSimulatorTab('excel'); });

        // ======================================================================
        // 7. Xử lý Bộ lọc tìm kiếm trên Toolbar
        // ======================================================================
        fieldSearchInput.addEventListener('input', function (e) {
            state.filters.keyword = e.target.value;
            renderTable();
        });

        filterFieldType.addEventListener('change', function (e) {
            state.filters.type = e.target.value;
            renderTable();
        });

        filterRequired.addEventListener('change', function (e) {
            state.filters.required = e.target.value;
            renderTable();
        });

        filterActiveStatus.addEventListener('change', function (e) {
            state.filters.status = e.target.value;
            renderTable();
        });

        btnResetFilter.addEventListener('click', function () {
            fieldSearchInput.value = '';
            filterFieldType.value = '';
            filterRequired.value = '';
            filterActiveStatus.value = '';
            state.filters = { keyword: '', type: '', required: '', status: '' };
            renderTable();
        });

        // ======================================================================
        // 8. Quản lý Modal & Options Builder (AC 1, AC 2)
        // ======================================================================
        function openCreateModal() {
            state.currentEditingId = null;
            state.pendingOptions = [];

            modalTitle.textContent = 'Thêm trường tuỳ chỉnh mới';
            formFieldId.value = '';
            formEntityType.value = state.currentEntity;
            formEntityType.disabled = false;
            formFieldLabel.value = '';
            formFieldName.value = '';
            formFieldName.disabled = false;
            formFieldType.value = 'TEXT';
            formFieldType.disabled = false;
            formIsRequired.checked = false;
            formInForm.checked = true;
            formInFilter.checked = true;
            formInExport.checked = true;
            formIsActive.checked = true;

            renderOptionChips();
            toggleOptionsBuilderArea('TEXT');
            cfFormModal.style.display = 'flex';
            formFieldLabel.focus();
        }

        function openEditModal(id) {
            var fullList = getCurrentList();
            var item = fullList.find(function(it) { return it.id === id; });
            if (!item) return;

            state.currentEditingId = id;
            state.pendingOptions = Array.isArray(item.options) ? item.options.slice() : [];

            modalTitle.textContent = 'Chỉnh sửa trường tuỳ chỉnh: ' + item.fieldLabel;
            formFieldId.value = item.id;
            formEntityType.value = item.entityType;
            formEntityType.disabled = true; // Không đổi entity khi đang sửa
            formFieldLabel.value = item.fieldLabel;
            formFieldName.value = item.fieldName;
            formFieldName.disabled = true; // Mã hệ thống giữ nguyên để bảo toàn cấu trúc DB
            formFieldType.value = item.fieldType;
            formFieldType.disabled = (item.usageCount > 0); // Nếu đã có dữ liệu, không đổi kiểu
            formIsRequired.checked = !!item.isRequired;
            formInForm.checked = (item.inForm !== false);
            formInFilter.checked = (item.inFilter !== false);
            formInExport.checked = (item.inExport !== false);
            formIsActive.checked = (item.active !== false);

            renderOptionChips();
            toggleOptionsBuilderArea(item.fieldType);
            cfFormModal.style.display = 'flex';
            formFieldLabel.focus();
        }

        function closeModal() {
            cfFormModal.style.display = 'none';
            state.currentEditingId = null;
            state.pendingOptions = [];
            cfFieldForm.reset();
        }

        btnOpenCreateModal.addEventListener('click', openCreateModal);
        btnEmptyCreate.addEventListener('click', openCreateModal);
        btnCloseModal.addEventListener('click', closeModal);
        btnCancelModal.addEventListener('click', closeModal);

        cfFormModal.addEventListener('click', function(e) {
            if (e.target === cfFormModal) closeModal();
        });

        // Tự động gợi ý mã fieldName khi nhập fieldLabel (nếu chưa gõ thủ công)
        formFieldLabel.addEventListener('input', function() {
            if (!state.currentEditingId && !formFieldName.dataset.userEdited) {
                formFieldName.value = slugifyFieldName(formFieldLabel.value);
            }
        });

        formFieldName.addEventListener('input', function() {
            formFieldName.dataset.userEdited = 'true';
        });

        formFieldType.addEventListener('change', function() {
            toggleOptionsBuilderArea(formFieldType.value);
        });

        function toggleOptionsBuilderArea(type) {
            if (type === 'DROPDOWN') {
                cfOptionsBuilderArea.classList.add('visible');
            } else {
                cfOptionsBuilderArea.classList.remove('visible');
            }
        }

        // Xử lý thêm / xóa Option Chip cho kiểu DROPDOWN
        function renderOptionChips() {
            optChipsContainer.innerHTML = '';
            if (state.pendingOptions.length === 0) {
                optChipsContainer.innerHTML = '<span style="color:#94a3b8;font-size:0.75rem;padding:4px;">Chưa có tùy chọn nào. Hãy nhập giá trị vào ô trên.</span>';
                return;
            }

            state.pendingOptions.forEach(function(opt, idx) {
                var chip = document.createElement('div');
                chip.className = 'cf-opt-chip-item';
                chip.innerHTML = '<span>' + escapeHtml(opt) + '</span>' +
                                 '<button type="button" class="cf-opt-chip-remove" data-index="' + idx + '" title="Xóa tùy chọn này">&times;</button>';
                optChipsContainer.appendChild(chip);
            });
        }

        function addPendingOption() {
            var val = newOptionInput.value.trim();
            if (!val) return;
            if (state.pendingOptions.indexOf(val) !== -1) {
                showErrorAlert('Giá trị tùy chọn [' + val + '] đã tồn tại trong danh sách!');
                newOptionInput.focus();
                return;
            }
            state.pendingOptions.push(val);
            newOptionInput.value = '';
            renderOptionChips();
            newOptionInput.focus();
        }

        btnAddOption.addEventListener('click', addPendingOption);

        newOptionInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                addPendingOption();
            }
        });

        optChipsContainer.addEventListener('click', function(e) {
            var btn = e.target.closest('.cf-opt-chip-remove');
            if (!btn) return;
            var idx = Number(btn.getAttribute('data-index'));
            state.pendingOptions.splice(idx, 1);
            renderOptionChips();
        });

        // ======================================================================
        // 9. Xử lý Lưu Form (POST / PUT)
        // ======================================================================
        cfFieldForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            var id = formFieldId.value;
            var isEdit = !!id;
            var entity = formEntityType.value;
            var label = formFieldLabel.value.trim();
            var name = formFieldName.value.trim();
            var type = formFieldType.value;
            var isRequired = formIsRequired.checked;
            var inForm = formInForm.checked;
            var inFilter = formInFilter.checked;
            var inExport = formInExport.checked;
            var active = formIsActive.checked;

            // Kiểm tra tính hợp lệ dữ liệu
            if (!label) {
                showErrorAlert('Vui lòng nhập Nhãn hiển thị của trường tuỳ chỉnh!');
                formFieldLabel.focus();
                return;
            }

            if (!name) {
                showErrorAlert('Vui lòng nhập Mã hệ thống của trường tuỳ chỉnh!');
                formFieldName.focus();
                return;
            }

            if (!/^[a-z][a-z0-9_]*$/.test(name)) {
                showErrorAlert('Mã hệ thống không hợp lệ! Chỉ dùng chữ cái thường, số và dấu gạch dưới (bắt đầu bằng chữ cái).');
                formFieldName.focus();
                return;
            }

            var list = state.data[entity];

            // Kiểm tra trùng mã fieldName trong cùng entity
            var duplicate = list.find(function(it) {
                return it.fieldName === name && (!isEdit || it.id !== Number(id));
            });
            if (duplicate) {
                showErrorAlert('Mã hệ thống [' + name + '] đã được sử dụng bởi trường "' + duplicate.fieldLabel + '"!');
                formFieldName.focus();
                return;
            }

            // Kiểm tra options nếu là DROPDOWN
            if (type === 'DROPDOWN' && state.pendingOptions.length < 2) {
                showErrorAlert('Kiểu danh sách chọn (DROPDOWN) yêu cầu ít nhất 2 tùy chọn hợp lệ!');
                newOptionInput.focus();
                return;
            }

            var payload = {
                entityType: entity,
                fieldName: name,
                fieldLabel: label,
                fieldType: type,
                isRequired: isRequired,
                options: (type === 'DROPDOWN' ? state.pendingOptions.slice() : []),
                active: active,
                inForm: inForm,
                inFilter: inFilter,
                inExport: inExport
            };

            var endpoint = isEdit
                ? (contextPath + '/api/custom-fields/' + id)
                : (contextPath + '/api/custom-fields');
            var method = isEdit ? 'PUT' : 'POST';
            var savedItem = null;
            var localOnly = false;

            cfLoadingOverlay.style.display = 'flex';

            try {
                var res = await fetch(endpoint, {
                    method: method,
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    try {
                        var resBody = await res.json();
                        savedItem = resBody.data || resBody;
                        state.isBackendConnected = true;
                    } catch (ignored) {}
                } else {
                    localOnly = true;
                    console.info('Backend API chưa sẵn sàng. Lưu thay đổi vào Local State để kiểm thử giao diện.');
                }
            } catch (err) {
                localOnly = true;
                console.info('Không thể kết nối Backend API. Lưu thay đổi vào Local State:', err);
            } finally {
                cfLoadingOverlay.style.display = 'none';
            }

            // Cập nhật State
            if (isEdit) {
                var idx = list.findIndex(function(it) { return it.id === Number(id); });
                if (idx !== -1) {
                    payload.id = Number(id);
                    payload.sortOrder = list[idx].sortOrder;
                    payload.usageCount = list[idx].usageCount;
                    list[idx] = Object.assign({}, list[idx], payload, savedItem || {});
                }
            } else {
                var newId = (savedItem && savedItem.id != null)
                    ? Number(savedItem.id)
                    : (list.length > 0 ? Math.max.apply(null, list.map(function(it) { return it.id; })) + 1 : 1);
                payload.id = newId;
                payload.sortOrder = list.length + 1;
                payload.usageCount = 0;
                list.push(Object.assign({}, payload, savedItem || {}));
            }

            if (localOnly) {
                showInfoAlert('Đã cập nhật trường [' + payload.fieldLabel + '] (Chế độ Demo Local State - Backend API chưa sẵn sàng).');
            } else {
                showSuccessAlert(isEdit
                    ? 'Đã cập nhật cấu hình trường [' + payload.fieldLabel + '] thành công!'
                    : 'Đã thêm trường tuỳ chỉnh mới [' + payload.fieldLabel + '] thành công!');
            }

            closeModal();
            updateStatsAndBadges();
            renderTable();
            renderSimulator();
        });

        // ======================================================================
        // 10. Xử lý Xóa / Ngừng kích hoạt (AC: usageCount > 0 chặn xóa)
        // ======================================================================
        function handleDeleteField(id) {
            var fullList = getCurrentList();
            var item = fullList.find(function(it) { return it.id === id; });
            if (!item) return;

            // AC: Nếu trường đã có dữ liệu nhập (usageCount > 0), CHẶN XÓA CỨNG
            if (item.usageCount && item.usageCount > 0) {
                state.pendingDeactivateItem = item;
                inUseModalContent.innerHTML =
                    'Trường tuỳ chỉnh <strong>[' + escapeHtml(item.fieldLabel) + ']</strong> đang được lưu trữ dữ liệu bởi <strong>' +
                    item.usageCount + ' bản ghi</strong> trong hệ thống. Để bảo vệ an toàn và tính toàn vẹn dữ liệu kế toán/kinh doanh, hệ thống <span style="color:#ef4444;font-weight:700;">không cho phép xóa vĩnh viễn</span>. Bạn có muốn chuyển trường này sang trạng thái <strong>Ngừng kích hoạt (Inactive)</strong> để ẩn khỏi biểu mẫu không?';
                cfInUseNoticeModal.style.display = 'flex';
                return;
            }

            // Nếu usageCount === 0, cho phép xóa
            if (confirm('Bạn có chắc chắn muốn xóa vĩnh viễn trường tuỳ chỉnh [' + item.fieldLabel + '] không?')) {
                executeDirectDelete(id);
            }
        }

        async function executeDirectDelete(id) {
            var endpoint = contextPath + '/api/custom-fields/' + id;
            var localOnly = false;

            cfLoadingOverlay.style.display = 'flex';
            try {
                var res = await fetch(endpoint, {
                    method: 'DELETE',
                    headers: { 'Accept': 'application/json' }
                });

                if (!res.ok) {
                    localOnly = true;
                }
            } catch (err) {
                localOnly = true;
            } finally {
                cfLoadingOverlay.style.display = 'none';
            }

            var list = state.data[state.currentEntity];
            state.data[state.currentEntity] = list.filter(function(it) { return it.id !== id; });

            if (localOnly) {
                showInfoAlert('Đã xóa trường khỏi Local State (Backend API chưa sẵn sàng).');
            } else {
                showSuccessAlert('Đã xóa trường tuỳ chỉnh thành công!');
            }

            updateStatsAndBadges();
            renderTable();
            renderSimulator();
        }

        // Chuyển sang Ngừng kích hoạt khi đang được sử dụng
        btnConfirmDeactivate.addEventListener('click', async function() {
            if (!state.pendingDeactivateItem) return;
            var item = state.pendingDeactivateItem;

            var endpoint = contextPath + '/api/custom-fields/' + item.id;
            try {
                await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify({ active: false })
                });
            } catch (ignored) {}

            item.active = false;
            cfInUseNoticeModal.style.display = 'none';
            state.pendingDeactivateItem = null;

            showSuccessAlert('Đã chuyển trường [' + item.fieldLabel + '] sang trạng thái [Ngừng kích hoạt]!');
            updateStatsAndBadges();
            renderTable();
            renderSimulator();
        });

        btnCancelInUse.addEventListener('click', function() {
            cfInUseNoticeModal.style.display = 'none';
            state.pendingDeactivateItem = null;
        });

        cfInUseNoticeModal.addEventListener('click', function(e) {
            if (e.target === cfInUseNoticeModal) {
                cfInUseNoticeModal.style.display = 'none';
                state.pendingDeactivateItem = null;
            }
        });

        // Bật / Tắt Active nhanh từ bảng
        async function toggleActiveStatus(id) {
            var fullList = getCurrentList();
            var item = fullList.find(function(it) { return it.id === id; });
            if (!item) return;

            item.active = !item.active;

            var endpoint = contextPath + '/api/custom-fields/' + id;
            try {
                await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify({ active: item.active })
                });
            } catch (ignored) {}

            showSuccessAlert('Đã ' + (item.active ? 'kích hoạt lại' : 'ngừng kích hoạt') + ' trường [' + item.fieldLabel + ']!');
            updateStatsAndBadges();
            renderTable();
            renderSimulator();
        }

        // Sắp xếp thứ tự (Move Up / Move Down)
        function moveFieldOrder(id, direction) {
            var list = getCurrentList();
            var idx = list.findIndex(function(it) { return it.id === id; });
            if (idx === -1) return;

            var targetIdx = direction === 'up' ? idx - 1 : idx + 1;
            if (targetIdx < 0 || targetIdx >= list.length) return;

            var currentSort = list[idx].sortOrder;
            var targetSort = list[targetIdx].sortOrder;

            list[idx].sortOrder = targetSort;
            list[targetIdx].sortOrder = currentSort;

            state.data[state.currentEntity] = list;

            renderTable();
            renderSimulator();
        }

        // Bắt sự kiện bảng
        cfTableBody.addEventListener('click', function(e) {
            var btn = e.target.closest('[data-action]');
            if (!btn) return;
            var action = btn.getAttribute('data-action');
            var id = Number(btn.getAttribute('data-id'));

            if (action === 'edit') {
                openEditModal(id);
            } else if (action === 'delete') {
                handleDeleteField(id);
            } else if (action === 'toggle-active') {
                toggleActiveStatus(id);
            } else if (action === 'move-up') {
                moveFieldOrder(id, 'up');
            } else if (action === 'move-down') {
                moveFieldOrder(id, 'down');
            }
        });

        // Khởi động module
        fetchCustomFields('CUSTOMER');

    })();
    </script>
</body>
</html>
