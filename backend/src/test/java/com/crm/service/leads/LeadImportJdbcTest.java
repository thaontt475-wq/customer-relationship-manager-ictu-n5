package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.*;
import com.crm.service.permissions.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** MySQL/InnoDB statements against connection-local tables; no application rows changed. */
class LeadImportJdbcTest {
    private Connection physical;
    private boolean failSecond;
    private int inserted;
    private LeadService leads;
    private LeadImportService imports;
    private final LeadDAO dao=new LeadDAO() {
        @Override public Connection open() {
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->{
                if(method.getName().equals("close"))return null;
                try{return method.invoke(physical,args);}catch(InvocationTargetException error){throw error.getCause();}
            });
        }
        @Override public long insert(Connection c,LeadWriteRequest lead,long user,String batch) throws SQLException {
            if(failSecond && ++inserted==2)throw new SQLException("Injected QA insert failure");
            return super.insert(c,lead,user,batch);
        }
    };
    private final LeadService.ScopeResolver scope=(user,action)->{
        if(user==99)throw new SecurityException("Forbidden");
        return new DataScopeContext(user,"lead",ScopeType.SELF,null,Set.of(user));
    };
    @BeforeEach void setup() throws Exception {
        assertTrue(String.valueOf(System.getenv("CRM_DB_URL")).contains("/crm_sprint1_test"),"Use isolated test DB");
        physical=DatabaseConfig.getConnection();
        try(var s=physical.createStatement()) {
            s.execute("CREATE TEMPORARY TABLE master_data(id BIGINT PRIMARY KEY,type VARCHAR(50),code VARCHAR(100),name VARCHAR(200),active BOOLEAN) ENGINE=InnoDB");
            s.execute("INSERT INTO master_data VALUES(10,'lead-source','EVENT','Event',1),(11,'lead-source','OTHER','Other',0),(12,'industry','IT','IT',1)");
            s.execute("CREATE TEMPORARY TABLE lead_import_batches(token_hash CHAR(64) PRIMARY KEY,owner_user_id BIGINT,total_rows INT,success_count INT,failure_count INT) ENGINE=InnoDB");
            s.execute("""
                CREATE TEMPORARY TABLE leads(id BIGINT AUTO_INCREMENT PRIMARY KEY,full_name VARCHAR(150),company_name VARCHAR(255),
                email VARCHAR(255),phone VARCHAR(50),source_id BIGINT,notes VARCHAR(2000),status VARCHAR(30),owner_user_id BIGINT,
                created_by BIGINT,import_batch_hash CHAR(64),created_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB
                """);
        }
        leads=new LeadService(dao,scope);imports=new LeadImportService(dao,leads,new LeadExcelReader(),Clock.systemUTC());
    }
    @AfterEach void close() throws Exception {if(physical!=null)physical.close();}
    private Map<String,Object> preview(String[][] rows) throws Exception {return imports.preview(1,new ByteArrayInputStream(LeadExcelReaderTest.file(rows)));}
    private String token(Map<String,Object> preview) {return (String)preview.get("batchToken");}
    private long count(String table) throws Exception {
        try(var s=physical.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}
    }
    @Test void manualCreationPersistsFieldsScopeAndSource() throws Exception {
        var lead=leads.create(1,new LeadWriteRequest("An ' SQL ","Company","an@example.invalid","0901234567",10L,"Note"));
        long id=((Number)lead.get("id")).longValue();assertEquals("An ' SQL",lead.get("fullName"));assertEquals("NEW",lead.get("status"));assertEquals(1L,lead.get("ownerUserId"));
        assertEquals("EVENT",leads.get(1,id).get("sourceCode"));assertNull(leads.get(2,id));assertEquals(1,count("leads"));
        assertInstanceOf(String.class,lead.get("createdAt"));
        assertTrue(com.crm.util.JsonUtil.getGson().toJson(lead).contains("createdAt"),"Java 21 response must serialize without reflective java.time access");
    }
    @Test void manualMissingSourceInvalidEmailInactiveAndWrongTypeRejected() throws Exception {
        for(LeadWriteRequest lead:List.of(new LeadWriteRequest("An",null,"an@example.invalid",null,null,null),
                new LeadWriteRequest("An",null,"bad",null,10L,null),new LeadWriteRequest("An",null,"a@b.invalid",null,11L,null),
                new LeadWriteRequest("An",null,"a@b.invalid",null,12L,null)))
            assertThrows(LeadValidationException.class,()->leads.create(1,lead));
        assertEquals(0,count("leads"));
    }
    @Test void previewWritesNothingConfirmImportsOnlyValidAndRepeatConflict() throws Exception {
        var preview=preview(new String[][]{{"An","","an@example.invalid","0901234567","EVENT"},{"Bình","","bad","",""}});
        assertEquals(1,preview.get("validCount"));assertEquals(1,preview.get("errorCount"));assertEquals(0,count("leads"));assertEquals(0,count("lead_import_batches"));
        var result=imports.confirm(1,token(preview));assertEquals(1,result.get("successCount"));assertEquals(1,result.get("failureCount"));
        assertEquals(1,count("leads"));assertEquals(1,count("lead_import_batches"));
        assertEquals(409,assertThrows(LeadImportService.Failure.class,()->imports.confirm(1,token(preview))).status());
        var restarted=new LeadImportService(dao,leads,new LeadExcelReader(),Clock.systemUTC());
        assertEquals(409,assertThrows(LeadImportService.Failure.class,()->restarted.confirm(1,token(preview))).status());assertEquals(1,count("leads"));
    }
    @Test void databaseFailureRollsBackAllValidRowsAndReceiptThenRetryWorks() throws Exception {
        var preview=preview(new String[][]{{"An","","an@example.invalid","","EVENT"},{"Bình","","b@b.invalid","","EVENT"}});
        failSecond=true;
        var failure=assertThrows(LeadImportService.Failure.class,()->imports.confirm(1,token(preview)));assertEquals(500,failure.status());
        assertEquals(0,((Map<?,?>)failure.data()).get("successCount"));assertEquals(0,count("leads"));assertEquals(0,count("lead_import_batches"));
        failSecond=false;assertEquals(2,imports.confirm(1,token(preview)).get("successCount"));assertEquals(2,count("leads"));
    }
    @Test void pendingAndCommittedBatchRemainBoundToOwner() throws Exception {
        var preview=preview(new String[][]{{"An","","a@b.invalid","","EVENT"}});
        assertThrows(SecurityException.class,()->imports.confirm(2,token(preview)));assertEquals(0,count("leads"));
        imports.confirm(1,token(preview));assertThrows(SecurityException.class,()->imports.confirm(2,token(preview)));
    }
    @Test void revokedSourceBetweenPreviewConfirmRollsBack() throws Exception {
        var preview=preview(new String[][]{{"An","","a@b.invalid","","EVENT"}});
        try(var s=physical.createStatement()){s.executeUpdate("UPDATE master_data SET active=0 WHERE id=10");}
        var error=assertThrows(LeadImportService.Failure.class,()->imports.confirm(1,token(preview)));assertEquals(400,error.status());
        assertEquals(0,count("leads"));assertEquals(0,count("lead_import_batches"));
    }
    @Test void permissionFailurePrecedesDatabaseWork() {
        assertThrows(SecurityException.class,()->leads.create(99,new LeadWriteRequest("An",null,"a@b.invalid",null,10L,null)));
        assertThrows(SecurityException.class,()->imports.preview(99,new ByteArrayInputStream(new byte[0])));
        assertThrows(SecurityException.class,()->imports.confirm(99,"bad"));
    }
    @Test void quotasAndExpiredTokensDoNotCreateLeads() throws Exception {
        byte[] bytes=LeadExcelReaderTest.file(new String[][]{{"An","","a@b.invalid","","EVENT"}});
        for(int i=0;i<5;i++)imports.preview(1,new ByteArrayInputStream(bytes));
        assertEquals(429,assertThrows(LeadImportService.Failure.class,()->imports.preview(1,new ByteArrayInputStream(bytes))).status());
        class MutableClock extends Clock {
            Instant now=Instant.parse("2026-10-14T00:00:00Z");
            public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
        }
        var clock=new MutableClock();var service=new LeadImportService(dao,leads,new LeadExcelReader(),clock);
        String token=token(service.preview(1,new ByteArrayInputStream(bytes)));clock.now=clock.now.plusSeconds(901);
        assertThrows(IllegalArgumentException.class,()->service.confirm(1,token));assertEquals(0,count("leads"));
    }
}
