package com.crm.config;

import java.io.InputStream;
import java.util.Properties;

public final class SupportRiskConfig {
 private SupportRiskConfig() {}
 public static int threshold() {
  try(InputStream input=SupportRiskConfig.class.getClassLoader().getResourceAsStream("support-risk.properties")) {
   if(input==null) throw new IllegalStateException("Thiếu support-risk.properties");
   Properties properties=new Properties();properties.load(input);
   String override=System.getenv("CRM_SUPPORT_RISK_THRESHOLD");
   return parse(override==null?properties.getProperty("support.risk.threshold"):override);
  } catch(java.io.IOException e) {throw new IllegalStateException("Không đọc được cấu hình risk",e);}
 }
 public static int parse(String value) {
  try {
   if(value==null || !value.matches("[1-9][0-9]*")) throw new NumberFormatException();
   int result=Integer.parseInt(value);
   if(result>10000) throw new NumberFormatException();
   return result;
  } catch(NumberFormatException e) {throw new IllegalStateException("Ngưỡng risk phải là số nguyên từ 1 đến 10000");}
 }
}
