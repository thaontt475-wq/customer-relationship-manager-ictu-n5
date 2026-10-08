package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.dto.customers.CustomerWriteRequest;
import java.sql.*;
import java.util.*;

/** Import-specific JDBC operations; every write uses the caller's transaction. */
public class CustomerImportDAO {
    public record Existing(long id,long ownerUserId,boolean deleted) {}
    public Connection open() throws SQLException { return DatabaseConfig.getConnection(); }

    public Existing findByTaxCode(Connection c,String taxCode,boolean lock) throws SQLException {
        if(taxCode==null) return null;
        try(PreparedStatement p=c.prepareStatement(
                "SELECT id,owner_user_id,is_deleted FROM customers WHERE tax_code_import_key=?"+(lock?" FOR UPDATE":""))) {
            p.setString(1,taxCode.replace("-",""));
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) return null;
                Existing result=new Existing(r.getLong(1),r.getLong(2),r.getBoolean(3));
                if(r.next()) throw new SQLException("Customer tax code is ambiguous");
                return result;
            }
        }
    }

    public boolean referenceExists(Connection c,Long id,String type) throws SQLException {
        if(id==null) return true;
        try(PreparedStatement p=c.prepareStatement("SELECT id FROM master_data WHERE id=? AND type=? AND active=1 FOR SHARE")) {
            p.setLong(1,id);p.setString(2,type);
            try(ResultSet r=p.executeQuery()) {return r.next();}
        }
    }

    /** Old schemas upgraded by 009 may not have the unique key supplied by 012. */
    public void requireUniqueTaxCode(Connection c) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("""
                SELECT INDEX_NAME FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers'
                  AND NON_UNIQUE=0
                GROUP BY INDEX_NAME
                HAVING COUNT(*)=1 AND MAX(COLUMN_NAME)='tax_code_import_key' AND MAX(SUB_PART) IS NULL
                """); ResultSet r=p.executeQuery()) {
            if(!r.next()) throw new SQLException("Apply migration 015_s3_06_customer_import.sql before importing");
        }
    }

    public void insert(Connection c,CustomerWriteRequest row,long user) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("""
                INSERT INTO customers(name,tax_code,status,email,phone,website,address,
                                      industry_id,company_size_id,owner_user_id)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """)) {
            bind(p,row);p.setLong(10,user);
            if(p.executeUpdate()!=1) throw new SQLException("Customer insert did not persist");
        }
    }

    /** Owner, parent, custom fields and all unrelated fields are retained on UPDATE. */
    public void update(Connection c,long id,CustomerWriteRequest row) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("""
                UPDATE customers SET name=?,tax_code=?,status=?,email=?,phone=?,website=?,
                    address=?,industry_id=?,company_size_id=?,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND is_deleted=0
                """)) {
            bind(p,row);p.setLong(10,id);
            // MySQL can report 0 affected rows for an identical UPDATE; existence is locked by the service.
            p.executeUpdate();
        }
    }

    private void bind(PreparedStatement p,CustomerWriteRequest r) throws SQLException {
        p.setString(1,r.getName());p.setString(2,r.getTaxCode());p.setString(3,r.getStatus());
        p.setString(4,r.getEmail());p.setString(5,r.getPhone());p.setString(6,r.getWebsite());
        p.setString(7,r.getAddress());p.setObject(8,r.getIndustryId(),Types.BIGINT);
        p.setObject(9,r.getCompanySizeId(),Types.BIGINT);
    }
}
