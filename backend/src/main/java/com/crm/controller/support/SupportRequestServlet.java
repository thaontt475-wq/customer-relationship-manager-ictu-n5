package com.crm.controller.support;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.support.SupportRequestWriteRequest;
import com.crm.service.support.SupportRequestService;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.*;
import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

@WebServlet({"/api/support-requests","/api/support-requests/*"})
public class SupportRequestServlet extends HttpServlet {
 private final SupportRequestService service=new SupportRequestService();
 @Override protected void service(HttpServletRequest req,HttpServletResponse resp) throws IOException {
  try {
   if(req.getSession(false)==null || !(req.getSession(false).getAttribute("userId") instanceof Number)) {
    ResponseUtil.json(resp,401,ApiResponse.error("Chưa đăng nhập",null));return;
   }
   long user=AuthorizationService.currentUserId(req);String path=req.getPathInfo();Object result;int status=200;
   if(path==null || path.equals("/")) {
    switch(req.getMethod()) {
     case "GET" -> {
      Long customer=req.getParameter("customerId")==null?null:id(req.getParameter("customerId"));
      int page=integer(req.getParameter("page"),1),size=integer(req.getParameter("size"),20);
      result=Map.of("items",service.list(user,customer,req.getParameter("status"),page,size),"page",page,"size",size);
     }
     case "POST" -> {var json=body(req);result=service.create(user,request(json));status=201;}
     default -> {unsupported(resp,"GET, POST");return;}
    }
   } else {
    if(!path.matches("/[1-9][0-9]*/?")) {ResponseUtil.json(resp,404,ApiResponse.error("Không tìm thấy API Support",null));return;}
    long ticket=id(path.replace("/",""));
    switch(req.getMethod()) {
     case "GET" -> result=service.get(user,ticket);
     case "PUT" -> {
      var json=body(req);
      if(json.size()==0) throw new IllegalArgumentException("Thiếu trường cập nhật");
      result=service.update(user,ticket,request(json),json.has("description") && json.get("description").isJsonNull());
     }
     case "DELETE" -> {service.delete(user,ticket);result=null;}
     default -> {unsupported(resp,"GET, PUT, DELETE");return;}
    }
   }
   ResponseUtil.json(resp,status,ApiResponse.success("Xử lý yêu cầu hỗ trợ thành công",result));
  } catch(SecurityException e) {ResponseUtil.json(resp,403,ApiResponse.error(e.getMessage(),null));}
  catch(NoSuchElementException e) {ResponseUtil.json(resp,404,ApiResponse.error(e.getMessage(),null));}
  catch(PayloadTooLarge e) {ResponseUtil.json(resp,413,ApiResponse.error(e.getMessage(),null));}
  catch(JsonParseException | IllegalArgumentException e) {ResponseUtil.json(resp,400,ApiResponse.error(e.getMessage(),null));}
  catch(IllegalStateException e) {ResponseUtil.json(resp,409,ApiResponse.error(e.getMessage(),null));}
  catch(SQLException e) {
   log("Support database failure",e);
   boolean conflict="23000".equals(e.getSQLState()) || "40001".equals(e.getSQLState()) || e.getErrorCode()==1205;
   ResponseUtil.json(resp,conflict?409:500,ApiResponse.error(conflict?"Dữ liệu xung đột; thao tác đã rollback":"Không thể xử lý Support; kiểm tra kết nối và migration S3-08",null));
  } catch(Exception e) {log("Support request failure",e);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý yêu cầu hỗ trợ",null));}
 }
 private static class PayloadTooLarge extends IllegalArgumentException {
  PayloadTooLarge() {super("JSON tối đa 16384 ký tự");}
 }
 private JsonObject body(HttpServletRequest req) throws IOException {
  if(req.getContentType()==null || !req.getContentType().split(";")[0].trim().equalsIgnoreCase("application/json")) throw new IllegalArgumentException("Yêu cầu application/json");
  req.setCharacterEncoding("UTF-8");
  char[] chars=new char[16385];int length=0,n;Reader reader=req.getReader();
  while(length<chars.length && (n=reader.read(chars,length,chars.length-length))!=-1) length+=n;
  if(length>16384) throw new PayloadTooLarge();
  var object=JsonUtil.getGson().fromJson(new String(chars,0,length),JsonObject.class);
  if(object==null) throw new IllegalArgumentException("Thiếu nội dung JSON");
  for(String key:object.keySet()) if(!Set.of("customerId","title","description","priority","assigneeId","status").contains(key))
   throw new IllegalArgumentException("Trường không được hỗ trợ: "+key);
  return object;
 }
 private SupportRequestWriteRequest request(JsonObject json) {
  var r=new SupportRequestWriteRequest();
  r.customerId=number(json,"customerId");r.assigneeId=number(json,"assigneeId");
  r.title=string(json,"title",false);r.description=string(json,"description",true);
  r.priority=string(json,"priority",false);r.status=string(json,"status",false);return r;
 }
 private Long number(JsonObject json,String key) {
  if(!json.has(key)) return null;
  if(!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber()) throw new IllegalArgumentException(key+" phải là số nguyên dương");
  return id(json.get(key).getAsString());
 }
 private String string(JsonObject json,String key,boolean nullable) {
  if(!json.has(key)) return null;
  if(json.get(key).isJsonNull() && nullable) return null;
  if(!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isString()) throw new IllegalArgumentException(key+" phải là chuỗi");
  return json.get(key).getAsString();
 }
 private long id(String raw) {
  if(raw==null || !raw.matches("[1-9][0-9]*")) throw new IllegalArgumentException("ID phải là số nguyên dương");return Long.parseLong(raw);
 }
 private int integer(String raw,int fallback) {return raw==null?fallback:Integer.parseInt(raw);}
 private void unsupported(HttpServletResponse resp,String allow) throws IOException {
  resp.setHeader("Allow",allow);ResponseUtil.json(resp,405,ApiResponse.error("Phương thức không được hỗ trợ",null));
 }
}
