package com.crm.dao.contacts;

import com.crm.dto.contacts.ContactWriteRequest;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class ContactDAO {
    private static final String SELECT = "SELECT id,customer_id AS customerId,full_name AS fullName,title,email,phone,buying_role AS buyingRole,is_primary AS isPrimary,created_at AS createdAt FROM contacts";
    public List<Map<String,Object>> rows(Connection c,String sql,List<?> args) throws SQLException {
        try (PreparedStatement p=c.prepareStatement(sql)) {
            for(int i=0;i<args.size();i++) p.setObject(i+1,args.get(i));
            try(ResultSet r=p.executeQuery()) {
                List<Map<String,Object>> out=new ArrayList<>();
                while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=r.getMetaData().getColumnCount();i++) {
                        Object value=r.getObject(i);
                        if(value instanceof Timestamp t) value=t.toLocalDateTime().toString();
                        row.put(r.getMetaData().getColumnLabel(i),value);
                    }
                    if(row.containsKey("isPrimary")) row.put("isPrimary",r.getBoolean("isPrimary"));
                    out.add(row);
                }
                return out;
            }
        }
    }
    public Map<String,Object> find(Connection c,long id,boolean lock) throws SQLException {
        var rows=rows(c,SELECT+" WHERE id=? AND is_deleted=0"+(lock?" FOR UPDATE":""),List.of(id));
        if(rows.isEmpty()) throw new NoSuchElementException("Không tìm thấy người liên hệ");
        return rows.getFirst();
    }
    public List<Map<String,Object>> list(Connection c,Long customer,DataScopeContext scope) throws SQLException {
        StringBuilder sql=new StringBuilder("SELECT ct.id,ct.customer_id AS customerId,ct.full_name AS fullName,ct.title,ct.email,ct.phone,ct.buying_role AS buyingRole,ct.is_primary AS isPrimary,ct.created_at AS createdAt FROM contacts ct JOIN customers c ON c.id=ct.customer_id WHERE ct.is_deleted=0 AND c.is_deleted=0");
        List<Object> args=new ArrayList<>();
        if(customer!=null) {sql.append(" AND ct.customer_id=?");args.add(customer);}
        scope.appendOwnerPredicate("c.owner_user_id",sql,args);
        sql.append(" ORDER BY ct.is_primary DESC,ct.id DESC");
        return rows(c,sql.toString(),args);
    }
    public long customerOwner(Connection c,long id,boolean lock) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT owner_user_id FROM customers WHERE id=? AND is_deleted=0"+(lock?" FOR UPDATE":""))) {
            p.setLong(1,id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) throw new NoSuchElementException("Không tìm thấy khách hàng");
                return r.getLong(1);
            }
        }
    }
    public long historicalCustomerOwner(Connection c,long id) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT owner_user_id FROM customers WHERE id=?")) {
            p.setLong(1,id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) throw new NoSuchElementException("Không tìm thấy khách hàng trong lịch sử");
                return r.getLong(1);
            }
        }
    }
    public int execute(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
            return p.executeUpdate();
        }
    }
    public void clearPrimary(Connection c,long customer) throws SQLException {
        execute(c,"UPDATE contacts SET is_primary=0 WHERE customer_id=? AND is_deleted=0 AND is_primary=1",customer);
    }
    public long create(Connection c,ContactWriteRequest r) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO contacts(customer_id,full_name,title,email,phone,buying_role,is_primary) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            Object[] args={r.customerId,r.fullName,r.title,r.email,r.phone,r.buyingRole,Boolean.TRUE.equals(r.isPrimary)};
            for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
            if(p.executeUpdate()!=1) throw new SQLException("Không thể tạo Contact");
            try(ResultSet keys=p.getGeneratedKeys()) {if(!keys.next()) throw new SQLException("Thiếu Contact ID");return keys.getLong(1);}
        }
    }
    public void update(Connection c,long id,ContactWriteRequest r,boolean primary) throws SQLException {
        if(execute(c,"UPDATE contacts SET full_name=?,title=?,email=?,phone=?,buying_role=?,is_primary=? WHERE id=? AND is_deleted=0",r.fullName,r.title,r.email,r.phone,r.buyingRole,primary,id)!=1)
            throw new NoSuchElementException("Không tìm thấy người liên hệ");
    }
    public List<Map<String,Object>> history(Connection c,long id) throws SQLException {
        return rows(c,"SELECT id,contact_id AS contactId,from_customer_id AS fromCustomerId,to_customer_id AS toCustomerId,changed_by AS changedBy,changed_at AS changedAt,note FROM contact_company_history WHERE contact_id=? ORDER BY changed_at DESC,id DESC",List.of(id));
    }
}
