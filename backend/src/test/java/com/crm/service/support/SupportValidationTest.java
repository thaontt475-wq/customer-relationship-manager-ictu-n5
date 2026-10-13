package com.crm.service.support;
import com.crm.config.SupportRiskConfig;
import com.crm.dto.support.SupportRequestWriteRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupportValidationTest {
 @Test void validatesConfigAndDoesNotSilentlyDefaultBadThresholds() {
  assertEquals(2,SupportRiskConfig.parse("2"));
  for(String value:new String[]{"0","-1","1.5","abc","10001",""}) assertThrows(IllegalStateException.class,()->SupportRiskConfig.parse(value));
  assertThrows(IllegalStateException.class,()->SupportRiskConfig.parse(null));
 }
 @Test void rejectsInvalidRequiredFieldsAndEnums() {
  assertThrows(IllegalArgumentException.class,()->SupportRequestService.validate(null));
  var r=new SupportRequestWriteRequest();r.title="";r.assigneeId=1L;r.priority="MEDIUM";r.status="NEW";
  assertThrows(IllegalArgumentException.class,()->SupportRequestService.validate(r));
  r.title=" Support ";SupportRequestService.validate(r);assertEquals("Support",r.title);
  r.priority="FAKE";assertThrows(IllegalArgumentException.class,()->SupportRequestService.validate(r));
  r.priority="HIGH";r.status="FAKE";assertThrows(IllegalArgumentException.class,()->SupportRequestService.validate(r));
  r.status="NEW";r.assigneeId=0L;assertThrows(IllegalArgumentException.class,()->SupportRequestService.validate(r));
 }
 @Test void boundsPaginationAndDescription() {
  assertThrows(IllegalArgumentException.class,()->SupportRequestService.pagination(0,20));
  assertThrows(IllegalArgumentException.class,()->SupportRequestService.pagination(1,101));
  var r=new SupportRequestWriteRequest();r.title="Support";r.priority="LOW";r.status="CLOSED";r.assigneeId=1L;r.description="a".repeat(10001);
  assertThrows(IllegalArgumentException.class,()->SupportRequestService.validate(r));
 }
}
