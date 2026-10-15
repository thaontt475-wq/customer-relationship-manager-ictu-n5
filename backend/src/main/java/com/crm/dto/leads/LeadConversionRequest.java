package com.crm.dto.leads;

import java.math.BigDecimal;

public class LeadConversionRequest {
    public String customerMode; // "CREATE_NEW" or "MERGE_EXISTING"
    public Long existingCustomerId;
    public CustomerData customer;
    public ContactData contact;
    public DealData deal;

    public static class CustomerData {
        public String name;
        public String taxCode;
        public String industry;
        public String phone;
        public String website;
        public String address;
    }

    public static class ContactData {
        public String fullName;
        public String title;
        public String buyingRole;
        public String phone;
        public String email;
        public String notes;
    }

    public static class DealData {
        public boolean createDeal;
        public String name;
        public BigDecimal amount;
        public String closeDate; // yyyy-MM-dd
        public String pipeline;
        public Long stageId;
        public String stageName;
        public Integer probability;
    }
}
