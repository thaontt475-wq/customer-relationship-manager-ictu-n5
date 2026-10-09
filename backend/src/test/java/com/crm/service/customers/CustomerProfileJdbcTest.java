package com.crm.service.customers;

import com.crm.config.DatabaseConfig;
import com.crm.dao.customers.*;
import com.crm.dao.customfields.CustomFieldValueDAO;
import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.service.permissions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Session-local temporary tables shadow CRM tables; no persisted CRM data is modified. */
class CustomerProfileJdbcTest {
    private Connection connection;
    private ScopeType scope = ScopeType.SELF;
    private Set<Long> owners = Set.of(10L);
    private boolean failFields;
    private String deniedAction;
    private final CustomerDAO dao = new CustomerDAO() {
        @Override public Connection open() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("close")) return null;
                        try { return method.invoke(connection, args); }
                        catch (InvocationTargetException e) { throw e.getCause(); }
                    });
        }
    };

    private CustomerService service() {
        return new CustomerService(dao, new CustomFieldValueDAO() {
            @Override public void saveValues(Connection c, String entity, long id, Map<String, Object> values) throws SQLException {
                super.saveValues(c, entity, id, values);
                if (failFields) throw new SQLException("injected custom field failure");
            }
        }, new DataScopeService() {
            @Override public DataScopeContext resolve(long user, String module, String action) {
                if (action.equals(deniedAction)) throw new SecurityException("Missing customer permission");
                return new DataScopeContext(user, module, scope, null, owners);
            }
        }, new CustomerImportDAO() {
            @Override public void requireUniqueTaxCode(Connection c) throws SQLException {
                // information_schema excludes temporary tables; inspect the actual session table.
                try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SHOW INDEX FROM customers")) {
                    boolean unique = false;
                    while (rs.next()) unique |= "tax_code_import_key".equals(rs.getString("Column_name")) && !rs.getBoolean("Non_unique");
                    assertTrue(unique);
                }
            }
        });
    }

    @BeforeEach void setup() throws Exception {
        connection = DatabaseConfig.getConnection();
        try (Statement s = connection.createStatement()) {
            s.execute("""
                    CREATE TEMPORARY TABLE customers (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), tax_code VARCHAR(50), status VARCHAR(50),
                    email VARCHAR(255),phone VARCHAR(50),website VARCHAR(255),address VARCHAR(500),
                    industry_id BIGINT,company_size_id BIGINT,owner_user_id BIGINT,is_deleted TINYINT DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    tax_code_import_key VARCHAR(50) GENERATED ALWAYS AS(NULLIF(REPLACE(TRIM(tax_code),'-',''),'')) STORED,
                    UNIQUE KEY uq_s3_06_customer_tax_key(tax_code_import_key)) ENGINE=InnoDB
                    """);
            s.execute("CREATE TEMPORARY TABLE users(id BIGINT PRIMARY KEY,full_name VARCHAR(255),status VARCHAR(30)) ENGINE=InnoDB");
            s.execute("CREATE TEMPORARY TABLE master_data(id BIGINT PRIMARY KEY,type VARCHAR(50),active BOOLEAN) ENGINE=InnoDB");
            s.execute("CREATE TEMPORARY TABLE custom_fields(id BIGINT PRIMARY KEY,field_key VARCHAR(100),field_type VARCHAR(30),entity_type VARCHAR(50),active BOOLEAN) ENGINE=InnoDB");
            s.execute("CREATE TEMPORARY TABLE custom_field_values(custom_field_id BIGINT,record_id BIGINT,field_value TEXT,PRIMARY KEY(custom_field_id,record_id)) ENGINE=InnoDB");
            s.execute("INSERT INTO users VALUES(10,'Sales A','ACTIVE'),(20,'Sales B','ACTIVE'),(30,'Outside','ACTIVE'),(40,'Locked','LOCKED')");
            s.execute("INSERT INTO master_data VALUES(1,'industry',1),(2,'company-size',1),(3,'industry',0)");
            s.execute("INSERT INTO custom_fields VALUES(1,'note','TEXT','CUSTOMER',1)");
            s.execute("INSERT INTO customers(id,name,tax_code,status,owner_user_id) VALUES(1,'Own','0123456789-001','CHINH_THUC',10),(2,'Team',NULL,'TIEM_NANG',20),(3,'Outside',NULL,'TIEM_NANG',30)");
        }
    }

    @AfterEach void close() throws Exception { if (connection != null) connection.close(); }

    private CustomerWriteRequest request(String tax) {
        var req = new CustomerWriteRequest(); req.setName("New Company"); req.setTaxCode(tax);
        req.setIndustryId(1L); req.setCompanySizeId(2L); return req;
    }

    @Test void createReadUpdateAndSoftDeleteWithOptionalTax() throws Exception {
        var svc = service(); var req = request(" ");
        req.setCustomFields(Map.of("note", "original"));
        var created = svc.create(10, req); long id = ((Number) created.get("id")).longValue();
        assertNull(created.get("taxCode")); assertEquals(10L, created.get("ownerUserId"));
        assertEquals("original", ((Map<?, ?>) created.get("customFields")).get("note"));
        req.setStatus("NGUNG_HOP_TAC"); req.setTaxCode("1123456789");
        assertEquals("NGUNG_HOP_TAC", svc.update(10, id, req).get("status"));
        assertEquals("1123456789", svc.getById(10, id).get("taxCode"));
        svc.delete(10, id); assertNull(svc.getById(10, id));
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT is_deleted,tax_code FROM customers WHERE id=" + id)) {
            assertTrue(rs.next()); assertTrue(rs.getBoolean(1)); assertEquals("1123456789", rs.getString(2));
        }
        assertThrows(NoSuchElementException.class, () -> svc.delete(10, id));
    }

    @Test void duplicateTaxIsRejectedOnCreateUpdateAndAfterSoftDelete() throws Exception {
        var svc = service();
        SQLException create = assertThrows(SQLException.class, () -> svc.create(10, request("0123456789001")));
        assertEquals(1062, create.getErrorCode());
        scope = ScopeType.TEAM; owners = Set.of(10L, 20L);
        SQLException update = assertThrows(SQLException.class, () -> svc.update(10, 2, request("0123456789001")));
        assertEquals(1062, update.getErrorCode());
        assertNull(dao.findById(connection, 2, false).get("taxCode"));
        svc.update(10, 1, request("0123456789001")); // Same normalized MST is allowed on its own row.
        svc.delete(10, 1);
        assertEquals(1062, assertThrows(SQLException.class, () -> svc.create(10, request("0123456789-001"))).getErrorCode());
    }

    @Test void selfTeamAndAllProtectDetailUpdateDeleteAndOwnerAssignment() throws Exception {
        var svc = service();
        assertNotNull(svc.getById(10, 1));
        for (long id : List.of(2L, 3L)) {
            assertThrows(SecurityException.class, () -> svc.getById(10, id));
            assertThrows(SecurityException.class, () -> svc.update(10, id, request(null)));
            assertThrows(SecurityException.class, () -> svc.delete(10, id));
        }
        var req = request(null); req.setOwnerUserId(20L);
        assertThrows(SecurityException.class, () -> svc.create(10, req));
        assertThrows(SecurityException.class, () -> svc.update(10, 1, req));
        scope = ScopeType.TEAM; owners = Set.of(10L, 20L);
        assertNotNull(svc.getById(10, 2)); assertThrows(SecurityException.class, () -> svc.getById(10, 3));
        assertEquals(20L, svc.create(10, req).get("ownerUserId"));
        assertEquals(20L, svc.update(10, 1, req).get("ownerUserId"));
        scope = ScopeType.ALL; owners = Set.of();
        assertNotNull(svc.getById(10, 3)); svc.delete(10, 3);
    }

    @Test void invalidMasterDataAndInactiveOwnerAreRejectedWithoutWrites() throws Exception {
        var svc = service(); var req = request(null); req.setIndustryId(2L);
        assertThrows(IllegalArgumentException.class, () -> svc.create(10, req));
        req.setIndustryId(3L); assertThrows(IllegalArgumentException.class, () -> svc.create(10, req));
        req.setIndustryId(1L); req.setCompanySizeId(999L);
        assertThrows(IllegalArgumentException.class, () -> svc.update(10, 1, req));
        req.setCompanySizeId(2L); req.setOwnerUserId(40L); scope = ScopeType.ALL;
        assertThrows(IllegalArgumentException.class, () -> svc.create(10, req));
    }

    @Test void customFieldFailureRollsBackProfileAndCustomFieldsTogether() throws Exception {
        var svc = service(); var req = request("1123456789"); req.setCustomFields(Map.of("note", "failed"));
        failFields = true;
        assertThrows(SQLException.class, () -> svc.create(10, req));
        assertThrows(SQLException.class, () -> svc.update(10, 1, req));
        assertEquals("Own", dao.findById(connection, 1, false).get("name"));
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM custom_field_values")) {
            rs.next(); assertEquals(0, rs.getInt(1));
        }
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM customers")) {
            rs.next(); assertEquals(3, rs.getInt(1));
        }
    }

    @Test void invalidPaginationAndFilterAreRejected() {
        var svc = service();
        assertThrows(IllegalArgumentException.class, () -> svc.search(10, null, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> svc.search(10, null, null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> svc.search(10, null, null, 1, 101));
        assertThrows(IllegalArgumentException.class, () -> svc.search(10, null, "DA_GOP", 1, 20));
    }

    @Test void scopedListCountFilterAndPaginationUseTheSameOwnerPredicate() throws Exception {
        var svc = service();
        var self = svc.search(10, null, null, 1, 20);
        assertEquals(1L, self.get("totalItems"));
        assertEquals(1, ((List<?>) self.get("items")).size());
        scope = ScopeType.TEAM; owners = Set.of(10L, 20L);
        var team = svc.search(10, null, null, 2, 1);
        assertEquals(2L, team.get("totalItems")); assertEquals(2, team.get("totalPages"));
        assertEquals(1L, ((Map<?, ?>) ((List<?>) team.get("items")).getFirst()).get("id"));
        assertEquals(1L, svc.search(10, "Team", "TIEM_NANG", 1, 20).get("totalItems"));
        assertEquals(0L, svc.search(10, "Outside", null, 1, 20).get("totalItems"));
        scope = ScopeType.ALL; owners = Set.of();
        assertEquals(3L, svc.search(10, null, null, 1, 20).get("totalItems"));
        svc.delete(10, 3);
        assertEquals(2L, svc.search(10, null, null, 1, 20).get("totalItems"));
    }

    @Test void missingActionPermissionDeniesEachCrudOperation() {
        var svc = service();
        deniedAction = "read";
        assertThrows(SecurityException.class, () -> svc.search(10, null, null, 1, 20));
        assertThrows(SecurityException.class, () -> svc.getById(10, 1));
        deniedAction = "create";
        assertThrows(SecurityException.class, () -> svc.create(10, request(null)));
        deniedAction = "update";
        assertThrows(SecurityException.class, () -> svc.update(10, 1, request(null)));
        deniedAction = "delete";
        assertThrows(SecurityException.class, () -> svc.delete(10, 1));
    }
}
