package com.crm.controller.support;

import com.crm.controller.notifications.NotificationServlet;
import com.crm.controller.customers.CustomerServlet;
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

class SupportServletTest {
 private int call(HttpServlet servlet,String path,String method,String json,boolean logged) throws Exception {
  int[] code={0};var output=new StringWriter();
  HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->1L);
  HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()) {
   case "getSession" -> logged?session:null;
   case "getPathInfo" -> path;
   case "getMethod" -> method;
   case "getContentType" -> "application/json";
   case "getReader" -> new BufferedReader(new StringReader(json==null?"":json));
   default -> null;
  });
  HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
   if(m.getName().equals("setStatus")) code[0]=(int)a[0];
   return m.getName().equals("getWriter")?new PrintWriter(output):null;
  });
  servlet.service((ServletRequest)req,(ServletResponse)resp);
  var response=JsonUtil.getGson().fromJson(output.toString(),com.google.gson.JsonObject.class);
  assertFalse(response.get("success").getAsBoolean());
  assertTrue(response.has("message"));assertTrue(response.has("data"));
  return code[0];
 }
 @Test void unauthenticatedSupportAndNotificationsReturn401Json() throws Exception {
  assertEquals(401,call(new SupportRequestServlet(),null,"GET",null,false));
  assertEquals(401,call(new NotificationServlet(),null,"GET",null,false));
 }
 @Test void rejectsUnknownRoutesAndMethods() throws Exception {
  assertEquals(404,call(new SupportRequestServlet(),"/1/unknown","GET",null,true));
  assertEquals(405,call(new SupportRequestServlet(),null,"DELETE",null,true));
  assertEquals(405,call(new SupportRequestServlet(),"/1","POST","{}",true));
  assertEquals(404,call(new NotificationServlet(),"/1/unknown","GET",null,true));
  assertEquals(405,call(new NotificationServlet(),"/1/read","POST",null,true));
 }
 @Test void malformedIdsAndJsonCannotReachServiceWrites() throws Exception {
  var servlet=new SupportRequestServlet();
  assertEquals(400,call(servlet,null,"POST","[",true));
  assertEquals(400,call(servlet,null,"POST","{\"customerId\":1.5}",true));
  assertEquals(400,call(servlet,null,"POST","{\"customerId\":\"1\"}",true));
  assertEquals(400,call(servlet,null,"POST","{\"ownerUserId\":99}",true));
  assertEquals(400,call(servlet,"/1","PUT","{\"status\":null}",true));
  assertEquals(400,call(servlet,"/1","PUT","{}",true));
  assertEquals(400,call(servlet,null,"POST","{\"customerId\":1,\"title\":true}",true));
  assertEquals(413,call(servlet,null,"POST","a".repeat(16385),true));
 }
 @Test void newMappingsDoNotConflictWithCustomer() {
  Set<String> maps=new HashSet<>(List.of(CustomerServlet.class.getAnnotation(WebServlet.class).value()));
  for(Class<?> servlet:List.of(SupportRequestServlet.class,NotificationServlet.class))
   for(String route:servlet.getAnnotation(WebServlet.class).value()) assertTrue(maps.add(route));
 }
}
