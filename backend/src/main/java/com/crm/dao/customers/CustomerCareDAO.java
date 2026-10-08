package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class CustomerCareDAO {
    public Map<String,Object> due(DataScopeContext scope,int days,int page,int size) throws SQLException {
        StringBuilder where=new StringBuilder(" FROM customers c LEFT JOIN (SELECT customer_id, MAX(created_at) AS last_at FROM activities WHERE is_deleted=0 GROUP BY customer_id) la ON la.customer_id=c.id LEFT JOIN (SELECT customer_id,SUM(total_amount) AS total_value FROM customer_contracts WHERE status='SIGNED' GROUP BY customer_id) ct ON ct.customer_id=c.id WHERE c.is_deleted=0 AND (la.last_at IS NULL OR la.last_at < DATE_SUB(NOW(), INTERVAL ? DAY))");
        List<Object> args=new ArrayList<>();args.add(days);scope.appendOwnerPredicate("c.owner_user_id",where,args);
        long total;
        try(Connection conn=DatabaseConfig.getConnection();PreparedStatement ps=conn.prepareStatement("SELECT COUNT(*) "+where)){
            bind(ps,args);try(ResultSet rs=ps.executeQuery()){rs.next();total=rs.getLong(1);}
        }
        String sql="SELECT c.id,c.name,c.status,c.owner_user_id AS ownerUserId,la.last_at AS lastInteractionAt,COALESCE(ct.total_value,0) AS contractValue "+where+" ORDER BY contractValue DESC,c.id DESC LIMIT ? OFFSET ?";
        List<Object> params=new ArrayList<>(args);params.add(size);params.add((page-1)*size);
        List<Map<String,Object>> items=new ArrayList<>();
        try(Connection conn=DatabaseConfig.getConnection();PreparedStatement ps=conn.prepareStatement(sql)){
            bind(ps,params);try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("id",rs.getLong("id"));row.put("name",rs.getString("name"));row.put("status",rs.getString("status"));row.put("ownerUserId",rs.getLong("ownerUserId"));row.put("lastInteractionAt",rs.getTimestamp("lastInteractionAt"));row.put("contractValue",rs.getBigDecimal("contractValue"));items.add(row);
            }}
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("items",items);out.put("page",page);out.put("size",size);out.put("totalItems",total);out.put("totalPages",(total+size-1)/size);out.put("days",days);return out;
    }
    private static void bind(PreparedStatement ps,List<Object> a)throws SQLException{for(int i=0;i<a.size();i++)ps.setObject(i+1,a.get(i));}
    public long markContacted(long customer,long user,String note)throws SQLException{
        String sql="INSERT INTO activities(subject,type,description,status,customer_id,owner_user_id,created_at,is_deleted) VALUES ('Đã liên hệ','CALL',?,'COMPLETED',?,?,NOW(),0)";
        try(Connection c=DatabaseConfig.getConnection();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1,note);ps.setLong(2,customer);ps.setLong(3,user);ps.executeUpdate();
            try(ResultSet keys=ps.getGeneratedKeys()){if(keys.next())return keys.getLong(1);throw new SQLException("Không lấy được ID Activity");}
        }
    }
}
