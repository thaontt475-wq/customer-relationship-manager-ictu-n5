package com.crm.dto.leads;

import java.util.Map;

public class LeadConvertDuplicateCheckDTO {
    public boolean hasDuplicate;
    public Map<String, Object> duplicateCustomer;
    public String reason;

    public LeadConvertDuplicateCheckDTO() {}

    public LeadConvertDuplicateCheckDTO(boolean hasDuplicate, Map<String, Object> duplicateCustomer, String reason) {
        this.hasDuplicate = hasDuplicate;
        this.duplicateCustomer = duplicateCustomer;
        this.reason = reason;
    }
}
