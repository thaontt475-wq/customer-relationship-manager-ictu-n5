package com.crm.dto.customers;

import java.util.Map;

public class CustomerWriteRequest {
    private String name;
    private String companyName;
    private String taxCode;
    private String status;
    private String email;
    private String phone;
    private String website;
    private String address;
    private Long industryId;
    private Long companySizeId;
    private Long ownerUserId;
    private Map<String, Object> customFields;

    public String getName() {
        return name != null && !name.isBlank() ? name : companyName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyName() {
        return companyName != null && !companyName.isBlank() ? companyName : name;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getStatus() {
        return status != null && !status.isBlank() ? status : "TIEM_NANG";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getIndustryId() {
        return industryId;
    }

    public void setIndustryId(Long industryId) {
        this.industryId = industryId;
    }

    public Long getCompanySizeId() {
        return companySizeId;
    }

    public void setCompanySizeId(Long companySizeId) {
        this.companySizeId = companySizeId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
