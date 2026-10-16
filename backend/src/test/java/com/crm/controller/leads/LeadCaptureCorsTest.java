package com.crm.controller.leads;

import com.crm.filter.CorsFilter;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LeadCaptureCorsTest {
    private Map<String,String> headers(String origin,String method,String path) throws Exception {
        Map<String,String> headers=new HashMap<>();
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()) {
            case "getHeader"->a[0].equals("Origin")?origin:a[0].equals("Access-Control-Request-Method")?"POST":null;
            case "getMethod"->method;case "getContextPath"->"/crm";case "getRequestURI"->"/crm/api/leads"+path;default->null;
        });
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if(m.getName().equals("setHeader"))headers.put((String)a[0],(String)a[1]);return null;
        });
        new CorsFilter().doFilter(req,resp,(r,s)->{});return headers;
    }
    @Test void externalSitesOnlyHavePublicSubmissionAccess() throws Exception {
        for(String path:List.of("/web-form","/web-form/submit")) {
            assertEquals("https://example.invalid",headers("https://example.invalid","OPTIONS",path).get("Access-Control-Allow-Origin"));
            assertNotEquals("true",headers("https://example.invalid","POST",path).get("Access-Control-Allow-Credentials"));
        }
        assertNull(headers("https://example.invalid","POST","").get("Access-Control-Allow-Origin"));
        assertNull(headers("https://example.invalid","GET","/embed-code").get("Access-Control-Allow-Origin"));
        assertNull(headers("https://example.invalid","GET","/web-form").get("Access-Control-Allow-Origin"));
    }
    @Test void sameOriginRequestsDoNotNeedAnOriginHeader() throws Exception {
        assertNull(headers(null,"GET","").get("Access-Control-Allow-Origin"));
        assertNull(headers(null,"POST","/web-form").get("Access-Control-Allow-Origin"));
    }
    @Test void developmentOriginSupportsSessionCookies() throws Exception {
        assertEquals("true",headers("http://localhost:5500","GET","").get("Access-Control-Allow-Credentials"));
    }
}
