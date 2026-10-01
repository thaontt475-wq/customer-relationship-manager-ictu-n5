(function () {
    'use strict';
    var ui = window.CrmSettingsUI;
    if (!ui) return;
    var el = ui.el, app = ui.root, canManage = app.dataset.canManage === 'true';
    var endpoint = '/api/categories';
    var types = {
        INDUSTRY: { label:'Ngành nghề', create:'Thêm ngành nghề', badge:'badgeIndustry' },
        COMPANY_SIZE: { label:'Quy mô công ty', create:'Thêm quy mô công ty', badge:'badgeCompanySize' },
        LEAD_SOURCE: { label:'Nguồn khách hàng tiềm năng', create:'Thêm nguồn', badge:'badgeLeadSource' },
        ACTIVITY_TYPE: { label:'Loại hoạt động', create:'Thêm loại hoạt động', badge:'badgeActivityType' }
    };
    var state = { current:'INDUSTRY', data:{ INDUSTRY:[], COMPANY_SIZE:[], LEAD_SOURCE:[], ACTIVITY_TYPE:[] }, errors:{}, loading:false, changing:false, pending:null };
    var modal = ui.dialog('categoryModal'), confirmation = ui.dialog('inUseNoticeModal');
    var form = el('categoryForm'), deleteForm = el('categoryDeleteForm');
    function order(item) { return Number(item.displayOrder == null ? item.sortOrder : item.displayOrder); }
    function fullPayload(item, overrides) {
        return Object.assign({ type:item.type || state.current, code:item.code, name:item.name, description:item.description || '', displayOrder:order(item), active:Boolean(item.active) }, overrides || {});
    }
    function currentList() { return state.data[state.current].slice().sort(function (a,b) { return order(a)-order(b) || Number(a.id)-Number(b.id); }); }
    function find(id) { return state.data[state.current].find(function (item) { return Number(item.id) === Number(id); }); }
    function updateTabs() {
        Object.keys(types).forEach(function (type) {
            el(types[type].badge).textContent = state.errors[type] ? '!' : state.data[type].length;
            var button = app.querySelector('[data-type="' + type + '"]');
            button.classList.toggle('active', type === state.current);
            button.setAttribute('aria-selected', String(type === state.current));
        });
    }
    function render() {
        updateTabs();
        var config = types[state.current];
        var keyword = el('categorySearchInput').value.trim().toLocaleLowerCase('vi');
        var status = el('filterActiveStatus').value;
        var all = currentList();
        var filtered = all.filter(function (item) {
            return (!keyword || (String(item.code || '') + ' ' + String(item.name || '') + ' ' + String(item.description || '')).toLocaleLowerCase('vi').includes(keyword))
                && (!status || String(Boolean(item.active)) === status);
        });
        el('categoryCardTitle').textContent = config.label;
        el('btnCreateText').textContent = config.create;
        el('categorySummary').textContent = state.loading ? 'Đang tải…' : state.errors[state.current] ? 'Dữ liệu chưa sẵn sàng' : filtered.length + ' / ' + all.length + ' giá trị';
        el('configLoadingOverlay').hidden = !state.loading;
        if (state.loading) { el('categoryTableBody').replaceChildren(); return; }
        if (!filtered.length) {
            ui.empty(el('categoryTableBody'), 6,
                state.errors[state.current] ? 'Chưa tải được danh mục' : all.length ? 'Không tìm thấy giá trị' : 'Chưa có giá trị nào',
                state.errors[state.current] ? 'Chọn Làm mới để thử lại.' : all.length ? 'Thử từ khóa hoặc trạng thái khác.' : canManage ? 'Chọn nút thêm để tạo giá trị đầu tiên.' : 'Dữ liệu sẽ xuất hiện tại đây khi được thiết lập.');
            return;
        }
        el('categoryTableBody').innerHTML = filtered.map(function (item) {
            var index = all.indexOf(item);
            var ordering = canManage ? ui.action('move-up',item.id,'Đưa lên trước','up',index===0)+ui.action('move-down',item.id,'Đưa xuống sau','down',index===all.length-1) : '';
            var actions = canManage ? ui.action('edit',item.id,'Sửa '+item.name,'edit')+ui.action('delete',item.id,'Xóa hoặc ngừng sử dụng '+item.name,'delete') : '';
            return '<tr><td><div class="crm-settings-actions"><span>'+ui.escape(order(item))+'</span>'+ordering+'</div></td><td>'+ui.escape(item.code)+'</td><td class="crm-settings-name">'+ui.escape(item.name)+'</td><td class="crm-settings-description">'+ui.escape(item.description || '—')+'</td><td>'+ui.status(Boolean(item.active))+'</td><td><div class="crm-settings-actions">'+actions+'</div></td></tr>';
        }).join('');
    }
    async function loadAll() {
        state.loading=true; state.errors={}; render();
        var names=Object.keys(types);
        var results=await Promise.allSettled(names.map(function(type){return ui.api(endpoint+'?type='+encodeURIComponent(type));}));
        results.forEach(function(result,index){
            var type=names[index];
            if(result.status==='fulfilled'){
                try { state.data[type]=ui.items(result.value); }
                catch(error){state.data[type]=[];state.errors[type]=error.message;}
            } else { state.data[type]=[];state.errors[type]=result.reason.message; }
        });
        state.loading=false; render();
        if(state.errors[state.current]) ui.notice(state.errors[state.current]);
        return Object.keys(state.errors).length===0;
    }
    function switchType(type) {
        if(!types[type] || state.changing)return;
        state.current=type; el('categorySearchInput').value='';el('filterActiveStatus').value='';render();
        if(state.errors[type])ui.notice(state.errors[type]);else el('globalErrorAlert').hidden=true;
    }
    function open(id,trigger){
        if(!canManage||state.loading||state.changing)return;
        var item=id?find(id):null;if(id&&!item)return;
        form.reset();ui.clearErrors(form);
        el('formCategoryId').value=item?item.id:'';
        el('formCategoryType').value=state.current;
        el('modalHeadingText').textContent=item?'Sửa '+types[state.current].label.toLocaleLowerCase('vi'):types[state.current].create;
        el('formCategoryCode').value=item?item.code:'';
        el('formCategoryName').value=item?item.name:'';
        el('formCategorySortOrder').value=item?order(item):Math.max(0,...state.data[state.current].map(order))+1;
        el('formCategoryActive').value=item?String(Boolean(item.active)):'true';
        el('formCategoryDesc').value=item?item.description||'':'';
        modal.open(trigger);el('formCategoryCode').focus();
    }
    function valid(){
        return ui.validate(form,[
            {id:'formCategoryCode',feedback:'feedbackCategoryCode',invalid:!el('formCategoryCode').value.trim()||el('formCategoryCode').value.trim().length>50,message:'Mã danh mục là bắt buộc và không vượt quá 50 ký tự.'},
            {id:'formCategoryName',feedback:'feedbackCategoryName',invalid:!el('formCategoryName').value.trim()||el('formCategoryName').value.trim().length>255,message:'Tên danh mục là bắt buộc và không vượt quá 255 ký tự.'},
            {id:'formCategorySortOrder',feedback:'feedbackCategorySortOrder',invalid:!Number.isInteger(Number(el('formCategorySortOrder').value))||Number(el('formCategorySortOrder').value)<=0,message:'Thứ tự hiển thị phải là số nguyên lớn hơn 0.'},
            {id:'formCategoryDesc',feedback:'feedbackCategoryDesc',invalid:el('formCategoryDesc').value.trim().length>500,message:'Mô tả không được vượt quá 500 ký tự.'}
        ]);
    }
    function formPayload(){return {type:state.current,code:el('formCategoryCode').value.trim().toUpperCase(),name:el('formCategoryName').value.trim(),description:el('formCategoryDesc').value.trim(),displayOrder:Number(el('formCategorySortOrder').value),active:el('formCategoryActive').value==='true'};}
    async function save(path,method,payload,owner,targetForm,message){
        if(!canManage||state.changing)return false;
        state.changing=true;owner.saving=true;ui.busy(targetForm,true);var saved=false;
        try{await ui.api(path,method,payload);saved=true;}catch(error){var box=el(targetForm.id+'Error');box.textContent=error.message;box.hidden=false;}
        finally{state.changing=false;owner.saving=false;ui.busy(targetForm,false);}
        if(saved){owner.close();if(await loadAll())ui.notice(message,true);else ui.notice('Đã lưu nhưng chưa tải lại được đầy đủ danh mục. Chọn Làm mới để kiểm tra.');}
        return saved;
    }
    form.addEventListener('submit',function(event){event.preventDefault();if(!valid())return;var id=el('formCategoryId').value;save(endpoint+(id?'/'+id:''),id?'PUT':'POST',formPayload(),modal,form,id?'Đã cập nhật danh mục.':'Đã thêm danh mục.');});
    function confirmDelete(id,trigger,inUse){
        if(!canManage||state.changing)return;var item=find(id);if(!item)return;
        state.pending={item:item,inUse:Boolean(inUse)};ui.clearErrors(deleteForm);
        el('inUseModalHeading').textContent=state.pending.inUse?'Ngừng sử dụng danh mục':'Xóa giá trị danh mục';
        el('inUseModalContent').textContent=state.pending.inUse?'Giá trị “'+item.name+'” đang có dữ liệu liên quan. Bạn có muốn chuyển sang ngừng sử dụng?':'Bạn có chắc muốn xóa giá trị “'+item.name+'”?';
        el('btnConfirmDeactivate').textContent=state.pending.inUse?'Ngừng sử dụng':'Xóa';
        confirmation.open(trigger);
    }
    deleteForm.addEventListener('submit',async function(event){
        event.preventDefault();if(!state.pending||state.changing||!canManage)return;
        var item=state.pending.item;
        if(state.pending.inUse){await save(endpoint+'/'+item.id,'PUT',fullPayload(item,{active:false}),confirmation,deleteForm,'Đã ngừng sử dụng danh mục.');return;}
        state.changing=true;confirmation.saving=true;ui.busy(deleteForm,true);var removed=false;
        try{await ui.api(endpoint+'/'+item.id,'DELETE');removed=true;}
        catch(error){if(error.status===409){state.pending.inUse=true;el('inUseModalHeading').textContent='Danh mục đang được sử dụng';el('inUseModalContent').textContent=error.message;el('btnConfirmDeactivate').textContent='Ngừng sử dụng';}else{el('categoryDeleteFormError').textContent=error.message;el('categoryDeleteFormError').hidden=false;}}
        finally{state.changing=false;confirmation.saving=false;ui.busy(deleteForm,false);}
        if(removed){confirmation.close();if(await loadAll())ui.notice('Đã xóa danh mục.',true);}
    });
    async function move(id,delta){
        if(!canManage||state.changing||state.loading)return;var list=currentList(),index=list.findIndex(function(item){return Number(item.id)===Number(id);}),target=index+delta;if(index<0||target<0||target>=list.length)return;
        state.changing=true;
        try{await Promise.all([ui.api(endpoint+'/'+list[index].id,'PUT',fullPayload(list[index],{displayOrder:order(list[target])})),ui.api(endpoint+'/'+list[target].id,'PUT',fullPayload(list[target],{displayOrder:order(list[index])}))]);if(await loadAll())ui.notice('Đã cập nhật thứ tự danh mục.',true);}
        catch(error){await loadAll();ui.notice(error.message);}finally{state.changing=false;}
    }
    app.addEventListener('click',function(event){
        var tab=event.target.closest('[data-type]');if(tab){switchType(tab.dataset.type);return;}
        var button=event.target.closest('[data-action]');if(!button)return;var id=Number(button.dataset.id);
        if(button.dataset.action==='edit')open(id,button);else if(button.dataset.action==='delete')confirmDelete(id,button,false);else if(button.dataset.action==='move-up')move(id,-1);else if(button.dataset.action==='move-down')move(id,1);
    });
    el('btnOpenCreateCategoryModal').addEventListener('click',function(event){open(null,event.currentTarget);});
    el('btnReloadCategories').addEventListener('click',loadAll);
    el('categorySearchInput').addEventListener('input',render);el('filterActiveStatus').addEventListener('change',render);
    el('btnResetFilter').addEventListener('click',function(){el('categorySearchInput').value='';el('filterActiveStatus').value='';render();el('categorySearchInput').focus();});
    loadAll();
})();
