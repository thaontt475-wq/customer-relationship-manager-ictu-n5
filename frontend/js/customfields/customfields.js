(function () {
    'use strict';
    var ui = window.CrmSettingsUI;
    if (!ui) return;
    var el=ui.el, app=ui.root, canManage=app.dataset.canManage==='true', endpoint='/api/custom-fields';
    var state={current:'CUSTOMER',data:{CUSTOMER:[],OPPORTUNITY:[]},errors:{},loading:false,changing:false,options:[],pending:null};
    var modal=ui.dialog('cfFormModal'),confirmation=ui.dialog('cfInUseNoticeModal'),form=el('cfFieldForm'),deleteForm=el('cfDeleteForm');
    var labels={CUSTOMER:'Khách hàng',OPPORTUNITY:'Cơ hội bán hàng'};
    var typeLabels={TEXT:'Văn bản',NUMBER:'Số',DATE:'Ngày',DROPDOWN:'Danh sách chọn',SELECT:'Danh sách chọn'};
    function normalize(item){return Object.assign({},item,{id:Number(item.id),fieldType:item.fieldType==='SELECT'?'DROPDOWN':item.fieldType,sortOrder:Number(item.sortOrder)||0,options:Array.isArray(item.options)?item.options:[],isRequired:item.isRequired===true||item.required===true,active:item.active!==false,inForm:item.inForm===true,inFilter:item.inFilter===true,inExport:item.inExport===true,usageCount:Number(item.usageCount)||0});}
    function current(){return state.data[state.current].slice().sort(function(a,b){return a.sortOrder-b.sortOrder||a.id-b.id;});}
    function find(id){return state.data[state.current].find(function(field){return field.id===Number(id);});}
    function payload(field,overrides){return Object.assign({entityType:field.entityType,fieldName:field.fieldName,fieldLabel:field.fieldLabel,fieldType:field.fieldType,isRequired:Boolean(field.isRequired),options:field.fieldType==='DROPDOWN'?field.options:[],sortOrder:Number(field.sortOrder)||0,active:Boolean(field.active),inForm:Boolean(field.inForm),inFilter:Boolean(field.inFilter),inExport:Boolean(field.inExport)},overrides||{});}
    function scope(field){
        var parts=[];
        if(field.inForm)parts.push('<span class="crm-badge">Biểu mẫu</span>');
        if(field.inFilter)parts.push('<span class="crm-badge">Bộ lọc</span>');
        if(field.inExport)parts.push('<span class="crm-badge">Xuất Excel</span>');
        return parts.length?'<div class="cf-scope-list">'+parts.join('')+'</div>':'<span class="crm-settings-summary">Chưa chọn phạm vi</span>';
    }
    function render(){
        ['CUSTOMER','OPPORTUNITY'].forEach(function(entity){
            el(entity==='CUSTOMER'?'badgeCustomerCount':'badgeOpportunityCount').textContent=state.errors[entity]?'!':state.data[entity].length;
            var tab=app.querySelector('[data-entity="'+entity+'"]');tab.classList.toggle('active',entity===state.current);tab.setAttribute('aria-selected',String(entity===state.current));
        });
        var keyword=el('fieldSearchInput').value.trim().toLocaleLowerCase('vi'),type=el('filterFieldType').value,required=el('filterRequired').value,status=el('filterActiveStatus').value,all=current();
        var filtered=all.filter(function(field){return(!keyword||(field.fieldName+' '+field.fieldLabel).toLocaleLowerCase('vi').includes(keyword))&&(!type||field.fieldType===type)&&(!required||String(field.isRequired)===required)&&(!status||String(field.active)===status);});
        el('cfCardTitle').textContent='Trường của '+labels[state.current];el('btnCreateText').textContent='Thêm trường cho '+labels[state.current];
        el('fieldSummary').textContent=state.loading?'Đang tải…':state.errors[state.current]?'Dữ liệu chưa sẵn sàng':filtered.length+' / '+all.length+' trường';
        el('cfLoadingOverlay').hidden=!state.loading;el('cfEmptyState').hidden=true;
        if(state.loading){el('cfTableBody').replaceChildren();return;}
        if(!filtered.length){ui.empty(el('cfTableBody'),8,state.errors[state.current]?'Chưa tải được trường tùy chỉnh':all.length?'Không tìm thấy trường':'Chưa có trường tùy chỉnh',state.errors[state.current]?'Chọn Làm mới để thử lại.':all.length?'Thử bộ lọc khác.':canManage?'Chọn nút thêm để tạo định nghĩa trường đầu tiên.':'Trường sẽ xuất hiện tại đây khi được cấu hình.');return;}
        el('cfTableBody').innerHTML=filtered.map(function(field){
            var actions=canManage?ui.action('edit',field.id,'Sửa '+field.fieldLabel,'edit')+ui.action('delete',field.id,'Xóa hoặc ngừng sử dụng '+field.fieldLabel,'delete'):'';
            return '<tr><td>'+ui.escape(field.sortOrder)+'</td><td>'+ui.escape(field.fieldName)+'</td><td class="crm-settings-name">'+ui.escape(field.fieldLabel)+(field.usageCount?'<span class="crm-settings-summary">Đang có '+ui.escape(field.usageCount)+' bản ghi dữ liệu</span>':'')+'</td><td>'+ui.escape(typeLabels[field.fieldType]||field.fieldType)+'</td><td>'+(field.isRequired?'<span class="crm-badge crm-badge-warning">Bắt buộc</span>':'<span class="crm-badge">Không bắt buộc</span>')+'</td><td>'+scope(field)+'</td><td>'+ui.status(field.active)+'</td><td><div class="crm-settings-actions">'+actions+'</div></td></tr>';
        }).join('');
    }
    async function loadAll(){
        state.loading=true;state.errors={};render();
        var entities=['CUSTOMER','OPPORTUNITY'];
        var results=await Promise.allSettled(entities.map(function(entity){return ui.api(endpoint+'?entity='+entity);}));
        results.forEach(function(result,index){var entity=entities[index];if(result.status==='fulfilled'){try{state.data[entity]=ui.items(result.value).map(normalize);}catch(error){state.data[entity]=[];state.errors[entity]=error.message;}}else{state.data[entity]=[];state.errors[entity]=result.reason.message;}});
        state.loading=false;render();if(state.errors[state.current])ui.notice(state.errors[state.current]);return Object.keys(state.errors).length===0;
    }
    function slug(value){return value.toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/[^a-z0-9]+/g,'_').replace(/^_+|_+$/g,'');}
    function renderOptions(){el('optChipsContainer').innerHTML=state.options.map(function(option,index){return '<span class="cf-option-chip">'+ui.escape(option)+'<button type="button" data-option-index="'+index+'" aria-label="Xóa lựa chọn '+ui.escape(option)+'">&times;</button></span>';}).join('');}
    function toggleOptions(){el('cfOptionsBuilderArea').hidden=el('formFieldType').value!=='DROPDOWN';}
    function addOption(){
        var value=el('newOptionInput').value.trim();if(!value)return;
        if(state.options.some(function(option){return option.toLocaleLowerCase('vi')===value.toLocaleLowerCase('vi');})){el('feedbackOptions').textContent='Giá trị này đã tồn tại.';return;}
        state.options.push(value);el('newOptionInput').value='';el('feedbackOptions').textContent='';renderOptions();el('newOptionInput').focus();
    }
    function open(id,trigger){
        if(!canManage||state.loading||state.changing)return;var field=id?find(id):null;if(id&&!field)return;
        form.reset();ui.clearErrors(form);state.options=field?field.options.slice():[];
        el('formFieldId').value=field?field.id:'';el('modalTitle').firstElementChild.textContent=field?'Sửa trường tùy chỉnh':'Thêm trường tùy chỉnh';
        el('formEntityType').value=field?field.entityType:state.current;el('formFieldLabel').value=field?field.fieldLabel:'';el('formFieldName').value=field?field.fieldName:'';
        el('formFieldType').value=field?field.fieldType:'TEXT';el('formFieldType').disabled=Boolean(field&&field.usageCount>0);el('formSortOrder').value=field?field.sortOrder:Math.max(0,...state.data[state.current].map(function(item){return item.sortOrder;}))+1;
        el('formIsRequired').checked=field?field.isRequired:false;el('formIsActive').checked=field?field.active:true;el('formInForm').checked=field?field.inForm:true;el('formInFilter').checked=field?field.inFilter:true;el('formInExport').checked=field?field.inExport:true;
        toggleOptions();renderOptions();modal.open(trigger);el('formFieldLabel').focus();
    }
    function valid(){
        var label=el('formFieldLabel').value.trim(),name=el('formFieldName').value.trim(),type=el('formFieldType').value,id=Number(el('formFieldId').value)||null,entity=el('formEntityType').value;
        return ui.validate(form,[
            {id:'formFieldLabel',feedback:'feedbackFieldLabel',invalid:!label||label.length>100,message:'Nhãn hiển thị là bắt buộc và không vượt quá 100 ký tự.'},
            {id:'formFieldName',feedback:'feedbackFieldName',invalid:!/^[a-z][a-z0-9_]*$/.test(name)||name.length>50,message:'Mã phải bắt đầu bằng chữ thường và chỉ gồm chữ, số, dấu gạch dưới.'},
            {id:'formFieldName',feedback:'feedbackFieldName',invalid:state.data[entity].some(function(item){return item.fieldName===name&&item.id!==id;}),message:'Mã trường đã tồn tại trong đối tượng này.'},
            {id:'formSortOrder',feedback:'feedbackSortOrder',invalid:!Number.isInteger(Number(el('formSortOrder').value))||Number(el('formSortOrder').value)<0,message:'Thứ tự phải là số nguyên không âm.'},
            {id:'formFieldType',feedback:'feedbackFieldType',invalid:!['TEXT','NUMBER','DATE','DROPDOWN'].includes(type),message:'Kiểu dữ liệu không hợp lệ.'},
            {id:'formFieldType',feedback:'feedbackOptions',invalid:type==='DROPDOWN'&&state.options.length===0,message:'Danh sách chọn cần ít nhất một giá trị.'}
        ]);
    }
    function formPayload(){
        var existing=find(el('formFieldId').value)||{};
        return payload(existing,{entityType:el('formEntityType').value,fieldName:el('formFieldName').value.trim(),fieldLabel:el('formFieldLabel').value.trim(),fieldType:el('formFieldType').value,isRequired:el('formIsRequired').checked,options:el('formFieldType').value==='DROPDOWN'?state.options.slice():[],sortOrder:Number(el('formSortOrder').value),active:el('formIsActive').checked,inForm:el('formInForm').checked,inFilter:el('formInFilter').checked,inExport:el('formInExport').checked});
    }
    async function save(path,method,data,owner,targetForm,message){
        if(!canManage||state.changing)return false;state.changing=true;owner.saving=true;ui.busy(targetForm,true);var saved=false;
        try{await ui.api(path,method,data);saved=true;}catch(error){var box=el(targetForm.id+'Error');box.textContent=error.message;box.hidden=false;}
        finally{state.changing=false;owner.saving=false;ui.busy(targetForm,false);}
        if(saved){owner.close();if(await loadAll())ui.notice(message,true);else ui.notice('Đã lưu nhưng chưa tải lại được danh sách. Chọn Làm mới để kiểm tra.');}return saved;
    }
    form.addEventListener('submit',function(event){event.preventDefault();if(!valid())return;var id=el('formFieldId').value;save(endpoint+(id?'/'+id:''),id?'PUT':'POST',formPayload(),modal,form,id?'Đã cập nhật trường tùy chỉnh.':'Đã thêm trường tùy chỉnh.');});
    function confirmDelete(id,trigger){
        if(!canManage||state.changing)return;var field=find(id);if(!field)return;state.pending=field;ui.clearErrors(deleteForm);
        el('inUseModalTitleText').textContent=field.usageCount>0?'Ngừng sử dụng trường':'Xóa trường tùy chỉnh';
        el('inUseModalContent').textContent=field.usageCount>0?'Trường “'+field.fieldLabel+'” đang có '+field.usageCount+' bản ghi dữ liệu. Hệ thống sẽ ngừng sử dụng trường để bảo toàn dữ liệu.':'Bạn có chắc muốn xóa trường “'+field.fieldLabel+'”?';
        el('btnConfirmDeactivate').textContent=field.usageCount>0?'Ngừng sử dụng':'Xóa';confirmation.open(trigger);
    }
    deleteForm.addEventListener('submit',async function(event){
        event.preventDefault();if(!state.pending||state.changing||!canManage)return;
        state.changing=true;confirmation.saving=true;ui.busy(deleteForm,true);var removed=false;
        try{var result=await ui.api(endpoint+'/'+state.pending.id,'DELETE');removed=true;state.pending.deleteOutcome=result&&result.outcome;}
        catch(error){el('cfDeleteFormError').textContent=error.message;el('cfDeleteFormError').hidden=false;}
        finally{state.changing=false;confirmation.saving=false;ui.busy(deleteForm,false);}
        if(removed){var deactivated=state.pending.deleteOutcome==='DEACTIVATED';confirmation.close();if(await loadAll())ui.notice(deactivated?'Trường đang có dữ liệu nên đã được chuyển sang ngừng sử dụng.':'Đã xóa trường tùy chỉnh.',true);}
    });
    app.addEventListener('click',function(event){
        var tab=event.target.closest('[data-entity]');if(tab){state.current=tab.dataset.entity;el('fieldSearchInput').value='';el('filterFieldType').value='';el('filterRequired').value='';el('filterActiveStatus').value='';render();if(state.errors[state.current])ui.notice(state.errors[state.current]);return;}
        var option=event.target.closest('[data-option-index]');if(option){state.options.splice(Number(option.dataset.optionIndex),1);renderOptions();return;}
        var button=event.target.closest('[data-action]');if(!button)return;var id=Number(button.dataset.id);if(button.dataset.action==='edit')open(id,button);else if(button.dataset.action==='delete')confirmDelete(id,button);
    });
    el('btnOpenCreateModal').addEventListener('click',function(event){open(null,event.currentTarget);});el('btnReloadFields').addEventListener('click',loadAll);
    el('btnAddOption').addEventListener('click',addOption);el('newOptionInput').addEventListener('keydown',function(event){if(event.key==='Enter'){event.preventDefault();addOption();}});
    el('formFieldType').addEventListener('change',toggleOptions);el('formFieldLabel').addEventListener('input',function(){if(!el('formFieldName').dataset.manual)el('formFieldName').value=slug(this.value);});
    el('formFieldName').addEventListener('input',function(){this.dataset.manual=this.value?'true':'';});
    ['fieldSearchInput','filterFieldType','filterRequired','filterActiveStatus'].forEach(function(id){el(id).addEventListener(id==='fieldSearchInput'?'input':'change',render);});
    el('btnResetFilter').addEventListener('click',function(){el('fieldSearchInput').value='';el('filterFieldType').value='';el('filterRequired').value='';el('filterActiveStatus').value='';render();el('fieldSearchInput').focus();});
    loadAll();
})();
