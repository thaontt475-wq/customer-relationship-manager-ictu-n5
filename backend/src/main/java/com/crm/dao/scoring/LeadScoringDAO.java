package com.crm.dao.scoring;

import com.crm.config.DatabaseConfig;
import com.crm.dto.scoring.*;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.scoring.LeadScoringEngine.Result;
import com.crm.util.JsonUtil;
import java.sql.*;
import java.util.*;

public class LeadScoringDAO {
    public Connection open() throws SQLException { return DatabaseConfig.getConnection(); }
    public ScoringConfig config(Connection c, boolean write) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM lead_scoring_config WHERE id=1 " + (write ? "FOR UPDATE" : "FOR SHARE"));
             ResultSet r = p.executeQuery()) {
            if (!r.next()) throw new IllegalStateException("Thiếu cấu hình; áp dụng migration 025");
            return new ScoringConfig(r.getLong("version"), r.getObject("hot",Integer.class), r.getObject("warm",Integer.class),
                    r.getString("aggregation"), r.getObject("min_score",Integer.class), r.getObject("max_score",Integer.class),
                    Arrays.asList(JsonUtil.getGson().fromJson(r.getString("rules_json"),ScoringRule[].class)));
        }
    }
    public void saveConfig(Connection c, ScoringConfig config, long user) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("""
            UPDATE lead_scoring_config SET version=?,hot=?,warm=?,aggregation=?,min_score=?,max_score=?,
            rules_json=?,updated_by=?,updated_at=CURRENT_TIMESTAMP(6) WHERE id=1
            """)) {
            p.setLong(1,config.version());p.setObject(2,config.hot());p.setObject(3,config.warm());p.setString(4,config.aggregation());
            p.setObject(5,config.minScore());p.setObject(6,config.maxScore());p.setString(7,JsonUtil.getGson().toJson(config.rules()));p.setLong(8,user);
            if (p.executeUpdate()!=1) throw new SQLException("Scoring config missing");
        }
    }
    /** Lock order: config -> leads -> profiles/scores. Owner predicates always bound. */
    public List<Map<String,Object>> leads(Connection c,DataScopeContext scope,Long id,int page,int size) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT l.id,l.full_name AS fullName,l.company_name AS companyName,l.email,l.phone,l.status,
            l.owner_user_id AS ownerUserId,l.source_id AS sourceId,l.created_at AS createdAt,
            m.name AS source,m.code AS sourceCode,p.industry,p.employee_count AS employeeCount,p.title,
            p.recent_activity AS recentActivity,s.raw_score AS rawScore,s.score,s.classification,
            s.breakdown_json AS breakdownJson,s.config_version AS configVersion,s.input_hash AS inputHash,s.scored_at AS scoredAt
            FROM leads l JOIN master_data m ON m.id=l.source_id
            LEFT JOIN lead_scoring_profiles p ON p.lead_id=l.id LEFT JOIN lead_scores s ON s.lead_id=l.id WHERE 1=1
            """);
        List<Object> params=new ArrayList<>();
        if (id!=null) {sql.append(" AND l.id=?");params.add(id);}
        if (scope!=null) scope.appendOwnerPredicate("l.owner_user_id",sql,params);
        sql.append(" ORDER BY l.id LIMIT ? OFFSET ? FOR UPDATE");params.add(size);params.add((page-1)*size);
        List<Map<String,Object>> rows=new ArrayList<>();
        try (PreparedStatement p=c.prepareStatement(sql.toString())) {
            bind(p,params);
            try(ResultSet r=p.executeQuery()) {
                while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=r.getMetaData().getColumnCount();i++) {
                        Object value=r.getObject(i);
                        row.put(r.getMetaData().getColumnLabel(i),value instanceof Timestamp date?date.toLocalDateTime().toString():
                                value instanceof java.time.temporal.TemporalAccessor?value.toString():value);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }
    public long count(Connection c,DataScopeContext scope) throws SQLException {
        var sql=new StringBuilder("SELECT COUNT(*) FROM leads WHERE 1=1");var params=new ArrayList<Object>();
        scope.appendOwnerPredicate("owner_user_id",sql,params);
        try(var p=c.prepareStatement(sql.toString())) {bind(p,params);try(var r=p.executeQuery()){r.next();return r.getLong(1);}}
    }
    public void saveScore(Connection c,long id,Result score,long version,String hash) throws SQLException {
        try(var p=c.prepareStatement("""
            INSERT INTO lead_scores(lead_id,raw_score,score,classification,breakdown_json,config_version,input_hash)
            VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE raw_score=?,score=?,classification=?,breakdown_json=?,
            config_version=?,input_hash=?,scored_at=CURRENT_TIMESTAMP(6)
            """)) {
            String details=JsonUtil.getGson().toJson(score.breakdown());
            p.setLong(1,id);p.setInt(2,score.rawScore());p.setInt(3,score.score());p.setString(4,score.classification());p.setString(5,details);
            p.setLong(6,version);p.setString(7,hash);p.setInt(8,score.rawScore());p.setInt(9,score.score());p.setString(10,score.classification());
            p.setString(11,details);p.setLong(12,version);p.setString(13,hash);p.executeUpdate();
        }
    }
    public void saveProfile(Connection c,long id,Map<String,Object> profile,long user) throws SQLException {
        try(var p=c.prepareStatement("""
            INSERT INTO lead_scoring_profiles(lead_id,industry,employee_count,title,recent_activity,updated_by)
            VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE industry=?,employee_count=?,title=?,recent_activity=?,
            updated_by=?,updated_at=CURRENT_TIMESTAMP(6)
            """)) {
            p.setLong(1,id);p.setObject(2,profile.get("industry"));p.setObject(3,profile.get("employeeCount"));p.setObject(4,profile.get("title"));
            p.setObject(5,profile.get("recentActivity"));p.setLong(6,user);p.setObject(7,profile.get("industry"));p.setObject(8,profile.get("employeeCount"));
            p.setObject(9,profile.get("title"));p.setObject(10,profile.get("recentActivity"));p.setLong(11,user);p.executeUpdate();
        }
    }
    private void bind(PreparedStatement p,List<Object> values) throws SQLException {
        for(int i=0;i<values.size();i++)p.setObject(i+1,values.get(i));
    }
}
