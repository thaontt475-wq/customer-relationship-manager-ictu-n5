package com.crm.service.notifications;

import com.crm.config.DatabaseConfig;
import com.crm.dao.notifications.NotificationDAO;
import com.crm.service.permissions.*;
import com.crm.service.support.SupportRequestService;
import java.sql.Connection;
import java.util.*;

public class NotificationService {
 private final NotificationDAO dao=new NotificationDAO();
 private DataScopeContext scope(long user) throws Exception {
  if(!new PermissionService().hasPermission(user,"notification.read")) throw new SecurityException("Không có quyền xem thông báo");
  return new DataScopeService().resolve(user,"customer","read");
 }
 public Map<String,Object> list(long user,int page,int size) throws Exception {
  SupportRequestService.pagination(page,size);var scope=scope(user);
  try(Connection c=DatabaseConfig.getConnection()) {
   return Map.of("items",dao.list(c,user,scope,page,size),"unreadCount",dao.unread(c,user,scope),"page",page,"size",size);
  }
 }
 public Map<String,Object> markRead(long user,long id) throws Exception {
  SupportRequestService.id(id);var scope=scope(user);
  try(Connection c=DatabaseConfig.getConnection()) {
   c.setAutoCommit(false);
   try {var result=dao.markRead(c,user,id,scope);c.commit();return result;}
   catch(Exception e){try{c.rollback();}catch(Exception rollback){e.addSuppressed(rollback);}throw e;}
  }
 }
}
