<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phân quyền & Phạm vi dữ liệu sở hữu - CRM</title>

    <!-- CSS dùng chung của hệ thống -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">

    <!-- CSS riêng của module Phân quyền & Data Scope (CRM-25 & CRM-29) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/permissions/permissions.css">
</head>
<body class="crm-body">

    <!-- Sử dụng layout Header dùng chung theo quy chuẩn kiến trúc bắt buộc -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Include Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Phân quyền & Nhóm kinh doanh -->
        <main class="permission-page" id="permissionApp">
            <div class="permission-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="permission-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="#">Hệ thống</a>
                    <span class="separator">/</span>
                    <span class="active">Phân quyền & Nhóm kinh doanh</span>
                </nav>

                <!-- Header màn hình -->
                <header class="permission-header">
                    <div class="permission-header-info">
                        <h1>Phân quyền & Nhóm kinh doanh</h1>
                        <p>Cấu hình nhân sự, vai trò hệ thống, gán nhóm kinh doanh (CRM-29) và phạm vi dữ liệu sở hữu Data Scope (CRM-25).</p>
                    </div>
                    <div class="permission-header-badges">
                        <span class="permission-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            </svg>
                            S1-05 / CRM-25
                        </span>
                        <span class="permission-badge permission-badge-secondary">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                            </svg>
                            CRM-29 Sales Team
                        </span>
                    </div>
                </header>

                <!-- Khu vực thông báo động (Alerts) -->
                <div class="permission-alerts" id="permissionAlertsArea">
                    <div class="permission-alert permission-alert-danger" id="clientErrorAlert" style="display: none;">
                        <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="permission-alert-content">
                            <div class="permission-alert-title">Thông báo lỗi</div>
                            <div id="clientErrorText"></div>
                        </div>
                        <button type="button" class="permission-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>

                    <div class="permission-alert permission-alert-success" id="clientSuccessAlert" style="display: none;">
                        <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="permission-alert-content">
                            <div class="permission-alert-title">Thành công</div>
                            <div id="clientSuccessText"></div>
                        </div>
                        <button type="button" class="permission-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>
                </div>

                <!-- ======================================================= -->
                <!-- BỐ CỤC 2 CỘT (RESPONSIVE GRID)                           -->
                <!-- ======================================================= -->
                <div class="permission-layout-grid">

                    <!-- =================================================== -->
                    <!-- CỘT TRÁI: FORM CẤU HÌNH PHÂN QUYỀN                   -->
                    <!-- =================================================== -->
                    <div class="permission-left-col">
                        <!-- Endpoint chuẩn REST POST /api/permissions/assign theo đúng quy chuẩn kỹ thuật -->
                        <form id="permissionForm" method="POST" action="${pageContext.request.contextPath}/api/permissions/assign">

                            <!-- BƯỚC 1: Chọn nhân viên -->
                            <section class="permission-card" id="userCardSection">
                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">1</span>
                                        <div>
                                            <h2>Chọn nhân viên</h2>
                                            <div class="permission-card-subtitle">Lựa chọn nhân sự cần thiết lập phân quyền và nhóm phụ trách</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <div class="permission-user-selector">
                                        <div class="permission-form-group">
                                            <label for="userSelect" class="permission-label">Tài khoản nhân viên <span style="color: var(--perm-danger);">*</span></label>
                                            <div class="permission-select-wrapper">
                                                <select id="userSelect" name="userId" class="permission-select" required>
                                                    <option value="">-- Đang nạp danh sách nhân viên từ hệ thống... --</option>
                                                </select>
                                            </div>
                                        </div>

                                        <!-- Thẻ hiển thị tóm tắt thông tin nhân viên được chọn -->
                                        <div class="permission-user-summary" id="userSummaryCard" style="display: none;">
                                            <div class="permission-user-avatar" id="userAvatarText">U</div>
                                            <div class="permission-user-meta">
                                                <div class="permission-user-name" id="userNameDisplay">Họ và tên nhân viên</div>
                                                <div class="permission-user-subdetails">
                                                    <span id="userEmailDisplay">email@crm.vn</span>
                                                    <span class="permission-user-tag">
                                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                            <circle cx="9" cy="7" r="4"></circle>
                                                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                        </svg>
                                                        Phòng / Nhóm: <strong id="userTeamDisplay" style="margin-left: 4px;">Chưa phân nhóm</strong>
                                                    </span>
                                                    <span class="permission-user-tag">
                                                        Đang chọn: <strong id="activeRoleCountDisplay" style="margin-left: 4px;">0 vai trò</strong>
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!-- BƯỚC 2: Gán nhóm kinh doanh (Sales Team) - CRM-29 -->
                            <section class="permission-card" id="teamCardSection">
                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">2</span>
                                        <div>
                                            <h2>Gán nhóm kinh doanh (Sales Team)</h2>
                                            <div class="permission-card-subtitle">Phân bổ nhân sự vào đội ngũ kinh doanh phụ trách (CRM-29)</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <div class="permission-team-layout">
                                        <!-- Cảnh báo ràng buộc dành cho vai trò Trưởng nhóm (Team Lead) khi chưa có nhóm -->
                                        <div class="permission-lead-warning" id="teamLeadWarning" style="display: none;">
                                            <svg class="permission-lead-warning-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                                <line x1="12" y1="9" x2="12" y2="13"></line>
                                                <line x1="12" y1="17" x2="12.01" y2="17"></line>
                                            </svg>
                                            <div class="permission-lead-warning-content">
                                                <div class="permission-lead-warning-title">Yêu cầu ràng buộc: Vai trò Trưởng nhóm (Team Lead)</div>
                                                <div class="permission-lead-warning-desc">
                                                    Tài khoản này đang giữ vai trò Quản lý / Trưởng nhóm nhưng hiện tại chưa được gán vào nhóm kinh doanh nào.
                                                    Vui lòng chọn và gán nhóm quản lý phụ trách cho nhân sự này.
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Thẻ hiển thị nhóm hiện tại -->
                                        <div class="permission-current-team-card">
                                            <div class="permission-current-team-left">
                                                <div class="permission-current-team-icon">
                                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                        <circle cx="9" cy="7" r="4"></circle>
                                                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                    </svg>
                                                </div>
                                                <div class="permission-current-team-info">
                                                    <span class="permission-current-team-label">Nhóm kinh doanh hiện tại</span>
                                                    <span class="permission-current-team-name" id="currentTeamDisplay">Chưa phân nhóm</span>
                                                </div>
                                            </div>
                                            <div class="permission-status-hint">
                                                <span class="permission-status-dot" id="teamStatusDot"></span>
                                                <span id="teamStatusText">Chưa chọn nhân viên</span>
                                            </div>
                                        </div>

                                        <!-- Form chọn nhóm và nút gán nhóm -->
                                        <div class="permission-team-assign-form">
                                            <div class="permission-team-select-group">
                                                <label for="teamSelect" class="permission-label">Chọn nhóm kinh doanh mới <span style="color: var(--perm-danger);">*</span></label>
                                                <div class="permission-select-wrapper" style="max-width: 100%;">
                                                    <select id="teamSelect" name="teamId" class="permission-select">
                                                        <option value="">-- Chọn nhóm kinh doanh --</option>
                                                    </select>
                                                </div>
                                            </div>
                                            <button type="button" class="permission-btn permission-btn-primary" id="btnAssignTeam">
                                                <span class="permission-spinner" id="btnAssignTeamSpinner" style="display: none;"></span>
                                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" id="btnAssignTeamIcon">
                                                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="9" cy="7" r="4"></circle>
                                                    <polyline points="16 11 18 13 22 9"></polyline>
                                                </svg>
                                                <span id="btnAssignTeamText">Gán nhóm</span>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!-- BƯỚC 3: Chọn vai trò hệ thống với TOGGLE SWITCH (Không hard-code ID) -->
                            <section class="permission-card" id="rolesCardSection">
                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">3</span>
                                        <div>
                                            <h2>Vai trò hệ thống (Roles)</h2>
                                            <div class="permission-card-subtitle">Bật / tắt các vai trò chức năng phù hợp cho tài khoản</div>
                                        </div>
                                    </div>
                                    <div class="permission-quick-actions">
                                        <button type="button" class="permission-btn-subtle" id="btnSelectAllRoles">Bật tất cả</button>
                                        <button type="button" class="permission-btn-subtle" id="btnDeselectAllRoles">Tắt tất cả</button>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <!-- Overlay hiển thị khi đang tải dữ liệu phân quyền -->
                                    <div class="permission-loading-overlay" id="rolesLoadingOverlay">
                                        <div class="permission-loading-box">
                                            <span class="permission-spinner permission-spinner-dark"></span>
                                            <span>Đang đồng bộ dữ liệu phân quyền...</span>
                                        </div>
                                    </div>

                                    <!-- Danh sách vai trò render hoàn toàn động qua JS từ GET /api/roles (Không hard-code) -->
                                    <div class="permission-role-grid" id="rolesGridContainer">
                                        <!-- Khối Empty State ban đầu khi đang chờ tải dữ liệu -->
                                        <div class="permission-empty-state" id="rolesEmptyState">
                                            <div class="permission-empty-icon">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                                                </svg>
                                            </div>
                                            <div class="permission-empty-title" id="rolesEmptyTitle">Đang nạp danh mục vai trò...</div>
                                            <div class="permission-empty-desc" id="rolesEmptyDesc">Hệ thống đang kết nối máy chủ để nạp danh sách vai trò chức năng.</div>
                                            <button type="button" class="permission-empty-btn" id="btnRetryRoles" style="display: none;">
                                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <polyline points="23 4 23 10 17 10"></polyline>
                                                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                                                </svg>
                                                Tải lại danh sách vai trò
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!-- BƯỚC 4: Chọn phạm vi dữ liệu sở hữu (Data Scope) -->
                            <section class="permission-card" id="dataScopeCardSection">
                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">4</span>
                                        <div>
                                            <h2>Phạm vi dữ liệu sở hữu (Data Scope)</h2>
                                            <div class="permission-card-subtitle">Quy định giới hạn bản ghi dữ liệu người dùng được phép xem, sửa hoặc thao tác (CRM-25)</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <div class="permission-scope-grid" id="dataScopeGrid">

                                        <!-- Cấp độ 1: SELF - Viền xanh lam nhạt, icon người dùng cá nhân -->
                                        <label class="permission-scope-card scope-self selected" for="scope_self">
                                            <div class="permission-scope-icon-wrap">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="12" cy="7" r="4"></circle>
                                                </svg>
                                            </div>
                                            <div class="permission-scope-content">
                                                <div class="permission-scope-top-row">
                                                    <h3 class="permission-scope-name">Cá nhân (SELF)</h3>
                                                    <span class="permission-scope-badge">Mức bảo mật hẹp</span>
                                                </div>
                                                <p class="permission-scope-desc">Nhân viên kinh doanh chỉ thấy dữ liệu cá nhân</p>
                                            </div>
                                            <div class="permission-scope-radio-wrap">
                                                <input type="radio"
                                                       class="permission-scope-radio"
                                                       name="dataScope"
                                                       id="scope_self"
                                                       value="SELF"
                                                       checked>
                                            </div>
                                        </label>

                                        <!-- Cấp độ 2: TEAM - Viền vàng cam nhạt, icon nhóm -->
                                        <label class="permission-scope-card scope-team" for="scope_team">
                                            <div class="permission-scope-icon-wrap">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="9" cy="7" r="4"></circle>
                                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                </svg>
                                            </div>
                                            <div class="permission-scope-content">
                                                <div class="permission-scope-top-row">
                                                    <h3 class="permission-scope-name">Nhóm / Đội ngũ (TEAM)</h3>
                                                    <span class="permission-scope-badge">Cộng tác nhóm</span>
                                                </div>
                                                <p class="permission-scope-desc">Trưởng nhóm thấy toàn bộ dữ liệu của nhóm phụ trách</p>
                                            </div>
                                            <div class="permission-scope-radio-wrap">
                                                <input type="radio"
                                                       class="permission-scope-radio"
                                                       name="dataScope"
                                                       id="scope_team"
                                                       value="TEAM">
                                            </div>
                                        </label>

                                        <!-- Cấp độ 3: ALL - Viền xanh lá, icon toàn quyền/công ty -->
                                        <label class="permission-scope-card scope-all" for="scope_all">
                                            <div class="permission-scope-icon-wrap">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                    <rect x="4" y="2" width="16" height="20" rx="2" ry="2"></rect>
                                                    <line x1="9" y1="22" x2="9" y2="22.01"></line>
                                                    <line x1="15" y1="22" x2="15" y2="22.01"></line>
                                                    <line x1="9" y1="6" x2="9" y2="6.01"></line>
                                                    <line x1="15" y1="6" x2="15" y2="6.01"></line>
                                                    <line x1="9" y1="10" x2="9" y2="10.01"></line>
                                                    <line x1="15" y1="10" x2="15" y2="10.01"></line>
                                                    <line x1="9" y1="14" x2="9" y2="14.01"></line>
                                                    <line x1="15" y1="14" x2="15" y2="14.01"></line>
                                                </svg>
                                            </div>
                                            <div class="permission-scope-content">
                                                <div class="permission-scope-top-row">
                                                    <h3 class="permission-scope-name">Toàn công ty (ALL)</h3>
                                                    <span class="permission-scope-badge">Toàn quyền dữ liệu</span>
                                                </div>
                                                <p class="permission-scope-desc">Giám đốc / Quản trị viên thấy toàn bộ dữ liệu công ty</p>
                                            </div>
                                            <div class="permission-scope-radio-wrap">
                                                <input type="radio"
                                                       class="permission-scope-radio"
                                                       name="dataScope"
                                                       id="scope_all"
                                                       value="ALL">
                                            </div>
                                        </label>

                                    </div>
                                </div>
                            </section>

                            <!-- THANH HÀNH ĐỘNG (ACTION BAR) -->
                            <footer class="permission-actions-bar">
                                <div class="permission-status-hint">
                                    <span class="permission-status-dot" id="permissionStatusDot"></span>
                                    <span id="permissionStatusText">Sẵn sàng thiết lập phân quyền</span>
                                </div>
                                <div class="permission-buttons">
                                    <button type="button" class="permission-btn permission-btn-secondary" id="btnResetPermissions">
                                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <polyline points="1 4 1 10 7 10"></polyline>
                                            <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                        </svg>
                                        Đặt lại
                                    </button>
                                    <button type="submit" class="permission-btn permission-btn-primary" id="btnSavePermissions">
                                        <span class="permission-spinner" id="btnSaveSpinner" style="display: none;"></span>
                                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" id="btnSaveIcon">
                                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                                            <polyline points="7 3 7 8 15 8"></polyline>
                                        </svg>
                                        <span id="btnSaveText">Lưu phân quyền</span>
                                    </button>
                                </div>
                            </footer>
                        </form>
                    </div>

                    <!-- =================================================== -->
                    <!-- CỘT PHẢI: KHỐI HIỂN THỊ THÔNG TIN TRỰC QUAN 3 CẤP ĐỘ -->
                    <!-- DATA SCOPE (SELF, TEAM, ALL) & WARNING PREVIEW BANNER-->
                    <!-- =================================================== -->
                    <div class="permission-right-col">

                        <!-- KHỐI 1: Hướng dẫn trực quan 3 cấp độ Data Scope -->
                        <section class="permission-guide-card" id="scopeGuideCard">
                            <div class="permission-guide-header">
                                <div class="permission-guide-title">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color: var(--perm-primary);">
                                        <circle cx="12" cy="12" r="10"></circle>
                                        <line x1="12" y1="16" x2="12" y2="12"></line>
                                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                                    </svg>
                                    <span>3 Cấp độ Data Scope</span>
                                </div>
                                <span class="permission-active-scope-indicator" id="activeScopeBadge">
                                    Đang chọn: Cá nhân (SELF)
                                </span>
                            </div>

                            <div class="permission-guide-body">

                                <!-- Thẻ cấp độ SELF: Viền xanh lam nhạt -->
                                <div class="scope-info-card info-self active-scope" id="infoCardSelf">
                                    <div class="scope-info-header">
                                        <div class="scope-info-title-group">
                                            <div class="scope-info-icon-badge">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="12" cy="7" r="4"></circle>
                                                </svg>
                                            </div>
                                            <span class="scope-info-title">1. Cấp độ CÁ NHÂN (SELF)</span>
                                        </div>
                                        <span class="scope-info-tag">Sales Rep</span>
                                    </div>
                                    <div class="scope-info-desc">
                                        <strong>Mô tả:</strong> Nhân viên kinh doanh chỉ thấy dữ liệu cá nhân do chính mình tạo hoặc được phân công trực tiếp làm người phụ trách.
                                    </div>
                                    <ul class="scope-info-list">
                                        <li><strong>Khách hàng & Leads:</strong> Chỉ truy cập các bản ghi có <code>owner_id = user.id</code>.</li>
                                        <li><strong>Cơ hội (Deals):</strong> Theo dõi pipeline và chốt giao dịch cá nhân.</li>
                                        <li><strong>Báo cáo:</strong> Xem thống kê kết quả và chỉ tiêu KPI của bản thân.</li>
                                    </ul>
                                </div>

                                <!-- Thẻ cấp độ TEAM: Viền vàng cam nhạt -->
                                <div class="scope-info-card info-team" id="infoCardTeam">
                                    <div class="scope-info-header">
                                        <div class="scope-info-title-group">
                                            <div class="scope-info-icon-badge">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="9" cy="7" r="4"></circle>
                                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                </svg>
                                            </div>
                                            <span class="scope-info-title">2. Cấp độ NHÓM KINH DOANH (TEAM)</span>
                                        </div>
                                        <span class="scope-info-tag">Team Lead</span>
                                    </div>
                                    <div class="scope-info-desc">
                                        <strong>Mô tả:</strong> Trưởng nhóm thấy toàn bộ dữ liệu của nhóm phụ trách. Tự động kiểm soát dựa trên liên kết nhóm kinh doanh (Sales Team CRM-29).
                                    </div>
                                    <ul class="scope-info-list">
                                        <li><strong>Khách hàng & Leads:</strong> Thấy mọi khách hàng của các thành viên trực thuộc cùng đội nhóm.</li>
                                        <li><strong>Cơ hội (Deals):</strong> Theo dõi toàn bộ pipeline nhóm, hỗ trợ chốt đơn và duyệt báo giá.</li>
                                        <li><strong>Điều phối:</strong> Được quyền phân bổ và chuyển giao khách hàng trong nội bộ team.</li>
                                    </ul>
                                </div>

                                <!-- Thẻ cấp độ ALL: Viền xanh lá -->
                                <div class="scope-info-card info-all" id="infoCardAll">
                                    <div class="scope-info-header">
                                        <div class="scope-info-title-group">
                                            <div class="scope-info-icon-badge">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <rect x="4" y="2" width="16" height="20" rx="2" ry="2"></rect>
                                                    <line x1="9" y1="22" x2="9" y2="22.01"></line>
                                                    <line x1="15" y1="22" x2="15" y2="22.01"></line>
                                                    <line x1="9" y1="6" x2="9" y2="6.01"></line>
                                                    <line x1="15" y1="6" x2="15" y2="6.01"></line>
                                                </svg>
                                            </div>
                                            <span class="scope-info-title">3. Cấp độ TOÀN HỆ THỐNG (ALL)</span>
                                        </div>
                                        <span class="scope-info-tag">Giám đốc / Admin</span>
                                    </div>
                                    <div class="scope-info-desc">
                                        <strong>Mô tả:</strong> Giám đốc / Quản trị viên thấy toàn bộ dữ liệu công ty. Không bị giới hạn bởi phòng ban hay nhóm bán hàng.
                                    </div>
                                    <ul class="scope-info-list">
                                        <li><strong>Khách hàng & Leads:</strong> Toàn quyền tìm kiếm, xem và xuất toàn bộ cơ sở khách hàng.</li>
                                        <li><strong>Cơ hội (Deals):</strong> Giám sát tiến độ doanh số và hoạt động bán hàng toàn diện.</li>
                                        <li><strong>Báo cáo chiến lược:</strong> Đầy đủ số liệu doanh thu, tỷ lệ chốt đơn và KPI toàn công ty.</li>
                                    </ul>
                                </div>

                            </div>
                        </section>

                        <!-- KHỐI 2: Component thông báo trực quan (Warning Banner preview) theo AC -->
                        <section class="permission-warning-preview-card" id="warningPreviewSection">
                            <div class="permission-warning-preview-header">
                                <div class="permission-warning-preview-title">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color: #d97706;">
                                        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                        <line x1="12" y1="9" x2="12" y2="13"></line>
                                        <line x1="12" y1="17" x2="12.01" y2="17"></line>
                                    </svg>
                                    <span>Mô phỏng cảnh báo truy cập ngoài phạm vi (AC Preview)</span>
                                </div>
                                <span class="permission-warning-ac-badge">Security Policy</span>
                            </div>
                            <div class="permission-warning-preview-body">

                                <!-- Component thông báo trực quan mô phỏng banner AC -->
                                <div class="permission-mock-banner">
                                    <svg class="permission-mock-banner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <circle cx="12" cy="12" r="10"></circle>
                                        <line x1="12" y1="8" x2="12" y2="12"></line>
                                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                                    </svg>
                                    <div class="permission-mock-banner-content">
                                        <div class="permission-mock-banner-status">
                                            <span class="permission-mock-code-badge">HTTP 403 FORBIDDEN</span>
                                        </div>
                                        <div class="permission-mock-banner-title">
                                            "Bạn không có quyền truy cập dữ liệu ngoài phạm vi được phân công"
                                        </div>
                                        <p class="permission-mock-banner-desc">
                                            Thông báo chuẩn mực kích hoạt bởi bộ lọc bảo mật Data Filter khi người dùng cố gắng mở xem, sửa hoặc xuất bản ghi ngoài phạm vi Data Scope.
                                        </p>
                                    </div>
                                </div>

                                <!-- Khối giả lập tình huống theo Scope đang chọn -->
                                <div class="permission-mock-scenario" id="warningScenarioText">
                                    <strong>Ngữ cảnh kích hoạt:</strong> Nhân viên có phạm vi <strong>SELF</strong>. Khi truy cập trực tiếp đường link khách hàng hoặc cơ hội <code>/pipeline/deals/1092</code> do đồng nghiệp khác phụ trách, hệ thống sẽ chặn và hiển thị cảnh báo trên.
                                </div>

                                <!-- Bảng tóm tắt so sánh nhanh phạm vi -->
                                <div class="permission-matrix-wrap">
                                    <table class="permission-matrix-table">
                                        <thead>
                                            <tr>
                                                <th>Cấp độ</th>
                                                <th>Khách hàng cá nhân</th>
                                                <th>Khách hàng nhóm</th>
                                                <th>Khách hàng công ty</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td><strong>SELF</strong></td>
                                                <td><span class="permission-matrix-check">✓ Toàn quyền</span></td>
                                                <td><span class="permission-matrix-limit">✕ Bị chặn</span></td>
                                                <td><span class="permission-matrix-limit">✕ Bị chặn</span></td>
                                            </tr>
                                            <tr>
                                                <td><strong>TEAM</strong></td>
                                                <td><span class="permission-matrix-check">✓ Toàn quyền</span></td>
                                                <td><span class="permission-matrix-check">✓ Toàn quyền</span></td>
                                                <td><span class="permission-matrix-limit">✕ Bị chặn</span></td>
                                            </tr>
                                            <tr>
                                                <td><strong>ALL</strong></td>
                                                <td><span class="permission-matrix-check">✓ Toàn quyền</span></td>
                                                <td><span class="permission-matrix-check">✓ Toàn quyền</span></td>
                                                <td><span class="permission-matrix-check">✓ Toàn quyền</span></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>

                            </div>
                        </section>

                    </div>

                </div>

            </div>
        </main>
    </div>

    <!-- Include Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- =================================================================== -->
    <!-- JAVASCRIPT DOM THUẦN TÚY (REST API & UI INTERACTION)               -->
    <!-- Hoàn toàn không phụ thuộc thẻ scriptlet JSP hay JSTL taglib          -->
    <!-- =================================================================== -->
    <script>
        (function() {
            'use strict';

            // Đường dẫn gốc ứng dụng (Context Path)
            const contextPath = '${pageContext.request.contextPath}';

            // DOM elements - Chọn người dùng & phân quyền (CRM-25)
            const userSelect = document.getElementById('userSelect');
            const userSummaryCard = document.getElementById('userSummaryCard');
            const userAvatarText = document.getElementById('userAvatarText');
            const userNameDisplay = document.getElementById('userNameDisplay');
            const userEmailDisplay = document.getElementById('userEmailDisplay');
            const userTeamDisplay = document.getElementById('userTeamDisplay');
            const activeRoleCountDisplay = document.getElementById('activeRoleCountDisplay');
            const rolesGridContainer = document.getElementById('rolesGridContainer');
            const rolesEmptyState = document.getElementById('rolesEmptyState');
            const rolesEmptyTitle = document.getElementById('rolesEmptyTitle');
            const rolesEmptyDesc = document.getElementById('rolesEmptyDesc');
            const btnRetryRoles = document.getElementById('btnRetryRoles');
            const rolesLoadingOverlay = document.getElementById('rolesLoadingOverlay');
            const btnSelectAllRoles = document.getElementById('btnSelectAllRoles');
            const btnDeselectAllRoles = document.getElementById('btnDeselectAllRoles');
            const btnSavePermissions = document.getElementById('btnSavePermissions');
            const btnSaveSpinner = document.getElementById('btnSaveSpinner');
            const btnSaveIcon = document.getElementById('btnSaveIcon');
            const btnSaveText = document.getElementById('btnSaveText');
            const btnResetPermissions = document.getElementById('btnResetPermissions');
            const clientErrorAlert = document.getElementById('clientErrorAlert');
            const clientErrorText = document.getElementById('clientErrorText');
            const clientSuccessAlert = document.getElementById('clientSuccessAlert');
            const clientSuccessText = document.getElementById('clientSuccessText');
            const permissionStatusDot = document.getElementById('permissionStatusDot');
            const permissionStatusText = document.getElementById('permissionStatusText');

            // DOM elements - Nhóm kinh doanh (CRM-29)
            const teamSelect = document.getElementById('teamSelect');
            const currentTeamDisplay = document.getElementById('currentTeamDisplay');
            const teamStatusDot = document.getElementById('teamStatusDot');
            const teamStatusText = document.getElementById('teamStatusText');
            const btnAssignTeam = document.getElementById('btnAssignTeam');
            const btnAssignTeamSpinner = document.getElementById('btnAssignTeamSpinner');
            const btnAssignTeamIcon = document.getElementById('btnAssignTeamIcon');
            const btnAssignTeamText = document.getElementById('btnAssignTeamText');
            const teamLeadWarning = document.getElementById('teamLeadWarning');

            // DOM elements - Cột phải: Thông tin trực quan 3 cấp độ & Warning Preview
            const activeScopeBadge = document.getElementById('activeScopeBadge');
            const infoCardSelf = document.getElementById('infoCardSelf');
            const infoCardTeam = document.getElementById('infoCardTeam');
            const infoCardAll = document.getElementById('infoCardAll');
            const warningScenarioText = document.getElementById('warningScenarioText');

            // Bộ nhớ đệm lưu trạng thái ban đầu để khôi phục khi nhấn "Đặt lại"
            let initialUserState = null;
            let currentUserTeamName = '';
            let currentRolesList = [];

            // Tiện ích escape HTML chống XSS trên giao diện
            function escapeHtml(str) {
                if (str === null || str === undefined) return '';
                return String(str)
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;')
                    .replace(/'/g, '&#39;');
            }

            // Xóa thông báo lỗi / thành công trên UI
            function clearAlerts() {
                if (clientErrorAlert) {
                    clientErrorAlert.style.display = 'none';
                    clientErrorText.textContent = '';
                }
                if (clientSuccessAlert) {
                    clientSuccessAlert.style.display = 'none';
                    clientSuccessText.textContent = '';
                }
            }

            // Hiển thị thông báo lỗi
            function showError(message) {
                clearAlerts();
                if (clientErrorAlert) {
                    clientErrorText.textContent = message || 'Đã xảy ra lỗi trong quá trình thực hiện.';
                    clientErrorAlert.style.display = 'flex';
                    clientErrorAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                }
                setStatus('Lỗi thao tác', false);
            }

            // Hiển thị thông báo thành công
            function showSuccess(message) {
                clearAlerts();
                if (clientSuccessAlert) {
                    clientSuccessText.textContent = message || 'Cập nhật thành công!';
                    clientSuccessAlert.style.display = 'flex';
                    clientSuccessAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                }
                setStatus('Đã đồng bộ thành công', true);
            }

            // Cập nhật trạng thái thanh status bar dưới chân trang
            function setStatus(text, isActive) {
                if (permissionStatusText) permissionStatusText.textContent = text;
                if (permissionStatusDot) {
                    if (isActive) {
                        permissionStatusDot.classList.add('active');
                    } else {
                        permissionStatusDot.classList.remove('active');
                    }
                }
            }

            // Cập nhật số lượng vai trò được chọn
            function updateRoleCountBadge() {
                const checkedBoxes = document.querySelectorAll('input[name="roleIds"]:checked');
                const count = checkedBoxes.length;
                if (activeRoleCountDisplay) {
                    activeRoleCountDisplay.textContent = count + ' vai trò';
                }
            }

            // Cập nhật hiển thị thẻ người dùng được chọn
            function updateUserSummary(name, email, team) {
                if (!userSummaryCard) return;
                if (!name) {
                    userSummaryCard.style.display = 'none';
                    return;
                }
                userSummaryCard.style.display = 'flex';
                if (userNameDisplay) userNameDisplay.textContent = name;
                if (userEmailDisplay) userEmailDisplay.textContent = email || 'Chưa có email';
                if (userTeamDisplay) userTeamDisplay.textContent = team || 'Chưa phân nhóm';

                // Ký tự đại diện Avatar
                if (userAvatarText) {
                    const initials = name.trim().split(' ').map(function(w) { return w.charAt(0); }).join('').toUpperCase();
                    userAvatarText.textContent = initials.substring(0, 2) || 'U';
                }

                updateRoleCountBadge();
            }

            // =================================================================
            // 1. TẢI DANH MỤC VAI TRÒ QUA API GET /api/roles (KHÔNG HARD-CODE)
            // =================================================================
            function renderRolesList(roles) {
                if (!rolesGridContainer) return;

                // Nếu không có dữ liệu trả về từ backend, hiển thị Empty State thân thiện
                if (!Array.isArray(roles) || roles.length === 0) {
                    showRolesEmptyState(
                        'Chưa có dữ liệu vai trò từ hệ thống',
                        'Máy chủ hiện chưa cung cấp danh mục vai trò nào. Vui lòng kiểm tra lại cấu hình Backend hoặc thử lại sau.'
                    );
                    return;
                }

                currentRolesList = roles;
                rolesGridContainer.innerHTML = '';

                roles.forEach(function(role) {
                    const roleId = role.id !== undefined ? role.id : role.roleId;
                    const roleName = role.name || role.roleName || ('Vai trò #' + roleId);
                    const roleCode = role.code || role.roleCode || '';
                    const roleDesc = role.description || role.desc || 'Vai trò chức năng phân quyền trong hệ thống CRM';

                    const item = document.createElement('div');
                    item.className = 'permission-role-item';
                    item.setAttribute('data-role-id', String(roleId));

                    let codeBadgeHtml = '';
                    if (roleCode) {
                        codeBadgeHtml = '<span class="permission-role-code">' + escapeHtml(roleCode) + '</span>';
                    }

                    item.innerHTML =
                        '<div class="permission-role-details">' +
                            '<div class="permission-role-top">' +
                                '<span class="permission-role-title">' + escapeHtml(roleName) + '</span>' +
                                codeBadgeHtml +
                            '</div>' +
                            '<p class="permission-role-desc">' + escapeHtml(roleDesc) + '</p>' +
                        '</div>' +
                        '<div class="permission-toggle-wrap">' +
                            '<input type="checkbox"' +
                                   ' class="permission-role-checkbox"' +
                                   ' name="roleIds"' +
                                   ' id="role_' + escapeHtml(String(roleId)) + '"' +
                                   ' value="' + escapeHtml(String(roleId)) + '">' +
                            '<label class="permission-switch" for="role_' + escapeHtml(String(roleId)) + '" aria-hidden="true">' +
                                '<span class="permission-switch-slider"></span>' +
                            '</label>' +
                        '</div>';

                    // Gán sự kiện click chuyển đổi Toggle Switch
                    item.addEventListener('click', function(e) {
                        if (e.target && e.target.tagName.toLowerCase() === 'input') {
                            syncRoleItemState(item, e.target.checked);
                            return;
                        }
                        const cb = item.querySelector('input[type="checkbox"]');
                        if (cb) {
                            cb.checked = !cb.checked;
                            syncRoleItemState(item, cb.checked);
                        }
                    });

                    rolesGridContainer.appendChild(item);
                });

                updateRoleCountBadge();
            }

            function showRolesEmptyState(title, desc) {
                if (!rolesGridContainer) return;
                rolesGridContainer.innerHTML =
                    '<div class="permission-empty-state" id="rolesEmptyState">' +
                        '<div class="permission-empty-icon">' +
                            '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                                '<circle cx="12" cy="12" r="10"></circle>' +
                                '<line x1="12" y1="8" x2="12" y2="12"></line>' +
                                '<line x1="12" y1="16" x2="12.01" y2="16"></line>' +
                            '</svg>' +
                        '</div>' +
                        '<div class="permission-empty-title">' + escapeHtml(title) + '</div>' +
                        '<div class="permission-empty-desc">' + escapeHtml(desc) + '</div>' +
                        '<button type="button" class="permission-empty-btn" id="btnRetryRolesNow">' +
                            '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                                '<polyline points="23 4 23 10 17 10"></polyline>' +
                                '<path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>' +
                            '</svg>' +
                            'Thử tải lại vai trò' +
                        '</button>' +
                    '</div>';

                const retryBtn = document.getElementById('btnRetryRolesNow');
                if (retryBtn) {
                    retryBtn.addEventListener('click', function() {
                        fetchRolesList();
                    });
                }
            }

            function fetchRolesList() {
                const endpoint = contextPath + '/api/roles';
                if (rolesLoadingOverlay) rolesLoadingOverlay.classList.add('active');

                fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                })
                .then(function(res) {
                    if (!res.ok) {
                        // Thử endpoint fallback nếu có
                        return fetch(contextPath + '/api/permissions/roles', {
                            method: 'GET',
                            headers: { 'Accept': 'application/json' }
                        }).then(function(fbRes) {
                            if (!fbRes.ok) throw new Error('Endpoint GET /api/roles chưa sẵn sàng (HTTP ' + res.status + ')');
                            return fbRes.json();
                        });
                    }
                    return res.json();
                })
                .then(function(json) {
                    const rolesData = (json && json.data !== undefined) ? json.data : json;
                    renderRolesList(rolesData);
                })
                .catch(function(err) {
                    showRolesEmptyState(
                        'Chưa tải được danh mục vai trò',
                        'Không thể kết nối đến máy chủ để nạp vai trò. (' + err.message + ')'
                    );
                })
                .finally(function() {
                    if (rolesLoadingOverlay) rolesLoadingOverlay.classList.remove('active');
                });
            }

            function syncRoleItemState(item, isChecked) {
                if (isChecked) {
                    item.classList.add('checked');
                } else {
                    item.classList.remove('checked');
                }
                updateRoleCountBadge();
                checkTeamLeadValidation();
                setStatus('Có thay đổi vai trò chưa lưu', false);
            }

            // =================================================================
            // 2. XỬ LÝ TƯƠNG TÁC RADIO CARDS DATA SCOPE (CRM-25)
            // =================================================================
            function updateDataScopeUI(selectedScope) {
                const scope = (selectedScope || 'SELF').toUpperCase();

                // Đổi class active trên các Radio Cards (Cột trái)
                const cards = document.querySelectorAll('.permission-scope-card');
                cards.forEach(function(card) {
                    card.classList.remove('selected');
                });
                const activeLeftCard = document.querySelector('.permission-scope-card.scope-' + scope.toLowerCase());
                if (activeLeftCard) {
                    activeLeftCard.classList.add('selected');
                    const radio = activeLeftCard.querySelector('input[type="radio"]');
                    if (radio && !radio.checked) radio.checked = true;
                }

                // Cập nhật Badge hiển thị trên Cột phải
                if (activeScopeBadge) {
                    let label = 'Cá nhân (SELF)';
                    if (scope === 'TEAM') label = 'Nhóm kinh doanh (TEAM)';
                    if (scope === 'ALL') label = 'Toàn công ty (ALL)';
                    activeScopeBadge.textContent = 'Đang chọn: ' + label;
                }

                // Highlight thẻ thông tin chi tiết tương ứng trên Cột phải
                if (infoCardSelf) infoCardSelf.classList.remove('active-scope');
                if (infoCardTeam) infoCardTeam.classList.remove('active-scope');
                if (infoCardAll) infoCardAll.classList.remove('active-scope');

                if (scope === 'SELF' && infoCardSelf) infoCardSelf.classList.add('active-scope');
                if (scope === 'TEAM' && infoCardTeam) infoCardTeam.classList.add('active-scope');
                if (scope === 'ALL' && infoCardAll) infoCardAll.classList.add('active-scope');

                // Cập nhật đoạn văn mô phỏng ngữ cảnh cho Warning Banner Preview theo AC
                if (warningScenarioText) {
                    if (scope === 'SELF') {
                        warningScenarioText.innerHTML = '<strong>Ngữ cảnh kích hoạt:</strong> Nhân viên có phạm vi <strong>SELF</strong>. Khi truy cập trực tiếp liên kết khách hàng hoặc cơ hội <code>/pipeline/deals/1092</code> do đồng nghiệp khác phụ trách, bộ lọc bảo mật sẽ chặn và hiển thị: <em>"Bạn không có quyền truy cập dữ liệu ngoài phạm vi được phân công"</em>.';
                    } else if (scope === 'TEAM') {
                        warningScenarioText.innerHTML = '<strong>Ngữ cảnh kích hoạt:</strong> Trưởng nhóm có phạm vi <strong>TEAM</strong>. Khi truy cập khách hàng thuộc Nhóm Bán hàng khác <code>/customers/932</code> (ngoài nhóm quản lý), hệ thống sẽ chặn và hiển thị: <em>"Bạn không có quyền truy cập dữ liệu ngoài phạm vi được phân công"</em>.';
                    } else if (scope === 'ALL') {
                        warningScenarioText.innerHTML = '<strong>Ngữ cảnh toàn quyền:</strong> Tài khoản có phạm vi <strong>ALL</strong>. Được toàn quyền truy cập bản ghi trên toàn hệ thống công ty, không kích hoạt hạn chế dữ liệu này.';
                    }
                }

                setStatus('Có thay đổi phạm vi dữ liệu chưa lưu', false);
            }

            function bindDataScopeRadios() {
                const cards = document.querySelectorAll('.permission-scope-card');
                cards.forEach(function(card) {
                    card.addEventListener('click', function() {
                        const radio = card.querySelector('input[type="radio"]');
                        if (radio) {
                            radio.checked = true;
                            updateDataScopeUI(radio.value);
                        }
                    });
                });

                const radios = document.querySelectorAll('input[name="dataScope"]');
                radios.forEach(function(radio) {
                    radio.addEventListener('change', function() {
                        updateDataScopeUI(this.value);
                    });
                });
            }

            // =================================================================
            // 3. XỬ LÝ DANH SÁCH NHÂN VIÊN QUA GET /api/users
            // =================================================================
            function fetchUsersList() {
                if (!userSelect) return;
                const endpoint = contextPath + '/api/users';

                fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                })
                .then(function(res) {
                    if (!res.ok) throw new Error('Endpoint GET /api/users trả về mã HTTP ' + res.status);
                    return res.json();
                })
                .then(function(json) {
                    const usersData = (json && json.data !== undefined) ? json.data : json;
                    if (Array.isArray(usersData) && usersData.length > 0) {
                        userSelect.innerHTML = '<option value="">-- Chọn nhân viên cần phân quyền --</option>';
                        usersData.forEach(function(user) {
                            const opt = document.createElement('option');
                            const uId = user.id !== undefined ? user.id : user.userId;
                            const uName = user.fullName || user.name || user.username || ('Nhân viên #' + uId);
                            const uEmail = user.email || '';
                            const uTeam = user.team || user.teamName || user.department || '';

                            opt.value = uId;
                            opt.setAttribute('data-name', uName);
                            opt.setAttribute('data-email', uEmail);
                            opt.setAttribute('data-team', uTeam);
                            opt.textContent = uName + (uEmail ? ' (' + uEmail + ')' : '');
                            userSelect.appendChild(opt);
                        });
                    } else {
                        userSelect.innerHTML = '<option value="" disabled>-- Chưa có danh sách nhân viên từ hệ thống --</option>';
                    }
                })
                .catch(function(err) {
                    userSelect.innerHTML = '<option value="">-- Chưa tải được danh sách nhân viên (' + escapeHtml(err.message) + ') --</option>';
                });
            }

            // =================================================================
            // 4. XỬ LÝ NHÓM KINH DOANH CRM-29 (GET /api/teams & POST /api/users/{userId}/team)
            // =================================================================

            // Tải danh sách nhóm kinh doanh qua API GET /api/teams theo chuẩn CRM-29
            function fetchTeamsList() {
                if (!teamSelect) return;
                const endpoint = contextPath + '/api/teams';

                fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                })
                .then(function(res) {
                    if (!res.ok) throw new Error('Endpoint GET /api/teams trả về mã HTTP ' + res.status);
                    return res.json();
                })
                .then(function(json) {
                    const teamsData = (json && json.data !== undefined) ? json.data : json;
                    if (Array.isArray(teamsData) && teamsData.length > 0) {
                        const currentVal = teamSelect.value;
                        teamSelect.innerHTML = '<option value="">-- Chọn nhóm kinh doanh --</option>';
                        teamsData.forEach(function(item) {
                            const opt = document.createElement('option');
                            opt.value = item.id !== undefined ? item.id : item.teamId;
                            opt.textContent = item.name || item.teamName || item.title || ('Nhóm #' + opt.value);
                            teamSelect.appendChild(opt);
                        });
                        if (currentVal) teamSelect.value = currentVal;
                    }
                })
                .catch(function(err) {
                    if (teamSelect.options.length <= 1) {
                        const opt = document.createElement('option');
                        opt.value = '';
                        opt.disabled = true;
                        opt.textContent = '-- Chưa có danh mục nhóm kinh doanh (GET /api/teams) --';
                        teamSelect.appendChild(opt);
                    }
                });
            }

            // Kiểm tra xem tài khoản đang chọn có được tích vai trò Trưởng nhóm (Team Lead) hay không
            function isCurrentSelectedUserTeamLead() {
                const checkedBoxes = document.querySelectorAll('input[name="roleIds"]:checked');
                for (let i = 0; i < checkedBoxes.length; i++) {
                    const cb = checkedBoxes[i];
                    const parent = cb.closest('.permission-role-item');
                    if (!parent) continue;
                    const title = (parent.querySelector('.permission-role-title')?.textContent || '').toUpperCase();
                    const code = (parent.querySelector('.permission-role-code')?.textContent || '').toUpperCase();
                    if (code.includes('MANAGER') || code.includes('LEAD') || code.includes('LEADER') ||
                        title.includes('TRƯỞNG NHÓM') || title.includes('QUẢN LÝ') || title.includes('LEAD')) {
                        return true;
                    }
                }
                return false;
            }

            // Kiểm tra và hiển thị validation rõ ràng khi Team Lead chưa có Team (CRM-29)
            function checkTeamLeadValidation() {
                if (!userSelect || !userSelect.value) {
                    if (teamLeadWarning) teamLeadWarning.style.display = 'none';
                    if (teamSelect) teamSelect.classList.remove('warning-border');
                    if (teamStatusText) teamStatusText.textContent = 'Chưa chọn nhân viên';
                    if (teamStatusDot) teamStatusDot.classList.remove('active');
                    return;
                }

                const isLead = isCurrentSelectedUserTeamLead();
                const hasAssignedTeam = currentUserTeamName && currentUserTeamName !== 'Chưa phân nhóm' && currentUserTeamName !== '';
                const hasSelectedTeam = teamSelect && teamSelect.value !== '';

                if (isLead && !hasAssignedTeam && !hasSelectedTeam) {
                    if (teamLeadWarning) teamLeadWarning.style.display = 'flex';
                    if (teamSelect) teamSelect.classList.add('warning-border');
                    if (teamStatusText) teamStatusText.textContent = 'Cần gán nhóm cho Team Lead';
                    if (teamStatusDot) teamStatusDot.classList.remove('active');
                } else {
                    if (teamLeadWarning) teamLeadWarning.style.display = 'none';
                    if (teamSelect) teamSelect.classList.remove('warning-border');
                    if (teamStatusText) {
                        teamStatusText.textContent = hasAssignedTeam ? 'Đã phân nhóm: ' + currentUserTeamName : 'Chưa phân nhóm';
                    }
                    if (teamStatusDot) {
                        if (hasAssignedTeam) teamStatusDot.classList.add('active');
                        else teamStatusDot.classList.remove('active');
                    }
                }
            }

            // Gán nhóm kinh doanh chuẩn endpoint POST /api/users/${userId}/team (CRM-29)
            function assignUserTeam() {
                const userId = userSelect ? userSelect.value : '';
                if (!userId) {
                    showError('Vui lòng chọn nhân viên trước khi thực hiện gán nhóm kinh doanh.');
                    if (userSelect) userSelect.focus();
                    return;
                }

                const teamIdVal = teamSelect ? teamSelect.value : '';
                if (!teamIdVal) {
                    showError('Vui lòng chọn nhóm kinh doanh cần gán cho nhân viên.');
                    if (teamSelect) teamSelect.focus();
                    return;
                }

                const payload = {
                    teamId: isNaN(teamIdVal) ? teamIdVal : Number(teamIdVal)
                };

                if (btnAssignTeam) btnAssignTeam.disabled = true;
                if (btnAssignTeamSpinner) btnAssignTeamSpinner.style.display = 'inline-block';
                if (btnAssignTeamIcon) btnAssignTeamIcon.style.display = 'none';
                if (btnAssignTeamText) btnAssignTeamText.textContent = 'Đang gán nhóm...';
                clearAlerts();
                setStatus('Đang gửi yêu cầu gán nhóm kinh doanh...', false);

                // Endpoint REST chuẩn: POST /api/users/${userId}/team
                const endpoint = contextPath + '/api/users/' + encodeURIComponent(userId) + '/team';

                fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                })
                .then(function(response) {
                    const contentType = response.headers.get('content-type') || '';
                    if (contentType.indexOf('application/json') !== -1) {
                        return response.json().then(function(json) {
                            return { ok: response.ok, status: response.status, body: json };
                        });
                    } else {
                        return response.text().then(function(text) {
                            return { ok: response.ok, status: response.status, rawText: text };
                        });
                    }
                })
                .then(function(res) {
                    if (!res.ok) {
                        let errMsg = '';
                        if (res.body && res.body.message) {
                            errMsg = res.body.message;
                        } else if (res.status === 404) {
                            errMsg = 'Endpoint POST /api/users/{userId}/team chưa sẵn sàng phía Backend (HTTP 404 Not Found).';
                        } else if (res.status === 403) {
                            errMsg = 'Bạn không có quyền thực hiện gán nhóm kinh doanh (HTTP 403 Forbidden).';
                        } else {
                            errMsg = 'Gán nhóm kinh doanh thất bại (HTTP ' + res.status + ').';
                        }
                        throw new Error(errMsg);
                    }

                    const result = res.body;
                    if (result && result.success === false) {
                        throw new Error(result.message || 'Gán nhóm kinh doanh thất bại từ máy chủ.');
                    }

                    const successMsg = (result && result.message) ? result.message : 'Gán nhân viên vào nhóm kinh doanh thành công!';
                    showSuccess(successMsg);

                    // Cập nhật tên nhóm mới hiển thị trên giao diện
                    const selectedOpt = teamSelect.options[teamSelect.selectedIndex];
                    const newTeamName = selectedOpt ? selectedOpt.text : 'Đã phân nhóm';
                    currentUserTeamName = newTeamName;
                    if (currentTeamDisplay) currentTeamDisplay.textContent = newTeamName;
                    if (userTeamDisplay) userTeamDisplay.textContent = newTeamName;

                    // Cập nhật data-team trên option của userSelect
                    const currentSelectedUserOpt = userSelect.options[userSelect.selectedIndex];
                    if (currentSelectedUserOpt) {
                        currentSelectedUserOpt.setAttribute('data-team', newTeamName);
                    }

                    checkTeamLeadValidation();
                })
                .catch(function(err) {
                    showError(err.message || 'Lỗi không xác định khi gán nhóm kinh doanh.');
                })
                .finally(function() {
                    if (btnAssignTeam) btnAssignTeam.disabled = false;
                    if (btnAssignTeamSpinner) btnAssignTeamSpinner.style.display = 'none';
                    if (btnAssignTeamIcon) btnAssignTeamIcon.style.display = 'inline-block';
                    if (btnAssignTeamText) btnAssignTeamText.textContent = 'Gán nhóm';
                });
            }

            // =================================================================
            // 5. TẢI VÀ LƯU PHÂN QUYỀN (GET & POST /api/permissions/...)
            // =================================================================

            // Áp dụng dữ liệu phân quyền lên Form từ API
            function applyUserPermissionsToForm(data) {
                if (!data) return;

                const roleList = Array.isArray(data.roles) ? data.roles : [];
                const roleIdSet = new Set();
                roleList.forEach(function(item) {
                    if (typeof item === 'object' && item !== null) {
                        const val = item.id !== undefined ? item.id : (item.roleId !== undefined ? item.roleId : item.code);
                        if (val !== undefined && val !== null) roleIdSet.add(String(val));
                    } else if (item !== undefined && item !== null) {
                        roleIdSet.add(String(item));
                    }
                });

                // Cập nhật các switch vai trò
                const roleItems = document.querySelectorAll('.permission-role-item');
                roleItems.forEach(function(item) {
                    const cb = item.querySelector('input[type="checkbox"]');
                    if (cb) {
                        const isChecked = roleIdSet.has(String(cb.value));
                        cb.checked = isChecked;
                        if (isChecked) item.classList.add('checked');
                        else item.classList.remove('checked');
                    }
                });

                // Cập nhật phạm vi dữ liệu Data Scope (SELF | TEAM | ALL)
                const dataScopeVal = (data.dataScope || 'SELF').toUpperCase();
                updateDataScopeUI(dataScopeVal);

                // Cập nhật team nếu API trả về
                if (data.team) {
                    const teamName = typeof data.team === 'object' ? (data.team.name || data.team.teamName || '') : String(data.team);
                    const teamId = typeof data.team === 'object' ? (data.team.id || data.team.teamId || '') : '';
                    if (teamName) {
                        currentUserTeamName = teamName;
                        if (userTeamDisplay) userTeamDisplay.textContent = teamName;
                        if (currentTeamDisplay) currentTeamDisplay.textContent = teamName;

                        if (teamSelect) {
                            for (let i = 0; i < teamSelect.options.length; i++) {
                                const opt = teamSelect.options[i];
                                if ((teamId && String(opt.value) === String(teamId)) || (opt.text === teamName)) {
                                    teamSelect.selectedIndex = i;
                                    break;
                                }
                            }
                        }
                    }
                }

                updateRoleCountBadge();
                checkTeamLeadValidation();
            }

            // Gọi API GET /api/permissions/users/{userId} khi đổi nhân viên
            function loadUserPermissions(userId) {
                if (!userId) {
                    if (userSummaryCard) userSummaryCard.style.display = 'none';
                    currentUserTeamName = '';
                    if (currentTeamDisplay) currentTeamDisplay.textContent = 'Chưa phân nhóm';
                    if (teamSelect) teamSelect.value = '';

                    // Reset form vai trò về mặc định
                    document.querySelectorAll('.permission-role-item').forEach(function(item) {
                        const cb = item.querySelector('input[type="checkbox"]');
                        if (cb) cb.checked = false;
                        item.classList.remove('checked');
                    });
                    updateDataScopeUI('SELF');
                    setStatus('Chờ chọn nhân viên', false);
                    checkTeamLeadValidation();
                    return;
                }

                // Hiển thị thông tin nhân viên từ data-attributes của option
                const selectedOption = userSelect.options[userSelect.selectedIndex];
                const userName = selectedOption ? selectedOption.getAttribute('data-name') : '';
                const userEmail = selectedOption ? selectedOption.getAttribute('data-email') : '';
                const userTeam = selectedOption ? selectedOption.getAttribute('data-team') : '';
                currentUserTeamName = userTeam || '';
                if (currentTeamDisplay) currentTeamDisplay.textContent = currentUserTeamName || 'Chưa phân nhóm';
                updateUserSummary(userName, userEmail, userTeam);

                // Khớp chọn nhóm trong teamSelect nếu trùng tên
                if (teamSelect) {
                    if (currentUserTeamName) {
                        for (let i = 0; i < teamSelect.options.length; i++) {
                            if (teamSelect.options[i].text === currentUserTeamName) {
                                teamSelect.selectedIndex = i;
                                break;
                            }
                        }
                    } else {
                        teamSelect.value = '';
                    }
                }

                // Bật overlay loading
                if (rolesLoadingOverlay) rolesLoadingOverlay.classList.add('active');
                clearAlerts();
                setStatus('Đang tải dữ liệu phân quyền...', false);

                const endpoint = contextPath + '/api/permissions/users/' + encodeURIComponent(userId);

                fetch(endpoint, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                })
                .then(function(response) {
                    if (!response.ok) {
                        if (response.status === 404) {
                            throw new Error('Endpoint GET /api/permissions/users/{userId} chưa sẵn sàng phía Backend (HTTP 404 Not Found).');
                        } else {
                            throw new Error('Không thể tải phân quyền nhân viên (Mã HTTP: ' + response.status + ').');
                        }
                    }
                    return response.json();
                })
                .then(function(resData) {
                    const payload = (resData && resData.data !== undefined) ? resData.data : resData;
                    applyUserPermissionsToForm(payload);
                    initialUserState = JSON.parse(JSON.stringify(payload));
                    setStatus('Đã tải thông tin phân quyền nhân viên', true);
                })
                .catch(function(err) {
                    showError(err.message);
                })
                .finally(function() {
                    if (rolesLoadingOverlay) rolesLoadingOverlay.classList.remove('active');
                    checkTeamLeadValidation();
                });
            }

            // Lưu phân quyền gửi tới POST /api/permissions/assign theo đúng quy chuẩn REST
            function savePermissions() {
                const userId = userSelect ? userSelect.value : '';
                if (!userId) {
                    showError('Vui lòng chọn nhân viên cần phân quyền trước khi lưu.');
                    if (userSelect) userSelect.focus();
                    return;
                }

                // Thu thập danh sách roleIds
                const selectedBoxes = document.querySelectorAll('input[name="roleIds"]:checked');
                const roleIds = [];
                selectedBoxes.forEach(function(box) {
                    const val = box.value;
                    roleIds.push(isNaN(val) ? val : Number(val));
                });

                // Thu thập dataScope
                const scopeRadio = document.querySelector('input[name="dataScope"]:checked');
                const dataScope = scopeRadio ? scopeRadio.value : 'SELF';

                // Trạng thái Loading trên nút bấm
                if (btnSavePermissions) btnSavePermissions.disabled = true;
                if (btnSaveSpinner) btnSaveSpinner.style.display = 'inline-block';
                if (btnSaveIcon) btnSaveIcon.style.display = 'none';
                if (btnSaveText) btnSaveText.textContent = 'Đang lưu phân quyền...';
                clearAlerts();
                setStatus('Đang gửi yêu cầu lưu phân quyền...', false);

                // Endpoint REST chuẩn: POST /api/permissions/assign (Quy chuẩn kỹ thuật #1)
                const endpoint = contextPath + '/api/permissions/assign';
                const requestPayload = {
                    userId: isNaN(userId) ? userId : Number(userId),
                    roleIds: roleIds,
                    dataScope: dataScope
                };

                fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(requestPayload)
                })
                .then(function(response) {
                    const contentType = response.headers.get('content-type') || '';
                    if (contentType.indexOf('application/json') !== -1) {
                        return response.json().then(function(json) {
                            return { ok: response.ok, status: response.status, body: json };
                        });
                    } else {
                        if (response.ok) {
                            return { ok: true, status: response.status, body: { success: true } };
                        }
                        return response.text().then(function(text) {
                            return { ok: response.ok, status: response.status, rawText: text };
                        });
                    }
                })
                .then(function(res) {
                    if (!res.ok) {
                        let errMsg = '';
                        if (res.body && res.body.message) {
                            errMsg = res.body.message;
                        } else if (res.status === 404) {
                            errMsg = 'Endpoint POST /api/permissions/assign chưa sẵn sàng phía Backend (HTTP 404 Not Found).';
                        } else if (res.status === 500) {
                            errMsg = 'Máy chủ gặp sự cố nội bộ khi lưu phân quyền (HTTP 500 Internal Server Error).';
                        } else {
                            errMsg = 'Yêu cầu lưu phân quyền thất bại (HTTP ' + res.status + ').';
                        }
                        throw new Error(errMsg);
                    }

                    const result = res.body;
                    if (result && result.success === false) {
                        throw new Error(result.message || 'Lưu phân quyền thất bại từ hệ thống.');
                    }

                    const message = (result && result.message) ? result.message : 'Lưu vai trò và phạm vi dữ liệu sở hữu thành công!';
                    showSuccess(message);

                    initialUserState = {
                        roles: roleIds,
                        dataScope: dataScope
                    };
                })
                .catch(function(err) {
                    showError(err.message || 'Lỗi không xác định khi lưu phân quyền.');
                })
                .finally(function() {
                    if (btnSavePermissions) btnSavePermissions.disabled = false;
                    if (btnSaveSpinner) btnSaveSpinner.style.display = 'none';
                    if (btnSaveIcon) btnSaveIcon.style.display = 'inline-block';
                    if (btnSaveText) btnSaveText.textContent = 'Lưu phân quyền';
                });
            }

            // Gán các sự kiện tương tác
            function initEvents() {
                // Đổi nhân viên
                if (userSelect) {
                    userSelect.addEventListener('change', function() {
                        loadUserPermissions(this.value);
                    });
                }

                // Đổi nhóm kinh doanh trong dropdown (CRM-29)
                if (teamSelect) {
                    teamSelect.addEventListener('change', function() {
                        checkTeamLeadValidation();
                        setStatus('Có thay đổi nhóm chưa lưu', false);
                    });
                }

                // Nút Gán nhóm kinh doanh (CRM-29)
                if (btnAssignTeam) {
                    btnAssignTeam.addEventListener('click', function(e) {
                        e.preventDefault();
                        assignUserTeam();
                    });
                }

                // Nút bật tất cả vai trò
                if (btnSelectAllRoles) {
                    btnSelectAllRoles.addEventListener('click', function() {
                        document.querySelectorAll('.permission-role-item').forEach(function(item) {
                            const cb = item.querySelector('input[type="checkbox"]');
                            if (cb) cb.checked = true;
                            item.classList.add('checked');
                        });
                        updateRoleCountBadge();
                        checkTeamLeadValidation();
                        setStatus('Có thay đổi vai trò chưa lưu', false);
                    });
                }

                // Nút tắt tất cả vai trò
                if (btnDeselectAllRoles) {
                    btnDeselectAllRoles.addEventListener('click', function() {
                        document.querySelectorAll('.permission-role-item').forEach(function(item) {
                            const cb = item.querySelector('input[type="checkbox"]');
                            if (cb) cb.checked = false;
                            item.classList.remove('checked');
                        });
                        updateRoleCountBadge();
                        checkTeamLeadValidation();
                        setStatus('Có thay đổi vai trò chưa lưu', false);
                    });
                }

                // Nút thử tải lại vai trò (Empty state)
                if (btnRetryRoles) {
                    btnRetryRoles.addEventListener('click', function() {
                        fetchRolesList();
                    });
                }

                // Submit Form
                const permForm = document.getElementById('permissionForm');
                if (permForm) {
                    permForm.addEventListener('submit', function(e) {
                        e.preventDefault();
                        savePermissions();
                    });
                }

                // Nút Đặt lại
                if (btnResetPermissions) {
                    btnResetPermissions.addEventListener('click', function() {
                        clearAlerts();
                        if (userSelect && userSelect.value && initialUserState) {
                            applyUserPermissionsToForm(initialUserState);
                            setStatus('Đã khôi phục trạng thái gần nhất', true);
                        } else if (userSelect && userSelect.value) {
                            loadUserPermissions(userSelect.value);
                        } else {
                            setStatus('Sẵn sàng thiết lập phân quyền', false);
                        }
                    });
                }

                // Gán tương tác Radio Cards
                bindDataScopeRadios();

                // Nạp dữ liệu ban đầu qua REST APIs
                fetchUsersList();
                fetchTeamsList();
                fetchRolesList();

                // Mặc định ban đầu là SELF
                updateDataScopeUI('SELF');
            }

            // Khởi chạy khi DOM sẵn sàng
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initEvents);
            } else {
                initEvents();
            }
        })();
    </script>
</body>
</html>
