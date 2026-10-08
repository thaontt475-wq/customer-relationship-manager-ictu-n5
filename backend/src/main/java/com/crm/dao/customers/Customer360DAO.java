package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Customer 360 queries. All child queries are filtered by customer and data scope. */
public class Customer360DAO {
    private static List<Map<String,Object>> rows(Connection c, String sql, List<Object> args) throws SQLException {
        List<Map<String,Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i=0; i<args.size(); i++) ps.setObject(i+1, args.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> item = new LinkedHashMap<>();
                    for (int i=1; i<=md.getColumnCount(); i++) item.put(md.getColumnLabel(i), rs.getObject(i));
                    result.add(item);
                }
            }
        }
        return result;
    }
    public List<Map<String,Object>> contacts(long id) throws SQLException {
        try (Connection c=DatabaseConfig.getConnection()) {
            return rows(c, "SELECT id, customer_id AS customerId, full_name AS fullName, email, phone, title, buying_role AS buyingRole, is_primary AS isPrimary FROM contacts WHERE customer_id=? AND is_deleted=0 ORDER BY is_primary DESC,id DESC", List.of(id));
        }
    }
    public List<Map<String,Object>> opportunities(long id, DataScopeContext scope) throws SQLException {
        StringBuilder q=new StringBuilder("SELECT o.id,o.name,o.amount,o.status,o.stage_id AS stageId,o.probability,o.created_at AS createdAt FROM opportunities o WHERE o.customer_id=? AND o.is_deleted=0");
        List<Object> args=new ArrayList<>(); args.add(id); scope.appendOwnerPredicate("o.owner_user_id",q,args);
        q.append(" ORDER BY o.created_at DESC,o.id DESC");
        try(Connection c=DatabaseConfig.getConnection()){return rows(c,q.toString(),args);}
    }
    public List<Map<String,Object>> activities(long id, DataScopeContext scope) throws SQLException {
        StringBuilder q=new StringBuilder("SELECT a.id,a.subject,a.type,a.description,a.status,a.due_date AS dueDate,a.owner_user_id AS ownerUserId,a.created_at AS createdAt FROM activities a WHERE a.customer_id=? AND a.is_deleted=0");
        List<Object> args=new ArrayList<>();args.add(id);scope.appendOwnerPredicate("a.owner_user_id",q,args);
        q.append(" ORDER BY a.created_at DESC,a.id DESC LIMIT 500");
        try(Connection c=DatabaseConfig.getConnection()){return rows(c,q.toString(),args);}
    }
    public List<Map<String,Object>> attachments(long id) throws SQLException {
        try(Connection c=DatabaseConfig.getConnection()){
            return rows(c,"SELECT id,customer_id AS customerId,file_name AS fileName,file_url AS fileUrl,uploaded_at AS uploadedAt FROM customer_attachments WHERE customer_id=? AND is_deleted=0 ORDER BY uploaded_at DESC,id DESC",List.of(id));
        }
    }
    public BigDecimal signedValue(long id) throws SQLException {
        try(Connection c=DatabaseConfig.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT COALESCE(SUM(total_amount),0) FROM customer_contracts WHERE customer_id=? AND status='SIGNED'")) {
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getBigDecimal(1);}
        }
    }
}
