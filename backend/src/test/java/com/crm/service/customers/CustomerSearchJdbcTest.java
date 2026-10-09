package com.crm.service.customers;

import com.crm.config.DatabaseConfig;
import com.crm.dao.customers.*;
import com.crm.dto.customers.*;
import com.crm.service.permissions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** All writes target session-local temporary tables; persistent CRM rows are untouched. */
class CustomerSearchJdbcTest {
    private Connection connection;
    private ScopeType scope = ScopeType.SELF;
    private Set<Long> owners = Set.of(10L);
    private boolean denied;
    private final DataScopeService scopes = new DataScopeService() {
        @Override public DataScopeContext resolve(long user, String module, String action) {
            if (denied) throw new SecurityException("Missing customer.read permission");
            return new DataScopeContext(user, module, scope, null, owners);
        }
    };
    private void requireTemporaryTables() throws SQLException {
        for (String table : List.of("customers", "users", "contacts", "master_data", "saved_customer_filters")) {
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + table)) {
                assertTrue(rs.next());
                assertTrue(rs.getString(2).startsWith("CREATE TEMPORARY TABLE"), "Refuse to access persistent table " + table);
            }
        }
    }
    private Connection borrowed() throws SQLException {
        requireTemporaryTables();
        connection.setAutoCommit(true); // Each DAO borrows the equivalent of a fresh connection.
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) return null;
                    try { return method.invoke(connection, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
    }
    private final CustomerDAO dao = new CustomerDAO() {
        @Override public Connection open() throws SQLException { return borrowed(); }
    };
    private final SavedCustomerFilterDAO savedDAO = new SavedCustomerFilterDAO() {
        @Override public Connection open() throws SQLException { return borrowed(); }
    };
    private CustomerService service() { return new CustomerService(dao, scopes); }
    private SavedCustomerFilterService saved() { return new SavedCustomerFilterService(savedDAO, scopes, service()); }
    private CustomerSearchFilter filter(String keyword) { return new CustomerSearchFilter(keyword, null, null, null, null, null); }
    private Map<String, Object> search(CustomerSearchFilter filter) throws Exception { return service().search(10, filter, 1, 20); }
    private List<Long> ids(Map<String, Object> result) {
        return ((List<?>) result.get("items")).stream().map(row -> ((Number) ((Map<?, ?>) row).get("id")).longValue()).toList();
    }

    @BeforeEach void setup() throws Exception {
        connection = DatabaseConfig.getConnection();
        try (Statement s = connection.createStatement()) {
            s.execute("""
                    CREATE TEMPORARY TABLE customers (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(255),tax_code VARCHAR(50),status VARCHAR(50),
                    email VARCHAR(255),phone VARCHAR(50),website VARCHAR(255),address VARCHAR(500),
                    industry_id BIGINT,company_size_id BIGINT,owner_user_id BIGINT,region VARCHAR(20),
                    is_deleted TINYINT DEFAULT 0,created_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB
                    """);
            s.execute("CREATE TEMPORARY TABLE users(id BIGINT PRIMARY KEY,full_name VARCHAR(255)) ENGINE=InnoDB");
            s.execute("CREATE TEMPORARY TABLE contacts(id BIGINT PRIMARY KEY,customer_id BIGINT,phone VARCHAR(50),is_deleted TINYINT DEFAULT 0) ENGINE=InnoDB");
            s.execute("CREATE TEMPORARY TABLE master_data(id BIGINT PRIMARY KEY,type VARCHAR(50),code VARCHAR(100)) ENGINE=InnoDB");
            s.execute("""
                    CREATE TEMPORARY TABLE saved_customer_filters (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,name VARCHAR(100),
                    criteria JSON,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,KEY ix_user(user_id,id)) ENGINE=InnoDB
                    """);
            requireTemporaryTables();
            s.execute("INSERT INTO users VALUES(10,'Sales A'),(20,'Sales B'),(30,'Outside')");
            s.execute("INSERT INTO master_data VALUES(1,'industry','IT'),(2,'industry','RETAIL'),(11,'company-size','SMALL')");
            s.execute("""
                    INSERT INTO customers(id,name,tax_code,status,industry_id,company_size_id,owner_user_id,region,address,is_deleted) VALUES
                    (1,'Alpha COMPANY','0123456789-001','CHINH_THUC',1,11,10,'MB','Ho Chi Minh',0),
                    (2,'Beta Company','1123456789','TIEM_NANG',2,12,10,'MN','Ha Noi',0),
                    (3,'Team Company','2123456789','CHINH_THUC',1,11,20,'MT','Address',0),
                    (4,'Outside Company','3123456789','TIEM_NANG',1,11,30,'MB','Address',0),
                    (5,'Deleted Company',NULL,'CHINH_THUC',1,11,10,'MB','Address',1),
                    (6,'100%_! Holdings',NULL,'TIEM_NANG',1,11,10,NULL,'Ha Noi',0)
                    """);
            s.execute("INSERT INTO contacts VALUES(1,1,'0987654321',0),(2,1,'0987654321',0),(3,2,'0999999999',1),(4,4,'0987654321',0)");
        }
    }

    @AfterEach void close() throws Exception { if (connection != null) connection.close(); }

    @Test void findsNameTaxAndActiveContactPhoneWithoutDuplicatingCustomers() throws Exception {
        assertEquals(List.of(1L), ids(search(filter("aLpHa company"))));
        assertEquals(List.of(1L), ids(search(filter("0123456789001"))));
        assertEquals(List.of(1L), ids(search(filter("0123456789-001"))));
        var phones = search(filter("0987654321"));
        assertEquals(List.of(1L), ids(phones)); assertEquals(1L, phones.get("totalItems"));
        assertEquals(0L, search(filter("0999999999")).get("totalItems"));
    }

    @Test void combinesAllFiltersAndUsesExplicitRegionRatherThanAddress() throws Exception {
        var all = new CustomerSearchFilter("company", "CHINH_THUC", 1L, 11L, "MB", 10L);
        assertEquals(List.of(1L), ids(search(all)));
        assertEquals(List.of(2L), ids(search(new CustomerSearchFilter(null, null, null, null, "MN", null))));
        assertEquals(List.of(1L), ids(search(new CustomerSearchFilter("company", null, null, null, null, null, "IT"))));
        assertEquals(0L, search(new CustomerSearchFilter(null, "TIEM_NANG", 1L, 11L, "MB", 10L)).get("totalItems"));
        assertEquals(0L, search(new CustomerSearchFilter(null, null, 999L, null, null, null)).get("totalItems"));
    }

    @Test void selfTeamAndAllRestrictListAndCountEvenWhenOwnerIsSpecified() throws Exception {
        assertEquals(List.of(2L, 1L), ids(search(filter("company"))));
        assertEquals(0L, search(new CustomerSearchFilter(null, null, null, null, null, 30L)).get("totalItems"));
        scope = ScopeType.TEAM; owners = Set.of(10L, 20L);
        var team = search(filter("company")); assertEquals(List.of(3L, 2L, 1L), ids(team)); assertEquals(3L, team.get("totalItems"));
        scope = ScopeType.ALL; owners = Set.of();
        assertEquals(List.of(4L, 3L, 2L, 1L), ids(search(filter("company"))));
        scope = ScopeType.TEAM;
        assertEquals(0L, search(filter(null)).get("totalItems"));
    }

    @Test void paginationHasStableOrderMatchingCountAndBoundedEmptyPages() throws Exception {
        var first = service().search(10, filter(null), 1, 2);
        var second = service().search(10, filter(null), 2, 2);
        assertEquals(List.of(6L, 2L), ids(first)); assertEquals(List.of(1L), ids(second));
        assertEquals(3L, first.get("totalItems")); assertEquals(2, first.get("totalPages"));
        assertTrue(ids(service().search(10, filter(null), Integer.MAX_VALUE, 100)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service().search(10, filter(null), 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service().search(10, filter(null), 1, 101));
    }

    @Test void injectionAndLikeWildcardsAreLiteralAndDoNotBypassScope() throws Exception {
        for (String query : List.of("' OR 1=1 --", "%; DROP TABLE customers; --", "_")) {
            var result = search(filter(query));
            if (query.equals("_")) assertEquals(List.of(6L), ids(result));
            else assertEquals(0L, result.get("totalItems"));
        }
        assertEquals(List.of(6L), ids(search(filter("100%_!"))));
        assertEquals(3L, search(filter(null)).get("totalItems"));
    }

    @Test void savedFiltersArePrivateAndNeverPersistScope() throws Exception {
        var svc = saved(); var created = svc.create(10, "  Active Customers  ", filter("company"));
        assertEquals("Active Customers", created.name());
        assertEquals(1, svc.list(10, 1, 20).size()); assertTrue(svc.list(20, 1, 20).isEmpty());
        assertThrows(NoSuchElementException.class, () -> svc.get(20, created.id()));
        assertThrows(NoSuchElementException.class, () -> svc.apply(20, created.id(), 1, 20));
        assertThrows(NoSuchElementException.class, () -> svc.delete(20, created.id()));
        assertEquals(List.of(2L, 1L), ids(svc.apply(10, created.id(), 1, 20)));
        scope = ScopeType.ALL; owners = Set.of();
        assertEquals(List.of(4L, 3L, 2L, 1L), ids(svc.apply(10, created.id(), 1, 20)));
        scope = ScopeType.SELF; owners = Set.of(10L);
        assertEquals(List.of(2L, 1L), ids(svc.apply(10, created.id(), 1, 20)));
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT criteria FROM saved_customer_filters")) {
            rs.next(); String json = rs.getString(1);
            assertFalse(json.contains("scope")); assertFalse(json.contains("allowedOwnerIds"));
        }
        svc.delete(10, created.id()); assertTrue(svc.list(10, 1, 20).isEmpty());
    }

    @Test void savedFiltersValidateNamesAndRespectRevokedPermission() throws Exception {
        var svc = saved();
        assertThrows(IllegalArgumentException.class, () -> svc.create(10, " ", filter(null)));
        assertThrows(IllegalArgumentException.class, () -> svc.create(10, "x".repeat(101), filter(null)));
        assertThrows(IllegalArgumentException.class, () -> svc.create(10, "Name", null));
        var created = svc.create(10, "Name", filter(null)); denied = true;
        assertThrows(SecurityException.class, () -> svc.apply(10, created.id(), 1, 20));
        assertThrows(SecurityException.class, () -> service().search(10, filter(null), 1, 20));
    }

    @Test void originalCrudDaoStillCreatesUpdatesAndSoftDeletesCustomers() throws Exception {
        var request = new CustomerWriteRequest(); request.setName("CRUD customer");
        long id = dao.create(request, 10);
        assertEquals("CRUD customer", dao.findById(id).get("name"));
        assertEquals(4L, search(filter(null)).get("totalItems"));
        assertEquals("TIEM_NANG", ((Map<?, ?>) ((List<?>) search(filter("CRUD")).get("items")).getFirst()).get("status"));
        request.setName("Updated CRUD"); dao.update(id, request);
        assertEquals("Updated CRUD", dao.findById(id).get("name"));
        dao.softDelete(id); assertNull(dao.findById(id));
        assertEquals(3L, search(filter(null)).get("totalItems"));
    }

    @Test void regionIsWrittenPreservedClearedAndSearchableWithoutInferringAddress() throws Exception {
        var req = new CustomerWriteRequest(); req.setName("Region Customer"); req.setRegion("Hà Nội");
        long id = dao.create(req, 10);
        assertEquals("Hà Nội", dao.findById(id).get("region"));
        var update = new CustomerWriteRequest(); update.setName("Other fields updated");
        dao.update(id, update); assertEquals("Hà Nội", dao.findById(id).get("region"));
        assertEquals(List.of(id), ids(search(new CustomerSearchFilter(null, null, null, null, "Hà Nội", null))));
        var stored = saved().create(10, "Hanoi", new CustomerSearchFilter(null, null, null, null, "Hà Nội", null));
        assertEquals(List.of(id), ids(saved().apply(10, stored.id(), 1, 20)));
        update.setRegion(null); dao.update(id, update); assertNull(dao.findById(id).get("region"));
        assertEquals(0L, saved().apply(10, stored.id(), 1, 20).get("totalItems"));
        update.setAddress("Hà Nội"); dao.update(id, update); assertNull(dao.findById(id).get("region"));
    }
}
