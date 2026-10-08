package com.crm.service.importer;

import com.crm.dao.customers.CustomerImportDAO;
import com.crm.dto.customers.*;
import com.crm.service.permissions.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated doubles exercise business rules and commit/rollback; no application DB is written. */
class CustomerImportServiceTest {
    static class Transaction implements InvocationHandler {
        Runnable pending=()->{};int commits,rollbacks;
        @Override public Object invoke(Object proxy,Method method,Object[] args) {
            switch(method.getName()) {
                case "commit" -> {pending.run();pending=()->{};commits++;}
                case "rollback" -> {pending=()->{};rollbacks++;}
            }
            if(method.getReturnType()==boolean.class) return false;
            if(method.getReturnType()==int.class) return 0;
            return null;
        }
    }
    static class TestDAO extends CustomerImportDAO {
        final Map<String,Existing> existing=new HashMap<>();
        final Map<Long,String> names=new HashMap<>();
        final List<Transaction> transactions=new ArrayList<>();
        long next=100;
        boolean failWrite,missingReference;
        @Override public Connection open() {
            Transaction t=new Transaction();transactions.add(t);
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},t);
        }
        @Override public void requireUniqueTaxCode(Connection c) {}
        @Override public Existing findByTaxCode(Connection c,String tax,boolean lock) {return tax==null?null:existing.get(tax.replace("-",""));}
        @Override public boolean referenceExists(Connection c,Long id,String type) {return id==null || !missingReference;}
        @Override public void insert(Connection c,CustomerWriteRequest row,long user) throws SQLException {
            long id=next++;String tax=row.getTaxCode(),name=row.getName();
            ((Transaction)Proxy.getInvocationHandler(c)).pending=()->{
                if(tax!=null) existing.put(tax.replace("-",""),new Existing(id,user,false));names.put(id,name);
            };
            if(failWrite && name.equals("Fail")) throw new SQLException("database failure","23000",1062);
        }
        @Override public void update(Connection c,long id,CustomerWriteRequest row) {
            ((Transaction)Proxy.getInvocationHandler(c)).pending=()->names.put(id,row.getName());
        }
    }
    static class MutableClock extends Clock {
        Instant now=Instant.parse("2026-10-08T00:00:00Z");
        @Override public ZoneId getZone() {return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone) {return this;}
        @Override public Instant instant() {return now;}
    }
    static class TestScopes implements CustomerImportService.ScopeResolver {
        ScopeType type=ScopeType.SELF;
        Set<Long> owners=Set.of(1L);
        boolean allowUpdate=true,allowCreate=true;
        @Override public DataScopeContext resolve(long user,String action) {
            if(action.equals("update") && !allowUpdate || action.equals("create") && !allowCreate) throw new SecurityException("No permission");
            return new DataScopeContext(user,"customer",type,1L,owners);
        }
    }
    private final TestDAO dao=new TestDAO();
    private final TestScopes scopes=new TestScopes();
    private final MutableClock clock=new MutableClock();
    private final CustomerImportService service=new CustomerImportService(dao,new CustomerExcelReader(),scopes,clock);

    private Map<String,Object> preview(String[][] rows) throws Exception {
        return service.preview(new ByteArrayInputStream(file(rows)),1);
    }
    private byte[] file(String[][] rows) throws Exception {
        try(XSSFWorkbook book=new XSSFWorkbook()) {
            Sheet sheet=book.createSheet("Customers");
            Row header=sheet.createRow(0);
            for(int i=0;i<CustomerExcelReader.COLUMNS.size();i++) header.createCell(i).setCellValue(CustomerExcelReader.COLUMNS.get(i));
            for(int i=0;i<rows.length;i++) {
                Row row=sheet.createRow(i+1);
                for(int col=0;col<9;col++) row.createCell(col).setCellValue(col<rows[i].length?rows[i][col]:"");
            }
            var out=new ByteArrayOutputStream();book.write(out);return out.toByteArray();
        }
    }
    private Map<String,Object> confirm(Map<String,Object> preview,String mode,long user) throws Exception {
        var request=new CustomerImportConfirmRequest();
        request.batchToken=(String)preview.get("batchToken");request.duplicateMode=mode;
        return service.confirm(request,user);
    }
    @SuppressWarnings("unchecked") private Map<String,Object> first(Map<String,Object> preview,String key) {return ((List<Map<String,Object>>)preview.get(key)).getFirst();}

    @Test void previewMarksDuplicatesWithoutWritingOrDisclosingOutsideScope() throws Exception {
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,2,false));
        var preview=preview(new String[][]{{"Import","0123456789"}});
        assertEquals(1,preview.get("duplicateCount"));
        assertEquals(true,first(preview,"validRows").get("duplicate"));
        assertEquals(false,first(preview,"validRows").get("canUpdate"));
        assertFalse(first(preview,"validRows").containsKey("customerId"));
        assertTrue(dao.names.isEmpty());
        assertEquals(0,dao.transactions.stream().mapToInt(t->t.commits).sum());
    }
    @Test void reportsNewUpdatedSkippedAndInvalidRowsSeparately() throws Exception {
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,1,false));
        var preview=preview(new String[][]{{"Updated","0123456789"},{"New","1123456789"},{"","2123456789"}});
        var result=confirm(preview,"UPDATE",1);
        assertEquals(3,result.get("totalRows"));assertEquals(1,result.get("created"));
        assertEquals(1,result.get("updated"));assertEquals(0,result.get("skipped"));
        assertEquals(1,result.get("errorCount"));assertEquals("Updated",dao.names.get(20L));
        assertEquals(2,dao.transactions.stream().mapToInt(t->t.commits).sum());
    }
    @Test void skipDoesNotRequireUpdatePermissionOrOverwrite() throws Exception {
        scopes.allowUpdate=false;
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,2,false));
        dao.names.put(20L,"Retain");
        var result=confirm(preview(new String[][]{{"Overwrite","0123456789"},{"New","1123456789"}}),"SKIP",1);
        assertEquals(1,result.get("skipped"));assertEquals(1,result.get("created"));assertEquals("Retain",dao.names.get(20L));
    }
    @Test void updateHonorsSelfTeamAndAllScopes() throws Exception {
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,2,false));
        var denied=confirm(preview(new String[][]{{"Denied","0123456789"}}),"UPDATE",1);
        assertEquals(1,denied.get("errorCount"));assertFalse(dao.names.containsKey(20L));
        scopes.type=ScopeType.TEAM;scopes.owners=Set.of(1L,2L);
        var team=confirm(preview(new String[][]{{"Team","0123456789"}}),"UPDATE",1);
        assertEquals(1,team.get("updated"));
        scopes.type=ScopeType.ALL;scopes.owners=Set.of();
        var all=confirm(preview(new String[][]{{"All","0123456789"}}),"UPDATE",1);
        assertEquals(1,all.get("updated"));assertEquals("All",dao.names.get(20L));
    }
    @Test void changedOwnershipAfterPreviewIsRechecked() throws Exception {
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,1,false));
        var batch=preview(new String[][]{{"Updated","0123456789"}});
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,2,false));
        assertEquals(1,confirm(batch,"UPDATE",1).get("errorCount"));
        assertTrue(dao.names.isEmpty());
    }
    @Test void doesNotOverwriteNewDuplicateThatWasNotPreviewed() throws Exception {
        var batch=preview(new String[][]{{"Overwrite","0123456789"}});
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,1,false));
        assertEquals(1,confirm(batch,"UPDATE",1).get("errorCount"));
        assertTrue(dao.names.isEmpty());
    }
    @Test void failedRowRollsBackWhileOtherRowsContinue() throws Exception {
        dao.failWrite=true;
        var result=confirm(preview(new String[][]{{"Fail","0123456789"},{"Good","1123456789"}}),"SKIP",1);
        assertEquals(1,result.get("created"));assertEquals(1,result.get("errorCount"));
        assertFalse(dao.existing.containsKey("0123456789"));assertTrue(dao.existing.containsKey("1123456789"));
        assertEquals(1,dao.transactions.stream().mapToInt(t->t.rollbacks).sum());
    }
    @Test void tokensAreOwnedSingleUseAndExpire() throws Exception {
        var batch=preview(new String[][]{});
        assertThrows(SecurityException.class,()->confirm(batch,"SKIP",2));
        assertEquals(0,confirm(batch,"SKIP",1).get("created"));
        assertThrows(IllegalArgumentException.class,()->confirm(batch,"SKIP",1));
        var expired=preview(new String[][]{});
        clock.now=clock.now.plus(Duration.ofMinutes(16));
        assertThrows(IllegalArgumentException.class,()->confirm(expired,"SKIP",1));
    }
    @Test void permissionRevocationDoesNotConsumePreviewOrWrite() throws Exception {
        var batch=preview(new String[][]{{"A","0123456789"}});
        scopes.allowCreate=false;
        assertThrows(SecurityException.class,()->confirm(batch,"SKIP",1));
        assertTrue(dao.names.isEmpty());
        scopes.allowCreate=true;
        assertEquals(1,confirm(batch,"SKIP",1).get("created"));
    }
    @Test void invalidDuplicateChoiceCannotDefaultToOverwrite() throws Exception {
        var batch=preview(new String[][]{});
        assertThrows(IllegalArgumentException.class,()->confirm(batch,null,1));
        assertThrows(IllegalArgumentException.class,()->confirm(batch,"UPSERT",1));
        assertEquals(0,confirm(batch,"SKIP",1).get("created"));
    }
    @Test void deletedCustomersAndMissingReferencesAreReported() throws Exception {
        dao.existing.put("0123456789",new CustomerImportDAO.Existing(20,1,true));
        dao.missingReference=true;
        var p=preview(new String[][]{{"Archived","0123456789"},{"Bad reference","1123456789","","","","","","99"}});
        assertEquals(2,p.get("errorCount"));assertEquals(2,confirm(p,"SKIP",1).get("errorCount"));
    }
    @Test void capacityIsBoundedPerUser() throws Exception {
        preview(new String[][]{});preview(new String[][]{});preview(new String[][]{});
        assertThrows(IllegalStateException.class,()->preview(new String[][]{}));
        clock.now=clock.now.plus(Duration.ofMinutes(16));
        assertNotNull(preview(new String[][]{}).get("batchToken"));
    }
}
