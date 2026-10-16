package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.*;
import com.crm.service.leads.*;
import com.crm.service.importer.BoundedXlsx;
import com.crm.util.*;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/api/leads/*")
@MultipartConfig(maxFileSize=5242880,maxRequestSize=5500000,fileSizeThreshold=0)
public class LeadServlet extends HttpServlet {
    private final LeadService leads;
    private final LeadImportService imports;
    public LeadServlet() {this(new LeadService(),new LeadImportService());}
    public LeadServlet(LeadService leads,LeadImportService imports) {this.leads=leads;this.imports=imports;}

    @Override protected void service(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        try {
            HttpSession session=req.getSession(false);
            if(session==null || !(session.getAttribute("userId") instanceof Number id)) {
                ResponseUtil.json(resp,401,ApiResponse.error("Chưa đăng nhập",null));return;
            }
            long user=id.longValue();String path=req.getPathInfo(),method=req.getMethod();
            if(path==null || path.equals("/")) {
                if(!method.equals("POST")){unsupported(resp,"POST");return;}
                leads.authorizeCreate(user);
                ResponseUtil.json(resp,201,ApiResponse.success("Tạo Lead thành công",leads.create(user,LeadWriteRequest.fromJson(json(req,16384)))));return;
            }
            if(path.equals("/sources")) {
                if(!method.equals("GET")){unsupported(resp,"GET");return;}
                ResponseUtil.json(resp,200,ApiResponse.success("Nguồn Lead",leads.sources(user)));return;
            }
            if(path.equals("/import/template")) {
                if(!method.equals("GET")){unsupported(resp,"GET");return;}
                ByteArrayOutputStream out=new ByteArrayOutputStream();imports.template(user,out);
                resp.setHeader("Cache-Control","no-store");resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition","attachment; filename=\"lead_import_template.xlsx\"");
                resp.setContentLength(out.size());out.writeTo(resp.getOutputStream());return;
            }
            if(path.equals("/import/preview")) {
                if(!method.equals("POST")){unsupported(resp,"POST");return;}
                imports.authorize(user);
                if(req.getContentType()==null || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/form-data"))
                    throw new IllegalArgumentException("Preview yêu cầu multipart/form-data, trường file");
                Part file=null;
                try {
                    file=req.getPart("file");
                    if(file==null || file.getSubmittedFileName()==null || !file.getSubmittedFileName().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                        throw new IllegalArgumentException("Chỉ chấp nhận file .xlsx");
                    if(file.getSize()>BoundedXlsx.MAX_BYTES) {ResponseUtil.json(resp,413,ApiResponse.error("File Excel tối đa 5MB",null));return;}
                    try(InputStream input=file.getInputStream()) {
                        ResponseUtil.json(resp,200,ApiResponse.success("Preview Lead thành công",imports.preview(user,input)));return;
                    }
                } finally {if(file!=null)file.delete();}
            }
            if(path.equals("/import/confirm")) {
                if(!method.equals("POST")){unsupported(resp,"POST");return;}
                imports.authorize(user);JsonObject body=json(req,8192);
                if(!body.keySet().equals(Set.of("batchToken")) || !body.get("batchToken").isJsonPrimitive() || !body.getAsJsonPrimitive("batchToken").isString())
                    throw new IllegalArgumentException("Confirm chỉ nhận batchToken dạng chuỗi");
                ResponseUtil.json(resp,200,ApiResponse.success("Import Lead hoàn tất",imports.confirm(user,body.get("batchToken").getAsString())));return;
            }
            if(path.matches("/[1-9][0-9]*")) {
                if(!method.equals("GET")){unsupported(resp,"GET");return;}
                var lead=leads.get(user,Long.parseLong(path.substring(1)));
                ResponseUtil.json(resp,lead==null?404:200,lead==null?ApiResponse.error("Không tìm thấy Lead",null):ApiResponse.success("Hồ sơ Lead",lead));return;
            }
            ResponseUtil.json(resp,404,ApiResponse.error("Không tìm thấy API Lead",null));
        } catch(LeadValidationException error) {ResponseUtil.json(resp,400,ApiResponse.error(error.getMessage(),Map.of("errors",error.errors())));}
        catch(LeadImportService.Failure error) {
            if(error.status()==500)log("Lead import failed",error);
            ResponseUtil.json(resp,error.status(),ApiResponse.error(error.getMessage(),error.data()));
        } catch(SecurityException error) {ResponseUtil.json(resp,403,ApiResponse.error(error.getMessage(),null));}
        catch(JsonSyntaxException | IllegalArgumentException error) {ResponseUtil.json(resp,400,ApiResponse.error("Dữ liệu yêu cầu không hợp lệ: "+error.getMessage(),null));}
        catch(IllegalStateException error) {ResponseUtil.json(resp,413,ApiResponse.error("Upload vượt dung lượng cho phép",null));}
        catch(ServletException error) {ResponseUtil.json(resp,400,ApiResponse.error("Dữ liệu multipart không hợp lệ",null));}
        catch(SQLException error) {log("Lead database failure",error);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý dữ liệu Lead; kiểm tra DB và migration 024",null));}
        catch(Exception error) {log("Lead request failed",error);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý yêu cầu Lead",null));}
    }
    private JsonObject json(HttpServletRequest req,int limit) throws Exception {
        if(req.getContentType()==null || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("application/json"))
            throw new IllegalArgumentException("Yêu cầu application/json");
        char[] chars=new char[limit+1];int size=0,count;
        Reader input=req.getReader();
        while(size<chars.length && (count=input.read(chars,size,chars.length-size))!=-1)size+=count;
        if(size>limit)throw new LeadImportService.Failure(413,"JSON vượt giới hạn cho phép",null);
        var body=JsonParser.parseString(new String(chars,0,size));
        if(!body.isJsonObject())throw new IllegalArgumentException("Body phải là JSON object");
        return body.getAsJsonObject();
    }
    private void unsupported(HttpServletResponse resp,String allow) throws IOException {
        resp.setHeader("Allow",allow);ResponseUtil.json(resp,405,ApiResponse.error("Phương thức không được hỗ trợ",null));
    }
}
