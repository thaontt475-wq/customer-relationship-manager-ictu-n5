<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý người dùng - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Quản lý người dùng (CRM-28) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/users.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Quản trị người dùng -->
        <main class="user-page" id="userApp" role="main">
            <div class="user-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="user-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Quản lý người dùng</span>
                </nav>

                <!-- Header màn hình -->
                <header class="user-header">
                    <div class="user-header-info">
                        <h1>Quản lý người dùng & Tài khoản</h1>
                        <p>Quản lý danh sách nhân sự, tìm kiếm, phân quyền và trạng thái hoạt động của tài khoản trong hệ thống CRM.</p>
                    </div>
                    <div class="user-header-badges">
                        <span class="user-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                            </svg>
                            S1-08 / CRM-28
                        </span>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo phản hồi (Alerts / Banners) -->
                <div class="user-alerts" id="userAlertsArea" aria-live="polite">
                    <div class="user-alert user-alert-danger" id="globalErrorAlert" style="display: none;" role="alert">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title" id="globalErrorTitle">Đã xảy ra lỗi</div>
                            <div id="globalErrorMessage"></div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>

                    <div class="user-alert user-alert-success" id="globalSuccessAlert" style="display: none;" role="status">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title" id="globalSuccessTitle">Thành công</div>
                            <div id="globalSuccessMessage"></div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.style.display='none';" aria-label="Đóng">&times;</button>
                    </div>
                </div>

                <!-- Thẻ Card danh sách người dùng -->
                <section class="user-card" style="position: relative;" aria-labelledby="userCardTitle">

                    <!-- Loading Overlay -->
                    <div class="user-loading-overlay" id="userTableLoading" aria-hidden="true">
                        <div class="user-spinner"></div>
                    </div>

                    <!-- Header của Card: Tiêu đề + Thống kê sơ bộ -->
                    <div class="user-card-header">
                        <div>
                            <h2 id="userCardTitle" class="user-card-title">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                    <circle cx="9" cy="7" r="4"></circle>
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                </svg>
                                Danh sách tài khoản người dùng
                            </h2>
                            <p class="user-card-subtitle">Hiển thị thông tin định danh, vai trò, nhóm kinh doanh và trạng thái tài khoản</p>
                        </div>
                    </div>

                    <!-- Thanh công cụ tìm kiếm và bộ lọc (Toolbar & Filters) -->
                    <div class="user-toolbar">
                        <div class="user-toolbar-left">
                            <!-- Ô tìm kiếm từ khóa -->
                            <div class="user-search-wrap">
                                <svg class="user-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="search" id="userSearchInput" class="user-search-input"
                                       placeholder="Tìm kiếm theo họ tên, email..."
                                       aria-label="Tìm kiếm người dùng">
                            </div>

                            <!-- Bộ lọc trạng thái -->
                            <select id="userStatusFilter" class="user-filter-select" aria-label="Lọc theo trạng thái">
                                <option value="">Tất cả trạng thái</option>
                                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                                <option value="INACTIVE">Đã khóa (INACTIVE)</option>
                            </select>

                            <!-- Bộ lọc vai trò -->
                            <select id="userRoleFilter" class="user-filter-select" aria-label="Lọc theo vai trò">
                                <option value="">Tất cả vai trò</option>
                                <option value="Admin">Admin (Quản trị viên)</option>
                                <option value="Sales Rep">Sales Rep (Kinh doanh)</option>
                                <option value="Manager">Manager (Trưởng nhóm)</option>
                                <option value="Accountant">Accountant (Kế toán)</option>
                            </select>

                            <!-- Nút đặt lại bộ lọc -->
                            <button type="button" class="btn btn-secondary" id="btnResetFilter" title="Xóa toàn bộ bộ lọc và từ khóa">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="1 4 1 10 7 10"></polyline>
                                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                </svg>
                                Đặt lại
                            </button>
                        </div>

                        <div class="user-toolbar-right">
                            <!-- Nút Thêm mới người dùng -->
                            <button type="button" class="btn btn-create-user" id="btnOpenCreateModal">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                <span>Thêm người dùng</span>
                            </button>
                        </div>
                    </div>

                    <!-- Bảng dữ liệu người dùng (Table Responsive) -->
                    <div class="user-table-responsive">
                        <table class="user-table" id="userTable" aria-label="Bảng dữ liệu người dùng">
                            <thead>
                                <tr>
                                    <th scope="col" class="table-col-id">ID</th>
                                    <th scope="col">Người dùng</th>
                                    <th scope="col">Vai trò</th>
                                    <th scope="col">Nhóm kinh doanh</th>
                                    <th scope="col">Ngày tạo</th>
                                    <th scope="col" class="table-col-status">Trạng thái</th>
                                    <th scope="col" class="table-col-actions">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="userTableBody">
                                <!-- Dữ liệu sẽ được render động từ Fetch API qua JS -->
                            </tbody>
                        </table>
                    </div>

                    <!-- Empty State khi không có dữ liệu -->
                    <div class="user-empty-state" id="userEmptyState" style="display: none;">
                        <div class="user-empty-icon" aria-hidden="true">👥</div>
                        <div class="user-empty-text">Không tìm thấy người dùng nào</div>
                        <p class="table-empty-desc">Không có kết quả nào phù hợp với bộ lọc hiện tại. Hãy thử thay đổi từ khóa hoặc đặt lại bộ lọc.</p>
                        <button type="button" class="btn btn-secondary" style="margin-top: 14px;" onclick="document.getElementById('btnResetFilter').click()">
                            Đặt lại bộ lọc
                        </button>
                    </div>

                    <!-- Thanh phân trang động (Pagination) -->
                    <div class="user-pagination" id="userPagination" style="display: none;">
                        <div class="pagination-info" id="paginationInfo">
                            Hiển thị 0 trên tổng số 0 người dùng
                        </div>
                        <div class="pagination-controls" id="paginationControls">
                            <!-- Nút trang sẽ được render bằng JS -->
                        </div>
                    </div>

                </section>

            </div>
        </main>
    </div>

    <!-- Modal Thêm mới & Chỉnh sửa Người dùng (Create / Edit User Modal) -->
    <div class="user-modal-overlay" id="userFormModal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
        <div class="user-modal-card">
            <header class="user-modal-header">
                <h3 class="user-modal-title" id="modalTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="8.5" cy="7.5" r="4"></circle>
                        <line x1="20" y1="8" x2="20" y2="14"></line>
                        <line x1="23" y1="11" x2="17" y2="11"></line>
                    </svg>
                    <span id="modalTitleText">Thêm người dùng mới</span>
                </h3>
                <button type="button" class="user-modal-close-btn" id="btnCloseModal" aria-label="Đóng cửa sổ">&times;</button>
            </header>

            <form id="userForm" novalidate>
                <input type="hidden" id="formUserId" name="id">

                <div class="user-modal-body">
                    <div class="modal-field">
                        <div style="padding:12px 14px;border-radius:8px;background:#f8fafc;color:#475569;font-size:0.9rem;">
                            Khi tạo tài khoản, hệ thống tự sinh mật khẩu tạm và gửi thông tin kích hoạt tới email người dùng.
                        </div>
                    </div>
                    <!-- Trường Họ và tên -->
                    <div class="modal-field">
                        <label for="formFullName" class="modal-label">
                            Họ và tên <span class="modal-required">*</span>
                        </label>
                        <input type="text" id="formFullName" name="fullName" class="modal-input"
                               placeholder="Ví dụ: Nguyễn Văn A" required>
                        <div class="modal-field-feedback" id="feedbackFullName"></div>
                    </div>

                    <!-- Trường Email -->
                    <div class="modal-field">
                        <label for="formEmail" class="modal-label">
                            Địa chỉ Email <span class="modal-required">*</span>
                        </label>
                        <input type="email" id="formEmail" name="email" class="modal-input"
                               placeholder="Ví dụ: vana@example.com" required>
                        <div class="modal-field-feedback" id="feedbackEmail"></div>
                    </div>

                    <!-- Trường Mật khẩu (khi tạo mới bắt buộc, khi sửa là tùy chọn) -->
                    <div class="modal-field" id="formPasswordGroup">
                        <label for="formPassword" class="modal-label">
                            <span id="labelPasswordText">Mật khẩu ban đầu</span>
                            <span class="modal-required" id="markPasswordRequired">*</span>
                        </label>
                        <input type="password" id="formPassword" name="password" class="modal-input"
                               placeholder="Tối thiểu 8 ký tự, gồm cả chữ và số" minlength="8">
                        <div class="modal-field-feedback" id="feedbackPassword"></div>
                    </div>

                    <!-- Checkbox gửi email kích hoạt -->
                    <div class="modal-field" id="formSendMailGroup">
                        <label class="modal-checkbox-label">
                            <input type="checkbox" id="formSendActivation" name="sendActivation" checked>
                            <span>Gửi thông tin tài khoản và liên kết kích hoạt qua email</span>
                        </label>
                    </div>

                    <!-- Trường Trạng thái tài khoản -->
                    <div class="modal-field">
                        <label for="formStatus" class="modal-label">
                            Trạng thái hoạt động <span class="modal-required">*</span>
                        </label>
                        <select id="formStatus" name="status" class="modal-select">
                            <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                            <option value="INACTIVE">Khóa tạm thời (INACTIVE)</option>
                        </select>
                    </div>
                </div>

                <footer class="user-modal-footer">
                    <button type="button" class="btn btn-secondary" id="btnCancelModal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveUser">
                        <span id="saveUserSpinner" class="user-spinner" style="width: 14px; height: 14px; display: none; margin-right: 4px;" aria-hidden="true"></span>
                        <span id="saveUserBtnText">Lưu người dùng</span>
                    </button>
                </footer>
            </form>
        </div>
    </div>

    <!-- Modal Xác nhận Xóa Người dùng (Confirm Delete Modal) -->
    <div class="user-modal-overlay" id="deleteConfirmModal" role="dialog" aria-modal="true" aria-labelledby="confirmDeleteTitle">
        <div class="user-modal-card" style="max-width: 440px;">
            <div class="user-modal-body confirm-modal-content">
                <div class="confirm-modal-icon-wrap" aria-hidden="true">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        <line x1="10" y1="11" x2="10" y2="17"></line>
                        <line x1="14" y1="11" x2="14" y2="17"></line>
                    </svg>
                </div>
                <h3 class="confirm-modal-title" id="confirmDeleteTitle">Xác nhận xóa tài khoản</h3>
                <p class="confirm-modal-desc">
                    Bạn có chắc chắn muốn xóa người dùng <span class="confirm-user-highlight" id="deleteUserTargetName"></span> không?
                    Thao tác này sẽ thu hồi toàn bộ quyền truy cập và không thể hoàn tác.
                </p>
            </div>
            <footer class="user-modal-footer" style="justify-content: center;">
                <button type="button" class="btn btn-secondary" id="btnCancelDelete">Hủy bỏ</button>
                <button type="button" class="btn btn-danger" id="btnConfirmDelete">
                    <span id="deleteUserSpinner" class="user-spinner" style="width: 14px; height: 14px; display: none; margin-right: 4px;" aria-hidden="true"></span>
                    <span id="deleteUserBtnText">Xác nhận xóa</span>
                </button>
            </footer>
        </div>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <!-- Script tương tác Frontend & Gọi API Contract CRM-28 -->
    <script>
    document.addEventListener('DOMContentLoaded', function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}';

        // State quản lý danh sách & phân trang
        var state = {
            page: 1,
            size: 20,
            keyword: '',
            status: '',
            role: '',
            totalPages: 1,
            totalItems: 0,
            items: []
        };

        // DOM Elements
        var tableBody = document.getElementById('userTableBody');
        var loadingOverlay = document.getElementById('userTableLoading');
        var emptyState = document.getElementById('userEmptyState');
        var paginationEl = document.getElementById('userPagination');
        var paginationInfo = document.getElementById('paginationInfo');
        var paginationControls = document.getElementById('paginationControls');

        var searchInput = document.getElementById('userSearchInput');
        var statusFilter = document.getElementById('userStatusFilter');
        var roleFilter = document.getElementById('userRoleFilter');
        var resetFilterBtn = document.getElementById('btnResetFilter');
        var openCreateModalBtn = document.getElementById('btnOpenCreateModal');

        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');
        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');

        // Modal Form Elements
        var userFormModal = document.getElementById('userFormModal');
        var userForm = document.getElementById('userForm');
        var modalTitleText = document.getElementById('modalTitleText');
        var formUserId = document.getElementById('formUserId');
        var formFullName = document.getElementById('formFullName');
        var formEmail = document.getElementById('formEmail');
        var formPassword = document.getElementById('formPassword');
        var formPasswordGroup = document.getElementById('formPasswordGroup');
        var formSendMailGroup = document.getElementById('formSendMailGroup');
        var labelPasswordText = document.getElementById('labelPasswordText');
        var markPasswordRequired = document.getElementById('markPasswordRequired');
        var formStatus = document.getElementById('formStatus');
        var btnSaveUser = document.getElementById('btnSaveUser');
        var saveUserSpinner = document.getElementById('saveUserSpinner');
        var saveUserBtnText = document.getElementById('saveUserBtnText');
        var btnCloseModal = document.getElementById('btnCloseModal');
        var btnCancelModal = document.getElementById('btnCancelModal');

        var feedbackFullName = document.getElementById('feedbackFullName');
        var feedbackEmail = document.getElementById('feedbackEmail');
        var feedbackPassword = document.getElementById('feedbackPassword');

        // Delete Modal Elements
        var deleteConfirmModal = document.getElementById('deleteConfirmModal');
        var deleteUserTargetName = document.getElementById('deleteUserTargetName');
        var btnConfirmDelete = document.getElementById('btnConfirmDelete');
        var btnCancelDelete = document.getElementById('btnCancelDelete');
        var deleteUserSpinner = document.getElementById('deleteUserSpinner');
        var deleteUserBtnText = document.getElementById('deleteUserBtnText');
        var userToDeleteId = null;

        // Escape HTML helper
        function escapeHtml(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        // Thông báo
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

        // 1. Tải danh sách người dùng (GET /api/users)
        async function fetchUsers() {
            loadingOverlay.style.display = 'flex';
            hideAlerts();

            var queryParams = new URLSearchParams({
                keyword: state.keyword,
                status: state.status,
                role: state.role,
                page: state.page,
                size: state.size
            });

            var endpoint = contextPath + '/api/users?' + queryParams.toString();

            try {
                var response = await fetch(endpoint, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json'
                    }
                });

                loadingOverlay.style.display = 'none';

                if (!response.ok) {
                    if (response.status === 404) {
                        showErrorAlert('Endpoint /api/users chưa sẵn sàng trên máy chủ backend.');
                    } else if (response.status === 401) {
                        showErrorAlert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    } else if (response.status === 403) {
                        showErrorAlert('Bạn không có quyền truy cập vào danh sách quản lý người dùng.');
                    } else {
                        showErrorAlert('Không thể tải danh sách người dùng (Mã lỗi: ' + response.status + ').');
                    }
                    renderEmpty();
                    return;
                }

                var responseBody = await response.json();
                var data = responseBody && responseBody.data ? responseBody.data : {};
                var items = Array.isArray(data.items) ? data.items : [];

                state.items = items;
                state.page = Number(data.page) || state.page;
                state.size = Number(data.size) || state.size;
                state.totalItems = Number(data.totalItems) || 0;
                state.totalPages = Number(data.totalPages) || 0;
                if (items.length === 0) {
                    renderEmpty();
                } else {
                    renderTable(items);
                    renderPagination();
                }

            } catch (err) {
                loadingOverlay.style.display = 'none';
                console.error('Lỗi khi tải danh sách người dùng:', err);
                showErrorAlert('Không thể kết nối đến máy chủ backend hoặc mạng bị gián đoạn.');
                renderEmpty();
            }
        }

        // Render Table Rows
        function renderTable(items) {
            emptyState.style.display = 'none';
            tableBody.innerHTML = '';

            items.forEach(function (user) {
                var uid = user.id != null ? user.id : '';
                var fullName = user.fullName || user.name || 'Chưa đặt tên';
                var email = user.email || 'Chưa có email';
                var status = (user.status || 'ACTIVE').toUpperCase();
                var isLocked = (status === 'INACTIVE' || status === 'LOCKED');
                var teamName = user.teamName || user.team || 'Chưa phân nhóm';
                var createdAt = user.createdAt ? user.createdAt.substring(0, 10) : '-';

                // Xử lý vai trò (roles có thể là mảng hoặc chuỗi)
                var rolesText = 'Chưa phân vai trò';
                if (Array.isArray(user.roles) && user.roles.length > 0) {
                    rolesText = user.roles.join(', ');
                } else if (typeof user.role === 'string' && user.role.trim() !== '') {
                    rolesText = user.role;
                }

                var avatarChar = fullName.trim().charAt(0).toUpperCase() || 'U';

                var row = document.createElement('tr');
                row.innerHTML =
                    '<td><strong>#' + escapeHtml(uid) + '</strong></td>' +
                    '<td>' +
                        '<div class="user-info-cell">' +
                            '<div class="user-avatar-sm' + (isLocked ? ' user-avatar-sm--locked' : '') + '">' +
                                escapeHtml(avatarChar) +
                            '</div>' +
                            '<div>' +
                                '<div class="user-name-text">' + escapeHtml(fullName) + '</div>' +
                                '<div class="user-email-text">' + escapeHtml(email) + '</div>' +
                            '</div>' +
                        '</div>' +
                    '</td>' +
                    '<td><span class="role-badge">' + escapeHtml(rolesText) + '</span></td>' +
                    '<td>' + escapeHtml(teamName) + '</td>' +
                    '<td><span style="font-size: 0.85rem; color: #64748b;">' + escapeHtml(createdAt) + '</span></td>' +
                    '<td>' +
                        (isLocked ?
                            '<span class="status-badge status-badge--locked"><span class="status-dot"></span>Đã khóa</span>' :
                            '<span class="status-badge status-badge--active"><span class="status-dot"></span>Hoạt động</span>') +
                    '</td>' +
                    '<td class="table-col-actions">' +
                        '<div class="user-actions-group">' +
                            '<button type="button" class="btn btn-sm btn-secondary btn-edit-user" data-id="' + escapeHtml(uid) + '" title="Chỉnh sửa thông tin">' +
                                '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>' +
                                    '<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>' +
                                '</svg>' +
                                'Sửa' +
                            '</button>' +
                            '<button type="button" class="btn btn-sm btn-outline-danger btn-delete-user" data-id="' + escapeHtml(uid) + '" data-name="' + escapeHtml(fullName) + '" title="Xóa tài khoản người dùng">' +
                                '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<polyline points="3 6 5 6 21 6"></polyline>' +
                                    '<path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>' +
                                '</svg>' +
                                'Xóa' +
                            '</button>' +
                        '</div>' +
                    '</td>';

                tableBody.appendChild(row);
            });

            // Gán sự kiện cho các nút hành động Sửa & Xóa
            document.querySelectorAll('.btn-edit-user').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var uid = this.getAttribute('data-id');
                    openEditModal(uid);
                });
            });

            document.querySelectorAll('.btn-delete-user').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var uid = this.getAttribute('data-id');
                    var uname = this.getAttribute('data-name');
                    openDeleteModal(uid, uname);
                });
            });
        }

        // Render Empty State
        function renderEmpty() {
            tableBody.innerHTML = '';
            emptyState.style.display = 'block';
            paginationEl.style.display = 'none';
        }

        // Render Pagination UI
        function renderPagination() {
            if (state.totalItems <= state.size) {
                paginationEl.style.display = 'none';
                return;
            }

            paginationEl.style.display = 'flex';
            var startIdx = ((state.page - 1) * state.size) + 1;
            var endIdx = Math.min(state.page * state.size, state.totalItems);
            paginationInfo.textContent = 'Hiển thị ' + startIdx + ' - ' + endIdx + ' trên tổng số ' + state.totalItems + ' người dùng';

            paginationControls.innerHTML = '';

            // Nút Previous
            var prevBtn = document.createElement('button');
            prevBtn.type = 'button';
            prevBtn.className = 'pagination-btn';
            prevBtn.innerHTML = '&laquo;';
            prevBtn.disabled = state.page <= 1;
            prevBtn.title = 'Trang trước';
            prevBtn.addEventListener('click', function () {
                if (state.page > 1) {
                    state.page--;
                    fetchUsers();
                }
            });
            paginationControls.appendChild(prevBtn);

            // Nút các trang số
            var maxDisplay = 5;
            var startPage = Math.max(1, state.page - Math.floor(maxDisplay / 2));
            var endPage = Math.min(state.totalPages, startPage + maxDisplay - 1);
            if (endPage - startPage + 1 < maxDisplay) {
                startPage = Math.max(1, endPage - maxDisplay + 1);
            }

            for (var p = startPage; p <= endPage; p++) {
                (function (pageNumber) {
                    var pageBtn = document.createElement('button');
                    pageBtn.type = 'button';
                    pageBtn.className = 'pagination-btn' + (pageNumber === state.page ? ' is-active' : '');
                    pageBtn.textContent = pageNumber;
                    pageBtn.addEventListener('click', function () {
                        if (pageNumber !== state.page) {
                            state.page = pageNumber;
                            fetchUsers();
                        }
                    });
                    paginationControls.appendChild(pageBtn);
                })(p);
            }

            // Nút Next
            var nextBtn = document.createElement('button');
            nextBtn.type = 'button';
            nextBtn.className = 'pagination-btn';
            nextBtn.innerHTML = '&raquo;';
            nextBtn.disabled = state.page >= state.totalPages;
            nextBtn.title = 'Trang sau';
            nextBtn.addEventListener('click', function () {
                if (state.page < state.totalPages) {
                    state.page++;
                    fetchUsers();
                }
            });
            paginationControls.appendChild(nextBtn);
        }

        // 2. Tìm kiếm và Lọc
        var searchDebounceTimer = null;
        searchInput.addEventListener('input', function () {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(function () {
                state.keyword = searchInput.value.trim();
                state.page = 1;
                fetchUsers();
            }, 350);
        });

        statusFilter.addEventListener('change', function () {
            state.status = statusFilter.value;
            state.page = 1;
            fetchUsers();
        });

        roleFilter.addEventListener('change', function () {
            state.role = roleFilter.value;
            state.page = 1;
            fetchUsers();
        });

        resetFilterBtn.addEventListener('click', function () {
            searchInput.value = '';
            statusFilter.value = '';
            roleFilter.value = '';
            state.keyword = '';
            state.status = '';
            state.role = '';
            state.page = 1;
            fetchUsers();
        });

        // 3. Modal Thêm mới / Chỉnh sửa Người dùng
        function clearModalFeedback() {
            feedbackFullName.textContent = '';
            feedbackEmail.textContent = '';
            feedbackPassword.textContent = '';
            formFullName.classList.remove('is-invalid');
            formEmail.classList.remove('is-invalid');
            formPassword.classList.remove('is-invalid');
        }

        function openCreateModal() {
            clearModalFeedback();
            userForm.reset();
            formUserId.value = '';
            modalTitleText.textContent = 'Thêm người dùng mới';
            saveUserBtnText.textContent = 'Lưu người dùng';
            formPasswordGroup.style.display = 'none';
            formSendMailGroup.style.display = 'none';
            formStatus.closest('.modal-field').style.display = 'none';
            formPassword.required = false;
            userFormModal.classList.add('is-open');
            formFullName.focus();
        }

        function openEditModal(uid) {
            clearModalFeedback();
            userForm.reset();
            formUserId.value = uid;
            modalTitleText.textContent = 'Chỉnh sửa thông tin người dùng';
            saveUserBtnText.textContent = 'Cập nhật';

            // Tìm thông tin user trong state hiện tại
            var user = state.items.find(function (item) {
                return String(item.id) === String(uid);
            });

            if (user) {
                formFullName.value = user.fullName || user.name || '';
                formEmail.value = user.email || '';
            }

            formPassword.required = false;
            formPasswordGroup.style.display = 'none';
            formSendMailGroup.style.display = 'none';
            formStatus.closest('.modal-field').style.display = 'none';

            userFormModal.classList.add('is-open');
            formFullName.focus();
        }

        function closeUserModal() {
            userFormModal.classList.remove('is-open');
            clearModalFeedback();
        }

        openCreateModalBtn.addEventListener('click', openCreateModal);
        btnCloseModal.addEventListener('click', closeUserModal);
        btnCancelModal.addEventListener('click', closeUserModal);

        userFormModal.addEventListener('click', function (e) {
            if (e.target === userFormModal) {
                closeUserModal();
            }
        });

        // Submit Form Tạo mới / Cập nhật người dùng (POST / PUT /api/users)
        userForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            clearModalFeedback();

            var id = formUserId.value.trim();
            var isEdit = Boolean(id);
            var fullName = formFullName.value.trim();
            var email = formEmail.value.trim();
            var isValid = true;

            // Client Validation
            if (!fullName) {
                formFullName.classList.add('is-invalid');
                feedbackFullName.textContent = 'Vui lòng nhập họ và tên.';
                isValid = false;
            }

            var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!email) {
                formEmail.classList.add('is-invalid');
                feedbackEmail.textContent = 'Vui lòng nhập địa chỉ email.';
                isValid = false;
            } else if (!emailRegex.test(email)) {
                formEmail.classList.add('is-invalid');
                feedbackEmail.textContent = 'Địa chỉ email không đúng định dạng.';
                isValid = false;
            }

            if (!isValid) return;

            // Chuẩn bị payload gửi lên API Contract CRM-28
            var payload = {
                fullName: fullName,
                email: email
            };
            // Trạng thái Loading của nút Save
            btnSaveUser.disabled = true;
            btnCancelModal.disabled = true;
            saveUserSpinner.style.display = 'inline-block';
            saveUserBtnText.textContent = isEdit ? 'Đang cập nhật...' : 'Đang lưu...';

            var url = isEdit ? (contextPath + '/api/users/' + encodeURIComponent(id)) : (contextPath + '/api/users');
            var method = isEdit ? 'PUT' : 'POST';

            try {
                var response = await fetch(url, {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                btnSaveUser.disabled = false;
                btnCancelModal.disabled = false;
                saveUserSpinner.style.display = 'none';
                saveUserBtnText.textContent = isEdit ? 'Cập nhật' : 'Lưu người dùng';

                var resData = null;
                var contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    resData = await response.json();
                }

                if (response.ok && (!resData || resData.success !== false)) {
                    closeUserModal();
                    showSuccessAlert(isEdit ? 'Cập nhật thông tin người dùng thành công.' : 'Tạo mới tài khoản người dùng thành công.');
                    fetchUsers();
                } else {
                    var errorMsg = (resData && resData.message) ? resData.message : 'Không thể lưu người dùng.';

                    // Kiểm tra lỗi Email trùng lặp (HTTP 400 hoặc 409)
                    if (response.status === 409 || errorMsg.toLowerCase().includes('email')) {
                        formEmail.classList.add('is-invalid');
                        feedbackEmail.textContent = errorMsg || 'Địa chỉ email này đã tồn tại trên hệ thống.';
                    } else {
                        showErrorAlert(errorMsg + ' (Mã lỗi: ' + response.status + ')');
                    }
                }

            } catch (err) {
                btnSaveUser.disabled = false;
                btnCancelModal.disabled = false;
                saveUserSpinner.style.display = 'none';
                saveUserBtnText.textContent = isEdit ? 'Cập nhật' : 'Lưu người dùng';
                console.error('Lỗi khi gửi form người dùng:', err);
                showErrorAlert('Không thể kết nối đến máy chủ backend để lưu người dùng.');
            }
        });

        // 4. Modal Xác nhận Xóa người dùng (DELETE /api/users/{id})
        function openDeleteModal(uid, uname) {
            userToDeleteId = uid;
            deleteUserTargetName.textContent = uname || ('#' + uid);
            deleteConfirmModal.classList.add('is-open');
        }

        function closeDeleteModal() {
            deleteConfirmModal.classList.remove('is-open');
            userToDeleteId = null;
        }

        btnCancelDelete.addEventListener('click', closeDeleteModal);
        deleteConfirmModal.addEventListener('click', function (e) {
            if (e.target === deleteConfirmModal) {
                closeDeleteModal();
            }
        });

        btnConfirmDelete.addEventListener('click', async function () {
            if (!userToDeleteId) return;

            btnConfirmDelete.disabled = true;
            btnCancelDelete.disabled = true;
            deleteUserSpinner.style.display = 'inline-block';
            deleteUserBtnText.textContent = 'Đang xóa...';

            var deleteEndpoint = contextPath + '/api/users/' + encodeURIComponent(userToDeleteId);

            try {
                var response = await fetch(deleteEndpoint, {
                    method: 'DELETE',
                    headers: {
                        'Accept': 'application/json'
                    }
                });

                btnConfirmDelete.disabled = false;
                btnCancelDelete.disabled = false;
                deleteUserSpinner.style.display = 'none';
                deleteUserBtnText.textContent = 'Xác nhận xóa';

                var resData = null;
                var contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    resData = await response.json();
                }

                if (response.ok && (!resData || resData.success !== false)) {
                    closeDeleteModal();
                    showSuccessAlert('Đã xóa tài khoản người dùng thành công.');
                    fetchUsers();
                } else {
                    var errorMsg = (resData && resData.message) ? resData.message : 'Không thể xóa người dùng.';
                    closeDeleteModal();
                    showErrorAlert(errorMsg + ' (Mã lỗi: ' + response.status + ')');
                }

            } catch (err) {
                btnConfirmDelete.disabled = false;
                btnCancelDelete.disabled = false;
                deleteUserSpinner.style.display = 'none';
                deleteUserBtnText.textContent = 'Xác nhận xóa';
                closeDeleteModal();
                console.error('Lỗi khi xóa người dùng:', err);
                showErrorAlert('Không thể kết nối đến máy chủ backend để xóa người dùng.');
            }
        });

        // Tự động tải danh sách người dùng ban đầu khi trang sẵn sàng
        fetchUsers();

    });
    </script>
</body>
</html>