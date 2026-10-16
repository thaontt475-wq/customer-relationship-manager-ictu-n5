package com.crm.controller.leads;

import com.crm.dto.leads.LeadConversionRequest;
import com.crm.dto.leads.LeadConversionResultDTO;
import com.crm.dto.leads.LeadConvertDuplicateCheckDTO;
import com.crm.dto.leads.LeadDTO;
import com.crm.service.leads.LeadConversionService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadConversionServletTest {

    private final LeadConversionService mockService = new LeadConversionService(null, null) {
        @Override
        public LeadConvertDuplicateCheckDTO checkConvertDuplicates(long userId, long leadId) {
            if (leadId == 404L) throw new NoSuchElementException("Không tìm thấy Lead");
            if (leadId == 403L) throw new SecurityException("Không có quyền");
            if (leadId == 2L) {
                return new LeadConvertDuplicateCheckDTO(true, Map.of("id", 1001L, "name", "Mediphar"), "Trùng MST");
            }
            return new LeadConvertDuplicateCheckDTO(false, null, null);
        }

        @Override
        public LeadConversionResultDTO convert(long userId, long leadId, LeadConversionRequest request) {
            if (leadId == 404L) throw new NoSuchElementException("Không tìm thấy Lead");
            if (leadId == 403L) throw new SecurityException("Không có quyền chuyển đổi");
            if (leadId == 409L) throw new IllegalStateException("Lead đã được chuyển đổi trước đó");
            if (request.customer != null && "CONFLICT_TAX".equals(request.customer.taxCode)) {
                throw new IllegalStateException("Mã số thuế đã tồn tại trên hệ thống");
            }

            LeadConversionResultDTO res = new LeadConversionResultDTO();
            res.success = true;
            res.customerId = 1001L;
            res.customerName = request.customerMode.equals("MERGE_EXISTING") ? "Mediphar" : request.customer.name;
            res.customerInfo = "MST: 010123";
            res.contactId = 501L;
            res.contactName = request.contact.fullName;
            res.contactInfo = "Giám đốc - 0912345678";
            res.dealId = request.deal != null && request.deal.createDeal ? 801L : null;
            res.dealName = request.deal != null && request.deal.createDeal ? request.deal.name : "";
            res.dealInfo = res.dealId != null ? "250.000.000 ₫" : "Không tạo cơ hội";
            res.leadCode = "LEAD-" + leadId;
            res.convertedAt = "2026-10-15T12:00:00Z";
            return res;
        }

        @Override
        public Map<String, Object> getLeadById(long userId, long leadId) {
            if (leadId == 404L) return null;
            return Map.of("id", leadId, "fullName", "Lead " + leadId, "status", "QUALIFIED");
        }

        @Override
        public List<LeadDTO> getLeads(long userId, String status, String keyword, int limit, int offset) {
            LeadDTO l = new LeadDTO();
            l.id = 1L;
            l.fullName = "Lead 1";
            l.status = "QUALIFIED";
            return List.of(l);
        }
    };

    private static class CallResult {
        int status;
        String body;
        String allowHeader;
    }

    private CallResult invokeServlet(String servletPath, String pathInfo, String method, String jsonBody, String contentType, Map<String, String> params, boolean authenticated) throws Exception {
        CallResult result = new CallResult();
        StringWriter out = new StringWriter();

        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpSession.class},
                (proxy, m, args) -> {
                    if ("getAttribute".equals(m.getName()) && "userId".equals(args[0])) {
                        return 10L;
                    }
                    return null;
                }
        );

        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (proxy, m, args) -> {
                    String name = m.getName();
                    if ("getSession".equals(name)) return authenticated ? session : null;
                    if ("getServletPath".equals(name)) return servletPath;
                    if ("getPathInfo".equals(name)) return pathInfo;
                    if ("getMethod".equals(name)) return method;
                    if ("getContentType".equals(name)) return contentType;
                    if ("getParameter".equals(name)) return params != null ? params.get(args[0]) : null;
                    if ("getReader".equals(name)) {
                        return new BufferedReader(new StringReader(jsonBody != null ? jsonBody : ""));
                    }
                    return null;
                }
        );

        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletResponse.class},
                (proxy, m, args) -> {
                    String name = m.getName();
                    if ("setStatus".equals(name)) {
                        result.status = (int) args[0];
                    }
                    if ("setHeader".equals(name)) {
                        if ("Allow".equals(args[0])) result.allowHeader = (String) args[1];
                    }
                    if ("getWriter".equals(name)) {
                        return new java.io.PrintWriter(out);
                    }
                    return null;
                }
        );

        LeadConversionServlet servlet = new LeadConversionServlet(mockService);
        servlet.service(req, resp);
        result.body = out.toString();
        return result;
    }

    @Test
    void testCheckDuplicatesSuccess() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/2/check-convert-duplicates", "GET", null, null, null, true);
        assertEquals(200, r.status);
        JsonObject json = JsonUtil.getGson().fromJson(r.body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertTrue(json.getAsJsonObject("data").get("hasDuplicate").getAsBoolean());
    }

    @Test
    void testConvertSuccess() throws Exception {
        LeadConversionRequest req = new LeadConversionRequest();
        req.customerMode = "CREATE_NEW";
        req.customer = new LeadConversionRequest.CustomerData();
        req.customer.name = "Công ty ABC";
        req.contact = new LeadConversionRequest.ContactData();
        req.contact.fullName = "Nguyễn Văn A";
        req.deal = new LeadConversionRequest.DealData();
        req.deal.createDeal = true;
        req.deal.name = "Dự án CRM";
        req.deal.amount = new BigDecimal("250000000");

        String body = JsonUtil.getGson().toJson(req);
        CallResult r = invokeServlet("/api/leads", "/1/convert", "POST", body, "application/json", null, true);

        assertEquals(200, r.status);
        JsonObject json = JsonUtil.getGson().fromJson(r.body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("Công ty ABC", json.getAsJsonObject("data").get("customerName").getAsString());
        assertEquals(801L, json.getAsJsonObject("data").get("dealId").getAsLong());
    }

    @Test
    void testConvertWithAlphanumericId() throws Exception {
        LeadConversionRequest req = new LeadConversionRequest();
        req.customerMode = "CREATE_NEW";
        req.customer = new LeadConversionRequest.CustomerData();
        req.customer.name = "Công ty ABC";
        req.contact = new LeadConversionRequest.ContactData();
        req.contact.fullName = "Nguyễn Văn A";

        String body = JsonUtil.getGson().toJson(req);
        CallResult r = invokeServlet("/api/leads", "/LD-CONV-01/convert", "POST", body, "application/json", null, true);

        assertEquals(200, r.status);
        JsonObject json = JsonUtil.getGson().fromJson(r.body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
    }

    @Test
    void testUnauthenticatedReturns401() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/1/convert", "POST", "{}", "application/json", null, false);
        assertEquals(401, r.status);
    }

    @Test
    void testForbiddenReturns403() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/403/convert", "POST", "{}", "application/json", null, true);
        assertEquals(403, r.status);
    }

    @Test
    void testNotFoundReturns404() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/404/convert", "POST", "{}", "application/json", null, true);
        assertEquals(404, r.status);
    }

    @Test
    void testConflictReturns409() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/409/convert", "POST", "{}", "application/json", null, true);
        assertEquals(409, r.status);
    }

    @Test
    void testNonJsonContentTypeReturns400() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/1/convert", "POST", "name=ABC", "application/x-www-form-urlencoded", null, true);
        assertEquals(400, r.status);
    }

    @Test
    void testUnsupportedMethodReturns405() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/1/convert", "PUT", "{}", "application/json", null, true);
        assertEquals(405, r.status);
        assertEquals("POST", r.allowHeader);
    }

    @Test
    void testGetSingleLead() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/1", "GET", null, null, null, true);
        assertEquals(200, r.status);
        JsonObject json = JsonUtil.getGson().fromJson(r.body, JsonObject.class);
        assertEquals(1L, json.getAsJsonObject("data").get("id").getAsLong());
    }

    @Test
    void testGetLeadsList() throws Exception {
        CallResult r = invokeServlet("/api/leads", "/", "GET", null, null, null, true);
        assertEquals(200, r.status);
        JsonObject json = JsonUtil.getGson().fromJson(r.body, JsonObject.class);
        assertEquals(1, json.getAsJsonArray("data").size());
    }
}
