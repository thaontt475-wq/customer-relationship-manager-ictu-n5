package com.crm.controller.customers;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.customers.CustomerImportConfirmRequest;
import com.crm.service.importer.CustomerImportService;
import com.crm.service.importer.CustomerExcelReader;
import com.crm.service.permissions.AuthorizationService;
import com.crm.util.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.Locale;

@WebServlet("/api/customers/import/*")
@MultipartConfig(maxFileSize=5242880,maxRequestSize=5500000,fileSizeThreshold=0)
public class CustomerImportServlet extends HttpServlet {
    private final CustomerImportService service=new CustomerImportService();
    @Override protected void service(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        try {
            if(req.getSession(false)==null || !(req.getSession(false).getAttribute("userId") instanceof Number)) {
                ResponseUtil.json(resp,401,ApiResponse.error("Chưa đăng nhập",null));return;
            }
            long user=AuthorizationService.currentUserId(req);
            String path=req.getPathInfo(),method=req.getMethod();
            if("/template".equals(path)) {
                if(!"GET".equals(method)) {unsupported(resp,"GET");return;}
                // Generate before committing the response, so workbook errors can still return JSON.
                ByteArrayOutputStream output=new ByteArrayOutputStream();
                service.writeTemplate(output,user);
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition","attachment; filename=\"customer_import_template.xlsx\"");
                resp.setContentLength(output.size());output.writeTo(resp.getOutputStream());return;
            }
            if("/preview".equals(path)) {
                if(!"POST".equals(method)) {unsupported(resp,"POST");return;}
                if(req.getContentType()==null || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/form-data"))
                    throw new IllegalArgumentException("Preview yêu cầu multipart/form-data, trường file");
                Part file=null;
                try {
                    file=req.getPart("file");
                    if(file==null || file.getSubmittedFileName()==null || !file.getSubmittedFileName().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                        throw new IllegalArgumentException("Chỉ chấp nhận file Excel .xlsx");
                    if(file.getSize()==0) throw new IllegalArgumentException("File Excel rỗng");
                    if(file.getSize()>CustomerExcelReader.MAX_BYTES) {ResponseUtil.json(resp,413,ApiResponse.error("File Excel tối đa 5MB",null));return;}
                    try(InputStream input=file.getInputStream()) {
                        ResponseUtil.json(resp,200,ApiResponse.success("Preview khách hàng thành công",service.preview(input,user)));
                    }
                } finally {if(file!=null) file.delete();}
                return;
            }
            if("/confirm".equals(path)) {
                if(!"POST".equals(method)) {unsupported(resp,"POST");return;}
                if(req.getContentType()==null || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("application/json"))
                    throw new IllegalArgumentException("Confirm yêu cầu application/json");
                char[] chars=new char[8193];int count=0,n;
                Reader reader=req.getReader();
                while(count<chars.length && (n=reader.read(chars,count,chars.length-count))!=-1) count+=n;
                if(count>8192) {ResponseUtil.json(resp,413,ApiResponse.error("JSON confirm tối đa 8KB",null));return;}
                JsonObject json=JsonUtil.getGson().fromJson(new String(chars,0,count),JsonObject.class);
                if(json==null) throw new IllegalArgumentException("Thiếu nội dung confirm");
                CustomerImportConfirmRequest body=new CustomerImportConfirmRequest();
                body.batchToken=string(json,"batchToken");body.duplicateMode=string(json,"duplicateMode");
                ResponseUtil.json(resp,200,ApiResponse.success("Import khách hàng hoàn tất",service.confirm(body,user)));return;
            }
            ResponseUtil.json(resp,404,ApiResponse.error("Không tìm thấy API import khách hàng",null));
        } catch(SecurityException e) {ResponseUtil.json(resp,403,ApiResponse.error(e.getMessage(),null));}
        catch(JsonParseException | IllegalArgumentException e) {ResponseUtil.json(resp,400,ApiResponse.error(e.getMessage(),null));}
        catch(IllegalStateException e) {
            ResponseUtil.json(resp,e.getMessage()!=null && e.getMessage().startsWith("Đã đạt")?429:413,
                    ApiResponse.error(e.getMessage()!=null && e.getMessage().startsWith("Đã đạt")?e.getMessage():"Upload vượt dung lượng cho phép",null));
        } catch(ServletException e) {ResponseUtil.json(resp,400,ApiResponse.error("Dữ liệu multipart không hợp lệ",null));}
        catch(SQLException e) {log("Customer import database failure",e);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý DB import; kiểm tra kết nối và migration S3-06",null));}
        catch(Exception e) {log("Customer import failure",e);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý import khách hàng",null));}
    }
    private String string(JsonObject object,String key) {
        if(!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString())
            throw new IllegalArgumentException(key+" phải là chuỗi");
        return object.get(key).getAsString();
    }
    private void unsupported(HttpServletResponse resp,String allow) throws IOException {
        resp.setHeader("Allow",allow);ResponseUtil.json(resp,405,ApiResponse.error("Phương thức không được hỗ trợ",null));
    }
}
