package com.crm.controller.leads;

import com.crm.service.leads.*;
import com.crm.service.permissions.*;
import com.crm.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LeadServletTest {
    private record Result(int status,String body,String allow) {}
    private Result call(String method,String path,String type,String body,boolean session,boolean allowed,Part part) throws Exception {
        LeadService leads=new LeadService() {
            @Override public DataScopeContext authorizeCreate(long user) {
                if(!allowed)throw new SecurityException("Forbidden");
                return new DataScopeContext(user,"lead",ScopeType.SELF,null,Set.of(user));
            }
        };
        LeadImportService imports=new LeadImportService() {@Override public void authorize(long user) throws Exception {leads.authorizeCreate(user);}};
        HttpSession auth=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},
                (p,m,a)->m.getName().equals("getAttribute")&&a[0].equals("userId")?1L:null);
        HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){
            case "getSession"->session?auth:null;case "getMethod"->method;case "getPathInfo"->path;case "getContentType"->type;
            case "getReader"->new BufferedReader(new StringReader(body==null?"":body));case "getPart"->part;default->null;
        });
        int[] status={200};String[] allow={null};StringWriter writer=new StringWriter();
        HttpServletResponse response=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if(m.getName().equals("setStatus"))status[0]=(int)a[0];
            if(m.getName().equals("setHeader")&&a[0].equals("Allow"))allow[0]=(String)a[1];
            return m.getName().equals("getWriter")?new PrintWriter(writer):null;
        });
        new LeadServlet(leads,imports).service((ServletRequest)request,(ServletResponse)response);
        return new Result(status[0],writer.toString(),allow[0]);
    }
    @Test void authAndPermissionFailClosedBeforeUploadParsing() throws Exception {
        assertEquals(401,call("POST",null,"application/json","{}",false,true,null).status);
        assertEquals(403,call("POST",null,"application/json","{}",true,false,null).status);
        assertEquals(403,call("POST","/import/preview","multipart/form-data",null,true,false,null).status);
    }
    @Test void wrongMethodsAndUnknownPathsUseJsonAllow() throws Exception {
        var result=call("DELETE","/import/template",null,null,true,true,null);
        assertEquals(405,result.status);assertEquals("GET",result.allow);
        assertEquals(405,call("GET","/import/preview",null,null,true,true,null).status);
        assertEquals(404,call("GET","/unknown",null,null,true,true,null).status);
        assertFalse(JsonUtil.getGson().fromJson(result.body,com.google.gson.JsonObject.class).get("success").getAsBoolean());
    }
    @Test void malformedJsonContentTypeAndConfirmationRowsRejected() throws Exception {
        assertEquals(400,call("POST",null,"text/plain","{}",true,true,null).status);
        assertEquals(400,call("POST",null,"application/json","[",true,true,null).status);
        assertEquals(400,call("POST",null,"application/json","{\"ownerUserId\":2}",true,true,null).status);
        assertEquals(400,call("POST","/import/confirm","application/json","{\"batchToken\":false}",true,true,null).status);
        assertEquals(400,call("POST","/import/confirm","application/json","{\"batchToken\":\"x\",\"rows\":[]}",true,true,null).status);
    }
    @Test void oversizedJsonAndFileReturn413AndPartIsCleaned() throws Exception {
        assertEquals(413,call("POST",null,"application/json"," ".repeat(16385),true,true,null).status);
        assertEquals(413,call("POST","/import/confirm","application/json"," ".repeat(8193),true,true,null).status);
        boolean[] deleted={false};
        Part part=(Part)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Part.class},(p,m,a)->{
            if(m.getName().equals("delete")){deleted[0]=true;return null;}
            return switch(m.getName()){case "getSubmittedFileName"->"oversize.xlsx";case "getSize"->5242881L;default->null;};
        });
        assertEquals(413,call("POST","/import/preview","multipart/form-data",null,true,true,part).status);assertTrue(deleted[0]);
    }
}
