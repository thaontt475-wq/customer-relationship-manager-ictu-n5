package com.crm.dto.customers;

import com.google.gson.JsonObject;

public class CustomerMergeRequest {
    public Long masterId;
    public Long secondaryId;
    public JsonObject fieldOverrides;
}
