package com.crm.service.leads;

import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.*;
import com.crm.dto.leads.LeadValidationException.FieldError;
import com.crm.service.permissions.*;
import java.sql.*;
import java.util.*;

public class LeadService {
    @FunctionalInterface public interface ScopeResolver { DataScopeContext resolve(long user,String action) throws Exception; }
    private final LeadDAO dao;
    private final ScopeResolver scopes;
    public interface ScoringHook {
        void beforeCreate(Connection c) throws Exception;
        void scoreCreated(Connection c,long id) throws Exception;
    }
    public static ScoringHook productionScoring() {
        var scoring=new com.crm.service.scoring.LeadScoringService();
        return new ScoringHook() {
            public void beforeCreate(Connection c) throws Exception {scoring.beforeCreate(c);}
            public void scoreCreated(Connection c,long id) throws Exception {scoring.scoreCreated(c,id);}
        };
    }
    public static final ScoringHook NO_SCORING=new ScoringHook() {
        public void beforeCreate(Connection c) {}
        public void scoreCreated(Connection c,long id) {}
    };
    private final ScoringHook scoring;
    public LeadService() { this(new LeadDAO(),(user,action)->new DataScopeService().resolve(user,"lead",action),productionScoring()); }
    public LeadService(LeadDAO dao,ScopeResolver scopes) { this(dao,scopes,NO_SCORING); }
    public LeadService(LeadDAO dao,ScopeResolver scopes,ScoringHook scoring) { this.dao=dao;this.scopes=scopes;this.scoring=scoring; }
    public DataScopeContext authorizeCreate(long user) throws Exception { return scopes.resolve(user,"create"); }
    public List<Map<String,Object>> sources(long user) throws Exception {
        authorizeCreate(user);try(Connection c=dao.open()){return dao.sources(c,false);}
    }
    public Map<String,Object> create(long user,LeadWriteRequest request) throws Exception {
        DataScopeContext scope=authorizeCreate(user);
        LeadWriteRequest lead=LeadValidation.normalize(request);
        try(Connection c=dao.open()) {
            c.setAutoCommit(false);
            try {
                scoring.beforeCreate(c);
                requireSource(lead,dao.sources(c,true));
                long id=dao.insert(c,lead,user,null);
                scoring.scoreCreated(c,id);
                Map<String,Object> result=dao.get(c,id,scope);
                c.commit();return result;
            } catch(Exception error) {c.rollback();throw error;}
        }
    }
    public Map<String,Object> get(long user,long id) throws Exception {
        if(id<=0)throw new IllegalArgumentException("ID Lead phải dương");
        DataScopeContext scope=scopes.resolve(user,"read");
        try(Connection c=dao.open()){return dao.get(c,id,scope);}
    }
    /** Full base-field replacement; owner/status/score remain server-owned. */
    public Map<String,Object> update(long user,long id,LeadWriteRequest request) throws Exception {
        if(id<=0)throw new IllegalArgumentException("ID Lead phải dương");
        DataScopeContext scope=scopes.resolve(user,"update");var lead=LeadValidation.normalize(request);
        try(Connection c=dao.open()) {
            c.setAutoCommit(false);
            try {
                scoring.beforeCreate(c);
                if(dao.get(c,id,scope,true)==null)throw new NoSuchElementException("Không tìm thấy Lead trong phạm vi dữ liệu");
                requireSource(lead,dao.sources(c,true));dao.update(c,id,lead);scoring.scoreCreated(c,id);
                var result=dao.get(c,id,scope);c.commit();return result;
            } catch(Exception error) {c.rollback();throw error;}
        }
    }
    public static void requireSource(LeadWriteRequest lead,List<Map<String,Object>> sources) {
        if(sources.stream().noneMatch(s->((Number)s.get("id")).longValue()==lead.sourceId()))
            throw new LeadValidationException(List.of(new FieldError("sourceId","Nguồn Lead không tồn tại, đã ngừng hoạt động hoặc sai loại")));
    }
}
