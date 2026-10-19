package com.crm.service.importer;

import com.crm.dto.customers.CustomerWriteRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.net.URI;
import java.math.BigDecimal;
import java.util.*;

/** Bounded, side-effect-free Excel parsing and field validation. */
public class CustomerExcelReader {
    public static final int MAX_ROWS=1000;
    public static final int MAX_BYTES=5*1024*1024;
    public static final List<String> COLUMNS=List.of("name","taxCode","status","email","phone","website","address","industryId","companySizeId");
    private static final Set<String> STATUSES=com.crm.service.customers.CustomerValidation.STATUSES;
    public record ImportRow(int row,Map<String,String> values,List<String> errors) {
        public ImportRow {values=Collections.unmodifiableMap(new LinkedHashMap<>(values));errors=List.copyOf(errors);}
    }

    public void writeTemplate(OutputStream output) throws IOException {
        writeTemplate(output, false);
    }

    public void writeTemplate(OutputStream output, boolean withSampleData) throws IOException {
        try(XSSFWorkbook book=new XSSFWorkbook()) {
            Sheet sheet=book.createSheet("Customers");
            sheet.setDisplayGridlines(true);

            CellStyle textStyle=book.createCellStyle();
            textStyle.setDataFormat(book.createDataFormat().getFormat("@"));
            Font fontData=book.createFont();
            fontData.setFontName("Segoe UI");
            textStyle.setFont(fontData);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(textStyle, IndexedColors.GREY_25_PERCENT.getIndex());

            CellStyle headerStyle=book.createCellStyle();
            Font font=book.createFont();
            font.setFontName("Segoe UI");
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle, IndexedColors.GREY_40_PERCENT.getIndex());

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

            Row header=sheet.createRow(0);
            header.setHeightInPoints(28);
            for(int i=0;i<COLUMNS.size();i++) {
                sheet.setDefaultColumnStyle(i,textStyle);
                Cell cell=header.createCell(i);
                cell.setCellValue(COLUMNS.get(i));
                cell.setCellStyle(headerStyle);
            }


            if (withSampleData) {
                String[][] sampleRows = {
                    {"Công ty Cổ phần Công nghệ NovaTech", "0108923456", "CHINH_THUC", "contact@novatech.vn", "02438889999", "https://novatech.vn", "Tầng 8, Tòa Keangnam, Mễ Trì, Hà Nội", "1", "3"},
                    {"Tập đoàn Bán lẻ Sao Việt", "0312456789", "DANG_GIAO_DICH", "info@saovietretail.com", "02839998888", "https://saoviet.com.vn", "123 Nguyễn Thị Minh Khai, Quận 1, TP.HCM", "2", "4"},
                    {"Công ty TNHH Giải pháp Số Nam Á", "0401889922", "TIEM_NANG", "admin@nama-solutions.vn", "0988776655", "https://nama-solutions.vn", "45 Lê Duẩn, Quận Hải Châu, Đà Nẵng", "1", "2"},
                    {"Tổng Công ty Cơ Khí Chế Tạo Miền Bắc", "0200112233", "NGUNG_HOP_TAC", "vanphong@cokhimienbac.vn", "02253778899", "https://cokhimienbac.vn", "Lô 5 KCN Đình Vũ, Hải Phòng", "3", "5"}
                };

                for (int r = 0; r < sampleRows.length; r++) {
                    Row row = sheet.createRow(r + 1);
                    row.setHeightInPoints(22);
                    boolean isZebra = (r % 2 == 1);
                    for (int c = 0; c < sampleRows[r].length; c++) {
                        Cell cell = row.createCell(c);
                        cell.setCellValue(sampleRows[r][c]);
                        if (c == 1 || c == 2 || c == 4 || c == 7 || c == 8) {
                            cell.setCellStyle(isZebra ? zebraCenterStyle : centerStyle);
                        } else {
                            cell.setCellStyle(isZebra ? zebraTextStyle : textStyle);
                        }
                    }
                }
            }

            int[] widths = {10000, 4800, 4800, 7500, 5200, 7000, 11000, 3800, 4000};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0,1);
            book.write(output);
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


