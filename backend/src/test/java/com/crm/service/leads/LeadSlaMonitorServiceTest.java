package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadDAO;
import com.crm.dao.notifications.NotificationDAO;
import com.crm.model.leads.Lead;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LeadSlaMonitorServiceTest {

    private LeadSlaMonitorService monitorService;
    private NotificationDAO notificationDAO;
    private LeadDAO leadDAO;

    private static final long REP_USER_ID = 3001L;
    private static final long TEAM_LEAD_USER_ID = 3002L;
    private static final long OTHER_LEAD_USER_ID = 3003L;
    private static final long REP_TEAM_ID = 9001L;
    private static final long OTHER_TEAM_ID = 9002L;

    private final List<Long> createdLeadIds = new ArrayList<>();
    private final List<Long> createdUserIds = List.of(REP_USER_ID, TEAM_LEAD_USER_ID, OTHER_LEAD_USER_ID);
    private final List<Long> createdTeamIds = List.of(REP_TEAM_ID, OTHER_TEAM_ID);

    @BeforeEach
    void setUp() throws Exception {
        notificationDAO = new NotificationDAO();
        monitorService = new LeadSlaMonitorService(notificationDAO);
        leadDAO = new LeadDAO();

        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Setup test teams (without leader_id initially to respect FK)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO teams(id, name, active) VALUES(?, 'Test Rep Team', 1), (?, 'Test Other Team', 1)")) {
                ps.setLong(1, REP_TEAM_ID);
                ps.setLong(2, OTHER_TEAM_ID);
                ps.executeUpdate();
            }

            // 2. Setup test users
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO users(id, full_name, email, password_hash, status, team_id) VALUES " +
                            "(?, 'Sales Rep Test', 'rep.test@crm.invalid', 'hash', 'ACTIVE', ?), " +
                            "(?, 'Team Lead Test', 'lead.test@crm.invalid', 'hash', 'ACTIVE', ?), " +
                            "(?, 'Other Lead Test', 'other.test@crm.invalid', 'hash', 'ACTIVE', ?)")) {
                ps.setLong(1, REP_USER_ID);
                ps.setLong(2, REP_TEAM_ID);
                ps.setLong(3, TEAM_LEAD_USER_ID);
                ps.setLong(4, REP_TEAM_ID);
                ps.setLong(5, OTHER_LEAD_USER_ID);
                ps.setLong(6, OTHER_TEAM_ID);
                ps.executeUpdate();
            }

            // 3. Set leader_id on teams now that users exist
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE teams SET leader_id = ? WHERE id = ?")) {
                ps.setLong(1, TEAM_LEAD_USER_ID);
                ps.setLong(2, REP_TEAM_ID);
                ps.executeUpdate();
                ps.setLong(1, OTHER_LEAD_USER_ID);
                ps.setLong(2, OTHER_TEAM_ID);
                ps.executeUpdate();
            }

            // 4. Link Team Lead role
            long teamLeadRoleId = 5L;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM roles WHERE code = 'TEAM_LEAD' LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) teamLeadRoleId = rs.getLong(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO user_roles(user_id, role_id) VALUES (?, ?), (?, ?)")) {
                ps.setLong(1, TEAM_LEAD_USER_ID);
                ps.setLong(2, teamLeadRoleId);
                ps.setLong(3, OTHER_LEAD_USER_ID);
                ps.setLong(4, teamLeadRoleId);
                ps.executeUpdate();
            }
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DatabaseConfig.getConnection()) {
            for (Long leadId : createdLeadIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notifications WHERE lead_id = ?")) {
                    ps.setLong(1, leadId);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lead_assignment_history WHERE lead_id = ?")) {
                    ps.setLong(1, leadId);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM leads WHERE id = ?")) {
                    ps.setLong(1, leadId);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
            }
            createdLeadIds.clear();

            for (Long uid : createdUserIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notifications WHERE user_id = ?")) {
                    ps.setLong(1, uid);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
                    ps.setLong(1, uid);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE teams SET leader_id = NULL WHERE id IN (?, ?)")) {
                ps.setLong(1, REP_TEAM_ID);
                ps.setLong(2, OTHER_TEAM_ID);
                ps.executeUpdate();
            } catch (Exception ignored) {}

            for (Long uid : createdUserIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setLong(1, uid);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
            }

            for (Long tid : createdTeamIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM teams WHERE id = ?")) {
                    ps.setLong(1, tid);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
            }
        }
    }

    private Lead createLead(String name, String status, int assignedMinutesAgo, int slaMinutes) throws SQLException {
        Lead lead = new Lead();
        lead.setFullName(name);
        lead.setStatus(status);
        lead.setAssignedTo("USR-" + REP_USER_ID);
        lead.setAssignedToUserId(REP_USER_ID);
        long assignedTime = System.currentTimeMillis() - (assignedMinutesAgo * 60_000L);
        lead.setAssignedAt(new Timestamp(assignedTime));
        lead.setSlaTotalMinutes(slaMinutes);
        lead.setSlaDeadline(new Timestamp(assignedTime + (slaMinutes * 60_000L)));
        long id = leadDAO.insert(lead);
        lead.setId(id);
        createdLeadIds.add(id);
        return lead;
    }

    @Test
    @Order(1)
    void testOverdueLeadGeneratesNotificationForDirectTeamLeadOnly() throws Exception {
        // Assigned 150 min ago, 120 min SLA -> 30 min overdue
        Lead overdueLead = createLead("Nguyễn Văn Overdue", "PENDING_RESPONSE", 150, 120);

        int sent = monitorService.checkAndNotifyOverdueLeads(overdueLead.getId());
        assertEquals(1, sent, "Should have dispatched exactly 1 notification for overdue lead");

        // Verify lead is flagged OVERDUE in database
        try (Connection conn = DatabaseConfig.getConnection()) {
            Lead updated = leadDAO.findById(conn, overdueLead.getId());
            assertEquals("OVERDUE", updated.getSlaResult());

            // Verify Team Lead received the alert
            DataScopeContext tlScope = new DataScopeContext(TEAM_LEAD_USER_ID, "customer", ScopeType.SELF, REP_TEAM_ID, Set.of(TEAM_LEAD_USER_ID));
            List<Map<String, Object>> tlNotifs = notificationDAO.list(conn, TEAM_LEAD_USER_ID, tlScope, 1, 10);
            assertFalse(tlNotifs.isEmpty(), "Team Lead should have received the notification");
            Map<String, Object> notif = tlNotifs.get(0);
            assertEquals("LEAD_SLA_OVERDUE", notif.get("type"));
            assertTrue(((String) notif.get("title")).contains("SLA"));
            assertTrue(((String) notif.get("message")).contains("Nguyễn Văn Overdue"));

            // Verify other team lead in OTHER team did NOT receive the alert
            DataScopeContext otherScope = new DataScopeContext(OTHER_LEAD_USER_ID, "customer", ScopeType.SELF, OTHER_TEAM_ID, Set.of(OTHER_LEAD_USER_ID));
            List<Map<String, Object>> otherNotifs = notificationDAO.list(conn, OTHER_LEAD_USER_ID, otherScope, 1, 10);
            assertTrue(otherNotifs.isEmpty(), "Unrelated Team Lead in another team must NOT receive the alert");
        }
    }

    @Test
    @Order(2)
    void testSchedulerDeduplicationDoesNotSpamOnRepeatedRuns() throws Exception {
        Lead dedupLead = createLead("Trần Thị Dedup", "PENDING_RESPONSE", 180, 120);

        int firstRun = monitorService.checkAndNotifyOverdueLeads(dedupLead.getId());
        assertEquals(1, firstRun);

        // Second run immediately after for the same lead
        int secondRun = monitorService.checkAndNotifyOverdueLeads(dedupLead.getId());
        assertEquals(0, secondRun, "Repeated scheduler execution must NOT dispatch duplicate alerts (count must be 0)");

        // Verify total notification count for user is still exactly 1 for this lead
        try (Connection conn = DatabaseConfig.getConnection()) {
            DataScopeContext tlScope = new DataScopeContext(TEAM_LEAD_USER_ID, "customer", ScopeType.SELF, REP_TEAM_ID, Set.of(TEAM_LEAD_USER_ID));
            List<Map<String, Object>> list = notificationDAO.list(conn, TEAM_LEAD_USER_ID, tlScope, 1, 10);
            long countForLead = list.stream().filter(m -> ((String) m.get("message")).contains("Trần Thị Dedup")).count();
            assertEquals(1, countForLead);
        }
    }

    @Test
    @Order(3)
    void testProcessedOrAcceptedLeadsAreNotFlaggedOrNotified() throws Exception {
        Lead accepted = createLead("Lê Hoàng Accepted", "CONTACTING", 200, 120);
        Lead rejected = createLead("Phạm Bích Rejected", "REJECTED", 200, 120);

        int sentAccepted = monitorService.checkAndNotifyOverdueLeads(accepted.getId());
        assertEquals(0, sentAccepted, "Lead already CONTACTING must not trigger overdue SLA alert");

        int sentRejected = monitorService.checkAndNotifyOverdueLeads(rejected.getId());
        assertEquals(0, sentRejected, "Lead already REJECTED must not trigger overdue SLA alert");
    }

    @Test
    @Order(4)
    void testTeamLeadCanMarkNotificationAsRead() throws Exception {
        Lead lead = createLead("Hoàng Đọc Thông Báo", "PENDING_RESPONSE", 160, 120);
        int sent = monitorService.checkAndNotifyOverdueLeads(lead.getId());
        assertEquals(1, sent);

        try (Connection conn = DatabaseConfig.getConnection()) {
            DataScopeContext tlScope = new DataScopeContext(TEAM_LEAD_USER_ID, "customer", ScopeType.SELF, REP_TEAM_ID, Set.of(TEAM_LEAD_USER_ID));
            List<Map<String, Object>> list = notificationDAO.list(conn, TEAM_LEAD_USER_ID, tlScope, 1, 10);
            Map<String, Object> targetNotif = list.stream()
                    .filter(m -> ((String) m.get("message")).contains("Hoàng Đọc Thông Báo"))
                    .findFirst().orElseThrow();
            long notifId = ((Number) targetNotif.get("id")).longValue();

            Map<String, Object> marked = notificationDAO.markRead(conn, TEAM_LEAD_USER_ID, notifId, tlScope);
            assertNotNull(marked.get("readAt"));
        }
    }
}
