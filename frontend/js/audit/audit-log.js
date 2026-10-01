/* Read-only presentation of the existing /api/audit-logs contract. */
document.addEventListener('DOMContentLoaded', function () {
    'use strict';
    var app = document.getElementById('auditApp');
    if (!app) return;
    var contextPath = app.dataset.contextPath || '';
    var form = document.getElementById('auditFilterForm');
    var actor = document.getElementById('filterActor');
    var objectType = document.getElementById('filterEntityType');
    var objectId = document.getElementById('filterObjectId');
    var from = document.getElementById('filterFromDate');
    var to = document.getElementById('filterToDate');
    var filterError = document.getElementById('auditFilterError');
    var loading = document.getElementById('auditLoading');
    var errorState = document.getElementById('auditErrorState');
    var errorMessage = document.getElementById('auditErrorMessage');
    var empty = document.getElementById('auditEmptyState');
    var tableWrap = document.getElementById('auditTableWrap');
    var body = document.getElementById('auditTableBody');
    var pagination = document.getElementById('auditPagination');
    var paginationInfo = document.getElementById('auditPaginationInfo');
    var controls = document.getElementById('auditPaginationControls');
    var limitNotice = document.getElementById('auditLimitNotice');
    var rows = [];
    var page = 1;
    var pageSize = 15;
    var requestController;
    var filters = { userId: '', objectType: '', objectId: '', from: '', to: '' };
    var typeLabels = { USER: 'Người dùng', CUSTOMER: 'Khách hàng', OPPORTUNITY: 'Cơ hội', ACTIVITY: 'Hoạt động', QUOTE: 'Báo giá' };
    var actionLabels = { ROLE_CHANGED: 'Đổi vai trò', OWNERSHIP_CHANGED: 'Bàn giao quyền sở hữu', DISCOUNT_CHANGED: 'Đổi chiết khấu', TARGET_CHANGED: 'Đổi chỉ tiêu', CREATE: 'Tạo mới', UPDATE: 'Cập nhật', DELETE: 'Xóa' };

    function element(tag, className, text) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = String(text);
        return node;
    }

    function valueText(value) {
        if (value === null || value === undefined || value === '') return 'Không có giá trị';
        return typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value);
    }

    function dateText(value) {
        if (!value) return '—';
        var date = new Date(value);
        return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('vi-VN', {
            year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
        });
    }

    function cell(row, child) {
        var td = element('td');
        td.appendChild(child);
        row.appendChild(td);
    }

    function renderTable() {
        body.replaceChildren();
        rows.slice((page - 1) * pageSize, page * pageSize).forEach(function (log) {
            var row = element('tr');
            cell(row, element('time', 'audit-time', dateText(log.createdAt)));
            cell(row, element('span', 'audit-actor', 'Người dùng #' + log.actorUserId));
            cell(row, element('span', 'audit-type-badge crm-badge', typeLabels[log.objectType] || log.objectType || '—'));
            cell(row, element('span', 'audit-record-id', log.objectId == null ? '—' : log.objectId));
            cell(row, element('span', 'audit-action-tag crm-badge', actionLabels[log.action] || log.action || '—'));
            cell(row, element('pre', 'value-box-old', valueText(log.beforeValue)));
            cell(row, element('pre', 'value-box-new', valueText(log.afterValue)));
            body.appendChild(row);
        });
        renderPagination();
    }

    function renderPagination() {
        var totalPages = Math.ceil(rows.length / pageSize);
        var start = (page - 1) * pageSize + 1;
        paginationInfo.textContent = 'Hiển thị ' + start + '–' + Math.min(page * pageSize, rows.length) + ' / ' + rows.length + ' bản ghi đã tải';
        controls.replaceChildren();
        var prev = element('button', 'crm-btn crm-btn-secondary', 'Trước');
        prev.type = 'button';
        prev.dataset.direction = 'prev';
        prev.setAttribute('aria-label', 'Trang trước');
        prev.disabled = page <= 1;
        prev.addEventListener('click', function () { page--; renderTable(); restorePageFocus('prev'); });
        var number = element('span', 'audit-page-number', page + ' / ' + totalPages);
        number.setAttribute('aria-label', 'Trang ' + page + ' trên ' + totalPages);
        var next = element('button', 'crm-btn crm-btn-secondary', 'Sau');
        next.type = 'button';
        next.dataset.direction = 'next';
        next.setAttribute('aria-label', 'Trang sau');
        next.disabled = page >= totalPages;
        next.addEventListener('click', function () { page++; renderTable(); restorePageFocus('next'); });
        controls.append(prev, number, next);
    }

    function restorePageFocus(direction) {
        var button = controls.querySelector('[data-direction="' + direction + '"]');
        if (button.disabled) button = controls.querySelector('button:not(:disabled)');
        if (button) button.focus();
    }

    function showError(message) {
        body.replaceChildren();
        rows = [];
        loading.hidden = true;
        errorMessage.textContent = message;
        errorState.hidden = false;
    }

    async function fetchAuditLogs() {
        if (requestController) requestController.abort();
        var controller = new AbortController();
        requestController = controller;
        app.setAttribute('aria-busy', 'true');
        loading.hidden = false;
        errorState.hidden = true;
        empty.hidden = true;
        tableWrap.hidden = true;
        pagination.hidden = true;
        limitNotice.hidden = true;
        var params = new URLSearchParams();
        Object.keys(filters).forEach(function (key) { if (filters[key]) params.set(key, filters[key]); });
        params.set('limit', '500');
        try {
            var response = await fetch(contextPath + '/api/audit-logs?' + params.toString(), {
                headers: { Accept: 'application/json' }, signal: controller.signal
            });
            if (!response.ok) {
                var message = response.status === 401 ? 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
                    : response.status === 403 ? 'Bạn không có quyền xem nhật ký này.'
                    : 'Không thể tải nhật ký. Vui lòng thử lại.';
                try { var errorBody = await response.json(); if (errorBody.message) message = errorBody.message; } catch (ignored) { /* Keep readable fallback. */ }
                if (controller !== requestController || controller.signal.aborted) return;
                showError(message);
                return;
            }
            var result = await response.json();
            if (controller !== requestController || controller.signal.aborted) return;
            if (!result || result.success !== true || !Array.isArray(result.data)) {
                showError('Không thể đọc dữ liệu nhật ký. Vui lòng thử lại.');
                return;
            }
            rows = result.data;
            page = 1;
            loading.hidden = true;
            if (!rows.length) { body.replaceChildren(); empty.hidden = false; return; }
            renderTable();
            tableWrap.hidden = false;
            pagination.hidden = false;
            limitNotice.hidden = rows.length < 500;
        } catch (error) {
            if (error.name !== 'AbortError' && controller === requestController) showError('Không thể kết nối đến máy chủ. Vui lòng thử lại.');
        } finally {
            if (controller === requestController) {
                loading.hidden = true;
                app.setAttribute('aria-busy', 'false');
            }
        }
    }

    function clearValidation() {
        filterError.hidden = true;
        [from, to].forEach(function (input) { input.removeAttribute('aria-invalid'); input.removeAttribute('aria-describedby'); });
    }

    window.applyFilter = function () {
        clearValidation();
        if (!form.reportValidity()) return;
        if (from.value && to.value && from.value > to.value) {
            filterError.textContent = 'Ngày bắt đầu không được sau ngày kết thúc.';
            filterError.hidden = false;
            from.setAttribute('aria-invalid', 'true');
            from.setAttribute('aria-describedby', 'auditFilterError');
            from.focus();
            return;
        }
        filters = { userId: actor.value.trim(), objectType: objectType.value.trim(), objectId: objectId.value.trim(), from: from.value, to: to.value };
        fetchAuditLogs();
    };
    form.addEventListener('submit', function (event) { event.preventDefault(); window.applyFilter(); });
    form.addEventListener('input', clearValidation);
    function resetFilters() { form.reset(); window.applyFilter(); }
    document.getElementById('btnResetFilter').addEventListener('click', resetFilters);
    document.getElementById('btnEmptyReset').addEventListener('click', resetFilters);
    document.getElementById('btnRefreshAudit').addEventListener('click', fetchAuditLogs);
    document.getElementById('btnRetryAudit').addEventListener('click', fetchAuditLogs);
    fetchAuditLogs();
});
