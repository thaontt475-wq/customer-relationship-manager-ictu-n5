package com.crm.service.leads;

import com.crm.dao.leads.LeadConversionDAO;
import com.crm.dto.leads.LeadConversionRequest;
import com.crm.dto.leads.LeadConversionResultDTO;
import com.crm.dto.leads.LeadConvertDuplicateCheckDTO;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.ScopeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadConversionServiceTest {

    private Connection mockConnection;
    private final Map<Long, Map<String, Object>> mockLeads = new HashMap<>();
    private final Map<Long, Map<String, Object>> mockCustomers = new HashMap<>();
    private final List<Map<String, Object>> createdContacts = new ArrayList<>();
    private final List<Map<String, Object>> createdOpportunities = new ArrayList<>();
    private final List<Map<String, Object>> conversionHistory = new ArrayList<>();

    private ScopeType currentScopeType = ScopeType.ALL;
    private Set<Long> allowedOwners = Set.of(10L, 20L);
    private boolean hasPermission = true;

    private LeadConversionDAO dao;
    private LeadConversionService service;

    @BeforeEach
    void setUp() {
        mockLeads.clear();
        mockCustomers.clear();
        createdContacts.clear();
        createdOpportunities.clear();
        conversionHistory.clear();
        currentScopeType = ScopeType.ALL;
        allowedOwners = Set.of(10L, 20L);
        hasPermission = true;

        mockConnection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName()) || "setAutoCommit".equals(method.getName())
                            || "commit".equals(method.getName()) || "rollback".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
        );

        dao = new LeadConversionDAO() {
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
                if (taxCode == null) return null;
                return mockCustomers.values().stream()
                        .filter(cust -> taxCode.equalsIgnoreCase((String) cust.get("taxCode")))
                        .findFirst().orElse(null);
            }

            @Override
            public Map<String, Object> findCustomerByName(Connection c, String companyName) {
                if (companyName == null) return null;
                return mockCustomers.values().stream()
                        .filter(cust -> companyName.equalsIgnoreCase((String) cust.get("name")))
                        .findFirst().orElse(null);
            }

            @Override
            public Map<String, Object> findCustomerById(Connection c, long customerId) {
                return mockCustomers.get(customerId);
            }

            @Override
            public long createCustomer(Connection c, LeadConversionRequest.CustomerData cust, long ownerUserId) {
                long id = 1000L + mockCustomers.size() + 1;
                Map<String, Object> record = new HashMap<>();
                record.put("id", id);
                record.put("name", cust.name);
                record.put("taxCode", cust.taxCode);
                record.put("ownerUserId", ownerUserId);
                mockCustomers.put(id, record);
                return id;
            }

            @Override
            public long createContact(Connection c, long customerId, LeadConversionRequest.ContactData contact) {
                long id = 5000L + createdContacts.size() + 1;
                Map<String, Object> record = new HashMap<>();
                record.put("id", id);
                record.put("customerId", customerId);
                record.put("fullName", contact.fullName);
                record.put("phone", contact.phone);
                record.put("email", contact.email);
                createdContacts.add(record);
                return id;
            }

            @Override
            public long createOpportunity(Connection c, long customerId, String contactName, LeadConversionRequest.DealData deal, long ownerUserId) {
                long id = 8000L + createdOpportunities.size() + 1;
                Map<String, Object> record = new HashMap<>();
                record.put("id", id);
                record.put("customerId", customerId);
                record.put("name", deal.name);
                record.put("amount", deal.amount);
                createdOpportunities.add(record);
                return id;
            }

            @Override
            public void updateLeadAsConverted(Connection c, long leadId, long userId, long customerId, long contactId, Long opportunityId) {
                Map<String, Object> lead = mockLeads.get(leadId);
                if (lead != null) {
                    lead.put("status", "CONVERTED");
                    lead.put("isLocked", true);
                    lead.put("convertedBy", userId);
                    lead.put("convertedCustomerId", customerId);
                    lead.put("convertedContactId", contactId);
                    lead.put("convertedOpportunityId", opportunityId);
                }
            }

            @Override
            public void insertConversionHistory(Connection c, long leadId, long customerId, long contactId, Long opportunityId, long userId, String customerMode, String payloadJson, String snapshotJson) {
                conversionHistory.add(Map.of("leadId", leadId, "customerId", customerId, "contactId", contactId));
            }

            @Override
            public int transferLeadActivitiesToCustomer(Connection c, long leadId, long customerId, Long opportunityId, long currentUserId, String leadNotes) {
                return 0;
            }

            @Override
            public void insertAuditLog(Connection c, long userId, String action, String entityType, long entityId, String description) throws SQLException {
                // mock no-op
            }
        };

        service = new LeadConversionService(dao, (user, module, action) -> {
            if (!hasPermission) {
                throw new SecurityException("Người dùng không có quyền truy cập " + module + "." + action);
            }
            return new DataScopeContext(user, module, currentScopeType, 1L, allowedOwners);
        });

        // Seed a standard qualified lead
        Map<String, Object> lead1 = new HashMap<>();
        lead1.put("id", 1L);
        lead1.put("fullName", "Nguyễn Hoàng Nam");
        lead1.put("companyName", "Công ty Cổ phần Công nghệ Cao Việt Tech");
        lead1.put("taxCode", "0108923451");
        lead1.put("email", "nam.nh@viettech.com.vn");
        lead1.put("phone", "0912345678");
        lead1.put("status", "QUALIFIED");
        lead1.put("ownerUserId", 10L);
        lead1.put("isLocked", false);
        mockLeads.put(1L, lead1);

        // Seed an existing customer
        Map<String, Object> custExisting = new HashMap<>();
        custExisting.put("id", 1001L);
        custExisting.put("name", "Công ty Cổ phần Mediphar Quốc tế");
        custExisting.put("taxCode", "0107892341");
        custExisting.put("ownerUserId", 10L);
        mockCustomers.put(1001L, custExisting);
    }

    private LeadConversionRequest sampleRequest(String customerMode, Long existingCustomerId, boolean createDeal) {
        LeadConversionRequest req = new LeadConversionRequest();
        req.customerMode = customerMode;
        req.existingCustomerId = existingCustomerId;

        req.customer = new LeadConversionRequest.CustomerData();
        req.customer.name = "Công ty TNHH Phần Mềm Đại Việt";
        req.customer.taxCode = "0109998888";
        req.customer.industry = "CNTT & Viễn thông";
        req.customer.phone = "0243123456";
        req.customer.address = "Tầng 5, Cầu Giấy, Hà Nội";

        req.contact = new LeadConversionRequest.ContactData();
        req.contact.fullName = "Nguyễn Hoàng Nam";
        req.contact.phone = "0912345678";
        req.contact.email = "nam.nh@viettech.com.vn";
        req.contact.title = "Tổng Giám đốc (CEO)";
        req.contact.buyingRole = "DECISION_MAKER";

        req.deal = new LeadConversionRequest.DealData();
        req.deal.createDeal = createDeal;
        if (createDeal) {
            req.deal.name = "Hợp đồng triển khai CRM Doanh nghiệp";
            req.deal.amount = new BigDecimal("250000000");
            req.deal.closeDate = "2026-11-15";
            req.deal.stageId = 2L;
            req.deal.probability = 40;
        }
        return req;
    }

    @Test
    void testConvertValidLeadCreateNewCustomer() throws Exception {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        LeadConversionResultDTO res = service.convert(10L, 1L, req);

        assertTrue(res.success);
        assertNotNull(res.customerId);
        assertEquals("Công ty TNHH Phần Mềm Đại Việt", res.customerName);
        assertNotNull(res.contactId);
        assertEquals("Nguyễn Hoàng Nam", res.contactName);
        assertNotNull(res.dealId);
        assertEquals("Hợp đồng triển khai CRM Doanh nghiệp", res.dealName);

        Map<String, Object> updatedLead = mockLeads.get(1L);
        assertEquals("CONVERTED", updatedLead.get("status"));
        assertEquals(true, updatedLead.get("isLocked"));
        assertEquals(1, conversionHistory.size());
    }

    @Test
    void testConvertValidLeadMergeExistingCustomer() throws Exception {
        LeadConversionRequest req = sampleRequest("MERGE_EXISTING", 1001L, true);
        LeadConversionResultDTO res = service.convert(10L, 1L, req);

        assertTrue(res.success);
        assertEquals(1001L, res.customerId);
        assertEquals("Công ty Cổ phần Mediphar Quốc tế", res.customerName);
        assertTrue(res.customerInfo.contains("0107892341"));

        Map<String, Object> updatedLead = mockLeads.get(1L);
        assertEquals("CONVERTED", updatedLead.get("status"));
    }

    @Test
    void testConvertWithoutDealCreation() throws Exception {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, false);
        LeadConversionResultDTO res = service.convert(10L, 1L, req);

        assertTrue(res.success);
        assertNull(res.dealId);
        assertEquals("", res.dealName);
        assertEquals("Không tạo cơ hội", res.dealInfo);
        assertEquals(0, createdOpportunities.size());
    }

    @Test
    void testConvertLeadNotFound() {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        assertThrows(NoSuchElementException.class, () -> service.convert(10L, 9999L, req));
    }

    @Test
    void testConvertLeadForbiddenScope() {
        // Lead owned by 20L, but user 99L is outside allowedOwners
        currentScopeType = ScopeType.SELF;
        allowedOwners = Set.of(99L);

        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        assertThrows(SecurityException.class, () -> service.convert(99L, 1L, req));
    }

    @Test
    void testConvertLeadNoPermission() {
        hasPermission = false;
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        assertThrows(SecurityException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertLeadAlreadyConverted() {
        mockLeads.get(1L).put("status", "CONVERTED");
        mockLeads.get(1L).put("isLocked", true);

        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.convert(10L, 1L, req));
        assertTrue(ex.getMessage().contains("đã được chuyển đổi"));
    }

    @Test
    void testConvertLeadAlreadyLocked() {
        mockLeads.get(1L).put("isLocked", true);

        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        assertThrows(IllegalStateException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertLeadMerged() {
        mockLeads.get(1L).put("status", "MERGED");

        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.convert(10L, 1L, req));
        assertTrue(ex.getMessage().contains("Lead đã bị gộp"));
    }

    @Test
    void testConvertInvalidCustomerName() {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        req.customer.name = "   ";
        assertThrows(IllegalArgumentException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertMissingExistingCustomerSelector() {
        LeadConversionRequest req = sampleRequest("MERGE_EXISTING", null, true);
        assertThrows(IllegalArgumentException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertNonExistentMergeCustomer() {
        LeadConversionRequest req = sampleRequest("MERGE_EXISTING", 8888L, true);
        assertThrows(NoSuchElementException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertInvalidContactEmail() {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        req.contact.email = "invalid-email";
        assertThrows(IllegalArgumentException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertInvalidDealAmount() {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        req.deal.amount = BigDecimal.ZERO;
        assertThrows(IllegalArgumentException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertInvalidDealDate() {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        req.deal.closeDate = "not-a-date";
        assertThrows(IllegalArgumentException.class, () -> service.convert(10L, 1L, req));
    }

    @Test
    void testConvertTaxCodeDuplicateOnCreateNew() {
        LeadConversionRequest req = sampleRequest("CREATE_NEW", null, true);
        req.customer.taxCode = "0107892341"; // matches existing customer 1001

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.convert(10L, 1L, req));
        assertTrue(ex.getMessage().contains("đã tồn tại trên hệ thống"));
    }

    @Test
    void testCheckDuplicatesTaxCodeMatch() throws Exception {
        // Set lead tax code to match existing customer
        mockLeads.get(1L).put("taxCode", "0107892341");
        LeadConvertDuplicateCheckDTO check = service.checkConvertDuplicates(10L, 1L);

        assertTrue(check.hasDuplicate);
        assertNotNull(check.duplicateCustomer);
        assertTrue(check.reason.contains("Trùng Mã số thuế"));
    }

    @Test
    void testCheckDuplicatesCompanyNameMatch() throws Exception {
        // Set lead company name to match existing customer
        mockLeads.get(1L).put("taxCode", "9999999999");
        mockLeads.get(1L).put("companyName", "Công ty Cổ phần Mediphar Quốc tế");
        LeadConvertDuplicateCheckDTO check = service.checkConvertDuplicates(10L, 1L);

        assertTrue(check.hasDuplicate);
        assertTrue(check.reason.contains("Trùng Tên doanh nghiệp"));
    }

    @Test
    void testCheckDuplicatesNoMatch() throws Exception {
        mockLeads.get(1L).put("taxCode", "0108923451");
        mockLeads.get(1L).put("companyName", "Việt Tech Không Trùng");
        LeadConvertDuplicateCheckDTO check = service.checkConvertDuplicates(10L, 1L);

        assertFalse(check.hasDuplicate);
        assertNull(check.duplicateCustomer);
    }
}
