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
            CellStyle text=book.createCellStyle();text.setDataFormat(book.createDataFormat().getFormat("@"));
            Row header=sheet.createRow(0);
            for(int i=0;i<COLUMNS.size();i++) {
                sheet.setDefaultColumnStyle(i,text);sheet.setColumnWidth(i,6000);
                header.createCell(i).setCellValue(COLUMNS.get(i));
            }
            sheet.createFreezePane(0,1);book.write(out);
        }
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
