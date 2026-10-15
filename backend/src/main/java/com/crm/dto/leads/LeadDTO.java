package com.crm.dto.leads;

import java.math.BigDecimal;
import java.util.Map;

public class LeadDTO {
    public Long id;
    public String code;
    public String fullName;
    public String title;
    public String buyingRole;
    public String phone;
    public String email;
    public String companyName;
    public String industry;
    public String taxCode;
    public String companyPhone;
    public String address;
    public String website;
    public Integer score;
    public String tier;
    public BigDecimal estimatedValue;
    public String qualifiedAt;
    public String assignedSales;
    public String notes;
    public String status;
    public boolean isLocked;
    public String convertedAt;
    public String convertedBy;
    public Map<String, Object> convertedTo;

    public LeadDTO() {}
}
