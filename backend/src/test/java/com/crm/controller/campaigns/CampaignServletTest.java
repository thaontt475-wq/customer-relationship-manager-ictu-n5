package com.crm.controller.campaigns;

import com.crm.dto.campaigns.CampaignLeadLinkRequest;
import com.crm.dto.campaigns.CampaignWriteRequest;
import com.crm.model.campaigns.Campaign;
import com.crm.model.campaigns.CampaignLead;
import com.crm.model.campaigns.Lead;
import com.crm.service.campaigns.CampaignService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CampaignServletTest {

    private CampaignService mockService;
    private CampaignServlet servlet;

    private record Result(int status, JsonObject body) {}

    @BeforeEach
    void setUp() {
        mockService = new CampaignService() {
            @Override
            public Map<String, Object> search(String keyword, String status, int page, int size) {
                Map<String, Object> res = new HashMap<>();
                res.put("items", List.of(createDummyCampaign(1L, "CAMP-01", "Chiến dịch 1")));
                res.put("pagination", Map.of("page", page, "size", size, "totalItems", 1, "totalPages", 1));
                return res;
            }

            @Override
            public Campaign getById(long id) {
                if (id == 999L) throw new NoSuchElementException("Không tìm thấy chiến dịch");
                return createDummyCampaign(id, "CAMP-" + id, "Chiến dịch " + id);
            }

            @Override
            public long create(long currentUserId, CampaignWriteRequest req) {
                return 42L;
            }

            @Override
            public void update(long id, CampaignWriteRequest req) {
                if (id == 999L) throw new NoSuchElementException("Không tìm thấy chiến dịch");
            }

            @Override
            public void delete(long id) {
                if (id == 999L) throw new NoSuchElementException("Không tìm thấy chiến dịch");
            }

            @Override
            public List<CampaignLead> getLeads(long campaignId) {
                CampaignLead cl = new CampaignLead();
                cl.setCampaignId(campaignId);
                cl.setLeadId(10L);
                cl.setLeadName("Lead Mẫu");
                cl.setStatus("SENT");
                return List.of(cl);
            }

            @Override
            public List<Lead> getAvailableLeads(long campaignId) {
                Lead l = new Lead();
                l.setId(20L);
                l.setFullName("Lead Khả Dụng");
                return List.of(l);
            }

            @Override
            public int linkLeads(long campaignId, CampaignLeadLinkRequest req) {
                return req.getLeadIds().size();
            }

            @Override
            public void updateLeadStatus(long campaignId, long leadId, String status, String notes) {
            }

            @Override
            public void unlinkLead(long campaignId, long leadId) {
            }
        };

        servlet = new CampaignServlet(mockService);
    }

    private Campaign createDummyCampaign(long id, String code, String name) {
        Campaign c = new Campaign();
        c.setId(id);
        c.setCode(code);
        c.setName(name);
        c.setStatus("PLANNING");
        c.setType("EMAIL");
        return c;
    }

    private Result request(String method, String path, String body, boolean loggedIn) throws Exception {
        StringWriter output = new StringWriter();
        int[] status = {200};

        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpSession.class},
                (p, m, a) -> {
                    if ("getAttribute".equals(m.getName()) && "userId".equals(a[0])) return 100L;
                    return null;
                }
        );

        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getSession" -> loggedIn ? session : null;
                    case "getMethod" -> method;
                    case "getPathInfo" -> path;
                    case "getReader" -> new BufferedReader(new StringReader(body == null ? "" : body));
                    case "getParameter" -> null;
                    default -> null;
                }
        );

        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletResponse.class},
                (p, m, a) -> {
                    if ("setStatus".equals(m.getName())) status[0] = (int) a[0];
                    return "getWriter".equals(m.getName()) ? new PrintWriter(output) : null;
                }
        );

        servlet.service(req, resp);
        JsonObject json = output.toString().isBlank() ? new JsonObject()
                : JsonUtil.getGson().fromJson(output.toString(), JsonObject.class);
        return new Result(status[0], json);
    }

    @Test
    void rejectsUnauthenticatedRequestsWith401() throws Exception {
        Result res = request("GET", null, null, false);
        assertEquals(401, res.status);
        assertFalse(res.body.get("success").getAsBoolean());
    }

    @Test
    void getCampaignListSucceeds() throws Exception {
        Result res = request("GET", null, null, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
        assertNotNull(res.body.getAsJsonObject("data").getAsJsonArray("items"));
    }

    @Test
    void getCampaignByIdSucceeds() throws Exception {
        Result res = request("GET", "/1", null, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
        assertEquals(1L, res.body.getAsJsonObject("data").get("id").getAsLong());
    }

    @Test
    void getCampaignLeadsSucceeds() throws Exception {
        Result res = request("GET", "/1/leads", null, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
        assertEquals(1, res.body.getAsJsonArray("data").size());
    }

    @Test
    void getAvailableLeadsSucceeds() throws Exception {
        Result res = request("GET", "/1/available-leads", null, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
        assertEquals(1, res.body.getAsJsonArray("data").size());
    }

    @Test
    void createCampaignSucceedsWith201() throws Exception {
        String json = """
            {
                "code": "CAMP-NEW",
                "name": "Chiến dịch Tết 2027",
                "type": "EMAIL",
                "status": "PLANNING"
            }
        """;
        Result res = request("POST", null, json, true);
        assertEquals(201, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
    }

    @Test
    void linkLeadsSucceedsWith201() throws Exception {
        String json = """
            {
                "leadIds": [1, 2],
                "status": "SENT"
            }
        """;
        Result res = request("POST", "/1/leads", json, true);
        assertEquals(201, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
        assertEquals(2, res.body.getAsJsonObject("data").get("addedCount").getAsInt());
    }

    @Test
    void updateCampaignSucceeds() throws Exception {
        String json = """
            {
                "code": "CAMP-UPD",
                "name": "Chiến dịch cập nhật",
                "type": "ADS",
                "status": "ACTIVE"
            }
        """;
        Result res = request("PUT", "/1", json, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
    }

    @Test
    void updateLeadStatusSucceeds() throws Exception {
        String json = "{\"status\": \"RESPONDED\"}";
        Result res = request("PUT", "/1/leads/10", json, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
    }

    @Test
    void deleteCampaignSucceeds() throws Exception {
        Result res = request("DELETE", "/1", null, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
    }

    @Test
    void unlinkLeadSucceeds() throws Exception {
        Result res = request("DELETE", "/1/leads/10", null, true);
        assertEquals(200, res.status);
        assertTrue(res.body.get("success").getAsBoolean());
    }

    @Test
    void notFoundReturns404() throws Exception {
        Result res = request("GET", "/999", null, true);
        assertEquals(404, res.status);
        assertFalse(res.body.get("success").getAsBoolean());
    }
}
