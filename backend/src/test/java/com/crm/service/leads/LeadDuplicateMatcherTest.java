package com.crm.service.leads;

import com.crm.dto.leads.LeadDuplicatePairDTO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeadDuplicateMatcherTest {

    @Test
    void testNormalizePhone() {
        assertEquals("0987123456", LeadDuplicateMatcher.normalizePhone("0987123456"));
        assertEquals("0987123456", LeadDuplicateMatcher.normalizePhone("+84 987 123 456"));
        assertEquals("0987123456", LeadDuplicateMatcher.normalizePhone("+84-987.123.456"));
        assertEquals("0987123456", LeadDuplicateMatcher.normalizePhone("84987123456"));
        assertEquals("0987123456", LeadDuplicateMatcher.normalizePhone("(0987) 123-456"));
        assertNull(LeadDuplicateMatcher.normalizePhone("1234")); // Too short
        assertNull(LeadDuplicateMatcher.normalizePhone(null));
        assertNull(LeadDuplicateMatcher.normalizePhone("   "));
    }

    @Test
    void testNormalizeEmail() {
        assertEquals("test.user@example.com", LeadDuplicateMatcher.normalizeEmail("  Test.User@Example.COM "));
        assertNull(LeadDuplicateMatcher.normalizeEmail("invalid-email"));
        assertNull(LeadDuplicateMatcher.normalizeEmail(null));
        assertNull(LeadDuplicateMatcher.normalizeEmail(""));
    }

    @Test
    void testNormalizeCompanyName() {
        assertEquals("fpt", LeadDuplicateMatcher.normalizeCompanyName("Công ty Cổ phần FPT"));
        assertEquals("vinasmart", LeadDuplicateMatcher.normalizeCompanyName("Tập đoàn VinaSmart Global"));
        assertEquals("digitech", LeadDuplicateMatcher.normalizeCompanyName("Cty TNHH DigiTech VN"));
        assertNull(LeadDuplicateMatcher.normalizeCompanyName(null));
        assertNull(LeadDuplicateMatcher.normalizeCompanyName("  "));
    }

    @Test
    void testDuplicateByEmail() {
        Map<String, Object> a = new HashMap<>();
        a.put("id", 1L);
        a.put("fullName", "Nguyen Van A");
        a.put("email", "van.a@example.com");
        a.put("phone", "0901111111");

        Map<String, Object> b = new HashMap<>();
        b.put("id", 2L);
        b.put("fullName", "N. V. A");
        b.put("email", "VAN.A@example.com");
        b.put("phone", "0902222222");

        LeadDuplicatePairDTO pair = LeadDuplicateMatcher.matchLeads(a, b);
        assertNotNull(pair);
        assertEquals("EMAIL", pair.getCriterion());
        assertTrue(pair.getConfidence() >= 90);
        assertFalse(pair.isCustomerMatch());
        assertTrue(pair.getMatchedFields().stream().anyMatch(f -> f.contains("Email")));
    }

    @Test
    void testDuplicateByPhone() {
        Map<String, Object> a = new HashMap<>();
        a.put("id", 10L);
        a.put("fullName", "Tran Thi B");
        a.put("phone", "+84 987 654 321");
        a.put("email", "b1@example.com");

        Map<String, Object> b = new HashMap<>();
        b.put("id", 11L);
        b.put("fullName", "Tran Thi B");
        b.put("phone", "0987654321");
        b.put("email", "b2@example.com");

        LeadDuplicatePairDTO pair = LeadDuplicateMatcher.matchLeads(a, b);
        assertNotNull(pair);
        assertEquals("PHONE", pair.getCriterion());
        assertTrue(pair.getConfidence() >= 90);
        assertFalse(pair.isCustomerMatch());
        assertTrue(pair.getMatchedFields().stream().anyMatch(f -> f.contains("Số điện thoại")));
    }

    @Test
    void testDuplicateByCompanyName() {
        Map<String, Object> a = new HashMap<>();
        a.put("id", 20L);
        a.put("fullName", "Le C");
        a.put("companyName", "Công ty Cổ phần Công nghệ FPT");
        a.put("email", "c1@c.com");
        a.put("phone", "0911000001");

        Map<String, Object> b = new HashMap<>();
        b.put("id", 21L);
        b.put("fullName", "Pham D");
        b.put("companyName", "Tập đoàn FPT");
        b.put("email", "d2@d.com");
        b.put("phone", "0911000002");

        LeadDuplicatePairDTO pair = LeadDuplicateMatcher.matchLeads(a, b);
        assertNotNull(pair);
        assertEquals("COMPANY", pair.getCriterion());
        assertTrue(pair.getConfidence() >= 80);
        assertTrue(pair.getMatchedFields().stream().anyMatch(f -> f.contains("doanh nghiệp")));
    }

    @Test
    void testNotDuplicateWhenDifferent() {
        Map<String, Object> a = new HashMap<>();
        a.put("id", 30L);
        a.put("fullName", "Hoang E");
        a.put("email", "hoang.e@alpha.vn");
        a.put("phone", "0981111222");
        a.put("companyName", "Alpha Corporation");

        Map<String, Object> b = new HashMap<>();
        b.put("id", 31L);
        b.put("fullName", "Vu F");
        b.put("email", "vu.f@omega.vn");
        b.put("phone", "0973333444");
        b.put("companyName", "Omega Global");

        LeadDuplicatePairDTO pair = LeadDuplicateMatcher.matchLeads(a, b);
        assertNull(pair);
    }

    @Test
    void testSuggestMatchingCustomer() {
        Map<String, Object> lead = new HashMap<>();
        lead.put("id", 40L);
        lead.put("fullName", "Ngo Thi G");
        lead.put("phone", "0912345678");
        lead.put("email", "g.ngo@vietnam.vn");
        lead.put("companyName", "Hoa Linh Pharma");

        Map<String, Object> customer = new HashMap<>();
        customer.put("id", 500L);
        customer.put("name", "Tập đoàn Dược phẩm Hoa Linh");
        customer.put("phone", "+84 912 345 678");
        customer.put("email", "contact@hoalinh.vn");

        LeadDuplicatePairDTO pair = LeadDuplicateMatcher.matchLeadAndCustomer(lead, customer);
        assertNotNull(pair);
        assertEquals("CUSTOMER", pair.getCriterion());
        assertTrue(pair.isCustomerMatch());
        assertEquals(99, pair.getConfidence());
        assertTrue(pair.getId().startsWith("DUP-CUST-"));
        assertEquals("CUST-500", pair.getRecordB().get("id"));
    }
}
