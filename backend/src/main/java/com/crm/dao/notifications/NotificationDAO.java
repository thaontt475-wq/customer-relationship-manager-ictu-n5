package com.crm.dao.notifications;

import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class NotificationDAO {
 private static final String SELECT="""
  SELECT n.id,n.customer_id AS customerId,n.type,n.title,n.message,
   n.created_at AS createdAt,n.read_at AS readAt
  FROM notifications n JOIN customers c ON c.id=n.customer_id
  """;
 private List<Map<String,Object>> rows(Connection c,String sql,List<Object> args) throws SQLException {
  try(PreparedStatement p=c.prepareStatement(sql)) {
   for(int i=0;i<args.size();i++) p.setObject(i+1,args.get(i));
   try(ResultSet r=p.executeQuery()) {
    List<Map<String,Object>> rows=new ArrayList<>();
    while(r.next()) {
     Map<String,Object> row=new LinkedHashMap<>();
     for(int i=1;i<=r.getMetaData().getColumnCount();i++) {
      Object value=r.getObject(i);
      if(value instanceof Timestamp t) value=t.toLocalDateTime().toString();
      else if(value instanceof java.time.temporal.TemporalAccessor) value=value.toString();
      row.put(r.getMetaData().getColumnLabel(i),value);
     }
     rows.add(row);
    }
    return rows;
   }
  }
 }
 public void createRisk(Connection c,long recipient,long customer,long episode,long open,int threshold,String name) throws SQLException {
  String title="Customer có rủi ro hỗ trợ";
  String message="Customer "+name+" có "+open+" yêu cầu hỗ trợ chưa xử lý (ngưỡng "+threshold+").";
  if(message.length()>1000) message=message.substring(0,1000);
  String key="SUPPORT_RISK:"+customer+":"+episode+":"+recipient;
  try(PreparedStatement p=c.prepareStatement("INSERT INTO notifications(user_id,customer_id,type,title,message,event_key) VALUES(?,?,'SUPPORT_RISK',?,?,?) ON DUPLICATE KEY UPDATE id=id")) {
   p.setLong(1,recipient);p.setLong(2,customer);p.setString(3,title);p.setString(4,message);p.setString(5,key);
   p.executeUpdate();
  }
 }
 private void ownerScope(DataScopeContext scope,StringBuilder sql,List<Object> args,long user) {
  sql.append(" WHERE n.user_id=? AND c.is_deleted=0 AND c.status<>'DA_GOP'");args.add(user);
  scope.appendOwnerPredicate("c.owner_user_id",sql,args);
 }
 public List<Map<String,Object>> list(Connection c,long user,DataScopeContext scope,int page,int size) throws SQLException {
  StringBuilder sql=new StringBuilder(SELECT);List<Object> args=new ArrayList<>();ownerScope(scope,sql,args,user);
  sql.append(" ORDER BY n.id DESC LIMIT ? OFFSET ?");args.add(size);args.add((long)(page-1)*size);
  return rows(c,sql.toString(),args);
 }
 public long unread(Connection c,long user,DataScopeContext scope) throws SQLException {
  StringBuilder sql=new StringBuilder("SELECT COUNT(*) AS total FROM notifications n JOIN customers c ON c.id=n.customer_id");
  List<Object> args=new ArrayList<>();ownerScope(scope,sql,args,user);sql.append(" AND n.read_at IS NULL");
  return ((Number)rows(c,sql.toString(),args).getFirst().get("total")).longValue();
 }
 public Map<String,Object> markRead(Connection c,long user,long id,DataScopeContext scope) throws SQLException {
  StringBuilder sql=new StringBuilder(SELECT);List<Object> args=new ArrayList<>();ownerScope(scope,sql,args,user);
  sql.append(" AND n.id=? FOR UPDATE");args.add(id);
  if(rows(c,sql.toString(),args).isEmpty()) throw new NoSuchElementException("Không tìm thấy thông báo thuộc người dùng");
  try(PreparedStatement p=c.prepareStatement("UPDATE notifications SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=? AND user_id=?")) {
   p.setLong(1,id);p.setLong(2,user);p.executeUpdate();
  }
  sql.setLength(sql.length()-" FOR UPDATE".length());
  return rows(c,sql.toString(),args).getFirst();
 }
}
