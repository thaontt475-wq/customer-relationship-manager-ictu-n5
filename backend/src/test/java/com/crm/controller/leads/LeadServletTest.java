package com.crm.controller.leads;

import com.crm.dto.leads.LeadFilterRequest;
import com.crm.model.leads.Lead;
import com.crm.model.leads.SavedLeadFilter;
import com.crm.service.leads.LeadService;
import com.crm.service.leads.SavedLeadFilterService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LeadServletTest {

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
        SavedLeadFilter f1 = new SavedLeadFilter();
        f1.setId("view_all");
        f1.setName("Tất cả Lead");
        f1.setSystem(true);
        mockFilters.add(f1);

        leadService = new LeadService() {
            @Override
            public Map<String, Object> searchLeads(LeadFilterRequest filter, long currentUserId) throws Exception {
                if (serviceFailure != null) throw serviceFailure;
                capturedFilter = filter;
                capturedUserId = currentUserId;
                return Map.of("items", List.of(mockLead), "totalItems", 1, "page", 1, "size", 10);
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
}
