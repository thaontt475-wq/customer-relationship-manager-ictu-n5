<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ==========================================================================
  FEATURE S2-09: Cấu hình Pipeline bán hàng & Tỷ lệ thắng (Pipeline Stages & Win Probabilities)
  Phụ trách Frontend/View: Tiệp
  
  BE CONTRACT NEEDED:
    1. GET /pipeline/stages?status={status}
       - Lấy danh sách các giai đoạn cơ hội kèm tỷ lệ thắng và điều kiện chuyển giai đoạn
       - Response dự kiến:
         {
           "stages": [
             {
               "id": 1,
               "code": "QUALIFICATION",
               "name": "Qualification",
               "winProbability": 25,
               "displayOrder": 2,
               "status": "ACTIVE",
               "opportunityCount": 18,
               "exitConditions": [
                 { "id": 101, "name": "Đã xác định nhu cầu", "required": true },
                 { "id": 102, "name": "Đã xác định người quyết định", "required": true }
               ]
             }
           ]
         }

    2. POST /pipeline/stages
       - Tạo mới Stage (code unique, winProbability 0-100, displayOrder, status)

    3. PUT /pipeline/stages/{id}
       - Cập nhật Stage (name, winProbability, displayOrder, status, exitConditions)

    4. POST /pipeline/stages/{id}/status
       - Bật/tắt trạng thái Stage (ACTIVE / INACTIVE)

    5. PUT /pipeline/stages/reorder
       - Lưu thứ tự hiển thị displayOrder hàng loạt

    6. GET /pipeline/stages/{id}/exit-conditions
       - Lấy danh sách điều kiện thoát của Stage

    7. PUT /pipeline/stages/{id}/exit-conditions
       - Cập nhật danh sách điều kiện thoát (tên, required)

    8. GET /pipeline/stages/{id}/usage
       - Trả về số Opportunity đang sử dụng Stage
  ==========================================================================
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu hình Pipeline bán hàng - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Pipeline (S2-09) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/pipeline/pipeline.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Cấu hình Pipeline -->
        <main class="pipe-page" id="pipelineApp" role="main">
            <div class="pipe-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="pipe-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Pipeline</span>
                </nav>

                <!-- Header màn hình -->
                <header class="pipe-header">
                    <div class="pipe-header-info">
                        <h1>Cấu hình Pipeline bán hàng</h1>
                        <p>Quản lý các giai đoạn cơ hội, tỷ lệ thắng và điều kiện chuyển giai đoạn.</p>
                    </div>
                    <div class="pipe-header-badges">
                        <span class="pipe-badge-tag">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                            </svg>
                            S2-09 / Pipeline Configuration
                        </span>
                        <span class="pipe-badge-tag" style="background: #f0fdf4; color: #16a34a; border-color: #bbf7d0;">
                            Kiểm soát Win Probability
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts) -->
                <div class="pipe-alerts" id="pipeAlertsArea" aria-live="polite">
                    <div class="pipe-alert pipe-alert-success" id="clientSuccessAlert" style="display: none;" role="status">
                        <svg class="pipe-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="pipe-alert-content">
                            <div class="pipe-alert-title" id="clientSuccessTitle">Thao tác thành công</div>
                            <div id="clientSuccessMsg"></div>
                        </div>
                        <button type="button" class="pipe-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>

                    <div class="pipe-alert pipe-alert-danger" id="clientErrorAlert" style="display: none;" role="alert">
                        <svg class="pipe-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                        </svg>
                        <div class="pipe-alert-content">
                            <div class="pipe-alert-title" id="clientErrorTitle">Lỗi dữ liệu</div>
                            <div id="clientErrorMsg"></div>
                        </div>
                        <button type="button" class="pipe-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>
                </div>

                <!-- Thống kê sơ bộ (KPI Cards) -->
                <section class="pipe-stats-grid" aria-label="Thống kê tổng quan Pipeline">
                    <div class="pipe-stat-card">
                        <div class="pipe-stat-icon pipe-stat-icon--blue" aria-hidden="true">📊</div>
                        <div class="pipe-stat-data">
                            <span class="pipe-stat-val" id="statTotalStages">7</span>
                            <span class="pipe-stat-lbl">Tổng số Stage</span>
                        </div>
                    </div>
                    <div class="pipe-stat-card">
                        <div class="pipe-stat-icon pipe-stat-icon--green" aria-hidden="true">⚡</div>
                        <div class="pipe-stat-data">
                            <span class="pipe-stat-val" id="statActiveStages">7</span>
                            <span class="pipe-stat-lbl">Stage đang hoạt động</span>
                        </div>
                    </div>
                    <div class="pipe-stat-card">
                        <div class="pipe-stat-icon pipe-stat-icon--purple" aria-hidden="true">🎯</div>
                        <div class="pipe-stat-data">
                            <span class="pipe-stat-val" id="statAvgProbability">44%</span>
                            <span class="pipe-stat-lbl">Win Probability trung bình</span>
                        </div>
                    </div>
                    <div class="pipe-stat-card">
                        <div class="pipe-stat-icon pipe-stat-icon--amber" aria-hidden="true">🛡️</div>
                        <div class="pipe-stat-data">
                            <span class="pipe-stat-val" id="statStagesWithReq">7</span>
                            <span class="pipe-stat-lbl">Stage có điều kiện bắt buộc</span>
                        </div>
                    </div>
                </section>

                <!-- PIPELINE VISUALIZATION (Trực quan hóa Phễu Bán hàng) -->
                <section class="pipe-card" aria-label="Sơ đồ phễu Pipeline trực quan">
                    <div class="pipe-visual-section">
                        <div class="pipe-visual-header">
                            <h2 class="pipe-visual-title">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                                </svg>
                                Tiến trình Pipeline & Tỷ lệ thắng (Phễu trực quan)
                            </h2>
                            <span style="font-size: 0.8rem; color: var(--pipe-text-muted);">Tự động đồng bộ theo thứ tự và tỷ lệ thắng</span>
                        </div>
                        <div class="pipe-visual-track" id="pipeVisualTrack">
                            <!-- Render trực quan qua JavaScript -->
                        </div>
                    </div>

                    <!-- Toolbar: Tìm kiếm, lọc và thêm mới Stage -->
                    <div class="pipe-toolbar">
                        <div class="pipe-toolbar-left">
                            <!-- Ô tìm kiếm realtime -->
                            <div class="pipe-search-wrap">
                                <svg class="pipe-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="search" id="pipeSearchInput" class="pipe-search-input"
                                       placeholder="Tìm theo tên Stage hoặc mã Code..."
                                       aria-label="Tìm kiếm giai đoạn">
                            </div>

                            <!-- Lọc trạng thái -->
                            <select id="pipeStatusFilter" class="pipe-filter-select" aria-label="Lọc theo trạng thái">
                                <option value="">Tất cả trạng thái</option>
                                <option value="ACTIVE">Đang hoạt động (Active)</option>
                                <option value="INACTIVE">Tạm ngưng (Inactive)</option>
                            </select>

                            <!-- Nút Đặt lại bộ lọc -->
                            <button type="button" class="btn-pipe btn-pipe-secondary" id="btnResetFilter" onclick="resetFilters()">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="1 4 1 10 7 10"></polyline>
                                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                </svg>
                                <span>Đặt lại</span>
                            </button>
                        </div>

                        <div class="pipe-toolbar-right">
                            <button type="button" class="btn-pipe btn-pipe-secondary" onclick="normalizeOrder()" title="Chuẩn hóa thứ tự từ 1 đến N">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <polyline points="19 12 12 19 5 12"></polyline>
                                </svg>
                                <span>Chuẩn hóa thứ tự</span>
                            </button>

                            <button type="button" class="btn-pipe btn-pipe-primary" onclick="openCreateModal()">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                <span>Thêm Stage mới</span>
                            </button>
                        </div>
                    </div>

                    <!-- Bảng danh sách các Stage cấu hình -->
                    <div class="pipe-table-responsive">
                        <table class="pipe-table" id="pipeTable" aria-label="Bảng danh sách các Stage trong Pipeline">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 50px; text-align: center;">STT</th>
                                    <th scope="col" style="width: 130px;">Mã (Code)</th>
                                    <th scope="col">Tên Stage</th>
                                    <th scope="col" style="width: 170px;">Win Probability</th>
                                    <th scope="col" style="width: 110px; text-align: center;">Thứ tự</th>
                                    <th scope="col" style="width: 120px; text-align: center;">Opportunities</th>
                                    <th scope="col">Điều kiện chuyển tiếp (Exit Conditions)</th>
                                    <th scope="col" style="width: 130px; text-align: center;">Trạng thái</th>
                                    <th scope="col" style="width: 140px; text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="pipeTableBody">
                                <!-- Render động qua JavaScript -->
                            </tbody>
                        </table>
                    </div>

                    <!-- Empty State khi không có kết quả -->
                    <div class="pipe-empty-state" id="pipeEmptyState" style="display: none;">
                        <div class="pipe-empty-icon" aria-hidden="true">🔍</div>
                        <div class="pipe-empty-title">Không tìm thấy Stage nào</div>
                        <p class="pipe-empty-desc">Không có giai đoạn nào khớp với điều kiện tìm kiếm hoặc bộ lọc hiện tại.</p>
                        <button type="button" class="btn-pipe btn-pipe-secondary" onclick="resetFilters()">Đặt lại bộ lọc</button>
                    </div>

                </section>

            </div>
        </main>
    </div>

    <!-- MODAL 1: THÊM STAGE MỚI -->
    <div class="pipe-modal-overlay" id="modalCreateStage" role="dialog" aria-modal="true" aria-labelledby="createStageTitle">
        <div class="pipe-modal">
            <div class="pipe-modal-header">
                <h3 class="pipe-modal-title" id="createStageTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Thêm giai đoạn Pipeline mới
                </h3>
                <button type="button" class="pipe-modal-close" onclick="closeCreateModal()" aria-label="Đóng">&times;</button>
            </div>
            <form id="formCreateStage" onsubmit="handleCreateStage(event)">
                <div class="pipe-modal-body">
                    <div class="pipe-form-row">
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="createCode">Mã Stage (Code) <span class="required">*</span></label>
                            <input type="text" id="createCode" class="pipe-input" placeholder="VD: EVALUATION" required style="text-transform: uppercase;">
                            <span style="font-size: 0.75rem; color: var(--pipe-text-muted);">Mã định danh duy nhất</span>
                        </div>
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="createOrder">Thứ tự hiển thị <span class="required">*</span></label>
                            <input type="number" id="createOrder" class="pipe-input" min="1" step="1" value="1" required>
                        </div>
                    </div>

                    <div class="pipe-form-group">
                        <label class="pipe-label" for="createName">Tên giai đoạn <span class="required">*</span></label>
                        <input type="text" id="createName" class="pipe-input" placeholder="VD: Đánh giá kỹ thuật" required>
                    </div>

                    <div class="pipe-form-row">
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="createProbability">Tỷ lệ thắng (Win Probability) <span class="required">*</span></label>
                            <div class="pipe-input-group">
                                <input type="number" id="createProbability" class="pipe-input" min="0" max="100" step="1" placeholder="50" required>
                                <span class="pipe-input-suffix">%</span>
                            </div>
                            <span style="font-size: 0.75rem; color: var(--pipe-text-muted);">Giá trị hợp lệ từ 0% đến 100%</span>
                        </div>
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="createStatus">Trạng thái Stage</label>
                            <select id="createStatus" class="pipe-select">
                                <option value="ACTIVE" selected>Đang hoạt động (Active)</option>
                                <option value="INACTIVE">Tạm ngưng (Inactive)</option>
                            </select>
                        </div>
                    </div>

                    <!-- Exit Conditions Dynamic Editor -->
                    <div class="pipe-form-group">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                            <label class="pipe-label">Điều kiện thoát giai đoạn (Exit Conditions)</label>
                            <button type="button" class="btn-pipe btn-pipe-secondary" style="padding: 3px 8px; font-size: 0.78rem;" onclick="addCreateConditionRow()">
                                + Thêm điều kiện
                            </button>
                        </div>
                        <div class="pipe-conditions-box" id="createConditionsContainer">
                            <!-- Dynamic rows -->
                        </div>
                        <span style="font-size: 0.75rem; color: var(--pipe-text-muted);">Các điều kiện nhân viên cần thỏa mãn trước khi kéo cơ hội sang giai đoạn tiếp theo.</span>
                    </div>
                </div>
                <div class="pipe-modal-footer">
                    <button type="button" class="btn-pipe btn-pipe-secondary" onclick="closeCreateModal()">Hủy bỏ</button>
                    <button type="submit" class="btn-pipe btn-pipe-primary">Lưu Stage</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 2: CHỈNH SỬA STAGE -->
    <div class="pipe-modal-overlay" id="modalEditStage" role="dialog" aria-modal="true" aria-labelledby="editStageTitle">
        <div class="pipe-modal">
            <div class="pipe-modal-header">
                <h3 class="pipe-modal-title" id="editStageTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                    Chỉnh sửa cấu hình Stage
                </h3>
                <button type="button" class="pipe-modal-close" onclick="closeEditModal()" aria-label="Đóng">&times;</button>
            </div>
            <form id="formEditStage" onsubmit="handleUpdateStage(event)">
                <input type="hidden" id="editId">
                <div class="pipe-modal-body">

                    <!-- Cảnh báo khi Stage có Opportunity (Mục 10) -->
                    <div class="pipe-opp-warning-box" id="editOppWarningBox" style="display: none;">
                        <span style="font-size: 1.1rem; line-height: 1;">⚠️</span>
                        <div>
                            <strong>Cảnh báo ràng buộc dữ liệu:</strong>
                            Stage này đang được sử dụng bởi <strong id="editOppWarningCount">0</strong> Opportunity đang hoạt động.
                            Thay đổi cấu hình Stage không được làm mất hoặc thay đổi dữ liệu Opportunity hiện có.
                        </div>
                    </div>

                    <div class="pipe-form-row">
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="editCode">Mã Stage (Cố định)</label>
                            <input type="text" id="editCode" class="pipe-input" readonly style="background-color: #f1f5f9; cursor: not-allowed;">
                        </div>
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="editOrder">Thứ tự hiển thị <span class="required">*</span></label>
                            <input type="number" id="editOrder" class="pipe-input" min="1" step="1" required>
                        </div>
                    </div>

                    <div class="pipe-form-group">
                        <label class="pipe-label" for="editName">Tên giai đoạn <span class="required">*</span></label>
                        <input type="text" id="editName" class="pipe-input" required>
                    </div>

                    <div class="pipe-form-row">
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="editProbability">Tỷ lệ thắng (Win Probability) <span class="required">*</span></label>
                            <div class="pipe-input-group">
                                <input type="number" id="editProbability" class="pipe-input" min="0" max="100" step="1" required>
                                <span class="pipe-input-suffix">%</span>
                            </div>
                        </div>
                        <div class="pipe-form-group">
                            <label class="pipe-label" for="editStatus">Trạng thái Stage</label>
                            <select id="editStatus" class="pipe-select">
                                <option value="ACTIVE">Đang hoạt động (Active)</option>
                                <option value="INACTIVE">Tạm ngưng (Inactive)</option>
                            </select>
                        </div>
                    </div>

                    <!-- Exit Conditions Dynamic Editor -->
                    <div class="pipe-form-group">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                            <label class="pipe-label">Điều kiện chuyển tiếp (Exit Conditions)</label>
                            <button type="button" class="btn-pipe btn-pipe-secondary" style="padding: 3px 8px; font-size: 0.78rem;" onclick="addEditConditionRow()">
                                + Thêm điều kiện
                            </button>
                        </div>
                        <div class="pipe-conditions-box" id="editConditionsContainer">
                            <!-- Dynamic rows -->
                        </div>
                    </div>
                </div>
                <div class="pipe-modal-footer">
                    <button type="button" class="btn-pipe btn-pipe-secondary" onclick="closeEditModal()">Hủy</button>
                    <button type="submit" class="btn-pipe btn-pipe-primary">Lưu cập nhật</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 3: XEM CHI TIẾT STAGE -->
    <div class="pipe-modal-overlay" id="modalDetailStage" role="dialog" aria-modal="true" aria-labelledby="detailStageTitle">
        <div class="pipe-modal">
            <div class="pipe-modal-header">
                <h3 class="pipe-modal-title" id="detailStageTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    Chi tiết cấu hình Stage
                </h3>
                <button type="button" class="pipe-modal-close" onclick="closeDetailModal()" aria-label="Đóng">&times;</button>
            </div>
            <div class="pipe-modal-body">
                <div class="pipe-detail-grid">
                    <span class="pipe-detail-label">Mã Stage:</span>
                    <span class="pipe-detail-val" id="detailCode" style="font-weight: 700; color: var(--pipe-primary); font-family: monospace;"></span>

                    <span class="pipe-detail-label">Tên giai đoạn:</span>
                    <span class="pipe-detail-val" id="detailName" style="font-weight: 700; font-size: 1.05rem;"></span>

                    <span class="pipe-detail-label">Tỷ lệ thắng:</span>
                    <span class="pipe-detail-val" id="detailProbability" style="font-weight: 700; color: #16a34a;"></span>

                    <span class="pipe-detail-label">Thứ tự hiển thị:</span>
                    <span class="pipe-detail-val" id="detailOrder" style="font-weight: 600;"></span>

                    <span class="pipe-detail-label">Trạng thái:</span>
                    <span class="pipe-detail-val" id="detailStatus"></span>

                    <span class="pipe-detail-label">Cơ hội đang giữ:</span>
                    <span class="pipe-detail-val" id="detailOpportunityCount" style="font-weight: 700; color: var(--pipe-primary);"></span>

                    <span class="pipe-detail-label">Ngày tạo:</span>
                    <span class="pipe-detail-val" id="detailCreatedAt" style="color: var(--pipe-text-sub);"></span>

                    <span class="pipe-detail-label">Cập nhật cuối:</span>
                    <span class="pipe-detail-val" id="detailUpdatedAt" style="color: var(--pipe-text-sub);"></span>
                </div>

                <div style="margin-top: 12px; border-top: 1px solid var(--pipe-border-subtle); padding-top: 14px;">
                    <h4 style="margin: 0 0 10px 0; font-size: 0.95rem; color: var(--pipe-text-main);">
                        Điều kiện bắt buộc chuyển giai đoạn (Exit Conditions):
                    </h4>
                    <div id="detailExitConditionsList" style="display: flex; flex-direction: column; gap: 8px;">
                        <!-- List of conditions -->
                    </div>
                </div>
            </div>
            <div class="pipe-modal-footer">
                <button type="button" class="btn-pipe btn-pipe-secondary" onclick="closeDetailModal()">Đóng</button>
                <button type="button" class="btn-pipe btn-pipe-primary" id="btnDetailToEdit">Chỉnh sửa</button>
            </div>
        </div>
    </div>

    <!-- JAVASCRIPT XỬ LÝ FRONTEND MOCK DATA & CẤU HÌNH PIPELINE (S2-09) -->
    <script>
        // Dữ liệu Mock chuẩn đặc tả S2-09 (7 Giai đoạn kinh doanh)
        let stagesData = [
            {
                id: 1,
                code: "LEAD",
                name: "Lead (Đầu mối tiếp cận)",
                winProbability: 10,
                displayOrder: 1,
                status: "ACTIVE",
                opportunityCount: 12,
                exitConditions: [
                    { id: 101, name: "Đã có thông tin liên hệ hợp lệ (Email/SĐT)", required: true },
                    { id: 102, name: "Đã xác định nhu cầu sơ bộ", required: true }
                ],
                createdAt: "2026-01-01",
                updatedAt: "2026-02-10"
            },
            {
                id: 2,
                code: "QUALIFICATION",
                name: "Qualification (Thẩm định cơ hội)",
                winProbability: 25,
                displayOrder: 2,
                status: "ACTIVE",
                opportunityCount: 18,
                exitConditions: [
                    { id: 103, name: "Đã xác định người quyết định cuối cùng", required: true },
                    { id: 104, name: "Đã xác định ngân sách dự kiến", required: true },
                    { id: 105, name: "Khách hàng có kế hoạch mua trong 3 tháng", required: false }
                ],
                createdAt: "2026-01-01",
                updatedAt: "2026-02-12"
            },
            {
                id: 3,
                code: "NEED_ANALYSIS",
                name: "Need Analysis (Phân tích nhu cầu)",
                winProbability: 40,
                displayOrder: 3,
                status: "ACTIVE",
                opportunityCount: 9,
                exitConditions: [
                    { id: 106, name: "Đã hoàn thành khảo sát thực tế và quy trình nghiệp vụ", required: true },
                    { id: 107, name: "Tài liệu hóa yêu cầu chức năng", required: true }
                ],
                createdAt: "2026-01-01",
                updatedAt: "2026-02-15"
            },
            {
                id: 4,
                code: "PROPOSAL",
                name: "Proposal (Báo giá & Đề xuất)",
                winProbability: 60,
                displayOrder: 4,
                status: "ACTIVE",
                opportunityCount: 14,
                exitConditions: [
                    { id: 108, name: "Đã gửi bảng báo giá chính thức", required: true },
                    { id: 109, name: "Khách hàng xác nhận đã nhận và xem báo giá", required: true },
                    { id: 110, name: "Đã phê duyệt chiết khấu giá sàn (nếu có)", required: true }
                ],
                createdAt: "2026-01-01",
                updatedAt: "2026-02-20"
            },
            {
                id: 5,
                code: "NEGOTIATION",
                name: "Negotiation (Đàm phán & Thương thảo)",
                winProbability: 75,
                displayOrder: 5,
                status: "ACTIVE",
                opportunityCount: 7,
                exitConditions: [
                    { id: 111, name: "Thống nhất điều khoản thanh toán & SLA dịch vụ", required: true },
                    { id: 112, name: "Xác định thời gian ký kết hợp đồng", required: true }
                ],
                createdAt: "2026-01-01",
                updatedAt: "2026-02-25"
            },
            {
                id: 6,
                code: "CLOSED_WON",
                name: "Closed Won (Thắng hợp đồng)",
                winProbability: 100,
                displayOrder: 6,
                status: "ACTIVE",
                opportunityCount: 25,
                exitConditions: [
                    { id: 113, name: "Hợp đồng đã được ký kết đầy đủ 2 bên", required: true },
                    { id: 114, name: "Đã nhận tiền tạm ứng / đặt cọc theo điều khoản", required: true }
                ],
                createdAt: "2026-01-01",
                updatedAt: "2026-03-01"
            },
            {
                id: 7,
                code: "CLOSED_LOST",
                name: "Closed Lost (Thất bại / Hủy)",
                winProbability: 0,
                displayOrder: 7,
                status: "ACTIVE",
                opportunityCount: 8,
                exitConditions: [
                    { id: 115, name: "Ghi nhận lý do thất bại (Win-Loss Reason)", required: true },
                    { id: 116, name: "Phê duyệt đóng cơ hội của Trưởng nhóm", required: false }
                ],
                createdAt: "2026-01-01",
                updatedAt: "2026-03-01"
            }
        ];

        // Format Status Badge
        function getStatusBadge(status) {
            return status === "ACTIVE"
                ? '<span class="badge-status badge-status-active">● Hoạt động</span>'
                : '<span class="badge-status badge-status-inactive">○ Tạm ngưng</span>';
        }

        // Cập nhật thống kê sơ bộ (KPI Cards)
        function updateKPIs() {
            const total = stagesData.length;
            const active = stagesData.filter(s => s.status === "ACTIVE").length;
            
            let sumProb = 0;
            let stagesWithReq = 0;

            stagesData.forEach(s => {
                sumProb += s.winProbability;
                if (s.exitConditions && s.exitConditions.some(c => c.required)) {
                    stagesWithReq++;
                }
            });

            const avg = total > 0 ? Math.round(sumProb / total) : 0;

            document.getElementById("statTotalStages").textContent = total;
            document.getElementById("statActiveStages").textContent = active;
            document.getElementById("statAvgProbability").textContent = avg + "%";
            document.getElementById("statStagesWithReq").textContent = stagesWithReq;
        }

        // Render Trực quan hóa Phễu Pipeline
        function renderPipelineVisualizer() {
            const track = document.getElementById("pipeVisualTrack");
            const sortedStages = [...stagesData].sort((a, b) => a.displayOrder - b.displayOrder);

            let html = "";
            sortedStages.forEach(s => {
                let cardClass = "";
                if (s.code === "CLOSED_WON") cardClass = "won";
                else if (s.code === "CLOSED_LOST") cardClass = "lost";

                const reqCount = (s.exitConditions || []).filter(c => c.required).length;

                html += `
                    <div class="pipe-visual-step ${cardClass}" onclick="openDetailModal(${s.id})" style="cursor: pointer;" title="Xem chi tiết giai đoạn ${s.name}">
                        <div class="pipe-step-top">
                            <span class="pipe-step-order">#${s.displayOrder}</span>
                            <span class="pipe-step-code">${s.code}</span>
                        </div>
                        <div>
                            <div class="pipe-step-name">${s.name}</div>
                        </div>
                        <div class="pipe-prob-wrapper">
                            <div class="pipe-prob-label">
                                <span>Tỷ lệ thắng:</span>
                                <strong style="color: ${s.code === 'CLOSED_WON' ? '#16a34a' : s.code === 'CLOSED_LOST' ? '#dc2626' : 'var(--pipe-primary)'};">${s.winProbability}%</strong>
                            </div>
                            <div class="pipe-prob-bar">
                                <div class="pipe-prob-fill" style="width: ${s.winProbability}%;"></div>
                            </div>
                        </div>
                        <div class="pipe-step-footer">
                            <span class="pipe-opp-badge">${s.opportunityCount} cơ hội</span>
                            <span style="font-size: 0.72rem; color: var(--pipe-text-muted);">${reqCount} đ/k</span>
                        </div>
                    </div>
                `;
            });

            track.innerHTML = html;
        }

        // Render Bảng danh sách các Stage
        function renderTable() {
            const tbody = document.getElementById("pipeTableBody");
            const emptyState = document.getElementById("pipeEmptyState");

            const searchKw = (document.getElementById("pipeSearchInput").value || "").trim().toLowerCase();
            const statusFilter = document.getElementById("pipeStatusFilter").value;

            let filtered = stagesData.filter(s => {
                const matchKw = !searchKw ||
                    s.code.toLowerCase().includes(searchKw) ||
                    s.name.toLowerCase().includes(searchKw);
                const matchStatus = !statusFilter || s.status === statusFilter;
                return matchKw && matchStatus;
            });

            filtered.sort((a, b) => a.displayOrder - b.displayOrder);

            if (filtered.length === 0) {
                tbody.innerHTML = "";
                emptyState.style.display = "flex";
                return;
            }

            emptyState.style.display = "none";
            let html = "";

            filtered.forEach((s, idx) => {
                const reqCount = (s.exitConditions || []).filter(c => c.required).length;
                const totalConditions = (s.exitConditions || []).length;

                html += `
                    <tr>
                        <td style="text-align: center; color: var(--pipe-text-muted);">${idx + 1}</td>
                        <td style="font-weight: 700; color: var(--pipe-primary); font-family: monospace;">${s.code}</td>
                        <td style="font-weight: 600; color: var(--pipe-text-main);">${s.name}</td>
                        <td>
                            <div class="pipe-table-prob">
                                <div class="pipe-table-prob-bar">
                                    <div class="pipe-table-prob-fill" style="width: ${s.winProbability}%; background: ${s.code === 'CLOSED_WON' ? '#16a34a' : s.code === 'CLOSED_LOST' ? '#dc2626' : 'var(--pipe-primary)'};"></div>
                                </div>
                                <span style="font-weight: 700; font-size: 0.85rem; min-width: 36px;">${s.winProbability}%</span>
                            </div>
                        </td>
                        <td style="text-align: center;">
                            <div class="pipe-order-controls">
                                <button type="button" class="pipe-order-btn" title="Di chuyển lên" onclick="moveStageOrder(${s.id}, -1)">▲</button>
                                <span style="font-weight: 700; min-width: 20px; text-align: center;">${s.displayOrder}</span>
                                <button type="button" class="pipe-order-btn" title="Di chuyển xuống" onclick="moveStageOrder(${s.id}, 1)">▼</button>
                            </div>
                        </td>
                        <td style="text-align: center;">
                            <span class="badge-opp-count" title="${s.opportunityCount} cơ hội đang ở giai đoạn này">${s.opportunityCount} Opps</span>
                        </td>
                        <td>
                            <span class="badge-condition-req" title="${reqCount} điều kiện bắt buộc trong tổng số ${totalConditions} điều kiện">
                                🛡️ ${reqCount} điều kiện bắt buộc
                            </span>
                            <span style="font-size: 0.78rem; color: var(--pipe-text-muted); margin-left: 4px;">(${totalConditions} tổng)</span>
                        </td>
                        <td style="text-align: center;">${getStatusBadge(s.status)}</td>
                        <td style="text-align: center;">
                            <div style="display: inline-flex; gap: 4px;">
                                <button type="button" class="btn-pipe-icon" title="Xem chi tiết" onclick="openDetailModal(${s.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                                <button type="button" class="btn-pipe-icon btn-pipe-icon--edit" title="Chỉnh sửa cấu hình" onclick="openEditModal(${s.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                    </svg>
                                </button>
                                <button type="button" class="btn-pipe-icon btn-pipe-icon--toggle" title="${s.status === 'ACTIVE' ? 'Tạm ngưng giai đoạn' : 'Kích hoạt lại'}" onclick="toggleStageStatus(${s.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <rect x="1" y="5" width="22" height="14" rx="7" ry="7"></rect>
                                        <circle cx="${s.status === 'ACTIVE' ? '16' : '8'}" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            });

            tbody.innerHTML = html;
        }

        // Tăng / Giảm thứ tự (Reorder)
        function moveStageOrder(id, delta) {
            const sorted = [...stagesData].sort((a, b) => a.displayOrder - b.displayOrder);
            const idx = sorted.findIndex(s => s.id === id);
            if (idx === -1) return;

            const targetIdx = idx + delta;
            if (targetIdx < 0 || targetIdx >= sorted.length) return;

            const temp = sorted[idx].displayOrder;
            sorted[idx].displayOrder = sorted[targetIdx].displayOrder;
            sorted[targetIdx].displayOrder = temp;

            refreshAll();
            showSuccessToast("Đã điều chỉnh thứ tự", "Đã di chuyển giai đoạn " + sorted[idx].name + " thành công.");
        }

        // Chuẩn hóa thứ tự 1..N
        function normalizeOrder() {
            const sorted = [...stagesData].sort((a, b) => a.displayOrder - b.displayOrder);
            sorted.forEach((s, idx) => {
                s.displayOrder = idx + 1;
            });
            refreshAll();
            showSuccessToast("Đã chuẩn hóa thứ tự", "Thứ tự các giai đoạn Pipeline đã được đánh số lại từ 1 đến " + sorted.length + ".");
        }

        // Bật / Tắt trạng thái Stage
        function toggleStageStatus(id) {
            const stage = stagesData.find(s => s.id === id);
            if (!stage) return;

            if (stage.status === "ACTIVE" && stage.opportunityCount > 0) {
                // Warning khi tạm ngưng Stage đang có cơ hội
                const proceed = confirm("Stage '" + stage.name + "' hiện đang có " + stage.opportunityCount + " Opportunity đang hoạt động. Bạn có chắc chắn muốn tạm ngưng không?");
                if (!proceed) return;
            }

            stage.status = stage.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
            stage.updatedAt = new Date().toISOString().split("T")[0];
            refreshAll();
            showSuccessToast("Cập nhật trạng thái", "Đã chuyển Stage " + stage.code + " sang " + (stage.status === "ACTIVE" ? "Đang hoạt động" : "Tạm ngưng") + ".");
        }

        // Đặt lại bộ lọc
        function resetFilters() {
            document.getElementById("pipeSearchInput").value = "";
            document.getElementById("pipeStatusFilter").value = "";
            renderTable();
        }

        // Refresh toàn bộ giao diện
        function refreshAll() {
            updateKPIs();
            renderPipelineVisualizer();
            renderTable();
        }

        // MODAL 1: CREATE STAGE
        function openCreateModal() {
            document.getElementById("formCreateStage").reset();
            document.getElementById("createOrder").value = stagesData.length + 1;
            
            const container = document.getElementById("createConditionsContainer");
            container.innerHTML = `
                <div class="pipe-condition-row">
                    <input type="text" placeholder="Nhập tên điều kiện thoát..." class="cond-name" required>
                    <label class="pipe-condition-req-label">
                        <input type="checkbox" class="cond-req" checked> Bắt buộc
                    </label>
                    <button type="button" class="pipe-condition-del" onclick="this.parentElement.remove()" title="Xóa">&times;</button>
                </div>
            `;

            document.getElementById("modalCreateStage").classList.add("active");
        }

        function closeCreateModal() {
            document.getElementById("modalCreateStage").classList.remove("active");
        }

        function addCreateConditionRow() {
            const container = document.getElementById("createConditionsContainer");
            const div = document.createElement("div");
            div.className = "pipe-condition-row";
            div.innerHTML = `
                <input type="text" placeholder="Nhập tên điều kiện thoát..." class="cond-name" required>
                <label class="pipe-condition-req-label">
                    <input type="checkbox" class="cond-req" checked> Bắt buộc
                </label>
                <button type="button" class="pipe-condition-del" onclick="this.parentElement.remove()" title="Xóa">&times;</button>
            `;
            container.appendChild(div);
        }

        function handleCreateStage(e) {
            e.preventDefault();
            const code = document.getElementById("createCode").value.trim().toUpperCase();
            const name = document.getElementById("createName").value.trim();
            const prob = parseInt(document.getElementById("createProbability").value);
            const order = parseInt(document.getElementById("createOrder").value) || 1;

            if (isNaN(prob) || prob < 0 || prob > 100) {
                showErrorToast("Lỗi tỷ lệ thắng", "Tỷ lệ Win Probability phải là số hợp lệ từ 0 đến 100.");
                return;
            }

            if (stagesData.some(s => s.code === code)) {
                showErrorToast("Mã Code đã tồn tại", "Mã Stage '" + code + "' đã tồn tại trong hệ thống. Vui lòng chọn mã khác.");
                return;
            }

            // Thu thập exit conditions
            const conditionRows = document.querySelectorAll("#createConditionsContainer .pipe-condition-row");
            const conditions = [];
            conditionRows.forEach((row, i) => {
                const cName = row.querySelector(".cond-name").value.trim();
                const cReq = row.querySelector(".cond-req").checked;
                if (cName) {
                    conditions.push({ id: Date.now() + i, name: cName, required: cReq });
                }
            });

            const newStage = {
                id: Date.now(),
                code: code,
                name: name,
                winProbability: prob,
                displayOrder: order,
                status: document.getElementById("createStatus").value,
                opportunityCount: 0,
                exitConditions: conditions,
                createdAt: new Date().toISOString().split("T")[0],
                updatedAt: new Date().toISOString().split("T")[0]
            };

            stagesData.push(newStage);
            closeCreateModal();
            refreshAll();
            showSuccessToast("Thêm Stage thành công", "Đã thêm giai đoạn mới '" + newStage.name + "' vào Pipeline.");
        }

        // MODAL 2: EDIT STAGE
        function openEditModal(id) {
            const stage = stagesData.find(s => s.id === id);
            if (!stage) return;

            document.getElementById("editId").value = stage.id;
            document.getElementById("editCode").value = stage.code;
            document.getElementById("editName").value = stage.name;
            document.getElementById("editProbability").value = stage.winProbability;
            document.getElementById("editOrder").value = stage.displayOrder;
            document.getElementById("editStatus").value = stage.status;

            // Kiểm tra và hiển thị Warning nếu có Opportunity (Mục 10)
            const warnBox = document.getElementById("editOppWarningBox");
            if (stage.opportunityCount > 0) {
                document.getElementById("editOppWarningCount").textContent = stage.opportunityCount;
                warnBox.style.display = "flex";
            } else {
                warnBox.style.display = "none";
            }

            // Populate exit conditions
            const container = document.getElementById("editConditionsContainer");
            container.innerHTML = "";
            (stage.exitConditions || []).forEach(c => {
                const div = document.createElement("div");
                div.className = "pipe-condition-row";
                div.innerHTML = `
                    <input type="text" value="${c.name.replace(/"/g, '&quot;')}" class="cond-name" required>
                    <label class="pipe-condition-req-label">
                        <input type="checkbox" class="cond-req" ${c.required ? 'checked' : ''}> Bắt buộc
                    </label>
                    <button type="button" class="pipe-condition-del" onclick="this.parentElement.remove()" title="Xóa">&times;</button>
                `;
                container.appendChild(div);
            });

            if ((stage.exitConditions || []).length === 0) {
                addEditConditionRow();
            }

            document.getElementById("modalEditStage").classList.add("active");
        }

        function closeEditModal() {
            document.getElementById("modalEditStage").classList.remove("active");
        }

        function addEditConditionRow() {
            const container = document.getElementById("editConditionsContainer");
            const div = document.createElement("div");
            div.className = "pipe-condition-row";
            div.innerHTML = `
                <input type="text" placeholder="Nhập tên điều kiện thoát..." class="cond-name" required>
                <label class="pipe-condition-req-label">
                    <input type="checkbox" class="cond-req" checked> Bắt buộc
                </label>
                <button type="button" class="pipe-condition-del" onclick="this.parentElement.remove()" title="Xóa">&times;</button>
            `;
            container.appendChild(div);
        }

        function handleUpdateStage(e) {
            e.preventDefault();
            const id = parseInt(document.getElementById("editId").value);
            const stage = stagesData.find(s => s.id === id);
            if (!stage) return;

            const prob = parseInt(document.getElementById("editProbability").value);
            if (isNaN(prob) || prob < 0 || prob > 100) {
                showErrorToast("Lỗi tỷ lệ thắng", "Tỷ lệ Win Probability phải là số hợp lệ từ 0 đến 100.");
                return;
            }

            stage.name = document.getElementById("editName").value.trim();
            stage.winProbability = prob;
            stage.displayOrder = parseInt(document.getElementById("editOrder").value) || stage.displayOrder;
            stage.status = document.getElementById("editStatus").value;
            stage.updatedAt = new Date().toISOString().split("T")[0];

            // Cập nhật điều kiện thoát
            const conditionRows = document.querySelectorAll("#editConditionsContainer .pipe-condition-row");
            const conditions = [];
            conditionRows.forEach((row, i) => {
                const cName = row.querySelector(".cond-name").value.trim();
                const cReq = row.querySelector(".cond-req").checked;
                if (cName) {
                    conditions.push({ id: Date.now() + i, name: cName, required: cReq });
                }
            });
            stage.exitConditions = conditions;

            closeEditModal();
            refreshAll();
            showSuccessToast("Cập nhật thành công", "Đã lưu thay đổi cấu hình giai đoạn " + stage.code + ".");
        }

        // MODAL 3: DETAIL STAGE
        function openDetailModal(id) {
            const stage = stagesData.find(s => s.id === id);
            if (!stage) return;

            document.getElementById("detailCode").textContent = stage.code;
            document.getElementById("detailName").textContent = stage.name;
            document.getElementById("detailProbability").textContent = stage.winProbability + "%";
            document.getElementById("detailOrder").textContent = "#" + stage.displayOrder;
            document.getElementById("detailStatus").innerHTML = getStatusBadge(stage.status);
            document.getElementById("detailOpportunityCount").textContent = stage.opportunityCount + " cơ hội đang hoạt động";
            document.getElementById("detailCreatedAt").textContent = stage.createdAt || "2026-01-01";
            document.getElementById("detailUpdatedAt").textContent = stage.updatedAt || "2026-02-01";

            const container = document.getElementById("detailExitConditionsList");
            let conditionsHtml = "";
            if (stage.exitConditions && stage.exitConditions.length > 0) {
                stage.exitConditions.forEach(c => {
                    conditionsHtml += `
                        <div style="display: flex; align-items: center; justify-content: space-between; padding: 6px 10px; background: var(--pipe-bg-page); border: 1px solid var(--pipe-border); border-radius: 6px; font-size: 0.85rem;">
                            <span>${c.required ? '☑' : '☐'} ${c.name}</span>
                            <span class="${c.required ? 'badge-condition-req' : 'badge-status-inactive'}" style="font-size: 0.72rem;">
                                ${c.required ? 'Bắt buộc' : 'Tùy chọn'}
                            </span>
                        </div>
                    `;
                });
            } else {
                conditionsHtml = '<span style="color: var(--pipe-text-muted); font-size: 0.85rem;">(Chưa cấu hình điều kiện chuyển tiếp)</span>';
            }
            container.innerHTML = conditionsHtml;

            document.getElementById("btnDetailToEdit").onclick = function() {
                closeDetailModal();
                openEditModal(stage.id);
            };

            document.getElementById("modalDetailStage").classList.add("active");
        }

        function closeDetailModal() {
            document.getElementById("modalDetailStage").classList.remove("active");
        }

        // Toasts
        function showSuccessToast(title, msg) {
            const box = document.getElementById("clientSuccessAlert");
            document.getElementById("clientSuccessTitle").textContent = title;
            document.getElementById("clientSuccessMsg").textContent = msg;
            box.style.display = "flex";
            setTimeout(() => { box.style.display = "none"; }, 5000);
        }

        function showErrorToast(title, msg) {
            const box = document.getElementById("clientErrorAlert");
            document.getElementById("clientErrorTitle").textContent = title;
            document.getElementById("clientErrorMsg").textContent = msg;
            box.style.display = "flex";
        }

        // Lắng nghe sự kiện Search & Filter
        document.getElementById("pipeSearchInput").addEventListener("input", renderTable);
        document.getElementById("pipeStatusFilter").addEventListener("change", renderTable);

        // Đóng modal khi bấm Escape
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeCreateModal();
                closeEditModal();
                closeDetailModal();
            }
        });

        // Khởi tạo ban đầu
        window.addEventListener("DOMContentLoaded", () => {
            refreshAll();
        });
    </script>
</body>
</html>