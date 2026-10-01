(function () {
    'use strict';
    var root = document.querySelector('[data-settings-app]');
    if (!root) return;
    var activeDialog = null;
    var toastTimer;
    function el(id) { return document.getElementById(id); }
    function escape(value) { return String(value == null ? '' : value).replace(/[&<>"']/g, function (char) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]; }); }
    function icon(name) {
        var paths = { add: '<path d="M12 5v14M5 12h14"/>', edit: '<path d="m16 3 5 5-12 12-6 1 1-6Z"/><path d="m14 5 5 5"/>', delete: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>', up: '<path d="m6 15 6-6 6 6"/>', down: '<path d="m6 9 6 6 6-6"/>' };
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + (paths[name] || paths.add) + '</svg>';
    }
    function notice(message, success) {
        clearTimeout(toastTimer);
        var target = el(success ? 'globalSuccessAlert' : 'globalErrorAlert');
        var other = el(success ? 'globalErrorAlert' : 'globalSuccessAlert');
        if (other) other.hidden = true;
        el(success ? 'globalSuccessMessage' : 'globalErrorMessage').textContent = message;
        target.hidden = false;
        if (success) toastTimer = setTimeout(function () { target.hidden = true; }, 5000);
    }
    async function api(path, method, payload) {
        var response;
        try {
            response = await fetch((root.dataset.contextPath || '') + path, {
                method: method || 'GET', credentials: 'same-origin',
                headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
                body: payload === undefined ? undefined : JSON.stringify(payload)
            });
        } catch (error) { throw new Error('Không thể kết nối đến hệ thống. Vui lòng thử lại.'); }
        var result;
        try { result = await response.json(); } catch (error) { throw new Error('Phản hồi không hợp lệ. Vui lòng tải lại trang hoặc đăng nhập lại.'); }
        if (!response.ok || !result || result.success !== true) {
            var failure = new Error(result && result.message ? result.message : 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.');
            failure.status = response.status;
            failure.data = result && result.data;
            throw failure;
        }
        return result.data;
    }
    function items(data) {
        if (Array.isArray(data)) return data;
        if (data && Array.isArray(data.items)) return data.items;
        throw new Error('Danh sách dữ liệu không hợp lệ.');
    }
    function action(action, id, label, glyph, disabled) {
        return '<button type="button" class="crm-settings-icon" data-action="' + action + '" data-id="' + id + '" title="' + escape(label) + '" aria-label="' + escape(label) + '"' + (disabled ? ' disabled' : '') + '>' + icon(glyph) + '</button>';
    }
    function status(active) { return '<span class="crm-badge ' + (active ? 'crm-badge-success' : 'crm-badge-warning') + '">' + (active ? 'Đang sử dụng' : 'Ngừng sử dụng') + '</span>'; }
    function empty(body, columns, title, description) {
        body.innerHTML = '<tr><td colspan="' + columns + '"><div class="crm-state"><h3 class="crm-state-title">' + escape(title) + '</h3><p>' + escape(description) + '</p></div></td></tr>';
    }
    function clearErrors(form) {
        form.querySelectorAll('[aria-invalid]').forEach(function (field) { field.removeAttribute('aria-invalid'); });
        form.querySelectorAll('.form-feedback').forEach(function (feedback) { feedback.textContent = ''; });
        form.querySelectorAll('[data-form-error]').forEach(function (error) { error.hidden = true; });
    }
    function validate(form, checks) {
        clearErrors(form);
        var invalid = checks.filter(function (check) { return check.invalid; });
        invalid.forEach(function (check) {
            el(check.id).setAttribute('aria-invalid', 'true');
            el(check.feedback).textContent = check.message;
        });
        if (invalid.length) el(invalid[0].id).focus();
        return !invalid.length;
    }
    function busy(form, saving) {
        form.setAttribute('aria-busy', String(saving));
        form.querySelectorAll('input,select,textarea,button').forEach(function (control) {
            if (saving) { control.dataset.preBusyDisabled = String(control.disabled); control.disabled = true; }
            else { control.disabled = control.dataset.preBusyDisabled === 'true'; delete control.dataset.preBusyDisabled; }
        });
    }
    function dialog(id) {
        var element = el(id);
        var savedFocus;
        var background = [];
        var overflow;
        var controller = {
            element: element, saving: false,
            open: function (trigger) {
                if (activeDialog) activeDialog.close();
                savedFocus = trigger || document.activeElement;
                overflow = document.body.style.overflow;
                document.body.style.overflow = 'hidden';
                background = Array.from(document.querySelectorAll('.crm-settings-content, .crm-header, .sidebar, .sidebar__backdrop, body > footer')).filter(function (node) { return !element.contains(node); }).map(function (node) {
                    var prior = node.inert; node.inert = true; return { node: node, prior: prior };
                });
                element.hidden = false;
                element.classList.add('is-open');
                activeDialog = controller;
                var first = element.querySelector('[data-autofocus]') || element.querySelector('button,input,select,textarea');
                if (first) first.focus();
            },
            close: function () {
                if (controller.saving || element.hidden) return;
                element.hidden = true; element.classList.remove('is-open');
                document.body.style.overflow = overflow;
                background.forEach(function (entry) { entry.node.inert = entry.prior; });
                activeDialog = null;
                if (savedFocus && savedFocus.isConnected) savedFocus.focus();
                else { var fallback = root.querySelector('[data-create]'); if (fallback) fallback.focus(); }
            }
        };
        element.addEventListener('click', function (event) { if (event.target === element || event.target.closest('[data-modal-close]')) controller.close(); });
        element.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') { event.preventDefault(); controller.close(); return; }
            if (event.key !== 'Tab') return;
            var focusable = Array.from(element.querySelectorAll('button:not(:disabled),input:not([type="hidden"]):not(:disabled),select:not(:disabled),textarea:not(:disabled),[tabindex="0"]')).filter(function (node) { return node.getClientRects().length; });
            if (!focusable.length) { event.preventDefault(); return; }
            var first = focusable[0], last = focusable[focusable.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        });
        return controller;
    }
    root.addEventListener('click', function (event) { var button = event.target.closest('[data-dismiss]'); if (button) el(button.dataset.dismiss).hidden = true; });
    window.CrmSettingsUI = Object.freeze({ root: root, el: el, escape: escape, icon: icon, api: api, items: items, notice: notice, action: action, status: status, empty: empty, validate: validate, clearErrors: clearErrors, busy: busy, dialog: dialog });
})();
