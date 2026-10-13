package com.crm.controller.customers;

import com.crm.dto.common.ApiResponse;
import com.crm.service.customers.CustomerAttachmentService;
import com.crm.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.NoSuchElementException;

/** Downloads private attachment files only after the Customer scope check. */
@WebServlet("/api/customer-attachments/*")
public class CustomerAttachmentServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session=request.getSession(false);
        if (session==null || !(session.getAttribute("userId") instanceof Number user)) {
            ResponseUtil.json(response,401,ApiResponse.error("Chưa đăng nhập",null)); return;
        }
        try {
            String path=request.getPathInfo();
            if (path==null || !path.matches("/[1-9][0-9]*/download")) throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ");
            long id=Long.parseLong(path.split("/")[1]);
            var download=new CustomerAttachmentService().download(user.longValue(),id);
            Path file=download.path();
            response.setHeader("Cache-Control","no-store");
            response.setHeader("X-Content-Type-Options","nosniff");
            response.setContentType("application/octet-stream");
            String encoded=java.net.URLEncoder.encode(download.name(),java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");
            response.setHeader("Content-Disposition","attachment; filename*=UTF-8''"+encoded);
            response.setContentLengthLong(Files.size(file));
            Files.copy(file,response.getOutputStream());
        } catch(SecurityException error) { ResponseUtil.json(response,403,ApiResponse.error(error.getMessage(),null)); }
        catch(IllegalArgumentException error) { ResponseUtil.json(response,400,ApiResponse.error(error.getMessage(),null)); }
        catch(NoSuchFileException error) { ResponseUtil.json(response,404,ApiResponse.error("Tệp chưa có trong kho tài liệu",null)); }
        catch(NoSuchElementException error) { ResponseUtil.json(response,404,ApiResponse.error(error.getMessage(),null)); }
        catch(Exception error) { log("Attachment download failed",error);ResponseUtil.json(response,500,ApiResponse.error("Không thể tải tài liệu",null)); }
    }
}
