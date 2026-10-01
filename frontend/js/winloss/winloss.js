(function () {
    'use strict';
    var ui=window.CrmSettingsUI;if(!ui)return;
    var el=ui.el,app=ui.root,canManage=app.dataset.canManage==='true';
    var reasonEndpoint='/api/winloss/reasons',competitorEndpoint='/api/winloss/competitors';
    var state={tab:'WIN',reasons:[],competitors:[],errors:{},loading:false,changing:false,pending:null};
    var reasonModal=ui.dialog('reasonModal'),compModal=ui.dialog('competitorModal'),deleteModal=ui.dialog('winLossDeleteModal');
    var reasonForm=el('reasonForm'),compForm=el('compForm'),deleteForm=el('winLossDeleteForm');
    function sort(list){return list.slice().sort(function(a,b){return Number(a.displayOrder)-Number(b.displayOrder)||Number(a.id)-Number(b.id);});}
    function reason(id){return state.reasons.find(function(item){return Number(item.id)===Number(id);});}
    function competitor(id){return state.competitors.find(function(item){return Number(item.id)===Number(id);});}
    function safeWebsite(value){
        if(!value)return '—';
        try{var url=new URL(value);if(url.protocol==='http:'||url.protocol==='https:')return '<a href="'+ui.escape(url.href)+'" target="_blank" rel="noopener noreferrer">'+ui.escape(value)+'</a>';}catch(error){}
        return ui.escape(value);
    }
    function updateTabs(){
        [['WIN','tabBtnWin'],['LOSS','tabBtnLoss'],['COMPETITOR','tabBtnCompetitor']].forEach(function(entry){var button=el(entry[1]);button.classList.toggle('active',state.tab===entry[0]);button.setAttribute('aria-selected',String(state.tab===entry[0]));});
        el('badgeCountWin').textContent=state.errors.reasons?'!':state.reasons.filter(function(item){return item.type==='WIN';}).length;
        el('badgeCountLoss').textContent=state.errors.reasons?'!':state.reasons.filter(function(item){return item.type==='LOSS';}).length;
        el('badgeCountComp').textContent=state.errors.competitors?'!':state.competitors.length;
    }
    function renderReasons(){
        var type=state.tab==='LOSS'?'LOSS':'WIN',label=type==='WIN'?'Lý do thắng':'Lý do thua';
        var keyword=el('reasonSearchInput').value.trim().toLocaleLowerCase('vi'),status=el('reasonStatusFilter').value;
        var all=sort(state.reasons.filter(function(item){return item.type===type;}));
        var filtered=all.filter(function(item){return(!keyword||(String(item.reasonText||'')+' '+String(item.description||'')).toLocaleLowerCase('vi').includes(keyword))&&(!status||String(Boolean(item.active))===status);});
        el('reasonsCardTitle').textContent=label;el('reasonsCardSubtitle').textContent=state.loading?'Đang tải…':state.errors.reasons?'Dữ liệu chưa sẵn sàng':filtered.length+' / '+all.length+' lý do';
        if(state.loading){el('reasonsTableBody').replaceChildren();return;}
        if(!filtered.length){ui.empty(el('reasonsTableBody'),5,state.errors.reasons?'Chưa tải được danh sách':all.length?'Không tìm thấy lý do':'Chưa có '+label.toLocaleLowerCase('vi'),state.errors.reasons?'Chọn Làm mới để thử lại.':all.length?'Thử từ khóa hoặc trạng thái khác.':canManage?'Chọn Thêm lý do để tạo giá trị đầu tiên.':'Dữ liệu sẽ xuất hiện tại đây khi được cấu hình.');return;}
        el('reasonsTableBody').innerHTML=filtered.map(function(item){
            var actions=canManage?ui.action('edit-reason',item.id,'Sửa '+item.reasonText,'edit')+ui.action('delete-reason',item.id,'Xóa hoặc ngừng sử dụng '+item.reasonText,'delete'):'';
            return '<tr><td>'+ui.escape(item.displayOrder)+'</td><td class="crm-settings-name">'+ui.escape(item.reasonText)+'</td><td class="crm-settings-description">'+ui.escape(item.description||'—')+'</td><td>'+ui.status(Boolean(item.active))+'</td><td><div class="crm-settings-actions">'+actions+'</div></td></tr>';
        }).join('');
    }
    function renderCompetitors(){
        var keyword=el('competitorSearchInput').value.trim().toLocaleLowerCase('vi'),status=el('competitorStatusFilter').value,all=sort(state.competitors);
        var filtered=all.filter(function(item){return(!keyword||(String(item.name||'')+' '+String(item.strengths||'')+' '+String(item.weaknesses||'')).toLocaleLowerCase('vi').includes(keyword))&&(!status||String(Boolean(item.active))===status);});
        el('competitorSummary').textContent=state.loading?'Đang tải…':state.errors.competitors?'Dữ liệu chưa sẵn sàng':filtered.length+' / '+all.length+' đối thủ';
        if(state.loading){el('competitorsTableBody').replaceChildren();return;}
        if(!filtered.length){ui.empty(el('competitorsTableBody'),7,state.errors.competitors?'Chưa tải được danh sách':all.length?'Không tìm thấy đối thủ':'Chưa có đối thủ cạnh tranh',state.errors.competitors?'Chọn Làm mới để thử lại.':all.length?'Thử từ khóa hoặc trạng thái khác.':canManage?'Chọn Thêm đối thủ để tạo giá trị đầu tiên.':'Dữ liệu sẽ xuất hiện tại đây khi được cấu hình.');return;}
        el('competitorsTableBody').innerHTML=filtered.map(function(item){
            var actions=canManage?ui.action('edit-competitor',item.id,'Sửa '+item.name,'edit')+ui.action('delete-competitor',item.id,'Xóa hoặc ngừng sử dụng '+item.name,'delete'):'';
            return '<tr><td>'+ui.escape(item.displayOrder)+'</td><td class="crm-settings-name">'+ui.escape(item.name)+'</td><td class="crm-settings-description">'+ui.escape(item.strengths||'—')+'</td><td class="crm-settings-description">'+ui.escape(item.weaknesses||'—')+'</td><td>'+safeWebsite(item.website)+'</td><td>'+ui.status(Boolean(item.active))+'</td><td><div class="crm-settings-actions">'+actions+'</div></td></tr>';
        }).join('');
    }
    function render(){
        updateTabs();var competitors=state.tab==='COMPETITOR';el('paneReasons').hidden=competitors;el('paneCompetitors').hidden=!competitors;el('wlLoading').hidden=!state.loading;
        if(competitors)renderCompetitors();else renderReasons();
    }
    async function load(){
        state.loading=true;state.errors={};render();
        var results=await Promise.allSettled([ui.api(reasonEndpoint+'?includeInactive=true'),ui.api(competitorEndpoint+'?includeInactive=true')]);
        if(results[0].status==='fulfilled'){try{state.reasons=ui.items(results[0].value);}catch(error){state.reasons=[];state.errors.reasons=error.message;}}else{state.reasons=[];state.errors.reasons=results[0].reason.message;}
        if(results[1].status==='fulfilled'){try{state.competitors=ui.items(results[1].value);}catch(error){state.competitors=[];state.errors.competitors=error.message;}}else{state.competitors=[];state.errors.competitors=results[1].reason.message;}
        state.loading=false;render();var error=state.tab==='COMPETITOR'?state.errors.competitors:state.errors.reasons;if(error)ui.notice(error);return Object.keys(state.errors).length===0;
    }
    function openReason(id,trigger){
        if(!canManage||state.loading||state.changing)return;var item=id?reason(id):null;if(id&&!item)return;
        reasonForm.reset();ui.clearErrors(reasonForm);var type=item?item.type:(state.tab==='LOSS'?'LOSS':'WIN');
        el('reasonId').value=item?item.id:'';el('reasonType').value=type;el('reasonModalTitleText').textContent=item?'Sửa '+(type==='WIN'?'lý do thắng':'lý do thua'):'Thêm '+(type==='WIN'?'lý do thắng':'lý do thua');
        el('reasonText').value=item?item.reasonText:'';el('reasonDesc').value=item?item.description||'':'';el('reasonDisplayOrder').value=item?item.displayOrder:Math.max(0,...state.reasons.filter(function(value){return value.type===type;}).map(function(value){return Number(value.displayOrder)||0;}))+1;el('reasonActive').value=item?String(Boolean(item.active)):'true';
        reasonModal.open(trigger);el('reasonText').focus();
    }
    function openCompetitor(id,trigger){
        if(!canManage||state.loading||state.changing)return;var item=id?competitor(id):null;if(id&&!item)return;
        compForm.reset();ui.clearErrors(compForm);el('compId').value=item?item.id:'';el('compModalTitleText').textContent=item?'Sửa đối thủ cạnh tranh':'Thêm đối thủ cạnh tranh';
        el('compName').value=item?item.name:'';el('compStrengths').value=item?item.strengths||'':'';el('compWeaknesses').value=item?item.weaknesses||'':'';el('compWebsite').value=item?item.website||'':'';el('compDisplayOrder').value=item?item.displayOrder:Math.max(0,...state.competitors.map(function(value){return Number(value.displayOrder)||0;}))+1;el('compActive').value=item?String(Boolean(item.active)):'true';
        compModal.open(trigger);el('compName').focus();
    }
    function reasonValid(){return ui.validate(reasonForm,[{id:'reasonText',feedback:'feedbackReasonText',invalid:!el('reasonText').value.trim(),message:'Vui lòng nhập nội dung lý do.'},{id:'reasonDisplayOrder',feedback:'feedbackReasonOrder',invalid:!Number.isInteger(Number(el('reasonDisplayOrder').value))||Number(el('reasonDisplayOrder').value)<0,message:'Thứ tự phải là số nguyên không âm.'}]);}
    function compValid(){
        var website=el('compWebsite').value.trim(),invalidWebsite=false;if(website){try{var url=new URL(website);invalidWebsite=!['http:','https:'].includes(url.protocol);}catch(error){invalidWebsite=true;}}
        return ui.validate(compForm,[{id:'compName',feedback:'feedbackCompName',invalid:!el('compName').value.trim(),message:'Vui lòng nhập tên đối thủ.'},{id:'compWebsite',feedback:'feedbackCompWebsite',invalid:invalidWebsite,message:'Trang web phải là địa chỉ HTTP hoặc HTTPS hợp lệ.'},{id:'compDisplayOrder',feedback:'feedbackCompOrder',invalid:!Number.isInteger(Number(el('compDisplayOrder').value))||Number(el('compDisplayOrder').value)<0,message:'Thứ tự phải là số nguyên không âm.'}]);
    }
    async function save(path,method,data,owner,targetForm,message){
        if(!canManage||state.changing)return false;state.changing=true;owner.saving=true;ui.busy(targetForm,true);var saved=false;
        try{await ui.api(path,method,data);saved=true;}catch(error){var box=el(targetForm.id+'Error');box.textContent=error.message;box.hidden=false;}
        finally{state.changing=false;owner.saving=false;ui.busy(targetForm,false);}
        if(saved){owner.close();if(await load())ui.notice(message,true);else ui.notice('Đã lưu nhưng chưa tải lại được đầy đủ dữ liệu. Chọn Làm mới để kiểm tra.');}return saved;
    }
    reasonForm.addEventListener('submit',function(event){event.preventDefault();if(!reasonValid())return;var id=el('reasonId').value,data={id:id?Number(id):null,reasonText:el('reasonText').value.trim(),type:el('reasonType').value,description:el('reasonDesc').value.trim(),active:el('reasonActive').value==='true',displayOrder:Number(el('reasonDisplayOrder').value)};save(reasonEndpoint,id?'PUT':'POST',data,reasonModal,reasonForm,id?'Đã cập nhật lý do.':'Đã thêm lý do.');});
    compForm.addEventListener('submit',function(event){event.preventDefault();if(!compValid())return;var id=el('compId').value,data={id:id?Number(id):null,name:el('compName').value.trim(),strengths:el('compStrengths').value.trim(),weaknesses:el('compWeaknesses').value.trim(),website:el('compWebsite').value.trim(),active:el('compActive').value==='true',displayOrder:Number(el('compDisplayOrder').value)};save(competitorEndpoint,id?'PUT':'POST',data,compModal,compForm,id?'Đã cập nhật đối thủ.':'Đã thêm đối thủ.');});
    function confirmDelete(kind,id,trigger){
        if(!canManage||state.changing)return;var item=kind==='reason'?reason(id):competitor(id);if(!item)return;state.pending={kind:kind,item:item};ui.clearErrors(deleteForm);
        var name=kind==='reason'?item.reasonText:item.name;el('winLossDeleteTitleText').textContent=kind==='reason'?'Xóa lý do':'Xóa đối thủ';el('winLossDeleteContent').textContent='Bạn có chắc muốn xóa “'+name+'”? Nếu dữ liệu đang được tham chiếu, hệ thống sẽ chuyển sang ngừng sử dụng để bảo toàn liên kết.';deleteModal.open(trigger);
    }
    deleteForm.addEventListener('submit',async function(event){
        event.preventDefault();if(!state.pending||state.changing||!canManage)return;var path=(state.pending.kind==='reason'?reasonEndpoint:competitorEndpoint)+'?id='+encodeURIComponent(state.pending.item.id);
        state.changing=true;deleteModal.saving=true;ui.busy(deleteForm,true);var result;
        try{result=await ui.api(path,'DELETE');}catch(error){el('winLossDeleteFormError').textContent=error.message;el('winLossDeleteFormError').hidden=false;}
        finally{state.changing=false;deleteModal.saving=false;ui.busy(deleteForm,false);}
        if(result){deleteModal.close();if(await load())ui.notice(result.outcome==='DEACTIVATED'?'Dữ liệu đang được tham chiếu nên đã chuyển sang ngừng sử dụng.':'Đã xóa dữ liệu.',true);}
    });
    app.addEventListener('click',function(event){
        var tab=event.target.closest('[data-tab]');if(tab){state.tab=tab.dataset.tab;el('reasonSearchInput').value='';el('reasonStatusFilter').value='';el('competitorSearchInput').value='';el('competitorStatusFilter').value='';render();var error=state.tab==='COMPETITOR'?state.errors.competitors:state.errors.reasons;if(error)ui.notice(error);return;}
        var button=event.target.closest('[data-action]');if(!button)return;var id=Number(button.dataset.id);if(button.dataset.action==='edit-reason')openReason(id,button);else if(button.dataset.action==='delete-reason')confirmDelete('reason',id,button);else if(button.dataset.action==='edit-competitor')openCompetitor(id,button);else if(button.dataset.action==='delete-competitor')confirmDelete('competitor',id,button);
    });
    el('btnOpenAddReasonModal').addEventListener('click',function(event){openReason(null,event.currentTarget);});el('btnOpenAddCompetitorModal').addEventListener('click',function(event){openCompetitor(null,event.currentTarget);});
    el('btnReloadWinLoss').addEventListener('click',load);el('btnReloadCompetitors').addEventListener('click',load);
    el('reasonSearchInput').addEventListener('input',render);el('reasonStatusFilter').addEventListener('change',render);el('competitorSearchInput').addEventListener('input',render);el('competitorStatusFilter').addEventListener('change',render);
    load();
})();
