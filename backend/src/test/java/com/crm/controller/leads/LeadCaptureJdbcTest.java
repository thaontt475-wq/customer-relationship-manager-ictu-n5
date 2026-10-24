package com.crm.controller.leads;

import com.crm.config.DatabaseConfig;
import com.crm.dto.leads.WebFormLeadRequest;
import com.crm.filter.AuthenticationFilter;
import com.crm.model.leads.Lead;
import com.crm.service.leads.LeadService;
import com.crm.util.JsonUtil;
import com.google.gson.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Full filter/servlet/service/DAO tests against an explicitly selected isolated MySQL schema. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeadCaptureJdbcTest {
    private long marketing, otherMarketing, denied;
    private String token;
    private final String marker="S401-"+UUID.randomUUID();
    @BeforeAll void setup() throws Exception {
        try (Connection c=DatabaseConfig.getConnection()) {
            assertTrue(c.getCatalog().startsWith("crm_sprint1_test"), "Refuse to mutate development database");
            marketing=user(c,"MARKETING"); otherMarketing=user(c,"MARKETING"); denied=user(c,null);
        }
        var embed=new LeadService().registeredEmbedCode(marketing, marker,"http://localhost:8080/crm","light","QA");
        token=embed.getFormId();
        assertEquals(token,new LeadService().registeredEmbedCode(marketing,marker,"http://localhost:8080/crm","light","QA").getFormId());
    }
    private long user(Connection c,String role) throws Exception {
        long id;
        try(var p=c.prepareStatement("INSERT INTO users(full_name,email,password_hash,status,data_scope) VALUES (?,?,?,'ACTIVE','SELF')",Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1,marker);p.setString(2,UUID.randomUUID()+"@example.invalid");p.setString(3,"unused-test-hash");p.executeUpdate();
            try(var r=p.getGeneratedKeys()){r.next();id=r.getLong(1);}
        }
        if(role!=null) try(var p=c.prepareStatement("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code=?")) {
            p.setLong(1,id);p.setString(2,role);assertEquals(1,p.executeUpdate());
        }
        return id;
    }
    @AfterAll void cleanup() throws Exception {
        if(marketing==0)return;
        try(Connection c=DatabaseConfig.getConnection()) {
            assertTrue(c.getCatalog().startsWith("crm_sprint1_test"));
            for(String sql:List.of("DELETE FROM lead_capture_submissions WHERE form_token IN (SELECT token FROM lead_capture_forms WHERE owner_user_id=?)",
                    "DELETE FROM lead_scores WHERE lead_id IN (SELECT id FROM leads WHERE owner_user_id=?)",
                    "DELETE FROM lead_scoring_profiles WHERE lead_id IN (SELECT id FROM leads WHERE owner_user_id=?)",
                    "DELETE FROM leads WHERE owner_user_id=?","DELETE FROM lead_capture_forms WHERE owner_user_id=?"))
                try(var p=c.prepareStatement(sql)){p.setLong(1,marketing);p.executeUpdate();}
            for(long id:new long[]{marketing,otherMarketing,denied}) {
                try(var p=c.prepareStatement("DELETE FROM user_roles WHERE user_id=?")){p.setLong(1,id);p.executeUpdate();}
                try(var p=c.prepareStatement("DELETE FROM users WHERE id=?")){p.setLong(1,id);p.executeUpdate();}
            }
        }
    }
    private WebFormLeadRequest payload() {
        var r=new WebFormLeadRequest();r.setFullName(marker);r.setEmail("capture@example.invalid");r.setPhone("0901234567");
        r.setSource("WEB_FORM");r.setFormId(token);r.setRequestId(UUID.randomUUID().toString());return r;
    }
    private record Result(int status,JsonObject body) {}
    private Result call(String method,String path,Long user,String body,Map<String,String> params) throws Exception {
        Map<String,Object> attrs=new HashMap<>();attrs.put("userId",user);attrs.put("sessionVersion",1L);
        if(user!=null)attrs.put("sessionVersion",new com.crm.service.auth.SessionService().account(user).get("sessionVersion"));
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->m.getName().equals("getAttribute")?attrs.get(a[0]):null);
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()) {
            case "getMethod"->method;case "getPathInfo"->path;case "getRequestURI"->"/crm/api/leads"+path;
            case "getContextPath"->"/crm";case "getSession"->user==null?null:session;
            case "getReader"->new BufferedReader(new StringReader(body==null?"":body));
            case "getContentType"->"application/json";case "getParameter"->params.get(a[0]);
            case "getScheme"->"http";case "getServerName"->"localhost";case "getServerPort"->8080;
            case "getRemoteAddr"->"127.0.0.1";default->null;
        });
        StringWriter out=new StringWriter();int[] status={200};
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if(m.getName().equals("setStatus"))status[0]=(int)a[0];return m.getName().equals("getWriter")?new PrintWriter(out):null;
        });
        new AuthenticationFilter().doFilter(req,resp,(r,s)->new LeadServlet() { @Override public void log(String message, Throwable e) { e.printStackTrace(); } }.service(req,resp));
        return new Result(status[0],JsonParser.parseString(out.toString()).getAsJsonObject());
    }
    @Test void marketingSessionCanReadAndGenerateEmbedButMissingSessionIs401() throws Exception {
        var read=call("GET","",marketing,null,Map.of()); assertEquals(200,read.status,read.body.toString());
        assertEquals(200,call("GET","/embed-code",marketing,null,Map.of()).status);
        assertEquals(401,call("GET","",null,null,Map.of()).status);
        assertEquals(401,call("POST","",null,"{}",Map.of()).status);
    }
    @Test void authenticatedUserWithoutPermissionsGets403() throws Exception {
        assertEquals(403,call("GET","",denied,null,Map.of()).status);
        assertEquals(403,call("GET","/embed-code",denied,null,Map.of()).status);
        assertEquals(403,call("POST","",denied,"{}",Map.of()).status);
    }
    @Test void publicSubmissionPersistsCatalogSourceAndAppearsOnlyForOwningMarketing() throws Exception {
        var result=call("POST","/web-form",null,JsonUtil.getGson().toJson(payload()),Map.of());
        assertEquals(201,result.status,result.body.toString());long id=result.body.getAsJsonObject("data").get("id").getAsLong();
        try(Connection c=DatabaseConfig.getConnection();var p=c.prepareStatement("SELECT l.source,l.source_id,l.owner_user_id,m.type FROM leads l JOIN master_data m ON m.id=l.source_id WHERE l.id=?")) {
            p.setLong(1,id);try(var r=p.executeQuery()){assertTrue(r.next());assertEquals("WEB_FORM",r.getString(1));assertTrue(r.getLong(2)>0);assertEquals(marketing,r.getLong(3));assertEquals("lead-source",r.getString(4));}
        }
        assertEquals(200,call("GET","/"+id,marketing,null,Map.of()).status);
        assertEquals(403,call("GET","/"+id,otherMarketing,null,Map.of()).status);
        var list=call("GET","",marketing,null,Map.of("search",marker,"size","100"));
        assertTrue(list.body.getAsJsonObject("data").getAsJsonArray("items").asList().stream().anyMatch(x->x.getAsJsonObject().get("id").getAsLong()==id));
    }
    @Test void missingSourceBadContactsAndUnregisteredFormAre400() throws Exception {
        for(String field:List.of("source","email","phone","formId")) {
            var r=payload();switch(field){case "source"->r.setSource(null);case "email"->r.setEmail("bad");case "phone"->r.setPhone("++++++++");default->r.setFormId(UUID.randomUUID().toString());}
            var result=call("POST","/web-form",null,JsonUtil.getGson().toJson(r),Map.of());assertEquals(400,result.status,result.body.toString());
        }
    }
    @Test void simultaneousRetriesCreateExactlyOneLeadAndChangedPayloadConflicts() throws Exception {
        var r=payload();var pool=Executors.newFixedThreadPool(2);
        try {
            List<Future<Lead>> results=pool.invokeAll(List.of(()->new LeadService().createWebFormLead(r,"127.0.0.1"),()->new LeadService().createWebFormLead(r,"127.0.0.1")));
            assertEquals(results.get(0).get().getId(),results.get(1).get().getId());
            r.setFullName("Changed");assertThrows(IllegalStateException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1"));
        } finally {pool.shutdownNow();}
    }
    @Test void manualContractAcceptsSourceIdAndMissingSourceHasUsefulError() throws Exception {
        long source;try(Connection c=DatabaseConfig.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT id FROM master_data WHERE type='lead-source' AND active=1 LIMIT 1")){assertTrue(r.next());source=r.getLong(1);}
        String json=JsonUtil.getGson().toJson(Map.of("fullName",marker,"email","manual@example.invalid","sourceId",source));
        var result=call("POST","",marketing,json,Map.of());assertEquals(201,result.status,result.body.toString());assertFalse(result.body.toString().contains("Field không hỗ trợ"));
        var bad=call("POST","",marketing,"{\"fullName\":\"QA\",\"email\":\"qa@example.invalid\"}",Map.of());
        assertEquals(400,bad.status);assertTrue(bad.body.toString().contains("sourceId"));
    }
}
