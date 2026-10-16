package com.crm.service.leads;

import com.crm.dto.leads.*;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeadValidationTest {
    private LeadWriteRequest lead(String email,String phone,Long source) {
        return new LeadWriteRequest("  Nguyễn An  ","  Company ",email,phone,source," note ");
    }
    @Test void trimsFieldsAndSupportsEmailOrPhone() {
        var result=LeadValidation.normalize(lead(" an@example.invalid ",null,1L));
        assertEquals("Nguyễn An",result.fullName());assertNull(result.phone());assertEquals("note",result.notes());
        assertEquals("+84 (90) 123-4567",LeadValidation.normalize(lead(null,"+84 (90) 123-4567",1L)).phone());
    }
    @Test void sourceRequiredAndFieldsHaveUsefulErrors() {
        var error=assertThrows(LeadValidationException.class,()->LeadValidation.normalize(lead("bad",null,null)));
        assertTrue(error.errors().stream().anyMatch(e->e.field().equals("sourceId")));
        assertTrue(error.errors().stream().anyMatch(e->e.field().equals("email")));
    }
    @Test void nameContactAndPhoneValidation() {
        assertThrows(LeadValidationException.class,()->LeadValidation.normalize(new LeadWriteRequest(" ",null,null,null,1L,null)));
        for(String phone:new String[]{"badPhone","12345","1".repeat(21)})
            assertThrows(LeadValidationException.class,()->LeadValidation.normalize(lead(null,phone,1L)));
    }
    @Test void unicodeLengthsCountCodepoints() {
        assertDoesNotThrow(()->LeadValidation.normalize(new LeadWriteRequest("😀".repeat(150),null,"a@b.invalid",null,1L,null)));
        assertThrows(LeadValidationException.class,()->LeadValidation.normalize(new LeadWriteRequest("a".repeat(151),null,"a@b.invalid",null,1L,null)));
    }
    @Test void strictDtoRejectsSpoofedOwnerWrongTypesAndPrecisionLoss() {
        for(String body:new String[]{"{\"ownerUserId\":1}","{\"fullName\":false}","{\"sourceId\":\"1\"}","{\"sourceId\":1.5}","{\"sourceId\":0}","{\"sourceId\":9223372036854775808}"})
            assertThrows(IllegalArgumentException.class,()->LeadWriteRequest.fromJson(JsonParser.parseString(body).getAsJsonObject()));
        assertEquals(9007199254740993L,LeadWriteRequest.fromJson(JsonParser.parseString("{\"sourceId\":9007199254740993}").getAsJsonObject()).sourceId());
    }
}
