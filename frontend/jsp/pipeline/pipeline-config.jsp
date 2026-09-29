<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu hình giai đoạn Pipeline & Xác suất thắng - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Cấu hình Pipeline (CRM-47) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/pipeline/pipeline.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Cấu hình Pipeline -->
        <main class="pipeline-page" id="pipelineApp" role="main">
            <div class="pipeline-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="pipeline-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Thiết lập bán hàng</span>
                    <span class="separator">/</span>
                    <span class="active">Giai đoạn Pipeline & Xác suất thắng</span>
                </nav>

                <!-- Header màn hình -->
                <header class="pipeline-header">
                    <div class="pipeline-header-info">
                        <h1>Cấu hình các giai đoạn Pipeline & Xác suất thắng</h1>
                        <p>Khai báo chuỗi tiến trình bán hàng tuần tự, thiết lập tỷ lệ thắng (%) để tự động tính dự báo doanh số có trọng số và ràng buộc điều kiện rời giai đoạn.</p>
                    </div>

                    <div class="pipeline-header-badges">
                        <span class="pipeline-badge pipeline-badge-forecast" title="Công thức tự động: Doanh số dự báo = Giá trị cơ hội × Xác suất thắng">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <line x1="18" y1="20" x2="18" y2="10"></line>
                                <line x1="12" y1="20" x2="12" y2="4"></line>
                                <line x1="6" y1="20" x2="6" y2="14"></line>
                            </svg>
                            Weighted Forecast Engine
                        </span>
                        <span class="pipeline-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                            S2-09 / CRM-47
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts) -->
                <div class="pipeline-alerts" id="pipelineAlertsArea" aria-live="polite">
                    <div class="pipeline-alert pipeline-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="pipeline-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="pipeline-alert-content">
                            <div class="pipeline-alert-title" id="globalErrorTitle">Đã xảy ra lỗi</div>
                            <div id="globalErrorMessage"></div>
                        </div>
                        <button type="button" class="pipeline-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="pipeline-alert pipeline-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="pipeline-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="pipeline-alert-content">
                            <div class="pipeline-alert-title" id="globalSuccessTitle">Thành công</div>
                            <div id="globalSuccessMessage"></div>
                        </div>
                        <button type="button" class="pipeline-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>
                </div>

                <!-- Banner Nghiệp vụ Dự báo Doanh số & Điều kiện rời giai đoạn (AC 2, AC 3) -->
                <aside class="pipeline-banner" role="region" aria-label="Quy tắc dự báo doanh số">
                    <svg class="pipeline-banner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    <div>
                        <div class="pipeline-banner-title">Nguyên tắc tính Doanh số dự báo (Weighted Forecast) & Điều kiện rời giai đoạn (Exit Criteria)</div>
                        <p class="pipeline-banner-desc">
                            Mỗi giai đoạn được gắn tỷ lệ xác suất thắng (%) mặc định để hệ thống tự động tính Doanh số dự báo có trọng số (<strong>Weighted Forecast = Tổng giá trị Deal × Xác suất thắng</strong>). Điều này giúp Giám đốc kinh doanh dự báo doanh số chính xác dựa trên dữ liệu thay vì cảm nhận. Đồng thời, điều kiện bắt buộc rời giai đoạn (<strong>Stage Exit Criteria</strong>) bảo đảm nhân viên kinh doanh phải hoàn thành đủ các bước thực tế (gặp mặt, báo giá đã duyệt) trước khi chuyển giai đoạn.
                        </p>
                    </div>
                </aside>

                <!-- Thống kê nhanh Pipeline -->
                <section class="pipeline-stats-grid" aria-label="Thống kê tiến trình bán hàng">
                    <div class="pipeline-stat-card">
                        <div class="pipeline-stat-icon-wrap stat-icon-blue">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                        </div>
                        <div class="pipeline-stat-content">
                            <span class="pipeline-stat-value" id="statStageCount">6</span>
                            <span class="pipeline-stat-label">Giai đoạn bán hàng</span>
                        </div>
                    </div>

                    <div class="pipeline-stat-card">
                        <div class="pipeline-stat-icon-wrap stat-icon-purple">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                            </svg>
                        </div>
                        <div class="pipeline-stat-content">
                            <span class="pipeline-stat-value" id="statActiveDeals">56</span>
                            <span class="pipeline-stat-label">Cơ hội đang chạy (Active Deals)</span>
                        </div>
                    </div>

                    <div class="pipeline-stat-card">
                        <div class="pipeline-stat-icon-wrap stat-icon-amber">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <polyline points="12 6 12 12 14 14"></polyline>
                            </svg>
                        </div>
                        <div class="pipeline-stat-content">
                            <span class="pipeline-stat-value" id="statAvgProb">58.3%</span>
                            <span class="pipeline-stat-label">Xác suất thắng trung bình</span>
                        </div>
                    </div>

                    <div class="pipeline-stat-card">
                        <div class="pipeline-stat-icon-wrap stat-icon-green">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                        </div>
                        <div class="pipeline-stat-content">
                            <span class="pipeline-stat-value">100%</span>
                            <span class="pipeline-stat-label">Tỷ lệ chốt thành công tối đa</span>
                        </div>
                    </div>
                </section>

                <!-- Thanh Tiến trình Trực quan Pipeline (Funnel / Stepper Bar - AC 1 & AC 2) -->
                <section class="pipeline-visual-wrapper" aria-label="Tiến trình bán hàng trực quan">
                    <div class="pipeline-visual-header">
                        <h2 class="pipeline-visual-title">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                            <span>Chuỗi tiến trình bán hàng tuần tự (Pipeline Flow)</span>
                        </h2>
                        <span style="font-size: 0.8125rem; color: #64748b;">(Thứ tự từ trái sang phải phản ánh chu kỳ bán hàng)</span>
                    </div>

                    <div class="pipeline-stepper" id="pipelineStepper">
                        <!-- Render các bước pipeline tự động từ JS -->
                    </div>
                </section>

                <!-- Hộp Mô phỏng Dự báo Doanh số (Weighted Forecast Calculator - AC 2) -->
                <section class="forecast-calc-card" aria-label="Công cụ tính dự báo doanh số">
                    <div class="forecast-calc-header">
                        <h3 class="forecast-calc-title">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <line x1="12" y1="1" x2="12" y2="23"></line>
                                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                            </svg>
                            <span>Mô phỏng Doanh số dự báo có trọng số (Weighted Forecast Calculator)</span>
                        </h3>

                        <div class="forecast-calc-inputs">
                            <div class="forecast-input-group">
                                <label for="calcDealAmount">Giá trị Deal mẫu (VNĐ):</label>
                                <input type="number" id="calcDealAmount" class="forecast-input" value="100000000" min="1000000" step="1000000">
                            </div>
                        </div>
                    </div>

                    <div class="forecast-chips-grid" id="forecastChipsGrid">
                        <!-- Render các thẻ dự báo tương ứng với từng giai đoạn từ JS -->
                    </div>
                </section>

                <!-- Card Danh sách Giai đoạn & Bảng Quản trị (AC 1, AC 3, AC 4) -->
                <section class="pipeline-card" style="position: relative;" aria-labelledby="pipelineCardTitle">

                    <!-- Loading Overlay -->
                    <div class="pipeline-loading-overlay" id="pipelineLoadingOverlay" aria-hidden="true">
                        <div class="pipeline-spinner"></div>
                    </div>

                    <!-- Toolbar -->
                    <div class="pipeline-toolbar">
                        <div class="pipeline-toolbar-left">
                            <span style="font-size: 1rem; font-weight: 700; color: #0f172a;" id="pipelineCardTitle">
                                Danh sách các giai đoạn & Quy định rời giai đoạn (Exit Criteria)
                            </span>
                        </div>

                        <div class="pipeline-toolbar-right">
                            <button type="button" class="btn btn-primary" id="btnOpenCreateStageModal">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                <span>Thêm giai đoạn mới</span>
                            </button>
                        </div>
                    </div>

                    <!-- Bảng dữ liệu giai đoạn -->
                    <div class="pipeline-table-responsive">
                        <table class="pipeline-table" id="stageTable" aria-label="Bảng cấu hình giai đoạn bán hàng">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 80px;" title="Thứ tự hiển thị trong chuỗi tiến trình bán hàng (AC 1)">
                                        Thứ tự
                                    </th>
                                    <th scope="col" style="width: 140px;">Mã giai đoạn</th>
                                    <th scope="col">Tên giai đoạn bán hàng</th>
                                    <th scope="col" style="width: 180px;" title="Xác suất thắng mặc định dùng để tự động tính weighted forecast (AC 2)">
                                        Xác suất thắng (%)
                                    </th>
                                    <th scope="col" title="Điều kiện bắt buộc phải thỏa mãn trước khi rời sang giai đoạn kế tiếp (AC 3)">
                                        Điều kiện rời giai đoạn (Exit Criteria)
                                    </th>
                                    <th scope="col" style="text-align: center; width: 130px;" title="Số lượng cơ hội kinh doanh đang chạy trong giai đoạn này (AC 4)">
                                        Cơ hội đang chạy
                                    </th>
                                    <th scope="col" style="text-align: center; width: 110px;">Trạng thái</th>
                                    <th scope="col" style="text-align: center; width: 120px;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="stageTableBody">
                                <!-- Render động từ JS -->
                            </tbody>
                        </table>
                    </div>

                </section>

            </div>
        </main>
    </div>

    <!-- =================================================================
         MODAL 1: THÊM / CHỈNH SỬA GIAI ĐOẠN (AC 1, AC 2, AC 3, AC 4)
         ================================================================= -->
    <div class="crm-modal-overlay" id="stageModal" role="dialog" aria-modal="true" aria-labelledby="stageModalTitle">
        <div class="crm-modal-card">
            <header class="crm-modal-header">
                <h3 class="crm-modal-title" id="stageModalTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                        <polyline points="2 17 12 22 22 17"></polyline>
                        <polyline points="2 12 12 17 22 12"></polyline>
                    </svg>
                    <span id="stageModalHeading">Thêm giai đoạn mới</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseStageModal" aria-label="Đóng">&times;</button>
            </header>

            <form id="stageForm" novalidate>
                <input type="hidden" id="formStageId" name="id">

                <div class="crm-modal-body">
                    <!-- Cảnh báo bảo vệ cơ hội đang chạy (AC 4) -->
                    <div id="noticeActiveDealsAlert" style="display: none; padding: 12px 14px; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; font-size: 0.8125rem; color: #1e40af; line-height: 1.45;">
                        <div style="font-weight: 700; margin-bottom: 2px;">Lưu ý bảo toàn cơ hội (AC 4):</div>
                        <span id="noticeActiveDealsText">Giai đoạn này đang có N cơ hội kinh doanh đang chạy. Khi điều chỉnh xác suất thắng, hệ thống sẽ tự động cập nhật lại Doanh số dự báo (weighted forecast) tương ứng mà không làm gián đoạn tiến trình cơ hội.</span>
                    </div>

                    <!-- Hàng 1: Mã giai đoạn & Tên giai đoạn -->
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formStageCode" class="form-label">
                                <span>Mã giai đoạn <span class="required-mark">*</span></span>
                                <span class="form-hint">Duy nhất (VD: PROPOSAL)</span>
                            </label>
                            <input type="text" id="formStageCode" name="code" class="form-control"
                                   placeholder="VD: PROPOSAL, QUOTATION..." required uppercase>
                            <div class="form-feedback" id="feedbackStageCode"></div>
                        </div>

                        <div class="form-group">
                            <label for="formStageName" class="form-label">
                                <span>Tên giai đoạn <span class="required-mark">*</span></span>
                            </label>
                            <input type="text" id="formStageName" name="name" class="form-control"
                                   placeholder="VD: Đề xuất giải pháp & Demo" required>
                            <div class="form-feedback" id="feedbackStageName"></div>
                        </div>
                    </div>

                    <!-- Hàng 2: Xác suất thắng (%) kèm Slider (AC 2) -->
                    <div class="form-group">
                        <label for="formWinProbability" class="form-label">
                            <span>Xác suất thắng mặc định (% - AC 2) <span class="required-mark">*</span></span>
                            <span class="form-hint">Dùng tự động tính Doanh số dự báo</span>
                        </label>
                        <div class="prob-slider-wrap">
                            <input type="range" id="formWinProbSlider" min="0" max="100" step="5" value="50" class="prob-slider">
                            <input type="number" id="formWinProbability" name="winProbability" class="form-control prob-input-num"
                                   min="0" max="100" step="1" value="50" required>
                            <span style="font-weight: 700; color: #475569;">%</span>
                        </div>
                        <div class="form-feedback" id="feedbackWinProbability"></div>
                    </div>

                    <!-- Hàng 3: Điều kiện rời giai đoạn (Exit Criteria - AC 3) -->
                    <div class="form-group">
                        <label for="formExitCriteria" class="form-label">
                            <span>Điều kiện bắt buộc để rời giai đoạn (Stage Exit Criteria - AC 3) <span class="required-mark">*</span></span>
                            <span class="form-hint">Quy tắc bắt buộc hoàn thành</span>
                        </label>
                        <textarea id="formExitCriteria" name="exitCriteria" class="form-control" rows="3"
                                  placeholder="VD: Phải thực hiện ít nhất 1 cuộc gặp/demo và đính kèm biên bản xác nhận nhu cầu..." required></textarea>
                        <div class="form-feedback" id="feedbackExitCriteria"></div>
                    </div>

                    <!-- Hàng 4: Thứ tự & Trạng thái -->
                    <div class="form-row-2">
                        <div class="form-group">
                            <label for="formStageSortOrder" class="form-label">
                                <span>Thứ tự tiến trình (Sort Order) <span class="required-mark">*</span></span>
                                <span class="form-hint">Số nhỏ đứng trước</span>
                            </label>
                            <input type="number" id="formStageSortOrder" name="sortOrder" class="form-control"
                                   placeholder="VD: 1, 2, 3..." min="1" step="1" required>
                            <div class="form-feedback" id="feedbackStageSortOrder"></div>
                        </div>

                        <div class="form-group">
                            <label for="formStageActive" class="form-label">
                                <span>Trạng thái sử dụng <span class="required-mark">*</span></span>
                            </label>
                            <select id="formStageActive" name="active" class="form-control" required>
                                <option value="true">Đang kích hoạt (Active)</option>
                                <option value="false">Ngừng kích hoạt (Inactive)</option>
                            </select>
                            <div class="form-feedback" id="feedbackStageActive"></div>
                        </div>
                    </div>
                </div>

                <footer class="crm-modal-footer">
                    <button type="button" class="btn btn-secondary" id="btnCancelStageModal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveStage">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        <span>Lưu giai đoạn</span>
                    </button>
                </footer>
            </form>
        </div>
    </div>

    <!-- =================================================================
         MODAL 2: CẢNH BÁO CHẶN XÓA GIAI ĐOẠN ĐANG CÓ DEAL (AC 4)
         ================================================================= -->
    <div class="crm-modal-overlay" id="activeDealsNoticeModal" role="dialog" aria-modal="true" aria-labelledby="activeDealsModalTitle">
        <div class="crm-modal-card" style="max-width: 520px;">
            <header class="crm-modal-header" style="background-color: #fffbeb;">
                <h3 class="crm-modal-title" id="activeDealsModalTitle" style="color: #92400e;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                        <line x1="12" y1="9" x2="12" y2="13"></line>
                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                    </svg>
                    <span>Cảnh báo bảo toàn tiến trình cơ hội (AC 4)</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseDealsModal" aria-label="Đóng">&times;</button>
            </header>

            <div class="crm-modal-body">
                <p style="margin: 0; font-size: 0.9375rem; color: #334155; line-height: 1.5;" id="activeDealsModalContent">
                    Giai đoạn này đang có các cơ hội kinh doanh đang chạy. Theo tiêu chuẩn nghiệm thu <strong>AC 4</strong>, hệ thống <strong style="color:#ef4444;">chặn hoàn toàn việc xóa giai đoạn</strong> để không làm hỏng dữ liệu các cơ hội đang bám đuổi.
                </p>
                <div style="padding: 12px 14px; border-radius: 8px; background-color: #f8fafc; border: 1px solid #e2e8f0; font-size: 0.875rem; color: #475569;">
                    Bạn có muốn chuyển giai đoạn này sang trạng thái <strong>[Ngừng sử dụng - Inactive]</strong> không? Các cơ hội hiện tại vẫn được giữ nguyên dữ liệu và tiến độ lịch sử.
                </div>
            </div>

            <footer class="crm-modal-footer">
                <button type="button" class="btn btn-secondary" id="btnCancelDealsModal">Giữ nguyên</button>
                <button type="button" class="btn btn-danger" id="btnConfirmDeactivateStage">
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
         JAVASCRIPT DOM THUẦN & FETCH API TÍCH HỢP (CRM-47)
         ================================================================= -->
    <script>
    (function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}' || '';

        // Dữ liệu 6 giai đoạn bán hàng chuẩn B2B thực tế (Kèm winProbability, exitCriteria và activeOpportunitiesCount)
        var INITIAL_STAGES = [
            {
                id: 1,
                code: 'PROSPECTING',
                name: 'Tiếp cận & Nhận diện',
                winProbability: 10,
                exitCriteria: 'Xác định được người ra quyết định (DM) và có thông tin liên hệ hợp lệ',
                sortOrder: 1,
                active: true,
                activeOpportunitiesCount: 14
            },
            {
                id: 2,
                code: 'QUALIFICATION',
                name: 'Xác định & Đánh giá nhu cầu',
                winProbability: 25,
                exitCriteria: 'Hoàn thành tối thiểu 1 cuộc gặp/cuộc gọi khảo sát nhu cầu & ngân sách',
                sortOrder: 2,
                active: true,
                activeOpportunitiesCount: 9
            },
            {
                id: 3,
                code: 'PROPOSAL',
                name: 'Đề xuất giải pháp & Demo',
                winProbability: 50,
                exitCriteria: 'Thực hiện buổi demo phần mềm và gửi tài liệu giải pháp kỹ thuật',
                sortOrder: 3,
                active: true,
                activeOpportunitiesCount: 7
            },
            {
                id: 4,
                code: 'QUOTATION',
                name: 'Gửi Báo giá & Dự thảo hợp đồng',
                winProbability: 75,
                exitCriteria: 'Đính kèm file báo giá chính thức đã được Giám đốc kinh doanh duyệt',
                sortOrder: 4,
                active: true,
                activeOpportunitiesCount: 5
            },
            {
                id: 5,
                code: 'NEGOTIATION',
                name: 'Đàm phán điều khoản & Pháp lý',
                winProbability: 90,
                exitCriteria: 'Thống nhất các điều khoản thương mại, tiến độ thanh toán và bản thảo hợp đồng',
                sortOrder: 5,
                active: true,
                activeOpportunitiesCount: 3
            },
            {
                id: 6,
                code: 'CLOSED_WON',
                name: 'Chốt hợp đồng thành công',
                winProbability: 100,
                exitCriteria: 'Hợp đồng có đủ chữ ký và dấu của 2 bên, nhận tiền đặt cọc',
                sortOrder: 6,
                active: true,
                activeOpportunitiesCount: 18
            }
        ];

        // State quản lý
        var state = {
            stages: JSON.parse(JSON.stringify(INITIAL_STAGES)),
            calcDealAmount: 100000000,
            pendingDeactivateStage: null
        };

        // DOM Elements
        var stageTableBody = document.getElementById('stageTableBody');
        var pipelineStepper = document.getElementById('pipelineStepper');
        var forecastChipsGrid = document.getElementById('forecastChipsGrid');
        var calcDealAmountInput = document.getElementById('calcDealAmount');
        var pipelineLoadingOverlay = document.getElementById('pipelineLoadingOverlay');

        var statStageCount = document.getElementById('statStageCount');
        var statActiveDeals = document.getElementById('statActiveDeals');
        var statAvgProb = document.getElementById('statAvgProb');

        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');
        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');

        // Modal Elements
        var stageModal = document.getElementById('stageModal');
        var stageForm = document.getElementById('stageForm');
        var stageModalHeading = document.getElementById('stageModalHeading');
        var btnOpenCreateStageModal = document.getElementById('btnOpenCreateStageModal');
        var btnCloseStageModal = document.getElementById('btnCloseStageModal');
        var btnCancelStageModal = document.getElementById('btnCancelStageModal');

        var formStageId = document.getElementById('formStageId');
        var formStageCode = document.getElementById('formStageCode');
        var formStageName = document.getElementById('formStageName');
        var formWinProbability = document.getElementById('formWinProbability');
        var formWinProbSlider = document.getElementById('formWinProbSlider');
        var formExitCriteria = document.getElementById('formExitCriteria');
        var formStageSortOrder = document.getElementById('formStageSortOrder');
        var formStageActive = document.getElementById('formStageActive');
        var noticeActiveDealsAlert = document.getElementById('noticeActiveDealsAlert');
        var noticeActiveDealsText = document.getElementById('noticeActiveDealsText');

        var activeDealsNoticeModal = document.getElementById('activeDealsNoticeModal');
        var activeDealsModalContent = document.getElementById('activeDealsModalContent');
        var btnCloseDealsModal = document.getElementById('btnCloseDealsModal');
        var btnCancelDealsModal = document.getElementById('btnCancelDealsModal');
        var btnConfirmDeactivateStage = document.getElementById('btnConfirmDeactivateStage');

        function formatVND(amount) {
            if (amount == null || isNaN(amount)) return '0 đ';
            return new Intl.NumberFormat('vi-VN').format(Math.round(amount)) + ' đ';
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

        // Lấy danh sách stage đã sắp xếp theo sortOrder
        function getSortedStages() {
            return state.stages.slice().sort(function (a, b) {
                return (a.sortOrder || 0) - (b.sortOrder || 0);
            });
        }

        // Cập nhật thống kê nhanh
        function updateStats() {
            var sorted = getSortedStages();
            var totalDeals = sorted.reduce(function (sum, s) { return sum + (s.activeOpportunitiesCount || 0); }, 0);
            var avgProb = sorted.length > 0 ? (sorted.reduce(function (sum, s) { return sum + (s.winProbability || 0); }, 0) / sorted.length) : 0;

            statStageCount.textContent = sorted.length;
            statActiveDeals.textContent = totalDeals;
            statAvgProb.textContent = avgProb.toFixed(1) + '%';
        }

        // Render Thanh tiến trình trực quan Pipeline (Funnel / Stepper Bar - AC 1)
        function renderPipelineStepper() {
            pipelineStepper.innerHTML = '';
            var sorted = getSortedStages();

            sorted.forEach(function (s, index) {
                var item = document.createElement('div');
                item.className = 'pipeline-step-item' + (s.winProbability === 100 ? ' step-final' : '');
                item.setAttribute('title', 'Giai đoạn: ' + s.name + '\nĐiều kiện rời giai đoạn: ' + (s.exitCriteria || 'Chưa quy định'));

                item.innerHTML =
                    '<div class="step-seq-prob">' +
                        '<span class="step-seq-num">' + (index + 1) + '</span>' +
                        '<span class="step-prob-badge">' + s.winProbability + '%</span>' +
                    '</div>' +
                    '<div class="step-name">' + escapeHtml(s.name) + '</div>' +
                    '<div class="step-deals-count">' +
                        '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg>' +
                        '<span>' + (s.activeOpportunitiesCount || 0) + ' cơ hội</span>' +
                    '</div>';

                item.addEventListener('click', function () {
                    openEditStageModal(s.id);
                });

                pipelineStepper.appendChild(item);
            });
        }

        // Render Hộp tính Doanh số dự báo (Weighted Forecast Calculator - AC 2)
        function renderForecastCalculator() {
            forecastChipsGrid.innerHTML = '';
            var sorted = getSortedStages();
            var dealAmount = parseFloat(calcDealAmountInput.value) || 0;

            sorted.forEach(function (s) {
                var weightedAmount = dealAmount * (s.winProbability / 100);
                var chip = document.createElement('div');
                chip.className = 'forecast-chip';
                chip.innerHTML =
                    '<div class="forecast-chip-title" title="' + escapeHtml(s.name) + '">' + escapeHtml(s.name) + ' (' + s.winProbability + '%)</div>' +
                    '<div class="forecast-chip-val">' + formatVND(weightedAmount) + '</div>' +
                    '<div class="forecast-chip-sub">Dự báo = ' + s.winProbability + '% giá trị</div>';
                forecastChipsGrid.appendChild(chip);
            });
        }

        // Render Bảng dữ liệu Giai đoạn
        function renderStageTable() {
            stageTableBody.innerHTML = '';
            var sorted = getSortedStages();

            if (sorted.length === 0) {
                stageTableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 40px; color: #64748b;">Chưa có giai đoạn bán hàng nào. Hãy thêm mới để thiết lập chuỗi tiến trình.</td></tr>';
                return;
            }

            sorted.forEach(function (s, index) {
                var tr = document.createElement('tr');
                if (!s.active) {
                    tr.className = 'row-inactive';
                }

                var isFirst = index === 0;
                var isLast = index === sorted.length - 1;

                // Cột % xác suất thắng (AC 2)
                var prob = s.winProbability || 0;
                var probBarColor = prob >= 90 ? '#10b981' : (prob >= 60 ? '#2563eb' : (prob >= 30 ? '#7c3aed' : '#f59e0b'));

                // Cột số cơ hội đang chạy (AC 4)
                var deals = s.activeOpportunitiesCount || 0;
                var dealsHtml = deals > 0
                    ? '<span class="deals-badge deals-in-use" title="Đang có ' + deals + ' cơ hội kinh doanh chạy trong giai đoạn này (AC 4)">' + deals + ' cơ hội</span>'
                    : '<span class="deals-badge deals-zero">0 cơ hội</span>';

                // Cột trạng thái
                var statusHtml = s.active
                    ? '<span class="badge-status badge-status-active"><span style="width:6px;height:6px;border-radius:50%;background:#10b981;display:inline-block;"></span> Kích hoạt</span>'
                    : '<span class="badge-status badge-status-inactive"><span style="width:6px;height:6px;border-radius:50%;background:#94a3b8;display:inline-block;"></span> Tạm ngừng</span>';

                tr.innerHTML =
                    '<!-- Thứ tự & nút di chuyển (AC 1) -->' +
                    '<td>' +
                        '<div class="order-control-cell">' +
                            '<span class="order-badge">' + s.sortOrder + '</span>' +
                            '<div class="order-btn-group">' +
                                '<button type="button" class="btn-order-arrow" data-action="move-up" data-id="' + s.id + '" title="Đẩy lên trước" ' + (isFirst ? 'disabled' : '') + '>▲</button>' +
                                '<button type="button" class="btn-order-arrow" data-action="move-down" data-id="' + s.id + '" title="Đẩy xuống sau" ' + (isLast ? 'disabled' : '') + '>▼</button>' +
                            '</div>' +
                        '</div>' +
                    '</td>' +
                    '<td><span class="col-code">' + escapeHtml(s.code) + '</span></td>' +
                    '<td><strong>' + escapeHtml(s.name) + '</strong></td>' +
                    '<!-- Xác suất thắng (%) kèm thanh tiến trình (AC 2) -->' +
                    '<td>' +
                        '<div class="prob-cell">' +
                            '<div class="prob-bar-track">' +
                                '<div class="prob-bar-fill" style="width:' + prob + '%; background:' + probBarColor + ';"></div>' +
                            '</div>' +
                            '<span class="prob-val-text">' + prob + '%</span>' +
                        '</div>' +
                    '</td>' +
                    '<!-- Điều kiện bắt buộc rời giai đoạn (AC 3) -->' +
                    '<td class="criteria-badge-cell">' +
                        '<span class="criteria-text" title="' + escapeHtml(s.exitCriteria || 'Chưa quy định') + '">' +
                            escapeHtml(s.exitCriteria || 'Chưa quy định') +
                        '</span>' +
                    '</td>' +
                    '<!-- Cơ hội đang chạy (AC 4) -->' +
                    '<td style="text-align: center;">' + dealsHtml + '</td>' +
                    '<td style="text-align: center;">' + statusHtml + '</td>' +
                    '<td>' +
                        '<div class="action-cell" style="justify-content: center;">' +
                            '<button type="button" class="btn-icon" data-action="edit" data-id="' + s.id + '" title="Chỉnh sửa giai đoạn">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>' +
                                    '<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>' +
                                '</svg>' +
                            '</button>' +
                            '<button type="button" class="btn-icon btn-icon-danger" data-action="delete" data-id="' + s.id + '" title="Xóa hoặc Ngừng kích hoạt (AC 4)">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<polyline points="3 6 5 6 21 6"></polyline>' +
                                    '<path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>' +
                                '</svg>' +
                            '</button>' +
                        '</div>' +
                    '</td>';

                stageTableBody.appendChild(tr);
            });
        }

        // Tải danh sách giai đoạn từ Backend (GET /api/pipeline/stages)
        async function fetchStages() {
            pipelineLoadingOverlay.style.display = 'flex';
            hideAlerts();

            var endpoint = contextPath + '/api/pipeline/stages';
            try {
                var res = await fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });

                pipelineLoadingOverlay.style.display = 'none';

                if (res.ok) {
                    var data = await res.json();
                    var list = (data && data.data && Array.isArray(data.data.items))
                        ? data.data.items
                        : (Array.isArray(data) ? data : null);

                    if (list && list.length > 0) {
                        state.stages = list;
                    }
                } else {
                    console.info('Backend /api/pipeline/stages trả mã ' + res.status + ' - Sử dụng fallback local data cho CRM-47.');
                }
            } catch (err) {
                pipelineLoadingOverlay.style.display = 'none';
                console.info('Chưa kết nối Backend Servlet - Sử dụng dữ liệu mẫu tiến trình CRM-47:', err);
            }

            updateStats();
            renderPipelineStepper();
            renderForecastCalculator();
            renderStageTable();
        }

        // Lắng nghe thay đổi giá trị Deal mẫu
        calcDealAmountInput.addEventListener('input', function () {
            renderForecastCalculator();
        });

        // Đồng bộ slider và input số xác suất thắng trong modal
        formWinProbSlider.addEventListener('input', function () {
            formWinProbability.value = formWinProbSlider.value;
        });

        formWinProbability.addEventListener('input', function () {
            var val = Math.min(100, Math.max(0, parseInt(formWinProbability.value, 10) || 0));
            formWinProbSlider.value = val;
        });

        // Hoán đổi thứ tự hiển thị bằng mũi tên Lên/Xuống (AC 1)
        async function moveStageOrder(id, direction) {
            var sorted = getSortedStages();
            var index = sorted.findIndex(function (s) { return s.id === id; });
            if (index === -1) return;

            var targetIndex = direction === 'up' ? index - 1 : index + 1;
            if (targetIndex < 0 || targetIndex >= sorted.length) return;

            var curStage = sorted[index];
            var swapStage = sorted[targetIndex];

            var tempOrder = curStage.sortOrder;
            curStage.sortOrder = swapStage.sortOrder;
            swapStage.sortOrder = tempOrder;

            var endpointCur = contextPath + '/api/pipeline/stages/' + curStage.id;
            var endpointSwap = contextPath + '/api/pipeline/stages/' + swapStage.id;
            try {
                fetch(endpointCur, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sortOrder: curStage.sortOrder })
                });
                fetch(endpointSwap, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sortOrder: swapStage.sortOrder })
                });
            } catch (e) {}

            showSuccessAlert('Đã cập nhật thứ tự tiến trình bán hàng!');
            renderPipelineStepper();
            renderForecastCalculator();
            renderStageTable();
        }

        // Mở Modal Thêm mới giai đoạn
        btnOpenCreateStageModal.addEventListener('click', function () {
            hideAlerts();
            stageForm.reset();
            formStageId.value = '';
            stageModalHeading.textContent = 'Thêm giai đoạn Pipeline mới';

            var sorted = getSortedStages();
            var nextOrder = sorted.length > 0 ? Math.max.apply(null, sorted.map(function(s){ return s.sortOrder || 0; })) + 1 : 1;
            formStageSortOrder.value = nextOrder;

            formWinProbability.value = 50;
            formWinProbSlider.value = 50;
            noticeActiveDealsAlert.style.display = 'none';

            clearFormErrors();
            stageModal.style.display = 'flex';
        });

        function closeStageModal() {
            stageModal.style.display = 'none';
            clearFormErrors();
        }

        btnCloseStageModal.addEventListener('click', closeStageModal);
        btnCancelStageModal.addEventListener('click', closeStageModal);

        function clearFormErrors() {
            document.querySelectorAll('.form-feedback').forEach(function (el) { el.textContent = ''; });
            document.querySelectorAll('.form-control').forEach(function (el) { el.classList.remove('is-invalid'); });
        }

        // Mở Modal Chỉnh sửa giai đoạn
        function openEditStageModal(id) {
            var s = state.stages.find(function (item) { return item.id === id; });
            if (!s) return;

            hideAlerts();
            clearFormErrors();
            formStageId.value = s.id;
            formStageCode.value = s.code;
            formStageName.value = s.name;
            formWinProbability.value = s.winProbability;
            formWinProbSlider.value = s.winProbability;
            formExitCriteria.value = s.exitCriteria || '';
            formStageSortOrder.value = s.sortOrder;
            formStageActive.value = s.active ? 'true' : 'false';

            // AC 4: Cảnh báo khi sửa giai đoạn có cơ hội đang chạy
            var deals = s.activeOpportunitiesCount || 0;
            if (deals > 0) {
                noticeActiveDealsAlert.style.display = 'block';
                noticeActiveDealsText.textContent = 'Giai đoạn này đang có ' + deals + ' cơ hội kinh doanh đang chạy. Khi điều chỉnh xác suất thắng, hệ thống sẽ tự động cập nhật lại Doanh số dự báo (weighted forecast) tương ứng mà không làm gián đoạn tiến trình cơ hội (AC 4).';
            } else {
                noticeActiveDealsAlert.style.display = 'none';
            }

            stageModalHeading.textContent = 'Chỉnh sửa giai đoạn: ' + s.name;
            stageModal.style.display = 'flex';
        }

        // Validate Form Giai đoạn
        function validateStageForm() {
            clearFormErrors();
            var valid = true;

            var code = formStageCode.value.trim();
            if (!code) {
                document.getElementById('feedbackStageCode').textContent = 'Vui lòng nhập mã giai đoạn.';
                formStageCode.classList.add('is-invalid');
                valid = false;
            }

            var name = formStageName.value.trim();
            if (!name) {
                document.getElementById('feedbackStageName').textContent = 'Vui lòng nhập tên giai đoạn.';
                formStageName.classList.add('is-invalid');
                valid = false;
            }

            var prob = parseInt(formWinProbability.value, 10);
            if (isNaN(prob) || prob < 0 || prob > 100) {
                document.getElementById('feedbackWinProbability').textContent = 'Xác suất thắng phải từ 0% đến 100%.';
                formWinProbability.classList.add('is-invalid');
                valid = false;
            }

            var criteria = formExitCriteria.value.trim();
            if (!criteria) {
                document.getElementById('feedbackExitCriteria').textContent = 'Vui lòng nhập điều kiện bắt buộc để rời giai đoạn (Exit Criteria - AC 3).';
                formExitCriteria.classList.add('is-invalid');
                valid = false;
            }

            var sortOrder = parseInt(formStageSortOrder.value, 10);
            if (isNaN(sortOrder) || sortOrder <= 0) {
                document.getElementById('feedbackStageSortOrder').textContent = 'Thứ tự hiển thị phải là số nguyên dương lớn hơn 0.';
                formStageSortOrder.classList.add('is-invalid');
                valid = false;
            }

            return valid;
        }

        // Submit Form (POST / PUT /api/pipeline/stages)
        stageForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            if (!validateStageForm()) return;

            var id = formStageId.value;
            var isEdit = Boolean(id);

            var payload = {
                code: formStageCode.value.trim().toUpperCase(),
                name: formStageName.value.trim(),
                winProbability: parseInt(formWinProbability.value, 10),
                exitCriteria: formExitCriteria.value.trim(),
                sortOrder: parseInt(formStageSortOrder.value, 10),
                active: formStageActive.value === 'true'
            };

            var endpoint = isEdit
                ? (contextPath + '/api/pipeline/stages/' + id)
                : (contextPath + '/api/pipeline/stages');
            var method = isEdit ? 'PUT' : 'POST';

            try {
                var res = await fetch(endpoint, {
                    method: method,
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify(payload)
                });
                if (res.ok) {
                    showSuccessAlert(isEdit ? 'Cập nhật giai đoạn thành công!' : 'Tạo mới giai đoạn thành công!');
                }
            } catch (err) {
                console.info('Backend chưa sẵn sàng - Cập nhật dữ liệu tại local state:', err);
            }

            if (isEdit) {
                var idx = state.stages.findIndex(function (s) { return s.id === Number(id); });
                if (idx !== -1) {
                    payload.id = Number(id);
                    payload.activeOpportunitiesCount = state.stages[idx].activeOpportunitiesCount;
                    state.stages[idx] = Object.assign({}, state.stages[idx], payload);
                }
                showSuccessAlert('Đã cập nhật giai đoạn [' + payload.name + '] thành công!');
            } else {
                var newId = state.stages.length > 0 ? Math.max.apply(null, state.stages.map(function(s){ return s.id; })) + 1 : 1;
                payload.id = newId;
                payload.activeOpportunitiesCount = 0;
                state.stages.push(payload);
                showSuccessAlert('Đã thêm mới giai đoạn [' + payload.name + '] vào pipeline!');
            }

            closeStageModal();
            updateStats();
            renderPipelineStepper();
            renderForecastCalculator();
            renderStageTable();
        });

        // Xử lý Xóa / Chặn xóa (AC 4: Cơ hội đang chạy không làm hỏng dữ liệu)
        function handleDeleteStage(id) {
            var s = state.stages.find(function (item) { return item.id === id; });
            if (!s) return;

            // AC 4: Nếu đang có cơ hội (activeOpportunitiesCount > 0) thì CHẶN XÓA
            if (s.activeOpportunitiesCount && s.activeOpportunitiesCount > 0) {
                state.pendingDeactivateStage = s;
                activeDealsModalContent.innerHTML =
                    'Giai đoạn <strong>[' + escapeHtml(s.code) + ' - ' + escapeHtml(s.name) + ']</strong> đang có <strong>' +
                    s.activeOpportunitiesCount + ' cơ hội kinh doanh</strong> đang chạy. Theo tiêu chuẩn nghiệm thu <strong>AC 4</strong>, hệ thống <strong style="color:#ef4444;">chặn hoàn toàn việc xóa giai đoạn</strong> để không làm hỏng tiến trình và dữ liệu các cơ hội này.';
                activeDealsNoticeModal.style.display = 'flex';
                return;
            }

            // Nếu không có cơ hội nào, cho phép xóa an toàn
            if (confirm('Giai đoạn [' + s.name + '] hiện không có cơ hội nào. Bạn có chắc chắn muốn xóa không?')) {
                deleteStageDirectly(id);
            }
        }

        async function deleteStageDirectly(id) {
            var endpoint = contextPath + '/api/pipeline/stages/' + id;
            try {
                await fetch(endpoint, { method: 'DELETE' });
            } catch (e) {}

            state.stages = state.stages.filter(function (s) { return s.id !== id; });
            showSuccessAlert('Đã xóa giai đoạn bán hàng thành công!');
            updateStats();
            renderPipelineStepper();
            renderForecastCalculator();
            renderStageTable();
        }

        // Chuyển sang Ngừng sử dụng khi có cơ hội đang chạy (AC 4)
        btnConfirmDeactivateStage.addEventListener('click', async function () {
            if (!state.pendingDeactivateStage) return;
            var s = state.pendingDeactivateStage;
            s.active = false;

            var endpoint = contextPath + '/api/pipeline/stages/' + s.id;
            try {
                await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ active: false })
                });
            } catch (e) {}

            activeDealsNoticeModal.style.display = 'none';
            state.pendingDeactivateStage = null;
            showSuccessAlert('Đã chuyển giai đoạn [' + s.name + '] sang trạng thái [Ngừng kích hoạt] theo tiêu chuẩn AC 4!');
            renderPipelineStepper();
            renderStageTable();
        });

        btnCloseDealsModal.addEventListener('click', function () {
            activeDealsNoticeModal.style.display = 'none';
            state.pendingDeactivateStage = null;
        });
        btnCancelDealsModal.addEventListener('click', function () {
            activeDealsNoticeModal.style.display = 'none';
            state.pendingDeactivateStage = null;
        });

        // Bắt sự kiện bảng
        stageTableBody.addEventListener('click', function (e) {
            var btn = e.target.closest('[data-action]');
            if (!btn) return;
            var action = btn.getAttribute('data-action');
            var id = Number(btn.getAttribute('data-id'));

            if (action === 'edit') {
                openEditStageModal(id);
            } else if (action === 'delete') {
                handleDeleteStage(id);
            } else if (action === 'move-up') {
                moveStageOrder(id, 'up');
            } else if (action === 'move-down') {
                moveStageOrder(id, 'down');
            }
        });

        // Khởi động
        fetchStages();

    })();
    </script>
</body>
</html>
