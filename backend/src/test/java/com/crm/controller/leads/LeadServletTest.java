package com.crm.controller.leads;

import com.crm.dto.leads.LeadFilterRequest;
import com.crm.dto.leads.LeadSlaResponseDTO;
import com.crm.model.leads.Lead;
import com.crm.model.leads.SavedLeadFilter;
import com.crm.service.leads.*;
import com.crm.service.permissions.*;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadServletTest {

    // =========================================================================
    // S4-09 Search, Filter & Saved Filters Tests
    // =========================================================================

    private LeadFilterRequest capturedFilter;
    private Long capturedUserId;
    private Long detailRequestedId;
    private Exception serviceFailure;

    private Lead mockLead;
    private List<SavedLeadFilter> mockFilters;
    private SavedLeadFilter capturedSavedFilter;

    private LeadService leadService;
    private SavedLeadFilterService savedFilterService;

    @BeforeEach
    void setUp() {
        capturedFilter = null;
        capturedUserId = null;
        detailRequestedId = null;
        serviceFailure = null;
        capturedSavedFilter = null;

        mockLead = new Lead();
        mockLead.setId(42L);
        mockLead.setFullName("Nguyen Van Test");
        mockLead.setCode("LEAD-42");
        mockLead.setStatus("NEW");

        mockFilters = new ArrayList<>();
        mockFilters.add(new SavedLeadFilter("sys_1", 0L, "Tất cả Lead", Map.of(), true, true));

        leadService = new LeadService() {
            @Override
            public Map<String, Object> searchLeads(LeadFilterRequest filter, long currentUserId) throws Exception {
                if (serviceFailure != null) throw serviceFailure;
                capturedFilter = filter;
                capturedUserId = currentUserId;

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("items", List.of(mockLead));
                res.put("totalRecords", 1);
                res.put("page", filter.getPage());
                res.put("size", filter.getSize());
                res.put("totalPages", 1);
                return res;
            }

            @Override
            public Lead getLeadById(long id, long currentUserId) throws Exception {
                if (serviceFailure != null) throw serviceFailure;
                detailRequestedId = id;
                capturedUserId = currentUserId;
                if (id == 42L) return mockLead;
                return null;
            }

            @Override
            public boolean updateLead(Lead lead, long currentUserId) throws Exception {
                if (serviceFailure != null) throw serviceFailure;
                return true;
            }

            @Override
            public boolean deleteLead(long id, long currentUserId) throws Exception {
                if (serviceFailure != null) throw serviceFailure;
                return true;
            }
        };

        savedFilterService = new SavedLeadFilterService() {
            @Override
            public List<SavedLeadFilter> getAllFilters(long currentUserId) {
                capturedUserId = currentUserId;
                return mockFilters;
            }

            @Override
            public SavedLeadFilter saveCustomFilter(SavedLeadFilter filter, long currentUserId) {
                if (serviceFailure != null) throw (RuntimeException) serviceFailure;
                capturedSavedFilter = filter;
                capturedUserId = currentUserId;
                filter.setId(101L);
                return filter;
            }

            @Override
            public boolean updateCustomFilter(SavedLeadFilter filter, long currentUserId) {
                if (serviceFailure != null) throw (RuntimeException) serviceFailure;
                capturedSavedFilter = filter;
                capturedUserId = currentUserId;
                return true;
            }

            @Override
            public boolean deleteCustomFilter(long filterId, long currentUserId) {
                if (serviceFailure != null) throw (RuntimeException) serviceFailure;
                capturedUserId = currentUserId;
                return true;
            }
        };
    }

    private record ServletResult(int status, JsonObject body) {}

    private ServletResult execute(String method, String path, Map<String, String> params, String bodyJson, Long sessionUserId) throws Exception {
        StringWriter output = new StringWriter();
        int[] status = {200};

        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpSession.class},
                (proxy, m, args) -> {
                    if ("getAttribute".equals(m.getName()) && "userId".equals(args[0])) {
                        return sessionUserId;
                    }
                    return null;
                }
        );

        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (proxy, m, args) -> switch (m.getName()) {
                    case "getSession" -> session;
                    case "getMethod" -> method;
                    case "getPathInfo" -> path;
                    case "getParameter" -> params != null ? params.get((String) args[0]) : null;
                    case "getReader" -> new BufferedReader(new StringReader(bodyJson != null ? bodyJson : ""));
                    case "getContentType" -> "application/json";
                    default -> null;
                }
        );

        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletResponse.class},
                (proxy, m, args) -> {
                    if ("setStatus".equals(m.getName())) status[0] = (int) args[0];
                    if ("getWriter".equals(m.getName())) return new PrintWriter(output);
                    return null;
                }
        );

        LeadServlet servlet = new LeadServlet(leadService, savedFilterService);
        switch (method.toUpperCase()) {
            case "GET" -> servlet.doGet(req, resp);
            case "POST" -> servlet.doPost(req, resp);
            case "PUT" -> servlet.doPut(req, resp);
            case "DELETE" -> servlet.doDelete(req, resp);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        JsonObject json = null;
        if (!output.toString().isBlank()) {
            json = JsonUtil.getGson().fromJson(output.toString(), JsonObject.class);
        }
        return new ServletResult(status[0], json);
    }

    @Test
    void testSearchLeadsRouteSuccess() throws Exception {
        var res = execute("GET", null, Map.of(
                "search", "Acme",
                "status", "NEW",
                "tier", "HOT",
                "slaOverdueOnly", "true"
        ), null, 99L);

        assertEquals(200, res.status());
        assertNotNull(res.body());
        assertTrue(res.body().get("success").getAsBoolean());
        assertEquals(99L, capturedUserId);
        assertEquals("Acme", capturedFilter.getKeyword());
        assertEquals("NEW", capturedFilter.getStatus());
        assertEquals("HOT", capturedFilter.getTier());
        assertTrue(capturedFilter.isSlaOverdueOnly());
    }

    @Test
    void testLeadDetailRouteRegexMatch() throws Exception {
        var res = execute("GET", "/42", Map.of(), null, 99L);
        assertEquals(200, res.status());
        assertEquals(42L, detailRequestedId);
        assertTrue(res.body().get("success").getAsBoolean());

        var notFound = execute("GET", "/9999", Map.of(), null, 99L);
        assertEquals(404, notFound.status());
    }

    @Test
    void testSavedFiltersListRoute() throws Exception {
        var res = execute("GET", "/saved-filters", Map.of(), null, 99L);
        assertEquals(200, res.status());
        assertEquals(99L, capturedUserId);
        assertTrue(res.body().get("success").getAsBoolean());
    }

    @Test
    void testCreateSavedFilter() throws Exception {
        String body = """
            {
                "name": "Khách hàng VIP Nóng",
                "filter": {"tier": "HOT", "status": "IN_PROGRESS"},
                "setAsDefault": true
            }
            """;
        var res = execute("POST", "/saved-filters", Map.of(), body, 99L);
        assertEquals(201, res.status());
        assertNotNull(capturedSavedFilter);
        assertEquals("Khách hàng VIP Nóng", capturedSavedFilter.getName());
        assertEquals(99L, capturedUserId);
    }

    @Test
    void testUpdateSavedFilter() throws Exception {
        String body = """
            {
                "name": "Bộ lọc cập nhật",
                "filter": {"tier": "WARM"},
                "isDefault": false
            }
            """;
        var res = execute("PUT", "/saved-filters/101", Map.of(), body, 99L);
        assertEquals(200, res.status());
        assertNotNull(capturedSavedFilter);
        assertEquals("Bộ lọc cập nhật", capturedSavedFilter.getName());
        assertEquals(101L, capturedSavedFilter.getId());
    }

    @Test
    void testDeleteSavedFilter() throws Exception {
        var res = execute("DELETE", "/saved-filters/101", Map.of(), null, 99L);
        assertEquals(200, res.status());
        assertEquals(99L, capturedUserId);
    }

    @Test
    void testErrorHandlingStatusMappings() throws Exception {
        serviceFailure = new NoSuchElementException("Không tìm thấy dữ liệu");
        var res404 = execute("GET", "/42", Map.of(), null, 99L);
        assertEquals(404, res404.status());

        serviceFailure = new IllegalStateException("Lead đã bị khóa");
        var res409 = execute("GET", "/42", Map.of(), null, 99L);
        assertEquals(409, res409.status());

        serviceFailure = new SecurityException("Không có quyền truy cập");
        var res403 = execute("GET", "/42", Map.of(), null, 99L);
        assertEquals(403, res403.status());
    }

    // =========================================================================
    // S4-07 SLA Mock & Tests
    // =========================================================================

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
                (p, m, a) -> m.getName().equals("getAttribute") && "userId".equals(a[0]) ? 10L : null
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

    // =========================================================================
    // Develop S4-01 / S4-02 / S4-08 Tests
    // =========================================================================

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

    @Test
    void leadConversionEndpointsRouteCorrectly() throws Exception {
        var res1 = call("GET", "/1/check-convert-duplicates", null, null, false, true, null);
        assertEquals(401, res1.status);

        var res2 = call("POST", "/1/convert", "application/json", "{}", false, true, null);
        assertEquals(401, res2.status);

        var res3 = call("PUT", "/1/convert", "application/json", "{}", true, true, null);
        assertEquals(405, res3.status);

        var res4 = call("POST", "/1/check-convert-duplicates", "application/json", "{}", true, true, null);
        assertEquals(405, res4.status);
    }
}
