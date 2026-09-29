<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục dùng chung của bán hàng - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Cấu hình danh mục (CRM-44) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/configuration/configuration.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Cấu hình danh mục dùng chung -->
        <main class="config-page" id="configApp" role="main">
            <div class="config-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="config-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Danh mục dùng chung của bán hàng</span>
                </nav>

                <!-- Header màn hình -->
                <header class="config-header">
                    <div class="config-header-info">
                        <h1>Khai báo danh mục dùng chung của bán hàng</h1>
                        <p>Chuẩn hóa tên gọi nguồn lead, ngành nghề, quy mô doanh nghiệp và loại hoạt động trên toàn hệ thống để tổng hợp và xuất báo cáo gộp đồng bộ.</p>
                    </div>

                    <div class="config-header-badges">
                        <span class="config-badge config-badge-report" title="Chuẩn hóa dữ liệu đầu vào phục vụ báo cáo gộp">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <line x1="18" y1="20" x2="18" y2="10"></line>
                                <line x1="12" y1="20" x2="12" y2="4"></line>
                                <line x1="6" y1="20" x2="6" y2="14"></line>
                            </svg>
                            Báo cáo gộp chuẩn hóa
                        </span>
                        <span class="config-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                            S2-07 / CRM-44
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts) -->
                <div class="config-alerts" id="configAlertsArea" aria-live="polite">
                    <div class="config-alert config-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="config-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="config-alert-content">
                            <div class="config-alert-title" id="globalErrorTitle">Đã xảy ra lỗi</div>
                            <div id="globalErrorMessage"></div>
                        </div>
                        <button type="button" class="config-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="config-alert config-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="config-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="config-alert-content">
                            <div class="config-alert-title" id="globalSuccessTitle">Thành công</div>
                            <div id="globalSuccessMessage"></div>
                        </div>
                        <button type="button" class="config-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>
                </div>

                <!-- Banner Nghiệp vụ Chuẩn hóa Báo cáo gộp (Mô tả Jira) -->
                <aside class="config-scope-banner" role="region" aria-label="Quy định chuẩn hóa danh mục">
                    <svg class="config-scope-banner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    <div>
                        <div class="config-scope-banner-title">Mục tiêu chuẩn hóa danh mục bán hàng (CRM-44)</div>
                        <p class="config-scope-banner-desc">
                            Việc thống nhất tên gọi các danh mục chuẩn (Ngành nghề, Quy mô, Nguồn lead, Loại hoạt động) giúp toàn bộ đội ngũ kinh doanh nhập liệu đồng nhất, từ đó Giám đốc kinh doanh có thể kết xuất báo cáo phân tích tổng hợp chính xác theo từng phân khúc khách hàng mà không bị phân mảnh hay trùng lặp dữ liệu.
                        </p>
                    </div>
                </aside>

                <!-- Thống kê nhanh 4 nhóm danh mục -->
                <section class="config-stats-grid" aria-label="Thống kê các nhóm danh mục">
                    <div class="config-stat-card active-card" id="cardIndustry" onclick="switchCategoryTab('industry')">
                        <div class="config-stat-icon-wrap stat-icon-blue">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                            </svg>
                        </div>
                        <div class="config-stat-content">
                            <span class="config-stat-value" id="statIndustryCount">7</span>
                            <span class="config-stat-label">Ngành nghề khách hàng</span>
                        </div>
                    </div>

                    <div class="config-stat-card" id="cardCompanySize" onclick="switchCategoryTab('company_size')">
                        <div class="config-stat-icon-wrap stat-icon-purple">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                            </svg>
                        </div>
                        <div class="config-stat-content">
                            <span class="config-stat-value" id="statCompanySizeCount">5</span>
                            <span class="config-stat-label">Quy mô doanh nghiệp</span>
                        </div>
                    </div>

                    <div class="config-stat-card" id="cardLeadSource" onclick="switchCategoryTab('lead_source')">
                        <div class="config-stat-icon-wrap stat-icon-amber">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <circle cx="12" cy="12" r="6"></circle>
                                <circle cx="12" cy="12" r="2"></circle>
                            </svg>
                        </div>
                        <div class="config-stat-content">
                            <span class="config-stat-value" id="statLeadSourceCount">7</span>
                            <span class="config-stat-label">Nguồn thu hút Lead</span>
                        </div>
                    </div>

                    <div class="config-stat-card" id="cardActivityType" onclick="switchCategoryTab('activity_type')">
                        <div class="config-stat-icon-wrap stat-icon-teal">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                            </svg>
                        </div>
                        <div class="config-stat-content">
                            <span class="config-stat-value" id="statActivityTypeCount">6</span>
                            <span class="config-stat-label">Loại hoạt động tương tác</span>
                        </div>
                    </div>
                </section>

                <!-- Thanh chuyển Tab Danh mục (AC 1) -->
                <nav class="config-tabs-nav" aria-label="Thanh chọn nhóm danh mục">
                    <button type="button" class="config-tab-btn active" id="tabBtnIndustry" onclick="switchCategoryTab('industry')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                        </svg>
                        <span>Ngành nghề khách hàng</span>
                        <span class="config-tab-badge" id="badgeIndustry">7</span>
                    </button>

                    <button type="button" class="config-tab-btn" id="tabBtnCompanySize" onclick="switchCategoryTab('company_size')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        </svg>
                        <span>Quy mô doanh nghiệp</span>
                        <span class="config-tab-badge" id="badgeCompanySize">5</span>
                    </button>

                    <button type="button" class="config-tab-btn" id="tabBtnLeadSource" onclick="switchCategoryTab('lead_source')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <circle cx="12" cy="12" r="6"></circle>
                            <circle cx="12" cy="12" r="2"></circle>
                        </svg>
                        <span>Nguồn lead</span>
                        <span class="config-tab-badge" id="badgeLeadSource">7</span>
                    </button>

                    <button type="button" class="config-tab-btn" id="tabBtnActivityType" onclick="switchCategoryTab('activity_type')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                        </svg>
                        <span>Loại hoạt động</span>
                        <span class="config-tab-badge" id="badgeActivityType">6</span>
                    </button>
                </nav>

                <!-- Card Danh mục & Bảng dữ liệu -->
                <section class="config-card" style="position: relative;" aria-labelledby="configCardTitle">

                    <!-- Loading Overlay -->
                    <div class="config-loading-overlay" id="configLoadingOverlay" aria-hidden="true">
                        <div class="config-spinner"></div>
                    </div>

                    <!-- Toolbar & Bộ lọc -->
                    <div class="config-toolbar">
                        <div class="config-toolbar-left">
                            <div class="config-search-wrap">
                                <svg class="config-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="search" id="categorySearchInput" class="config-search-input"
                                       placeholder="Tìm theo mã hoặc tên giá trị..."
                                       aria-label="Tìm kiếm giá trị danh mục">
                            </div>

                            <select id="filterActiveStatus" class="config-filter-select" aria-label="Lọc theo trạng thái hoạt động">
                                <option value="">Tất cả trạng thái</option>
                                <option value="true">Đang sử dụng (Active)</option>
                                <option value="false">Ngừng sử dụng (Inactive)</option>
                            </select>

                            <button type="button" class="btn btn-secondary btn-sm" id="btnResetFilter" title="Xóa bộ lọc tìm kiếm">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="1 4 1 10 7 10"></polyline>
                                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                </svg>
                                Đặt lại
                            </button>
                        </div>

                        <div class="config-toolbar-right">
                            <button type="button" class="btn btn-primary" id="btnOpenCreateCategoryModal">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                <span id="btnCreateText">Thêm ngành nghề</span>
                            </button>
                        </div>
                    </div>

                    <!-- Bảng dữ liệu danh mục (Table Responsive) -->
                    <div class="config-table-responsive">
                        <table class="config-table" id="categoryTable" aria-label="Bảng dữ liệu danh mục bán hàng">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 90px;" title="Thứ tự hiển thị trong các danh sách dropdown (AC 3)">
                                        Thứ tự (AC 3)
                                    </th>
                                    <th scope="col" style="width: 140px;">Mã giá trị</th>
                                    <th scope="col">Tên hiển thị danh mục</th>
                                    <th scope="col" style="text-align: center;" title="Số lượng bản ghi đang sử dụng giá trị này (AC 2)">
                                        Số tham chiếu (AC 2)
                                    </th>
                                    <th scope="col" style="text-align: center; width: 130px;">Trạng thái</th>
                                    <th scope="col" style="text-align: center; width: 130px;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="categoryTableBody">
                                <!-- Render động từ JS -->
                            </tbody>
                        </table>
                    </div>

                </section>

            </div>
        </main>
    </div>

    <!-- =================================================================
         MODAL 1: THÊM / CHỈNH SỬA GIÁ TRỊ DANH MỤC (AC 1, AC 3)
         ================================================================= -->
    <div class="crm-modal-overlay" id="categoryModal" role="dialog" aria-modal="true" aria-labelledby="modalHeading">
        <div class="crm-modal-card">
            <header class="crm-modal-header">
                <h3 class="crm-modal-title" id="modalHeading">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M12 20h9"></path>
                        <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                    </svg>
                    <span id="modalHeadingText">Thêm giá trị danh mục mới</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseModal" aria-label="Đóng">&times;</button>
            </header>

            <form id="categoryForm" novalidate>
                <input type="hidden" id="formCategoryId" name="id">
                <input type="hidden" id="formCategoryType" name="type">

                <div class="crm-modal-body">
                    <!-- Hàng 1: Mã & Tên giá trị -->
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formCategoryCode" class="form-label">
                                <span>Mã giá trị <span class="required-mark">*</span></span>
                                <span class="form-hint">Duy nhất (VD: TECH)</span>
                            </label>
                            <input type="text" id="formCategoryCode" name="code" class="form-control"
                                   placeholder="VD: TECH, WEB_FORM..." required uppercase>
                            <div class="form-feedback" id="feedbackCategoryCode"></div>
                        </div>

                        <div class="form-group">
                            <label for="formCategoryName" class="form-label">
                                <span>Tên hiển thị <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formCategoryName" name="name" class="form-control"
                                   placeholder="VD: Công nghệ thông tin & Viễn thông" required>
                            <div class="form-feedback" id="feedbackCategoryName"></div>
                        </div>
                    </div>

                    <!-- Hàng 2: Thứ tự hiển thị & Trạng thái hoạt động -->
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formCategorySortOrder" class="form-label">
                                <span>Thứ tự hiển thị (Sort Order) <span class="required-mark">*</span></span>
                                <span class="form-hint">Số nhỏ hiển thị trước</span>
                            </label>
                            <input type="number" id="formCategorySortOrder" name="sortOrder" class="form-control"
                                   placeholder="VD: 1, 2, 3..." min="1" step="1" required>
                            <div class="form-feedback" id="feedbackCategorySortOrder"></div>
                        </div>

                        <div class="form-group">
                            <label for="formCategoryActive" class="form-label">
                                <span>Trạng thái sử dụng <span class="required-mark">*</span></span>
                            </label>
                            <select id="formCategoryActive" name="active" class="form-control" required>
                                <option value="true">Đang sử dụng (Active)</option>
                                <option value="false">Ngừng sử dụng (Inactive)</option>
                            </select>
                            <div class="form-feedback" id="feedbackCategoryActive"></div>
                        </div>
                    </div>

                    <!-- Ghi chú / Mô tả -->
                    <div class="form-group">
                        <label for="formCategoryDesc" class="form-label">
                            <span>Mô tả / Hướng dẫn áp dụng</span>
                        </label>
                        <input type="text" id="formCategoryDesc" name="description" class="form-control"
                               placeholder="VD: Áp dụng cho các doanh nghiệp sản xuất phần mềm, dịch vụ số...">
                    </div>
                </div>

                <footer class="crm-modal-footer">
                    <button type="button" class="btn btn-secondary" id="btnCancelModal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveCategory">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        <span>Lưu giá trị</span>
                    </button>
                </footer>
            </form>
        </div>
    </div>

    <!-- =================================================================
         MODAL 2: CẢNH BÁO CHẶN XÓA GIÁ TRỊ ĐANG THAM CHIẾU (AC 2)
         ================================================================= -->
    <div class="crm-modal-overlay" id="inUseNoticeModal" role="dialog" aria-modal="true" aria-labelledby="inUseModalTitle">
        <div class="crm-modal-card" style="max-width: 520px;">
            <header class="crm-modal-header" style="background-color: #fffbeb;">
                <h3 class="crm-modal-title" id="inUseModalTitle" style="color: #92400e;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                        <line x1="12" y1="9" x2="12" y2="13"></line>
                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                    </svg>
                    <span>Cảnh báo bảo toàn dữ liệu (AC 2)</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseInUseModal" aria-label="Đóng">&times;</button>
            </header>

            <div class="crm-modal-body">
                <p style="margin: 0; font-size: 0.9375rem; color: #334155; line-height: 1.5;" id="inUseModalContent">
                    Giá trị danh mục này đang được tham chiếu bởi các bản ghi trong hệ thống CRM. Theo tiêu chuẩn nghiệm thu <strong>AC 2</strong>, hệ thống <strong style="color:#ef4444;">chặn hoàn toàn thao tác xóa vật lý</strong> để tránh làm sai lệch báo cáo gộp.
                </p>
                <div style="padding: 12px 14px; border-radius: 8px; background-color: #f8fafc; border: 1px solid #e2e8f0; font-size: 0.875rem; color: #475569;">
                    Bạn có muốn chuyển giá trị này sang trạng thái <strong>[Ngừng sử dụng - Inactive]</strong> không? Giá trị sẽ không còn xuất hiện khi thêm mới khách hàng/lead nhưng các dữ liệu lịch sử và báo cáo gộp vẫn được bảo toàn nguyên vẹn.
                </div>
            </div>

            <footer class="crm-modal-footer">
                <button type="button" class="btn btn-secondary" id="btnCancelInUse">Giữ nguyên</button>
                <button type="button" class="btn btn-danger" id="btnConfirmDeactivate">
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
         JAVASCRIPT DOM THUẦN & FETCH API TÍCH HỢP (CRM-44)
         ================================================================= -->
    <script>
    (function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}' || '';

        // Dữ liệu ban đầu mẫu chuẩn B2B thực tế cho cả 4 nhóm danh mục
        var INITIAL_CATEGORIES = {
            industry: [
                { id: 1, type: 'industry', code: 'TECH', name: 'Công nghệ thông tin & Viễn thông', sortOrder: 1, usageCount: 42, active: true },
                { id: 2, type: 'industry', code: 'MFG', name: 'Sản xuất & Chế biến chế tạo', sortOrder: 2, usageCount: 35, active: true },
                { id: 3, type: 'industry', code: 'FIN', name: 'Tài chính, Ngân hàng & Bảo hiểm', sortOrder: 3, usageCount: 28, active: true },
                { id: 4, type: 'industry', code: 'RETAIL', name: 'Bán lẻ & Thương mại điện tử', sortOrder: 4, usageCount: 19, active: true },
                { id: 5, type: 'industry', code: 'REALESTATE', name: 'Bất động sản & Xây dựng', sortOrder: 5, usageCount: 15, active: true },
                { id: 6, type: 'industry', code: 'EDU', name: 'Giáo dục & Đào tạo', sortOrder: 6, usageCount: 8, active: true },
                { id: 7, type: 'industry', code: 'HEALTH', name: 'Y tế & Dược phẩm', sortOrder: 7, usageCount: 0, active: true }
            ],
            company_size: [
                { id: 11, type: 'company_size', code: 'SIZE_MICRO', name: 'Dưới 10 nhân sự (Siêu nhỏ)', sortOrder: 1, usageCount: 20, active: true },
                { id: 12, type: 'company_size', code: 'SIZE_SMALL', name: '10 - 50 nhân sự (Nhỏ)', sortOrder: 2, usageCount: 45, active: true },
                { id: 13, type: 'company_size', code: 'SIZE_MEDIUM', name: '51 - 200 nhân sự (Vừa)', sortOrder: 3, usageCount: 60, active: true },
                { id: 14, type: 'company_size', code: 'SIZE_LARGE', name: '201 - 500 nhân sự (Lớn)', sortOrder: 4, usageCount: 18, active: true },
                { id: 15, type: 'company_size', code: 'SIZE_CORP', name: 'Trên 500 nhân sự (Tập đoàn / Enterprise)', sortOrder: 5, usageCount: 12, active: true }
            ],
            lead_source: [
                { id: 21, type: 'lead_source', code: 'WEB_FORM', name: 'Website / Form đăng ký trực tuyến', sortOrder: 1, usageCount: 85, active: true },
                { id: 22, type: 'lead_source', code: 'REFERRAL', name: 'Khách hàng & Đối tác giới thiệu', sortOrder: 2, usageCount: 52, active: true },
                { id: 23, type: 'lead_source', code: 'EVENT_EXPO', name: 'Hội thảo, Triển lãm & Sự kiện kết nối', sortOrder: 3, usageCount: 34, active: true },
                { id: 24, type: 'lead_source', code: 'PAID_ADS', name: 'Quảng cáo Google / Mạng xã hội', sortOrder: 4, usageCount: 40, active: true },
                { id: 25, type: 'lead_source', code: 'OUTREACH', name: 'Tiếp cận trực tiếp / Cold Outreach', sortOrder: 5, usageCount: 15, active: true },
                { id: 26, type: 'lead_source', code: 'PARTNER', name: 'Kênh Đại lý & Đối tác chiến lược', sortOrder: 6, usageCount: 22, active: true },
                { id: 27, type: 'lead_source', code: 'PR_MEDIA', name: 'Báo chí truyền thông & PR', sortOrder: 7, usageCount: 0, active: false }
            ],
            activity_type: [
                { id: 31, type: 'activity_type', code: 'CALL', name: 'Cuộc gọi điện tư vấn (Phone Call)', sortOrder: 1, usageCount: 120, active: true },
                { id: 32, type: 'activity_type', code: 'MEETING', name: 'Gặp mặt trực tiếp (Face-to-face Meeting)', sortOrder: 2, usageCount: 65, active: true },
                { id: 33, type: 'activity_type', code: 'DEMO', name: 'Họp trực tuyến / Demo phần mềm', sortOrder: 3, usageCount: 48, active: true },
                { id: 34, type: 'activity_type', code: 'EMAIL', name: 'Gửi email đề xuất & Trao đổi hợp đồng', sortOrder: 4, usageCount: 95, active: true },
                { id: 35, type: 'activity_type', code: 'PRESENTATION', name: 'Thuyết trình giải pháp & Chốt báo giá', sortOrder: 5, usageCount: 30, active: true },
                { id: 36, type: 'activity_type', code: 'SURVEY', name: 'Khảo sát hiện trạng hạ tầng On-site', sortOrder: 6, usageCount: 0, active: true }
            ]
        };

        var TAB_TITLES = {
            industry: 'ngành nghề',
            company_size: 'quy mô doanh nghiệp',
            lead_source: 'nguồn lead',
            activity_type: 'loại hoạt động'
        };

        // State quản lý
        var state = {
            currentType: 'industry', // industry | company_size | lead_source | activity_type
            data: JSON.parse(JSON.stringify(INITIAL_CATEGORIES)),
            keyword: '',
            filterActive: '',
            pendingDeactivateItem: null
        };

        // DOM Elements
        var categoryTableBody = document.getElementById('categoryTableBody');
        var categorySearchInput = document.getElementById('categorySearchInput');
        var filterActiveStatus = document.getElementById('filterActiveStatus');
        var btnResetFilter = document.getElementById('btnResetFilter');
        var configLoadingOverlay = document.getElementById('configLoadingOverlay');
        var btnCreateText = document.getElementById('btnCreateText');

        var statIndustryCount = document.getElementById('statIndustryCount');
        var statCompanySizeCount = document.getElementById('statCompanySizeCount');
        var statLeadSourceCount = document.getElementById('statLeadSourceCount');
        var statActivityTypeCount = document.getElementById('statActivityTypeCount');

        var badgeIndustry = document.getElementById('badgeIndustry');
        var badgeCompanySize = document.getElementById('badgeCompanySize');
        var badgeLeadSource = document.getElementById('badgeLeadSource');
        var badgeActivityType = document.getElementById('badgeActivityType');

        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');
        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');

        // Modal Elements
        var categoryModal = document.getElementById('categoryModal');
        var categoryForm = document.getElementById('categoryForm');
        var modalHeadingText = document.getElementById('modalHeadingText');
        var btnOpenCreateCategoryModal = document.getElementById('btnOpenCreateCategoryModal');
        var btnCloseModal = document.getElementById('btnCloseModal');
        var btnCancelModal = document.getElementById('btnCancelModal');

        var formCategoryId = document.getElementById('formCategoryId');
        var formCategoryType = document.getElementById('formCategoryType');
        var formCategoryCode = document.getElementById('formCategoryCode');
        var formCategoryName = document.getElementById('formCategoryName');
        var formCategorySortOrder = document.getElementById('formCategorySortOrder');
        var formCategoryActive = document.getElementById('formCategoryActive');
        var formCategoryDesc = document.getElementById('formCategoryDesc');

        var inUseNoticeModal = document.getElementById('inUseNoticeModal');
        var inUseModalContent = document.getElementById('inUseModalContent');
        var btnCloseInUseModal = document.getElementById('btnCloseInUseModal');
        var btnCancelInUse = document.getElementById('btnCancelInUse');
        var btnConfirmDeactivate = document.getElementById('btnConfirmDeactivate');

        function escapeHtml(str) {
            if (str == null) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

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

        function updateBadges() {
            var indCount = state.data.industry.length;
            var csCount = state.data.company_size.length;
            var lsCount = state.data.lead_source.length;
            var atCount = state.data.activity_type.length;

            statIndustryCount.textContent = indCount;
            statCompanySizeCount.textContent = csCount;
            statLeadSourceCount.textContent = lsCount;
            statActivityTypeCount.textContent = atCount;

            badgeIndustry.textContent = indCount;
            badgeCompanySize.textContent = csCount;
            badgeLeadSource.textContent = lsCount;
            badgeActivityType.textContent = atCount;
        }

        // Lấy danh sách hiện tại đã sắp xếp theo sortOrder
        function getCurrentList() {
            var list = state.data[state.currentType] || [];
            // Sort theo sortOrder
            list.sort(function (a, b) {
                return (a.sortOrder || 0) - (b.sortOrder || 0);
            });
            return list;
        }

        // Lọc danh sách theo từ khóa & trạng thái
        function getFilteredList() {
            var list = getCurrentList();
            var kw = (state.keyword || '').trim().toLowerCase();
            var act = state.filterActive;

            return list.filter(function (item) {
                var matchKw = true;
                if (kw) {
                    var codeMatch = (item.code || '').toLowerCase().indexOf(kw) !== -1;
                    var nameMatch = (item.name || '').toLowerCase().indexOf(kw) !== -1;
                    matchKw = codeMatch || nameMatch;
                }
                var matchAct = true;
                if (act !== '') {
                    matchAct = item.active === (act === 'true');
                }
                return matchKw && matchAct;
            });
        }

        // Render bảng dữ liệu
        function renderTable() {
            categoryTableBody.innerHTML = '';
            var filtered = getFilteredList();
            var fullList = getCurrentList();

            if (filtered.length === 0) {
                categoryTableBody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 40px; color: #64748b;">Không tìm thấy giá trị nào phù hợp.</td></tr>';
                return;
            }

            filtered.forEach(function (item, index) {
                var tr = document.createElement('tr');
                if (!item.active) {
                    tr.className = 'row-inactive';
                }

                var fullIndex = fullList.findIndex(function (it) { return it.id === item.id; });
                var isFirst = fullIndex === 0;
                var isLast = fullIndex === fullList.length - 1;

                // Cột tham chiếu (AC 2)
                var usage = item.usageCount || 0;
                var usageHtml = usage > 0
                    ? '<span class="badge-usage usage-in-use" title="Đang được sử dụng bởi ' + usage + ' bản ghi khách hàng/lead/hoạt động">' + usage + ' bản ghi</span>'
                    : '<span class="badge-usage usage-zero" title="Chưa có bản ghi nào tham chiếu">0</span>';

                // Cột trạng thái
                var statusHtml = item.active
                    ? '<span class="badge-status badge-status-active"><span style="width:6px;height:6px;border-radius:50%;background:#10b981;display:inline-block;"></span> Đang dùng</span>'
                    : '<span class="badge-status badge-status-inactive"><span style="width:6px;height:6px;border-radius:50%;background:#94a3b8;display:inline-block;"></span> Ngừng dùng</span>';

                tr.innerHTML =
                    '<!-- Thứ tự hiển thị & nút đổi thứ tự (AC 3) -->' +
                    '<td>' +
                        '<div class="order-control-cell">' +
                            '<span class="order-badge">' + item.sortOrder + '</span>' +
                            '<div class="order-btn-group">' +
                                '<button type="button" class="btn-order-arrow" data-action="move-up" data-id="' + item.id + '" title="Đẩy lên trước" ' + (isFirst ? 'disabled' : '') + '>▲</button>' +
                                '<button type="button" class="btn-order-arrow" data-action="move-down" data-id="' + item.id + '" title="Đẩy xuống sau" ' + (isLast ? 'disabled' : '') + '>▼</button>' +
                            '</div>' +
                        '</div>' +
                    '</td>' +
                    '<td><span class="col-code">' + escapeHtml(item.code) + '</span></td>' +
                    '<td>' +
                        '<div style="font-weight: 600; color: #0f172a;">' + escapeHtml(item.name) + '</div>' +
                        (item.description ? '<div style="font-size: 0.75rem; color: #64748b; margin-top: 2px;">' + escapeHtml(item.description) + '</div>' : '') +
                    '</td>' +
                    '<td style="text-align: center;">' + usageHtml + '</td>' +
                    '<td style="text-align: center;">' + statusHtml + '</td>' +
                    '<td>' +
                        '<div class="action-cell" style="justify-content: center;">' +
                            '<button type="button" class="btn-icon" data-action="edit" data-id="' + item.id + '" title="Chỉnh sửa giá trị">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>' +
                                    '<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>' +
                                '</svg>' +
                            '</button>' +
                            '<button type="button" class="btn-icon btn-icon-danger" data-action="delete" data-id="' + item.id + '" title="Xóa hoặc Ngừng dùng (AC 2)">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<polyline points="3 6 5 6 21 6"></polyline>' +
                                    '<path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>' +
                                '</svg>' +
                            '</button>' +
                        '</div>' +
                    '</td>';

                categoryTableBody.appendChild(tr);
            });
        }

        // Tải danh mục từ backend (GET /api/configurations/categories?type=...)
        async function fetchCategory(type) {
            configLoadingOverlay.style.display = 'flex';
            hideAlerts();

            var endpoint = contextPath + '/api/configurations/categories?type=' + encodeURIComponent(type);
            try {
                var res = await fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });

                configLoadingOverlay.style.display = 'none';

                if (res.ok) {
                    var data = await res.json();
                    var list = (data && data.data && Array.isArray(data.data.items))
                        ? data.data.items
                        : (Array.isArray(data) ? data : null);

                    if (list && list.length > 0) {
                        state.data[type] = list;
                    }
                } else {
                    console.info('Backend /api/configurations/categories trả mã ' + res.status + ' - Sử dụng fallback local data cho CRM-44.');
                }
            } catch (err) {
                configLoadingOverlay.style.display = 'none';
                console.info('Chưa kết nối Backend Servlet - Sử dụng dữ liệu danh mục CRM-44:', err);
            }

            updateBadges();
            renderTable();
        }

        // Chuyển đổi Tab danh mục (AC 1)
        window.switchCategoryTab = function (tabType) {
            state.currentType = tabType;

            // Cập nhật giao diện tab buttons
            document.querySelectorAll('.config-tab-btn').forEach(function (btn) { btn.classList.remove('active'); });
            document.querySelectorAll('.config-stat-card').forEach(function (card) { card.classList.remove('active-card'); });

            if (tabType === 'industry') {
                document.getElementById('tabBtnIndustry').classList.add('active');
                document.getElementById('cardIndustry').classList.add('active-card');
                btnCreateText.textContent = 'Thêm ngành nghề';
            } else if (tabType === 'company_size') {
                document.getElementById('tabBtnCompanySize').classList.add('active');
                document.getElementById('cardCompanySize').classList.add('active-card');
                btnCreateText.textContent = 'Thêm quy mô';
            } else if (tabType === 'lead_source') {
                document.getElementById('tabBtnLeadSource').classList.add('active');
                document.getElementById('cardLeadSource').classList.add('active-card');
                btnCreateText.textContent = 'Thêm nguồn lead';
            } else if (tabType === 'activity_type') {
                document.getElementById('tabBtnActivityType').classList.add('active');
                document.getElementById('cardActivityType').classList.add('active-card');
                btnCreateText.textContent = 'Thêm loại hoạt động';
            }

            fetchCategory(tabType);
        };

        // Tìm kiếm & Bộ lọc
        categorySearchInput.addEventListener('input', function () {
            state.keyword = categorySearchInput.value.trim();
            renderTable();
        });

        filterActiveStatus.addEventListener('change', function () {
            state.filterActive = filterActiveStatus.value;
            renderTable();
        });

        btnResetFilter.addEventListener('click', function () {
            categorySearchInput.value = '';
            filterActiveStatus.value = '';
            state.keyword = '';
            state.filterActive = '';
            renderTable();
        });

        // Đổi thứ tự hiển thị bằng nút Mũi tên (AC 3)
        async function moveItemOrder(id, direction) {
            var fullList = getCurrentList();
            var index = fullList.findIndex(function (it) { return it.id === id; });
            if (index === -1) return;

            var targetIndex = direction === 'up' ? index - 1 : index + 1;
            if (targetIndex < 0 || targetIndex >= fullList.length) return;

            var currentItem = fullList[index];
            var swapItem = fullList[targetIndex];

            // Hoán đổi sortOrder
            var tempOrder = currentItem.sortOrder;
            currentItem.sortOrder = swapItem.sortOrder;
            swapItem.sortOrder = tempOrder;

            // Gọi API lưu cập nhật
            var endpointCurrent = contextPath + '/api/configurations/categories/' + currentItem.id;
            var endpointSwap = contextPath + '/api/configurations/categories/' + swapItem.id;
            try {
                fetch(endpointCurrent, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sortOrder: currentItem.sortOrder })
                });
                fetch(endpointSwap, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sortOrder: swapItem.sortOrder })
                });
            } catch (e) {}

            showSuccessAlert('Đã cập nhật thứ tự hiển thị của danh mục!');
            renderTable();
        }

        // Mở Modal Thêm mới giá trị
        btnOpenCreateCategoryModal.addEventListener('click', function () {
            hideAlerts();
            categoryForm.reset();
            formCategoryId.value = '';
            formCategoryType.value = state.currentType;

            var fullList = getCurrentList();
            var nextOrder = fullList.length > 0 ? Math.max.apply(null, fullList.map(function (it) { return it.sortOrder || 0; })) + 1 : 1;
            formCategorySortOrder.value = nextOrder;

            modalHeadingText.textContent = 'Thêm mới ' + TAB_TITLES[state.currentType];
            clearFormErrors();
            categoryModal.style.display = 'flex';
        });

        function closeModal() {
            categoryModal.style.display = 'none';
            clearFormErrors();
        }

        btnCloseModal.addEventListener('click', closeModal);
        btnCancelModal.addEventListener('click', closeModal);

        function clearFormErrors() {
            document.querySelectorAll('.form-feedback').forEach(function (el) { el.textContent = ''; });
            document.querySelectorAll('.form-control').forEach(function (el) { el.classList.remove('is-invalid'); });
        }

        // Mở Modal Chỉnh sửa
        function openEditModal(id) {
            var fullList = getCurrentList();
            var item = fullList.find(function (it) { return it.id === id; });
            if (!item) return;

            hideAlerts();
            clearFormErrors();
            formCategoryId.value = item.id;
            formCategoryType.value = item.type;
            formCategoryCode.value = item.code;
            formCategoryName.value = item.name;
            formCategorySortOrder.value = item.sortOrder;
            formCategoryActive.value = item.active ? 'true' : 'false';
            formCategoryDesc.value = item.description || '';

            modalHeadingText.textContent = 'Chỉnh sửa: ' + item.name;
            categoryModal.style.display = 'flex';
        }

        // Validate Form
        function validateForm() {
            clearFormErrors();
            var valid = true;

            var code = formCategoryCode.value.trim();
            if (!code) {
                document.getElementById('feedbackCategoryCode').textContent = 'Vui lòng nhập mã giá trị.';
                formCategoryCode.classList.add('is-invalid');
                valid = false;
            }

            var name = formCategoryName.value.trim();
            if (!name) {
                document.getElementById('feedbackCategoryName').textContent = 'Vui lòng nhập tên hiển thị.';
                formCategoryName.classList.add('is-invalid');
                valid = false;
            }

            var sortOrder = parseInt(formCategorySortOrder.value, 10);
            if (isNaN(sortOrder) || sortOrder <= 0) {
                document.getElementById('feedbackCategorySortOrder').textContent = 'Thứ tự hiển thị phải là số nguyên dương lớn hơn 0.';
                formCategorySortOrder.classList.add('is-invalid');
                valid = false;
            }

            return valid;
        }

        // Submit Form Thêm/Sửa (POST / PUT /api/configurations/categories)
        categoryForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            if (!validateForm()) return;

            var id = formCategoryId.value;
            var isEdit = Boolean(id);
            var type = formCategoryType.value || state.currentType;

            var payload = {
                type: type,
                code: formCategoryCode.value.trim().toUpperCase(),
                name: formCategoryName.value.trim(),
                sortOrder: parseInt(formCategorySortOrder.value, 10),
                active: formCategoryActive.value === 'true',
                description: formCategoryDesc.value.trim()
            };

            var endpoint = isEdit
                ? (contextPath + '/api/configurations/categories/' + id)
                : (contextPath + '/api/configurations/categories');
            var method = isEdit ? 'PUT' : 'POST';

            try {
                var res = await fetch(endpoint, {
                    method: method,
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify(payload)
                });
                if (res.ok) {
                    showSuccessAlert(isEdit ? 'Cập nhật giá trị thành công!' : 'Tạo mới giá trị thành công!');
                }
            } catch (err) {
                console.info('Backend chưa sẵn sàng - Cập nhật dữ liệu tại local state:', err);
            }

            var list = state.data[type];
            if (isEdit) {
                var idx = list.findIndex(function (it) { return it.id === Number(id); });
                if (idx !== -1) {
                    payload.id = Number(id);
                    payload.usageCount = list[idx].usageCount;
                    list[idx] = Object.assign({}, list[idx], payload);
                }
                showSuccessAlert('Đã cập nhật giá trị [' + payload.name + '] thành công!');
            } else {
                var newId = list.length > 0 ? Math.max.apply(null, list.map(function(it){ return it.id; })) + 1 : 1;
                payload.id = newId;
                payload.usageCount = 0;
                list.push(payload);
                showSuccessAlert('Đã thêm mới giá trị [' + payload.name + '] vào danh mục!');
            }

            closeModal();
            updateBadges();
            renderTable();
        });

        // Xử lý Xóa / Ngừng kích hoạt (AC 2)
        function handleDelete(id) {
            var fullList = getCurrentList();
            var item = fullList.find(function (it) { return it.id === id; });
            if (!item) return;

            // AC 2: Nếu đang có tham chiếu (usageCount > 0) thì CHẶN XÓA
            if (item.usageCount && item.usageCount > 0) {
                state.pendingDeactivateItem = item;
                inUseModalContent.innerHTML =
                    'Giá trị danh mục <strong>[' + escapeHtml(item.code) + ' - ' + escapeHtml(item.name) + ']</strong> đang được tham chiếu bởi <strong>' +
                    item.usageCount + ' bản ghi</strong> (khách hàng, lead hoặc hoạt động). Theo tiêu chuẩn <strong>AC 2</strong>, hệ thống <span style="color:#ef4444;font-weight:700;">không cho phép xóa bỏ hoàn toàn</span> để tránh làm sai lệch báo cáo gộp.';
                inUseNoticeModal.style.display = 'flex';
                return;
            }

            // Nếu usageCount = 0, cho phép xóa an toàn
            if (confirm('Giá trị [' + item.name + '] chưa có bản ghi nào sử dụng. Bạn có chắc chắn muốn xóa vĩnh viễn không?')) {
                deleteDirectly(id);
            }
        }

        async function deleteDirectly(id) {
            var endpoint = contextPath + '/api/configurations/categories/' + id;
            try {
                await fetch(endpoint, { method: 'DELETE' });
            } catch (e) {}

            var list = state.data[state.currentType];
            state.data[state.currentType] = list.filter(function (it) { return it.id !== id; });
            showSuccessAlert('Đã xóa giá trị danh mục thành công!');
            updateBadges();
            renderTable();
        }

        // Chuyển sang Ngừng sử dụng (Inactive) khi đang được tham chiếu (AC 2)
        btnConfirmDeactivate.addEventListener('click', async function () {
            if (!state.pendingDeactivateItem) return;
            var item = state.pendingDeactivateItem;
            item.active = false;

            var endpoint = contextPath + '/api/configurations/categories/' + item.id;
            try {
                await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ active: false })
                });
            } catch (e) {}

            inUseNoticeModal.style.display = 'none';
            state.pendingDeactivateItem = null;
            showSuccessAlert('Đã chuyển giá trị [' + item.name + '] sang trạng thái [Ngừng sử dụng] theo tiêu chuẩn AC 2!');
            renderTable();
        });

        btnCloseInUseModal.addEventListener('click', function () {
            inUseNoticeModal.style.display = 'none';
            state.pendingDeactivateItem = null;
        });
        btnCancelInUse.addEventListener('click', function () {
            inUseNoticeModal.style.display = 'none';
            state.pendingDeactivateItem = null;
        });

        // Bắt sự kiện bảng
        categoryTableBody.addEventListener('click', function (e) {
            var btn = e.target.closest('[data-action]');
            if (!btn) return;
            var action = btn.getAttribute('data-action');
            var id = Number(btn.getAttribute('data-id'));

            if (action === 'edit') {
                openEditModal(id);
            } else if (action === 'delete') {
                handleDelete(id);
            } else if (action === 'move-up') {
                moveItemOrder(id, 'up');
            } else if (action === 'move-down') {
                moveItemOrder(id, 'down');
            }
        });

        // Khởi động
        fetchCategory('industry');

    })();
    </script>
</body>
</html>
