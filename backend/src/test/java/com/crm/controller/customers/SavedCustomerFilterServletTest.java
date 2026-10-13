package com.crm.controller.customers;

import com.crm.dao.customers.SavedCustomerFilterDAO.SavedFilter;
import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.service.customers.SavedCustomerFilterService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SavedCustomerFilterServletTest {
    private final Map<Long, SavedFilter> filters = new HashMap<>();
    private long creator;
    private final SavedCustomerFilterService service = new SavedCustomerFilterService() {
        @Override public SavedFilter create(long user, String name, CustomerSearchFilter filter) {
            if (name == null || name.isBlank() || name.length() > 100) throw new IllegalArgumentException("Invalid name");
            creator = user; var result = new SavedFilter(1, name, filter, "2026-10-09T16:00:00"); filters.put(1L, result); return result;
        }
        @Override public List<SavedFilter> list(long user, int page, int size) {
            CustomerSearchFilter.pagination(page, size); return creator == user ? new ArrayList<>(filters.values()) : List.of();
        }
        @Override public SavedFilter get(long user, long id) {
            if (user != creator || !filters.containsKey(id)) throw new NoSuchElementException("Missing filter");
            return filters.get(id);
        }
        @Override public void delete(long user, long id) { get(user, id); filters.remove(id); }
    };
    private record Result(int status, JsonObject body) {}
    private Result request(String method, String path, long user, String body, String entity) throws Exception {
        var output = new StringWriter(); int[] status = {200};
        HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpSession.class},
                (p,m,a) -> m.getName().equals("getAttribute") ? user : null);
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class},
                (p,m,a) -> switch(m.getName()) {
                    case "getSession" -> user == 0 ? null : session;
                    case "getMethod" -> method;
                    case "getPathInfo" -> path;
                    case "getParameter" -> a[0].equals("entity") ? entity : null;
                    case "getReader" -> new BufferedReader(new StringReader(body == null ? "" : body));
                    default -> null;
                });
        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class},
                (p,m,a) -> { if(m.getName().equals("setStatus")) status[0]=(int)a[0]; return m.getName().equals("getWriter") ? new PrintWriter(output) : null; });
        new SavedCustomerFilterServlet(service) { @Override public void log(String text, Throwable error) {} }.service(req,resp);
        return new Result(status[0],JsonUtil.getGson().fromJson(output.toString(),JsonObject.class));
    }
    private String valid() { return "{\"entity\":\"CUSTOMER\",\"userId\":999,\"name\":\"Hanoi\",\"criteria\":{\"keyword\":\"\",\"status\":\"TIEM_NANG\",\"industry\":null,\"size\":null,\"region\":\"Hà Nội\",\"ownerId\":null}}"; }

    @Test void createsListsGetsAndDeletesUsingSessionOwnerAndProposedCriteriaShape() throws Exception {
        var created=request("POST",null,10,valid(),null); assertEquals(201,created.status); assertEquals(10,creator);
        var criteria=created.body.getAsJsonObject("data").getAsJsonObject("criteria");
        assertEquals("Hà Nội",criteria.get("region").getAsString()); assertTrue(criteria.has("size")); assertTrue(criteria.get("size").isJsonNull());
        assertEquals(1,request("GET",null,10,null,"CUSTOMER").body.getAsJsonArray("data").size());
        assertEquals(200,request("GET","/1",10,null,null).status);
        assertEquals(200,request("DELETE","/1",10,null,null).status);
        assertEquals(404,request("GET","/1",10,null,null).status);
    }
    @Test void otherUsersCannotReadOrDeleteAndMissingSessionReturns401() throws Exception {
        request("POST",null,10,valid(),null);
        assertEquals(0,request("GET",null,20,null,"CUSTOMER").body.getAsJsonArray("data").size());
        assertEquals(404,request("GET","/1",20,null,null).status);
        assertEquals(404,request("DELETE","/1",20,null,null).status);
        assertEquals(401,request("GET",null,0,null,"CUSTOMER").status);
        assertEquals(200,request("GET","/1",10,null,null).status);
    }
    @Test void validatesEntityJsonCriteriaAndMethods() throws Exception {
        assertEquals(400,request("GET",null,10,null,"LEAD").status);
        for(String json:List.of("[", "{}", valid().replace("TIEM_NANG","UNKNOWN"), valid().replace("\"ownerId\":null","\"ownerId\":1.5"), valid().replace("\"keyword\":\"\"","\"scope\":\"ALL\"")))
            assertEquals(400,request("POST",null,10,json,null).status,json);
        assertEquals(405,request("PUT","/1",10,null,null).status);
        assertEquals(405,request("POST","/1",10,valid(),null).status);
    }
}
