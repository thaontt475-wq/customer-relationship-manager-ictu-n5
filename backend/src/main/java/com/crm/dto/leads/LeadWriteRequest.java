package com.crm.dto.leads;

import com.google.gson.JsonObject;
import java.util.Set;

public record LeadWriteRequest(String fullName, String companyName, String email, String phone,
                               Long sourceId, String notes) {
    public static LeadWriteRequest fromJson(JsonObject body) {
        if (body == null) throw new IllegalArgumentException("Thiếu dữ liệu Lead");
        Set<String> fields = Set.of("fullName", "companyName", "email", "phone", "sourceId", "notes");
        for (String key : body.keySet()) if (!fields.contains(key))
            throw new IllegalArgumentException("Field không hỗ trợ: " + key);
        Long source = null;
        if (body.has("sourceId") && !body.get("sourceId").isJsonNull()) {
            var value = body.get("sourceId");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                    || !value.toString().matches("[1-9][0-9]*"))
                throw new IllegalArgumentException("sourceId phải là số nguyên dương");
            try { source = Long.parseLong(value.getAsString()); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("sourceId vượt phạm vi Long"); }
        }
        return new LeadWriteRequest(string(body,"fullName"), string(body,"companyName"),
                string(body,"email"), string(body,"phone"), source, string(body,"notes"));
    }
    private static String string(JsonObject body, String key) {
        if (!body.has(key) || body.get(key).isJsonNull()) return null;
        var value = body.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException(key + " phải là chuỗi hoặc null");
        return value.getAsString();
    }
}
