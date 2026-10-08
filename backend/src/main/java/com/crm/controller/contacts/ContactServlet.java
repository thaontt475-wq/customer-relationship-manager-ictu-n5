package com.crm.controller.contacts;
import com.crm.dto.common.ApiResponse;
import com.crm.dto.contacts.*;
import com.crm.service.contacts.ContactService;
import com.crm.util.*;
import com.google.gson.JsonParseException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;

@WebServlet({"/api/contacts","/api/contacts/*"})
public class ContactServlet extends HttpServlet {
 private final ContactService contacts=new ContactService();
 @Override protected void service(HttpServletRequest req,HttpServletResponse resp) throws IOException {
  try {
   var session=req.getSession(false);
   if(session==null || !(session.getAttribute("userId") instanceof Number)) {
    ResponseUtil.json(resp,401,ApiResponse.error("Chưa đăng nhập",null));return;
   }
   long user=((Number)session.getAttribute("userId")).longValue();
   String path=req.getPathInfo(),method=req.getMethod();
   Object data;
   int status=200;
   if(path==null || path.equals("/")) {
    if(method.equals("GET")) data=contacts.list(user,req.getParameter("customerId")==null?null:Long.valueOf(req.getParameter("customerId")));
    else if(method.equals("POST")) {data=contacts.create(user,JsonUtil.getGson().fromJson(req.getReader(),ContactWriteRequest.class));status=201;}
    else {unsupported(resp,"GET, POST");return;}
   } else {
    if(!path.matches("/[1-9][0-9]*(/(primary|transfer|history))?/?")) {
     ResponseUtil.json(resp,404,ApiResponse.error("Không tìm thấy API người liên hệ",null));return;
    }
    String[] parts=path.substring(1).split("/");
    long id=Long.parseLong(parts[0]);
    String action=parts.length>1?parts[1]:"";
    if(action.equals("history")) {
     if(!method.equals("GET")) {unsupported(resp,"GET");return;} data=contacts.history(user,id);
    } else if(action.equals("primary")) {
     if(!method.equals("PUT")) {unsupported(resp,"PUT");return;} data=contacts.primary(user,id);
    } else if(action.equals("transfer")) {
     if(!method.equals("POST")) {unsupported(resp,"POST");return;}
     data=contacts.transfer(user,id,JsonUtil.getGson().fromJson(req.getReader(),ContactTransferRequest.class));
    } else {
     switch(method) {
      case "GET" -> data=contacts.get(user,id);
      case "PUT" -> data=contacts.update(user,id,JsonUtil.getGson().fromJson(req.getReader(),ContactWriteRequest.class));
      case "DELETE" -> {contacts.delete(user,id);data=null;}
      default -> {unsupported(resp,"GET, PUT, DELETE");return;}
     }
    }
   }
   ResponseUtil.json(resp,status,ApiResponse.success("Thao tác người liên hệ thành công",data));
  } catch(SecurityException e) {ResponseUtil.json(resp,403,ApiResponse.error(e.getMessage(),null));}
  catch(NoSuchElementException e) {ResponseUtil.json(resp,404,ApiResponse.error(e.getMessage(),null));}
  catch(JsonParseException | IllegalArgumentException e) {ResponseUtil.json(resp,400,ApiResponse.error("Dữ liệu không hợp lệ: "+e.getMessage(),null));}
  catch(SQLException e) {
   boolean conflict="23000".equals(e.getSQLState()) || "40001".equals(e.getSQLState()) || e.getErrorCode()==1205;
   log("Contact database operation failed",e);
   ResponseUtil.json(resp,conflict?409:500,ApiResponse.error(conflict?"Dữ liệu xung đột, vui lòng thử lại":"Không thể xử lý dữ liệu người liên hệ",null));
  } catch(Exception e) {log("Contact operation failed",e);ResponseUtil.json(resp,500,ApiResponse.error("Lỗi máy chủ khi xử lý người liên hệ",null));}
 }
 private void unsupported(HttpServletResponse resp,String allow) throws IOException {
  resp.setHeader("Allow",allow);ResponseUtil.json(resp,405,ApiResponse.error("Phương thức không được hỗ trợ",null));
 }
}
