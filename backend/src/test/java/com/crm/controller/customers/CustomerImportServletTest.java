package com.crm.controller.customers;

import com.crm.util.JsonUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerImportServletTest {
    private record Result(int status,String body,String allow) {}
    private Result request(String method,String path,String contentType,String body,boolean loggedIn) throws Exception {
        var output=new StringWriter();int[] status={0};String[] allow={null};
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},
            (p,m,a)->m.getName().equals("getAttribute")?1L:null);
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},
            (p,m,a)->switch(m.getName()) {
                case "getSession" -> loggedIn?session:null;
                case "getMethod" -> method;
                case "getPathInfo" -> path;
                case "getContentType" -> contentType;
                case "getReader" -> new BufferedReader(new StringReader(body==null?"":body));
                default -> null;
            });
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},
            (p,m,a)->{
                if(m.getName().equals("setStatus")) status[0]=(int)a[0];
                if(m.getName().equals("setHeader") && a[0].equals("Allow")) allow[0]=(String)a[1];
                return m.getName().equals("getWriter")?new PrintWriter(output):null;
            });
        new CustomerImportServlet().service((ServletRequest)req,(ServletResponse)resp);
        return new Result(status[0],output.toString(),allow[0]);
    }
    @Test void independentMappingIsMoreSpecificWithoutDuplicatingCustomerMapping() {
        assertArrayEquals(new String[]{"/api/customers/import/*"},CustomerImportServlet.class.getAnnotation(WebServlet.class).value());
        assertTrue(Arrays.stream(CustomerServlet.class.getAnnotation(WebServlet.class).value()).noneMatch("/api/customers/import/*"::equals));
    }
    @Test void unauthenticatedRequestReturns401ApiResponse() throws Exception {
        var r=request("POST","/confirm","application/json","{}",false);
        assertEquals(401,r.status);
        assertFalse(JsonUtil.getGson().fromJson(r.body,com.google.gson.JsonObject.class).get("success").getAsBoolean());
    }
    @Test void wrongMethodsAndUnknownRoutesReturnJson() throws Exception {
        var template=request("POST","/template",null,null,true);
        assertEquals(405,template.status);assertEquals("GET",template.allow);
        assertEquals(405,request("GET","/preview",null,null,true).status);
        assertEquals(405,request("DELETE","/confirm",null,null,true).status);
        assertEquals(404,request("GET","/unknown",null,null,true).status);
    }
    @Test void invalidContentTypesMalformedJsonAndNonStringFieldsAreRejected() throws Exception {
        assertEquals(400,request("POST","/preview","application/json","{}",true).status);
        assertEquals(400,request("POST","/confirm","text/plain","{}",true).status);
        assertEquals(400,request("POST","/confirm","application/json","[",true).status);
        assertEquals(400,request("POST","/confirm","application/json","{\"batchToken\":1,\"duplicateMode\":\"SKIP\"}",true).status);
        assertEquals(400,request("POST","/confirm","application/json","{\"batchToken\":\"abc\",\"duplicateMode\":true}",true).status);
    }
    @Test void oversizedConfirmationReturns413() throws Exception {
        assertEquals(413,request("POST","/confirm","application/json","a".repeat(8193),true).status);
    }
}
