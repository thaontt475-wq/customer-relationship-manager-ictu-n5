package com.crm.service.scoring;

import com.crm.config.DatabaseConfig;
import com.crm.dao.scoring.LeadScoringDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.scoring.*;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.service.leads.*;
import com.crm.service.permissions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL/InnoDB against connection-local tables; no application rows mutated. */
class LeadScoringJdbcTest {
    private Connection physical;
    private boolean failScore,deny;
    private final LeadScoringDAO dao=new LeadScoringDAO() {
        @Override public Connection open() {return connection();}
        @Override public void saveScore(Connection c,long id,LeadScoringEngine.Result score,long version,String hash) throws SQLException {
            super.saveScore(c,id,score,version,hash);if(failScore)throw new SQLException("Injected score failure");
        }
    };
    private Connection connection() {
        return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("close"))return null;
            try{return m.invoke(physical,a);}catch(InvocationTargetException e){throw e.getCause();}
        });
    }
    private LeadScoringService service;
    private final LeadScoringService.ScopeResolver scopes=(user,action)->{
        if(deny)throw new SecurityException("Denied");
        return new DataScopeContext(user,"lead",user==1?ScopeType.ALL:ScopeType.SELF,null,Set.of(user));
    };
    @BeforeEach void setup() throws Exception {
        assertTrue(String.valueOf(System.getenv("CRM_DB_URL")).matches("jdbc:mysql://[^/]+/crm_sprint1_test.*"),"Exact isolated test database required");
        physical=DatabaseConfig.getConnection();
        try(var s=physical.createStatement()) {
            s.execute("CREATE TEMPORARY TABLE master_data(id BIGINT PRIMARY KEY,type VARCHAR(50),code VARCHAR(50),name VARCHAR(255),active BOOLEAN) ENGINE=InnoDB");
            s.execute("INSERT INTO master_data VALUES(1,'lead-source','WEBSITE','Website',1),(2,'lead-source','EVENT','Event',1)");
            s.execute("""
                CREATE TEMPORARY TABLE leads(id BIGINT AUTO_INCREMENT PRIMARY KEY,full_name VARCHAR(150),company_name VARCHAR(255),
                email VARCHAR(255),phone VARCHAR(50),source_id BIGINT, source VARCHAR(50), source_name VARCHAR(150), notes VARCHAR(2000),status VARCHAR(30),owner_user_id BIGINT,
                created_by BIGINT,import_batch_hash CHAR(64),created_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB
                """);
            s.execute("INSERT INTO leads(id,full_name,email,source_id,owner_user_id) VALUES(1,'One','one@example.invalid',1,2),(2,'Outside','out@example.invalid',2,3)");
            s.execute("""
                CREATE TEMPORARY TABLE lead_scoring_config(id TINYINT PRIMARY KEY,version BIGINT DEFAULT 0,hot INT,warm INT,
                aggregation VARCHAR(30),min_score INT,max_score INT,rules_json JSON,updated_by BIGINT,updated_at DATETIME(6)) ENGINE=InnoDB
                """);
            s.execute("INSERT INTO lead_scoring_config(id,rules_json) VALUES(1,JSON_ARRAY())");
            s.execute("""
                CREATE TEMPORARY TABLE lead_scoring_profiles(lead_id BIGINT PRIMARY KEY,industry VARCHAR(255),employee_count INT,
                title VARCHAR(150),recent_activity VARCHAR(1000),updated_by BIGINT,updated_at DATETIME(6)) ENGINE=InnoDB
                """);
            s.execute("""
                CREATE TEMPORARY TABLE lead_scores(lead_id BIGINT PRIMARY KEY,raw_score INT,score INT,classification VARCHAR(10),
                breakdown_json JSON,config_version BIGINT,input_hash CHAR(64),scored_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)) ENGINE=InnoDB
                """);
        }
        service=new LeadScoringService(dao,scopes,user->{if(user!=1 || deny)throw new SecurityException("Admin only");});
    }
    @AfterEach void close() throws Exception {if(physical!=null)physical.close();}
    private ScoringRule rule(String id,String field,String value,int points) {return new ScoringRule(id,"CUSTOM",id,field,"CONTAINS",value,points,"",true);}
    private ScoringConfig config(long version,int hot,int warm,ScoringRule...rules) {return new ScoringConfig(version,hot,warm,"SUM_ALL",0,null,List.of(rules));}
    private long scalar(String sql) throws Exception {try(var s=physical.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    @Test void unconfiguredStateDoesNotInventZeroScoreOrClassification() throws Exception {
        assertEquals(false,service.config(2).get("configured"));var lead=service.get(2,1);
        assertNull(lead.get("score"));assertEquals("UNCONFIGURED",lead.get("classification"));assertEquals(0,scalar("SELECT COUNT(*) FROM lead_scores"));
        assertThrows(IllegalStateException.class,()->service.reScore(1));
    }
    @Test void configAndScoresPersistAndRepeatedReadsNeverAccumulate() throws Exception {
        var data=service.saveConfig(1,config(0,70,40,rule("a","source","Website",70)),false);
        assertEquals(2,data.get("reScoredCount"));assertEquals(70,scalar("SELECT score FROM lead_scores WHERE lead_id=1"));
        for(int i=0;i<3;i++)assertEquals(70,service.get(2,1).get("score"));
        service.reScore(1);assertEquals(70,scalar("SELECT score FROM lead_scores WHERE lead_id=1"));
        var renewed=new LeadScoringService(dao,scopes,user->{});assertEquals(1L,renewed.config(1).get("version"));
        assertEquals("HOT",renewed.get(2,1).get("classification"));assertInstanceOf(String.class,renewed.get(2,1).get("scoredAt"));
    }
    @Test void baseSourceAndProfileUpdatesRecalculateAndClearPreviousMatches() throws Exception {
        service.saveConfig(1,config(0,70,40,rule("a","source","Website",40),rule("b","industry","CNTT",30)),false);
        assertEquals(70,service.updateProfile(2,1,Map.of("industry","CNTT")).get("score"));
        assertEquals(70,scalar("SELECT score FROM lead_scores WHERE lead_id=1"));
        var cleared=new HashMap<String,Object>();cleared.put("industry",null);
        assertEquals(40,service.updateProfile(2,1,cleared).get("score"));
        try(var s=physical.createStatement()){s.executeUpdate("UPDATE leads SET source_id=2 WHERE id=1");}
        assertEquals(0,service.get(2,1).get("score"));assertEquals(0,scalar("SELECT score FROM lead_scores WHERE lead_id=1"));
    }
    @Test void thresholdsAndRulesReplaceAtomicallyAndStaleVersionCannotOverwrite() throws Exception {
        service.saveConfig(1,config(0,70,40,rule("a","source","Website",40)),false);
        service.saveConfig(1,config(1,40,20,rule("a","source","Website",40)),false);
        assertEquals("HOT",service.get(2,1).get("classification"));
        assertThrows(IllegalStateException.class,()->service.saveConfig(1,config(1,70,40),false));
        assertEquals(2,scalar("SELECT version FROM lead_scoring_config"));
        service.saveConfig(1,config(2,0,0),true);assertEquals(0,service.get(2,1).get("score"));
    }
    @Test void failingScoreRollsBackConfigAllScoresAndProfile() throws Exception {
        service.saveConfig(1,config(0,70,40,rule("a","source","Website",40)),false);
        failScore=true;
        assertThrows(SQLException.class,()->service.saveConfig(1,config(1,70,40,rule("b","source","Website",100)),false));
        assertEquals(1,scalar("SELECT version FROM lead_scoring_config"));assertEquals(40,scalar("SELECT score FROM lead_scores WHERE lead_id=1"));
        assertThrows(SQLException.class,()->service.updateProfile(2,1,Map.of("industry","CNTT")));
        assertEquals(0,scalar("SELECT COUNT(*) FROM lead_scoring_profiles"));assertEquals(40,scalar("SELECT score FROM lead_scores WHERE lead_id=1"));
    }
    @Test void ownerScopeAndAuthorizationCoverReadsWritesAndReScore() throws Exception {
        service.saveConfig(1,config(0,70,40,rule("a","source","Website",40)),false);
        assertEquals(1,((List<?>)service.list(2,1,100).get("items")).size());
        assertThrows(NoSuchElementException.class,()->service.get(2,2));
        assertThrows(NoSuchElementException.class,()->service.updateProfile(2,2,Map.of("industry","CNTT")));
        assertEquals(1,service.reScore(2).get("count"));
        assertThrows(SecurityException.class,()->service.saveConfig(2,config(1,70,40),false));
        deny=true;assertThrows(SecurityException.class,()->service.get(2,1));assertThrows(SecurityException.class,()->service.reScore(2));
    }
    @Test void invalidProfileAndScoreOverrideCannotWriteData() throws Exception {
        for(var patch:List.<Map<String,Object>>of(Map.of("score",999),Map.of("ownerUserId",1),Map.of("employeeCount",-1),Map.of("employeeCount",1.5),Map.of("title","x".repeat(151))))
            assertThrows(IllegalArgumentException.class,()->service.updateProfile(2,1,patch));
        assertEquals(0,scalar("SELECT COUNT(*) FROM lead_scoring_profiles"));
        assertThrows(IllegalArgumentException.class,()->service.list(1,0,100));
    }
    private LeadService baseService() {
        var baseDao=new LeadDAO(){@Override public Connection open(){return connection();}};
        return new LeadService(baseDao,(user,action)->scopes.resolve(user,action),new LeadService.ScoringHook(){
            public void beforeCreate(Connection c) throws Exception {service.beforeCreate(c);}
            public void scoreCreated(Connection c,long id) throws Exception {service.scoreCreated(c,id);}
        });
    }
    @Test void manualCreationAndBaseUpdateScoreInSameTransaction() throws Exception {
        service.saveConfig(1,config(0,70,40,rule("a","source","Website",70)),false);
        var lead=baseService().create(2,new LeadWriteRequest("New",null,"new@example.invalid",null,1L,null));
        long id=((Number)lead.get("id")).longValue();assertEquals(70,scalar("SELECT score FROM lead_scores WHERE lead_id="+id));
        baseService().update(2,id,new LeadWriteRequest("Updated",null,"new@example.invalid",null,2L,null));
        assertEquals(0,scalar("SELECT score FROM lead_scores WHERE lead_id="+id));
    }
    @Test void scoringFailureRollsBackLeadCreationAndBaseUpdate() throws Exception {
        service.saveConfig(1,config(0,70,40,rule("a","source","Website",70)),false);failScore=true;
        assertThrows(SQLException.class,()->baseService().create(2,new LeadWriteRequest("New",null,"new@example.invalid",null,1L,null)));
        assertEquals(2,scalar("SELECT COUNT(*) FROM leads"));
        assertThrows(SQLException.class,()->baseService().update(2,1,new LeadWriteRequest("Updated",null,"one@example.invalid",null,2L,null)));
        assertEquals(1,scalar("SELECT source_id FROM leads WHERE id=1"));assertEquals(70,scalar("SELECT score FROM lead_scores WHERE lead_id=1"));
    }
}
