package com.crm.dao.campaigns;
import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

/** Links existing opportunities without changing their owner or commercial lifecycle. */
public class CampaignOpportunityDAO {
    protected Connection open() throws SQLException { return DatabaseConfig.getConnection(); }
    public List<Map<String,Object>> list(long campaign,boolean available,DataScopeContext scope) throws SQLException {
        StringBuilder sql=new StringBuilder("SELECT o.id,o.name,o.amount,o.status FROM opportunities o WHERE o.is_deleted=0 AND "+(available?"NOT ":"")+"EXISTS(SELECT 1 FROM campaign_opportunities co WHERE co.opportunity_id=o.id AND co.campaign_id=?)");
        List<Object> args=new ArrayList<>();args.add(campaign);scope.appendOwnerPredicate("o.owner_user_id",sql,args);sql.append(" ORDER BY o.id DESC LIMIT 500");
        try(Connection c=open();PreparedStatement p=c.prepareStatement(sql.toString())) {
            for(int i=0;i<args.size();i++)p.setObject(i+1,args.get(i));
            try(ResultSet r=p.executeQuery()) {List<Map<String,Object>> rows=new ArrayList<>();while(r.next())rows.add(Map.of("id",r.getLong(1),"name",r.getString(2),"amount",r.getBigDecimal(3),"status",r.getString(4)));return rows;}
        }
    }
    public int link(long campaign,List<Long> ids,DataScopeContext scope) throws SQLException {
        if(ids==null || ids.isEmpty() || ids.size()>500 || ids.stream().anyMatch(id->id==null || id<=0))throw new IllegalArgumentException("Chọn 1–500 cơ hội hợp lệ");
        try(Connection c=open()) {c.setAutoCommit(false);try {
            checkCampaign(c,campaign,scope);
            int count=0;
            for(long id:new TreeSet<>(ids)) {
                try(PreparedStatement p=c.prepareStatement("SELECT owner_user_id FROM opportunities WHERE id=? AND is_deleted=0 FOR UPDATE")) {
                    p.setLong(1,id);try(ResultSet r=p.executeQuery()) {if(!r.next())throw new NoSuchElementException("Không tìm thấy cơ hội");if(!scope.canAccessOwner(r.getLong(1)))throw new SecurityException("Cơ hội nằm ngoài phạm vi dữ liệu");}
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO campaign_opportunities(campaign_id,opportunity_id) VALUES(?,?) ON DUPLICATE KEY UPDATE opportunity_id=VALUES(opportunity_id)")) {p.setLong(1,campaign);p.setLong(2,id);count+=p.executeUpdate()>0?1:0;}
            }
            c.commit();return count;
        }catch(SQLException | RuntimeException e){c.rollback();throw e;}}
    }
    private void checkCampaign(Connection c,long campaign,DataScopeContext scope) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT owner_id FROM campaigns WHERE id=? AND is_deleted=0 FOR UPDATE")) {p.setLong(1,campaign);try(ResultSet r=p.executeQuery()){if(!r.next())throw new NoSuchElementException("Không tìm thấy chiến dịch");if(!scope.canAccessOwner(r.getLong(1)))throw new SecurityException("Chiến dịch ngoài phạm vi");}}
    }
    public void unlink(long campaign,long opportunity,DataScopeContext scope) throws SQLException {
        try(Connection c=open()){c.setAutoCommit(false);try {checkCampaign(c,campaign,scope);try(PreparedStatement p=c.prepareStatement("DELETE FROM campaign_opportunities WHERE campaign_id=? AND opportunity_id=?")){p.setLong(1,campaign);p.setLong(2,opportunity);if(p.executeUpdate()!=1)throw new NoSuchElementException("Liên kết không tồn tại");}c.commit();}catch(SQLException | RuntimeException e){c.rollback();throw e;}}
    }
}
