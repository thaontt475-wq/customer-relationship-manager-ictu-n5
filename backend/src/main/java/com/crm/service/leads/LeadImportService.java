package com.crm.service.leads;

import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.LeadValidationException.FieldError;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LeadImportService {
    public static class Failure extends Exception {
        private final int status;
        private final Object data;
        public Failure(int status,String message,Object data) {super(message);this.status=status;this.data=data;}
        public Failure(int status,String message,Object data,Throwable cause) {super(message,cause);this.status=status;this.data=data;}
        public int status() {return status;}
        public Object data() {return data;}
    }
    private record Batch(long owner,Instant expires,List<LeadExcelReader.ImportRow> rows) {}
    private final Map<String,Batch> batches=new ConcurrentHashMap<>();
    private final LeadDAO dao;
    private final LeadService leads;
    private final LeadExcelReader excel;
    private final Clock clock;
    public LeadImportService() {this(new LeadDAO(),new LeadService(),new LeadExcelReader(),Clock.systemUTC());}
    public LeadImportService(LeadDAO dao,LeadService leads,LeadExcelReader excel,Clock clock) {
        this.dao=dao;this.leads=leads;this.excel=excel;this.clock=clock;
    }
    public void authorize(long user) throws Exception {leads.authorizeCreate(user);}
    public void template(long user,OutputStream out) throws Exception {authorize(user);excel.template(out);}
    public Map<String,Object> preview(long user,InputStream input) throws Exception {
        authorize(user);
        List<LeadExcelReader.ImportRow> rows;
        try(Connection c=dao.open()) {rows=excel.read(input,dao.sources(c,false));}
        String token=UUID.randomUUID().toString();
        synchronized(batches) {
            batches.entrySet().removeIf(e->!e.getValue().expires().isAfter(clock.instant()));
            if(batches.size()>=64 || batches.values().stream().filter(b->b.owner()==user).count()>=5)
                throw new Failure(429,"Đã đạt giới hạn preview; confirm hoặc chờ 15 phút",null);
            batches.put(token,new Batch(user,clock.instant().plusSeconds(900),rows));
        }
        List<LeadExcelReader.ImportRow> valid=rows.stream().filter(r->r.errors().isEmpty()).toList();
        List<LeadExcelReader.ImportRow> invalid=rows.stream().filter(r->!r.errors().isEmpty()).toList();
        return Map.of("batchToken",token,"expiresInSeconds",900,"totalRows",rows.size(),"validCount",valid.size(),
                "errorCount",invalid.size(),"validRows",valid,"errorRows",invalid);
    }
    public Map<String,Object> confirm(long user,String token) throws Exception {
        authorize(user);
        if(token==null || !token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw new IllegalArgumentException("batchToken không hợp lệ");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        Batch batch=batches.get(token);
        if(batch==null) {
            try(Connection c=dao.open()) {checkReceipt(c,hash,user);}
            throw new IllegalArgumentException("Preview không tồn tại hoặc hết hạn; hãy upload lại");
        }
        synchronized(batch) {
            if(batch.owner()!=user)throw new SecurityException("Preview không thuộc người dùng hiện tại");
            if(!batch.expires().isAfter(clock.instant())) {batches.remove(token,batch);throw new IllegalArgumentException("Preview đã hết hạn; hãy upload lại");}
            List<LeadExcelReader.ImportRow> valid=batch.rows().stream().filter(r->r.errors().isEmpty()).toList();
            List<Map<String,Object>> errors=new ArrayList<>();
            batch.rows().stream().filter(r->!r.errors().isEmpty()).forEach(r->errors.add(Map.of("row",r.row(),"errors",r.errors())));
            try(Connection c=dao.open()) {
                c.setAutoCommit(false);
                try {
                    checkReceipt(c,hash,user);
                    var sources=dao.sources(c,true);
                    for(var row:valid) {
                        try {LeadService.requireSource(row.lead(),sources);}
                        catch(IllegalArgumentException error) {throw new Failure(400,"Nguồn Lead đã thay đổi; giao dịch rollback",rollbackReport(batch,"sourceId","Nguồn Lead thay đổi; hãy preview lại"));}
                    }
                    dao.receipt(c,hash,user,batch.rows().size(),valid.size(),errors.size());
                    List<Long> ids=new ArrayList<>();
                    for(var row:valid)ids.add(dao.insert(c,row.lead(),user,hash));
                    c.commit();batches.remove(token,batch);
                    return Map.of("totalRows",batch.rows().size(),"successCount",ids.size(),"failureCount",errors.size(),"leadIds",ids,"errors",errors);
                } catch(Exception error) {c.rollback();throw error;}
            } catch(SQLException error) {
                if(error.getErrorCode()==1062)throw new Failure(409,"Batch đã được xác nhận",null,error);
                throw new Failure(500,"Không thể import Lead; toàn bộ dòng hợp lệ đã rollback",rollbackReport(batch,"database","Giao dịch rollback do lỗi Database; có thể retry khi token còn hạn"),error);
            }
        }
    }
    private void checkReceipt(Connection c,String hash,long user) throws Exception {
        Long owner=dao.receiptOwner(c,hash);
        if(owner==null)return;
        if(owner!=user)throw new SecurityException("Batch không thuộc người dùng hiện tại");
        throw new Failure(409,"Batch đã được xác nhận; không tạo thêm Lead",null);
    }
    private Map<String,Object> rollbackReport(Batch batch,String field,String message) {
        List<Map<String,Object>> errors=new ArrayList<>();
        for(var row:batch.rows())errors.add(Map.of("row",row.row(),"errors",row.errors().isEmpty()?List.of(new FieldError(field,message)):row.errors()));
        return Map.of("totalRows",batch.rows().size(),"successCount",0,"failureCount",batch.rows().size(),"leadIds",List.of(),"errors",errors);
    }
}
