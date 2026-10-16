package com.crm.controller.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadDAO;
import com.crm.model.leads.Lead;
import com.crm.service.leads.LeadService;
import com.crm.service.permissions.DataScopeService;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WebFormScopeJdbcTest {
    @Test void realLeadListAndCountRespectSelfTeamAndAll() throws Exception {
        String marker="S401-"+UUID.randomUUID();
        List<Long> users=new ArrayList<>(), teams=new ArrayList<>();
        try(Connection c=DatabaseConfig.getConnection()) {
            assertTrue(c.getCatalog().startsWith("crm_sprint1_test_"));
            try {
                for(int i=0;i<2;i++) {
                    try(PreparedStatement s=c.prepareStatement("INSERT INTO teams(name,active) VALUES (?,1)",Statement.RETURN_GENERATED_KEYS)) {
                        s.setString(1,marker+i);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();teams.add(r.getLong(1));}
                    }
                }
                for(int i=0;i<5;i++) {
                    try(PreparedStatement s=c.prepareStatement("INSERT INTO users(full_name,email,password_hash,status,team_id) VALUES (?,?,?,'ACTIVE',?)",Statement.RETURN_GENERATED_KEYS)) {
                        s.setString(1,marker+i);s.setString(2,marker+i+"@example.invalid");s.setString(3,"unused-test-hash");s.setLong(4,teams.get(i==2?1:0));s.executeUpdate();
                        try(ResultSet r=s.getGeneratedKeys()){r.next();users.add(r.getLong(1));}
                    }
                    try(PreparedStatement s=c.prepareStatement("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code=?")) {
                        s.setLong(1,users.get(i));s.setString(2,i==3?"TEAM_LEAD":i==4?"ADMIN":"MARKETING");s.executeUpdate();
                    }
                }
                LeadDAO dao=new LeadDAO();
                for(int i=0;i<3;i++) {Lead lead=new Lead();lead.setFullName(marker);lead.setSource(marker);lead.setAssignedTo(users.get(i));dao.insert(lead);}
                DataScopeService scopes=new DataScopeService();
                var self=scopes.resolve(users.get(0),"lead","read");
                assertEquals(1,dao.count(null,marker,self));
                assertEquals(users.get(0),dao.findAll(1,20,null,marker,self).getFirst().getAssignedTo());
                var team=scopes.resolve(users.get(3),"lead","read");
                assertEquals(2,dao.count(null,marker,team));assertEquals(2,dao.findAll(1,20,null,marker,team).size());
                var all=scopes.resolve(users.get(4),"lead","read");
                assertEquals(3,dao.count(null,marker,all));assertEquals(3,dao.findAll(1,20,null,marker,all).size());
            } finally {
                try(PreparedStatement s=c.prepareStatement("DELETE FROM leads WHERE source=?")){s.setString(1,marker);s.executeUpdate();}
                for(Long id:users)try(PreparedStatement s=c.prepareStatement("DELETE FROM users WHERE id=?")){s.setLong(1,id);s.executeUpdate();}
                for(Long id:teams)try(PreparedStatement s=c.prepareStatement("DELETE FROM teams WHERE id=?")){s.setLong(1,id);s.executeUpdate();}
            }
        }
    }
    @Test void embedParametersCannotBreakOutOfHtmlOrScriptAttributes() {
        var embed=new LeadService().generateEmbedCode("bad\"'><script>","http://127.0.0.1:8080/crm","light","\"><img src=x onerror=alert(1)>");
        assertFalse(embed.getIframeSnippet().contains("<img"));
        assertFalse(embed.getScriptSnippet().contains("<script>\""));
        assertTrue(embed.getFormId().matches("[A-Za-z0-9_-]+"));
    }
}
