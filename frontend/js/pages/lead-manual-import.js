/* S4-02: persisted manual leads and server preview/confirm import. */
"use strict";
(() => {
  const $=id=>document.getElementById(id);
  const base=(window.CRM_BACKEND_BASE || `${location.protocol}//${location.hostname}:8080/crm`).replace(/\/+$/,"")+"/api/leads";
  const esc=v=>String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  let sources=[],leads=[],preview=null,generation=0;
  function toast(message,type="info") {
    const e=document.createElement("div");e.className="lead-toast "+type;e.setAttribute("role",type==="danger"?"alert":"status");
    e.style.cssText="padding:14px;background:var(--crm-surface,#fff);color:var(--crm-text,#172033);border:1px solid currentColor;border-radius:8px;max-width:480px";
    e.textContent=message;$("leadManualToastContainer").append(e);setTimeout(()=>e.remove(),7000);
  }
  async function api(path,options={},binary=false) {
    const r=await fetch(base+path,{...options,credentials:"include",headers:{Accept:"application/json",...(options.body && !(options.body instanceof FormData)?{"Content-Type":"application/json"}:{}),...options.headers}});
    if(binary && r.ok && r.headers.get("Content-Type")?.includes("spreadsheetml"))return r.blob();
    const json=await r.json().catch(()=>null);
    if(!r.ok || json?.success!==true)throw new Error((json?.message || `Yêu cầu thất bại (HTTP ${r.status}).`)+
      (json?.data?.errors?.length?" — "+json.data.errors.map(e=>(e.field||e.row||"")+": "+(e.message||(e.errors||[]).map(x=>x.message).join(", "))).join("; "):""));
    return json.data;
  }
  function tab(name) {for(const [key,id] of [["manual","Manual"],["excel","Excel"],["list","List"]]){$("tabBtn"+id).classList.toggle("active",key===name);$("pane"+id).style.display=key===name?"block":"none";}render();}
  function step(number) {for(let i=1;i<=3;i++){$("stepBox"+i).style.display=i===number?"block":"none";$("stepNode"+i).classList.toggle("active",i===number);$("stepNode"+i).classList.toggle("completed",i<number);}}
  const source=l=>l.sourceName||sources.find(s=>Number(s.id)===Number(l.sourceId))?.name||l.sourceCode||"";
  function render() {
    $("badgeTotalLeads").textContent=leads.length;
    $("recentManualLeadsList").innerHTML=leads.slice(0,5).map(l=>`<div class="recent-lead-item"><div class="recent-lead-info"><div class="recent-lead-name">${esc(l.fullName)}</div><div class="recent-lead-sub">${esc(l.companyName)} · ${esc(l.email||l.phone)}</div></div><span class="row-status-badge valid">${esc(source(l))}</span></div>`).join("")||"Chưa có Lead vừa thao tác.";
    const q=$("listSearchInput").value.trim().toLowerCase(),s=$("listFilterSource").value,t=$("listFilterStatus").value;
    const rows=leads.filter(l=>(!q||[l.fullName,l.email,l.phone,l.companyName].some(v=>String(v||"").toLowerCase().includes(q)))&&(s==="ALL"||String(l.sourceId)===s)&&(t==="ALL"||l.status===t));
    $("listTableBody").innerHTML=rows.map((l,i)=>`<tr><td>${i+1}</td><td>${esc(l.fullName)} #${esc(l.id)}</td><td>${esc(l.phone)}<br>${esc(l.email)}</td><td>${esc(l.companyName)}</td><td>${esc(source(l))}</td><td>${esc(l.notes)}</td><td>${esc(l.status)}</td><td>${esc(l.createdAt)}</td><td>Đã lưu</td></tr>`).join("")||'<tr><td colspan="9">Không có kết quả trong các Lead vừa thao tác.</td></tr>';
  }
  async function reload(ids) {
    const unique=[...new Set(ids.map(String))].filter(id=>/^[1-9][0-9]*$/.test(id)).slice(0,20);
    leads=await Promise.all(unique.map(id=>api("/"+id)));
    const url=new URL(location.href);if(unique.length)url.searchParams.set("leads",unique.join(","));else url.searchParams.delete("leads");history.replaceState(null,"",url);render();
  }
  async function manual(event) {
    event.preventDefault();const sourceId=Number($("manualSource").value);
    if(!sourceId){toast("Vui lòng chọn nguồn Lead.","danger");$("manualSource").focus();return;}
    const extra=[["manualTitle","Chức danh"],["manualCompanySize","Quy mô"],["manualEstimatedBudget","Ngân sách"]].filter(([id])=>$(id).value).map(([id,label])=>label+": "+$(id).value);
    const payload={fullName:$("manualFullName").value.trim(),companyName:$("manualCompany").value.trim(),email:$("manualEmail").value.trim(),phone:$("manualPhone").value.trim(),sourceId,notes:[$("manualNotes").value.trim(),...extra].filter(Boolean).join("\n")};
    $("btnSubmitManualLead").disabled=true;$("btnSubmitText").textContent="Đang lưu Lead...";
    try {
      const created=await api("",{method:"POST",body:JSON.stringify(payload)});if(!created?.id)throw new Error("Máy chủ chưa xác nhận Lead đã lưu.");
      await reload([created.id,...leads.map(l=>l.id)]);$("formManualLead").reset();toast(`Đã tạo thành công Lead "${created.fullName}" (#${created.id}).`,"success");
    }catch(e){toast(e.message,"danger");}finally{$("btnSubmitManualLead").disabled=false;$("btnSubmitText").textContent="Lưu Khách hàng tiềm năng";}
  }
  function renderPreview() {
    if(!preview)return;$("statTotalRows").textContent=preview.totalRows;$("statValidRows").textContent=preview.validCount;$("statErrorRows").textContent=preview.errorCount;
    const q=$("previewSearchInput").value.trim().toLowerCase(),mode=$("previewFilterMode").value;
    $("previewTableBody").innerHTML=[...preview.validRows,...preview.errorRows].sort((a,b)=>a.row-b.row).filter(r=>(mode==="ALL"||(mode==="VALID")===!r.errors.length)&&(!q||JSON.stringify(r.lead).toLowerCase().includes(q))).map(r=>{
      const l=r.lead||{};return `<tr><td>${r.row}</td><td>${esc(l.fullName)}</td><td>${esc(l.phone)}</td><td>${esc(l.email)}</td><td>${esc(l.companyName)}</td><td>${esc(source(l))}</td><td>${esc(l.notes)}</td><td><span class="row-status-badge ${r.errors.length?"error":"valid"}">${r.errors.length?"Có lỗi":"Hợp lệ"}</span></td><td>${r.errors.map(e=>esc(e.field+": "+e.message)).join("<br>")}</td></tr>`;
    }).join("");$("btnExecuteImport").disabled=!preview.validCount;
  }
  async function upload(file) {
    const current=++generation;preview=null;if(!file)return;
    if(!/\.xlsx$/i.test(file.name)){toast("Chỉ chấp nhận tệp Excel .xlsx.","danger");return;}
    if(!file.size||file.size>5242880){toast("Tệp phải có dữ liệu và không vượt quá 5 MiB.","danger");return;}
    $("selectedFileName").textContent=file.name;$("selectedFileSize").textContent=`${Math.ceil(file.size/1024)} KB — Đang xem trước...`;$("selectedFileCard").style.display="flex";
    const body=new FormData();body.append("file",file);
    try{const data=await api("/import/preview",{method:"POST",body});if(current!==generation)return;
      if(!data?.batchToken||!Array.isArray(data.validRows)||!Array.isArray(data.errorRows))throw new Error("Kết quả xem trước không hợp lệ.");preview=data;renderPreview();step(2);
    }catch(e){if(current===generation){$("selectedFileSize").textContent="Xem trước thất bại";toast(e.message,"danger");}}
  }
  async function confirm() {
    if(!preview?.batchToken||!preview.validCount)return;const batch=preview;$("btnExecuteImport").disabled=true;$("btnExecuteImportText").textContent="Đang nhập...";
    try{const data=await api("/import/confirm",{method:"POST",body:JSON.stringify({batchToken:batch.batchToken})});
      if(!Number.isInteger(data?.successCount)||!Array.isArray(data.leadIds))throw new Error("Máy chủ chưa xác nhận kết quả nhập.");
      preview=null;$("resSuccessCount").textContent=data.successCount;$("resSkippedCount").textContent=data.failureCount;step(3);toast(`Đã lưu ${data.successCount} Lead; ${data.failureCount} dòng có lỗi.`,"success");
      try{await reload([...data.leadIds,...leads.map(l=>l.id)]);}catch(e){toast("Đã nhập dữ liệu, nhưng chưa tải lại được chi tiết. "+e.message,"danger");}
    }catch(e){toast(e.message,"danger");}finally{$("btnExecuteImport").disabled=!preview?.validCount;$("btnExecuteImportText").textContent="Tiến hành Import Lead vào hệ thống";}
  }
  function reset(){++generation;preview=null;$("excelFileInput").value="";$("selectedFileCard").style.display="none";step(1);}
  async function template(){ $("btnDownloadExcelTemplate").disabled=true;try{const blob=await api("/import/template",{},true),url=URL.createObjectURL(blob),link=document.createElement("a");link.href=url;link.download="lead_import_template.xlsx";document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch(e){toast(e.message,"danger");}finally{$("btnDownloadExcelTemplate").disabled=false;} }
  async function init(){
    for(const [name,id] of [["manual","Manual"],["excel","Excel"],["list","List"]])$("tabBtn"+id).addEventListener("click",()=>tab(name));
    for(const id of ["btnFillDemoManualLead","btnLoadValidDemo","btnLoadInvalidDemo","btnDownloadCsvTemplate"])if($(id))$(id).hidden=true;
    $("excelFileInput").accept=".xlsx";$("manualPhone").required=false;$("manualEmail").required=false;
    $("formManualLead").addEventListener("submit",manual);$("btnDownloadExcelTemplate").addEventListener("click",template);
    $("excelFileInput").addEventListener("change",e=>upload(e.target.files[0]));
    $("dropzoneContainer").addEventListener("click",e=>{if(e.target!==$("excelFileInput"))$("excelFileInput").click();});
    $("dropzoneContainer").addEventListener("dragover",e=>e.preventDefault());$("dropzoneContainer").addEventListener("drop",e=>{e.preventDefault();upload(e.dataTransfer.files[0]);});
    for(const id of ["btnRemoveSelectedFile","btnBackToStep1","btnCancelPreview","btnImportAnotherFile"])$(id).addEventListener("click",reset);
    $("btnExecuteImport").addEventListener("click",confirm);$("btnGoToLeadsList").addEventListener("click",()=>tab("list"));
    for(const id of ["previewSearchInput","previewFilterMode"])$(id).addEventListener("input",renderPreview);
    for(const id of ["listSearchInput","listFilterSource","listFilterStatus"])$(id).addEventListener("input",render);
    $("btnResetListFilters").addEventListener("click",()=>{$("listSearchInput").value="";$("listFilterSource").value="ALL";$("listFilterStatus").value="ALL";render();});
    $("chkOnlyValid").checked=true;$("chkOnlyValid").disabled=true;$("btnSubmitManualLead").disabled=true;
    try{sources=await api("/sources");$("manualSource").innerHTML='<option value="">-- Chọn nguồn tiếp nhận --</option>'+sources.map(s=>`<option value="${esc(s.id)}">${esc(s.name)} (${esc(s.code)})</option>`).join("");
      $("listFilterSource").innerHTML='<option value="ALL">Tất cả nguồn tiếp nhận</option>'+sources.map(s=>`<option value="${esc(s.id)}">${esc(s.name)}</option>`).join("");$("btnSubmitManualLead").disabled=false;
      const ids=new URL(location.href).searchParams.get("leads");if(ids)await reload(ids.split(","));
    }catch(e){toast(e.message,"danger");}render();
  }
  if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",init);else init();
})();
