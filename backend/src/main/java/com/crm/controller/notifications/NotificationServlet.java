package com.crm.controller.notifications;

import com.crm.dto.common.ApiResponse;
import com.crm.service.notifications.NotificationService;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;

@WebServlet({"/api/notifications","/api/notifications/*"})
public class NotificationServlet extends HttpServlet {
 private final NotificationService service=new NotificationService();
 @Override protected void service(HttpServletRequest req,HttpServletResponse resp) throws IOException {
  try {
   if(req.getSession(false)==null || !(req.getSession(false).getAttribute("userId") instanceof Number)) {
    ResponseUtil.json(resp,401,ApiResponse.error("Chưa đăng nhập",null));return;
   }
   long user=AuthorizationService.currentUserId(req);String path=req.getPathInfo();Object result;
   if(path==null || path.equals("/")) {
    if(!req.getMethod().equals("GET")) {unsupported(resp,"GET");return;}
    int page=req.getParameter("page")==null?1:Integer.parseInt(req.getParameter("page"));
    int size=req.getParameter("size")==null?20:Integer.parseInt(req.getParameter("size"));
    result=service.list(user,page,size);
   } else if(path.matches("/[1-9][0-9]*/read/?")) {
    if(!req.getMethod().equals("PUT")) {unsupported(resp,"PUT");return;}
    result=service.markRead(user,Long.parseLong(path.split("/")[1]));
   } else {ResponseUtil.json(resp,404,ApiResponse.error("Không tìm thấy API Notification",null));return;}
   ResponseUtil.json(resp,200,ApiResponse.success("Xử lý thông báo thành công",result));
  } catch(SecurityException e) {ResponseUtil.json(resp,403,ApiResponse.error(e.getMessage(),null));}
  catch(NoSuchElementException e) {ResponseUtil.json(resp,404,ApiResponse.error(e.getMessage(),null));}
  catch(IllegalArgumentException e) {ResponseUtil.json(resp,400,ApiResponse.error(e.getMessage(),null));}
  catch(SQLException e) {
   log("Notification database failure",e);
   boolean conflict="40001".equals(e.getSQLState()) || e.getErrorCode()==1205;
   ResponseUtil.json(resp,conflict?409:500,ApiResponse.error(conflict?"Thông báo đang được cập nhật; vui lòng thử lại":"Không thể xử lý thông báo",null));
  } catch(Exception e) {log("Notification failure",e);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý thông báo",null));}
 }
 private void unsupported(HttpServletResponse resp,String allow) throws IOException {
  resp.setHeader("Allow",allow);ResponseUtil.json(resp,405,ApiResponse.error("Phương thức không được hỗ trợ",null));
 }
}
