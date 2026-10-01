(function () {
    'use strict';
    var app = document.getElementById('orgApp');
    if (!app) return;
    var contextPath = app.dataset.contextPath || '';
    var canManage = app.dataset.canManage === 'true';
    var regions = { NORTH: 'Miền Bắc', CENTRAL: 'Miền Trung', SOUTH: 'Miền Nam', NATIONAL: 'Toàn quốc', OVERSEAS: 'Quốc tế' };
    var roleLabels = { Admin: 'Quản trị viên', Director: 'Giám đốc', 'Team Lead': 'Trưởng nhóm', 'Sales Rep': 'Nhân viên kinh doanh', ADMIN: 'Quản trị viên', DIRECTOR: 'Giám đốc', TEAM_LEAD: 'Trưởng nhóm', SALES_REP: 'Nhân viên kinh doanh' };
    var state = { units: [], managers: [], selectedId: null, viewMode: 'tree', keyword: '', filterRegion: '', collapsedNodes: {}, loading: false, error: false, saving: false };
    var modalTrigger = null;
    var inertElements = [];
    var previousOverflow = '';
    var successTimer;
    var managerRequest = null;
    var modalVersion = 0;
    var unitsRequestId = 0;
    function el(id) { return document.getElementById(id); }
    function escapeHtml(value) {
        return String(value == null ? '' : value).replace(/[&<>"']/g, function (char) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char];
        });
    }
    function roleText(value) {
        return String(value || '').split(',').map(function (role) {
            var name = role.trim();
            return roleLabels[name] || name;
        }).filter(Boolean).join(', ');
    }
    function regionText(value) { return regions[value] || 'Chưa xác định'; }
    function findUnit(id) { return state.units.find(function (unit) { return Number(unit.id) === Number(id); }); }
    function svg(paths) { return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + paths + '</svg>'; }
    var icons = {
        chevron: svg('<path d="m9 5 7 7-7 7"/>'),
        unit: svg('<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 7h6M9 11h6M10 21v-6h4v6"/>'),
        edit: svg('<path d="m16 3 5 5-12 12-6 1 1-6Z"/><path d="m14 5 5 5"/>'),
        add: svg('<path d="M12 5v14M5 12h14"/>')
    };
    function showError(message) {
        el('globalErrorMessage').textContent = message;
        el('globalErrorAlert').hidden = false;
        el('globalSuccessAlert').hidden = true;
    }
    function showSuccess(message) {
        clearTimeout(successTimer);
        el('globalSuccessMessage').textContent = message;
        el('globalSuccessAlert').hidden = false;
        el('globalErrorAlert').hidden = true;
        successTimer = setTimeout(function () { el('globalSuccessAlert').hidden = true; }, 5000);
    }
    async function requestJson(url, options) {
        var response;
        try {
            response = await fetch(url, Object.assign({ credentials: 'same-origin', headers: { Accept: 'application/json' } }, options || {}));
        } catch (error) {
            throw new Error('Không thể kết nối đến hệ thống. Vui lòng thử lại.');
        }
        var result;
        try { result = await response.json(); } catch (error) { throw new Error('Phản hồi không hợp lệ. Vui lòng tải lại trang hoặc đăng nhập lại.'); }
        if (!response.ok || !result || result.success !== true) {
            throw new Error(result && result.message ? result.message : 'Không thể thực hiện yêu cầu. Vui lòng thử lại.');
        }
        return result.data;
    }
    function buildTreeData(units) {
        var map = {};
        var roots = [];
        units.forEach(function (unit) { map[unit.id] = Object.assign({}, unit, { children: [] }); });
        units.forEach(function (unit) {
            if (unit.parentId && map[unit.parentId]) map[unit.parentId].children.push(map[unit.id]);
            else roots.push(map[unit.id]);
        });
        return roots;
    }
    function filterUnits() {
        var keyword = state.keyword.trim().toLocaleLowerCase('vi');
        return state.units.filter(function (unit) {
            return (!state.filterRegion || state.filterRegion === unit.region) &&
                (!keyword || (String(unit.name || '') + ' ' + String(unit.managerName || '')).toLocaleLowerCase('vi').includes(keyword));
        });
    }
    function emptyMessage() {
        if (state.error) return '<li class="crm-state crm-state-error"><h3 class="crm-state-title">Chưa tải được cơ cấu tổ chức</h3><p>Vui lòng chọn Làm mới để thử lại.</p></li>';
        if (state.units.length) return '<li class="crm-state"><h3 class="crm-state-title">Không tìm thấy đơn vị</h3><p>Thử từ khóa khác hoặc chọn tất cả khu vực.</p></li>';
        return '<li class="crm-state"><h3 class="crm-state-title">Chưa có đơn vị nào</h3><p>' + (canManage ? 'Chọn Thêm đơn vị để bắt đầu xây dựng cơ cấu tổ chức.' : 'Các đơn vị sẽ xuất hiện tại đây khi được thiết lập.') + '</p></li>';
    }
    function memberCount(unit) { return unit.memberCount == null ? (unit.members || []).length : unit.memberCount; }
    function nodeActions(unit) {
        return canManage ? '<div class="org-node-actions"><button type="button" class="org-icon-btn" data-action="edit" data-id="' + unit.id + '" aria-label="Sửa đơn vị ' + escapeHtml(unit.name) + '" title="Sửa đơn vị">' + icons.edit + '</button><button type="button" class="org-icon-btn" data-action="add-child" data-id="' + unit.id + '" aria-label="Thêm đơn vị con của ' + escapeHtml(unit.name) + '" title="Thêm đơn vị con">' + icons.add + '</button></div>' : '';
    }
    function createTreeNodeElement(node, depth, visible, matched) {
        var li = document.createElement('li');
        li.className = 'org-tree-node';
        var children = node.children.filter(function (child) { return visible[child.id]; });
        var filtering = Boolean(state.keyword.trim() || state.filterRegion);
        var expanded = filtering || !state.collapsedNodes[node.id];
        var card = document.createElement('div');
        card.className = 'org-node-card' + (state.selectedId === node.id ? ' is-selected' : '') + (!matched[node.id] ? ' is-context' : '');
        card.style.setProperty('--org-depth', String(depth));
        card.innerHTML = (children.length ? '<button type="button" class="org-icon-btn org-tree-toggle" data-toggle-id="' + node.id + '" aria-expanded="' + expanded + '" aria-controls="orgChildren' + node.id + '" aria-label="Thu gọn hoặc mở rộng ' + escapeHtml(node.name) + '"' + (filtering ? ' disabled' : '') + '>' + icons.chevron + '</button>' : '<span class="org-toggle-placeholder">' + icons.unit + '</span>') +
            '<div class="org-node-content"><button type="button" class="org-node-name" data-action="view-members" data-id="' + node.id + '"' + (state.selectedId === node.id ? ' aria-current="true"' : '') + '>' + escapeHtml(node.name) + '</button><div class="org-node-meta"><span>Cấp ' + (depth + 1) + '</span><span class="crm-badge">' + escapeHtml(regionText(node.region)) + '</span><span>' + escapeHtml(node.managerName || 'Chưa phân công trưởng đơn vị') + '</span>' + (node.active === false ? '<span class="crm-badge crm-badge-warning">Ngừng hoạt động</span>' : '') + '</div><div class="org-node-footer"><button type="button" class="org-count-btn" data-action="view-members" data-id="' + node.id + '">' + escapeHtml(memberCount(node)) + ' thành viên</button>' + nodeActions(node) + '</div></div>';
        li.appendChild(card);
        if (children.length) {
            var ul = document.createElement('ul');
            ul.className = 'org-children-container';
            ul.id = 'orgChildren' + node.id;
            ul.style.setProperty('--org-depth', String(depth + 1));
            ul.hidden = !expanded;
            children.forEach(function (child) { ul.appendChild(createTreeNodeElement(child, depth + 1, visible, matched)); });
            li.appendChild(ul);
        }
        return li;
    }
    function renderTreeView() {
        var root = el('orgTreeRoot');
        root.replaceChildren();
        if (state.loading) return;
        var matched = {};
        var visible = {};
        filterUnits().forEach(function (unit) {
            matched[unit.id] = true;
            var cursor = unit;
            while (cursor && !visible[cursor.id]) {
                visible[cursor.id] = true;
                cursor = findUnit(cursor.parentId);
            }
        });
        var roots = buildTreeData(state.units).filter(function (node) { return visible[node.id]; });
        if (!roots.length) { root.innerHTML = emptyMessage(); return; }
        roots.forEach(function (node) { root.appendChild(createTreeNodeElement(node, 0, visible, matched)); });
    }
    function renderTableView() {
        var units = filterUnits();
        var body = el('orgTableBody');
        if (state.loading) { body.replaceChildren(); return; }
        if (!units.length) { body.innerHTML = '<tr><td colspan="8">' + emptyMessage().replace(/^<li/, '<div').replace(/<\/li>$/, '</div>') + '</td></tr>'; return; }
        body.innerHTML = units.map(function (unit, index) {
            var parent = findUnit(unit.parentId);
            return '<tr' + (unit.id === state.selectedId ? ' class="is-selected"' : '') + '><td>' + (index + 1) + '</td><td><button type="button" class="org-node-name" data-action="view-members" data-id="' + unit.id + '">' + escapeHtml(unit.name) + '</button></td><td>' + escapeHtml(unit.managerName || 'Chưa phân công') + '</td><td>' + escapeHtml(regionText(unit.region)) + '</td><td>' + escapeHtml(parent ? parent.name : 'Đơn vị gốc') + '</td><td><button type="button" class="org-count-btn" data-action="view-members" data-id="' + unit.id + '">' + escapeHtml(memberCount(unit)) + ' thành viên</button></td><td><span class="crm-badge ' + (unit.active === false ? 'crm-badge-warning' : 'crm-badge-success') + '">' + (unit.active === false ? 'Ngừng hoạt động' : 'Đang hoạt động') + '</span></td><td>' + nodeActions(unit) + '</td></tr>';
        }).join('');
    }
    function renderCurrentView() {
        var table = state.viewMode === 'table';
        el('orgTreeViewArea').hidden = table;
        el('orgTableViewArea').hidden = !table;
        el('btnViewTree').classList.toggle('active', !table);
        el('btnViewTable').classList.toggle('active', table);
        el('btnViewTree').setAttribute('aria-pressed', String(!table));
        el('btnViewTable').setAttribute('aria-pressed', String(table));
        el('btnExpandAll').hidden = table;
        el('btnCollapseAll').hidden = table;
        el('orgUnitCount').textContent = state.loading ? 'Đang tải cơ cấu tổ chức…' : state.error ? 'Dữ liệu chưa sẵn sàng' : filterUnits().length + ' / ' + state.units.length + ' đơn vị';
        if (table) renderTableView(); else renderTreeView();
    }
    function renderDetail() {
        var unit = state.selectedId == null ? null : findUnit(state.selectedId);
        el('unitDrawer').hidden = !unit;
        el('orgDetailEmpty').hidden = Boolean(unit);
        if (!unit) return;
        el('drawerTitle').textContent = unit.name;
        el('drawerSubtitle').textContent = unit.active === false ? 'Ngừng hoạt động' : 'Đang hoạt động';
        var parent = findUnit(unit.parentId);
        el('drawerParent').textContent = parent ? parent.name : 'Đơn vị gốc';
        el('drawerRegion').textContent = regionText(unit.region);
        el('drawerManagerName').textContent = unit.managerName || 'Chưa phân công';
        el('drawerManagerRole').textContent = roleText(unit.managerRole);
        el('drawerManagerAvatar').textContent = unit.managerName ? unit.managerName.charAt(0).toLocaleUpperCase('vi') : '—';
        el('btnEditSelected').dataset.id = unit.id;
        el('btnAddSelectedChild').dataset.id = unit.id;
        var members = Array.isArray(unit.members) ? unit.members : [];
        el('drawerMemberCount').textContent = members.length;
        el('drawerMembersList').innerHTML = members.length ? members.map(function (member) {
            return '<li class="org-member-item"><div class="org-member-profile"><span class="org-avatar" aria-hidden="true">' + escapeHtml(member.name ? member.name.charAt(0).toLocaleUpperCase('vi') : '—') + '</span><div class="org-profile-text"><strong class="org-member-name">' + escapeHtml(member.name) + '</strong><p class="org-member-email">' + escapeHtml(member.email) + '</p><p class="org-help">' + escapeHtml(roleText(member.role)) + '</p>' + (member.joinedDate ? '<p class="org-help">Ngày tạo tài khoản: ' + escapeHtml(member.joinedDate) + '</p>' : '') + '</div></div></li>';
        }).join('') : '<li class="crm-state">Chưa có thành viên trong đơn vị này.</li>';
    }
    async function fetchUnits() {
        var requestId = ++unitsRequestId;
        state.loading = true;
        state.error = false;
        el('orgLoadingOverlay').hidden = false;
        el('orgApp').setAttribute('aria-busy', 'true');
        el('globalErrorAlert').hidden = true;
        renderCurrentView();
        var loaded = false;
        try {
            var data = await requestJson(contextPath + '/api/organization/units');
            if (requestId !== unitsRequestId) return false;
            if (!data || !Array.isArray(data.items)) throw new Error('Dữ liệu cơ cấu tổ chức không hợp lệ.');
            state.units = data.items;
            if (!findUnit(state.selectedId)) state.selectedId = null;
            loaded = true;
        } catch (error) {
            if (requestId !== unitsRequestId) return false;
            state.units = [];
            state.selectedId = null;
            state.error = true;
            showError(error.message || 'Không thể tải cơ cấu tổ chức. Vui lòng thử lại.');
        } finally {
            if (requestId === unitsRequestId) {
                state.loading = false;
                el('orgLoadingOverlay').hidden = true;
                el('orgApp').removeAttribute('aria-busy');
                renderCurrentView();
                renderDetail();
            }
        }
        return loaded;
    }
    function populateParentSelect(currentUnitId) {
        var disallowed = {};
        if (currentUnitId) {
            disallowed[currentUnitId] = true;
            var changed = true;
            while (changed) {
                changed = false;
                state.units.forEach(function (unit) {
                    if (disallowed[unit.parentId] && !disallowed[unit.id]) { disallowed[unit.id] = true; changed = true; }
                });
            }
        }
        el('formUnitParent').innerHTML = '<option value="">Không có đơn vị cấp trên</option>';
        function appendOptions(nodes, depth) {
            nodes.forEach(function (unit) {
                if (!disallowed[unit.id]) {
                    var option = document.createElement('option');
                    option.value = unit.id;
                    option.textContent = '— '.repeat(Math.min(depth, 5)) + unit.name;
                    el('formUnitParent').appendChild(option);
                }
                appendOptions(unit.children, depth + 1);
            });
        }
        appendOptions(buildTreeData(state.units), 0);
    }
    function populateManagers(selectedId) {
        var select = el('formUnitManager');
        select.innerHTML = '<option value="">Chọn trưởng đơn vị</option>';
        state.managers.forEach(function (user) {
            var option = document.createElement('option');
            option.value = user.id;
            option.textContent = (user.fullName || user.displayName || user.email || '') + (user.email ? ' · ' + user.email : '');
            select.appendChild(option);
        });
        select.value = selectedId || '';
    }
    async function loadManagers(selectedId, version) {
        el('formUnitManager').disabled = true;
        el('managerLoadStatus').textContent = 'Đang tải nhân sự đang hoạt động…';
        try {
            if (!managerRequest) {
                managerRequest = (async function () {
                    var items = [];
                    var page = 1;
                    var totalPages = 1;
                    do {
                        var data = await requestJson(contextPath + '/api/users?status=ACTIVE&page=' + page + '&size=100');
                        if (!data || !Array.isArray(data.items)) throw new Error('Danh sách nhân sự không hợp lệ.');
                        items = items.concat(data.items.filter(function (user) { return user.status === 'ACTIVE'; }));
                        totalPages = Number(data.totalPages) || 0;
                        page++;
                    } while (page <= totalPages);
                    return items;
                })();
            }
            state.managers = await managerRequest;
            if (el('unitModal').hidden || version !== modalVersion) return;
            populateManagers(selectedId);
            el('managerLoadStatus').textContent = state.managers.length ? 'Chọn nhân sự đang hoạt động. Mỗi đơn vị có một trưởng đơn vị.' : 'Chưa có nhân sự đang hoạt động để chọn.';
        } catch (error) {
            if (el('unitModal').hidden || version !== modalVersion) return;
            state.managers = [];
            populateManagers(null);
            el('managerLoadStatus').textContent = 'Không thể tải nhân sự. Đóng và mở lại biểu mẫu để thử lại.';
            el('unitFormError').textContent = error.message;
            el('unitFormError').hidden = false;
        } finally {
            managerRequest = null;
            if (version === modalVersion) el('formUnitManager').disabled = false;
        }
    }
    function clearFormErrors() {
        el('unitFormError').hidden = true;
        el('unitForm').querySelectorAll('.form-feedback').forEach(function (feedback) { feedback.textContent = ''; });
        el('unitForm').querySelectorAll('.form-control').forEach(function (input) { input.classList.remove('is-invalid'); input.removeAttribute('aria-invalid'); });
    }
    function openUnitModal(id, parentId, trigger) {
        if (!canManage || state.loading || state.saving) return;
        var unit = id ? findUnit(id) : null;
        if (id && !unit) return;
        modalTrigger = trigger || document.activeElement;
        el('unitForm').reset();
        clearFormErrors();
        el('formUnitId').value = unit ? unit.id : '';
        el('unitModalHeading').textContent = unit ? 'Sửa đơn vị' : parentId ? 'Thêm đơn vị con' : 'Thêm đơn vị';
        el('formUnitName').value = unit ? unit.name : '';
        populateParentSelect(unit ? unit.id : null);
        el('formUnitParent').value = unit ? unit.parentId || '' : parentId || '';
        el('formUnitRegion').value = unit ? unit.region || 'NATIONAL' : 'NORTH';
        populateManagers(null);
        el('unitModal').hidden = false;
        el('unitModal').classList.add('is-open');
        previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        inertElements = Array.from(document.querySelectorAll('.org-container, .crm-header, .sidebar, .sidebar__backdrop, footer')).filter(function (element) {
            return !el('unitModal').contains(element);
        }).map(function (element) {
            var prior = element.inert;
            element.inert = true;
            return { element: element, prior: prior };
        });
        el('formUnitName').focus();
        loadManagers(unit ? unit.managerId : null, ++modalVersion);
    }
    function closeUnitModal() {
        if (state.saving || el('unitModal').hidden) return;
        el('unitModal').hidden = true;
        el('unitModal').classList.remove('is-open');
        document.body.style.overflow = previousOverflow;
        inertElements.forEach(function (entry) { entry.element.inert = entry.prior; });
        inertElements = [];
        if (modalTrigger && modalTrigger.isConnected) modalTrigger.focus();
        else el('btnOpenCreateUnitModal').focus();
    }
    function validateUnitForm() {
        clearFormErrors();
        var fields = [
            ['formUnitName', 'feedbackUnitName', 'Vui lòng nhập tên đơn vị.', !el('formUnitName').value.trim()],
            ['formUnitManager', 'feedbackUnitManager', 'Vui lòng chọn một trưởng đơn vị.', !el('formUnitManager').value],
            ['formUnitRegion', 'feedbackUnitRegion', 'Vui lòng chọn khu vực.', !el('formUnitRegion').value]
        ];
        var invalid = fields.filter(function (field) { return field[3]; });
        invalid.forEach(function (field) { el(field[1]).textContent = field[2]; el(field[0]).classList.add('is-invalid'); el(field[0]).setAttribute('aria-invalid', 'true'); });
        if (invalid.length) el(invalid[0][0]).focus();
        return !invalid.length;
    }
    el('unitForm').addEventListener('submit', async function (event) {
        event.preventDefault();
        if (!canManage || state.saving || el('formUnitManager').disabled || !validateUnitForm()) return;
        var id = el('formUnitId').value;
        var manager = state.managers.find(function (user) { return String(user.id) === el('formUnitManager').value; });
        if (!manager) return;
        var managerRole = manager.role || (manager.roles || []).map(function (role) { return typeof role === 'string' ? role : role.name; }).join(', ');
        var payload = {
            name: el('formUnitName').value.trim(),
            parentId: el('formUnitParent').value ? Number(el('formUnitParent').value) : null,
            managerId: Number(el('formUnitManager').value),
            managerName: manager.fullName || manager.displayName || '',
            managerRole: managerRole,
            region: el('formUnitRegion').value,
            active: true
        };
        state.saving = true;
        el('btnSaveUnit').disabled = true;
        el('btnSaveUnit').textContent = 'Đang lưu…';
        el('btnCloseUnitModal').disabled = true;
        el('btnCancelUnitModal').disabled = true;
        el('unitForm').setAttribute('aria-busy', 'true');
        // Lock controls while saving; the server remains authoritative for all validation.
        var controls = Array.from(el('unitForm').querySelectorAll('.form-control'));
        controls.forEach(function (control) { control.disabled = true; });
        var saved = false;
        try {
            await requestJson(contextPath + '/api/organization/units' + (id ? '/' + id : ''), {
                method: id ? 'PUT' : 'POST',
                headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            saved = true;
        } catch (error) {
            el('unitFormError').textContent = error.message || 'Không thể lưu đơn vị. Vui lòng thử lại.';
            el('unitFormError').hidden = false;
        } finally {
            state.saving = false;
            controls.forEach(function (control) { control.disabled = false; });
            el('btnSaveUnit').disabled = false;
            el('btnSaveUnit').textContent = 'Lưu';
            el('btnCloseUnitModal').disabled = false;
            el('btnCancelUnitModal').disabled = false;
            el('unitForm').removeAttribute('aria-busy');
        }
        if (saved) {
            closeUnitModal();
            var loaded = await fetchUnits();
            if (loaded) showSuccess(id ? 'Đã cập nhật đơn vị.' : 'Đã thêm đơn vị.');
            else showError('Đã lưu đơn vị nhưng chưa tải lại được danh sách. Chọn Làm mới để kiểm tra dữ liệu.');
        }
    });
    app.addEventListener('click', function (event) {
        var dismiss = event.target.closest('[data-dismiss]');
        if (dismiss) { el(dismiss.dataset.dismiss).hidden = true; return; }
        var toggle = event.target.closest('[data-toggle-id]');
        if (toggle) {
            var id = Number(toggle.dataset.toggleId);
            state.collapsedNodes[id] = !state.collapsedNodes[id];
            renderTreeView();
            var replacement = el('orgTreeRoot').querySelector('[data-toggle-id="' + id + '"]');
            if (replacement) replacement.focus();
            return;
        }
        var button = event.target.closest('[data-action]');
        if (!button) return;
        var unitId = Number(button.dataset.id);
        if (button.dataset.action === 'view-members') {
            state.selectedId = unitId;
            renderCurrentView();
            renderDetail();
            el('drawerTitle').focus({ preventScroll: true });
            if (window.matchMedia('(max-width: 1000px)').matches) {
                el('unitDrawer').scrollIntoView({ block: 'start', behavior: 'instant' });
            }
        } else if (button.dataset.action === 'edit') openUnitModal(unitId, null, button);
        else if (button.dataset.action === 'add-child') openUnitModal(null, unitId, button);
    });
    el('btnCloseDrawer').addEventListener('click', function () {
        var previousId = state.selectedId;
        state.selectedId = null;
        renderCurrentView();
        renderDetail();
        var button = app.querySelector('[data-action="view-members"][data-id="' + previousId + '"]');
        if (button) button.focus();
    });
    el('btnOpenCreateUnitModal').addEventListener('click', function (event) { openUnitModal(null, null, event.currentTarget); });
    el('btnCloseUnitModal').addEventListener('click', closeUnitModal);
    el('btnCancelUnitModal').addEventListener('click', closeUnitModal);
    el('unitModal').addEventListener('click', function (event) { if (event.target === el('unitModal')) closeUnitModal(); });
    el('unitModal').addEventListener('keydown', function (event) {
        if (event.key === 'Escape') { event.preventDefault(); closeUnitModal(); }
        if (event.key !== 'Tab') return;
        var focusable = Array.from(el('unitModal').querySelectorAll('button:not(:disabled), input:not([type="hidden"]):not(:disabled), select:not(:disabled), [tabindex="0"]')).filter(function (element) { return element.getClientRects().length; });
        var first = focusable[0];
        var last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    });
    ['Tree', 'Table'].forEach(function (mode) { el('btnView' + mode).addEventListener('click', function () { state.viewMode = mode.toLowerCase(); renderCurrentView(); }); });
    el('btnExpandAll').addEventListener('click', function () { state.collapsedNodes = {}; renderCurrentView(); });
    el('btnCollapseAll').addEventListener('click', function () { state.units.forEach(function (unit) { state.collapsedNodes[unit.id] = true; }); renderCurrentView(); });
    el('orgSearchInput').addEventListener('input', function () { state.keyword = this.value; renderCurrentView(); });
    el('filterRegion').addEventListener('change', function () { state.filterRegion = this.value; renderCurrentView(); });
    el('btnReloadUnits').addEventListener('click', fetchUnits);
    fetchUnits();
})();
