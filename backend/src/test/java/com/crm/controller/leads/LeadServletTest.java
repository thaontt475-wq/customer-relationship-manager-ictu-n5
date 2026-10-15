package com.crm.controller.leads;

import com.crm.dto.leads.LeadSlaResponseDTO;
import com.crm.service.leads.LeadSlaService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class LeadServletTest {

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

    private record Result(int status, JsonObject body) {}

    private Result request(String method, String path, String body, boolean loggedIn) throws Exception {
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
        return new Result(status[0], json);
    }

    @Test
    void testUnauthenticatedReturns401() throws Exception {
        Result res = request("GET", "/my-assigned", null, false);
        assertEquals(401, res.status());
        assertFalse(res.body().get("success").getAsBoolean());
    }

    @Test
    void testGetMyAssignedLeadsReturns200() throws Exception {
        Result res = request("GET", "/my-assigned", null, true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
        assertTrue(res.body().get("data").isJsonArray());
    }

    @Test
    void testAcceptLeadSuccessReturns200() throws Exception {
        Result res = request("POST", "/15/accept", "{}", true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
        assertEquals("CONTACTING", res.body().getAsJsonObject("data").get("status").getAsString());
    }

    @Test
    void testAcceptLeadPrefixCompat() throws Exception {
        Result res = request("POST", "/LD-15/accept", "{}", true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
    }

    @Test
    void testRejectLeadSuccessReturns200() throws Exception {
        String body = "{\"reason\":\"INVALID_PHONE\",\"details\":\"Không liên lạc được\"}";
        Result res = request("POST", "/20/reject", body, true);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
        assertEquals("PENDING_ASSIGNMENT", res.body().getAsJsonObject("data").get("status").getAsString());
    }

    @Test
    void testRejectLeadMissingReasonOrDetailsReturns400() throws Exception {
        // Missing details
        String body1 = "{\"reason\":\"INVALID_PHONE\",\"details\":\"\"}";
        Result res1 = request("POST", "/20/reject", body1, true);
        assertEquals(400, res1.status());

        // Empty body
        Result res2 = request("POST", "/20/reject", "{}", true);
        assertEquals(400, res2.status());
    }

    @Test
    void testLeadNotFoundReturns404() throws Exception {
        Result res = request("POST", "/404/accept", "{}", true);
        assertEquals(404, res.status());
    }

    @Test
    void testForbiddenReturns403() throws Exception {
        Result res = request("POST", "/403/accept", "{}", true);
        assertEquals(403, res.status());
    }

    @Test
    void testConflictLockedReturns409() throws Exception {
        Result res = request("POST", "/409/accept", "{}", true);
        assertEquals(409, res.status());
    }

    @Test
    void testInvalidPathReturns404() throws Exception {
        Result res = request("GET", "/unknown-subpath", null, true);
        assertEquals(404, res.status());
    }

    @Test
    void testHealthEndpointReturns200WithoutAuth() throws Exception {
        Result res = request("GET", "/health", null, false);
        assertEquals(200, res.status());
        assertTrue(res.body().get("success").getAsBoolean());
    }
}
