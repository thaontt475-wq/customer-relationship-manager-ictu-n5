package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL hierarchy. Fixture rows stay uncommitted and are rolled back. */
class LeadSlaMonitorJdbcTest {
    private Connection connection;
    private long rep, parentLeader, outsideLeader, childTeam, leadId;
    private final String marker = "sla-jdbc-" + UUID.randomUUID();
    private long insert(String sql, Object... args) throws Exception {
        try (PreparedStatement p=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for (int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
            p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();return r.getLong(1);}
        }
    }
    private long user(String suffix) throws Exception {
        return insert("INSERT INTO users(full_name,email,password_hash,status) VALUES(?,?,?,'ACTIVE')",
                marker+suffix,marker+suffix+"@example.invalid","unusable-transaction-only-fixture");
    }
    private void execute(String sql) throws Exception {try(Statement s=connection.createStatement()){s.execute(sql);}}
    private void role(long user,String code) throws Exception {
        execute("INSERT INTO user_roles(user_id,role_id) SELECT "+user+",id FROM roles WHERE code='"+code+"'");
    }
    @BeforeEach void setUp() throws Exception {
        assertTrue(String.valueOf(System.getenv("CRM_DB_URL")).contains("/crm_sprint1_test"));
        connection=DatabaseConfig.getConnection();connection.setAutoCommit(false);
        rep=user("rep");parentLeader=user("parent");outsideLeader=user("outside");
        long parent=insert("INSERT INTO teams(name,leader_id) VALUES(?,?)",marker+"parent",parentLeader);
        childTeam=insert("INSERT INTO teams(name,parent_id) VALUES(?,?)",marker+"child",parent);
        long outside=insert("INSERT INTO teams(name,leader_id) VALUES(?,?)",marker+"outside",outsideLeader);
        execute("UPDATE users SET team_id="+parent+" WHERE id="+parentLeader);
        execute("UPDATE users SET team_id="+childTeam+" WHERE id="+rep);
        execute("UPDATE users SET team_id="+outside+" WHERE id="+outsideLeader);
        role(rep,"SALES_REP");role(parentLeader,"TEAM_LEAD");role(outsideLeader,"TEAM_LEAD");
        long source;try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT id FROM master_data WHERE type='lead-source' AND active=TRUE LIMIT 1")){assertTrue(r.next());source=r.getLong(1);}
        leadId=insert("INSERT INTO leads(full_name,source_id,owner_user_id,created_by,assigned_to_user_id,status) VALUES(?,?,?,?,?,'PENDING_RESPONSE')",
                marker,source,outsideLeader,outsideLeader,rep);
    }
    @AfterEach void close() throws Exception {
        if(connection!=null){try{connection.rollback();}finally{connection.close();}}
    }
    @Test void childRepNotifiesAncestorLeaderAndExcludesOutsideOwner() throws Exception {
        assertEquals(Set.of(parentLeader),new HashSet<>(new LeadSlaMonitorService().findTeamLeadUserIds(connection,leadId)));
    }
    @Test void inactiveAncestorLeaderIsExcluded() throws Exception {
        execute("UPDATE users SET status='INACTIVE' WHERE id="+parentLeader);
        assertTrue(new LeadSlaMonitorService().findTeamLeadUserIds(connection,leadId).isEmpty());
    }
    @Test void directAndAncestorRecipientsAreDistinctAndRepIsExcluded() throws Exception {
        long direct=user("direct");role(direct,"TEAM_LEAD");role(rep,"TEAM_LEAD");role(parentLeader,"ADMIN");
        execute("UPDATE users SET team_id="+childTeam+" WHERE id="+direct);
        execute("UPDATE teams SET leader_id="+direct+" WHERE id="+childTeam);
        List<Long> result=new LeadSlaMonitorService().findTeamLeadUserIds(connection,leadId);
        assertEquals(2,result.size());assertEquals(Set.of(parentLeader,direct),new HashSet<>(result));
    }
}
