package com.crm.controller.customers;

import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.service.customers.CustomerService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerSearchServletTest {
    private CustomerSearchFilter received;
    private int receivedPage, receivedSize;
    private Exception failure;
    private final CustomerService service = new CustomerService() {
        @Override public Map<String, Object> search(long user, CustomerSearchFilter filter, int page, int size) throws Exception {
            CustomerSearchFilter.pagination(page, size);
            if (failure != null) throw failure;
            received = filter; receivedPage = page; receivedSize = size;
            return Map.of("items", List.of(), "page", page, "size", size, "totalItems", 0, "totalPages", 0, "scope", "SELF");
        }
    };
    private record Result(int status, JsonObject body) {}
    private Result request(String path, Map<String, String> params) throws Exception {
        StringWriter output = new StringWriter(); int[] status = {200};
        HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpSession.class},
                (p, m, a) -> m.getName().equals("getAttribute") ? 10L : null);
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getSession" -> session;
                    case "getMethod" -> "GET";
                    case "getPathInfo" -> path;
                    case "getParameter" -> params.get((String) a[0]);
                    default -> null;
                });
        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class},
                (p, m, a) -> {
                    if (m.getName().equals("setStatus")) status[0] = (int) a[0];
                    return m.getName().equals("getWriter") ? new PrintWriter(output) : null;
                });
        CustomerServlet servlet = new CustomerServlet(service) {
            @Override public void log(String message, Throwable error) { }
        };
        servlet.doGet(req, resp);
        return new Result(status[0], JsonUtil.getGson().fromJson(output.toString(), JsonObject.class));
    }

    @Test void contractSearchRouteAndExistingListKeepTheirEnvelope() throws Exception {
        var result = request("/search", Map.of("keyword", "Company", "status", "CHINH_THUC", "industry", "IT", "ownerId", "10", "page", "2", "size", "5"));
        assertEquals(200, result.status); assertTrue(result.body.get("success").getAsBoolean());
        assertEquals("Company", received.search()); assertEquals("IT", received.industry()); assertEquals(10L, received.ownerId());
        assertEquals(2, receivedPage); assertEquals(5, receivedSize);
        assertTrue(result.body.getAsJsonObject("data").has("totalItems"));
        assertEquals(200, request(null, Map.of("search", "legacy", "industryId", "1", "companySizeId", "11", "region", "MB")).status);
        assertEquals("legacy", received.search()); assertEquals(1L, received.industryId());
        assertEquals(200, request("/search/", Map.of("industry", "1")).status);
        assertEquals(1L, received.industryId());
    }

    @Test void emptyParamsUseDefaultsAndClientScopeCannotSelectPermissions() throws Exception {
        assertEquals(200, request("/search", Map.of("keyword", " ", "ownerId", "", "page", "", "size", " ", "scope", "ALL")).status);
        assertNull(received.search()); assertNull(received.ownerId()); assertEquals(1, receivedPage); assertEquals(20, receivedSize);
    }

    @Test void malformedIdsPaginationStatusesAndRegionsReturn400() throws Exception {
        for (var params : List.of(Map.of("page", "0"), Map.of("size", "101"), Map.of("page", "abc"),
                Map.of("page", "999999999999999"), Map.of("ownerId", "1 OR 1=1"), Map.of("industryId", "1.5"),
                Map.of("industry", "0"), Map.of("industry", "1", "industryId", "2"),
                Map.of("status", "UNKNOWN"), Map.of("region", "x".repeat(21)))) {
            assertEquals(400, request("/search", params).status, params.toString());
        }
    }

    @Test void revokedPermissionAndDatabaseErrorsAreHandledWithoutSqlDisclosure() throws Exception {
        failure = new SecurityException("Missing customer.read");
        assertEquals(403, request("/search", Map.of()).status);
        failure = new SQLException("secret SQL connection details");
        var result = request("/search", Map.of());
        assertEquals(500, result.status); assertFalse(result.body.toString().contains("secret"));
    }
}
