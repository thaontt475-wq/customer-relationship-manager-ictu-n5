package com.crm.controller.customers;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.customers.CustomerMergeRequest;
import com.crm.service.customers.CustomerMergeService;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.*;
import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.NoSuchElementException;

@WebServlet({"/api/customers/duplicates","/api/customers/compare","/api/customers/merge"})
public class CustomerMergeServlet extends HttpServlet {
 private final CustomerMergeService service=new CustomerMergeService();
 @Override protected void service(HttpServletRequest req,HttpServletResponse resp) throws IOException {
  try {
   if(req.getSession(false)==null || !(req.getSession(false).getAttribute("userId") instanceof Number)) {
    ResponseUtil.json(resp,401,ApiResponse.error("Chưa đăng nhập",null));return;
   }
   long user=AuthorizationService.currentUserId(req);String route=req.getServletPath();Object result;
   if("/api/customers/duplicates".equals(route)) {
    if(!req.getMethod().equals("GET")) {unsupported(resp,"GET");return;}
    result=service.duplicates(user,req.getParameter("customerId")==null?null:parseId(req.getParameter("customerId")));
   } else if("/api/customers/compare".equals(route)) {
    if(!req.getMethod().equals("GET")) {unsupported(resp,"GET");return;}
    result=service.compare(user,parseId(req.getParameter("masterId")),parseId(req.getParameter("secondaryId")));
   } else if("/api/customers/merge".equals(route)) {
    if(!req.getMethod().equals("POST")) {unsupported(resp,"POST");return;}
    if(req.getContentType()==null || !req.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) throw new IllegalArgumentException("Merge yêu cầu application/json");
    char[] buffer=new char[16385];int count=0,n;Reader reader=req.getReader();
    while(count<buffer.length && (n=reader.read(buffer,count,buffer.length-count))!=-1) count+=n;
    if(count>16384) {ResponseUtil.json(resp,413,ApiResponse.error("JSON merge tối đa 16KB",null));return;}
    JsonObject json=JsonUtil.getGson().fromJson(new String(buffer,0,count),JsonObject.class);
    if(json==null) throw new IllegalArgumentException("Thiếu nội dung merge");
    CustomerMergeRequest body=new CustomerMergeRequest();
    body.masterId=jsonId(json,"masterId");body.secondaryId=jsonId(json,"secondaryId");
    if(json.has("fieldOverrides") && !json.get("fieldOverrides").isJsonNull()) {
     if(!json.get("fieldOverrides").isJsonObject()) throw new IllegalArgumentException("fieldOverrides phải là object");
     body.fieldOverrides=json.getAsJsonObject("fieldOverrides");
    }
    result=service.merge(user,body);
   } else {ResponseUtil.json(resp,404,ApiResponse.error("Không tìm thấy API",null));return;}
   ResponseUtil.json(resp,200,ApiResponse.success("Xử lý duplicate/merge thành công",result));
  } catch(SecurityException e) {ResponseUtil.json(resp,403,ApiResponse.error(e.getMessage(),null));}
  catch(NoSuchElementException e) {ResponseUtil.json(resp,404,ApiResponse.error(e.getMessage(),null));}
  catch(JsonParseException | IllegalArgumentException e) {ResponseUtil.json(resp,400,ApiResponse.error(e.getMessage(),null));}
  catch(IllegalStateException e) {ResponseUtil.json(resp,409,ApiResponse.error(e.getMessage(),null));}
  catch(SQLException e) {
   log("Customer merge database failure",e);
   boolean conflict="23000".equals(e.getSQLState()) || "40001".equals(e.getSQLState()) || e.getErrorCode()==1205;
   ResponseUtil.json(resp,conflict?409:500,ApiResponse.error(conflict?"Dữ liệu liên kết xung đột; merge đã rollback":"Không thể xử lý dữ liệu Customer; kiểm tra kết nối và migration S3-04",null));
  } catch(Exception e) {log("Customer merge failure",e);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý duplicate/merge",null));}
 }
 private long jsonId(JsonObject json,String key) {
  if(!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isNumber()) throw new IllegalArgumentException(key+" phải là số nguyên dương");
  return parseId(json.get(key).getAsString());
 }
 private long parseId(String value) {
  if(value==null || !value.matches("[1-9][0-9]*")) throw new IllegalArgumentException("Customer ID phải là số nguyên dương");
  return Long.parseLong(value);
 }
 private void unsupported(HttpServletResponse resp,String allow) throws IOException {
  resp.setHeader("Allow",allow);ResponseUtil.json(resp,405,ApiResponse.error("Phương thức không được hỗ trợ",null));
 }
}
