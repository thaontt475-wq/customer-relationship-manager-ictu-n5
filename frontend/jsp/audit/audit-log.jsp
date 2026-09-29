<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhật ký thay đổi dữ liệu nhạy cảm - CRM ICTU</title>

    <!-- CSS dùng chung -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS module Audit -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/audit/audit.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Nội dung chính màn hình Audit Log -->
        <main class="audit-page" id="auditApp" role="main">
            <div class="audit-container">

                <!-- Breadcrumb -->
                <nav class="audit-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Nhật ký dữ liệu nhạy cảm</span>
                </nav>

                <!-- Header màn hình -->
                <header class="audit-header">
                    <div class="audit-header-info">
                        <h1>Nhật ký thay đổi dữ liệu nhạy cảm (Audit Log)</h1>
                        <p>Theo dõi và truy vết lịch sử thay đổi chiết khấu đơn hàng, chỉ tiêu doanh số, quyền sở hữu dữ liệu và phân quyền vai trò.</p>
                    </div>
                    <div class="audit-header-badges">
                        <span class="audit-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            </svg>
                            S2-04 / CRM-37
                        </span>
                    </div>
                </header>

                <!-- Thẻ danh sách Audit Log -->
                <section class="audit-card" aria-labelledby="auditCardTitle">
                    <h2 id="auditCardTitle" style="font-size: 1.15rem; font-weight: 700; color: #0f172a; margin: 0 0 16px 0;">
                        Lịch sử thay đổi hệ thống
                    </h2>

                    <!-- Bộ lọc đa tiêu chí theo Acceptance Criteria -->
                    <form id="auditFilterForm" class="audit-filter-bar" onsubmit="event.preventDefault(); applyFilter();">
                        <!-- 1. Lọc theo người thực hiện -->
                        <div class="audit-filter-item">
                            <label class="audit-filter-label" for="filterActor">Người thực hiện</label>
                            <input type="text" id="filterActor" class="audit-input" placeholder="Ví dụ: Admin, Nguyễn Văn A">
                        </div>

                        <!-- 2. Lọc theo loại đối tượng nhạy cảm -->
                        <div class="audit-filter-item">
                            <label class="audit-filter-label" for="filterEntityType">Loại đối tượng</label>
                            <select id="filterEntityType" class="audit-select">
                                <option value="ALL">Tất cả loại đối tượng</option>
                                <option value="DISCOUNT">Chiết khấu (DISCOUNT)</option>
                                <option value="SALES_TARGET">Chỉ tiêu doanh số (SALES_TARGET)</option>
                                <option value="DATA_OWNERSHIP">Quyền sở hữu dữ liệu (DATA_OWNERSHIP)</option>
                                <option value="USER_ROLE">Vai trò người dùng (USER_ROLE)</option>
                            </select>
                        </div>

                        <!-- 3. Lọc theo ngày bắt đầu -->
                        <div class="audit-filter-item">
                            <label class="audit-filter-label" for="filterFromDate">Từ ngày</label>
                            <input type="date" id="filterFromDate" class="audit-input">
                        </div>

                        <!-- 4. Lọc theo ngày kết thúc -->
                        <div class="audit-filter-item">
                            <label class="audit-filter-label" for="filterToDate">Đến ngày</label>
                            <input type="date" id="filterToDate" class="audit-input">
                        </div>

                        <!-- Nút Lọc và Đặt lại -->
                        <div style="display: flex; gap: 8px;">
                            <button type="submit" class="btn btn-primary" id="btnFilter" style="height: 38px;">
                                Lọc
                            </button>
                            <button type="button" class="btn btn-secondary" id="btnResetFilter" style="height: 38px;">
                                Đặt lại
                            </button>
                        </div>
                    </form>

                    <div id="auditFilterError" class="audit-filter-error" style="display: none;" role="alert"></div>

                    <!-- Loading Indicator -->
                    <div id="auditLoading" style="display: none; text-align: center; padding: 24px;">
                        <span class="user-spinner" style="display: inline-block; width: 24px; height: 24px;"></span>
                        <div style="margin-top: 8px; color: #64748b; font-size: 0.88rem;">Đang tải nhật ký...</div>
                    </div>

                    <!-- BE CONTRACT NEEDED: /api/audit-logs -->
                    <div id="auditErrorState" class="audit-error-state" style="display: none;" role="alert">
                        <strong>Không thể tải Audit Log</strong>
                        <span id="auditErrorMessage"></span>
                    </div>

                    <!-- Bảng dữ liệu Audit Logs -->
                    <div class="audit-table-responsive" id="auditTableWrap">
                        <table class="audit-table" id="auditTable" aria-label="Bảng nhật ký kiểm toán">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 150px;">Thời điểm</th>
                                    <th scope="col" style="width: 170px;">Người thực hiện</th>
                                    <th scope="col" style="width: 180px;">Loại đối tượng</th>
                                    <th scope="col" style="width: 120px;">Mã bản ghi</th>
                                    <th scope="col" style="width: 110px;">Hành động</th>
                                    <th scope="col">Giá trị trước</th>
                                    <th scope="col">Giá trị sau</th>
                                </tr>
                            </thead>
                            <tbody id="auditTableBody">
                                <!-- Rendered dynamically by JS -->
                            </tbody>
                        </table>
                    </div>

                    <!-- Empty State -->
                    <div id="auditEmptyState" style="display: none; text-align: center; padding: 48px 16px;">
                        <div style="font-size: 2.5rem; margin-bottom: 8px;">📋</div>
                        <h3 style="font-size: 1.1rem; color: #1e293b; margin: 0 0 6px 0;">Không có nhật ký nào phù hợp</h3>
                        <p style="color: #64748b; font-size: 0.9rem; margin: 0 0 16px 0;">Thử điều chỉnh lại từ khóa hoặc khoảng thời gian tìm kiếm.</p>
                        <button type="button" class="btn btn-secondary" onclick="document.getElementById('btnResetFilter').click()">Đặt lại bộ lọc</button>
                    </div>

                    <!-- Phân trang -->
                    <div id="auditPagination" style="display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 14px; border-top: 1px solid #e2e8f0;">
                        <div id="auditPaginationInfo" style="font-size: 0.86rem; color: #64748b;">
                            Hiển thị 0 trên tổng số 0 bản ghi
                        </div>
                        <div id="auditPaginationControls" style="display: flex; gap: 6px;">
                            <!-- Page buttons -->
                        </div>
                    </div>
                </section>

            </div>
        </main>
    </div>

    <!-- Footer dùng chung -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <script>
    document.addEventListener('DOMContentLoaded', function () {
        'use strict';

        var contextPath = '${pageContext.request.contextPath}';
        var tableBody = document.getElementById('auditTableBody');
        var loading = document.getElementById('auditLoading');
        var emptyState = document.getElementById('auditEmptyState');
        var tableWrap = document.getElementById('auditTableWrap');
        var paginationEl = document.getElementById('auditPagination');
        var paginationInfo = document.getElementById('auditPaginationInfo');
        var paginationControls = document.getElementById('auditPaginationControls');
        var errorState = document.getElementById('auditErrorState');
        var errorMessage = document.getElementById('auditErrorMessage');
        var filterError = document.getElementById('auditFilterError');

        var filterActor = document.getElementById('filterActor');
        var filterEntityType = document.getElementById('filterEntityType');
        var filterFromDate = document.getElementById('filterFromDate');
        var filterToDate = document.getElementById('filterToDate');
        var btnReset = document.getElementById('btnResetFilter');

        var state = {
            actor: '',
            entityType: 'ALL',
            fromDate: '',
            toDate: '',
            page: 1,
            size: 15,
            totalPages: 1,
            totalItems: 0
        };

        function escapeHtml(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function getEntityBadge(type) {
            var t = (type || '').toUpperCase();
            switch (t) {
                case 'DISCOUNT':
                    return '<span class="audit-type-badge audit-type--discount">🏷️ Chiết khấu</span>';
                case 'SALES_TARGET':
                    return '<span class="audit-type-badge audit-type--target">🎯 Chỉ tiêu</span>';
                case 'DATA_OWNERSHIP':
                    return '<span class="audit-type-badge audit-type--ownership">🔑 Quyền sở hữu</span>';
                case 'USER_ROLE':
                    return '<span class="audit-type-badge audit-type--role">🛡️ Vai trò</span>';
                default:
                    return '<span class="audit-type-badge">' + escapeHtml(type) + '</span>';
            }
        }

        function getActionBadge(action) {
            var act = (action || 'UPDATE').toUpperCase();
            return '<span class="audit-action-tag">' + escapeHtml(act) + '</span>';
        }

        function getAuditValue(value, emptyText) {
            return value === null || value === undefined || value === '' ? emptyText : value;
        }

        function showLoadError(message) {
            loading.style.display = 'none';
            tableBody.innerHTML = '';
            tableWrap.style.display = 'none';
            emptyState.style.display = 'none';
            paginationEl.style.display = 'none';
            paginationControls.innerHTML = '';
            state.totalItems = 0;
            state.totalPages = 1;
            errorMessage.textContent = message;
            errorState.style.display = 'flex';
        }

        async function fetchAuditLogs() {
            loading.style.display = 'block';
            tableBody.innerHTML = '';
            tableWrap.style.display = 'none';
            emptyState.style.display = 'none';
            paginationEl.style.display = 'none';
            errorState.style.display = 'none';

            var params = new URLSearchParams({
                actor: state.actor,
                entityType: state.entityType === 'ALL' ? '' : state.entityType,
                fromDate: state.fromDate,
                toDate: state.toDate,
                page: state.page,
                size: state.size
            });

            try {
                // BE CONTRACT NEEDED: /api/audit-logs.
                var response = await fetch(contextPath + '/api/audit-logs?' + params.toString(), {
                    headers: { 'Accept': 'application/json' }
                });

                loading.style.display = 'none';

                if (!response.ok) {
                    showLoadError('API trả về lỗi ' + response.status + '. Vui lòng thử lại sau.');
                    return;
                }

                var resData = await response.json();
                if (!resData || resData.success === false || !resData.data || !Array.isArray(resData.data.items)) {
                    showLoadError((resData && resData.message)
                        ? resData.message
                        : 'API Audit Log chưa khả dụng hoặc trả về dữ liệu không hợp lệ.');
                    return;
                }

                var pageData = resData.data;

                if (pageData.items.length === 0) {
                    state.totalItems = 0;
                    state.totalPages = 1;
                    tableWrap.style.display = 'none';
                    emptyState.style.display = 'block';
                    paginationEl.style.display = 'none';
                    return;
                }

                tableWrap.style.display = 'block';
                emptyState.style.display = 'none';
                paginationEl.style.display = 'flex';

                state.totalItems = Number(pageData.totalItems) || pageData.items.length;
                state.totalPages = Math.max(1, Number(pageData.totalPages) || 1);

                renderTable(pageData.items);
                renderPagination();

            } catch (err) {
                console.error('Lỗi khi fetch audit logs:', err);
                showLoadError('Không thể kết nối đến máy chủ hoặc dữ liệu trả về không hợp lệ.');
            }
        }

        function renderTable(items) {
            tableBody.innerHTML = '';

            items.forEach(function (log) {
                var dateStr = '-';
                if (log.createdAt) {
                    if (typeof log.createdAt === 'object' && log.createdAt.year) {
                        dateStr = log.createdAt.year + '-' +
                            String(log.createdAt.monthValue || log.createdAt.month).padStart(2, '0') + '-' +
                            String(log.createdAt.dayOfMonth).padStart(2, '0') + ' ' +
                            String(log.createdAt.hour).padStart(2, '0') + ':' +
                            String(log.createdAt.minute).padStart(2, '0');
                    } else {
                        dateStr = String(log.createdAt).replace('T', ' ').substring(0, 16);
                    }
                }

                var actorStr = log.actorName || (log.actorId ? ('User #' + log.actorId) : '-');
                var recordId = log.recordId !== null && log.recordId !== undefined ? log.recordId : '-';

                var row = document.createElement('tr');
                row.innerHTML =
                    '<td><span style="font-size: 0.85rem; color: #475569; font-weight: 500;">' + escapeHtml(dateStr) + '</span></td>' +
                    '<td><strong>' + escapeHtml(actorStr) + '</strong></td>' +
                    '<td>' + getEntityBadge(log.entityType) + '</td>' +
                    '<td><span class="audit-record-id">' + escapeHtml(recordId) + '</span></td>' +
                    '<td>' + getActionBadge(log.action) + '</td>' +
                    '<td><div class="value-box-old">' + escapeHtml(getAuditValue(log.oldValue, '(Không có giá trị trước)')) + '</div></td>' +
                    '<td><div class="value-box-new">' + escapeHtml(getAuditValue(log.newValue, '(Không có giá trị sau)')) + '</div></td>';

                tableBody.appendChild(row);
            });
        }

        function renderPagination() {
            var startIdx = ((state.page - 1) * state.size) + 1;
            var endIdx = Math.min(state.page * state.size, state.totalItems);
            paginationInfo.textContent = 'Hiển thị ' + startIdx + ' - ' + endIdx + ' trên tổng số ' + state.totalItems + ' bản ghi';

            paginationControls.innerHTML = '';

            // Nút Prev
            var prevBtn = document.createElement('button');
            prevBtn.type = 'button';
            prevBtn.className = 'btn btn-secondary';
            prevBtn.style.padding = '4px 10px';
            prevBtn.style.fontSize = '0.82rem';
            prevBtn.innerHTML = '&laquo; Trước';
            prevBtn.disabled = state.page <= 1;
            prevBtn.addEventListener('click', function () {
                if (state.page > 1) {
                    state.page--;
                    fetchAuditLogs();
                }
            });
            paginationControls.appendChild(prevBtn);

            // Nút Page Number
            var pageSpan = document.createElement('span');
            pageSpan.style.display = 'inline-flex';
            pageSpan.style.alignItems = 'center';
            pageSpan.style.padding = '0 8px';
            pageSpan.style.fontSize = '0.86rem';
            pageSpan.style.fontWeight = '600';
            pageSpan.textContent = state.page + ' / ' + state.totalPages;
            paginationControls.appendChild(pageSpan);

            // Nút Next
            var nextBtn = document.createElement('button');
            nextBtn.type = 'button';
            nextBtn.className = 'btn btn-secondary';
            nextBtn.style.padding = '4px 10px';
            nextBtn.style.fontSize = '0.82rem';
            nextBtn.innerHTML = 'Sau &raquo;';
            nextBtn.disabled = state.page >= state.totalPages;
            nextBtn.addEventListener('click', function () {
                if (state.page < state.totalPages) {
                    state.page++;
                    fetchAuditLogs();
                }
            });
            paginationControls.appendChild(nextBtn);
        }

        window.applyFilter = function () {
            state.page = 1;
            filterError.style.display = 'none';

            if (filterFromDate.value && filterToDate.value && filterFromDate.value > filterToDate.value) {
                filterError.textContent = 'Từ ngày không được lớn hơn Đến ngày.';
                filterError.style.display = 'block';
                return;
            }

            state.actor = filterActor.value.trim();
            state.entityType = filterEntityType.value;
            state.fromDate = filterFromDate.value;
            state.toDate = filterToDate.value;
            fetchAuditLogs();
        };

        filterActor.addEventListener('input', function () {
            state.page = 1;
            filterError.style.display = 'none';
        });

        [filterEntityType, filterFromDate, filterToDate].forEach(function (filter) {
            filter.addEventListener('change', function () {
                state.page = 1;
                filterError.style.display = 'none';
            });
        });

        btnReset.addEventListener('click', function () {
            filterActor.value = '';
            filterEntityType.value = 'ALL';
            filterFromDate.value = '';
            filterToDate.value = '';
            filterError.style.display = 'none';
            state.page = 1;
            applyFilter();
        });

        fetchAuditLogs();
    });
    </script>
</body>
</html>
