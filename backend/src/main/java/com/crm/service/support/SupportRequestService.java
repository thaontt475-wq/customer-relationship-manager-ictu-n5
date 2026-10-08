package com.crm.service.support;

import com.crm.config.SupportRiskConfig;
import com.crm.dao.notifications.NotificationDAO;
import com.crm.dao.support.SupportRequestDAO;
import com.crm.dto.support.SupportRequestWriteRequest;
import com.crm.service.permissions.*;
import java.sql.*;
import java.util.*;

public class SupportRequestService {
 public static final Set<String> STATUSES=Set.of("NEW","PROCESSING","WAITING_CUSTOMER","RESOLVED","CLOSED");
 public static final Set<String> PRIORITIES=Set.of("URGENT","HIGH","MEDIUM","LOW");
 @FunctionalInterface public interface ScopeResolver {DataScopeContext resolve(long user,String action) throws Exception;}
 @FunctionalInterface private interface Work<T> {T run(Connection c) throws Exception;}
 private final SupportRequestDAO dao;
 private final NotificationDAO notifications;
 private final ScopeResolver scopes;
 private final int threshold;
 public SupportRequestService() {
  this(new SupportRequestDAO(),new NotificationDAO(),(user,action)->{
   if(!action.equals("risk") && !new PermissionService().hasPermission(user,"support."+action)) throw new SecurityException("Không có quyền support."+action);
   return new DataScopeService().resolve(user,"customer",Set.of("risk","read").contains(action)?"read":"update");
  },SupportRiskConfig.threshold());
 }
 public SupportRequestService(SupportRequestDAO dao,NotificationDAO notifications,ScopeResolver scopes,int threshold) {
  if(threshold<1 || threshold>10000) throw new IllegalArgumentException("Ngưỡng risk không hợp lệ");
  this.dao=dao;this.notifications=notifications;this.scopes=scopes;this.threshold=threshold;
 }
 private <T> T transaction(Work<T> work) throws Exception {
  try(Connection c=dao.open()) {
   c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);c.setAutoCommit(false);
   try {T result=work.run(c);c.commit();return result;}
   catch(Exception e) {try {c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
  }
 }
 private Map<String,Object> customer(Connection c,long id,DataScopeContext scope,boolean lock) throws SQLException {
  var row=dao.customer(c,id,lock);
  if(!scope.canAccessOwner(((Number)row.get("ownerUserId")).longValue())) throw new SecurityException("Customer nằm ngoài phạm vi SELF/TEAM/ALL");
  return row;
 }
 public static void id(Long id) {if(id==null || id<=0) throw new IllegalArgumentException("ID phải là số nguyên dương");}
 public static void pagination(int page,int size) {if(page<1 || size<1 || size>100) throw new IllegalArgumentException("page >=1, size từ 1 đến 100");}
 public List<Map<String,Object>> list(long user,Long customerId,String status,int page,int size) throws Exception {
  pagination(page,size);if(customerId!=null) id(customerId);
  if(status!=null && !STATUSES.contains(status)) throw new IllegalArgumentException("status không hợp lệ");
  var scope=scopes.resolve(user,"read");
  try(Connection c=dao.open()) {
   if(customerId!=null) customer(c,customerId,scope,false);
   return dao.list(c,scope,customerId,status,page,size);
  }
 }
 public Map<String,Object> get(long user,long id) throws Exception {
  id(id);var scope=scopes.resolve(user,"read");
  try(Connection c=dao.open()) {
   var row=dao.find(c,id,false);customer(c,((Number)row.get("customerId")).longValue(),scope,false);return row;
  }
 }
 public Map<String,Object> create(long user,SupportRequestWriteRequest request) throws Exception {
  if(request==null) throw new IllegalArgumentException("Thiếu nội dung yêu cầu hỗ trợ");
  id(request.customerId);
  if(request.priority==null) request.priority="MEDIUM";
  if(request.status==null) request.status="NEW";
  validate(request);var scope=scopes.resolve(user,"create");
  return transaction(c->{
   var customer=customer(c,request.customerId,scope,true);
   dao.validateAssignee(c,request.assigneeId);
   long ticket=dao.create(c,request,user);
   refresh(c,customer);
   return dao.find(c,ticket,false);
  });
 }
 public Map<String,Object> update(long user,long ticket,SupportRequestWriteRequest patch,boolean clearDescription) throws Exception {
  id(ticket);if(patch==null) throw new IllegalArgumentException("Thiếu nội dung cập nhật");
  var scope=scopes.resolve(user,"update");
  return transaction(c->{
   // Discover the parent first, then lock Customer before ticket to share one lock order with all support writers/merge.
   long customerId=((Number)dao.find(c,ticket,false).get("customerId")).longValue();
   var customer=customer(c,customerId,scope,true);var current=dao.find(c,ticket,true);
   if(((Number)current.get("customerId")).longValue()!=customerId) throw new IllegalStateException("Customer của ticket đã thay đổi; tải lại");
   if(patch.customerId!=null && patch.customerId!=customerId) throw new IllegalArgumentException("Không chuyển ticket sang Customer khác qua API cập nhật");
   SupportRequestWriteRequest request=new SupportRequestWriteRequest();
   request.customerId=customerId;request.title=patch.title==null?(String)current.get("title"):patch.title;
   request.description=clearDescription?null:patch.description==null?(String)current.get("description"):patch.description;
   request.priority=patch.priority==null?(String)current.get("priority"):patch.priority;
   request.status=patch.status==null?(String)current.get("status"):patch.status;
   request.assigneeId=patch.assigneeId==null?((Number)current.get("assigneeId")).longValue():patch.assigneeId;
   validate(request);dao.validateAssignee(c,request.assigneeId);dao.update(c,ticket,request,user);
   refresh(c,customer);return dao.find(c,ticket,false);
  });
 }
 public void delete(long user,long ticket) throws Exception {
  id(ticket);var scope=scopes.resolve(user,"delete");
  transaction(c->{
   long customerId=((Number)dao.find(c,ticket,false).get("customerId")).longValue();
   var customer=customer(c,customerId,scope,true);var current=dao.find(c,ticket,true);
   if(((Number)current.get("customerId")).longValue()!=customerId) throw new IllegalStateException("Customer của ticket đã thay đổi; tải lại");
   dao.delete(c,ticket,user);refresh(c,customer);return null;
  });
 }
 public Map<String,Object> risk(long user,long customerId) throws Exception {
  id(customerId);
  try {
   var scope=scopes.resolve(user,"risk");
   // Reconcile config/owner changes or moved tickets under the same parent lock.
   return transaction(c->refresh(c,customer(c,customerId,scope,true)));
  } catch(SQLException e) {
   // CustomerServlet's existing generic handler includes exception messages in JSON.
   // Never let a new support SQL driver message escape through that existing endpoint.
   throw new SQLException("Không thể tải trạng thái rủi ro Customer",e);
  }
 }
 private Map<String,Object> refresh(Connection c,Map<String,Object> customer) throws SQLException {
  long id=((Number)customer.get("id")).longValue(),owner=((Number)customer.get("ownerUserId")).longValue();
  long open=dao.openCount(c,id),episode=((Number)customer.get("riskEpisode")).longValue();
  Object old=customer.get("riskActive");
  boolean previous=Boolean.TRUE.equals(old) || old instanceof Number n && n.intValue()!=0;
  boolean active=open>=threshold;
  Object rawOwner=customer.get("notifiedOwnerId");
  Long notified=rawOwner==null?null:((Number)rawOwner).longValue();
  if(active && !previous) episode++;
  if(active && (!previous || notified==null || notified!=owner)) {
   notifications.createRisk(c,owner,id,episode,open,threshold,(String)customer.get("name"));
   notified=owner;
  }
  if(!active) notified=null;
  if(active!=previous || !Objects.equals(notified,rawOwner==null?null:((Number)rawOwner).longValue()))
   dao.saveRisk(c,id,active,episode,notified);
  Map<String,Object> result=new LinkedHashMap<>();
  result.put("customerId",id);result.put("customerName",customer.get("name"));
  result.put("isRisk",active);result.put("openTicketCount",open);result.put("riskThreshold",threshold);
  result.put("riskLevel",active?"MEDIUM":"NONE");
  result.put("reasons",active?List.of("Tồn đọng "+open+" yêu cầu hỗ trợ chưa xử lý (ngưỡng "+threshold+")"):List.of());
  return result;
 }
 public static void validate(SupportRequestWriteRequest r) {
  if(r==null || r.title==null || r.title.isBlank()) throw new IllegalArgumentException("title là bắt buộc");
  r.title=r.title.trim();if(r.title.length()>255) throw new IllegalArgumentException("title tối đa 255 ký tự");
  if(r.description!=null && r.description.length()>10000) throw new IllegalArgumentException("description tối đa 10000 ký tự");
  if(!PRIORITIES.contains(String.valueOf(r.priority))) throw new IllegalArgumentException("priority: URGENT, HIGH, MEDIUM hoặc LOW");
  if(!STATUSES.contains(String.valueOf(r.status))) throw new IllegalArgumentException("status không hợp lệ");
  id(r.assigneeId);
 }
}
