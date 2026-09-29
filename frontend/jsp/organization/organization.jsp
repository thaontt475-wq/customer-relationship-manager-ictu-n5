<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cơ cấu tổ chức kinh doanh - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Cơ cấu tổ chức kinh doanh (CRM-42) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization/organization.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Cơ cấu tổ chức -->
        <main class="org-page" id="orgApp" role="main">
            <div class="org-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="org-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Cơ cấu tổ chức kinh doanh</span>
                </nav>

                <!-- Header màn hình -->
                <header class="org-header">
                    <div class="org-header-info">
                        <h1>Cơ cấu tổ chức kinh doanh & Phân cấp nhóm</h1>
                        <p>Quản lý sơ đồ cây phân cấp đơn vị bán hàng, chỉ định duy nhất một trưởng nhóm và phân bổ khu vực địa lý để bám đúng phạm vi dữ liệu thực tế.</p>
                    </div>

                    <div class="org-header-badges">
                        <span class="org-badge org-badge-scope" title="Cây tổ chức này quyết định trực tiếp phạm vi dữ liệu TEAM">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                            </svg>
                            Data Scope: TEAM
                        </span>
                        <span class="org-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                                <polyline points="2 17 12 22 22 17"></polyline>
                                <polyline points="2 12 12 17 22 12"></polyline>
                            </svg>
                            S2-06 / CRM-42
                        </span>
                    </div>
                </header>

                <!-- Khu vực thông báo phản hồi (Alerts) -->
                <div class="org-alerts" id="orgAlertsArea" aria-live="polite">
                    <div class="org-alert org-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="org-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="org-alert-content">
                            <div class="org-alert-title" id="globalErrorTitle">Đã xảy ra lỗi</div>
                            <div id="globalErrorMessage"></div>
                        </div>
                        <button type="button" class="org-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="org-alert org-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="org-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="org-alert-content">
                            <div class="org-alert-title" id="globalSuccessTitle">Thành công</div>
                            <div id="globalSuccessMessage"></div>
                        </div>
                        <button type="button" class="org-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>
                </div>

                <!-- Banner Nghiệp vụ Quyết định Phạm vi dữ liệu (AC 3) -->
                <aside class="org-scope-banner" role="region" aria-label="Quy định phạm vi dữ liệu tổ chức">
                    <svg class="org-scope-banner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                    <div>
                        <div class="org-scope-banner-title">Quy tắc phân quyền phạm vi dữ liệu sở hữu (AC 3)</div>
                        <p class="org-scope-banner-desc">
                            Cây tổ chức này quyết định trực tiếp phạm vi dữ liệu mà Trưởng nhóm được quyền xem (<strong>Data Scope: TEAM</strong>). Khi một người dùng được chỉ định làm Trưởng nhóm (Manager/Team Lead), hệ thống sẽ cấp quyền truy cập toàn bộ Khách hàng, Cơ hội, Báo giá và Hoạt động thuộc nhóm mình và tất cả các nhóm con trực thuộc phân cấp bên dưới.
                        </p>
                    </div>
                </aside>

                <!-- Thống kê nhanh cơ cấu tổ chức -->
                <section class="org-stats-grid" aria-label="Thống kê cơ cấu tổ chức">
                    <div class="org-stat-card">
                        <div class="org-stat-icon-wrap stat-icon-blue">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                            </svg>
                        </div>
                        <div class="org-stat-content">
                            <span class="org-stat-value" id="statTotalUnits">6</span>
                            <span class="org-stat-label">Tổng số đơn vị / nhóm</span>
                        </div>
                    </div>

                    <div class="org-stat-card">
                        <div class="org-stat-icon-wrap stat-icon-purple">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                            </svg>
                        </div>
                        <div class="org-stat-content">
                            <span class="org-stat-value" id="statTotalMembers">24</span>
                            <span class="org-stat-label">Tổng nhân sự kinh doanh</span>
                        </div>
                    </div>

                    <div class="org-stat-card">
                        <div class="org-stat-icon-wrap stat-icon-amber">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <polyline points="12 6 12 12 16 14"></polyline>
                            </svg>
                        </div>
                        <div class="org-stat-content">
                            <span class="org-stat-value" id="statTreeDepth">3 cấp</span>
                            <span class="org-stat-label">Độ sâu phân cấp tối đa</span>
                        </div>
                    </div>

                    <div class="org-stat-card">
                        <div class="org-stat-icon-wrap stat-icon-teal">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="2" y1="12" x2="22" y2="12"></line>
                                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
                            </svg>
                        </div>
                        <div class="org-stat-content">
                            <span class="org-stat-value" id="statRegionsCount">3 vùng</span>
                            <span class="org-stat-label">Khu vực địa lý hoạt động</span>
                        </div>
                    </div>
                </section>

                <!-- Card Nội dung chính (Cơ cấu tổ chức) -->
                <section class="org-card" style="position: relative;" aria-labelledby="orgCardTitle">

                    <!-- Loading Overlay -->
                    <div class="org-loading-overlay" id="orgLoadingOverlay" aria-hidden="true">
                        <div class="org-spinner"></div>
                    </div>

                    <!-- Toolbar & Bộ lọc -->
                    <div class="org-toolbar">
                        <div class="org-toolbar-left">
                            <!-- Ô tìm kiếm tên nhóm / trưởng nhóm -->
                            <div class="org-search-wrap">
                                <svg class="org-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="search" id="orgSearchInput" class="org-search-input"
                                       placeholder="Tìm nhóm hoặc trưởng nhóm..."
                                       aria-label="Tìm kiếm đơn vị">
                            </div>

                            <!-- Bộ lọc Khu vực địa lý (AC 4) -->
                            <select id="filterRegion" class="org-filter-select" aria-label="Lọc theo khu vực địa lý">
                                <option value="">Tất cả khu vực địa lý</option>
                                <option value="NORTH">Miền Bắc</option>
                                <option value="CENTRAL">Miền Trung</option>
                                <option value="SOUTH">Miền Nam</option>
                                <option value="NATIONAL">Toàn quốc</option>
                                <option value="OVERSEAS">Quốc tế / Hải ngoại</option>
                            </select>

                            <!-- Nhóm nút Chuyển chế độ xem (Sơ đồ cây vs Danh sách bảng) -->
                            <div class="view-switch-group" role="group" aria-label="Chế độ xem">
                                <button type="button" class="view-switch-btn active" id="btnViewTree" title="Xem sơ đồ dạng cây phân cấp">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <rect x="3" y="3" width="7" height="7"></rect>
                                        <rect x="14" y="3" width="7" height="7"></rect>
                                        <rect x="14" y="14" width="7" height="7"></rect>
                                        <rect x="3" y="14" width="7" height="7"></rect>
                                    </svg>
                                    <span>Sơ đồ cây</span>
                                </button>
                                <button type="button" class="view-switch-btn" id="btnViewTable" title="Xem danh sách bảng">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <line x1="8" y1="6" x2="21" y2="6"></line>
                                        <line x1="8" y1="12" x2="21" y2="12"></line>
                                        <line x1="8" y1="18" x2="21" y2="18"></line>
                                        <line x1="3" y1="6" x2="3.01" y2="6"></line>
                                        <line x1="3" y1="12" x2="3.01" y2="12"></line>
                                        <line x1="3" y1="18" x2="3.01" y2="18"></line>
                                    </svg>
                                    <span>Danh sách</span>
                                </button>
                            </div>
                        </div>

                        <div class="org-toolbar-right">
                            <button type="button" class="btn btn-secondary btn-sm" id="btnExpandAll" title="Mở rộng tất cả các nhánh">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="7 13 12 18 17 13"></polyline>
                                    <polyline points="7 6 12 11 17 6"></polyline>
                                </svg>
                                Mở rộng
                            </button>
                            <button type="button" class="btn btn-secondary btn-sm" id="btnCollapseAll" title="Thu gọn các nhánh con">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="17 11 12 6 7 11"></polyline>
                                    <polyline points="17 18 12 13 7 18"></polyline>
                                </svg>
                                Thu gọn
                            </button>
                            <!-- Nút Thêm đơn vị / Nhóm mới -->
                            <button type="button" class="btn btn-primary" id="btnOpenCreateUnitModal">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                <span>Thêm đơn vị mới</span>
                            </button>
                        </div>
                    </div>

                    <!-- CHẾ ĐỘ 1: SƠ ĐỒ CÂY PHÂN CẤP (ORG TREE VIEW - AC 1) -->
                    <div class="org-tree-view-wrapper" id="orgTreeViewArea">
                        <ul class="org-tree" id="orgTreeRoot">
                            <!-- Render cây phân cấp tự động bằng JS -->
                        </ul>
                    </div>

                    <!-- CHẾ ĐỘ 2: BẢNG DANH SÁCH ĐƠN VỊ (TABLE VIEW) -->
                    <div class="org-table-responsive" id="orgTableViewArea" style="display: none;">
                        <table class="org-table" id="orgTable" aria-label="Bảng cơ cấu tổ chức">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 50px;">#</th>
                                    <th scope="col">Tên đơn vị / Nhóm kinh doanh</th>
                                    <th scope="col">Trưởng nhóm (Manager)</th>
                                    <th scope="col">Khu vực địa lý</th>
                                    <th scope="col">Đơn vị cấp trên (Parent)</th>
                                    <th scope="col" style="text-align: center;">Số thành viên</th>
                                    <th scope="col" style="text-align: center;">Trạng thái</th>
                                    <th scope="col" style="text-align: center; width: 140px;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="orgTableBody">
                                <!-- Render danh sách bằng JS -->
                            </tbody>
                        </table>
                    </div>

                </section>

            </div>
        </main>
    </div>

    <!-- =================================================================
         MODAL: THÊM / CHỈNH SỬA ĐƠN VỊ / NHÓM KINH DOANH (AC 1, AC 4)
         ================================================================= -->
    <div class="crm-modal-overlay" id="unitModal" role="dialog" aria-modal="true" aria-labelledby="unitModalTitle">
        <div class="crm-modal-card">
            <header class="crm-modal-header">
                <h3 class="crm-modal-title" id="unitModalTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                    </svg>
                    <span id="unitModalHeading">Thêm đơn vị / Nhóm mới</span>
                </h3>
                <button type="button" class="crm-modal-close" id="btnCloseUnitModal" aria-label="Đóng">&times;</button>
            </header>

            <form id="unitForm" novalidate>
                <input type="hidden" id="formUnitId" name="id">

                <div class="crm-modal-body">
                    <!-- Tên nhóm / đơn vị -->
                    <div class="form-group">
                        <label for="formUnitName" class="form-label">
                            <span>Tên đơn vị / Nhóm kinh doanh <span class="required-mark">*</span></span>
                        </label>
                        <input type="text" id="formUnitName" name="name" class="form-control"
                               placeholder="VD: Nhóm Kinh doanh Doanh nghiệp Enterprise Miền Bắc" required>
                        <div class="form-feedback" id="feedbackUnitName"></div>
                    </div>

                    <!-- Đơn vị cha (Parent Unit - AC 1: Cấu trúc cây & Chống vòng lặp) -->
                    <div class="form-group">
                        <label for="formUnitParent" class="form-label">
                            <span>Đơn vị trực thuộc cấp trên (Parent Unit)</span>
                            <span class="form-hint">Để trống nếu là Đơn vị gốc</span>
                        </label>
                        <select id="formUnitParent" name="parentId" class="form-control">
                            <option value="">-- Là đơn vị gốc (Không có đơn vị cha) --</option>
                            <!-- Tùy chọn sẽ được điền động từ JS kèm chống chọn chính nó -->
                        </select>
                        <div class="form-feedback" id="feedbackUnitParent"></div>
                    </div>

                    <!-- Trưởng nhóm duy nhất (Manager / Team Lead - AC 1) -->
                    <div class="form-group">
                        <label for="formUnitManager" class="form-label">
                            <span>Trưởng nhóm (Manager / Team Lead) <span class="required-mark">*</span></span>
                            <span class="form-hint">Mỗi nhóm có duy nhất 1 trưởng nhóm</span>
                        </label>
                        <select id="formUnitManager" name="managerId" class="form-control" required>
                            <option value="">-- Chọn nhân sự làm trưởng nhóm --</option>
                            <option value="1">Trần Văn Hùng (Giám đốc kinh doanh)</option>
                            <option value="2">Nguyễn Văn Thắng (Giám đốc vùng Miền Bắc)</option>
                            <option value="3">Lê Thị Mai (Trưởng nhóm Enterprise)</option>
                            <option value="4">Phạm Quốc Toàn (Trưởng nhóm SME & Giáo dục)</option>
                            <option value="5">Hoàng Minh Tuấn (Giám đốc vùng Miền Nam)</option>
                            <option value="6">Vũ Hoàng Long (Trưởng nhóm B2B Miền Nam)</option>
                        </select>
                        <div class="form-feedback" id="feedbackUnitManager"></div>
                    </div>

                    <!-- Khu vực địa lý (Region - AC 4) -->
                    <div class="form-group">
                        <label for="formUnitRegion" class="form-label">
                            <span>Khu vực địa lý phụ trách <span class="required-mark">*</span></span>
                        </label>
                        <select id="formUnitRegion" name="region" class="form-control" required>
                            <option value="NORTH">Miền Bắc (Hà Nội & các tỉnh phía Bắc)</option>
                            <option value="CENTRAL">Miền Trung (Đà Nẵng & Tây Nguyên)</option>
                            <option value="SOUTH">Miền Nam (TP.HCM & miền Nam)</option>
                            <option value="NATIONAL">Toàn quốc (National)</option>
                            <option value="OVERSEAS">Quốc tế / Hải ngoại</option>
                        </select>
                        <div class="form-feedback" id="feedbackUnitRegion"></div>
                    </div>
                </div>

                <footer class="crm-modal-footer">
                    <button type="button" class="btn btn-secondary" id="btnCancelUnitModal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveUnit">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        <span>Lưu thông tin</span>
                    </button>
                </footer>
            </form>
        </div>
    </div>

    <!-- =================================================================
         DRAWER: CHI TIẾT THÀNH VIÊN TRONG NHÓM (AC 2 & AC 3)
         ================================================================= -->
    <div class="org-drawer-overlay" id="unitDrawer" role="dialog" aria-modal="true" aria-labelledby="drawerTitle">
        <div class="org-drawer-content">
            <header class="org-drawer-header">
                <div>
                    <h3 class="org-drawer-title" id="drawerTitle">Danh sách thành viên nhóm</h3>
                    <p class="org-drawer-subtitle" id="drawerSubtitle">Khối Kinh doanh Toàn quốc</p>
                </div>
                <button type="button" class="org-drawer-close" id="btnCloseDrawer" aria-label="Đóng">&times;</button>
            </header>

            <div class="org-drawer-body">
                <!-- Quy tắc AC 2: Mỗi nhân viên chỉ thuộc đúng 1 nhóm -->
                <div class="org-ac2-rule-card">
                    <strong>Quy định cơ cấu nhân sự (AC 2):</strong> Mỗi nhân viên chỉ thuộc đúng một nhóm kinh doanh tại một thời điểm nhất định để bảo đảm tính nhất quán trong phân bổ cơ hội bán hàng và tính toán doanh số/KPI.
                </div>

                <!-- Thông tin Trưởng nhóm -->
                <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px 14px;">
                    <div style="font-size: 0.75rem; text-transform: uppercase; color: #64748b; font-weight: 700; margin-bottom: 6px;">
                        Trưởng nhóm phụ trách (Manager)
                    </div>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div class="org-manager-avatar" id="drawerManagerAvatar">H</div>
                        <div>
                            <div style="font-weight: 700; color: #0f172a;" id="drawerManagerName">Trần Văn Hùng</div>
                            <div style="font-size: 0.8125rem; color: #2563eb;" id="drawerManagerRole">Giám đốc kinh doanh</div>
                        </div>
                    </div>
                </div>

                <!-- Danh sách nhân viên trong nhóm -->
                <div>
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                        <h4 style="margin: 0; font-size: 0.9375rem; color: #0f172a;">Nhân sự thuộc nhóm (<span id="drawerMemberCount">4</span>)</h4>
                    </div>

                    <ul class="org-members-list" id="drawerMembersList">
                        <!-- Render danh sách nhân viên từ JS -->
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- =================================================================
         JAVASCRIPT DOM THUẦN & FETCH API TÍCH HỢP (CRM-42)
         ================================================================= -->
    <script>
    (function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}' || '';

        // Dữ liệu ban đầu mẫu chuẩn B2B thực tế (5-6 đơn vị tổ chức chuẩn cây phân cấp)
        var INITIAL_UNITS = [
            {
                id: 1,
                name: 'Khối Kinh doanh Toàn quốc (National Sales Division)',
                parentId: null,
                managerId: 1,
                managerName: 'Trần Văn Hùng',
                managerRole: 'Giám đốc kinh doanh',
                region: 'NATIONAL',
                active: true,
                memberCount: 24,
                members: [
                    { id: 101, name: 'Nguyễn Thị Bích', email: 'bichnt@crm.ictu.vn', role: 'Thư ký kinh doanh', joinedDate: '2024-01-15' },
                    { id: 102, name: 'Lê Hoàng Nam', email: 'namlh@crm.ictu.vn', role: 'Chuyên viên điều phối', joinedDate: '2024-03-01' }
                ]
            },
            {
                id: 2,
                name: 'Trung tâm Kinh doanh Miền Bắc (Northern Region)',
                parentId: 1,
                managerId: 2,
                managerName: 'Nguyễn Văn Thắng',
                managerRole: 'Giám đốc vùng Miền Bắc',
                region: 'NORTH',
                active: true,
                memberCount: 11,
                members: [
                    { id: 201, name: 'Trần Đình Trọng', email: 'trongtd@crm.ictu.vn', role: 'Sales Lead', joinedDate: '2024-02-10' }
                ]
            },
            {
                id: 3,
                name: 'Nhóm Bán hàng Doanh nghiệp Enterprise - Miền Bắc',
                parentId: 2,
                managerId: 3,
                managerName: 'Lê Thị Mai',
                managerRole: 'Trưởng nhóm Enterprise',
                region: 'NORTH',
                active: true,
                memberCount: 6,
                members: [
                    { id: 301, name: 'Phạm Minh Đức', email: 'ducpm@crm.ictu.vn', role: 'Senior Sales Rep', joinedDate: '2024-04-12' },
                    { id: 302, name: 'Vũ Thu Trang', email: 'trangvt@crm.ictu.vn', role: 'Sales Rep', joinedDate: '2024-05-18' },
                    { id: 303, name: 'Đỗ Anh Tuấn', email: 'tuanda@crm.ictu.vn', role: 'Sales Executive', joinedDate: '2024-06-20' }
                ]
            },
            {
                id: 4,
                name: 'Nhóm Kinh doanh SME & Khối Giáo dục - Miền Bắc',
                parentId: 2,
                managerId: 4,
                managerName: 'Phạm Quốc Toàn',
                managerRole: 'Trưởng nhóm SME & Giáo dục',
                region: 'NORTH',
                active: true,
                memberCount: 5,
                members: [
                    { id: 401, name: 'Bùi Lan Anh', email: 'anhbl@crm.ictu.vn', role: 'Sales Rep', joinedDate: '2024-07-01' },
                    { id: 402, name: 'Ngô Quốc Huy', email: 'huyng@crm.ictu.vn', role: 'Sales Rep', joinedDate: '2024-08-15' }
                ]
            },
            {
                id: 5,
                name: 'Trung tâm Kinh doanh Miền Nam (Southern Region)',
                parentId: 1,
                managerId: 5,
                managerName: 'Hoàng Minh Tuấn',
                managerRole: 'Giám đốc vùng Miền Nam',
                region: 'SOUTH',
                active: true,
                memberCount: 12,
                members: [
                    { id: 501, name: 'Lê Thanh Thảo', email: 'thaolt@crm.ictu.vn', role: 'Điều phối viên Miền Nam', joinedDate: '2024-03-10' }
                ]
            },
            {
                id: 6,
                name: 'Nhóm Bán hàng B2B & Kênh Đối tác - Miền Nam',
                parentId: 5,
                managerId: 6,
                managerName: 'Vũ Hoàng Long',
                managerRole: 'Trưởng nhóm B2B & Kênh Đối tác',
                region: 'SOUTH',
                active: true,
                memberCount: 7,
                members: [
                    { id: 601, name: 'Đinh Công Thành', email: 'thanhdc@crm.ictu.vn', role: 'Partner Specialist', joinedDate: '2024-04-01' },
                    { id: 602, name: 'Trương Ngọc Ánh', email: 'anhtn@crm.ictu.vn', role: 'Sales Rep', joinedDate: '2024-05-15' }
                ]
            }
        ];

        // Danh sách Trưởng nhóm mẫu
        var MANAGERS_MAP = {
            1: { name: 'Trần Văn Hùng', role: 'Giám đốc kinh doanh' },
            2: { name: 'Nguyễn Văn Thắng', role: 'Giám đốc vùng Miền Bắc' },
            3: { name: 'Lê Thị Mai', role: 'Trưởng nhóm Enterprise' },
            4: { name: 'Phạm Quốc Toàn', role: 'Trưởng nhóm SME & Giáo dục' },
            5: { name: 'Hoàng Minh Tuấn', role: 'Giám đốc vùng Miền Nam' },
            6: { name: 'Vũ Hoàng Long', role: 'Trưởng nhóm B2B Miền Nam' }
        };

        var REGION_LABELS = {
            'NORTH': { text: 'Miền Bắc', cls: 'region-north' },
            'CENTRAL': { text: 'Miền Trung', cls: 'region-central' },
            'SOUTH': { text: 'Miền Nam', cls: 'region-south' },
            'NATIONAL': { text: 'Toàn quốc', cls: 'region-national' },
            'OVERSEAS': { text: 'Quốc tế', cls: 'region-overseas' }
        };

        // State quản lý
        var state = {
            units: JSON.parse(JSON.stringify(INITIAL_UNITS)),
            viewMode: 'tree', // 'tree' hoặc 'table'
            keyword: '',
            filterRegion: '',
            collapsedNodes: {} // lưu trạng thái thu gọn node ID
        };

        // DOM Elements
        var orgTreeRoot = document.getElementById('orgTreeRoot');
        var orgTableBody = document.getElementById('orgTableBody');
        var orgTreeViewArea = document.getElementById('orgTreeViewArea');
        var orgTableViewArea = document.getElementById('orgTableViewArea');
        var btnViewTree = document.getElementById('btnViewTree');
        var btnViewTable = document.getElementById('btnViewTable');
        var btnExpandAll = document.getElementById('btnExpandAll');
        var btnCollapseAll = document.getElementById('btnCollapseAll');
        var orgSearchInput = document.getElementById('orgSearchInput');
        var filterRegion = document.getElementById('filterRegion');
        var orgLoadingOverlay = document.getElementById('orgLoadingOverlay');

        var statTotalUnits = document.getElementById('statTotalUnits');
        var statTotalMembers = document.getElementById('statTotalMembers');
        var statTreeDepth = document.getElementById('statTreeDepth');
        var statRegionsCount = document.getElementById('statRegionsCount');

        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');
        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');

        // Modal Elements
        var unitModal = document.getElementById('unitModal');
        var unitForm = document.getElementById('unitForm');
        var unitModalHeading = document.getElementById('unitModalHeading');
        var btnOpenCreateUnitModal = document.getElementById('btnOpenCreateUnitModal');
        var btnCloseUnitModal = document.getElementById('btnCloseUnitModal');
        var btnCancelUnitModal = document.getElementById('btnCancelUnitModal');

        var formUnitId = document.getElementById('formUnitId');
        var formUnitName = document.getElementById('formUnitName');
        var formUnitParent = document.getElementById('formUnitParent');
        var formUnitManager = document.getElementById('formUnitManager');
        var formUnitRegion = document.getElementById('formUnitRegion');

        // Drawer Elements
        var unitDrawer = document.getElementById('unitDrawer');
        var btnCloseDrawer = document.getElementById('btnCloseDrawer');
        var drawerTitle = document.getElementById('drawerTitle');
        var drawerSubtitle = document.getElementById('drawerSubtitle');
        var drawerManagerName = document.getElementById('drawerManagerName');
        var drawerManagerRole = document.getElementById('drawerManagerRole');
        var drawerManagerAvatar = document.getElementById('drawerManagerAvatar');
        var drawerMemberCount = document.getElementById('drawerMemberCount');
        var drawerMembersList = document.getElementById('drawerMembersList');

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

        // Cập nhật thống kê nhanh
        function updateStats() {
            var total = state.units.length;
            var members = state.units.reduce(function (sum, u) { return sum + (u.memberCount || 0); }, 0);
            var regions = {};
            state.units.forEach(function (u) {
                if (u.region) regions[u.region] = true;
            });
            var regionCount = Object.keys(regions).length;

            statTotalUnits.textContent = total;
            statTotalMembers.textContent = members;
            statRegionsCount.textContent = regionCount + ' vùng';

            // Tính độ sâu cây
            var maxDepth = 1;
            function calcDepth(parentId, currentDepth) {
                if (currentDepth > maxDepth) maxDepth = currentDepth;
                var children = state.units.filter(function (u) { return u.parentId === parentId; });
                children.forEach(function (c) {
                    calcDepth(c.id, currentDepth + 1);
                });
            }
            var roots = state.units.filter(function (u) { return !u.parentId; });
            roots.forEach(function (r) { calcDepth(r.id, 1); });
            statTreeDepth.textContent = maxDepth + ' cấp';
        }

        // Xây dựng cấu trúc cây (Tree Hierarchy)
        function buildTreeData(units) {
            var map = {};
            var roots = [];

            units.forEach(function (u) {
                map[u.id] = Object.assign({}, u, { children: [] });
            });

            units.forEach(function (u) {
                if (u.parentId && map[u.parentId]) {
                    map[u.parentId].children.push(map[u.id]);
                } else {
                    roots.push(map[u.id]);
                }
            });

            return roots;
        }

        // Lọc theo từ khóa & vùng
        function filterUnits(units) {
            var kw = (state.keyword || '').trim().toLowerCase();
            var reg = state.filterRegion;

            return units.filter(function (u) {
                var matchKw = true;
                if (kw) {
                    var nameMatch = (u.name || '').toLowerCase().indexOf(kw) !== -1;
                    var mgrMatch = (u.managerName || '').toLowerCase().indexOf(kw) !== -1;
                    matchKw = nameMatch || mgrMatch;
                }
                var matchReg = true;
                if (reg) {
                    matchReg = u.region === reg;
                }
                return matchKw && matchReg;
            });
        }

        // Render Sơ đồ cây (Hierarchy Tree - AC 1)
        function renderTreeView() {
            orgTreeRoot.innerHTML = '';
            var roots = buildTreeData(state.units);

            if (roots.length === 0) {
                orgTreeRoot.innerHTML = '<li style="padding: 40px; text-align: center; color: #64748b;">Chưa có cơ cấu tổ chức nào. Hãy nhấn "+ Thêm đơn vị mới" để khởi tạo.</li>';
                return;
            }

            roots.forEach(function (rootNode) {
                orgTreeRoot.appendChild(createTreeNodeElement(rootNode, 0));
            });
        }

        function createTreeNodeElement(node, level) {
            var li = document.createElement('li');
            li.className = 'org-tree-node';

            var hasChildren = node.children && node.children.length > 0;
            var isCollapsed = Boolean(state.collapsedNodes[node.id]);

            // Khu vực địa lý
            var regInfo = REGION_LABELS[node.region] || { text: node.region, cls: 'region-national' };

            // Card Đơn vị
            var card = document.createElement('div');
            card.className = 'org-node-card level-' + Math.min(level, 3);

            var toggleBtnHtml = hasChildren
                ? '<button type="button" class="org-node-toggle-btn" data-toggle-id="' + node.id + '" title="' + (isCollapsed ? 'Mở rộng nhánh con' : 'Thu gọn nhánh con') + '">' +
                    (isCollapsed ? '+' : '−') +
                  '</button>'
                : '<span style="width: 24px; display: inline-block;"></span>';

            card.innerHTML =
                '<div class="org-node-header">' +
                    '<div class="org-node-title-wrap">' +
                        toggleBtnHtml +
                        '<span class="org-node-name">' + escapeHtml(node.name) + '</span>' +
                    '</div>' +
                    '<span class="badge-region ' + regInfo.cls + '">' +
                        '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line></svg>' +
                        escapeHtml(regInfo.text) +
                    '</span>' +
                '</div>' +
                '<div class="org-node-body">' +
                    '<!-- Trưởng nhóm duy nhất (AC 1) -->' +
                    '<div class="org-manager-info" title="Trưởng nhóm duy nhất của đơn vị (AC 1)">' +
                        '<div class="org-manager-avatar">' +
                            escapeHtml((node.managerName || 'U').charAt(0).toUpperCase()) +
                        '</div>' +
                        '<div class="org-manager-text">' +
                            '<span class="org-manager-name">' + escapeHtml(node.managerName || 'Chưa gán') + '</span>' +
                            '<span class="org-manager-role">' + escapeHtml(node.managerRole || 'Trưởng nhóm') + '</span>' +
                        '</div>' +
                    '</div>' +
                    '<!-- Thao tác -->' +
                    '<div class="org-node-actions">' +
                        '<button type="button" class="badge-members" data-action="view-members" data-id="' + node.id + '" title="Xem danh sách nhân viên thuộc nhóm (AC 2)">' +
                            '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle></svg>' +
                            (node.memberCount || 0) + ' nhân sự' +
                        '</button>' +
                        '<button type="button" class="org-action-icon-btn" data-action="add-child" data-id="' + node.id + '" title="Thêm đơn vị con trực thuộc">' +
                            '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>' +
                        '</button>' +
                        '<button type="button" class="org-action-icon-btn" data-action="edit" data-id="' + node.id + '" title="Chỉnh sửa thông tin đơn vị">' +
                            '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>' +
                        '</button>' +
                    '</div>' +
                '</div>';

            li.appendChild(card);

            // Nhánh con nếu có
            if (hasChildren) {
                var childrenUl = document.createElement('ul');
                childrenUl.className = 'org-children-container' + (isCollapsed ? ' collapsed' : '');
                node.children.forEach(function (childNode) {
                    childrenUl.appendChild(createTreeNodeElement(childNode, level + 1));
                });
                li.appendChild(childrenUl);
            }

            return li;
        }

        // Render Bảng Danh sách (Table View)
        function renderTableView() {
            orgTableBody.innerHTML = '';
            var filtered = filterUnits(state.units);

            if (filtered.length === 0) {
                orgTableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 32px; color: #64748b;">Không tìm thấy đơn vị nào phù hợp với bộ lọc.</td></tr>';
                return;
            }

            filtered.forEach(function (u, index) {
                var parent = state.units.find(function (p) { return p.id === u.parentId; });
                var regInfo = REGION_LABELS[u.region] || { text: u.region, cls: 'region-national' };

                var tr = document.createElement('tr');
                tr.innerHTML =
                    '<td><strong>' + (index + 1) + '</strong></td>' +
                    '<td><strong>' + escapeHtml(u.name) + '</strong></td>' +
                    '<td>' +
                        '<div style="display:flex;align-items:center;gap:8px;">' +
                            '<span style="width:28px;height:28px;border-radius:50%;background:#eff6ff;color:#2563eb;display:inline-flex;align-items:center;justify-content:center;font-weight:700;font-size:0.75rem;">' +
                                escapeHtml((u.managerName || 'U').charAt(0).toUpperCase()) +
                            '</span>' +
                            '<div>' +
                                '<div style="font-weight:600;color:#0f172a;">' + escapeHtml(u.managerName || 'Chưa gán') + '</div>' +
                                '<div style="font-size:0.75rem;color:#64748b;">' + escapeHtml(u.managerRole || 'Trưởng nhóm') + '</div>' +
                            '</div>' +
                        '</div>' +
                    '</td>' +
                    '<td><span class="badge-region ' + regInfo.cls + '">' + escapeHtml(regInfo.text) + '</span></td>' +
                    '<td>' + (parent ? '<span style="color:#2563eb;font-weight:500;">' + escapeHtml(parent.name) + '</span>' : '<span style="color:#94a3b8;font-style:italic;">Đơn vị gốc (Root)</span>') + '</td>' +
                    '<td style="text-align: center;"><span class="badge-members" style="cursor:pointer;" data-action="view-members" data-id="' + u.id + '">' + (u.memberCount || 0) + ' nhân sự</span></td>' +
                    '<td style="text-align: center;"><span class="badge-region region-south" style="background:#ecfdf5;color:#047857;border-color:#a7f3d0;">Hoạt động</span></td>' +
                    '<td>' +
                        '<div style="display:flex;align-items:center;gap:6px;justify-content:center;">' +
                            '<button type="button" class="btn btn-secondary btn-sm" data-action="view-members" data-id="' + u.id + '">Thành viên</button>' +
                            '<button type="button" class="btn btn-primary btn-sm" data-action="edit" data-id="' + u.id + '">Sửa</button>' +
                        '</div>' +
                    '</td>';
                orgTableBody.appendChild(tr);
            });
        }

        // Tải danh sách đơn vị từ Backend (GET /api/organization/units)
        async function fetchUnits() {
            orgLoadingOverlay.style.display = 'flex';
            hideAlerts();

            var endpoint = contextPath + '/api/organization/units';
            try {
                var res = await fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });

                orgLoadingOverlay.style.display = 'none';

                if (res.ok) {
                    var data = await res.json();
                    var list = (data && data.data && Array.isArray(data.data.items))
                        ? data.data.items
                        : (Array.isArray(data) ? data : null);

                    if (list && list.length > 0) {
                        state.units = list;
                    }
                } else {
                    console.info('Backend /api/organization/units trả mã ' + res.status + ' - Sử dụng fallback local data cho CRM-42.');
                }
            } catch (err) {
                orgLoadingOverlay.style.display = 'none';
                console.info('Chưa kết nối Backend Servlet - Sử dụng dữ liệu mẫu cơ cấu tổ chức CRM-42:', err);
            }

            updateStats();
            renderCurrentView();
        }

        function renderCurrentView() {
            if (state.viewMode === 'tree') {
                orgTreeViewArea.style.display = 'block';
                orgTableViewArea.style.display = 'none';
                renderTreeView();
            } else {
                orgTreeViewArea.style.display = 'none';
                orgTableViewArea.style.display = 'block';
                renderTableView();
            }
        }

        // Chuyển đổi View Switcher
        btnViewTree.addEventListener('click', function () {
            state.viewMode = 'tree';
            btnViewTree.classList.add('active');
            btnViewTable.classList.remove('active');
            renderCurrentView();
        });

        btnViewTable.addEventListener('click', function () {
            state.viewMode = 'table';
            btnViewTable.classList.add('active');
            btnViewTree.classList.remove('active');
            renderCurrentView();
        });

        // Mở rộng / Thu gọn tất cả
        btnExpandAll.addEventListener('click', function () {
            state.collapsedNodes = {};
            renderCurrentView();
        });

        btnCollapseAll.addEventListener('click', function () {
            state.units.forEach(function (u) {
                state.collapsedNodes[u.id] = true;
            });
            renderCurrentView();
        });

        // Tìm kiếm & Lọc
        orgSearchInput.addEventListener('input', function () {
            state.keyword = orgSearchInput.value.trim();
            renderCurrentView();
        });

        filterRegion.addEventListener('change', function () {
            state.filterRegion = filterRegion.value;
            renderCurrentView();
        });

        // Toggle thu gọn từng nhánh trên sơ đồ cây
        orgTreeRoot.addEventListener('click', function (e) {
            var btn = e.target.closest('[data-toggle-id]');
            if (!btn) return;
            var nodeId = Number(btn.getAttribute('data-toggle-id'));
            state.collapsedNodes[nodeId] = !state.collapsedNodes[nodeId];
            renderTreeView();
        });

        // Điền danh sách đơn vị cha vào Select (Chống chọn chính nó và con cháu của nó - AC 1)
        function populateParentSelect(currentUnitId) {
            formUnitParent.innerHTML = '<option value="">-- Là đơn vị gốc (Không có đơn vị cha) --</option>';

            // Tìm toàn bộ con cháu của currentUnitId để cấm chọn (Tránh vòng lặp vô tận)
            var disallowedIds = {};
            if (currentUnitId) {
                disallowedIds[currentUnitId] = true;
                function collectDescendants(parentId) {
                    state.units.forEach(function (u) {
                        if (u.parentId === parentId && !disallowedIds[u.id]) {
                            disallowedIds[u.id] = true;
                            collectDescendants(u.id);
                        }
                    });
                }
                collectDescendants(currentUnitId);
            }

            // Render select có thụt đầu dòng theo cấp bậc
            function appendOptions(parentId, prefix) {
                var children = state.units.filter(function (u) { return u.parentId === parentId; });
                children.forEach(function (c) {
                    if (!disallowedIds[c.id]) {
                        var opt = document.createElement('option');
                        opt.value = c.id;
                        opt.textContent = prefix + c.name;
                        formUnitParent.appendChild(opt);
                        appendOptions(c.id, prefix + '— ');
                    }
                });
            }

            var roots = state.units.filter(function (u) { return !u.parentId; });
            roots.forEach(function (r) {
                if (!disallowedIds[r.id]) {
                    var opt = document.createElement('option');
                    opt.value = r.id;
                    opt.textContent = '• ' + r.name;
                    formUnitParent.appendChild(opt);
                    appendOptions(r.id, '  — ');
                }
            });
        }

        // Mở Modal Thêm mới đơn vị
        btnOpenCreateUnitModal.addEventListener('click', function () {
            hideAlerts();
            unitForm.reset();
            formUnitId.value = '';
            unitModalHeading.textContent = 'Thêm đơn vị / Nhóm mới';
            populateParentSelect(null);
            clearFormErrors();
            unitModal.style.display = 'flex';
        });

        function closeUnitModal() {
            unitModal.style.display = 'none';
            clearFormErrors();
        }

        btnCloseUnitModal.addEventListener('click', closeUnitModal);
        btnCancelUnitModal.addEventListener('click', closeUnitModal);

        function clearFormErrors() {
            document.querySelectorAll('.form-feedback').forEach(function (el) { el.textContent = ''; });
            document.querySelectorAll('.form-control').forEach(function (el) { el.classList.remove('is-invalid'); });
        }

        // Mở Modal Sửa đơn vị
        function openEditUnitModal(id) {
            var u = state.units.find(function (item) { return item.id === id; });
            if (!u) return;

            hideAlerts();
            clearFormErrors();
            formUnitId.value = u.id;
            formUnitName.value = u.name;
            populateParentSelect(u.id);
            formUnitParent.value = u.parentId ? String(u.parentId) : '';
            formUnitManager.value = u.managerId ? String(u.managerId) : '';
            formUnitRegion.value = u.region || 'NATIONAL';

            unitModalHeading.textContent = 'Chỉnh sửa: ' + u.name;
            unitModal.style.display = 'flex';
        }

        // Mở Modal Thêm đơn vị con
        function openAddChildUnitModal(parentId) {
            hideAlerts();
            unitForm.reset();
            formUnitId.value = '';
            unitModalHeading.textContent = 'Thêm đơn vị con trực thuộc';
            populateParentSelect(null);
            formUnitParent.value = String(parentId);
            clearFormErrors();
            unitModal.style.display = 'flex';
        }

        // Mở Drawer Xem thành viên (AC 2)
        function openMembersDrawer(unitId) {
            var u = state.units.find(function (item) { return item.id === unitId; });
            if (!u) return;

            drawerTitle.textContent = u.name;
            var regInfo = REGION_LABELS[u.region] || { text: u.region };
            drawerSubtitle.textContent = 'Khu vực: ' + regInfo.text + ' | ID Nhóm: #' + u.id;

            drawerManagerName.textContent = u.managerName || 'Chưa gán';
            drawerManagerRole.textContent = u.managerRole || 'Trưởng nhóm phụ trách';
            drawerManagerAvatar.textContent = (u.managerName || 'U').charAt(0).toUpperCase();

            drawerMembersList.innerHTML = '';
            var members = u.members || [];
            drawerMemberCount.textContent = members.length;

            if (members.length === 0) {
                drawerMembersList.innerHTML = '<li style="padding: 24px; text-align: center; color: #64748b; font-size: 0.875rem;">Chưa có nhân sự nào được phân bổ vào nhóm này.</li>';
            } else {
                members.forEach(function (m) {
                    var li = document.createElement('li');
                    li.className = 'org-member-item';
                    li.innerHTML =
                        '<div class="org-member-profile">' +
                            '<div style="width:34px;height:34px;border-radius:50%;background:#eff6ff;color:#2563eb;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:0.8125rem;border:1px solid #bfdbfe;">' +
                                escapeHtml((m.name || 'U').charAt(0).toUpperCase()) +
                            '</div>' +
                            '<div>' +
                                '<div class="org-member-name">' + escapeHtml(m.name) + '</div>' +
                                '<div class="org-member-email">' + escapeHtml(m.email) + '</div>' +
                            '</div>' +
                        '</div>' +
                        '<div style="text-align: right;">' +
                            '<span style="font-size:0.75rem;font-weight:600;color:#2563eb;display:block;">' + escapeHtml(m.role) + '</span>' +
                            '<span style="font-size:0.6875rem;color:#94a3b8;">Từ: ' + escapeHtml(m.joinedDate || '-') + '</span>' +
                        '</div>';
                    drawerMembersList.appendChild(li);
                });
            }

            unitDrawer.style.display = 'flex';
        }

        function closeMembersDrawer() {
            unitDrawer.style.display = 'none';
        }
        btnCloseDrawer.addEventListener('click', closeMembersDrawer);
        unitDrawer.addEventListener('click', function (e) {
            if (e.target === unitDrawer) closeMembersDrawer();
        });

        // Xử lý click các nút trong Tree và Table
        document.addEventListener('click', function (e) {
            var btn = e.target.closest('[data-action]');
            if (!btn) return;
            var action = btn.getAttribute('data-action');
            var id = Number(btn.getAttribute('data-id'));

            if (action === 'edit') {
                openEditUnitModal(id);
            } else if (action === 'add-child') {
                openAddChildUnitModal(id);
            } else if (action === 'view-members') {
                openMembersDrawer(id);
            }
        });

        // Validate Form Đơn vị (AC 1, AC 4)
        function validateUnitForm() {
            clearFormErrors();
            var valid = true;

            var name = formUnitName.value.trim();
            if (!name) {
                document.getElementById('feedbackUnitName').textContent = 'Vui lòng nhập tên đơn vị / nhóm kinh doanh.';
                formUnitName.classList.add('is-invalid');
                valid = false;
            }

            var managerId = formUnitManager.value;
            if (!managerId) {
                document.getElementById('feedbackUnitManager').textContent = 'Vui lòng chọn duy nhất một trưởng nhóm (AC 1).';
                formUnitManager.classList.add('is-invalid');
                valid = false;
            }

            var region = formUnitRegion.value;
            if (!region) {
                document.getElementById('feedbackUnitRegion').textContent = 'Vui lòng chọn khu vực địa lý (AC 4).';
                formUnitRegion.classList.add('is-invalid');
                valid = false;
            }

            return valid;
        }

        // Submit Form Đơn vị (POST / PUT /api/organization/units)
        unitForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            if (!validateUnitForm()) return;

            var id = formUnitId.value;
            var isEdit = Boolean(id);

            var parentVal = formUnitParent.value;
            var parentId = parentVal ? Number(parentVal) : null;
            var managerId = Number(formUnitManager.value);
            var mgrInfo = MANAGERS_MAP[managerId] || { name: 'Trưởng nhóm', role: 'Team Lead' };

            var payload = {
                name: formUnitName.value.trim(),
                parentId: parentId,
                managerId: managerId,
                managerName: mgrInfo.name,
                managerRole: mgrInfo.role,
                region: formUnitRegion.value,
                active: true
            };

            var endpoint = isEdit
                ? (contextPath + '/api/organization/units/' + id)
                : (contextPath + '/api/organization/units');
            var method = isEdit ? 'PUT' : 'POST';

            try {
                var res = await fetch(endpoint, {
                    method: method,
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify(payload)
                });
                if (res.ok) {
                    showSuccessAlert(isEdit ? 'Cập nhật đơn vị thành công!' : 'Tạo mới đơn vị thành công!');
                }
            } catch (err) {
                console.info('Backend chưa sẵn sàng - Cập nhật dữ liệu tại local state:', err);
            }

            // Cập nhật local state
            if (isEdit) {
                var idx = state.units.findIndex(function (item) { return item.id === Number(id); });
                if (idx !== -1) {
                    payload.id = Number(id);
                    payload.memberCount = state.units[idx].memberCount;
                    payload.members = state.units[idx].members;
                    state.units[idx] = Object.assign({}, state.units[idx], payload);
                }
                showSuccessAlert('Đã cập nhật thông tin đơn vị [' + payload.name + ']!');
            } else {
                var newId = state.units.length > 0 ? Math.max.apply(null, state.units.map(function(u){ return u.id; })) + 1 : 1;
                payload.id = newId;
                payload.memberCount = 1;
                payload.members = [];
                state.units.push(payload);
                showSuccessAlert('Đã thêm mới đơn vị [' + payload.name + '] vào cây tổ chức!');
            }

            closeUnitModal();
            updateStats();
            renderCurrentView();
        });

        // Khởi động
        fetchUnits();

    })();
    </script>
</body>
</html>