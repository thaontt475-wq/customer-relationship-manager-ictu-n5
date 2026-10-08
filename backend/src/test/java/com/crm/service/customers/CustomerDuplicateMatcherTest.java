package com.crm.service.customers;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerDuplicateMatcherTest {
 @Test void normalizesTaxAndWebsitesWithoutMatchingBlanks() {
  assertEquals("0123456789001",CustomerDuplicateMatcher.tax(" 0123456789-001 "));
  assertEquals("fpt.vn",CustomerDuplicateMatcher.website("HTTPS://WWW.FPT.VN/path?x=1"));
  assertEquals("fpt.vn",CustomerDuplicateMatcher.website("fpt.vn/other"));
  assertEquals("",CustomerDuplicateMatcher.website("javascript:alert(1)"));
  assertEquals(0,CustomerDuplicateMatcher.similarity("Công ty TNHH",""));
 }
 @Test void namesIgnoreVietnameseAccentsAndLegalForms() {
  assertEquals(1,CustomerDuplicateMatcher.similarity("Công ty TNHH Công nghệ Đại Việt","CONG NGHE DAI VIET"),0.0001);
  assertTrue(CustomerDuplicateMatcher.similarity("FPT Telecom","Công ty FPT Telecom Việt Nam")<0.65);
 }
 @Test void returnsReasonsForTaxWebsiteAndSimilarName() {
  Map<String,Object> a=Map.of("name","Công ty FPT","taxCode","0123456789-001","website","https://www.fpt.vn/a");
  Map<String,Object> b=Map.of("name","FPT","taxCode","0123456789001","website","http://fpt.vn");
  var result=CustomerDuplicateMatcher.match(a,b);
  assertEquals(3,((List<?>)result.get("reasons")).size());
  assertNull(CustomerDuplicateMatcher.match(a,Map.of("name","Unrelated Business","taxCode","9999999999","website","https://elsewhere.invalid")));
 }
}
