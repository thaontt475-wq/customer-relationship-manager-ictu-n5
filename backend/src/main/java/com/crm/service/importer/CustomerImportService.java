package com.crm.service.importer;

import com.crm.dao.customers.CustomerImportDAO;
import com.crm.dto.customers.*;
import com.crm.service.permissions.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomerImportService {
    @FunctionalInterface public interface ScopeResolver {
        DataScopeContext resolve(long user,String action) throws Exception;
    }
    private final CustomerImportDAO dao;
    private final CustomerExcelReader excel;
    private final ScopeResolver scopes;
    private final Clock clock;
    private final Map<String,Batch> batches=new ConcurrentHashMap<>();
    private static final Duration TTL=Duration.ofMinutes(15);
    private record PreviewRow(CustomerExcelReader.ImportRow row,Long duplicateId,boolean duplicate,List<String> errors) {
        private PreviewRow {errors=List.copyOf(errors);}
    }
    private record Batch(long owner,Instant expires,List<PreviewRow> rows) {}

    public CustomerImportService() {
        this(new CustomerImportDAO(),new CustomerExcelReader(),(user,action)->new DataScopeService().resolve(user,"customer",action),Clock.systemUTC());
    }
    // Explicit dependencies keep parsing, token lifecycle and authorization independently testable.
    public CustomerImportService(CustomerImportDAO dao,CustomerExcelReader excel,ScopeResolver scopes,Clock clock) {
        this.dao=dao;this.excel=excel;this.scopes=scopes;this.clock=clock;
    }

    public void writeTemplate(OutputStream out,long user) throws Exception {
        scopes.resolve(user,"create");excel.writeTemplate(out);
    }

    public Map<String,Object> preview(InputStream input,long user) throws Exception {
        scopes.resolve(user,"create");
        DataScopeContext read=scopes.resolve(user,"read");
        DataScopeContext update;
        try {update=scopes.resolve(user,"update");} catch(SecurityException e) {update=null;}
        List<CustomerExcelReader.ImportRow> rows=excel.read(input);
        List<PreviewRow> saved=new ArrayList<>();
        List<Map<String,Object>> validRows=new ArrayList<>(),errorRows=new ArrayList<>();
        int duplicates=0;
        try(Connection c=dao.open()) {
            dao.requireUniqueTaxCode(c);
            for(var row:rows) {
                List<String> errors=new ArrayList<>(row.errors());
                CustomerImportDAO.Existing existing=null;
                if(errors.isEmpty()) {
                    var request=excel.request(row);
                    validateReferences(c,request,errors);
                }
                String taxCode=row.values().get("taxCode");
                if(!taxCode.isBlank() && taxCode.matches("[0-9]{10}(-?[0-9]{3})?")) {
                    existing=dao.findByTaxCode(c,taxCode,false);
                    if(existing!=null && existing.deleted()) errors.add("taxCode thuộc khách hàng đã xóa; không tự khôi phục hoặc ghi đè");
                }
                boolean duplicate=existing!=null || row.errors().stream().anyMatch(e->e.contains("trùng trong file"));
                if(duplicate) duplicates++;
                // Cache an internal ID, but only expose it when the reader owns the appropriate scope.
                Long existingId=existing==null?null:existing.id();
                saved.add(new PreviewRow(row,existingId,duplicate,errors));
                Map<String,Object> item=item(row,errors);
                item.put("duplicate",duplicate);
                item.put("duplicateSource",existing!=null?"DATABASE":duplicate?"FILE":null);
                item.put("canUpdate",existing!=null && !existing.deleted() && errors.isEmpty() && update!=null && update.canAccessOwner(existing.ownerUserId()));
                if(existing!=null && read.canAccessOwner(existing.ownerUserId())) item.put("customerId",existing.id());
                if(errors.isEmpty()) validRows.add(item);else errorRows.add(item);
            }
        }
        String token;
        synchronized(batches) {
            batches.entrySet().removeIf(e->!e.getValue().expires().isAfter(clock.instant()));
            long own=batches.values().stream().filter(b->b.owner()==user).count();
            if(batches.size()>=64 || own>=3) throw new IllegalStateException("Đã đạt giới hạn preview; hãy confirm hoặc chờ 15 phút");
            token=UUID.randomUUID().toString();
            batches.put(token,new Batch(user,clock.instant().plus(TTL),List.copyOf(saved)));
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("batchToken",token);result.put("expiresInSeconds",TTL.toSeconds());
        result.put("totalRows",rows.size());result.put("validCount",validRows.size());
        result.put("errorCount",errorRows.size());result.put("duplicateCount",duplicates);
        result.put("validRows",validRows);result.put("errorRows",errorRows);
        return result;
    }

    public Map<String,Object> confirm(CustomerImportConfirmRequest request,long user) throws Exception {
        if(request==null || request.batchToken==null || request.batchToken.isBlank()) throw invalidBatch();
        if(!Set.of("SKIP","UPDATE").contains(String.valueOf(request.duplicateMode))) throw new IllegalArgumentException("duplicateMode phải là SKIP hoặc UPDATE");
        DataScopeContext create=scopes.resolve(user,"create");
        DataScopeContext update=request.duplicateMode.equals("UPDATE")?scopes.resolve(user,"update"):null;
        Batch batch;
        synchronized(batches) {
            batch=batches.get(request.batchToken);
            if(batch==null) throw invalidBatch();
            if(batch.owner()!=user) throw new SecurityException("Preview không thuộc người dùng hiện tại");
            if(!batch.expires().isAfter(clock.instant())) {batches.remove(request.batchToken);throw invalidBatch();}
            // Check schema before consuming the token; a configuration outage should not lose a preview.
            try(Connection c=dao.open()) {dao.requireUniqueTaxCode(c);}
            batches.remove(request.batchToken);
        }
        int created=0,updated=0,skipped=0;
        List<Map<String,Object>> errors=new ArrayList<>();
        for(PreviewRow preview:batch.rows()) {
            if(!preview.errors().isEmpty()) {errors.add(error(preview.row().row(),preview.errors()));continue;}
            try {
                String outcome=process(preview,user,request.duplicateMode,create,update);
                switch(outcome) {case "CREATED" -> created++;case "UPDATED" -> updated++;case "SKIPPED" -> skipped++;default -> throw new IllegalStateException("Unknown import outcome");}
            } catch(SecurityException | IllegalArgumentException e) {
                errors.add(error(preview.row().row(),List.of(e.getMessage())));
            } catch(SQLException e) {
                String message=e.getErrorCode()==1062?"taxCode đã được tạo đồng thời; hãy preview lại":
                        e.getErrorCode()==1213 || e.getErrorCode()==1205?"Dữ liệu bị khóa hoặc xung đột; hãy preview lại":"Không thể ghi khách hàng; dòng đã rollback";
                errors.add(error(preview.row().row(),List.of(message)));
            }
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("totalRows",batch.rows().size());result.put("created",created);
        result.put("updated",updated);result.put("skipped",skipped);
        result.put("errorCount",errors.size());result.put("errors",errors);
        return result;
    }

    private String process(PreviewRow preview,long user,String mode,DataScopeContext create,DataScopeContext update) throws SQLException {
        try(Connection c=dao.open()) {
            // Read committed avoids gap-lock deadlocks between independent new customer inserts.
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try {
                var request=excel.request(preview.row());
                var existing=dao.findByTaxCode(c,request.getTaxCode(),true);
                String outcome;
                if(existing!=null && mode.equals("SKIP")) outcome="SKIPPED";
                else {
                    if(existing!=null && existing.deleted()) throw new IllegalArgumentException("Không cập nhật khách hàng đã xóa");
                    if(existing!=null && (preview.duplicateId()==null || preview.duplicateId()!=existing.id()))
                        throw new IllegalArgumentException("Bản ghi trùng đã thay đổi sau preview; hãy preview lại");
                    if(existing==null && preview.duplicateId()!=null)
                        throw new IllegalArgumentException("Khách hàng đã thay đổi hoặc bị xóa sau preview; hãy preview lại");
                    List<String> referenceErrors=new ArrayList<>();
                    validateReferences(c,request,referenceErrors);
                    if(!referenceErrors.isEmpty()) throw new IllegalArgumentException(String.join("; ",referenceErrors));
                    if(existing==null) {
                        if(!create.canAccessOwner(user)) throw new SecurityException("Không có quyền tạo Customer trong phạm vi hiện tại");
                        dao.insert(c,request,user);outcome="CREATED";
                    } else {
                        if(update==null || !update.canAccessOwner(existing.ownerUserId())) throw new SecurityException("Customer trùng nằm ngoài phạm vi UPDATE SELF/TEAM/ALL");
                        dao.update(c,existing.id(),request);outcome="UPDATED";
                    }
                }
                c.commit();return outcome;
            } catch(SQLException | RuntimeException e) {
                try {c.rollback();}catch(SQLException rollback) {e.addSuppressed(rollback);}
                throw e;
            }
        }
    }

    private void validateReferences(Connection c,CustomerWriteRequest r,List<String> errors) throws SQLException {
        if(!dao.referenceExists(c,r.getIndustryId(),"industry")) errors.add("industryId không tồn tại hoặc không hoạt động");
        if(!dao.referenceExists(c,r.getCompanySizeId(),"company-size")) errors.add("companySizeId không tồn tại hoặc không hoạt động");
    }
    private Map<String,Object> item(CustomerExcelReader.ImportRow row,List<String> errors) {
        Map<String,Object> item=new LinkedHashMap<>();
        item.put("row",row.row());item.putAll(row.values());item.put("valid",errors.isEmpty());
        item.put("errors",List.copyOf(errors));return item;
    }
    private Map<String,Object> error(int row,List<String> errors) {return Map.of("row",row,"errors",List.copyOf(errors));}
    private IllegalArgumentException invalidBatch() {return new IllegalArgumentException("batchToken không hợp lệ, hết hạn hoặc đã sử dụng");}
}
