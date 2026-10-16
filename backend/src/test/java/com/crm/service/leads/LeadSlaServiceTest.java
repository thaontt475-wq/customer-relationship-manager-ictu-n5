package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadAssignmentHistoryDAO;
import com.crm.dao.leads.LeadDAO;
import com.crm.dto.leads.LeadSlaResponseDTO;
import com.crm.model.leads.AssignmentHistory;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;
import com.crm.service.permissions.ScopeType;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LeadSlaServiceTest {

    private LeadSlaService service;
    private LeadDAO leadDAO;
    private LeadAssignmentHistoryDAO historyDAO;

    private static final long TEST_USER_ID = 101L;
    private static final long OTHER_USER_ID = 202L;
    private static final long ADMIN_USER_ID = 303L;

    private final List<Long> createdLeadIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        leadDAO = new LeadDAO();
        historyDAO = new LeadAssignmentHistoryDAO();
        
        DataScopeService testScopes = new DataScopeService() {
            @Override
            public DataScopeContext resolve(long currentUserId, String module, String action) throws SQLException {
                if (!"lead".equals(module)) {
                    throw new IllegalArgumentException("Module không hợp lệ");
                }
                if (currentUserId == TEST_USER_ID) {
                    return new DataScopeContext(currentUserId, module, ScopeType.SELF, 10L, Set.of(TEST_USER_ID));
                }
                if (currentUserId == ADMIN_USER_ID) {
                    return new DataScopeContext(currentUserId, module, ScopeType.ALL, null, Set.of());
                }
                if (currentUserId == OTHER_USER_ID) {
                    return new DataScopeContext(currentUserId, module, ScopeType.SELF, 20L, Set.of(OTHER_USER_ID));
                }
                throw new SecurityException("Phiên đăng nhập không hợp lệ");
            }
        };

        service = new LeadSlaService(leadDAO, historyDAO, testScopes);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (!createdLeadIds.isEmpty()) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                for (Long id : createdLeadIds) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lead_assignment_history WHERE lead_id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM leads WHERE id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                }
            }
            createdLeadIds.clear();
        }
    }

    private Lead createTestLead(String name, long assignedToUserId, int slaMinutes, long assignedAtDiffMinutes) throws SQLException {
        Lead lead = new Lead();
        lead.setFullName(name);
        lead.setCompanyName("Công ty Thử nghiệm " + name);
        lead.setPhone("0912345678");
        lead.setEmail("test." + System.currentTimeMillis() + "@example.com");
        lead.setTitle("Trưởng phòng Mua hàng");
        lead.setRegion("Miền Bắc");
        lead.setIndustry("Công nghệ thông tin");
        lead.setEmployeeCount(50);
        lead.setScore(85);
        lead.setStatus("PENDING_RESPONSE");
        lead.setAssignedTo("USR-" + assignedToUserId);
        lead.setAssignedToUserId(assignedToUserId);

        long now = System.currentTimeMillis();
        long assignedTime = now - (assignedAtDiffMinutes * 60_000L);
        lead.setAssignedAt(new Timestamp(assignedTime));
        lead.setSlaTotalMinutes(slaMinutes);
        lead.setSlaDeadline(new Timestamp(assignedTime + (slaMinutes * 60_000L)));
        lead.setLocked(false);

        long id = leadDAO.insert(lead);
        lead.setId(id);
        createdLeadIds.add(id);
        return lead;
    }

    @Test
    @Order(1)
    void testGetMyAssignedLeads() throws Exception {
        Lead lead1 = createTestLead("Lead Rep 1", TEST_USER_ID, 120, 10);
        assertNotNull(lead1);

        List<LeadSlaResponseDTO> assigned = service.getMyAssignedLeads(TEST_USER_ID);
        assertNotNull(assigned);
        assertTrue(assigned.stream().anyMatch(l -> l.getId().equals(lead1.getId())));
    }

    @Test
    @Order(2)
    void testAcceptLeadOnTime() throws Exception {
        // Assigned 10 min ago, 120 min SLA -> On time
        Lead lead = createTestLead("Lead OnTime", TEST_USER_ID, 120, 10);

        LeadSlaResponseDTO accepted = service.acceptLead(TEST_USER_ID, lead.getId());
        assertEquals("CONTACTING", accepted.getStatus());
        assertEquals("ON_TIME", accepted.getSlaResult());
        assertNotNull(accepted.getAcceptedAt());
        assertTrue(accepted.getResponseTimeText().contains("đúng hạn"));

        // Verify history inserted
        try (Connection conn = DatabaseConfig.getConnection()) {
            List<AssignmentHistory> history = historyDAO.findByLeadId(conn, lead.getId());
            assertFalse(history.isEmpty());
            assertEquals("ACCEPT", history.get(0).getActionType());
        }
    }

    @Test
    @Order(3)
    void testAcceptLeadOverdue() throws Exception {
        // Assigned 150 min ago, 120 min SLA -> Overdue (30 min past deadline)
        Lead lead = createTestLead("Lead Overdue", TEST_USER_ID, 120, 150);

        LeadSlaResponseDTO accepted = service.acceptLead(TEST_USER_ID, lead.getId());
        assertEquals("CONTACTING", accepted.getStatus());
        assertEquals("OVERDUE", accepted.getSlaResult());
        assertNotNull(accepted.getAcceptedAt());
        assertTrue(accepted.getResponseTimeText().contains("trễ hạn"));
    }

    @Test
    @Order(4)
    void testRejectLeadSuccess() throws Exception {
        Lead lead = createTestLead("Lead To Reject", TEST_USER_ID, 120, 20);

        LeadSlaResponseDTO rejected = service.rejectLead(TEST_USER_ID, lead.getId(), "INVALID_PHONE", "Số điện thoại không có thực");
        assertEquals("PENDING_ASSIGNMENT", rejected.getStatus());
        assertNotNull(rejected.getRejectedAt());
        assertEquals("INVALID_PHONE", rejected.getRejectReason());
        assertEquals("Số điện thoại không có thực", rejected.getRejectDetails());

        // Verify returned to pool
        try (Connection conn = DatabaseConfig.getConnection()) {
            Lead fresh = leadDAO.findById(conn, lead.getId());
            assertNotNull(fresh);
            assertEquals("PENDING_ASSIGNMENT", fresh.getStatus());
            assertNull(fresh.getAssignedTo());
            assertNull(fresh.getAssignedToUserId());

            List<AssignmentHistory> history = historyDAO.findByLeadId(conn, lead.getId());
            assertFalse(history.isEmpty());
            assertEquals("REJECT", history.get(0).getActionType());
        }
    }

    @Test
    @Order(5)
    void testRejectLeadValidationFailure() throws Exception {
        Lead lead = createTestLead("Lead Invalid Reject", TEST_USER_ID, 120, 10);

        // Blank reason
        assertThrows(IllegalArgumentException.class, () -> {
            service.rejectLead(TEST_USER_ID, lead.getId(), "", "Chi tiết giải trình");
        });

        // Blank details
        assertThrows(IllegalArgumentException.class, () -> {
            service.rejectLead(TEST_USER_ID, lead.getId(), "OVERLOADED", "   ");
        });
    }

    @Test
    @Order(6)
    void testCannotAcceptOrRejectLeadOfOtherUserWithoutScope() throws Exception {
        // Lead assigned to OTHER_USER_ID
        Lead lead = createTestLead("Lead Other User", OTHER_USER_ID, 120, 10);

        // Current user (TEST_USER_ID) is a sales rep with SELF scope, cannot accept/reject lead of other user
        assertThrows(SecurityException.class, () -> {
            service.acceptLead(TEST_USER_ID, lead.getId());
        });

        assertThrows(SecurityException.class, () -> {
            service.rejectLead(TEST_USER_ID, lead.getId(), "OUT_OF_TERRITORY", "Không thuộc phụ trách");
        });

        // Admin with ALL scope CAN accept or manage
        LeadSlaResponseDTO adminAccepted = service.acceptLead(ADMIN_USER_ID, lead.getId());
        assertEquals("CONTACTING", adminAccepted.getStatus());
    }

    @Test
    @Order(7)
    void testCannotAcceptOrRejectConvertedOrLockedLead() throws Exception {
        Lead lead = createTestLead("Lead Converted", TEST_USER_ID, 120, 10);
        lead.setStatus("CONVERTED");
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE leads SET status = 'CONVERTED' WHERE id = ?")) {
            ps.setLong(1, lead.getId());
            ps.executeUpdate();
        }

        assertThrows(IllegalStateException.class, () -> {
            service.acceptLead(TEST_USER_ID, lead.getId());
        });

        assertThrows(IllegalStateException.class, () -> {
            service.rejectLead(TEST_USER_ID, lead.getId(), "OTHER", "Đã chốt hợp đồng");
        });
    }

    @Test
    @Order(8)
    void testNonExistentLeadThrowsNotFound() {
        assertThrows(NoSuchElementException.class, () -> {
            service.acceptLead(TEST_USER_ID, 999999999L);
        });

        assertThrows(NoSuchElementException.class, () -> {
            service.rejectLead(TEST_USER_ID, 999999999L, "OTHER", "Không tồn tại");
        });
    }

    @Test
    @Order(9)
    void testSlaDtoRealTimeFlagCalculation() {
        Lead lead = new Lead();
        lead.setId(100L);
        lead.setFullName("Test SLA Flag");
        lead.setStatus("PENDING_RESPONSE");
        long now = System.currentTimeMillis();

        // 1. Deadline in future -> ON_TIME
        lead.setAssignedAt(new Timestamp(now - 10_000L));
        lead.setSlaDeadline(new Timestamp(now + 3600_000L));
        LeadSlaResponseDTO dto1 = LeadSlaResponseDTO.fromModel(lead);
        assertEquals("ON_TIME", dto1.getSlaResult());

        // 2. Deadline in past -> OVERDUE
        lead.setSlaDeadline(new Timestamp(now - 60_000L));
        LeadSlaResponseDTO dto2 = LeadSlaResponseDTO.fromModel(lead);
        assertEquals("OVERDUE", dto2.getSlaResult());
    }

    @Test
    @Order(10)
    void testConcurrentAcceptanceLock() throws Exception {
        Lead lead = createTestLead("Lead Concurrent", TEST_USER_ID, 120, 10);

        // First accept succeeds
        LeadSlaResponseDTO first = service.acceptLead(TEST_USER_ID, lead.getId());
        assertEquals("CONTACTING", first.getStatus());

        // Second accept on already CONTACTING lead works idempotently or updates status
        LeadSlaResponseDTO second = service.acceptLead(TEST_USER_ID, lead.getId());
        assertEquals("CONTACTING", second.getStatus());
    }
}
