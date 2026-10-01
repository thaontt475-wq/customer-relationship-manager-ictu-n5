package com.crm.controller.organization;

import com.crm.model.Organization;
import com.crm.service.organization.OrganizationService;
import com.crm.service.organization.OrganizationService.ErrorCode;
import com.crm.service.organization.OrganizationService.OrganizationException;
import com.crm.service.organization.OrganizationService.UnitInput;
import com.crm.util.SessionKey;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServletTest {

    @Mock
    private OrganizationService organizationService;

    private OrganizationServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new OrganizationServlet(organizationService);
    }

    @Test
    void unauthenticatedGetReturns401() throws Exception {
        ResponseCapture response = new ResponseCapture();

        servlet.doGet(request(null, null, null), response.proxy());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status);
        JsonObject envelope = response.json();
        assertFalse(envelope.get("success").getAsBoolean());
        assertTrue(envelope.get("data").isJsonNull());
        verify(organizationService, never()).getUnits();
    }

    @Test
    void authenticatedGetReturnsSuccessEnvelopeWithFlatUnits() throws Exception {
        Organization parent = organization(
                1L, "Head Office", null, 101L, "Authoritative Parent", "Director", "NATIONAL", true);
        Organization child = organization(
                2L, "North Branch", 1L, 102L, "Authoritative Child", "Manager", "NORTH", true);
        when(organizationService.getUnits()).thenReturn(List.of(parent, child));
        ResponseCapture response = new ResponseCapture();

        servlet.doGet(request(authenticatedSession(), null, null), response.proxy());

        assertEquals(HttpServletResponse.SC_OK, response.status);
        JsonObject envelope = response.json();
        assertTrue(envelope.get("success").getAsBoolean());
        JsonArray items = envelope.getAsJsonObject("data").getAsJsonArray("items");
        assertEquals(2, items.size());
        assertEquals(1L, items.get(0).getAsJsonObject().get("id").getAsLong());
        assertTrue(items.get(0).getAsJsonObject().get("parentId").isJsonNull());
        assertEquals(2L, items.get(1).getAsJsonObject().get("id").getAsLong());
        assertEquals(1L, items.get(1).getAsJsonObject().get("parentId").getAsLong());
    }

    @Test
    void postAcceptsFrontendPayloadAndReturnsAuthoritativeCreatedUnit() throws Exception {
        String payload = """
                {
                  "name": "Client supplied name",
                  "parentId": 7,
                  "managerId": 42,
                  "managerName": "Client supplied manager",
                  "managerRole": "Client supplied role",
                  "region": "NORTH",
                  "active": true
                }
                """;
        Organization authoritative = organization(
                99L, "Canonical unit", 7L, 42L, "Database Manager", "Regional Director", "NORTH", true);
        when(organizationService.createUnit(org.mockito.ArgumentMatchers.any(UnitInput.class)))
                .thenReturn(authoritative);
        ResponseCapture response = new ResponseCapture();

        servlet.doPost(request(adminSession(), null, payload), response.proxy());

        ArgumentCaptor<UnitInput> inputCaptor = ArgumentCaptor.forClass(UnitInput.class);
        verify(organizationService).createUnit(inputCaptor.capture());
        UnitInput input = inputCaptor.getValue();
        assertEquals("Client supplied name", input.name());
        assertEquals(7L, input.parentId());
        assertEquals(42L, input.managerId());
        assertEquals("NORTH", input.region());
        assertEquals(Boolean.TRUE, input.active());

        assertEquals(HttpServletResponse.SC_CREATED, response.status);
        JsonObject envelope = response.json();
        assertTrue(envelope.get("success").getAsBoolean());
        JsonObject data = envelope.getAsJsonObject("data");
        assertEquals(99L, data.get("id").getAsLong());
        assertEquals("Canonical unit", data.get("name").getAsString());
        assertEquals("Database Manager", data.get("managerName").getAsString());
        assertEquals("Regional Director", data.get("managerRole").getAsString());
    }

    @Test
    void putCycleReturns409AndStructuredBusinessError() throws Exception {
        String payload = """
                {
                  "name": "North Branch",
                  "parentId": 12,
                  "managerId": 42,
                  "managerName": "Ignored display name",
                  "managerRole": "Ignored display role",
                  "region": "NORTH",
                  "active": true
                }
                """;
        when(organizationService.updateUnit(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any(UnitInput.class)))
                .thenThrow(new OrganizationException(ErrorCode.CYCLE, "Cập nhật sẽ tạo chu trình"));
        ResponseCapture response = new ResponseCapture();

        servlet.doPut(request(adminSession(), "/10", payload), response.proxy());

        assertEquals(HttpServletResponse.SC_CONFLICT, response.status);
        JsonObject envelope = response.json();
        assertFalse(envelope.get("success").getAsBoolean());
        assertEquals("CYCLE", envelope.getAsJsonObject("data").get("code").getAsString());
    }

    @Test
    void ordinaryAuthenticatedUserCannotCreateOrUpdateUnits() throws Exception {
        String payload = """
                {
                  "name": "Forbidden unit",
                  "region": "NORTH",
                  "active": true
                }
                """;
        HttpSession session = authenticatedSession();
        ResponseCapture postResponse = new ResponseCapture();
        ResponseCapture putResponse = new ResponseCapture();

        servlet.doPost(request(session, null, payload), postResponse.proxy());
        servlet.doPut(request(session, "/10", payload), putResponse.proxy());

        assertForbiddenEnvelope(postResponse);
        assertForbiddenEnvelope(putResponse);
        verify(organizationService, never())
                .createUnit(org.mockito.ArgumentMatchers.any(UnitInput.class));
        verify(organizationService, never()).updateUnit(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(UnitInput.class));
    }

    private void assertForbiddenEnvelope(ResponseCapture response) {
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status);
        JsonObject envelope = response.json();
        assertFalse(envelope.get("success").getAsBoolean());
        assertEquals("Không có quyền quản lý cơ cấu tổ chức",
                envelope.get("message").getAsString());
        assertTrue(envelope.get("data").isJsonNull());
    }

    private Organization organization(
            long id,
            String name,
            Long parentId,
            Long managerId,
            String managerName,
            String managerRole,
            String region,
            boolean active) {
        return new Organization(
                id, name, parentId, managerId, managerName, managerRole, region,
                active, 0, List.of());
    }

    private HttpSession authenticatedSession() {
        Map<String, Object> attributes = Map.of(SessionKey.CURRENT_USER, "authenticated-user");
        return session(attributes);
    }

    private HttpSession adminSession() {
        Map<String, Object> attributes = Map.of(
                SessionKey.CURRENT_USER, "authenticated-user",
                SessionKey.ROLES, List.of("Admin"));
        return session(attributes);
    }

    private HttpSession session(Map<String, Object> attributes) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> attributes.get((String) args[0]);
                    default -> defaultValue(method.getReturnType());
                });
    }

    private HttpServletRequest request(HttpSession session, String pathInfo, String body) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "getPathInfo" -> pathInfo;
                    case "getReader" -> new BufferedReader(new StringReader(body == null ? "" : body));
                    case "setCharacterEncoding" -> null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }

    private static final class ResponseCapture {
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private int status;

        HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setStatus" -> {
                            status = (int) args[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        case "setContentType", "setCharacterEncoding" -> null;
                        default -> defaultValue(method.getReturnType());
                    });
        }

        JsonObject json() {
            writer.flush();
            assertFalse(body.toString().isBlank());
            return JsonParser.parseString(body.toString()).getAsJsonObject();
        }
    }
}
