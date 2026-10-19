package com.crm.service.leads;

import com.crm.dto.leads.*;
import com.crm.dto.leads.LeadValidationException.FieldError;
import com.crm.service.importer.BoundedXlsx;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;

public class LeadExcelReader {
    public static final int MAX_ROWS=1000;
    public static final List<String> COLUMNS=List.of("fullName","companyName","email","phone","sourceCode","notes");
    public record ImportRow(int row,LeadWriteRequest lead,List<FieldError> errors) {
        public ImportRow { errors=List.copyOf(errors); }
    }
    public void template(OutputStream out) throws IOException {
        try(XSSFWorkbook book=new XSSFWorkbook()) {
            Sheet sheet=book.createSheet("Leads");
            sheet.setDisplayGridlines(true);

            // 1. Header Style (Navy Blue #1E3A8A)
            CellStyle headerStyle=book.createCellStyle();
            Font headerFont=book.createFont();
            headerFont.setFontName("Segoe UI");
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle, IndexedColors.GREY_40_PERCENT.getIndex());

            // 2. Data Styles
            short textFormat=book.createDataFormat().getFormat("@");
            
            CellStyle textStyle=book.createCellStyle();
            Font dataFont=book.createFont();
            dataFont.setFontName("Segoe UI");
            textStyle.setFont(dataFont);
            textStyle.setDataFormat(textFormat);
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(textStyle, IndexedColors.GREY_25_PERCENT.getIndex());

            CellStyle centerStyle=book.createCellStyle();
            centerStyle.cloneStyleFrom(textStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle zebraTextStyle=book.createCellStyle();
            zebraTextStyle.cloneStyleFrom(textStyle);
            zebraTextStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            zebraTextStyle.setFillPattern(FillPatternType.FINE_DOTS);

            CellStyle zebraCenterStyle=book.createCellStyle();
            zebraCenterStyle.cloneStyleFrom(centerStyle);
            zebraCenterStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            zebraCenterStyle.setFillPattern(FillPatternType.FINE_DOTS);

            // Default column style as text
            for(int i=0;i<COLUMNS.size();i++) {
                sheet.setDefaultColumnStyle(i, textStyle);
            }

            // Row 0: Headers
            Row header=sheet.createRow(0);
            header.setHeightInPoints(28);
            for(int i=0;i<COLUMNS.size();i++) {
                Cell cell=header.createCell(i);
                cell.setCellValue(COLUMNS.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 3. Real Sample Data Rows (5 valid sample leads)
            String[][] sampleData = {
                {"Nguyễn Văn An", "Công ty TNHH Thương mại Minh Phúc", "an.nguyen@minhphuc.vn", "0901234567", "WEBSITE", "Quan tâm phần mềm CRM gói 50 người dùng"},
                {"Trần Thị Bích", "CP Bất động sản Địa Ốc Xanh", "bich.tran@diaocthanh.com", "0918765432", "EVENT", "Khách mời hội thảo TechExpo 2026, cần gọi lại sau 15h"},
                {"Lê Hoàng Cường", "Tập đoàn Công nghệ Viễn thông Nam Á", "cuong.le@namatelecom.vn", "0988776655", "REFERRAL", "Được giới thiệu từ đối tác FPT, dự án quý 4"},
                {"Phạm Thu Dung", "Chuỗi Thời trang An Phước", "dung.pham@anphuocfashion.com", "0933221100", "SOCIAL_MEDIA", "Đăng ký tư vấn qua Fanpage Facebook"},
                {"Vũ Minh Đức", "Bệnh viện Quốc tế Mediphar", "duc.vu@mediphar.vn", "0977665544", "COLD_CALL", "Nhu cầu nâng cấp hệ thống quản lý chăm sóc bệnh nhân"}
            };

            for(int r=0; r<sampleData.length; r++) {
                Row row=sheet.createRow(r + 1);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);
                for(int c=0; c<sampleData[r].length; c++) {
                    Cell cell=row.createCell(c);
                    cell.setCellValue(sampleData[r][c]);
                    if(c == 3 || c == 4) { // phone, sourceCode centered
                        cell.setCellStyle(isZebra ? zebraCenterStyle : centerStyle);
                    } else {
                        cell.setCellStyle(isZebra ? zebraTextStyle : textStyle);
                    }
                }
            }

            // Column Widths
            int[] widths = {7500, 10500, 8500, 5200, 4800, 12000};
            for(int i=0; i<widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0,1);
            book.write(out);
        }
    }

    private static void setBorders(CellStyle style, short color) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
    }

