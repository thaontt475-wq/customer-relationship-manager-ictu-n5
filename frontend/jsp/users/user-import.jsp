<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhập người dùng hàng loạt từ Excel - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Import Excel (CRM-32 / S2-01) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/user-import.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Nhập người dùng từ Excel -->
        <main class="import-page" id="importApp" role="main">
            <div class="import-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="import-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="${pageContext.request.contextPath}/users">Quản lý người dùng</a>
                    <span class="separator">/</span>
                    <span class="active">Nhập hàng loạt từ Excel</span>
                </nav>

                <!-- Header màn hình -->
                <header class="import-header">
                    <div class="import-header-info">
                        <h1>Nhập danh sách người dùng từ tệp Excel</h1>
                        <p>Tạo tài khoản người dùng hàng loạt cho khối kinh doanh từ bảng tính Excel (.xlsx hoặc .csv).</p>
                    </div>

                    <div class="import-header-badges">
                        <span class="import-badge-feature" title="User Story CRM-32 / S2-01">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="16" y1="13" x2="8" y2="13"></line>
                                <line x1="16" y1="17" x2="8" y2="17"></line>
                                <polyline points="10 9 9 9 8 9"></polyline>
                            </svg>
                            S2-01 / CRM-32
                        </span>
                    </div>
                </header>

                <!-- Stepper 3 bước trực quan -->
                <nav class="import-stepper" aria-label="Các bước thực hiện">
                    <div class="import-step active" id="stepIndicator1">
                        <div class="import-step-circle" id="stepCircle1">1</div>
                        <div class="import-step-info">
                            <span class="import-step-title">Tải lên tệp Excel</span>
                            <span class="import-step-desc">Tải mẫu & chọn tệp</span>
                        </div>
                    </div>

                    <div class="import-step-divider"></div>

                    <div class="import-step" id="stepIndicator2">
                        <div class="import-step-circle" id="stepCircle2">2</div>
                        <div class="import-step-info">
                            <span class="import-step-title">Kiểm tra & Xem trước</span>
                            <span class="import-step-desc">Báo lỗi chi tiết từng dòng</span>
                        </div>
                    </div>

                    <div class="import-step-divider"></div>

                    <div class="import-step" id="stepIndicator3">
                        <div class="import-step-circle" id="stepCircle3">3</div>
                        <div class="import-step-info">
                            <span class="import-step-title">Hoàn tất & Báo cáo</span>
                            <span class="import-step-desc">Tạo tài khoản thành công</span>
                        </div>
                    </div>
                </nav>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts) -->
                <div id="importAlertArea" aria-live="polite">
                    <div class="import-alert import-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="import-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div style="flex-grow: 1;">
                            <strong>Thông báo lỗi: </strong>
                            <span id="globalErrorMessage"></span>
                        </div>
                        <button type="button" class="import-btn-secondary" style="padding: 2px 8px; border: none; background: transparent; cursor: pointer;" onclick="document.getElementById('globalErrorAlert').style.display='none';">&times;</button>
                    </div>

                    <div class="import-alert import-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="import-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div style="flex-grow: 1;">
                            <strong>Thành công: </strong>
                            <span id="globalSuccessMessage"></span>
                        </div>
                        <button type="button" class="import-btn-secondary" style="padding: 2px 8px; border: none; background: transparent; cursor: pointer;" onclick="document.getElementById('globalSuccessAlert').style.display='none';">&times;</button>
                    </div>
                </div>

                <!-- BƯỚC 1: TẢI LÊN TỆP EXCEL -->
                <section class="import-card" id="step1Section">
                    <h2 class="import-card-title">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                            <polyline points="17 8 12 3 7 8"></polyline>
                            <line x1="12" y1="3" x2="12" y2="15"></line>
                        </svg>
                        Bước 1: Tải mẫu và chọn tệp dữ liệu
                    </h2>

                    <!-- Khối tải tệp mẫu Excel / CSV (AC 1) -->
                    <div class="import-template-box">
                        <div class="import-template-info">
                            <div class="import-excel-icon" aria-hidden="true">X</div>
                            <div class="import-template-text">
                                <h4>Tải tệp mẫu chuẩn hệ thống CRM</h4>
                                <p>Sử dụng tệp mẫu để đảm bảo đúng cấu trúc cột: Họ và tên, Email, Tên đăng nhập, Số điện thoại, Vai trò, Nhóm kinh doanh.</p>
                            </div>
                        </div>

                        <div class="import-template-actions">
                            <a href="${pageContext.request.contextPath}/api/users/import/template?format=xlsx" class="import-btn import-btn-excel" id="btnDownloadTemplateXlsx" download="mau_nhap_nguoi_dung.xlsx">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                    <polyline points="7 10 12 15 17 10"></polyline>
                                    <line x1="12" y1="15" x2="12" y2="3"></line>
                                </svg>
                                Tải tệp mẫu Excel (.xlsx)
                            </a>
                            <a href="${pageContext.request.contextPath}/api/users/import/template?format=csv" class="import-btn import-btn-secondary" id="btnDownloadTemplateCsv" download="mau_nhap_nguoi_dung.csv">
                                Tải mẫu CSV (.csv)
                            </a>
                        </div>
                    </div>

                    <!-- Khu vực Drag & Drop tải tệp lên -->
                    <form id="uploadForm">
                        <input type="file" id="fileInput" class="import-file-input" accept=".xlsx, .xls, .csv">
                        <div class="import-dropzone" id="dropzone" tabindex="0" role="button" aria-label="Khu vực kéo thả tệp Excel">
                            <svg class="import-dropzone-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="12" y1="18" x2="12" y2="12"></line>
                                <line x1="9" y1="15" x2="15" y2="15"></line>
                            </svg>
                            <div class="import-dropzone-text">Kéo thả tệp Excel vào đây hoặc bấm để chọn tệp</div>
                            <div class="import-dropzone-hint">Hỗ trợ định dạng: .xlsx, .xls, .csv (Kích thước tối đa 10 MB)</div>

                            <div id="fileSelectedDisplay" style="display: none;">
                                <div class="import-selected-file">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="20 6 9 17 4 12"></polyline>
                                    </svg>
                                    <span id="selectedFileName">ten_tep.xlsx</span>
                                    <span id="selectedFileSize" style="color: var(--import-text-muted); font-size: 12px; margin-left: 4px;">(0 KB)</span>
                                </div>
                            </div>
                        </div>

                        <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px;">
                            <button type="button" class="import-btn import-btn-secondary" id="btnClearFile" style="display: none;">
                                Chọn lại tệp
                            </button>
                            <button type="button" class="import-btn import-btn-primary" id="btnUploadPreview" disabled>
                                <span class="import-spinner" id="previewSpinner" style="display: none;"></span>
                                <span id="previewBtnText">Kiểm tra & Xem trước</span>
                            </button>
                        </div>
                    </form>
                </section>

                <!-- BƯỚC 2: XEM TRƯỚC & KIỂM TRA LỖI (PREVIEW & VALIDATION) -->
                <section class="import-card" id="step2Section" style="display: none;">
                    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                        <h2 class="import-card-title">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                            Bước 2: Kết quả xem trước dữ liệu
                        </h2>
                        <span style="font-size: 13px; color: var(--import-text-muted);" id="previewFileNameDisplay"></span>
                    </div>

                    <!-- Thống kê tổng hợp (AC 2 & AC 3) -->
                    <div class="import-stats-grid">
                        <div class="import-stat-card total">
                            <span class="import-stat-label">Tổng số dòng đọc được</span>
                            <span class="import-stat-value" id="statTotalRows">0</span>
                        </div>
                        <div class="import-stat-card valid">
                            <span class="import-stat-label">Dòng hợp lệ (Sẵn sàng tạo)</span>
                            <span class="import-stat-value" id="statValidRows">0</span>
                        </div>
                        <div class="import-stat-card error">
                            <span class="import-stat-label">Dòng lỗi (Sẽ tự động bỏ qua)</span>
                            <span class="import-stat-value" id="statErrorRows">0</span>
                        </div>
                    </div>

                    <!-- Thông báo chỉ dẫn AC 3 -->
                    <div class="import-alert import-alert-success" style="padding: 12px 16px;">
                        <svg class="import-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="16" x2="12" y2="12"></line>
                            <line x1="12" y1="8" x2="12.01" y2="8"></line>
                        </svg>
                        <div>
                            <strong>Cơ chế xử lý linh hoạt:</strong> Các dòng bị lỗi sẽ tự động được bỏ qua, hệ thống sẽ tiến hành nhập toàn bộ các dòng hợp lệ. Mật khẩu khởi tạo tạm thời sẽ được tự sinh và gửi tới email của người dùng.
                        </div>
                    </div>

                    <!-- Thanh lọc dữ liệu xem trước -->
                    <div class="import-filter-bar">
                        <div class="import-filter-pills">
                            <button type="button" class="import-filter-pill active" id="filterAllBtn">Tất cả (<span id="countFilterAll">0</span>)</button>
                            <button type="button" class="import-filter-pill" id="filterValidBtn">Hợp lệ (<span id="countFilterValid">0</span>)</button>
                            <button type="button" class="import-filter-pill" id="filterErrorBtn">Bị lỗi (<span id="countFilterError">0</span>)</button>
                        </div>
                        <span style="font-size: 13px; color: var(--import-text-muted);" id="tableDisplayCount"></span>
                    </div>

                    <!-- Bảng chi tiết từng dòng (AC 2) -->
                    <div class="import-table-wrap">
                        <table class="import-table" id="previewTable" aria-label="Bảng xem trước dữ liệu Excel">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 60px;">Dòng</th>
                                    <th scope="col">Họ và tên</th>
                                    <th scope="col">Địa chỉ Email</th>
                                    <th scope="col">Tên đăng nhập</th>
                                    <th scope="col">Số điện thoại</th>
                                    <th scope="col">Vai trò</th>
                                    <th scope="col">Nhóm kinh doanh</th>
                                    <th scope="col" style="width: 200px;">Trạng thái & Ghi chú lỗi</th>
                                </tr>
                            </thead>
                            <tbody id="previewTableBody">
                                <!-- Dữ liệu xem trước render bằng JavaScript -->
                            </tbody>
                        </table>
                    </div>

                    <!-- Nút thao tác xác nhận -->
                    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-top: 10px;">
                        <button type="button" class="import-btn import-btn-secondary" id="btnBackToUpload">
                            &larr; Chọn tệp khác
                        </button>

                        <button type="button" class="import-btn import-btn-success" id="btnConfirmImport">
                            <span class="import-spinner" id="confirmSpinner" style="display: none;"></span>
                            <span id="confirmBtnText">Xác nhận nhập danh sách người dùng</span>
                        </button>
                    </div>
                </section>

                <!-- BƯỚC 3: BÁO CÁO KẾT QUẢ IMPORT (AC 3) -->
                <section class="import-card" id="step3Section" style="display: none;">
                    <div class="import-report-box">
                        <div class="import-report-icon" aria-hidden="true">&#10003;</div>
                        <h2 class="import-report-title">Nhập danh sách người dùng thành công!</h2>
                        <p class="import-report-desc">
                            Hệ thống đã hoàn tất tạo tài khoản cho các nhân sự hợp lệ. Thông tin đăng nhập ban đầu và liên kết kích hoạt đã được gửi qua email.
                        </p>

                        <!-- Thống kê kết quả thực tế -->
                        <div class="import-stats-grid" style="width: 100%; max-width: 600px; margin: 16px 0;">
                            <div class="import-stat-card success">
                                <span class="import-stat-label">Tài khoản tạo thành công</span>
                                <span class="import-stat-value" id="reportSuccessCount">0</span>
                            </div>
                            <div class="import-stat-card error">
                                <span class="import-stat-label">Dòng bỏ qua do lỗi</span>
                                <span class="import-stat-value" id="reportSkippedCount">0</span>
                            </div>
                        </div>

                        <!-- Danh sách tài khoản đã tạo -->
                        <div style="width: 100%; text-align: left;" id="reportTableWrap">
                            <h3 style="font-size: 15px; margin-bottom: 8px;">Danh sách tài khoản vừa tạo:</h3>
                            <div class="import-table-wrap" style="max-height: 260px;">
                                <table class="import-table" id="reportTable">
                                    <thead>
                                        <tr>
                                            <th>#</th>
                                            <th>Họ và tên</th>
                                            <th>Email</th>
                                            <th>Tên đăng nhập</th>
                                            <th>Vai trò</th>
                                            <th>Trạng thái</th>
                                        </tr>
                                    </thead>
                                    <tbody id="reportTableBody">
                                        <!-- Danh sách người dùng tạo thành công -->
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <div style="display: flex; gap: 12px; margin-top: 14px; flex-wrap: wrap;">
                            <button type="button" class="import-btn import-btn-secondary" id="btnRestartProcess">
                                Nhập thêm tệp khác
                            </button>
                            <a href="${pageContext.request.contextPath}/users" class="import-btn import-btn-primary">
                                Về danh sách người dùng &rarr;
                            </a>
                        </div>
                    </div>
                </section>

            </div>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- Script điều khiển Frontend tương tác JavaScript DOM Fetch API thuần -->
    <script>
    document.addEventListener('DOMContentLoaded', function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}';

        // State quản lý quá trình Import
        var state = {
            file: null,
            batchToken: null,
            totalRows: 0,
            validRows: [],
            errorRows: [],
            allRows: [],
            activeFilter: 'all', // 'all' | 'valid' | 'error'
            report: null
        };

        // DOM Elements - Stepper
        var stepIndicator1 = document.getElementById('stepIndicator1');
        var stepIndicator2 = document.getElementById('stepIndicator2');
        var stepIndicator3 = document.getElementById('stepIndicator3');
        var stepCircle1 = document.getElementById('stepCircle1');
        var stepCircle2 = document.getElementById('stepCircle2');
        var stepCircle3 = document.getElementById('stepCircle3');

        // DOM Elements - Sections
        var step1Section = document.getElementById('step1Section');
        var step2Section = document.getElementById('step2Section');
        var step3Section = document.getElementById('step3Section');

        // DOM Elements - Alerts
        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');
        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');

        // DOM Elements - Step 1: Upload
        var dropzone = document.getElementById('dropzone');
        var fileInput = document.getElementById('fileInput');
        var fileSelectedDisplay = document.getElementById('fileSelectedDisplay');
        var selectedFileName = document.getElementById('selectedFileName');
        var selectedFileSize = document.getElementById('selectedFileSize');
        var btnClearFile = document.getElementById('btnClearFile');
        var btnUploadPreview = document.getElementById('btnUploadPreview');
        var previewSpinner = document.getElementById('previewSpinner');
        var previewBtnText = document.getElementById('previewBtnText');

        // DOM Elements - Step 2: Preview
        var previewFileNameDisplay = document.getElementById('previewFileNameDisplay');
        var statTotalRows = document.getElementById('statTotalRows');
        var statValidRows = document.getElementById('statValidRows');
        var statErrorRows = document.getElementById('statErrorRows');
        var countFilterAll = document.getElementById('countFilterAll');
        var countFilterValid = document.getElementById('countFilterValid');
        var countFilterError = document.getElementById('countFilterError');
        var filterAllBtn = document.getElementById('filterAllBtn');
        var filterValidBtn = document.getElementById('filterValidBtn');
        var filterErrorBtn = document.getElementById('filterErrorBtn');
        var tableDisplayCount = document.getElementById('tableDisplayCount');
        var previewTableBody = document.getElementById('previewTableBody');
        var btnBackToUpload = document.getElementById('btnBackToUpload');
        var btnConfirmImport = document.getElementById('btnConfirmImport');
        var confirmSpinner = document.getElementById('confirmSpinner');
        var confirmBtnText = document.getElementById('confirmBtnText');

        // DOM Elements - Step 3: Report
        var reportSuccessCount = document.getElementById('reportSuccessCount');
        var reportSkippedCount = document.getElementById('reportSkippedCount');
        var reportTableBody = document.getElementById('reportTableBody');
        var btnRestartProcess = document.getElementById('btnRestartProcess');

        // Helper: Hiển thị thông báo
        function showError(msg) {
            globalErrorMessage.textContent = msg;
            globalErrorAlert.style.display = 'flex';
            globalSuccessAlert.style.display = 'none';
        }

        function showSuccess(msg) {
            globalSuccessMessage.textContent = msg;
            globalSuccessAlert.style.display = 'flex';
            globalErrorAlert.style.display = 'none';
        }

        function hideAlerts() {
            globalErrorAlert.style.display = 'none';
            globalSuccessAlert.style.display = 'none';
        }

        function formatBytes(bytes) {
            if (bytes === 0) return '0 Bytes';
            var k = 1024;
            var sizes = ['Bytes', 'KB', 'MB', 'GB'];
            var i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        }

        function escapeHtml(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        // Cập nhật Stepper
        function setStepperStep(step) {
            stepIndicator1.className = 'import-step' + (step >= 1 ? ' active' : '') + (step > 1 ? ' completed' : '');
            stepIndicator2.className = 'import-step' + (step >= 2 ? ' active' : '') + (step > 2 ? ' completed' : '');
            stepIndicator3.className = 'import-step' + (step >= 3 ? ' active' : '') + (step > 3 ? ' completed' : '');

            stepCircle1.textContent = step > 1 ? '✓' : '1';
            stepCircle2.textContent = step > 2 ? '✓' : '2';
            stepCircle3.textContent = step >= 3 ? '✓' : '3';

            step1Section.style.display = (step === 1) ? 'flex' : 'none';
            step2Section.style.display = (step === 2) ? 'flex' : 'none';
            step3Section.style.display = (step === 3) ? 'flex' : 'none';

            window.scrollTo({ top: 0, behavior: 'smooth' });
        }

        // 1. Quản lý việc chọn tệp & Drag/Drop
        function handleFileSelect(file) {
            hideAlerts();
            if (!file) return;

            var fileName = file.name || '';
            var extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();

            if (extension !== '.xlsx' && extension !== '.xls' && extension !== '.csv') {
                showError('Định dạng tệp không được hỗ trợ. Vui lòng chọn tệp bảng tính có đuôi .xlsx, .xls hoặc .csv');
                return;
            }

            if (file.size > 10 * 1024 * 1024) {
                showError('Dung lượng tệp vượt quá giới hạn 10 MB cho phép.');
                return;
            }

            state.file = file;
            selectedFileName.textContent = fileName;
            selectedFileSize.textContent = '(' + formatBytes(file.size) + ')';
            fileSelectedDisplay.style.display = 'block';
            btnClearFile.style.display = 'inline-flex';
            btnUploadPreview.disabled = false;
        }

        function clearSelectedFile() {
            state.file = null;
            fileInput.value = '';
            fileSelectedDisplay.style.display = 'none';
            btnClearFile.style.display = 'none';
            btnUploadPreview.disabled = true;
            hideAlerts();
        }

        dropzone.addEventListener('click', function () {
            fileInput.click();
        });

        dropzone.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                fileInput.click();
            }
        });

        fileInput.addEventListener('change', function () {
            if (this.files && this.files.length > 0) {
                handleFileSelect(this.files[0]);
            }
        });

        btnClearFile.addEventListener('click', function (e) {
            e.stopPropagation();
            clearSelectedFile();
        });

        // Kéo thả (Drag & Drop)
        ['dragenter', 'dragover'].forEach(function (eventName) {
            dropzone.addEventListener(eventName, function (e) {
                e.preventDefault();
                e.stopPropagation();
                dropzone.classList.add('dragover');
            });
        });

        ['dragleave', 'drop'].forEach(function (eventName) {
            dropzone.addEventListener(eventName, function (e) {
                e.preventDefault();
                e.stopPropagation();
                dropzone.classList.remove('dragover');
            });
        });

        dropzone.addEventListener('drop', function (e) {
            var dt = e.dataTransfer;
            if (dt && dt.files && dt.files.length > 0) {
                handleFileSelect(dt.files[0]);
            }
        });

        // 2. Gửi tệp để Kiểm tra & Xem trước (POST /api/users/import/preview)
        btnUploadPreview.addEventListener('click', async function () {
            if (!state.file) {
                showError('Vui lòng chọn tệp Excel trước khi kiểm tra.');
                return;
            }

            hideAlerts();
            btnUploadPreview.disabled = true;
            previewSpinner.style.display = 'inline-block';
            previewBtnText.textContent = 'Đang đọc và kiểm tra...';

            var formData = new FormData();
            formData.append('file', state.file);

            try {
                // Thử gọi endpoint chuẩn REST API
                var response = await fetch(contextPath + '/api/users/import/preview', {
                    method: 'POST',
                    body: formData,
                    headers: { 'Accept': 'application/json' }
                });

                // Nếu API trả về 404 thì thử endpoint /users/import/preview
                if (response.status === 404) {
                    response = await fetch(contextPath + '/users/import/preview', {
                        method: 'POST',
                        body: formData,
                        headers: { 'Accept': 'application/json' }
                    });
                }

                if (response.ok) {
                    var resData = await response.json();
                    handlePreviewSuccess(resData);
                } else {
                    var errText = 'Lỗi máy chủ khi đọc tệp Excel (Mã: ' + response.status + ').';
                    try {
                        var errJson = await response.json();
                        if (errJson.message) errText = errJson.message;
                    } catch (ignored) {}
                    showError(errText);
                }

            } catch (err) {
                console.error('Lỗi khi tải file preview:', err);
                // Cơ chế tự động fallback thông minh nếu backend chưa merge để hỗ trợ QA test giao diện
                handleClientSidePreviewFallback(state.file);
            } finally {
                btnUploadPreview.disabled = false;
                previewSpinner.style.display = 'none';
                previewBtnText.textContent = 'Kiểm tra & Xem trước';
            }
        });

        // Xử lý dữ liệu Preview nhận từ Server
        function handlePreviewSuccess(resData) {
            var data = (resData && resData.data) ? resData.data : resData;
            var validRows = Array.isArray(data.validRows) ? data.validRows : [];
            var errorRows = Array.isArray(data.errorRows) ? data.errorRows : [];

            // Nếu backend trả theo UserImportResult structure
            if (data.rows && Array.isArray(data.rows)) {
                validRows = data.rows.filter(function (r) { return r.valid; });
                errorRows = data.rows.filter(function (r) { return !r.valid; });
            }

            state.batchToken = data.batchToken || ('token_' + Date.now());
            state.validRows = validRows;
            state.errorRows = errorRows;

            // Gắn cờ trạng thái cho từng row để render thống nhất
            var normalizedAll = [];
            validRows.forEach(function (r, idx) {
                r.isValid = true;
                r.rowNum = r.rowNumber || r.rowNum || (idx + 2);
                normalizedAll.push(r);
            });
            errorRows.forEach(function (r, idx) {
                r.isValid = false;
                r.rowNum = r.rowNumber || r.rowNum || (validRows.length + idx + 2);
                normalizedAll.push(r);
            });

            // Sắp xếp theo thứ tự dòng
            normalizedAll.sort(function (a, b) { return a.rowNum - b.rowNum; });
            state.allRows = normalizedAll;
            state.totalRows = normalizedAll.length;

            renderPreviewSection();
            setStepperStep(2);
        }

        // Fallback đọc thử file CSV / giả lập xem trước khi backend chưa kết nối
        function handleClientSidePreviewFallback(file) {
            var reader = new FileReader();
            reader.onload = function (e) {
                var content = e.target.result;
                var lines = content.split(/\r?\n/).filter(function (l) { return l.trim().length > 0; });

                if (lines.length <= 1) {
                    showError('Tệp không có dữ liệu để kiểm tra (chỉ có dòng tiêu đề hoặc tệp rỗng).');
                    return;
                }

                var valid = [];
                var errors = [];

                for (var i = 1; i < lines.length; i++) {
                    var cols = lines[i].split(',').map(function (c) { return c.replace(/^"|"$/g, '').trim(); });
                    var rowObj = {
                        rowNum: i + 1,
                        fullName: cols[0] || '',
                        email: cols[1] || '',
                        username: cols[2] || '',
                        phone: cols[3] || '',
                        role: cols[4] || 'Sales Rep',
                        team: cols[5] || 'Nhóm kinh doanh 1',
                        errors: []
                    };

                    // Kiểm tra validation
                    if (!rowObj.fullName) rowObj.errors.push('Thiếu họ và tên');
                    if (!rowObj.email) {
                        rowObj.errors.push('Thiếu địa chỉ email');
                    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(rowObj.email)) {
                        rowObj.errors.push('Email không đúng định dạng');
                    }

                    if (rowObj.errors.length === 0) {
                        rowObj.isValid = true;
                        valid.push(rowObj);
                    } else {
                        rowObj.isValid = false;
                        errors.push(rowObj);
                    }
                }

                handlePreviewSuccess({
                    validRows: valid,
                    errorRows: errors,
                    batchToken: 'client_token_' + Date.now()
                });
            };
            reader.readAsText(file);
        }

        // 3. Render bảng Preview (Bước 2)
        function renderPreviewSection() {
            previewFileNameDisplay.textContent = 'Tệp: ' + (state.file ? state.file.name : '');
            statTotalRows.textContent = state.totalRows;
            statValidRows.textContent = state.validRows.length;
            statErrorRows.textContent = state.errorRows.length;

            countFilterAll.textContent = state.totalRows;
            countFilterValid.textContent = state.validRows.length;
            countFilterError.textContent = state.errorRows.length;

            // Xử lý nút Confirm (Vô hiệu hóa nếu validCount === 0 theo AC 3)
            var validCount = state.validRows.length;
            if (validCount === 0) {
                btnConfirmImport.disabled = true;
                confirmBtnText.textContent = 'Không có dòng hợp lệ để nhập';
            } else {
                btnConfirmImport.disabled = false;
                confirmBtnText.textContent = 'Xác nhận nhập ' + validCount + ' người dùng hợp lệ';
            }

            renderPreviewTableRows();
        }

        function renderPreviewTableRows() {
            previewTableBody.innerHTML = '';

            var rowsToDisplay = state.allRows;
            if (state.activeFilter === 'valid') {
                rowsToDisplay = state.allRows.filter(function (r) { return r.isValid; });
            } else if (state.activeFilter === 'error') {
                rowsToDisplay = state.allRows.filter(function (r) { return !r.isValid; });
            }

            tableDisplayCount.textContent = 'Đang hiển thị ' + rowsToDisplay.length + ' / ' + state.totalRows + ' dòng';

            if (rowsToDisplay.length === 0) {
                var emptyTr = document.createElement('tr');
                emptyTr.innerHTML = '<td colspan="8" style="text-align: center; padding: 24px; color: var(--import-text-muted);">Không có dòng dữ liệu nào phù hợp với bộ lọc hiện tại.</td>';
                previewTableBody.appendChild(emptyTr);
                return;
            }

            rowsToDisplay.forEach(function (row) {
                var tr = document.createElement('tr');
                tr.className = row.isValid ? 'row-valid' : 'row-error';

                var statusHtml = '';
                if (row.isValid) {
                    statusHtml = '<span class="import-tag import-tag-success">&#10003; Hợp lệ</span>';
                } else {
                    statusHtml = '<span class="import-tag import-tag-danger">&#10007; Lỗi</span>';
                    var errArr = row.errors || row.errorMessages || (row.errorMessage ? [row.errorMessage] : ['Dữ liệu không hợp lệ']);
                    if (Array.isArray(errArr) && errArr.length > 0) {
                        statusHtml += '<ul class="import-error-list">';
                        errArr.forEach(function (msg) {
                            statusHtml += '<li>' + escapeHtml(msg) + '</li>';
                        });
                        statusHtml += '</ul>';
                    }
                }

                tr.innerHTML =
                    '<td><strong>#' + escapeHtml(row.rowNum) + '</strong></td>' +
                    '<td>' + escapeHtml(row.fullName || row.name || '-') + '</td>' +
                    '<td><code>' + escapeHtml(row.email || '-') + '</code></td>' +
                    '<td>' + escapeHtml(row.username || '-') + '</td>' +
                    '<td>' + escapeHtml(row.phone || '-') + '</td>' +
                    '<td>' + escapeHtml(row.role || row.roleName || 'Sales Rep') + '</td>' +
                    '<td>' + escapeHtml(row.team || row.teamName || 'Chưa phân nhóm') + '</td>' +
                    '<td>' + statusHtml + '</td>';

                previewTableBody.appendChild(tr);
            });
        }

        // Bộ lọc bảng Preview
        filterAllBtn.addEventListener('click', function () {
            state.activeFilter = 'all';
            filterAllBtn.classList.add('active');
            filterValidBtn.classList.remove('active');
            filterErrorBtn.classList.remove('active');
            renderPreviewTableRows();
        });

        filterValidBtn.addEventListener('click', function () {
            state.activeFilter = 'valid';
            filterValidBtn.classList.add('active');
            filterAllBtn.classList.remove('active');
            filterErrorBtn.classList.remove('active');
            renderPreviewTableRows();
        });

        filterErrorBtn.addEventListener('click', function () {
            state.activeFilter = 'error';
            filterErrorBtn.classList.add('active');
            filterAllBtn.classList.remove('active');
            filterValidBtn.classList.remove('active');
            renderPreviewTableRows();
        });

        btnBackToUpload.addEventListener('click', function () {
            setStepperStep(1);
        });

        // 4. Xác nhận nhập danh sách người dùng (POST /api/users/import/confirm)
        btnConfirmImport.addEventListener('click', async function () {
            if (state.validRows.length === 0) {
                showError('Không có dòng dữ liệu hợp lệ nào để nhập vào hệ thống.');
                return;
            }

            hideAlerts();
            btnConfirmImport.disabled = true;
            btnBackToUpload.disabled = true;
            confirmSpinner.style.display = 'inline-block';
            confirmBtnText.textContent = 'Đang tiến hành tạo tài khoản...';

            var payload = { batchToken: state.batchToken };

            try {
                var response = await fetch(contextPath + '/api/users/import/confirm', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                // Chỉ dùng endpoint cũ để tương thích với backend chưa migrate.
                // Kết quả fallback vẫn phải là phản hồi thành công thực từ server.
                if (response.status === 404) {
                    response = await fetch(contextPath + '/users/import/execute', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify(payload)
                    });
                }

                var resJson = null;
                try {
                    resJson = await response.json();
                } catch (parseError) {
                    console.error('Phản hồi confirm import không phải JSON hợp lệ:', parseError);
                }

                var apiReportedFailure = resJson && resJson.success === false;
                if (response.ok) {
                    if (apiReportedFailure) {
                        var rejectedMessage = resJson.message || 'Máy chủ từ chối xác nhận nhập người dùng.';
                        showError(rejectedMessage + ' Dữ liệu chưa được lưu vào hệ thống.');
                    } else if (resJson) {
                        handleConfirmSuccess(resJson);
                    } else {
                        showError('Máy chủ không trả về kết quả xác nhận hợp lệ. Dữ liệu chưa được lưu vào hệ thống.');
                    }
                } else {
                    var errorMessage = resJson && resJson.message
                        ? resJson.message
                        : 'Không thể xác nhận nhập người dùng (Mã lỗi: ' + response.status + ').';
                    showError(errorMessage + ' Dữ liệu chưa được lưu vào hệ thống.');
                }

            } catch (err) {
                console.error('Lỗi khi xác nhận import:', err);
                showError('Không thể kết nối tới máy chủ để xác nhận nhập người dùng. Dữ liệu chưa được lưu vào hệ thống.');
            } finally {
                btnConfirmImport.disabled = false;
                btnBackToUpload.disabled = false;
                confirmSpinner.style.display = 'none';
            }
        });

        // 5. Xử lý Hoàn tất & Báo cáo kết quả (Bước 3)
        function handleConfirmSuccess(data) {
            var report = (data && data.data) ? data.data : data;
            var createdCount = report.created != null ? report.created : (report.successCount || state.validRows.length);
            var skippedCount = report.skipped != null ? report.skipped : (report.errorCount || state.errorRows.length);
            var createdItems = report.items || state.validRows;

            reportSuccessCount.textContent = createdCount;
            reportSkippedCount.textContent = skippedCount;

            reportTableBody.innerHTML = '';
            if (Array.isArray(createdItems) && createdItems.length > 0) {
                createdItems.forEach(function (u, idx) {
                    var tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td>' + (idx + 1) + '</td>' +
                        '<td><strong>' + escapeHtml(u.fullName || u.name) + '</strong></td>' +
                        '<td><code>' + escapeHtml(u.email) + '</code></td>' +
                        '<td>' + escapeHtml(u.username || '-') + '</td>' +
                        '<td>' + escapeHtml(u.role || u.roleName || 'Sales Rep') + '</td>' +
                        '<td><span class="import-tag import-tag-success">&#10003; Đã tạo</span></td>';
                    reportTableBody.appendChild(tr);
                });
            }

            setStepperStep(3);
            showSuccess('Đã nhập thành công ' + createdCount + ' tài khoản người dùng vào hệ thống.');
        }

        btnRestartProcess.addEventListener('click', function () {
            clearSelectedFile();
            setStepperStep(1);
        });

    });
    </script>
</body>
</html>
