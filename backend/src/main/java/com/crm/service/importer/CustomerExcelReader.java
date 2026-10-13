package com.crm.service.importer;

import com.crm.dto.customers.CustomerWriteRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.net.URI;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.*;

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
        try(XSSFWorkbook book=new XSSFWorkbook()) {
            Sheet sheet=book.createSheet("Customers");
            CellStyle text=book.createCellStyle();text.setDataFormat(book.createDataFormat().getFormat("@"));
            CellStyle headerStyle=book.createCellStyle();
            Font font=book.createFont();font.setBold(true);headerStyle.setFont(font);
            Row header=sheet.createRow(0);
            for(int i=0;i<COLUMNS.size();i++) {
                sheet.setDefaultColumnStyle(i,text);sheet.setColumnWidth(i,6000);
                Cell cell=header.createCell(i);cell.setCellValue(COLUMNS.get(i));cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0,1);
            book.write(output);
        }
    }

    public List<ImportRow> read(InputStream input) throws IOException {
        byte[] bytes=input.readNBytes(MAX_BYTES+1);
        if(bytes.length==0) throw new IllegalArgumentException("File Excel rỗng");
        if(bytes.length>MAX_BYTES) throw new IllegalArgumentException("File Excel tối đa 5MB");
        checkArchive(bytes);
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

    private void checkArchive(byte[] bytes) throws IOException {
        long total=0;int entries=0;boolean contentTypes=false;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer=new byte[8192];
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                if(++entries>1000) throw new IllegalArgumentException("File Excel có quá nhiều thành phần");
                if(entry.getName().equals("[Content_Types].xml")) contentTypes=true;
                if(entry.getName().toLowerCase(Locale.ROOT).contains("vbaproject")) throw new IllegalArgumentException("Không chấp nhận file có macro");
                long size=0;int n;
                while((n=zip.read(buffer))!=-1) {
                    total+=n;size+=n;
                    if(total>64L*1024*1024 || size>20L*1024*1024) throw new IllegalArgumentException("Nội dung giải nén Excel vượt giới hạn");
                }
            }
        } catch(ZipException e) {throw new IllegalArgumentException("File .xlsx không hợp lệ",e);}
        if(!contentTypes) throw new IllegalArgumentException("File phải có định dạng Excel .xlsx");
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
