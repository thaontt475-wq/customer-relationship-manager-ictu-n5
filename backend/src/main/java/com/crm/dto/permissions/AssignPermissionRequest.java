package com.crm.dto.permissions;

import java.util.List;

public class AssignPermissionRequest {

    private long userId;
    private List<Long> roleIds;
    private String dataScope;

    public long getUserId() {
        return userId;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public String getDataScope() {
        return dataScope;
    }
}