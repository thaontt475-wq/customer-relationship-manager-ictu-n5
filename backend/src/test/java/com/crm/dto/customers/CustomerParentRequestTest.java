package com.crm.dto.customers;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerParentRequestTest {
    private Long parse(String json) { return CustomerParentRequest.parentId(JsonParser.parseString(json).getAsJsonObject()); }
    @Test void nullRemovesParent() { assertNull(parse("{\"parentCustomerId\":null}")); }
    @Test void preservesLargeIdWithoutDoubleRounding() { assertEquals(9007199254740993L,parse("{\"parentCustomerId\":9007199254740993}")); }
    @Test void rejectsFractionsStringsBooleansAndOverflow() {
        for (String value : new String[]{"1.5","\"1\"","true","{}","[]","0","-1","9223372036854775808"}) {
            assertThrows(IllegalArgumentException.class,()->parse("{\"parentCustomerId\":"+value+"}"),value);
        }
    }
    @Test void requiresContractKey() { assertThrows(IllegalArgumentException.class,()->parse("{\"parent_customer_id\":1}")); }
}
