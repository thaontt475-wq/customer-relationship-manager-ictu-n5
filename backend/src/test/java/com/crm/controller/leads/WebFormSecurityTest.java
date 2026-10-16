package com.crm.controller.leads;

import com.crm.filter.AuthenticationFilter;
import com.crm.util.IpRateLimiter;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class WebFormSecurityTest {
    private record Result(int status, String body, boolean passed) {}
    private Result invoke(String method, String path, String body, boolean filter) throws Exception {
        StringWriter output = new StringWriter(); int[] status = {200}; boolean[] passed = {false};
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class},
                (p,m,a) -> switch(m.getName()) {
                    case "getMethod" -> method;
                    case "getRequestURI" -> "/crm/api/leads" + path;
                    case "getContextPath" -> "/crm";
                    case "getPathInfo" -> path;
                    case "getContentType" -> "application/json";
                    case "getRemoteAddr" -> "127.0.0.1";
                    case "getReader" -> new BufferedReader(new StringReader(body));
                    default -> null;
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class},
                (p,m,a) -> {
                    if(m.getName().equals("setStatus")) status[0]=(Integer)a[0];
                    return m.getName().equals("getWriter") ? new PrintWriter(output) : null;
                });
        if(filter) new AuthenticationFilter().doFilter(request,response,(req,res)->passed[0]=true);
        else new LeadServlet(null,new IpRateLimiter(100,60_000)).service(request,response);
        return new Result(status[0],output.toString(),passed[0]);
    }
    @Test void anonymousManagementRoutesAreNotPublic() throws Exception {
        for(String path : new String[]{"", "/embed-code", "/1", "/web-form/extra"}) {
            Result r=invoke("GET",path,"",true);
            assertEquals(401,r.status); assertFalse(r.passed);
        }
    }
    @Test void onlyExactSubmissionRoutesAndHealthArePublic() throws Exception {
        assertTrue(invoke("POST","/web-form","{}",true).passed);
        assertTrue(invoke("POST","/web-form/submit","{}",true).passed);
        assertEquals(401,invoke("POST","","{}",true).status);
        assertTrue(invoke("GET","/health","",true).passed);
        assertEquals(401,invoke("POST","/web-form/extra","{}",true).status);
    }
    @Test void malformedJsonIsClientError() throws Exception {
        Result r=invoke("POST","/web-form","{broken",false);
        assertEquals(400,r.status); assertTrue(r.body.contains("false"));
    }
    @Test void servletAlsoRejectsAnonymousManagement() throws Exception {
        assertEquals(401,invoke("GET","/embed-code","",false).status);
    }
    @Test void untrustedProxyHeadersCannotCreateFreshRateLimitBuckets() {
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},
                (p,m,a)->m.getName().equals("getRemoteAddr") ? "127.0.0.1" : m.getName().equals("getHeader") ? "203.0.113.99" : null);
        assertEquals("127.0.0.1",IpRateLimiter.extractClientIp(req));
        IpRateLimiter limiter = new IpRateLimiter(5,60_000);
        for(int i=0;i<5;i++) assertTrue(limiter.tryAcquire(IpRateLimiter.extractClientIp(req)));
        assertFalse(limiter.tryAcquire(IpRateLimiter.extractClientIp(req)));
    }
}
