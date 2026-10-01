(function () {
    'use strict';
    var ui = window.CrmSettingsUI;
    if (!ui) return;
    var el = ui.el, app = ui.root, canManage = app.dataset.canManage === 'true';
    var stages = [], loading = false, failed = false, changing = false, loadVersion = 0, pending = null;
    var modal = ui.dialog('stageModal'), confirmation = ui.dialog('activeDealsNoticeModal');
    var form = el('stageForm'), deleteForm = el('stageDeleteForm'), endpoint = '/api/pipeline/stages';
    function order(stage) { return Number(stage.stageOrder == null ? stage.sortOrder : stage.stageOrder); }
    function requirements(stage) { return stage.requirements == null ? stage.exitCriteria || '' : stage.requirements; }
    function opportunityCount(stage) { return stage.opportunityCount == null ? stage.activeOpportunitiesCount : stage.opportunityCount; }
    function sorted() { return stages.slice().sort(function (a, b) { return order(a) - order(b) || Number(a.id) - Number(b.id); }); }
    function find(id) { return stages.find(function (stage) { return Number(stage.id) === Number(id); }); }
    function fullPayload(stage, overrides) {
        return Object.assign({
            pipelineId: Number(stage.pipelineId) || 1,
            code: stage.code,
            name: stage.name,
            stageOrder: order(stage),
            winProbability: Number(stage.winProbability),
            requirements: requirements(stage),
            won: Boolean(stage.won),
            lost: Boolean(stage.lost),
            active: Boolean(stage.active)
        }, overrides || {});
    }
    function render() {
        var all = sorted();
        var keyword = el('stageSearchInput').value.trim().toLocaleLowerCase('vi');
        var status = el('stageStatusFilter').value;
        var filtered = all.filter(function (stage) {
            return (!keyword || (String(stage.code || '') + ' ' + String(stage.name || '')).toLocaleLowerCase('vi').includes(keyword))
                && (!status || String(Boolean(stage.active)) === status);
        });
        el('stageSummary').textContent = loading ? 'Đang tải…' : failed ? 'Dữ liệu chưa sẵn sàng' : filtered.length + ' / ' + stages.length + ' giai đoạn';
        el('pipelineLoadingOverlay').hidden = !loading;
        el('pipelineStepper').innerHTML = loading ? '' : all.map(function (stage) {
            var content = '<span class="crm-badge">' + ui.escape(order(stage)) + '</span><span>' + ui.escape(stage.name) + '<span class="crm-settings-summary"> · ' + ui.escape(stage.winProbability) + '%</span></span>';
            return canManage ? '<button type="button" class="crm-btn crm-btn-secondary" data-action="edit" data-id="' + stage.id + '">' + content + '</button>'
                : '<div class="crm-btn crm-btn-secondary" aria-label="Giai đoạn ' + ui.escape(stage.name) + '">' + content + '</div>';
        }).join('');
        if (loading) { el('stageTableBody').replaceChildren(); return; }
        if (!filtered.length) {
            ui.empty(el('stageTableBody'), 8,
                failed ? 'Chưa tải được giai đoạn' : stages.length ? 'Không tìm thấy giai đoạn' : 'Chưa có giai đoạn nào',
                failed ? 'Chọn Làm mới để thử lại.' : stages.length ? 'Thử từ khóa hoặc trạng thái khác.' : canManage ? 'Chọn Thêm giai đoạn để thiết lập quy trình bán hàng.' : 'Giai đoạn sẽ xuất hiện tại đây khi được thiết lập.');
            return;
        }
        el('stageTableBody').innerHTML = filtered.map(function (stage) {
            var index = all.indexOf(stage);
            var actions = canManage ? ui.action('edit', stage.id, 'Sửa giai đoạn ' + stage.name, 'edit')
                + ui.action('delete', stage.id, 'Xóa hoặc ngừng sử dụng ' + stage.name, 'delete') : '';
            var ordering = canManage ? ui.action('move-up', stage.id, 'Đưa lên trước', 'up', index === 0)
                + ui.action('move-down', stage.id, 'Đưa xuống sau', 'down', index === all.length - 1) : '';
            return '<tr><td><div class="crm-settings-actions"><span>' + ui.escape(order(stage)) + '</span>' + ordering + '</div></td>'
                + '<td>' + ui.escape(stage.code) + '</td><td class="crm-settings-name">' + ui.escape(stage.name) + '</td>'
                + '<td>' + ui.escape(stage.winProbability) + '%</td><td class="crm-settings-description">' + ui.escape(requirements(stage) || 'Chưa thiết lập') + '</td>'
                + '<td>' + ui.escape(opportunityCount(stage) == null ? 'Chưa có thông tin' : opportunityCount(stage)) + '</td>'
                + '<td>' + ui.status(Boolean(stage.active)) + '</td><td><div class="crm-settings-actions">' + actions + '</div></td></tr>';
        }).join('');
    }
    async function load() {
        var version = ++loadVersion;
        loading = true; failed = false; render();
        try {
            var data = ui.items(await ui.api(endpoint));
            if (version !== loadVersion) return false;
            stages = data;
        } catch (error) {
            if (version !== loadVersion) return false;
            stages = []; failed = true; ui.notice(error.message);
        } finally {
            if (version === loadVersion) { loading = false; render(); }
        }
        return !failed;
    }
    function open(id, trigger) {
        if (!canManage || changing || loading) return;
        var stage = id ? find(id) : null;
        if (id && !stage) return;
        form.reset(); ui.clearErrors(form);
        el('formStageId').value = stage ? stage.id : '';
        el('stageModalHeading').textContent = stage ? 'Sửa giai đoạn' : 'Thêm giai đoạn';
        el('formStageCode').value = stage ? stage.code : '';
        el('formStageName').value = stage ? stage.name : '';
        el('formWinProbability').value = stage ? stage.winProbability : 50;
        el('formWinProbSlider').value = el('formWinProbability').value;
        el('formExitCriteria').value = stage ? requirements(stage) : '';
        el('formStageSortOrder').value = stage ? order(stage) : Math.max(0, ...stages.map(order)) + 1;
        el('formStageActive').value = stage ? String(Boolean(stage.active)) : 'true';
        var count = stage ? opportunityCount(stage) : 0;
        el('noticeActiveDealsAlert').hidden = !(count > 0);
        if (count > 0) el('noticeActiveDealsText').textContent = 'Giai đoạn này có ' + count + ' cơ hội liên quan. Thay đổi cấu hình không xóa các cơ hội này.';
        modal.open(trigger); el('formStageCode').focus();
    }
    function valid() {
        return ui.validate(form, [
            { id:'formStageCode', feedback:'feedbackStageCode', invalid:!el('formStageCode').value.trim(), message:'Vui lòng nhập mã giai đoạn.' },
            { id:'formStageName', feedback:'feedbackStageName', invalid:!el('formStageName').value.trim(), message:'Vui lòng nhập tên giai đoạn.' },
            { id:'formWinProbability', feedback:'feedbackWinProbability', invalid:el('formWinProbability').value === '' || !Number.isInteger(Number(el('formWinProbability').value)) || Number(el('formWinProbability').value) < 0 || Number(el('formWinProbability').value) > 100, message:'Xác suất thắng phải là số nguyên từ 0 đến 100.' },
            { id:'formExitCriteria', feedback:'feedbackExitCriteria', invalid:!el('formExitCriteria').value.trim(), message:'Vui lòng nhập điều kiện chuyển bước.' },
            { id:'formStageSortOrder', feedback:'feedbackStageSortOrder', invalid:!Number.isInteger(Number(el('formStageSortOrder').value)) || Number(el('formStageSortOrder').value) <= 0, message:'Thứ tự hiển thị phải là số nguyên lớn hơn 0.' }
        ]);
    }
    function formPayload() {
        var existing = find(el('formStageId').value) || {};
        return fullPayload(existing, {
            pipelineId: Number(existing.pipelineId) || 1,
            code: el('formStageCode').value.trim().toUpperCase(),
            name: el('formStageName').value.trim(),
            stageOrder: Number(el('formStageSortOrder').value),
            winProbability: Number(el('formWinProbability').value),
            requirements: el('formExitCriteria').value.trim(),
            active: el('formStageActive').value === 'true'
        });
    }
    async function save(path, method, payload, owner, targetForm, successMessage) {
        if (!canManage || changing) return false;
        changing = true; owner.saving = true; ui.busy(targetForm, true);
        var saved = false;
        try { await ui.api(path, method, payload); saved = true; }
        catch (error) {
            var errorBox = el(targetForm.id + 'Error');
            errorBox.textContent = error.message; errorBox.hidden = false;
        } finally {
            changing = false; owner.saving = false; ui.busy(targetForm, false);
        }
        if (saved) {
            owner.close();
            if (await load()) ui.notice(successMessage, true);
            else ui.notice('Đã lưu nhưng chưa tải lại được danh sách. Chọn Làm mới để kiểm tra.');
        }
        return saved;
    }
    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (!valid()) return;
        var id = el('formStageId').value;
        save(endpoint + (id ? '/' + id : ''), id ? 'PUT' : 'POST', formPayload(), modal, form, id ? 'Đã cập nhật giai đoạn.' : 'Đã thêm giai đoạn.');
    });
    function confirmStage(id, trigger, inUse) {
        if (!canManage || changing) return;
        var stage = find(id);
        if (!stage) return;
        pending = { stage: stage, inUse: inUse == null ? opportunityCount(stage) > 0 : inUse };
        ui.clearErrors(deleteForm);
        el('dealsModalHeading').textContent = pending.inUse ? 'Ngừng sử dụng giai đoạn' : 'Xóa giai đoạn';
        el('activeDealsModalContent').textContent = pending.inUse
            ? 'Giai đoạn “' + stage.name + '” đang có dữ liệu liên quan. Bạn có muốn chuyển sang ngừng sử dụng?'
            : 'Bạn có chắc muốn xóa giai đoạn “' + stage.name + '”?';
        el('btnConfirmDeactivateStage').textContent = pending.inUse ? 'Ngừng sử dụng' : 'Xóa';
        confirmation.open(trigger);
    }
    deleteForm.addEventListener('submit', async function (event) {
        event.preventDefault();
        if (!pending || changing || !canManage) return;
        if (pending.inUse) {
            await save(endpoint + '/' + pending.stage.id, 'PUT', fullPayload(pending.stage, { active:false }), confirmation, deleteForm, 'Đã ngừng sử dụng giai đoạn.');
            return;
        }
        changing = true; confirmation.saving = true; ui.busy(deleteForm, true);
        var deleted = false;
        try { await ui.api(endpoint + '/' + pending.stage.id, 'DELETE'); deleted = true; }
        catch (error) {
            if (error.status === 409) {
                pending.inUse = true;
                el('dealsModalHeading').textContent = 'Giai đoạn đang được sử dụng';
                el('activeDealsModalContent').textContent = error.message;
                el('btnConfirmDeactivateStage').textContent = 'Ngừng sử dụng';
            } else {
                el('stageDeleteFormError').textContent = error.message;
                el('stageDeleteFormError').hidden = false;
            }
        } finally {
            changing = false; confirmation.saving = false; ui.busy(deleteForm, false);
        }
        if (deleted) {
            confirmation.close();
            if (await load()) ui.notice('Đã xóa giai đoạn.', true);
        }
    });
    async function move(id, delta) {
        if (!canManage || changing || loading) return;
        var list = sorted(), index = list.findIndex(function (stage) { return Number(stage.id) === Number(id); });
        var target = index + delta;
        if (index < 0 || target < 0 || target >= list.length) return;
        var reordered = list.slice();
        var moved = reordered.splice(index, 1)[0];
        reordered.splice(target, 0, moved);
        changing = true;
        try {
            await ui.api(endpoint + '/reorder', 'PUT', { pipelineId:Number(moved.pipelineId) || 1, stageIds:reordered.map(function (stage) { return Number(stage.id); }) });
            if (await load()) ui.notice('Đã cập nhật thứ tự giai đoạn.', true);
        } catch (error) { await load(); ui.notice(error.message); }
        finally { changing = false; }
    }
    app.addEventListener('click', function (event) {
        var button = event.target.closest('[data-action]');
        if (!button) return;
        var id = Number(button.dataset.id);
        if (button.dataset.action === 'edit') open(id, button);
        else if (button.dataset.action === 'delete') confirmStage(id, button);
        else if (button.dataset.action === 'move-up') move(id, -1);
        else if (button.dataset.action === 'move-down') move(id, 1);
    });
    el('btnOpenCreateStageModal').addEventListener('click', function (event) { open(null, event.currentTarget); });
    el('btnReloadStages').addEventListener('click', load);
    el('stageSearchInput').addEventListener('input', render);
    el('stageStatusFilter').addEventListener('change', render);
    el('formWinProbSlider').addEventListener('input', function () { el('formWinProbability').value = this.value; });
    el('formWinProbability').addEventListener('input', function () { el('formWinProbSlider').value = this.value; });
    load();
})();
