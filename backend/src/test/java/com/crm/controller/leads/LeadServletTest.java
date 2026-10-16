package com.crm.controller.leads;

import com.crm.dto.leads.LeadSlaResponseDTO;
import com.crm.service.leads.*;
import com.crm.service.permissions.*;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadServletTest {

    // --- S4-07 SLA Mock & Tests ---

    private Exception failure;
    private final LeadSlaService mockService = new LeadSlaService(null, null, null) {
        private void fail() throws Exception {
            if (failure != null) throw failure;
        }

        @Override
        public List<LeadSlaResponseDTO> getMyAssignedLeads(long currentUserId) throws java.sql.SQLException {
            if (failure instanceof java.sql.SQLException se) throw se;
            if (failure != null) throw new RuntimeException(failure);
            LeadSlaResponseDTO dto = new LeadSlaResponseDTO();
            return List.of(dto);
        }

        @Override
        public LeadSlaResponseDTO acceptLead(long currentUserId, long leadId) throws Exception {
            fail();
            if (leadId == 404L) throw new NoSuchElementException("Không tìm thấy Lead");
            if (leadId == 403L) throw new SecurityException("Không có quyền");
            if (leadId == 409L) throw new IllegalStateException("Lead đã bị khóa");
            LeadSlaResponseDTO dto = new LeadSlaResponseDTO();
            dto.setStatus("CONTACTING");
            dto.setSlaResult("ON_TIME");
            return dto;
        }

        @Override
        public LeadSlaResponseDTO rejectLead(long currentUserId, long leadId, String reason, String details) throws Exception {
            fail();
            if (leadId == 404L) throw new NoSuchElementException("Không tìm thấy Lead");
            if (leadId == 403L) throw new SecurityException("Không có quyền");
            if (leadId == 409L) throw new IllegalStateException("Lead đã bị khóa");
            LeadSlaResponseDTO dto = new LeadSlaResponseDTO();
            dto.setStatus("PENDING_ASSIGNMENT");
            dto.setRejectReason(reason);
            dto.setRejectDetails(details);
            return dto;
        }
    };

    private record SlaResult(int status, JsonObject body) {}

    private SlaResult request(String method, String path, String body, boolean loggedIn) throws Exception {
        StringWriter output = new StringWriter();
        int[] status = {200};
        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpSession.class},
                (p, m, a) -> m.getName().equals("getAttribute") ? 10L : null
        );
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getSession" -> loggedIn ? session : null;
                    case "getMethod" -> method;
                    case "getPathInfo" -> path;
                    case "getContentType" -> "application/json";
                    case "getReader" -> new BufferedReader(new StringReader(body == null ? "" : body));
                    default -> null;
                }
        );
        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletResponse.class},
                (p, m, a) -> {
                    if (m.getName().equals("setStatus")) status[0] = (int) a[0];
                    return m.getName().equals("getWriter") ? new PrintWriter(output) : null;
                }
        );

        LeadServlet servlet = new LeadServlet(mockService) {
            @Override
            public void log(String message, Throwable error) {
                // Test log stub
            }
        };

        servlet.service(req, resp);
        JsonObject json = JsonUtil.getGson().fromJson(output.toString(), JsonObject.class);
        return new SlaResult(status[0], json);
    }

    @Test
    void testUnauthenticatedReturns401() throws Exception {
        SlaResult res = request("GET", "/my-assigned", null, false);
        assertEquals(401, res.status());
        assertFalse(res.body().get("success").getAsBoolean());
    }

    @Test
    void testGetMyAssignedLeadsReturns200() throws Exception {
        SlaResult res = request("GET", "/my-assigned", null, true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
        assertTrue(res.body().get("data").isJsonArray());
    }

    @Test
    void testAcceptLeadSuccessReturns200() throws Exception {
        SlaResult res = request("POST", "/15/accept", "{}", true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
        assertEquals("CONTACTING", res.body().getAsJsonObject("data").get("status").getAsString());
    }

    @Test
    void testAcceptLeadPrefixCompat() throws Exception {
        SlaResult res = request("POST", "/LD-15/accept", "{}", true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
    }

    @Test
    void testRejectLeadSuccessReturns200() throws Exception {
        String body = "{\"reason\":\"INVALID_PHONE\",\"details\":\"Không liên lạc được\"}";
        SlaResult res = request("POST", "/20/reject", body, true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
        assertEquals("PENDING_ASSIGNMENT", res.body().getAsJsonObject("data").get("status").getAsString());
    }

    @Test
    void testRejectLeadMissingReasonOrDetailsReturns400() throws Exception {
        String body1 = "{\"reason\":\"INVALID_PHONE\",\"details\":\"\"}";
        SlaResult res1 = request("POST", "/20/reject", body1, true);
        assertEquals(400, res1.status());

        String body2 = "{\"reason\":\"INVALID_PHONE\",\"details\":null}";
        SlaResult res2 = request("POST", "/20/reject", body2, true);
        assertEquals(400, res2.status());
    }

    @Test
    void testLeadNotFoundReturns404() throws Exception {
        SlaResult res = request("POST", "/404/accept", "{}", true);
        assertEquals(404, res.status());
    }

    @Test
    void testForbiddenReturns403() throws Exception {
        SlaResult res = request("POST", "/403/accept", "{}", true);
        assertEquals(403, res.status());
    }

    @Test
    void testConflictLockedReturns409() throws Exception {
        SlaResult res = request("POST", "/409/accept", "{}", true);
        assertEquals(409, res.status());
    }

    @Test
    void testInvalidPathReturns404() throws Exception {
        SlaResult res = request("GET", "/unknown-subpath", null, true);
        assertEquals(404, res.status());
    }

    @Test
    void testHealthEndpointReturns200WithoutAuth() throws Exception {
        SlaResult res = request("GET", "/health", null, false);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
    }

    // --- Develop S4-01 / S4-02 Tests ---

    private record Result(int status, String body, String allow) {}

    private Result call(String method, String path, String type, String body, boolean session, boolean allowed, Part part) throws Exception {
        LeadService leads = new LeadService() {
            @Override
            public DataScopeContext authorizeCreate(long user) {
                if (!allowed) throw new SecurityException("Forbidden");
                return new DataScopeContext(user, "lead", ScopeType.SELF, null, Set.of(user));
            }
        };
        LeadImportService imports = new LeadImportService() {
            @Override
            public void authorize(long user) throws Exception {
                leads.authorizeCreate(user);
            }
        };
        HttpSession auth = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpSession.class},
                (p, m, a) -> m.getName().equals("getAttribute") && a[0].equals("userId") ? 1L : null);
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
            case "getSession" -> session ? auth : null;
            case "getMethod" -> method;
            case "getPathInfo" -> path;
            case "getContentType" -> type;
            case "getReader" -> new BufferedReader(new StringReader(body == null ? "" : body));
            case "getPart" -> part;
            default -> null;
        });
        int[] status = {200};
        String[] allow = {null};
        StringWriter writer = new StringWriter();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class}, (p, m, a) -> {
            if (m.getName().equals("setStatus")) status[0] = (int) a[0];
            if (m.getName().equals("setHeader") && a[0].equals("Allow")) allow[0] = (String) a[1];
            return m.getName().equals("getWriter") ? new PrintWriter(writer) : null;
        });
        new LeadServlet(leads, imports).service(request, response);
        return new Result(status[0], writer.toString(), allow[0]);
    }

    @Test
    void authAndPermissionFailClosedBeforeUploadParsing() throws Exception {
        assertEquals(401, call("POST", null, "application/json", "{}", false, true, null).status);
        assertEquals(403, call("POST", null, "application/json", "{}", true, false, null).status);
        assertEquals(403, call("POST", "/import/preview", "multipart/form-data", null, true, false, null).status);
    }

    @Test
    void wrongMethodsAndUnknownPathsUseJsonAllow() throws Exception {
        var result = call("DELETE", "/import/template", null, null, true, true, null);
        assertEquals(405, result.status);
        assertEquals("GET", result.allow);
        assertEquals(405, call("GET", "/import/preview", null, null, true, true, null).status);
        assertEquals(404, call("GET", "/unknown", null, null, true, true, null).status);
        assertFalse(JsonUtil.getGson().fromJson(result.body, com.google.gson.JsonObject.class).get("success").getAsBoolean());
    }

    @Test
    void malformedJsonContentTypeAndConfirmationRowsRejected() throws Exception {
        assertEquals(400, call("POST", null, "text/plain", "{}", true, true, null).status);
        assertEquals(400, call("POST", null, "application/json", "[", true, true, null).status);
        assertEquals(400, call("POST", null, "application/json", "{\"ownerUserId\":2}", true, true, null).status);
        assertEquals(400, call("POST", "/import/confirm", "application/json", "{\"batchToken\":false}", true, true, null).status);
        assertEquals(400, call("POST", "/import/confirm", "application/json", "{\"batchToken\":\"x\",\"rows\":[]}", true, true, null).status);
    }

    @Test
    void oversizedJsonAndFileReturn413AndPartIsCleaned() throws Exception {
        assertEquals(413, call("POST", null, "application/json", " ".repeat(16385), true, true, null).status);
        assertEquals(413, call("POST", "/import/confirm", "application/json", " ".repeat(8193), true, true, null).status);
        boolean[] deleted = {false};
        Part part = (Part) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Part.class}, (p, m, a) -> {
            if (m.getName().equals("delete")) {
                deleted[0] = true;
                return null;
            }
            return switch (m.getName()) {
                case "getSubmittedFileName" -> "oversize.xlsx";
                case "getSize" -> 5242881L;
                default -> null;
            };
        });
        assertEquals(413, call("POST", "/import/preview", "multipart/form-data", null, true, true, part).status);
        assertTrue(deleted[0]);
    }
}
