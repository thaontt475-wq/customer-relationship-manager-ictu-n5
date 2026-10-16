package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadDuplicateMergeDAO;
import com.crm.dto.leads.LeadAttachCustomerRequest;
import com.crm.dto.leads.LeadDuplicatePairDTO;
import com.crm.dto.leads.LeadMergeRequest;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadDuplicateMergeJdbcTest {

    private Connection connection;
    private String failureStage = "";
    private ScopeType scopeType = ScopeType.ALL;
    private Set<Long> allowedOwners = Set.of(1L, 2L);

    private final LeadDuplicateMergeDAO dao = new LeadDuplicateMergeDAO() {
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
        public void updateMasterLead(Connection c, long masterId, Map<String, Object> fields, long userId) throws SQLException {
            super.updateMasterLead(c, masterId, fields, userId);
            if ("updateMaster".equals(failureStage)) {
                throw new SQLException("Injected database failure at updateMaster");
            }
        }

        @Override
        public void archiveDuplicateLead(Connection c, long duplicateId, long masterId, long userId) throws SQLException {
            super.archiveDuplicateLead(c, duplicateId, masterId, userId);
            if ("archiveDuplicate".equals(failureStage)) {
                throw new SQLException("Injected database failure at archiveDuplicate");
            }
        }

        @Override
        public void insertMergeHistory(Connection c, long masterId, long duplicateId, long userId, String beforeMaster, String beforeDuplicate, String afterMaster, String overrides, boolean retainHistory) throws SQLException {
            super.insertMergeHistory(c, masterId, duplicateId, userId, beforeMaster, beforeDuplicate, afterMaster, overrides, retainHistory);
            if ("insertHistory".equals(failureStage)) {
                throw new SQLException("Injected database failure at insertHistory");
            }
        }
    };

    private LeadDuplicateMergeService service() {
        return new LeadDuplicateMergeService(dao, (user, module, action) -> {
            if (user == 99L) {
                throw new SecurityException("Người dùng không có quyền truy cập module " + module);
            }
            return new DataScopeContext(user, module, scopeType, null, allowedOwners);
        });
    }

    @BeforeEach
    void setup() throws Exception {
        connection = DatabaseConfig.getConnection();
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
                CREATE TEMPORARY TABLE IF NOT EXISTS customers (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(255),
                    email VARCHAR(255),
                    phone VARCHAR(50),
                    address VARCHAR(500),
                    status VARCHAR(50),
                    owner_user_id BIGINT,
                    is_deleted TINYINT DEFAULT 0,
                    merged_into_id BIGINT NULL,
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
                    title VARCHAR(150) NULL,
                    source_id BIGINT NOT NULL,
                    notes VARCHAR(2000) NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
                    owner_user_id BIGINT NOT NULL,
                    created_by BIGINT NOT NULL,
                    customer_id BIGINT NULL,
                    merged_into_id BIGINT NULL,
                    merged_at DATETIME NULL,
                    merged_by BIGINT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS lead_merge_history (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    master_lead_id BIGINT NOT NULL,
                    duplicate_lead_id BIGINT NOT NULL,
                    performed_by BIGINT NOT NULL,
                    performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    before_master JSON NOT NULL,
                    before_duplicate JSON NOT NULL,
                    after_master JSON NOT NULL,
                    field_overrides JSON NOT NULL,
                    retain_history BOOLEAN NOT NULL DEFAULT TRUE
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS lead_customer_links (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    lead_id BIGINT NOT NULL,
                    customer_id BIGINT NOT NULL,
                    linked_by BIGINT NOT NULL,
                    linked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    notes TEXT NULL
                ) ENGINE=InnoDB
            """);

            s.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS audit_logs (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT,
                    entity_type VARCHAR(100),
                    entity_id VARCHAR(100),
                    action VARCHAR(100),
                    description TEXT,
                    before_value TEXT,
                    after_value TEXT
                ) ENGINE=InnoDB
            """);

            // Clear data
            s.execute("DELETE FROM lead_merge_history");
            s.execute("DELETE FROM lead_customer_links");
            s.execute("DELETE FROM leads");
            s.execute("DELETE FROM customers");
            s.execute("DELETE FROM master_data");
            s.execute("DELETE FROM audit_logs");

            // Seed master data
            s.execute("INSERT INTO master_data VALUES (1, 'lead-source', 'WEBSITE', 'Website Form', 1)");
            s.execute("INSERT INTO master_data VALUES (2, 'lead-source', 'EVENT', 'Hội thảo / Sự kiện', 1)");
            s.execute("INSERT INTO master_data VALUES (3, 'lead-source', 'COLD_CALL', 'Telesales', 1)");

            // Seed Customers
            s.execute("INSERT INTO customers(id, name, email, phone, address, status, owner_user_id) VALUES (100, 'Tập đoàn Dược phẩm Hoa Linh', 'contact@hoalinh.vn', '0912345678', 'Hà Nội', 'ACTIVE', 1)");

            // Seed Leads
            // Lead 1 & Lead 2: Duplicate Phone
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (1, 'Nguyen Minh Quang', 'DigiTech Corp', 'quang1@digitech.vn', '0987123456', 'CEO', 1, 'Nhu cầu CRM quý 4', 'NEW', 1, 1)");
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (2, 'N. M. Quang', 'DigiTech VN', 'quang2@digitech.vn', '+84 987 123 456', 'Giám đốc', 2, 'Gặp tại TechExpo 2026', 'NEW', 1, 1)");

            // Lead 3 & Lead 4: Duplicate Email
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (3, 'Tran Thu Ha', 'Hoa Linh Retail', 'ha.tran@hoalinh.vn', '0901111222', 'Manager', 1, 'Cần tư vấn', 'NEW', 1, 1)");
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (4, 'Thu Ha Tran', 'Chi nhánh miền Bắc', 'HA.TRAN@HOALINH.VN', '0903333444', 'Trưởng phòng', 2, 'Gặp trao đổi', 'NEW', 1, 1)");

            // Lead 5 & Lead 6: Duplicate Company
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (5, 'Le Hoang Cuong', 'Công ty Cổ phần VinaSmart Global', 'cuong@vinasmart.vn', '0915000001', 'CTO', 1, 'Xin tài liệu', 'NEW', 1, 1)");
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (6, 'Vu Hai Dang', 'Tập đoàn VinaSmart', 'dang@vinasmart.vn', '0915000002', 'IT Lead', 2, 'Xin demo', 'NEW', 1, 1)");

            // Lead 7: Unique, no duplicate
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (7, 'Dinh Khac Toan', 'Cong ty Doc Lap', 'toan@doclap.com', '0999888777', 'Developer', 1, 'Doc lap hoan toan', 'NEW', 2, 2)");

            // Lead 8: Matches Customer 100
            s.execute("INSERT INTO leads(id, full_name, company_name, email, phone, title, source_id, notes, status, owner_user_id, created_by) VALUES (8, 'Tran Thu Ha', 'Dược phẩm Hoa Linh', 'ha.tran@hoalinh.vn', '0912345678', 'GĐ Bán lẻ', 1, 'Khách cũ quay lại', 'NEW', 1, 1)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private long count(String table) throws SQLException {
        try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next();
            return r.getLong(1);
        }
    }

    @Test
    void testDuplicateEmailDetected() throws Exception {
        List<LeadDuplicatePairDTO> duplicates = service().getDuplicates(1L);
        assertNotNull(duplicates);
        boolean foundEmailDup = duplicates.stream().anyMatch(d ->
                "EMAIL".equals(d.getCriterion()) &&
                ((Number) d.getRecordA().get("id")).longValue() == 3L &&
                ((Number) d.getRecordB().get("id")).longValue() == 4L
        );
        assertTrue(foundEmailDup, "Cặp trùng email giữa Lead 3 và Lead 4 phải được phát hiện");
    }

    @Test
    void testDuplicatePhoneDetected() throws Exception {
        List<LeadDuplicatePairDTO> duplicates = service().getDuplicates(1L);
        assertNotNull(duplicates);
        boolean foundPhoneDup = duplicates.stream().anyMatch(d ->
                "PHONE".equals(d.getCriterion()) &&
                ((Number) d.getRecordA().get("id")).longValue() == 1L &&
                ((Number) d.getRecordB().get("id")).longValue() == 2L
        );
        assertTrue(foundPhoneDup, "Cặp trùng số điện thoại giữa Lead 1 và Lead 2 phải được phát hiện");
    }

    @Test
    void testDuplicateCompanyDetected() throws Exception {
        List<LeadDuplicatePairDTO> duplicates = service().getDuplicates(1L);
        assertNotNull(duplicates);
        boolean foundCompanyDup = duplicates.stream().anyMatch(d ->
                "COMPANY".equals(d.getCriterion()) &&
                ((Number) d.getRecordA().get("id")).longValue() == 5L &&
                ((Number) d.getRecordB().get("id")).longValue() == 6L
        );
        assertTrue(foundCompanyDup, "Cặp trùng tên công ty giữa Lead 5 và Lead 6 phải được phát hiện");
    }

    @Test
    void testNotDuplicateWhenDistinct() throws Exception {
        List<LeadDuplicatePairDTO> duplicates = service().getDuplicates(1L);
        assertNotNull(duplicates);
        boolean lead7HasDuplicate = duplicates.stream().anyMatch(d ->
                !d.isCustomerMatch() && (
                        Objects.equals(d.getRecordA().get("id"), 7L) ||
                        Objects.equals(d.getRecordB().get("id"), 7L)
                )
        );
        assertFalse(lead7HasDuplicate, "Lead 7 độc lập không được xuất hiện trong bất kỳ cặp trùng Lead nào");
    }

    @Test
    void testSuggestMatchingCustomer() throws Exception {
        List<LeadDuplicatePairDTO> duplicates = service().getDuplicates(1L);
        assertNotNull(duplicates);
        Optional<LeadDuplicatePairDTO> custMatch = duplicates.stream().filter(LeadDuplicatePairDTO::isCustomerMatch).findFirst();
        assertTrue(custMatch.isPresent(), "Phải gợi ý khách hàng trùng cho Lead 8");
        LeadDuplicatePairDTO pair = custMatch.get();
        assertEquals("CUSTOMER", pair.getCriterion());
        assertEquals(8L, ((Number) pair.getRecordA().get("id")).longValue());
        assertEquals("CUST-100", pair.getRecordB().get("id"));
        assertEquals(99, pair.getConfidence());
    }

    @Test
    void testMergeSuccessUpdatesMasterAndArchivesSecondary() throws Exception {
        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 1L;
        req.duplicateId = 2L;
        req.retainHistory = true;
        req.mergedData = new LinkedHashMap<>();
        req.mergedData.put("fullName", "Nguyen Minh Quang (Đã Gộp)");
        req.mergedData.put("email", "quang.merged@digitech.vn");
        req.mergedData.put("phone", "0987123456");
        req.mergedData.put("companyName", "DigiTech Global Corporation");
        req.mergedData.put("title", "Tổng giám đốc");
        req.mergedData.put("source", "EVENT");
        req.mergedData.put("notes", "Đã gộp đầy đủ lịch sử 2 bên");

        Map<String, Object> res = service().merge(1L, req);
        assertTrue((Boolean) res.get("success"));
        assertEquals(1L, res.get("masterId"));
        assertEquals(2L, res.get("duplicateId"));

        // Check database
        try (Statement s = connection.createStatement()) {
            // Master updated
            try (ResultSet r = s.executeQuery("SELECT * FROM leads WHERE id = 1")) {
                assertTrue(r.next());
                assertEquals("Nguyen Minh Quang (Đã Gộp)", r.getString("full_name"));
                assertEquals("quang.merged@digitech.vn", r.getString("email"));
                assertEquals("NEW", r.getString("status"));
                assertNull(r.getObject("merged_into_id"));
                assertEquals(2L, r.getLong("source_id")); // EVENT was source 2
            }

            // Duplicate archived (soft delete / status='MERGED') - NOT physically deleted
            try (ResultSet r = s.executeQuery("SELECT * FROM leads WHERE id = 2")) {
                assertTrue(r.next(), "Lead phụ không được xóa vật lý");
                assertEquals("MERGED", r.getString("status"));
                assertEquals(1L, r.getLong("merged_into_id"));
                assertEquals(1L, r.getLong("merged_by"));
                assertNotNull(r.getTimestamp("merged_at"));
            }
        }
    }

    @Test
    void testFullHistoryRetainedAfterMerge() throws Exception {
        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 3L;
        req.duplicateId = 4L;
        req.retainHistory = true;
        req.mergedData = new LinkedHashMap<>();
        req.mergedData.put("fullName", "Tran Thu Ha");
        req.mergedData.put("email", "ha.tran@hoalinh.vn");
        req.mergedData.put("phone", "0901111222");
        req.mergedData.put("companyName", "Hoa Linh Group");
        req.mergedData.put("title", "GĐ Kinh doanh");
        req.mergedData.put("source", "WEBSITE");
        req.mergedData.put("notes", "[Gốc]: Cần tư vấn\n--- [Lịch sử bổ sung từ Lead #4]: Gặp trao đổi");

        service().merge(1L, req);

        // Check history table
        assertEquals(1, count("lead_merge_history"));
        try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM lead_merge_history WHERE master_lead_id = 3")) {
            assertTrue(r.next());
            assertEquals(4L, r.getLong("duplicate_lead_id"));
            assertEquals(1L, r.getLong("performed_by"));
            assertNotNull(r.getString("before_master"));
            assertNotNull(r.getString("before_duplicate"));
            assertNotNull(r.getString("after_master"));
            assertTrue(r.getString("before_master").contains("Tran Thu Ha"));
            assertTrue(r.getString("before_duplicate").contains("Thu Ha Tran"));
            assertTrue(r.getBoolean("retain_history"));
        }

        // Check audit log
        assertTrue(count("audit_logs") >= 1);
    }

    @Test
    void testMergeSameIdThrowsIllegalArgument() {
        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 1L;
        req.duplicateId = 1L;
        req.retainHistory = true;
        req.mergedData = Map.of("fullName", "Nguyen Minh Quang", "phone", "0987123456");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().merge(1L, req));
        assertTrue(ex.getMessage().contains("Không thể gộp Lead với chính nó"));
    }

    @Test
    void testNonExistentLeadThrowsNoSuchElement() {
        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 1L;
        req.duplicateId = 9999L; // Does not exist
        req.retainHistory = true;
        req.mergedData = Map.of("fullName", "Nguyen Minh Quang", "phone", "0987123456");

        assertThrows(NoSuchElementException.class, () -> service().merge(1L, req));
    }

    @Test
    void testUnauthorizedUserThrowsSecurityException() {
        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 1L;
        req.duplicateId = 2L;
        req.retainHistory = true;
        req.mergedData = Map.of("fullName", "Nguyen Minh Quang", "phone", "0987123456");

        assertThrows(SecurityException.class, () -> service().merge(99L, req));
    }

    @Test
    void testUserCannotMergeOutOfScopeLeads() {
        scopeType = ScopeType.SELF;
        allowedOwners = Set.of(1L); // User only allowed to access owner_user_id = 1

        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 1L; // Owner is 1
        req.duplicateId = 7L; // Owner is 2 (outside scope)
        req.retainHistory = true;
        req.mergedData = Map.of("fullName", "Nguyen Minh Quang", "phone", "0987123456");

        assertThrows(SecurityException.class, () -> service().merge(1L, req));
    }

    @Test
    void testRemergingAlreadyMergedLeadThrowsConflict() throws Exception {
        // First merge
        LeadMergeRequest req1 = new LeadMergeRequest();
        req1.masterId = 1L;
        req1.duplicateId = 2L;
        req1.retainHistory = true;
        req1.mergedData = Map.of("fullName", "Master Lead", "phone", "0987123456");
        service().merge(1L, req1);

        // Attempting to merge again using Lead 2 (already merged) as duplicate
        LeadMergeRequest req2 = new LeadMergeRequest();
        req2.masterId = 3L;
        req2.duplicateId = 2L;
        req2.retainHistory = true;
        req2.mergedData = Map.of("fullName", "Another Master", "phone", "0987123456");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service().merge(1L, req2));
        assertTrue(ex.getMessage().contains("đã được gộp"));

        // Attempting to merge using Lead 2 as master
        LeadMergeRequest req3 = new LeadMergeRequest();
        req3.masterId = 2L;
        req3.duplicateId = 3L;
        req3.retainHistory = true;
        req3.mergedData = Map.of("fullName", "Merged Master", "phone", "0987123456");

        assertThrows(IllegalStateException.class, () -> service().merge(1L, req3));
    }

    @Test
    void testRollbackWhenDatabaseErrorOccurs() throws Exception {
        failureStage = "insertHistory"; // Failure injected after master update and secondary archive

        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 5L;
        req.duplicateId = 6L;
        req.retainHistory = true;
        req.mergedData = Map.of("fullName", "Le Hoang Cuong (Failed Merge)", "phone", "0915000001");

        assertThrows(SQLException.class, () -> service().merge(1L, req));

        // Verify entire transaction was rolled back!
        try (Statement s = connection.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT full_name, status FROM leads WHERE id = 5")) {
                assertTrue(r.next());
                assertEquals("Le Hoang Cuong", r.getString("full_name"), "Master Lead không được thay đổi khi rollback");
                assertEquals("NEW", r.getString("status"));
            }
            try (ResultSet r = s.executeQuery("SELECT status, merged_into_id FROM leads WHERE id = 6")) {
                assertTrue(r.next());
                assertEquals("NEW", r.getString("status"), "Duplicate Lead không được chuyển sang MERGED khi rollback");
                assertNull(r.getObject("merged_into_id"));
            }
        }
        assertEquals(0, count("lead_merge_history"), "Không được lưu lịch sử gộp khi giao tác bị hủy");
    }

    @Test
    void testConcurrentMergeDeadlockPreventionAndLocking() throws Exception {
        // Test that locking pairs in ascending order prevents deadlock
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Map<String, Object>> future1 = pool.submit(() -> {
            startLatch.await();
            synchronized (connection) {
                LeadMergeRequest req = new LeadMergeRequest();
                req.masterId = 1L;
                req.duplicateId = 2L;
                req.retainHistory = true;
                req.mergedData = Map.of("fullName", "Concurrent A", "phone", "0987123456");
                return service().merge(1L, req);
            }
        });

        Future<Map<String, Object>> future2 = pool.submit(() -> {
            startLatch.await();
            synchronized (connection) {
                LeadMergeRequest req = new LeadMergeRequest();
                req.masterId = 2L;
                req.duplicateId = 1L; // Inverse direction
                req.retainHistory = true;
                req.mergedData = Map.of("fullName", "Concurrent B", "phone", "0987123456");
                return service().merge(1L, req);
            }
        });

        startLatch.countDown();

        int successes = 0;
        int conflicts = 0;

        try {
            future1.get(5, TimeUnit.SECONDS);
            successes++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException || e.getCause() instanceof SQLException) {
                conflicts++;
            }
        }

        try {
            future2.get(5, TimeUnit.SECONDS);
            successes++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException || e.getCause() instanceof SQLException) {
                conflicts++;
            }
        }

        pool.shutdown();
        assertEquals(1, successes, "Chính xác 1 trong 2 yêu cầu gộp đồng thời được chấp nhận");
        assertEquals(1, conflicts, "Yêu cầu gộp thứ hai bị từ chối an toàn vì bản ghi đã bị gộp");
    }

    @Test
    void testSqlInjectionAndInvalidInput() throws Exception {
        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 1L;
        req.duplicateId = 2L;
        req.retainHistory = true;
        req.mergedData = new LinkedHashMap<>();
        req.mergedData.put("fullName", "Robert'; DROP TABLE leads; --");
        req.mergedData.put("email", "hacker@test.com' OR '1'='1");
        req.mergedData.put("phone", "0987123456");
        req.mergedData.put("companyName", "Evil Corp'--");
        req.mergedData.put("notes", "<script>alert('xss')</script>");

        service().merge(1L, req);

        // Verify table still intact and data stored verbatim safely
        try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT full_name, email FROM leads WHERE id = 1")) {
            assertTrue(r.next());
            assertEquals("Robert'; DROP TABLE leads; --", r.getString("full_name"));
            assertEquals("hacker@test.com' OR '1'='1", r.getString("email"));
        }
        assertTrue(count("leads") > 0);

        // Test invalid inputs
        LeadMergeRequest badReq1 = new LeadMergeRequest(1L, 2L, Map.of("fullName", "  ", "phone", "0987123456"), true);
        assertThrows(IllegalArgumentException.class, () -> service().merge(1L, badReq1));

        LeadMergeRequest badReq2 = new LeadMergeRequest(1L, 2L, Map.of("fullName", "Name", "phone", " ", "email", " "), true);
        assertThrows(IllegalArgumentException.class, () -> service().merge(1L, badReq2));

        LeadMergeRequest badReq3 = new LeadMergeRequest(-1L, 2L, Map.of("fullName", "Name", "phone", "0987123456"), true);
        assertThrows(IllegalArgumentException.class, () -> service().merge(1L, badReq3));
    }

    @Test
    void testRejectRetainHistoryFalse() {
        LeadMergeRequest req = new LeadMergeRequest();
        req.masterId = 1L;
        req.duplicateId = 2L;
        req.retainHistory = false; // Violates history retention rule
        req.mergedData = Map.of("fullName", "Nguyen Minh Quang", "phone", "0987123456");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().merge(1L, req));
        assertTrue(ex.getMessage().contains("Bảo toàn lịch sử"));
    }

    @Test
    void testAttachLeadToCustomerSuccess() throws Exception {
        LeadAttachCustomerRequest req = new LeadAttachCustomerRequest();
        req.leadId = 8L;
        req.customerId = 100L;
        req.notes = "Gắn lead vào khách hàng đã có";

        Map<String, Object> res = service().attachToCustomer(1L, req);
        assertTrue((Boolean) res.get("success"));
        assertEquals(8L, res.get("leadId"));
        assertEquals(100L, res.get("customerId"));

        try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT status, customer_id FROM leads WHERE id = 8")) {
            assertTrue(r.next());
            assertEquals("CONVERTED", r.getString("status"));
            assertEquals(100L, r.getLong("customer_id"));
        }

        assertEquals(1, count("lead_customer_links"));
    }
}
