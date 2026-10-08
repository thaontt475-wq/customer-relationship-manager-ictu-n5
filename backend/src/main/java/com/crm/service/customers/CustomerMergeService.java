package com.crm.service.customers;

import com.crm.dao.customers.CustomerMergeDAO;
import com.crm.dto.customers.CustomerMergeRequest;
import com.crm.service.permissions.*;
import com.crm.util.JsonUtil;
import com.google.gson.JsonElement;
import java.sql.*;
import java.util.*;

public class CustomerMergeService {
 @FunctionalInterface public interface ScopeResolver {DataScopeContext resolve(long user,String action) throws Exception;}
 private final CustomerMergeDAO dao;
 private final ScopeResolver scopes;
 public CustomerMergeService() {this(new CustomerMergeDAO(),(user,action)->new DataScopeService().resolve(user,"customer",action));}
 public CustomerMergeService(CustomerMergeDAO dao,ScopeResolver scopes) {this.dao=dao;this.scopes=scopes;}
 public static void id(Long id) {if(id==null || id<=0) throw new IllegalArgumentException("Customer ID phải là số nguyên dương");}
 private void accessible(Map<String,Object> row,DataScopeContext scope) {
  if(!scope.canAccessOwner(((Number)row.get("ownerUserId")).longValue())) throw new SecurityException("Customer nằm ngoài phạm vi SELF/TEAM/ALL");
  Object deleted=row.get("isDeleted");
  if(Boolean.TRUE.equals(deleted) || deleted instanceof Number n && n.intValue()!=0 || "DA_GOP".equals(row.get("status")) || row.get("mergedIntoId")!=null)
   throw new IllegalStateException("Customer đã xóa hoặc đã gộp");
 }
 public Map<String,Object> duplicates(long user,Long customerId) throws Exception {
  if(customerId!=null) id(customerId);
  var scope=scopes.resolve(user,"read");
  try(Connection c=dao.open()) {
   int maximum=customerId==null?1000:10000;
   var candidates=dao.candidates(c,scope,maximum+1);
   if(candidates.size()>maximum) throw new IllegalArgumentException("Phạm vi quét vượt "+maximum+" Customer; hãy thu hẹp phạm vi hoặc dùng customerId");
   if(customerId!=null) {
    var anchor=dao.find(c,customerId);accessible(anchor,scope);
    List<Map<String,Object>> matches=new ArrayList<>();
    for(var candidate:candidates) if(((Number)candidate.get("id")).longValue()!=customerId) {
     var match=CustomerDuplicateMatcher.match(anchor,candidate);if(match!=null) matches.add(match);
    }
    return Map.of("isDuplicate",!matches.isEmpty(),"matches",matches);
   }
   List<Map<String,Object>> groups=new ArrayList<>();
   for(int i=0;i<candidates.size();i++) {
    List<Map<String,Object>> matches=new ArrayList<>();
    for(int j=i+1;j<candidates.size();j++) {
     var match=CustomerDuplicateMatcher.match(candidates.get(i),candidates.get(j));if(match!=null) matches.add(match);
    }
    if(!matches.isEmpty()) groups.add(Map.of("record",candidates.get(i),"matches",matches));
   }
   return Map.of("items",groups,"scanned",candidates.size());
  }
 }
 public Map<String,Object> compare(long user,long master,long secondary) throws Exception {
  id(master);id(secondary);if(master==secondary) throw new IllegalArgumentException("Hai Customer phải khác nhau");
  var scope=scopes.resolve(user,"read");
  try(Connection c=dao.open()) {
   c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
   try {
    var target=dao.find(c,master);var source=dao.find(c,secondary);
    accessible(target,scope);accessible(source,scope);
    target.put("customFields",dao.customValues(c,master));source.put("customFields",dao.customValues(c,secondary));
    Map<String,Object> result=new LinkedHashMap<>();
    result.put("master",target);result.put("secondary",source);
    result.put("masterRelated",dao.relatedCounts(c,master));result.put("secondaryRelated",dao.relatedCounts(c,secondary));
    result.put("selectableFields",List.of("name","companyName","taxCode","status","email","phone","website","address","industryId","companySizeId","ownerUserId"));
    c.commit();return result;
   } catch(Exception e) {c.rollback();throw e;}
  }
 }
 public Map<String,Object> merge(long user,CustomerMergeRequest request) throws Exception {
  if(request==null) throw new IllegalArgumentException("Thiếu yêu cầu merge");
  id(request.masterId);id(request.secondaryId);
  if(request.masterId.equals(request.secondaryId)) throw new IllegalArgumentException("Không thể gộp Customer với chính nó");
  var scope=scopes.resolve(user,"merge");
  // Require ordinary edit scope as well as the dedicated merge permission.
  var update=scopes.resolve(user,"update");
  try(Connection c=dao.open()) {
   c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
   try {
    if(!dao.eligibleRole(c,user)) throw new SecurityException("Chỉ Team Lead, Director hoặc Admin được merge");
    dao.lockCustomers(c);
    long targetId=request.masterId,sourceId=request.secondaryId;
    var target=dao.find(c,targetId);var source=dao.find(c,sourceId);
    accessible(target,scope);accessible(source,scope);accessible(target,update);accessible(source,update);
    target.put("customFields",dao.customValues(c,targetId));source.put("customFields",dao.customValues(c,sourceId));
    source.put("primaryContacts",dao.rows(c,"SELECT id FROM contacts WHERE customer_id=? AND is_primary=1 AND is_deleted=0",sourceId));
    target.put("primaryContacts",dao.rows(c,"SELECT id FROM contacts WHERE customer_id=? AND is_primary=1 AND is_deleted=0",targetId));
    String beforeSource=JsonUtil.getGson().toJson(source),beforeTarget=JsonUtil.getGson().toJson(target);
    Map<String,Object> fields=selectedFields(request,source,target);
    var relations=dao.relations(c);
    dao.requireTransactionalTables(c,relations);
    Map<String,Object> transferred=new LinkedHashMap<>();
    transferred.put("companyChildren",dao.transferHierarchy(c,sourceId,targetId,scope,update));
    transferred.putAll(dao.transferContacts(c,sourceId,targetId));
    for(var relation:relations) {
     int count=dao.execute(c,"UPDATE "+CustomerMergeDAO.identifier(relation.table())+" SET "+CustomerMergeDAO.identifier(relation.column())+"=? WHERE "+CustomerMergeDAO.identifier(relation.column())+"=?",targetId,sourceId);
     transferred.put(relation.table()+"."+relation.column(),count);
    }
    transferred.put("customValuesCopied",dao.copyCustomValues(c,sourceId,targetId));
    // Release the archived source's unique tax key. Original tax remains in the history snapshot.
    dao.execute(c,"UPDATE customers SET tax_code=NULL WHERE id=?",sourceId);
    dao.writeTarget(c,targetId,fields);
    dao.archive(c,sourceId,targetId,user);
    var after=dao.find(c,targetId);after.put("customFields",dao.customValues(c,targetId));
    dao.history(c,sourceId,targetId,user,beforeSource,beforeTarget,JsonUtil.getGson().toJson(after),JsonUtil.getGson().toJson(fields),JsonUtil.getGson().toJson(transferred));
    c.commit();
    return Map.of("masterId",targetId,"secondaryId",sourceId,"master",after,"transferred",transferred,"merged",true);
   } catch(Exception e) {try {c.rollback();}catch(SQLException rollback) {e.addSuppressed(rollback);}throw e;}
  }
 }
 private Map<String,Object> selectedFields(CustomerMergeRequest request,Map<String,Object> source,Map<String,Object> target) {
  Map<String,Object> chosen=new LinkedHashMap<>();
  if(request.fieldOverrides==null) return chosen;
  Set<String> allowed=Set.of("name","companyName","taxCode","status","email","phone","website","address","industryId","companySizeId","ownerUserId");
  for(var entry:request.fieldOverrides.entrySet()) {
   String key=entry.getKey();if(!allowed.contains(key)) throw new IllegalArgumentException("Trường không cho phép ưu tiên: "+key);
   String actual=key.equals("companyName")?"name":key;
   JsonElement proposed=entry.getValue();
   JsonElement originalSource=JsonUtil.getGson().toJsonTree(source.get(actual)),originalTarget=JsonUtil.getGson().toJsonTree(target.get(actual));
   if(!proposed.equals(originalSource) && !proposed.equals(originalTarget)) throw new IllegalArgumentException("Giá trị "+key+" phải lấy từ một trong hai Customer hiện tại");
   Object value=proposed.equals(originalSource)?source.get(actual):target.get(actual);
   if(chosen.containsKey(actual) && !Objects.equals(chosen.get(actual),value)) throw new IllegalArgumentException("name và companyName mâu thuẫn");
   if(actual.equals("name") && (value==null || value.toString().isBlank())) throw new IllegalArgumentException("Tên Customer chính không được rỗng");
   if(actual.equals("status") && "DA_GOP".equals(value)) throw new IllegalArgumentException("Không dùng trạng thái đã gộp cho Customer chính");
   chosen.put(actual,value);
  }
  return chosen;
 }
}
