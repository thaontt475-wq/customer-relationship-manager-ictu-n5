package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class CustomerRelationDAO {
    public void setParent(long child,Long parent) throws SQLException {
        try(Connection c=DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Serialize parent changes to prevent concurrent cycles.
                try(PreparedStatement lock=c.prepareStatement("SELECT id FROM customers WHERE is_deleted=0 ORDER BY id FOR UPDATE");ResultSet ignored=lock.executeQuery()) {while(ignored.next()) {}}
                if(parent!=null){
                    if(child==parent) throw new IllegalArgumentException("Không thể tự đặt làm công ty mẹ");
                    try(PreparedStatement ps=c.prepareStatement("WITH RECURSIVE descendants AS (SELECT id,parent_customer_id FROM customers WHERE id=? UNION ALL SELECT c.id,c.parent_customer_id FROM customers c JOIN descendants d ON c.parent_customer_id=d.id) SELECT COUNT(*) FROM descendants WHERE id=?")) {
                        ps.setLong(1,child);ps.setLong(2,parent);
                        try(ResultSet rs=ps.executeQuery()){rs.next();if(rs.getLong(1)>0) throw new IllegalArgumentException("Quan hệ công ty tạo vòng lặp");}
                    }
                    try(PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM customers WHERE id=? AND is_deleted=0")) {
                        ps.setLong(1,parent);try(ResultSet rs=ps.executeQuery()){rs.next();if(rs.getLong(1)==0) throw new IllegalArgumentException("Công ty mẹ không tồn tại");}
                    }
                }
                try(PreparedStatement ps=c.prepareStatement("UPDATE customers SET parent_customer_id=? WHERE id=? AND is_deleted=0")) {
                    if(parent==null) ps.setNull(1,Types.BIGINT);else ps.setLong(1,parent);
                    ps.setLong(2,child);if(ps.executeUpdate()!=1) throw new IllegalArgumentException("Khách hàng không tồn tại");
                }
                c.commit();
            }catch(Exception ex){c.rollback();if(ex instanceof SQLException e)throw e;if(ex instanceof RuntimeException e)throw e;throw new SQLException(ex);}
            finally {c.setAutoCommit(true);}
        }
    }
    public List<Map<String,Object>> children(long id,DataScopeContext scope) throws SQLException {
        StringBuilder q=new StringBuilder("SELECT c.id,c.name,c.status,c.owner_user_id AS ownerUserId FROM customers c WHERE c.parent_customer_id=? AND c.is_deleted=0");
        List<Object> args=new ArrayList<>();args.add(id);scope.appendOwnerPredicate("c.owner_user_id",q,args);
        q.append(" ORDER BY c.name");
        try(Connection c=DatabaseConfig.getConnection();PreparedStatement ps=c.prepareStatement(q.toString())){
            for(int i=0;i<args.size();i++)ps.setObject(i+1,args.get(i));
            List<Map<String,Object>> result=new ArrayList<>();
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){Map<String,Object> row=new LinkedHashMap<>();row.put("id",rs.getLong("id"));row.put("name",rs.getString("name"));row.put("status",rs.getString("status"));row.put("ownerUserId",rs.getLong("ownerUserId"));result.add(row);}}
            return result;
        }
    }
    public BigDecimal groupContractValue(long id,DataScopeContext scope) throws SQLException {
        StringBuilder q=new StringBuilder("WITH RECURSIVE grp AS (SELECT id,owner_user_id FROM customers WHERE id=? AND is_deleted=0 UNION ALL SELECT c.id,c.owner_user_id FROM customers c JOIN grp g ON c.parent_customer_id=g.id WHERE c.is_deleted=0) SELECT COALESCE(SUM(ct.total_amount),0) FROM grp g JOIN customer_contracts ct ON ct.customer_id=g.id AND ct.status='SIGNED' WHERE 1=1");
        List<Object> args=new ArrayList<>();args.add(id);scope.appendOwnerPredicate("g.owner_user_id",q,args);
        try(Connection c=DatabaseConfig.getConnection();PreparedStatement ps=c.prepareStatement(q.toString())){
            for(int i=0;i<args.size();i++)ps.setObject(i+1,args.get(i));
            try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getBigDecimal(1);}
        }
    }
}
