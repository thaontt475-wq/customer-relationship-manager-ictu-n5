package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class CustomerMergeDAO {
 public record Relation(String table,String column) {}
 public Connection open() throws SQLException {return DatabaseConfig.getConnection();}
 public int execute(Connection c,String sql,Object... args) throws SQLException {
  try(PreparedStatement p=c.prepareStatement(sql)) {
   for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);return p.executeUpdate();
  }
 }
 public List<Map<String,Object>> rows(Connection c,String sql,Object... args) throws SQLException {
  try(PreparedStatement p=c.prepareStatement(sql)) {
   for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
   try(ResultSet r=p.executeQuery()) {
    List<Map<String,Object>> result=new ArrayList<>();
    while(r.next()) {
     Map<String,Object> row=new LinkedHashMap<>();
     for(int i=1;i<=r.getMetaData().getColumnCount();i++) {
      Object value=r.getObject(i);
      if(value instanceof Timestamp t) value=t.toLocalDateTime().toString();
      else if(value instanceof java.time.temporal.TemporalAccessor) value=value.toString();
      row.put(r.getMetaData().getColumnLabel(i),value);
     }
     result.add(row);
    }
    return result;
   }
  }
 }
 private Map<String,Object> map(Map<String,Object> raw) {
  Map<String,Object> row=new LinkedHashMap<>();
  for(var field:Map.ofEntries(
   Map.entry("id","id"),Map.entry("name","name"),Map.entry("taxCode","tax_code"),
   Map.entry("status","status"),Map.entry("email","email"),Map.entry("phone","phone"),
   Map.entry("website","website"),Map.entry("address","address"),Map.entry("industryId","industry_id"),
   Map.entry("companySizeId","company_size_id"),Map.entry("ownerUserId","owner_user_id"),
   Map.entry("parentCustomerId","parent_customer_id"),Map.entry("mergedIntoId","merged_into_id"),
   Map.entry("mergedAt","merged_at"),Map.entry("mergedBy","merged_by"),
   Map.entry("createdAt","created_at"),Map.entry("updatedAt","updated_at"),Map.entry("isDeleted","is_deleted")).entrySet())
   row.put(field.getKey(),raw.get(field.getValue()));
  row.put("companyName",row.get("name"));return row;
 }
 public Map<String,Object> find(Connection c,long id) throws SQLException {
  var list=rows(c,"SELECT * FROM customers WHERE id=?",id);
  if(list.isEmpty()) throw new NoSuchElementException("Không tìm thấy khách hàng");
  return map(list.getFirst());
 }
 public List<Map<String,Object>> candidates(Connection c,DataScopeContext scope,int limit) throws SQLException {
  StringBuilder sql=new StringBuilder("SELECT * FROM customers c WHERE c.is_deleted=0 AND c.status<>'DA_GOP'");
  List<Object> args=new ArrayList<>();scope.appendOwnerPredicate("c.owner_user_id",sql,args);
  sql.append(" ORDER BY c.id LIMIT ?");args.add(limit);
  var raw=rows(c,sql.toString(),args.toArray());
  List<Map<String,Object>> result=new ArrayList<>();for(var row:raw) result.add(map(row));return result;
 }
 public void lockCustomers(Connection c) throws SQLException {
  try(PreparedStatement p=c.prepareStatement("SELECT id FROM customers ORDER BY id FOR UPDATE");ResultSet r=p.executeQuery()) {
   while(r.next()) { /* Ordered parent locks serialize merge and hierarchy edits. */ }
  }
 }
 public boolean eligibleRole(Connection c,long user) throws SQLException {
  return !rows(c,"SELECT r.id FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=? AND r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD') LIMIT 1",user).isEmpty();
 }
 public static String identifier(String value) {
  if(value==null || !value.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("Tên schema liên kết không được hỗ trợ");
  return Character.toString(96)+value+Character.toString(96);
 }
 public List<Relation> relations(Connection c) throws SQLException {
  var list=rows(c,"""
   SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE
   WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_SCHEMA=DATABASE()
    AND REFERENCED_TABLE_NAME='customers' AND REFERENCED_COLUMN_NAME='id'
   UNION
   SELECT cols.TABLE_NAME,cols.COLUMN_NAME FROM information_schema.COLUMNS cols
   JOIN information_schema.TABLES tbl ON tbl.TABLE_SCHEMA=cols.TABLE_SCHEMA AND tbl.TABLE_NAME=cols.TABLE_NAME
   WHERE cols.TABLE_SCHEMA=DATABASE() AND cols.COLUMN_NAME='customer_id' AND tbl.TABLE_TYPE='BASE TABLE'
   ORDER BY TABLE_NAME,COLUMN_NAME
   """);
  List<Relation> result=new ArrayList<>();
  for(var row:list) {
   String table=(String)row.get("TABLE_NAME"),column=(String)row.get("COLUMN_NAME");
   if(Set.of("customers","contacts","customer_merge_history","contact_company_history").contains(table)) continue;
   identifier(table);identifier(column);result.add(new Relation(table,column));
  }
  return result;
 }
 public void requireTransactionalTables(Connection c,List<Relation> relations) throws SQLException {
  Set<String> tables=new LinkedHashSet<>(List.of("customers","contacts","custom_field_values","audit_logs","customer_merge_history"));
  for(Relation relation:relations) tables.add(relation.table());
  for(String table:tables) {
   var info=rows(c,"SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?",table);
   if(info.size()!=1 || !"InnoDB".equalsIgnoreCase(String.valueOf(info.getFirst().get("ENGINE"))))
    throw new SQLException("Merge requires transactional InnoDB tables and S3-04 migration");
  }
 }
 public Map<String,Object> relatedCounts(Connection c,long id) throws SQLException {
  Map<String,Object> result=new LinkedHashMap<>();
  result.put("contacts",rows(c,"SELECT COUNT(*) AS total FROM contacts WHERE customer_id=?",id).getFirst().get("total"));
  for(Relation r:relations(c)) result.put(r.table()+"."+r.column(),rows(c,"SELECT COUNT(*) AS total FROM "+identifier(r.table())+" WHERE "+identifier(r.column())+"=?",id).getFirst().get("total"));
  return result;
 }
 public List<Map<String,Object>> customValues(Connection c,long customer) throws SQLException {
  return rows(c,"SELECT v.custom_field_id AS fieldId,f.field_key AS fieldKey,v.field_value AS value FROM custom_field_values v JOIN custom_fields f ON f.id=v.custom_field_id WHERE v.record_id=? AND f.entity_type='CUSTOMER' ORDER BY v.custom_field_id",customer);
 }
 public Map<String,Object> transferContacts(Connection c,long source,long target) throws SQLException {
  var primary=rows(c,"SELECT id FROM contacts WHERE customer_id IN (?,?) AND is_primary=1 AND is_deleted=0 ORDER BY (customer_id=?) DESC,id FOR UPDATE",source,target,target);
  Long kept=primary.isEmpty()?null:((Number)primary.getFirst().get("id")).longValue();
  execute(c,"UPDATE contacts SET is_primary=0 WHERE customer_id IN (?,?) AND is_primary=1",source,target);
  int moved=execute(c,"UPDATE contacts SET customer_id=? WHERE customer_id=?",target,source);
  if(kept!=null) execute(c,"UPDATE contacts SET is_primary=1 WHERE id=?",kept);
  Map<String,Object> result=new LinkedHashMap<>();result.put("contacts",moved);result.put("primaryContactId",kept);return result;
 }
 public int transferHierarchy(Connection c,long source,long target,DataScopeContext... scopes) throws SQLException {
  var children=rows(c,"SELECT id,owner_user_id FROM customers WHERE parent_customer_id=?",source);
  for(var child:children) for(var scope:scopes) if(!scope.canAccessOwner(((Number)child.get("owner_user_id")).longValue())) throw new SecurityException("Công ty con của nguồn nằm ngoài phạm vi merge");
  Set<Long> visited=new HashSet<>();Long cursor=target;boolean descendant=false;
  while(cursor!=null) {
   if(!visited.add(cursor)) throw new IllegalStateException("Phân cấp Customer hiện có bị vòng lặp");
   Object parent=find(c,cursor).get("parentCustomerId");
   cursor=parent==null?null:((Number)parent).longValue();
   if(cursor!=null && cursor==source) descendant=true;
  }
  visited.clear();cursor=source;
  while(cursor!=null) {
   if(!visited.add(cursor)) throw new IllegalStateException("Phân cấp Customer nguồn bị vòng lặp");
   Object parent=find(c,cursor).get("parentCustomerId");
   cursor=parent==null?null:((Number)parent).longValue();
  }
  if(descendant) execute(c,"UPDATE customers SET parent_customer_id=? WHERE id=?",find(c,source).get("parentCustomerId"),target);
  return execute(c,"UPDATE customers SET parent_customer_id=? WHERE parent_customer_id=? AND id<>?",target,source,target);
 }
 public int copyCustomValues(Connection c,long source,long target) throws SQLException {
  int copied=0;
  for(var field:customValues(c,source)) {
   long fieldId=((Number)field.get("fieldId")).longValue();
   if(rows(c,"SELECT custom_field_id FROM custom_field_values WHERE custom_field_id=? AND record_id=? FOR UPDATE",fieldId,target).isEmpty())
    copied+=execute(c,"INSERT INTO custom_field_values(custom_field_id,record_id,field_value) VALUES(?,?,?)",fieldId,target,field.get("value"));
  }
  return copied;
 }
 public void writeTarget(Connection c,long target,Map<String,Object> fields) throws SQLException {
  StringBuilder sql=new StringBuilder("UPDATE customers SET ");
  Map<String,String> columns=Map.ofEntries(Map.entry("name","name"),Map.entry("taxCode","tax_code"),Map.entry("status","status"),
   Map.entry("email","email"),Map.entry("phone","phone"),Map.entry("website","website"),Map.entry("address","address"),
   Map.entry("industryId","industry_id"),Map.entry("companySizeId","company_size_id"),Map.entry("ownerUserId","owner_user_id"));
  List<Object> args=new ArrayList<>();
  for(var field:fields.entrySet()) {
   if(!columns.containsKey(field.getKey())) throw new IllegalArgumentException("Trường không hỗ trợ");
   sql.append(identifier(columns.get(field.getKey()))).append("=?,");args.add(field.getValue());
  }
  sql.append("updated_at=CURRENT_TIMESTAMP WHERE id=?");args.add(target);execute(c,sql.toString(),args.toArray());
 }
 public void archive(Connection c,long source,long target,long user) throws SQLException {
  if(execute(c,"UPDATE customers SET is_deleted=1,status='DA_GOP',merged_into_id=?,merged_at=CURRENT_TIMESTAMP,merged_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND is_deleted=0 AND merged_into_id IS NULL",target,user,source)!=1)
   throw new IllegalStateException("Customer nguồn đã được xử lý bởi yêu cầu khác");
 }
 public void history(Connection c,long source,long target,long user,String beforeSource,String beforeTarget,String after,String overrides,String counts) throws SQLException {
  execute(c,"INSERT INTO customer_merge_history(source_customer_id,target_customer_id,performed_by,before_source,before_target,after_target,field_overrides,transfer_counts) VALUES(?,?,?,?,?,?,?,?)",source,target,user,beforeSource,beforeTarget,after,overrides,counts);
  execute(c,"INSERT INTO audit_logs(user_id,entity_type,entity_id,action,description,before_value,after_value) VALUES(?,'CUSTOMER',?,'MERGE',?,?,?)",user,String.valueOf(target),"Gộp Customer "+source+" vào "+target,beforeTarget,after);
 }
}
