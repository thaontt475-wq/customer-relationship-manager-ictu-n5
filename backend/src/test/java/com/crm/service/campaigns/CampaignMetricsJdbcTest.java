package com.crm.service.campaigns;

import com.crm.config.DatabaseConfig;
import com.crm.dao.campaigns.CampaignDAO;
import com.crm.dao.campaigns.CampaignOpportunityDAO;
import com.crm.service.permissions.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CampaignMetricsJdbcTest {
    Connection physical;
    Connection borrowed() {
        return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("close"))return null;
            try{return m.invoke(physical,a);}catch(InvocationTargetException e){throw e.getCause();}
        });
    }
    final CampaignDAO campaigns=new CampaignDAO(){@Override protected Connection open(){return borrowed();}};
    final CampaignOpportunityDAO links=new CampaignOpportunityDAO(){@Override protected Connection open(){return borrowed();}};
    DataScopeContext all=new DataScopeContext(1,"campaign",ScopeType.ALL,null,Set.of());
    DataScopeContext self=new DataScopeContext(1,"campaign",ScopeType.SELF,1L,Set.of(1L));
    @BeforeEach void setup() throws Exception {
        assertTrue(String.valueOf(System.getenv("CRM_DB_URL")).contains("/crm_sprint1_test"),"Isolated test schema required");
        physical=DatabaseConfig.getConnection();
        try(var s=physical.createStatement()) {
            s.execute("CREATE TEMPORARY TABLE users(id BIGINT PRIMARY KEY,full_name VARCHAR(255)) ENGINE=InnoDB");
            s.execute("INSERT INTO users VALUES(1,'One'),(2,'Other')");
            s.execute("CREATE TEMPORARY TABLE campaigns(id BIGINT PRIMARY KEY,code VARCHAR(50),name VARCHAR(255),type VARCHAR(50),status VARCHAR(50),start_date DATE,end_date DATE,budget DECIMAL(18,2) DEFAULT 0,actual_cost DECIMAL(18,2) DEFAULT 0,expected_revenue DECIMAL(18,2) DEFAULT 0,description TEXT,owner_id BIGINT,is_deleted BOOLEAN DEFAULT 0,created_at DATETIME,updated_at DATETIME) ENGINE=InnoDB");
            s.execute("INSERT INTO campaigns(id,code,name,type,status,owner_id) VALUES(10,'C10','First','EMAIL','ACTIVE',1),(20,'C20','Outside','EVENT','PLANNING',2)");
            s.execute("CREATE TEMPORARY TABLE campaign_leads(campaign_id BIGINT,lead_id BIGINT) ENGINE=InnoDB");
            s.execute("INSERT INTO campaign_leads VALUES(10,1),(10,2)");
            s.execute("CREATE TEMPORARY TABLE opportunities(id BIGINT PRIMARY KEY,name VARCHAR(255),amount DECIMAL(18,2),status VARCHAR(20),owner_user_id BIGINT,is_deleted BOOLEAN DEFAULT 0) ENGINE=InnoDB");
            s.execute("INSERT INTO opportunities VALUES(101,'Won',125.50,'WON',1,0),(102,'Open',999,'OPEN',1,0),(103,'Lost',500,'LOST',1,0),(104,'Deleted Won',1000,'WON',1,1),(201,'Outside',200,'WON',2,0)");
            s.execute("CREATE TEMPORARY TABLE campaign_opportunities(campaign_id BIGINT,opportunity_id BIGINT,PRIMARY KEY(campaign_id,opportunity_id)) ENGINE=InnoDB");
        }
    }
    @AfterEach void close() throws Exception {if(physical!=null)physical.close();}
    @Test void countsOpportunitiesAndOnlyNonDeletedWonRevenue() throws Exception {
        links.link(10,List.of(101L,102L,103L),all);
        try(var s=physical.createStatement()){s.execute("INSERT INTO campaign_opportunities VALUES(10,104)");}
        var c=campaigns.findById(10);
        assertEquals(2,c.getLeadCount());assertEquals(3,c.getOpportunityCount());
        assertEquals(0,new BigDecimal("125.50").compareTo(c.getWonRevenue()));
    }
    @Test void listAndCountUseIdenticalSelfAndTeamScope() throws Exception {
        assertEquals(1,campaigns.countTotal(null,null,self));
        assertEquals(List.of(10L),campaigns.findPaged(null,null,0,20,self).stream().map(c->c.getId()).toList());
        var team=new DataScopeContext(1,"campaign",ScopeType.TEAM,1L,Set.of(1L,2L));
        assertEquals(2,campaigns.countTotal(null,null,team));assertEquals(2,campaigns.findPaged(null,null,0,20,team).size());
        assertEquals(0,campaigns.countTotal("Outside",null,self));
    }
    @Test void repeatedLinksDoNotDoubleCount() throws Exception {
        links.link(10,List.of(101L,101L),all);links.link(10,List.of(101L),all);
        assertEquals(1,campaigns.findById(10).getOpportunityCount());
    }
    @Test void outOfScopeOpportunityRollsBackEarlierLink() throws Exception {
        assertThrows(SecurityException.class,()->links.link(10,List.of(101L,201L),self));
        assertEquals(0,campaigns.findById(10).getOpportunityCount());
    }
    @Test void missingOpportunityRollsBackAndOutsideCampaignCannotBeMutated() throws Exception {
        assertThrows(NoSuchElementException.class,()->links.link(10,List.of(101L,999L),all));
        assertEquals(0,campaigns.findById(10).getOpportunityCount());
        assertThrows(SecurityException.class,()->links.link(20,List.of(101L),self));
        assertThrows(SecurityException.class,()->links.unlink(20,101,self));
    }
    @Test void listAvailabilityAndUnlinkUseRealRelation() throws Exception {
        assertEquals(3,links.list(10,true,self).size());
        links.link(10,List.of(101L),self);
        assertEquals(1,links.list(10,false,self).size());assertEquals(2,links.list(10,true,self).size());
        links.unlink(10,101,self);assertEquals(0,campaigns.findById(10).getOpportunityCount());
    }
    @Test void invalidIdsCannotWriteAndEmptyCampaignRevenueIsZero() throws Exception {
        assertThrows(IllegalArgumentException.class,()->links.link(10,List.of(),all));
        assertThrows(IllegalArgumentException.class,()->links.link(10,List.of(-1L),all));
        assertEquals(0,BigDecimal.ZERO.compareTo(campaigns.findById(10).getWonRevenue()));
    }
}
