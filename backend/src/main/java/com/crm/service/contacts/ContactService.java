package com.crm.service.contacts;

import com.crm.config.DatabaseConfig;
import com.crm.dao.contacts.ContactDAO;
import com.crm.dto.contacts.*;
import com.crm.service.permissions.*;
import java.sql.*;
import java.util.*;

public class ContactService {
    private final ContactDAO dao=new ContactDAO();
    private final PermissionService permissions=new PermissionService();
    private final DataScopeService scopes=new DataScopeService();
    private DataScopeContext scope(long user,String action) throws Exception {
        if(!permissions.hasPermission(user,"contact."+action)) throw new SecurityException("Bạn không có quyền thao tác người liên hệ");
        return scopes.resolve(user,"customer",action.equals("create")?"update":action.equals("delete")?"update":action);
    }
    private void customer(Connection c,long id,DataScopeContext scope,boolean lock) throws SQLException {
        if(!scope.canAccessOwner(dao.customerOwner(c,id,lock))) throw new SecurityException("Khách hàng nằm ngoài phạm vi SELF/TEAM/ALL");
    }
    private static long customerId(Map<String,Object> r) {return ((Number)r.get("customerId")).longValue();}
    public List<Map<String,Object>> list(long user,Long customerId) throws Exception {
        var scope=scope(user,"read");
        try(Connection c=DatabaseConfig.getConnection()) {
            if(customerId!=null) {positive(customerId);customer(c,customerId,scope,false);}
            return dao.list(c,customerId,scope);
        }
    }
    public Map<String,Object> get(long user,long id) throws Exception {
        var scope=scope(user,"read");
        try(Connection c=DatabaseConfig.getConnection()) {
            var row=dao.find(c,id,false);customer(c,customerId(row),scope,false);return row;
        }
    }
    public List<Map<String,Object>> history(long user,long id) throws Exception {
        var scope=scope(user,"read");
        try(Connection c=DatabaseConfig.getConnection()) {
            var row=dao.find(c,id,false);customer(c,customerId(row),scope,false);
            var history=dao.history(c,id);
            // Historical companies must also be visible; never disclose companies outside the user's scope.
            for(var entry:history) {
                if(!scope.canAccessOwner(dao.historicalCustomerOwner(c,((Number)entry.get("fromCustomerId")).longValue()))) throw new SecurityException("Công ty cũ nằm ngoài phạm vi truy cập");
                if(!scope.canAccessOwner(dao.historicalCustomerOwner(c,((Number)entry.get("toCustomerId")).longValue()))) throw new SecurityException("Công ty mới nằm ngoài phạm vi truy cập");
            }
            return history;
        }
    }
    @FunctionalInterface private interface Work<T> {T run(Connection c) throws Exception;}
    private <T> T transaction(Work<T> work) throws Exception {
        try(Connection c=DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {T result=work.run(c);c.commit();return result;}
            catch(Exception e) {try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
        }
    }
    public Map<String,Object> create(long user,ContactWriteRequest r) throws Exception {
        var scope=scope(user,"create");validate(r);positive(r.customerId);
        return transaction(c->{
            customer(c,r.customerId,scope,true);
            if(Boolean.TRUE.equals(r.isPrimary)) dao.clearPrimary(c,r.customerId);
            return dao.find(c,dao.create(c,r),false);
        });
    }
    public Map<String,Object> update(long user,long id,ContactWriteRequest r) throws Exception {
        var scope=scope(user,"update");validate(r);
        return transaction(c->{
            var old=dao.find(c,id,true);long company=customerId(old);customer(c,company,scope,true);
            if(r.customerId!=null && r.customerId!=company) throw new IllegalArgumentException("Dùng API transfer để chuyển công ty và lưu lịch sử");
            boolean primary=r.isPrimary==null?(Boolean)old.get("isPrimary"):r.isPrimary;
            if(primary) dao.clearPrimary(c,company);
            dao.update(c,id,r,primary);return dao.find(c,id,false);
        });
    }
    public Map<String,Object> primary(long user,long id) throws Exception {
        var scope=scope(user,"update");
        return transaction(c->{
            var old=dao.find(c,id,true);long company=customerId(old);customer(c,company,scope,true);
            dao.clearPrimary(c,company);
            dao.execute(c,"UPDATE contacts SET is_primary=1 WHERE id=? AND is_deleted=0",id);
            return dao.find(c,id,false);
        });
    }
    public Map<String,Object> transfer(long user,long id,ContactTransferRequest r) throws Exception {
        var scope=scope(user,"update");
        if(r==null) throw new IllegalArgumentException("Thiếu nội dung chuyển công ty");positive(r.customerId);
        if(r.note!=null && r.note.length()>1000) throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự");
        return transaction(c->{
            var old=dao.find(c,id,true);long from=customerId(old),to=r.customerId;
            if(from==to) throw new IllegalArgumentException("Công ty mới phải khác công ty hiện tại");
            customer(c,Math.min(from,to),scope,true);customer(c,Math.max(from,to),scope,true);
            boolean primary=Boolean.TRUE.equals(r.isPrimary);
            if(primary) dao.clearPrimary(c,to);
            dao.execute(c,"UPDATE contacts SET customer_id=?,is_primary=? WHERE id=? AND is_deleted=0",to,primary,id);
            dao.execute(c,"INSERT INTO contact_company_history(contact_id,from_customer_id,to_customer_id,changed_by,note) VALUES(?,?,?,?,?)",id,from,to,user,r.note);
            return dao.find(c,id,false);
        });
    }
    public void delete(long user,long id) throws Exception {
        var scope=scope(user,"delete");
        transaction(c->{
            var row=dao.find(c,id,true);customer(c,customerId(row),scope,true);
            dao.execute(c,"UPDATE contacts SET is_deleted=1,is_primary=0 WHERE id=?",id);return null;
        });
    }
    public static void positive(Long id) {
        if(id==null || id<=0) throw new IllegalArgumentException("customerId phải là số nguyên dương");
    }
    public static void validate(ContactWriteRequest r) {
        if(r==null) throw new IllegalArgumentException("Thiếu nội dung người liên hệ");
        r.fullName=required(r.fullName,"fullName",200);
        r.title=required(r.title,"title",150);
        r.email=required(r.email,"email",255);
        r.phone=required(r.phone,"phone",50);
        if(!r.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Email không hợp lệ");
        if(!r.phone.matches("^[+()0-9 .-]{3,50}$") || r.phone.replaceAll("\\D","").length()<3) throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        r.buyingRole=required(r.buyingRole,"buyingRole",40);
        if(!Set.of("DECISION_MAKER","INFLUENCER","END_USER","BLOCKER").contains(r.buyingRole))
            throw new IllegalArgumentException("buyingRole: DECISION_MAKER, INFLUENCER, END_USER hoặc BLOCKER");
    }
    private static String required(String value,String field,int max) {
        if(value==null || value.isBlank()) throw new IllegalArgumentException(field+" là bắt buộc");
        value=value.trim();
        if(value.length()>max) throw new IllegalArgumentException(field+" tối đa "+max+" ký tự");
        return value;
    }
}
