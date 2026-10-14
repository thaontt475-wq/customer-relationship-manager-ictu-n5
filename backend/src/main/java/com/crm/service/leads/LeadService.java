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
    public LeadService() { this(new LeadDAO(),(user,action)->new DataScopeService().resolve(user,"lead",action)); }
    public LeadService(LeadDAO dao,ScopeResolver scopes) { this.dao=dao;this.scopes=scopes; }
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
                requireSource(lead,dao.sources(c,true));
                long id=dao.insert(c,lead,user,null);
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
    public static void requireSource(LeadWriteRequest lead,List<Map<String,Object>> sources) {
        if(sources.stream().noneMatch(s->((Number)s.get("id")).longValue()==lead.sourceId()))
            throw new LeadValidationException(List.of(new FieldError("sourceId","Nguồn Lead không tồn tại, đã ngừng hoạt động hoặc sai loại")));
    }
}
