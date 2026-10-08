package com.crm.dto.organization;

public class OrganizationUnitRequest {

    private String code;
    private String name;
    private String type;
    private Long parentId;
    private Long managerId;
    private String description;
    private Boolean active;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getActive() {
        return active;
    }
}