package com.crm.controller.scoring;

import com.crm.controller.leads.LeadServlet;
import com.crm.dto.scoring.ScoringConfig;
import com.crm.service.scoring.LeadScoringService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LeadScoringServletTest {
    private String failure;
    private ScoringConfig saved;
    private final LeadScoringService fake=new LeadScoringService() {
        private void check() throws Exception {
            if("forbidden".equals(failure))throw new SecurityException("Denied");
            if("missing".equals(failure))throw new NoSuchElementException("Missing");
            if("stale".equals(failure))throw new IllegalStateException("Stale");
            if("sql".equals(failure))throw new SQLException("Sensitive database details");
        }
        @Override public Map<String,Object> config(long user) throws Exception {check();return Map.of("version",0,"configured",false,"rules",List.of());}
        @Override public Map<String,Object> list(long user,int page,int size) throws Exception {check();return Map.of("items",List.of(),"page",page,"size",size);}
        @Override public Map<String,Object> get(long user,long id) throws Exception {check();return Map.of("id",id,"score",70,"classification","HOT");}
        @Override public Map<String,Object> saveConfig(long user,ScoringConfig config,boolean rulesOnly) throws Exception {check();saved=config;return Map.of("version",config.version()+1);}
        @Override public Map<String,Object> updateProfile(long user,long id,Map<String,Object> patch) throws Exception {check();return patch;}
        @Override public Map<String,Object> reScore(long user) throws Exception {check();return Map.of("count",2);}
    };
    private record Response(int status,JsonObject json,Map<String,String> headers) {}
    private Response call(String path,String method,String body,boolean logged,String type) throws Exception {
        int[] code={0};var output=new StringWriter();var headers=new HashMap<String,String>();
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->7L);
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()) {
            case "getSession" -> logged?session:null;
            case "getPathInfo" -> path;
            case "getMethod" -> method;
            case "getContentType" -> type;
            case "getReader" -> new BufferedReader(new StringReader(body==null?"":body));
            default -> null;
        });
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if(m.getName().equals("setStatus"))code[0]=(int)a[0];
            if(m.getName().equals("setHeader"))headers.put((String)a[0],(String)a[1]);
            return m.getName().equals("getWriter")?new PrintWriter(output):null;
        });
        var servlet=new LeadScoringServlet(fake){@Override public void log(String message,Throwable cause){}};
        servlet.service((ServletRequest)req,(ServletResponse)resp);
        var json=JsonUtil.getGson().fromJson(output.toString(),JsonObject.class);
        assertEquals(Set.of("success","message","data"),json.keySet());assertEquals("no-store",headers.get("Cache-Control"));
        return new Response(code[0],json,headers);
    }
    private Response call(String path,String method,String body) throws Exception {return call(path,method,body,true,"application/json");}
    @Test void sessionAndPermissionFailuresUse401And403() throws Exception {
        assertEquals(401,call("/config","GET",null,false,null).status());
        failure="forbidden";assertEquals(403,call("/leads","GET",null).status());assertEquals(403,call("/re-score","POST","{}").status());
    }
    @Test void successfulEndpointsReturnStandardJsonAndServerScore() throws Exception {
        assertEquals(200,call("/config","GET",null).status());
        assertEquals(200,call("/leads","GET",null).status());
        var detail=call("/leads/12","GET",null);assertTrue(detail.json().get("success").getAsBoolean());
        assertEquals(70,detail.json().getAsJsonObject("data").get("score").getAsInt());
        assertEquals(200,call("/re-score","POST","{}").status());
        assertEquals(200,call("/leads/12/profile","PUT","{\"employeeCount\":100,\"title\":null}").status());
        String config="{\"version\":0,\"thresholds\":{\"hot\":70,\"warm\":40},\"aggregation\":\"SUM_ALL\",\"minScore\":0,\"maxScore\":null,\"rules\":[]}";
        assertEquals(200,call("/config","PUT",config).status());assertEquals(70,saved.hot());assertNull(saved.maxScore());
        assertEquals(200,call("/rules","POST","{\"version\":1,\"rules\":[]}").status());assertEquals(1,saved.version());
    }
    @Test void malformedTypesUnknownFieldsAndScoreOverrideAre400() throws Exception {
        for(String json:List.of("[", "[]", "{\"score\":999}","{\"ownerUserId\":8}","{\"employeeCount\":1.5}","{\"employeeCount\":\"12\"}","{\"employeeCount\":2147483648}","{\"title\":true}"))
            assertEquals(400,call("/leads/1/profile","PUT",json).status(),json);
        for(String json:List.of("{\"version\":\"0\",\"rules\":[]}","{\"version\":-1,\"rules\":[]}","{\"version\":0,\"rules\":[{}]}","{\"version\":0,\"rules\":null}"))
            assertEquals(400,call("/rules","POST",json).status());
        assertEquals(400,call("/re-score","POST","{\"score\":99}").status());
        assertEquals(400,call("/rules","POST","{}",true,"text/plain").status());
        assertEquals(413,call("/rules","POST","a".repeat(262145)).status());
    }
    @Test void correct404405409AndSanitized500() throws Exception {
        assertEquals(404,call("/unknown","GET",null).status());assertEquals(404,call("/leads/0","GET",null).status());
        var response=call("/config","DELETE",null);assertEquals(405,response.status());assertEquals("GET, PUT",response.headers().get("Allow"));
        failure="missing";assertEquals(404,call("/leads/1","GET",null).status());
        failure="stale";assertEquals(409,call("/rules","POST","{\"version\":0,\"rules\":[]}").status());
        failure="sql";var error=call("/leads","GET",null);assertEquals(500,error.status());assertFalse(error.json().toString().contains("Sensitive"));
    }
    @Test void routeDoesNotTakeOverS4LeadImport() {
        assertEquals("/api/lead-scoring/*",LeadScoringServlet.class.getAnnotation(WebServlet.class).value()[0]);
        assertEquals("/api/leads/*",LeadServlet.class.getAnnotation(WebServlet.class).value()[0]);
    }
}
