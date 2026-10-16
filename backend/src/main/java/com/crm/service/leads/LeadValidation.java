package com.crm.service.leads;

import com.crm.dto.leads.*;
import com.crm.dto.leads.LeadValidationException.FieldError;
import java.util.*;

public final class LeadValidation {
    private LeadValidation() {}
    public static LeadWriteRequest normalize(LeadWriteRequest request) {
        if (request == null) throw new IllegalArgumentException("Thiếu dữ liệu Lead");
        List<FieldError> errors = new ArrayList<>();
        String name = value(request.fullName(),"fullName",150,errors);
        String company = value(request.companyName(),"companyName",255,errors);
        String email = value(request.email(),"email",255,errors);
        String phone = value(request.phone(),"phone",50,errors);
        String notes = value(request.notes(),"notes",2000,errors);
        if (name == null) errors.add(new FieldError("fullName","Tên Lead là bắt buộc"));
        if (request.sourceId() == null || request.sourceId() <= 0)
            errors.add(new FieldError("sourceId","Nguồn Lead là bắt buộc và phải là ID dương"));
        if (email == null && phone == null) errors.add(new FieldError("contact","Cần ít nhất email hoặc số điện thoại"));
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            errors.add(new FieldError("email","email không đúng định dạng"));
        if (phone != null && (!phone.matches("[+()0-9 .-]+") || phone.replaceAll("\\D","").length() < 6
                || phone.replaceAll("\\D","").length() > 20))
            errors.add(new FieldError("phone","phone không đúng định dạng"));
        if (!errors.isEmpty()) throw new LeadValidationException(errors);
        return new LeadWriteRequest(name,company,email,phone,request.sourceId(),notes);
    }
    private static String value(String value,String field,int max,List<FieldError> errors) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        if (result.codePointCount(0,result.length()) > max)
            errors.add(new FieldError(field,field+": tối đa "+max+" ký tự"));
        return result;
    }
}
