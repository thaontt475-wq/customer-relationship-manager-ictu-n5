package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.LeadWriteRequest;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class LeadDAO {
    public Connection open() throws SQLException { return DatabaseConfig.getConnection(); }

    public List<Map<String,Object>> sources(Connection c, boolean lock) throws SQLException {
        String query="SELECT id,code,name FROM master_data WHERE type='lead-source' AND active=TRUE ORDER BY id"+(lock?" FOR SHARE":"");
        List<Map<String,Object>> result=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(query);ResultSet r=p.executeQuery()) {
            while(r.next()) result.add(Map.of("id",r.getLong("id"),"code",r.getString("code"),"name",r.getString("name")));
        }
        return result;
    }
    public long insert(Connection c, LeadWriteRequest lead, long user, String batch) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("""
            INSERT INTO leads(full_name,company_name,email,phone,source_id,notes,status,owner_user_id,created_by,import_batch_hash)
            VALUES (?,?,?,?,?,?,'NEW',?,?,?)
            """,Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1,lead.fullName());p.setString(2,lead.companyName());p.setString(3,lead.email());p.setString(4,lead.phone());
            p.setLong(5,lead.sourceId());p.setString(6,lead.notes());p.setLong(7,user);p.setLong(8,user);p.setString(9,batch);
            p.executeUpdate();
            try(ResultSet keys=p.getGeneratedKeys()) {if(!keys.next())throw new SQLException("Missing Lead generated key");return keys.getLong(1);}
        }
    }
    public Map<String,Object> get(Connection c,long id,DataScopeContext scope) throws SQLException {
        return get(c,id,scope,false);
    }
    public Map<String,Object> get(Connection c,long id,DataScopeContext scope,boolean lock) throws SQLException {
        StringBuilder query=new StringBuilder("""
            SELECT l.id,l.full_name AS fullName,l.company_name AS companyName,l.email,l.phone,l.source_id AS sourceId,
            s.code AS sourceCode,s.name AS sourceName,l.notes,l.status,l.owner_user_id AS ownerUserId,
            l.created_by AS createdBy,l.created_at AS createdAt
            FROM leads l JOIN master_data s ON s.id=l.source_id WHERE l.id=?
            """);
        List<Object> values=new ArrayList<>();values.add(id);scope.appendOwnerPredicate("l.owner_user_id",query,values);
        if(lock)query.append(" FOR UPDATE");
        try(PreparedStatement p=c.prepareStatement(query.toString())) {
            for(int i=0;i<values.size();i++)p.setObject(i+1,values.get(i));
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())return null;
                Map<String,Object> result=new LinkedHashMap<>();
                for(int i=1;i<=r.getMetaData().getColumnCount();i++) {
                    Object value=r.getObject(i);
                    result.put(r.getMetaData().getColumnLabel(i),value instanceof Timestamp date?date.toLocalDateTime().toString():
                            value instanceof java.time.temporal.TemporalAccessor?value.toString():value);
                }
                return result;
            }
        }
    }
    public void update(Connection c,long id,LeadWriteRequest lead) throws SQLException {
        try(var p=c.prepareStatement("UPDATE leads SET full_name=?,company_name=?,email=?,phone=?,source_id=?,notes=? WHERE id=?")) {
            p.setString(1,lead.fullName());p.setString(2,lead.companyName());p.setString(3,lead.email());p.setString(4,lead.phone());
            p.setLong(5,lead.sourceId());p.setString(6,lead.notes());p.setLong(7,id);p.executeUpdate();
        }
    }
    public Long receiptOwner(Connection c,String hash) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT owner_user_id FROM lead_import_batches WHERE token_hash=?")) {
            p.setString(1,hash);try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):null;}
        }
    }
    public void receipt(Connection c,String hash,long user,int total,int success,int failure) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO lead_import_batches(token_hash,owner_user_id,total_rows,success_count,failure_count) VALUES (?,?,?,?,?)")) {
            p.setString(1,hash);p.setLong(2,user);p.setInt(3,total);p.setInt(4,success);p.setInt(5,failure);p.executeUpdate();
        }
    }
}
