package com.crm.service.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dao.leads.LeadDAO;
import com.crm.dao.leads.SavedLeadFilterDAO;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.model.leads.Lead;
import com.crm.model.leads.SavedLeadFilter;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadFilterJdbcTest {

    private LeadDAO leadDAO;
    private LeadService leadService;
    private SavedLeadFilterDAO filterDAO;
    private SavedLeadFilterService filterService;

    private final List<Long> createdLeadIds = new ArrayList<>();
    private final List<Long> createdFilterIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        leadDAO = new LeadDAO();
        com.crm.service.permissions.DataScopeService mockScopeService = new com.crm.service.permissions.DataScopeService() {
            @Override
            public DataScopeContext resolve(long currentUserId, String module, String action) {
                return new DataScopeContext(currentUserId, module, ScopeType.ALL, null, Set.of());
            }
        };
        leadService = new LeadService(leadDAO, mockScopeService);
        filterDAO = new SavedLeadFilterDAO();
        filterService = new SavedLeadFilterService(filterDAO);
    }

    @AfterEach
    void tearDown() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (!createdLeadIds.isEmpty()) {
                String leadIn = String.join(",", Collections.nCopies(createdLeadIds.size(), "?"));
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM leads WHERE id IN (" + leadIn + ")")) {
                    for (int i = 0; i < createdLeadIds.size(); i++) {
                        ps.setLong(i + 1, createdLeadIds.get(i));
                    }
                    ps.executeUpdate();
                }
            }
            if (!createdFilterIds.isEmpty()) {
                String filterIn = String.join(",", Collections.nCopies(createdFilterIds.size(), "?"));
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM saved_lead_filters WHERE id IN (" + filterIn + ")")) {
                    for (int i = 0; i < createdFilterIds.size(); i++) {
                        ps.setLong(i + 1, createdFilterIds.get(i));
                    }
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
        }
    }

    @Test
    void testEvaluateSlaCalculations() {
        long now = System.currentTimeMillis();

        // 1. Overdue Lead
        Lead overdueLead = new Lead();
        overdueLead.setStatus("NEW");
        overdueLead.setSlaDeadline(new Timestamp(now - (90 * 60 * 1000))); // 90 mins ago
        leadService.evaluateSla(overdueLead, now);

        assertTrue(overdueLead.isOverdueSla());
        assertEquals("OVERDUE", overdueLead.getSlaStatus());
        assertNotNull(overdueLead.getSlaRemainingText());
        assertTrue(overdueLead.getSlaRemainingText().contains("Quá hạn"));

        // 2. Warning Lead (<= 30 mins)
        Lead warningLead = new Lead();
        warningLead.setStatus("IN_PROGRESS");
        warningLead.setSlaDeadline(new Timestamp(now + (20 * 60 * 1000))); // 20 mins left
        leadService.evaluateSla(warningLead, now);

        assertFalse(warningLead.isOverdueSla());
        assertEquals("WARNING", warningLead.getSlaStatus());
        assertNotNull(warningLead.getSlaRemainingText());
        assertTrue(warningLead.getSlaRemainingText().contains("Còn"));

        // 3. On Track Lead (> 30 mins)
        Lead onTrackLead = new Lead();
        onTrackLead.setStatus("IN_PROGRESS");
        onTrackLead.setSlaDeadline(new Timestamp(now + (100 * 60 * 1000))); // 100 mins left
        leadService.evaluateSla(onTrackLead, now);

        assertFalse(onTrackLead.isOverdueSla());
        assertEquals("SAFE", onTrackLead.getSlaStatus());

        // 4. Closed Lead (CONVERTED or LOST)
        Lead convertedLead = new Lead();
        convertedLead.setStatus("CONVERTED");
        convertedLead.setSlaDeadline(new Timestamp(now - (120 * 60 * 1000)));
        leadService.evaluateSla(convertedLead, now);

        assertFalse(convertedLead.isOverdueSla());
        assertEquals("CLOSED", convertedLead.getSlaStatus());
    }

    @Test
    void testLeadDaoFilterAndSearch() {
        long now = System.currentTimeMillis();

        // Insert Test Lead 1: HOT, Website, NEW, Overdue
        Lead l1 = new Lead();
        l1.setFullName("VIP Cong Ty Alpha S409");
        l1.setCompanyName("Alpha Tech S409");
        l1.setEmail("alpha.s409@example.com");
        l1.setPhone("0912345678");
        l1.setSource("WEBSITE");
        l1.setRating("HOT");
        l1.setTier("HOT");
        l1.setStatus("NEW");
        l1.setSlaDeadline(new Timestamp(now - 86400000L)); // overdue by 1 day
        l1.setEstimatedValue(50000000);
        long id1 = leadDAO.insert(l1);
        assertTrue(id1 > 0);
        createdLeadIds.add(id1);

        // Insert Test Lead 2: COLD, Referral, CONVERTED
        Lead l2 = new Lead();
        l2.setFullName("Cong Ty Beta S409");
        l2.setCompanyName("Beta Logistics S409");
        l2.setEmail("beta.s409@example.com");
        l2.setPhone("0987654321");
        l2.setSource("REFERRAL");
        l2.setRating("COLD");
        l2.setTier("COLD");
        l2.setStatus("CONVERTED");
        l2.setSlaDeadline(new Timestamp(now + 7200000));
        l2.setEstimatedValue(20000000);
        long id2 = leadDAO.insert(l2);
        assertTrue(id2 > 0);
        createdLeadIds.add(id2);

        DataScopeContext scope = new DataScopeContext(1L, "lead", ScopeType.ALL, null, Set.of());

        // 1. Filter by Keyword
        LeadFilterRequest kwFilter = new LeadFilterRequest();
        kwFilter.setKeyword("Alpha Tech S409");
        kwFilter.setSize(100);
        List<Lead> kwResults = leadDAO.findWithFilters(kwFilter, scope, 1L);
        assertFalse(kwResults.isEmpty());
        assertTrue(kwResults.stream().anyMatch(l -> l.getId() == id1));
        assertFalse(kwResults.stream().anyMatch(l -> l.getId() == id2));

        // 2. Filter by Tier HOT
        LeadFilterRequest tierFilter = new LeadFilterRequest();
        tierFilter.setTier("HOT");
        tierFilter.setSize(100);
        List<Lead> tierResults = leadDAO.findWithFilters(tierFilter, scope, 1L);
        assertTrue(tierResults.stream().anyMatch(l -> l.getId() == id1));
        assertFalse(tierResults.stream().anyMatch(l -> l.getId() == id2));

        // 3. Filter by SLA Overdue
        LeadFilterRequest slaFilter = new LeadFilterRequest();
        slaFilter.setSlaOverdueOnly(true);
        slaFilter.setSize(100);
        List<Lead> slaResults = leadDAO.findWithFilters(slaFilter, scope, 1L);
        assertTrue(slaResults.stream().anyMatch(l -> l.getId() == id1));
        assertFalse(slaResults.stream().anyMatch(l -> l.getId() == id2));
    }

    @Test
    void testLockedAndConvertedLeadModificationProtected() throws Exception {
        Lead converted = new Lead();
        converted.setFullName("Doanh Nghiep Da Chuyen Doi");
        converted.setCompanyName("Da Chuyen Doi Corp");
        converted.setStatus("CONVERTED");
        long id = leadDAO.insert(converted);
        assertTrue(id > 0);
        createdLeadIds.add(id);

        Lead updateTarget = new Lead();
        updateTarget.setId(id);
        updateTarget.setFullName("Cố tình sửa tên");

        // Attempting to update a converted lead must throw IllegalStateException
        IllegalStateException updateEx = assertThrows(IllegalStateException.class, () ->
                leadService.updateLead(updateTarget, 1L));
        assertTrue(updateEx.getMessage().contains("chuyển đổi") || updateEx.getMessage().contains("khóa"));

        // Attempting to delete a converted lead must throw IllegalStateException
        IllegalStateException deleteEx = assertThrows(IllegalStateException.class, () ->
                leadService.deleteLead(id, 1L));
        assertTrue(deleteEx.getMessage().contains("chuyển đổi") || deleteEx.getMessage().contains("khóa"));
    }

    @Test
    void testSavedFiltersCrossUserSecurity() {
        long user1 = 10L;
        long user2 = 20L;

        // User 1 creates custom filter
        SavedLeadFilter filter = new SavedLeadFilter();
        filter.setName("Bộ lọc riêng của User 10");
        filter.setFilterCriteria("{\"tier\":\"HOT\"}");
        filter.setDefault(false);

        SavedLeadFilter created = filterService.saveCustomFilter(filter, user1);
        assertNotNull(created);
        long filterId = Long.parseLong(created.getId().toString().replace("custom_filter_", ""));
        createdFilterIds.add(filterId);

        // User 2 attempts to update User 1's filter -> Must throw SecurityException
        SavedLeadFilter maliciousUpdate = new SavedLeadFilter();
        maliciousUpdate.setId(filterId);
        maliciousUpdate.setName("Tên bị sửa bởi User 20");

        assertThrows(SecurityException.class, () ->
                filterService.updateCustomFilter(maliciousUpdate, user2));

        // User 2 attempts to delete User 1's filter -> Must throw SecurityException
        assertThrows(SecurityException.class, () ->
                filterService.deleteCustomFilter(filterId, user2));

        // User 1 updates successfully
        created.setName("User 10 cập nhật bộ lọc thành công");
        boolean updated = filterService.updateCustomFilter(created, user1);
        assertTrue(updated);

        // User 1 deletes successfully
        boolean deleted = filterService.deleteCustomFilter(filterId, user1);
        assertTrue(deleted);
    }
}
