package com.crm.service.importer;

import com.crm.config.DatabaseConfig;
import com.crm.dao.customers.CustomerImportDAO;
import com.crm.dto.customers.CustomerImportConfirmRequest;
import com.crm.service.permissions.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL JDBC tests use session-local TEMPORARY tables, never CRM rows. */
class CustomerImportJdbcTest {
    private Connection connection;
    private CustomerImportService service;
    private final CustomerImportDAO dao=new CustomerImportDAO() {
        @Override public Connection open() {
            // Service closes each borrowed handle; only @AfterEach closes the physical test session.
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},
                (p,m,a)->{
                    if(m.getName().equals("close")) return null;
                    try {return m.invoke(connection,a);}
                    catch(InvocationTargetException e) {throw e.getCause();}
                });
        }
        @Override public void requireUniqueTaxCode(Connection c) throws SQLException {
            // information_schema excludes temporary tables. Verify their key using SHOW INDEX.
            try(Statement p=c.createStatement();ResultSet r=p.executeQuery("SHOW INDEX FROM customers")) {
                boolean found=false;
                while(r.next()) if(r.getString("Key_name").equals("uq_s3_06_customer_tax_key") && !r.getBoolean("Non_unique")) found=true;
                assertTrue(found);
            }
        }
    };
    @BeforeEach void connect() throws Exception {
        connection=DatabaseConfig.getConnection();
        try(Statement p=connection.createStatement()) {
            p.execute("""
                CREATE TEMPORARY TABLE customers(
                 id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(255) NOT NULL,tax_code VARCHAR(50),
                 status VARCHAR(50) NOT NULL,email VARCHAR(255),phone VARCHAR(50),website VARCHAR(255),
                 address VARCHAR(500),industry_id BIGINT,company_size_id BIGINT,owner_user_id BIGINT NOT NULL,
                 is_deleted BOOLEAN NOT NULL DEFAULT FALSE,updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                 tax_code_import_key VARCHAR(50) GENERATED ALWAYS AS
                   (NULLIF(REPLACE(TRIM(tax_code),'-',''),'')) STORED,
                 UNIQUE KEY uq_s3_06_customer_tax_key(tax_code_import_key)
                ) ENGINE=InnoDB
                """);
            p.execute("CREATE TEMPORARY TABLE master_data(id BIGINT PRIMARY KEY,type VARCHAR(50),active BOOLEAN)");
        }
        service=new CustomerImportService(dao,new CustomerExcelReader(),
            (user,action)->new DataScopeContext(user,"customer",ScopeType.SELF,null,Set.of(user)),Clock.systemUTC());
    }
    @AfterEach void close() throws Exception {if(connection!=null) connection.close();}

    private Map<String,Object> preview(String[][] rows) throws Exception {
        try(XSSFWorkbook book=new XSSFWorkbook()) {
            var sheet=book.createSheet("Customers");var header=sheet.createRow(0);
            for(int i=0;i<CustomerExcelReader.COLUMNS.size();i++) header.createCell(i).setCellValue(CustomerExcelReader.COLUMNS.get(i));
            for(int index=0;index<rows.length;index++) {
                var row=sheet.createRow(index+1);
                for(int col=0;col<9;col++) row.createCell(col).setCellValue(col<rows[index].length?rows[index][col]:"");
            }
            var out=new ByteArrayOutputStream();book.write(out);
            return service.preview(new ByteArrayInputStream(out.toByteArray()),1);
        }
    }
    private Map<String,Object> confirm(Map<String,Object> batch,String mode) throws Exception {
        var r=new CustomerImportConfirmRequest();r.batchToken=(String)batch.get("batchToken");r.duplicateMode=mode;
        return service.confirm(r,1);
    }
    private long count() throws SQLException {
        try(Statement p=connection.createStatement();ResultSet r=p.executeQuery("SELECT COUNT(*) FROM customers")) {r.next();return r.getLong(1);}
    }
    @Test void realJdbcPreviewCreateSkipUpdateAndScope() throws Exception {
        var batch=preview(new String[][]{{"Initial","0123456789"},{"","1123456789"}});
        assertEquals(0,count());
        var first=confirm(batch,"SKIP");assertEquals(1,first.get("created"));assertEquals(1,first.get("errorCount"));
        assertEquals(1,count());
        assertEquals(1,confirm(preview(new String[][]{{"Ignored","0123456789"}}),"SKIP").get("skipped"));
        assertEquals(1,confirm(preview(new String[][]{{"Updated","0123456789"}}),"UPDATE").get("updated"));
        assertEquals(1,confirm(preview(new String[][]{{"Updated","0123456789"}}),"UPDATE").get("updated"),"Identical update still counts as processed");
        try(Statement p=connection.createStatement();ResultSet r=p.executeQuery("SELECT name,owner_user_id FROM customers")) {
            r.next();assertEquals("Updated",r.getString(1));assertEquals(1,r.getLong(2));
        }
        try(Statement p=connection.createStatement()) {p.executeUpdate("UPDATE customers SET owner_user_id=2");}
        assertEquals(1,confirm(preview(new String[][]{{"Forbidden","0123456789"}}),"UPDATE").get("errorCount"));
        assertEquals(1,count());
    }
    @Test void mysqlUniqueKeyAndRollbackKeepStateConsistent() throws Exception {
        var batch=preview(new String[][]{{"Branch","0123456789-001"}});
        assertEquals(1,confirm(batch,"SKIP").get("created"));
        assertEquals(1,preview(new String[][]{{"Same","0123456789001"}}).get("duplicateCount"));
        connection.setAutoCommit(false);
        var row=new CustomerExcelReader().request(new CustomerExcelReader().read(new ByteArrayInputStream(templateWithRow("Duplicate","0123456789001"))).getFirst());
        assertThrows(SQLException.class,()->dao.insert(connection,row,1));
        connection.rollback();
        assertEquals(1,count());
        row.setTaxCode("1123456789");
        dao.insert(connection,row,1);connection.rollback();
        assertEquals(1,count(),"New row was rolled back in MySQL");
    }
    private byte[] templateWithRow(String name,String tax) throws Exception {
        try(XSSFWorkbook book=new XSSFWorkbook()) {
            var sheet=book.createSheet();var header=sheet.createRow(0);
            for(int i=0;i<9;i++) header.createCell(i).setCellValue(CustomerExcelReader.COLUMNS.get(i));
            var row=sheet.createRow(1);
            for(int i=0;i<9;i++) row.createCell(i).setCellValue(i==0?name:i==1?tax:"");
            var out=new ByteArrayOutputStream();book.write(out);return out.toByteArray();
        }
    }
}
