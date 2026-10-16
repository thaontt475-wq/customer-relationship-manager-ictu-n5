package com.crm.service.scoring;

import com.crm.dao.scoring.LeadScoringDAO;
import com.crm.dto.scoring.*;
import com.crm.service.permissions.*;
import com.crm.util.JsonUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

public class LeadScoringService {
    @FunctionalInterface public interface ScopeResolver { DataScopeContext resolve(long user,String action) throws Exception; }
    @FunctionalInterface public interface ManagerGuard { void require(long user) throws Exception; }
    private final LeadScoringDAO dao;
    private final ScopeResolver scopes;
    private final ManagerGuard managers;
    private final LeadScoringEngine engine=new LeadScoringEngine();
    public LeadScoringService() {
        this(new LeadScoringDAO(),(user,action)->new DataScopeService().resolve(user,"lead",action),user->{
            if(!new PermissionService().hasPermission(user,"lead.scoring.manage")) throw new SecurityException("Không có quyền quản lý quy tắc chấm điểm");
        });
    }
    public LeadScoringService(LeadScoringDAO dao,ScopeResolver scopes,ManagerGuard managers) {
        this.dao=dao;this.scopes=scopes;this.managers=managers;
    }
    @FunctionalInterface private interface Work<T> { T run(Connection c) throws Exception; }
    private <T> T transaction(Work<T> work) throws Exception {
        try(Connection c=dao.open()) {
            c.setAutoCommit(false);
            try {T result=work.run(c);c.commit();return result;}
            catch(Exception error) {c.rollback();throw error;}
        }
    }
    public Map<String,Object> config(long user) throws Exception {
        scopes.resolve(user,"read");return transaction(c->view(dao.config(c,false)));
    }
    public Map<String,Object> list(long user,int page,int size) throws Exception {
        if(page<1 || page>1000000 || size<1 || size>100)throw new IllegalArgumentException("page >= 1, size 1..100");
        var scope=scopes.resolve(user,"read");
        return transaction(c->{
            var config=dao.config(c,false);var rows=dao.leads(c,scope,null,page,size);
            for(var row:rows) refresh(c,row,config,false);
            long total=dao.count(c,scope);
            return Map.of("items",rows,"page",page,"size",size,"total",total,"totalPages",(total+size-1)/size,
                    "configured",config.configured(),"version",config.version());
        });
    }
    public Map<String,Object> get(long user,long id) throws Exception {
        var scope=scopes.resolve(user,"read");
        return transaction(c->{var config=dao.config(c,false);var row=requireLead(c,scope,id);refresh(c,row,config,false);return row;});
    }
    public Map<String,Object> updateProfile(long user,long id,Map<String,Object> patch) throws Exception {
        var scope=scopes.resolve(user,"update");validateProfile(patch);
        return transaction(c->{
            var config=dao.config(c,false);var row=requireLead(c,scope,id);row.putAll(patch);
            dao.saveProfile(c,id,row,user);refresh(c,row,config,true);return row;
        });
    }
    public Map<String,Object> reScore(long user) throws Exception {
        var scope=scopes.resolve(user,"score");
        return transaction(c->{var config=dao.config(c,false);engine.validate(config);return Map.of("count",refreshAll(c,scope,config),"version",config.version());});
    }
    /** Full replacement uses optimistic version and one transaction including all affected scores. */
    public Map<String,Object> saveConfig(long user,ScoringConfig requested,boolean rulesOnly) throws Exception {
        managers.require(user);
        return transaction(c->{
            var current=dao.config(c,true);
            if(current.version()!=requested.version())throw new IllegalStateException("Cấu hình đã thay đổi; tải lại trước khi lưu");
            var config=rulesOnly?new ScoringConfig(current.version()+1,current.hot(),current.warm(),current.aggregation(),current.minScore(),current.maxScore(),requested.rules())
                    :new ScoringConfig(current.version()+1,requested.hot(),requested.warm(),requested.aggregation(),requested.minScore(),requested.maxScore(),requested.rules());
            engine.validate(config);dao.saveConfig(c,config,user);
            int count=refreshAll(c,null,config);var result=view(config);result.put("reScoredCount",count);return result;
        });
    }
    private int refreshAll(Connection c,DataScopeContext scope,ScoringConfig config) throws Exception {
        int count=0;
        for(int page=1;;page++) {
            var rows=dao.leads(c,scope,null,page,100);
            for(var row:rows)refresh(c,row,config,true);
            count+=rows.size();if(rows.size()<100)return count;
        }
    }
    private Map<String,Object> requireLead(Connection c,DataScopeContext scope,long id) throws SQLException {
        if(id<=0)throw new IllegalArgumentException("ID Lead phải dương");
        var rows=dao.leads(c,scope,id,1,1);
        if(rows.isEmpty())throw new NoSuchElementException("Không tìm thấy Lead trong phạm vi dữ liệu");return rows.getFirst();
    }
    private void refresh(Connection c,Map<String,Object> row,ScoringConfig config,boolean force) throws Exception {
        if(!config.configured()) {
            row.put("score",null);row.put("rawScore",null);row.put("classification","UNCONFIGURED");row.put("breakdown",List.of());
        } else {
            var inputs=new TreeMap<String,Object>();for(String field:LeadScoringEngine.FIELDS)inputs.put(field,row.get(field));
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JsonUtil.getGson().toJson(inputs).getBytes(StandardCharsets.UTF_8)));
            var result=engine.calculate(inputs,config);
            boolean changed=force || !hash.equals(row.get("inputHash")) || !Objects.equals(config.version(),row.get("configVersion"));
            if(changed) {
                dao.saveScore(c,((Number)row.get("id")).longValue(),result,config.version(),hash);
                // Read DB timestamp rather than manufacturing a successful persistence timestamp.
                try(var p=c.prepareStatement("SELECT scored_at FROM lead_scores WHERE lead_id=?")) {
                    p.setObject(1,row.get("id"));try(var r=p.executeQuery()){r.next();row.put("scoredAt",r.getTimestamp(1).toLocalDateTime().toString());}
                }
            }
            row.put("rawScore",result.rawScore());row.put("score",result.score());row.put("classification",result.classification());
            row.put("breakdown",result.breakdown());row.put("configVersion",config.version());
        }
        row.remove("inputHash");row.remove("breakdownJson");
    }
    /** Called inside the S4-02 transaction; config locks precede lead/source locks. */
    public void beforeCreate(Connection c) throws SQLException {dao.config(c,false);}
    public void scoreCreated(Connection c,long id) throws Exception {
        var config=dao.config(c,false);var row=requireLead(c,null,id);refresh(c,row,config,true);
    }
    private Map<String,Object> view(ScoringConfig config) {
        var data=new LinkedHashMap<String,Object>();data.put("configured",config.configured());data.put("version",config.version());
        var thresholds=new LinkedHashMap<String,Object>();thresholds.put("hot",config.hot());thresholds.put("warm",config.warm());
        data.put("thresholds",thresholds);data.put("aggregation",config.aggregation());data.put("minScore",config.minScore());data.put("maxScore",config.maxScore());data.put("rules",config.rules());return data;
    }
    private void validateProfile(Map<String,Object> patch) {
        if(patch.isEmpty() || !Set.of("industry","employeeCount","title","recentActivity").containsAll(patch.keySet()))
            throw new IllegalArgumentException("Chỉ cập nhật industry, employeeCount, title, recentActivity; không được ghi đè điểm");
        for(var entry:patch.entrySet()) {
            Object value=entry.getValue();if(value==null)continue;
            if(entry.getKey().equals("employeeCount")) {
                if(!(value instanceof Integer count) || count<0 || count>10000000)throw new IllegalArgumentException("employeeCount phải là số nguyên 0..10000000");
            } else {
                int max=entry.getKey().equals("title")?150:entry.getKey().equals("industry")?255:1000;
                if(!(value instanceof String text) || text.length()>max)throw new IllegalArgumentException(entry.getKey()+" vượt giới hạn ký tự");
            }
        }
    }
}
