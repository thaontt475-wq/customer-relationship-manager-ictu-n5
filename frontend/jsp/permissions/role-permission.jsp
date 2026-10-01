<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phân quyền & Phạm vi dữ liệu sở hữu - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">


    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/permissions/permissions.css">
</head>
<body class="crm-body">

    <!-- Include Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Include Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Phân quyền -->
        <main class="permission-page crm-page" id="permissionApp" role="main">
            <div class="permission-container crm-page-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="permission-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="#">Hệ thống</a>
                    <span class="separator">/</span>
                    <span class="active">Phân quyền & Phạm vi dữ liệu</span>
                </nav>

                <!-- Header màn hình -->
                <header class="permission-header crm-page-header">
                    <div class="permission-header-info">
                        <h1>Phân quyền & Phạm vi dữ liệu sở hữu</h1>
                        <p>Cấu hình vai trò hệ thống (Roles) và phạm vi truy cập dữ liệu (Data Scope: SELF, TEAM, ALL) cho nhân sự trong CRM.</p>
                    </div>

                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts / Banners) -->
                <div class="permission-alerts" id="permissionAlertsArea" aria-live="polite">
                    <!-- Alert Báo Lỗi -->
                    <div class="permission-alert permission-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="permission-alert-content">
                            <div class="permission-alert-title" id="globalErrorTitle">Thông báo lỗi</div>
                            <div id="globalErrorMessage"></div>
                        </div>
                        <button type="button" class="permission-alert-close" id="btnCloseErrorAlert" aria-label="Đóng thông báo">&times;</button>
                    </div>

                    <!-- Alert Báo Thành Công -->
                    <div class="permission-alert permission-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="permission-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="permission-alert-content">
                            <div class="permission-alert-title" id="globalSuccessTitle">Thành công</div>
                            <div id="globalSuccessMessage"></div>
                        </div>
                        <button type="button" class="permission-alert-close" id="btnCloseSuccessAlert" aria-label="Đóng thông báo">&times;</button>
                    </div>
                </div>

                <!-- Bố cục 2 cột (Responsive Layout Grid) -->
                <div class="permission-layout-grid">

                    <!-- CỘT TRÁI: FORM CẤU HÌNH PHÂN QUYỀN -->
                    <div class="permission-left-col">
                        <form id="permissionForm" novalidate>
                            <input type="hidden" id="selectedUserIdHidden" name="userId" value="">

                            <!-- BƯỚC 1: Chọn người dùng -->
                            <section class="permission-card crm-card" id="userCardSection" style="position: relative;">
                                <!-- Loading overlay -->
                                <div class="permission-loading-overlay" id="userCardLoading" aria-hidden="true">
                                    <div class="permission-loading-box">
                                        <span class="permission-spinner permission-spinner-dark"></span>
                                        <span>Đang tải thông tin...</span>
                                    </div>
                                </div>

                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">1</span>
                                        <div>
                                            <h2>Chọn người dùng</h2>
                                            <div class="permission-card-subtitle">Lựa chọn nhân sự cần thiết lập vai trò và phạm vi dữ liệu</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <div class="permission-user-selector">
                                        <div class="permission-form-group">
                                            <label for="userSelect" class="permission-label">
                                                Tài khoản người dùng <span style="color: var(--perm-danger);">*</span>
                                            </label>
                                            <div class="permission-select-wrapper">
                                                <select id="userSelect" name="viewUserId" class="permission-select crm-select" required>
                                                    <option value="">-- Đang nạp danh sách tài khoản người dùng... --</option>
                                                </select>
                                            </div>
                                        </div>

                                        <!-- Thẻ tóm tắt thông tin người dùng được chọn -->
                                        <div class="permission-user-summary" id="userSummaryCard" style="display: none;">
                                            <div class="permission-user-avatar" id="userAvatarText">U</div>
                                            <div class="permission-user-meta">
                                                <div class="permission-user-name" id="userNameDisplay">Họ và tên người dùng</div>
                                                <div class="permission-user-subdetails">
                                                    <span id="userEmailDisplay">email@crm.vn</span>
                                                    <span class="permission-user-tag">
                                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                            <circle cx="9" cy="7" r="4"></circle>
                                                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                        </svg>
                                                        Phòng ban / Nhóm: <strong id="userTeamDisplay" style="margin-left: 4px;">Chưa phân nhóm</strong>
                                                    </span>
                                                    <span class="permission-user-tag">
                                                        Đang chọn: <strong id="activeRoleCountDisplay" style="margin-left: 4px;">0 vai trò</strong>
                                                    </span>
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Empty state khi chưa chọn người dùng -->
                                        <div class="permission-empty-state" id="userEmptyStateNotice">
                                            <div class="permission-empty-icon" aria-hidden="true"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></svg></div>
                                            <div class="permission-empty-title">Chưa chọn tài khoản</div>
                                            <p class="permission-empty-desc">Vui lòng chọn một người dùng từ danh sách phía trên để nạp quyền hạn và phạm vi dữ liệu hiện tại.</p>
                                        </div>
                                    </div>
                                </div>
                            </section>


                            <section class="permission-card crm-card" id="teamCardSection" style="margin-top: 20px;">
                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">2</span>
                                        <div>
                                            <h2>Nhóm kinh doanh (Sales Team)</h2>
                                            <div class="permission-card-subtitle">Gán người dùng vào nhóm kinh doanh phụ trách (bắt buộc cho Team Lead)</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <div class="permission-team-layout">
                                        <div class="permission-current-team-card">
                                            <div class="permission-current-team-left">
                                                <div class="permission-current-team-icon" aria-hidden="true">
                                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                        <circle cx="9" cy="7" r="4"></circle>
                                                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                    </svg>
                                                </div>
                                                <div class="permission-current-team-info">
                                                    <span class="permission-current-team-label">Nhóm kinh doanh hiện tại</span>
                                                    <span class="permission-current-team-name" id="currentTeamNameDisplay">Chưa phân nhóm</span>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="permission-team-assign-form">
                                            <div class="permission-team-select-group">
                                                <label for="teamSelect" class="permission-label">Chọn nhóm phân bổ mới</label>
                                                <div class="permission-select-wrapper">
                                                    <select id="teamSelect" name="teamId" class="permission-select crm-select" disabled>
                                                        <option value="">-- Chọn nhóm kinh doanh --</option>
                                                    </select>
                                                </div>
                                            </div>
                                            <button type="button" class="permission-btn crm-btn permission-btn-secondary crm-btn-secondary" id="btnAssignTeam" disabled>
                                                <span class="permission-spinner permission-spinner-dark" id="btnAssignTeamSpinner" style="display: none;"></span>
                                                <span id="btnAssignTeamText">Gán nhóm</span>
                                            </button>
                                        </div>

                                        <!-- Cảnh báo khi tick chọn Team Lead mà user chưa có nhóm -->
                                        <div class="permission-lead-warning" id="teamLeadWarning" style="display: none;">
                                            <svg class="permission-lead-warning-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                                <line x1="12" y1="9" x2="12" y2="13"></line>
                                                <line x1="12" y1="17" x2="12.01" y2="17"></line>
                                            </svg>
                                            <div class="permission-lead-warning-content">
                                                <div class="permission-lead-warning-title">Yêu cầu nhóm kinh doanh</div>
                                                <div>Vai trò <strong>Team Lead</strong> bắt buộc tài khoản phải thuộc một nhóm kinh doanh để quản lý thành viên và dữ liệu đội nhóm. Hãy chọn nhóm và bấm "Gán nhóm".</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <!-- BƯỚC 3: Chọn vai trò hệ thống (Roles) -->
                            <section class="permission-card crm-card" id="rolesCardSection" style="margin-top: 20px;">
                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">3</span>
                                        <div>
                                            <h2>Vai trò hệ thống (Roles)</h2>
                                            <div class="permission-card-subtitle">Có thể chọn một hoặc nhiều vai trò nghiệp vụ cho tài khoản</div>
                                        </div>
                                    </div>
                                    <div class="permission-quick-actions">
                                        <button type="button" class="permission-btn-subtle" id="btnSelectAllRoles">Chọn tất cả</button>
                                        <button type="button" class="permission-btn-subtle" id="btnDeselectAllRoles">Bỏ chọn</button>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <div class="permission-role-grid" id="rolesGridContainer">
                                        <!-- Role 1: Admin -->
                                        <label class="permission-role-item" for="role_1" id="roleItem_1">
                                            <div class="permission-role-details">
                                                <div class="permission-role-top">
                                                    <span class="permission-role-title">Admin (Quản trị viên)</span>
                                                    <span class="permission-role-code">ROLE_ADMIN</span>
                                                </div>
                                                <p class="permission-role-desc">Toàn quyền cấu hình hệ thống, quản lý tài khoản người dùng, phân quyền và giám sát hoạt động CRM.</p>
                                            </div>
                                            <div class="permission-toggle-wrap">
                                                <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_1" value="1">
                                                <span class="permission-switch" aria-hidden="true">
                                                    <span class="permission-switch-slider"></span>
                                                </span>
                                            </div>
                                        </label>

                                        <!-- Role 2: Sales Rep -->
                                        <label class="permission-role-item" for="role_2" id="roleItem_2">
                                            <div class="permission-role-details">
                                                <div class="permission-role-top">
                                                    <span class="permission-role-title">Sales Rep (Kinh doanh)</span>
                                                    <span class="permission-role-code">ROLE_SALES</span>
                                                </div>
                                                <p class="permission-role-desc">Trực tiếp tiếp cận khách hàng tiềm năng, tạo và xử lý cơ hội, lập báo giá và ghi nhận tương tác bán hàng.</p>
                                            </div>
                                            <div class="permission-toggle-wrap">
                                                <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_2" value="2">
                                                <span class="permission-switch" aria-hidden="true">
                                                    <span class="permission-switch-slider"></span>
                                                </span>
                                            </div>
                                        </label>

                                        <!-- Role 3: Accountant -->
                                        <label class="permission-role-item" for="role_3" id="roleItem_3">
                                            <div class="permission-role-details">
                                                <div class="permission-role-top">
                                                    <span class="permission-role-title">Accountant (Kế toán)</span>
                                                    <span class="permission-role-code">ROLE_ACCOUNTANT</span>
                                                </div>
                                                <p class="permission-role-desc">Theo dõi hóa đơn, duyệt và đối soát báo giá hợp đồng, quản lý thanh toán và thông tin tài chính.</p>
                                            </div>
                                            <div class="permission-toggle-wrap">
                                                <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_3" value="3">
                                                <span class="permission-switch" aria-hidden="true">
                                                    <span class="permission-switch-slider"></span>
                                                </span>
                                            </div>
                                        </label>

                                        <!-- Role 4: Team Lead -->
                                        <label class="permission-role-item" for="role_4" id="roleItem_4">
                                            <div class="permission-role-details">
                                                <div class="permission-role-top">
                                                    <span class="permission-role-title">Team Lead (Trưởng nhóm)</span>
                                                    <span class="permission-role-code">ROLE_LEAD</span>
                                                </div>
                                                <p class="permission-role-desc">Trưởng nhóm kinh doanh, quản lý đội ngũ nhân viên, phân công khách hàng và giám sát cơ hội của toàn đội nhóm.</p>
                                            </div>
                                            <div class="permission-toggle-wrap">
                                                <input type="checkbox" class="permission-role-checkbox" name="roleIds" id="role_4" value="4">
                                                <span class="permission-switch" aria-hidden="true">
                                                    <span class="permission-switch-slider"></span>
                                                </span>
                                            </div>
                                        </label>
                                    </div>
                                </div>
                            </section>

                            <!-- BƯỚC 4: Phạm vi dữ liệu sở hữu (Data Scope) -->
                            <section class="permission-card crm-card" id="dataScopeCardSection" style="margin-top: 20px;">
                                <div class="permission-card-header">
                                    <div class="permission-card-title-group">
                                        <span class="permission-card-step">4</span>
                                        <div>
                                            <h2>Phạm vi dữ liệu sở hữu (Data Scope)</h2>
                                            <div class="permission-card-subtitle">Quy định giới hạn bản ghi dữ liệu người dùng được phép xem, sửa hoặc thao tác</div>
                                        </div>
                                    </div>
                                </div>
                                <div class="permission-card-body">
                                    <div class="permission-scope-grid" id="dataScopeGrid">
                                        <!-- Option 1: SELF -->
                                        <label class="permission-scope-card scope-self selected" for="scope_self" id="scopeCardSelf">
                                            <div class="permission-scope-icon-wrap" aria-hidden="true">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="12" cy="7" r="4"></circle>
                                                </svg>
                                            </div>
                                            <div class="permission-scope-content">
                                                <div class="permission-scope-top-row">
                                                    <div class="permission-scope-name">Cá nhân (SELF)</div>
                                                    <span class="permission-scope-badge">Mức bảo mật hẹp</span>
                                                </div>
                                                <p class="permission-scope-desc">
                                                    Chỉ xem và thao tác trên dữ liệu (khách hàng, cơ hội, hoạt động, báo giá) do chính tài khoản tạo ra hoặc được phân công phụ trách trực tiếp.
                                                </p>
                                            </div>
                                            <div class="permission-scope-radio-wrap">
                                                <input type="radio" class="permission-scope-radio" name="dataScope" id="scope_self" value="SELF" checked>
                                            </div>
                                        </label>

                                        <!-- Option 2: TEAM -->
                                        <label class="permission-scope-card scope-team" for="scope_team" id="scopeCardTeam">
                                            <div class="permission-scope-icon-wrap" aria-hidden="true">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="9" cy="7" r="4"></circle>
                                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                                </svg>
                                            </div>
                                            <div class="permission-scope-content">
                                                <div class="permission-scope-top-row">
                                                    <div class="permission-scope-name">Nhóm / Phòng ban (TEAM)</div>
                                                    <span class="permission-scope-badge">Cộng tác nhóm</span>
                                                </div>
                                                <p class="permission-scope-desc">
                                                    Được quyền xem và thao tác trên toàn bộ dữ liệu của tất cả các thành viên trực thuộc cùng đội nhóm / phòng ban làm việc.
                                                </p>
                                            </div>
                                            <div class="permission-scope-radio-wrap">
                                                <input type="radio" class="permission-scope-radio" name="dataScope" id="scope_team" value="TEAM">
                                            </div>
                                        </label>

                                        <!-- Option 3: ALL -->
                                        <label class="permission-scope-card scope-all" for="scope_all" id="scopeCardAll">
                                            <div class="permission-scope-icon-wrap" aria-hidden="true">
                                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                    <circle cx="12" cy="12" r="10"></circle>
                                                    <line x1="2" y1="12" x2="22" y2="12"></line>
                                                    <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
                                                </svg>
                                            </div>
                                            <div class="permission-scope-content">
                                                <div class="permission-scope-top-row">
                                                    <div class="permission-scope-name">Toàn hệ thống (ALL)</div>
                                                    <span class="permission-scope-badge">Toàn quyền dữ liệu</span>
                                                </div>
                                                <p class="permission-scope-desc">
                                                    Toàn quyền truy cập, xem và xử lý toàn bộ dữ liệu khách hàng, cơ hội, hoạt động và báo giá trên mọi phòng ban toàn doanh nghiệp.
                                                </p>
                                            </div>
                                            <div class="permission-scope-radio-wrap">
                                                <input type="radio" class="permission-scope-radio" name="dataScope" id="scope_all" value="ALL">
                                            </div>
                                        </label>
                                    </div>
                                </div>
                            </section>

                            <!-- THANH HÀNH ĐỘNG (ACTION BAR) -->
                            <footer class="permission-actions-bar" style="margin-top: 20px;">
                                <div class="permission-status-hint">
                                    <span class="permission-status-dot" id="permissionStatusDot"></span>
                                    <span id="permissionStatusText">Chưa chọn người dùng</span>
                                </div>
                                <div class="permission-buttons">
                                    <button type="button" class="permission-btn crm-btn permission-btn-secondary crm-btn-secondary" id="btnResetPermissions" disabled>
                                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                            <polyline points="1 4 1 10 7 10"></polyline>
                                            <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                        </svg>
                                        Đặt lại
                                    </button>
                                    <button type="button" class="permission-btn crm-btn permission-btn-primary crm-btn-primary" id="btnSavePermissions" disabled>
                                        <span class="permission-spinner" id="btnSaveSpinner" style="display: none;"></span>
                                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" id="btnSaveIcon" aria-hidden="true">
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

                    <!-- CỘT PHẢI: KHỐI HƯỚNG DẪN TRỰC QUAN & MA TRẬN PHÂN QUYỀN -->
                    <div class="permission-right-col">

                        <!-- 1. Hướng dẫn trực quan 3 cấp độ Data Scope (Tương tác theo radio đang chọn) -->
                        <section class="permission-guide-card">
                            <header class="permission-guide-header">
                                <div class="permission-guide-title">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                        <circle cx="12" cy="12" r="10"></circle>
                                        <line x1="12" y1="16" x2="12" y2="12"></line>
                                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                                    </svg>
                                    <span>Quy tắc 3 cấp độ dữ liệu</span>
                                </div>
                                <span class="permission-active-scope-indicator" id="activeScopeBadgeText">
                                    Đang xem: SELF
                                </span>
                            </header>
                            <div class="permission-guide-body">
                                <!-- Card thông tin SELF -->
                                <div class="scope-info-card info-self active-scope" id="infoCardSelf">
                                    <div class="scope-info-header">
                                        <div class="scope-info-title-group">
                                            <div class="scope-info-icon-badge" aria-hidden="true"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></svg></div>
                                            <div class="scope-info-title">Cá nhân (SELF)</div>
                                        </div>
                                        <span class="scope-info-tag">Cơ bản</span>
                                    </div>
                                    <p class="scope-info-desc">Phù hợp nhất cho Nhân viên kinh doanh (Sales Rep) và cộng tác viên bán hàng.</p>
                                    <ul class="scope-info-list">
                                        <li><strong>Khách hàng:</strong> Chỉ xem danh sách khách do chính mình tạo hoặc được bàn giao.</li>
                                        <li><strong>Cơ hội & Giao dịch:</strong> Chỉ truy cập deals mình đang phụ trách.</li>
                                        <li><strong>Hoạt động & Báo giá:</strong> Giới hạn theo các tác vụ cá nhân.</li>
                                        <li><strong>Tìm kiếm & Excel:</strong> Bộ lọc tự động ẩn toàn bộ dữ liệu của đồng nghiệp.</li>
                                    </ul>
                                </div>

                                <!-- Card thông tin TEAM -->
                                <div class="scope-info-card info-team" id="infoCardTeam">
                                    <div class="scope-info-header">
                                        <div class="scope-info-title-group">
                                            <div class="scope-info-icon-badge" aria-hidden="true"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></svg></div>
                                            <div class="scope-info-title">Đội nhóm (TEAM)</div>
                                        </div>
                                        <span class="scope-info-tag">Quản lý nhóm</span>
                                    </div>
                                    <p class="scope-info-desc">Dành cho Trưởng nhóm kinh doanh  để điều phối chỉ tiêu và hỗ trợ thành viên.</p>
                                    <ul class="scope-info-list">
                                        <li><strong>Khách hàng:</strong> Xem và phân bổ khách hàng của tất cả thành viên trong nhóm.</li>
                                        <li><strong>Cơ hội:</strong> Giám sát tổng thể tiến độ các cơ hội của đội ngũ.</li>
                                        <li><strong>Hoạt động:</strong> Theo dõi lịch chăm sóc, cuộc gọi và phân công chéo.</li>
                                        <li><strong>Tìm kiếm & Excel:</strong> Kết quả truy vấn bao quát toàn bộ dữ liệu trong nhóm.</li>
                                    </ul>
                                </div>

                                <!-- Card thông tin ALL -->
                                <div class="scope-info-card info-all" id="infoCardAll">
                                    <div class="scope-info-header">
                                        <div class="scope-info-title-group">
                                            <div class="scope-info-icon-badge" aria-hidden="true"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></svg></div>
                                            <div class="scope-info-title">Toàn hệ thống (ALL)</div>
                                        </div>
                                        <span class="scope-info-tag">Toàn quyền</span>
                                    </div>
                                    <p class="scope-info-desc">Dành cho Ban Giám đốc  và Quản trị viên hệ thống .</p>
                                    <ul class="scope-info-list">
                                        <li><strong>Khách hàng:</strong> Toàn quyền xem và quản trị cơ sở dữ liệu khách toàn công ty.</li>
                                        <li><strong>Cơ hội:</strong> Bức tranh toàn cảnh quy trình bán hàng đa phòng ban.</li>
                                        <li><strong>Hoạt động & Báo giá:</strong> Kiểm toán, phê duyệt và giám sát không giới hạn.</li>
                                        <li><strong>Tìm kiếm & Excel:</strong> Xuất báo cáo tổng thể toàn bộ hệ thống.</li>
                                    </ul>
                                </div>
                            </div>
                        </section>





                        <section class="permission-card crm-card">
                            <div class="permission-card-header">
                                <div class="permission-card-title-group">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                        <line x1="3" y1="9" x2="21" y2="9"></line>
                                        <line x1="9" y1="21" x2="9" y2="9"></line>
                                    </svg>
                                    <div>
                                        <h2 style="font-size: 14.5px;">Ma trận phân quyền thực thể</h2>
                                        <div class="permission-card-subtitle">Áp dụng đồng bộ cho 4 nghiệp vụ chính & Tìm kiếm, Xuất Excel</div>
                                    </div>
                                </div>
                            </div>
                            <div class="permission-card-body" style="padding: 16px;">
                                <div class="permission-matrix-wrap">
                                    <table class="permission-matrix-table" aria-label="Bảng ma trận phân quyền dữ liệu">
                                        <thead>
                                            <tr>
                                                <th scope="col">Nghiệp vụ / Thực thể</th>
                                                <th scope="col">Cá nhân (SELF)</th>
                                                <th scope="col">Nhóm (TEAM)</th>
                                                <th scope="col">Tất cả (ALL)</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td><strong>Khách hàng</strong></td>
                                                <td><span class="permission-matrix-limit">Chỉ khách của mình</span></td>
                                                <td><span class="permission-matrix-check">Khách toàn nhóm</span></td>
                                                <td><span class="permission-matrix-check">Toàn bộ khách hàng</span></td>
                                            </tr>
                                            <tr>
                                                <td><strong>Cơ hội bán hàng</strong></td>
                                                <td><span class="permission-matrix-limit">Cơ hội cá nhân</span></td>
                                                <td><span class="permission-matrix-check">Cơ hội của nhóm</span></td>
                                                <td><span class="permission-matrix-check">Toàn bộ cơ hội</span></td>
                                            </tr>
                                            <tr>
                                                <td><strong>Hoạt động chăm sóc</strong></td>
                                                <td><span class="permission-matrix-limit">Tương tác của mình</span></td>
                                                <td><span class="permission-matrix-check">Tương tác nhóm</span></td>
                                                <td><span class="permission-matrix-check">Toàn bộ hoạt động</span></td>
                                            </tr>
                                            <tr>
                                                <td><strong>Báo giá hợp đồng</strong></td>
                                                <td><span class="permission-matrix-limit">Báo giá tự tạo</span></td>
                                                <td><span class="permission-matrix-check">Báo giá của nhóm</span></td>
                                                <td><span class="permission-matrix-check">Mọi báo giá</span></td>
                                            </tr>
                                            <tr>
                                                <td><strong>Tìm kiếm & Xuất Excel</strong></td>
                                                <td><span class="permission-matrix-limit">Tự động lọc SELF</span></td>
                                                <td><span class="permission-matrix-check">Tự động lọc TEAM</span></td>
                                                <td><span class="permission-matrix-check">Xuất không giới hạn</span></td>
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

    <!-- Script điều khiển Frontend tương tác JavaScript DOM Fetch API thuần -->
    <script>
    document.addEventListener('DOMContentLoaded', function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}';

        // State quản lý màn hình phân quyền
        var state = {
            users: [],
            teams: [],
            selectedUserId: null,
            selectedUser: null,
            originalRoleIds: [],
            originalDataScope: 'SELF',
            currentRoleIds: [],
            currentDataScope: 'SELF'
        };

        // DOM Elements - Thông báo
        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');
        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');
        var btnCloseErrorAlert = document.getElementById('btnCloseErrorAlert');
        var btnCloseSuccessAlert = document.getElementById('btnCloseSuccessAlert');

        // DOM Elements - Người dùng & Gán nhóm
        var userSelect = document.getElementById('userSelect');
        var userCardLoading = document.getElementById('userCardLoading');
        var userSummaryCard = document.getElementById('userSummaryCard');
        var userAvatarText = document.getElementById('userAvatarText');
        var userNameDisplay = document.getElementById('userNameDisplay');
        var userEmailDisplay = document.getElementById('userEmailDisplay');
        var userTeamDisplay = document.getElementById('userTeamDisplay');
        var activeRoleCountDisplay = document.getElementById('activeRoleCountDisplay');
        var userEmptyStateNotice = document.getElementById('userEmptyStateNotice');
        var selectedUserIdHidden = document.getElementById('selectedUserIdHidden');

        var currentTeamNameDisplay = document.getElementById('currentTeamNameDisplay');
        var teamSelect = document.getElementById('teamSelect');
        var btnAssignTeam = document.getElementById('btnAssignTeam');
        var btnAssignTeamSpinner = document.getElementById('btnAssignTeamSpinner');
        var btnAssignTeamText = document.getElementById('btnAssignTeamText');
        var teamLeadWarning = document.getElementById('teamLeadWarning');

        // DOM Elements - Vai trò & Phạm vi dữ liệu
        var btnSelectAllRoles = document.getElementById('btnSelectAllRoles');
        var btnDeselectAllRoles = document.getElementById('btnDeselectAllRoles');
        var roleCheckboxes = document.querySelectorAll('.permission-role-checkbox');

        var scopeRadioSelf = document.getElementById('scope_self');
        var scopeRadioTeam = document.getElementById('scope_team');
        var scopeRadioAll = document.getElementById('scope_all');
        var scopeCardSelf = document.getElementById('scopeCardSelf');
        var scopeCardTeam = document.getElementById('scopeCardTeam');
        var scopeCardAll = document.getElementById('scopeCardAll');

        // DOM Elements - Cột phải chỉ báo
        var activeScopeBadgeText = document.getElementById('activeScopeBadgeText');
        var infoCardSelf = document.getElementById('infoCardSelf');
        var infoCardTeam = document.getElementById('infoCardTeam');
        var infoCardAll = document.getElementById('infoCardAll');

        // DOM Elements - Action bar
        var permissionStatusDot = document.getElementById('permissionStatusDot');
        var permissionStatusText = document.getElementById('permissionStatusText');
        var btnResetPermissions = document.getElementById('btnResetPermissions');
        var btnSavePermissions = document.getElementById('btnSavePermissions');
        var btnSaveSpinner = document.getElementById('btnSaveSpinner');
        var btnSaveIcon = document.getElementById('btnSaveIcon');
        var btnSaveText = document.getElementById('btnSaveText');

        // 1. Tiện ích hiển thị thông báo
        var successAlertTimer = null;
        function showSuccess(msg) {
            clearTimeout(successAlertTimer);
            globalSuccessMessage.textContent = msg;
            globalSuccessAlert.style.display = 'flex';
            globalErrorAlert.style.display = 'none';
            successAlertTimer = setTimeout(function () {
                globalSuccessAlert.style.display = 'none';
            }, 5000);
        }

        function showError(msg) {
            clearTimeout(successAlertTimer);
            globalErrorMessage.textContent = msg;
            globalErrorAlert.style.display = 'flex';
            globalSuccessAlert.style.display = 'none';
        }

        function hideAlerts() {
            globalSuccessAlert.style.display = 'none';
            globalErrorAlert.style.display = 'none';
        }

        if (btnCloseErrorAlert) {
            btnCloseErrorAlert.addEventListener('click', function () {
                globalErrorAlert.style.display = 'none';
            });
        }
        if (btnCloseSuccessAlert) {
            btnCloseSuccessAlert.addEventListener('click', function () {
                globalSuccessAlert.style.display = 'none';
            });
        }

        // 2. Cập nhật giao diện chỉ báo phạm vi dữ liệu (Data Scope)
        function updateScopeUI(scopeValue) {
            var scope = (scopeValue || 'SELF').toUpperCase();
            state.currentDataScope = scope;

            // Xóa class selected trên các thẻ radio
            scopeCardSelf.classList.remove('selected');
            scopeCardTeam.classList.remove('selected');
            scopeCardAll.classList.remove('selected');

            // Xóa class active-scope trên các thẻ hướng dẫn cột phải
            infoCardSelf.classList.remove('active-scope');
            infoCardTeam.classList.remove('active-scope');
            infoCardAll.classList.remove('active-scope');

            if (scope === 'TEAM') {
                scopeRadioTeam.checked = true;
                scopeCardTeam.classList.add('selected');
                infoCardTeam.classList.add('active-scope');
                activeScopeBadgeText.textContent = 'Đang xem: TEAM';
            } else if (scope === 'ALL') {
                scopeRadioAll.checked = true;
                scopeCardAll.classList.add('selected');
                infoCardAll.classList.add('active-scope');
                activeScopeBadgeText.textContent = 'Đang xem: ALL';
            } else {
                scopeRadioSelf.checked = true;
                scopeCardSelf.classList.add('selected');
                infoCardSelf.classList.add('active-scope');
                activeScopeBadgeText.textContent = 'Đang xem: SELF';
            }
        }

        // 3. Cập nhật số lượng vai trò được tick chọn và cảnh báo Team Lead
        function updateRoleSelectionState() {
            var selectedIds = [];
            roleCheckboxes.forEach(function (cb) {
                var roleId = Number(cb.value);
                var itemLabel = document.getElementById('roleItem_' + roleId);
                if (cb.checked) {
                    selectedIds.push(roleId);
                    if (itemLabel) itemLabel.classList.add('checked');
                } else {
                    if (itemLabel) itemLabel.classList.remove('checked');
                }
            });

            state.currentRoleIds = selectedIds;
            activeRoleCountDisplay.textContent = selectedIds.length + ' vai trò';

            // Kiểm tra ràng buộc Team Lead (roleId = 4)
            var hasTeamLead = selectedIds.includes(4);
            var userHasTeam = state.selectedUser && state.selectedUser.teamId != null;

            if (hasTeamLead && !userHasTeam) {
                teamLeadWarning.style.display = 'flex';
                teamSelect.classList.add('warning-border');
            } else {
                teamLeadWarning.style.display = 'none';
                teamSelect.classList.remove('warning-border');
            }
        }

        // 4. Tải danh sách người dùng và nhóm từ API
        async function loadInitialData() {
            userCardLoading.classList.add('active');

            try {
                // Tải song song danh sách người dùng và nhóm kinh doanh
                var [usersRes, teamsRes] = await Promise.all([
                    fetch(contextPath + '/api/users?size=300', { headers: { 'Accept': 'application/json' } }),
                    fetch(contextPath + '/api/teams', { headers: { 'Accept': 'application/json' } })
                ]);

                // Xử lý nạp danh sách Teams
                if (teamsRes.ok) {
                    var teamsBody = await teamsRes.json();
                    var teamsData = Array.isArray(teamsBody) ? teamsBody : (teamsBody.data || []);
                    state.teams = teamsData;

                    teamSelect.innerHTML = '<option value="">-- Chọn nhóm kinh doanh --</option>';
                    teamsData.forEach(function (t) {
                        var opt = document.createElement('option');
                        opt.value = t.id || t.teamId;
                        opt.textContent = t.name || t.teamName || ('Nhóm #' + opt.value);
                        teamSelect.appendChild(opt);
                    });
                }

                // Xử lý nạp danh sách Users
                if (usersRes.ok) {
                    var usersBody = await usersRes.json();
                    var userItems = [];
                    if (usersBody && usersBody.data && Array.isArray(usersBody.data.items)) {
                        userItems = usersBody.data.items;
                    } else if (usersBody && Array.isArray(usersBody.data)) {
                        userItems = usersBody.data;
                    } else if (Array.isArray(usersBody)) {
                        userItems = usersBody;
                    }
                    state.users = userItems;

                    userSelect.innerHTML = '<option value="">-- Chọn người dùng cần phân quyền --</option>';
                    userItems.forEach(function (u) {
                        var opt = document.createElement('option');
                        opt.value = u.id;
                        var label = (u.fullName || u.name || 'Người dùng #' + u.id);
                        if (u.email) {
                            label += ' (' + u.email + ')';
                        }
                        opt.textContent = label;
                        userSelect.appendChild(opt);
                    });
                } else {
                    userSelect.innerHTML = '<option value="">-- Không thể tải danh sách người dùng --</option>';
                    showError('Không thể tải danh sách tài khoản người dùng từ máy chủ.');
                }

            } catch (err) {
                console.error('Lỗi khi nạp dữ liệu ban đầu:', err);
                userSelect.innerHTML = '<option value="">-- Lỗi kết nối máy chủ --</option>';
                showError('Lỗi kết nối máy chủ khi nạp danh sách tài khoản và nhóm kinh doanh.');
            } finally {
                userCardLoading.classList.remove('active');
            }

            // Kiểm tra tham số viewUserId từ query string trên URL
            var urlParams = new URLSearchParams(window.location.search);
            var initialUserId = urlParams.get('viewUserId');
            if (initialUserId) {
                userSelect.value = initialUserId;
                loadUserPermissions(initialUserId);
            }
        }

        // 5. Tải thông tin phân quyền chi tiết của người dùng được chọn
        async function loadUserPermissions(userId) {
            if (!userId) {
                resetFormToEmpty();
                return;
            }

            userCardLoading.classList.add('active');
            hideAlerts();

            // Tìm thông tin user trong state đã nạp
            var foundUser = state.users.find(function (u) {
                return String(u.id) === String(userId);
            });

            state.selectedUserId = userId;
            state.selectedUser = foundUser || null;
            selectedUserIdHidden.value = userId;

            // Cập nhật thẻ tóm tắt người dùng
            var fullName = foundUser ? (foundUser.fullName || foundUser.name || 'Chưa đặt tên') : ('Tài khoản #' + userId);
            var email = foundUser ? (foundUser.email || 'Chưa có email') : '';
            var teamName = (foundUser && (foundUser.teamName || foundUser.team)) ? (foundUser.teamName || foundUser.team) : 'Chưa phân nhóm';

            userNameDisplay.textContent = fullName;
            userEmailDisplay.textContent = email;
            userTeamDisplay.textContent = teamName;
            currentTeamNameDisplay.textContent = teamName;
            userAvatarText.textContent = fullName.trim().charAt(0).toUpperCase() || 'U';

            userSummaryCard.style.display = 'flex';
            userEmptyStateNotice.style.display = 'none';

            // Kích hoạt các nút hành động
            btnResetPermissions.disabled = false;
            btnSavePermissions.disabled = false;
            teamSelect.disabled = false;
            btnAssignTeam.disabled = false;

            if (foundUser && foundUser.teamId) {
                teamSelect.value = foundUser.teamId;
            } else {
                teamSelect.value = '';
            }

            // Gọi API lấy phân quyền: GET /api/permissions/users/{userId}
            try {
                var response = await fetch(contextPath + '/api/permissions/users/' + encodeURIComponent(userId), {
                    headers: { 'Accept': 'application/json' }
                });

                if (response.ok) {
                    var resBody = await response.json();
                    var permData = (resBody && resBody.data) ? resBody.data : {};

                    var roleIds = Array.isArray(permData.roles) ? permData.roles.map(Number) : [];
                    var dataScope = permData.dataScope || (foundUser && foundUser.dataScope) || 'SELF';

                    state.originalRoleIds = roleIds.slice();
                    state.originalDataScope = dataScope;

                    // Tick chọn checkbox tương ứng
                    roleCheckboxes.forEach(function (cb) {
                        cb.checked = roleIds.includes(Number(cb.value));
                    });

                    updateRoleSelectionState();
                    updateScopeUI(dataScope);

                    permissionStatusDot.classList.add('active');
                    permissionStatusText.textContent = 'Đã nạp quyền hạn: ' + fullName;

                } else if (response.status === 404) {
                    showError('Không tìm thấy thông tin phân quyền của người dùng #' + userId);
                } else if (response.status === 401) {
                    showError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                } else if (response.status === 403) {
                    showError('Bạn không có quyền quản lý phân quyền hệ thống.');
                } else {
                    showError('Không thể lấy phân quyền của tài khoản (Mã lỗi: ' + response.status + ').');
                }

            } catch (err) {
                console.error('Lỗi khi lấy phân quyền:', err);
                showError('Lỗi kết nối máy chủ khi lấy dữ liệu phân quyền.');
            } finally {
                userCardLoading.classList.remove('active');
            }
        }

        // 6. Reset form về trạng thái rỗng khi chưa chọn user
        function resetFormToEmpty() {
            state.selectedUserId = null;
            state.selectedUser = null;
            state.originalRoleIds = [];
            state.originalDataScope = 'SELF';
            state.currentRoleIds = [];
            state.currentDataScope = 'SELF';

            selectedUserIdHidden.value = '';
            userSummaryCard.style.display = 'none';
            userEmptyStateNotice.style.display = 'flex';
            currentTeamNameDisplay.textContent = 'Chưa phân nhóm';

            roleCheckboxes.forEach(function (cb) {
                cb.checked = false;
                var itemLabel = document.getElementById('roleItem_' + cb.value);
                if (itemLabel) itemLabel.classList.remove('checked');
            });

            updateScopeUI('SELF');
            teamLeadWarning.style.display = 'none';
            teamSelect.value = '';
            teamSelect.disabled = true;
            btnAssignTeam.disabled = true;
            btnResetPermissions.disabled = true;
            btnSavePermissions.disabled = true;

            permissionStatusDot.classList.remove('active');
            permissionStatusText.textContent = 'Chưa chọn người dùng';
        }

        // 7. Xử lý sự kiện khi đổi người dùng ở dropdown
        userSelect.addEventListener('change', function () {
            var userId = this.value;
            if (userId) {
                loadUserPermissions(userId);
            } else {
                resetFormToEmpty();
            }
        });

        // 8. Xử lý sự kiện khi tick/bỏ tick vai trò Roles
        roleCheckboxes.forEach(function (cb) {
            cb.addEventListener('change', function () {
                updateRoleSelectionState();
            });
        });

        btnSelectAllRoles.addEventListener('click', function () {
            if (!state.selectedUserId) return;
            roleCheckboxes.forEach(function (cb) {
                cb.checked = true;
            });
            updateRoleSelectionState();
        });

        btnDeselectAllRoles.addEventListener('click', function () {
            if (!state.selectedUserId) return;
            roleCheckboxes.forEach(function (cb) {
                cb.checked = false;
            });
            updateRoleSelectionState();
        });

        // 9. Xử lý sự kiện khi chọn các cấp độ Data Scope
        scopeRadioSelf.addEventListener('change', function () {
            if (this.checked) updateScopeUI('SELF');
        });
        scopeRadioTeam.addEventListener('change', function () {
            if (this.checked) updateScopeUI('TEAM');
        });
        scopeRadioAll.addEventListener('change', function () {
            if (this.checked) updateScopeUI('ALL');
        });

        scopeCardSelf.addEventListener('click', function () {
            updateScopeUI('SELF');
        });
        scopeCardTeam.addEventListener('click', function () {
            updateScopeUI('TEAM');
        });
        scopeCardAll.addEventListener('click', function () {
            updateScopeUI('ALL');
        });

        // 10. Xử lý nút Đặt lại (Reset)
        btnResetPermissions.addEventListener('click', function () {
            if (!state.selectedUserId) return;

            roleCheckboxes.forEach(function (cb) {
                cb.checked = state.originalRoleIds.includes(Number(cb.value));
            });
            updateRoleSelectionState();
            updateScopeUI(state.originalDataScope);
            hideAlerts();
            permissionStatusText.textContent = 'Đã khôi phục trạng thái ban đầu';
        });

        // 11. Xử lý Gán nhóm kinh doanh (CRM-29: POST /api/users/{userId}/team)
        btnAssignTeam.addEventListener('click', async function () {
            if (!state.selectedUserId) {
                showError('Vui lòng chọn người dùng trước khi gán nhóm kinh doanh.');
                return;
            }

            var teamId = teamSelect.value;
            if (!teamId) {
                showError('Vui lòng chọn một nhóm kinh doanh từ danh sách.');
                teamSelect.focus();
                return;
            }

            btnAssignTeam.disabled = true;
            btnAssignTeamSpinner.style.display = 'inline-block';
            btnAssignTeamText.textContent = 'Đang gán...';
            hideAlerts();

            try {
                var response = await fetch(contextPath + '/api/users/' + encodeURIComponent(state.selectedUserId) + '/team', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ teamId: Number(teamId) })
                });

                var resBody = await response.json();

                if (response.ok && resBody.success) {
                    var selectedTeamOption = teamSelect.options[teamSelect.selectedIndex];
                    var teamName = selectedTeamOption ? selectedTeamOption.textContent : ('Nhóm #' + teamId);

                    currentTeamNameDisplay.textContent = teamName;
                    userTeamDisplay.textContent = teamName;
                    if (state.selectedUser) {
                        state.selectedUser.teamId = Number(teamId);
                        state.selectedUser.teamName = teamName;
                    }

                    updateRoleSelectionState();
                    showSuccess('Gán người dùng vào ' + teamName + ' thành công.');

                } else {
                    var errMessage = resBody.message || 'Không thể gán nhóm kinh doanh.';
                    showError(errMessage);
                }

            } catch (err) {
                console.error('Lỗi khi gán nhóm:', err);
                showError('Lỗi kết nối máy chủ khi thực hiện gán nhóm kinh doanh.');
            } finally {
                btnAssignTeam.disabled = false;
                btnAssignTeamSpinner.style.display = 'none';
                btnAssignTeamText.textContent = 'Gán nhóm';
            }
        });

        // 12. Xử lý Submit Lưu phân quyền (POST /api/permissions/assign)
        btnSavePermissions.addEventListener('click', async function (e) {
            e.preventDefault();
            hideAlerts();

            if (!state.selectedUserId) {
                showError('Vui lòng chọn tài khoản người dùng cần phân quyền.');
                userSelect.focus();
                return;
            }

            // Client Validation: Ràng buộc Team Lead
            var selectedRoleIds = [];
            roleCheckboxes.forEach(function (cb) {
                if (cb.checked) selectedRoleIds.push(Number(cb.value));
            });

            var isTeamLead = selectedRoleIds.includes(4);
            var hasTeam = state.selectedUser && state.selectedUser.teamId != null;

            if (isTeamLead && !hasTeam) {
                showError('Vai trò Team Lead bắt buộc người dùng phải thuộc một nhóm kinh doanh. Vui lòng gán nhóm ở Bước 2 trước khi lưu.');
                teamLeadWarning.style.display = 'flex';
                teamSelect.scrollIntoView({ behavior: 'smooth', block: 'center' });
                return;
            }

            var selectedDataScope = state.currentDataScope || 'SELF';

            // Payload chuẩn API Contract CRM-25
            var payload = {
                userId: Number(state.selectedUserId),
                roleIds: selectedRoleIds,
                dataScope: selectedDataScope
            };

            // Trạng thái Loading
            btnSavePermissions.disabled = true;
            btnResetPermissions.disabled = true;
            btnSaveSpinner.style.display = 'inline-block';
            btnSaveIcon.style.display = 'none';
            btnSaveText.textContent = 'Đang lưu phân quyền...';
            permissionStatusText.textContent = 'Đang gửi dữ liệu phân quyền lên máy chủ...';

            try {
                var response = await fetch(contextPath + '/api/permissions/assign', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                var resBody = {};
                try {
                    resBody = await response.json();
                } catch (ignored) {}

                if (response.ok && resBody.success) {
                    state.originalRoleIds = selectedRoleIds.slice();
                    state.originalDataScope = selectedDataScope;

                    var targetName = state.selectedUser ? (state.selectedUser.fullName || state.selectedUser.name) : ('tài khoản #' + state.selectedUserId);
                    showSuccess('Lưu phân quyền và phạm vi dữ liệu thành công cho ' + targetName + '.');
                    permissionStatusText.textContent = 'Đã lưu phân quyền lúc ' + new Date().toLocaleTimeString('vi-VN');

                } else {
                    var msg = resBody.message;
                    if (!msg) {
                        if (response.status === 409) {
                            msg = 'Xung đột dữ liệu hoặc vi phạm quy tắc phân quyền (Team Lead cần có nhóm / Không thể tự gỡ vai trò Admin).';
                        } else if (response.status === 401) {
                            msg = 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.';
                        } else if (response.status === 403) {
                            msg = 'Bạn không có quyền quản lý phân quyền hệ thống.';
                        } else if (response.status === 400) {
                            msg = 'Dữ liệu phân quyền gửi lên không hợp lệ.';
                        } else if (response.status === 404) {
                            msg = 'Không tìm thấy người dùng mục tiêu.';
                        } else {
                            msg = 'Lỗi hệ thống khi lưu phân quyền (Mã lỗi: ' + response.status + ').';
                        }
                    }
                    showError(msg);
                    permissionStatusText.textContent = 'Lưu phân quyền thất bại';
                }

            } catch (err) {
                console.error('Lỗi khi lưu phân quyền:', err);
                showError('Lỗi kết nối máy chủ hoặc gián đoạn mạng khi lưu phân quyền.');
                permissionStatusText.textContent = 'Lỗi kết nối';
            } finally {
                btnSavePermissions.disabled = false;
                btnResetPermissions.disabled = false;
                btnSaveSpinner.style.display = 'none';
                btnSaveIcon.style.display = 'inline-block';
                btnSaveText.textContent = 'Lưu phân quyền';
            }
        });

        // Khởi chạy nạp dữ liệu ban đầu
        loadInitialData();
    });
    </script>
</body>
</html>
