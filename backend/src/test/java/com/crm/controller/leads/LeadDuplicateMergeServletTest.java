package com.crm.controller.leads;

import com.crm.dto.leads.LeadDuplicatePairDTO;
import com.crm.service.leads.LeadDuplicateMergeService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadDuplicateMergeServletTest {

    private final LeadDuplicateMergeService mockService = new LeadDuplicateMergeService(null, null) {
        @Override
        public List<LeadDuplicatePairDTO> getDuplicates(long userId) {
            Map<String, Object> a = Map.of("id", 1L, "fullName", "Lead A", "phone", "0901234567");
            Map<String, Object> b = Map.of("id", 2L, "fullName", "Lead B", "phone", "0901234567");
            return List.of(new LeadDuplicatePairDTO("DUP-1-2", "PHONE", 95, "2026-10-15T12:00:00Z", a, b, false, List.of("Phone")));
        }

        @Override
        public Map<String, Object> merge(long userId, com.crm.dto.leads.LeadMergeRequest request) {
            if (request.masterId == 404L) throw new NoSuchElementException("Không tìm thấy lead");
            if (request.masterId == 403L) throw new SecurityException("Không có quyền");
            if (request.masterId == 409L) throw new IllegalStateException("Lead đã gộp");
            return Map.of("success", true, "masterId", request.masterId, "duplicateId", request.duplicateId, "merged", true);
        }

        @Override
        public Map<String, Object> attachToCustomer(long userId, com.crm.dto.leads.LeadAttachCustomerRequest request) {
            return Map.of("success", true, "leadId", request.leadId, "customerId", request.customerId, "linked", true);
        }
    };

    private static class CallResult {
        int status;
        String body;
        String allowHeader;
    }

    private CallResult invokeServlet(String route, String method, String jsonBody, String contentType, Map<String, String> params, boolean authenticated) throws Exception {
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
                    if ("getServletPath".equals(name)) return route;
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
                        return new PrintWriter(out);
                    }
                    return null;
                }
        );

        LeadDuplicateMergeServlet servlet = new LeadDuplicateMergeServlet(mockService);
        servlet.service((ServletRequest) req, (ServletResponse) resp);
        result.body = out.toString();
        return result;
    }

    @Test
    void testServletRouteAnnotations() {
        WebServlet ws = LeadDuplicateMergeServlet.class.getAnnotation(WebServlet.class);
        assertNotNull(ws);
        List<String> routes = Arrays.asList(ws.value());
        assertTrue(routes.contains("/api/leads/duplicates"));
        assertTrue(routes.contains("/api/leads/merge"));
        assertTrue(routes.contains("/api/leads/attach-customer"));
        assertTrue(routes.contains("/api/leads/link-customer"));
    }

    @Test
    void testUnauthenticatedReturns401() throws Exception {
        CallResult res = invokeServlet("/api/leads/duplicates", "GET", null, "application/json", null, false);
        assertEquals(401, res.status);
        JsonObject json = JsonUtil.getGson().fromJson(res.body, JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
    }

    @Test
    void testMethodNotAllowedReturns405() throws Exception {
        CallResult res = invokeServlet("/api/leads/duplicates", "POST", "{}", "application/json", null, true);
        assertEquals(405, res.status);
        assertEquals("GET", res.allowHeader);

        CallResult res2 = invokeServlet("/api/leads/merge", "GET", null, "application/json", null, true);
        assertEquals(405, res2.status);
        assertEquals("POST", res2.allowHeader);
    }

    @Test
    void testGetDuplicatesReturnsRawJsonArrayForFrontend() throws Exception {
        CallResult res = invokeServlet("/api/leads/duplicates", "GET", null, "application/json", null, true);
        assertEquals(200, res.status);

        // FE specifically checks Array.isArray(data)
        JsonArray arr = JsonUtil.getGson().fromJson(res.body, JsonArray.class);
        assertNotNull(arr);
        assertEquals(1, arr.size());
        JsonObject first = arr.get(0).getAsJsonObject();
        assertEquals("DUP-1-2", first.get("id").getAsString());
        assertEquals("PHONE", first.get("criterion").getAsString());
        assertEquals(95, first.get("confidence").getAsInt());
    }

    @Test
    void testGetDuplicatesWithEnvelopeParamReturnsApiResponse() throws Exception {
        CallResult res = invokeServlet("/api/leads/duplicates", "GET", null, "application/json", Map.of("envelope", "true"), true);
        assertEquals(200, res.status);

        JsonObject json = JsonUtil.getGson().fromJson(res.body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertTrue(json.get("data").isJsonArray());
    }

    @Test
    void testPostMergeSuccess() throws Exception {
        String payload = """
            {
                "masterId": 1,
                "duplicateId": 2,
                "retainHistory": true,
                "mergedData": {
                    "id": 1,
                    "fullName": "Nguyen Van A",
                    "phone": "0912345678",
                    "email": "a@example.com"
                }
            }
            """;
        CallResult res = invokeServlet("/api/leads/merge", "POST", payload, "application/json", null, true);
        assertEquals(200, res.status);
        JsonObject json = JsonUtil.getGson().fromJson(res.body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        JsonObject data = json.getAsJsonObject("data");
        assertEquals(1L, data.get("masterId").getAsLong());
        assertEquals(2L, data.get("duplicateId").getAsLong());
    }

    @Test
    void testPostMergeAcceptsPrefixedStringIds() throws Exception {
        String payload = """
            {
                "masterId": "LD-10",
                "duplicateId": "LD-20",
                "retainHistory": true,
                "mergedData": {
                    "id": "LD-10",
                    "fullName": "Le Van C",
                    "phone": "0909090909"
                }
            }
            """;
        CallResult res = invokeServlet("/api/leads/merge", "POST", payload, "application/json", null, true);
        assertEquals(200, res.status);
        JsonObject json = JsonUtil.getGson().fromJson(res.body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        JsonObject data = json.getAsJsonObject("data");
        assertEquals(10L, data.get("masterId").getAsLong());
        assertEquals(20L, data.get("duplicateId").getAsLong());
    }

    @Test
    void testAttachCustomerSuccess() throws Exception {
        String payload = """
            {
                "leadId": 5,
                "customerId": "CUST-100",
                "notes": "Lien ket khach hang"
            }
            """;
        CallResult res = invokeServlet("/api/leads/attach-customer", "POST", payload, "application/json", null, true);
        assertEquals(200, res.status);
        JsonObject json = JsonUtil.getGson().fromJson(res.body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        JsonObject data = json.getAsJsonObject("data");
        assertEquals(5L, data.get("leadId").getAsLong());
        assertEquals(100L, data.get("customerId").getAsLong());
    }

    @Test
    void testErrorHandlingStatusMappings() throws Exception {
        // 404
        CallResult res404 = invokeServlet("/api/leads/merge", "POST", "{\"masterId\":404,\"duplicateId\":2,\"mergedData\":{\"fullName\":\"A\",\"phone\":\"091\"}}", "application/json", null, true);
        assertEquals(404, res404.status);

        // 403
        CallResult res403 = invokeServlet("/api/leads/merge", "POST", "{\"masterId\":403,\"duplicateId\":2,\"mergedData\":{\"fullName\":\"A\",\"phone\":\"091\"}}", "application/json", null, true);
        assertEquals(403, res403.status);

        // 409
        CallResult res409 = invokeServlet("/api/leads/merge", "POST", "{\"masterId\":409,\"duplicateId\":2,\"mergedData\":{\"fullName\":\"A\",\"phone\":\"091\"}}", "application/json", null, true);
        assertEquals(409, res409.status);

        // 400 - missing mergedData
        CallResult res400 = invokeServlet("/api/leads/merge", "POST", "{\"masterId\":1,\"duplicateId\":2}", "application/json", null, true);
        assertEquals(400, res400.status);

        // 400 - invalid Content-Type
        CallResult resType = invokeServlet("/api/leads/merge", "POST", "{}", "text/plain", null, true);
        assertEquals(400, resType.status);
    }
}
