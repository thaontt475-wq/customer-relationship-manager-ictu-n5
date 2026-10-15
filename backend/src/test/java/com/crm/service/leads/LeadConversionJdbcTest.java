package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadConversionDAO;
import com.crm.dto.leads.LeadConversionRequest;
import com.crm.dto.leads.LeadConversionResultDTO;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadConversionJdbcTest {

    private Connection connection;
    private String failureStage = "";
    private ScopeType scopeType = ScopeType.ALL;
    private Set<Long> allowedOwners = Set.of(1L, 2L);

    private final LeadConversionDAO dao = new LeadConversionDAO() {
        @Override
        public Connection open() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("close".equals(method.getName())) return null;
                        synchronized (connection) {
                            try {
                                return method.invoke(connection, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        }
                    }
            );
        }

        @Override
        public long createOpportunity(Connection c, long customerId, String contactName, LeadConversionRequest.DealData deal, long ownerUserId) throws SQLException {
            if ("opportunityFailure".equals(failureStage)) {
                throw new SQLException("Injected database failure at createOpportunity");
            }
            return super.createOpportunity(c, customerId, contactName, deal, ownerUserId);
        }

        @Override
        public void updateLeadAsConverted(Connection c, long leadId, long userId, long customerId, long contactId, Long opportunityId) throws SQLException {
            if ("updateLeadFailure".equals(failureStage)) {
                throw new SQLException("Injected database failure at updateLeadAsConverted");
            }
            super.updateLeadAsConverted(c, leadId, userId, customerId, contactId, opportunityId);
        }
    };

    private LeadConversionService service() {
        return new LeadConversionService(dao, (user, module, action) -> {
            if (user == 99L) {
                throw new SecurityException("Người dùng không có quyền truy cập module " + module);
            }
            return new DataScopeContext(user, module, scopeType, 1L, allowedOwners);
        });
    }

    @BeforeEach
    void setup() throws Exception {
        connection = DatabaseConfig.getConnection();
        failureStage = "";
        scopeType = ScopeType.ALL;
        allowedOwners = Set.of(1L, 2L);

        try (Statement s = connection.createStatement()) {
            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS master_data (
                    id BIGINT PRIMARY KEY,
                    type VARCHAR(50),
                    code VARCHAR(100),
                    name VARCHAR(200),
                    active BOOLEAN
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS users (
                    id BIGINT PRIMARY KEY,
                    full_name VARCHAR(150),
                    status VARCHAR(30) DEFAULT 'ACTIVE'
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS customers (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(255) NOT NULL,
                    tax_code VARCHAR(50) NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'TIEM_NANG',
                    email VARCHAR(255) NULL,
                    phone VARCHAR(50) NULL,
                    website VARCHAR(255) NULL,
                    address VARCHAR(500) NULL,
                    industry_id BIGINT NULL,
                    owner_user_id BIGINT NOT NULL,
                    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS contacts (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    customer_id BIGINT NOT NULL,
                    full_name VARCHAR(200) NOT NULL,
                    email VARCHAR(255) NULL,
                    phone VARCHAR(50) NULL,
                    title VARCHAR(150) NULL,
                    buying_role VARCHAR(40) NULL,
                    is_primary TINYINT(1) DEFAULT 0,
                    is_deleted TINYINT(1) DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS opportunities (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(255) NOT NULL,
                    customer_id BIGINT NOT NULL,
                    contact_name VARCHAR(255) NULL,
                    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
                    stage_id BIGINT NULL,
                    probability INT NULL DEFAULT 0,
                    expected_close_date DATE NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                    owner_user_id BIGINT NOT NULL,
                    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS leads (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    full_name VARCHAR(150) NOT NULL,
                    company_name VARCHAR(255) NULL,
                    email VARCHAR(255) NULL,
                    phone VARCHAR(50) NULL,
                    source_id BIGINT NOT NULL DEFAULT 1,
                    notes VARCHAR(2000) NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
                    owner_user_id BIGINT NOT NULL,
                    created_by BIGINT NOT NULL,
                    title VARCHAR(150) NULL,
                    tax_code VARCHAR(50) NULL,
                    address VARCHAR(500) NULL,
                    website VARCHAR(255) NULL,
                    company_phone VARCHAR(50) NULL,
                    industry VARCHAR(100) NULL,
                    score INT NULL DEFAULT 0,
                    tier VARCHAR(20) NULL,
                    estimated_value DECIMAL(18,2) NULL DEFAULT 0.00,
                    buying_role VARCHAR(40) NULL,
                    is_locked TINYINT(1) NOT NULL DEFAULT 0,
                    converted_at DATETIME NULL,
                    converted_by BIGINT NULL,
                    converted_customer_id BIGINT NULL,
                    converted_contact_id BIGINT NULL,
                    converted_opportunity_id BIGINT NULL,
                    merged_into_id BIGINT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS lead_conversion_history (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    lead_id BIGINT NOT NULL,
                    customer_id BIGINT NOT NULL,
                    contact_id BIGINT NOT NULL,
                    opportunity_id BIGINT NULL,
                    converted_by BIGINT NOT NULL,
                    converted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    customer_mode VARCHAR(30) NOT NULL,
                    conversion_payload JSON NULL,
                    lead_snapshot JSON NOT NULL
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS audit_logs (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    action VARCHAR(50) NOT NULL,
                    entity_type VARCHAR(50) NOT NULL,
                    entity_id BIGINT NOT NULL,
                    details TEXT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);

            // Seed master data
            s.execute("INSERT IGNORE INTO master_data VALUES (1, 'industry', 'IT', 'CNTT & Viễn thông', 1)");
            s.execute("INSERT IGNORE INTO users VALUES (1, 'User 1', 'ACTIVE'), (2, 'User 2', 'ACTIVE')");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TEMPORARY TABLE IF EXISTS audit_logs");
                s.execute("DROP TEMPORARY TABLE IF EXISTS lead_conversion_history");
                s.execute("DROP TEMPORARY TABLE IF EXISTS leads");
                s.execute("DROP TEMPORARY TABLE IF EXISTS opportunities");
                s.execute("DROP TEMPORARY TABLE IF EXISTS contacts");
                s.execute("DROP TEMPORARY TABLE IF EXISTS customers");
                s.execute("DROP TEMPORARY TABLE IF EXISTS users");
                s.execute("DROP TEMPORARY TABLE IF EXISTS master_data");
            }
            connection.close();
        }
    }

    private long insertLead(String fullName, String company, String email, String phone, String status, long ownerId) throws SQLException {
        String sql = """
                INSERT INTO leads (full_name, company_name, email, phone, status, owner_user_id, created_by, source_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """;
        try (var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, company);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setString(5, status);
            ps.setLong(6, ownerId);
            ps.setLong(7, ownerId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long insertCustomer(String name, String taxCode, long ownerId) throws SQLException {
        String sql = "INSERT INTO customers (name, tax_code, owner_user_id) VALUES (?, ?, ?)";
        try (var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, taxCode);
            ps.setLong(3, ownerId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private LeadConversionRequest sampleRequest(String customerMode, Long existingCustomerId, boolean createDeal) {
        LeadConversionRequest req = new LeadConversionRequest();
        req.customerMode = customerMode;
        req.existingCustomerId = existingCustomerId;

        req.customer = new LeadConversionRequest.CustomerData();
        req.customer.name = "Công ty TNHH Giải Pháp Trí Tuệ Nhân Tạo";
        req.customer.taxCode = "0109988776";
        req.customer.industry = "CNTT & Viễn thông";
        req.customer.phone = "0243888777";
        req.customer.address = "Tòa nhà Keangnam, Hà Nội";

        req.contact = new LeadConversionRequest.ContactData();
        req.contact.fullName = "Đặng Hoàng Giang";
        req.contact.phone = "0987654321";
        req.contact.email = "giang.dh@ai-solutions.vn";
        req.contact.title = "Giám đốc Công nghệ (CTO)";
        req.contact.buyingRole = "DECISION_MAKER";

        req.deal = new LeadConversionRequest.DealData();
        req.deal.createDeal = createDeal;
        if (createDeal) {
            req.deal.name = "Hợp đồng giải pháp AI Enterprise";
            req.deal.amount = new BigDecimal("450000000");
            req.deal.closeDate = "2026-12-01";
            req.deal.stageId = 2L;
            req.deal.probability = 40;
        }
        return req;
    }

    @Test
    void testSuccessfulConversionCreatesCustomerContactOpportunity() throws Exception {
        long leadId = insertLead("Đặng Hoàng Giang", "AI Solutions", "giang.dh@ai.vn", "0987654321", "QUALIFIED", 1L);
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);

        LeadConversionResultDTO res = service().convert(1L, leadId, req);

        assertTrue(res.success);
        assertNotNull(res.customerId);
        assertNotNull(res.contactId);
        assertNotNull(res.dealId);

        // Verify customer in DB
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT name, tax_code, status FROM customers WHERE id = " + res.customerId)) {
            assertTrue(rs.next());
            assertEquals("Công ty TNHH Giải Pháp Trí Tuệ Nhân Tạo", rs.getString("name"));
            assertEquals("0109988776", rs.getString("tax_code"));
        }

        // Verify contact in DB
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT full_name, is_primary FROM contacts WHERE id = " + res.contactId)) {
            assertTrue(rs.next());
            assertEquals("Đặng Hoàng Giang", rs.getString("full_name"));
            assertEquals(1, rs.getInt("is_primary"));
        }

        // Verify opportunity in DB
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT name, amount, customer_id FROM opportunities WHERE id = " + res.dealId)) {
            assertTrue(rs.next());
            assertEquals("Hợp đồng giải pháp AI Enterprise", rs.getString("name"));
            assertEquals(new BigDecimal("450000000.00"), rs.getBigDecimal("amount"));
            assertEquals(res.customerId, rs.getLong("customer_id"));
        }

        // Verify lead is converted & locked
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT status, is_locked, converted_customer_id, converted_contact_id, converted_opportunity_id FROM leads WHERE id = " + leadId)) {
            assertTrue(rs.next());
            assertEquals("CONVERTED", rs.getString("status"));
            assertEquals(1, rs.getInt("is_locked"));
            assertEquals(res.customerId, rs.getLong("converted_customer_id"));
            assertEquals(res.contactId, rs.getLong("converted_contact_id"));
            assertEquals(res.dealId, rs.getLong("converted_opportunity_id"));
        }

        // Verify conversion history
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT customer_mode, customer_id, contact_id, opportunity_id FROM lead_conversion_history WHERE lead_id = " + leadId)) {
            assertTrue(rs.next());
            assertEquals("CREATE_NEW", rs.getString("customer_mode"));
            assertEquals(res.customerId, rs.getLong("customer_id"));
            assertEquals(res.contactId, rs.getLong("contact_id"));
        }
    }

    @Test
    void testSuccessfulConversionMergeExistingCustomer() throws Exception {
        long custId = insertCustomer("Tập đoàn Mediphar Quốc tế", "0107776665", 1L);
        long leadId = insertLead("Lê Thanh Hải", "Mediphar", "hai.lt@mediphar.vn", "0911223344", "QUALIFIED", 1L);

        LeadConversionRequest req = sampleRequest("MERGE_EXISTING", custId, true);
        LeadConversionResultDTO res = service().convert(1L, leadId, req);

        assertTrue(res.success);
        assertEquals(custId, res.customerId);

        // Verify only 1 customer exists (no duplicate created)
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM customers")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        // Verify contact attached to existing customer
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT customer_id FROM contacts WHERE id = " + res.contactId)) {
            assertTrue(rs.next());
            assertEquals(custId, rs.getLong("customer_id"));
        }
    }

    @Test
    void testConversionRollbackOnDatabaseFailure() throws Exception {
        long leadId = insertLead("Trần Đình Trọng", "Trọng Phát", "trong@phat.vn", "0909090909", "QUALIFIED", 1L);
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);

        failureStage = "opportunityFailure";

        assertThrows(SQLException.class, () -> service().convert(1L, leadId, req));

        // Verify complete rollback: no customer, no contact, no deal
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM customers")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM contacts")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM opportunities")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        // Verify lead is untouched
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT status, is_locked FROM leads WHERE id = " + leadId)) {
            assertTrue(rs.next());
            assertEquals("QUALIFIED", rs.getString("status"));
            assertEquals(0, rs.getInt("is_locked"));
        }
    }

    @Test
    void testLeadDataScopeEnforcement() throws Exception {
        // Lead owned by user 2, but current user 1 has SELF scope and only owns user 1
        long leadId = insertLead("Người của User 2", "Công ty 2", "u2@company.com", "0988001122", "QUALIFIED", 2L);

        scopeType = ScopeType.SELF;
        allowedOwners = Set.of(1L);

        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);

        SecurityException ex = assertThrows(SecurityException.class, () -> service().convert(1L, leadId, req));
        assertTrue(ex.getMessage().contains("không có quyền"));
    }

    @Test
    void testDoubleConversionBlocked() throws Exception {
        long leadId = insertLead("Lê Hoàng", "Hoàng Nam", "nam@hn.vn", "0912121212", "QUALIFIED", 1L);
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, false);

        // First conversion succeeds
        LeadConversionResultDTO first = service().convert(1L, leadId, req);
        assertTrue(first.success);

        // Second conversion fails
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service().convert(1L, leadId, req));
        assertTrue(ex.getMessage().contains("đã được chuyển đổi"));
    }

    @Test
    void testTaxCodeDuplicatePreventedOnCreateNew() throws Exception {
        insertCustomer("Công ty Cũ", "0109988776", 1L);
        long leadId = insertLead("Phạm Minh", "Công ty Mới", "minh@moi.vn", "0901234567", "QUALIFIED", 1L);

        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, false);
        req.customer.taxCode = "0109988776"; // matches existing customer

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service().convert(1L, leadId, req));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    void testConcurrentConversionSafety() throws Exception {
        long leadId = insertLead("Concurrency Lead", "Concurrent Corp", "test@concurrent.com", "0900112233", "QUALIFIED", 1L);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<LeadConversionResultDTO> f1 = executor.submit(() -> {
            start.await();
            synchronized (connection) {
                LeadConversionRequest req = sampleRequest("CREATE_NEW", null, false);
                req.customer.taxCode = "TAX-A";
                return service().convert(1L, leadId, req);
            }
        });

        Future<LeadConversionResultDTO> f2 = executor.submit(() -> {
            start.await();
            synchronized (connection) {
                LeadConversionRequest req = sampleRequest("CREATE_NEW", null, false);
                req.customer.taxCode = "TAX-B";
                return service().convert(1L, leadId, req);
            }
        });

        start.countDown();

        int successes = 0;
        int conflicts = 0;

        try {
            f1.get(10, TimeUnit.SECONDS);
            successes++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException || e.getCause() instanceof SQLException) {
                conflicts++;
            }
        }

        try {
            f2.get(10, TimeUnit.SECONDS);
            successes++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException || e.getCause() instanceof SQLException) {
                conflicts++;
            }
        }

        executor.shutdown();

        assertEquals(1, successes, "Chính xác 1 trong 2 yêu cầu đồng thời được chấp nhận");
        assertEquals(1, conflicts, "Yêu cầu thứ hai bị từ chối an toàn vì bản ghi đã bị chuyển đổi");

        // Exactly 1 customer and 1 contact created
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM customers")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
        try (var s = connection.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM contacts")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}
