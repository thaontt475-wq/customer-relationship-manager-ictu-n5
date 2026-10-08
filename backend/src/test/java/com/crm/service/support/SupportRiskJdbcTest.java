package com.crm.service.support;

import com.crm.config.DatabaseConfig;
import com.crm.dao.support.SupportRequestDAO;
import com.crm.dao.notifications.NotificationDAO;
import com.crm.dto.support.SupportRequestWriteRequest;
import com.crm.service.permissions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real SQL tests on session-local TEMPORARY tables; no production data/schema is modified. */
class SupportRiskJdbcTest {
 private Connection connection;
 private boolean failNotification,failRisk,deny;
 private ScopeType type=ScopeType.ALL;
 private Set<Long> owners=Set.of();
 private final SupportRequestDAO dao=new SupportRequestDAO() {
  @Override public Connection open() {
   return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
    if(m.getName().equals("close")) return null;
    try{return m.invoke(connection,a);}catch(InvocationTargetException e){throw e.getCause();}
   });
  }
  @Override public void saveRisk(Connection c,long customer,boolean active,long episode,Long owner) throws SQLException {
   super.saveRisk(c,customer,active,episode,owner);
   if(failRisk) throw new SQLException("injected failure");
  }
 };
 private final NotificationDAO notifications=new NotificationDAO() {
  @Override public void createRisk(Connection c,long user,long customer,long episode,long open,int threshold,String name) throws SQLException {
   super.createRisk(c,user,customer,episode,open,threshold,name);
   if(failNotification) throw new SQLException("injected failure");
  }
 };
 private SupportRequestService service(int threshold) {
  return new SupportRequestService(dao,notifications,(user,action)->{
   if(deny) throw new SecurityException("No permission");
   return new DataScopeContext(user,"customer",type,null,owners);
  },threshold);
 }
 @BeforeEach void setup() throws Exception {
  connection=DatabaseConfig.getConnection();
  try(Statement s=connection.createStatement()) {
   s.execute("""
    CREATE TEMPORARY TABLE customers(
     id BIGINT PRIMARY KEY,name VARCHAR(255),owner_user_id BIGINT,status VARCHAR(30) DEFAULT 'CHINH_THUC',
     is_deleted TINYINT DEFAULT 0,support_risk_active TINYINT DEFAULT 0,support_risk_episode BIGINT DEFAULT 0,
     support_risk_notified_owner_id BIGINT NULL,support_risk_updated_at DATETIME NULL
    ) ENGINE=InnoDB
    """);
   s.execute("CREATE TEMPORARY TABLE users(id BIGINT PRIMARY KEY,status VARCHAR(20)) ENGINE=InnoDB");
   s.execute("""
    CREATE TEMPORARY TABLE support_requests(
     id BIGINT AUTO_INCREMENT PRIMARY KEY,customer_id BIGINT,title VARCHAR(255),description TEXT,
     priority VARCHAR(20),assignee_user_id BIGINT,status VARCHAR(30),created_by BIGINT,updated_by BIGINT,
     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
     is_deleted TINYINT DEFAULT 0,deleted_by BIGINT,deleted_at DATETIME
    ) ENGINE=InnoDB
    """);
   s.execute("""
    CREATE TEMPORARY TABLE notifications(
     id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,customer_id BIGINT,type VARCHAR(50),
     title VARCHAR(255),message VARCHAR(1000),event_key VARCHAR(150) UNIQUE,
     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,read_at DATETIME NULL
    ) ENGINE=InnoDB
    """);
   s.execute("INSERT INTO customers(id,name,owner_user_id) VALUES(1,'Customer one',10),(2,'Customer outside',20)");
   s.execute("INSERT INTO users VALUES(10,'ACTIVE'),(20,'ACTIVE'),(30,'ACTIVE'),(40,'LOCKED')");
  }
 }
 @AfterEach void close() throws Exception {if(connection!=null) connection.close();}
 private SupportRequestWriteRequest request(long customer) {
  var r=new SupportRequestWriteRequest();r.customerId=customer;r.title="Yêu cầu hỗ trợ";r.assigneeId=30L;return r;
 }
 private long id(Map<String,Object> r) {return ((Number)r.get("id")).longValue();}
 private long scalar(String sql) throws SQLException {
  try(Statement s=connection.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}
 }
 @Test void crudRiskTransitionAndEpisodesAreAtomic() throws Exception {
  var service=service(2);long first=id(service.create(10,request(1)));
  assertEquals(false,service.risk(10,1).get("isRisk"));assertEquals(0,scalar("SELECT COUNT(*) FROM notifications"));
  long second=id(service.create(10,request(1)));
  assertEquals(true,service.risk(10,1).get("isRisk"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM notifications WHERE user_id=10"));
  var patch=new SupportRequestWriteRequest();patch.priority="URGENT";
  service.update(10,first,patch,false);
  assertEquals(1,scalar("SELECT COUNT(*) FROM notifications"),"Risk updates must not spam the owner");
  patch=new SupportRequestWriteRequest();patch.status="RESOLVED";service.update(10,second,patch,false);
  assertEquals(false,service.risk(10,1).get("isRisk"));
  patch.status="PROCESSING";service.update(10,second,patch,false);
  assertEquals(2,scalar("SELECT COUNT(*) FROM notifications"),"A new risk episode creates one new notification");
  service.delete(10,second);
  assertEquals(false,service.risk(10,1).get("isRisk"));
  assertThrows(NoSuchElementException.class,()->service.get(10,second));
  assertEquals(1,service.list(10,1L,null,1,20).size());
  assertEquals(1,scalar("SELECT COUNT(*) FROM support_requests WHERE deleted_by=10 AND deleted_at IS NOT NULL"));
 }
 @Test void closedAndResolvedAreNotOpenButWaitingCustomerIsOpen() throws Exception {
  var service=service(1);
  for(String status:List.of("CLOSED","RESOLVED")) {
   var r=request(1);r.status=status;service.create(10,r);
  }
  assertEquals(0L,service.risk(10,1).get("openTicketCount"));
  var waiting=request(1);waiting.status="WAITING_CUSTOMER";service.create(10,waiting);
  assertEquals(1L,service.risk(10,1).get("openTicketCount"));
  assertEquals(true,service.risk(10,1).get("isRisk"));
 }
 @Test void failedNotificationRollsBackTicketFlagEpisodeAndNotification() throws Exception {
  var service=service(2);service.create(10,request(1));failNotification=true;
  assertThrows(SQLException.class,()->service.create(10,request(1)));
  assertEquals(1,scalar("SELECT COUNT(*) FROM support_requests"));
  assertEquals(0,scalar("SELECT support_risk_active FROM customers WHERE id=1"));
  assertEquals(0,scalar("SELECT support_risk_episode FROM customers WHERE id=1"));
  assertEquals(0,scalar("SELECT COUNT(*) FROM notifications"));
 }
 @Test void failedRiskSaveRollsBackInsertedTicketAndAlert() throws Exception {
  failRisk=true;assertThrows(SQLException.class,()->service(1).create(10,request(1)));
  assertEquals(0,scalar("SELECT COUNT(*) FROM support_requests"));
  assertEquals(0,scalar("SELECT COUNT(*) FROM notifications"));
  assertEquals(0,scalar("SELECT support_risk_active FROM customers WHERE id=1"));
 }
 @Test void currentOwnerChangesAreNotifiedOncePerEpisodeAndRecipient() throws Exception {
  var service=service(1);service.create(10,request(1));
  try(Statement s=connection.createStatement()){s.executeUpdate("UPDATE customers SET owner_user_id=20 WHERE id=1");}
  service.risk(20,1);service.risk(20,1);
  assertEquals(1,scalar("SELECT COUNT(*) FROM notifications WHERE user_id=20"));
  try(Statement s=connection.createStatement()){s.executeUpdate("UPDATE customers SET owner_user_id=10 WHERE id=1");}
  service.risk(10,1);
  assertEquals(1,scalar("SELECT COUNT(*) FROM notifications WHERE user_id=10"),"Returning to an already-notified owner must deduplicate");
 }
 @Test void notificationInboxAndReadArePrivateAndScoped() throws Exception {
  var service=service(1);service.create(10,request(1));
  DataScopeContext scope=new DataScopeContext(10,"customer",ScopeType.SELF,null,Set.of(10L));
  connection.setAutoCommit(false);
  assertEquals(1,notifications.list(connection,10,scope,1,20).size());
  assertEquals(0,notifications.list(connection,20,scope,1,20).size());
  long notification=scalar("SELECT id FROM notifications");
  assertThrows(NoSuchElementException.class,()->notifications.markRead(connection,20,notification,scope));
  notifications.markRead(connection,10,notification,scope);connection.commit();
  assertEquals(0,notifications.unread(connection,10,scope));
  assertNotNull(notifications.markRead(connection,10,notification,scope).get("readAt"));
  connection.commit();
 }
 @Test void customerScopeAndPermissionProtectReadWriteAndRisk() throws Exception {
  type=ScopeType.SELF;owners=Set.of(10L);var service=service(2);
  assertThrows(SecurityException.class,()->service.create(10,request(2)));
  assertThrows(SecurityException.class,()->service.risk(10,2));
  assertEquals(0,service.list(10,null,null,1,20).size());
  type=ScopeType.TEAM;owners=Set.of(10L,20L);
  long ticket=id(service.create(10,request(2)));
  assertNotNull(service.get(10,ticket));
  type=ScopeType.SELF;owners=Set.of(10L);
  assertThrows(SecurityException.class,()->service.get(10,ticket));
  assertThrows(SecurityException.class,()->service.delete(10,ticket));
  deny=true;assertThrows(SecurityException.class,()->service.create(10,request(1)));
  assertEquals(1,scalar("SELECT COUNT(*) FROM support_requests"));
 }
 @Test void validatesAssigneeCustomerAndImmutableCustomerLink() throws Exception {
  var service=service(2);
  var r=request(1);r.assigneeId=40L;assertThrows(IllegalArgumentException.class,()->service.create(10,r));
  assertThrows(NoSuchElementException.class,()->service.create(10,request(999)));
  long ticket=id(service.create(10,request(1)));
  var patch=new SupportRequestWriteRequest();patch.customerId=2L;
  assertThrows(IllegalArgumentException.class,()->service.update(10,ticket,patch,false));
  assertEquals(1L,service.get(10,ticket).get("customerId"));
 }
 @Test void partialUpdatesRetainOtherFieldsAndAllowClearingDescription() throws Exception {
  var service=service(2);var r=request(1);r.description="Original";
  long ticket=id(service.create(10,r));var patch=new SupportRequestWriteRequest();patch.status="PROCESSING";
  var result=service.update(10,ticket,patch,false);
  assertEquals("Original",result.get("description"));assertEquals("MEDIUM",result.get("priority"));
  assertEquals("PROCESSING",result.get("status"));
  result=service.update(10,ticket,new SupportRequestWriteRequest(),true);
  assertNull(result.get("description"));
 }
 @Test void changedThresholdReconcilesPersistedRiskWithoutDuplicateAlerts() throws Exception {
  service(3).create(10,request(1));service(3).create(10,request(1));
  assertEquals(false,service(3).risk(10,1).get("isRisk"));
  assertEquals(true,service(2).risk(10,1).get("isRisk"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM notifications"));
  assertEquals(false,service(3).risk(10,1).get("isRisk"));
 }
}
