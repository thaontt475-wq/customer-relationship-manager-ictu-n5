package com.crm.dto.leads;

import java.util.List;

public class LeadValidationException extends IllegalArgumentException {
    public record FieldError(String field, String message) {}
    private final List<FieldError> errors;
    public LeadValidationException(List<FieldError> errors) {
        super("Dữ liệu Lead không hợp lệ");
        this.errors = List.copyOf(errors);
    }
    public List<FieldError> errors() { return errors; }
}
