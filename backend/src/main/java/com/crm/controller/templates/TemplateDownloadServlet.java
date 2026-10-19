package com.crm.controller.templates;

import com.crm.dto.common.ApiResponse;
import com.crm.util.ExcelTemplateGenerator;
import com.crm.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/templates/excel/*")
public class TemplateDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String filename;

        try {
            switch (path.toLowerCase()) {
                case "/customers-import":
                case "/customer-import":
                    ExcelTemplateGenerator.buildCustomerImportTemplate(out);
                    filename = "Mau_Nhap_Khach_Hang_NovaCRM.xlsx";
                    break;
                case "/leads-import":
                case "/lead-import":
                    ExcelTemplateGenerator.buildLeadImportTemplate(out);
                    filename = "Mau_Nhap_Lead_Tiem_Nang_NovaCRM.xlsx";
                    break;
                case "/users-import":
                case "/user-import":
                    ExcelTemplateGenerator.buildUserImportTemplate(out);
                    filename = "Mau_Nhap_Nguoi_Dung_NovaCRM.xlsx";
                    break;
                case "/quotes-export":
                case "/quotations-export":
                    ExcelTemplateGenerator.buildQuotationsExportTemplate(out);
                    filename = "Mau_Xuat_Bao_Gia_NovaCRM.xlsx";
                    break;
                case "/contracts-export":
                case "/contract-export":
                    ExcelTemplateGenerator.buildContractsExportTemplate(out);
                    filename = "Mau_Xuat_Hop_Dong_NovaCRM.xlsx";
                    break;
                case "/customers-export":
                case "/customer-export":
                    ExcelTemplateGenerator.buildCustomersExportTemplate(out);
                    filename = "Mau_Xuat_Khach_Hang_NovaCRM.xlsx";
                    break;
                case "/contacts-export":
                case "/contact-export":
                    ExcelTemplateGenerator.buildContactsExportTemplate(out);
                    filename = "Mau_Xuat_Nguoi_Lien_He_NovaCRM.xlsx";
                    break;
                default:
                    ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy tệp mẫu Excel yêu cầu", null));
                    return;
            }

            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);
            resp.setContentLength(out.size());
            out.writeTo(resp.getOutputStream());

        } catch (Exception e) {
            resp.setStatus(500);
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi khi kết xuất tệp mẫu Excel: " + e.getMessage(), null));
        }
    }
}
