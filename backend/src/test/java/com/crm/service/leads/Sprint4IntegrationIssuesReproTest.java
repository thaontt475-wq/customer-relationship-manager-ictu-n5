package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadConversionDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.LeadConversionRequest;
import com.crm.dto.leads.LeadConversionResultDTO;
import com.crm.dto.leads.LeadConvertDuplicateCheckDTO;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class Sprint4IntegrationIssuesReproTest {

    private final List<Long> createdLeadIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    private LeadDAO leadDAO;

    @BeforeEach
    void setUp() {
        leadDAO = new LeadDAO();
    }

    @AfterEach
    void tearDown() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (!createdLeadIds.isEmpty()) {
                String leadIn = String.join(",", Collections.nCopies(createdLeadIds.size(), "?"));
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lead_scores WHERE lead_id IN (" + leadIn + ")")) {
                    for (int i = 0; i < createdLeadIds.size(); i++) ps.setLong(i + 1, createdLeadIds.get(i));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM leads WHERE id IN (" + leadIn + ")")) {
                    for (int i = 0; i < createdLeadIds.size(); i++) ps.setLong(i + 1, createdLeadIds.get(i));
                    ps.executeUpdate();
                }
            }
            if (!createdUserIds.isEmpty()) {
                String userIn = String.join(",", Collections.nCopies(createdUserIds.size(), "?"));
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id IN (" + userIn + ")")) {
                    for (int i = 0; i < createdUserIds.size(); i++) ps.setLong(i + 1, createdUserIds.get(i));
                    ps.executeUpdate();
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("S4-08 Repro: Assigned Sales rep can check duplicates and convert, but unrelated Sales rep is denied (403)")
    void testS408_assignedSalesRepCanConvertAndCheckDuplicates_otherSalesRepDenied() throws Exception {
        Map<Long, Map<String, Object>> mockLeads = new HashMap<>();
        Map<Long, Map<String, Object>> mockCustomers = new HashMap<>();

        Connection mockConnection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> null
        );

        LeadConversionDAO dao = new LeadConversionDAO() {
            @Override
            public Connection open() {
                return mockConnection;
            }

            @Override
            public Map<String, Object> lockLeadForConversion(Connection c, long leadId) {
                return mockLeads.get(leadId);
            }

            @Override
            public Map<String, Object> findLeadById(Connection c, long leadId) {
                return mockLeads.get(leadId);
            }

            @Override
            public Map<String, Object> findCustomerByTaxCode(Connection c, String taxCode) {
                return null;
            }

            @Override
            public Map<String, Object> findCustomerByName(Connection c, String companyName) {
                return null;
            }

            @Override
            public Map<String, Object> findCustomerById(Connection c, long customerId) {
                return mockCustomers.get(customerId);
            }

            @Override
            public long createCustomer(Connection c, LeadConversionRequest.CustomerData cust, long ownerUserId) {
                return 1001L;
            }

            @Override
            public long createContact(Connection c, long customerId, LeadConversionRequest.ContactData contact) {
                return 2001L;
            }

            @Override
            public long createOpportunity(Connection c, long customerId, String contactName, LeadConversionRequest.DealData deal, long ownerUserId) {
                return 3001L;
            }

            @Override
            public void updateLeadAsConverted(Connection c, long leadId, long userId, long customerId, long contactId, Long opportunityId) {
                Map<String, Object> lead = mockLeads.get(leadId);
                if (lead != null) {
                    lead.put("isLocked", true);
                    lead.put("status", "CONVERTED");
                }
            }

            @Override
            public void insertConversionHistory(Connection c, long leadId, long customerId, long contactId, Long opportunityId, long userId, String customerMode, String payloadJson, String snapshotJson) {
            }

            @Override
            public int transferLeadActivitiesToCustomer(Connection c, long leadId, long customerId, Long opportunityId, long currentUserId, String leadNotes) {
                return 0;
            }

            @Override
            public void insertAuditLog(Connection c, long userId, String action, String entityType, long entityId, String description) throws SQLException {
            }
        };

        // Lead has ownerUserId=4270 (Marketing) and assignedToUserId=4272 (Sales Rep A)
        Map<String, Object> lead = new HashMap<>();
        lead.put("id", 782L);
        lead.put("fullName", "QA Lead 782");
        lead.put("companyName", "Tech Corp 782");
        lead.put("taxCode", "0109988776");
        lead.put("status", "CONTACTING");
        lead.put("ownerUserId", 4270L); // Marketing
        lead.put("assignedToUserId", 4272L); // Assigned Sales Rep A
        lead.put("assignedTo", 4272L);
        lead.put("isLocked", false);
        mockLeads.put(782L, lead);

        LeadConversionService service = new LeadConversionService(dao, (user, module, action) -> {
            // ScopeType.SELF: user can only access themselves
            return new DataScopeContext(user, module, ScopeType.SELF, null, Set.of(user));
        });

        // 1. Sales Rep A (4272) is the assigned rep -> MUST SUCCEED (not 403)
        LeadConvertDuplicateCheckDTO checkResult = assertDoesNotThrow(
                () -> service.checkConvertDuplicates(4272L, 782L),
                "Assigned sales rep (4272) must NOT get SecurityException on checkConvertDuplicates"
        );
        assertNotNull(checkResult);

        LeadConversionRequest convReq = new LeadConversionRequest();
        convReq.customerMode = "CREATE_NEW";
        convReq.customer = new LeadConversionRequest.CustomerData();
        convReq.customer.name = "Tech Corp 782";
        convReq.contact = new LeadConversionRequest.ContactData();
        convReq.contact.fullName = "Nguyen Rep";
        convReq.contact.phone = "0901234567";
        convReq.contact.email = "rep@techcorp.example";

        LeadConversionResultDTO convResult = assertDoesNotThrow(
                () -> service.convert(4272L, 782L, convReq),
                "Assigned sales rep (4272) must NOT get SecurityException on convert"
        );
        assertNotNull(convResult);
        assertTrue(convResult.success);

        // 2. Another Sales Rep B (4273) who is NOT assigned -> MUST FAIL with SecurityException (403)
        lead.put("isLocked", false);
        lead.put("status", "CONTACTING");
        assertThrows(SecurityException.class,
                () -> service.checkConvertDuplicates(4273L, 782L),
                "Unassigned sales rep (4273) must get SecurityException 403 on checkConvertDuplicates");
        assertThrows(SecurityException.class,
                () -> service.convert(4273L, 782L, convReq),
                "Unassigned sales rep (4273) must get SecurityException 403 on convert");
    }

    @Test
    @DisplayName("S4-09 Repro: Joining owner and assignee must NOT produce duplicate lead rows")
    void testS409_joinOwnerAndAssigneeDoesNotDuplicateRecords() throws Exception {
        long ownerId = createTestUser("QA Owner " + System.currentTimeMillis());
        long assigneeId = createTestUser("QA Assignee " + System.currentTimeMillis());
        createdUserIds.add(ownerId);
        createdUserIds.add(assigneeId);

        String marker = "S409_DUP_" + System.currentTimeMillis();
        Lead lead = new Lead();
        lead.setFullName(marker + " FullName");
        lead.setCompanyName(marker + " Company");
        lead.setEmail(marker.toLowerCase() + "@example.invalid");
        lead.setPhone("0987654321");
        lead.setStatus("CONTACTING");
        lead.setOwnerUserId(ownerId);
        lead.setAssignedTo(assigneeId);
        lead.setAssignedToUserId(assigneeId);

        long leadId = leadDAO.insert(lead);
        assertTrue(leadId > 0);
        createdLeadIds.add(leadId);

        // Ensure both owner_user_id and assigned_to_user_id are distinct and populated
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE leads SET owner_user_id = ?, assigned_to_user_id = ?, assigned_to = ? WHERE id = ?")) {
            ps.setLong(1, ownerId);
            ps.setLong(2, assigneeId);
            ps.setString(3, String.valueOf(assigneeId));
            ps.setLong(4, leadId);
            ps.executeUpdate();
        }

        DataScopeContext scope = new DataScopeContext(assigneeId, "lead", ScopeType.SELF, null, Set.of(assigneeId));
        LeadFilterRequest filter = new LeadFilterRequest();
        filter.setKeyword(marker);
        filter.setSize(100);

        List<Lead> items = leadDAO.findWithFilters(filter, scope, assigneeId);
        int count = leadDAO.countWithFilters(filter, scope, assigneeId);

        assertEquals(1, items.size(), "Lead matching keyword must appear EXACTLY ONCE, but was: " + items.size());
        assertEquals(1, count, "countWithFilters must return 1, but was: " + count);
    }

    @Test
    @DisplayName("S4-05/S4-09 Repro: lead_scores 60/HOT must reflect in leads filter and sorting")
    void testS405_S409_scoringHotFilterAndSync() throws Exception {
        String marker = "S405_SCORE_" + System.currentTimeMillis();
        Lead lead = new Lead();
        lead.setFullName(marker + " Scored Lead");
        lead.setCompanyName(marker + " Corp");
        lead.setEmail(marker.toLowerCase() + "@example.invalid");
        lead.setPhone("0912345678");
        lead.setStatus("CONTACTING");
        lead.setScore(0);
        lead.setTier("WARM");
        lead.setRating("WARM");

        long leadId = leadDAO.insert(lead);
        assertTrue(leadId > 0);
        createdLeadIds.add(leadId);

        // Explicitly set leads table score=0, tier=WARM and lead_scores table score=60, classification=HOT
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS lead_scores (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        lead_id BIGINT NOT NULL UNIQUE,
                        raw_score INT,
                        score INT,
                        classification VARCHAR(20),
                        breakdown_json TEXT,
                        config_version BIGINT,
                        input_hash VARCHAR(64),
                        calculated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB
                """);
            } catch (SQLException ignored) {}
            try (PreparedStatement ps = conn.prepareStatement("UPDATE leads SET score = 0, tier = 'WARM', rating = 'WARM' WHERE id = ?")) {
                ps.setLong(1, leadId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lead_scores(lead_id, raw_score, score, classification, breakdown_json, config_version, input_hash) " +
                            "VALUES (?, 60, 60, 'HOT', '{}', 1, 'qa_hash') ON DUPLICATE KEY UPDATE score = 60, classification = 'HOT'")) {
                ps.setLong(1, leadId);
                ps.executeUpdate();
            }
        }

        DataScopeContext scope = new DataScopeContext(1L, "lead", ScopeType.ALL, null, Set.of());

        // Filter by keyword and tier=HOT
        LeadFilterRequest hotFilter = new LeadFilterRequest();
        hotFilter.setKeyword(marker);
        hotFilter.setTier("HOT");
        hotFilter.setSize(100);

        List<Lead> hotResults = leadDAO.findWithFilters(hotFilter, scope, 1L);
        assertFalse(hotResults.isEmpty(), "Lead with lead_scores 60/HOT MUST be returned when filtering tier=HOT");
        assertEquals(leadId, hotResults.get(0).getId());
        assertEquals(60, hotResults.get(0).getScore(), "Lead score must be 60");
        assertEquals("HOT", hotResults.get(0).getTier(), "Lead tier must be HOT");

        // Stats should also include this in hotCount
        Map<String, Object> stats = leadDAO.getLeadStats(scope, 1L);
        int hotCount = (int) stats.get("hotCount");
        assertTrue(hotCount >= 1, "hotCount in lead stats must be >= 1");
    }

    private long createTestUser(String fullName) throws Exception {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users(full_name, email, password_hash, status, data_scope) VALUES (?, ?, 'dummy', 'ACTIVE', 'SELF')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName);
            ps.setString(2, "user_" + UUID.randomUUID() + "@example.invalid");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new RuntimeException("Cannot create test user");
    }
}
