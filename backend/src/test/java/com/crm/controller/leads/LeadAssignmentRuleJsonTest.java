package com.crm.controller.leads;

import com.crm.model.leads.AssignmentRule;
import com.crm.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LeadAssignmentRuleJsonTest {

    @Test
    void testSanitizeRuleJsonWithRuleCodeStringId() throws Exception {
        LeadServlet servlet = new LeadServlet();
        Method sanitizeMethod = LeadServlet.class.getDeclaredMethod("sanitizeRuleJson", JsonObject.class);
        sanitizeMethod.setAccessible(true);

        String json = "{\"id\":\"RULE-6581\",\"name\":\"SOS Rule\",\"region\":\"NORTH\",\"industry\":\"CNTT\"," +
                "\"mechanism\":\"ROUND_ROBIN\",\"priority\":6,\"reps\":[\"USR-101\",\"USR-102\"],\"description\":\"test note\",\"isActive\":true}";
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        // Before sanitize: obj has "id": "RULE-6581"
        assertEquals("RULE-6581", obj.get("id").getAsString());

        // Invoke sanitize
        sanitizeMethod.invoke(servlet, obj);

        // After sanitize: "id" should be removed so rule.getId() is null (insert mode)
        assertFalse(obj.has("id"), "Non-numeric id must be removed");
        assertTrue(obj.has("ruleCode"), "ruleCode should be populated from raw id");
        assertEquals("RULE-6581", obj.get("ruleCode").getAsString());

        // Deserialization with Gson must succeed without NumberFormatException!
        AssignmentRule rule = JsonUtil.getGson().fromJson(obj, AssignmentRule.class);
        assertNotNull(rule);
        assertNull(rule.getId(), "Rule id must be null for new insert");
        assertEquals("RULE-6581", rule.getRuleCode());
        assertEquals("SOS Rule", rule.getName());
        assertEquals(6, rule.getPriority());
        assertEquals(2, rule.getReps().size());
    }

    @Test
    void testSanitizeRuleJsonWithNumericStringId() throws Exception {
        LeadServlet servlet = new LeadServlet();
        Method sanitizeMethod = LeadServlet.class.getDeclaredMethod("sanitizeRuleJson", JsonObject.class);
        sanitizeMethod.setAccessible(true);

        String json = "{\"id\":\"42\",\"name\":\"Existing Rule\",\"priority\":2,\"reps\":[]}";
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        sanitizeMethod.invoke(servlet, obj);

        // Numeric string "42" must be preserved as numeric 42
        assertTrue(obj.has("id"));
        assertEquals(42L, obj.get("id").getAsLong());

        AssignmentRule rule = JsonUtil.getGson().fromJson(obj, AssignmentRule.class);
        assertNotNull(rule);
        assertEquals(42L, rule.getId());
    }

    @Test
    void testBatchArrayDeserializationWithMixedIds() throws Exception {
        LeadServlet servlet = new LeadServlet();
        Method sanitizeMethod = LeadServlet.class.getDeclaredMethod("sanitizeRuleJson", JsonObject.class);
        sanitizeMethod.setAccessible(true);

        String batchJson = "[" +
                "{\"id\":\"RULE-6581\",\"name\":\"New Rule\",\"priority\":6,\"reps\":[\"USR-101\"]}," +
                "{\"id\":1,\"name\":\"Existing Rule 1\",\"priority\":1,\"reps\":[\"USR-102\"]}," +
                "{\"id\":\"2\",\"name\":\"Existing Rule 2\",\"priority\":2,\"reps\":[]}" +
                "]";

        JsonArray array = JsonParser.parseString(batchJson).getAsJsonArray();
        List<AssignmentRule> rules = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            sanitizeMethod.invoke(servlet, obj);
            rules.add(JsonUtil.getGson().fromJson(obj, AssignmentRule.class));
        }

        assertEquals(3, rules.size());
        assertNull(rules.get(0).getId());
        assertEquals("RULE-6581", rules.get(0).getRuleCode());
        assertEquals(1L, rules.get(1).getId());
        assertEquals(2L, rules.get(2).getId());
    }
}