    public List<ImportRow> read(InputStream input) throws IOException {
        byte[] bytes=BoundedXlsx.read(input);
        try(XSSFWorkbook book=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if(book.getNumberOfSheets()!=1) throw new IllegalArgumentException("File phải có đúng một worksheet Customers");
            Sheet sheet=book.getSheetAt(0);
            if(sheet.getLastRowNum()>MAX_ROWS) throw new IllegalArgumentException("File tối đa 1000 dòng dữ liệu");
            Row header=sheet.getRow(0);
            if(header==null || header.getLastCellNum()>32) throw new IllegalArgumentException("Dòng tiêu đề không hợp lệ");
            Map<String,Integer> columns=new HashMap<>();
            DataFormatter format=new DataFormatter(Locale.ROOT);
            for(int i=0;i<header.getLastCellNum();i++) {
                Cell cell=header.getCell(i);
                if(cell!=null && cell.getCellType()==CellType.FORMULA) throw new IllegalArgumentException("Tiêu đề không được chứa công thức");
                String key=format.formatCellValue(cell).trim();
                if(key.isEmpty()) continue;
                if(!COLUMNS.contains(key)) throw new IllegalArgumentException("Cột không hỗ trợ: "+key);
                if(columns.putIfAbsent(key,i)!=null) throw new IllegalArgumentException("Cột bị trùng: "+key);
            }
            if(!columns.keySet().containsAll(COLUMNS)) throw new IllegalArgumentException("Thiếu cột theo file mẫu: "+String.join(", ",COLUMNS));
            List<ImportRow> result=new ArrayList<>();
            long totalCharacters=0;
            for(int index=1;index<=sheet.getLastRowNum();index++) {
                Row row=sheet.getRow(index);
                if(row==null) continue;
                Map<String,String> values=new LinkedHashMap<>();
                List<String> errors=new ArrayList<>();
                boolean hasData=false;
                if(row.getLastCellNum()>32) errors.add("Dòng vượt quá 32 cột");
                for(String column:COLUMNS) {
                    Cell cell=row.getCell(columns.get(column));
                    String value="";
                    if(cell!=null) {
                        if(cell.getCellType()==CellType.FORMULA || cell.getCellType()==CellType.ERROR) {
                            errors.add(column+": không chấp nhận công thức hoặc ô lỗi");hasData=true;
                        } else if(cell.getCellType()==CellType.NUMERIC && Set.of("taxCode","phone").contains(column)) {
                            // Text avoids silently losing leading zeros and Excel's numeric rounding.
                            errors.add(column+": phải định dạng Text để giữ chính xác dữ liệu");hasData=true;
                            value=BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
                        } else value=format.formatCellValue(cell).trim();
                    }
                    totalCharacters+=value.length();
                    if(totalCharacters>1_000_000) throw new IllegalArgumentException("Tổng nội dung ô Excel vượt 1 triệu ký tự");
                    if(!value.isEmpty()) hasData=true;
                    if(value.length()>4096) {errors.add(column+": dữ liệu quá dài");value=value.substring(0,4096);}
                    values.put(column,value);
                }
                int knownWidth=columns.values().stream().max(Integer::compareTo).orElse(0)+1;
                for(int i=0;i<Math.min(row.getLastCellNum(),32);i++) {
                    if(i>=knownWidth || !columns.containsValue(i)) {
                        if(!format.formatCellValue(row.getCell(i)).isBlank()) {errors.add("Dữ liệu ở cột không có tiêu đề");hasData=true;}
                    }
                }
                if(!hasData && errors.isEmpty()) continue;
                if(values.get("status").isBlank()) values.put("status","TIEM_NANG");
                errors.addAll(validate(values));
                result.add(new ImportRow(index+1,values,errors));
            }
            // Reject all repeated keys within a file, rather than choosing an arbitrary winner.
            Map<String,Integer> occurrences=new HashMap<>();
            for(ImportRow row:result) if(!row.values().get("taxCode").isBlank()) occurrences.merge(row.values().get("taxCode").replace("-",""),1,Integer::sum);
            List<ImportRow> validated=new ArrayList<>();
            for(ImportRow row:result) {
                List<String> errors=new ArrayList<>(row.errors());
                if(occurrences.getOrDefault(row.values().get("taxCode").replace("-",""),0)>1) errors.add("taxCode bị trùng trong file Excel");
                validated.add(new ImportRow(row.row(),row.values(),errors));
            }
            return validated;
        } catch(IllegalArgumentException e) {throw e;}
        catch(Exception e) {throw new IllegalArgumentException("File .xlsx không hợp lệ hoặc bị hỏng",e);}
    }

    private List<String> validate(Map<String,String> v) {
        List<String> errors=new ArrayList<>();
        if(v.get("name").isBlank()) errors.add("name là bắt buộc");
        Map<String,Integer> lengths=Map.of("name",255,"taxCode",50,"status",50,"email",255,"phone",50,"website",255,"address",500);
        for(var field:lengths.entrySet()) if(v.get(field.getKey()).length()>field.getValue()) errors.add(field.getKey()+": tối đa "+field.getValue()+" ký tự");
        String tax=v.get("taxCode");
        if(!tax.isBlank() && !tax.matches("[0-9]{10}(-?[0-9]{3})?")) errors.add("taxCode phải gồm 10 chữ số hoặc mã chi nhánh 13 chữ số");
        if(!STATUSES.contains(v.get("status"))) errors.add("status phải là TIEM_NANG, DANG_GIAO_DICH, CHINH_THUC hoặc NGUNG_HOP_TAC");
        String email=v.get("email");
        if(!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) errors.add("email không đúng định dạng");
        String phone=v.get("phone");
        if(!phone.isBlank() && (!phone.matches("[+()0-9 .-]+") || phone.replaceAll("\\D","").length()<6 || phone.replaceAll("\\D","").length()>20)) errors.add("phone không đúng định dạng");
        if(!v.get("website").isBlank()) {
            try {
                URI uri=URI.create(v.get("website"));
                if(!Set.of("http","https").contains(String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT)) || uri.getHost()==null || uri.getUserInfo()!=null) errors.add("website phải là URL http/https hợp lệ");
            } catch(IllegalArgumentException e) {errors.add("website không đúng định dạng");}
        }
        for(String key:List.of("industryId","companySizeId")) {
            if(!v.get(key).isBlank()) {
                try {
                    if(!v.get(key).matches("[1-9][0-9]*") || Long.parseLong(v.get(key))<=0) errors.add(key+": phải là số nguyên dương");
                } catch(NumberFormatException e) {errors.add(key+": phải là số nguyên dương");}
            }
        }
        return errors;
    }

    public CustomerWriteRequest request(ImportRow row) {
        var v=row.values();var r=new CustomerWriteRequest();
        r.setName(v.get("name"));r.setTaxCode(nullable(v.get("taxCode")));
        r.setStatus(v.get("status"));r.setEmail(nullable(v.get("email")));r.setPhone(nullable(v.get("phone")));
        r.setWebsite(nullable(v.get("website")));r.setAddress(nullable(v.get("address")));
        r.setIndustryId(v.get("industryId").isBlank()?null:Long.valueOf(v.get("industryId")));
        r.setCompanySizeId(v.get("companySizeId").isBlank()?null:Long.valueOf(v.get("companySizeId")));
        return r;
    }
    private String nullable(String value) {return value.isBlank()?null:value;}
}
