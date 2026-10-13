package com.crm.dao.support;

import com.crm.config.DatabaseConfig;
import com.crm.dto.support.SupportRequestWriteRequest;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class SupportRequestDAO {
 private static final String SELECT="""
  SELECT s.id,s.customer_id AS customerId,s.title,s.description,s.priority,
   s.assignee_user_id AS assigneeId,s.status,s.created_by AS createdBy,s.updated_by AS updatedBy,
   s.created_at AS createdAt,s.updated_at AS updatedAt
  FROM support_requests s
  """;
 public Connection open() throws SQLException {return DatabaseConfig.getConnection();}
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
 public int execute(Connection c,String sql,Object... args) throws SQLException {
  try(PreparedStatement p=c.prepareStatement(sql)) {
   for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);return p.executeUpdate();
  }
 }
 public Map<String,Object> customer(Connection c,long id,boolean lock) throws SQLException {
  var list=rows(c,"SELECT id,name,owner_user_id AS ownerUserId,support_risk_active AS riskActive,support_risk_episode AS riskEpisode,support_risk_notified_owner_id AS notifiedOwnerId FROM customers WHERE id=? AND is_deleted=0 AND status<>'DA_GOP'"+(lock?" FOR UPDATE":""),id);
  if(list.isEmpty()) throw new NoSuchElementException("Không tìm thấy Customer đang hoạt động");
  return list.getFirst();
 }
 public Map<String,Object> find(Connection c,long id,boolean lock) throws SQLException {
  var list=rows(c,SELECT+" WHERE s.id=? AND s.is_deleted=0"+(lock?" FOR UPDATE":""),id);
  if(list.isEmpty()) throw new NoSuchElementException("Không tìm thấy yêu cầu hỗ trợ");
  return list.getFirst();
 }
 public List<Map<String,Object>> list(Connection c,DataScopeContext scope,Long customer,String status,int page,int size) throws SQLException {
  StringBuilder sql=new StringBuilder(SELECT+" JOIN customers c ON c.id=s.customer_id WHERE s.is_deleted=0 AND c.is_deleted=0 AND c.status<>'DA_GOP'");
  List<Object> args=new ArrayList<>();scope.appendOwnerPredicate("c.owner_user_id",sql,args);
  if(customer!=null) {sql.append(" AND s.customer_id=?");args.add(customer);}
  if(status!=null) {sql.append(" AND s.status=?");args.add(status);}
  sql.append(" ORDER BY s.id DESC LIMIT ? OFFSET ?");args.add(size);args.add((long)(page-1)*size);
  return rows(c,sql.toString(),args.toArray());
 }
 public void validateAssignee(Connection c,long user) throws SQLException {
  if(rows(c,"SELECT id FROM users WHERE id=? AND status='ACTIVE' FOR SHARE",user).isEmpty())
   throw new IllegalArgumentException("assigneeId phải thuộc tài khoản ACTIVE");
 }
 public long create(Connection c,SupportRequestWriteRequest r,long actor) throws SQLException {
  try(PreparedStatement p=c.prepareStatement("INSERT INTO support_requests(customer_id,title,description,priority,assignee_user_id,status,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
   Object[] args={r.customerId,r.title,r.description,r.priority,r.assigneeId,r.status,actor,actor};
   for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
   if(p.executeUpdate()!=1) throw new SQLException("Support insert failed");
   try(ResultSet keys=p.getGeneratedKeys()){if(!keys.next()) throw new SQLException("No support id");return keys.getLong(1);}
  }
 }
 public void update(Connection c,long id,SupportRequestWriteRequest r,long actor) throws SQLException {
  execute(c,"UPDATE support_requests SET title=?,description=?,priority=?,assignee_user_id=?,status=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND is_deleted=0",r.title,r.description,r.priority,r.assigneeId,r.status,actor,id);
 }
 public void delete(Connection c,long id,long actor) throws SQLException {
  if(execute(c,"UPDATE support_requests SET is_deleted=1,deleted_by=?,deleted_at=CURRENT_TIMESTAMP,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND is_deleted=0",actor,actor,id)!=1)
   throw new NoSuchElementException("Không tìm thấy yêu cầu hỗ trợ");
 }
 public long openCount(Connection c,long customer) throws SQLException {
  return ((Number)rows(c,"SELECT COUNT(*) AS total FROM support_requests WHERE customer_id=? AND is_deleted=0 AND status IN ('NEW','PROCESSING','WAITING_CUSTOMER')",customer).getFirst().get("total")).longValue();
 }
 public void saveRisk(Connection c,long customer,boolean active,long episode,Long notifiedOwner) throws SQLException {
  execute(c,"UPDATE customers SET support_risk_active=?,support_risk_episode=?,support_risk_notified_owner_id=?,support_risk_updated_at=CURRENT_TIMESTAMP WHERE id=?",active,episode,notifiedOwner,customer);
 }
}