    public List<ImportRow> read(InputStream input,List<Map<String,Object>> sources) throws IOException {
        byte[] bytes=BoundedXlsx.read(input);
        Map<String,Long> sourceIds=new HashMap<>();
        for(var source:sources)sourceIds.put(source.get("code").toString(),((Number)source.get("id")).longValue());
        try(XSSFWorkbook book=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if(book.getNumberOfSheets()!=1 || !book.getSheetName(0).equals("Leads"))
                throw new IllegalArgumentException("File phải có đúng một worksheet Leads");
            Sheet sheet=book.getSheetAt(0);
            if(sheet.getLastRowNum()>MAX_ROWS)throw new IllegalArgumentException("File tối đa 1000 dòng dữ liệu");
            Row header=sheet.getRow(0);
            if(header==null || header.getLastCellNum()>32)throw new IllegalArgumentException("Header Excel không hợp lệ");
            DataFormatter formatter=new DataFormatter(Locale.ROOT);
            Map<String,Integer> columns=new HashMap<>();
            for(int i=0;i<header.getLastCellNum();i++) {
                Cell cell=header.getCell(i);
                if(cell!=null && cell.getCellType()==CellType.FORMULA)throw new IllegalArgumentException("Header không được có công thức");
                String key=formatter.formatCellValue(cell).trim();
                if(key.isEmpty())continue;
                if(!COLUMNS.contains(key) || columns.putIfAbsent(key,i)!=null)
                    throw new IllegalArgumentException("Cột không hỗ trợ hoặc bị trùng: "+key);
            }
            if(!columns.keySet().containsAll(COLUMNS))throw new IllegalArgumentException("Thiếu cột theo file mẫu Lead");
            List<ImportRow> result=new ArrayList<>();long characters=0;
            for(int index=1;index<=sheet.getLastRowNum();index++) {
                Row row=sheet.getRow(index);if(row==null)continue;
                Map<String,String> values=new HashMap<>();List<FieldError> errors=new ArrayList<>();boolean hasData=false;
                if(row.getLastCellNum()>32)errors.add(new FieldError("row","Dòng vượt quá 32 cột"));
                for(String column:COLUMNS) {
                    Cell cell=row.getCell(columns.get(column));String value="";
                    if(cell!=null) {
                        if(cell.getCellType()==CellType.FORMULA || cell.getCellType()==CellType.ERROR) {
                            errors.add(new FieldError(column,"Không chấp nhận công thức hoặc ô lỗi"));hasData=true;
                        } else {
                            value=formatter.formatCellValue(cell).trim();
                            if(column.equals("phone") && cell.getCellType()==CellType.NUMERIC)
                                errors.add(new FieldError(column,"Phone phải định dạng Text để giữ số 0 đầu"));
                        }
                    }
                    characters+=value.length();
                    if(characters>1_000_000)throw new IllegalArgumentException("Tổng nội dung ô Excel vượt 1 triệu ký tự");
                    if(!value.isEmpty())hasData=true;
                    if(value.length()>4096) {errors.add(new FieldError(column,"Nội dung ô vượt 4096 ký tự"));value=value.substring(0,4096);}
                    values.put(column,value);
                }
                for(int i=0;i<Math.min(row.getLastCellNum(),32);i++)if(!columns.containsValue(i)) {
                    Cell cell=row.getCell(i);
                    if(cell!=null && (cell.getCellType()==CellType.FORMULA || !formatter.formatCellValue(cell).isBlank())) {
                        hasData=true;errors.add(new FieldError("row","Dữ liệu ở cột không có tiêu đề"));
                    }
                }
                if(!hasData && errors.isEmpty())continue;
                String code=values.get("sourceCode");Long source=sourceIds.get(code);
                if(source==null)errors.add(new FieldError("sourceCode",code.isBlank()?"Thiếu nguồn Lead":"Nguồn Lead không tồn tại hoặc đã ngừng hoạt động"));
                LeadWriteRequest lead=new LeadWriteRequest(values.get("fullName"),values.get("companyName"),
                        values.get("email"),values.get("phone"),source,values.get("notes"));
                try {lead=LeadValidation.normalize(lead);}
                catch(LeadValidationException error) {
                    // Excel presents source errors against sourceCode, not an invisible ID column.
                    errors.addAll(error.errors().stream().filter(e->!e.field().equals("sourceId")).toList());
                }
                result.add(new ImportRow(index+1,lead,errors));
            }
            if(result.isEmpty())throw new IllegalArgumentException("File Excel không có dòng dữ liệu");
            return List.copyOf(result);
        } catch(IllegalArgumentException error) {throw error;}
        catch(Exception error) {throw new IllegalArgumentException("File .xlsx không hợp lệ hoặc bị hỏng",error);}
    }
}
