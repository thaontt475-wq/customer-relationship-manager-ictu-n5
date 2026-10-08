package com.crm.controller.customers;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import com.crm.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerMergeServletTest {
 private int call(String route,String method,String json,boolean logged) throws Exception {
  int[] code={0};var output=new StringWriter();
  HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->1L);
  HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()) {
   case "getSession" -> logged?session:null;
   case "getServletPath" -> route;
   case "getMethod" -> method;
   case "getContentType" -> "application/json";
   case "getReader" -> new BufferedReader(new StringReader(json==null?"":json));
   default -> null;
  });
  HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
   if(m.getName().equals("setStatus")) code[0]=(int)a[0];
   return m.getName().equals("getWriter")?new PrintWriter(output):null;
  });
  new CustomerMergeServlet().service((ServletRequest)req,(ServletResponse)resp);
  assertFalse(JsonUtil.getGson().fromJson(output.toString(),com.google.gson.JsonObject.class).get("success").getAsBoolean());
  return code[0];
 }
 @Test void mappingsDoNotDuplicateCustomerServlet() {
  var original=List.of(CustomerServlet.class.getAnnotation(WebServlet.class).value());
  for(String path:CustomerMergeServlet.class.getAnnotation(WebServlet.class).value()) assertFalse(original.contains(path));
 }
 @Test void unauthenticatedWrongMethodAndInvalidIdsHaveProperStatus() throws Exception {
  assertEquals(401,call("/api/customers/merge","POST","{}",false));
  assertEquals(405,call("/api/customers/merge","GET",null,true));
  assertEquals(405,call("/api/customers/compare","POST",null,true));
  assertEquals(400,call("/api/customers/compare","GET",null,true));
  assertEquals(400,call("/api/customers/merge","POST","{\"masterId\":1.5,\"secondaryId\":2}",true));
  assertEquals(400,call("/api/customers/merge","POST","{\"masterId\":1,\"secondaryId\":1}",true));
  assertEquals(400,call("/api/customers/merge","POST","{\"masterId\":1,\"secondaryId\":2,\"fieldOverrides\":[]}",true));
 }
}
