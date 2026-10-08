package com.crm.service.customers;

import com.crm.config.DatabaseConfig;
import com.crm.dao.customers.CustomerMergeDAO;
import com.crm.dto.customers.CustomerMergeRequest;
import com.crm.service.permissions.*;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** All writes use session-local temporary tables. Production CRM rows are untouched. */
class CustomerMergeJdbcTest {
 private Connection connection;
 private String failure="";
 private boolean role=true;
 private ScopeType type=ScopeType.ALL;
 private Set<Long> owners=Set.of();
 private final List<String> linked=List.of("opportunities","activities","quotes","customer_contracts","customer_attachments","support_requests");
 private final CustomerMergeDAO dao=new CustomerMergeDAO() {
  @Override public Connection open() {
   return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
    if(m.getName().equals("close")) return null;
    try{return m.invoke(connection,a);}catch(InvocationTargetException e){throw e.getCause();}
   });
  }
  @Override public void requireTransactionalTables(Connection c,List<Relation> relations) throws SQLException {
   // SHOW CREATE TABLE can see session-local temporary tables; information_schema cannot.
   for(String table:List.of("customers","contacts","custom_field_values","audit_logs","customer_merge_history")) {
    try(Statement statement=c.createStatement();ResultSet r=statement.executeQuery("SHOW CREATE TABLE "+identifier(table))) {
     r.next();assertTrue(r.getString(2).contains("ENGINE=InnoDB"));
    }
   }
  }
  @Override public boolean eligibleRole(Connection c,long user) {return role;}
  @Override public List<Relation> relations(Connection c) {
   List<Relation> out=new ArrayList<>();for(String table:linked) out.add(new Relation(table,"customer_id"));return out;
  }
  @Override public Map<String,Object> transferContacts(Connection c,long source,long target) throws SQLException {
   var result=super.transferContacts(c,source,target);
   if(failure.equals("contacts")) throw new SQLException("injected contact failure");
   return result;
  }
  @Override public void writeTarget(Connection c,long target,Map<String,Object> fields) throws SQLException {
   super.writeTarget(c,target,fields);if(failure.equals("target")) throw new SQLException("injected write failure");
  }
  @Override public void archive(Connection c,long source,long target,long user) throws SQLException {
   super.archive(c,source,target,user);if(failure.equals("archive")) throw new SQLException("injected archive failure");
  }
  @Override public void history(Connection c,long source,long target,long user,String a,String b,String after,String overrides,String counts) throws SQLException {
   super.history(c,source,target,user,a,b,after,overrides,counts);
   if(failure.equals("history")) throw new SQLException("injected history failure");
  }
  @Override public int execute(Connection c,String sql,Object... args) throws SQLException {
   int count=super.execute(c,sql,args);
   if(failure.equals("activity") && sql.startsWith("UPDATE") && sql.contains("activities")) throw new SQLException("injected linked failure");
   return count;
  }
 };
 private CustomerMergeService service() {
  return new CustomerMergeService(dao,(user,action)->new DataScopeContext(user,"customer",type,null,owners));
 }
 @BeforeEach void setup() throws Exception {
  connection=DatabaseConfig.getConnection();
  try(Statement s=connection.createStatement()) {
   s.execute("""
    CREATE TEMPORARY TABLE customers (
     id BIGINT PRIMARY KEY,name VARCHAR(255),tax_code VARCHAR(50) UNIQUE,status VARCHAR(50),
     email VARCHAR(255),phone VARCHAR(50),website VARCHAR(255),address VARCHAR(500),
     industry_id BIGINT,company_size_id BIGINT,owner_user_id BIGINT,parent_customer_id BIGINT,
     is_deleted TINYINT DEFAULT 0,merged_into_id BIGINT,merged_at DATETIME,merged_by BIGINT,
     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB
    """);
   s.execute("""
    CREATE TEMPORARY TABLE contacts(id BIGINT PRIMARY KEY,customer_id BIGINT,is_primary TINYINT,is_deleted TINYINT DEFAULT 0,
     primary_customer_id BIGINT GENERATED ALWAYS AS(CASE WHEN is_primary=1 AND is_deleted=0 THEN customer_id ELSE NULL END) STORED,
     UNIQUE KEY uq_primary(primary_customer_id)) ENGINE=InnoDB
    """);
   for(String table:linked) {
    s.execute("CREATE TEMPORARY TABLE "+CustomerMergeDAO.identifier(table)+"(id BIGINT PRIMARY KEY,customer_id BIGINT,amount DECIMAL(18,2)) ENGINE=InnoDB");
    s.execute("INSERT INTO "+CustomerMergeDAO.identifier(table)+" VALUES(1,2,125.50),(2,1,50.25)");
   }
   s.execute("CREATE TEMPORARY TABLE custom_fields(id BIGINT PRIMARY KEY,field_key VARCHAR(50),entity_type VARCHAR(50)) ENGINE=InnoDB");
   s.execute("CREATE TEMPORARY TABLE custom_field_values(custom_field_id BIGINT,record_id BIGINT,field_value TEXT,PRIMARY KEY(custom_field_id,record_id)) ENGINE=InnoDB");
   s.execute("""
    CREATE TEMPORARY TABLE customer_merge_history(
     id BIGINT PRIMARY KEY AUTO_INCREMENT,source_customer_id BIGINT UNIQUE,target_customer_id BIGINT,performed_by BIGINT,
     before_source JSON,before_target JSON,after_target JSON,field_overrides JSON,transfer_counts JSON
    ) ENGINE=InnoDB
    """);
   s.execute("CREATE TEMPORARY TABLE audit_logs(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,entity_type VARCHAR(100),entity_id VARCHAR(100),action VARCHAR(100),description TEXT,before_value TEXT,after_value TEXT) ENGINE=InnoDB");
   s.execute("INSERT INTO customers(id,name,tax_code,status,owner_user_id,website) VALUES(1,'Công ty FPT','0123456789','CHINH_THUC',10,'https://www.fpt.vn/a'),(2,'FPT','1123456789','TIEM_NANG',10,'http://fpt.vn')");
   s.execute("INSERT INTO contacts(id,customer_id,is_primary) VALUES(1,1,1),(2,2,1),(3,2,0)");
   s.execute("INSERT INTO custom_fields VALUES(1,'shared','CUSTOMER'),(2,'source_only','CUSTOMER')");
   s.execute("INSERT INTO custom_field_values VALUES(1,1,'target'),(1,2,'source'),(2,2,'preserved')");
  }
 }
 @AfterEach void close() throws Exception {if(connection!=null) connection.close();}
 private CustomerMergeRequest request() {
  var r=new CustomerMergeRequest();r.masterId=1L;r.secondaryId=2L;return r;
 }
 private long scalar(String sql) throws SQLException {
  try(Statement s=connection.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}
 }
 @Test void mergeMovesAllRelationsPreservesAmountsPrimaryAndAudit() throws Exception {
  var r=request();r.fieldOverrides=new JsonObject();r.fieldOverrides.addProperty("taxCode","1123456789");
  var result=service().merge(10,r);assertEquals(true,result.get("merged"));
  for(String table:linked) {
   assertEquals(0,scalar("SELECT COUNT(*) FROM "+CustomerMergeDAO.identifier(table)+" WHERE customer_id=2"));
   assertEquals(17575,scalar("SELECT SUM(amount)*100 FROM "+CustomerMergeDAO.identifier(table)));
  }
  assertEquals(3,scalar("SELECT COUNT(*) FROM contacts WHERE customer_id=1"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM contacts WHERE customer_id=1 AND is_primary=1"));
  assertEquals(1,scalar("SELECT id FROM contacts WHERE is_primary=1"));
  assertEquals(1,scalar("SELECT is_deleted FROM customers WHERE id=2"));
  assertEquals(1,scalar("SELECT merged_into_id FROM customers WHERE id=2"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM customer_merge_history"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM audit_logs"));
  assertEquals(2,scalar("SELECT COUNT(*) FROM custom_field_values WHERE record_id=1"));
  assertEquals(2,scalar("SELECT COUNT(*) FROM custom_field_values WHERE record_id=2"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM customers WHERE id=1 AND tax_code='1123456789'"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM customer_merge_history WHERE JSON_UNQUOTE(JSON_EXTRACT(before_source,'$.taxCode'))='1123456789'"));
 }
 @ParameterizedTest @ValueSource(strings={"contacts","activity","target","archive","history"})
 void anyFailureRollsBackEveryStep(String at) throws Exception {
  failure=at;assertThrows(SQLException.class,()->service().merge(10,request()));
  assertEquals(0,scalar("SELECT is_deleted FROM customers WHERE id=2"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM customers WHERE id=2 AND tax_code='1123456789'"));
  assertEquals(2,scalar("SELECT COUNT(*) FROM contacts WHERE customer_id=2"));
  assertEquals(2,scalar("SELECT COUNT(*) FROM contacts WHERE is_primary=1"));
  for(String table:linked) assertEquals(1,scalar("SELECT COUNT(*) FROM "+CustomerMergeDAO.identifier(table)+" WHERE customer_id=2"));
  assertEquals(0,scalar("SELECT COUNT(*) FROM customer_merge_history"));
  assertEquals(0,scalar("SELECT COUNT(*) FROM audit_logs"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM custom_field_values WHERE record_id=1"));
 }
 @Test void rejectsNonLeaderAndOutOfScopeWithoutChanges() throws Exception {
  role=false;assertThrows(SecurityException.class,()->service().merge(10,request()));
  role=true;type=ScopeType.SELF;owners=Set.of(99L);
  assertThrows(SecurityException.class,()->service().merge(10,request()));
  assertEquals(0,scalar("SELECT is_deleted FROM customers WHERE id=2"));
  type=ScopeType.TEAM;owners=Set.of(10L);
  assertEquals(true,service().merge(10,request()).get("merged"));
 }
 @Test void rejectsSelfAlreadyMergedAndForgedOverrides() throws Exception {
  var self=request();self.secondaryId=1L;
  assertThrows(IllegalArgumentException.class,()->service().merge(10,self));
  var forged=request();forged.fieldOverrides=new JsonObject();forged.fieldOverrides.addProperty("ownerUserId",99);
  assertThrows(IllegalArgumentException.class,()->service().merge(10,forged));
  service().merge(10,request());
  assertThrows(IllegalStateException.class,()->service().merge(10,request()));
  assertEquals(1,scalar("SELECT COUNT(*) FROM customer_merge_history"));
 }
 @Test void compareAndDuplicateAreScopedReadOnly() throws Exception {
  assertNotNull(service().compare(10,1,2).get("masterRelated"));
  assertEquals(true,service().duplicates(10,1L).get("isDuplicate"));
  assertEquals(0,scalar("SELECT COUNT(*) FROM customer_merge_history"));
  assertEquals(0,scalar("SELECT is_deleted FROM customers WHERE id=2"));
  type=ScopeType.SELF;owners=Set.of(99L);
  assertThrows(SecurityException.class,()->service().compare(99,1,2));
 }
 @Test void descendantTargetHierarchyDoesNotCreateCycle() throws Exception {
  try(Statement s=connection.createStatement()) {
   s.execute("UPDATE customers SET parent_customer_id=3 WHERE id=1");
   s.execute("INSERT INTO customers(id,name,status,owner_user_id,parent_customer_id) VALUES(3,'Child','TIEM_NANG',10,2)");
  }
  service().merge(10,request());
  assertEquals(1,scalar("SELECT parent_customer_id FROM customers WHERE id=3"));
  assertEquals(1,scalar("SELECT COUNT(*) FROM customers WHERE id=1 AND parent_customer_id IS NULL"));
 }
}
