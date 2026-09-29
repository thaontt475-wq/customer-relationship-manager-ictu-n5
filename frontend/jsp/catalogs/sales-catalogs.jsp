<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ==========================================================================
  FEATURE S2-07: Danh mục bán hàng dùng chung (Shared Sales Catalogs)
  Phụ trách Frontend/View: Tiệp
  
  BE CONTRACT NEEDED:
    1. GET /sales-catalogs?category={cat}&search={kw}&status={st}
       - Lấy danh sách các giá trị danh mục theo loại (INDUSTRY, COMPANY_SIZE, LEAD_SOURCE, ACTIVITY_TYPE)
       - Response dự kiến:
         {
           "items": [
             {
               "id": 1,
               "category": "INDUSTRY",
               "code": "IT",
               "name": "Công nghệ thông tin",
               "description": "Doanh nghiệp trong lĩnh vực phần mềm, giải pháp CNTT",
               "displayOrder": 1,
               "status": "ACTIVE",
               "usageCount": 25,
               "createdAt": "2026-01-10T08:00:00",
               "updatedAt": "2026-03-15T14:30:00"
             }
           ]
         }

    2. POST /sales-catalogs
       - Tạo mới giá trị danh mục
       - Body: { category, code, name, description, displayOrder, status }
       - BE xử lý kiểm tra mã code duy nhất trong cùng category và lưu trữ DB.

    3. PUT /sales-catalogs/{id}
       - Cập nhật giá trị danh mục (name, description, displayOrder, status)

    4. POST /sales-catalogs/{id}/status
       - Bật/tắt trạng thái (ACTIVE / INACTIVE)

    5. DELETE /sales-catalogs/{id}
       - Kiểm tra ràng buộc dữ liệu: Nếu usageCount > 0 thì từ chối xóa vật lý và chuyển trạng thái INACTIVE.

    6. PUT /sales-catalogs/reorder
       - Lưu thứ tự hiển thị displayOrder hàng loạt
  ==========================================================================
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục bán hàng dùng chung - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Danh mục bán hàng (S2-07) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalogs/sales-catalogs.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Danh mục bán hàng -->
        <main class="cat-page" id="salesCatalogApp" role="main">
            <div class="cat-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="cat-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Danh mục bán hàng</span>
                </nav>

                <!-- Header màn hình -->
                <header class="cat-header">
                    <div class="cat-header-info">
                        <h1>Danh mục bán hàng dùng chung</h1>
                        <p>Quản lý các giá trị danh mục được sử dụng thống nhất trong hoạt động bán hàng.</p>
                    </div>
                    <div class="cat-header-badges">
                        <span class="cat-badge-tag">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="3" y="3" width="7" height="7"></rect>
                                <rect x="14" y="3" width="7" height="7"></rect>
                                <rect x="14" y="14" width="7" height="7"></rect>
                                <rect x="3" y="14" width="7" height="7"></rect>
                            </svg>
                            S2-07 / Shared Sales Catalogs
                        </span>
                        <span class="cat-badge-tag" style="background: #f0fdf4; color: #16a34a; border-color: #bbf7d0;">
                            4 Danh mục chuẩn
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts) -->
                <div class="cat-alerts" id="catAlertsArea" aria-live="polite">
                    <div class="cat-alert cat-alert-success" id="clientSuccessAlert" style="display: none;" role="status">
                        <svg class="cat-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="cat-alert-content">
                            <div class="cat-alert-title" id="clientSuccessTitle">Thao tác thành công</div>
                            <div id="clientSuccessMsg"></div>
                        </div>
                        <button type="button" class="cat-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>

                    <div class="cat-alert cat-alert-danger" id="clientErrorAlert" style="display: none;" role="alert">
                        <svg class="cat-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="cat-alert-content">
                            <div class="cat-alert-title" id="clientErrorTitle">Lỗi dữ liệu</div>
                            <div id="clientErrorMsg"></div>
                        </div>
                        <button type="button" class="cat-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>
                </div>

                <!-- Thống kê sơ bộ (KPI Cards) -->
                <section class="cat-stats-grid" aria-label="Thống kê danh mục bán hàng">
                    <div class="cat-stat-card">
                        <div class="cat-stat-icon cat-stat-icon--blue" aria-hidden="true">📑</div>
                        <div class="cat-stat-data">
                            <span class="cat-stat-val" id="statTotalCatalogs">4</span>
                            <span class="cat-stat-lbl">Tổng danh mục chuẩn</span>
                        </div>
                    </div>
                    <div class="cat-stat-card">
                        <div class="cat-stat-icon cat-stat-icon--purple" aria-hidden="true">🔢</div>
                        <div class="cat-stat-data">
                            <span class="cat-stat-val" id="statTotalValues">28</span>
                            <span class="cat-stat-lbl">Tổng số giá trị</span>
                        </div>
                    </div>
                    <div class="cat-stat-card">
                        <div class="cat-stat-icon cat-stat-icon--green" aria-hidden="true">🔗</div>
                        <div class="cat-stat-data">
                            <span class="cat-stat-val" id="statInUseCount">23</span>
                            <span class="cat-stat-lbl">Đang được sử dụng trong CRM</span>
                        </div>
                    </div>
                    <div class="cat-stat-card">
                        <div class="cat-stat-icon cat-stat-icon--amber" aria-hidden="true">⚡</div>
                        <div class="cat-stat-data">
                            <span class="cat-stat-val" id="statActiveCount">25</span>
                            <span class="cat-stat-lbl">Đang hoạt động (Active)</span>
                        </div>
                    </div>
                </section>

                <!-- Navigation Tabs: 4 Danh mục -->
                <div class="cat-nav-tabs" role="tablist" aria-label="Tabs danh mục bán hàng">
                    <button type="button" class="cat-tab-btn active" id="tabBtnIndustry" role="tab" aria-selected="true" onclick="switchCategory('INDUSTRY')">
                        <span>🏢 Ngành nghề</span>
                        <span class="cat-tab-badge" id="tabBadgeIndustry">8</span>
                    </button>
                    <button type="button" class="cat-tab-btn" id="tabBtnCompanySize" role="tab" aria-selected="false" onclick="switchCategory('COMPANY_SIZE')">
                        <span>👥 Quy mô doanh nghiệp</span>
                        <span class="cat-tab-badge" id="tabBadgeCompanySize">5</span>
                    </button>
                    <button type="button" class="cat-tab-btn" id="tabBtnLeadSource" role="tab" aria-selected="false" onclick="switchCategory('LEAD_SOURCE')">
                        <span>🎯 Nguồn Lead</span>
                        <span class="cat-tab-badge" id="tabBadgeLeadSource">8</span>
                    </button>
                    <button type="button" class="cat-tab-btn" id="tabBtnActivityType" role="tab" aria-selected="false" onclick="switchCategory('ACTIVITY_TYPE')">
                        <span>📞 Loại hoạt động</span>
                        <span class="cat-tab-badge" id="tabBadgeActivityType">7</span>
                    </button>
                </div>

                <!-- Thẻ Card hiển thị bảng dữ liệu của Tab -->
                <div class="cat-card">

                    <!-- Toolbar: Tìm kiếm, lọc và thêm mới -->
                    <div class="cat-toolbar">
                        <div class="cat-toolbar-left">
                            <!-- Ô tìm kiếm realtime -->
                            <div class="cat-search-wrap">
                                <svg class="cat-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="search" id="catSearchInput" class="cat-search-input"
                                       placeholder="Tìm theo mã, tên hoặc mô tả..."
                                       aria-label="Tìm kiếm giá trị danh mục">
                            </div>

                            <!-- Lọc trạng thái -->
                            <select id="catStatusFilter" class="cat-filter-select" aria-label="Lọc theo trạng thái">
                                <option value="">Tất cả trạng thái</option>
                                <option value="ACTIVE">Đang hoạt động (Active)</option>
                                <option value="INACTIVE">Ngừng sử dụng (Inactive)</option>
                            </select>

                            <!-- Nút Đặt lại bộ lọc -->
                            <button type="button" class="btn-cat btn-cat-secondary" id="btnResetFilter" onclick="resetFilters()">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="1 4 1 10 7 10"></polyline>
                                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                </svg>
                                <span>Đặt lại</span>
                            </button>
                        </div>

                        <div class="cat-toolbar-right">
                            <button type="button" class="btn-cat btn-cat-secondary" onclick="sortByOrder()" title="Sắp xếp danh sách theo thứ tự hiển thị">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <polyline points="19 12 12 19 5 12"></polyline>
                                </svg>
                                <span>Sắp xếp thứ tự</span>
                            </button>

                            <button type="button" class="btn-cat btn-cat-primary" onclick="openCreateModal()">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                <span>Thêm giá trị danh mục</span>
                            </button>
                        </div>
                    </div>

                    <!-- Bảng hiển thị danh mục -->
                    <div class="cat-table-responsive">
                        <table class="cat-table" id="catTable" aria-label="Bảng dữ liệu danh mục bán hàng">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 50px; text-align: center;">STT</th>
                                    <th scope="col" style="width: 120px;">Mã (Code)</th>
                                    <th scope="col">Tên giá trị</th>
                                    <th scope="col">Mô tả chi tiết</th>
                                    <th scope="col" style="width: 110px; text-align: center;">Thứ tự</th>
                                    <th scope="col" style="width: 140px; text-align: center;">Đang sử dụng</th>
                                    <th scope="col" style="width: 140px; text-align: center;">Trạng thái</th>
                                    <th scope="col" style="width: 150px; text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="catTableBody">
                                <!-- Dữ liệu render động qua JavaScript -->
                            </tbody>
                        </table>
                    </div>

                    <!-- Empty State khi không tìm thấy kết quả -->
                    <div class="cat-empty-state" id="catEmptyState" style="display: none;">
                        <div class="cat-empty-icon" aria-hidden="true">📂</div>
                        <div class="cat-empty-title">Không tìm thấy giá trị danh mục nào</div>
                        <p class="cat-empty-desc">Không có dữ liệu nào khớp với từ khóa tìm kiếm hoặc bộ lọc hiện tại trong danh mục này.</p>
                        <button type="button" class="btn-cat btn-cat-secondary" onclick="resetFilters()">Đặt lại bộ lọc</button>
                    </div>

                </div>

            </div>
        </main>
    </div>

    <!-- MODAL 1: THÊM GIÁ TRỊ DANH MỤC MỚI -->
    <div class="cat-modal-overlay" id="modalCreateCatalog" role="dialog" aria-modal="true" aria-labelledby="createCatalogTitle">
        <div class="cat-modal">
            <div class="cat-modal-header">
                <h3 class="cat-modal-title" id="createCatalogTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Thêm giá trị danh mục
                </h3>
                <button type="button" class="cat-modal-close" onclick="closeCreateModal()" aria-label="Đóng">&times;</button>
            </div>
            <form id="formCreateCatalog" onsubmit="handleCreateCatalog(event)">
                <div class="cat-modal-body">
                    <div class="cat-form-group">
                        <label class="cat-label" for="createCategory">Loại danh mục <span class="required">*</span></label>
                        <select id="createCategory" class="cat-select" required>
                            <option value="INDUSTRY">Ngành nghề (Industry)</option>
                            <option value="COMPANY_SIZE">Quy mô doanh nghiệp (Company Size)</option>
                            <option value="LEAD_SOURCE">Nguồn Lead (Lead Source)</option>
                            <option value="ACTIVITY_TYPE">Loại hoạt động (Activity Type)</option>
                        </select>
                    </div>

                    <div class="cat-form-row">
                        <div class="cat-form-group">
                            <label class="cat-label" for="createCode">Mã định danh (Code) <span class="required">*</span></label>
                            <input type="text" id="createCode" class="cat-input" placeholder="VD: TECH, VIP_SOURCE" required style="text-transform: uppercase;">
                            <span class="cat-hint">Mã duy nhất trong cùng danh mục</span>
                        </div>
                        <div class="cat-form-group">
                            <label class="cat-label" for="createOrder">Thứ tự hiển thị <span class="required">*</span></label>
                            <input type="number" id="createOrder" class="cat-input" min="1" step="1" value="1" required>
                        </div>
                    </div>

                    <div class="cat-form-group">
                        <label class="cat-label" for="createName">Tên hiển thị <span class="required">*</span></label>
                        <input type="text" id="createName" class="cat-input" placeholder="Nhập tên gọi danh mục..." required>
                    </div>

                    <div class="cat-form-group">
                        <label class="cat-label" for="createStatus">Trạng thái áp dụng</label>
                        <select id="createStatus" class="cat-select">
                            <option value="ACTIVE" selected>Đang hoạt động (Active)</option>
                            <option value="INACTIVE">Ngừng sử dụng (Inactive)</option>
                        </select>
                    </div>

                    <div class="cat-form-group">
                        <label class="cat-label" for="createDesc">Mô tả / Ý nghĩa</label>
                        <textarea id="createDesc" class="cat-textarea" rows="3" placeholder="Nhập mô tả giải thích mục đích sử dụng..."></textarea>
                    </div>
                </div>
                <div class="cat-modal-footer">
                    <button type="button" class="btn-cat btn-cat-secondary" onclick="closeCreateModal()">Hủy bỏ</button>
                    <button type="submit" class="btn-cat btn-cat-primary">Lưu danh mục</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 2: CHỈNH SỬA GIÁ TRỊ DANH MỤC -->
    <div class="cat-modal-overlay" id="modalEditCatalog" role="dialog" aria-modal="true" aria-labelledby="editCatalogTitle">
        <div class="cat-modal">
            <div class="cat-modal-header">
                <h3 class="cat-modal-title" id="editCatalogTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                    Chỉnh sửa giá trị danh mục
                </h3>
                <button type="button" class="cat-modal-close" onclick="closeEditModal()" aria-label="Đóng">&times;</button>
            </div>
            <form id="formEditCatalog" onsubmit="handleUpdateCatalog(event)">
                <input type="hidden" id="editId">
                <div class="cat-modal-body">
                    <div class="cat-form-row">
                        <div class="cat-form-group">
                            <label class="cat-label" for="editCode">Mã Code (Cố định)</label>
                            <input type="text" id="editCode" class="cat-input" readonly style="background-color: #f1f5f9; cursor: not-allowed;">
                            <span class="cat-hint">Mã định danh không được thay đổi</span>
                        </div>
                        <div class="cat-form-group">
                            <label class="cat-label" for="editOrder">Thứ tự hiển thị <span class="required">*</span></label>
                            <input type="number" id="editOrder" class="cat-input" min="1" step="1" required>
                        </div>
                    </div>

                    <div class="cat-form-group">
                        <label class="cat-label" for="editName">Tên hiển thị <span class="required">*</span></label>
                        <input type="text" id="editName" class="cat-input" required>
                    </div>

                    <div class="cat-form-group">
                        <label class="cat-label" for="editStatus">Trạng thái</label>
                        <select id="editStatus" class="cat-select">
                            <option value="ACTIVE">Đang hoạt động (Active)</option>
                            <option value="INACTIVE">Ngừng sử dụng (Inactive)</option>
                        </select>
                    </div>

                    <div class="cat-form-group">
                        <label class="cat-label" for="editDesc">Mô tả</label>
                        <textarea id="editDesc" class="cat-textarea" rows="3"></textarea>
                    </div>
                </div>
                <div class="cat-modal-footer">
                    <button type="button" class="btn-cat btn-cat-secondary" onclick="closeEditModal()">Hủy</button>
                    <button type="submit" class="btn-cat btn-cat-primary">Lưu thay đổi</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 3: XEM CHI TIẾT GIÁ TRỊ DANH MỤC -->
    <div class="cat-modal-overlay" id="modalDetailCatalog" role="dialog" aria-modal="true" aria-labelledby="detailCatalogTitle">
        <div class="cat-modal">
            <div class="cat-modal-header">
                <h3 class="cat-modal-title" id="detailCatalogTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    Chi tiết giá trị danh mục
                </h3>
                <button type="button" class="cat-modal-close" onclick="closeDetailModal()" aria-label="Đóng">&times;</button>
            </div>
            <div class="cat-modal-body">
                <div class="cat-detail-grid">
                    <span class="cat-detail-label">Mã Code:</span>
                    <span class="cat-detail-val" id="detailCode" style="font-weight: 700; color: var(--cat-primary);"></span>

                    <span class="cat-detail-label">Tên giá trị:</span>
                    <span class="cat-detail-val" id="detailName" style="font-weight: 600;"></span>

                    <span class="cat-detail-label">Loại danh mục:</span>
                    <span class="cat-detail-val" id="detailCategory"></span>

                    <span class="cat-detail-label">Thứ tự hiển thị:</span>
                    <span class="cat-detail-val" id="detailOrder" style="font-weight: 700;"></span>

                    <span class="cat-detail-label">Đang sử dụng:</span>
                    <span class="cat-detail-val" id="detailUsage"></span>

                    <span class="cat-detail-label">Trạng thái:</span>
                    <span class="cat-detail-val" id="detailStatus"></span>

                    <span class="cat-detail-label">Ngày tạo:</span>
                    <span class="cat-detail-val" id="detailCreatedAt" style="color: var(--cat-text-sub);"></span>

                    <span class="cat-detail-label">Cập nhật cuối:</span>
                    <span class="cat-detail-val" id="detailUpdatedAt" style="color: var(--cat-text-sub);"></span>

                    <span class="cat-detail-label">Mô tả:</span>
                    <span class="cat-detail-val" id="detailDesc" style="line-height: 1.5; color: var(--cat-text-sub);"></span>
                </div>
            </div>
            <div class="cat-modal-footer">
                <button type="button" class="btn-cat btn-cat-secondary" onclick="closeDetailModal()">Đóng</button>
                <button type="button" class="btn-cat btn-cat-primary" id="btnDetailToEdit">Chỉnh sửa</button>
            </div>
        </div>
    </div>

    <!-- MODAL 4: XÁC NHẬN XÓA / NGỪNG SỬ DỤNG (QUY TẮC MỤC 8) -->
    <div class="cat-modal-overlay" id="modalDeleteCatalog" role="dialog" aria-modal="true" aria-labelledby="deleteCatalogTitle">
        <div class="cat-modal" style="max-width: 480px;">
            <div class="cat-modal-header">
                <h3 class="cat-modal-title" id="deleteCatalogTitle" style="color: var(--cat-danger);">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    Xác nhận thao tác danh mục
                </h3>
                <button type="button" class="cat-modal-close" onclick="closeDeleteModal()" aria-label="Đóng">&times;</button>
            </div>
            <div class="cat-modal-body">
                <p style="margin: 0; line-height: 1.5; font-size: 0.92rem;">
                    Bạn đang yêu cầu xóa mục danh mục: <strong id="deleteItemName"></strong> (<span id="deleteItemCode"></span>).
                </p>

                <!-- Cảnh báo nếu đang có bản ghi sử dụng -->
                <div id="deleteWarningInUse" class="cat-hint-warning" style="margin-top: 12px; display: none;">
                    🛡️ <strong>Ràng buộc toàn vẹn dữ liệu (S2-07):</strong>
                    Mục danh mục này hiện đang được liên kết với <strong id="deleteUsageCount"></strong> bản ghi trong hệ thống CRM.
                    Hệ thống sẽ <strong>không xóa vật lý</strong> mà sẽ tự động chuyển sang trạng thái <strong>Ngừng sử dụng (INACTIVE)</strong> để bảo toàn lịch sử dữ liệu.
                </div>

                <!-- Thông báo nếu chưa được sử dụng -->
                <div id="deleteInfoZeroUse" style="margin-top: 12px; font-size: 0.85rem; color: var(--cat-text-sub); display: none;">
                    Mục này chưa được sử dụng bởi bất kỳ bản ghi nào trong hệ thống. Thao tác sẽ loại bỏ giá trị này khỏi danh mục.
                </div>
            </div>
            <div class="cat-modal-footer">
                <button type="button" class="btn-cat btn-cat-secondary" onclick="closeDeleteModal()">Hủy bỏ</button>
                <button type="button" class="btn-cat btn-cat-danger" id="btnConfirmDelete" onclick="confirmDeleteCatalog()">Xác nhận thực hiện</button>
            </div>
        </div>
    </div>

    <!-- JAVASCRIPT XỬ LÝ FRONTEND MOCK DATA & TƯƠNG TÁC (S2-07) -->
    <script>
        // Dữ liệu Mock toàn diện theo đặc tả S2-07
        let currentCategory = "INDUSTRY";
        let deletingItemId = null;

        let catalogsData = [
            // 1. NGÀNH NGHỀ (INDUSTRY)
            { id: 1, category: "INDUSTRY", code: "IT", name: "Công nghệ thông tin & Viễn thông", description: "Các công ty phần mềm, dịch vụ CNTT, hạ tầng mạng", displayOrder: 1, status: "ACTIVE", usageCount: 42, createdAt: "2026-01-10", updatedAt: "2026-02-15" },
            { id: 2, category: "INDUSTRY", code: "BANKING", name: "Tài chính - Ngân hàng - Bảo hiểm", description: "Ngân hàng thương mại, quỹ đầu tư, công ty bảo hiểm", displayOrder: 2, status: "ACTIVE", usageCount: 28, createdAt: "2026-01-10", updatedAt: "2026-02-20" },
            { id: 3, category: "INDUSTRY", code: "EDUCATION", name: "Giáo dục & Đào tạo", description: "Các trường đại học, viện nghiên cứu, trung tâm ngoại ngữ", displayOrder: 3, status: "ACTIVE", usageCount: 19, createdAt: "2026-01-10", updatedAt: "2026-01-15" },
            { id: 4, category: "INDUSTRY", code: "HEALTHCARE", name: "Y tế & Dược phẩm", description: "Bệnh viện, phòng khám đa khoa, doanh nghiệp sản xuất dược", displayOrder: 4, status: "ACTIVE", usageCount: 15, createdAt: "2026-01-11", updatedAt: "2026-02-01" },
            { id: 5, category: "INDUSTRY", code: "MANUFACTURING", name: "Sản xuất & Chế tạo", description: "Nhà máy công nghiệp nặng, chế biến lương thực, dệt may", displayOrder: 5, status: "ACTIVE", usageCount: 31, createdAt: "2026-01-11", updatedAt: "2026-02-10" },
            { id: 6, category: "INDUSTRY", code: "RETAIL", name: "Bán lẻ & Thương mại điện tử", description: "Chuỗi siêu thị, sàn TMĐT, cửa hàng tiện lợi", displayOrder: 6, status: "ACTIVE", usageCount: 24, createdAt: "2026-01-12", updatedAt: "2026-02-14" },
            { id: 7, category: "INDUSTRY", code: "LOGISTICS", name: "Vận tải & Kho bãi (Logistics)", description: "Đơn vị vận chuyển hàng hóa, giao nhận quốc tế, kho bãi", displayOrder: 7, status: "ACTIVE", usageCount: 12, createdAt: "2026-01-12", updatedAt: "2026-02-18" },
            { id: 8, category: "INDUSTRY", code: "TRADING_OLD", name: "Thương mại tổng hợp (Cũ)", description: "Mã ngành nghề cũ đã gộp vào Bán lẻ, ngừng áp dụng mới", displayOrder: 8, status: "INACTIVE", usageCount: 6, createdAt: "2025-12-01", updatedAt: "2026-01-05" },

            // 2. QUY MÔ DOANH NGHIỆP (COMPANY_SIZE)
            { id: 9, category: "COMPANY_SIZE", code: "STARTUP", name: "Startup (Khởi nghiệp)", description: "Quy mô từ 1 đến 10 nhân sự", displayOrder: 1, status: "ACTIVE", usageCount: 18, createdAt: "2026-01-05", updatedAt: "2026-01-20" },
            { id: 10, category: "COMPANY_SIZE", code: "SMALL", name: "Doanh nghiệp nhỏ (Small)", description: "Quy mô từ 11 đến 50 nhân sự", displayOrder: 2, status: "ACTIVE", usageCount: 45, createdAt: "2026-01-05", updatedAt: "2026-01-20" },
            { id: 11, category: "COMPANY_SIZE", code: "MEDIUM", name: "Doanh nghiệp vừa (Medium)", description: "Quy mô từ 51 đến 200 nhân sự", displayOrder: 3, status: "ACTIVE", usageCount: 38, createdAt: "2026-01-05", updatedAt: "2026-02-01" },
            { id: 12, category: "COMPANY_SIZE", code: "LARGE", name: "Doanh nghiệp lớn (Large)", description: "Quy mô từ 201 đến 1000 nhân sự", displayOrder: 4, status: "ACTIVE", usageCount: 14, createdAt: "2026-01-05", updatedAt: "2026-02-05" },
            { id: 13, category: "COMPANY_SIZE", code: "ENTERPRISE", name: "Tập đoàn (Enterprise)", description: "Quy mô trên 1000 nhân sự, nhiều chi nhánh đa quốc gia", displayOrder: 5, status: "ACTIVE", usageCount: 9, createdAt: "2026-01-05", updatedAt: "2026-02-12" },

            // 3. NGUỒN LEAD (LEAD_SOURCE)
            { id: 14, category: "LEAD_SOURCE", code: "WEBSITE", name: "Website trực tiếp", description: "Khách hàng đăng ký form tư vấn hoặc dùng thử qua cổng Web", displayOrder: 1, status: "ACTIVE", usageCount: 65, createdAt: "2026-01-02", updatedAt: "2026-01-25" },
            { id: 15, category: "LEAD_SOURCE", code: "FACEBOOK", name: "Mạng xã hội Facebook", description: "Chiến dịch quảng cáo Facebook Lead Ads & Fanpage", displayOrder: 2, status: "ACTIVE", usageCount: 52, createdAt: "2026-01-02", updatedAt: "2026-02-01" },
            { id: 16, category: "LEAD_SOURCE", code: "GOOGLE_ADS", name: "Quảng cáo Google Search", description: "Khách hàng click qua chiến dịch từ khóa Google Ads", displayOrder: 3, status: "ACTIVE", usageCount: 48, createdAt: "2026-01-02", updatedAt: "2026-02-05" },
            { id: 17, category: "LEAD_SOURCE", code: "REFERRAL", name: "Khách hàng giới thiệu", description: "Được khách hàng hiện tại hoặc nhân viên nội bộ giới thiệu", displayOrder: 4, status: "ACTIVE", usageCount: 34, createdAt: "2026-01-02", updatedAt: "2026-02-10" },
            { id: 18, category: "LEAD_SOURCE", code: "EVENT", name: "Hội thảo & Sự kiện", description: "Thu thập danh thiếp tại các hội thảo công nghệ, triển lãm B2B", displayOrder: 5, status: "ACTIVE", usageCount: 22, createdAt: "2026-01-02", updatedAt: "2026-02-15" },
            { id: 19, category: "LEAD_SOURCE", code: "COLD_CALL", name: "Tiếp cận trực tiếp (Cold Call)", description: "Nhân viên Telesales gọi điện tiếp cận theo danh bạ doanh nghiệp", displayOrder: 6, status: "ACTIVE", usageCount: 16, createdAt: "2026-01-02", updatedAt: "2026-02-18" },
            { id: 20, category: "LEAD_SOURCE", code: "PARTNER", name: "Đối tác chiến lược", description: "Khách hàng chuyển tiếp từ mạng lưới đại lý đối tác", displayOrder: 7, status: "ACTIVE", usageCount: 11, createdAt: "2026-01-02", updatedAt: "2026-02-22" },
            { id: 21, category: "LEAD_SOURCE", code: "OLD_CAMPAIGN", name: "Chiến dịch Email 2025", description: "Chiến dịch năm cũ đã đóng, ngừng ghi nhận Lead mới", displayOrder: 8, status: "INACTIVE", usageCount: 5, createdAt: "2025-11-01", updatedAt: "2026-01-01" },

            // 4. LOẠI HOẠT ĐỘNG (ACTIVITY_TYPE)
            { id: 22, category: "ACTIVITY_TYPE", code: "CALL", name: "Cuộc gọi điện thoại", description: "Cuộc gọi tư vấn, khảo sát nhanh hoặc xác nhận lịch hẹn", displayOrder: 1, status: "ACTIVE", usageCount: 120, createdAt: "2026-01-01", updatedAt: "2026-01-10" },
            { id: 23, category: "ACTIVITY_TYPE", code: "EMAIL", name: "Gửi Email báo giá & tài liệu", description: "Gửi hồ sơ năng lực, bảng báo giá hoặc hợp đồng mẫu", displayOrder: 2, status: "ACTIVE", usageCount: 95, createdAt: "2026-01-01", updatedAt: "2026-01-12" },
            { id: 24, category: "ACTIVITY_TYPE", code: "MEETING", name: "Cuộc gặp trực tiếp", description: "Gặp mặt đàm phán tại văn phòng khách hàng hoặc công ty", displayOrder: 3, status: "ACTIVE", usageCount: 58, createdAt: "2026-01-01", updatedAt: "2026-01-15" },
            { id: 25, category: "ACTIVITY_TYPE", code: "DEMO", name: "Demo giới thiệu phần mềm", description: "Trình diễn trực tiếp các tính năng sản phẩm cho khách hàng", displayOrder: 4, status: "ACTIVE", usageCount: 36, createdAt: "2026-01-01", updatedAt: "2026-01-18" },
            { id: 26, category: "ACTIVITY_TYPE", code: "FOLLOW_UP", name: "Theo dõi & Chăm sóc tiến độ", description: "Nhắc hẹn phản hồi báo giá, giải đáp thắc mắc phát sinh", displayOrder: 5, status: "ACTIVE", usageCount: 44, createdAt: "2026-01-01", updatedAt: "2026-01-20" },
            { id: 27, category: "ACTIVITY_TYPE", code: "SURVEY", name: "Khảo sát thực địa & Yêu cầu", description: "Chuyên viên khảo sát hạ tầng và nghiệp vụ chi tiết của khách", displayOrder: 6, status: "ACTIVE", usageCount: 20, createdAt: "2026-01-01", updatedAt: "2026-01-22" },
            { id: 28, category: "ACTIVITY_TYPE", code: "CUSTOMER_CARE", name: "Chăm sóc sau bán hàng", description: "Hỗ trợ khách hàng sau ký hợp đồng và ghi nhận phản hồi", displayOrder: 7, status: "ACTIVE", usageCount: 29, createdAt: "2026-01-01", updatedAt: "2026-01-25" }
        ];

        // Format Status Badge
        function getStatusBadge(status) {
            return status === "ACTIVE"
                ? '<span class="badge-status badge-status-active">● Hoạt động</span>'
                : '<span class="badge-status badge-status-inactive">○ Ngừng sử dụng</span>';
        }

        // Format Usage Badge
        function getUsageBadge(count) {
            if (count > 0) {
                return '<span class="badge-usage badge-usage-active" title="Đang được sử dụng trong CRM">' + count + ' bản ghi</span>';
            }
            return '<span class="badge-usage badge-usage-zero">Chưa dùng (0)</span>';
        }

        // Tên danh mục thân thiện
        function getCategoryLabel(cat) {
            switch(cat) {
                case "INDUSTRY": return "Ngành nghề";
                case "COMPANY_SIZE": return "Quy mô doanh nghiệp";
                case "LEAD_SOURCE": return "Nguồn Lead";
                case "ACTIVITY_TYPE": return "Loại hoạt động";
                default: return cat;
            }
        }

        // Cập nhật thống kê sơ bộ (KPI Cards)
        function updateKPIs() {
            const totalValues = catalogsData.length;
            const inUse = catalogsData.filter(i => i.usageCount > 0).length;
            const active = catalogsData.filter(i => i.status === "ACTIVE").length;

            document.getElementById("statTotalCatalogs").textContent = "4";
            document.getElementById("statTotalValues").textContent = totalValues;
            document.getElementById("statInUseCount").textContent = inUse;
            document.getElementById("statActiveCount").textContent = active;

            // Update tab badges
            document.getElementById("tabBadgeIndustry").textContent = catalogsData.filter(i => i.category === "INDUSTRY").length;
            document.getElementById("tabBadgeCompanySize").textContent = catalogsData.filter(i => i.category === "COMPANY_SIZE").length;
            document.getElementById("tabBadgeLeadSource").textContent = catalogsData.filter(i => i.category === "LEAD_SOURCE").length;
            document.getElementById("tabBadgeActivityType").textContent = catalogsData.filter(i => i.category === "ACTIVITY_TYPE").length;
        }

        // Chuyển đổi Tab danh mục
        function switchCategory(cat) {
            currentCategory = cat;
            document.querySelectorAll(".cat-tab-btn").forEach(btn => btn.classList.remove("active"));
            
            if (cat === "INDUSTRY") document.getElementById("tabBtnIndustry").classList.add("active");
            else if (cat === "COMPANY_SIZE") document.getElementById("tabBtnCompanySize").classList.add("active");
            else if (cat === "LEAD_SOURCE") document.getElementById("tabBtnLeadSource").classList.add("active");
            else if (cat === "ACTIVITY_TYPE") document.getElementById("tabBtnActivityType").classList.add("active");

            renderTable();
        }

        // Render Bảng dữ liệu của Tab hiện tại
        function renderTable() {
            const tbody = document.getElementById("catTableBody");
            const emptyState = document.getElementById("catEmptyState");

            const searchKw = (document.getElementById("catSearchInput").value || "").trim().toLowerCase();
            const statusFilter = document.getElementById("catStatusFilter").value;

            // Lọc theo Category
            let items = catalogsData.filter(i => i.category === currentCategory);

            // Lọc theo Search & Status
            items = items.filter(i => {
                const matchKw = !searchKw ||
                    i.code.toLowerCase().includes(searchKw) ||
                    i.name.toLowerCase().includes(searchKw) ||
                    (i.description && i.description.toLowerCase().includes(searchKw));
                const matchStatus = !statusFilter || i.status === statusFilter;
                return matchKw && matchStatus;
            });

            // Sắp xếp theo displayOrder
            items.sort((a, b) => a.displayOrder - b.displayOrder);

            if (items.length === 0) {
                tbody.innerHTML = "";
                emptyState.style.display = "flex";
                return;
            }

            emptyState.style.display = "none";
            let html = "";

            items.forEach((item, index) => {
                html += `
                    <tr>
                        <td style="text-align: center; color: var(--cat-text-muted);">${index + 1}</td>
                        <td style="font-weight: 700; color: var(--cat-primary);">${item.code}</td>
                        <td>
                            <div style="font-weight: 600; color: var(--cat-text-main);">${item.name}</div>
                        </td>
                        <td style="color: var(--cat-text-sub); font-size: 0.82rem;">${item.description || '<span style="color:#cbd5e1;">(Không có mô tả)</span>'}</td>
                        <td style="text-align: center;">
                            <div class="cat-order-controls">
                                <button type="button" class="cat-order-btn" title="Giảm thứ tự (Đưa lên trên)" onclick="moveOrder(${item.id}, -1)">▲</button>
                                <span style="font-weight: 700; min-width: 24px; text-align: center;">${item.displayOrder}</span>
                                <button type="button" class="cat-order-btn" title="Tăng thứ tự (Đưa xuống dưới)" onclick="moveOrder(${item.id}, 1)">▼</button>
                            </div>
                        </td>
                        <td style="text-align: center;">${getUsageBadge(item.usageCount)}</td>
                        <td style="text-align: center;">${getStatusBadge(item.status)}</td>
                        <td style="text-align: center;">
                            <div style="display: inline-flex; gap: 4px;">
                                <button type="button" class="btn-cat-icon" title="Xem chi tiết" onclick="openDetailModal(${item.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                                <button type="button" class="btn-cat-icon btn-cat-icon--edit" title="Chỉnh sửa" onclick="openEditModal(${item.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                    </svg>
                                </button>
                                <button type="button" class="btn-cat-icon btn-cat-icon--toggle" title="${item.status === 'ACTIVE' ? 'Chuyển sang Ngừng sử dụng' : 'Kích hoạt lại'}" onclick="toggleStatus(${item.id})">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <rect x="1" y="5" width="22" height="14" rx="7" ry="7"></rect>
                                        <circle cx="${item.status === 'ACTIVE' ? '16' : '8'}" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                                <button type="button" class="btn-cat-icon btn-cat-icon--delete" title="Xóa hoặc ngừng sử dụng" onclick="openDeleteModal(${item.id})">
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

            tbody.innerHTML = html;
        }

        // Tăng/Giảm thứ tự (Reorder)
        function moveOrder(id, delta) {
            const items = catalogsData.filter(i => i.category === currentCategory).sort((a, b) => a.displayOrder - b.displayOrder);
            const idx = items.findIndex(i => i.id === id);
            if (idx === -1) return;

            const targetIdx = idx + delta;
            if (targetIdx < 0 || targetIdx >= items.length) return;

            // Hoán đổi displayOrder giữa hai item
            const temp = items[idx].displayOrder;
            items[idx].displayOrder = items[targetIdx].displayOrder;
            items[targetIdx].displayOrder = temp;

            renderTable();
            showSuccessToast("Đã điều chỉnh thứ tự", "Thứ tự hiển thị của " + items[idx].name + " đã được cập nhật thành công.");
        }

        // Sắp xếp lại theo thứ tự chuẩn 1, 2, 3...
        function sortByOrder() {
            const items = catalogsData.filter(i => i.category === currentCategory).sort((a, b) => a.displayOrder - b.displayOrder);
            items.forEach((item, idx) => {
                item.displayOrder = idx + 1;
            });
            renderTable();
            showSuccessToast("Đã chuẩn hóa thứ tự", "Các giá trị trong danh mục " + getCategoryLabel(currentCategory) + " đã được sắp xếp từ 1 đến " + items.length + ".");
        }

        // Bật / Tắt trạng thái nhanh
        function toggleStatus(id) {
            const item = catalogsData.find(i => i.id === id);
            if (!item) return;

            item.status = item.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
            item.updatedAt = new Date().toISOString().split("T")[0];
            updateKPIs();
            renderTable();
            showSuccessToast("Cập nhật trạng thái", "Đã chuyển trạng thái mục " + item.code + " sang " + (item.status === "ACTIVE" ? "Đang hoạt động" : "Ngừng sử dụng") + ".");
        }

        // Đặt lại bộ lọc
        function resetFilters() {
            document.getElementById("catSearchInput").value = "";
            document.getElementById("catStatusFilter").value = "";
            renderTable();
        }

        // Modal Controls
        function openCreateModal() {
            document.getElementById("formCreateCatalog").reset();
            document.getElementById("createCategory").value = currentCategory;
            
            // Tính toán thứ tự tiếp theo
            const currentCatItems = catalogsData.filter(i => i.category === currentCategory);
            document.getElementById("createOrder").value = currentCatItems.length + 1;

            document.getElementById("modalCreateCatalog").classList.add("active");
        }

        function closeCreateModal() {
            document.getElementById("modalCreateCatalog").classList.remove("active");
        }

        function openEditModal(id) {
            const item = catalogsData.find(i => i.id === id);
            if (!item) return;

            document.getElementById("editId").value = item.id;
            document.getElementById("editCode").value = item.code;
            document.getElementById("editName").value = item.name;
            document.getElementById("editOrder").value = item.displayOrder;
            document.getElementById("editStatus").value = item.status;
            document.getElementById("editDesc").value = item.description || "";

            document.getElementById("modalEditCatalog").classList.add("active");
        }

        function closeEditModal() {
            document.getElementById("modalEditCatalog").classList.remove("active");
        }

        function openDetailModal(id) {
            const item = catalogsData.find(i => i.id === id);
            if (!item) return;

            document.getElementById("detailCode").textContent = item.code;
            document.getElementById("detailName").textContent = item.name;
            document.getElementById("detailCategory").textContent = getCategoryLabel(item.category) + " (" + item.category + ")";
            document.getElementById("detailOrder").textContent = item.displayOrder;
            document.getElementById("detailUsage").innerHTML = getUsageBadge(item.usageCount);
            document.getElementById("detailStatus").innerHTML = getStatusBadge(item.status);
            document.getElementById("detailCreatedAt").textContent = item.createdAt || "2026-01-01";
            document.getElementById("detailUpdatedAt").textContent = item.updatedAt || "2026-02-01";
            document.getElementById("detailDesc").textContent = item.description || "Không có mô tả bổ sung.";

            document.getElementById("btnDetailToEdit").onclick = function() {
                closeDetailModal();
                openEditModal(item.id);
            };

            document.getElementById("modalDetailCatalog").classList.add("active");
        }

        function closeDetailModal() {
            document.getElementById("modalDetailCatalog").classList.remove("active");
        }

        function openDeleteModal(id) {
            const item = catalogsData.find(i => i.id === id);
            if (!item) return;

            deletingItemId = id;
            document.getElementById("deleteItemName").textContent = item.name;
            document.getElementById("deleteItemCode").textContent = item.code;

            const warnInUse = document.getElementById("deleteWarningInUse");
            const infoZero = document.getElementById("deleteInfoZeroUse");
            const btnConfirm = document.getElementById("btnConfirmDelete");

            if (item.usageCount > 0) {
                document.getElementById("deleteUsageCount").textContent = item.usageCount;
                warnInUse.style.display = "block";
                infoZero.style.display = "none";
                btnConfirm.textContent = "Chuyển sang Ngừng sử dụng";
            } else {
                warnInUse.style.display = "none";
                infoZero.style.display = "block";
                btnConfirm.textContent = "Xác nhận xóa";
            }

            document.getElementById("modalDeleteCatalog").classList.add("active");
        }

        function closeDeleteModal() {
            deletingItemId = null;
            document.getElementById("modalDeleteCatalog").classList.remove("active");
        }

        // Xử lý Thêm mới (Form Submit)
        function handleCreateCatalog(e) {
            e.preventDefault();
            const cat = document.getElementById("createCategory").value;
            const code = document.getElementById("createCode").value.trim().toUpperCase();
            const name = document.getElementById("createName").value.trim();
            const order = parseInt(document.getElementById("createOrder").value) || 1;

            // Kiểm tra trùng lặp Code trong cùng Category
            const isDup = catalogsData.some(i => i.category === cat && i.code === code);
            if (isDup) {
                showErrorToast("Mã Code đã tồn tại", "Mã danh mục '" + code + "' đã tồn tại trong danh mục " + getCategoryLabel(cat) + ". Vui lòng chọn mã khác.");
                return;
            }

            const newItem = {
                id: Date.now(),
                category: cat,
                code: code,
                name: name,
                description: document.getElementById("createDesc").value.trim(),
                displayOrder: order,
                status: document.getElementById("createStatus").value,
                usageCount: 0,
                createdAt: new Date().toISOString().split("T")[0],
                updatedAt: new Date().toISOString().split("T")[0]
            };

            catalogsData.push(newItem);
            closeCreateModal();
            updateKPIs();
            
            // Nếu thêm vào danh mục khác thì chuyển sang tab đó
            if (currentCategory !== cat) {
                switchCategory(cat);
            } else {
                renderTable();
            }

            showSuccessToast("Thêm danh mục thành công", "Đã lưu giá trị mới '" + newItem.name + "' (" + newItem.code + ") vào danh mục.");
        }

        // Xử lý Cập nhật (Form Submit)
        function handleUpdateCatalog(e) {
            e.preventDefault();
            const id = parseInt(document.getElementById("editId").value);
            const item = catalogsData.find(i => i.id === id);
            if (!item) return;

            item.name = document.getElementById("editName").value.trim();
            item.description = document.getElementById("editDesc").value.trim();
            item.displayOrder = parseInt(document.getElementById("editOrder").value) || item.displayOrder;
            item.status = document.getElementById("editStatus").value;
            item.updatedAt = new Date().toISOString().split("T")[0];

            closeEditModal();
            updateKPIs();
            renderTable();
            showSuccessToast("Cập nhật thành công", "Đã cập nhật thông tin giá trị danh mục " + item.code + ".");
        }

        // Xử lý Xác nhận Xóa / Ngừng sử dụng (Quy tắc Mục 8)
        function confirmDeleteCatalog() {
            if (!deletingItemId) return;
            const index = catalogsData.findIndex(i => i.id === deletingItemId);
            if (index === -1) return;

            const item = catalogsData[index];

            if (item.usageCount > 0) {
                // Quy tắc: Nếu đang được sử dụng -> Không xóa vật lý, chuyển sang INACTIVE
                item.status = "INACTIVE";
                item.updatedAt = new Date().toISOString().split("T")[0];
                showSuccessToast("Đã chuyển sang Ngừng sử dụng", "Mục '" + item.name + "' đang được dùng bởi " + item.usageCount + " bản ghi nên đã được chuyển sang trạng thái Ngừng sử dụng để bảo toàn dữ liệu.");
            } else {
                // Xóa khỏi danh sách nếu chưa sử dụng
                catalogsData.splice(index, 1);
                showSuccessToast("Đã xóa danh mục", "Đã loại bỏ mục '" + item.name + "' (" + item.code + ") khỏi hệ thống.");
            }

            closeDeleteModal();
            updateKPIs();
            renderTable();
        }

        // Toast Banners
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

        // Lắng nghe sự kiện Search & Filter Realtime
        document.getElementById("catSearchInput").addEventListener("input", renderTable);
        document.getElementById("catStatusFilter").addEventListener("change", renderTable);

        // Đóng modal khi bấm phím Escape
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeCreateModal();
                closeEditModal();
                closeDetailModal();
                closeDeleteModal();
            }
        });

        // Khởi tạo ban đầu
        window.addEventListener("DOMContentLoaded", () => {
            updateKPIs();
            renderTable();
        });
    </script>
</body>
</html>
