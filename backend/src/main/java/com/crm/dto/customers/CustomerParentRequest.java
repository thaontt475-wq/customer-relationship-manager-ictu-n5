package com.crm.dto.customers;

import com.google.gson.JsonObject;

public final class CustomerParentRequest {
    private CustomerParentRequest() {}
    public static Long parentId(JsonObject body) {
        if (body == null || !body.has("parentCustomerId")) throw new IllegalArgumentException("Thiếu parentCustomerId");
        var value = body.get("parentCustomerId");
        if (value.isJsonNull()) return null;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                || !value.getAsString().matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("parentCustomerId phải là số nguyên dương hoặc null");
        }
        try { return Long.parseLong(value.getAsString()); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("parentCustomerId vượt giới hạn"); }
    }
}
