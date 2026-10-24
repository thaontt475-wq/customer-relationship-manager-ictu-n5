import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFileSync} from 'node:fs';
const source=readFileSync(new URL('../js/pages/leads.js',import.meta.url),'utf8');
const modal=source.slice(source.indexOf('    let createSources'),source.indexOf('    // 10. EXPORT TO EXCEL'));
function harness(fail=false) {
 const nodes=new Map(); const el=id=>{if(!nodes.has(id))nodes.set(id,{value:'',disabled:false,style:{},setAttribute(){},focus(){},reset(){},replaceChildren(){},append(){}});return nodes.get(id);};
 const requests=[],toasts=[]; let refresh=0;
 const context={document:{getElementById:el},window:{CrmApi:{request:async()=>[{id:10,name:'Event'}]}},Option:class{},showToast:(m,t)=>toasts.push({m,t}),LeadsService:{createLead:async p=>{requests.push(p);if(fail)throw Error('HTTP 400: sourceId không hợp lệ');return {id:42};}},refreshData:async()=>{refresh++;},currentPage:2};
 vm.createContext(context);vm.runInContext(modal+'\nglobalThis.open=openCreateLeadModal;globalThis.submit=handleCreateLeadSubmit;',context);
 return {context,el,requests,toasts,get refresh(){return refresh;}};
}
async function ready(fail) {const h=harness(fail);await h.context.open();for(const [id,v] of Object.entries({newFullName:'An',newEmail:'a@example.invalid',newSource:'10',newTitle:'CEO',newRegion:'North',newIndustry:'IT',newTier:'HOT',newNotes:'Need CRM'}))h.el(id).value=v;return h;}
test('modal sends six contract fields and preserves additional input in notes',async()=>{const h=await ready();await h.context.submit();assert.deepEqual(Object.keys(h.requests[0]).sort(),['companyName','email','fullName','notes','phone','sourceId']);assert.equal(h.requests[0].sourceId,10);for(const value of ['CEO','North','IT','HOT','Need CRM'])assert.ok(h.requests[0].notes.includes(value));assert.equal(h.refresh,1);});
test('HTTP 400 leaves modal open and shows server error',async()=>{const h=await ready(true);await h.context.submit();assert.equal(h.el('createLeadModal').style.display,'flex');assert.match(h.toasts.at(-1).m,/HTTP 400/);assert.equal(h.el('btnSubmitCreateLead').disabled,false);});
test('unknown source and invalid contacts never call create',async()=>{const h=await ready();h.el('newSource').value='999';await h.context.submit();assert.equal(h.requests.length,0);h.el('newSource').value='10';h.el('newEmail').value='bad';await h.context.submit();assert.equal(h.requests.length,0);});

test('pending and rejected statuses are not displayed as lost leads',()=>{
 const block=source.slice(source.indexOf('            // Status Badge'),source.indexOf('            // SLA Tag'));
 for(const [status,label] of [['PENDING_ASSIGNMENT','Chờ phân bổ'],['PENDING_RESPONSE','Chờ phản hồi'],['REJECTED','Đã từ chối'],['UNRECOGNIZED','Chưa xác định']]){
  const badge=vm.runInNewContext(block+'\nstatusBadge;',{lead:{status}});
  assert.ok(badge.includes(label));assert.ok(!badge.includes('Đóng / Mất'));
 }
});
