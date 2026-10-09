package com.crm.controller.customers;

import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.service.customers.CustomerService;
import com.crm.service.customers.CustomerValidation;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerServletTest {
    private Exception failure;
    private boolean deleted;
    private final CustomerService service = new CustomerService() {
        private void fail() throws Exception { if (failure != null) throw failure; }
        @Override public Map<String, Object> create(long user, CustomerWriteRequest body) throws Exception {
            CustomerValidation.normalize(body); fail(); return Map.of("id", 1L, "name", body.getName());
        }
        @Override public Map<String, Object> getById(long user, long id) throws Exception {
            fail(); return id == 1 ? Map.of("id", id) : null;
        }
        @Override public Map<String, Object> update(long user, long id, CustomerWriteRequest body) throws Exception {
            CustomerValidation.normalize(body); fail(); return id == 1 ? Map.of("id", id) : null;
        }
        @Override public void delete(long user, long id) throws Exception {
            fail(); if (id != 1) throw new NoSuchElementException("Missing customer"); deleted = true;
        }
    };
    private record Result(int status, JsonObject body) {}

    private Result request(String method, String path, String body, boolean loggedIn) throws Exception {
        StringWriter output = new StringWriter(); int[] status = {200};
        HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpSession.class},
                (p, m, a) -> m.getName().equals("getAttribute") ? 10L : null);
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getSession" -> loggedIn ? session : null;
                    case "getMethod" -> method;
                    case "getPathInfo" -> path;
                    case "getReader" -> new BufferedReader(new StringReader(body == null ? "" : body));
                    default -> null;
                });
        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class},
                (p, m, a) -> {
                    if (m.getName().equals("setStatus")) status[0] = (int) a[0];
                    return m.getName().equals("getWriter") ? new PrintWriter(output) : null;
                });
        CustomerServlet servlet = new CustomerServlet(service) {
            @Override public void log(String message, Throwable error) { /* No container in this unit test. */ }
        };
        servlet.service(req, resp);
        return new Result(status[0], JsonUtil.getGson().fromJson(output.toString(), JsonObject.class));
    }

    @Test void crudKeepsSuccessCodesAndApiResponseEnvelope() throws Exception {
        var create = request("POST", null, "{\"companyName\":\"ABC\"}", true);
        assertEquals(201, create.status); assertTrue(create.body.get("success").getAsBoolean());
        assertEquals(200, request("GET", "/1", null, true).status);
        assertEquals(200, request("PUT", "/1", "{\"name\":\"ABC\"}", true).status);
        assertEquals(200, request("DELETE", "/1", null, true).status); assertTrue(deleted);
    }

    @Test void missingSessionScopeAndRecordReturnTheirExistingErrorCodes() throws Exception {
        assertEquals(401, request("GET", "/1", null, false).status);
        assertEquals(404, request("GET", "/999", null, true).status);
        assertEquals(404, request("PUT", "/999", "{\"name\":\"ABC\"}", true).status);
        assertEquals(404, request("DELETE", "/999", null, true).status);
        failure = new SecurityException("Outside scope");
        assertEquals(403, request("GET", "/1", null, true).status);
        assertEquals(403, request("PUT", "/1", "{\"name\":\"ABC\"}", true).status);
        assertEquals(403, request("DELETE", "/1", null, true).status);
    }

    @Test void invalidJsonFieldsAndDeleteSubroutesCannotPerformWrites() throws Exception {
        assertEquals(400, request("POST", null, "[", true).status);
        assertEquals(400, request("POST", null, "{}", true).status);
        assertEquals(400, request("PUT", "/0", "{\"name\":\"ABC\"}", true).status);
        assertEquals(400, request("DELETE", "/1/360", null, true).status);
        assertFalse(deleted);
    }

    @Test void sqlFailuresNeverExposeSqlAndDuplicateTaxReturns409() throws Exception {
        failure = new SQLException("secret SQL customers table", "23000", 1062);
        for (String method : List.of("POST", "PUT")) {
            var result = request(method, method.equals("POST") ? null : "/1", "{\"name\":\"ABC\"}", true);
            assertEquals(409, result.status); assertFalse(result.body.toString().contains("secret"));
            assertTrue(result.body.get("message").getAsString().contains("Mã số thuế"));
        }
        failure = new SQLException("secret connection details", "08001");
        var result = request("GET", "/1", null, true);
        assertEquals(500, result.status); assertFalse(result.body.toString().contains("secret"));
    }
}
